package com.datecourse.controller;

import com.datecourse.model.*;
import com.datecourse.service.CoursePreviewImage;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.*;
import javafx.scene.control.ListView;
import javafx.scene.image.*;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class SavedCoursePreviewTest {
    @BeforeAll static void startFx() throws Exception {
        var ready=new CountDownLatch(1);
        try {Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException started){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(10,TimeUnit.SECONDS));
    }
    @Test void savedPngLoadsAsynchronouslyAndInvalidatedCoursesRefreshMissingPreview() throws Exception {
        var config=new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL");
        config.setUsername("sa");config.setPassword("");
        var pool=new com.zaxxer.hikari.HikariDataSource(config);
        try(pool) {
            try(var connection=pool.getConnection()) {
                for(String name:List.of("02_create_tables.sql","07_saved_courses.sql","08_saved_course_price_estimates.sql"))
                    try(var reader=Files.newBufferedReader(Path.of("data/sql",name))) {org.h2.tools.RunScript.execute(connection,reader);}
                connection.createStatement().executeUpdate("INSERT INTO members(member_id,email,password_hash,name) VALUES(7,'preview@test.test','test-only','Demo')");
            }
            var repository=new kr.ac.university.localdb.db.SavedCourseRepository(pool);
            var owner=new kr.ac.university.localdb.UserSession(7,"Demo");
            var plan=new RoutePlan(35.157,129.059,2,1000,"테스트",List.of(
                    new Place(1,"a","첫 장소","MEAL",35.158,129.06,9000,4)));
            var saved=repository.save(owner,plan);
            // Initially emulate a saved legacy course with no image.
            try(var c=pool.getConnection()){c.createStatement().executeUpdate("DELETE FROM saved_course_previews");}
            try(var context=new com.datecourse.service.UserContext(owner,null,()->repository.findAll(owner))) {
                context.courses().get(5,TimeUnit.SECONDS);
                var loader=new FXMLLoader(getClass().getResource("/saved-course-view.fxml"));
                var ready=new CountDownLatch(1);
                var load=new FutureTask<Parent>(()->{
                    Parent root=loader.load();new Scene(root,1000,680);
                    var status=(javafx.scene.control.Label)root.lookup("#previewStatus");
                    status.textProperty().addListener((o,old,value)->{if(value.startsWith("저장된 미리보기가 없어요"))ready.countDown();});
                    loader.<SavedCourseController>getController().configure(repository,context,course->{});
                    return root;
                });Platform.runLater(load);var root=load.get(5,TimeUnit.SECONDS);
                assertTrue(ready.await(5,TimeUnit.SECONDS));
                repository.save(owner,saved.courseId(),plan);context.invalidateCourses();context.courses().get(5,TimeUnit.SECONDS);
                var loaded=new CountDownLatch(1);
                var refresh=new FutureTask<Void>(()->{
                    ((ImageView)root.lookup("#coursePreview")).imageProperty().addListener((o,old,value)->{if(value!=null)loaded.countDown();});
                    loader.<SavedCourseController>getController().configure(repository,context,course->{});
                    return null;
                });Platform.runLater(refresh);refresh.get(5,TimeUnit.SECONDS);
                assertTrue(loaded.await(5,TimeUnit.SECONDS));
                var clear=new FutureTask<Void>(()->{loader.<SavedCourseController>getController().clear();
                    assertNull(((ImageView)root.lookup("#coursePreview")).getImage());return null;});
                Platform.runLater(clear);clear.get(5,TimeUnit.SECONDS);
            }
        }
    }

    @Test void previewFitsTheSavedCourseScreenAndKeepsPlaceListVisible() throws Exception {
        var work=new FutureTask<Void>(()->{
            var loader=new FXMLLoader(getClass().getResource("/saved-course-view.fxml"));
            var root=(Parent)loader.load();new Scene(root,1000,680);
            var plan=new RoutePlan(35.157,129.059,2,1000,"서면 오후 산책",List.of(
                    new Place(1,"a","고소한 한 끼","MEAL",35.158,129.06,9000,4),
                    new Place(2,"b","전포 골목 카페","CAFE",35.159,129.063,5000,4),
                    new Place(3,"c","작은 전시 공간","CULTURE",35.155,129.062,0,4)));
            var list=(ListView<SavedCourse>)root.lookup("#courseList");
            list.getItems().add(new SavedCourse(1,7,plan));list.getSelectionModel().selectFirst();
            var preview=(ImageView)root.lookup("#coursePreview");
            preview.setImage(new Image(new java.io.ByteArrayInputStream(new CoursePreviewImage().render(plan))));
            root.lookup("#previewStatus").setVisible(false);
            root.resize(1000,680);root.applyCss();root.layout();root.layout();
            var pane=(StackPane)root.lookup("#previewPane");
            assertTrue(preview.getBoundsInParent().getMinY()>=0);
            assertTrue(preview.getBoundsInParent().getMaxY()<=pane.getHeight()+1);
            assertTrue(pane.getBoundsInParent().getMinY()>=root.lookup("#detailSummary").getBoundsInParent().getMaxY());
            assertTrue(preview.getBoundsInParent().getWidth()<=pane.getWidth()+1);
            assertTrue(root.lookup("#placeList").getBoundsInParent().getHeight()>100);
            var target=Path.of("build/ui-preview/saved-course-preview.png");Files.createDirectories(target.getParent());
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",target.toFile());
            root.resize(920,620);root.layout();
            assertTrue(root.lookup("#placeList").getBoundsInParent().getHeight()>60);
            return null;
        });Platform.runLater(work);work.get(15,TimeUnit.SECONDS);
    }
}
