package com.datecourse.controller;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import com.datecourse.service.NextPlaceService;
import com.datecourse.service.PlaceSearchService;
import com.datecourse.service.RouteRangeValidator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.geometry.Pos;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

/** 편집본만 변경한다. 메인 화면으로의 전달은 완료 버튼에서만 일어난다. */
public final class RouteEditorController {
    @FXML private WebView editMapView;
    @FXML private ComboBox<String> activityBox;
    @FXML private TextField placePriceField, titleField;
    @FXML private Button priceUp, priceDown, finishButton;
    @FXML private Button upButton, downButton, removeButton;
    @FXML private Button placeInfoButton;
    @FXML private TitledPane coursePane, conditionPane, choicePane;
    @FXML private ListView<PlaceSearchService.NearbyPlace> candidateList;
    @FXML private Slider radiusSlider;
    @FXML private Label radiusLabel, summaryLabel, editorError, commonLabel;
    @FXML private Label rangeWarning;
    @FXML private Slider selectedRadiusSlider;
    @FXML private Label selectedRadiusLabel;
    @FXML private Button recordRadiusButton;
    private boolean syncingSelectedRadius;
    @FXML private ListView<Place> placeList;
    private final ObservableList<Place> draftPlaces = FXCollections.observableArrayList();
    private final NextPlaceService search = new NextPlaceService();
    private final RouteRangeValidator rangeValidator = new RouteRangeValidator();
    private List<Place> outsideRange = List.of();
    private final java.util.Map<Long,Integer> arrivalRadii = new java.util.HashMap<>();
    private List<RouteRangeValidator.Leg> legs = List.of();
    private List<RouteRangeValidator.Leg> exceededLegs = List.of();
    private List<Place> allPlaces = List.of();
    private List<PlaceSearchService.NearbyPlace> candidates = List.of();
    private int candidateIndex = -1;
    private RoutePlan original;
    private PlannerMap map;
    private Consumer<RoutePlan> onComplete = ignored -> {};
    private Runnable onCancel = () -> {};
    private enum EditorState { CONDITION, CHOICE }
    private EditorState state = EditorState.CONDITION;
    private boolean changingState;

