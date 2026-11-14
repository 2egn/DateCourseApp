package com.datecourse.controller;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import com.datecourse.model.CostEstimate;
import com.datecourse.model.NextPlaceCondition;
import com.datecourse.model.PlaceTag;
import com.datecourse.service.PlaceFacets;
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
public final class RouteEditorController extends BasePageController {
    @FXML private WebView editMapView;
    @FXML private ComboBox<String> activityBox;
    @FXML private ComboBox<String> subtypeBox;
    @FXML private ComboBox<PlaceTag> tagBox;
    @FXML private ComboBox<Integer> candidateLimitBox;
    @FXML private Label candidateStatus, candidateCount;
    @FXML private Button recommendNextButton;
    private List<PlaceSearchService.NearbyPlace> matchingCandidates = List.of();
    private boolean candidatesDirty;
    private long searchVersion;
    private boolean updatingFacets;
    private String facetsActivity;
    private final java.util.Map<String,String> rememberedSubtypes = new java.util.HashMap<>();
    private final java.util.Map<String,PlaceTag> rememberedTags = new java.util.HashMap<>();
    private Consumer<Place> onReport = ignored -> notifyUser(PageFeedback.Kind.INFO,"로그인 후 제보할 수 있어요.");
    @FunctionalInterface public interface PlaceRefresh { List<Place> apply(List<Place> places) throws Exception; }
    private PlaceRefresh placeRefresh;
    public void setPlaceRefresh(PlaceRefresh refresh) { placeRefresh = refresh; }
    public void setOnReport(Consumer<Place> report) { onReport = report; }
    @FXML private CheckBox includeUncertainPrice;
    @FXML private TextField placePriceField;
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
        watchNotice(editorError, PageFeedback.Kind.ERROR);
        map = new PlannerMap(editMapView, (lat, lng) -> {}, editorError::setText);
        map.setLoadingUi(this::beginLoading);
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
        candidateLimitBox.getItems().setAll(5,10,20,30);
        candidateLimitBox.setValue(5);
        subtypeBox.setPromptText("전체 종류"); tagBox.setPromptText("특징 제한 없음");
        subtypeBox.setCellFactory(v->new ListCell<>() {
            @Override protected void updateItem(String s,boolean empty){super.updateItem(s,empty);setText(empty?null:s==null?"전체 종류":s);}
        });
        tagBox.setCellFactory(v->new ListCell<>() {
            @Override protected void updateItem(PlaceTag t,boolean empty){super.updateItem(t,empty);setText(empty?null:t==null?"특징 제한 없음":t.label());}
        });
        subtypeBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(String s,boolean empty){super.updateItem(s,empty);setText(empty||s==null?"전체 종류":s);}
        });
        tagBox.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(PlaceTag t,boolean empty){super.updateItem(t,empty);setText(empty||t==null?"특징 제한 없음":t.label());}
        });
        candidateLimitBox.valueProperty().addListener((o,a,b)->{if(b!=null)displayCandidates();});
        NumberStepper.install(placePriceField, priceUp, priceDown, 0, 10000, 2);
        includeUncertainPrice.setTooltip(new Tooltip("금액 또는 상한을 모르는 장소도 표시해요. 이 경우 가격 상한 충족 여부를 보장할 수 없어요. 숫자가 있는 추정 가격은 기본적으로 계산에 활용해요."));
        candidateList.setPlaceholder(new Label("조건을 정하고 추천받기를 눌러 주세요."));
        candidateList.setCellFactory(list -> new ListCell<>() {
            private final Label rank = new Label();
            private final Label name = new Label();
            private final Label meta = new Label();
            private final Label badges = new Label();
            private final Button detail = new Button("상세");
            private final Button add = new Button("추가");
            private final Button report = new Button("정정 제보");
            private final HBox row;
            {
                rank.getStyleClass().add("candidate-rank");
                name.getStyleClass().add("section");
                meta.getStyleClass().add("planner-caption");
                badges.getStyleClass().add("candidate-evidence");
                VBox copy = new VBox(2, name, meta, badges);
                copy.setMinWidth(0);
                HBox.setHgrow(copy, Priority.ALWAYS);
                name.setMaxWidth(Double.MAX_VALUE);
                name.setMinWidth(0);badges.setMinWidth(0);meta.setMinWidth(0);
                detail.getStyleClass().add("candidate-row-detail");
                add.getStyleClass().add("candidate-row-add");
                detail.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                add.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
                VBox actions = new VBox(3,new HBox(4,detail,add),report);
                report.getStyleClass().add("candidate-report");
                report.setOnAction(e->{if(getItem()!=null)onReport.accept(getItem().place());});
                row = new HBox(8, rank, copy, actions);
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
                meta.setText(String.format("%.0fm · %s", nearby.distanceMeters(), nearby.place().priceText()));
                badges.setText(nearby.place().priceEstimate().evidenceText()
                    + nearby.place().details().tags().stream().limit(2).map(t->" · "+t.tag().label()).collect(java.util.stream.Collectors.joining()));
                badges.setTooltip(new Tooltip(nearby.place().priceEstimate().explanation()
                    + nearby.place().details().tags().stream().map(t->"\n"+t.tag().label()+" · "+t.basis()).collect(java.util.stream.Collectors.joining())));
                add.setDisable(candidatesDirty);
                detail.setAccessibleText(nearby.place().name() + " 상세 보기");
                add.setAccessibleText(nearby.place().name() + " 코스에 추가");
                setGraphic(row);
            }
        });
        candidateList.getSelectionModel().selectedIndexProperty().addListener((obs, old, value) -> {
            candidateIndex = value.intValue();
            showCandidate();
        });
        activityBox.valueProperty().addListener((obs, old, value) -> {
            if(old!=null){rememberedSubtypes.put(old,subtypeBox.getValue());rememberedTags.put(old,tagBox.getValue());}
            refreshFacets();markCandidatesDirty();
        });
        subtypeBox.valueProperty().addListener((o,a,b)->{if(!updatingFacets)markCandidatesDirty();});
        tagBox.valueProperty().addListener((o,a,b)->{if(!updatingFacets)markCandidatesDirty();});
        placePriceField.textProperty().addListener((obs, old, value) -> markCandidatesDirty());
        includeUncertainPrice.selectedProperty().addListener((obs, old, value) -> markCandidatesDirty());
        radiusSlider.valueProperty().addListener((obs, old, value) -> {
            radiusLabel.setText("직선 반경 · " + radius() + "m");
            markCandidatesDirty();
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
                        + "\n" + place.priceText() + " · " + (original == null ? 1 : original.people()) + "명 "
                        + CostEstimate.of(List.of(place), original == null ? 1 : original.people()).display() + distance);
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
        updateButtons();
        setState(EditorState.CONDITION);
    }

    public void prepareMap() { map.load(); }
    public void dispose() { searchVersion++;map.dispose(); }

    /** Reuse the live form so selected candidates and conditions also survive navigation. */
    public void resume(RoutePlan context, List<Place> places) {
        boolean changed = original.startLat() != context.startLat() || original.startLng() != context.startLng()
                || original.people() != context.people() || original.radius() != context.radius();
        original = context;
        allPlaces = List.copyOf(places);
        refreshFacets();
        commonLabel.setText(String.format("%d명 · 출발지 중심 %dm 범위", context.people(), context.radius()));
        if (changed) clearCandidate();
        refreshSummary();
        refreshMap(false);
    }

    public RoutePlan draftSnapshot() { return draft(); }

    @FXML private void onResetDraft() {
        if (original == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "편집 중인 장소와 후보를 비울까요? 저장한 코스는 유지돼요.", ButtonType.OK, ButtonType.CANCEL);
        confirm.initOwner(editMapView.getScene().getWindow());
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) resetDraft();
    }

    void resetDraft() {
        draftPlaces.clear();
        arrivalRadii.clear();
        clearCandidate();
        refreshSummary();
        refreshMap(false);
        notifyUser(PageFeedback.Kind.INFO, "편집 내용을 비웠어요.");
    }

    public void begin(RoutePlan snapshot, List<Place> places, Consumer<RoutePlan> complete, Runnable cancel) {
        original = snapshot;
        allPlaces = List.copyOf(places);
        refreshFacets();
        onComplete = complete;
        onCancel = cancel;
        arrivalRadii.clear();
        arrivalRadii.putAll(snapshot.arrivalRadii());
        draftPlaces.setAll(snapshot.places());
        coursePane.setExpanded(false);
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
                original.title(), draftPlaces, arrivalRadii);
    }

    private int radius() { return (int) Math.round(radiusSlider.getValue() / 100) * 100; }

    @FXML
    private void onRecommendNext() {
        if (original == null) return;
        try {
            int price = NumberStepper.read(placePriceField, 0, 10000, "1인 가격 상한(만원)") * 10000;
            NextPlaceCondition condition=new NextPlaceCondition(activityBox.getValue(),price,radius(),includeUncertainPrice.isSelected(),
                subtypeBox.getValue()==null?java.util.Set.of():java.util.Set.of(subtypeBox.getValue()),
                tagBox.getValue()==null?java.util.Set.of():java.util.Set.of(tagBox.getValue()),candidateLimitBox.getValue());
            long version=++searchVersion;
            RoutePlan context=draft(); List<Place> source=allPlaces;
            candidatesDirty=true;candidateList.refresh();candidateStatus.setText("후보 갱신 중…");
            Consumer<List<PlaceSearchService.NearbyPlace>> present=result->{
                if(version!=searchVersion)return;
                matchingCandidates=result;candidatesDirty=false;displayCandidates();
                editorError.setText(result.isEmpty()?"조건에 맞는 새 장소가 없어요. 세부 조건·가격·반경을 조정해 주세요.":"");
                candidateStatus.setText(result.isEmpty()?"일치하는 장소가 없어요. 세부 조건·가격·반경을 조정해 주세요.":
                    result.stream().anyMatch(p->p.place().priceEstimate().estimated())?"추정 가격 기준 후보가 포함돼요.":"조건 적용됨");
                setState(result.isEmpty()?EditorState.CONDITION:EditorState.CHOICE);
            };
            if(placeRefresh==null)present.accept(search.matching(source,context.places(),context.startLat(),context.startLng(),context.people(),context.radius(),condition));
            else runWork("후보를 찾고 있어요",()-> {
                List<Place> fresh=placeRefresh.apply(source);
                return search.matching(fresh,context.places(),context.startLat(),context.startLng(),context.people(),context.radius(),condition);
            },present,failure->{if(version==searchVersion){markCandidatesDirty();editorError.setText("후보 갱신에 실패했어요. 다시 시도해 주세요.");}});
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
        searchVersion++;
        matchingCandidates=List.of();candidatesDirty=false;
        candidates = List.of();
        candidateIndex = -1;
        candidateList.getItems().clear();
        candidateCount.setText("추천 전");candidateStatus.setText("조건을 정하고 후보를 찾아 주세요.");
        setState(EditorState.CONDITION);
        editorError.setText("");
    }

    @FXML
    private void onAddCandidate() {
        if (state != EditorState.CHOICE || candidatesDirty) return;
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
        summaryLabel.setText(String.format("%d곳 · %d명 · %s",
                draftPlaces.size(), plan.people(), plan.costEstimate().display()));
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
        onComplete.accept(draft());
    }
    @FXML private void onCancel() { onCancel.run(); }
    @FXML private void onModifyConditions() { setState(EditorState.CONDITION); }

    private void setState(EditorState requested) {
        state = requested == EditorState.CHOICE && candidates.isEmpty() ? EditorState.CONDITION : requested;
        changingState = true;
        try {
            conditionPane.setExpanded(true);
            choicePane.setExpanded(true);
            choicePane.setDisable(false);
            choicePane.setText(candidates.isEmpty() ? "추천 후보" : "추천 후보 · " + candidates.size() + "곳");
            conditionPane.setText("다음 장소의 조건");
        } finally { changingState = false; }
        refreshMap(false);
    }
    private void markCandidatesDirty() {
        searchVersion++; candidatesDirty=true;
        candidateStatus.setText(matchingCandidates.isEmpty()?"조건을 적용해 후보를 찾아 주세요.":"조건 변경됨 · 후보 업데이트 필요");
        candidateList.refresh();
    }
    private void displayCandidates() {
        if(candidateLimitBox.getValue()==null)return;
        Place selected=candidate();
        candidates=matchingCandidates.stream().limit(candidateLimitBox.getValue()).toList();
        candidateIndex=-1;candidateList.getItems().setAll(candidates);
        int index=selected==null?-1:java.util.stream.IntStream.range(0,candidates.size())
            .filter(i->candidates.get(i).place().placeId().equals(selected.placeId())).findFirst().orElse(-1);
        candidateList.getSelectionModel().select(index>=0?index:candidates.isEmpty()?-1:0);
        candidateCount.setText("일치 "+matchingCandidates.size()+"곳 · 표시 "+candidates.size()+"곳");
        candidateList.refresh();refreshMap(false);
    }
    private void refreshFacets() {
        if(subtypeBox==null)return;
        updatingFacets=true;
        try {
            var category=activityBox.getValue();
            if(java.util.Objects.equals(category,facetsActivity)) {
                rememberedSubtypes.put(category,subtypeBox.getValue());rememberedTags.put(category,tagBox.getValue());
            }
            var source=allPlaces.stream().filter(p->PlaceSearchService.categoryName(p.category()).equals(category)).toList();
            var subtypes=new java.util.ArrayList<String>();subtypes.add(null);
            subtypes.addAll(source.stream().map(PlaceFacets::subtype).distinct().sorted().toList());
            subtypeBox.getItems().setAll(subtypes);
            String remembered=rememberedSubtypes.get(category);
            subtypeBox.setValue(subtypes.contains(remembered)?remembered:null);
            var tags=new java.util.ArrayList<PlaceTag>();tags.add(null);
            tags.addAll(source.stream().flatMap(p->p.details().tags().stream()).map(t->t.tag()).distinct().sorted().toList());
            tagBox.getItems().setAll(tags);
            tagBox.setValue(tags.contains(rememberedTags.get(category))?rememberedTags.get(category):null);
            facetsActivity=category;
        } finally {updatingFacets=false;}
    }
    @FXML private void onCandidateDetail() { if (state == EditorState.CHOICE && candidate() != null) map.showDetail(candidate()); }
    @FXML private void onPlaceDetail() {
        Place selected = placeList.getSelectionModel().getSelectedItem();
        if (selected != null) map.showDetail(selected);
    }
    @FXML private void onReportPlace() {
        Place selected=placeList.getSelectionModel().getSelectedItem();
        if(selected!=null)onReport.accept(selected);
    }
    @FXML private void onFitRange() { map.fit(false); }
    @FXML private void onFitRoute() { map.fit(true); }

    public void fitRange() { refreshMap(true); }
    @FXML private void onWalkingRoute() { map.requestWalkingRoute(); }
    @FXML private void onWalkingExplanation() { map.toggleWalkingExplanation(); }
    @FXML private void onTmapSettings() {
        if (TmapSettingsDialog.show(editMapView.getScene().getWindow())) map.refreshWalkingPreferences();
    }

    private void refreshMap(boolean fit) {
        if (original != null) map.presentCandidates(draft(), state == EditorState.CHOICE
                        ? candidates.stream().map(PlaceSearchService.NearbyPlace::place).toList() : List.of(),
                state == EditorState.CHOICE ? candidate() : null, radius(), true, fit);
    }
}
