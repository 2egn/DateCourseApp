package com.datecourse.controller;

import com.datecourse.model.SavedCourse;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.beans.property.ReadOnlyStringWrapper;
import com.datecourse.service.UserContext;
import kr.ac.university.localdb.db.SavedCourseRepository;
import java.util.function.Consumer;

/** List and details use the same saved snapshot returned to the planner. */
public class SavedCourseController extends BasePageController {
    @FXML private javafx.scene.layout.BorderPane savedCourseRoot;
    @FXML private javafx.scene.layout.VBox courseListPane;
    @FXML private ListView<SavedCourse> courseList;
    @FXML private TableView<CourseDetailRow> summaryTable;
    @FXML private TableColumn<CourseDetailRow, String> summaryLabelColumn, summaryValueColumn;
    @FXML private Label emptyLabel, detailTitle, messageLabel;
    @FXML private javafx.scene.layout.HBox bottomInfo;
    @FXML private TableView<PlaceInfoRow> placeTable;
    @FXML private TableColumn<PlaceInfoRow, String> placeOrderColumn, placeNameColumn, placePriceColumn, placeAddressColumn;
    private final com.datecourse.service.SavedCoursePresenter presenter = new com.datecourse.service.SavedCoursePresenter();
    @FXML private Button deleteButton, renameButton, openButton, refreshButton;
    @FXML private javafx.scene.web.WebView previewMap;
    @FXML private javafx.scene.layout.StackPane mapPane;
    @FXML private Label mapStatus;
    @FXML private javafx.scene.layout.HBox mapNotice;
    @FXML private Button mapRetry, toggleMapButton, requestRouteButton;
    @FXML private javafx.scene.image.ImageView coursePreview;
    @FXML private Label previewCaption;
    private PlannerMap map;
    private SavedCourseRepository repository;
    private UserContext userContext;
    private long viewVersion;
    private Consumer<SavedCourse> onOpen;
    private boolean busy;
    private SavedCourse previewCourse;
    private long previewVersion;
    private boolean showingMap, selectedPreviewView, storedPreview, routeRequested;
    private java.util.concurrent.CompletableFuture<PreviewImage> previewTask;
    private record PreviewImage(javafx.scene.image.Image image, boolean stored) {}
    private String mapFailure = "", routeFailure = "", routeMessage = "";

    private record CourseDetailRow(String label, String value) {}
    private record PlaceInfoRow(String order, String name, String price, String address) {}

