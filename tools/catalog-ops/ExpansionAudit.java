package com.datecourse.db;

import com.datecourse.demo.PublicPlaceImport;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.sql.*;
import java.util.*;

/** 전체 기준본을 확인하고 확장 전후 기존 행과 사용자 데이터를 검증하는 유지보수 도구다. */
public final class ExpansionAudit {
    private static final ObjectMapper JSON = new ObjectMapper();
    private record Entry(long recordId, String payload) {}

    /** 기본 모드는 읽기 전용이며 apply를 지정해야 확장 적재를 수행한다. */
    public static void main(String[] args) {
        try {
            if (args.length != 6 || !Set.of("preflight", "apply", "price-preflight", "price-apply").contains(args[0]))
                throw new IllegalArgumentException("INVALID_ARGUMENTS");
            var baseline = PublicPlaceImport.batch(Path.of(args[1]), Path.of(args[2]));
            var next = PublicPlaceImport.batch(Path.of(args[3]), Path.of(args[4]));
            try (var pool = DatabasePool.create(DbSettings.load())) {
                var report = verify(pool, baseline, next, args[0].endsWith("apply"), args[0].startsWith("price-"));
                Path output = Path.of(args[5]);
                if (output.getParent() != null) Files.createDirectories(output.getParent());
                Files.writeString(output, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(report).replace("\r\n","\n") + "\n", StandardCharsets.UTF_8);
                System.out.println(JSON.writeValueAsString(report));
            }
        } catch (Exception failure) {
            String code = failure.getMessage();
            System.err.println("확장 검증 실패: " + (code != null && code.matches("[A-Z_]+") ? code : failure.getClass().getSimpleName()));
            System.exit(1);
        }
    }

    /** 자료와 DB 기준본을 대조하고 선택적으로 적재·반복 적재·보존 검사를 수행한다. */
    static Map<String,Object> verify(HikariDataSource pool, PlaceCatalogRepository.Batch baseline,
                                    PlaceCatalogRepository.Batch next, boolean apply) throws Exception {
        return verify(pool,baseline,next,apply,false);
    }

    /** 가격 변경 모드에서는 장소 수·ID·가격 외 내용을 고정한다. */
    static Map<String,Object> verify(HikariDataSource pool, PlaceCatalogRepository.Batch baseline,
                                    PlaceCatalogRepository.Batch next, boolean apply, boolean pricesOnly) throws Exception {
        var oldPrepared = PlaceCatalogRepository.prepare(baseline);
        var nextPrepared = PlaceCatalogRepository.prepare(next);
        require(baseline.sourceKey().equals(next.sourceKey()), "SOURCE_CHANGED");
        if (pricesOnly) {
            require(nextPrepared.payloads().keySet().equals(oldPrepared.payloads().keySet()),"PLACE_SET_CHANGED");
            for (var row : oldPrepared.payloads().entrySet()) {
                var oldValue=(com.fasterxml.jackson.databind.node.ObjectNode)JSON.readTree(row.getValue());
                var newValue=(com.fasterxml.jackson.databind.node.ObjectNode)JSON.readTree(nextPrepared.payloads().get(row.getKey()));
                oldValue.remove("priceEstimate");newValue.remove("priceEstimate");
                require(oldValue.equals(newValue),"NON_PRICE_FIELD_CHANGED");
            }
        } else require(nextPrepared.payloads().entrySet().containsAll(oldPrepared.payloads().entrySet()), "BASELINE_CHANGED");
        Map<String,Entry> before;
        Map<String,String> protectedBefore;
        Map<Long,String> recordsBefore;
        try (var connection = pool.getConnection()) {
            connection.setReadOnly(true);
            before = entries(connection, baseline.sourceKey());
            require(payloads(before).equals(oldPrepared.payloads()), "DATABASE_BASELINE_MISMATCH");
            protectedBefore = protectedTables(connection);
            recordsBefore = dataRecords(connection);
        }
        var report = new LinkedHashMap<String,Object>();
        report.put("schemaVersion",1);
        report.put("sourceKey",next.sourceKey());
        report.put("generatedAt",next.generatedAt().toString());
        report.put("baselinePlaces",before.size());
        report.put("stagedPlaces",next.places().size());
        report.put("payloadFingerprint",nextPrepared.fingerprint());
        report.put("baselineMatchesDatabase",true);
        report.put(pricesOnly?"nonPriceFieldsPreserved":"existingPayloadsPreserved",true);
        report.put("pricesOnly",pricesOnly);
        report.put("applied",apply);
        if (!apply) return report;
        var catalog = new PlaceCatalogRepository(pool);
        long started = System.nanoTime();
        var mode=pricesOnly?PlaceCatalogRepository.ImportMode.PRICE_UPDATE:PlaceCatalogRepository.ImportMode.EXPANSION;
        long expectedUpdates=oldPrepared.payloads().entrySet().stream()
            .filter(row->!row.getValue().equals(nextPrepared.payloads().get(row.getKey()))).count();
        var result = catalog.synchronize(next,mode);
        report.put("result",result);
        require(result.updated()==(pricesOnly?expectedUpdates:0) && result.deactivated()==0 && result.reactivated()==0
            && (!pricesOnly || result.inserted()==0), "UNEXPECTED_EXISTING_CHANGE");
        try (var connection = pool.getConnection()) {
            connection.setReadOnly(true);
            var after = entries(connection,next.sourceKey());
            require(payloads(after).equals(nextPrepared.payloads()), "IMPORTED_PAYLOAD_MISMATCH");
            if(pricesOnly) {
                for(var row:before.entrySet()) require(after.get(row.getKey()).recordId()==row.getValue().recordId(),"EXISTING_RECORD_CHANGED");
            } else require(after.entrySet().containsAll(before.entrySet()), "EXISTING_RECORD_CHANGED");
            require(protectedBefore.equals(protectedTables(connection)), "PROTECTED_TABLE_CHANGED");
            require(dataRecords(connection).entrySet().containsAll(recordsBefore.entrySet()), "EXISTING_DATA_RECORD_CHANGED");
            report.put("activeAfter",after.size());
        }
        report.put("effectiveAfter",new InformationReportRepository(pool).apply(catalog.findAll()).size());
        report.put("existingRecordIdsPreserved",true);
        report.put("payloadMismatches",0);
        report.put("unchangedTables",new ArrayList<>(protectedBefore.keySet()));
        var repeat = catalog.synchronize(next,mode);
        require(repeat.inserted()==0 && repeat.updated()==0 && repeat.reactivated()==0 && repeat.deactivated()==0
                && repeat.unchanged()==next.places().size(), "REPEAT_IMPORT_CHANGED_DATA");
        try (var connection = pool.getConnection()) {
            connection.setReadOnly(true);
            require(protectedBefore.equals(protectedTables(connection)), "PROTECTED_TABLE_CHANGED");
            require(dataRecords(connection).entrySet().containsAll(recordsBefore.entrySet()), "EXISTING_DATA_RECORD_CHANGED");
        }
        report.put("repeat",repeat);
        report.put("applyAndVerificationSeconds",(System.nanoTime()-started)/1_000_000_000.0);
        report.put("providerChecks",pricesOnly?"Official statistical estimates; no new venue price or identity verification"
            :"Existing public snapshots only; no new provider identity or price verification");
        return report;
    }

