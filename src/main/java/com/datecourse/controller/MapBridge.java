package com.datecourse.controller;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.net.URI;
import java.util.Locale;

/** Narrow JavaScript interface; held by the controller so WebView cannot garbage-collect it. */
public final class MapBridge {
    private final Runnable ready;
    private final BiConsumer<Double, Double> selected;
    private final Consumer<String> failed;
    private final Consumer<String> externalLink;
    private final Consumer<Long> placeSelected;
    private final Runnable originChangeBlocked;
    private Consumer<Long> reportPlace = ignored -> {};
    private Consumer<Long> addCandidate = ignored -> {};
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
    public MapBridge(
            Runnable ready,
            BiConsumer<Double, Double> selected,
            Consumer<String> failed
    ) {
        this(ready, selected, failed, ignored -> {});
    }

    public MapBridge(Runnable ready, BiConsumer<Double, Double> selected,
                     Consumer<String> failed, Consumer<String> externalLink) {
        this(ready, selected, failed, externalLink, ignored -> {});
    }

    public MapBridge(Runnable ready, BiConsumer<Double, Double> selected,
                     Consumer<String> failed, Consumer<String> externalLink, Consumer<Long> placeSelected) {
        this(ready, selected, failed, externalLink, placeSelected, () -> {});
    }

    public MapBridge(Runnable ready, BiConsumer<Double, Double> selected, Consumer<String> failed,
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

    /** Synchronous, pure decision; no UI mutation or SDK calls cross this bridge. */
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
