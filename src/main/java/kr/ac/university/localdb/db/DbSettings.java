package kr.ac.university.localdb.db;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;

/** Loads connection settings from db.properties, with optional environment overrides. */
public record DbSettings(String url, String user, String password) {
    public static DbSettings load() {
        Properties properties = new Properties();
        try (InputStream input = DbSettings.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (input != null) properties.load(input);
        } catch (IOException exception) {
            throw new IllegalStateException("db.properties could not be read.", exception);
        }
        return from(properties, System.getenv());
    }

    static DbSettings from(Properties properties, Map<String, String> environment) {
        String url = value(environment, "DB_URL", properties, "db.url").trim();
        String user = value(environment, "DB_USER", properties, "db.user").trim();
        // Password whitespace may be intentional and must be passed through unchanged.
        String password = value(environment, "DB_PASSWORD", properties, "db.password");

        if (url.isBlank() || user.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Database settings are missing. Set db.url, db.user, and db.password in db.properties, "
                            + "or set DB_URL, DB_USER, and DB_PASSWORD environment variables.");
        }
        return new DbSettings(url, user, password);
    }

    private static String value(Map<String, String> environment, String environmentName,
                                Properties properties, String propertyName) {
        String override = environment.get(environmentName);
        return override != null && !override.isBlank()
                ? override : properties.getProperty(propertyName, "");
    }
}