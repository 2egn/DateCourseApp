package com.datecourse;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) {
        Label label = new Label("예산 맞춤형 데이트 코스 추천 프로그램 실행 테스트");
        StackPane root = new StackPane(label);
        Scene scene = new Scene(root, 600, 400);

        primaryStage.setTitle("DateCourseApp");
        primaryStage.setScene(scene);
        primaryStage.show();
    }


}