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
    @FXML private javafx.scene.image.ImageView coursePreview;
    @FXML private javafx.scene.layout.StackPane previewPane;
    @FXML private Label previewStatus;
    private long previewVersion;
    private java.util.List<SavedCourse> previewCourses;
    private final java.util.Map<String, java.util.Optional<javafx.scene.image.Image>> previews =
            new java.util.LinkedHashMap<>(24, .75f, true) {
                @Override protected boolean removeEldestEntry(java.util.Map.Entry<String, java.util.Optional<javafx.scene.image.Image>> entry) {
                    return size() > 24;
                }
            };
    private SavedCourseRepository repository;
    private UserContext userContext;
    private long viewVersion;
    private Consumer<SavedCourse> onOpen;
    private boolean busy;

    @FXML public void initialize() {
        coursePreview.fitWidthProperty().bind(previewPane.widthProperty());
        coursePreview.fitHeightProperty().bind(previewPane.heightProperty());
        coursePreview.setCursor(javafx.scene.Cursor.HAND);
        coursePreview.setFocusTraversable(true);
        javafx.scene.control.Tooltip.install(coursePreview,new Tooltip("클릭해서 크게 보기"));
        coursePreview.setOnMouseClicked(event -> {if(event.getButton()==javafx.scene.input.MouseButton.PRIMARY)openPreview();});
        coursePreview.setOnKeyPressed(event -> {
            if(event.getCode()==javafx.scene.input.KeyCode.ENTER){openPreview();event.consume();}
        });
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
        if (this.userContext != context) previews.clear();
        this.repository = repository;
        this.userContext = context;
        this.onOpen = onOpen;
        loadCourses();
    }

    public void clear() {
        viewVersion++;
        previewVersion++;
        previews.clear();
        previewCourses = null;
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
        showPreview(course);
        updateButtons();
    }

    private void showPreview(SavedCourse course) {
        long version = ++previewVersion;
        coursePreview.setImage(null);
        previewPane.setVisible(course != null);
        previewPane.setManaged(course != null);
        previewStatus.setVisible(true);
        if (course == null) return;
        if (repository == null || userContext == null) {
            previewStatus.setText("저장된 미리보기가 없어요. 코스를 다시 저장하면 생성돼요.");
            return;
        }
        String key = course.courseId() + ":" + com.datecourse.service.CoursePreviewImage.fingerprint(course.plan());
        if (previews.containsKey(key)) { displayPreview(previews.get(key)); return; }
        previewStatus.setText("미리보기를 불러오고 있어요…");
        var context = userContext;
        var storage = repository;
        var snapshot = previewCourses;
        // No full-page loading overlay; selection and course opening remain responsive.
        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                return storage.findPreview(context.session(), course).map(png -> {
                    var image = new javafx.scene.image.Image(new java.io.ByteArrayInputStream(png));
                    if (image.isError()) throw new IllegalArgumentException("이미지를 읽지 못했습니다.");
                    return image;
                });
            } catch (Exception failure) { throw new java.util.concurrent.CompletionException(failure); }
        }).whenComplete((image, failure) -> javafx.application.Platform.runLater(() -> {
            if (context != userContext || context.isClosed()) return;
            if (snapshot != previewCourses) return;
            if (failure == null) previews.put(key, image);
            if (version != previewVersion) return;
            if (failure == null) displayPreview(image);
            else previewStatus.setText("미리보기를 불러오지 못했어요. 코스는 바로 열 수 있어요.");
        }));
    }

    private void displayPreview(java.util.Optional<javafx.scene.image.Image> image) {
        coursePreview.setImage(image.orElse(null));
        previewStatus.setVisible(image.isEmpty());
        previewStatus.setText("저장된 미리보기가 없어요. 코스를 다시 저장하면 생성돼요.");
    }

    private void openPreview() {
        if(coursePreview.getImage()==null)return;
        var dialog=new javafx.stage.Stage();dialog.initOwner(courseList.getScene().getWindow());
        dialog.setTitle(detailTitle.getText()+" · 코스 미리보기");
        var image=new javafx.scene.image.ImageView(coursePreview.getImage());image.setPreserveRatio(true);
        var pane=new javafx.scene.layout.StackPane(image);
        image.fitWidthProperty().bind(pane.widthProperty());image.fitHeightProperty().bind(pane.heightProperty());
        var bounds=javafx.stage.Screen.getPrimary().getVisualBounds();
        dialog.setScene(new javafx.scene.Scene(pane,Math.min(1100,bounds.getWidth()-80),Math.min(720,bounds.getHeight()-100)));
        dialog.getScene().setOnKeyPressed(event->{if(event.getCode()==javafx.scene.input.KeyCode.ESCAPE)dialog.close();});
        dialog.show();
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
        previews.clear();
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
            if (previewCourses != courses) {
                previews.clear();
                previewCourses = courses;
            }
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
