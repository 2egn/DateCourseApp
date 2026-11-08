package controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class FeedBack {

    @FXML
    private TextField titleField;

    @FXML
    private TextArea contentArea;

    @FXML
    private void submitFeedback(ActionEvent event) {

        String title = titleField.getText().trim();
        String content = contentArea.getText().trim();

        // 입력값 검사
        if (title.isEmpty() || content.isEmpty()) {

            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("입력 오류");
            alert.setHeaderText(null);
            alert.setContentText("제목과 내용을 모두 입력해주세요.");
            alert.showAndWait();

            return;
        }


        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("제출 완료");
        alert.setHeaderText(null);
        alert.setContentText("피드백이 제출되었습니다.");

        alert.showAndWait();

        // 피드백 창 닫기
        Stage stage = (Stage) ((Node) event.getSource())
                .getScene()
                .getWindow();

        stage.close();
    }
}