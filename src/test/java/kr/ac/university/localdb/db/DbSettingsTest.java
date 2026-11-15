package kr.ac.university.localdb.db;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.Properties;
import static org.junit.jupiter.api.Assertions.*;

class DbSettingsTest {
    private Properties configured() {
        Properties properties = new Properties();
        properties.setProperty("db.url", "jdbc:mysql://example.invalid:1234/custom_database?sslMode=VERIFY_CA");
        properties.setProperty("db.user", "example-user");
        properties.setProperty("db.password", " example-password ");
        return properties;
    }

    @Test
    void readsConfiguredUrlUserAndExactPassword() {
        Properties properties = configured();
        DbSettings settings = DbSettings.from(properties, Map.of());
        assertEquals(properties.getProperty("db.url"), settings.url());
        assertEquals(properties.getProperty("db.user"), settings.user());
        assertEquals(properties.getProperty("db.password"), settings.password());
    }

    @Test
    void missingSettingsNeverFallBackToLocalhost() {
        for (String key : new String[]{"db.url", "db.user", "db.password"}) {
            Properties properties = configured();
            properties.remove(key);
            assertThrows(IllegalStateException.class, () -> DbSettings.from(properties, Map.of()), key);
        }
    }

    @Test
    void explicitEnvironmentOverridesArePreserved() {
        DbSettings settings = DbSettings.from(configured(), Map.of(
                "DB_URL", "jdbc:mysql://example.invalid/another_database",
                "DB_USER", "override-user", "DB_PASSWORD", "override-password"));
        assertEquals("jdbc:mysql://example.invalid/another_database", settings.url());
        assertEquals("override-user", settings.user());
        assertEquals("override-password", settings.password());
    }

    @Test
    void blankEnvironmentValuesUseProperties() {
        Properties properties = configured();
        DbSettings settings = DbSettings.from(properties, Map.of(
                "DB_URL", " ", "DB_USER", "", "DB_PASSWORD", ""));
        assertEquals(properties.getProperty("db.url"), settings.url());
        assertEquals(properties.getProperty("db.user"), settings.user());
        assertEquals(properties.getProperty("db.password"), settings.password());
    }
    @Test void serverSettingsKeepExactPasswordWithoutDisclosingItInToString() {
        DbSettings settings=DbSettings.fromRemote(Map.of("url","jdbc:mysql://example.invalid/course",
                "user","team-user","password","  p@ss:word=한글  "));
        assertEquals("  p@ss:word=한글  ",settings.password());
        assertFalse(settings.toString().contains(settings.password()));
        assertFalse(settings.toString().contains(settings.url()));
    }

    @Test void partialServerSettingsAreRejectedInsteadOfMixingWithLocalSettings() {
        assertThrows(IllegalStateException.class,()->DbSettings.fromRemote(Map.of("url","jdbc:mysql://host/db","user","team")));
        assertThrows(IllegalStateException.class,()->DbSettings.fromRemote(Map.of("url","jdbc:mysql://host/db","user","team","password"," ")));
    }
}