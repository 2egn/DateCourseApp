package kr.ac.university.localdb.db;

import kr.ac.university.localdb.PasswordHash;

import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.*;

/** 가입과 관리자 기능에서 공통으로 사용하는 DB 코드입니다. */
public class MemberRepository implements AutoCloseable {
    public static final String ADMIN_USERNAME = "a1234";
    private static final String INITIAL_ADMIN_HASH = "pbkdf2-sha256$600000$VWNKiLVOeQPuuXhHV0lPKQ==$jo1T+y7gfBK3eGRNLyp9EBmnbbLaEe6czQss6sZVkpI=";
    public record Login(long memberId, String name, AdminSession adminSession) {}
    public record Member(long id, String email, String name, String phone, String status, String created) {}
    private final HikariDataSource pool;

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

    public void initialize() throws SQLException {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            // 기존 bcrypt 60자 값도 그대로 보존하면서 새 해시가 들어갈 공간을 확보합니다.
            try (ResultSet r = s.executeQuery("SELECT CHARACTER_MAXIMUM_LENGTH FROM information_schema.columns "
                    + "WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='members' AND COLUMN_NAME='password_hash'")) {
                if (!r.next()) throw new SQLException("members 테이블이 없습니다. 02_create_tables.sql을 먼저 실행하세요.");
                if (r.getInt(1) < 255) {
                    try (Statement alter = c.createStatement()) {
                        alter.executeUpdate("ALTER TABLE members MODIFY password_hash VARCHAR(255) NOT NULL");
                    }
                }
            }
            s.executeUpdate("CREATE TABLE IF NOT EXISTS app_admins (username VARCHAR(50) PRIMARY KEY, "
                    + "password_hash VARCHAR(255) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            try (PreparedStatement add = c.prepareStatement("INSERT INTO app_admins(username,password_hash) "
                    + "VALUES (?,?) ON DUPLICATE KEY UPDATE username=VALUES(username)")) {
                add.setString(1, ADMIN_USERNAME); add.setString(2, INITIAL_ADMIN_HASH); add.executeUpdate();
            }
        }
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
        long started = System.nanoTime();

        try (Connection c = connect()) {
            long connected = System.nanoTime();

            try (PreparedStatement s = c.prepareStatement(
                    "SELECT member_id,name,password_hash,status FROM members WHERE email=?")) {
                s.setString(1, username.toLowerCase(Locale.ROOT));

                try (ResultSet r = s.executeQuery()) {
                    boolean found = r.next();
                    long queried = System.nanoTime();

                    boolean valid = found
                            && PasswordHash.verify(password, r.getString("password_hash"));
                    long verified = System.nanoTime();

                    System.out.printf(
                            "DB 연결: %.1f ms%n회원 조회: %.1f ms%n비밀번호 검증: %.1f ms%n",
                            (connected - started) / 1_000_000.0,
                            (queried - connected) / 1_000_000.0,
                            (verified - queried) / 1_000_000.0
                    );

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
        public void deleteInactive(long id) throws SQLException {
            try (Connection c = connect(); PreparedStatement s = c.prepareStatement(
                    "DELETE FROM members WHERE member_id=? AND status='INACTIVE'")) {
                s.setLong(1,id);
                if(s.executeUpdate()!=1) throw new IllegalArgumentException("비활성 회원만 삭제할 수 있습니다. 목록을 새로고침하세요.");
            }
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

        public void update(long id, String name, String phone, String status) throws SQLException {
            if (name.trim().isEmpty() || name.trim().length()>100 || phone.trim().length()>30)
                throw new IllegalArgumentException("이름(1~100자)과 전화번호(최대 30자)를 확인하세요.");
            if (!Set.of("ACTIVE","INACTIVE").contains(status)) throw new IllegalArgumentException("잘못된 회원 상태입니다.");
            try (Connection c = connect(); PreparedStatement s = c.prepareStatement(
                    "UPDATE members SET name=?,phone=?,status=? WHERE member_id=?")) {
                s.setString(1,name.trim()); s.setString(2,phone.trim()); s.setString(3,status); s.setLong(4,id);
                if (s.executeUpdate()!=1) throw new SQLException("회원이 없거나 이미 변경되었습니다.");
            }
        }
    }
}
