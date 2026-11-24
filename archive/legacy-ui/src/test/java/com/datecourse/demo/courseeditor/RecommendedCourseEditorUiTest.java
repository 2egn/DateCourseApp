package com.datecourse.demo.courseeditor;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import static org.junit.jupiter.api.Assertions.*;

/** 통합 앱과 연결되지 않은 추천 코스 편집 예제의 보관용 검사다. */
class RecommendedCourseEditorUiTest {
    @BeforeAll static void startJavaFx() throws Exception {
        CountDownLatch ready=new CountDownLatch(1);
        try { Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();}); }
        catch(IllegalStateException alreadyStarted) { Platform.runLater(ready::countDown); }
        assertTrue(ready.await(10,TimeUnit.SECONDS));
    }
    @Test
    void recommendationOpensInEditorWithEditablePlacesInOrder() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/datecourse/demo/courseeditor/recommended-course-editor-view.fxml"));
            Parent root = loader.load();
            RecommendedCourseEditorController controller = loader.getController();
            var course = com.datecourse.service.RecommendedCourseMapper.toCourse(java.util.List.of(
                    new com.datecourse.model.Place(8, "b", "두 번째 ID", "CAFE", 35, 129, 5000, 4),
                    new com.datecourse.model.Place(1, "a", "첫 번째 ID", "MEAL", 35.1, 129.1, 10000, 4)), 2);
            controller.setRecommendedCourse(course, 40000);
            javafx.scene.control.ListView<?> places = (javafx.scene.control.ListView<?>) root.lookup("#placeList");
            assertEquals(course.coursePlaces(), places.getItems());
            assertEquals("총 30,000원 · 2곳", ((javafx.scene.control.Label) root.lookup("#summaryLabel")).getText());
            assertFalse(((javafx.scene.control.Button) root.lookup("#clearButton")).isDisabled());
            controller.setRecommendedCourse(course, 40000);
            assertEquals(2, places.getItems().size());
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
    }

}
