package com.datecourse.page._03step_saved;

import com.datecourse.page.common.BasePageController;
import com.datecourse.page.common.PageFeedback;
import com.datecourse.page.common.map.CourseMapView;

import com.datecourse.model.SavedCourse;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import com.datecourse.service.UserContext;
import com.datecourse.db.SavedCourseRepository;
import java.util.function.Consumer;

/**
 * 저장한 코스 화면 컨트롤러 (saved-course-view.fxml과 연결).
 * 목록과 상세 화면은 플래너에 전달할 동일한 저장 코스를 사용한다.
 *
 * <p>구성 순서 (FXML과 동일):
 * <ol>
 *   <li>필드: FXML 컴포넌트(왼쪽 목록 → 가운데 상세) → 내부 상태</li>
 *   <li>초기화: initialize(), configure(), clear()</li>
 *   <li>버튼 동작: FXML 위에서 아래 순서대로</li>
 *   <li>화면 갱신: 코스 목록 / 상세 / 지도 / 버튼 상태</li>
 *   <li>보조 메서드</li>
 * </ol>
 */
public class SavedCourseController extends BasePageController {

    // =====================================================================
    // 1. 필드
    // =====================================================================

    // ---------- FXML 컴포넌트 (fxml 위에서 아래 순서) ----------

    // 전체 화면 루트
    @FXML private javafx.scene.layout.BorderPane savedCourseRoot;

    // [왼쪽] 코스 목록
    @FXML private javafx.scene.layout.VBox courseListPane;
    @FXML private ListView<SavedCourse> courseList;
    @FXML private Label emptyLabel;

    // [가운데] 상세 제목
    @FXML private Label detailTitle;

    // [가운데] 지도
    @FXML private javafx.scene.layout.StackPane mapPane;
    @FXML private javafx.scene.web.WebView previewMap;
    @FXML private javafx.scene.layout.HBox mapNotice;
    @FXML private Label mapStatus;
    @FXML private Button mapRetry, toggleMapButton, requestRouteButton;
    @FXML private javafx.scene.image.ImageView coursePreview;
    @FXML private Label previewCaption, travelSummaryLabel, mapLoadDescription;
    @FXML private Button imageViewButton, mapLoadButton;
    @FXML private javafx.scene.layout.HBox previewViewControls;
    @FXML private javafx.scene.layout.VBox mapLoadPrompt;

    // [가운데] 하단 정보 표
    @FXML private javafx.scene.layout.GridPane bottomInfo;
    @FXML private ScrollPane bottomScroll;
    @FXML private TableView<CourseDetailRow> summaryTable;
    @FXML private TableColumn<CourseDetailRow, String> summaryLabelColumn, summaryValueColumn;
    @FXML private TableView<PlaceInfoRow> placeTable;
    @FXML private TableColumn<PlaceInfoRow, String> placeOrderColumn, placeNameColumn, placePriceColumn, placeAddressColumn, placeTravelColumn;

    // [가운데] 안내 메시지
    @FXML private Label messageLabel;

    // [가운데] 버튼
    @FXML private Button openButton, refreshButton, renameButton, deleteButton;

    // ---------- 내부 상태 (FXML과 무관) ----------

    private final com.datecourse.service.SavedCoursePresenter presenter = new com.datecourse.service.SavedCoursePresenter();
    private CourseMapView map;                        // 지도 제어
    private SavedCourseRepository repository;      // 코스 저장소(DB)
    private UserContext userContext;               // 로그인한 사용자 정보
    private Consumer<SavedCourse> onOpen;          // "이 코스 불러오기" 눌렀을 때 실행할 동작
    private Runnable onNewCourse;                  // "새 코스" 눌렀을 때 실행할 동작
    private boolean busy;                          // 처리 중이면 true (버튼 중복 클릭 방지)
    private long viewVersion;                      // 목록 불러오기 요청 번호 (오래된 응답 무시용)
    private long previewVersion;                   // 지도 미리보기 요청 번호 (오래된 응답 무시용)

    private SavedCourse previewCourse;
    private com.datecourse.model.Place pendingFocusPlace;
    private long previewLoadVersion;
    private boolean showingMap, selectedPreviewView, storedPreview, routeRequested, mapSelected, mapPrepared, previewLoading;
    private record RouteSummary(com.datecourse.model.TravelRoute route, long preferencesVersion) {}
    private final java.util.Map<SavedCourse,RouteSummary> routeSummaries=new java.util.HashMap<>();
    private java.util.concurrent.CompletableFuture<PreviewImage> previewTask;
    private record PreviewImage(javafx.scene.image.Image image, boolean stored) {}
    private String mapFailure = "", routeFailure = "", routeMessage = "";

