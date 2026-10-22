package com.datecourse.controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class MapSearchTest {
    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            started.countDown();
        });
        assertTrue(started.await(10, TimeUnit.SECONDS));
    }

    private static Object script(WebEngine engine, String code) throws Exception {
        FutureTask<Object> task = new FutureTask<>(() -> engine.executeScript(code));
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    private WebEngine loadMap() throws Exception {
        String html;
        String js;
        try (var page = getClass().getResourceAsStream("/map/map.html");
             var source = getClass().getResourceAsStream("/map/map.js")) {
            html = new String(page.readAllBytes(), StandardCharsets.UTF_8);
            js = new String(source.readAllBytes(), StandardCharsets.UTF_8);
        }
        // Exercise the real page in JavaFX WebKit without credentials or network requests.
        String mock = """
            window.KAKAO_MAP_KEY = 'test';
            window.requests = [];
            window.bridgeSelections = [];
            window.javaMapBridge = {
                onMapReady: function () {}, onMapError: function () {},
                onLocationSelected: function (lat, lng) { bridgeSelections.push([lat, lng]); }
            };
            function Position(lat, lng) { this.getLat = function () { return lat; }; this.getLng = function () { return lng; }; }
            function Shape() { this.setPosition = this.setRadius = this.setMap = function () {}; }
            window.kakao = { maps: {
                LatLng: Position, Marker: Shape, Circle: Shape,
                Map: function (element, options) {
                    this.getCenter = function () { return options.center; };
                    this.setCenter = function (p) { window.lastCenter = [p.getLat(), p.getLng()]; };
                    this.relayout = function () {};
                },
                event: { addListener: function () {} },
                services: {
                    Status: { OK: 'OK', ZERO_RESULT: 'ZERO_RESULT', ERROR: 'ERROR' },
                    Places: function () { this.keywordSearch = function (query, callback) { requests.push({ query: query, callback: callback }); }; }
                },
                load: function (callback) { callback(); }
            }};
            const append = document.head.appendChild.bind(document.head);
            document.head.appendChild = function (node) {
                if (node.tagName === 'SCRIPT') { setTimeout(function () { node.onload(); }, 0); return node; }
                return append(node);
            };
            window.submitQuery = function (query) {
                document.getElementById('place-query').value = query;
                document.getElementById('search-form').dispatchEvent(new Event('submit', { cancelable: true }));
            };
            """;
        html = html.replace("<script src=\"./kakao-config.js\"></script>", "<script>" + mock + "</script>")
                .replace("<script src=\"./map.js\"></script>", "<script>" + js + "</script>");
        String content = html;
        CountDownLatch loaded = new CountDownLatch(1);
        FutureTask<WebEngine> create = new FutureTask<>(() -> {
            WebView view = new WebView();
            StackPane root = new StackPane(view);
            new Scene(root, 420, 500);
            root.resize(420, 500);
            root.applyCss();
            root.layout();
            view.resize(420, 500);
            view.snapshot(null, null);
            WebEngine engine = view.getEngine();
            engine.getLoadWorker().stateProperty().addListener((o, previous, state) -> {
                if (state == Worker.State.SUCCEEDED) loaded.countDown();
            });
            engine.loadContent(content);
            return engine;
        });
        Platform.runLater(create);
        WebEngine engine = create.get(10, TimeUnit.SECONDS);
        assertTrue(loaded.await(10, TimeUnit.SECONDS));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while ((Boolean) script(engine, "document.getElementById('search-submit').disabled")) {
            assertTrue(System.nanoTime() < deadline, "Search SDK initialization failed");
            Thread.sleep(20);
        }
        return engine;
    }

    @Test
    void busanResultsSelectMapPositionAndNotifyJava() throws Exception {
        WebEngine engine = loadMap();
        script(engine, "submitQuery('서면역')");
        assertEquals("부산 서면역", script(engine, "requests[0].query"));
        script(engine, """
            requests[0].callback([
                {place_name:'서면역', address_name:'부산 부산진구 부전동', y:'35.157', x:'129.059'},
                {place_name:'다른 지역', address_name:'서울 중구', y:'37.5', x:'127.0'}
            ], 'OK');
            """);
        assertEquals(1, ((Number) script(engine, "document.querySelectorAll('#search-results button').length")).intValue());
        script(engine, "document.querySelector('#search-results button').click()");
        assertEquals("[35.157,129.059]", script(engine, "JSON.stringify(lastCenter)"));
        assertEquals("[[35.157,129.059]]", script(engine, "JSON.stringify(bridgeSelections)"));
        assertEquals(0, ((Number) script(engine, "document.querySelectorAll('#search-results button').length")).intValue());
    }

    @Test
    void outdatedResponsesCannotOverwriteLatestSearchAndErrorsKeepMapUsable() throws Exception {
        WebEngine engine = loadMap();
        script(engine, "submitQuery('서면역'); submitQuery('부산역'); requests[1].callback([], 'ZERO_RESULT');");
        String message = (String) script(engine, "document.getElementById('search-message').textContent");
        assertTrue(message.contains("검색 결과가 없어요"));
        script(engine, "requests[0].callback([], 'ERROR')");
        assertEquals(message, script(engine, "document.getElementById('search-message').textContent"));
        script(engine, "submitQuery('광안리'); requests[2].callback([], 'ERROR'); window.selectMapLocation(35.15, 129.12);");
        assertTrue(((String) script(engine, "document.getElementById('search-message').textContent")).contains("검색하지 못했어요"));
        assertEquals("[[35.15,129.12]]", script(engine, "JSON.stringify(bridgeSelections)"));
    }

    @Test
    void searchResultsNeverCoverMapAndCanBeDismissed() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
            submitQuery('서면역');
            requests[0].callback(Array.from({length:15}, function () {
                return {place_name:'서면역', address_name:'부산 부산진구 부전동', y:'35.157', x:'129.059'};
            }), 'OK');
            """);
        String geometry = (String) script(engine, "JSON.stringify(['place-search','map','map-status'].map(function(id){var r=document.getElementById(id).getBoundingClientRect();return {id:id,top:r.top,bottom:r.bottom,height:r.height};}))");
        assertEquals(true, script(engine, """
            (function () {
                const search = document.getElementById('place-search').getBoundingClientRect();
                const map = document.getElementById('map').getBoundingClientRect();
                const footer = document.getElementById('map-status').getBoundingClientRect();
                return search.bottom <= map.top && map.bottom <= footer.top && map.height > 200;
            })()
            """), geometry);
        script(engine, "document.getElementById('search-close').click()");
        assertEquals(0, ((Number) script(engine, "document.querySelectorAll('#search-results button').length")).intValue());
        assertEquals("", script(engine, "document.getElementById('search-message').textContent"));
    }
}
