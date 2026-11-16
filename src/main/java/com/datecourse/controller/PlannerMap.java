package com.datecourse.controller;

import com.datecourse.model.Place;
import com.datecourse.model.TravelRoute;
import com.datecourse.model.TravelRoute.Stop;
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
    private static final java.time.Duration CAPTURE_TIMEOUT = java.time.Duration.ofSeconds(20);
    private final com.datecourse.service.PlaceMapPayload placePayload = new com.datecourse.service.PlaceMapPayload(
            new ObjectMapper(), new com.datecourse.service.PlacePresenter());

    @FunctionalInterface
    interface WalkingRouter {
        TravelRoute route(List<Stop> stops) throws Exception;
    }
    @FunctionalInterface interface RouteRouter {TravelRoute route(List<Stop> stops,com.datecourse.model.TravelMode mode)throws Exception;}
    private static final com.datecourse.service.TravelRouteService ROUTE_SERVICE=new com.datecourse.service.TravelRouteService();
    private RouteRouter routeRouter=ROUTE_SERVICE::route;
    private com.datecourse.model.TravelMode routedMode=com.datecourse.model.TravelMode.WALKING;
    private com.datecourse.model.TravelMode mode(){return plan==null?com.datecourse.model.TravelMode.WALKING:plan.travelMode();}
    private List<Stop> walkingStops = List.of();
    private TravelRoute walkingRoute;
    private Task<TravelRoute> walkingTask;
    private java.util.function.Function<String, Runnable> loadingUi = text -> () -> {};
    void setLoadingUi(java.util.function.Function<String, Runnable> ui) { loadingUi = ui; }
    private long walkingVersion;
    private boolean walkingRequested;
    private boolean automaticWalking;
    private BiConsumer<TravelRoute,String> walkingResult = (route,message) -> {};
    private Consumer<Place> reportPlace = place -> {};
    private Consumer<Long> addCandidate = id -> {};
    private boolean candidateActionsEnabled=true;
    void setOnAddCandidate(Consumer<Long> listener) { addCandidate=listener; }
    void setCandidateActionsEnabled(boolean enabled) {
        candidateActionsEnabled=enabled;
        if(ready)view.getEngine().executeScript("if(window.setCandidateActionsEnabled)window.setCandidateActionsEnabled("+enabled+")");
    }
    void setOnReport(Consumer<Place> listener) { reportPlace=listener; }
    void setAutomaticWalking(boolean enabled) { automaticWalking=enabled; }
    void setWalkingResultListener(BiConsumer<TravelRoute,String> listener) { walkingResult=listener; }
    private void publishWalking() { walkingResult.accept(walkingRoute,walkingMessage); }
    private String walkingMessage = "";
    @FunctionalInterface
    interface PageSource { URI page() throws IOException; }
    private final PageSource pageSource;
    private URI page;
    private final WebView view;
    private final MapViewportLifecycle viewport;
    private boolean pageLoadPending;
    private final Consumer<String> error;
    private final MapBridge bridge;
    private boolean ready;
    private boolean capturing;
    private java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage> pendingCapture;
    private Runnable cancelCapture=()->{};
    private boolean loaded;
    private RoutePlan plan;
    private Place candidate;
    private List<Place> candidates = List.of();
    private Place pendingDetail;
    private Consumer<Long> placeSelected = ignored -> {};
    private int radius = 500;
    private boolean locked;
    private boolean originLocked, walkingPending;
    private boolean resetPending;
    private boolean rangeAdjusting;
    private Integer editingLegIndex, editingLegRadius;

    void setEditingLeg(Integer index, Integer radius) {
        editingLegIndex = index;
        editingLegRadius = radius;
    }
    private Runnable originChangeBlocked = () -> {};

    PlannerMap(WebView view, BiConsumer<Double, Double> selected, Consumer<String> error) {
        this(view, selected, error, MapPageServer::sharedPage);
    }

    PlannerMap(WebView view, BiConsumer<Double, Double> selected, Consumer<String> error, PageSource pageSource) {
        this.view = view;
        viewport = new MapViewportLifecycle(view, () -> {
            if(pageLoadPending){pageLoadPending=false;view.getEngine().load(page.toString());}
        });
        this.error = error;
        this.pageSource = pageSource;
        bridge = new MapBridge(() -> {
            ready = true;
            render(true);
            if (walkingPending) requestWalkingRoute(false);
            else if(walkingRequested && walkingRoute==null && walkingTask==null)requestWalkingRouteWhenReady();
        }, selected, message -> {
            ready = false;
            loaded = false;
            com.datecourse.service.AppDiagnostics.failure(com.datecourse.service.AppDiagnostics.Operation.MAP_LOAD,
                    com.datecourse.service.AppDiagnostics.Stage.MAP_PREPARE,new IllegalStateException());
            error.accept("지도를 불러오지 못했어요. 지도 새로고침을 눌러 다시 시도해 주세요.");
        }, address -> {
            Thread worker = new Thread(() -> {
                try { ExternalBrowser.open(URI.create(address)); }
                catch (Exception failure) { Platform.runLater(() -> error.accept("외부 브라우저를 열지 못했어요.")); }
            }, "planner-external-link");
            worker.setDaemon(true);
            worker.start();
        }, id -> Platform.runLater(()->placeSelected.accept(id)), () -> originChangeBlocked.run());
        bridge.setOnAddCandidate(id -> Platform.runLater(()->{
            if(candidateActionsEnabled && candidates.stream().anyMatch(p->p.recordId()==id))addCandidate.accept(id);
        }));
        bridge.setOnReportPlace(id -> Platform.runLater(() -> {
            java.util.stream.Stream.concat(plan==null?java.util.stream.Stream.empty():plan.places().stream(),candidates.stream())
                .filter(p->p.recordId()==id).findFirst().ifPresent(reportPlace);
        }));
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
            pageLoadPending = true;
            viewport.refresh();
        } catch (IOException failure) {
            com.datecourse.service.AppDiagnostics.failure(com.datecourse.service.AppDiagnostics.Operation.MAP_LOAD,
                    com.datecourse.service.AppDiagnostics.Stage.START,failure);
            error.accept("지도를 열지 못했어요. 다른 실행 창을 닫은 뒤 지도 새로고침을 눌러 주세요.");
        }
    }

    void reload() {
        pageLoadPending = false;
        ready = false;
        loaded = false;
        view.getEngine().getLoadWorker().cancel();
        view.getEngine().load(null);
        load();
    }

    boolean canCapturePreview() {
        return ready && !capturing && view.isVisible() && view.getWidth()>0 && view.getHeight()>0;
    }

    /** FX-thread only. Preserve the complete SDK attribution and restore the original viewport. */
    enum CaptureStage { PREPARING, CAPTURING }
    java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage> capturePreview(RoutePlan snapshot) {
        return capturePreview(snapshot,stage->{});
    }
    java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage> capturePreview(RoutePlan snapshot, Consumer<CaptureStage> progress) {
        progress.accept(CaptureStage.PREPARING);
        var result = new java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage>();
        if (capturing || !loaded && !ready || ready && !Boolean.TRUE.equals(view.getEngine().executeScript(
                "typeof window.beginCourseCapture === 'function'"))) {
            result.completeExceptionally(new IllegalStateException("지도가 준비되지 않았습니다."));
            return result;
        }
        double[] originalSize={view.getWidth(),view.getHeight()};
        boolean managed=view.isManaged();
        capturing=true;pendingCapture=result;
        var timer=new javafx.animation.Timeline();
        Runnable restore=()->{
            timer.stop();
            view.resize(originalSize[0],originalSize[1]);view.setManaged(managed);
            try {view.getEngine().executeScript("if(window.endCourseCapture)window.endCourseCapture()");}
            catch(Exception ignored) { /* Navigation may have disposed the page. */ }
            finally {capturing=false;pendingCapture=null;cancelCapture=()->{};render(false);}
        };
        cancelCapture=()->{restore.run();result.completeExceptionally(new java.util.concurrent.CancellationException());};
        result.whenComplete((image,failure)->{
            if(result.isCancelled())Platform.runLater(()->{if(pendingCapture==result)cancelCapture.run();});
        });
        long deadline=System.nanoTime()+CAPTURE_TIMEOUT.toNanos();
        boolean[] started={false};
        timer.getKeyFrames().add(new javafx.animation.KeyFrame(javafx.util.Duration.millis(100),event->{
            try {
                if(System.nanoTime()>deadline)throw new java.io.IOException("지도 타일 준비 시간 초과");
                if(!started[0]) {
                    if(view.getEngine().getLoadWorker().getState()==Worker.State.FAILED)
                        throw new java.io.IOException("지도 로딩 실패");
                    // A first save may arrive before SDK/bridge readiness or the first visible layout.
                    if(!ready || !view.isVisible() || view.getWidth()<=0 || view.getHeight()<=0)return;
                    // Let the native SDK animation finish before changing the viewport.
                    if(!Boolean.TRUE.equals(view.getEngine().executeScript(
                            "window.inspectMapCamera && !window.inspectMapCamera().moving")))return;
                    progress.accept(CaptureStage.CAPTURING);
                    originalSize[0]=view.getWidth();originalSize[1]=view.getHeight();
                    view.setManaged(false);view.resize(com.datecourse.service.CoursePreviewImage.MAP_WIDTH,com.datecourse.service.CoursePreviewImage.MAP_HEIGHT);
                    Object began=view.getEngine().executeScript("window.beginCourseCapture("+placePayload.plan(snapshot)+")");
                    if(!Boolean.TRUE.equals(began))throw new IllegalStateException("지도 캡처를 시작하지 못했습니다.");
                    started[0]=true;return;
                }
                if(Boolean.TRUE.equals(view.getEngine().executeScript("window.courseCaptureReady()"))) {
                    timer.stop();
                    view.snapshot(frame->{
                        try {
                            var image=javafx.embed.swing.SwingFXUtils.fromFXImage(frame.getImage(),null);
                            restore.run();result.complete(image);
                        } catch(Exception failure) {restore.run();result.completeExceptionally(failure);}
                        return null;
                    },null,null);
                }
            } catch(Exception failure) {restore.run();result.completeExceptionally(failure);}
        }));
        timer.setCycleCount(javafx.animation.Animation.INDEFINITE);timer.play();
        return result;
    }

    void dispose() {
        cancelCapture.run();
        pageLoadPending = false;
        viewport.close();
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
        boolean changed=synchronizeWalkingRoute();
        render(fit);
        if(automaticWalking&&changed)requestWalkingRouteWhenReady();
    }

    private void render(boolean fit) {
        if (capturing || !ready) return;
        try {
            if (resetPending) {
                view.getEngine().executeScript("if(window.resetPlannerMap)window.resetPlannerMap()");
                resetPending = false;
            }
            if (plan == null) return;
            setRangeAdjusting(rangeAdjusting);
            sendOriginLock();
            setCandidateActionsEnabled(candidateActionsEnabled);
            view.getEngine().executeScript("window.presentRoute(" + placePayload.plan(plan) + ","
                    + placePayload.place(candidate) + "," + radius + "," + locked + "," + fit + ","
                    + placePayload.places(candidates) + ")");
            view.getEngine().executeScript("if(window.focusRouteLeg)window.focusRouteLeg("
                    + editingLegIndex + "," + editingLegRadius + ")");
            renderWalkingRoute(false);
            if (pendingDetail != null) {
                view.getEngine().executeScript("window.openRoutePlace(" + pendingDetail.recordId() + ")");
                pendingDetail = null;
            }
        } catch (Exception failure) { error.accept("코스를 지도에 표시하지 못했어요."); }
    }

    void reset() {
        plan = null;
        candidate = null;
        candidates = List.of();
        pendingDetail = null;
        editingLegIndex = editingLegRadius = null;
        radius = 500;
        locked = false;
        originLocked = false;
        walkingRequested = false;
        walkingPending = false;
        synchronizeWalkingRoute();
        rangeAdjusting = false;
        resetPending = true;
        render(true);
    }

    void fit(boolean wholeRoute) {
        if (ready) view.getEngine().executeScript("window.fitRouteView(" + wholeRoute + ")");
    }

    void fitLeg(int index) {
        if (ready) view.getEngine().executeScript("window.fitRouteLeg("+index+")");
    }

    void setRangeAdjusting(boolean adjusting) {
        rangeAdjusting = adjusting;
        if (ready) view.getEngine().executeScript("if(window.setCameraRangeAdjusting)window.setCameraRangeAdjusting("+adjusting+")");
    }

    void ensureRangesVisible() { ensureRangesVisible(false); }
    void ensureRangesVisible(boolean adjusting) {
        if (ready) view.getEngine().executeScript("window.ensureRouteRangesVisible("+adjusting+")");
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
        if (!automaticWalking && candidate != null && plan.places().stream().noneMatch(place -> place.recordId() == candidate.recordId()))
            stops.add(new Stop(candidate.lat(), candidate.lng(), candidate.name() + " (후보)"));
        return List.copyOf(stops);
    }

    private boolean synchronizeWalkingRoute() {
        List<Stop> current = currentWalkingStops();
        if (current.equals(walkingStops) && routedMode==mode()) return false;
        routedMode=mode();
        walkingStops = current;
        walkingVersion++;
        walkingPending = false;
        if (walkingTask != null) walkingTask.cancel();
        walkingTask=null;
        walkingRoute = null;
        walkingMessage = walkingRequested ? "코스가 바뀌었어요. 이동 경로를 다시 확인해 주세요." : "";
        publishWalking();
        return true;
    }

    void requestWalkingRoute() {
        requestWalkingRoute(true);
    }

    void requestWalkingRouteWhenReady() {
        if (currentWalkingStops().size() < 2) return;
        if (!ready && !automaticWalking) { walkingPending = true;walkingMessage="지도 준비 후 "+mode().label()+" 시간을 조회해요.";publishWalking();return; }
        requestWalkingRoute(false);
    }

    private void requestWalkingRoute(boolean fitResult) {
        synchronizeWalkingRoute();
        walkingRequested = true;
        if (!ready && !automaticWalking) {
            error.accept("지도를 불러온 뒤 이동 경로를 조회해 주세요.");
            return;
        }
        walkingPending = false;
        if (walkingStops.size() < 2) {
            walkingMessage = "출발 위치를 선택하고 코스에 장소를 담거나 추천 후보를 선택해 주세요.";
            publishWalking();
            renderWalkingRoute(false);
            return;
        }
        if (walkingTask != null) walkingTask.cancel();
        long version = ++walkingVersion;
        List<Stop> stops = walkingStops;
        var requestedMode=mode();
        walkingRoute = null;
        walkingMessage = mode().label()+" 이동시간을 확인하는 중… (" + (stops.size() - 1) + "구간)";
        publishWalking();
        renderWalkingRoute(false);
        Task<TravelRoute> task = new Task<>() {
            @Override protected TravelRoute call() throws Exception { return routeRouter.route(stops,requestedMode); }
        };
        walkingTask = task;
        Runnable finishLoading = fitResult ? loadingUi.apply(mode().label()+" 경로를 조회하고 있어요") : () -> {};
        task.setOnCancelled(event -> finishLoading.run());
        task.setOnSucceeded(event -> {
            finishLoading.run();
            if (walkingVersion != version || walkingTask != task) return;
            walkingRoute = task.getValue();
            walkingTask=null;
            walkingMessage = "";
            publishWalking();
            renderWalkingRoute(fitResult);
        });
        task.setOnFailed(event -> {
            finishLoading.run();
            if (walkingVersion != version || walkingTask != task) return;
            com.datecourse.service.AppDiagnostics.failure(com.datecourse.service.AppDiagnostics.Operation.TRAVEL_ROUTE,
                    com.datecourse.service.AppDiagnostics.Stage.PROVIDER_REQUEST,task.getException());
            walkingRoute = null;
            walkingTask=null;
            walkingMessage = task.getException() instanceof java.io.IOException
                    || task.getException() instanceof IllegalArgumentException ? task.getException().getMessage()
                    : mode().label()+" 경로를 불러오지 못했어요. 다시 조회해 주세요.";
            publishWalking();
            renderWalkingRoute(false);
            error.accept(walkingMessage);
        });
        Thread worker = new Thread(task, "planner-walking-route");
        worker.setDaemon(true);
        worker.start();
    }

    private void renderWalkingRoute(boolean fit) {
        if (capturing || !ready) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            view.getEngine().executeScript("if (typeof window.showWalkingRoute === 'function') window.showWalkingRoute("
                    + mapper.writeValueAsString(walkingRoute) + "," + mapper.writeValueAsString(walkingMessage) + "," + fit + ")");
        } catch (Exception failure) { error.accept("이동 경로를 지도에 표시하지 못했어요."); }
    }

    void setWalkingRouter(WalkingRouter router) { routeRouter = (stops,mode)->router.route(stops); }
    void setRouteRouter(RouteRouter router){routeRouter=router;}

    void refreshWalkingPreferences() {
        if (ready && currentWalkingStops().size() >= 2) requestWalkingRoute();
    }
}
