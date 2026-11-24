package com.datecourse.maintenance;

import com.datecourse.db.DatabasePool;
import com.datecourse.db.DatabaseSchema;
import com.datecourse.db.DbSettings;
import com.datecourse.db.MemberRepository;
import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Set;

/** 관리자가 명시적으로 실행하는 추가 구조 준비. 기본 DB·회원·코스 스키마는 먼저 준비한다. */
public final class DatabaseSetup {
    private static final String INITIAL_ADMIN_HASH = "pbkdf2-sha256$600000$VWNKiLVOeQPuuXhHV0lPKQ==$jo1T+y7gfBK3eGRNLyp9EBmnbbLaEe6czQss6sZVkpI=";
    private DatabaseSetup() {}

    /** 접속 정보는 기존 설정에서 읽고 SQL 오류·자격증명은 출력하지 않는다. */
    public static void main(String[] args) {
        String scope = args.length == 0 ? "--all" : args[0];
        if (args.length > 1 || !Set.of("--all", "--membership", "--reports", "--catalog", "--previews", "--transport").contains(scope)) {
            System.err.println("사용법: setupDatabase [--args=--membership|--reports|--catalog|--previews|--transport]");
            System.exit(1); return;
        }
        try (var pool = DatabasePool.create(DbSettings.load())) {
            switch (scope) {
                case "--all" -> prepare(pool);
                case "--membership" -> prepareMembership(pool);
                case "--reports" -> prepareReports(pool);
                case "--catalog" -> prepareCatalog(pool);
                case "--previews" -> preparePreviews(pool);
                case "--transport" -> prepareTransport(pool);
            }
            System.out.println("DB 추가 구조 준비 완료. 기존 계정·장소·저장 코스와 관리자 비밀번호는 보존했습니다.");
        } catch (Exception failure) {
            System.err.println("DB 준비를 완료하지 못했어요. 기본 스키마·접속 설정·구조 변경 권한을 확인해 주세요. 일부 DDL은 반영됐을 수 있으므로 상태를 확인한 뒤 재실행하세요.");
            System.exit(1);
        }
    }

    /** 기본 스키마는 변경하지 않고 현재 앱에 필요한 추가 구조만 준비한다. */
    public static void prepare(DataSource source) throws SQLException, IOException {
        DatabaseSchema.requireTables(source, "members", "categories", "data_records", "saved_courses", "saved_course_places");
        // 일회성 가격 확장은 자동 재실행하지 않고, 미준비 상태는 쓰기 전에 차단한다.
        try (var connection = source.getConnection(); var statement = connection.createStatement();
             var rows = statement.executeQuery("SELECT price_estimate_snapshot,place_details_snapshot,unit_price_snapshot FROM saved_course_places WHERE 1=0")) {
            if (rows.getMetaData().isNullable(3) != java.sql.ResultSetMetaData.columnNullable)
                throw new SQLException("08_saved_course_price_estimates.sql 적용 상태를 확인해 주세요.");
        }
        prepareMembership(source);
        prepareReports(source);
        prepareCatalog(source);
        preparePreviews(source);
        prepareTransport(source);
    }

    /** 해시 열 확장과 최초 관리자 등록만 수행하며 기존 관리자 해시는 덮어쓰지 않는다. */
    public static void prepareMembership(DataSource source) throws SQLException {
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            try (var rows = statement.executeQuery("SELECT password_hash FROM members WHERE 1=0")) {
                if (rows.getMetaData().getPrecision(1) < 255) {
                    try (var alter = connection.createStatement()) {
                        alter.executeUpdate("ALTER TABLE members MODIFY password_hash VARCHAR(255) NOT NULL");
                    }
                }
            }
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS app_admins (username VARCHAR(50) PRIMARY KEY, password_hash VARCHAR(255) NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP)");
            try (var add = connection.prepareStatement("INSERT INTO app_admins(username,password_hash) VALUES (?,?) ON DUPLICATE KEY UPDATE username=VALUES(username)")) {
                add.setString(1, MemberRepository.ADMIN_USERNAME); add.setString(2, INITIAL_ADMIN_HASH); add.executeUpdate();
            }
        }
    }
    /** 제보·정정 추가 테이블만 준비한다. */
    public static void prepareReports(DataSource source) throws SQLException, IOException { execute(source, "09_place_information_reports.sql"); }
    /** 기존 이미지 행을 유지하며 미리보기 테이블만 준비한다. */
    public static void preparePreviews(DataSource source) throws SQLException, IOException { execute(source, "11_saved_course_previews.sql"); }
    /** 기존 이동수단 행을 유지하며 저장 테이블만 준비한다. */
    public static void prepareTransport(DataSource source) throws SQLException, IOException { execute(source, "12_saved_course_transport.sql"); }
    /** 빈 카탈로그 구조와 잠금 행만 준비하며 장소 자료는 적재하지 않는다. */
    public static void prepareCatalog(DataSource source) throws SQLException, IOException {
        execute(source, "10_place_catalog.sql");
        try (var connection = source.getConnection(); var statement = connection.createStatement()) {
            statement.executeUpdate("INSERT IGNORE INTO place_catalog_lock(lock_id) VALUES(1)");
        }
    }
    /** 리소스 SQL을 한 곳에서 실행하며 MySQL DDL의 전체 롤백을 약속하지 않는다. */
    private static void execute(DataSource source, String name) throws SQLException, IOException {
        try (var input = DatabaseSetup.class.getResourceAsStream("/db/" + name)) {
            if (input == null) throw new IOException("DB 준비 SQL 리소스가 없습니다.");
            String sql = new String(input.readAllBytes(), StandardCharsets.UTF_8).replaceAll("(?m)^--.*$", "");
            try (var connection = source.getConnection(); var statement = connection.createStatement()) {
                for (String command : sql.split(";")) if (!command.isBlank()) statement.executeUpdate(command);
            }
        }
    }
}
