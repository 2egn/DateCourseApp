package com.datecourse.page.common.map;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.net.URI;
import java.util.Locale;

/**
 * - WebView의 JavaScript 이벤트를 Java 콜백으로 전달하는 연결용 클래스
 * - 컨트롤러에서 참조를 유지해 브리지가 GC로 해제되지 않도록 처리
 */
public final class MapEventBridge {
    private final Runnable ready;
    private final BiConsumer<Double, Double> selected;
    private final Consumer<String> failed;
    private final Consumer<String> externalLink;
    private final Consumer<Long> placeSelected;
    private final Runnable originChangeBlocked;
    private Consumer<Long> reportPlace = ignored -> {};
    private Consumer<Long> addCandidate = ignored -> {};
    private Runnable detailClosed = () -> {};
    /** 지도 상세를 사용자 조작으로 닫았을 때 JavaFX 조작 위치로 돌아간다. */
    public void setOnDetailClosed(Runnable listener) { detailClosed = listener == null ? () -> {} : listener; }
    public void onDetailClosed() { detailClosed.run(); }
    private Runnable routeImageRequested = () -> {};
    public void setOnRouteImageRequested(Runnable listener) { routeImageRequested = listener == null ? () -> {} : listener; }
    public void requestRouteImage() { routeImageRequested.run(); }
    public void setOnAddCandidate(Consumer<Long> listener) { addCandidate=listener; }
    public void addCandidate(String recordId) {
        try {long id=Long.parseLong(recordId);if(id>0)addCandidate.accept(id);}
        catch(NumberFormatException ignored) { }
    }
    public void setOnReportPlace(Consumer<Long> listener) { reportPlace=listener; }
    public void reportPlace(String recordId) {
        try { long id=Long.parseLong(recordId);if(id>0)reportPlace.accept(id); }
        catch(NumberFormatException ignored) { }
    }
    public MapEventBridge(
            Runnable ready,
            BiConsumer<Double, Double> selected,
            Consumer<String> failed
    ) {
        this(ready, selected, failed, ignored -> {});
    }

    public MapEventBridge(Runnable ready, BiConsumer<Double, Double> selected,
                     Consumer<String> failed, Consumer<String> externalLink) {
        this(ready, selected, failed, externalLink, ignored -> {});
    }

    public MapEventBridge(Runnable ready, BiConsumer<Double, Double> selected,
                     Consumer<String> failed, Consumer<String> externalLink, Consumer<Long> placeSelected) {
        this(ready, selected, failed, externalLink, placeSelected, () -> {});
    }

    public MapEventBridge(Runnable ready, BiConsumer<Double, Double> selected, Consumer<String> failed,
                     Consumer<String> externalLink, Consumer<Long> placeSelected, Runnable originChangeBlocked) {
        this.ready = ready;
        this.selected = selected;
        this.failed = failed;
        this.externalLink = externalLink;
        this.placeSelected = placeSelected;
        this.originChangeBlocked = originChangeBlocked;
    }

    private final com.datecourse.service.CameraMotionPolicy cameraPolicy = new com.datecourse.service.CameraMotionPolicy();
    private final com.fasterxml.jackson.databind.ObjectMapper cameraJson = new com.fasterxml.jackson.databind.ObjectMapper();

    /** 동기식 판단 결과만 반환하는 연결용 함수. UI 변경이나 SDK 호출은 하지 않음 */
    public String decideCameraTransition(String intent, int level, int fitLevel, double distance, double overflow, boolean reduced) {
        try {
            var frame = new com.datecourse.service.CameraMotionPolicy.Frame(level, fitLevel, distance, overflow, reduced);
            return cameraJson.writeValueAsString(cameraPolicy.decide(frame,
                    com.datecourse.service.CameraMotionPolicy.Intent.valueOf(intent)));
        } catch (Exception invalidRequest) { return "{\"movement\":\"NONE\",\"level\":5,\"duration\":0}"; }
    }

    public void onMapReady() { ready.run(); }
    public void onOriginChangeBlocked() { originChangeBlocked.run(); }

    public void onLocationSelected(double latitude, double longitude) {
        if (Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
            selected.accept(latitude, longitude);
        }
    }

    public void onMapError(String message) { failed.accept(message); }
    public void onPlaceSelected(String recordId) {
        try { placeSelected.accept(Long.parseLong(recordId)); }
        catch (NumberFormatException ignored) { }
    }

    public void openExternalLink(String address) {
        if (address == null) return;
        try {
            URI uri = URI.create(address);
            String host = uri.getHost();
            if (host == null || uri.getUserInfo() != null
                    || !("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))) return;
            host = host.toLowerCase(Locale.ROOT);
            if (host.equals("kakao.com") || host.endsWith(".kakao.com")
                    || host.equals("daum.net") || host.endsWith(".daum.net")) {
                externalLink.accept(uri.toASCIIString());
            }
        } catch (IllegalArgumentException ignored) {
            // Invalid external links must not replace the embedded map.
        }
    }
}
