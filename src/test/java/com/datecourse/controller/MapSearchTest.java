package com.datecourse.controller;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.TextField;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.util.ResourceBundle;
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
            window.addressRequests = [];
            window.bridgeSelections = [];
            window.externalLinks = [];
            window.javaMapBridge = {
                onMapReady: function () {}, onMapError: function () {},
                openExternalLink: function (address) { externalLinks.push(address); },
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
                    Geocoder: function () { this.coord2Address = function (lng, lat, callback) { addressRequests.push({ lat: lat, lng: lng, callback: callback }); }; },
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
        assertEquals("서면역", script(engine, "document.getElementById('map-status').textContent"));
        script(engine, "window.setMapRadius(1000)");
        assertEquals("서면역", script(engine, "document.getElementById('map-status').textContent"));
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

    @Test
    void attributionLinksAndPopupsLeaveEmbeddedMapInPlace() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
            const link = document.createElement('a');
            link.href = 'https://map.kakao.com/';
            link.target = '_blank';
            link.textContent = 'Kakao';
            document.body.appendChild(link);
            link.click();
            window.open('https://map.kakao.com/link/map/35.1,129.1');
            """);
        assertEquals("[\"https://map.kakao.com/\",\"https://map.kakao.com/link/map/35.1,129.1\"]",
                script(engine, "JSON.stringify(externalLinks)"));
        assertEquals(true, script(engine, "document.getElementById('map') !== null"));
    }

    @Test
    void manualSelectionShowsAddressAndOldLookupsCannotReplaceNewPlaceName() throws Exception {
        WebEngine engine = loadMap();
        script(engine, "window.selectMapLocation(35.15,129.12)");
        assertEquals(129.12, ((Number) script(engine, "addressRequests[0].lng")).doubleValue());
        script(engine, "addressRequests[0].callback([{road_address:{address_name:'부산 수영구 광안해변로'}}], 'OK')");
        assertEquals("부산 수영구 광안해변로", script(engine, "document.getElementById('map-status').textContent"));
        script(engine, "window.selectMapLocation(35.16,129.13); window.selectMapLocation(35.157,129.059,'서면역'); addressRequests[1].callback([{address:{address_name:'이전 주소'}}], 'OK')");
        assertEquals("서면역", script(engine, "document.getElementById('map-status').textContent"));
        script(engine, "window.selectMapLocation(35.17,129.14); addressRequests[2].callback([], 'ZERO_RESULT')");
        assertEquals("선택한 위치", script(engine, "document.getElementById('map-status').textContent"));
    }

    @Test
    void plannerLayoutLoadsAndKeepsFieldsInsideSidebar() throws Exception {
        WebEngine fixture = loadMap();
        String mapPage = (String) script(fixture, "document.documentElement.outerHTML");
        CountDownLatch loaded = new CountDownLatch(1);
        FutureTask<Parent> create = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/condition-view.fxml"));
            // Layout inspection must not start the app's localhost server or use its real key.
            loader.setControllerFactory(type -> new ConditionController() {
                @Override public void initialize(URL url, ResourceBundle resources) {}
            });
            Parent root = loader.load();
            new Scene(root, 960, 680);
            root.resize(960, 680);
            FlowPane categories = (FlowPane) loader.getNamespace().get("categoryPane");
            for (String name : new String[]{"식사", "카페", "문화"}) {
                CheckBox box = new CheckBox(name);
                box.getStyleClass().add("category-chip");
                categories.getChildren().add(box);
            }
            ((CheckBox) categories.getChildren().get(0)).setSelected(true);
            WebView map = (WebView) root.lookup("#mapView");
            map.getEngine().getLoadWorker().stateProperty().addListener((o, previous, state) -> {
                if (state == Worker.State.SUCCEEDED) loaded.countDown();
            });
            root.applyCss();
            root.layout();
            map.snapshot(null, null);
            map.getEngine().loadContent(mapPage);
            return root;
        });
        Platform.runLater(create);
        Parent root = create.get(10, TimeUnit.SECONDS);
        assertTrue(loaded.await(10, TimeUnit.SECONDS));
        FutureTask<Void> inspect = new FutureTask<>(() -> {
            root.applyCss();
            root.layout();
            TextField budget = (TextField) root.lookup("#budgetField");
            TextField people = (TextField) root.lookup("#peopleField");
            assertTrue(budget.localToScene(budget.getBoundsInLocal()).getMaxX()
                    < people.localToScene(people.getBoundsInLocal()).getMinX());
            assertTrue(people.localToScene(people.getBoundsInLocal()).getMaxX() <= 300);
            assertTrue(root.lookup("#mapView").getBoundsInLocal().getWidth() > 500);
            CheckBox chip = (CheckBox) ((FlowPane) root.lookup("#categoryPane")).getChildren().get(0);
            chip.setSelected(false);
            chip.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("focused"), true);
            root.applyCss();
            assertEquals(javafx.scene.paint.Color.TRANSPARENT, chip.getBorder().getStrokes().get(0).getTopStroke());
            chip.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("hover"), true);
            root.applyCss();
            assertEquals(javafx.scene.paint.Color.TRANSPARENT, chip.getBorder().getStrokes().get(0).getTopStroke());
            Path image = Path.of("build", "ui-preview", "condition-planner.png");
            Files.createDirectories(image.getParent());
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null, null), null), "png", image.toFile());
            return null;
        });
        Platform.runLater(inspect);
        inspect.get(10, TimeUnit.SECONDS);
    }
}
