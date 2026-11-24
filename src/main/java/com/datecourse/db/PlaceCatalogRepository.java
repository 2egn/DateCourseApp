package com.datecourse.db;

import com.datecourse.model.*;
import com.datecourse.service.PlaceSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.Instant;
import java.time.Clock;
import java.time.Duration;
import java.util.*;

/** Full source snapshots are transactional; administrator corrections remain a separate overlay. */
public final class PlaceCatalogRepository implements PlaceSource {
    public record Batch(String sourceKey, Instant generatedAt, List<Place> places) {
        public Batch {
            if (sourceKey == null || !sourceKey.matches("[a-z0-9][a-z0-9_-]{0,99}") || generatedAt == null)
                throw new IllegalArgumentException("자료 식별자와 생성 시각이 필요합니다.");
            places = List.copyOf(places);
            if (places.isEmpty()) throw new IllegalArgumentException("빈 전체 스냅샷은 적재하지 않습니다.");
        }
    }
    public enum ImportMode { FULL_SNAPSHOT, EXPANSION, PRICE_UPDATE }
    public record ImportResult(long importId, int inserted, int updated, int reactivated, int deactivated, int unchanged) {}
    public record PreparedBatch(String fingerprint, SortedMap<String, String> payloads) {
        public PreparedBatch { payloads = Collections.unmodifiableSortedMap(new TreeMap<>(payloads)); }
    }
    private record Existing(long entryId, long recordId, String sourceKey, String hash, String payload, boolean active) {}
    private static final ObjectMapper JSON = new ObjectMapper().enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    // 큰 payload를 한 번에 지나치게 모으지 않도록 쓰기 묶음 크기를 제한한다.
    private static final int WRITE_BATCH_SIZE = 200;
    private static final Duration MAX_CLOCK_SKEW = Duration.ofMinutes(5);
    private final DataSource pool;
    private final Clock clock;
    public PlaceCatalogRepository(DataSource pool) { this(pool,Clock.systemUTC()); }
    PlaceCatalogRepository(DataSource pool,Clock clock) {
        this.pool=Objects.requireNonNull(pool);this.clock=Objects.requireNonNull(clock);
    }

    /** Performs source/model validation without connecting to a database. IDs come from source identities. */
    public static PreparedBatch prepare(Batch batch) throws IOException {
        var payloads = new TreeMap<String, String>();
        for (Place place : batch.places()) {
            String key = place.details().externalIds().getOrDefault("public", place.placeId());
            if (key == null || key.isBlank() || key.length() > 200 || !key.matches("[A-Za-z0-9:_-]+")
                    || place.name() == null || place.name().isBlank() || place.name().length() > 200
                    || !Set.of("MEAL", "CAFE", "CULTURE").contains(place.category())
                    || !Double.isFinite(place.lat()) || !Double.isFinite(place.lng())
                    || Math.abs(place.lat()) > 90 || Math.abs(place.lng()) > 180 || place.lat() == 0 && place.lng() == 0
                    || !Double.isFinite(place.rating()) || place.rating() < 0 || place.rating() > 5
                    || place.details().sourceUrl().isBlank())
                throw new IllegalArgumentException("공공 장소의 ID·이름·분류·좌표·출처를 확인해 주세요.");
            if (place.priceEstimate().evidence() == PriceEstimate.Evidence.GOOGLE_PROVIDED
                    || place.priceEstimate().evidence() == PriceEstimate.Evidence.ADMIN_REVIEWED
                    || place.details().fieldEvidence().values().stream().anyMatch(e -> e.origin() == SourceEvidence.Origin.ADMIN_REVIEW)
                    || place.details().attributeFacts().stream().anyMatch(f -> f.evidence().origin() == SourceEvidence.Origin.ADMIN_REVIEW))
                throw new IllegalArgumentException("외부 상세 값과 관리자 정정 결과는 공공 원본으로 재적재할 수 없습니다.");
            var value = JSON.valueToTree(copy(place, 0, key));
            ((com.fasterxml.jackson.databind.node.ObjectNode)value).remove("priceText");
            String payload = JSON.writeValueAsString(value);
            if (payloads.putIfAbsent(key, payload) != null) throw new IllegalArgumentException("중복된 공공 장소 ID입니다.");
        }
        return new PreparedBatch(hash(JSON.writeValueAsString(payloads)), payloads);
    }

