package com.datecourse.controller;

import com.datecourse.model.RoutePlan;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.*;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class PlannerMapCaptureTest {
    static <T>T fx(Callable<T> work)throws Exception {
        var task=new FutureTask<>(work);Platform.runLater(task);return task.get(5,TimeUnit.SECONDS);
    }
    @BeforeAll static void start()throws Exception {
        var ready=new CountDownLatch(1);
        try{Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException started){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(5,TimeUnit.SECONDS));
    }
    @Test void firstCaptureWaitsForThePageAndSdkInsteadOfDroppingTheImage()throws Exception {
        var page=Files.createTempFile("capture-delayed-sdk-", ".html");page.toFile().deleteOnExit();
        Files.writeString(page,"""
                <html><body><div>test map</div><script>
                function connectJavaMapBridge(){setTimeout(function(){javaMapBridge.onMapReady();},450);}
                function inspectMapCamera(){return {moving:false};}
                function beginCourseCapture(){window.captureStarted=true;return true;}
                function courseCaptureReady(){return true;}
                function endCourseCapture(){window.captureStarted=false;}
                </script></body></html>
                """);
        var view=fx(()->{var v=new WebView();new Scene(new StackPane(v),800,500);v.resize(800,500);return v;});
        var map=fx(()->new PlannerMap(view,(lat,lng)->{},message->{},page::toUri));
        var phases=new java.util.concurrent.CopyOnWriteArrayList<PlannerMap.CaptureStage>();
        try {
            var capture=fx(()->{
                // Equivalent to first navigation: load is pending and the bridge is not ready yet.
                var loaded=PlannerMap.class.getDeclaredField("loaded");loaded.setAccessible(true);loaded.setBoolean(map,true);
                var uri=PlannerMap.class.getDeclaredField("page");uri.setAccessible(true);uri.set(map,page.toUri());
                view.getEngine().load(page.toUri().toString());
                return map.capturePreview(new RoutePlan(35.15,129.05,2,1000,"처음 저장",List.of()),phases::add);
            });
            var image=capture.get(5,TimeUnit.SECONDS);
            assertEquals(1000,image.getWidth());assertEquals(520,image.getHeight());
            assertEquals(List.of(PlannerMap.CaptureStage.PREPARING,PlannerMap.CaptureStage.CAPTURING),phases);
            fx(()->{assertTrue(view.isManaged());assertEquals(800,view.getWidth(),1);return null;});
        } finally {fx(()->{map.dispose();return null;});}
    }
}
