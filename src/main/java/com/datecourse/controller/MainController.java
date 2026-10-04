package com.datecourse.controller;

import com.datecourse.data.DemoPlaces;
import com.datecourse.domain.*;
import com.datecourse.service.RecommendationService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class MainController {
    @FXML private TextField budgetField, peopleField, latitudeField, longitudeField, radiusField;
    @FXML private CheckBox mealCheck, cafeCheck, cultureCheck, entertainmentCheck;
    @FXML private Button searchButton;
    @FXML private ListView<Course> courseList;
    @FXML private Label statusLabel, detailLabel;
    private final RecommendationService service = new RecommendationService();

    @FXML private void initialize() {
        courseList.setCellFactory(list -> new ListCell<>() {
            { selectedProperty().addListener((o, old, selected) -> setOpacity(selected ? 1 : 0.55)); }
            @Override protected void updateItem(Course course, boolean empty) {
                super.updateItem(course, empty);
                setText(empty || course == null ? null : String.format(Locale.KOREA,
                        "%d위 · 예상 %,d~%,d원\n%s", getIndex() + 1,
                        course.lowerCostWon(), course.upperCostWon(),
                        String.join(" → ", course.stops().stream().map(Place::name).toList())));
                setOpacity(isSelected() ? 1 : 0.55);
            }
        });
        courseList.getSelectionModel().selectedItemProperty().addListener((o, old, course) -> show(course));
    }

    @FXML private void recommend() {
        try {
            List<Category> categories = new ArrayList<>();
            if (mealCheck.isSelected()) categories.add(Category.MEAL);
            if (cafeCheck.isSelected()) categories.add(Category.CAFE);
            if (cultureCheck.isSelected()) categories.add(Category.CULTURE);
            if (entertainmentCheck.isSelected()) categories.add(Category.ENTERTAINMENT);
            RecommendationRequest request = new RecommendationRequest(
                    Long.parseLong(budgetField.getText().trim()), Integer.parseInt(peopleField.getText().trim()),
                    new GeoPoint(Double.parseDouble(latitudeField.getText().trim()),
                            Double.parseDouble(longitudeField.getText().trim())),
                    Double.parseDouble(radiusField.getText().trim()), categories);
            Task<List<Course>> task = new Task<>() {
                @Override protected List<Course> call() { return service.recommend(request, DemoPlaces.all()); }
            };
            searchButton.setDisable(true);
            courseList.getItems().clear();
            detailLabel.setText("추천 코스를 계산하고 있습니다.");
            statusLabel.setText("검색 중…");
            task.setOnSucceeded(event -> {
                searchButton.setDisable(false);
                courseList.getItems().setAll(task.getValue());
                statusLabel.setText(task.getValue().isEmpty()
                        ? "조건에 맞는 코스가 없습니다. 예산·반경·카테고리를 조정해 주세요."
                        : "가상 데이터 추천 · 가격 상한 합계가 예산 이내인 코스입니다.");
                if (!task.getValue().isEmpty()) courseList.getSelectionModel().selectFirst();
                else detailLabel.setText("추천 결과가 없습니다.");
            });
            task.setOnFailed(event -> {
                searchButton.setDisable(false);
                statusLabel.setText("추천 계산에 실패했습니다. 입력값을 확인해 주세요.");
                detailLabel.setText("추천 결과를 계산할 수 없습니다.");
            });
            Thread worker = new Thread(task, "recommendation-worker");
            worker.setDaemon(true);
            worker.start();
        } catch (NumberFormatException exception) {
            statusLabel.setText("예산·인원은 정수, 좌표·반경은 숫자로 입력해 주세요.");
        } catch (IllegalArgumentException exception) {
            statusLabel.setText(exception.getMessage());
        }
    }

    private void show(Course course) {
        if (course == null) return;
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < course.stops().size(); i++) {
            Place place = course.stops().get(i);
            text.append(String.format(Locale.KOREA, "%d. %s (%s)\n   1인 예상 %,d~%,d원 · 샘플 평점 %.1f\n\n",
                    i + 1, place.name(), place.category().label(), place.priceLowerWon(),
                    place.priceUpperWon(), place.rating()));
        }
        text.append(String.format(Locale.KOREA, "전체 예상 비용: %,d~%,d원\n중심점부터 순서대로 잇는 직선거리: %.0fm\n\n",
                course.lowerCostWon(), course.upperCostWon(), course.distanceMeters()));
        text.append("실제 도보 경로·교통비·영업시간은 반영하지 않았습니다.\n모든 장소·가격·평점은 가상 데이터입니다.\n카카오 지도는 추후 이 패널에 연동합니다.");
        detailLabel.setText(text.toString());
    }
}
