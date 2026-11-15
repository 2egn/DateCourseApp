package com.datecourse.controller;

import com.datecourse.model.*;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="RUN_COURSE_PREVIEW_LIVE_TEST",matches="true")
class CourseMapCaptureLiveTest {
    private record Context(Stage stage,WebView view,PlannerMap map) {}
    private static <T>T fx(Callable<T> action)throws Exception {
        var task=new FutureTask<>(action);Platform.runLater(task);return task.get(10,TimeUnit.SECONDS);
    }
    @Test void capturesActualMapWithStraightLinesAndRestoresThePlanner() throws Exception {
        var ready=new CountDownLatch(1);
        try {Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException started){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(10,TimeUnit.SECONDS));
        var plan=new RoutePlan(35.1578,129.0601,2,1000,"지도 캡처 확인",List.of(
                new Place(1,"a","첫 번째 장소", "MEAL",35.1555,129.058,9000,4),
                new Place(2,"b","두 번째 장소", "CAFE",35.158,129.063,5000,4)));
        var errors=new java.util.concurrent.CopyOnWriteArrayList<String>();
        var context=fx(()->{
            var view=new WebView();var map=new PlannerMap(view,(lat,lng)->{},errors::add);
            map.setWalkingRouter(stops->{throw new AssertionError("저장용 캡처는 도보 API를 호출하면 안 됩니다.");});
            var stage=new Stage();stage.setScene(new Scene(new StackPane(view),900,620));stage.show();
            map.present(plan,null,500,false,true);map.load();return new Context(stage,view,map);
        });
        try {
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(40);
            boolean loaded=false;
            while(System.nanoTime()<end) {
                loaded=fx(()->context.map.canCapturePreview());
                if(loaded)break;Thread.sleep(100);
            }
            assertTrue(loaded,"SDK 지도 로딩 실패: "+errors);
            fx(()->{context.view.getEngine().executeScript("var originalReady=window.courseCaptureReady;window.courseCaptureReady=function(){var ok=originalReady();if(ok)window.captureInspection=JSON.stringify({camera:window.inspectMapCamera(),labels:document.querySelectorAll('.course-capture-pin').length,attribution:Array.from(document.querySelectorAll('#map a')).some(function(n){var r=n.getBoundingClientRect();return r.y>=0 && r.bottom<=520 && r.width>0;})});return ok;}");return null;});
            var capture=fx(()->context.map.capturePreview(plan));
            var image=capture.get(15,TimeUnit.SECONDS);
            var inspection=new com.fasterxml.jackson.databind.ObjectMapper().readTree(
                    (String)fx(()->context.view.getEngine().executeScript("window.captureInspection")));
            assertEquals(3,inspection.path("labels").asInt());
            assertTrue(inspection.path("attribution").asBoolean());
            assertTrue(inspection.path("camera").path("level").asInt()<=4);
            assertEquals(1000,image.getWidth());assertEquals(520,image.getHeight());
            var png=new com.datecourse.service.CoursePreviewImage().renderWithMap(plan,image);
            var file=Path.of("build/ui-preview/saved-course-map-straight-live.png");Files.createDirectories(file.getParent());Files.write(file,png);
            fx(()->{
                assertEquals(false,context.view.getEngine().executeScript("document.body.classList.contains('course-capture')"));
                assertEquals(0,((Number)context.view.getEngine().executeScript("document.querySelectorAll('.course-capture-pin').length")).intValue());
                assertTrue(context.view.isManaged());assertEquals(900,context.view.getWidth(),1);return null;
            });
        } finally {fx(()->{context.map.dispose();context.stage.close();return null;});}
    }
}
