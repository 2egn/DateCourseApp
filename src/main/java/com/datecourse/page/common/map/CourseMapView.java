package com.datecourse.page.common.map;

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
public final class CourseMapView {
    private static final java.time.Duration CAPTURE_TIMEOUT = java.time.Duration.ofSeconds(20);
    private static final ObjectMapper JSON = new ObjectMapper();
    private final com.datecourse.service.PlaceMapPayload placePayload = new com.datecourse.service.PlaceMapPayload(
            new ObjectMapper(), new com.datecourse.service.PlacePresenter());

    @FunctionalInterface
    public interface WalkingRouter {
        TravelRoute route(List<Stop> stops) throws Exception;
    }
    @FunctionalInterface public interface RouteRouter {TravelRoute route(List<Stop> stops,com.datecourse.model.TravelMode mode)throws Exception;}
    private static final com.datecourse.service.TravelRouteService ROUTE_SERVICE=new com.datecourse.service.TravelRouteService();
    private RouteRouter routeRouter=ROUTE_SERVICE::route;
    private com.datecourse.model.TravelMode routedMode=com.datecourse.model.TravelMode.WALKING;
    /** 현재 코스의 이동수단을 반환하며 코스가 없으면 도보를 사용한다. */
    private com.datecourse.model.TravelMode mode(){return plan==null?com.datecourse.model.TravelMode.WALKING:plan.travelMode();}
    private List<Stop> walkingStops = List.of();
    private TravelRoute walkingRoute;
    private Task<TravelRoute> walkingTask;
    private java.util.function.Function<String, Runnable> loadingUi = text -> () -> {};
    /** 경로 조회 중 표시할 로딩 처리기를 등록한다. */
    public void setLoadingUi(java.util.function.Function<String, Runnable> ui) { loadingUi = ui; }
    private long walkingVersion;
    private boolean walkingRequested;
    private boolean automaticWalking;
    private BiConsumer<TravelRoute,String> walkingResult = (route,message) -> {};
    private Consumer<Place> reportPlace = place -> {};
    private Consumer<Long> addCandidate = id -> {};
    private boolean candidateActionsEnabled=true;
    private boolean coursePreviewMode;
    /** 저장 코스용 직선 연결 미리보기 모드를 설정한다. */
    public void setCoursePreviewMode(boolean enabled) { coursePreviewMode = enabled; }

    /** 지도에서 후보를 추가할 때 실행할 동작을 등록한다. */
    public void setOnAddCandidate(Consumer<Long> listener) { addCandidate=listener; }

    /** 후보 추가 가능 여부를 지도 화면에 반영한다. */
    public void setCandidateActionsEnabled(boolean enabled) {
        candidateActionsEnabled=enabled;
        if(ready)view.getEngine().executeScript("if(window.setCandidateActionsEnabled)window.setCandidateActionsEnabled("+enabled+")");
    }

    /** 지도에서 장소 정정을 제보할 때 실행할 동작을 등록한다. */
    public void setOnReport(Consumer<Place> listener) { reportPlace=listener; }

    /** 코스 변경에 따른 이동 경로 자동 조회 여부를 설정한다. */
    public void setAutomaticWalking(boolean enabled) { automaticWalking=enabled; }

    /** 경로 결과와 상태 메시지를 받을 처리기를 등록한다. */
    public void setWalkingResultListener(BiConsumer<TravelRoute,String> listener) { walkingResult=listener; }

    /** 현재 경로 결과와 안내 메시지를 연결된 화면에 전달한다. */
    private void publishWalking() { walkingResult.accept(walkingRoute,walkingMessage); }
    private String walkingMessage = "";
    @FunctionalInterface
    public interface PageSource { URI page() throws IOException; }
    private final PageSource pageSource;
    private URI page;
    private final WebView view;
    private final MapViewportLifecycle viewport;
    private boolean pageLoadPending;
    private final Consumer<String> error;
    private Consumer<String> routeError;
    /** 이동 경로 조회 오류를 받을 처리기를 등록한다. */
    public void setOnRouteError(Consumer<String> listener) { routeError = listener; }
    private final MapEventBridge bridge;
    private boolean ready;
    private String uiStatus;
    private Runnable onReady = () -> {};
    private boolean capturing;
    private long routeImageVersion;
    private java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage> pendingCapture;
    private Runnable cancelCapture=()->{};
    private boolean loaded;
    private RoutePlan plan;
    private boolean fitWholeCourseOnRender;
    private Place pendingFocus;
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

