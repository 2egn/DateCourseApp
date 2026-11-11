package com.datecourse.controller;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
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

/** 공통 조건과 확정본을 소유한다. 편집 화면은 별도의 복사본만 변경한다. */
public class ConditionController {
    @FXML private StackPane plannerRoot;
    @FXML private BorderPane mainPage;
    @FXML private Parent routeEditor;
    @FXML private RouteEditorController routeEditorController;
    @FXML private TextField peopleField;
    @FXML private Button peopleUp, peopleDown, recommendButton;
    @FXML private Label errorLabel, resultLabel, hintLabel, radiusLabel;
    @FXML private Slider radiusSlider;
    @FXML private WebView mapView;
    private PlannerMap map;
    private PlaceRepository placeRepository;
    private List<Place> allPlaces = List.of();
    private boolean placesLoaded, placesLoading;
    private Double selectedLatitude, selectedLongitude;
    private RoutePlan appliedPlan;
    private Timeline screenAnimation;
    private boolean fitOnReturn;
    private UserSession userSession;

    @FXML
    public void initialize() {
        NumberStepper.install(peopleField, peopleUp, peopleDown, 1, 20, 2);
        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.managedProperty());
        map = new PlannerMap(mapView, this::onLocationSelected, errorLabel::setText);
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(plannerRoot.widthProperty());
        clip.heightProperty().bind(plannerRoot.heightProperty());
        plannerRoot.setClip(clip);
        radiusSlider.valueProperty().addListener((obs, old, value) -> updateRange());
        map.load();
    }

    public void setUserSession(UserSession session) { userSession = session; }
    @FXML private void onWalkingRoute() { map.requestWalkingRoute(); }
    @FXML private void onTmapSettings() {
        if (TmapSettingsDialog.show(mapView.getScene().getWindow())) map.refreshWalkingPreferences();
    }
    public void setPlaceRepository(PlaceRepository repository) { placeRepository = repository; loadPlaces(); }

    @FXML private void onReloadMap() {
        errorLabel.setText("");
        map.reload();
    }

    private void loadPlaces() {
        if (placeRepository == null || placesLoading) return;
        placesLoading = true;
        recommendButton.setDisable(true);
        recommendButton.setText("장소 불러오는 중…");
        Task<List<Place>> task = new Task<>() {
            @Override protected List<Place> call() throws Exception { return placeRepository.findAll(); }
        };
        task.setOnSucceeded(event -> {
            allPlaces = List.copyOf(task.getValue());
            placesLoaded = true;
            placesLoading = false;
            recommendButton.setDisable(false);
            recommendButton.setText(appliedPlan == null || appliedPlan.places().isEmpty() ? "코스 만들기 →" : "코스 편집 →");
        });
        task.setOnFailed(event -> {
            placesLoading = false;
            recommendButton.setDisable(false);
            recommendButton.setText("장소 다시 불러오기");
            errorLabel.setText("장소를 불러오지 못했어요. 다시 시도해 주세요.");
        });
        Thread worker = new Thread(task, "planner-place-load");
        worker.setDaemon(true);
        worker.start();
    }

    private void onLocationSelected(double lat, double lng) {
        selectedLatitude = lat;
        selectedLongitude = lng;
        hintLabel.setText("출발 위치를 선택했어요. 공통 조건을 정하고 장소를 골라 주세요.");
        errorLabel.setText("");
        updateRange();
    }

    private int radius() { return (int) Math.round(radiusSlider.getValue() / 100) * 100; }

    private void updateRange() {
        radiusLabel.setText("출발지 중심 · " + radius() + "m");
        if (selectedLatitude != null) {
            RoutePlan preview = new RoutePlan(selectedLatitude, selectedLongitude, 1, radius(), "",
                    appliedPlan == null ? List.of() : appliedPlan.places(),
                    appliedPlan == null ? java.util.Map.of() : appliedPlan.arrivalRadii());
            map.present(preview, null, radius(), false, true);
        }
    }

    @FXML
    private void onRecommendClick() {
        if (!placesLoaded) {
            if (placeRepository == null) errorLabel.setText("로그인 화면에서 실행해 장소 DB를 연결해 주세요.");
            else loadPlaces();
            return;
        }
        try {
            int people = NumberStepper.read(peopleField, 1, 20, "인원");
            if (selectedLatitude == null) throw new IllegalArgumentException("지도에서 출발 위치를 선택해 주세요.");
            RoutePlan snapshot = new RoutePlan(selectedLatitude, selectedLongitude, people, radius(),
                    appliedPlan == null ? "" : appliedPlan.title(), appliedPlan == null ? List.of() : appliedPlan.places(),
                    appliedPlan == null ? java.util.Map.of() : appliedPlan.arrivalRadii());
            openEditor(snapshot);
        } catch (IllegalArgumentException invalid) { errorLabel.setText(invalid.getMessage()); }
    }

    private void openEditor(RoutePlan snapshot) {
        errorLabel.setText("");
        routeEditorController.begin(snapshot, allPlaces, this::applyEdits, this::cancelEdits);
        showEditor(true);
    }

    private void applyEdits(RoutePlan edited) {
        fitOnReturn = true;
        appliedPlan = edited;
        selectedLatitude = edited.startLat();
        selectedLongitude = edited.startLng();
        radiusSlider.setValue(edited.radius());
        peopleField.setText(Integer.toString(edited.people()));
        resultLabel.setText(edited.places().isEmpty() ? String.format("코스가 비어 있어요 · %d명 · 예상 0원", edited.people()) : String.format("%s · %d곳 · %d명 · 예상 %,d원",
                edited.title().isBlank() ? "내 코스" : edited.title(), edited.places().size(), edited.people(), edited.totalCost()));
        recommendButton.setText(edited.places().isEmpty() ? "코스 만들기 →" : "코스 편집 →");
        map.present(edited, null, edited.radius(), false, true);
        showEditor(false);
    }

    private void cancelEdits() { fitOnReturn = false; showEditor(false); }

    private void showEditor(boolean open) {
        if (screenAnimation != null) screenAnimation.stop();
        double width = Math.max(1, plannerRoot.getWidth());
        if (open && !routeEditor.isVisible()) routeEditor.setTranslateX(width);
        routeEditor.setVisible(true);
        routeEditor.setManaged(true);
        routeEditor.setDisable(!open);
        mainPage.setDisable(open);
        screenAnimation = new Timeline(new KeyFrame(Duration.millis(280),
                new KeyValue(mainPage.translateXProperty(), open ? -width : 0, Interpolator.EASE_BOTH),
                new KeyValue(routeEditor.translateXProperty(), open ? 0 : width, Interpolator.EASE_BOTH)));
        screenAnimation.setOnFinished(event -> {
            routeEditor.setVisible(open);
            routeEditor.setManaged(open);
            mainPage.setVisible(!open);
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
