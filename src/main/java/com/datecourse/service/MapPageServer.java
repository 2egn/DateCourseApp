package com.datecourse.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Pattern;

/** Both planner maps share a loopback server; no separate Python process is required. */
public final class MapPageServer implements AutoCloseable {
    public static final int PORT = 8080;
    private static final Pattern KEY_ASSIGNMENT = Pattern.compile(
            "window\\.KAKAO_MAP_KEY\\s*=\\s*([\"'])([A-Za-z0-9_-]*)\\1");
    private static final Map<String, String> ASSETS = Map.of(
            "/map.html", "text/html", "/map.js", "application/javascript",
            "/itinerary-rail.js", "application/javascript", "/instant-zoom.js", "application/javascript",
            "/itinerary-rail.css", "text/css");
    private static MapPageServer shared;
    private final HttpServer server;
    private final ExecutorService executor;
    private final Path root;
    private final Map<String, String> environment;

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            synchronized (MapPageServer.class) {
                if (shared != null) shared.close();
            }
        }, "planner-map-shutdown"));
    }

    public static synchronized URI sharedPage() throws IOException {
        if (shared == null) shared = new MapPageServer(PORT, Path.of(""), System.getenv());
        return shared.pageUri();
    }

    MapPageServer(int port, Path root, Map<String, String> environment) throws IOException {
        this.root = root;
        this.environment = Map.copyOf(environment);
        Map<String, byte[]> resources = new java.util.HashMap<>();
        for (String path : ASSETS.keySet()) {
            try (InputStream input = MapPageServer.class.getResourceAsStream("/map" + path)) {
                if (input == null) throw new IOException("지도 리소스를 찾을 수 없습니다: " + path);
                resources.put(path, input.readAllBytes());
            }
        }
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
        executor = Executors.newFixedThreadPool(2, task -> {
            Thread thread = new Thread(task, "planner-map-http");
            thread.setDaemon(true);
            return thread;
        });
        server.setExecutor(executor);
        server.createContext("/", exchange -> {
            try (exchange) {
                if (!"GET".equals(exchange.getRequestMethod())) {
                    exchange.getResponseHeaders().set("Allow", "GET");
                    exchange.sendResponseHeaders(405, -1);
                    return;
                }
                String path = exchange.getRequestURI().getPath();
                byte[] body = resources.get(path);
                String contentType = ASSETS.get(path);
                if ("/kakao-config.js".equals(path)) {
                    try { body = configuration(); }
                    catch (IOException failure) {
                        exchange.sendResponseHeaders(500, -1);
                        return;
                    }
                    contentType = "application/javascript";
                }
                if (body == null) { exchange.sendResponseHeaders(404, -1); return; }
                exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
                exchange.getResponseHeaders().set("Cache-Control", "no-store");
                exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
                exchange.sendResponseHeaders(200, body.length);
                exchange.getResponseBody().write(body);
            }
        });
        // HttpServer's dispatcher inherits this thread's daemon flag, allowing JavaFX demos to exit normally.
        FutureTask<Void> startup = new FutureTask<>(() -> { server.start(); return null; });
        Thread starter = new Thread(startup, "planner-map-start");
        starter.setDaemon(true);
        starter.start();
        try { startup.get(); }
        catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            close();
            throw new IOException("지도 서버 시작이 중단되었습니다.", failure);
        } catch (ExecutionException failure) {
            close();
            throw new IOException("지도 서버를 시작하지 못했습니다.", failure.getCause());
        }
    }

    private byte[] configuration() throws IOException {
        String key = TeamApiKeys.get("kakaoJavascript", root, environment);
        if (key.isEmpty()) key = environment.getOrDefault("KAKAO_MAP_JAVASCRIPT_KEY", "").trim();
        Path source = root.resolve("src/main/resources/map/kakao-config.js");
        if (key.isEmpty() && Files.exists(source)) {
            var assignment = KEY_ASSIGNMENT.matcher(Files.readString(source));
            if (assignment.find()) key = assignment.group(2);
        }
        Path local = root.resolve(".local/kakao-map.properties");
        if (key.isEmpty() && Files.exists(local)) {
            Properties properties = new Properties();
            try (InputStream input = Files.newInputStream(local)) { properties.load(input); }
            key = properties.getProperty("kakao.javascript.key", "").trim();
        }
        return ("window.KAKAO_MAP_KEY = " + new ObjectMapper().writeValueAsString(key) + ";\n")
                .getBytes(StandardCharsets.UTF_8);
    }

    public URI pageUri() {
        return URI.create("http://localhost:" + server.getAddress().getPort() + "/map.html");
    }

    @Override public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
