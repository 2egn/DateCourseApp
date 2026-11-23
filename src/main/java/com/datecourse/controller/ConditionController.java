package com.datecourse.controller;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import com.datecourse.model.SavedCourse;
import kr.ac.university.localdb.db.SavedCourseRepository;
import javafx.fxml.FXMLLoader;
import com.datecourse.service.PlaceSource;
import com.datecourse.service.PilotPlaceRepository;
import kr.ac.university.localdb.UserSession;
import kr.ac.university.localdb.db.PlaceRepository;
import javafx.animation.*;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.util.List;

/**
 * 공통 조건과 확정본을 소유한다. 편집 화면은 별도의 복사본만 변경한다.
 */
public class ConditionController extends BasePageController {
    private final com.datecourse.service.PlacePresenter placePresenter = new com.datecourse.service.PlacePresenter();

    @FXML
    private StackPane plannerRoot;
    @FXML
    private BorderPane mainPage;
    @FXML
    private Parent routeEditor;
    @FXML
    private RouteEditorController routeEditorController;
    @FXML
    private TextField peopleField;
    @FXML
    private ComboBox<com.datecourse.model.TravelMode> travelModeBox;
    private boolean restoringTravelMode;

    private com.datecourse.model.TravelMode travelMode() {
        return travelModeBox.getValue() == null ? com.datecourse.model.TravelMode.WALKING : travelModeBox.getValue();
    }

    @FXML
    private Button peopleUp, peopleDown, recommendButton;
    @FXML
    private Label errorLabel, resultLabel, radiusLabel;
    @FXML
    private Label walkingSummaryLabel, completedStopsLabel;
    @FXML
    private VBox courseOverview;
    @FXML
    private TitledPane commonConditionsPane;
    @FXML
    private ScrollPane mainSidebar;
    @FXML
    private Button reloadMapButton;
    private com.datecourse.model.TravelRoute overviewWalking;
    @FXML
    private Slider radiusSlider;
    @FXML
    private Label radiusMaximumLabel;
    @FXML
    private ComboBox<String> sourceBox;
    @FXML
    private Label sourceStatusLabel;
    @FXML
    private WebView mapView;
    private PlannerMap map;
    private PlaceRepository placeRepository;
    private List<Place> allPlaces = List.of();
    private boolean placesLoaded, placesLoading;
    private Double selectedLatitude, selectedLongitude;
    private RoutePlan appliedPlan;
    private Timeline screenAnimation;
    private boolean fitOnReturn;
    private UserSession userSession;
    private com.datecourse.service.UserContext userContext;
    private static final String EXISTING = "운영 장소 목록", PILOT = "서면·전포 시연 목록";
    private static final String PUBLIC_DEMO = "서면 공공자료 · 추정 가격";
    private kr.ac.university.localdb.db.InformationReportRepository informationReports;
    private Task<List<Place>> placeLoadTask;
    private long placeLoadVersion;
    private boolean restoringSource;
    private SavedCourseRepository savedCourseRepository;
    private Long savedCourseId;
    private RoutePlan lastSavedPlan;
    private boolean saving;
    @FXML
    private Button saveCourseButton, savedCoursesButton, newCourseButton;
    @FXML
    private Label saveStatusLabel;
    @FXML
    private MenuButton userLabel;
    @FXML
    private HBox headerBar;
    @FXML
    private HBox walkingToolbar;
    @FXML
    private StackPane savedPageHost;
    @FXML
    private StackPane myPageHost;
    private MyPageController myPageController;
    private kr.ac.university.localdb.db.MemberRepository accountRepository;
    private Runnable logoutHandler;
    @FXML
    private Button homeButton;
    private boolean editorPaused;
    private boolean editorOpen;
    private boolean commonSettingsChanged;
    private Parent savedPage;
    private SavedCourseController savedController;
    private PageFeedback feedback;
    private Runnable startupReady;
    private java.util.function.Function<RoutePlan, java.util.Optional<String>> titlePrompt = this::askCourseTitle;

