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
        return loadMap(420, 500);
    }

    private WebView previewView;
    private WebEngine loadMap(int width, int height) throws Exception {
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
            window.categoryRequests = [];
            window.addressRequests = [];
            window.bridgeSelections = [];
            window.externalLinks = [];
            window.routeLines = [];
            window.pins = [];
            window.mapPans = [];
            window.zoomCalls = [];
            window.nativeWheelEvents = 0;
            window.javaMapBridge = {
                onMapReady: function () {}, onMapError: function () {},
                openExternalLink: function (address) { externalLinks.push(address); },
                onLocationSelected: function (lat, lng) { bridgeSelections.push([lat, lng]); }
            };
            function Position(lat, lng) { this.getLat = function () { return lat; }; this.getLng = function () { return lng; }; }
            function Shape(options) {
                this.options = options; window.pins.push(this);
                this.setPosition = function (position) { this.position = position; };
                this.setRadius = function (radius) { this.radius = radius; };
                this.setMap = function (map) { this.attached = map !== null; };
                this.setImage = function (image) { this.image = image; };
                this.setZIndex = function (z) { this.zIndex = z; };
                this.setOptions = function (options) { Object.assign(this.options,options); };
            }
            window.kakao = { maps: {
                LatLng: Position, Marker: Shape, Circle: Shape,
                Size: function (w, h) { this.width = w; this.height = h; },
                Point: function (x, y) { this.x = x; this.y = y; },
                MarkerImage: function (src, size, options) { this.src = src; this.size = size; this.options = options; },
                Polyline: function (options) {
                    this.options = options;
                    this.setMap = function (map) { this.attached = map !== null; };
                    routeLines.push(this);
                },
                Map: function (element, options) {
                    window.testMap = this;
                    window.testMapOptions = options;
                    var level = options.level;
                    this.getLevel = function () { return level; };
                    this.setLevel = function (value, settings) { level = value; zoomCalls.push({ level:value, settings:settings }); };
                    element.addEventListener('wheel', function () { nativeWheelEvents++; });
                    var center = options.center;
                    this.getCenter = function () { return center; };
                    this.setCenter = function (p) { center = p; window.lastCenter = [p.getLat(), p.getLng()]; };
                    this.panTo = function (p) { center = p; window.mapPans.push([p.getLat(),p.getLng()]); };
                    this.getProjection = function () { return {
                        containerPointFromCoords: function(p) { return {x:(p.getLng()-center.getLng())*100000+element.clientWidth/2,y:(center.getLat()-p.getLat())*100000+element.clientHeight/2}; },
                        coordsFromContainerPoint: function(p) { return new Position(center.getLat()-(p.y-element.clientHeight/2)/100000,center.getLng()+(p.x-element.clientWidth/2)/100000); }
                    }; };
                    this.relayout = function () {};
                },
                event: {
                    addListener: function (target, name, callback) { target['on' + name] = callback; },
                    removeListener: function (target, name, callback) { if (target['on' + name] === callback) delete target['on' + name]; }
                },
                services: {
                    SortBy: { DISTANCE: 'distance' },
                    Status: { OK: 'OK', ZERO_RESULT: 'ZERO_RESULT', ERROR: 'ERROR' },
                    Geocoder: function () { this.coord2Address = function (lng, lat, callback) { addressRequests.push({ lat: lat, lng: lng, callback: callback }); }; },
                    Places: function () {
                        this.keywordSearch = function (query, callback, options) { requests.push({ query: query, callback: callback, options: options }); };
                        this.categorySearch = function (category, callback, options) { categoryRequests.push({ category: category, callback: callback, options: options }); };
                    }
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
        try (var railJs = getClass().getResourceAsStream("/map/itinerary-rail.js");
             var railCss = getClass().getResourceAsStream("/map/itinerary-rail.css")) {
            html = html.replace("<script src=\"./itinerary-rail.js\"></script>", "<script>" + new String(railJs.readAllBytes(), StandardCharsets.UTF_8) + "</script>")
                    .replace("<link rel=\"stylesheet\" href=\"./itinerary-rail.css\">", "<style>" + new String(railCss.readAllBytes(), StandardCharsets.UTF_8) + "</style>");
        }
        try (var instantZoom = getClass().getResourceAsStream("/map/instant-zoom.js")) {
            html = html.replace("<script src=\"./instant-zoom.js\"></script>",
                    "<script>" + new String(instantZoom.readAllBytes(), StandardCharsets.UTF_8) + "</script>");
        }
        String content = html;
        CountDownLatch loaded = new CountDownLatch(1);
        FutureTask<WebEngine> create = new FutureTask<>(() -> {
            WebView view = new WebView();
            previewView = view;
            StackPane root = new StackPane(view);
            new Scene(root, width, height);
            root.resize(width, height);
            root.applyCss();
            root.layout();
            view.resize(width, height);
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
    void plannerTabsShareDetailsAndKeepRecommendedRouteWhileBrowsingNearby() throws Exception {
        WebEngine engine=loadMap(1100,720);
        seedItinerary(engine);
        script(engine,"""
            window.extraPlace={recordId:104,placeId:'CAFE_2',name:'주변 카페',category:'CAFE',lat:35.1595,lng:129.0615,price:4000,rating:4.2};
            window.updatePlannerPlaces(testCourse,testCourse.concat([extraPlace]));
            """);
        assertEquals("true",script(engine,"document.getElementById('course-tab').getAttribute('aria-selected')"));
        script(engine,"document.getElementById('nearby-tab').click()");
        assertEquals(4,((Number)script(engine,"document.querySelectorAll('.mini-place-card').length")).intValue());
        script(engine,"document.querySelectorAll('.mini-place-card')[3].click()");
        assertEquals(0,((Number)script(engine,"mapPans.length")).intValue());
        assertEquals(4,((Number)script(engine,"pins.filter(function(p){return p.visitId && p.attached;}).length")).intValue());
        assertEquals(2,((Number)script(engine,"routeLines.length")).intValue());
        assertEquals(true,script(engine,"routeLines.every(function(p){return p.attached;})"));
        script(engine,"document.querySelectorAll('.mini-place-card')[3].click()");
        assertEquals("주변 카페",script(engine,"document.getElementById('detail-title').textContent"));
        script(engine,"document.getElementById('course-tab').click()");
        Thread.sleep(250);
        assertEquals(3,((Number)script(engine,"document.querySelectorAll('.mini-place-card').length")).intValue());
        assertEquals(true,script(engine,"document.getElementById('place-detail').hidden"));
        assertEquals(2,((Number)script(engine,"routeLines.length")).intValue());
    }

    @Test
    void plannerEmptyStatesAndRadiusEmphasisFollowEditingAndExploration() throws Exception {
        WebEngine engine=loadMap(1100,720);
        script(engine,"window.updatePlannerPlaces([],[]);window.setPlanningMode(true)");
        assertEquals(false,script(engine,"document.getElementById('itinerary-rail').hidden"));
        assertTrue(((String)script(engine,"document.getElementById('itinerary-empty').textContent")).contains("출발 위치"));
        script(engine,"document.getElementById('course-tab').click()");
        assertTrue(((String)script(engine,"document.getElementById('itinerary-empty').textContent")).contains("아직 추천 코스"));
        assertEquals(0.25,((Number)script(engine,"pins.find(function(p){return p.options && p.options.fillOpacity===0 && p.options.strokeColor==='#2563eb';}).options.strokeOpacity")).doubleValue());
        script(engine,"window.setPlanningMode(true)");
        assertEquals(0.85,((Number)script(engine,"pins.find(function(p){return p.options && p.options.fillOpacity===0 && p.options.strokeColor==='#2563eb';}).options.strokeOpacity")).doubleValue());
    }

    @Test
    void instantWheelZoomPreservesPointerAnchorAndCourseObjects() throws Exception {
        WebEngine engine=loadMap(1100,720);
        seedItinerary(engine);
        script(engine,"""
            window.mapElement=document.getElementById('map');window.bounds=mapElement.getBoundingClientRect();
            window.oldPinCount=pins.length;window.oldLines=routeLines.length;
            window.expectedAnchor=testMap.getProjection().coordsFromContainerPoint(new kakao.maps.Point(100,120));
            window.sendWheel=function(delta){mapElement.dispatchEvent(new WheelEvent('wheel',{deltaY:delta,clientX:bounds.left+100,clientY:bounds.top+120,bubbles:true,cancelable:true}));};
            sendWheel(-120);
            """);
        assertEquals(4,((Number)script(engine,"testMap.getLevel()")).intValue());
        assertEquals(true,script(engine,"typeof mapElement.animate==='function' && mapElement.getAnimations().length>0"));
        assertEquals(true,script(engine,"zoomCalls[0].settings.animate===false && zoomCalls[0].settings.anchor.getLat()===expectedAnchor.getLat() && zoomCalls[0].settings.anchor.getLng()===expectedAnchor.getLng()"));
        assertEquals(true,script(engine,"nativeWheelEvents===0 && testMapOptions.disableDoubleClickZoom && pins.length===oldPinCount && routeLines.length===oldLines"));
        assertEquals("[[35.157,129.059]]",script(engine,"JSON.stringify(bridgeSelections)"));
        assertEquals("true",script(engine,"document.querySelector('.mini-place-card').getAttribute('aria-selected')"));
    }

    @Test
    void instantZoomGroupsTrackpadBurstsAndClampsDoubleClicksToMapLevels() throws Exception {
        WebEngine engine=loadMap(1100,720);
        script(engine,"""
            window.mapElement=document.getElementById('map');window.bounds=mapElement.getBoundingClientRect();
            window.sendWheel=function(delta){mapElement.dispatchEvent(new WheelEvent('wheel',{deltaY:delta,clientX:bounds.left+100,clientY:bounds.top+120,bubbles:true,cancelable:true}));};
            sendWheel(-10);sendWheel(-10);
            """);
        assertEquals(0,((Number)script(engine,"zoomCalls.length")).intValue());
        script(engine,"sendWheel(-10);for(var i=0;i<20;i++)sendWheel(-30)");
        Thread.sleep(180);
        assertEquals(2,((Number)script(engine,"zoomCalls.length")).intValue());
        assertEquals(3,((Number)script(engine,"testMap.getLevel()")).intValue());
        script(engine,"""
            window.doubleZoom=function(shift){mapElement.dispatchEvent(new MouseEvent('dblclick',{clientX:bounds.left+120,clientY:bounds.top+120,shiftKey:shift,bubbles:true,cancelable:true}));};
            testMap.setLevel(1,{});window.beforeLimit=zoomCalls.length;doubleZoom(false);
            """);
        assertEquals(true,script(engine,"zoomCalls.length===beforeLimit && testMap.getLevel()===1"));
        script(engine,"testMap.setLevel(14,{});window.beforeLimit=zoomCalls.length;doubleZoom(true)");
        assertEquals(true,script(engine,"zoomCalls.length===beforeLimit && testMap.getLevel()===14"));
        script(engine,"doubleZoom(false)");
        assertEquals(true,script(engine,"testMap.getLevel()===13 && zoomCalls[zoomCalls.length-1].settings.animate===false"));
    }

    @Test
    void detailLinkResolvesToActualPlaceAndCachesMatchingLookup() throws Exception {
        WebEngine engine = loadMap(1100,720);
        seedItinerary(engine);
        script(engine, "document.querySelector('.mini-place-card').click()");
        assertEquals("식당 테스트", script(engine,"requests[0].query"));
        assertEquals(500, ((Number)script(engine,"requests[0].options.radius")).intValue());
        script(engine,"""
            requests[0].callback([
                {place_name:'식당 테스트',y:'35.18',x:'129.08',place_url:'https://place.map.kakao.com/999'},
                {place_name:'식당 테스트',y:'35.158',x:'129.06',place_url:'http://place.map.kakao.com/12345'}
            ],'OK');
            """);
        Thread.sleep(100);
        assertEquals("https://place.map.kakao.com/12345",script(engine,"document.getElementById('detail-map-link').href"));
        script(engine,"document.getElementById('detail-map-link').click()");
        assertEquals("https://place.map.kakao.com/12345",script(engine,"externalLinks[0]"));
        script(engine,"document.querySelector('.mini-place-card').click();document.querySelector('.mini-place-card').click()");
        Thread.sleep(100);
        assertEquals(1,((Number)script(engine,"requests.length")).intValue());
        assertEquals("https://place.map.kakao.com/12345",script(engine,"document.getElementById('detail-map-link').href"));
    }

    @Test
    void outdatedPlaceLookupsCannotChangeAnotherPlacesLinkAndWrongBranchesFallBackToSearch() throws Exception {
        WebEngine engine=loadMap(1100,720);
        seedItinerary(engine);
        script(engine,"""
            document.querySelector('.mini-place-card').click();
            document.querySelectorAll('.mini-place-card')[1].click();
            document.querySelectorAll('.mini-place-card')[1].click();
            requests[0].callback([{place_name:'식당 테스트',y:'35.158',x:'129.06',place_url:'https://place.map.kakao.com/12345'}],'OK');
            requests[1].callback([{place_name:'카페 테스트 다른지점',y:'35.159',x:'129.061',place_url:'https://place.map.kakao.com/67890'}],'OK');
            categoryRequests[0].callback([], 'ZERO_RESULT');
            """);
        Thread.sleep(100);
        assertEquals("https://map.kakao.com/link/search/"+java.net.URLEncoder.encode("부산 카페 테스트",StandardCharsets.UTF_8).replace("+","%20"),
                script(engine,"document.getElementById('detail-map-link').href"));
        assertTrue(((String)script(engine,"document.getElementById('detail-map-link').textContent")).contains("검색"));
        assertEquals("false",script(engine,"document.getElementById('detail-map-link').getAttribute('aria-busy')"));
    }

    @Test
    void abbreviatedCafeNamesCanResolveThroughNearbyCategoryLookup() throws Exception {
        WebEngine engine=loadMap(1100,720);
        seedItinerary(engine);
        script(engine,"""
            testCourse[1].name='컴포즈파크에비뉴';window.showResultPlaces(testCourse,true);
            document.querySelectorAll('.mini-place-card')[1].click();document.querySelectorAll('.mini-place-card')[1].click();
            requests[0].callback([], 'ZERO_RESULT');
            categoryRequests[0].callback([{place_name:'컴포즈커피 부산파크에비뉴점',y:'35.159',x:'129.061',place_url:'https://place.map.kakao.com/54321'}],'OK');
            """);
        Thread.sleep(100);
        assertEquals("CE7",script(engine,"categoryRequests[0].category"));
        assertEquals("https://place.map.kakao.com/54321",script(engine,"document.getElementById('detail-map-link').href"));
    }

    @Test
    void radiusBorderAndWhiteOutlineFollowLocationAndRadiusWithoutFillingMap() throws Exception {
        WebEngine engine=loadMap();
        script(engine,"window.selectMapLocation(35.157,129.059,'서면역');window.setMapRadius(1200)");
        assertEquals(true,script(engine,"""
            (function(){var circles=pins.filter(function(p){return p.options && p.options.fillOpacity===0;});
            return circles.length===2 && circles.every(function(p){return p.radius===1200 && p.position.getLat()===35.157 && p.attached;})
            && circles.some(function(p){return p.options.strokeColor==='#ffffff' && p.options.strokeWeight===6;})
            && circles.some(function(p){return p.options.strokeColor==='#2563eb' && p.options.strokeWeight===2.5;});})()
            """));
    }

    @Test
    void itineraryCardsSeparateSelectionFromDetailsAndKeepMapObjects() throws Exception {
        WebEngine engine = loadMap(1100, 720);
        seedItinerary(engine);
        assertEquals(3, ((Number) script(engine, "document.querySelectorAll('.mini-place-card').length")).intValue());
        assertEquals(true, script(engine, "document.getElementById('itinerary-viewport').clientWidth===document.getElementById('itinerary-viewport').offsetWidth"));
        assertEquals(0, ((Number) script(engine, "mapPans.length")).intValue());
        script(engine, "window.pinCount=pins.length; window.lineCount=routeLines.length; document.querySelectorAll('.mini-place-card')[1].click()");
        Thread.sleep(350);
        assertEquals("true", script(engine, "document.querySelectorAll('.mini-place-card')[1].getAttribute('aria-selected')"));
        assertEquals(true, script(engine, "document.getElementById('place-detail').hidden"));
        assertEquals(0, ((Number) script(engine, "mapPans.length")).intValue());
        assertEquals(true, script(engine, "Math.abs(document.querySelectorAll('.mini-place-card')[1].parentElement.getBoundingClientRect().top + 39 - (document.getElementById('itinerary-viewport').getBoundingClientRect().top + document.getElementById('itinerary-viewport').clientHeight/2)) < 3"));
        script(engine, "window.railBefore=document.getElementById('itinerary-rail').getBoundingClientRect().toJSON(); document.querySelectorAll('.mini-place-card')[1].click()");
        Thread.sleep(250);
        assertEquals("카페 테스트", script(engine, "document.getElementById('detail-title').textContent"));
        assertEquals(1, ((Number) script(engine, "mapPans.length")).intValue());
        assertEquals(true, script(engine, """
            (function(){var map=document.getElementById('map').getBoundingClientRect(), detail=document.getElementById('place-detail').getBoundingClientRect();
            var p=testMap.getProjection().containerPointFromCoords(new kakao.maps.LatLng(testCourse[1].lat,testCourse[1].lng));
            var rail=document.getElementById('itinerary-rail').getBoundingClientRect();
            return p.x>20 && p.x+map.left<detail.left-15 && detail.left>map.left && detail.right<rail.left && rail.width===railBefore.width && rail.left===railBefore.left;})()
            """));
        assertEquals(true, script(engine, "pins.filter(function(p){return p.visitId && p.zIndex===6;}).length===1"));
        script(engine, "window.showResultPlaces(testCourse,true)");
        assertEquals(true, script(engine, "pins.length===pinCount && routeLines.length===lineCount"));
        script(engine, "document.getElementById('detail-close').click()");
        Thread.sleep(250);
        assertEquals(true, script(engine, "document.getElementById('place-detail').hidden"));
        assertEquals(1, ((Number) script(engine, "mapPans.length")).intValue());
    }

    private void seedItinerary(WebEngine engine) throws Exception {
        script(engine, """
            window.selectMapLocation(35.157,129.059,'서면역');
            window.testCourse = [
                {recordId:101,placeId:'MEAL_1',name:'식당 테스트',category:'MEAL',lat:35.158,lng:129.06,price:10000,rating:4.3},
                {recordId:102,placeId:'CAFE_1',name:'카페 테스트',category:'CAFE',lat:35.159,lng:129.061,price:5000,rating:4.5},
                {recordId:103,placeId:'CULTURE_1',name:'문화 공간 테스트',category:'CULTURE',lat:35.16,lng:129.062,price:0,rating:0}
            ];
            window.showResultPlaces(testCourse,true);
            """);
        Thread.sleep(80);
    }

    @Test
    void itineraryPinClicksScrollAndUserScrollingDoesNotPan() throws Exception {
        WebEngine engine = loadMap(1100, 720);
        seedItinerary(engine);
        script(engine, "pins.filter(function(p){return p.visitId;})[2].onclick()");
        Thread.sleep(450);
        assertEquals("문화 공간 테스트", script(engine, "document.getElementById('detail-title').textContent"));
        assertEquals("true", script(engine, "document.querySelectorAll('.mini-place-card')[2].getAttribute('aria-selected')"));
        assertEquals(1, ((Number) script(engine, "mapPans.length")).intValue());
        script(engine, """
            var viewport=document.getElementById('itinerary-viewport');
            viewport.dispatchEvent(new Event('wheel'));
            viewport.scrollTop=0;
            viewport.dispatchEvent(new Event('scroll'));
            viewport.scrollTop=86;
            viewport.dispatchEvent(new Event('scroll'));
            viewport.scrollTop=0;
            viewport.dispatchEvent(new Event('scroll'));
            """);
        Thread.sleep(650);
        assertEquals("true", script(engine, "document.querySelectorAll('.mini-place-card')[0].getAttribute('aria-selected')"));
        assertEquals(true, script(engine, "document.getElementById('place-detail').hidden"));
        assertEquals(1, ((Number) script(engine, "mapPans.length")).intValue());
        assertEquals(2, ((Number) script(engine, "routeLines.length")).intValue());
    }

    @Test
    void itineraryCourseReplacementCancelsAnimationsAndUsesUniqueVisitIds() throws Exception {
        WebEngine engine = loadMap(1100, 720);
        seedItinerary(engine);
        script(engine, """
            pins.filter(function(p){return p.visitId;})[2].onclick();
            window.oldPin=pins.filter(function(p){return p.visitId;})[2];
            window.showResultPlaces([testCourse[0],testCourse[0]],true);
            """);
        Thread.sleep(500);
        assertEquals(2, ((Number) script(engine, "document.querySelectorAll('.mini-place-card').length")).intValue());
        assertEquals(true, script(engine, "document.querySelectorAll('.mini-place-card')[0].dataset.visitId!==document.querySelectorAll('.mini-place-card')[1].dataset.visitId"));
        assertEquals(true, script(engine, "document.getElementById('place-detail').hidden && !oldPin.onclick"));
        assertEquals("true", script(engine, "document.querySelectorAll('.mini-place-card')[0].getAttribute('aria-selected')"));
        script(engine, "window.showResultPlaces([],false)");
        Thread.sleep(250);
        assertEquals(true, script(engine, "document.getElementById('itinerary-rail').hidden && document.getElementById('place-detail').hidden"));
    }

    @Test
    void itineraryKeyboardAndNarrowSheetStayInsideMap() throws Exception {
        WebEngine engine = loadMap(420, 680);
        seedItinerary(engine);
        script(engine, "document.querySelector('.mini-place-card').dispatchEvent(new KeyboardEvent('keydown',{key:'End',bubbles:true}));");
        Thread.sleep(350);
        assertEquals("true", script(engine, "document.querySelectorAll('.mini-place-card')[2].getAttribute('aria-selected')"));
        script(engine, "document.querySelectorAll('.mini-place-card')[2].click()");
        Thread.sleep(250);
        assertEquals(true, script(engine, """
            (function(){var map=document.getElementById('map').getBoundingClientRect(),detail=document.getElementById('place-detail').getBoundingClientRect(),rail=document.getElementById('itinerary-rail').getBoundingClientRect();
            return detail.left>=map.left && detail.right<=map.right && detail.bottom<=map.bottom && detail.top>=rail.bottom;})()
            """));
        assertEquals(false, script(engine, "document.getElementById('detail-facts').textContent.includes('평점')"));
        assertEquals(2, ((Number) script(engine, "document.getElementById('detail-facts').children.length")).intValue());
        script(engine, "document.getElementById('detail-close').dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}))");
        Thread.sleep(250);
        assertEquals(true, script(engine, "document.getElementById('place-detail').hidden && document.activeElement===document.querySelectorAll('.mini-place-card')[2]"));
    }

    @Test
    void textChangesSearchAutomaticallyAndWhitespaceCancelsPendingResults() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
            window.typeQuery = function (text) {
                document.getElementById('place-query').value = text;
                document.getElementById('place-query').dispatchEvent(new Event('input'));
            };
            typeQuery('서'); typeQuery('서면역');
            """);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (((Number) script(engine, "requests.length")).intValue() == 0) {
            assertTrue(System.nanoTime() < deadline, "Automatic search did not start");
            Thread.sleep(20);
        }
        assertEquals(1, ((Number) script(engine, "requests.length")).intValue());
        assertEquals("부산 서면역", script(engine, "requests[0].query"));
        script(engine, """
            typeQuery('부산역'); typeQuery('   ');
            requests[0].callback([{place_name:'서면역',address_name:'부산 부산진구',y:'35.157',x:'129.059'}], 'OK');
            """);
        Thread.sleep(450);
        assertEquals(1, ((Number) script(engine, "requests.length")).intValue());
        assertEquals(0, ((Number) script(engine, "document.querySelectorAll('#search-results button').length")).intValue());
        assertEquals("", script(engine, "document.getElementById('search-message').textContent"));
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
    void searchResultsOverlayMapWithoutResizingItAndCanBeDismissed() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
            window.mapBeforeSearch = document.getElementById('map').getBoundingClientRect().toJSON();
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
                const popup = document.getElementById('search-popup').getBoundingClientRect();
                return search.bottom <= map.top && map.bottom <= footer.top && map.height > 200
                    && map.top === mapBeforeSearch.top && map.height === mapBeforeSearch.height
                    && map.width === mapBeforeSearch.width && popup.bottom > map.top;
            })()
            """), geometry);
        script(engine, "document.getElementById('search-close').click()");
        assertEquals(0, ((Number) script(engine, "document.querySelectorAll('#search-results button').length")).intValue());
        assertEquals("", script(engine, "document.getElementById('search-message').textContent"));
        assertEquals(true, script(engine, "document.getElementById('search-popup').hidden"));
    }

    @Test
    void searchArrowKeysNavigateResultsAndEnterSelectsWithoutAnotherSearch() throws Exception {
        WebEngine engine=loadMap();
        script(engine,"""
            submitQuery('서면');
            requests[0].callback(Array.from({length:15},function(_,i){return {place_name:'서면 '+i,address_name:'부산 부산진구',y:'35.157',x:'129.059'};}),'OK');
            window.queryField=document.getElementById('place-query');queryField.focus();
            window.searchKey=function(key,composing){document.activeElement.dispatchEvent(new KeyboardEvent('keydown',{key:key,isComposing:!!composing,bubbles:true,cancelable:true}));};
            searchKey('ArrowDown',true);
            """);
        assertEquals(null,script(engine,"queryField.getAttribute('aria-activedescendant')"));
        script(engine,"searchKey('ArrowUp')");
        assertEquals("서면 14",script(engine,"document.querySelector('#search-results [aria-selected=true] .place-name').textContent"));
        assertEquals(true,script(engine,"document.activeElement===document.querySelectorAll('#search-results button')[14] && document.getElementById('search-results').scrollTop>0"));
        script(engine,"searchKey('ArrowDown');searchKey('ArrowUp')");
        assertEquals("서면 13",script(engine,"document.querySelector('#search-results [aria-selected=true] .place-name').textContent"));
        script(engine,"searchKey('Enter')");
        assertEquals("서면 13",script(engine,"document.getElementById('map-status').textContent"));
        assertEquals("서면 13",script(engine,"queryField.value"));
        assertEquals(1,((Number)script(engine,"requests.length")).intValue());
        assertEquals("[[35.157,129.059]]",script(engine,"JSON.stringify(bridgeSelections)"));
        assertEquals(true,script(engine,"document.getElementById('search-popup').hidden && queryField.getAttribute('aria-expanded')==='false' && !queryField.hasAttribute('aria-activedescendant')"));
        script(engine,"submitQuery('부산역');requests[1].callback([{place_name:'부산역',address_name:'부산 동구',y:'35.115',x:'129.042'}],'OK');searchKey('ArrowDown');searchKey('Escape')");
        assertEquals(true,script(engine,"document.getElementById('search-popup').hidden"));
        assertEquals("[[35.157,129.059]]",script(engine,"JSON.stringify(bridgeSelections)"));
        script(engine,"submitQuery('서면');requests[2].callback([{place_name:'서면역',address_name:'부산 부산진구',y:'35.157',x:'129.059'}],'OK');searchKey('Down')");
        assertEquals(true,script(engine,"document.activeElement===document.querySelector('#search-results button')"));
        script(engine,"searchKey('Up')");
        assertEquals(true,script(engine,"document.activeElement===queryField && !queryField.hasAttribute('aria-activedescendant')"));
        script(engine,"queryField.dispatchEvent(new KeyboardEvent('keydown',{keyCode:40,bubbles:true,cancelable:true}))");
        assertEquals(true,script(engine,"document.activeElement===document.querySelector('#search-results button')"));
        script(engine,"searchKey('Escape')");
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
    void databaseResultMarkersAndFocusPreserveStartingPoint() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
            window.resultPins = [];
            kakao.maps.Marker = function (options) {
                this.options = options;
                this.setMap = function (map) { this.attached = map !== null; };
                this.setImage = this.setZIndex = function () {};
                resultPins.push(this);
            };
            window.selectMapLocation(35.157, 129.059, '출발 위치');
            window.showResultPlaces([{lat:35.158,lng:129.06,name:'식당'}, {lat:35.159,lng:129.061,name:'카페'}], true);
            window.focusResultPlace(35.158,129.06);
            """);
        assertEquals(2, ((Number) script(engine, "resultPins.length")).intValue());
        assertEquals("[35.158,129.06]", script(engine, "JSON.stringify(lastCenter)"));
        assertEquals("[[35.157,129.059]]", script(engine, "JSON.stringify(bridgeSelections)"));
        assertEquals("[[35.157,129.059],[35.158,129.06],[35.159,129.061]]", script(engine,
                "JSON.stringify(routeLines[0].options.path.map(function(p){return [p.getLat(),p.getLng()];}))"));
        script(engine, "window.showResultPlaces([])");
        assertEquals(false, script(engine, "resultPins[0].attached || resultPins[1].attached"));
        assertEquals(false, script(engine, "routeLines[0].attached"));
        assertEquals(false, script(engine, "routeLines[1].attached"));
        script(engine, "window.showResultPlaces([{lat:35.158,lng:129.06,name:'주변 장소'}], false)");
        assertEquals(2, ((Number) script(engine, "routeLines.length")).intValue());
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
            assertNull(root.lookup("#placeList"));
            assertTrue(root.lookup("#resultSummary").getBoundsInLocal().getHeight() < 90);
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
