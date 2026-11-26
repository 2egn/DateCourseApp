package com.datecourse.db;

import com.datecourse.auth.PasswordHash;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;

/** 가입과 관리자 기능에서 공통으로 사용하는 DB 코드입니다. */
public class MemberRepository implements AutoCloseable {
    public static final String ADMIN_USERNAME = "a1234";
    public record Login(long memberId, String name, AdminSession adminSession) {}
    public record Member(long id, String email, String name, String phone, String status, String created) {}
    public record Profile(String email, String name, String phone) {}

    /** 로그인한 회원의 프로필 조회용 함수. 비밀번호 해시는 조회 대상에서 제외 */
    public Profile profile(com.datecourse.model.UserSession session) throws SQLException {
        if (session == null || session.memberId() <= 0) throw new IllegalArgumentException("로그인이 필요합니다.");
        try (Connection connection = connect(); PreparedStatement query = connection.prepareStatement(
                "SELECT email,name,phone FROM members WHERE member_id=? AND status='ACTIVE'")) {
            query.setLong(1, session.memberId());
            try (ResultSet row = query.executeQuery()) {
                if (!row.next()) throw new SQLException("회원 정보를 찾지 못했습니다.");
                return new Profile(row.getString("email"), row.getString("name"), row.getString("phone"));
            }
        }
    }
    private final HikariDataSource pool;

    /** 로그인 앱의 연결 풀을 장소 조회에서도 공유, 앱 종료 시 같이 정리 */
    public PlaceRepository places() {
        return new PlaceRepository(pool);
    }

    /** 코스 저장소도 회원 저장소의 연결 풀을 공유, 회원 저장소 종료 시 풀 정리 */
    public SavedCourseRepository savedCourses() {
        return new SavedCourseRepository(pool);
    }
    public PlaceCatalogRepository placeCatalog() { return new PlaceCatalogRepository(pool); }
    public InformationReportRepository informationReports() { return new InformationReportRepository(pool); }

    public MemberRepository() {
        this(DatabasePool.create(DbSettings.load()));
    }

    MemberRepository(HikariDataSource pool) {
        this.pool = pool;
    }

    private Connection connect() throws SQLException {
        // Closing this connection returns it to the pool for the next operation.
        return pool.getConnection();
    }

    @Override
    public void close() {
        pool.close();
    }

    /** 시작 시 구조만 확인한다. 생성·변경은 maintenance.DatabaseSetup에서 명시적으로 수행한다. */
    public void initialize() throws SQLException {
        DatabaseSchema.requireMembership(pool);
    }

