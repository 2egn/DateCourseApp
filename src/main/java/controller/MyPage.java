package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MyPage {

    @FXML
    private VBox profilePane;

    @FXML
    private VBox savedCoursePane;

    @FXML
    private TextField emailField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField nameField;

    @FXML
    private TextField phoneField;

    @FXML
    private ListView<String> savedCourseListView;

    @FXML
    public void initialize() {

        savedCourseListView.getItems().addAll(
                "야경 데이트",
                "카페 데이트",
                "산책 코스"
        );
    }

    // 회원정보 수정 화면

    @FXML
    private void showProfile(ActionEvent event) {

        profilePane.setVisible(true);
        profilePane.setManaged(true);

        savedCoursePane.setVisible(false);
        savedCoursePane.setManaged(false);
    }

    // 저장한 코스 화면

    @FXML
    private void showSavedCourse(ActionEvent event) {

        profilePane.setVisible(false);
        profilePane.setManaged(false);

        savedCoursePane.setVisible(true);
        savedCoursePane.setManaged(true);
    }

    // 회원정보 저장

    @FXML
    private void updateMemberInfo(ActionEvent event) {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("회원정보");
        alert.setHeaderText(null);
        alert.setContentText("회원정보가 저장되었습니다.");

        alert.showAndWait();
    }

    // 홈으로 이동


    @FXML
    private void goHome(MouseEvent event) {

        try {

            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/main.fxml")
            );

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            double width = stage.getWidth();
            double height = stage.getHeight();

            stage.setScene(
                    new Scene(root, 1180, 720)
            );

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 로그아웃

    @FXML
    private void logout(ActionEvent event) {

        try {

            Alert alert =
                    new Alert(Alert.AlertType.INFORMATION);

            alert.setTitle("로그아웃");
            alert.setHeaderText(null);
            alert.setContentText("로그아웃 되었습니다.");

            alert.showAndWait();

            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/login.fxml")
            );

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            double width = stage.getWidth();
            double height = stage.getHeight();

            stage.setScene(
                    new Scene(root, 1180, 720)
            );

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}