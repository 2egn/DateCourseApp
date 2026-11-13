package com.datecourse.controller;

import com.datecourse.model.Place;
import com.datecourse.model.WalkingRoute;
import com.datecourse.model.WalkingRoute.Stop;
import com.datecourse.service.WalkingRouteService;
import com.datecourse.service.TmapSettings;
import javafx.concurrent.Task;

import com.datecourse.model.RoutePlan;
import com.datecourse.service.ExternalBrowser;
import com.datecourse.service.MapPageServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

import java.net.URI;
import java.io.IOException;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/** 두 WebView는 같은 지도 페이지를 사용하지만 브리지와 상태를 각각 소유한다. */
final class PlannerMap {
    @FunctionalInterface
    interface WalkingRouter {
        WalkingRoute route(List<Stop> stops) throws Exception;
    }
    private static final WalkingRouteService WALKING_SERVICE = new WalkingRouteService();
    private WalkingRouter walkingRouter = stops -> WALKING_SERVICE.route(stops, TmapSettings.load());
    private List<Stop> walkingStops = List.of();
    private WalkingRoute walkingRoute;
    private Task<WalkingRoute> walkingTask;
    private java.util.function.Function<String, Runnable> loadingUi = text -> () -> {};
    void setLoadingUi(java.util.function.Function<String, Runnable> ui) { loadingUi = ui; }
    private long walkingVersion;
    private boolean walkingRequested;
    private String walkingMessage = "";
    @FunctionalInterface
    interface PageSource { URI page() throws IOException; }
    private final PageSource pageSource;
    private URI page;
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
    private boolean originLocked, walkingPending;
    private Runnable originChangeBlocked = () -> {};

    PlannerMap(WebView view, BiConsumer<Double, Double> selected, Consumer<String> error) {
        this(view, selected, error, MapPageServer::sharedPage);
    }

    PlannerMap(WebView view, BiConsumer<Double, Double> selected, Consumer<String> error, PageSource pageSource) {
        this.view = view;
        this.error = error;
        this.pageSource = pageSource;
        bridge = new MapBridge(() -> {
            ready = true;
            render(true);
            if (walkingPending) requestWalkingRoute(false);
        }, selected, message -> {
            ready = false;
            loaded = false;
            error.accept(message);
        }, address -> {
            Thread worker = new Thread(() -> {
                try { ExternalBrowser.open(URI.create(address)); }
                catch (Exception failure) { Platform.runLater(() -> error.accept("외부 브라우저를 열지 못했어요.")); }
            }, "planner-external-link");
            worker.setDaemon(true);
            worker.start();
        }, id -> placeSelected.accept(id), () -> originChangeBlocked.run());
        view.getEngine().getLoadWorker().stateProperty().addListener((obs, old, state) -> {
            if (state == Worker.State.SCHEDULED) ready = false;
            else if (state == Worker.State.SUCCEEDED) {
                if (page == null || !page.toString().equals(view.getEngine().getLocation())) return;
                try {
                    ((JSObject) view.getEngine().executeScript("window")).setMember("javaMapBridge", bridge);
                    view.getEngine().executeScript("window.connectJavaMapBridge()");
                } catch (RuntimeException failure) { error.accept("지도 연결을 준비하지 못했어요."); }
            } else if (state == Worker.State.FAILED) {
                loaded = false;
                error.accept("지도를 열지 못했어요. 지도 다시 불러오기를 눌러 주세요.");
            }
        });
    }

    void load() {
        if (loaded) return;
        try {
            page = pageSource.page();
            loaded = true;
            view.getEngine().load(page.toString());
        } catch (IOException failure) {
            error.accept("지도 서버를 시작하지 못했어요. 다른 앱이나 수동 지도 서버의 8080 포트 사용을 확인하고 다시 불러와 주세요.");
        }
    }

    void reload() {
        ready = false;
        loaded = false;
        view.getEngine().getLoadWorker().cancel();
        view.getEngine().load(null);
        load();
    }

    void dispose() {
        walkingPending = false;
        walkingVersion++;
        if (walkingTask != null) walkingTask.cancel();
        ready = false;
        view.getEngine().getLoadWorker().cancel();
        view.getEngine().load(null);
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
        synchronizeWalkingRoute();
        render(fit);
    }