    public long register(String email, char[] password, String name, String phone) throws Exception {
        email = email.trim().toLowerCase(Locale.ROOT);
        name = name.trim(); phone = phone.trim();
        validate(email, name, phone);
        if (password.length < 8 || password.length > 128) throw new IllegalArgumentException("비밀번호는 8~128자로 입력하세요.");
        String hash = PasswordHash.create(password);
        try (Connection c = connect(); PreparedStatement s = c.prepareStatement(
                "INSERT INTO members(email,password_hash,name,phone) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            s.setString(1, email); s.setString(2, hash); s.setString(3, name); s.setString(4, phone);
            s.executeUpdate();
            try (ResultSet r = s.getGeneratedKeys()) { r.next(); return r.getLong(1); }
        }
    }

    public static void validate(String email, String name, String phone) {
        if (email.length() > 254 || !email.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"))
            throw new IllegalArgumentException("올바른 이메일을 입력하세요.");
        if (name.isBlank() || name.length() > 100) throw new IllegalArgumentException("이름은 1~100자로 입력하세요.");
        if (phone.length() > 30) throw new IllegalArgumentException("전화번호는 30자 이내로 입력하세요.");
    }

    public Login login(String username, char[] password) throws Exception {
        username = username.trim();


        try (Connection c = connect()) {


            // The admin window requires a capability issued only after checking the stored admin hash.
            if (ADMIN_USERNAME.equals(username)) {
                try (PreparedStatement s=c.prepareStatement("SELECT password_hash FROM app_admins WHERE username=?")) {
                    s.setString(1,username);
                    try (ResultSet row=s.executeQuery()) {
                        if(row.next()&&PasswordHash.verify(password,row.getString("password_hash")))
                            return new Login(0,"관리자",new AdminSession());
                    }
                }
                throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
            }

            try (PreparedStatement s = c.prepareStatement(
                    "SELECT member_id,name,password_hash,status FROM members WHERE email=?")) {
                s.setString(1, username.toLowerCase(Locale.ROOT));

                try (ResultSet r = s.executeQuery()) {
                    boolean found = r.next();


                    boolean valid = found
                            && PasswordHash.verify(password, r.getString("password_hash"));


                    if (!valid)
                        throw new IllegalArgumentException(
                                "아이디 또는 비밀번호가 일치하지 않습니다.");

                    if (!"ACTIVE".equals(r.getString("status")))
                        throw new IllegalArgumentException(
                                "탈퇴 또는 비활성화된 계정입니다.");

                    return new Login(r.getLong("member_id"),r.getString("name"), null);
                }
            }
        }
    }

    /** 관리자 인증에 성공했을 때만 관리 메서드에 접근할 수 있습니다. */
    public final class AdminSession {
        private AdminSession() {}
        public java.util.List<com.datecourse.model.InformationReport> informationReports() throws SQLException {
            return MemberRepository.this.informationReports().reports();
        }
        public InformationReportRepository.ReportPage informationReports(InformationReportRepository.ReportFilter filter,Long beforeId) throws SQLException {
            return MemberRepository.this.informationReports().reports(filter,beforeId,50);
        }
        public InformationReportRepository.ReviewContext informationReportContext(long id) throws SQLException {
            return MemberRepository.this.informationReports().reviewContext(id);
        }
        public void reviewInformation(InformationReportRepository.ReviewContext expected,boolean approve,String corrected,String note) throws SQLException {
            MemberRepository.this.informationReports().review(expected,approve,corrected,note);
        }
        public void reviewInformation(long id, boolean approve, String corrected, String note) throws SQLException {
            MemberRepository.this.informationReports().review(id,approve,corrected,note);
        }
        public void deleteInactive(long id) throws SQLException { deleteInactive(readMember(id)); }

        public String deletionRestriction(Member expected) throws SQLException {
            try(Connection connection=connect()) {
                Member current=readMember(connection,expected.id(),false);requireUnchanged(current,expected);
                return deletionRestriction(connection,current);
            }
        }

        public void deleteInactive(Member expected) throws SQLException {
            try(Connection connection=connect()) {
                connection.setAutoCommit(false);
                try {
                    Member current=readMember(connection,expected.id(),true);requireUnchanged(current,expected);
                    String reason=deletionRestriction(connection,current);
                    if(!reason.isEmpty())throw new IllegalArgumentException(reason);
                    try(PreparedStatement query=connection.prepareStatement("DELETE FROM members WHERE member_id=? AND status='INACTIVE'")) {
                        query.setLong(1,current.id());
                        if(query.executeUpdate()!=1)throw new IllegalArgumentException("회원 상태가 바뀌었어요. 목록을 새로고침해 주세요.");
                    }
                    connection.commit();
                } catch(SQLException|RuntimeException failure) {
                    connection.rollback();
                    if(failure instanceof SQLException sql && "23000".equals(sql.getSQLState()))
                        throw new IllegalArgumentException("연결된 데이터가 있는 회원은 영구 삭제할 수 없어요. 탈퇴 상태로 유지해 주세요.");
                    throw failure;
                }
            }
        }

        private String deletionRestriction(Connection connection,Member member) throws SQLException {
            if(!"INACTIVE".equals(member.status()))return "탈퇴 상태의 회원만 영구 삭제할 수 있어요.";
            try(PreparedStatement query=connection.prepareStatement("SELECT course_id FROM saved_courses WHERE member_id=? LIMIT 1")) {
                query.setLong(1,member.id());
                try(ResultSet row=query.executeQuery()) {
                    if(row.next())return "저장한 코스가 있는 회원은 영구 삭제할 수 없어요. 탈퇴 상태로 유지해 주세요.";
                }
            } catch(SQLException failure) {
                if(!"42S02".equals(failure.getSQLState()) && failure.getErrorCode()!=42102)throw failure;
            }
            return "";
        }

        public List<Member> list(String keyword) throws SQLException {
            List<Member> members = new ArrayList<>();
            try (Connection c = connect(); PreparedStatement s = c.prepareStatement(
                    "SELECT member_id,email,name,phone,status,created_at FROM members "
                            + "WHERE LOCATE(?,email)>0 OR LOCATE(?,name)>0 ORDER BY member_id DESC")) {
                s.setString(1, keyword.trim()); s.setString(    2, keyword.trim());
                try (ResultSet r = s.executeQuery()) {
                    while (r.next()) members.add(new Member(r.getLong(1),r.getString(2),r.getString(3),
                            r.getString(4),r.getString(5),r.getString(6)));
                }
            }
            return members;
        }

        public void update(long id,String name,String phone,String status) throws SQLException {
            update(readMember(id),name,phone,status);
        }

        public void update(Member expected,String name,String phone,String status) throws SQLException {
            if(name==null || name.trim().isEmpty() || name.trim().length()>100 || phone==null || phone.trim().length()>30)
                throw new IllegalArgumentException("이름(1~100자)과 전화번호(최대 30자)를 확인해 주세요.");
            change(expected,name.trim(),phone.trim(),status,false);
        }

        public void changeStatus(Member expected,String status) throws SQLException {
            change(expected,null,null,status,true);
        }

        private void change(Member expected,String name,String phone,String status,boolean statusOnly) throws SQLException {
            if(!Set.of("ACTIVE","INACTIVE").contains(status))throw new IllegalArgumentException("회원 상태를 확인해 주세요.");
            try(Connection connection=connect()) {
                connection.setAutoCommit(false);
                try {
                    Member current=readMember(connection,expected.id(),true);requireUnchanged(current,expected);
                    String sql=statusOnly?"UPDATE members SET status=? WHERE member_id=?":"UPDATE members SET name=?,phone=?,status=? WHERE member_id=?";
                    try(PreparedStatement query=connection.prepareStatement(sql)) {
                        int parameter=1;
                        if(!statusOnly){query.setString(parameter++,name);query.setString(parameter++,phone);}
                        query.setString(parameter++,status);query.setLong(parameter,current.id());query.executeUpdate();
                    }
                    connection.commit();
                } catch(SQLException|RuntimeException failure){connection.rollback();throw failure;}
            }
        }

        private Member readMember(long id) throws SQLException {
            try(Connection connection=connect()){return readMember(connection,id,false);}
        }
        private Member readMember(Connection connection,long id,boolean lock) throws SQLException {
            try(PreparedStatement query=connection.prepareStatement("SELECT member_id,email,name,phone,status,created_at FROM members WHERE member_id=?"+(lock?" FOR UPDATE":""))) {
                query.setLong(1,id);
                try(ResultSet row=query.executeQuery()) {
                    if(!row.next())throw new IllegalArgumentException("회원을 찾지 못했어요. 목록을 새로고침해 주세요.");
                    return new Member(row.getLong(1),row.getString(2),row.getString(3),row.getString(4),row.getString(5),row.getString(6));
                }
            }
        }
        private void requireUnchanged(Member current,Member expected) {
            if(!current.equals(expected))throw new IllegalArgumentException("다른 작업에서 회원 정보가 변경됐어요. 목록을 새로고침한 뒤 다시 시도해 주세요.");
        }
    }
}
