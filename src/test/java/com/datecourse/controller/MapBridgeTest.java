package com.datecourse.controller;

import org.junit.jupiter.api.Test;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class MapBridgeTest {
    @Test
    void coordinatesKeepLatitudeLongitudeOrder() {
        AtomicReference<double[]> position = new AtomicReference<>();
        MapBridge bridge = new MapBridge(() -> {}, (lat, lng) -> position.set(new double[]{lat, lng}), error -> {});
        bridge.onLocationSelected(37.5665, 126.9780);
        assertArrayEquals(new double[]{37.5665, 126.9780}, position.get());
    }

    @Test
    void rejectsNonFiniteAndOutOfRangeCoordinates() {
        AtomicInteger accepted = new AtomicInteger();
        MapBridge bridge = new MapBridge(() -> {}, (lat, lng) -> accepted.incrementAndGet(), error -> {});
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
        MapBridge bridge = new MapBridge(ready::incrementAndGet, (lat, lng) -> {}, error::set);
        bridge.onMapReady();
        bridge.onMapError("지도 연결 실패");
        assertEquals(1, ready.get());
        assertEquals("지도 연결 실패", error.get());
    }
}
