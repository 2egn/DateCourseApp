package com.datecourse;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextInputDialog;

import java.net.URL;
import java.util.ResourceBundle;

public class SavedCourseController implements Initializable {

    @FXML private ListView<Course> courseList;
    @FXML private ListView<String> placeList;
    @FXML private Label emptyLabel;
    @FXML private Label detailTitle;
    @FXML private Label detailSummary;
    @FXML private Label messageLabel;
    @FXML private Button deleteButton;
    @FXML private Button renameButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // 목록에 코스를 어떤 글자로 보여줄지 지정
        courseList.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Course c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                } else {
                    setText(c.title() + "\n"
                            + c.coursePlaces().size() + "곳 · " + c.totalPrice() + "원");
                }
            }
        });

        // 코스를 선택하면 오른쪽 상세 갱신
        courseList.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldV, newV) -> showDetail(newV));

        loadCourses();
    }

    // 나중에 백엔드 호출로 바뀌는 자리
    private void loadCourses() {
        courseList.getItems().setAll(FakeCourseData.getSavedCourses());
        updateEmptyState();
        if (!courseList.getItems().isEmpty()) {
            courseList.getSelectionModel().selectFirst();
        }
    }

    private void showDetail(Course course) {
        placeList.getItems().clear();
        messageLabel.setText("");

        if (course == null) {
            detailTitle.setText("코스를 선택해주세요");
            detailSummary.setText("");
            deleteButton.setDisable(true);
            renameButton.setDisable(true);
            return;
        }

        detailTitle.setText(course.title());
        detailSummary.setText("총 " + course.totalPrice() + "원 · "
                + course.coursePlaces().size() + "곳");

        int order = 1;
        for (CoursePlace p : course.coursePlaces()) {
            placeList.getItems().add(order++ + ". " + p.name()
                    + " (" + p.category() + ") - " + p.price() + "원\n    " + p.address());
        }
        deleteButton.setDisable(false);
        renameButton.setDisable(false);
    }

    private void updateEmptyState() {
        emptyLabel.setVisible(courseList.getItems().isEmpty());
    }

    @FXML
    private void onDeleteClick() {
        Course selected = courseList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        // TODO: 백엔드에 삭제 요청 (selected.courseId())
        courseList.getItems().remove(selected);
        updateEmptyState();
        messageLabel.setText("'" + selected.title() + "' 코스를 삭제했어요.");
    }

    @FXML
    private void onRenameClick() {
        Course selected = courseList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        TextInputDialog dialog = new TextInputDialog(selected.title());
        dialog.setTitle("이름 변경");
        dialog.setHeaderText("코스 이름을 입력해주세요. (최대 20자)");
        dialog.setContentText("이름:");

        dialog.showAndWait().ifPresent(input -> {
            String newTitle = input.trim();
            if (newTitle.isEmpty() || newTitle.length() > 20) {
                messageLabel.setText("이름은 1~20자로 입력해주세요.");
                return;
            }

            // TODO: 백엔드에 이름 변경 요청 (selected.courseId(), newTitle)
            Course renamed = new Course(selected.courseId(), newTitle,
                    selected.coursePlaces(), selected.totalPrice());
            int index = courseList.getItems().indexOf(selected);
            courseList.getItems().set(index, renamed);
            courseList.getSelectionModel().select(index);
            messageLabel.setText("이름을 '" + newTitle + "'(으)로 바꿨어요.");
        });
    }
}