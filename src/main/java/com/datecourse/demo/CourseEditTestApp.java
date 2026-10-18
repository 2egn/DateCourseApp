package com.datecourse.demo;

import com.datecourse.controller.CourseEditController;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class CourseEditTestApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/course-edit-view.fxml"));
        Parent root = loader.load();

        // 3번 화면이 추천 코스 3개 + 예산을 넘겨주는 상황을 흉내냄
        CourseEditController controller = loader.getController();
        controller.setCourses(FakeCourseData.getSavedCourses(), 30000);

        stage.setScene(new Scene(root));
        stage.setTitle("코스 수정 테스트");
        stage.show();
    }
}