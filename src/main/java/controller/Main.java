package controller;

import javafx.animation.ScaleTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.stage.Stage;
import javafx.util.Duration;

public class Main {

    //화면 전환 공통 메서드

    private void changePage(Node node, String fxmlFile) {

        try {

            var page = getClass().getResource("/fxml/" + fxmlFile);
            if (page == null) {
                showNotice(node,"아직 연결되지 않은 예제 화면이에요. 실제 코스 기능은 기본 앱에서 이용해 주세요.");
                return;
            }
            Parent root = FXMLLoader.load(page);

            Stage stage =
                    (Stage) node.getScene().getWindow();

            double width = stage.getWidth();
            double height = stage.getHeight();

            stage.setScene(
                    new Scene(root, width, height)
            );

            stage.show();

        } catch (Exception e) {
            com.datecourse.service.AppDiagnostics.failure(com.datecourse.service.AppDiagnostics.Operation.SCREEN_LOAD,
                    com.datecourse.service.AppDiagnostics.Stage.START,e);
            showNotice(node,"화면을 열지 못했어요. 잠시 후 다시 시도해 주세요.");
        }
    }


    static void showNotice(Node source,String message) {
        var alert=new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION,message);
        alert.setTitle("예제 화면 안내");alert.setHeaderText(null);
        if(source.getScene()!=null && source.getScene().getWindow()!=null)alert.initOwner(source.getScene().getWindow());
        com.datecourse.ui.DialogTheme.apply(alert);alert.showAndWait();
    }

    // 왼쪽 메뉴

    @FXML
    public void goCourseRecommend(MouseEvent event) {

        changePage(
                (Node) event.getSource(),
                "courseRecommend.fxml"
        );
    }

    @FXML
    public void goSavedCourse(MouseEvent event) {

        changePage(
                (Node) event.getSource(),
                "savedCourse.fxml"
        );
    }

    @FXML
    public void goCommunity(MouseEvent event) {

        changePage(
                (Node) event.getSource(),
                "community.fxml"
        );
    }

    @FXML
    public void goMyPage(MouseEvent event) {

        changePage(
                (Node) event.getSource(),
                "myPage.fxml"
        );
    }

    //피드백은 새 창으로 열기

    @FXML
    public void goFeedback(MouseEvent event) {

        try {

            FXMLLoader loader =
                    new FXMLLoader(
                            getClass().getResource("/fxml/feedBack.fxml")
                    );

            Parent root = loader.load();

            Stage feedbackStage = new Stage();

            feedbackStage.setTitle("피드백");

            feedbackStage.setScene(
                    new Scene(root, 800, 650)
            );

            feedbackStage.setResizable(false);

            feedbackStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 빠른 메뉴 버튼

    @FXML
    public void goCourseRecommendAction(ActionEvent event) {

        changePage(
                (Node) event.getSource(),
                "courseRecommend.fxml"
        );
    }

    @FXML
    public void goCommunityButton(ActionEvent event) {

        changePage(
                (Node) event.getSource(),
                "community.fxml"
        );
    }

    @FXML
    public void goSavedCourseAction(ActionEvent event) {

        changePage(
                (Node) event.getSource(),
                "savedCourse.fxml"
        );
    }

    // 카드 Hover 애니메이션

    @FXML
    public void cardHoverEnter(MouseEvent event) {

        Region card =
                (Region) event.getSource();

        card.setCursor(Cursor.HAND);

        ScaleTransition scale =
                new ScaleTransition(
                        Duration.millis(120),
                        card
                );

        scale.setToX(1.01);
        scale.setToY(1.01);

        scale.play();
    }

    @FXML
    public void cardHoverExit(MouseEvent event) {

        Region card =
                (Region) event.getSource();

        ScaleTransition scale =
                new ScaleTransition(
                        Duration.millis(120),
                        card
                );

        scale.setToX(1.0);
        scale.setToY(1.0);

        scale.play();
    }
}