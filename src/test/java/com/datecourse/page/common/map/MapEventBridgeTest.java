package com.datecourse.page.common.map;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class MapEventBridgeTest {
    @Test void cameraBridgeUsesSharedPolicyAndIgnoresInvalidMeasurements() throws Exception {
        var bridge=new MapEventBridge(()->{},(lat,lng)->{},message->{});
        var json=new com.fasterxml.jackson.databind.ObjectMapper();
        var near=json.readTree(bridge.decideCameraTransition("FOCUS",5,4,.2,.8,false));
        assertEquals("PAN",near.get("movement").asText());assertEquals(5,near.get("level").asInt());
        assertEquals("NONE",json.readTree(bridge.decideCameraTransition("FOCUS",5,4,Double.NaN,1,false)).get("movement").asText());
        assertEquals("NONE",json.readTree(bridge.decideCameraTransition("unknown",5,4,.2,1,false)).get("movement").asText());
    }

    @Test void reportsOnlyValidPositivePlaceIdentifiers() {
        var reported=new java.util.ArrayList<Long>();
        MapEventBridge bridge=new MapEventBridge(()->{},(lat,lng)->{},error->{});bridge.setOnReportPlace(reported::add);
        for(String id:new String[]{null,"no-id","0","-1","9223372036854775808"})bridge.reportPlace(id);
        assertTrue(reported.isEmpty());bridge.reportPlace("12");assertEquals(java.util.List.of(12L),reported);
    }
    @Test
    void coordinatesKeepLatitudeLongitudeOrder() {
        AtomicReference<double[]> position = new AtomicReference<>();
        MapEventBridge bridge = new MapEventBridge(() -> {}, (lat, lng) -> position.set(new double[]{lat, lng}), error -> {});
        bridge.onLocationSelected(37.5665, 126.9780);
        assertArrayEquals(new double[]{37.5665, 126.9780}, position.get());
    }

    @Test
    void rejectsNonFiniteAndOutOfRangeCoordinates() {
        AtomicInteger accepted = new AtomicInteger();
        MapEventBridge bridge = new MapEventBridge(() -> {}, (lat, lng) -> accepted.incrementAndGet(), error -> {});
        bridge.onLocationSelected(Double.NaN, 127);
        bridge.onLocationSelected(37, Double.POSITIVE_INFINITY);
        bridge.onLocationSelected(91, 127);
        bridge.onLocationSelected(-91, 127);
        bridge.onLocationSelected(37, 181);
        bridge.onLocationSelected(37, -181);
        assertEquals(0, accepted.get());
        bridge.onLocationSelected(0, 0);
        assertEquals(1, accepted.get());
    }

    @Test
    void forwardsReadyAndFailureNotifications() {
        AtomicInteger ready = new AtomicInteger();
        AtomicReference<String> error = new AtomicReference<>();
        MapEventBridge bridge = new MapEventBridge(ready::incrementAndGet, (lat, lng) -> {}, error::set);
        bridge.onMapReady();
        bridge.onMapError("지도 연결 실패");
        assertEquals(1, ready.get());
        assertEquals("지도 연결 실패", error.get());
    }

    @Test
    void onlyKakaoWebLinksAreForwardedToExternalBrowser() {
        AtomicReference<String> opened = new AtomicReference<>();
        MapEventBridge bridge = new MapEventBridge(() -> {}, (lat, lng) -> {}, error -> {}, opened::set);
        bridge.openExternalLink("https://map.kakao.com/link/map/35.1,129.1");
        assertEquals("https://map.kakao.com/link/map/35.1,129.1", opened.get());
        opened.set(null);
        for (String address : new String[]{"javascript:alert(1)", "file:///C:/test", "https://kakao.com.evil.test/",
                "https://example.com/", "https://user@map.kakao.com/", "not a URL"}) {
            bridge.openExternalLink(address);
            assertNull(opened.get());
        }
    }
}
