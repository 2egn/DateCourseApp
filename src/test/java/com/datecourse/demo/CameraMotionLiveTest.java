package com.datecourse.demo;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.embed.swing.SwingFXUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import java.util.concurrent.*;
import java.util.function.Supplier;
import java.nio.file.*;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit live SDK check, separate from offline regression tests. */
@EnabledIfEnvironmentVariable(named="RUN_CAMERA_LIVE_TEST", matches="true")
class CameraMotionLiveTest {
    private record Context(CameraMotionDemoApp app, Stage stage) {}
    @Test void loadsBothMapsAndExercisesSharedControls() throws Exception {
        CountDownLatch started=new CountDownLatch(1);
        try {Platform.startup(()->{Platform.setImplicitExit(false);started.countDown();});}
        catch(IllegalStateException ignored){Platform.runLater(started::countDown);}
        assertTrue(started.await(10,TimeUnit.SECONDS));
        Context context=fx(()->{try {var app=new CameraMotionDemoApp();var stage=new Stage();app.start(stage);return new Context(app,stage);}catch(Exception e){throw new RuntimeException(e);}});
        try {
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(40);
            while(System.nanoTime()<end && !fx(()->ready(context)))Thread.sleep(100);
            assertTrue(fx(()->ready(context)),fx(()->context.stage.getScene().getRoot().lookupAll(".label").stream().map(n->((javafx.scene.control.Label)n).getText()).toList().toString()));
            fx(()->{button(context,"가까운 곳 비교").fire();return null;});
            Thread.sleep(1200);
            fx(()->{((Slider)context.stage.getScene().getRoot().lookup(".slider")).setValue(1600);return null;});
            Thread.sleep(1500);
            fx(()->{button(context,"빠른 3곳 선택").fire();return null;});
            Thread.sleep(2500);
            fx(()->{
                var views=context.stage.getScene().getRoot().lookupAll("WebView");
                assertEquals(2,views.size());
                var selected=(com.datecourse.model.Place)((javafx.scene.control.ComboBox<?>)context.stage.getScene().getRoot().lookup(".combo-box")).getValue();
                for(var node:views) {
                    var view=(WebView)node;
                    assertEquals("undefined",view.getEngine().executeScript("typeof window.cameraDemoError"));
                    try {
                        var state=new com.fasterxml.jackson.databind.ObjectMapper().readTree((String)view.getEngine().executeScript("window.cameraMotionDemo.inspect()"));
                        assertTrue(state.get("ready").asBoolean());
                        assertFalse(state.get("moving").asBoolean());assertFalse(state.get("pending").asBoolean());
                        assertEquals(selected.lat(),state.get("lat").asDouble(),.0001);
                        assertEquals(selected.lng(),state.get("lng").asDouble(),.0001);
                        assertEquals(1600,state.get("radius").asInt());
                    }catch(java.io.IOException e){throw new RuntimeException(e);}
                }
                try {var path=Path.of("build/ui-preview/camera-motion-demo.png");Files.createDirectories(path.getParent());ImageIO.write(SwingFXUtils.fromFXImage(context.stage.getScene().getRoot().snapshot(null,null),null),"png",path.toFile());}
                catch(Exception e){throw new RuntimeException(e);}return null;
            });
        } finally {fx(()->{context.app.stop();context.stage.close();return null;});}
    }
    private static Button button(Context c,String text){return c.stage.getScene().getRoot().lookupAll(".button").stream().map(n->(Button)n).filter(b->text.equals(b.getText())).findFirst().orElseThrow();}
    private static boolean ready(Context c){return !button(c,"가까운 곳 비교").isDisabled();}
    private static <T>T fx(Supplier<T> work)throws Exception{FutureTask<T> task=new FutureTask<>(work::get);Platform.runLater(task);return task.get(10,TimeUnit.SECONDS);}
}
