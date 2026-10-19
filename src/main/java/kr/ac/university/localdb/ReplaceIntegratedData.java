package kr.ac.university.localdb;

import kr.ac.university.localdb.db.DbSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Locale;
import java.util.List;
import java.util.function.IntConsumer;

/** 프로젝트 폴더에서 실행. 기존 데이터를 백업하고 통합 데이터를 교체합니다. */
public class ReplaceIntegratedData {
    private static final int BATCH_SIZE = 250;
    private static final String PLACE_INSERT =
            "INSERT INTO data_records(category_id,source_file,source_row_number,data_payload) VALUES ";
    public static void main(String[] args) throws Exception {
        DbSettings settings = DbSettings.load();
        var lines = Files.readAllLines(Path.of("database/05_replace_integrated_data.sql"));
        try (Connection c = DriverManager.getConnection(settings.url(), settings.user(), settings.password());
             Statement s = c.createStatement()) {
            if (c.getCatalog() == null || c.getCatalog().isBlank()) {
                throw new SQLException("Select the target database in db.url before importing data.");
            }
            String suffix = Long.toString(System.currentTimeMillis());
            // Copy the schema first so backups retain primary keys required by managed MySQL.
            s.executeUpdate("CREATE TABLE data_records_backup_" + suffix + " LIKE data_records");
            s.executeUpdate("INSERT INTO data_records_backup_" + suffix + " SELECT * FROM data_records");
            s.executeUpdate("CREATE TABLE categories_backup_" + suffix + " LIKE categories");
            s.executeUpdate("INSERT INTO categories_backup_" + suffix + " SELECT * FROM categories");
            long membersBefore;
            try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM members")) { r.next(); membersBefore = r.getLong(1); }
            c.setAutoCommit(false);
            try {
                long total = lines.stream().filter(line -> line.strip().startsWith(PLACE_INSERT)).count();
                System.out.printf("장소 적재 시작: %,d건 (묶음당 최대 %d건)%n", total, BATCH_SIZE);
                executeScript(s, lines, BATCH_SIZE,
                        done -> System.out.printf("적재 진행 (커밋 전): %,d / %,d건%n", done, total));
                try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM data_records WHERE source_file='integrated_places.csv'")) {
                    r.next(); if (r.getInt(1) != 6139) throw new SQLException("Unexpected imported row count");
                }
                try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM members")) {
                    r.next(); if (r.getLong(1) != membersBefore) throw new SQLException("Member count changed");
                }
                c.commit();
                System.out.println("장소 적재 커밋 완료");
            } catch (Exception e) { c.rollback(); throw e; }
            try (ResultSet r = s.executeQuery("SELECT category_code,COUNT(*) FROM data_records JOIN categories USING(category_id) WHERE source_file='integrated_places.csv' GROUP BY category_code")) {
                while (r.next()) System.out.println(r.getString(1) + ": " + r.getInt(2));
            }
            System.out.println("Members unchanged: " + membersBefore);
            System.out.println("Backup suffix: " + suffix);
            System.out.println("SUCCESS: integrated dataset replaced");
        }
    }

    /**
     * Send multi-row INSERTs, flushing before other statements to preserve script order.
     * The caller owns the transaction; batches are never committed independently.
     */
    static void executeScript(Statement statement, List<String> lines, int batchSize,
                              IntConsumer progress) throws SQLException {
        if (batchSize < 1) throw new IllegalArgumentException("Batch size must be positive");
        StringBuilder batch = new StringBuilder(PLACE_INSERT);
        int pending = 0;
        int completed = 0;
        for (String line : lines) {
            if (shouldSkipStatement(line)) continue;
            String sql = line.strip();
            if (sql.startsWith(PLACE_INSERT)) {
                if (!sql.endsWith(";")) throw new SQLException("Place INSERT must end with a semicolon");
                if (pending > 0) batch.append(',');
                batch.append(sql, PLACE_INSERT.length(), sql.length() - 1);
                pending++;
                if (pending == batchSize) {
                    statement.executeUpdate(batch.toString());
                    completed += pending;
                    progress.accept(completed);
                    pending = 0;
                    batch.setLength(PLACE_INSERT.length());
                }
            } else {
                if (pending > 0) {
                    statement.executeUpdate(batch.toString());
                    completed += pending;
                    progress.accept(completed);
                    pending = 0;
                    batch.setLength(PLACE_INSERT.length());
                }
                statement.execute(sql);
            }
        }
        if (pending > 0) {
            statement.executeUpdate(batch.toString());
            progress.accept(completed + pending);
        }
    }
    /** Keep db.url authoritative even when importing an older script with a USE statement. */
    static boolean shouldSkipStatement(String line) {
        String statement = line.strip().toUpperCase(Locale.ROOT);
        return statement.isEmpty() || statement.startsWith("--")
                || (statement.length() > 3 && statement.startsWith("USE") && Character.isWhitespace(statement.charAt(3)))
                || statement.equals("START TRANSACTION;") || statement.equals("COMMIT;")
                || statement.startsWith("SELECT ");
    }
}
