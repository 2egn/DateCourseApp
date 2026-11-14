package kr.ac.university.localdb.db;

import com.datecourse.model.*;
import kr.ac.university.localdb.UserSession;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

/** Original place data and saved snapshots are never rewritten. Approved fields form an overlay. */
public final class InformationReportRepository {
    private final DataSource pool;
    private volatile boolean ready;
    public InformationReportRepository(DataSource pool) { this.pool = pool; }

    public synchronized void initialize() throws SQLException {
        if (ready) return;
        try (Connection c = pool.getConnection(); Statement s = c.createStatement()) {
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS place_information_reports (
                  report_id BIGINT AUTO_INCREMENT PRIMARY KEY, member_id BIGINT NOT NULL,
                  place_id VARCHAR(200) NOT NULL, place_name VARCHAR(200) NOT NULL,
                  field_name VARCHAR(30) NOT NULL, original_value VARCHAR(1000) NOT NULL,
                  proposed_value VARCHAR(1000) NOT NULL, reason VARCHAR(1000) NOT NULL,
                  source_url VARCHAR(1000) NOT NULL, status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                  review_note VARCHAR(1000) NOT NULL DEFAULT '', reviewed_value VARCHAR(1000) NOT NULL DEFAULT '',
                  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  reviewed_at TIMESTAMP NULL)
                """);
            s.executeUpdate("""
                CREATE TABLE IF NOT EXISTS place_information_overrides (
                  place_id VARCHAR(200) NOT NULL, field_name VARCHAR(30) NOT NULL,
                  corrected_value VARCHAR(1000) NOT NULL, source_url VARCHAR(1000) NOT NULL,
                  reviewed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (place_id, field_name))
                """);
        }
        ready = true;
    }

    public long submit(UserSession session, Place place, InformationReport.Field field,
                       String proposed, String reason, String sourceUrl) throws SQLException {
        if (session == null || session.memberId() <= 0) throw new IllegalArgumentException("로그인 후 제보할 수 있어요.");
        String value = value(field, proposed);
        reason = text(reason, 1000, false); sourceUrl = url(sourceUrl);
        if (place == null || place.placeId() == null || place.placeId().isBlank() || place.placeId().length() > 200)
            throw new IllegalArgumentException("장소 식별자가 필요해요.");
        initialize();
        try (Connection c = pool.getConnection()) {
            try (PreparedStatement active = c.prepareStatement("SELECT member_id FROM members WHERE member_id=? AND status='ACTIVE'")) {
                active.setLong(1, session.memberId());
                try (ResultSet row = active.executeQuery()) { if (!row.next()) throw new IllegalArgumentException("활성 회원만 제보할 수 있어요."); }
            }
            try (PreparedStatement s = c.prepareStatement("""
                INSERT INTO place_information_reports(member_id,place_id,place_name,field_name,original_value,
                  proposed_value,reason,source_url) VALUES (?,?,?,?,?,?,?,?)
                """, Statement.RETURN_GENERATED_KEYS)) {
                s.setLong(1,session.memberId()); s.setString(2,key(place)); s.setString(3,text(place.name(),200,false));
                s.setString(4,field.name()); s.setString(5,original(place,field)); s.setString(6,value);
                s.setString(7,reason); s.setString(8,sourceUrl); s.executeUpdate();
                try (ResultSet keys=s.getGeneratedKeys()) { keys.next(); return keys.getLong(1); }
            }
        }
    }

    // These management operations are exposed to the UI only through an authenticated AdminSession.
    List<InformationReport> reports() throws SQLException {
        initialize();
        try (Connection c=pool.getConnection(); PreparedStatement s=c.prepareStatement(
                "SELECT * FROM place_information_reports ORDER BY report_id DESC LIMIT 300"); ResultSet r=s.executeQuery()) {
            List<InformationReport> rows=new ArrayList<>();
            while(r.next()) rows.add(new InformationReport(r.getLong("report_id"),r.getString("place_id"),r.getString("place_name"),
                r.getString("field_name"),r.getString("original_value"),r.getString("proposed_value"),r.getString("reason"),
                r.getString("source_url"),r.getString("status"),r.getString("review_note"),r.getString("created_at"),r.getString("reviewed_value")));
            return List.copyOf(rows);
        }
    }

    void review(long id, boolean approve, String corrected, String note) throws SQLException {
        note=text(note,1000,false); initialize();
        try (Connection c=pool.getConnection()) {
            c.setAutoCommit(false);
            try {
                String placeId, field, source;
                try (PreparedStatement s=c.prepareStatement("SELECT * FROM place_information_reports WHERE report_id=? FOR UPDATE")) {
                    s.setLong(1,id);
                    try (ResultSet r=s.executeQuery()) {
                        if(!r.next() || !"PENDING".equals(r.getString("status"))) throw new IllegalArgumentException("이미 처리됐거나 없는 제보예요.");
                        placeId=r.getString("place_id");field=r.getString("field_name");source=r.getString("source_url");
                    }
                }
                if(approve) {
                    corrected=value(InformationReport.Field.valueOf(field),corrected);
                    try (PreparedStatement d=c.prepareStatement("DELETE FROM place_information_overrides WHERE place_id=? AND field_name=?")) {
                        d.setString(1,placeId);d.setString(2,field);d.executeUpdate();
                    }
                    try (PreparedStatement s=c.prepareStatement("INSERT INTO place_information_overrides(place_id,field_name,corrected_value,source_url) VALUES (?,?,?,?)")) {
                        s.setString(1,placeId);s.setString(2,field);s.setString(3,corrected);s.setString(4,source);s.executeUpdate();
                    }
                }
                try (PreparedStatement s=c.prepareStatement("UPDATE place_information_reports SET status=?,review_note=?,reviewed_value=?,reviewed_at=CURRENT_TIMESTAMP WHERE report_id=?")) {
                    s.setString(1,approve?"APPROVED":"REJECTED");s.setString(2,note);s.setString(3,approve?corrected:"");s.setLong(4,id);s.executeUpdate();
                }
                c.commit();
            } catch (SQLException | RuntimeException e) { c.rollback(); throw e; }
        }
    }

    public List<Place> apply(List<Place> places) throws SQLException {
        initialize();
        record Override(String value,String source,String date) {}
        Map<String,Map<String,Override>> values=new HashMap<>();
        try (Connection c=pool.getConnection(); Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT * FROM place_information_overrides")) {
            while(r.next()) values.computeIfAbsent(r.getString("place_id"),k->new HashMap<>()).put(r.getString("field_name"),
                    new Override(r.getString("corrected_value"),r.getString("source_url"),r.getString("reviewed_at")));
        }
        return places.stream().map(p->{
            var fields=values.get(key(p)); if(fields==null) return p;
            var d=p.details(); var price=p.priceEstimate();
            if(fields.containsKey("PRICE")) {
                var v=fields.get("PRICE");int amount=Integer.parseInt(v.value());
                price=new PriceEstimate(amount,amount,PriceEstimate.Unit.PER_PERSON,"관리자 검토 후 정정 · 1인 기본 이용 기준",
                        PriceEstimate.Evidence.ADMIN_REVIEWED,v.source(),v.date());
            }
            return new Place(p.recordId(),p.placeId(),fields.containsKey("NAME")?fields.get("NAME").value():p.name(),p.category(),p.lat(),p.lng(),price,p.rating(),
                new PlaceDetails(fields.containsKey("ADDRESS")?fields.get("ADDRESS").value():d.address(),
                    fields.containsKey("SUBCATEGORY")?fields.get("SUBCATEGORY").value():d.subcategory(),d.phone(),d.website(),
                    d.sourceUrl(),d.sourceDate(),d.identityCheckedAt(),d.verificationUrl(),d.externalIds(),
                    price.minimum()!=null&&price.minimum()>0?d.tags().stream().filter(t->t.tag()!=PlaceTag.FREE_ADMISSION).toList():d.tags()));
        }).toList();
    }
    private static String key(Place p) {
        String publicId=p.details().externalIds().get("public");
        return publicId==null||publicId.isBlank()?p.placeId():publicId;
    }
    public static String original(Place p, InformationReport.Field f) {
        return switch(f) {case PRICE->p.priceText();case NAME->p.name();case ADDRESS->p.details().address();case SUBCATEGORY->p.details().subcategory();};
    }
    private static String value(InformationReport.Field f,String v) {
        v=text(v,f==InformationReport.Field.NAME?200:1000,false);
        if(f==InformationReport.Field.PRICE) {
            try { int n=Integer.parseInt(v);if(n<0||n>10000000) throw new NumberFormatException();return Integer.toString(n); }
            catch(NumberFormatException e){throw new IllegalArgumentException("가격은 1인 기준 0~10,000,000원의 정수로 입력해 주세요.");}
        }
        return v;
    }
    private static String text(String v,int max,boolean empty) {
        v=v==null?"":v.trim(); if((!empty&&v.isBlank())||v.length()>max)throw new IllegalArgumentException("필수 항목과 글자 수를 확인해 주세요. 최대 "+max+"자예요.");return v;
    }
    private static String url(String s) {
        s=text(s,1000,true);if(s.isEmpty()) return s;
        try {var u=java.net.URI.create(s);if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null)throw new IllegalArgumentException();}
        catch(IllegalArgumentException e){throw new IllegalArgumentException("근거 링크는 https 주소로 입력해 주세요.");}return s;
    }
}
