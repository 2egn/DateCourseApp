package com.datecourse.demo;

import com.datecourse.page._01step_planner.CoursePlannerController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Public demonstration places, with no member DB connection. */
public final class PilotPlannerApp extends Application {
    @Override public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));
        stage.setScene(new Scene(loader.load(), 1440, 960));
        stage.setTitle("데이트 코스 · 서면·전포 시연");
        stage.show();
        loader.<CoursePlannerController>getController().usePilotPlaces();
    }
}
