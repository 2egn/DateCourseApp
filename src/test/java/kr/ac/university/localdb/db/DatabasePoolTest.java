package kr.ac.university.localdb.db;

import kr.ac.university.localdb.PasswordHash;

import com.zaxxer.hikari.HikariDataSource;
import org.h2.jdbc.JdbcConnection;
import org.junit.jupiter.api.*;
import java.sql.*;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class DatabasePoolTest {
    private HikariDataSource pool;
    private static String passwordHash;

    @BeforeAll
    static void hashPassword() throws Exception {
        passwordHash = PasswordHash.create("test-password".toCharArray());
    }

    @BeforeEach
    void createPool() {
        pool = DatabasePool.create(new DbSettings(
                "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
    }

    @AfterEach
    void closePool() {
        pool.close();
    }

    @Test
    void initializationConnectionIsReusedAndPoolShutsDown() throws Exception {
        assertNull(pool.getHikariPoolMXBean(), "Creating the repository must not connect on the UI thread");
        Connection physical;
        try (Connection first = pool.getConnection()) {
            physical = first.unwrap(JdbcConnection.class);
        }
        assertFalse(physical.isClosed(), "Returning a connection must not disconnect from the database");
        try (Connection next = pool.getConnection()) {
            assertSame(physical, next.unwrap(JdbcConnection.class));
        }
        pool.close();
        assertTrue(physical.isClosed());
        assertThrows(SQLException.class, () -> pool.getConnection());
    }

    @Test
    void retiredConnectionIsReplaced() throws Exception {
        Connection previous;
        try (Connection c = pool.getConnection()) {
            previous = c.unwrap(JdbcConnection.class);
        }
        pool.getHikariPoolMXBean().softEvictConnections();
        try (Connection replacement = pool.getConnection()) {
            assertNotSame(previous, replacement.unwrap(JdbcConnection.class));
            assertTrue(replacement.isValid(1));
        }
    }

    @Test
    void loginCanReusePoolAfterAnAuthenticationFailure() throws Exception {
        prepareMember("ACTIVE");
        try (MemberRepository repository = new MemberRepository(pool)) {
            assertThrows(IllegalArgumentException.class,
                    () -> repository.login("member@example.com", "wrong".toCharArray()));
            assertEquals("Test member",
                    repository.login("member@example.com", "test-password".toCharArray()).name());
            assertEquals("Test member",
                    repository.login("member@example.com", "test-password".toCharArray()).name());
        }
    }

    @Test
    void unknownMemberIsRejected() throws Exception {
        prepareMember("ACTIVE");
        try (MemberRepository repository = new MemberRepository(pool)) {
            assertThrows(IllegalArgumentException.class,
                    () -> repository.login("unknown@example.com", "test-password".toCharArray()));
        }
    }

    @Test
    void inactiveMemberIsRejected() throws Exception {
        prepareMember("INACTIVE");
        try (MemberRepository repository = new MemberRepository(pool)) {
            IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                    () -> repository.login("member@example.com", "test-password".toCharArray()));
            assertEquals("탈퇴 또는 비활성화된 계정입니다.", failure.getMessage());
        }
    }

    private void prepareMember(String status) throws SQLException {
        try (Connection c = pool.getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE members (member_id BIGINT AUTO_INCREMENT PRIMARY KEY, "
                    +"email VARCHAR(254), name VARCHAR(100), "
                    + "password_hash VARCHAR(255), status VARCHAR(20))");
            try (PreparedStatement insert = c.prepareStatement("INSERT INTO members (email, name, password_hash,status) VALUES (?, ?, ?, ?)")) {
                insert.setString(1, "member@example.com");
                insert.setString(2, "Test member");
                insert.setString(3, passwordHash);
                insert.setString(4, status);
                insert.executeUpdate();
            }
        }
    }
}