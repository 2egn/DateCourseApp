package com.datecourse.page.editor;

import com.datecourse.page.common.BasePageController;
import com.datecourse.page.common.NumberStepper;
import com.datecourse.page.common.PageFeedback;
import com.datecourse.page.common.TmapSettingsDialog;
import com.datecourse.page.common.map.CourseMapView;

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
public final class CourseEditorController extends BasePageController {
    private final com.datecourse.service.PlacePresenter placePresenter = new com.datecourse.service.PlacePresenter();
    @FXML private WebView editMapView;
    @FXML private ComboBox<String> activityBox;
    @FXML private ComboBox<String> subtypeBox;
    @FXML private Label priceFilterDescription;
    @FXML private ComboBox<String> detailTypeBox;
    @FXML private ComboBox<com.datecourse.model.PlacePriceFilter> priceFilterBox;
    @FXML private com.datecourse.ui.PlaceFeatureSelector featureSelector;
    @FXML private ComboBox<Integer> candidateLimitBox;
    @FXML private ComboBox<Integer> conditionCandidateLimitBox;
    @FXML private Label candidateStatus, candidateCount, priceWonLabel, emptyResultMessage;
    @FXML private VBox emptyResults;
    @FXML private Button includeUnknownRecovery, clearFacetsRecovery, expandRadiusRecovery, reviewPriceRecovery;
    private final com.datecourse.service.CandidateRecoveryService recovery=new com.datecourse.service.CandidateRecoveryService();
    @FXML private Button recommendNextButton;
    private List<PlaceSearchService.NearbyPlace> matchingCandidates = List.of();
    private boolean candidatesDirty;
    private long searchVersion;
    private boolean updatingFacets;
    private String facetsActivity;
    private record FacetSelection(String subtype,String detailType,java.util.Set<PlaceTag> tags,
                                  com.datecourse.model.PlacePriceFilter priceFilter) {}
    private final java.util.Map<String,FacetSelection> rememberedFacets = new java.util.HashMap<>();
    private Consumer<Place> onReport = ignored -> notifyUser(PageFeedback.Kind.INFO,"로그인 후 제보할 수 있어요.");
    @FunctionalInterface public interface PlaceRefresh { List<Place> apply(List<Place> places) throws Exception; }
    private PlaceRefresh placeRefresh;
    private record CandidateSearch(List<Place> source,List<PlaceSearchService.NearbyPlace> matches) {}

    /** 후보 검색 시 최신 장소 목록을 공급할 동작을 등록한다. */
    public void setPlaceRefresh(PlaceRefresh refresh) { placeRefresh = refresh; }

    /** 장소 정정 제보를 전달할 동작을 등록한다. */
    public void setOnReport(Consumer<Place> report) { onReport = report; }
    @FXML private CheckBox includeUncertainPrice;
    @FXML private TextField placePriceField;
    @FXML private Button priceUp, priceDown, finishButton;
    @FXML private Button upButton, downButton, removeButton;
    @FXML private Button placeInfoButton;
    @FXML private Button fitRouteButton, walkingSettingsButton, resetDraftButton;
    @FXML private TitledPane coursePane, conditionPane, choicePane;
    @FXML private com.datecourse.ui.ExpandableFilterPane filterPane;
    @FXML private VBox typeFilterSection, priceFilterSection;
    @FXML private VBox subtypeField, tagField;
    @FXML private ListView<PlaceSearchService.NearbyPlace> candidateList;
    @FXML private ScrollPane candidateScroll;
    @FXML private Slider radiusSlider;
    @FXML private Label metricTransportLabel, metricTimeLabel, metricPriceCaption, metricPriceLabel;
    @FXML private Label radiusLabel, summaryLabel, editorError, commonLabel;
    @FXML private TextField originSearchField;
    @FXML private Label walkingTimeLabel;
    @FXML private ComboBox<com.datecourse.model.TravelMode> travelModeBox;
    private boolean restoringTravelMode;
    /** 편집 코스의 이동수단을 반환하며 코스가 없으면 도보를 사용한다. */
    private com.datecourse.model.TravelMode travelMode(){return original==null?com.datecourse.model.TravelMode.WALKING:original.travelMode();}
    private com.datecourse.model.TravelRoute walkingRoute;
    private String walkingMessage="";
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
    private CourseMapView map;
    private Consumer<RoutePlan> onComplete = ignored -> {};
    private Runnable onCancel = () -> {};
    private enum EditorState { COURSE, CONDITION, CHOICE }
    private EditorState state = EditorState.CONDITION;
    private boolean changingState;

    /** 편집 화면의 표시·입력·지도·목록 연결을 순서대로 준비한다. */
    @FXML
    private void initialize() {
        configureVisibilityAndFilters();
        configureMapAndTravelMode();
        configureConditionInputs();
        configureCandidateList();
        registerConditionListeners();
        configureCourseList();
        configureEditorNavigation();
        updateButtons();
        setState(EditorState.CONDITION);
    }

    /** 조건 패널의 표시 규칙과 필터 구성을 연결한다. */
    private void configureVisibilityAndFilters() {
        watchNotice(editorError, PageFeedback.Kind.ERROR);
        coursePane.managedProperty().bind(coursePane.visibleProperty());
        fitRouteButton.managedProperty().bind(fitRouteButton.visibleProperty());
        walkingSettingsButton.managedProperty().bind(walkingSettingsButton.visibleProperty());
        resetDraftButton.managedProperty().bind(resetDraftButton.visibleProperty());
        selectedRadiusSlider.managedProperty().bind(selectedRadiusSlider.visibleProperty());
        filterPane.setOnClear(this::onClearAdvancedFilters);
        filterPane.setOnSearch(this::onRecommendNext);
        recommendNextButton.visibleProperty().bind(filterPane.expandedProperty().not());
        recommendNextButton.managedProperty().bind(recommendNextButton.visibleProperty());
        filterPane.setSections(List.of(
            new com.datecourse.ui.ExpandableFilterPane.Section("type","종류·업종","활동 안에서 원하는 종류와 세부 업종을 골라 주세요.",typeFilterSection,
                ()->(subtypeBox.getValue()==null?0:1)+(detailTypeBox.getValue()==null?0:1),com.datecourse.ui.ExpandableFilterPane.Tone.BLUE),
            new com.datecourse.ui.ExpandableFilterPane.Section("price","가격 기준","기본 가격 상한은 유지하면서 참고·추정·무료 조건을 추가해요.",priceFilterSection,
                ()->priceFilterBox.getValue()==null||priceFilterBox.getValue()==com.datecourse.model.PlacePriceFilter.ANY?0:1,com.datecourse.ui.ExpandableFilterPane.Tone.AMBER),
            new com.datecourse.ui.ExpandableFilterPane.Section("features","특징","확인된 특징만 제공해요. 여러 개 고르면 모두 충족해야 해요.",tagField,
                ()->featureSelector.selectedTags().size(),com.datecourse.ui.ExpandableFilterPane.Tone.GREEN)
        ));
        subtypeField.managedProperty().bind(subtypeField.visibleProperty());
        tagField.managedProperty().bind(tagField.visibleProperty());
    }

