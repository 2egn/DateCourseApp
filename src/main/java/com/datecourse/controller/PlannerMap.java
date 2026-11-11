package com.datecourse.controller;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import com.datecourse.service.ExternalBrowser;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URI;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** 두 WebView는 같은 지도 페이지를 사용하지만 브리지와 상태를 각각 소유한다. */
final class PlannerMap {
    private static final String MAP_URL = "http://localhost:8080/map.html";
    private final WebView view;
    private final Consumer<String> error;
    private final MapBridge bridge;
    private boolean ready;
    private boolean loaded;
    private RoutePlan plan;
    private Place candidate;
    private List<Place> candidates = List.of();
    private Place pendingDetail;
    private Consumer<Long> placeSelected = ignored -> {};
    private int radius = 500;
    private boolean locked;

    PlannerMap(WebView view, BiConsumer<Double, Double> selected, Consumer<String> error) {
        this.view = view;
        this.error = error;
        bridge = new MapBridge(() -> { ready = true; render(true); }, selected, error, address -> {
            Thread worker = new Thread(() -> {
                try { ExternalBrowser.open(URI.create(address)); }
                catch (Exception failure) { Platform.runLater(() -> error.accept("외부 브라우저를 열지 못했어요.")); }
            }, "planner-external-link");
            worker.setDaemon(true);
            worker.start();
        }, id -> placeSelected.accept(id));
        view.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SCHEDULED) ready = false;
            else if (state == Worker.State.SUCCEEDED) {
                if (!MAP_URL.equals(view.getEngine().getLocation())) return;
                try {
                    ((JSObject) view.getEngine().executeScript("window")).setMember("javaMapBridge", bridge);
                    view.getEngine().executeScript("window.connectJavaMapBridge()");
                } catch (RuntimeException failure) { error.accept("지도 연결을 준비하지 못했어요."); }
            } else if (state == Worker.State.FAILED) error.accept("지도를 열지 못했어요. 로컬 지도 서버를 확인해 주세요.");
        });
    }

    void load() {
        if (!loaded) { loaded = true; view.getEngine().load(MAP_URL); }
    }

    void present(RoutePlan plan, Place candidate, int radius, boolean locked, boolean fit) {
        presentCandidates(plan, candidate == null ? List.of() : List.of(candidate), candidate, radius, locked, fit);
    }

    void presentCandidates(RoutePlan plan, List<Place> candidates, Place candidate, int radius, boolean locked, boolean fit) {
        this.plan = plan;
        this.candidates = List.copyOf(candidates);
        this.candidate = candidate;
        this.radius = radius;
        this.locked = locked;
        render(fit);
    }

    private void render(boolean fit) {
        if (!ready || plan == null) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            view.getEngine().executeScript("window.presentRoute(" + mapper.writeValueAsString(plan) + ","
                    + mapper.writeValueAsString(candidate) + "," + radius + "," + locked + "," + fit + ","
                    + mapper.writeValueAsString(candidates) + ")");
            if (pendingDetail != null) {
                view.getEngine().executeScript("window.openRoutePlace(" + pendingDetail.recordId() + ")");
                pendingDetail = null;
            }
        } catch (Exception failure) { error.accept("코스를 지도에 표시하지 못했어요."); }
    }

    void fit(boolean wholeRoute) {
        if (ready) view.getEngine().executeScript("window.fitRouteView(" + wholeRoute + ")");
    }

    void ensureRangesVisible() {
        if (ready) view.getEngine().executeScript("window.ensureRouteRangesVisible()");
    }

    void showDetail(Place place) {
        view.requestFocus();
        pendingDetail = place;
        if (ready) render(false);
    }

    void setOnPlaceSelected(Consumer<Long> listener) { placeSelected = listener; }
}
