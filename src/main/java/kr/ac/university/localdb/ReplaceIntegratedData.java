package kr.ac.university.localdb;

import kr.ac.university.localdb.db.DbSettings;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;

/** 프로젝트 폴더에서 실행. 기존 데이터를 백업하고 통합 데이터를 교체합니다. */
public class ReplaceIntegratedData {
    public static void main(String[] args) throws Exception {
        DbSettings settings = DbSettings.load();
        var lines = Files.readAllLines(Path.of("database/05_replace_integrated_data.sql"));
        try (Connection c = DriverManager.getConnection(settings.url(), settings.user(), settings.password());
             Statement s = c.createStatement()) {
            String suffix = Long.toString(System.currentTimeMillis());
            s.executeUpdate("CREATE TABLE data_records_backup_" + suffix + " AS SELECT * FROM data_records");
            s.executeUpdate("CREATE TABLE categories_backup_" + suffix + " AS SELECT * FROM categories");
            long membersBefore;
            try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM members")) { r.next(); membersBefore = r.getLong(1); }
            c.setAutoCommit(false);
            try {
                for (String line : lines) {
                    if (line.startsWith("--") || line.isBlank() || line.equals("START TRANSACTION;")
                            || line.equals("COMMIT;") || line.startsWith("SELECT ")) continue;
                    s.execute(line);
                }
                try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM data_records WHERE source_file='integrated_places.csv'")) {
                    r.next(); if (r.getInt(1) != 6139) throw new SQLException("Unexpected imported row count");
                }
                try (ResultSet r = s.executeQuery("SELECT COUNT(*) FROM members")) {
                    r.next(); if (r.getLong(1) != membersBefore) throw new SQLException("Member count changed");
                }
                c.commit();
            } catch (Exception e) { c.rollback(); throw e; }
            try (ResultSet r = s.executeQuery("SELECT category_code,COUNT(*) FROM data_records JOIN categories USING(category_id) WHERE source_file='integrated_places.csv' GROUP BY category_code")) {
                while (r.next()) System.out.println(r.getString(1) + ": " + r.getInt(2));
            }
            System.out.println("Members unchanged: " + membersBefore);
            System.out.println("Backup suffix: " + suffix);
            System.out.println("SUCCESS: integrated dataset replaced");
        }
    }
}
