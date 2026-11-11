package com.datecourse.controller;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

/** Explicit opt-in only: uses the user's configured Kakao and TMAP keys. */
@EnabledIfEnvironmentVariable(named="RUN_TMAP_LIVE_TEST", matches="true")
class WalkingRouteLiveTest {
    @BeforeAll static void startFx() throws Exception {
        CountDownLatch started=new CountDownLatch(1);
        try { Platform.startup(() -> {Platform.setImplicitExit(false);started.countDown();}); }
        catch(IllegalStateException alreadyStarted){Platform.runLater(started::countDown);}
        assertTrue(started.await(10,TimeUnit.SECONDS));
    }

    private record Context(Stage stage, Parent root, WebView view, PlannerMap map) {}

    @Test void displaysAnActualTmapWalkingRouteOnTheActualKakaoMap() throws Exception {
        assertEquals("localhost", com.datecourse.service.MapPageServer.sharedPage().getHost());
        Context context=fx(() -> {
            try {
                FXMLLoader loader=new FXMLLoader(getClass().getResource("/condition-view.fxml"));
                Parent root=loader.load();
                ConditionController controller=loader.getController();
                var member=ConditionController.class.getDeclaredField("map");member.setAccessible(true);
                PlannerMap map=(PlannerMap)member.get(controller);
                WebView view=(WebView)loader.getNamespace().get("mapView");
                Stage stage=new Stage();stage.setTitle("TMAP 도보 경로 실제 연동 확인");
                stage.setScene(new Scene(root,1200,850));stage.show();
                RoutePlan planned=new RoutePlan(35.1796,129.0756,2,1000,"도보 시험",List.of(
                        new Place(1,"live","부산 도보 시험 도착지","CULTURE",35.1813,129.0797,0,0)));
                var applied=ConditionController.class.getDeclaredField("appliedPlan");applied.setAccessible(true);applied.set(controller,planned);
                ((javafx.scene.control.Label)loader.getNamespace().get("resultLabel")).setText("TMAP 도보 경로 실제 연동 확인 · 1곳");
                map.present(planned,null,1000,false,true);
                return new Context(stage,root,view,map);
            } catch(Exception failure){throw new RuntimeException(failure);}
        });
        try {
            await(context,"!!window.javaMapBridge && typeof window.showWalkingRoute==='function' && document.getElementById('search-submit').disabled===false",25);
            fx(() -> {
                context.view().getEngine().executeScript("var originalWalking=window.showWalkingRoute;window.showWalkingRoute=function(route,message,fit){window.verifiedWalking=route;originalWalking(route,message,fit);};");
                context.map().requestWalkingRoute();
                return null;
            });
            await(context,"!!window.verifiedWalking && window.verifiedWalking.totalDistance>0 && !document.getElementById('walking-panel').hidden",25);
            assertTrue(fx(() -> context.view().getEngine().executeScript("document.getElementById('walking-summary').textContent")).toString().contains("TMAP 도보"));
            // Allow the newly fitted map tiles to arrive before recording the preview.
            Thread.sleep(1500);
            fx(() -> {
                try {
                    Path directory=Path.of("build/ui-preview");Files.createDirectories(directory);
                    ImageIO.write(SwingFXUtils.fromFXImage(context.root().snapshot(null,null),null),"png",directory.resolve("tmap-walking-live.png").toFile());
                    Files.writeString(Path.of(".local/tmap-java-verification.json"),context.view().getEngine().executeScript("JSON.stringify(window.verifiedWalking)").toString());
                    return null;
                }catch(Exception failure){throw new RuntimeException(failure);}
            });
        } finally { fx(() -> {context.view().getEngine().load(null);context.stage().close();return null;}); }
    }

    private void await(Context context,String condition,int seconds) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
        while(System.nanoTime()<deadline){
            if(Boolean.TRUE.equals(fx(() -> context.view().getEngine().executeScript(condition)))) return;
            Thread.sleep(100);
        }
        String diagnostic=fx(() -> context.view().getEngine().getLocation()+" / "+context.view().getEngine().getLoadWorker().getState()
                +" / "+((javafx.scene.control.Label)context.root().lookup("#errorLabel")).getText()
                +" / "+context.view().getEngine().executeScript("document.body ? document.body.textContent.slice(-700) : 'no body'"));
        fail("Live map did not reach the expected state: "+condition+" / "+diagnostic);
    }

    private static <T>T fx(Supplier<T> action)throws Exception{
        FutureTask<T> task=new FutureTask<>(action::get);Platform.runLater(task);return task.get(10,TimeUnit.SECONDS);
    }
}