    /** 검색 범위를 조정 중인 구간과 반경을 기록한다. */
    public void setEditingLeg(Integer index, Integer radius) {
        editingLegIndex = index;
        editingLegRadius = radius;
    }
    private Runnable originChangeBlocked = () -> {};

    /** 공용 지도 페이지를 사용하는 지도 제어기를 만든다. */
    public CourseMapView(WebView view, BiConsumer<Double, Double> selected, Consumer<String> error) {
        this(view, selected, error, MapPageServer::sharedPage);
    }

    /** 지도 페이지 공급자와 선택·오류 콜백을 연결한다. */
    public CourseMapView(WebView view, BiConsumer<Double, Double> selected, Consumer<String> error, PageSource pageSource) {
        this.view = view;
        viewport = new MapViewportLifecycle(view, () -> {
            if(pageLoadPending){pageLoadPending=false;view.getEngine().load(page.toString());}
        });
        this.error = error;
        this.routeError = error;
        this.pageSource = pageSource;
        bridge = new MapEventBridge(() -> {
            ready = true;
            render(true);
            applyUiStatus();
            onReady.run();
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
        bridge.setOnRouteImageRequested(this::captureRouteImage);
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

    /** 현재 코스의 지도 이미지를 만들고 유효한 요청 결과만 표시한다. */
    private void captureRouteImage() {
        if (!ready || !view.isVisible() || view.getWidth() <= 0 || view.getHeight() <= 0
                || plan == null || plan.places().isEmpty()) {
            error.accept("지도가 준비된 뒤 경로 이미지를 볼 수 있어요.");
            return;
        }
        RoutePlan snapshot = plan;
        long version = ++routeImageVersion;
        int requestVersion = ((Number) view.getEngine().executeScript("window.routeImageRequestVersion || 0")).intValue();
        int targetWidth = Math.max(1, ((Number) view.getEngine().executeScript(
                "document.getElementById('map-surface').clientWidth")).intValue());
        int targetHeight = Math.max(1, ((Number) view.getEngine().executeScript(
                "document.getElementById('map-surface').clientHeight")).intValue());
        capturePreview(snapshot, targetWidth, targetHeight).whenComplete((mapImage, captureFailure) -> {
            if (captureFailure != null) {
                Platform.runLater(() -> {
                    if (!isCurrentRouteImage(version, snapshot)) return;
                    if (ready) view.getEngine().executeScript("if(window.routeImageFailed)window.routeImageFailed()");
                    error.accept("경로 이미지를 만들지 못했어요.");
                });
                return;
            }
            java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try { return new com.datecourse.service.CoursePreviewImage().renderMapOnly(mapImage); }
                catch (IOException failure) { throw new java.util.concurrent.CompletionException(failure); }
            }).whenComplete((png, renderFailure) -> Platform.runLater(() -> {
                if (!isCurrentRouteImage(version, snapshot)) return;
                if (renderFailure != null) {
                    if (ready) view.getEngine().executeScript("if(window.routeImageFailed)window.routeImageFailed()");
                    error.accept("경로 이미지를 만들지 못했어요.");
                    return;
                }
                String data = java.util.Base64.getEncoder().encodeToString(png);
                view.getEngine().executeScript("if(window.showRouteImage)window.showRouteImage('data:image/png;base64," + data + "'," + requestVersion + ")");
            }));
        });
    }

    /** 이미지 요청과 코스가 현재 화면의 상태와 일치하는지 확인한다. */
    private boolean isCurrentRouteImage(long version, RoutePlan snapshot) {
        return ready && version == routeImageVersion && java.util.Objects.equals(plan, snapshot);
    }

    /** 지도 페이지를 준비하고 표시 가능한 시점에 불러온다. */
    public void load() {
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

    /** 진행 중인 이미지 응답을 무효화하고 지도를 다시 불러온다. */
    public void reload() {
        routeImageVersion++;
        pageLoadPending = false;
        ready = false;
        loaded = false;
        view.getEngine().getLoadWorker().cancel();
        view.getEngine().load(null);
        load();
    }

    /** 지도가 표시 중이며 새 이미지 캡처를 시작할 수 있는지 확인한다. */
    public boolean canCapturePreview() {
        return ready && !capturing && view.isVisible() && view.getWidth()>0 && view.getHeight()>0;
    }

    /** 지도와 Java 브리지가 준비되었는지 반환한다. */
    public boolean isReady() { return ready; }

    /** 지도 준비가 끝났을 때 실행할 동작을 등록한다. */
    public void setOnReady(Runnable listener) { onReady = listener == null ? () -> {} : listener; }

    /** 지도 캡처 준비와 촬영 진행 단계를 구분한다. */
    public enum CaptureStage { PREPARING, CAPTURING }

    /** 기본 미리보기 크기로 코스를 캡처한다. */
    public java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage> capturePreview(RoutePlan snapshot) {
        return capturePreview(snapshot,stage->{});
    }

    /** 기본 크기로 캡처하며 준비·촬영 진행 단계를 알린다. */
    public java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage> capturePreview(RoutePlan snapshot, Consumer<CaptureStage> progress) {
        return capturePreview(snapshot, com.datecourse.service.CoursePreviewImage.MAP_WIDTH,
                com.datecourse.service.CoursePreviewImage.MAP_HEIGHT, progress);
    }

    /** 지정한 이미지 크기로 코스를 캡처한다. */
    public java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage> capturePreview(RoutePlan snapshot, int targetWidth, int targetHeight) {
        return capturePreview(snapshot, targetWidth, targetHeight, stage->{});
    }

    /** FX 스레드에서 지도를 캡처하고 원래 크기와 표시 상태를 복원한다. */
    private java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage> capturePreview(
            RoutePlan snapshot, int targetWidth, int targetHeight, Consumer<CaptureStage> progress) {
        progress.accept(CaptureStage.PREPARING);
        var result = new java.util.concurrent.CompletableFuture<java.awt.image.BufferedImage>();
        if (targetWidth <= 0 || targetHeight <= 0 || capturing || !loaded && !ready || ready && !Boolean.TRUE.equals(view.getEngine().executeScript(
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
            catch(Exception ignored) { /* 화면 전환으로 이미 해제된 페이지일 수 있다. */ }
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
                    // 첫 저장은 지도·브리지 준비나 최초 화면 배치보다 먼저 요청될 수 있다.
                    if(!ready || !view.isVisible() || view.getWidth()<=0 || view.getHeight()<=0)return;
                    // 지도 크기를 바꾸기 전에 SDK 카메라 이동이 끝나기를 기다린다.
                    if(!Boolean.TRUE.equals(view.getEngine().executeScript(
                            "window.inspectMapCamera && !window.inspectMapCamera().moving")))return;
                    progress.accept(CaptureStage.CAPTURING);
                    originalSize[0]=view.getWidth();originalSize[1]=view.getHeight();
                    view.setManaged(false);view.resize(targetWidth,targetHeight);
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

    /** 이미지·경로 요청과 지도 수명주기를 정리한다. */
    public void dispose() {
        routeImageVersion++;
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

    /** 단일 후보를 포함한 코스 표시 요청을 전달한다. */
    public void present(RoutePlan plan, Place candidate, int radius, boolean locked, boolean fit) {
        presentCandidates(plan, candidate == null ? List.of() : List.of(candidate), candidate, radius, locked, fit);
    }

    /** 다음 표시 시 코스 전체가 보이도록 맞춘다. */
    public void presentCourse(RoutePlan plan, boolean locked) {
        fitWholeCourseOnRender = true;
        present(plan, null, plan.radius(), locked, true);
    }

    /** 코스와 후보 상태를 갱신하고 지도 표시를 요청한다. */
    public void presentCandidates(RoutePlan plan, List<Place> candidates, Place candidate, int radius, boolean locked, boolean fit) {
        this.plan = plan;
        this.candidates = List.copyOf(candidates);
        this.candidate = candidate;
        this.radius = radius;
        this.locked = locked;
        boolean changed=synchronizeWalkingRoute();
        render(fit);
        if(automaticWalking&&changed)requestWalkingRouteWhenReady();
    }

    /** 준비된 지도에 코스·후보·검색 범위와 이동 경로를 반영한다. */
    private void render(boolean fit) {
        if (capturing || !ready) return;
        try {
            if (resetPending) {
                view.getEngine().executeScript("if(window.resetPlannerMap)window.resetPlannerMap()");
                resetPending = false;
            }
            view.getEngine().executeScript("if(window.setRouteViewVisible)window.setRouteViewVisible(" +
                    (plan != null && !plan.places().isEmpty()) + ")");
            if (plan == null) return;
            setRangeAdjusting(rangeAdjusting);
            sendOriginLock();
            setCandidateActionsEnabled(candidateActionsEnabled);
            view.getEngine().executeScript("if(window.setCoursePreviewMode)window.setCoursePreviewMode(" + coursePreviewMode + ")");
            boolean fitWholeCourse = fitWholeCourseOnRender;
            view.getEngine().executeScript("window.presentRoute(" + placePayload.plan(plan) + ","
                    + placePayload.place(candidate) + "," + radius + "," + locked + "," + fit + ","
                    + placePayload.places(candidates) + "," + fitWholeCourse + ")");
            fitWholeCourseOnRender = false;
            view.getEngine().executeScript("if(window.focusRouteLeg)window.focusRouteLeg("
                    + editingLegIndex + "," + editingLegRadius + ")");
            renderWalkingRoute(false);
            if (pendingFocus != null) {
                view.getEngine().executeScript("window.focusCoursePlace(" + pendingFocus.recordId() + ","
                        + pendingFocus.lat() + "," + pendingFocus.lng() + ")");
                pendingFocus = null;
            }
            if (pendingDetail != null) {
                view.getEngine().executeScript("window.openRoutePlace(" + pendingDetail.recordId() + ")");
                pendingDetail = null;
            }
        } catch (Exception failure) { error.accept("코스를 지도에 표시하지 못했어요."); }
    }

    /** 선택한 위치와 코스·경로 상태를 초기화한다. */
    public void reset() {
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

    /** 검색 범위 또는 이동 경로가 보이도록 지도를 맞춘다. */
    public void fit(boolean wholeRoute) {
        if (ready) view.getEngine().executeScript("window.fitRouteView(" + wholeRoute + ")");
    }

    /** 출발지와 선택한 장소 전체가 보이도록 지도를 맞춘다. */
    public void fitCourse() {
        if (ready && view.isVisible() && view.getWidth() > 0 && view.getHeight() > 0
                && plan != null && !plan.places().isEmpty())
            view.getEngine().executeScript("if(window.fitConfirmedCourseView)window.fitConfirmedCourseView()");
    }

    /** 지도가 준비되면 지정한 장소로 화면을 이동한다. */
    public void focusPlace(Place place) {
        if (place == null) return;
        pendingFocus = place;
        if (ready) {
            view.getEngine().executeScript("window.focusCoursePlace(" + place.recordId() + ","
                    + place.lat() + "," + place.lng() + ")");
            pendingFocus = null;
        }
    }

    /** 선택한 구간의 이전 장소와 도착 장소가 보이도록 맞춘다. */
    public void fitLeg(int index) {
        if (ready) view.getEngine().executeScript("window.fitRouteLeg("+index+")");
    }

    /** 반경 조절 중인지 지도 카메라에 전달한다. */
    public void setRangeAdjusting(boolean adjusting) {
        rangeAdjusting = adjusting;
        if (ready) view.getEngine().executeScript("if(window.setCameraRangeAdjusting)window.setCameraRangeAdjusting("+adjusting+")");
    }

    /** 현재 조정 중인 검색 범위가 보이도록 지도를 맞춘다. */
    public void ensureRangesVisible() { ensureRangesVisible(false); }

    /** 현재 조정 중인 검색 범위가 보이도록 지도를 맞춘다. */
    public void ensureRangesVisible(boolean adjusting) {
        if (ready) view.getEngine().executeScript("window.ensureRouteRangesVisible("+adjusting+")");
    }

    /** 지도가 준비되면 지정한 장소의 상세 카드를 연다. */
    public void showDetail(Place place) {
        view.requestFocus();
        pendingDetail = place;
        if (ready) render(false);
    }

    /** 지도에서 장소를 선택할 때 실행할 동작을 등록한다. */
    public void setOnPlaceSelected(Consumer<Long> listener) { placeSelected = listener; }

    /** 출발지 변경이 제한되었을 때의 안내 동작을 등록한다. */
    public void setOnOriginChangeBlocked(Runnable listener) { originChangeBlocked = listener; }

    /** 출발지 변경 제한 상태를 기록하고 지도에 전달한다. */
    public void setOriginLocked(boolean value) { originLocked = value; if (ready) sendOriginLock(); }

    /** 화면에서 지정한 지도 안내 문구를 적용한다. */
    public void setStatus(String message) { uiStatus = message; applyUiStatus(); }

    /** 준비된 지도에 현재 안내 문구를 전달한다. */
    private void applyUiStatus() {
        if (ready && uiStatus != null) view.getEngine().executeScript("if(window.setPlannerStatus)window.setPlannerStatus(" +
                JSON.valueToTree(uiStatus).toString() + ")");
    }

    /** 지도에 출발지 변경 제한 여부를 전달한다. */
    private void sendOriginLock() {
        view.getEngine().executeScript("if (typeof window.setOriginLocked === 'function') window.setOriginLocked(" + originLocked + ")");
    }

    /** 선택 구간의 이동 안내 표시를 전환한다. */
    public void toggleWalkingExplanation() {
        if (ready) view.getEngine().executeScript("if (typeof window.toggleWalkingExplanation === 'function') window.toggleWalkingExplanation()");
    }

    /** 출발지와 코스·후보에서 경로 조회에 사용할 방문 순서를 만든다. */
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

    /** 방문 순서나 이동수단이 바뀌면 이전 경로 요청과 결과를 무효화한다. */
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

    /** 조회 조건을 확인하고 새 이동 경로 요청을 시작한다. */
    public void requestWalkingRoute() {
        requestWalkingRoute(true);
    }

    /** 조회 가능한 방문 순서가 있으면 즉시 조회하거나 지도 준비를 기다린다. */
    public void requestWalkingRouteWhenReady() {
        if (currentWalkingStops().size() < 2) return;
        if (!ready && !automaticWalking) { walkingPending = true;walkingMessage="지도 준비 후 "+mode().label()+" 시간을 조회해요.";publishWalking();return; }
        requestWalkingRoute(false);
    }

    /** 조회 조건을 확인하고 새 이동 경로 요청을 시작한다. */
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
            applyRouteSuccess(task, version, fitResult);
        });
        task.setOnFailed(event -> {
            finishLoading.run();
            applyRouteFailure(task, version);
        });
        Thread worker = new Thread(task, "planner-walking-route");
        worker.setDaemon(true);
        worker.start();
    }

    /** 최신 경로 요청의 성공 결과만 화면에 반영한다. */
    private void applyRouteSuccess(Task<TravelRoute> task, long version, boolean fitResult) {
        if (walkingVersion != version || walkingTask != task) return;
        walkingRoute = task.getValue();
        walkingTask=null;
        walkingMessage = "";
        publishWalking();
        renderWalkingRoute(fitResult);
    }

    /** 최신 경로 요청의 실패를 기록하고 재시도 안내를 표시한다. */
    private void applyRouteFailure(Task<TravelRoute> task, long version) {
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
        routeError.accept(walkingMessage);
    }

    /** 현재 이동 경로와 안내 메시지를 지도에 표시한다. */
    private void renderWalkingRoute(boolean fit) {
        if (capturing || !ready) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            view.getEngine().executeScript("if (typeof window.showWalkingRoute === 'function') window.showWalkingRoute("
                    + mapper.writeValueAsString(walkingRoute) + "," + mapper.writeValueAsString(walkingMessage) + "," + fit + ")");
        } catch (Exception failure) { error.accept("이동 경로를 지도에 표시하지 못했어요."); }
    }

    /** 이동수단 구분 없이 사용할 경로 계산기를 주입한다. */
    public void setWalkingRouter(WalkingRouter router) { routeRouter = (stops,mode)->router.route(stops); }

    /** 이동수단별 경로 계산기를 주입한다. */
    public void setRouteRouter(RouteRouter router){routeRouter=router;}

    /** 경로 설정이 바뀌면 준비된 지도에서 다시 조회한다. */
    public void refreshWalkingPreferences() {
        if (ready && currentWalkingStops().size() >= 2) requestWalkingRoute();
    }
}
