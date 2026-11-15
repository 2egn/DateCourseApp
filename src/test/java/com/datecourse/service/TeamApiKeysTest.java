package com.datecourse.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class TeamApiKeysTest {
    @TempDir Path root;
    private static final String ACCESS = "ab".repeat(32);
    private static final String JSON = "{\"version\":1,\"keys\":{\"tmap\":\"testTmapKey0123456789\",\"kakaoJavascript\":\"kakao-test\",\"googlePlaces\":\"google-test\"}}";

    @Test void optsInWithOneLocalKeyAndEnvironmentCanOverrideIt() throws Exception {
        assertEquals("", TeamApiKeys.get("tmap", root, Map.of()));
        Files.createDirectories(root.resolve(".local"));
        Files.writeString(root.resolve(TeamApiKeys.LOCAL_FILE), "team.access.key=" + ACCESS);
        assertEquals(ACCESS, TeamApiKeys.accessKey(root, Map.of()));
        assertEquals("cd".repeat(32),TeamApiKeys.accessKey(root,Map.of("DATECOURSE_TEAM_ACCESS_KEY","cd".repeat(32))));
        assertThrows(IOException.class, () -> TeamApiKeys.accessKey(root, Map.of("DATECOURSE_TEAM_ACCESS_KEY", "invalid")));
    }

    @Test void decodesOnlyExpectedProviderKeysAndRejectsBadOrPartialPayloadsWithoutEchoingSecrets() throws Exception {
        assertEquals("google-test",TeamApiKeys.decode(input(JSON)).get("googlePlaces"));
        for(String payload : new String[]{"not-json-secret", "{}", JSON.replace("googlePlaces", "other"),
                JSON.replace("\"version\":1", "\"version\":2"), "x".repeat(4097)}) {
            IOException failure = assertThrows(IOException.class, () -> TeamApiKeys.decode(input(payload)));
            assertNull(failure.getCause());
            assertFalse(failure.getMessage().contains("secret"));
        }
    }

    @Test void sendsBearerInPostHeaderAndNeverWritesProviderKeysToDisk() throws Exception {
        AtomicReference<String> authorization = new AtomicReference<>();
        AtomicReference<String> method = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/keys",exchange -> {
            authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            method.set(exchange.getRequestMethod());
            byte[] bytes=JSON.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200,bytes.length);
            exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.start();
        try {
            assertEquals("kakao-test",TeamApiKeys.fetch(ACCESS, URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/keys")).get("kakaoJavascript"));
            assertEquals("Bearer "+ACCESS,authorization.get());assertEquals("POST",method.get());
            try(var files=Files.list(root)){assertEquals(0,files.count());}
        } finally {server.stop(0);}
    }

    @Test void doesNotFollowRedirectsOrIncludeErrorResponseInExceptions() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/keys",exchange -> {
            exchange.getResponseHeaders().set("Location","/leak");
            byte[] bytes="private-provider-secret".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(302,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();
        });
        server.createContext("/leak",exchange -> {exchange.sendResponseHeaders(200,-1);exchange.close();fail("Redirect followed");});
        server.start();
        try {
            IOException failure=assertThrows(IOException.class,()->TeamApiKeys.fetch(ACCESS,URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/keys")));
            assertNull(failure.getCause());assertFalse(failure.getMessage().contains("private-provider-secret"));
        } finally {server.stop(0);}
    }
    @Test void sharesSuccessfulFetchAcrossProvidersAndReloadsAfterCredentialChange() throws Exception {
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        TeamApiKeys.KeyLoader loader = access -> { calls.incrementAndGet(); return Map.of("tmap", "tmap-test", "googlePlaces", "google-test"); };
        Map<String,String> env=Map.of("DATECOURSE_TEAM_ACCESS_KEY", "12".repeat(32));
        assertEquals("tmap-test",TeamApiKeys.get("tmap",root,env,loader));
        assertEquals("google-test",TeamApiKeys.get("googlePlaces",root,env,loader));
        assertEquals(1,calls.get());
        TeamApiKeys.get("tmap",root,Map.of("DATECOURSE_TEAM_ACCESS_KEY","34".repeat(32)),loader);
        assertEquals(2,calls.get());
    }

    @Test void failedFetchIsNotCachedAndDoesNotReturnPreviousCredentialsKeys() throws Exception {
        Map<String,String> env=Map.of("DATECOURSE_TEAM_ACCESS_KEY", "56".repeat(32));
        assertThrows(IOException.class,()->TeamApiKeys.get("tmap",root,env,access->{throw new IOException("unavailable");}));
        assertEquals("retried-key",TeamApiKeys.get("tmap",root,env,access->Map.of("tmap","retried-key")));
    }
    @Test void databaseDecoderPreservesPasswordAndRejectsPartialOrOversizedResponses() throws Exception {
        Map<String,String> database=Map.of("url","jdbc:mysql://example.invalid/course?sslMode=REQUIRED",
                "user","team-user","password","  p@ss:word=한글  ");
        var mapper=new com.fasterxml.jackson.databind.ObjectMapper();
        String json=mapper.writeValueAsString(Map.of("version",1,"database",database));
        assertEquals(database,TeamApiKeys.decodeDatabase(input(json)));
        for(String payload:new String[]{"{}", "x".repeat(8193), json.replace("jdbc:mysql:","jdbc:other:"),
                mapper.writeValueAsString(Map.of("version",1,"database",Map.of("url","jdbc:mysql://host/db","user","team")))}) {
            IOException failure=assertThrows(IOException.class,()->TeamApiKeys.decodeDatabase(input(payload)));
            assertNull(failure.getCause());assertFalse(failure.getMessage().contains("p@ss"));
        }
    }

    private ByteArrayInputStream input(String value){return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));}
}