    @FXML
    private void initialize() {
        map = new PlannerMap(editMapView, (lat, lng) -> {}, editorError::setText);
        MenuButton walkingMenu = new MenuButton("도보 경로");
        walkingMenu.getStyleClass().add("drawer-toggle");
        MenuItem walkingQuery = new MenuItem("TMAP 도보 경로 조회");
        walkingQuery.setOnAction(event -> onWalkingRoute());
        MenuItem walkingSettings = new MenuItem("경로 탐색 방식");
        walkingSettings.setOnAction(event -> onTmapSettings());
        walkingMenu.getItems().addAll(walkingQuery, walkingSettings);
        ((HBox) finishButton.getParent()).getChildren().add(1, walkingMenu);
        map.setOnPlaceSelected(id -> {
            for (int index = 0; index < candidates.size(); index++) {
                if (candidates.get(index).place().recordId() == id) {
                    setState(EditorState.CHOICE);
                    candidateList.getSelectionModel().select(index);
                    break;
                }
            }
        });
        activityBox.getItems().setAll("식사", "카페", "문화");
        activityBox.getSelectionModel().selectFirst();
        NumberStepper.install(placePriceField, priceUp, priceDown, 0, 10000, 2);
        candidateList.setPlaceholder(new Label("조건을 정하고 추천받기를 눌러 주세요."));
        candidateList.setCellFactory(list -> new ListCell<>() {
            private final Label rank = new Label();
            private final Label name = new Label();
            private final Label meta = new Label();
            private final Button detail = new Button("상세");
            private final Button add = new Button("추가");
            private final HBox row;
            {
                rank.getStyleClass().add("candidate-rank");
                name.getStyleClass().add("section");
                meta.getStyleClass().add("planner-caption");
                VBox copy = new VBox(3, name, meta);
                copy.setMinWidth(0);
                HBox.setHgrow(copy, Priority.ALWAYS);
                name.setMaxWidth(Double.MAX_VALUE);
                detail.getStyleClass().add("candidate-row-detail");
                add.getStyleClass().add("candidate-row-add");
                detail.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                add.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                row = new HBox(8, rank, copy, detail, add);
                row.setAlignment(Pos.CENTER_LEFT);
                detail.setOnAction(event -> {
                    candidateList.getSelectionModel().select(getIndex());
                    onCandidateDetail();
                });
                add.setOnAction(event -> {
                    candidateList.getSelectionModel().select(getIndex());
                    onAddCandidate();
                });
            }
            @Override protected void updateItem(PlaceSearchService.NearbyPlace nearby, boolean empty) {
                super.updateItem(nearby, empty);
                setText(null);
                if (empty || nearby == null) { setGraphic(null); return; }
                rank.setText(Integer.toString(getIndex() + 1));
                name.setText(nearby.place().name());
                name.setTooltip(new Tooltip(nearby.place().name()));
                meta.setText(String.format("%.0fm · %,d원/인", nearby.distanceMeters(), nearby.place().price()));
                detail.setAccessibleText(nearby.place().name() + " 상세 보기");
                add.setAccessibleText(nearby.place().name() + " 코스에 추가");
                setGraphic(row);
            }
        });
        candidateList.getSelectionModel().selectedIndexProperty().addListener((obs, old, value) -> {
            candidateIndex = value.intValue();
            showCandidate();
        });
        activityBox.valueProperty().addListener((obs, old, value) -> clearCandidate());
        placePriceField.textProperty().addListener((obs, old, value) -> clearCandidate());
        radiusSlider.valueProperty().addListener((obs, old, value) -> {
            radiusLabel.setText("이전 장소에서 · " + radius() + "m 이내");
            clearCandidate();
            refreshMap(false);
            map.ensureRangesVisible();
        });
        editorError.managedProperty().bind(editorError.textProperty().isNotEmpty());
        editorError.visibleProperty().bind(editorError.managedProperty());
        rangeWarning.managedProperty().bind(rangeWarning.textProperty().isNotEmpty());
        rangeWarning.visibleProperty().bind(rangeWarning.managedProperty());
        placeList.setItems(draftPlaces);
        placeList.setPlaceholder(new Label("첫 장소를 추천받고 코스에 추가해 주세요."));
        placeList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Place place, boolean empty) {
                super.updateItem(place, empty);
                RouteRangeValidator.Leg leg = getIndex() >= 0 && getIndex() < legs.size() ? legs.get(getIndex()) : null;
                String distance = leg == null ? "" : String.format("\n%s → 여기: %.0fm · 허용 %s",
                        leg.from(), leg.distanceMeters(), leg.radius() == null ? "미기록" : leg.radius() + "m");
                setText(empty || place == null ? null : (outsideRange.contains(place) ? "[범위 밖] " : "")
                        + (leg != null && leg.exceeded() ? "[이동 거리 초과] " : "")
                        + (getIndex() + 1) + ". " + place.name()
                        + " · " + PlaceSearchService.categoryName(place.category())
                        + String.format("\n1인 %,d원 · %d명 %,d원", place.price(), original == null ? 1 : original.people(),
                        (long) place.price() * (original == null ? 1 : original.people())) + distance);
            }
        });
        placeList.getSelectionModel().selectedIndexProperty().addListener((obs, old, value) -> updateButtons());
        placeList.setOnMouseClicked(event -> { if (event.getClickCount() == 2) onPlaceDetail(); });
        selectedRadiusSlider.valueProperty().addListener((obs, old, value) -> {
            if (!syncingSelectedRadius) onUpdateSelectedRadius();
        });
        candidateList.setOnMouseClicked(event -> { if (event.getClickCount() == 2) onCandidateDetail(); });
        candidateList.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) { onCandidateDetail(); event.consume(); }
        });
        conditionPane.expandedProperty().addListener((obs, old, expanded) -> {
            if (!changingState) setState(expanded ? EditorState.CONDITION : EditorState.CHOICE);
        });
        choicePane.expandedProperty().addListener((obs, old, expanded) -> {
            if (!changingState) setState(expanded ? EditorState.CHOICE : EditorState.CONDITION);
        });
        updateButtons();
        setState(EditorState.CONDITION);
    }

    public void begin(RoutePlan snapshot, List<Place> places, Consumer<RoutePlan> complete, Runnable cancel) {
        original = snapshot;
        allPlaces = List.copyOf(places);
        onComplete = complete;
        onCancel = cancel;
        arrivalRadii.clear();
        arrivalRadii.putAll(snapshot.arrivalRadii());
        draftPlaces.setAll(snapshot.places());
        coursePane.setExpanded(false);
        titleField.setText(snapshot.title());
        commonLabel.setText(String.format("%d명 · 출발지 중심 %dm 범위", snapshot.people(), snapshot.radius()));
        clearCandidate();
        refreshSummary();
        map.load();
        refreshMap(true);
    }

    @FXML private void onReloadMap() {
        editorError.setText("");
        map.reload();
    }

    private RoutePlan draft() {
        return new RoutePlan(original.startLat(), original.startLng(), original.people(), original.radius(),
                titleField.getText().trim(), draftPlaces, arrivalRadii);
    }

    private int radius() { return (int) Math.round(radiusSlider.getValue() / 100) * 100; }

    @FXML
    private void onRecommendNext() {
        if (original == null) return;
        try {
            int price = NumberStepper.read(placePriceField, 0, 10000, "1인 가격 상한(만원)") * 10000;
            candidates = search.candidates(allPlaces, draftPlaces, original.startLat(), original.startLng(),
                    radius(), activityBox.getValue(), price, original.people(), original.radius());
            candidateIndex = -1;
            candidateList.getItems().setAll(candidates);
            candidateList.getSelectionModel().clearSelection();
            if (!candidates.isEmpty()) candidateList.getSelectionModel().selectFirst();
            editorError.setText(candidates.isEmpty() ? "조건에 맞는 새 장소가 없어요. 가격·반경·활동을 바꿔 주세요." : "");
            showCandidate();
            setState(candidates.isEmpty() ? EditorState.CONDITION : EditorState.CHOICE);
        } catch (IllegalArgumentException invalid) {
            clearCandidate();
            editorError.setText(invalid.getMessage());
        }
    }

    private Place candidate() { return candidateIndex < 0 || candidateIndex >= candidates.size() ? null : candidates.get(candidateIndex).place(); }

    private void showCandidate() {
        refreshMap(false);
    }

    private void clearCandidate() {
        candidates = List.of();
        candidateIndex = -1;
        candidateList.getItems().clear();
        setState(EditorState.CONDITION);
        editorError.setText("");
    }

    @FXML
    private void onAddCandidate() {
        if (state != EditorState.CHOICE) return;
        Place place = candidate();
        if (place == null) return;
        arrivalRadii.put(place.recordId(), radius());
        draftPlaces.add(place);
        placeList.getSelectionModel().selectLast();
        changed();
    }

    private void changed() {
        clearCandidate();
        refreshSummary();
        refreshMap(false);
    }

    private void refreshSummary() {
        if (original == null) return;
        RoutePlan plan = draft();
        summaryLabel.setText(String.format("%d곳 · %d명 누적 예상 금액 %,d원",
                draftPlaces.size(), plan.people(), plan.totalCost()));
        coursePane.setText("내 코스 보기 · " + draftPlaces.size() + "곳");
        updateButtons();
        placeList.refresh();
        if (!outsideRange.isEmpty() || !exceededLegs.isEmpty()) coursePane.setExpanded(true);
    }

    private void updateButtons() {
        int index = placeList.getSelectionModel().getSelectedIndex();
        upButton.setDisable(index <= 0);
        downButton.setDisable(index < 0 || index >= draftPlaces.size() - 1);
        removeButton.setDisable(index < 0);
        placeInfoButton.setDisable(index < 0);
        outsideRange = original == null ? List.of() : rangeValidator.outsideRange(draft());
        legs = original == null ? List.of() : rangeValidator.legs(draft());
        exceededLegs = legs.stream().filter(RouteRangeValidator.Leg::exceeded).toList();
        String globalWarning = outsideRange.isEmpty() ? "" : String.format(
                "전체 범위(%dm) 밖의 장소 %d곳: %s. 삭제하거나 메인 화면에서 출발지·범위를 조정해 주세요.",
                original.radius(), outsideRange.size(), outsideRange.stream().map(Place::name)
                        .collect(java.util.stream.Collectors.joining(", ")));
        String legWarning = exceededLegs.stream().map(leg -> String.format("%s → %s: %.0fm / 허용 %dm",
                leg.from(), leg.to().name(), leg.distanceMeters(), leg.radius()))
                .collect(java.util.stream.Collectors.joining("\n"));
        rangeWarning.setText(globalWarning + (globalWarning.isEmpty() || legWarning.isEmpty() ? "" : "\n")
                + (legWarning.isEmpty() ? "" : "이동 거리 초과 — 허용 반경·순서를 조정하거나 장소를 삭제해 주세요.\n" + legWarning));
        finishButton.setDisable(original == null || !outsideRange.isEmpty() || !exceededLegs.isEmpty());
        syncSelectedRadius();
    }

    private void syncSelectedRadius() {
        int index = placeList.getSelectionModel().getSelectedIndex();
        boolean selected = index >= 0 && index < legs.size();
        syncingSelectedRadius = true;
        try {
            selectedRadiusSlider.setDisable(!selected);
            RouteRangeValidator.Leg leg = selected ? legs.get(index) : null;
            selectedRadiusSlider.setValue(leg == null || leg.radius() == null ? 500 : leg.radius());
            boolean missing = leg != null && leg.radius() == null;
            recordRadiusButton.setVisible(missing);
            recordRadiusButton.setManaged(missing);
            selectedRadiusLabel.setText(leg == null ? "장소를 선택하면 이동 반경을 수정할 수 있어요."
                    : String.format("%s → %s · 실제 %.0fm · 허용 %s",
                    leg.from(), leg.to().name(), leg.distanceMeters(),
                    leg.radius() == null ? "미기록 (조절하거나 500m로 기록)" : leg.radius() + "m"));
        } finally { syncingSelectedRadius = false; }
    }

    @FXML private void onUpdateSelectedRadius() {
        Place selected = placeList.getSelectionModel().getSelectedItem();
        if (selected == null || original == null) return;
        int limit = (int) Math.round(selectedRadiusSlider.getValue() / 100) * 100;
        arrivalRadii.put(selected.recordId(), limit);
        refreshSummary(); // Revalidate only; keep next-place conditions, candidates and map camera.
    }

    @FXML private void onMoveUp() { move(-1); }
    @FXML private void onMoveDown() { move(1); }
    private void move(int direction) {
        int from = placeList.getSelectionModel().getSelectedIndex();
        int to = from + direction;
        if (from < 0 || to < 0 || to >= draftPlaces.size()) return;
        Collections.swap(draftPlaces, from, to);
        placeList.getSelectionModel().select(to);
        changed();
    }
    @FXML private void onRemove() {
        int index = placeList.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            arrivalRadii.remove(draftPlaces.get(index).recordId());
            draftPlaces.remove(index);
            changed();
        }
    }
    @FXML private void onFinish() {
        if (original == null) return;
        updateButtons(); // Guard the completion callback as well as the UI button.
        if (!outsideRange.isEmpty() || !exceededLegs.isEmpty()) { coursePane.setExpanded(true); return; }
        if (titleField.getText().trim().length() > 20) { editorError.setText("코스 이름은 20자 이하로 입력해 주세요."); return; }
        onComplete.accept(draft());
    }
    @FXML private void onCancel() { onCancel.run(); }
    @FXML private void onModifyConditions() { setState(EditorState.CONDITION); }

    private void setState(EditorState requested) {
        state = requested == EditorState.CHOICE && candidates.isEmpty() ? EditorState.CONDITION : requested;
        changingState = true;
        try {
            conditionPane.setExpanded(state == EditorState.CONDITION);
            choicePane.setExpanded(state == EditorState.CHOICE);
            choicePane.setDisable(candidates.isEmpty());
            choicePane.setText(candidates.isEmpty() ? "추천 후보" : "추천 후보 · " + candidates.size() + "곳");
            conditionPane.setText(state == EditorState.CONDITION ? "다음 장소의 조건"
                    : String.format("조건 · %s · %s만원 이하 · %dm", activityBox.getValue(), placePriceField.getText(), radius()));
        } finally { changingState = false; }
        refreshMap(false);
    }
    @FXML private void onCandidateDetail() { if (state == EditorState.CHOICE && candidate() != null) map.showDetail(candidate()); }
    @FXML private void onPlaceDetail() {
        Place selected = placeList.getSelectionModel().getSelectedItem();
        if (selected != null) map.showDetail(selected);
    }
    @FXML private void onFitRange() { map.fit(false); }
    @FXML private void onFitRoute() { map.fit(true); }

    public void fitRange() { refreshMap(true); }
    @FXML private void onWalkingRoute() { map.requestWalkingRoute(); }
    @FXML private void onTmapSettings() {
        if (TmapSettingsDialog.show(editMapView.getScene().getWindow())) map.refreshWalkingPreferences();
    }

    private void refreshMap(boolean fit) {
        if (original != null) map.presentCandidates(draft(), state == EditorState.CHOICE
                        ? candidates.stream().map(PlaceSearchService.NearbyPlace::place).toList() : List.of(),
                state == EditorState.CHOICE ? candidate() : null, radius(), true, fit);
    }
}