    // 표의 한 줄을 나타내는 데이터
    private record CourseDetailRow(String label, String value) {}
    private record PlaceInfoRow(String order, String name, String price, String address, String travel) {}

    // =====================================================================
    // 2. 초기화
    // =====================================================================

    /** 화면이 만들어질 때 한 번 실행됨. FXML 순서대로 각 영역을 설정한다. */
    @FXML public void initialize() {
        configureCourseList();
        configurePreviewMap();
        configureDetailTables();
        configureResponsiveDetails();
        configureMessages();
        // ----- 초기 화면 상태 -----
        showDetail(null);                       // 아무것도 선택하지 않은 상태로 시작
        summaryTable.setSelectionModel(null);   // 표는 줄 선택 불가 (읽기 전용)
    }

    /** 저장한 코스 목록의 폭과 재사용 셀, 선택 동작을 설정한다. */
    private void configureCourseList() {
        // ----- [왼쪽] 코스 목록 영역 -----
        // 창 너비의 28%로 목록 폭을 조절 (240~420 사이)
        savedCourseRoot.widthProperty().addListener((obs, oldWidth, newWidth) ->
                courseListPane.setPrefWidth(Math.clamp(newWidth.doubleValue() * 0.28, 240, 420)));
        courseListPane.setPrefWidth(Math.clamp(savedCourseRoot.getWidth() * 0.28, 240, 420));

        // 목록 한 칸(셀)의 모양
        courseList.setCellFactory(list -> new ListCell<>() {
            // 셀마다 한 번만 만들어서 재사용 (updateItem이 불릴 때마다 새로 만들지 않음)
            private final Label titleLabel = new Label();
            private final Label summaryLabel = new Label();
            private final Label updatedLabel = new Label();
            private final Label travelLabel = new Label();
            private final javafx.scene.layout.VBox box =
                    new javafx.scene.layout.VBox(3, titleLabel, travelLabel, summaryLabel, updatedLabel);

            {   // 초기화 블록: 셀이 만들어질 때 한 번 실행됨
                titleLabel.getStyleClass().add("saved-course-title");
                summaryLabel.getStyleClass().add("saved-course-summary");
                updatedLabel.getStyleClass().add("saved-course-updated");
                for (Label label : java.util.List.of(titleLabel, travelLabel, summaryLabel, updatedLabel)) {
                    label.setWrapText(true);                 // 길면 줄바꿈
                    label.setMaxWidth(Double.MAX_VALUE);
                }
                setPrefWidth(0);   // 셀이 내용 때문에 목록보다 넓어지지 않게 함
                // 안쪽 박스 폭을 셀 폭에 묶음 (좌우 padding 12*2 만큼 뺌)
                box.prefWidthProperty().bind(widthProperty().subtract(24));
                box.maxWidthProperty().bind(widthProperty().subtract(24));
            }

            // 셀에 코스 내용을 채움 (빈 칸이면 비움)
            @Override protected void updateItem(SavedCourse course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                titleLabel.setText(title(course));
                travelLabel.setText(travelText(course)); travelLabel.getStyleClass().setAll("saved-course-travel");
                summaryLabel.setText(course.plan().places().size() + "곳 · " + presenter.costSummary(course));
                updatedLabel.setText("최근 수정 " + updatedText(course));
                setText(null);
                setGraphic(box);
            }
        });
        // 목록에서 코스를 선택하면 오른쪽 상세 화면을 갱신
        courseList.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> showDetail(value));
    }

    /** 미리보기 지도와 경로 결과·오류 안내를 연결한다. */
    private void configurePreviewMap() {
        // ----- [가운데] 지도 영역 -----
        // 안내 문구 박스는 mapStatus가 보일 때만 보이게 연결
        mapNotice.visibleProperty().bind(mapStatus.visibleProperty());
        mapNotice.managedProperty().bind(mapStatus.visibleProperty());
        mapRetry.managedProperty().bind(mapRetry.visibleProperty());
        // 지도 객체 생성 및 설정 (후보 장소 추가는 끄고, 이동 경로는 요청할 때만 조회)
        map = new CourseMapView(previewMap, (latitude, longitude) -> {}, this::showMapError);
        map.setCandidateActionsEnabled(false);
        map.setAutomaticWalking(false);
        map.setCoursePreviewMode(true);
        map.setExternalViewControls(true);
        coursePreview.fitWidthProperty().bind(mapPane.widthProperty().subtract(8));
        coursePreview.fitHeightProperty().bind(mapPane.heightProperty().subtract(8));
        map.setOnRouteError(message -> { routeFailure = message; refreshMapNotice(); });
        // 도보 경로 결과에 따라 안내 문구 표시/숨김
        map.setWalkingResultListener((route, message) -> {
            routeMessage = route == null && message != null ? message : "";
            routeFailure = "";
            if (route != null && previewCourse != null) {
                routeSummaries.put(previewCourse,new RouteSummary(route,com.datecourse.service.TmapSettings.preferencesVersion()));
            }
            refreshTravelInformation();
            refreshMapNotice();
        });
        // 지도가 준비되면 안내 문구와 재시도 버튼을 숨김
        map.setOnReady(() -> {
            mapFailure = "";
            rememberMapPrepared();
            focusPendingPlace();
            refreshMapNotice();
        });
    }

    /** 코스 요약·장소 표와 장소 클릭 시 지도 이동을 설정한다. */
    private void configureDetailTables() {
        // ----- [가운데] 하단 정보 표 -----
        // 왼쪽: 코스 요약 표
        summaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        summaryTable.setFixedCellSize(-1);
        summaryLabelColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().label()));
        summaryValueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().value()));
        summaryLabelColumn.setSortable(false);
        summaryValueColumn.setSortable(false);
        summaryLabelColumn.setReorderable(false);
        summaryValueColumn.setReorderable(false);
        for (TableColumn<CourseDetailRow, String> column : java.util.List.of(summaryLabelColumn, summaryValueColumn))
            setWrappingCellFactory(column);

        // 오른쪽: 장소 표
        placeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        placeTable.setFixedCellSize(-1);
        placeTable.setSortPolicy(table -> false);
        placeOrderColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().order()));
        placeNameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        placePriceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().price()));
        placeTravelColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().travel()));
        placeAddressColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().address()));
        for (TableColumn<PlaceInfoRow, String> column : java.util.List.of(placeOrderColumn, placeNameColumn, placePriceColumn, placeAddressColumn, placeTravelColumn)) {
            column.setSortable(false);
            column.setReorderable(false);
            setWrappingCellFactory(column);
        }
        placeTable.getStyleClass().add("saved-place-table");
        placeTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        placeTable.setAccessibleHelp("방향키로 장소 선택, Enter로 지도에서 보기");
        placeTable.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                focusPlaceRow(placeTable.getSelectionModel().getSelectedIndex());
                event.consume();
            }
        });
        // 마우스와 키보드는 같은 지도 준비 규칙을 사용한다.
        placeTable.setRowFactory(table -> {
            TableRow<PlaceInfoRow> row = new TableRow<>();
            row.setCursor(javafx.scene.Cursor.HAND);
            row.setOnMouseClicked(event -> {
                if (row.isEmpty() || event.getButton() != javafx.scene.input.MouseButton.PRIMARY) return;
                placeTable.getSelectionModel().select(row.getIndex());
                focusPlaceRow(row.getIndex());
            });
            return row;
        });
    }

    /** 선택만으로 지도를 불러오지 않고, 명시적으로 연 장소만 포커스한다. */
    private void focusPlaceRow(int index) {
        SavedCourse selected = courseList.getSelectionModel().getSelectedItem();
        if (selected == null || index < 0 || index >= selected.plan().places().size()) return;
        selectedPreviewView = true;
        pendingFocusPlace = selected.plan().places().get(index);
        if (!mapSelected || !showingMap) { onSelectMap(); if (!showingMap) return; }
        focusPendingPlace();
    }

    /** 좁은 상세 영역에서는 장소 표를 먼저 보여주고 요약을 아래에 둔다. */
    private void configureResponsiveDetails() {
        bottomScroll.viewportBoundsProperty().addListener((o, before, bounds) -> layoutDetailTables(bounds.getWidth()));
        layoutDetailTables(0);
    }

    private void layoutDetailTables(double width) {
        boolean stacked = width < 900;
        placeTable.setPrefHeight(stacked ? Math.clamp(42 + placeTable.getItems().size() * 44, 100, 240) : 210);
        bottomInfo.getColumnConstraints().clear();
        if (stacked) {
            var column = new javafx.scene.layout.ColumnConstraints(); column.setPercentWidth(100);
            bottomInfo.getColumnConstraints().add(column);
            javafx.scene.layout.GridPane.setColumnIndex(placeTable, 0);
            javafx.scene.layout.GridPane.setRowIndex(placeTable, 0);
            javafx.scene.layout.GridPane.setColumnIndex(summaryTable, 0);
            javafx.scene.layout.GridPane.setRowIndex(summaryTable, 1);
        } else {
            var summary = new javafx.scene.layout.ColumnConstraints(); summary.setPercentWidth(29);
            var places = new javafx.scene.layout.ColumnConstraints(); places.setPercentWidth(71);
            bottomInfo.getColumnConstraints().addAll(summary, places);
            javafx.scene.layout.GridPane.setColumnIndex(summaryTable, 0);
            javafx.scene.layout.GridPane.setRowIndex(summaryTable, 0);
            javafx.scene.layout.GridPane.setColumnIndex(placeTable, 1);
            javafx.scene.layout.GridPane.setRowIndex(placeTable, 0);
        }
    }

    /** 메시지 표시 여부와 오류 알림을 연결한다. */
    private void configureMessages() {
        // ----- [가운데] 안내 메시지 -----
        // 글자가 있을 때만 자리를 차지하게 연결
        messageLabel.managedProperty().bind(messageLabel.textProperty().isNotEmpty());
        // 메시지가 바뀌면 알림 처리 (오류 종류)
        watchNotice(messageLabel, PageFeedback.Kind.ERROR);
    }


    /** 화면을 열 때 필요한 것들을 전달받고, 코스 목록을 불러온다. */
    public void configure(SavedCourseRepository repository, UserContext context, Consumer<SavedCourse> onOpen) {
        configure(repository, context, onOpen, null);
    }

    /** 사용자 세션과 저장소, 코스 불러오기·새 코스 동작을 연결한다. */
    public void configure(SavedCourseRepository repository, UserContext context, Consumer<SavedCourse> onOpen,
                          Runnable onNewCourse) {
        this.repository = repository;
        this.userContext = context;
        this.onOpen = onOpen;
        this.onNewCourse = onNewCourse;
        loadCourses();
    }

    /** 화면을 닫을 때 상태를 모두 비운다. */
    public void clear() {
        viewVersion++;
        map.reset();
        map.setOriginLocked(false);
        userContext = null;
        repository = null;
        onOpen = null;
        onNewCourse = null;
        busy = false;
        routeSummaries.clear();
        courseList.getItems().clear();
        showDetail(null);
    }

    /** 목록·이미지 응답을 무효화하고 미리보기 지도를 정리한다. */
    public void dispose() {
        clear();
        map.setOnReady(null);
        map.setWalkingResultListener((route, message) -> {});
        map.setOnRouteError(message -> {});
        map.dispose();
    }

    // =====================================================================
    // 3. 버튼 동작 (FXML 위에서 아래 순서)
    // =====================================================================

    /** [왼쪽] "새 코스" 버튼 */
    @FXML private void onNewCourseClick() {
        if (!busy && onNewCourse != null) onNewCourse.run();
    }

    /** [지도] "지도 다시 불러오기" 버튼 */
    @FXML private void onMapRetry() {
        if (previewCourse == null) return;
        mapFailure = routeFailure = routeMessage = "";
        if (map.isReady()) onRequestRoute();
        else map.reload();
        refreshMapNotice();
    }

    /** [하단] "이 코스 불러오기" 버튼 */
    @FXML private void onOpenClick() {
        SavedCourse selected = courseList.getSelectionModel().getSelectedItem();
        if (!busy && selected != null && onOpen != null) onOpen.accept(selected);
    }

    /** [하단] "새로고침" 버튼 */
    @FXML private void onRefreshClick() {
        if (repository == null || busy) return;
        userContext.invalidateCourses();
        loadCourses();
    }

    /** [하단] "이름 변경" 버튼 */
    @FXML private void onRenameClick() {
        SavedCourse selected = courseList.getSelectionModel().getSelectedItem();
        if (busy || selected == null) return;
        TextInputDialog dialog = new TextInputDialog(selected.plan().title());
        dialog.initOwner(courseList.getScene().getWindow());
        com.datecourse.page.common.controls.DialogTheme.apply(dialog);
        dialog.setHeaderText("코스 이름을 입력해 주세요. (1~20자)");
        dialog.showAndWait().ifPresent(input -> {
            String title = input.trim();
            if (title.isEmpty() || title.length() > 20) { messageLabel.setText("이름은 1~20자로 입력해 주세요."); return; }
            var owner = userContext.session();
            var storage = repository;
            run(() -> { storage.rename(owner, selected.courseId(), title); return true; }, ignored -> {
                notifyUser(PageFeedback.Kind.SUCCESS, "코스 이름을 바꿨어요.");
                onRefreshClick();
            });
        });
    }

    /** [하단] "삭제" 버튼 (확인 창 후 삭제) */
    @FXML private void onDeleteClick() {
        SavedCourse selected = courseList.getSelectionModel().getSelectedItem();
        if (busy || selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "'" + title(selected) + "' 코스를 삭제할까요?", ButtonType.OK, ButtonType.CANCEL);
        confirm.initOwner(courseList.getScene().getWindow());
        com.datecourse.page.common.controls.DialogTheme.apply(confirm);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        var owner = userContext.session();
        var storage = repository;
        run(() -> { storage.delete(owner, selected.courseId()); return true; }, ignored -> {
            notifyUser(PageFeedback.Kind.SUCCESS, "코스를 삭제했어요.");
            onRefreshClick();
        });
    }

    // =====================================================================
    // 4. 화면 갱신
    // =====================================================================

    // ---------- [왼쪽] 코스 목록 ----------

    /** DB에서 저장한 코스 목록을 불러와 목록에 채운다. */
    private void loadCourses() {
        if (userContext == null) return;
        UserContext context = userContext;
        long version = ++viewVersion;
        Long selected = courseList.getSelectionModel().getSelectedItem() == null ? null
                : courseList.getSelectionModel().getSelectedItem().courseId();
        busy = true;
        updateButtons();
        messageLabel.setText("처리 중…");
        showFuture("저장한 코스를 확인하고 있어요", context.courses(), courses -> {
            if (context != userContext || context.isClosed() || version != viewVersion) return;
            busy = false;
            routeSummaries.keySet().retainAll(courses);
            courseList.getItems().setAll(courses);
            emptyLabel.setVisible(courses.isEmpty());
            emptyLabel.setManaged(courses.isEmpty());
            if (!courses.isEmpty()) courseList.getSelectionModel().select(courses.stream()
                    .filter(course -> selected != null && course.courseId() == selected).findFirst().orElse(courses.getFirst()));
            else showDetail(null);
            updateButtons();
            messageLabel.setText("");
        }, failure -> {
            if (context != userContext || context.isClosed() || version != viewVersion) return;
            busy = false;
            updateButtons();
            messageLabel.setText("목록 조회 실패. 새로고침으로 다시 시도해 주세요.");
        });
    }

    // ---------- [가운데] 상세 정보 ----------

    /** 선택한 코스의 제목, 표, 지도를 갱신한다. (null이면 빈 화면) */
    private void showDetail(SavedCourse course) {
        summaryTable.getItems().clear();
        placeTable.getItems().clear();
        detailTitle.setText(course == null ? "코스를 선택해 주세요" : title(course));
        if (course != null) {
            var summary=presenter.present(course);
            summaryTable.getItems().setAll(java.util.Arrays.stream(summary.details().split("\\R"))
                    .map(SavedCourseController::toCourseDetailRow).toList());
            configurePlaceRows(summary.places());
        }
        showPreview(course);
        refreshTravelInformation();
        layoutDetailTables(bottomScroll.getViewportBounds().getWidth());
        updateButtons();
    }

    // ---------- [가운데] 지도 ----------

    /** 선택한 코스를 지도에 표시한다. */
    private void showPreview(SavedCourse course) {
        long version = ++previewLoadVersion;
        // 화면 전용 변환만 취소하고 UserContext의 공유 이미지 조회는 유지한다.
        if (previewTask != null) previewTask.cancel(false);
        previewTask = null;
        previewVersion++;
        previewCourse = course;
        previewLoading = course != null;
        pendingFocusPlace=null;
        showingMap = selectedPreviewView = storedPreview = routeRequested = mapSelected = mapPrepared = false;
        mapFailure = routeFailure = routeMessage = "";
        map.reset();
        coursePreview.setImage(null);
        mapPane.setVisible(course != null);
        mapPane.setManaged(course != null);
        refreshMapNotice();
        if (course == null) return;
        UserContext context = userContext;
        SavedCourseRepository storage = repository;
        var bytes = context == null || storage == null
                ? java.util.concurrent.CompletableFuture.completedFuture(java.util.Optional.<byte[]>empty())
                : context.preview(course, () -> storage.findPreview(context.session(), course));
        previewTask = bytes.handleAsync((png, failure) -> {
            if (failure != null) com.datecourse.service.AppDiagnostics.failure(
                    com.datecourse.service.AppDiagnostics.Operation.PREVIEW_READ,
                    com.datecourse.service.AppDiagnostics.Stage.DATABASE_READ, failure);
            boolean stored = failure == null && png.isPresent();
            try {
                byte[] content = stored ? png.get() : new com.datecourse.service.CoursePreviewImage().render(course.plan());
                var image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(content));
                if (image.isError()) throw new java.io.IOException("Invalid preview image");
                return new PreviewImage(image, stored);
            } catch (Exception failureImage) {
                com.datecourse.service.AppDiagnostics.failure(com.datecourse.service.AppDiagnostics.Operation.PREVIEW_READ,
                        com.datecourse.service.AppDiagnostics.Stage.IMAGE_ENCODE, failureImage);
                try {
                    byte[] fallback = new com.datecourse.service.CoursePreviewImage().render(course.plan());
                    return new PreviewImage(new javafx.scene.image.Image(new java.io.ByteArrayInputStream(fallback)), false);
                } catch (java.io.IOException fallbackFailure) {
                    throw new java.util.concurrent.CompletionException(fallbackFailure);
                }
            }
        });
        previewTask.whenComplete((preview, failure) -> javafx.application.Platform.runLater(() -> {
            if (version != previewLoadVersion || context != userContext || context != null && context.isClosed()) return;
            previewLoading = false;
            if (failure == null) {
                coursePreview.setImage(preview.image());
                storedPreview = preview.stored();
                mapPrepared = context != null && context.isMapPrepared(course);
            }
            boolean openMap = mapSelected ? mapPrepared || !storedPreview : !selectedPreviewView && !storedPreview;
            if (!showingMap && openMap) showMapPreview();
            refreshMapNotice();
        }));
    }

    /** 선택한 코스를 지도에 표시하고 늦은 화면 맞춤 요청을 구분한다. */
    private void showMapPreview() {
        if (previewCourse == null) return;
        showingMap = mapSelected = true;
        mapFailure = "";
        map.setOriginLocked(true);
        map.presentCourse(previewCourse.plan(), true);
        map.load();
        SavedCourse course = previewCourse;
        long version = ++previewVersion;
        var fitAfterLayout = new javafx.animation.PauseTransition(javafx.util.Duration.millis(180));
        fitAfterLayout.setOnFinished(event -> {
            if (previewVersion == version && previewCourse == course && showingMap) map.fitCourse();
        });
        fitAfterLayout.play();
        if(map.isReady()) {
            rememberMapPrepared();
            focusPendingPlace();
        }
        refreshMapNotice();
    }

    /** 이미지 전용 코스는 지도 탭에서 준비 버튼을 먼저 보여준다. */
    @FXML private void onSelectMap() {
        if (previewCourse==null) return;
        selectedPreviewView=mapSelected=true;
        if (!showingMap && !previewLoading && (mapPrepared || !storedPreview)) showMapPreview();
        refreshMapNotice();
    }
    @FXML private void onSelectImage() {
        if (previewCourse==null) return;
        selectedPreviewView=true; mapSelected=false;
        refreshMapNotice();
    }
    @FXML private void onLoadMap() {
        if (previewCourse==null || !mapSelected || showingMap || previewLoading) return;
        showMapPreview();
        if (map.isReady()) focusPendingPlace();
    }
    /** 불러오기 전에 선택한 장소를 열고 늦은 전체 화면 맞춤을 취소한다. */
    private void focusPendingPlace() {
        if (pendingFocusPlace == null) return;
        previewVersion++;
        map.focusPlace(pendingFocusPlace);
        pendingFocusPlace = null;
    }

    /** SDK 준비 완료는 현재 로그인 세션에만 기록한다. */
    private void rememberMapPrepared() {
        if (previewCourse == null || userContext == null || !showingMap || !map.isReady()) return;
        mapPrepared = userContext.markMapPrepared(previewCourse);
    }
    /** 도보 설정이 다른 옛 이동시간은 목록·표에서도 재사용하지 않는다. */
    private com.datecourse.model.TravelRoute cachedRoute(SavedCourse course) {
        var summary=routeSummaries.get(course);
        if (summary==null || course.plan().travelMode()!=com.datecourse.model.TravelMode.DRIVING
                && summary.preferencesVersion()!=com.datecourse.service.TmapSettings.preferencesVersion()) return null;
        return summary.route();
    }
    private String travelText(SavedCourse course) {
        var route=cachedRoute(course);
        return course.plan().travelMode().label()+" · "+(route==null?"이동시간 조회 전":com.datecourse.model.TravelRoute.durationText(route.totalTime()));
    }
    /** 공급사 응답이 있는 구간만 시간을 표시하며 미조회 시간을 추정하지 않는다. */
    private void refreshTravelInformation() {
        courseList.refresh();
        travelSummaryLabel.setText(previewCourse==null?"":travelText(previewCourse));
        if (previewCourse==null) return;
        var route=cachedRoute(previewCourse);
        for (int index=0;index<placeTable.getItems().size();index++) {
            var row=placeTable.getItems().get(index);
            String travel=previewCourse.plan().travelMode().label()+" · "+(route!=null && index<route.legs().size()
                    ?com.datecourse.model.TravelRoute.durationText(route.legs().get(index).time()):"조회 전");
            placeTable.getItems().set(index,new PlaceInfoRow(row.order(),row.name(),row.price(),row.address(),travel));
        }
    }

    /** 사용자가 선택한 이미지·지도 보기 방식으로 전환한다. */
    @FXML private void onToggleMap() {
        if (mapSelected) onSelectImage();
        else onSelectMap();
    }

    /** 사용자가 요청한 경우에만 선택 코스의 이동 경로를 조회한다. */
    @FXML private void onRequestRoute() {
        if (previewCourse == null || !showingMap || !map.isReady() || previewCourse.plan().places().isEmpty()) return;
        selectedPreviewView = routeRequested = true;
        routeFailure = "";
        map.requestWalkingRouteWhenReady();
        refreshMapNotice();
    }

    /** 지도 오류를 기록하고 대체 미리보기와 재시도 안내를 갱신한다. */
    private void showMapError(String message) {
        mapFailure = message;
        refreshMapNotice();
    }

    /** 지도·이미지 상태에 맞춰 미리보기와 안내를 갱신한다. */
    private void refreshMapNotice() {
        boolean hasCourse = previewCourse != null;
        boolean readyMap = mapSelected && showingMap && map.isReady() && mapFailure.isBlank();
        updatePreviewControls(hasCourse, readyMap);
        previewCaption.setText(buildPreviewCaption(hasCourse, readyMap));
        String message = buildMapNotice(hasCourse);
        mapStatus.setText(message);
        mapStatus.setVisible(!message.isBlank());
        mapRetry.setDisable(false);
        mapRetry.setVisible(hasCourse && mapSelected && showingMap && (!mapFailure.isBlank() || !routeFailure.isBlank()));
        mapRetry.setText(mapFailure.isBlank() && map.isReady() ? "이동 경로 다시 조회" : "지도 다시 불러오기");
    }

    /** 미리보기 화면과 지도 전환·경로 조회 버튼의 상태를 맞춘다. */
    private void updatePreviewControls(boolean hasCourse, boolean readyMap) {
        boolean waiting=hasCourse && mapSelected && !showingMap;
        previewMap.setVisible(hasCourse && mapSelected && showingMap);
        coursePreview.setVisible(hasCourse && (!mapSelected || showingMap && !readyMap));
        mapLoadPrompt.setVisible(waiting);mapLoadPrompt.setManaged(waiting);
        mapLoadButton.setDisable(previewLoading);
        mapLoadDescription.setText(previewLoading ? "저장된 미리보기를 확인하고 있어요…" : "이 코스는 이미지로 저장되어 있어요.");
        previewViewControls.setVisible(hasCourse);previewViewControls.setManaged(hasCourse);
        toggleMapButton.setDisable(hasCourse && coursePreview.getImage()==null && !showingMap);
        var active=javafx.css.PseudoClass.getPseudoClass("active");
        toggleMapButton.pseudoClassStateChanged(active,mapSelected);
        imageViewButton.pseudoClassStateChanged(active,!mapSelected);
        requestRouteButton.setVisible(hasCourse && mapSelected && showingMap);
        requestRouteButton.setManaged(hasCourse && mapSelected && showingMap);
        boolean pendingRoute = map.isRoutePending();
        requestRouteButton.setDisable(!readyMap || pendingRoute || !hasCourse || previewCourse.plan().places().isEmpty());
        requestRouteButton.setText(routeRequested && routeMessage.isBlank() ? "이동 경로 새로 조회" : "이동 경로 조회");
    }

    /** 현재 미리보기가 저장 이미지인지 지도·경로인지 짧게 설명한다. */
    private String buildPreviewCaption(boolean hasCourse, boolean readyMap) {
        if (!hasCourse) return "";
        if (mapSelected && !showingMap) return previewLoading ? "저장된 미리보기를 확인하고 있어요…" : "지도 불러오기를 누르면 이 코스를 지도에서 볼 수 있어요.";
        if (!readyMap) return storedPreview ? "저장된 미리보기 · 생성 시점의 정보예요" : "장소 연결도 · 실제 이동 경로와 다를 수 있어요";
        if (routeRequested && routeMessage.isBlank()) return "현재 조회한 이동 경로 · 저장 시점과 다를 수 있어요";
        return "직선 장소 연결 · 실제 이동 경로와 다를 수 있어요";
    }

    /** 미리보기 로딩과 지도·경로 오류 중 현재 필요한 안내를 고른다. */
    private String buildMapNotice(boolean hasCourse) {
        if (!hasCourse) return "";
        if (!mapSelected || !showingMap) return coursePreview.getImage() == null ? "미리보기를 불러오고 있어요…" : "";
        if (!mapFailure.isBlank()) return mapFailure + " · 기본 미리보기를 대신 표시해요.";
        if (!map.isReady()) return "지도를 불러오고 있어요. 미리보기와 코스 정보는 계속 볼 수 있어요…";
        if (!routeFailure.isBlank()) return routeFailure + " · 직선 장소 연결을 유지해요.";
        return routeMessage;
    }

    // ---------- [가운데] 버튼 상태 ----------

    /** 처리 중이거나 선택된 코스가 없으면 버튼을 비활성화한다. */
    private void updateButtons() {
        boolean unavailable = busy || courseList.getSelectionModel().getSelectedItem() == null;
        openButton.setDisable(unavailable);
        deleteButton.setDisable(unavailable);
        renameButton.setDisable(unavailable);
        refreshButton.setDisable(busy);
        courseList.setDisable(busy);
    }

    // =====================================================================
    // 5. 보조 메서드
    // =====================================================================

    // ---------- 목록 표시용 문자열 ----------

    /** 코스 제목 (비어 있으면 "내 코스") */
    private String title(SavedCourse course) { return course.plan().title().isBlank() ? "내 코스" : course.plan().title(); }

    /** 최근 수정 시각 문자열 (한국 시간 기준) */
    private String updatedText(SavedCourse course) {
        return course.updatedAt() == null ? "시각 미확인" : java.time.format.DateTimeFormatter
                .ofPattern("yyyy.MM.dd HH:mm").withZone(java.time.ZoneId.of("Asia/Seoul")).format(course.updatedAt());
    }

    // ---------- 표 데이터 변환 ----------

    /** "이름: 내용" 형태의 한 줄을 요약 표의 한 행으로 바꾼다. */
    private static CourseDetailRow toCourseDetailRow(String line) {
        int separator = line.indexOf(':');
        return separator < 0 ? new CourseDetailRow("카테고리", line)
                : new CourseDetailRow(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
    }

    /** 장소 설명 목록을 장소 표에 채운다. */
    private void configurePlaceRows(java.util.List<String> descriptions) {
        placeTable.getItems().setAll(descriptions.stream().map(SavedCourseController::toPlaceInfoRow).toList());
    }

    /** 장소 설명(여러 줄 문자열)을 순서/이름/가격/주소로 나눠 표의 한 행으로 바꾼다. */
    private static PlaceInfoRow toPlaceInfoRow(String text) {
        String[] lines = text.split("\\R", 3);
        String first = lines.length > 0 ? lines[0] : "";
        int separator = first.indexOf(". ");
        String order = separator > 0 ? first.substring(0, separator) : "";
        String name = separator > 0 ? first.substring(separator + 2) : first;
        String price = lines.length > 1 ? lines[1].replaceFirst("^가격:\\s*", "") : "가격 정보 없음";
        String address = lines.length > 2 ? lines[2].replaceFirst("^주소:\\s*", "") : "주소 정보 없음";
        return new PlaceInfoRow(order, name, price, address, "조회 전");
    }

    /** 표의 열이 긴 글을 자동 줄바꿈해서 보여주도록 설정한다. */
    private static <T> void setWrappingCellFactory(TableColumn<T, String> column) {
        column.setCellFactory(ignored -> {
            TableCell<T, String> cell = new TableCell<>();
            javafx.scene.text.Text text = new javafx.scene.text.Text();
            // 열 너비에 맞춰 자동 줄바꿈 (좌우 여백만큼 뺌)
            text.wrappingWidthProperty().bind(column.widthProperty().subtract(18));
            // 셀의 값이 바뀌면 글자도 자동으로 바뀜
            text.textProperty().bind(cell.itemProperty());
            cell.setGraphic(text);
            cell.setPrefHeight(Control.USE_COMPUTED_SIZE);
            return cell;
        });
    }

    // ---------- 작업 실행 ----------

    /** 이름 변경/삭제 같은 작업을 실행한다. (처리 중 표시 → 성공/실패 처리) */
    private <T> void run(java.util.concurrent.Callable<T> work, Consumer<T> success) {
        UserContext context = userContext;
        busy = true;
        updateButtons();
        messageLabel.setText("처리 중…");
        runWork("저장한 코스를 확인하고 있어요", work, value -> {
            if (context != userContext || context.isClosed()) return;
            busy = false;
            updateButtons();
            success.accept(value);
        }, failure -> {
            if (context != userContext || context.isClosed()) return;
            busy = false;
            updateButtons();
            messageLabel.setText("요청을 처리하지 못했어요. 잠시 후 다시 시도해 주세요.");
        });
    }
}