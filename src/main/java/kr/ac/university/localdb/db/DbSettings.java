package kr.ac.university.localdb.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/** Loads local credentials without placing them in source code. */
public record DbSettings(String url, String user, String password) {
    public static DbSettings load() {
        Properties properties = new Properties();
        try (InputStream input = DbSettings.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) {
                properties.load(input);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("db.properties could not be read.", exception);
        }

        String url = value("DB_URL", properties, "db.url",
                "jdbc:mysql://localhost:3306/university_project?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Seoul");
        String user = value("DB_USER", properties, "db.user", "");
        String password = value("DB_PASSWORD", properties, "db.password", "");

        if (user.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "MySQL credentials are missing. Copy db.properties.example to db.properties and fill it in, "
                            + "or set DB_URL, DB_USER, and DB_PASSWORD environment variables.");
        }
        return new DbSettings(url, user, password);
    }

    private static String value(String environmentName, Properties properties, String propertyName, String fallback) {
        String environmentValue = System.getenv(environmentName);
        return environmentValue != null && !environmentValue.isBlank()
                ? environmentValue
                : properties.getProperty(propertyName, fallback).trim();
    }
}