    @FXML public void initialize() {
        savedCourseRoot.widthProperty().addListener((obs, oldWidth, newWidth) ->
                courseListPane.setPrefWidth(Math.max(240, Math.min(420, newWidth.doubleValue() * 0.28))));
        courseListPane.setPrefWidth(Math.max(240, Math.min(420, savedCourseRoot.getWidth() * 0.28)));
        mapNotice.visibleProperty().bind(mapStatus.visibleProperty());
        mapNotice.managedProperty().bind(mapStatus.visibleProperty());
        mapRetry.managedProperty().bind(mapRetry.visibleProperty());
        map = new PlannerMap(previewMap, (latitude, longitude) -> {}, this::showMapError);
        map.setCandidateActionsEnabled(false);
        map.setAutomaticWalking(false);
        map.setCoursePreviewMode(true);
        coursePreview.fitWidthProperty().bind(mapPane.widthProperty().subtract(8));
        coursePreview.fitHeightProperty().bind(mapPane.heightProperty().subtract(8));
        map.setOnRouteError(message -> {
            routeFailure = message;
            refreshMapNotice();
        });
        map.setWalkingResultListener((route, message) -> {
            routeMessage = route == null && message != null ? message : "";
            routeFailure = "";
            refreshMapNotice();
        });
        map.setOnReady(() -> {
            mapFailure = "";
            refreshMapNotice();
        });
        messageLabel.managedProperty().bind(messageLabel.textProperty().isNotEmpty());
        summaryTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        summaryTable.setFixedCellSize(-1);
        summaryLabelColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().label()));
        summaryValueColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().value()));
        summaryLabelColumn.setSortable(false);
        summaryValueColumn.setSortable(false);
        summaryLabelColumn.setReorderable(false);
        summaryValueColumn.setReorderable(false);
        for (TableColumn<CourseDetailRow, String> column : java.util.List.of(summaryLabelColumn, summaryValueColumn))
            setWrappingCellFactory(column);
        placeTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        placeTable.setFixedCellSize(-1);
        placeTable.setSortPolicy(table -> false);
        placeOrderColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().order()));
        placeNameColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().name()));
        placePriceColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().price()));
        placeAddressColumn.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().address()));
        for (TableColumn<PlaceInfoRow, String> column : java.util.List.of(placeOrderColumn, placeNameColumn, placePriceColumn, placeAddressColumn)) {
            column.setSortable(false);
            column.setReorderable(false);
            setWrappingCellFactory(column);
        }
        watchNotice(messageLabel, PageFeedback.Kind.ERROR);
        courseList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(SavedCourse course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                Label titleLabel = new Label(title(course));
                titleLabel.getStyleClass().add("saved-course-title");
                titleLabel.setWrapText(true);
                Label summaryLabel = new Label(course.plan().places().size() + "곳 · "
                        + presenter.costSummary(course));
                summaryLabel.getStyleClass().add("saved-course-summary");
                Label updatedLabel = new Label("최근 수정 " + updatedText(course));
                updatedLabel.getStyleClass().add("saved-course-updated");
                setText(null);
                setGraphic(new javafx.scene.layout.VBox(3, titleLabel, summaryLabel, updatedLabel));
            }
        });
        courseList.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> showDetail(value));
        showDetail(null);
        summaryTable.setSelectionModel(null);
        placeTable.setSelectionModel(null);
    }

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

    private void configurePlaceRows(java.util.List<String> descriptions) {
        placeTable.getItems().setAll(descriptions.stream().map(SavedCourseController::toPlaceInfoRow).toList());
    }

    private static PlaceInfoRow toPlaceInfoRow(String text) {
        String[] lines = text.split("\\R", 3);
        String first = lines.length > 0 ? lines[0] : "";
        int separator = first.indexOf(". ");
        String order = separator > 0 ? first.substring(0, separator) : "";
        String name = separator > 0 ? first.substring(separator + 2) : first;
        String price = lines.length > 1 ? lines[1].replaceFirst("^가격:\\s*", "") : "가격 정보 없음";
        String address = lines.length > 2 ? lines[2].replaceFirst("^주소:\\s*", "") : "주소 정보 없음";
        return new PlaceInfoRow(order, name, price, address);
    }

    public void configure(SavedCourseRepository repository, UserContext context, Consumer<SavedCourse> onOpen) {
        this.repository = repository;
        this.userContext = context;
        this.onOpen = onOpen;
        loadCourses();
    }

    public void clear() {
        viewVersion++;
        map.reset();
        map.setOriginLocked(false);
        userContext = null;
        repository = null;
        onOpen = null;
        busy = false;
        courseList.getItems().clear();
        showDetail(null);
    }

    public void dispose() {
        clear();
        map.setOnReady(null);
        map.setWalkingResultListener((route, message) -> {});
        map.setOnRouteError(message -> {});
        map.dispose();
    }

    private String title(SavedCourse course) { return course.plan().title().isBlank() ? "내 코스" : course.plan().title(); }
    private String updatedText(SavedCourse course) {
        return course.updatedAt() == null ? "시각 미확인" : java.time.format.DateTimeFormatter
                .ofPattern("yyyy.MM.dd HH:mm").withZone(java.time.ZoneId.of("Asia/Seoul")).format(course.updatedAt());
    }

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
        updateButtons();
    }

    private static CourseDetailRow toCourseDetailRow(String line) {
        int separator = line.indexOf(':');
        return separator < 0 ? new CourseDetailRow("카테고리", line)
                : new CourseDetailRow(line.substring(0, separator).trim(), line.substring(separator + 1).trim());
    }

    private void showPreview(SavedCourse course) {
        long version = ++previewVersion;
        previewCourse = course;
        showingMap = selectedPreviewView = storedPreview = routeRequested = false;
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
            if (version != previewVersion || context != userContext || context != null && context.isClosed()) return;
            if (failure == null) {
                coursePreview.setImage(preview.image());
                storedPreview = preview.stored();
            }
            if (!selectedPreviewView && !storedPreview) showMapPreview();
            refreshMapNotice();
        }));
    }

    private void showMapPreview() {
        if (previewCourse == null) return;
        showingMap = true;
        mapFailure = "";
        map.setOriginLocked(true);
        map.present(previewCourse.plan(), null, previewCourse.plan().radius(), true, true);
        map.load();
        refreshMapNotice();
    }

    @FXML private void onToggleMap() {
        if (previewCourse == null) return;
        selectedPreviewView = true;
        if (showingMap) {
            showingMap = routeRequested = false;
            mapFailure = routeFailure = routeMessage = "";
            map.reset();
        } else showMapPreview();
        refreshMapNotice();
    }

    @FXML private void onRequestRoute() {
        if (previewCourse == null || !showingMap || !map.isReady() || previewCourse.plan().places().isEmpty()) return;
        selectedPreviewView = routeRequested = true;
        routeFailure = "";
        map.requestWalkingRouteWhenReady();
        refreshMapNotice();
    }

    private void showMapError(String message) {
        mapFailure = message;
        refreshMapNotice();
    }

    private void refreshMapNotice() {
        boolean hasCourse = previewCourse != null;
        boolean readyMap = showingMap && map.isReady() && mapFailure.isBlank();
        previewMap.setVisible(hasCourse && showingMap);
        coursePreview.setVisible(hasCourse && !readyMap);
        toggleMapButton.setVisible(hasCourse);
        toggleMapButton.setManaged(hasCourse);
        toggleMapButton.setText(showingMap ? "이미지 보기" : "지도 보기");
        requestRouteButton.setVisible(hasCourse && showingMap);
        requestRouteButton.setManaged(hasCourse && showingMap);
        boolean pendingRoute = routeRequested && !routeMessage.isBlank() && routeFailure.isBlank();
        requestRouteButton.setDisable(!readyMap || pendingRoute || !hasCourse || previewCourse.plan().places().isEmpty());
        requestRouteButton.setText(routeRequested && routeMessage.isBlank() ? "이동 경로 새로 조회" : "이동 경로 조회");
        previewCaption.setText(!hasCourse ? "" : readyMap
                ? routeRequested && routeMessage.isBlank() ? "현재 조회한 이동 경로 · 저장 시점과 다를 수 있어요"
                : "직선 장소 연결 · 실제 이동 경로와 다를 수 있어요"
                : storedPreview ? "저장된 미리보기 · 생성 시점의 정보예요" : "장소 연결도 · 실제 이동 경로와 다를 수 있어요");
        String message = "";
        if (hasCourse && !showingMap && coursePreview.getImage() == null) {
            message = "미리보기를 불러오고 있어요…";
        } else if (hasCourse && showingMap) {
            if (!mapFailure.isBlank()) message = mapFailure + " · 기본 미리보기를 대신 표시해요.";
            else if (!map.isReady()) message = "지도를 불러오고 있어요. 미리보기와 코스 정보는 계속 볼 수 있어요…";
            else if (!routeFailure.isBlank()) message = routeFailure + " · 직선 장소 연결을 유지해요.";
            else message = routeMessage;
        }
        mapStatus.setText(message);
        mapStatus.setVisible(!message.isBlank());
        mapRetry.setDisable(false);
        mapRetry.setVisible(hasCourse && showingMap && (!mapFailure.isBlank() || !routeFailure.isBlank()));
        mapRetry.setText(mapFailure.isBlank() && map.isReady() ? "이동 경로 다시 조회" : "지도 다시 불러오기");
    }

    @FXML private void onMapRetry() {
        if (previewCourse == null) return;
        mapFailure = routeFailure = routeMessage = "";
        if (map.isReady()) onRequestRoute();
        else map.reload();
        refreshMapNotice();
    }

    private void updateButtons() {
        boolean unavailable = busy || courseList.getSelectionModel().getSelectedItem() == null;
        openButton.setDisable(unavailable);
        deleteButton.setDisable(unavailable);
        renameButton.setDisable(unavailable);
        refreshButton.setDisable(busy);
        courseList.setDisable(busy);
    }

    @FXML private void onRefreshClick() {
        if (repository == null || busy) return;
        userContext.invalidateCourses();
        loadCourses();
    }

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

    @FXML private void onOpenClick() {
        SavedCourse selected = courseList.getSelectionModel().getSelectedItem();
        if (!busy && selected != null && onOpen != null) onOpen.accept(selected);
    }

    @FXML private void onDeleteClick() {
        SavedCourse selected = courseList.getSelectionModel().getSelectedItem();
        if (busy || selected == null) return;
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "'" + title(selected) + "' 코스를 삭제할까요?", ButtonType.OK, ButtonType.CANCEL);
        confirm.initOwner(courseList.getScene().getWindow());
        com.datecourse.ui.DialogTheme.apply(confirm);
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) return;
        var owner = userContext.session();
        var storage = repository;
        run(() -> { storage.delete(owner, selected.courseId()); return true; }, ignored -> {
            notifyUser(PageFeedback.Kind.SUCCESS, "코스를 삭제했어요.");
            onRefreshClick();
        });
    }

    @FXML private void onRenameClick() {
        SavedCourse selected = courseList.getSelectionModel().getSelectedItem();
        if (busy || selected == null) return;
        TextInputDialog dialog = new TextInputDialog(selected.plan().title());
        dialog.initOwner(courseList.getScene().getWindow());
        com.datecourse.ui.DialogTheme.apply(dialog);
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
