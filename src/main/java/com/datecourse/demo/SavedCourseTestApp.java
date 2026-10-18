package com.datecourse.demo;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class SavedCourseTestApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = FXMLLoader.load(getClass().getResource("/saved-course-view.fxml"));
        stage.setScene(new Scene(root));
        stage.setTitle("저장한 코스 테스트");
        stage.show();
    }
}