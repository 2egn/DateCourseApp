package com.datecourse.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/** TMAP appKey is used only by Java HTTP requests and never sent to the map page. */
public record TmapSettings(String appKey, int searchOption) {
    public static final Path LOCAL_FILE = Path.of(".local", "tmap.properties");
    public static final Path PREFERENCES_FILE = Path.of(".local", "tmap-route.properties");

    public TmapSettings {
        appKey = appKey == null ? "" : appKey.trim();
        if (!appKey.isEmpty() && !appKey.matches("[A-Za-z0-9_-]{16,256}"))
            throw new IllegalArgumentException("TMAP appKey 형식을 확인해 주세요.");
        if (!Set.of(0, 4, 10, 30).contains(searchOption))
            throw new IllegalArgumentException("TMAP 탐색 옵션은 0, 4, 10, 30 중 하나여야 합니다.");
    }

    public static TmapSettings load() throws IOException {
        Properties service = new Properties();
        try (InputStream input = TmapSettings.class.getResourceAsStream("/tmap-service.properties")) {
            if (input != null) service.load(input);
        }
        return load(Path.of(""), System.getenv(), service);
    }

    static TmapSettings load(Path root, Map<String, String> environment) throws IOException {
        return load(root, environment, new Properties());
    }

    static TmapSettings load(Path root, Map<String, String> environment, Properties service) throws IOException {
        Properties properties = new Properties();
        properties.putAll(service);
        Path file = root.resolve(LOCAL_FILE);
        if (Files.exists(file)) {
            try (InputStream input = Files.newInputStream(file)) { properties.load(input); }
        }
        Path preferences = root.resolve(PREFERENCES_FILE);
        if (Files.exists(preferences)) {
            Properties choices = new Properties();
            try (InputStream input = Files.newInputStream(preferences)) { choices.load(input); }
            if (choices.containsKey("tmap.search.option"))
                properties.setProperty("tmap.search.option", choices.getProperty("tmap.search.option"));
        }
        String key = environment.get("TMAP_APP_KEY");
        if (key == null || key.isBlank()) key = properties.getProperty("tmap.app.key", "");
        try { return new TmapSettings(key, Integer.parseInt(properties.getProperty("tmap.search.option", "0").trim())); }
        catch (NumberFormatException failure) { throw new IllegalArgumentException("TMAP 탐색 옵션은 숫자로 입력해 주세요."); }
    }

    public static void saveLocal(TmapSettings settings) throws IOException { saveLocal(Path.of(""), settings); }

    public static void saveSearchOption(int option) throws IOException { saveSearchOption(Path.of(""), option); }

    static void saveSearchOption(Path root, int option) throws IOException {
        new TmapSettings("", option);
        Properties properties = new Properties();
        properties.setProperty("tmap.search.option", Integer.toString(option));
        Path file = root.resolve(PREFERENCES_FILE);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "tmap-route-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) { properties.store(output, "User walking route preference"); }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }

    static void saveLocal(Path root, TmapSettings settings) throws IOException {
        Properties properties = new Properties();
        properties.setProperty("tmap.app.key", settings.appKey());
        properties.setProperty("tmap.search.option", Integer.toString(settings.searchOption()));
        Path file = root.resolve(LOCAL_FILE);
        Files.createDirectories(file.getParent());
        Path temporary = Files.createTempFile(file.getParent(), "tmap-", ".tmp");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) { properties.store(output, "Local TMAP pedestrian routing settings"); }
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }

    @Override public String toString() {
        return "TmapSettings[key=" + (appKey.isEmpty() ? "missing" : "configured") + ", searchOption=" + searchOption + "]";
    }
}