    private void render(boolean fit) {
        if (!ready || plan == null) return;
        try {
            sendOriginLock();
            ObjectMapper mapper = new ObjectMapper();
            view.getEngine().executeScript("window.presentRoute(" + mapper.writeValueAsString(plan) + ","
                    + mapper.writeValueAsString(candidate) + "," + radius + "," + locked + "," + fit + ","
                    + mapper.writeValueAsString(candidates) + ")");
            renderWalkingRoute(false);
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
    void setOnOriginChangeBlocked(Runnable listener) { originChangeBlocked = listener; }

    void setOriginLocked(boolean value) { originLocked = value; if (ready) sendOriginLock(); }
    private void sendOriginLock() {
        view.getEngine().executeScript("if (typeof window.setOriginLocked === 'function') window.setOriginLocked(" + originLocked + ")");
    }

    void toggleWalkingExplanation() {
        if (ready) view.getEngine().executeScript("if (typeof window.toggleWalkingExplanation === 'function') window.toggleWalkingExplanation()");
    }

    private List<Stop> currentWalkingStops() {
        if (plan == null) return List.of();
        List<Stop> stops = new java.util.ArrayList<>();
        stops.add(new Stop(plan.startLat(), plan.startLng(), "출발 위치"));
        for (Place place : plan.places())
            stops.add(new Stop(place.lat(), place.lng(), place.name()));
        if (candidate != null && plan.places().stream().noneMatch(place -> place.recordId() == candidate.recordId()))
            stops.add(new Stop(candidate.lat(), candidate.lng(), candidate.name() + " (후보)"));
        return List.copyOf(stops);
    }

    private void synchronizeWalkingRoute() {
        List<Stop> current = currentWalkingStops();
        if (current.equals(walkingStops)) return;
        walkingStops = current;
        walkingVersion++;
        walkingPending = false;
        if (walkingTask != null) walkingTask.cancel();
        walkingRoute = null;
        walkingMessage = walkingRequested ? "코스가 바뀌었어요. 도보 경로 조회를 눌러 다시 확인하세요." : "";
    }

    void requestWalkingRoute() {
        requestWalkingRoute(true);
    }

    void requestWalkingRouteWhenReady() {
        if (currentWalkingStops().size() < 2) return;
        if (!ready) { walkingPending = true; return; }
        requestWalkingRoute(false);
    }

    private void requestWalkingRoute(boolean fitResult) {
        synchronizeWalkingRoute();
        walkingRequested = true;
        if (!ready) {
            error.accept("지도를 불러온 뒤 도보 경로를 조회해 주세요.");
            return;
        }
        walkingPending = false;
        if (walkingStops.size() < 2) {
            walkingMessage = "출발 위치를 선택하고 코스에 장소를 담거나 추천 후보를 선택해 주세요.";
            renderWalkingRoute(false);
            return;
        }
        if (walkingTask != null) walkingTask.cancel();
        long version = ++walkingVersion;
        List<Stop> stops = walkingStops;
        walkingRoute = null;
        walkingMessage = "TMAP 도보 경로를 조회하는 중… (" + (stops.size() - 1) + "구간)";
        renderWalkingRoute(false);
        Task<WalkingRoute> task = new Task<>() {
            @Override protected WalkingRoute call() throws Exception { return walkingRouter.route(stops); }
        };
        walkingTask = task;
        Runnable finishLoading = loadingUi.apply("도보 경로를 조회하고 있어요");
        task.setOnCancelled(event -> finishLoading.run());
        task.setOnSucceeded(event -> {
            finishLoading.run();
            if (walkingVersion != version || walkingTask != task) return;
            walkingRoute = task.getValue();
            walkingMessage = "";
            renderWalkingRoute(fitResult);
        });
        task.setOnFailed(event -> {
            finishLoading.run();
            if (walkingVersion != version || walkingTask != task) return;
            walkingRoute = null;
            walkingMessage = task.getException() instanceof java.io.IOException
                    || task.getException() instanceof IllegalArgumentException ? task.getException().getMessage()
                    : "도보 경로를 불러오지 못했어요. 다시 조회해 주세요.";
            renderWalkingRoute(false);
            error.accept(walkingMessage);
        });
        Thread worker = new Thread(task, "planner-walking-route");
        worker.setDaemon(true);
        worker.start();
    }

    private void renderWalkingRoute(boolean fit) {
        if (!ready) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            view.getEngine().executeScript("if (typeof window.showWalkingRoute === 'function') window.showWalkingRoute("
                    + mapper.writeValueAsString(walkingRoute) + "," + mapper.writeValueAsString(walkingMessage) + "," + fit + ")");
        } catch (Exception failure) { error.accept("도보 경로를 지도에 표시하지 못했어요."); }
    }

    void setWalkingRouter(WalkingRouter router) { walkingRouter = router; }

    void refreshWalkingPreferences() {
        if (ready && currentWalkingStops().size() >= 2) requestWalkingRoute();
    }
}
