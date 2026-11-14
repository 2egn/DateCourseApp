package com.datecourse.controller;

import com.datecourse.model.Place;
import com.datecourse.model.SavedCourse;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import com.datecourse.service.UserContext;
import kr.ac.university.localdb.db.SavedCourseRepository;
import java.util.function.Consumer;

/** List and details use the same saved snapshot returned to the planner. */
public class SavedCourseController extends BasePageController {
    @FXML private ListView<SavedCourse> courseList;
    @FXML private ListView<String> placeList;
    @FXML private Label emptyLabel, detailTitle, detailSummary, messageLabel;
    @FXML private Button deleteButton, renameButton, openButton, refreshButton;
    private SavedCourseRepository repository;
    private UserContext userContext;
    private long viewVersion;
    private Consumer<SavedCourse> onOpen;
    private boolean busy;

    @FXML public void initialize() {
        watchNotice(messageLabel, PageFeedback.Kind.ERROR);
        courseList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(SavedCourse course, boolean empty) {
                super.updateItem(course, empty);
                setText(empty || course == null ? null : title(course) + "\n"
                        + course.plan().places().size() + "곳 · " + course.plan().costEstimate().display()
                        + "\n최근 수정 " + updatedText(course));
            }
        });
        courseList.getSelectionModel().selectedItemProperty().addListener((o, old, value) -> showDetail(value));
        showDetail(null);
    }

    public void configure(SavedCourseRepository repository, UserContext context, Consumer<SavedCourse> onOpen) {
        this.repository = repository;
        this.userContext = context;
        this.onOpen = onOpen;
        loadCourses();
    }

    public void clear() {
        viewVersion++;
        userContext = null;
        repository = null;
        onOpen = null;
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
        placeList.getItems().clear();
        detailTitle.setText(course == null ? "코스를 선택해 주세요" : title(course));
        detailSummary.setText(course == null ? "" : course.plan().people() + "명 · " + course.plan().radius()
                + "m · " + course.plan().costEstimate().display() + "\n최근 수정 " + updatedText(course) + " (한국 시간)");
        if (course != null) {
            int order = 1;
            for (Place place : course.plan().places()) placeList.getItems().add(order++ + ". " + place.name()
                    + " · " + place.priceText() + "\n" + place.details().address());
        }
        updateButtons();
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
            messageLabel.setText("요청 실패. DB 연결을 확인해 주세요.");
        });
    }
}
