package com.datecourse.controller;


import kr.ac.university.localdb.UserSession;

import com.datecourse.model.SearchCondition;
import com.datecourse.model.Place;
import com.datecourse.service.PlaceSearchService;
import com.datecourse.service.ExternalBrowser;
import kr.ac.university.localdb.db.PlaceRepository;
import javafx.concurrent.Task;
import com.fasterxml.jackson.databind.ObjectMapper;

import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.fxml.Initializable;
import javafx.concurrent.Worker;
import netscape.javascript.JSObject;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;
import javafx.util.Duration;

import java.net.URL;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class ConditionController implements Initializable {

    @FXML private TextField budgetField;
    @FXML private TextField peopleField;
    @FXML private TextField minPlacesField;
    @FXML private TextField maxPlacesField;
    @FXML private Button minPlacesUp, minPlacesDown, maxPlacesUp, maxPlacesDown;
    @FXML private Button peopleUp, peopleDown;
    @FXML private FlowPane categoryPane;
    @FXML private Slider radiusSlider;
    @FXML private Label radiusLabel;
    @FXML private Label countLabel;
    @FXML private Label errorLabel;
    @FXML private Label hintLabel;
    @FXML private WebView mapView;
    @FXML private StackPane mapPane;
    @FXML private StackPane conditionDrawer;
    @FXML private ScrollPane conditionScroll;
    @FXML private Button drawerToggle;
    @FXML private Button recommendButton;
    @FXML private Label resultLabel;
    @FXML private Label resultBadge, resultDetail;
    @FXML private javafx.scene.layout.VBox resultSummary;
    private List<PlaceSearchService.NearbyPlace> nearbyPlaces = List.of();
    private boolean hasRecommended;
    private final PlaceSearchService placeSearch = new PlaceSearchService();
    private PlaceRepository placeRepository;
    private List<Place> allPlaces = List.of();
    private List<PlaceSearchService.NearbyPlace> recommendedPlaces = List.of();
    private boolean placesLoaded;
    private boolean placesLoading;
    private int loadVersion;

    public void setPlaceRepository(PlaceRepository repository) {
        placeRepository = repository;
        loadPlaces();
    }

    private void loadPlaces() {
        if (placeRepository == null || placesLoading) return;
        placesLoading = true;
        placesLoaded = false;
        recommendButton.setDisable(true);
        recommendButton.setText("장소 불러오는 중…");
        countLabel.setText("DB에서 장소를 불러오고 있어요…");
        int version = ++loadVersion;
        PlaceRepository repository = placeRepository;
        Task<List<Place>> task = new Task<>() {
            @Override protected List<Place> call() throws Exception { return repository.findAll(); }
        };
        task.setOnSucceeded(event -> {
            if (version != loadVersion) return;
            allPlaces = List.copyOf(task.getValue());
            placesLoaded = true;
            placesLoading = false;
            recommendButton.setDisable(false);
            updateCount();
        });
        task.setOnFailed(event -> {
            if (version != loadVersion) return;
            placesLoading = false;
            recommendButton.setDisable(false);
            countLabel.setText("장소를 불러오지 못했어요. 추천 버튼으로 다시 시도해 주세요.");
            recommendButton.setText("장소 다시 불러오기");
        });
        Thread worker = new Thread(task, "planner-place-load");
        worker.setDaemon(true);
        worker.start();
    }

    private static final double DRAWER_WIDTH = 300;
    private boolean conditionsOpen = true;
    private Timeline drawerAnimation;

    private static final String[] CATEGORIES = {"식사", "카페", "문화"};

    private final List<CheckBox> categoryBoxes = new ArrayList<>();
    private static final String MAP_URL = "http://localhost:8080/map.html";
    private Double selectedLatitude, selectedLongitude;
    private boolean mapReady;
    private final MapBridge mapBridge = new MapBridge(
            this::onMapReady, this::onLocationSelected, this::onMapError, this::openExternalLink);

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        budgetField.textProperty().addListener((o, old, value) -> updateCount());
        peopleField.textProperty().addListener((o, old, value) -> updateCount());
        minPlacesField.textProperty().addListener((o, old, value) -> updateCount());
        maxPlacesField.textProperty().addListener((o, old, value) -> updateCount());
        configureNumberStepper(minPlacesField, minPlacesUp, minPlacesDown, 2, 10);
        configureNumberStepper(maxPlacesField, maxPlacesUp, maxPlacesDown, 5, 10);
        configureNumberStepper(peopleField, peopleUp, peopleDown, 2, 20);
        errorLabel.managedProperty().bind(errorLabel.textProperty().isNotEmpty());
        errorLabel.visibleProperty().bind(errorLabel.managedProperty());
        for (String name : CATEGORIES) {
            CheckBox box = new CheckBox(name);
            box.getStyleClass().add("category-chip");
            box.setOnAction(e -> updateCount());
            categoryBoxes.add(box);
            categoryPane.getChildren().add(box);
        }

        radiusSlider.valueProperty().addListener((obs, oldV, newV) -> {
            radiusLabel.setText("탐색 반경 · " + getRadiusMeter() + "m");
            updateMapRadius();
            updateCount();
        });

        mapView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldState, state) -> {
            if (state == Worker.State.SCHEDULED) {
                mapReady = false;
                hintLabel.setText("지도를 불러오고 있어요…");
            } else if (state == Worker.State.SUCCEEDED) {
                // Only our local map page receives a Java bridge.
                if (!MAP_URL.equals(mapView.getEngine().getLocation())) {
                    onMapError("등록된 지도 페이지를 열어 주세요.");
                    return;
                }
                try {
                    JSObject window = (JSObject) mapView.getEngine().executeScript("window");
                    window.setMember("javaMapBridge", mapBridge);
                    mapView.getEngine().executeScript("window.connectJavaMapBridge()");
                } catch (RuntimeException failure) {
                    onMapError("지도 연결을 준비하지 못했습니다. 페이지를 확인하고 앱을 다시 실행해 주세요.");
                }
            } else if (state == Worker.State.FAILED || state == Worker.State.CANCELLED) {
                onMapError("지도를 열지 못했습니다. 로컬 웹 서버와 인터넷 연결을 확인해 주세요.");
            }
        });
        mapView.getEngine().load(MAP_URL);

        Rectangle drawerClip = new Rectangle();
        drawerClip.widthProperty().bind(conditionDrawer.widthProperty());
        drawerClip.heightProperty().bind(conditionDrawer.heightProperty());
        conditionDrawer.setClip(drawerClip);
        Rectangle mapClip = new Rectangle();
        mapClip.widthProperty().bind(mapPane.widthProperty());
        mapClip.heightProperty().bind(mapPane.heightProperty());
        mapClip.setArcWidth(24);
        mapClip.setArcHeight(24);
        mapPane.setClip(mapClip);
        drawerToggle.setAccessibleText("조건 입력 패널 접기");

        updateCount();
        conditionScroll.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (mapReady) mapView.getEngine().executeScript("window.setPlanningMode(true)");
        });
    }

    private void onMapReady() {
        mapReady = true;
        errorLabel.setText("");
        hintLabel.setText("출발할 장소를 찾아보세요");
        updateMapRadius();
        syncPlannerResults();
        if (mapReady && selectedLatitude != null) {
            mapView.getEngine().executeScript(String.format(Locale.ROOT,
                    "window.selectMapLocation(%s, %s)", selectedLatitude, selectedLongitude));
        }
    }

    private void onLocationSelected(double latitude, double longitude) {
        selectedLatitude = latitude;
        selectedLongitude = longitude;
        hintLabel.setText("코스 조건을 입력해 주세요");
        errorLabel.setText("");
        updateCount();
    }

    private void onMapError(String message) {
        mapReady = false;
        hintLabel.setText("지도 연결을 확인해 주세요");
        errorLabel.setText(message);
    }

    private void openExternalLink(String address) {
        Thread browser = new Thread(() -> {
            try {
                ExternalBrowser.open(URI.create(address));
            } catch (Exception failure) {
                Platform.runLater(() -> errorLabel.setText("외부 브라우저를 열지 못했어요. 기본 브라우저 설정을 확인해 주세요."));
            }
        }, "map-external-link");
        browser.setDaemon(true);
        browser.start();
    }

    private void updateMapRadius() {
        if (!mapReady) return;
        try {
            mapView.getEngine().executeScript("window.setMapRadius(" + getRadiusMeter() + ")");
        } catch (RuntimeException failure) {
            onMapError("지도 반경을 변경하지 못했습니다. 앱을 다시 실행해 주세요.");
        }
    }

    @FXML
    private void onToggleConditions() {
        if (drawerAnimation != null) drawerAnimation.stop();
        conditionsOpen = !conditionsOpen;
        setExploringResults(!conditionsOpen && !recommendedPlaces.isEmpty());
        // Collapsed controls must also be excluded from keyboard navigation.
        conditionScroll.setVisible(true);
        conditionScroll.setDisable(!conditionsOpen);
        drawerToggle.setText(conditionsOpen ? "‹ 조건 접기" : "조건 열기 ›");
        drawerToggle.setAccessibleText(conditionsOpen ? "조건 입력 패널 접기" : "조건 입력 패널 열기");
        drawerAnimation = new Timeline(new KeyFrame(Duration.millis(240),
                new KeyValue(conditionDrawer.prefWidthProperty(), conditionsOpen ? DRAWER_WIDTH : 0,
                        Interpolator.EASE_BOTH),
                new KeyValue(conditionScroll.translateXProperty(), conditionsOpen ? 0 : -DRAWER_WIDTH,
                        Interpolator.EASE_BOTH)));
        drawerAnimation.setOnFinished(event -> conditionScroll.setVisible(conditionsOpen));
        drawerAnimation.play();
    }

    private int getRadiusMeter() {
        return (int) (Math.round(radiusSlider.getValue() / 100) * 100); // 100m 단위
    }

    private List<String> getSelectedCategories() {
        List<String> selected = new ArrayList<>();
        for (CheckBox box : categoryBoxes) {
            if (box.isSelected()) selected.add(box.getText());
        }
        return selected;
    }


    private void updateCount() {
        recommendedPlaces = List.of();
        if (mapReady) errorLabel.setText("");
        nearbyPlaces = List.of();
        syncPlannerResults();
        setExploringResults(false);
        recommendButton.setText(hasRecommended ? "변경한 조건으로 추천" : "코스 추천받기");
        setResultSummary("주변 장소", "출발 위치와 조건을 선택해 나만의 코스를 만들어 보세요.", "browse");

        if (selectedLatitude == null) {
            countLabel.setText("지도에서 출발 위치를 선택해 주세요.");
            return;
        }
        if (!placesLoaded) {
            countLabel.setText(placesLoading ? "DB에서 장소를 불러오고 있어요…" : "장소 조회가 필요합니다. 추천 버튼을 눌러 주세요.");
            return;
        }
        List<PlaceSearchService.NearbyPlace> nearby = placeSearch.nearby(allPlaces,
                selectedLatitude, selectedLongitude, getRadiusMeter(), getSelectedCategories());
        countLabel.setText("반경 " + getRadiusMeter() + "m · 주변 장소 " + nearby.size() + "곳");
        nearbyPlaces = nearby;
        syncPlannerResults();
        setResultSummary("주변 장소 · " + nearby.size() + "곳", "가까운 순으로 표시합니다 · 가격·평점은 저장된 참고 정보입니다.", "browse");
    }

    private UserSession userSession;

    public void setUserSession(UserSession userSession){
        this.userSession = userSession;
    }
    @FXML
    private void onRecommendClick() {
        if (!placesLoaded) {
            if (placeRepository == null) errorLabel.setText("DB 조회가 연결되지 않았어요. 로그인 화면에서 앱을 실행해 주세요.");
            else loadPlaces();
            return;
        }
        // 1. 예산 검사
        String budgetText = budgetField.getText().trim();
        if (budgetText.isEmpty()) {
            errorLabel.setText("예산을 입력해주세요.");
            return;
        }
        int budget;
        try {
            budget = Integer.parseInt(budgetText);
        } catch (NumberFormatException e) {
            errorLabel.setText("예산은 숫자만 입력해주세요.");
            return;
        }
        if (budget <= 0) {
            errorLabel.setText("예산은 0보다 커야 해요.");
            return;
        }

        // 2. 인원 검사
        String peopleText = peopleField.getText().trim();
        if (peopleText.isEmpty()) {
            errorLabel.setText("인원을 입력해주세요.");
            return;
        }
        int people;
        try {
            people = Integer.parseInt(peopleText);
        } catch (NumberFormatException e) {
            errorLabel.setText("인원은 숫자만 입력해주세요.");
            return;
        }
        if (people < 1 || people > 20) {
            errorLabel.setText("인원은 1~20명 사이로 입력해주세요.");
            return;
        }

        // 3. 카테고리, 위치 검사
        List<String> categories = getSelectedCategories();
        if (categories.isEmpty()) {
            errorLabel.setText("카테고리를 하나 이상 선택해주세요.");
            return;
        }
        int minPlaces, maxPlaces;
        try {
            minPlaces = Integer.parseInt(minPlacesField.getText().trim());
            maxPlaces = Integer.parseInt(maxPlacesField.getText().trim());
        } catch (NumberFormatException failure) {
            errorLabel.setText("최소·최대 방문 장소 수를 정수로 입력해 주세요.");
            return;
        }
        if (minPlaces < 1 || maxPlaces > 10 || minPlaces > maxPlaces) {
            errorLabel.setText("방문 장소 수는 1~10곳이며 최소 개수는 최대 개수 이하로 입력해 주세요.");
            return;
        }
        if (maxPlaces < categories.size()) {
            errorLabel.setText("선택한 활동을 모두 포함하려면 최대 방문 장소 수를 " + categories.size() + "곳 이상으로 설정해 주세요.");
            return;
        }
        if (!mapReady) {
            errorLabel.setText("지도가 준비된 뒤 위치를 선택해 주세요.");
            return;
        }
        if (selectedLatitude == null) {
            errorLabel.setText("지도에서 위치를 찍어주세요.");
            return;
        }

        // 4. 조건 묶기
        errorLabel.setText("");

        SearchCondition condition = new SearchCondition(
                budget,
                people,
                categories,
                selectedLatitude,
                selectedLongitude,
                getRadiusMeter(),
                minPlaces,
                maxPlaces
        );
        PlaceSearchService.Result result = placeSearch.recommend(allPlaces, condition);
        if (!result.missingCategories().isEmpty()) {
            setResultSummary("선택한 활동의 장소가 없어요", "주변에 " + String.join(", ", result.missingCategories()) + " 장소가 없어요. 반경을 늘리거나 분류를 바꿔 주세요.", "warning");
            nearbyPlaces = result.nearby();
            syncPlannerResults();
        } else if (result.insufficientPlaces()) {
            setResultSummary("주변 장소가 부족해요", "최소 " + minPlaces + "곳을 채우려면 반경을 늘리거나 최소 개수를 줄여 주세요.", "warning");
            nearbyPlaces = result.nearby();
            syncPlannerResults();
        } else if (result.overBudget()) {
            setResultSummary("예산이 부족해요", String.format(Locale.KOREA,
                    "활동·최소 장소 수를 만족하는 최저가 합계는 %d명 기준 %,d원입니다.", people, result.totalCost()), "warning");
            nearbyPlaces = result.nearby();
            syncPlannerResults();
        } else {
            recommendedPlaces = result.course();
            setResultSummary(String.format(Locale.KOREA,
                    "추천 코스 · %d곳 · %d명 · 총 예상 %,d원", result.course().size(), people, result.totalCost()),
                    String.format(Locale.KOREA, "예산 %,d원 중 %,d원 남음 · 가격·평점은 저장된 참고 정보입니다.", budget, budget - result.totalCost()), "success");
            nearbyPlaces = result.nearby();
            hasRecommended = true;
            recommendButton.setText("이 조건으로 다시 추천");
            hintLabel.setText("오른쪽 탭에서 코스와 주변 장소를 둘러보세요.");
            syncPlannerResults();
            setExploringResults(true);
        }
    }

    private void setResultSummary(String title, String detail, String state) {
        resultLabel.setText(title);
        resultDetail.setText(detail);
        resultSummary.setAccessibleText(title + " · " + detail);
        if (state.equals("warning")) errorLabel.setText(detail);
        resultBadge.setText(switch (state) { case "success" -> "추천 완료"; case "warning" -> "조건 확인"; default -> "주변 탐색"; });
        resultSummary.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("success"), state.equals("success"));
        resultSummary.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("warning"), state.equals("warning"));
    }

    private void configureNumberStepper(TextField field, Button up, Button down, int fallback, int maximum) {
        Runnable updateButtons = () -> {
            int value = placeCountValue(field, fallback);
            up.setDisable(value >= maximum);
            down.setDisable(value <= 1);
        };
        field.textProperty().addListener((o, old, value) -> updateButtons.run());
        field.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.UP || event.getCode() == javafx.scene.input.KeyCode.DOWN) {
                stepNumber(field, event.getCode() == javafx.scene.input.KeyCode.UP ? 1 : -1, fallback, maximum);
                event.consume();
            }
        });
        javafx.scene.Node stepper = field.getParent();
        stepper.addEventFilter(javafx.scene.input.ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() != 0) {
                stepNumber(field, event.getDeltaY() > 0 ? 1 : -1, fallback, maximum);
                event.consume();
            }
        });
        updateButtons.run();
    }

    private int placeCountValue(TextField field, int fallback) {
        try { return Integer.parseInt(field.getText().trim()); }
        catch (NumberFormatException invalid) { return fallback; }
    }

    private void stepNumber(TextField field, int step, int fallback, int maximum) {
        long value = (long) placeCountValue(field, fallback) + step;
        field.setText(Long.toString(Math.max(1, Math.min(maximum, value))));
        field.requestFocus();
        field.positionCaret(field.getText().length());
    }

    @FXML private void onMinPlacesUp() { stepNumber(minPlacesField, 1, 2, 10); }
    @FXML private void onMinPlacesDown() { stepNumber(minPlacesField, -1, 2, 10); }
    @FXML private void onMaxPlacesUp() { stepNumber(maxPlacesField, 1, 5, 10); }
    @FXML private void onMaxPlacesDown() { stepNumber(maxPlacesField, -1, 5, 10); }
    @FXML private void onPeopleUp() { stepNumber(peopleField, 1, 2, 20); }
    @FXML private void onPeopleDown() { stepNumber(peopleField, -1, 2, 20); }

    private void syncPlannerResults() {
        if (!mapReady) return;
        try {
            ObjectMapper mapper = new ObjectMapper();
            String course = mapper.writeValueAsString(recommendedPlaces.stream().map(PlaceSearchService.NearbyPlace::place).toList());
            String nearby = mapper.writeValueAsString(nearbyPlaces.stream().map(PlaceSearchService.NearbyPlace::place).toList());
            mapView.getEngine().executeScript("window.updatePlannerPlaces(" + course + "," + nearby + ")");
        } catch (Exception failure) {
            errorLabel.setText("장소 패널을 표시하지 못했어요. 지도를 다시 확인해 주세요.");
        }
    }

    private void setExploringResults(boolean exploring) {
        if (mapReady) mapView.getEngine().executeScript("window.setPlanningMode(" + !exploring + ")");
        if (!exploring && mapReady) hintLabel.setText(selectedLatitude == null
                ? "출발 위치를 선택하고 조건을 설정해 주세요."
                : hasRecommended ? "조건을 확인하고 다시 추천받을 수 있어요." : "주변 장소를 확인하고 코스 조건을 골라 주세요.");
    }
}
