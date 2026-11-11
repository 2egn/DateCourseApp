package com.datecourse.service;

import com.datecourse.model.WalkingRoute;
import com.datecourse.model.WalkingRoute.Stop;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class WalkingRouteServiceTest {
    private static final TmapSettings SETTINGS = new TmapSettings("testTmapKey01234567890123456789", 0);
    private static final Stop START = new Stop(35.1796, 129.0756, "출발 위치");
    private static final Stop END = new Stop(35.1813, 129.0797, "도착 위치");
    private static final String RESPONSE = """
            {"type":"FeatureCollection","features":[
              {"geometry":{"type":"Point","coordinates":[129.0756,35.1796]},
               "properties":{"pointType":"SP","description":"횡단보도 후 직진","totalDistance":619,"totalTime":514}},
              {"geometry":{"type":"LineString","coordinates":[[129.0756,35.1796],[129.0760,35.1789]]},"properties":{}},
              {"geometry":{"type":"LineString","coordinates":[[129.0760,35.1789],[129.0797,35.1813]]},"properties":{}},
              {"geometry":{"type":"Point","coordinates":[129.0797,35.1813]},"properties":{"description":"목적지 도착"}}
            ]}
            """;

    @Test void postsCorrectCoordinatesAndHeaderAndCachesRepeatedLegs() throws Exception {
        AtomicInteger calls = new AtomicInteger();
        AtomicReference<String> requestBody = new AtomicReference<>(), requestKey = new AtomicReference<>(), method = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/pedestrian", exchange -> {
            calls.incrementAndGet();
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            requestKey.set(exchange.getRequestHeaders().getFirst("appKey"));
            method.set(exchange.getRequestMethod());
            byte[] body = RESPONSE.getBytes(StandardCharsets.UTF_8);
            try (exchange) { exchange.sendResponseHeaders(200, body.length); exchange.getResponseBody().write(body); }
        });
        server.start();
        try {
            WalkingRouteService service = new WalkingRouteService(HttpClient.newHttpClient(),
                    URI.create("http://localhost:" + server.getAddress().getPort() + "/pedestrian?version=1"));
            WalkingRoute route = service.route(List.of(START, END), SETTINGS);
            assertEquals(619, route.totalDistance());
            assertEquals(514, route.totalTime());
            assertEquals(2, route.legs().get(0).paths().size());
            assertEquals(35.1796, route.legs().get(0).paths().get(0).get(0).lat(), 0.000001);
            assertEquals("횡단보도 후 직진", route.legs().get(0).steps().get(0).description());
            var body = new ObjectMapper().readTree(requestBody.get());
            assertEquals(129.0756, body.path("startX").asDouble(), 0.000001);
            assertEquals(35.1796, body.path("startY").asDouble(), 0.000001);
            assertEquals("WGS84GEO", body.path("resCoordType").asText());
            assertTrue(body.path("startName").asText().startsWith("%"));
            assertFalse(requestBody.get().contains(SETTINGS.appKey()));
            assertEquals(SETTINGS.appKey(), requestKey.get());
            assertEquals("POST", method.get());
            assertEquals(route, service.route(List.of(START, END), SETTINGS));
            assertEquals(1, calls.get());
            WalkingRoute multi = service.route(List.of(START, END, new Stop(35.182, 129.08, "다음")), SETTINGS);
            assertEquals(1238, multi.totalDistance());
            assertEquals(1028, multi.totalTime());
            assertEquals(2, calls.get());
        } finally { server.stop(0); }
    }

    @Test void distinguishesAuthorizationQuotaAndServerErrorsWithoutExposingResponseText() throws Exception {
        for (int status : new int[]{401, 403, 429, 500}) {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                byte[] body = SETTINGS.appKey().getBytes(StandardCharsets.UTF_8);
                try (exchange) { exchange.sendResponseHeaders(status, body.length); exchange.getResponseBody().write(body); }
            });
            server.start();
            try {
                WalkingRouteService service = new WalkingRouteService(HttpClient.newHttpClient(),
                        URI.create("http://localhost:" + server.getAddress().getPort()));
                var failure = assertThrows(java.io.IOException.class, () -> service.route(List.of(START, END), SETTINGS));
                assertTrue(failure.getMessage().contains("HTTP " + status));
                assertFalse(failure.getMessage().contains(SETTINGS.appKey()));
            } finally { server.stop(0); }
        }
    }

    @Test void rejectsIncompleteOrInvalidResponsesInsteadOfInventingWalkingDistances() {
        for (String bad : new String[]{"{}", "not json", RESPONSE.replace("\"totalTime\":514", "\"other\":514"),
                RESPONSE.replace("[129.0760,35.1789]", "[129.0760,135.1789]")}) {
            assertThrows(java.io.IOException.class, () -> WalkingRouteService.parse(START, END, bad));
        }
    }

    @Test void zeroLengthLegUsesNoNetworkAndMissingKeyStopsBeforeSendingCoordinates() throws Exception {
        WalkingRouteService service = new WalkingRouteService(HttpClient.newHttpClient(), URI.create("http://localhost:1/"));
        assertEquals(0, service.route(List.of(START, START), SETTINGS).totalDistance());
        assertThrows(java.io.IOException.class, () -> service.route(List.of(START, END), new TmapSettings("", 0)));
        assertThrows(IllegalArgumentException.class, () -> new Stop(Double.NaN, 129, "bad"));
    }
}
