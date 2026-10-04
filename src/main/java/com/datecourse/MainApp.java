package com.datecourse;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.fxml.FXMLLoader;
import javafx.stage.Stage;

public class MainApp extends Application {

    public static void main(String[] args) {
        launch(args);
    }
    @Override
    public void start(Stage primaryStage) throws java.io.IOException {
        FXMLLoader loader = new FXMLLoader(MainApp.class.getResource("/com/datecourse/view/main.fxml"));
        Scene scene = new Scene(loader.load(), 1000, 680);

        primaryStage.setTitle("DateCourseApp");
        primaryStage.setMinWidth(850);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }


}