    /** 편집 지도와 이동수단 변경 이벤트를 연결한다. */
    private void configureMapAndTravelMode() {
        map = new CourseMapView(editMapView, (lat, lng) -> {}, editorError::setText);
        map.setAutomaticWalking(true);
        travelModeBox.getItems().setAll(com.datecourse.model.TravelMode.values());
        travelModeBox.setValue(com.datecourse.model.TravelMode.WALKING);
        travelModeBox.valueProperty().addListener((o,a,b)->{
            if(restoringTravelMode||original==null||b==null)return;
            original=original.withTravelMode(b);walkingRoute=null;
            refreshSummary();refreshMap(false);
        });
        map.setOnReport(place->onReport.accept(place));
        map.setWalkingResultListener((route,message)->{
            walkingRoute=route;walkingMessage=message;refreshWalkingSummary();placeList.refresh();
        });
        map.setLoadingUi(this::beginLoading);
        map.setOnPlaceSelected(this::focusCandidate);
        map.setOnAddCandidate(id->{if(!candidatesDirty&&focusCandidate(id))onAddCandidate();});
    }

    /** 조건 입력의 기본값과 가격·후보 수 바인딩을 설정한다. */
    private void configureConditionInputs() {
        activityBox.getItems().setAll("식사", "카페", "문화");
        activityBox.getSelectionModel().selectFirst();
        candidateLimitBox.getItems().setAll(NextPlaceCondition.DISPLAY_LIMITS);
        candidateLimitBox.setValue(NextPlaceCondition.DEFAULT_DISPLAY_LIMIT);
        conditionCandidateLimitBox.setItems(candidateLimitBox.getItems());
        conditionCandidateLimitBox.valueProperty().bindBidirectional(candidateLimitBox.valueProperty());
        installFacetLabels(subtypeBox,"전체 종류");
        installFacetLabels(detailTypeBox,"전체 업종");
        priceFilterBox.getItems().setAll(com.datecourse.model.PlacePriceFilter.values());
        priceFilterBox.setValue(com.datecourse.model.PlacePriceFilter.ANY);
        includeUncertainPrice.disableProperty().bind(priceFilterBox.valueProperty().isNotEqualTo(com.datecourse.model.PlacePriceFilter.ANY));
        priceFilterDescription.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(()->switch(priceFilterBox.getValue()){
            case ANY->"가격 상한과 '가격 미확인도 포함' 설정을 사용해요.";
            case REFERENCE->"추정 가격과 미확인은 제외해요. 참고 가격은 현재 메뉴의 검증을 뜻하지 않아요.";
            case ESTIMATED->"추정 금액이 있는 장소만 포함해요. 가격 상한은 계속 적용돼요.";
            case FREE->"금액이 0원으로 확인된 장소만 포함해요. 미확인은 무료로 처리하지 않아요.";
        },priceFilterBox.valueProperty()));
        priceFilterBox.setTooltip(new Tooltip("참고 가격은 추정값을 제외한 참고 정보예요. 현재 메뉴의 검증을 뜻하지 않아요. 무료만은 금액이 0원으로 확인된 장소만 포함해요."));
        candidateLimitBox.valueProperty().addListener((o,a,b)->{if(b!=null)displayCandidates();});
        NumberStepper.installPrice(placePriceField, priceUp, priceDown);
        priceWonLabel.textProperty().bind(javafx.beans.binding.Bindings.createStringBinding(()->{
            try{return String.format("%,d원 / 1인",NumberStepper.readWon(placePriceField));}
            catch(IllegalArgumentException invalid){return "금액을 확인해 주세요.";}
        },placePriceField.textProperty()));
        emptyResults.managedProperty().bind(emptyResults.visibleProperty());
        for(Button button:List.of(includeUnknownRecovery,clearFacetsRecovery,expandRadiusRecovery,reviewPriceRecovery))
            button.managedProperty().bind(button.visibleProperty());
        placePriceField.setTooltip(new Tooltip("만원 단위 · 예: 0.7 = 7,000원, 0.82 = 8,200원\n화살표·스크롤은 0.1만원(1,000원)씩 조절해요."));
        includeUncertainPrice.setTooltip(new Tooltip("금액 또는 상한을 모르는 장소도 표시해요. 이 경우 가격 상한 충족 여부를 보장할 수 없어요. 숫자가 있는 추정 가격은 기본적으로 계산에 활용해요."));
    }

    /** 추천 후보 목록의 표시 방식과 선택 동작을 설정한다. */
    private void configureCandidateList() {
        candidateList.setPlaceholder(new Label("조건을 정하고 추천받기를 눌러 주세요."));
        candidateList.setCellFactory(list -> new ListCell<>() {
            private final Label rank = new Label();
            private final Label name = new Label();
            private final Label meta = new Label();
            private final Label badges = new Label();
            private final HBox row;
            {
                rank.getStyleClass().add("candidate-rank");
                name.getStyleClass().add("section");
                meta.getStyleClass().add("planner-caption");
                badges.getStyleClass().add("planner-caption");
                VBox copy = new VBox(2, name, meta, badges);
                copy.setMinWidth(0);
                HBox.setHgrow(copy, Priority.ALWAYS);
                name.setMaxWidth(Double.MAX_VALUE);
                name.setMinWidth(0);badges.setMinWidth(0);meta.setMinWidth(0);
                row = new HBox(8, rank, copy);
                row.setAlignment(Pos.CENTER_LEFT);
                setOnMouseClicked(event -> {
                    if (!isEmpty() && event.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                        candidateList.getSelectionModel().select(getIndex());
                        onCandidateDetail();
                    }
                });
            }
            @Override protected void updateItem(PlaceSearchService.NearbyPlace nearby, boolean empty) {
                super.updateItem(nearby, empty);
                setText(null);
                if (empty || nearby == null) { setGraphic(null); return; }
                rank.setText(Integer.toString(getIndex() + 1));
                name.setText(nearby.place().name());
                name.setTooltip(new Tooltip(nearby.place().name()+"\n"+nearby.place().details().address()+"\n"+nearby.place().details().identityText()));
                meta.setText(String.format("%.0fm · %s", nearby.distanceMeters(), nearby.place().priceText()));
                var presentation=placePresenter.present(nearby.place());
                String reason=presentation.badges().stream().limit(2).map(com.datecourse.service.PlacePresenter.Badge::label)
                        .collect(java.util.stream.Collectors.joining(" · "));
                String type=placePresenter.candidateType(nearby.place());
                badges.setText(type+(reason.isBlank()?"":" · "+reason));
                badges.setTooltip(new Tooltip(type+"\n"+presentation.candidateDescription()));
                setAccessibleText(nearby.place().name() + " · "+type+" · 선택하면 상세 카드 열기");
                setGraphic(row);
            }
        });
        candidateList.getSelectionModel().selectedIndexProperty().addListener((obs, old, value) -> {
            candidateIndex = value.intValue();
            showCandidate();
        });
    }

    /** 조건 변경 시 후보와 검색 범위를 갱신하도록 연결한다. */
    private void registerConditionListeners() {
        activityBox.valueProperty().addListener((obs, old, value) -> {
            if(old!=null)rememberedFacets.put(old,currentFacets());
            refreshFacets();markCandidatesDirty();
        });
        subtypeBox.valueProperty().addListener((o,a,b)->{
            if(!updatingFacets){updatingFacets=true;try{refreshDetailTypes(false);}finally{updatingFacets=false;}markCandidatesDirty();}
        });
        detailTypeBox.valueProperty().addListener((o,a,b)->{if(!updatingFacets)markCandidatesDirty();});
        priceFilterBox.valueProperty().addListener((o,a,b)->{if(!updatingFacets)markCandidatesDirty();});
        featureSelector.selectedTags().addListener((javafx.collections.SetChangeListener<PlaceTag>)change->{if(!updatingFacets)markCandidatesDirty();});
        placePriceField.textProperty().addListener((obs, old, value) -> markCandidatesDirty());
        includeUncertainPrice.selectedProperty().addListener((obs, old, value) -> markCandidatesDirty());
        radiusSlider.valueProperty().addListener((obs, old, value) -> {
            updateRadiusLabel();
            markCandidatesDirty();
            refreshMap(false);
            map.ensureRangesVisible(radiusSlider.isValueChanging());
        });
        radiusSlider.valueChangingProperty().addListener((obs, old, adjusting) -> {
            if(!adjusting && original != null) map.ensureRangesVisible();
        });
        selectedRadiusSlider.valueChangingProperty().addListener((obs, old, adjusting) -> {
            if(!adjusting && !syncingSelectedRadius && original != null) map.ensureRangesVisible();
        });
        editorError.managedProperty().bind(editorError.textProperty().isNotEmpty());
        editorError.visibleProperty().bind(editorError.managedProperty());
        rangeWarning.managedProperty().bind(rangeWarning.textProperty().isNotEmpty());
        rangeWarning.visibleProperty().bind(rangeWarning.managedProperty());
    }

    /** 편집 중인 장소 목록과 구간 반경 조절을 연결한다. */
    private void configureCourseList() {
        placeList.setItems(draftPlaces);
        placeList.setPlaceholder(new Label("첫 장소를 추천받고 코스에 추가해 주세요."));
        placeList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Place place, boolean empty) {
                super.updateItem(place, empty);
                RouteRangeValidator.Leg leg = getIndex() >= 0 && getIndex() < legs.size() ? legs.get(getIndex()) : null;
                String distance = leg == null ? "" : String.format("\n직선 %.0fm",leg.distanceMeters());
                String walking=walkingDescription(getIndex());
                if(leg!=null&&!walking.isEmpty())walking=" · "+walking.substring(1);
                setText(empty || place == null ? null : (outsideRange.contains(place) ? "[범위 밖] " : "")
                        + (leg != null && leg.exceeded() ? "[이동 거리 초과] " : "")
                        + (getIndex() + 1) + ". " + place.name()
                        + " · " + PlaceSearchService.categoryName(place.category())
                        + "\n" + place.priceText() + " · " + (original == null ? 1 : original.people()) + "명 "
                        + CostEstimate.of(List.of(place), original == null ? 1 : original.people()).display() + distance
                        + walking);
            }
        });
        placeList.getSelectionModel().selectedIndexProperty().addListener((obs, old, value) -> {
            if (value.intValue() >= 0 && !changingState) setState(EditorState.COURSE);
            updateButtons();
            refreshMap(false);
            if (value.intValue() >= 0) {
                map.fitLeg(value.intValue());
            }
        });
        placeList.setOnMouseClicked(event -> { if (event.getClickCount() == 2) onPlaceDetail(); });
        selectedRadiusSlider.valueProperty().addListener((obs, old, value) -> {
            if (!syncingSelectedRadius) onUpdateSelectedRadius();
        });
    }

    /** 키보드 선택과 편집 패널 전환을 연결한다. */
    private void configureEditorNavigation() {
        candidateList.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) { onCandidateDetail(); event.consume(); }
        });
        wirePane(coursePane, EditorState.COURSE);
        wirePane(conditionPane, EditorState.CONDITION);
        wirePane(choicePane, EditorState.CHOICE);
    }


    /** 편집 화면에서 사용할 지도를 미리 불러온다. */
    public void prepareMap() { map.load(); }

    /** 진행 중인 후보 응답을 무효화하고 지도를 정리한다. */
    public void dispose() { searchVersion++;map.dispose(); }

    /** 화면 전환 시에도 후보와 조건을 유지하도록 기존 편집 폼을 재사용한다. */
    public void resume(RoutePlan context, List<Place> places) {
        boolean changed = original.startLat() != context.startLat() || original.startLng() != context.startLng()
                || original.people() != context.people() || original.radius() != context.radius();
        original = context;
        restoringTravelMode=true;travelModeBox.setValue(context.travelMode());restoringTravelMode=false;
        allPlaces = List.copyOf(places);
        refreshFacets();
        commonLabel.setText(String.format("%d명 · 출발지 중심 %dm 범위", context.people(), context.radius()));
        if (changed) clearCandidate();
        refreshSummary();
        refreshMap(false);
    }

    /** 현재 편집 내용을 독립적인 코스 객체로 반환한다. */
    public RoutePlan draftSnapshot() { return draft(); }

    /** 편집 초안을 비우고 완료 흐름으로 공통 조건 화면에 돌아간다. */
    @FXML private void onResetDraft() {
        if (original == null) return;
        resetDraft();
        onFinish();
    }

    /** 장소와 구간 반경·후보를 비우고 화면을 갱신한다. */
    void resetDraft() {
        draftPlaces.clear();
        arrivalRadii.clear();
        clearCandidate();
        refreshSummary();
        refreshMap(false);
        notifyUser(PageFeedback.Kind.INFO, "편집 내용을 비웠어요.");
    }

    /** 전달받은 코스로 새 편집을 시작하고 완료·취소 동작을 연결한다. */
    public void begin(RoutePlan snapshot, List<Place> places, Consumer<RoutePlan> complete, Runnable cancel) {
        original = snapshot;
        restoringTravelMode=true;travelModeBox.setValue(snapshot.travelMode());restoringTravelMode=false;
        allPlaces = List.copyOf(places);
        refreshFacets();
        onComplete = complete;
        onCancel = cancel;
        arrivalRadii.clear();
        arrivalRadii.putAll(snapshot.arrivalRadii());
        draftPlaces.setAll(snapshot.places());
        commonLabel.setText(String.format("%d명 · 출발지 중심 %dm 범위", snapshot.people(), snapshot.radius()));
        clearCandidate();
        refreshSummary();
        map.load();
        refreshMap(true);
    }

    /** 이전 오류를 지우고 편집 지도를 다시 불러온다. */
    @FXML private void onReloadMap() {
        editorError.setText("");
        map.reload();
    }

    /** 현재 장소 순서와 구간 반경으로 편집 코스를 만든다. */
    private RoutePlan draft() {
        return new RoutePlan(original.startLat(), original.startLng(), original.people(), original.radius(),
                original.title(), draftPlaces, arrivalRadii,original.travelMode());
    }

    /** 다음 장소 검색 반경을 100m 단위로 읽는다. */
    private int radius() { return (int) Math.round(radiusSlider.getValue() / 100) * 100; }

    /** 현재 조건으로 후보를 조회하고 최신 검색 결과만 반영한다. */
    @FXML
    private void onRecommendNext() {
        if (original == null) return;
        try {
            int price = NumberStepper.readWon(placePriceField);
            NextPlaceCondition condition=new NextPlaceCondition(activityBox.getValue(),price,radius(),includeUncertainPrice.isSelected(),
                subtypeBox.getValue()==null?java.util.Set.of():java.util.Set.of(subtypeBox.getValue()),
                java.util.Set.copyOf(featureSelector.selectedTags()),candidateLimitBox.getValue(),
                detailTypeBox.getValue()==null?java.util.Set.of():java.util.Set.of(detailTypeBox.getValue()),priceFilterBox.getValue());
            long version=++searchVersion;
            RoutePlan context=draft(); List<Place> source=allPlaces;
            candidatesDirty=true;map.setCandidateActionsEnabled(false);candidateList.refresh();candidateStatus.setText("후보 갱신 중…");
            Consumer<CandidateSearch> present=response->{
                if(version!=searchVersion)return;
                List<PlaceSearchService.NearbyPlace> result=response.matches();
                allPlaces=List.copyOf(response.source());
                extendFacets();
                matchingCandidates=result;candidatesDirty=false;displayCandidates();
                editorError.setText("");
                emptyResults.setVisible(result.isEmpty());
                if(result.isEmpty())showRecovery(recovery.analyze(response.source(),context,condition,(int)radiusSlider.getMax()));
                candidateStatus.setText(result.isEmpty()?"일치하는 장소가 없어요. 세부 조건·가격·반경을 조정해 주세요.":
                    result.stream().anyMatch(p->p.place().priceEstimate().estimated())?"추정 가격 기준 후보가 포함돼요.":"조건 적용됨");
                setState(result.isEmpty()?EditorState.CONDITION:EditorState.CHOICE);
            };
            if(placeRefresh==null)present.accept(new CandidateSearch(source,search.matching(source,context.places(),context.startLat(),context.startLng(),context.people(),context.radius(),condition)));
            else runWork("후보를 찾고 있어요",()-> {
                List<Place> fresh=placeRefresh.apply(source);
                return new CandidateSearch(fresh,search.matching(fresh,context.places(),context.startLat(),context.startLng(),context.people(),context.radius(),condition));
            },present,failure->{if(version==searchVersion){markCandidatesDirty();editorError.setText("후보 갱신에 실패했어요. 다시 시도해 주세요.");}});
        } catch (IllegalArgumentException invalid) {
            clearCandidate();
            editorError.setText(invalid.getMessage());
        }
    }

    /** 검색 결과가 없을 때 적용 가능한 조건 완화 방법을 표시한다. */
    private void showRecovery(com.datecourse.service.CandidateRecoveryService.Advice advice) {
        emptyResultMessage.setText(advice.message());
        for(Button button:List.of(includeUnknownRecovery,clearFacetsRecovery,expandRadiusRecovery,reviewPriceRecovery))button.setVisible(false);
        for(var suggestion:advice.suggestions()) {
            Button button=switch(suggestion.change()) {
                case INCLUDE_UNKNOWN->includeUnknownRecovery;case CLEAR_FACETS->clearFacetsRecovery;
                case EXPAND_RADIUS->expandRadiusRecovery;case REVIEW_PRICE->reviewPriceRecovery;
            };
            String label=switch(suggestion.change()) {
                case INCLUDE_UNKNOWN->"가격 미확인 포함 · "+suggestion.matches()+"곳";
                case CLEAR_FACETS->"세부 조건 해제 · "+suggestion.matches()+"곳";
                case EXPAND_RADIUS->"반경 "+(int)radiusSlider.getMax()+"m로 검색 · "+suggestion.matches()+"곳";
                case REVIEW_PRICE->"가격 상한 수정";
            };
            button.setText(label);button.setVisible(true);
        }
    }

    /** 가격 미확인 장소를 포함해 후보를 다시 검색한다. */
    @FXML private void onIncludeUnknownRecovery(){includeUncertainPrice.setSelected(true);onRecommendNext();}

    /** 세부 필터를 비우고 후보를 다시 검색한다. */
    @FXML private void onClearFacetsRecovery(){onClearAdvancedFilters();onRecommendNext();}

    /** 허용되는 최대 반경으로 후보를 다시 검색한다. */
    @FXML private void onExpandRadiusRecovery(){radiusSlider.setValue(radiusSlider.getMax());onRecommendNext();}

    /** 가격 상한을 수정할 수 있도록 입력칸을 선택한다. */
    @FXML private void onReviewPriceRecovery(){placePriceField.requestFocus();placePriceField.selectAll();}

    /** 선택된 추천 후보를 반환하며 선택이 없으면 null을 반환한다. */
    private Place candidate() { return candidateIndex < 0 || candidateIndex >= candidates.size() ? null : candidates.get(candidateIndex).place(); }

    /** 후보 선택에 맞춰 지도와 버튼 상태를 갱신한다. */
    private void showCandidate() {
        refreshMap(false);
    }

    /** 후보 선택과 검색 결과를 초기 상태로 되돌린다. */
    private void clearCandidate() {
        searchVersion++;
        emptyResults.setVisible(false);
        matchingCandidates=List.of();candidatesDirty=false;
        candidates = List.of();
        candidateIndex = -1;
        candidateList.getItems().clear();
        candidateCount.setText("추천 전");candidateStatus.setText("조건을 정하고 후보를 찾아 주세요.");
        recommendNextButton.setText("후보 검색");
        setState(EditorState.CONDITION);
        editorError.setText("");
    }

    /** 선택한 후보를 코스에 추가하고 변경 내용을 반영한다. */
    @FXML
    private void onAddCandidate() {
        if (state != EditorState.CHOICE || candidatesDirty) return;
        Place place = candidate();
        if (place == null) return;
        arrivalRadii.put(place.recordId(), radius());
        draftPlaces.add(place);
        placeList.getSelectionModel().selectLast();
        changed(false);
        map.fitLeg(draftPlaces.size()-1);
    }

    /** 코스 변경 후 후보·요약·지도와 편집 상태를 갱신한다. */
    private void changed(boolean editingCourse) {
        int selected = placeList.getSelectionModel().getSelectedIndex();
        clearCandidate();
        if (editingCourse && !draftPlaces.isEmpty()) {
            placeList.getSelectionModel().select(Math.max(0, Math.min(selected, draftPlaces.size() - 1)));
            setState(EditorState.COURSE);
        }
        refreshSummary();
        refreshMap(false);
    }

    /** 편집 코스의 비용·이동수단·범위 요약을 갱신한다. */
    private void refreshSummary() {
        if (original == null) return;
        updateRadiusLimit();
        RoutePlan plan = draft();
        summaryLabel.setText(String.format("%d곳 · %d명 · %s",
                draftPlaces.size(), plan.people(), plan.costEstimate().display()));
        if (metricTransportLabel != null) metricTransportLabel.setText(travelMode().label());
        if (metricPriceCaption != null) metricPriceCaption.setText(String.format("총 예상 가격 (%d곳 · %d명)", draftPlaces.size(), plan.people()));
        if (metricPriceLabel != null) metricPriceLabel.setText(plan.costEstimate().display());
        coursePane.setText("내 코스 보기 · " + draftPlaces.size() + "곳");
        updateButtons();
        placeList.refresh();
        refreshWalkingSummary();
        if (!outsideRange.isEmpty() || !exceededLegs.isEmpty()) setState(EditorState.COURSE);
    }

    /** 전체 범위를 고려해 다음 장소의 최대 검색 반경을 제한한다. */
    private void updateRadiusLimit() {
        int maximum = com.datecourse.service.NextPlaceRadius.maximum(draft());
        radiusSlider.setMax(maximum);
        radiusSlider.setValue(Math.min(radiusSlider.getValue(), maximum));
        radiusSlider.setDisable(maximum == (int) radiusSlider.getMin());
        updateRadiusLabel();
        radiusSlider.setTooltip(new Tooltip("직전 장소(첫 장소는 출발지) 기준 직선 반경이에요.\n전체 범위를 덮는 반경까지, 현재 이동 방법과 전체 범위에 맞춰 조절해요."));
    }

    /** 현재 선택된 검색 반경을 안내 문구에 표시한다. */
    private void updateRadiusLabel() {
        radiusLabel.setText("직선 반경 · " + radius() + "m (최대 " + (int) radiusSlider.getMax() + "m)");
    }

    /** 초 단위 이동시간을 분 단위 문구로 바꾼다. */
    private static String minutes(long seconds) {
        return com.datecourse.model.TravelRoute.durationText(seconds);
    }

    /** 장소로 이동하는 해당 구간의 시간 안내를 만든다. */
    private String walkingDescription(int index) {
        if(index<0||index>=draftPlaces.size())return "";
        if(walkingRoute==null||walkingRoute.legs().size()!=draftPlaces.size())return "\n"+travelMode().label()+" 시간 미확인";
        long total=0;for(int i=0;i<=index;i++)total=Math.addExact(total,walkingRoute.legs().get(i).time());
        var leg=walkingRoute.legs().get(index);
        String movement=leg.movementSummary();
        return "\n"+(movement.isBlank()?travelMode().label()+" "+minutes(leg.time()):movement+" · 합계 "+minutes(leg.time()))+" · 누적 "+minutes(total);
    }

    /** 경로 결과와 조회 상태에 맞춰 총 이동시간을 표시한다. */
    private void refreshWalkingSummary() {
        if(draftPlaces.isEmpty()){
            walkingTimeLabel.setText("장소를 추가하면 "+travelMode().label()+" 시간을 계산해요.");
            if (metricTimeLabel != null) metricTimeLabel.setText("0분");
            return;
        }
        if(walkingRoute!=null&&walkingRoute.legs().size()==draftPlaces.size()) {
            long last=walkingRoute.legs().getLast().time();
            walkingTimeLabel.setText("최근 이동 "+minutes(last)+" · 누적 "+travelMode().label()+" "+minutes(walkingRoute.totalTime())+" · "+travelMode().timeNote()+(walkingRoute.provider().isBlank()?"":" · 경로 "+walkingRoute.provider()));
            if (metricTimeLabel != null) metricTimeLabel.setText("총 " + minutes(walkingRoute.totalTime()));
        } else {
            boolean loading = walkingMessage.contains("중")||walkingMessage.contains("준비");
            walkingTimeLabel.setText(loading ? travelMode().label()+" 이동시간 조회 중…" : travelMode().label()+" 시간 미확인"+(walkingMessage.isBlank()?"":" · "+walkingMessage));
            if (metricTimeLabel != null) metricTimeLabel.setText(loading ? "계산 중…" : "미확인");
        }
    }

    /** 선택된 장소와 후보·조회 상태에 맞춰 편집 버튼을 갱신한다. */
    private void updateButtons() {
        boolean hasPlaces=!draftPlaces.isEmpty();
        coursePane.setVisible(hasPlaces);fitRouteButton.setVisible(hasPlaces);walkingSettingsButton.setVisible(hasPlaces&&travelMode()==com.datecourse.model.TravelMode.WALKING);
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
        finishButton.setDisable(original == null || !hasPlaces&&original.places().isEmpty() || !outsideRange.isEmpty() || !exceededLegs.isEmpty());
        syncSelectedRadius();
    }

    /** 선택한 구간의 저장 반경을 조절 슬라이더에 반영한다. */
    private void syncSelectedRadius() {
        int index = placeList.getSelectionModel().getSelectedIndex();
        boolean selected = index >= 0 && index < legs.size();
        syncingSelectedRadius = true;
        try {
            selectedRadiusSlider.setDisable(!selected);
            selectedRadiusSlider.setVisible(selected);
            RouteRangeValidator.Leg leg = selected ? legs.get(index) : null;
            int maximum=selected?com.datecourse.service.NextPlaceRadius.maximum(draft(),index):travelMode().maximumLegRadius();
            selectedRadiusSlider.setMax(Math.max(maximum,leg==null||leg.radius()==null?0:leg.radius()));
            selectedRadiusSlider.setValue(leg == null || leg.radius() == null ? Math.min(500,maximum) : leg.radius());
            boolean missing = leg != null && leg.radius() == null;
            recordRadiusButton.setVisible(missing);
            recordRadiusButton.setManaged(missing);
            selectedRadiusLabel.setText(leg == null ? "장소를 선택하면 추가 당시의 ‘다음 장소의 조건’ 반경을 수정할 수 있어요."
                    : String.format("%s → %s · 직선 %.0fm · 설정 반경 %s\n이 장소를 추가할 때 ‘다음 장소의 조건’에서 정한 반경이에요. 아래에서 이 구간의 반경만 수정해요.",
                    leg.from(), leg.to().name(), leg.distanceMeters(),
                    leg.radius() == null ? "미기록 (조절하거나 500m로 기록)" : leg.radius() + "m"));
        } finally { syncingSelectedRadius = false; }
    }

    /** 선택한 구간의 반경 변경을 초안과 지도에 반영한다. */
    @FXML private void onUpdateSelectedRadius() {
        Place selected = placeList.getSelectionModel().getSelectedItem();
        if (selected == null || original == null) return;
        int limit = (int) Math.round(selectedRadiusSlider.getValue() / 100) * 100;
        arrivalRadii.put(selected.recordId(), limit);
        refreshSummary();
        refreshMap(false);
        map.ensureRangesVisible(selectedRadiusSlider.isValueChanging());
    }

    /** 선택한 장소를 방문 순서에서 한 칸 앞으로 옮긴다. */
    @FXML private void onMoveUp() { move(-1); }

    /** 선택한 장소를 방문 순서에서 한 칸 뒤로 옮긴다. */
    @FXML private void onMoveDown() { move(1); }

    /** 선택한 장소를 지정한 방향으로 옮기고 코스를 갱신한다. */
    private void move(int direction) {
        int from = placeList.getSelectionModel().getSelectedIndex();
        int to = from + direction;
        if (from < 0 || to < 0 || to >= draftPlaces.size()) return;
        Collections.swap(draftPlaces, from, to);
        placeList.getSelectionModel().select(to);
        changed(true);
    }

    /** 선택한 장소와 해당 구간 반경을 초안에서 제거한다. */
    @FXML private void onRemove() {
        int index = placeList.getSelectionModel().getSelectedIndex();
        if (index >= 0) {
            arrivalRadii.remove(draftPlaces.get(index).recordId());
            draftPlaces.remove(index);
            changed(true);
        }
    }

    /** 코스 범위를 검증하고 편집 결과를 완료 처리기에 전달한다. */
    @FXML private void onFinish() {
        if (original == null) return;
        updateButtons(); // 버튼 상태뿐 아니라 완료 콜백의 실행 조건도 확인한다.
        if (!outsideRange.isEmpty() || !exceededLegs.isEmpty()) { setState(EditorState.COURSE); return; }
        onComplete.accept(draft());
    }

    /** 편집 초안을 유지한 채 취소 동작을 실행한다. */
    @FXML private void onCancel() { onCancel.run(); }

    /** 다음 장소의 조건 패널로 전환한다. */
    @FXML private void onModifyConditions() { setState(EditorState.CONDITION); }

    /** 하나의 편집 패널만 열고 해당 상태에 맞춰 지도를 갱신한다. */
    private void setState(EditorState requested) {
        resetDraftButton.setVisible(!draftPlaces.isEmpty()||!candidates.isEmpty());
        state = requested == EditorState.CHOICE && candidates.isEmpty()
                || requested == EditorState.COURSE && draftPlaces.isEmpty() ? EditorState.CONDITION : requested;
        if (state == EditorState.CONDITION) placeList.getSelectionModel().clearSelection();
        changingState = true;
        try {
            coursePane.setExpanded(state == EditorState.COURSE);
            conditionPane.setExpanded(state == EditorState.CONDITION);
            choicePane.setExpanded(state == EditorState.CHOICE);
            choicePane.setDisable(candidates.isEmpty());
            choicePane.setText(candidates.isEmpty() ? "추천 후보" : "추천 후보 · " + candidates.size() + "곳");
            conditionPane.setText("다음 장소의 조건");
        } finally { changingState = false; }
        refreshMap(false);
    }

    /** 패널 전환과 열린 패널을 유지하는 클릭 동작을 연결한다. */
    private void wirePane(TitledPane pane, EditorState target) {
        pane.getChildrenUnmodifiable().addListener((javafx.collections.ListChangeListener<javafx.scene.Node>) change -> {
            while (change.next()) for (javafx.scene.Node child : change.getAddedSubList()) {
                if (!child.getStyleClass().contains("title")) continue;
                // 스킨이 교체되어도 실제로 붙은 제목 노드의 커서를 관리한다.
                child.cursorProperty().addListener((property, previous, cursor) -> {
                    if (pane.isExpanded() && cursor != javafx.scene.Cursor.DEFAULT)
                        child.setCursor(javafx.scene.Cursor.DEFAULT);
                });
                child.setCursor(pane.isExpanded() ? javafx.scene.Cursor.DEFAULT : javafx.scene.Cursor.HAND);
            }
        });
        pane.expandedProperty().addListener((observable, old, expanded) -> {
            updatePaneCursor(pane);
            if (changingState) return;
            if (expanded) setState(target);
            else if (state == target) {
                changingState = true;
                try { pane.setExpanded(true); } finally { changingState = false; }
            }
        });
    }

    /** 열린 패널과 닫힌 패널의 제목 커서를 구분한다. */
    private void updatePaneCursor(TitledPane pane) {
        pane.getChildrenUnmodifiable().stream().filter(node -> node.getStyleClass().contains("title"))
                .forEach(title -> title.setCursor(pane.isExpanded() ? javafx.scene.Cursor.DEFAULT : javafx.scene.Cursor.HAND));
    }

    /** 조건 변경으로 이전 후보를 무효화하고 재검색 필요 상태를 표시한다. */
    private void markCandidatesDirty() {
        updateFacetSummary();
        emptyResults.setVisible(false);
        searchVersion++; candidatesDirty=true;
        map.setCandidateActionsEnabled(false);
        candidateStatus.setText(matchingCandidates.isEmpty()?"조건을 적용해 후보를 찾아 주세요.":"조건 변경됨 · 후보 업데이트 필요");
        candidateList.refresh();
    }

    /** 표시 개수 제한을 적용하고 가능한 경우 기존 후보 선택을 유지한다. */
    private void displayCandidates() {
        if(candidateLimitBox.getValue()==null)return;
        Place selected=candidate();
        candidates=matchingCandidates.stream().limit(candidateLimitBox.getValue()).toList();
        candidateIndex=-1;candidateList.getItems().setAll(candidates);
        int index=selected==null?-1:java.util.stream.IntStream.range(0,candidates.size())
            .filter(i->candidates.get(i).place().sameIdentityAs(selected)).findFirst().orElse(-1);
        candidateList.getSelectionModel().select(index>=0?index:candidates.isEmpty()?-1:0);
        candidateCount.setText("일치 "+matchingCandidates.size()+"곳 · 표시 "+candidates.size()+"곳");
        recommendNextButton.setText(candidates.isEmpty() ? "후보 검색" : "후보 업데이트");
        candidateList.refresh();refreshMap(false);
    }

    /** 종류·업종 선택의 전체 항목 안내를 설정한다. */
    private void installFacetLabels(ComboBox<String> box,String all) {
        box.setPromptText(all);
        box.setCellFactory(v->new ListCell<>(){@Override protected void updateItem(String text,boolean empty){
            super.updateItem(text,empty);setText(empty?null:text==null?all:text);
        }});
        box.setButtonCell(new ListCell<>(){@Override protected void updateItem(String text,boolean empty){
            super.updateItem(text,empty);setText(text==null?all:text);
        }});
    }

    /** 현재 선택된 세부 필터를 묶어 반환한다. */
    private FacetSelection currentFacets(){return new FacetSelection(subtypeBox.getValue(),detailTypeBox.getValue(),
            java.util.Set.copyOf(featureSelector.selectedTags()),priceFilterBox.getValue());}

    /** 현재 활동에 해당하는 장소만 추린다. */
    private List<Place> activityPlaces(){return allPlaces.stream().filter(p->PlaceSearchService.categoryName(p.category()).equals(activityBox.getValue())).toList();}

    /** 활동별로 기억한 필터 선택을 복원하며 선택지를 갱신한다. */
    private void refreshFacets() {
        if(subtypeBox==null)return;
        FacetSelection selection=java.util.Objects.equals(activityBox.getValue(),facetsActivity)?currentFacets():
                rememberedFacets.getOrDefault(activityBox.getValue(),new FacetSelection(null,null,java.util.Set.of(),com.datecourse.model.PlacePriceFilter.ANY));
        populateFacets(selection);facetsActivity=activityBox.getValue();
    }

    /** 장소 데이터에서 필터 선택지를 만들고 지정한 선택을 반영한다. */
    private void populateFacets(FacetSelection selection) {
        updatingFacets=true;
        try {
            var source=activityPlaces();
            var types=new java.util.ArrayList<String>();types.add(null);
            types.addAll(source.stream().map(PlaceFacets::subtype).distinct().sorted().toList());
            if(selection.subtype()!=null&&!types.contains(selection.subtype()))types.add(selection.subtype());
            subtypeBox.getItems().setAll(types);subtypeBox.setValue(selection.subtype());
            detailTypeBox.setValue(selection.detailType());refreshDetailTypes(true);
            priceFilterBox.setValue(selection.priceFilter());
            featureSelector.configure(source,selection.tags());
            updateFacetVisibility();
        } finally {updatingFacets=false;}
    }

    /** 선택한 종류에 맞는 세부 업종 목록을 갱신한다. */
    private void refreshDetailTypes(boolean retainMissing) {
        String selected=detailTypeBox.getValue();
        var types=new java.util.ArrayList<String>();types.add(null);
        types.addAll(activityPlaces().stream().filter(p->subtypeBox.getValue()==null||subtypeBox.getValue().equals(PlaceFacets.subtype(p)))
                .map(PlaceFacets::detailType).distinct().sorted().toList());
        if(retainMissing&&selected!=null&&!types.contains(selected))types.add(selected);
        detailTypeBox.getItems().setAll(types);detailTypeBox.setValue(types.contains(selected)?selected:null);
    }

    /** 적용된 조건을 넓히지 않고 필터 선택지와 개수를 갱신한다. */
    private void extendFacets(){populateFacets(currentFacets());}

    /** 사용 가능한 필터의 표시 여부와 선택 요약을 갱신한다. */
    private void updateFacetVisibility() {
        subtypeField.setVisible(subtypeBox.getItems().size()>2);
        tagField.setVisible(true);updateFacetSummary();
    }

    /** 선택한 필터를 개별 해제 가능한 요약 항목으로 표시한다. */
    private void updateFacetSummary() {
        var selections=new java.util.ArrayList<com.datecourse.ui.ExpandableFilterPane.Selection>();
        if(subtypeBox.getValue()!=null)selections.add(new com.datecourse.ui.ExpandableFilterPane.Selection("type","종류: "+subtypeBox.getValue(),()->subtypeBox.setValue(null),"type"));
        if(detailTypeBox.getValue()!=null)selections.add(new com.datecourse.ui.ExpandableFilterPane.Selection("detail","업종: "+detailTypeBox.getValue(),()->detailTypeBox.setValue(null),"type"));
        if(priceFilterBox.getValue()!=null&&priceFilterBox.getValue()!=com.datecourse.model.PlacePriceFilter.ANY)
            selections.add(new com.datecourse.ui.ExpandableFilterPane.Selection("price",priceFilterBox.getValue().toString(),()->priceFilterBox.setValue(com.datecourse.model.PlacePriceFilter.ANY),"price"));
        featureSelector.selectedTags().stream().sorted().forEach(tag->selections.add(new com.datecourse.ui.ExpandableFilterPane.Selection("feature:"+tag.name(),tag.label(),()->featureSelector.selectedTags().remove(tag),"features")));
        filterPane.setSelections(selections);
    }

    /** 기본 조건을 유지하고 세부 필터만 초기화한다. */
    @FXML private void onClearAdvancedFilters() {
        updatingFacets=true;
        try {subtypeBox.setValue(null);detailTypeBox.setValue(null);priceFilterBox.setValue(com.datecourse.model.PlacePriceFilter.ANY);
            featureSelector.selectedTags().clear();refreshDetailTypes(false);
        } finally {updatingFacets=false;}
        markCandidatesDirty();
    }

    /** 추천 후보 패널에서 선택한 장소의 상세 카드를 연다. */
    @FXML private void onCandidateDetail() { if (state == EditorState.CHOICE && candidate() != null) map.showDetail(candidate()); }

    /** 내 코스에서 선택한 장소의 상세 카드를 연다. */
    @FXML private void onPlaceDetail() {
        Place selected = placeList.getSelectionModel().getSelectedItem();
        if (selected != null) map.showDetail(selected);
    }

    /** 현재 검색 범위가 보이도록 지도를 맞춘다. */
    @FXML private void onFitRange() { map.fit(false); }

    /** 현재 이동 경로가 보이도록 지도를 맞춘다. */
    @FXML private void onFitRoute() { map.fit(true); }

    /** 편집 코스를 반영하고 검색 범위에 맞춰 지도를 갱신한다. */
    public void fitRange() { refreshMap(true); }

    /** 현재 코스의 이동 경로를 직접 조회한다. */
    @FXML private void onWalkingRoute() { map.requestWalkingRoute(); }

    /** 이동 경로의 상세 안내 표시를 전환한다. */
    @FXML private void onWalkingExplanation() { map.toggleWalkingExplanation(); }

    /** 도보 경로 설정 창을 열고 변경되면 경로를 다시 조회한다. */
    @FXML private void onTmapSettings() {
        if (TmapSettingsDialog.show(editMapView.getScene().getWindow())) map.refreshWalkingPreferences();
    }

    /** 편집 상태와 선택 구간·후보를 지도에 반영한다. */
    private void refreshMap(boolean fit) {
        map.setCandidateActionsEnabled(!candidatesDirty);
        int index = placeList.getSelectionModel().getSelectedIndex();
        boolean editingLeg = state == EditorState.COURSE && index >= 0 && index < draftPlaces.size();
        map.setEditingLeg(editingLeg ? index : null, editingLeg ? (int)Math.round(selectedRadiusSlider.getValue()/100)*100 : null);
        if (original != null) map.presentCandidates(draft(), state == EditorState.CHOICE
                        ? candidates.stream().map(PlaceSearchService.NearbyPlace::place).toList() : List.of(),
                state == EditorState.CHOICE ? candidate() : null, radius(), true, fit);
    }

    /** 지도의 장소 선택에 맞춰 후보 목록을 선택하고 해당 항목을 보여준다. */
    private boolean focusCandidate(long id) {
        for(int i=0;i<candidates.size();i++)if(candidates.get(i).place().recordId()==id) {
            setState(EditorState.CHOICE);
            candidateList.getSelectionModel().select(i);
            candidateList.getFocusModel().focus(i);candidateList.scrollTo(i);candidateList.requestFocus();
            double overflow=candidateScroll.getContent().getBoundsInLocal().getHeight()-candidateScroll.getViewportBounds().getHeight();
            double top=choicePane.getBoundsInParent().getMinY();
            double start=candidateScroll.getVvalue()*Math.max(0,overflow);
            if(overflow>0&&(top<start||choicePane.getBoundsInParent().getMaxY()>start+candidateScroll.getViewportBounds().getHeight()))
                candidateScroll.setVvalue(Math.max(0,Math.min(1,top/overflow)));
            return true;
        }
        return false;
    }

}