    /** 같은 자료가 관리하는 활성 장소와 실제 DB 번호를 읽는다. */
    private static Map<String,Entry> entries(Connection connection,String source) throws SQLException {
        var result = new TreeMap<String,Entry>();
        try (var query = connection.prepareStatement("SELECT place_key,record_id,payload FROM place_catalog_entries WHERE source_key=? AND active=TRUE")) {
            query.setString(1,source);
            try (var rows=query.executeQuery()) {
                while(rows.next()) result.put(rows.getString(1),new Entry(rows.getLong(2),rows.getString(3)));
            }
        }
        return result;
    }

    /** 저장된 장소 payload를 자료 지문과 비교할 수 있게 정리한다. */
    private static Map<String,String> payloads(Map<String,Entry> entries) {
        var result = new TreeMap<String,String>();
        entries.forEach((key,entry)->result.put(key,entry.payload()));
        return result;
    }

    /** 적재 대상 외 테이블은 행 순서와 무관한 지문으로 보존 여부를 확인한다. */
    private static Map<String,String> protectedTables(Connection connection) throws Exception {
        var result = new TreeMap<String,String>();
        try (var tables = connection.getMetaData().getTables(connection.getCatalog(),connection.getSchema(),"%",new String[]{"TABLE"})) {
            while(tables.next()) {
                String table=tables.getString("TABLE_NAME");
                if(table.toLowerCase(Locale.ROOT).startsWith("place_catalog_") || table.equalsIgnoreCase("data_records")) continue;
                require(table.matches("[A-Za-z0-9_]+"),"UNEXPECTED_TABLE_NAME");
                var hashes=new ArrayList<String>();
                try(var query=connection.createStatement();var rows=query.executeQuery("SELECT * FROM `"+table+"`")) {
                    while(rows.next())hashes.add(rowHash(rows));
                }
                Collections.sort(hashes);
                result.put(table,hash(String.join("\n",hashes)));
            }
        }
        return result;
    }

    /** 기존 일반 데이터 행은 번호별 지문으로 비교해 확장 외 변경을 찾는다. */
    private static Map<Long,String> dataRecords(Connection connection) throws Exception {
        var result=new TreeMap<Long,String>();
        try(var query=connection.createStatement();var rows=query.executeQuery("SELECT * FROM data_records")) {
            while(rows.next()) result.put(rows.getLong("record_id"),rowHash(rows));
        }
        return result;
    }

    /** 개인정보·PNG 원문을 내보내지 않고 각 행의 내용을 지문화한다. */
    private static String rowHash(ResultSet row) throws Exception {
        var digest=MessageDigest.getInstance("SHA-256");
        for(int i=1;i<=row.getMetaData().getColumnCount();i++) {
            int type=row.getMetaData().getColumnType(i);
            byte[] value;
            if(type==Types.BINARY || type==Types.VARBINARY || type==Types.LONGVARBINARY || type==Types.BLOB) value=row.getBytes(i);
            else {
                String text=row.getString(i);
                value=text==null?null:text.getBytes(StandardCharsets.UTF_8);
            }
            digest.update(java.nio.ByteBuffer.allocate(4).putInt(value==null?-1:value.length).array());
            if(value!=null)digest.update(value);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static String hash(String value) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    }
    private static void require(boolean condition,String code) {
        if(!condition)throw new IllegalStateException(code);
    }
}
