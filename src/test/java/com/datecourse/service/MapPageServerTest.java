package com.datecourse.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class MapPageServerTest {
    @TempDir Path root;

    @Test void servesAllMapDependenciesAndDoesNotExposeOtherProjectFiles() throws Exception {
        try (MapPageServer server = new MapPageServer(0, root, Map.of())) {
            for (String file : new String[]{"map.html", "map.js", "instant-zoom.js", "itinerary-rail.js", "itinerary-rail.css", "kakao-config.js"}) {
                HttpURLConnection connection = request(server, file);
                try {
                    assertEquals(200, connection.getResponseCode(), file);
                    assertEquals("no-store", connection.getHeaderField("Cache-Control"));
                    assertFalse(body(connection).isBlank());
                } finally { connection.disconnect(); }
            }
            for (String path : new String[]{".local/db.properties", "../db.properties", "README.md"}) {
                HttpURLConnection connection = request(server, path);
                try { assertEquals(404, connection.getResponseCode()); }
                finally { connection.disconnect(); }
            }
        }
    }

    @Test void readsEditedKeysWithoutRebuildAndHonorsEnvironmentOverride() throws Exception {
        Path source = root.resolve("src/main/resources/map/kakao-config.js");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "window.KAKAO_MAP_KEY = 'first-key';");
        try (MapPageServer server = new MapPageServer(0, root, Map.of())) {
            assertEquals("window.KAKAO_MAP_KEY = \"first-key\";\n", config(server));
            Files.writeString(source, "window.KAKAO_MAP_KEY = \"second-key\";");
            assertEquals("window.KAKAO_MAP_KEY = \"second-key\";\n", config(server));
        }
        try (MapPageServer server = new MapPageServer(0, root, Map.of("KAKAO_MAP_JAVASCRIPT_KEY", "environment-key"))) {
            assertEquals("window.KAKAO_MAP_KEY = \"environment-key\";\n", config(server));
        }
    }

    @Test void supportsLocalSettingsOutsideTheSourceTree() throws Exception {
        Path local = root.resolve(".local/kakao-map.properties");
        Files.createDirectories(local.getParent());
        Files.writeString(local, "kakao.javascript.key=local-key\n");
        try (MapPageServer server = new MapPageServer(0, root, Map.of())) {
            assertEquals("window.KAKAO_MAP_KEY = \"local-key\";\n", config(server));
        }
    }

    private String config(MapPageServer server) throws Exception {
        HttpURLConnection connection = request(server, "kakao-config.js");
        try { return body(connection); } finally { connection.disconnect(); }
    }

    private HttpURLConnection request(MapPageServer server, String file) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) server.pageUri().resolve(file).toURL().openConnection();
        connection.setConnectTimeout(3000);
        connection.setReadTimeout(3000);
        return connection;
    }

    private String body(HttpURLConnection connection) throws Exception {
        return new String(connection.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
