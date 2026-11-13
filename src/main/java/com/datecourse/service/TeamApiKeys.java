package com.datecourse.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;

/** One team credential on disk; provider keys live only in this process's memory. */
public final class TeamApiKeys {
    public static final Path LOCAL_FILE = Path.of(".local", "team-access.properties");
    private static final URI ENDPOINT = URI.create(
            "https://datecourseapp-team-docs.ate-ourse-pp.workers.dev/api/app/keys");
    private static final URI DATABASE_ENDPOINT = ENDPOINT.resolve("/api/app/database");
    private static String cachedDatabaseAccess;
    private static Map<String, String> cachedDatabase;
    private static String cachedAccess;
    private static Map<String, String> cachedKeys;
    private TeamApiKeys() {}

    /** Empty means this installation has not opted into team key loading. */
    public static String get(String provider) throws IOException {
        return get(provider, Path.of(""), System.getenv());
    }

    /** DB credentials are fetched separately, so API-only tools do not receive them. */
    public static synchronized Map<String, String> database() throws IOException {
        String access = accessKey(Path.of(""), System.getenv());
        if (access.isEmpty()) return Map.of();
        if (!access.equals(cachedDatabaseAccess) || cachedDatabase == null) {
            Map<String, String> loaded = fetch(access, DATABASE_ENDPOINT, true);
            cachedDatabase = loaded;
            cachedDatabaseAccess = access;
        }
        return cachedDatabase;
    }

    @FunctionalInterface interface KeyLoader { Map<String, String> load(String access) throws IOException; }

    static String get(String provider, Path root, Map<String, String> environment) throws IOException {
        return get(provider, root, environment, TeamApiKeys::fetch);
    }

    static synchronized String get(String provider, Path root, Map<String, String> environment, KeyLoader loader) throws IOException {
        String access = accessKey(root, environment);
        if (access.isEmpty()) return "";
        if (!access.equals(cachedAccess) || cachedKeys == null) {
            Map<String, String> loaded = loader.load(access);
            cachedKeys = loaded;
            cachedAccess = access;
        }
        String key = cachedKeys.get(provider);
        if (key == null) throw new IllegalArgumentException("지원하지 않는 API 종류입니다.");
        return key;
    }

    static String accessKey(Path root, Map<String, String> environment) throws IOException {
        String access = environment.getOrDefault("DATECOURSE_TEAM_ACCESS_KEY", "").trim();
        if (access.isEmpty() && Files.exists(root.resolve(LOCAL_FILE))) {
            Properties values = new Properties();
            try (InputStream input = Files.newInputStream(root.resolve(LOCAL_FILE))) { values.load(input); }
            access = values.getProperty("team.access.key", "").trim();
        }
        if (!access.isEmpty() && !access.matches("[a-f0-9]{64}"))
            throw new IOException("공통 접속 키 형식을 확인해 주세요.");
        return access;
    }

    private static Map<String, String> fetch(String access) throws IOException {
        return fetch(access, ENDPOINT);
    }

    static Map<String, String> fetch(String access, URI endpoint) throws IOException {
        return fetch(access, endpoint, false);
    }

    private static Map<String, String> fetch(String access, URI endpoint, boolean database) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) endpoint.toURL().openConnection();
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(8000);
        connection.setRequestProperty("Authorization", "Bearer " + access);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "DateCourseApp/1.0");
        try {
            int status = connection.getResponseCode();
            if (status != 200) throw statusFailure(status);
            try (InputStream input = connection.getInputStream()) { return database ? decodeDatabase(input) : decode(input); }
        } catch (IOException failure) {
            // Neither HTTP response bodies nor connection details containing credentials enter UI/logs.
            throw new IOException("팀 설정을 불러오지 못했어요. 접속 키와 네트워크를 확인해 주세요.");
        } finally { connection.disconnect(); }
    }

    static IOException statusFailure(int status) {
        return new IOException(status == 401 ? "공통 접속 키가 올바르지 않습니다." : "팀 키 서버에 연결하지 못했습니다.");
    }

    static Map<String, String> decodeDatabase(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(8193);
        try {
            if (bytes.length > 8192) throw new IllegalArgumentException();
            JsonNode json = new ObjectMapper().readTree(bytes);
            if (json == null || json.path("version").asInt() != 1) throw new IllegalArgumentException();
            JsonNode database = json.path("database");
            Map<String, String> result = new java.util.HashMap<>();
            for (String field : new String[]{"url", "user", "password"}) {
                JsonNode value = database.path(field);
                if (!value.isTextual() || value.textValue().isBlank()) throw new IllegalArgumentException();
                result.put(field, value.textValue());
            }
            if (!result.get("url").matches("jdbc:mysql://[^\\r\\n]{1,4096}")
                    || result.get("user").length() > 256 || result.get("password").length() > 2048)
                throw new IllegalArgumentException();
            return Map.copyOf(result);
        } catch (IOException | RuntimeException failure) {
            throw new IOException("팀 DB 설정 응답 형식이 올바르지 않습니다.");
        }
    }

    static Map<String, String> decode(InputStream input) throws IOException {
        byte[] bytes = input.readNBytes(4097);
        if (bytes.length > 4096) throw new IOException("팀 키 응답 형식이 올바르지 않습니다.");
        try {
            JsonNode json = new ObjectMapper().readTree(bytes);
            if (json == null || json.path("version").asInt() != 1) throw new IllegalArgumentException();
            Map<String, String> result = new java.util.HashMap<>();
            for (String provider : new String[]{"tmap", "kakaoJavascript", "googlePlaces"}) {
                JsonNode value = json.path("keys").path(provider);
                if (!value.isTextual() || !value.textValue().matches("[A-Za-z0-9_-]{1,256}"))
                    throw new IllegalArgumentException();
                result.put(provider, value.textValue());
            }
            return Map.copyOf(result);
        } catch (IOException | RuntimeException failure) {
            throw new IOException("팀 키 응답 형식이 올바르지 않습니다.");
        }
    }
}
