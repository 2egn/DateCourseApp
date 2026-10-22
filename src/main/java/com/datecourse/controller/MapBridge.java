package com.datecourse.controller;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** Narrow JavaScript interface; held by the controller so WebView cannot garbage-collect it. */
public final class MapBridge {
    private final Runnable ready;
    private final BiConsumer<Double, Double> selected;
    private final Consumer<String> failed;

    public MapBridge(
            Runnable ready,
            BiConsumer<Double, Double> selected,
            Consumer<String> failed
    ) {
        this.ready = ready;
        this.selected = selected;
        this.failed = failed;
    }

    public void onMapReady() { ready.run(); }

    public void onLocationSelected(double latitude, double longitude) {
        if (Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180) {
            selected.accept(latitude, longitude);
        }
    }

    public void onMapError(String message) { failed.accept(message); }
}
