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

    public MapBridge(
            Runnable ready,
            BiConsumer<Double, Double> selected,
            Consumer<String> failed
    ) {
        this(ready, selected, failed, ignored -> {});
    }

    public MapBridge(Runnable ready, BiConsumer<Double, Double> selected,
                     Consumer<String> failed, Consumer<String> externalLink) {
        this.ready = ready;
        this.selected = selected;
        this.failed = failed;
        this.externalLink = externalLink;
    }

    public void onMapReady() { ready.run(); }

    public void onLocationSelected(double latitude, double longitude) {
        if (Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
            selected.accept(latitude, longitude);
        }
    }

    public void onMapError(String message) { failed.accept(message); }

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
