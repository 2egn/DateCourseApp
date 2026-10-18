package com.datecourse;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ConditionController implements Initializable {

    @FXML private TextField budgetField;
    @FXML private TextField peopleField;
    @FXML private FlowPane categoryPane;
    @FXML private Slider radiusSlider;
    @FXML private Label radiusLabel;
    @FXML private Label countLabel;
    @FXML private Label errorLabel;
    @FXML private Pane mapPane;

    private static final String[] CATEGORIES = {"식사", "카페", "문화"};
    private static final double METER_PER_PIXEL = 5; // 가짜 지도: 1픽셀 = 5m

    private final List<CheckBox> categoryBoxes = new ArrayList<>();
    private final PlaceCountService placeCountService = new PlaceCountService();
    private Double pickedX, pickedY; // 찍은 위치 (null이면 아직 안 찍음)

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 카테고리 체크박스 생성 (바뀌면 가게 수만 다시 계산)
        for (String name : CATEGORIES) {
            CheckBox box = new CheckBox(name);
            box.setOnAction(e -> updateCount());
            categoryBoxes.add(box);
            categoryPane.getChildren().add(box);
        }

        // 지도 영역 밖으로 그림이 넘치지 않게 자르기
        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(mapPane.widthProperty());
        clip.heightProperty().bind(mapPane.heightProperty());
        mapPane.setClip(clip);

        // 지도 클릭 → 위치 저장
        mapPane.setOnMouseClicked(e -> {
            pickedX = e.getX();
            pickedY = e.getY();
            redrawMap();
            updateCount();
        });

        // 반경 슬라이더
        radiusSlider.valueProperty().addListener((obs, oldV, newV) -> {
            radiusLabel.setText("반경: " + getRadiusMeter() + "m");
            redrawMap();
            updateCount();
        });

        redrawMap();
        updateCount();
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

    // 지도에는 "내 위치 + 반경 원"만 그림 (카카오 연동 후엔 지도 쪽이 대신 그림)
    private void redrawMap() {
        mapPane.getChildren().clear();
        if (pickedX == null) return;

        Circle area = new Circle(pickedX, pickedY, getRadiusMeter() / METER_PER_PIXEL);
        area.setFill(Color.rgb(66, 133, 244, 0.15));
        area.setStroke(Color.rgb(66, 133, 244));
        area.setMouseTransparent(true);

        Circle marker = new Circle(pickedX, pickedY, 7, Color.DODGERBLUE);
        marker.setMouseTransparent(true);

        mapPane.getChildren().addAll(area, marker);
    }

    // "반경 안 가게 N곳" 표시
    private void updateCount() {
        if (pickedX == null) {
            countLabel.setText("위치를 찍으면 주변 가게 수가 나와요");
            return;
        }
        // TODO: 지금은 가짜 좌표(픽셀). 카카오 연동 후 위도/경도로 교체
        int count = placeCountService.count(pickedY, pickedX, getRadiusMeter(), getSelectedCategories());
        countLabel.setText("반경 안 가게 약 " + count + "곳");
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
        if (pickedX == null) {
            errorLabel.setText("지도에서 위치를 찍어주세요.");
            return;
        }

        // 4. 조건 묶기
        errorLabel.setText("");

        SearchCondition condition = new SearchCondition(
                budget,
                people,
                categories,
                pickedY,   // 위도 (지금은 가짜 지도 픽셀)
                pickedX,   // 경도 (지금은 가짜 지도 픽셀)
                getRadiusMeter()
        );
        System.out.println(condition);
        // TODO: 백엔드 호출(condition 전송) → 3번 화면으로 이동
    }
}