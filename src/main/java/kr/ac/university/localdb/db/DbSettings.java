package kr.ac.university.localdb.db;

import com.datecourse.service.TeamApiKeys;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Team credentials take priority; installations without a shared key keep local settings. */
public record DbSettings(String url, String user, String password) {
    public static DbSettings load() {
        try {
            Map<String, String> remote = TeamApiKeys.database();
            if (!remote.isEmpty()) return fromRemote(remote);
        } catch (IOException failure) {
            // Do not include server responses or credentials in startup error logs.
            throw new IllegalStateException("팀 DB 설정을 불러오지 못했어요. 공통 접속 키와 네트워크를 확인해 주세요.");
        }
        Properties properties = new Properties();
        Path local = Path.of("src/main/resources/db.properties");
        try (InputStream input = Files.exists(local) ? Files.newInputStream(local)
                : DbSettings.class.getClassLoader().getResourceAsStream("db.properties")) {
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

    static DbSettings fromRemote(Map<String, String> remote) {
        Properties values = new Properties();
        for (String field : new String[]{"url", "user", "password"}) {
            String value = remote.get(field);
            if (value == null || value.isBlank()) throw new IllegalStateException("팀 DB 설정이 올바르지 않습니다.");
            values.setProperty("db." + field, value);
        }
        // Keep the server password exactly as returned, including intentional spaces.
        return from(values, Map.of());
    }

    @Override public String toString() { return "DbSettings[credentials=hidden]"; }

    private static String value(Map<String, String> environment, String environmentName,
                                Properties properties, String propertyName) {
        String override = environment.get(environmentName);
        return override != null && !override.isBlank()
                ? override : properties.getProperty(propertyName, "");
    }
}