package com.datecourse.service;

import com.datecourse.model.*;
import com.datecourse.model.TravelRoute.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class KakaoRouteServiceTest {
    final ObjectMapper json=new ObjectMapper();
    final Stop from=new Stop(35.15,129.11,"출발"),to=new Stop(35.16,129.12,"도착");
    static final String KEY="a".repeat(32);
    static final String DRIVE="""
        {"routes":[{"result_code":0,"summary":{"distance":1500,"duration":240},"sections":[{
        "roads":[{"vertexes":[129.11,35.15,129.12,35.16]}],"guides":[{"guidance":"우회전","x":129.11,"y":35.15}]}]}]}
        """;
    static final String TRANSIT="""
        {"status":"OK","routes":[{"properties":{"totalDistance":2000,"totalTime":900,"transfers":1,"fare":{"value":1550}},
        "steps":[{"properties":{"type":"WALKING","guidance":"정류장까지 이동","distance":100,"time":120},"path":{"points":[[129.11,35.15],[129.111,35.151]]}},
        {"properties":{"type":"BUS","guidance":"54번 버스 탑승","distance":1900,"time":780},"path":{"points":[[129.112,35.152],[129.12,35.16]]}}]},
        {"properties":{"totalDistance":2500,"totalTime":1200,"transfers":2},"steps":[]}]}
        """;

    @Test void drivingPreservesRoadGeometryAndGuideCoordinates() throws Exception {
        var leg=new KakaoRouteService().driving(json.readTree(DRIVE),from,to);
        assertEquals(1500,leg.distance());assertEquals(240,leg.time());
        assertEquals(new Coordinate(35.15,129.11),leg.paths().getFirst().getFirst());
        assertEquals("우회전",leg.steps().getFirst().description());assertNull(leg.fare());
    }
    @Test void transitRetainsWalkAndBusStagesWithoutInventingStraightConnectors() throws Exception {
        var leg=new KakaoRouteService().transit(json.readTree(TRANSIT),from,to);
        assertEquals(900,leg.time());assertEquals(1550,leg.fare());assertEquals(1,leg.transfers());
        assertEquals(List.of("WALKING","BUS"),leg.transitStages().stream().map(TransitStage::type).toList());
        assertEquals(2,leg.paths().size());assertNotEquals(leg.paths().get(0).getLast(),leg.paths().get(1).getFirst());
        assertTrue(leg.steps().get(1).description().contains("54번"));
    }
    @Test void invalidOrUnavailableRoutesNeverBecomeZeroTimeOrStraightLines() throws Exception {
        var service=new KakaoRouteService();
        assertThrows(java.io.IOException.class,()->service.transit(json.readTree("{\"status\":\"NO_RESULTS\"}"),from,to));
        assertThrows(java.io.IOException.class,()->service.driving(json.readTree(DRIVE.replace("1500","-1")),from,to));
        assertThrows(java.io.IOException.class,()->service.driving(json.readTree(DRIVE.replace("129.11,35.15,129.12,35.16","")),from,to));
        assertThrows(java.io.IOException.class,()->service.transit(json.readTree(TRANSIT.replace("\"BUS\"","\"UNKNOWN\"")),from,to));
    }
    @Test void requestsCorrectEndpointAndKeepsCredentialOutOfUrlAndFailures() throws Exception {
        AtomicReference<String> auth=new AtomicReference<>(),query=new AtomicReference<>();
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/drive",exchange->{auth.set(exchange.getRequestHeaders().getFirst("Authorization"));query.set(exchange.getRequestURI().getRawQuery());byte[] body=DRIVE.getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});
        server.createContext("/transit",exchange->{query.set(exchange.getRequestURI().getRawQuery());byte[] body=TRANSIT.getBytes(java.nio.charset.StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,body.length);exchange.getResponseBody().write(body);exchange.close();});
        server.createContext("/denied",exchange->{byte[] body=("private-key-"+KEY).getBytes();exchange.sendResponseHeaders(403,body.length);exchange.getResponseBody().write(body);exchange.close();});
        server.start();
        try {
            URI root=URI.create("http://127.0.0.1:"+server.getAddress().getPort());
            var service=new KakaoRouteService(HttpClient.newHttpClient(),root.resolve("/drive"),root.resolve("/transit"));
            assertEquals(TravelMode.DRIVING,service.route(List.of(from,to),TravelMode.DRIVING,new KakaoRouteSettings(KEY)).mode());
            assertEquals("KakaoAK "+KEY,auth.get());assertTrue(query.get().contains("origin=129.11,35.15"));assertFalse(query.get().contains(KEY));
            assertEquals(TravelMode.TRANSIT,service.route(List.of(from,to),TravelMode.TRANSIT,new KakaoRouteSettings(KEY)).mode());
            assertTrue(query.get().contains("start_x=129.11"));
            var denied=new KakaoRouteService(HttpClient.newHttpClient(),root.resolve("/denied"),root.resolve("/denied"));
            var failure=assertThrows(java.io.IOException.class,()->denied.route(List.of(from,to),TravelMode.DRIVING,new KakaoRouteSettings(KEY)));
            assertFalse(failure.getMessage().contains(KEY));assertNull(failure.getCause());
        }finally {server.stop(0);}
    }
    @Test void localRestKeyIsSeparateAndEnvironmentOverridesIt(@TempDir Path root)throws Exception {
        Files.createDirectories(root.resolve(".local"));Files.writeString(root.resolve(KakaoRouteSettings.LOCAL_FILE),"kakao.rest.api.key="+KEY);
        assertEquals(KEY,KakaoRouteSettings.load(root,Map.of()).restApiKey());
        assertEquals("b".repeat(32),KakaoRouteSettings.load(root,Map.of("KAKAO_REST_API_KEY","b".repeat(32))).restApiKey());
        assertFalse(new KakaoRouteSettings(KEY).toString().contains(KEY));
        assertThrows(IllegalArgumentException.class,()->new KakaoRouteSettings("unsafe\nheader"));
        assertThrows(java.io.IOException.class,()->new KakaoRouteService().route(List.of(from,to),TravelMode.TRANSIT,new KakaoRouteSettings("")));
    }
}
