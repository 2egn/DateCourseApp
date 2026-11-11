package com.datecourse.service;

import com.datecourse.model.WalkingRoute;
import com.datecourse.model.WalkingRoute.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Requests one walking leg per consecutive pair, retaining API geometry without straight-line joins. */
public final class WalkingRouteService {
    private static final URI ENDPOINT = URI.create("https://apis.openapi.sk.com/tmap/routes/pedestrian?version=1");
    private final HttpClient client;
    private final URI endpoint;
    private final ObjectMapper json = new ObjectMapper();
    private record CacheKey(String key, int option, Stop from, Stop to) {}
    private record Cached(Leg leg, long created) {}
    private final Map<CacheKey, Cached> cache = new LinkedHashMap<>(64, 0.75f, true);

    public WalkingRouteService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build(), ENDPOINT);
    }

    WalkingRouteService(HttpClient client, URI endpoint) { this.client = client; this.endpoint = endpoint; }

    public WalkingRoute route(List<Stop> stops, TmapSettings settings) throws IOException, InterruptedException {
        stops = List.copyOf(stops);
        if (stops.size() < 2) throw new IllegalArgumentException("출발 위치와 방문할 장소를 선택해 주세요.");
        if (settings.appKey().isEmpty()) throw new IOException("도보 경로 서비스가 설정되지 않았어요. 앱 제공자에게 문의해 주세요.");
        List<Leg> legs = new ArrayList<>();
        long distance = 0, time = 0;
        for (int index = 1; index < stops.size(); index++) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            Stop from = stops.get(index - 1), to = stops.get(index);
            Leg leg = from.lat() == to.lat() && from.lng() == to.lng()
                    ? new Leg(from, to, 0, 0, List.of(), List.of()) : leg(from, to, settings);
            legs.add(leg);
            distance = Math.addExact(distance, leg.distance());
            time = Math.addExact(time, leg.time());
        }
        return new WalkingRoute(legs, distance, time);
    }

    private Leg leg(Stop from, Stop to, TmapSettings settings) throws IOException, InterruptedException {
        CacheKey cacheKey = new CacheKey(settings.appKey(), settings.searchOption(), from, to);
        synchronized (cache) {
            Cached cached = cache.get(cacheKey);
            if (cached != null && System.nanoTime() - cached.created() < Duration.ofMinutes(5).toNanos()) return cached.leg();
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("startX", from.lng()); body.put("startY", from.lat());
        body.put("endX", to.lng()); body.put("endY", to.lat());
        body.put("startName", URLEncoder.encode(from.name(), StandardCharsets.UTF_8));
        body.put("endName", URLEncoder.encode(to.name(), StandardCharsets.UTF_8));
        body.put("reqCoordType", "WGS84GEO"); body.put("resCoordType", "WGS84GEO");
        body.put("searchOption", Integer.toString(settings.searchOption())); body.put("sort", "index");
        HttpRequest request = HttpRequest.newBuilder(endpoint).timeout(Duration.ofSeconds(20))
                .header("appKey", settings.appKey()).header("Accept", "application/json")
                .header("Content-Type", "application/json; charset=utf-8")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body), StandardCharsets.UTF_8)).build();
        HttpResponse<String> response;
        try { response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)); }
        catch (HttpTimeoutException failure) { throw new IOException("TMAP 도보 경로 응답 시간이 초과됐어요. 다시 조회해 주세요."); }
        catch (IOException failure) { throw new IOException("TMAP에 연결하지 못했어요. 인터넷 연결을 확인해 주세요."); }
        if (response.statusCode() != 200) {
            String message = switch (response.statusCode()) {
                case 401, 403 -> "도보 경로 서비스 인증을 확인할 수 없어요. 앱 제공자에게 문의해 주세요.";
                case 429 -> "TMAP 요청 한도를 초과했어요. 잠시 후 다시 조회해 주세요.";
                case 400, 404 -> "선택한 위치 사이의 도보 경로를 찾지 못했어요.";
                default -> "TMAP 도보 경로 조회에 실패했어요. 잠시 후 다시 시도해 주세요.";
            };
            throw new IOException(message + " (HTTP " + response.statusCode() + ")");
        }
        Leg leg = parse(from, to, response.body());
        synchronized (cache) {
            cache.put(cacheKey, new Cached(leg, System.nanoTime()));
            while (cache.size() > 64) cache.remove(cache.keySet().iterator().next());
        }
        return leg;
    }

    static Leg parse(Stop from, Stop to, String response) throws IOException {
        try {
            JsonNode root = new ObjectMapper().readTree(response);
            JsonNode features = root.path("features");
            if (!features.isArray() || features.isEmpty()) throw new IllegalArgumentException();
            List<List<Coordinate>> paths = new ArrayList<>();
            List<Step> steps = new ArrayList<>();
            long distance = -1, time = -1;
            for (JsonNode feature : features) {
                JsonNode properties = feature.path("properties");
                if (properties.has("totalDistance")) distance = nonnegative(properties.get("totalDistance"));
                if (properties.has("totalTime")) time = nonnegative(properties.get("totalTime"));
                JsonNode geometry = feature.path("geometry");
                JsonNode coordinates = geometry.path("coordinates");
                if ("LineString".equals(geometry.path("type").asText())) {
                    if (!coordinates.isArray() || coordinates.size() < 2) throw new IllegalArgumentException();
                    List<Coordinate> path = new ArrayList<>();
                    for (JsonNode point : coordinates) path.add(coordinate(point));
                    paths.add(List.copyOf(path));
                } else if ("Point".equals(geometry.path("type").asText())) {
                    String description = properties.path("description").asText().trim();
                    if (!description.isEmpty()) steps.add(new Step(description, coordinate(coordinates)));
                }
            }
            if (paths.isEmpty() || distance < 0 || time < 0) throw new IllegalArgumentException();
            return new Leg(from, to, distance, time, paths, steps);
        } catch (RuntimeException | IOException failure) {
            throw new IOException("TMAP에서 올바른 도보 경로를 받지 못했어요. 위치를 바꾸거나 다시 조회해 주세요.");
        }
    }

    private static long nonnegative(JsonNode value) {
        if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() < 0) throw new IllegalArgumentException();
        return value.longValue();
    }

    private static Coordinate coordinate(JsonNode value) {
        if (!value.isArray() || value.size() < 2 || !value.get(0).isNumber() || !value.get(1).isNumber())
            throw new IllegalArgumentException();
        return new Coordinate(value.get(1).doubleValue(), value.get(0).doubleValue());
    }
}