    @FXML
    public void initialize() {
        feedback = new PageFeedback(plannerRoot);
        useFeedback(feedback);
        routeEditorController.useFeedback(feedback);
        saveCourseButton.managedProperty().bind(saveCourseButton.visibleProperty());
        newCourseButton.visibleProperty().bind(newCourseButton.disableProperty().not());
        newCourseButton.managedProperty().bind(newCourseButton.visibleProperty());
        walkingToolbar.managedProperty().bind(walkingToolbar.visibleProperty());
        watchNotice(errorLabel, PageFeedback.Kind.ERROR);
        watchNotice(saveStatusLabel, PageFeedback.Kind.INFO);
        mainSidebar.prefWidthProperty().bind(mainPage.widthProperty().multiply(.45));
        reloadMapButton.managedProperty().bind(reloadMapButton.visibleProperty());
        reloadMapButton.setVisible(false);
        errorLabel.textProperty().addListener((o, a, b) -> reloadMapButton.setVisible(b != null && (b.contains("지도") || b.contains("8080"))));
        NumberStepper.install(peopleField, peopleUp, peopleDown, 1, 20, 2);
        travelModeBox.getItems().setAll(com.datecourse.model.TravelMode.values());
        travelModeBox.setValue(com.datecourse.model.TravelMode.WALKING);
        travelModeBox.valueProperty().addListener((o, a, b) -> {
            if (restoringTravelMode || b == null) return;
            configureRadiusMaximum(b.maximumRadius());
            commonSettingsChanged = true;
            if (appliedPlan != null) {
                appliedPlan = appliedPlan.withTravelMode(b);
                overviewWalking = null;
            }
            updateRange();
            updateCourseOverview();
            updateResetButton();
            if (appliedPlan != null && !appliedPlan.places().isEmpty()) map.requestWalkingRouteWhenReady();
        });
        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.managedProperty());
        map = new PlannerMap(mapView, this::onLocationSelected, errorLabel::setText);
        map.setOnReport(this::reportInformation);
        map.setWalkingResultListener((route, message) -> {
            overviewWalking = route;
            updateCourseOverview();
            boolean hasPlaces = appliedPlan != null && !appliedPlan.places().isEmpty();
            walkingSummaryLabel.setText(!hasPlaces ? "" : route == null ? travelMode().label() + " 이동시간 미확인"
                    : "누적 " + route.mode().label() + " " + com.datecourse.model.TravelRoute.durationText(route.totalTime()) + " · " + route.mode().timeNote() + (route.provider().isBlank() ? "" : " · 경로 " + route.provider()));
        });
        map.setLoadingUi(this::beginLoading);
        map.setOnOriginChangeBlocked(this::noticeOriginLocked);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(plannerRoot.widthProperty());
        clip.heightProperty().bind(plannerRoot.heightProperty());
        plannerRoot.setClip(clip);
        radiusSlider.valueProperty().addListener((obs, old, value) -> {
            updateRange();
            commonSettingsChanged = true;
            updateResetButton();
        });
        radiusSlider.valueChangingProperty().addListener((obs, old, adjusting) -> {
            if (!adjusting) updateRange();
        });
        peopleField.textProperty().addListener((o, old, value) -> {
            commonSettingsChanged = true;
            updateResetButton();
        });
        map.load();
        sourceBox.getItems().setAll(EXISTING, PILOT, PUBLIC_DEMO);
        sourceBox.setValue(EXISTING);
        sourceBox.valueProperty().addListener((obs, old, selected) -> changeSource(old, selected));
        selectNavigation(homeButton);
        updateResetButton();
    }

    public void usePilotPlaces() {
        sourceBox.setValue(PILOT);
    }

    private void changeSource(String old, String selected) {
        if (restoringSource) return;
        if (appliedPlan != null && !appliedPlan.places().isEmpty() || editorPaused) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "장소 목록을 바꾸면 현재 코스를 비워요. 계속할까요?", ButtonType.OK, ButtonType.CANCEL);
            confirm.initOwner(plannerRoot.getScene().getWindow());
            com.datecourse.ui.DialogTheme.apply(confirm);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
                restoringSource = true;
                sourceBox.setValue(old);
                restoringSource = false;
                return;
            }
        }
        appliedPlan = null;
        editorPaused = false;
        updateOriginProtection();
        savedCourseId = null;
        lastSavedPlan = null;
        saveStatusLabel.setText("");
        resultLabel.setText("아직 확정된 코스가 없어요.");
        if (PILOT.equals(selected) || PUBLIC_DEMO.equals(selected)) {
            radiusSlider.setValue(2000);
            onLocationSelected(PilotPlaceRepository.START_LAT, PilotPlaceRepository.START_LNG);
        }
        if (selectedLatitude != null) map.present(new RoutePlan(selectedLatitude, selectedLongitude, 1,
                radius(), "", List.of()), null, radius(), false, true);
        loadPlaces();
        commonSettingsChanged = false;
        updateResetButton();
    }

    public void setUserSession(UserSession session) {
        if (!java.util.Objects.equals(userSession, session)) clearUserContext();
        userSession = session;
        userLabel.setText(session == null ? "시연 모드" : session.name() + "님");
        userLabel.setDisable(session == null);
    }

    public void setAccountActions(kr.ac.university.localdb.db.MemberRepository repository, Runnable logout) {
        if (accountRepository != repository) clearUserContext();
        accountRepository = repository;
        logoutHandler = logout;
        informationReports = repository == null ? null : repository.informationReports();
        routeEditorController.setPlaceRefresh(informationReports == null ? null : informationReports::apply);
        routeEditorController.setOnReport(this::reportInformation);
    }

    private void reportInformation(Place place) {
        if (userSession == null || informationReports == null) {
            notifyUser(PageFeedback.Kind.INFO, "로그인 후 제보할 수 있어요.");
            return;
        }
        InformationReportDialog.show(plannerRoot.getScene().getWindow(), place).ifPresent(form -> {
            var session = userSession;
            runWork("제보를 접수하고 있어요", () -> informationReports.submit(session, place, form.field(), form.value(), form.reason(), form.source()),
                    id -> notifyUser(PageFeedback.Kind.SUCCESS, "제보 #" + id + " 접수 · 관리자 검토 후 반영돼요."),
                    failure -> notifyUser(PageFeedback.Kind.ERROR, failure instanceof IllegalArgumentException ? failure.getMessage() : "제보를 접수하지 못했어요. 다시 시도해 주세요."));
        });
    }

    @FXML
    private void onMyPage() {
        if (saving || feedback.isBusy() || userSession == null || accountRepository == null) return;
        try {
            pauseForNavigation();
            mainPage.setVisible(false);
            savedPageHost.setVisible(false);
            savedPageHost.setManaged(false);
            if (myPageController == null) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/my-page-view.fxml"));
                Parent page = loader.load();
                myPageController = loader.getController();
                myPageController.useFeedback(feedback);
                myPageHost.getChildren().setAll(page);
            }
            myPageHost.setVisible(true);
            myPageHost.setManaged(true);
            selectNavigation(null);
            myPageController.configure(ensureUserContext(), this::onSavedCourses, this::onHome);
        } catch (java.io.IOException failure) {
            onHome();
            notifyUser(PageFeedback.Kind.ERROR, "내 페이지를 열지 못했어요.");
        }
    }

    private java.util.function.BooleanSupplier closeConfirmation = this::confirmUnsavedClose;

    /**
     * Called on the JavaFX thread by the native Swing close handler.
     */
    public boolean confirmWindowClose() {
        if (saving || isLoading() || feedback != null && feedback.isBusy()) {
            notifyUser(PageFeedback.Kind.INFO, "진행 중인 작업이 끝난 뒤 종료해 주세요.");
            return false;
        }
        return !hasUnsavedChanges() || closeConfirmation.getAsBoolean();
    }

    private boolean confirmUnsavedClose() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "종료하면 저장하지 않은 코스와 편집 초안이 사라져요. 종료할까요?", ButtonType.OK, ButtonType.CANCEL);
        confirm.initOwner(plannerRoot.getScene().getWindow());
        com.datecourse.ui.DialogTheme.apply(confirm);
        return confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    @FXML
    private void onLogout() {
        if (saving || feedback.isBusy() || userSession == null || logoutHandler == null) return;
        if (hasUnsavedChanges()) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "로그아웃하면 저장하지 않은 코스와 편집 초안이 사라져요. 로그아웃할까요?", ButtonType.OK, ButtonType.CANCEL);
            confirm.initOwner(plannerRoot.getScene().getWindow());
            com.datecourse.ui.DialogTheme.apply(confirm);
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        }
        logoutNow();
    }

    private void logoutNow() {
        if (screenAnimation != null) screenAnimation.stop();
        if (placeLoadTask != null) placeLoadTask.cancel();
        placeLoadVersion++;
        map.dispose();
        routeEditorController.dispose();
        feedback.dispose();
        setUserSession(null);
        if (savedController != null) savedController.dispose();
        appliedPlan = null;
        lastSavedPlan = null;
        savedCourseId = null;
        editorPaused = false;
        editorOpen = false;
        allPlaces = List.of();
        accountRepository = null;
        savedCourseRepository = null;
        placeRepository = null;
        informationReports = null;
        routeEditorController.setPlaceRefresh(null);
        routeEditorController.setOnReport(p -> notifyUser(PageFeedback.Kind.INFO, "로그인 후 제보할 수 있어요."));
        Runnable logout = logoutHandler;
        logoutHandler = null;
        if (logout != null) logout.run();
    }

    public void setSavedCourseRepository(SavedCourseRepository repository) {
        if (savedCourseRepository != repository) clearUserContext();
        savedCourseRepository = repository;
    }

    private void clearUserContext() {
        if (userContext != null) userContext.close();
        userContext = null;
        if (savedController != null) savedController.clear();
        if (myPageController != null) myPageController.clear();
    }

    private com.datecourse.service.UserContext ensureUserContext() {
        if (userContext == null && userSession != null) {
            var session = userSession;
            var accounts = accountRepository;
            var courses = savedCourseRepository;
            userContext = new com.datecourse.service.UserContext(session,
                    accounts == null ? null : () -> accounts.profile(session),
                    courses == null ? null : () -> courses.findAll(session));
        }
        return userContext;
    }

    /**
     * Precreate the secondary form; wait only for the initial local/DB list, not external map services.
     */
    public void prepareStartup(Runnable ready) {
        var context = ensureUserContext();
        if (context != null) context.preload();
        routeEditorController.prepareMap();
        try {
            prepareSavedPage();
        } catch (java.io.IOException failure) {
            errorLabel.setText("저장 목록 화면을 준비하지 못했어요. 탭에서 다시 시도할 수 있어요.");
        }
        if (placesLoading) startupReady = ready;
        else ready.run();
    }

    private void finishStartup() {
        if (startupReady != null) {
            Runnable ready = startupReady;
            startupReady = null;
            ready.run();
        }
    }

    private java.util.Optional<String> askCourseTitle(RoutePlan plan) {
        TextInputDialog dialog = new TextInputDialog(plan.title());
        dialog.initOwner(plannerRoot.getScene().getWindow());
        com.datecourse.ui.DialogTheme.apply(dialog);
        dialog.setTitle(savedCourseId == null ? "새 코스 저장" : "코스 수정 저장");
        dialog.setHeaderText("저장할 코스 이름을 입력해 주세요. (1~20자)");
        dialog.setContentText("코스 이름");
        return dialog.showAndWait();
    }

    @FXML
    private void onSaveCourse() {
        if (saving || !hasStorage()) return;
        if (editorOpen || editorPaused) {
            notifyUser(PageFeedback.Kind.INFO, "편집 완료 후 저장해 주세요.");
            return;
        }
        if (appliedPlan == null || appliedPlan.places().isEmpty()) {
            saveStatusLabel.setText("장소를 고르고 편집 완료한 뒤 저장해 주세요.");
            return;
        }
        java.util.Optional<String> answer = titlePrompt.apply(appliedPlan);
        if (answer.isEmpty()) return;
        String title = answer.get().trim();
        if (title.isEmpty() || title.length() > 20) {
            saveStatusLabel.setText("코스 이름은 1~20자로 입력해 주세요.");
            return;
        }
        // Common conditions can change after returning from the editor. Save what the form displays.
        final RoutePlan snapshot;
        try {
            snapshot = new RoutePlan(selectedLatitude, selectedLongitude,
                    NumberStepper.read(peopleField, 1, 20, "인원"), radius(), title,
                    appliedPlan.places(), appliedPlan.arrivalRadii(), travelMode());
        } catch (IllegalArgumentException invalid) {
            saveStatusLabel.setText(invalid.getMessage());
            return;
        }
        Long id = savedCourseId;
        var context = ensureUserContext();
        saving = true;
        headerBar.setDisable(true);
        mainPage.setDisable(true);
        saveStatusLabel.setText("코스를 저장하고 있어요…");
        var diagnostic = com.datecourse.service.AppDiagnostics.begin(com.datecourse.service.AppDiagnostics.Operation.COURSE_SAVE);
        updateLoading(saveStatusLabel.getText());
        runWork("코스를 저장하고 있어요…", () -> {
            return savedCourseRepository.saveWithoutPreview(context.session(), id, snapshot);
        }, result -> {
            var saved = result;
            saving = false;
            headerBar.setDisable(false);
            mainPage.setDisable(false);
            savedCourseId = saved.courseId();
            lastSavedPlan = saved.plan();
            context.invalidateCourses();
            context.courses(); // Refresh once after a successful write, without blocking the form.
            presentPlan(saved.plan());
            saveStatusLabel.setText("코스를 저장했어요.");
            diagnostic.report(com.datecourse.service.AppDiagnostics.Stage.COMPLETE,
                    com.datecourse.service.AppDiagnostics.Outcome.SUCCESS, null);
            notifyUser(PageFeedback.Kind.SUCCESS, saveStatusLabel.getText());
        }, failure -> {
            saving = false;
            headerBar.setDisable(false);
            mainPage.setDisable(false);
            saveStatusLabel.setText(failure instanceof IllegalArgumentException ? failure.getMessage()
                    : "코스를 저장하지 못했어요. 잠시 후 다시 시도해 주세요. 편집 내용은 유지돼요.");
            notifyUser(PageFeedback.Kind.ERROR, saveStatusLabel.getText());
        });
    }

    private boolean hasStorage() {
        if (userSession != null && savedCourseRepository != null) return true;
        saveStatusLabel.setText("DB 저장·조회는 로그인 후 사용할 수 있어요.");
        return false;
    }

    @FXML
    private void onSavedCourses() {
        if (saving || !hasStorage()) return;
        try {
            pauseForNavigation();
            myPageHost.setVisible(false);
            myPageHost.setManaged(false);
            prepareSavedPage();
            mainPage.setVisible(false);
            saveCourseButton.setDisable(true);
            savedPageHost.setVisible(true);
            savedPageHost.setManaged(true);
            selectNavigation(savedCoursesButton);
            savedController.configure(savedCourseRepository, ensureUserContext(), saved -> {
                if (!confirmReplace()) return;
                restoreSavedCourse(saved);
                onHome();
            });
        } catch (java.io.IOException failure) {
            saveStatusLabel.setText("저장 목록 화면을 열지 못했어요.");
        }
    }

    private void prepareSavedPage() throws java.io.IOException {
        if (savedPage != null) return;
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/saved-course-view.fxml"));
        savedPage = loader.load();
        savedController = loader.getController();
        savedController.useFeedback(feedback);
        savedPageHost.getChildren().setAll(savedPage);
    }

    @FXML
    private void onHome() {
        if (saving) return;
        pauseForNavigation();
        myPageHost.setVisible(false);
        myPageHost.setManaged(false);
        savedPageHost.setVisible(false);
        savedPageHost.setManaged(false);
        mainPage.setVisible(true);
        saveCourseButton.setDisable(editorPaused);
        selectNavigation(homeButton);
    }

    private void selectNavigation(Button selected) {
        var active = javafx.css.PseudoClass.getPseudoClass("active");
        for (Button button : List.of(homeButton, savedCoursesButton))
            button.pseudoClassStateChanged(active, button == selected);
    }

    private void restoreSavedCourse(SavedCourse saved) {
        editorPaused = false;
        savedCourseId = saved.courseId();
        lastSavedPlan = saved.plan();
        // Match the candidate source without clearing the restored course.
        String source = saved.plan().places().stream().anyMatch(p -> p.placeId().startsWith("DCA-DEMO-PUBLIC-")) ? PUBLIC_DEMO
                : saved.plan().places().stream().anyMatch(p -> p.placeId().startsWith("DCA-DEMO-")) ? PILOT : EXISTING;
        if (!source.equals(sourceBox.getValue())) {
            restoringSource = true;
            sourceBox.setValue(source);
            restoringSource = false;
            loadPlaces();
        }
        presentPlan(saved.plan());
        map.requestWalkingRouteWhenReady();
        map.fit(false);
        saveStatusLabel.setText("코스를 불러왔어요.");
    }

    private boolean confirmReplace() {
        if (!hasUnsavedChanges()) return true;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "저장하지 않은 현재 코스를 바꿀까요?", ButtonType.OK, ButtonType.CANCEL);
        confirm.initOwner(plannerRoot.getScene().getWindow());
        com.datecourse.ui.DialogTheme.apply(confirm);
        return confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private boolean hasUnsavedChanges() {
        boolean draftChanged = (editorPaused || editorOpen)
                && !routeEditorController.draftSnapshot().equals(appliedPlan);
        return draftChanged || appliedPlan != null && !appliedPlan.places().isEmpty()
                && (!appliedPlan.equals(lastSavedPlan) || selectedLatitude != appliedPlan.startLat()
                || selectedLongitude != appliedPlan.startLng() || radius() != appliedPlan.radius()
                || !peopleField.getText().trim().equals(Integer.toString(appliedPlan.people())));
    }

    @FXML
    private void onNewCourse() {
        if (saving || !confirmReplace()) return;
        resetCourse();
    }

    private void resetCourse() {
        onHome();
        editorPaused = false;
        saveCourseButton.setDisable(false);
        savedCourseId = null;
        lastSavedPlan = null;
        appliedPlan = null;
        selectedLatitude = null;
        selectedLongitude = null;
        peopleField.setText("2");
        restoringTravelMode = true;
        travelModeBox.setValue(com.datecourse.model.TravelMode.WALKING);
        restoringTravelMode = false;
        configureRadiusMaximum(travelMode().maximumRadius());
        radiusSlider.setValue(500);
        map.reset();
        errorLabel.setText("");
        walkingSummaryLabel.setText("");
        map.setStatus("부산의 출발 장소를 검색하거나 지도를 클릭해 주세요.");
        resultLabel.setText("아직 확정된 코스가 없어요.");
        saveStatusLabel.setText("코스를 초기화했어요.");
        recommendButton.setText("코스 만들기 →");
        updateRange();
        commonSettingsChanged = false;
        updateResetButton();
    }

    @FXML
    private void onWalkingRoute() {
        map.requestWalkingRoute();
    }

    @FXML
    private void onWalkingExplanation() {
        map.toggleWalkingExplanation();
    }

    @FXML
    private void onTmapSettings() {
        if (TmapSettingsDialog.show(mapView.getScene().getWindow())) map.refreshWalkingPreferences();
    }

    public void setPlaceRepository(PlaceRepository repository) {
        placeRepository = repository;
        loadPlaces();
    }

    @FXML
    private void onReloadMap() {
        errorLabel.setText("");
        map.reload();
    }

    private void loadPlaces() {
        PlaceSource source = PUBLIC_DEMO.equals(sourceBox.getValue()) ? new com.datecourse.service.PublicDemoPlaceRepository()
                : PILOT.equals(sourceBox.getValue()) ? new PilotPlaceRepository() : placeRepository;
        if (placeLoadTask != null) placeLoadTask.cancel();
        long version = ++placeLoadVersion;
        placesLoaded = false;
        allPlaces = List.of();
        if (source == null) {
            placesLoading = false;
            recommendButton.setDisable(false);
            recommendButton.setText("코스 만들기 →");
            sourceStatusLabel.setText("운영 목록은 로그인 후 사용할 수 있어요.");
            finishStartup();
            return;
        }
        placesLoading = true;
        sourceStatusLabel.setText("장소 목록을 불러오는 중…");
        recommendButton.setDisable(true);
        recommendButton.setText("장소 불러오는 중…");
        Task<List<Place>> task = new Task<>() {
            @Override
            protected List<Place> call() throws Exception {
                List<Place> places = source.findAll();
                return informationReports == null ? places : informationReports.apply(places);
            }
        };
        Runnable finishLoading = beginLoading("장소 목록을 불러오고 있어요");
        task.setOnCancelled(event -> finishLoading.run());
        placeLoadTask = task;
        task.setOnSucceeded(event -> {
            finishLoading.run();
            if (version != placeLoadVersion) return;
            allPlaces = List.copyOf(task.getValue());
            placesLoaded = true;
            placesLoading = false;
            sourceStatusLabel.setText(allPlaces.size() + "곳의 장소를 불러왔어요.");
            sourceStatusLabel.setTooltip(new Tooltip(placePresenter.datasetSummary(allPlaces)));
            recommendButton.setDisable(false);
            recommendButton.setText(appliedPlan == null || appliedPlan.places().isEmpty() ? "코스 만들기 →" : "코스 편집 →");
            finishStartup();
        });
        task.setOnFailed(event -> {
            finishLoading.run();
            if (version != placeLoadVersion) return;
            placesLoading = false;
            sourceStatusLabel.setText("목록을 불러오지 못했어요.");
            recommendButton.setDisable(false);
            recommendButton.setText("장소 다시 불러오기");
            errorLabel.setText("장소를 불러오지 못했어요. 다시 시도해 주세요.");
            finishStartup();
        });
        Thread worker = new Thread(task, "planner-place-load");
        worker.setDaemon(true);
        worker.start();
    }

    private void onLocationSelected(double lat, double lng) {
        if (hasCoursePlaces()) {
            // The map bridge also reports its existing origin after a reload; that is not a user move.
            if (selectedLatitude == null || selectedLatitude != lat || selectedLongitude != lng) noticeOriginLocked();
            return;
        }
        selectedLatitude = lat;
        commonSettingsChanged = true;
        selectedLongitude = lng;
        map.setStatus("출발 위치를 선택했어요. 공통 조건을 정하고 코스를 만들어 주세요.");
        errorLabel.setText("");
        updateRange();
        updateResetButton();
    }

    private void configureRadiusMaximum(int maximum) {
        radiusSlider.setMax(maximum);
        radiusMaximumLabel.setText((maximum / 1000) + "km");
        radiusSlider.setTooltip(new javafx.scene.control.Tooltip(travelMode().label() + " · 출발지 중심 최대 " + (maximum / 1000) + "km의 직선 범위예요."));
    }

    private int radius() {
        return (int) Math.round(radiusSlider.getValue() / 100) * 100;
    }

    private boolean hasCoursePlaces() {
        return appliedPlan != null && !appliedPlan.places().isEmpty()
                || (editorPaused || editorOpen) && !routeEditorController.draftSnapshot().places().isEmpty();
    }

    private void noticeOriginLocked() {
        notifyUser(PageFeedback.Kind.INFO, "출발지를 변경하려면 현재 코스를 초기화해 주세요.");
    }

    private void updateOriginProtection() {
        map.setOriginLocked(hasCoursePlaces());
    }

    private void updateResetButton() {
        boolean hasPlaces = appliedPlan != null && !appliedPlan.places().isEmpty();
        saveCourseButton.setVisible(hasPlaces);
        walkingToolbar.setVisible(hasPlaces && travelMode() == com.datecourse.model.TravelMode.WALKING);
        courseOverview.setVisible(hasPlaces);
        courseOverview.setManaged(hasPlaces);
        if (!hasPlaces) commonConditionsPane.setExpanded(true);
        updateCourseOverview();
        recommendButton.getStyleClass().removeAll("planner-primary", "drawer-toggle");
        recommendButton.getStyleClass().add(hasPlaces ? "drawer-toggle" : "planner-primary");
        map.setStatus(hasPlaces
                ? editorPaused ? "편집 중인 초안이 있어요. 이어서 편집해 주세요."
                : "코스 순서와 이동 시간을 확인하고 저장해 주세요."
                : selectedLatitude == null ? "부산의 출발 장소를 검색하거나 지도를 클릭해 주세요."
                : hasCoursePlaces() ? "코스의 출발지는 고정돼 있어요. 장소별 조건은 코스 편집에서 바꿀 수 있어요."
                : "출발 위치를 선택했어요. 공통 조건을 정하고 코스를 만들어 주세요.");
        newCourseButton.setDisable(!commonSettingsChanged && appliedPlan == null && savedCourseId == null && !editorPaused && !editorOpen);
    }

    private void updateRange() {
        map.setRangeAdjusting(radiusSlider.isValueChanging());
        updateOriginProtection();
        radiusLabel.setText("출발지 중심 · " + radius() + "m");
        if (selectedLatitude != null) {
            RoutePlan preview = new RoutePlan(selectedLatitude, selectedLongitude, 1, radius(), "",
                    appliedPlan == null ? List.of() : appliedPlan.places(),
                    appliedPlan == null ? java.util.Map.of() : appliedPlan.arrivalRadii(), travelMode());
            map.present(preview, null, radius(), false, true);
        }
    }

    @FXML
    private void onRecommendClick() {
        if (!placesLoaded) {
            if (placeRepository == null && EXISTING.equals(sourceBox.getValue()))
                errorLabel.setText("로그인하거나 서면·전포 시연 목록을 선택해 주세요.");
            else loadPlaces();
            return;
        }
        try {
            int people = NumberStepper.read(peopleField, 1, 20, "인원");
            if (selectedLatitude == null) throw new IllegalArgumentException("부산의 출발 장소를 검색하거나 지도를 클릭해 주세요.");
            RoutePlan snapshot = new RoutePlan(selectedLatitude, selectedLongitude, people, radius(),
                    appliedPlan == null ? "" : appliedPlan.title(), appliedPlan == null ? List.of() : appliedPlan.places(),
                    appliedPlan == null ? java.util.Map.of() : appliedPlan.arrivalRadii(), travelMode());
            openEditor(snapshot);
        } catch (IllegalArgumentException invalid) {
            errorLabel.setText(invalid.getMessage());
        }
    }

    private void openEditor(RoutePlan snapshot) {
        errorLabel.setText("");
        if (editorPaused) routeEditorController.resume(snapshot, allPlaces);
        else routeEditorController.begin(snapshot, allPlaces, this::applyEdits, this::pauseEdits);
        editorPaused = false;
        showEditor(true);
    }

    private void applyEdits(RoutePlan edited) {
        editorPaused = false;
        fitOnReturn = true;
        presentPlan(edited);
        map.requestWalkingRouteWhenReady();
        saveStatusLabel.setText("편집 완료. 코스 저장을 눌러 보관하세요.");
        showEditor(false);
    }

    private void presentPlan(RoutePlan edited) {
        appliedPlan = edited;
        restoringTravelMode = true;
        travelModeBox.setValue(edited.travelMode());
        restoringTravelMode = false;
        commonConditionsPane.setExpanded(edited.places().isEmpty());
        updateCourseOverview();
        updateOriginProtection();
        selectedLatitude = edited.startLat();
        selectedLongitude = edited.startLng();
        configureRadiusMaximum(Math.max(edited.travelMode().maximumRadius(), edited.radius()));
        radiusSlider.setValue(edited.radius());
        peopleField.setText(Integer.toString(edited.people()));
        resultLabel.setText(String.format("%s · %d곳 · %d명 · %s",
                edited.title().isBlank() ? "내 코스" : edited.title(), edited.places().size(), edited.people(), edited.costEstimate().display()));
        recommendButton.setText(edited.places().isEmpty() ? "코스 만들기 →" : "코스 편집 →");
        map.present(edited, null, edited.radius(), false, true);
        updateResetButton();
    }

    private void updateCourseOverview() {
        if (appliedPlan == null) {
            completedStopsLabel.setText("");
            return;
        }
        try {
            int people = NumberStepper.read(peopleField, 1, 20, "인원");
            var preview = new RoutePlan(appliedPlan.startLat(), appliedPlan.startLng(), people, radius(), appliedPlan.title(), appliedPlan.places(), appliedPlan.arrivalRadii(), travelMode());
            resultLabel.setText(String.format("%s · %d곳 · %d명 · %s", preview.title().isBlank() ? "내 코스" : preview.title(),
                    preview.places().size(), people, preview.costEstimate().display()));
        } catch (IllegalArgumentException invalid) {
            resultLabel.setText("인원 수정 중 · 1~20명을 입력해 주세요.");
        }
        var lines = new java.util.ArrayList<String>();
        for (int i = 0; i < appliedPlan.places().size(); i++) {
            Place place = appliedPlan.places().get(i);
            String time = overviewWalking != null && overviewWalking.legs().size() == appliedPlan.places().size()
                    ? " · 이동 " + com.datecourse.model.TravelRoute.durationText(overviewWalking.legs().get(i).time()) : "";
            lines.add((i + 1) + ". " + place.name() + " · " + com.datecourse.service.PlaceSearchService.categoryName(place.category())
                    + "\n" + place.priceText() + time);
        }
        completedStopsLabel.setText(String.join("\n\n", lines));
    }

    private void pauseEdits() {
        editorPaused = true;
        updateOriginProtection();
        fitOnReturn = false;
        recommendButton.setText("이어서 편집 →");
        saveStatusLabel.setText("편집 초안을 유지했어요.");
        showEditor(false);
    }

    private void pauseForNavigation() {
        if (editorOpen) {
            editorPaused = true;
            recommendButton.setText("이어서 편집 →");
        }
        if (screenAnimation != null) screenAnimation.stop();
        editorOpen = false;
        updateResetButton();
        updateOriginProtection();
        routeEditor.setVisible(false);
        routeEditor.setManaged(false);
        routeEditor.setDisable(true);
        mainPage.setTranslateX(0);
        mainPage.setOpacity(1);
        routeEditor.setOpacity(1);
        mainPage.setDisable(false);
    }

    private void showEditor(boolean open) {
        editorOpen = open;
        updateResetButton();
        saveCourseButton.setDisable(open || editorPaused);
        selectNavigation(homeButton);
        if (screenAnimation != null) screenAnimation.stop();
        mainPage.setTranslateX(0);
        routeEditor.setTranslateX(0);
        if (open && !routeEditor.isVisible()) routeEditor.setOpacity(0);
        if (!open) mainPage.setOpacity(0);
        routeEditor.setVisible(true);
        routeEditor.setManaged(true);
        routeEditor.setDisable(!open);
        mainPage.setDisable(open);
        screenAnimation = new Timeline(new KeyFrame(Duration.millis(160),
                new KeyValue(mainPage.opacityProperty(), open ? 0 : 1, Interpolator.EASE_BOTH),
                new KeyValue(routeEditor.opacityProperty(), open ? 1 : 0, Interpolator.EASE_BOTH)));
        screenAnimation.setOnFinished(event -> {
            routeEditor.setVisible(open);
            routeEditor.setManaged(open);
            mainPage.setVisible(!open);
            mainPage.setOpacity(1);
            routeEditor.setOpacity(1);
            if (open) routeEditorController.fitRange();
            else {
                mainPage.setTranslateX(0);
                if (fitOnReturn) map.fit(false);
                fitOnReturn = false;
            }
        });
        mainPage.setVisible(true);
        screenAnimation.play();
    }
}