    /** Explicit import setup only; normal reads never create tables. */
    public void initialize() throws SQLException, IOException {
        try (var input = getClass().getResourceAsStream("/db/10_place_catalog.sql")) {
            if (input == null) throw new IOException("장소 카탈로그 스키마를 찾지 못했어요.");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
            try (var connection = pool.getConnection(); var statement = connection.createStatement()) {
                for (String command : sql.split(";")) if (!command.isBlank()) statement.executeUpdate(command);
                statement.executeUpdate("INSERT IGNORE INTO place_catalog_lock(lock_id) VALUES(1)");
            }
        }
    }

    public ImportResult synchronize(Batch batch) throws SQLException, IOException {
        return synchronize(batch, ImportMode.FULL_SNAPSHOT);
    }

    /** Expansion requires a complete retained snapshot; regional fragments cannot retire other regions. */
    public ImportResult synchronize(Batch batch, ImportMode mode) throws SQLException, IOException {
        Objects.requireNonNull(mode);
        var prepared = prepare(batch); // Reject malformed data before schema or row writes.
        // 미래 버전이 다음 정상 적재를 막지 않도록 작은 시계 오차만 허용한다.
        if(batch.generatedAt().isAfter(clock.instant().plus(MAX_CLOCK_SKEW)))
            throw new IllegalArgumentException("자료 생성 시각이 현재보다 너무 미래입니다. UTC 시각과 시스템 시계를 확인해 주세요.");
        initialize();
        try (var connection = pool.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (var lock = connection.prepareStatement("SELECT lock_id FROM place_catalog_lock WHERE lock_id=1 FOR UPDATE"); var ignored = lock.executeQuery()) {
                    if (!ignored.next()) throw new SQLException("자료 갱신 잠금을 얻지 못했습니다.");
                }
                checkVersion(connection, batch, prepared.fingerprint());
                var existing = existing(connection);
                if (mode == ImportMode.PRICE_UPDATE) validatePriceUpdate(batch, prepared, existing);
                if (mode == ImportMode.EXPANSION && existing.entrySet().stream().anyMatch(row ->
                        row.getValue().active() && row.getValue().sourceKey().equals(batch.sourceKey())
                                && !prepared.payloads().containsKey(row.getKey())))
                    throw new IllegalArgumentException("확대 자료에 기존 장소가 빠졌습니다. 기존 전체 목록과 합친 뒤 다시 검증해 주세요.");
                long run = newRun(connection, batch, prepared.fingerprint());
                int inserted=0, updated=0, reactivated=0, unchanged=0, deactivated=0;
                long category = category(connection);
                try (var updates=connection.prepareStatement("UPDATE place_catalog_entries SET payload=?,payload_hash=?,active=TRUE,last_import_id=? WHERE entry_id=?");
                     var deactivations=connection.prepareStatement("UPDATE place_catalog_entries SET active=FALSE,last_import_id=? WHERE entry_id=?");
                     var revisions=connection.prepareStatement("INSERT INTO place_catalog_revisions(import_id,place_key,record_id,change_kind,payload,active) VALUES(?,?,?,?,?,?)")) {
                    int pending=0;
                    for (var row : prepared.payloads().entrySet()) {
                        var previous = existing.get(row.getKey());
                        String digest = hash(row.getValue());
                        if (previous != null && !previous.sourceKey().equals(batch.sourceKey()))
                            throw new IllegalArgumentException("다른 자료가 관리 중인 장소 ID와 충돌합니다.");
                        if (previous == null) {
                            long entry = insertEntry(connection, row.getKey(), batch.sourceKey(), row.getValue(), digest, run);
                            long record = newRecord(connection, category, entry);
                            execute(connection, "UPDATE place_catalog_entries SET record_id=? WHERE entry_id=?", record, entry);
                            addBatch(revisions, run, row.getKey(), record, "INSERT", row.getValue(), true);
                            inserted++;
                        } else if (!previous.active() || !previous.hash().equals(digest)) {
                            addBatch(updates, row.getValue(), digest, run, previous.entryId());
                            addBatch(revisions, run, row.getKey(), previous.recordId(), previous.active()?"UPDATE":"REACTIVATE", row.getValue(), true);
                            if (previous.active()) updated++; else reactivated++;
                        } else { unchanged++; continue; }
                        if (++pending == WRITE_BATCH_SIZE) {
                            flushBatches(updates,deactivations,revisions);
                            pending=0;
                        }
                    }
                    for (var row : existing.entrySet()) {
                        var value = row.getValue();
                        if (value.active() && value.sourceKey().equals(batch.sourceKey()) && !prepared.payloads().containsKey(row.getKey())) {
                            addBatch(deactivations, run, value.entryId());
                            addBatch(revisions, run, row.getKey(), value.recordId(), "DEACTIVATE", value.payload(), false);
                            deactivated++;
                            if (++pending == WRITE_BATCH_SIZE) {
                                flushBatches(updates,deactivations,revisions);
                                pending=0;
                            }
                        }
                    }
                    if(pending>0)flushBatches(updates,deactivations,revisions);
                }
                execute(connection, "DELETE FROM place_catalog_sources WHERE source_key=?", batch.sourceKey());
                execute(connection, "INSERT INTO place_catalog_sources(source_key,generated_at,fingerprint) VALUES(?,?,?)",
                        batch.sourceKey(), batch.generatedAt().toString(), prepared.fingerprint());
                execute(connection, "UPDATE place_catalog_imports SET inserted_count=?,updated_count=?,reactivated_count=?,deactivated_count=?,unchanged_count=? WHERE import_id=?",
                        inserted,updated,reactivated,deactivated,unchanged,run);
                connection.commit();
                return new ImportResult(run,inserted,updated,reactivated,deactivated,unchanged);
            } catch (SQLException | IOException | RuntimeException failure) {
                connection.rollback(); throw failure;
            }
        }
    }
    /** 가격 보완은 잠금 안에서 전체 장소·가격 외 필드의 보존을 확인한 뒤에만 허용한다. */
    private static void validatePriceUpdate(Batch batch, PreparedBatch prepared, Map<String,Existing> existing) throws IOException {
        var active = new TreeMap<String,String>();
        existing.forEach((key,row) -> {
            if (row.active() && row.sourceKey().equals(batch.sourceKey())) active.put(key,row.payload());
        });
        if (!active.keySet().equals(prepared.payloads().keySet()))
            throw new IllegalArgumentException("가격 보완 중 장소 추가·삭제는 허용하지 않습니다.");
        for (var row : active.entrySet()) {
            var before = (com.fasterxml.jackson.databind.node.ObjectNode)JSON.readTree(row.getValue());
            var after = (com.fasterxml.jackson.databind.node.ObjectNode)JSON.readTree(prepared.payloads().get(row.getKey()));
            before.remove("priceEstimate");
            after.remove("priceEstimate");
            if (!before.equals(after)) throw new IllegalArgumentException("가격 보완 중 장소 정보 변경은 허용하지 않습니다.");
        }
    }
    private void checkVersion(Connection c, Batch batch, String fingerprint) throws SQLException {
        try (var query = c.prepareStatement("SELECT source_key,generated_at,fingerprint FROM place_catalog_sources WHERE source_key=?")) {
            query.setString(1,batch.sourceKey());
            try (var row=query.executeQuery()) { if (row.next()) {
                if (!row.getString("source_key").equals(batch.sourceKey())) throw new IllegalArgumentException("자료 식별자의 대소문자가 충돌합니다.");
                Instant previous=Instant.parse(row.getString("generated_at"));
                if (batch.generatedAt().isBefore(previous) || batch.generatedAt().equals(previous) && !fingerprint.equals(row.getString("fingerprint")))
                    throw new IllegalArgumentException("이전 자료 또는 같은 생성 시각의 다른 자료로 갱신할 수 없습니다.");
            }}
        }
    }
    private Map<String,Existing> existing(Connection c) throws SQLException {
        Map<String,Existing> result=new HashMap<>();
        try (var statement=c.createStatement(); var rows=statement.executeQuery("SELECT * FROM place_catalog_entries")) {
            while(rows.next()) result.put(rows.getString("place_key"),new Existing(rows.getLong("entry_id"),rows.getLong("record_id"),
                    rows.getString("source_key"),rows.getString("payload_hash"),rows.getString("payload"),rows.getBoolean("active")));
        }
        return result;
    }
    private long newRun(Connection c, Batch batch, String fingerprint) throws SQLException {
        return insert(c,"INSERT INTO place_catalog_imports(source_key,generated_at,fingerprint,inserted_count,updated_count,reactivated_count,deactivated_count,unchanged_count) VALUES(?,?,?,0,0,0,0,0)",
                batch.sourceKey(),batch.generatedAt().toString(),fingerprint);
    }
    private long insertEntry(Connection c,String key,String source,String payload,String digest,long run) throws SQLException {
        return insert(c,"INSERT INTO place_catalog_entries(place_key,source_key,payload,payload_hash,active,last_import_id) VALUES(?,?,?,?,TRUE,?)",key,source,payload,digest,run);
    }
    private long category(Connection c) throws SQLException {
        execute(c,"INSERT IGNORE INTO categories(category_code,category_name) VALUES('MANAGED_PLACE_CATALOG','공공 장소 카탈로그')");
        try(var statement=c.createStatement();var rows=statement.executeQuery("SELECT category_id FROM categories WHERE category_code='MANAGED_PLACE_CATALOG'")) {
            if(!rows.next())throw new SQLException("장소 자료 분류를 만들지 못했습니다.");return rows.getLong(1);
        }
    }
    private long newRecord(Connection c,long category,long entry) throws SQLException {
        return insert(c,"INSERT INTO data_records(category_id,source_file,source_row_number,data_payload) VALUES(?,'managed_place_catalog_v1',?,'{}')",category,entry);
    }
    /** 같은 SQL의 매개변수를 쌓고 실행은 제한된 묶음 단위로 처리한다. */
    private static void addBatch(PreparedStatement statement,Object... values) throws SQLException {
        parameters(statement,values);
        statement.addBatch();
    }
    /** 어느 묶음이 실패해도 호출부의 전체 트랜잭션을 롤백한다. */
    private static void flushBatches(PreparedStatement... statements) throws SQLException {
        for(var statement:statements) {
            for(int result:statement.executeBatch()) {
                if(result==Statement.EXECUTE_FAILED)throw new SQLException("장소 자료 일괄 쓰기에 실패했습니다.");
            }
            statement.clearBatch();
        }
    }
    private static void parameters(PreparedStatement statement,Object... values) throws SQLException {
        for(int i=0;i<values.length;i++)statement.setObject(i+1,values[i]);
    }
    private static void execute(Connection c,String sql,Object... values) throws SQLException {
        try(var statement=c.prepareStatement(sql)){parameters(statement,values);statement.executeUpdate();}
    }
    private static long insert(Connection c,String sql,Object... values) throws SQLException {
        try(var statement=c.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS)) {
            parameters(statement,values);statement.executeUpdate();
            try(var keys=statement.getGeneratedKeys()){if(!keys.next())throw new SQLException("생성된 장소 번호를 찾지 못했습니다.");return keys.getLong(1);}
        }
    }

    /** Empty optional means not migrated. An imported catalog intentionally replaces legacy recommendations. */
    public Optional<List<Place>> findImported() throws SQLException,IOException {
        try(var c=pool.getConnection()) {
            if(!tableExists(c,"place_catalog_sources"))return Optional.empty();
            // One statement gives readers one consistent view while another transaction imports.
            try(var s=c.createStatement();var rows=s.executeQuery("""
                SELECT e.place_key,e.record_id,e.payload,e.payload_hash,e.active FROM place_catalog_sources s
                LEFT JOIN place_catalog_entries e ON e.source_key=s.source_key AND e.active=TRUE
                ORDER BY e.record_id
                """)) {
                var places=new ArrayList<Place>(); boolean imported=false;
                while(rows.next()) {
                    imported=true;
                    if(rows.getString("payload")==null)continue;
                    String payload=rows.getString("payload");
                    if(!hash(payload).equals(rows.getString("payload_hash")))throw new IOException("저장된 장소 자료의 지문이 일치하지 않습니다.");
                    var value=JSON.readValue(payload,Place.class);
                    if (rows.getLong("record_id") <= 0 || !value.placeId().equals(rows.getString("place_key")))
                        throw new IOException("저장된 장소의 식별자가 일치하지 않습니다.");
                    places.add(copy(value,rows.getLong("record_id"),value.placeId()));
                }
                return imported?Optional.of(List.copyOf(places)):Optional.empty();
            }
        }
    }
    @Override public List<Place> findAll() throws SQLException,IOException { return findImported().orElse(List.of()); }
    private boolean tableExists(Connection c,String name) throws SQLException {
        for(String candidate:List.of(name,name.toUpperCase(Locale.ROOT)))
            try(var tables=c.getMetaData().getTables(c.getCatalog(),null,candidate,new String[]{"TABLE"})) {
                while(tables.next())if(name.equalsIgnoreCase(tables.getString("TABLE_NAME")))return true;
            }
        return false;
    }
    private static Place copy(Place p,long record,String key) {
        return new Place(record,key,p.name(),p.category(),p.lat(),p.lng(),p.priceEstimate(),p.rating(),p.details());
    }
    private static String hash(String value) {
        try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}
        catch(NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}
    }
}
