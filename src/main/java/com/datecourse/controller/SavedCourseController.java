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
    @FXML private javafx.scene.layout.VBox mapNotice;
    @FXML private Button mapRetry;
    private PlannerMap map;
    private SavedCourseRepository repository;
    private UserContext userContext;
    private long viewVersion;
    private Consumer<SavedCourse> onOpen;
    private Runnable onNewCourse;
    private boolean busy;
    private long previewVersion;

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
        map.setAutomaticWalking(true);
        map.setWalkingResultListener((route, message) -> {
            if (route == null && message != null && !message.isBlank()) {
                mapStatus.setText(message);
                mapStatus.setVisible(true);
            } else if (route != null) {
                mapStatus.setVisible(false);
            }
        });
        map.setOnReady(() -> {
            mapStatus.setVisible(false);
            mapRetry.setVisible(false);
            mapRetry.setDisable(false);
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
        placeTable.setRowFactory(table -> {
            TableRow<PlaceInfoRow> row = new TableRow<>();
            row.setCursor(javafx.scene.Cursor.HAND);
            row.setOnMouseClicked(event -> {
                if (row.isEmpty()) return;
                SavedCourse selected = courseList.getSelectionModel().getSelectedItem();
                int index = row.getIndex();
                if (selected == null || index < 0 || index >= selected.plan().places().size()) return;
                previewVersion++;
                map.focusPlace(selected.plan().places().get(index));
            });
            return row;
        });
        watchNotice(messageLabel, PageFeedback.Kind.ERROR);
        courseList.setCellFactory(list -> new ListCell<>() {
            // 셀마다 한 번만 만들어서 재사용 (updateItem이 불릴 때마다 새로 만들지 않음)
            private final Label titleLabel = new Label();
            private final Label summaryLabel = new Label();
            private final Label updatedLabel = new Label();
            private final javafx.scene.layout.VBox box =
                    new javafx.scene.layout.VBox(3, titleLabel, summaryLabel, updatedLabel);

            {   // 초기화 블록: 셀이 만들어질 때 한 번 실행됨
                titleLabel.getStyleClass().add("saved-course-title");
                summaryLabel.getStyleClass().add("saved-course-summary");
                updatedLabel.getStyleClass().add("saved-course-updated");
                for (Label label : java.util.List.of(titleLabel, summaryLabel, updatedLabel)) {
                    label.setWrapText(true);                 // 길면 줄바꿈
                    label.setMaxWidth(Double.MAX_VALUE);
                }
                setPrefWidth(0);   // 셀이 내용 때문에 목록보다 넓어지지 않게 함
                // 안쪽 박스 폭을 셀 폭에 묶음 (좌우 padding 12*2 만큼 뺌)
                box.prefWidthProperty().bind(widthProperty().subtract(24));
                box.maxWidthProperty().bind(widthProperty().subtract(24));
            }

            @Override protected void updateItem(SavedCourse course, boolean empty) {
                super.updateItem(course, empty);
                if (empty || course == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }
                titleLabel.setText(title(course));
                summaryLabel.setText(course.plan().places().size() + "곳 · " + presenter.costSummary(course));
                updatedLabel.setText("최근 수정 " + updatedText(course));
                setText(null);
                setGraphic(box);
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

    public void configure(SavedCourseRepository repository, UserContext context, Consumer<SavedCourse> onOpen,
                         Runnable onNewCourse) {
        this.repository = repository;
        this.userContext = context;
        this.onOpen = onOpen;
        this.onNewCourse = onNewCourse;
        loadCourses();
    }

    @FXML private void onNewCourseClick() {
        if (!busy && onNewCourse != null) onNewCourse.run();
    }

    public void clear() {
        viewVersion++;
        map.reset();
        map.setOriginLocked(false);
        userContext = null;
        repository = null;
        onOpen = null;
        onNewCourse = null;
        busy = false;
        courseList.getItems().clear();
        showDetail(null);
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
        mapPane.setVisible(course != null);
        mapPane.setManaged(course != null);
        mapRetry.setVisible(false);
        if (course == null) {
            map.reset();
            mapStatus.setVisible(false);
            return;
        }
        mapStatus.setText("실시간 지도를 불러오고 있어요…");
        mapStatus.setVisible(!map.isReady());
        map.setOriginLocked(true);
        map.setCandidateActionsEnabled(false);
        map.presentCourse(course.plan(), true);
        map.load();
        var fitAfterLayout = new javafx.animation.PauseTransition(javafx.util.Duration.millis(180));
        fitAfterLayout.setOnFinished(event -> {
            if (previewVersion == version && courseList.getSelectionModel().getSelectedItem() == course) map.fitCourse();
        });
        fitAfterLayout.play();
        if (map.isReady()) mapStatus.setVisible(false);
    }

    private void showMapError(String message) {
        mapStatus.setText(message);
        mapStatus.setVisible(true);
        mapRetry.setDisable(false);
        mapRetry.setVisible(true);
    }

    @FXML private void onMapRetry() {
        if (courseList.getSelectionModel().getSelectedItem() == null) return;
        mapStatus.setText("실시간 지도를 다시 불러오고 있어요…");
        mapStatus.setVisible(true);
        mapRetry.setVisible(false);
        map.reload();
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
