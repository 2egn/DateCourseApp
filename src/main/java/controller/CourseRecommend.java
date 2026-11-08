package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;

public class CourseRecommend {

    private static final int WIDTH = 1180;
    private static final int HEIGHT = 720;

    @FXML
    public void initialize() {

        // 카카오맵 API연결

    }


    // 홈으로 이동

    @FXML
    private void goHome(ActionEvent event) {

        try {

            Parent root = FXMLLoader.load(
                    getClass().getResource("/fxml/main.fxml")
            );

            Stage stage =
                    (Stage) ((Node) event.getSource())
                            .getScene()
                            .getWindow();

            stage.setScene(
                    new Scene(root, WIDTH, HEIGHT)
            );

            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 추천 코스 1 저장

    @FXML
    private void saveCourse1() {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("저장 완료");
        alert.setHeaderText(null);
        alert.setContentText("추천 코스 1이 저장되었습니다.");

        alert.showAndWait();

        // DB 저장 코드 추가
    }


    // 추천 코스 2 저장

    @FXML
    private void saveCourse2() {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("저장 완료");
        alert.setHeaderText(null);
        alert.setContentText("추천 코스 2가 저장되었습니다.");

        alert.showAndWait();

        //DB 저장 코드 추가
    }


    // 추천 코스 3 저장

    @FXML
    private void saveCourse3() {

        Alert alert =
                new Alert(Alert.AlertType.INFORMATION);

        alert.setTitle("저장 완료");
        alert.setHeaderText(null);
        alert.setContentText("추천 코스 3이 저장되었습니다.");

        alert.showAndWait();

        // DB 저장 코드 추가
    }
}