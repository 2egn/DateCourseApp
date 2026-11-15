package com.datecourse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class TmapSettingsTest {
    @Test void packagedServiceKeyWorksForNewUsersWhileTheirRoutePreferencesStaySeparate() throws Exception {
        java.util.Properties service = new java.util.Properties();
        service.setProperty("tmap.app.key", KEY);
        Path firstUser = root.resolve("first-user"), secondUser = root.resolve("second-user");
        assertEquals(KEY, TmapSettings.load(firstUser, Map.of(), service).appKey());
        assertEquals(KEY, TmapSettings.load(secondUser, Map.of(), service).appKey());
        TmapSettings.saveSearchOption(firstUser, 30);
        TmapSettings.saveSearchOption(secondUser, 4);
        assertEquals(30, TmapSettings.load(firstUser, Map.of(), service).searchOption());
        assertEquals(4, TmapSettings.load(secondUser, Map.of(), service).searchOption());
        assertFalse(Files.readString(firstUser.resolve(TmapSettings.PREFERENCES_FILE)).contains(KEY));
        assertFalse(Files.exists(firstUser.resolve(TmapSettings.LOCAL_FILE)));
    }

    @Test void changingRouteCategoryDoesNotRewriteTheSharedKey() throws Exception {
        TmapSettings.saveLocal(root, new TmapSettings(KEY, 0));
        String providerSettings = Files.readString(root.resolve(TmapSettings.LOCAL_FILE));
        TmapSettings.saveSearchOption(root, 10);
        assertEquals(providerSettings, Files.readString(root.resolve(TmapSettings.LOCAL_FILE)));
        assertEquals(KEY, TmapSettings.load(root, Map.of()).appKey());
        assertEquals(10, TmapSettings.load(root, Map.of()).searchOption());
        assertThrows(IllegalArgumentException.class, () -> TmapSettings.saveSearchOption(root, 99));
    }
    @TempDir Path root;
    private static final String KEY = "testTmapKey01234567890123456789";

    @Test void loadsLocalKeyAndAllowsEnvironmentOverrideWithoutLoggingTheKey() throws Exception {
        assertEquals("", TmapSettings.load(root, Map.of()).appKey());
        TmapSettings settings = new TmapSettings(KEY, 30);
        TmapSettings.saveLocal(root, settings);
        assertEquals(settings, TmapSettings.load(root, Map.of()));
        assertEquals("environmentKey0123456789", TmapSettings.load(root,
                Map.of("TMAP_APP_KEY", "environmentKey0123456789")).appKey());
        assertEquals(settings, TmapSettings.load(root, Map.of("TMAP_APP_KEY", " ")));
        assertFalse(settings.toString().contains(KEY));
        try (var files = Files.list(root.resolve(".local"))) {
            assertFalse(files.anyMatch(path -> path.toString().endsWith(".tmp")));
        }
    }

    @Test void rejectsInvalidKeysAndOptions() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new TmapSettings("bad\nkey", 0));
        assertThrows(IllegalArgumentException.class, () -> new TmapSettings(KEY, 99));
        Files.createDirectories(root.resolve(".local"));
        Files.writeString(root.resolve(TmapSettings.LOCAL_FILE), "tmap.search.option=abc\n");
        assertThrows(IllegalArgumentException.class, () -> TmapSettings.load(root, Map.of()));
    }
}
