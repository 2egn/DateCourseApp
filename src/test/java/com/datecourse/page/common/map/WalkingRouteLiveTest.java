package com.datecourse.page.common.map;

import com.datecourse.page._01step_planner.CoursePlannerController;
import com.datecourse.page._02step_editor.CourseEditorController;

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

    private record Context(Stage stage, Parent root, WebView view, CourseMapView map) {}

    @Test void editorAutomaticallyShowsActualTmapTimesAfterAddingConfirmedStops() throws Exception {
        com.datecourse.service.LiveMapTestServer isolated=new com.datecourse.service.LiveMapTestServer();
        java.net.URI livePage=isolated.page();
        var editor=new java.util.concurrent.atomic.AtomicReference<CourseEditorController>();
        var places=new com.datecourse.service.PublicDemoPlaceRepository().findAll();
        var first=places.stream().filter(p->p.name().equals("대모골")).findFirst().orElseThrow();
        var second=places.stream().filter(p->p.name().equals("카페이떼")).findFirst().orElseThrow();
        Context context=fx(()->{
            try {
                FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));Parent root=loader.load();
                editor.set(loader.getController());
                var field=CourseEditorController.class.getDeclaredField("map");field.setAccessible(true);
                CourseMapView map=(CourseMapView)field.get(editor.get());
                var pageSource=CourseMapView.class.getDeclaredField("pageSource");pageSource.setAccessible(true);
                pageSource.set(map,(CourseMapView.PageSource)()->livePage);
                Stage stage=new Stage();stage.setTitle("장소 추가별 도보 시간 확인");stage.setScene(new Scene(root,1440,960));stage.show();
                editor.get().begin(new RoutePlan(35.1578,129.06,2,2000,"시간 확인",List.of(first)),places,p->{},()->{});
                return new Context(stage,root,(WebView)loader.getNamespace().get("editMapView"),map);
            } catch(Exception e){throw new RuntimeException(e);}
        });
        try {
            awaitEditorRoute(editor.get(),1);
            fx(()->{editor.get().begin(new RoutePlan(35.1578,129.06,2,2000,"시간 확인",List.of(first,second)),places,p->{},()->{});return null;});
            awaitEditorRoute(editor.get(),2);
            fx(()->{
                try {
                    var field=CourseEditorController.class.getDeclaredField("walkingRoute");field.setAccessible(true);
                    var route=(com.datecourse.model.TravelRoute)field.get(editor.get());
                    assertTrue(route.totalTime()>0);assertEquals(route.legs().stream().mapToLong(l->l.time()).sum(),route.totalTime());
                    assertTrue(((javafx.scene.control.Label)context.root().lookup("#walkingTimeLabel")).getText().contains(com.datecourse.model.TravelRoute.durationText(route.totalTime())));
                    ((javafx.scene.control.TitledPane)context.root().lookup("#coursePane")).setExpanded(true);
                    return null;
                }catch(Exception e){throw new RuntimeException(e);}
            });
            fx(()->{
                try {
                    context.root().applyCss();context.root().layout();Path directory=Path.of("build/ui-preview");Files.createDirectories(directory);
                    ImageIO.write(SwingFXUtils.fromFXImage(context.root().snapshot(null,null),null),"png",directory.resolve("walking-editor-times-live.png").toFile());return null;
                }catch(Exception e){throw new RuntimeException(e);}
            });
        } finally {fx(()->{editor.get().dispose();context.stage().close();return null;});isolated.close();}
    }

    private void awaitEditorRoute(CourseEditorController editor,int legs) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(35);
        while(System.nanoTime()<end) {
            if(fx(()->{
                try{var field=CourseEditorController.class.getDeclaredField("walkingRoute");field.setAccessible(true);
                    var route=(com.datecourse.model.TravelRoute)field.get(editor);return route!=null&&route.legs().size()==legs;}
                catch(Exception e){throw new RuntimeException(e);}
            }))return;
            Thread.sleep(100);
        }
        String status=fx(()->{
            try {
                var field=CourseEditorController.class.getDeclaredField("editMapView");field.setAccessible(true);WebView view=(WebView)field.get(editor);
                var error=CourseEditorController.class.getDeclaredField("editorError");error.setAccessible(true);
                var message=CourseEditorController.class.getDeclaredField("walkingMessage");message.setAccessible(true);
                var mapField=CourseEditorController.class.getDeclaredField("map");mapField.setAccessible(true);CourseMapView map=(CourseMapView)mapField.get(editor);
                var ready=CourseMapView.class.getDeclaredField("ready");ready.setAccessible(true);
                return "SDK ready="+ready.get(map)+", load="+view.getEngine().getLoadWorker().getState()+", walking="+message.get(editor)
                    +", notice="+((javafx.scene.control.Label)error.get(editor)).getText();
            }catch(Exception e){return "diagnostic unavailable";}
        });
        fail("Automatic editor walking route did not complete for "+legs+" legs / "+status);
    }

    @Test void displaysAnActualTmapWalkingRouteOnTheActualKakaoMap() throws Exception {
        assertEquals("localhost", com.datecourse.service.MapPageServer.sharedPage().getHost());
        Context context=fx(() -> {
            try {
                FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));
                Parent root=loader.load();
                CoursePlannerController controller=loader.getController();
                var member=CoursePlannerController.class.getDeclaredField("map");member.setAccessible(true);
                CourseMapView map=(CourseMapView)member.get(controller);
                WebView view=(WebView)loader.getNamespace().get("mapView");
                Stage stage=new Stage();stage.setTitle("TMAP 도보 경로 실제 연동 확인");
                stage.setScene(new Scene(root,1200,850));stage.show();
                RoutePlan planned=new RoutePlan(35.1796,129.0756,2,1000,"도보 시험",List.of(
                        new Place(1,"live","부산 도보 시험 도착지","CULTURE",35.1813,129.0797,0,0)));
                var present=CoursePlannerController.class.getDeclaredMethod("presentPlan",RoutePlan.class);present.setAccessible(true);present.invoke(controller,planned);
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
            await(context,"!!window.verifiedWalking && window.verifiedWalking.totalDistance>0 && document.querySelectorAll('.walking-leg-badge').length===window.verifiedWalking.legs.length",25);
            assertTrue(fx(() -> context.view().getEngine().executeScript("document.getElementById('walking-summary').textContent")).toString().contains("1구간"));
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

    @Test void productionCameraSettlesOnLatestGoalAndFitsRadiusAfterRelease() throws Exception {
        {
            java.net.URI page=com.datecourse.service.MapPageServer.sharedPage();
            Context context=fx(()->{
                WebView view=new WebView();var root=new javafx.scene.layout.StackPane(view);
                Stage stage=new Stage();stage.setScene(new Scene(root,1100,800));stage.show();
                CourseMapView map=new CourseMapView(view,(lat,lng)->{},message->{},()->page);
                map.present(new RoutePlan(35.1796,129.0756,2,500,"camera",List.of()),null,500,false,true);
                map.load();return new Context(stage,root,view,map);
            });
            try {
                await(context,"!!window.inspectMapCamera && !!inspectMapCamera() && !!inspectMapCamera().last && !inspectMapCamera().moving && !inspectMapCamera().pending",25);
                fx(()->{context.view().getEngine().executeScript("focusResultPlace(35.1806,129.0756);focusResultPlace(35.1816,129.0756);focusResultPlace(35.1826,129.0756)");return null;});
                await(context,"Math.abs(inspectMapCamera().lat-35.1826)<.00005 && !inspectMapCamera().moving && !inspectMapCamera().pending",15);
                assertEquals("PAN",fx(()->context.view().getEngine().executeScript("inspectMapCamera().last.movement")));
                fx(()->{
                    context.map().setRangeAdjusting(true);
                    context.map().present(new RoutePlan(35.1796,129.0756,2,200,"camera",List.of()),null,200,false,true);
                    return null;
                });
                await(context,"!inspectMapCamera().moving && !inspectMapCamera().pending",10);
                int before=((Number)fx(()->context.view().getEngine().executeScript("inspectMapCamera().level"))).intValue();
                fx(()->{context.map().setRangeAdjusting(false);context.map().fit(false);return null;});
                await(context,"!inspectMapCamera().moving && !inspectMapCamera().pending && inspectMapCamera().last.intent==='FIT'",10);
                int after=((Number)fx(()->context.view().getEngine().executeScript("inspectMapCamera().level"))).intValue();
                assertTrue(after<=before,"Releasing the smaller radius should fit or keep zoom");
                fx(()->{
                    var previous=new com.datecourse.model.Place(101,"previous","이전 장소","MEAL",35.1816,129.0756,3000,4);
                    var added=new com.datecourse.model.Place(102,"added","추가 장소","MEAL",35.1826,129.0756,3000,4);
                    context.map().present(new RoutePlan(35.1796,129.0756,2,2000,"camera",List.of(previous,added)),null,200,true,true);
                    return null;
                });
                await(context,"!inspectMapCamera().moving && !inspectMapCamera().pending",10);
                int overview=((Number)fx(()->context.view().getEngine().executeScript("inspectMapCamera().level"))).intValue();
                fx(()->{context.map().fitLeg(1);return null;});
                await(context,"Math.abs(inspectMapCamera().lat-35.1821)<.00005 && !inspectMapCamera().moving && !inspectMapCamera().pending",10);
                assertTrue(((Number)fx(()->context.view().getEngine().executeScript("inspectMapCamera().level"))).intValue()<overview);
                fx(()->{context.map().ensureRangesVisible();return null;});
                await(context,"Math.abs(inspectMapCamera().lat-35.1826)<.00005 && !inspectMapCamera().moving && !inspectMapCamera().pending",10);
                assertTrue(((Number)fx(()->context.view().getEngine().executeScript("inspectMapCamera().level"))).intValue()<overview);
                fx(()->{context.map().reset();return null;});
                await(context,"Math.abs(inspectMapCamera().lat-35.1796)<.00005 && inspectMapCamera().level===5 && !inspectMapCamera().pending",10);
            } finally {fx(()->{context.map().dispose();context.stage().close();return null;});}
        }
    }

    @Test void hiddenEditorMapInitializesAtVisibleSizeAndSurvivesLargeResize() throws Exception {
        java.net.URI page=com.datecourse.service.MapPageServer.sharedPage();
        Context context=fx(()->{
            WebView view=new WebView();view.setVisible(false);view.setManaged(false);
            var root=new javafx.scene.layout.StackPane(view);
            Stage stage=new Stage();stage.setScene(new Scene(root,1100,760));stage.show();
            CourseMapView map=new CourseMapView(view,(lat,lng)->{},message->{},()->page);
            map.present(new RoutePlan(35.1578,129.06,2,1000,"hidden",List.of()),null,500,true,true);
            map.load();return new Context(stage,root,view,map);
        });
        try {
            Thread.sleep(250);
            assertEquals(javafx.concurrent.Worker.State.READY,fx(()->context.view().getEngine().getLoadWorker().getState()));
            fx(()->{context.view().setVisible(true);context.view().setManaged(true);context.root().applyCss();context.root().layout();context.map().fit(false);return null;});
            await(context,"typeof window.inspectMapCamera==='function' && !!inspectMapCamera() && !!inspectMapCamera().last && !inspectMapCamera().moving && !inspectMapCamera().pending && inspectMapCamera().level<10 && Math.abs(inspectMapCamera().lat-35.1578)<.0001",20);
            fx(()->{context.stage().setWidth(2560);context.stage().setHeight(1000);return null;});
            await(context,"document.documentElement.clientWidth>2000 && inspectMapCamera().width===document.getElementById('map').clientWidth && inspectMapCamera().height===document.getElementById('map').clientHeight",10);
            fx(()->{context.map().fit(false);return null;});
            await(context,"!inspectMapCamera().moving && !inspectMapCamera().pending && inspectMapCamera().level<10 && Math.abs(inspectMapCamera().lat-35.1578)<.0001 && Math.abs(inspectMapCamera().lng-129.06)<.0001",10);
            fx(()->{
                try {Path directory=Path.of("build/ui-preview");Files.createDirectories(directory);
                    ImageIO.write(SwingFXUtils.fromFXImage(context.root().snapshot(null,null),null),"png",directory.resolve("map-hidden-resize-live.png").toFile());
                }catch(Exception e){throw new RuntimeException(e);}return null;
            });
        } finally {fx(()->{context.map().dispose();context.stage().close();return null;});}
    }

    private void await(Context context,String condition,int seconds) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
        while(System.nanoTime()<deadline){
            if(Boolean.TRUE.equals(fx(() -> context.view().getEngine().executeScript(condition)))) return;
            Thread.sleep(100);
        }
        String diagnostic=fx(() -> context.view().getEngine().getLocation()+" / "+context.view().getEngine().getLoadWorker().getState()
                +" / "+(context.root().lookup("#errorLabel")==null?"":((javafx.scene.control.Label)context.root().lookup("#errorLabel")).getText())
                +" / camera="+context.view().getEngine().executeScript("window.inspectMapCamera ? JSON.stringify(inspectMapCamera()) : 'no camera'")
                +" / "+context.view().getEngine().executeScript("document.body ? document.body.textContent.slice(-700) : 'no body'"));
        fail("Live map did not reach the expected state: "+condition+" / "+diagnostic);
    }

    private static <T>T fx(Supplier<T> action)throws Exception{
        FutureTask<T> task=new FutureTask<>(action::get);Platform.runLater(task);return task.get(10,TimeUnit.SECONDS);
    }
}
