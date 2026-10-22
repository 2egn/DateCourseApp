package com.datecourse.controller;


import kr.ac.university.localdb.UserSession;

import com.datecourse.model.SearchCondition;

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
import java.awt.Desktop;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

public class ConditionController implements Initializable {

    @FXML private TextField budgetField;
    @FXML private TextField peopleField;
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
    }

    private void onMapReady() {
        mapReady = true;
        errorLabel.setText("");
        hintLabel.setText("출발할 장소를 찾아보세요");
        updateMapRadius();
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
                if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    throw new IllegalStateException("Browser is unavailable");
                }
                Desktop.getDesktop().browse(URI.create(address));
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


    // Actual place counts will be provided by the place repository, not a fabricated estimate.
    private void updateCount() {
        if (selectedLatitude == null) {
            countLabel.setText("지도에서 출발 위치를 선택해 주세요.");
            return;
        }
        countLabel.setText("출발 위치 선택 완료 · 반경 " + getRadiusMeter() + "m\n주변 장소 수는 DB 연동 후 표시됩니다.");
    }

    private UserSession userSession;

    public void setUserSession(UserSession userSession){
        this.userSession = userSession;
    }
    @FXML
    private void onRecommendClick() {
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
                getRadiusMeter()
        );
        System.out.println(condition);
        // TODO: 백엔드 호출(condition 전송) → 3번 화면으로 이동
    }
}
