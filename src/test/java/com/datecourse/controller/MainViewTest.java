package com.datecourse.controller;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

class MainViewTest {
    @Test void loadsFxmlWithControllerFieldsAndSearchAction() throws Exception {
        CompletableFuture<Void> result = new CompletableFuture<>();
        Platform.startup(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/datecourse/view/main.fxml"));
                Parent root = loader.load();
                assertNotNull(root);
                assertInstanceOf(MainController.class, loader.getController());
                Button search = (Button) loader.getNamespace().get("searchButton");
                assertNotNull(search.getOnAction());
                result.complete(null);
            } catch (Throwable error) {
                result.completeExceptionally(error);
            }
        });
        try {
            result.get(15, TimeUnit.SECONDS);
        } finally {
            Platform.exit();
        }
    }
}
