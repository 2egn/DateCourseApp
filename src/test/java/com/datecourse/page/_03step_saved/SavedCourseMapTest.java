package com.datecourse.page._03step_saved;

import com.datecourse.page.common.map.CourseMapView;
import com.datecourse.page.common.map.MapEventBridge;
import com.datecourse.page._01step_planner.CoursePlannerController;

import com.datecourse.model.*;
import com.datecourse.service.UserContext;
import com.datecourse.db.SavedCourseRepository;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class SavedCourseMapTest {
    private record Form(Parent root, SavedCourseController controller, CourseMapView map, WebView view) {}
    private static final RoutePlan PLAN=new RoutePlan(35.157,129.059,2,1000,"테스트",
            List.of(new Place(1,"a","첫 장소","MEAL",35.158,129.06,9000,4)));

    @BeforeAll static void startFx() throws Exception {
        var ready=new CountDownLatch(1);
        try {Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException started){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(10,TimeUnit.SECONDS));
    }

    private Form form() throws Exception {
        Form form=fx(()->{
            var loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_03step_saved/saved-course-view.fxml"));
            Parent root=loader.load();SavedCourseController controller=loader.getController();
            CourseMapView map=(CourseMapView)field(controller,"map");
            WebView view=(WebView)field(controller,"previewMap");
            // Exercise real controller callbacks without a map SDK, credentials or external requests.
            set(map,"loaded",true);
            view.getEngine().loadContent("""
                <html><script>
                window.presentRoute=function(){};window.showWalkingRoute=function(){};
                window.resetPlannerMap=function(){};window.setOriginLocked=function(){};
                window.setCandidateActionsEnabled=function(){};
                window.focusCoursePlace=function(id){window.focusedPlace=id;};
                window.fitConfirmedCourseView=function(){window.fitCalls=(window.fitCalls||0)+1;};
                </script></html>
                """);
            return new Form(root,controller,map,view);
        });
        awaitFx(()->Boolean.TRUE.equals(form.view.getEngine().executeScript("typeof window.presentRoute==='function'")));
        return form;
    }
    private void select(Form form) throws Exception {
        var list=(ListView<SavedCourse>)field(form.controller,"courseList");
        list.getItems().setAll(new SavedCourse(1,7,PLAN));list.getSelectionModel().selectFirst();
    }
    private void request(Form form) throws Exception {
        if (!(boolean)field(form.controller,"showingMap")) invoke(form.controller,"onToggleMap",null,null);
        ready(form);invoke(form.controller,"onRequestRoute",null,null);
    }
    private void awaitPreview(Form form) throws Exception {
        awaitFx(()->((javafx.scene.image.ImageView)field(form.controller,"coursePreview")).getImage()!=null);
    }
    private void ready(Form form) throws Exception {
        set(form.map,"ready",true);((Runnable)field(form.map,"onReady")).run();
    }
    private static TravelRoute route(List<TravelRoute.Stop> stops) {
        return new TravelRoute(List.of(new TravelRoute.Leg(stops.get(0),stops.get(1),100,60,List.of(),List.of())),100,60);
    }
    private static Label status(Form form) throws Exception {return (Label)field(form.controller,"mapStatus");}
    private static Button retry(Form form) throws Exception {return (Button)field(form.controller,"mapRetry");}

    @Test void selectingOrReopeningCourseNeverRequestsRoutesUntilUserClicks() throws Exception {
        Form form=form();var calls=new AtomicInteger();
        try {
            fx(()->{form.map.setRouteRouter((stops,mode)->{calls.incrementAndGet();return route(stops);});select(form);return null;});
            awaitPreview(form);
            fx(()->{ready(form);assertEquals(0,calls.get());
                assertFalse(((Button)field(form.controller,"requestRouteButton")).isDisabled());
                ((Button)field(form.controller,"requestRouteButton")).fire();return null;});
            awaitFx(()->field(form.map,"walkingRoute")!=null);assertEquals(1,calls.get());
            fx(()->{select(form);return null;});awaitPreview(form);
            fx(()->{assertNull(field(form.map,"walkingRoute"));assertEquals(1,calls.get());return null;});
        } finally {fx(()->{form.controller.dispose();return null;});}
    }

    @Test void lateRouteSuccessPreservesSdkFailureAndRetry() throws Exception {
        Form form=form();var started=new CountDownLatch(1);var release=new CountDownLatch(1);
        try {
            fx(()->{
                form.map.setRouteRouter((stops,mode)->{started.countDown();release.await(5,TimeUnit.SECONDS);return route(stops);});
                select(form);return null;
            });
            awaitPreview(form);fx(()->{request(form);return null;});
            assertTrue(started.await(5,TimeUnit.SECONDS));
            fx(()->{set(form.map,"ready",false);invoke(form.controller,"showMapError",String.class,"SDK 로딩 실패");return null;});
            release.countDown();awaitFx(()->field(form.map,"walkingRoute")!=null);
            fx(()->{
                assertFalse(form.map.isReady());assertTrue(status(form).isVisible());
                assertTrue(status(form).getText().contains("SDK 로딩 실패"));
                assertTrue(((javafx.scene.image.ImageView)field(form.controller,"coursePreview")).isVisible());assertTrue(retry(form).isVisible());
                assertTrue(((javafx.scene.Node)field(form.controller,"mapNotice")).isVisible());
                ready(form);assertFalse(status(form).isVisible());assertFalse(retry(form).isVisible());return null;
            });
        } finally {release.countDown();fx(()->{form.controller.dispose();return null;});}
    }

    @Test void routeSuccessWaitsForSdkBeforeDismissingLoadingNotice() throws Exception {
        Form form=form();
        try {
            fx(()->{form.map.setRouteRouter((stops,mode)->route(stops));select(form);return null;});
            awaitPreview(form);
            fx(()->{
                assertNull(field(form.map,"walkingRoute"));
                assertTrue(status(form).isVisible());assertTrue(status(form).getText().contains("지도를 불러오고"));
                assertFalse(retry(form).isVisible());ready(form);
                assertFalse(status(form).isVisible());return null;
            });
        } finally {fx(()->{form.controller.dispose();return null;});}
    }

    @Test void routeFailureSurvivesSdkReadinessAndRetryDoesNotReloadMap() throws Exception {
        Form form=form();var calls=new AtomicInteger();
        try {
            fx(()->{
                form.map.setRouteRouter((stops,mode)->{
                    if(calls.incrementAndGet()==1)throw new java.io.IOException("경로 조회 실패");return route(stops);
                });select(form);return null;
            });
            awaitPreview(form);fx(()->{request(form);return null;});
            awaitFx(()->!((String)field(form.controller,"routeFailure")).isBlank());
            fx(()->{
                ready(form);assertTrue(status(form).getText().contains("경로 조회 실패"));
                assertTrue(retry(form).isVisible());assertEquals("이동 경로 다시 조회",retry(form).getText());
                retry(form).fire();assertTrue(form.map.isReady());return null;
            });
            awaitFx(()->field(form.map,"walkingRoute")!=null);
            fx(()->{assertEquals(2,calls.get());assertFalse(status(form).isVisible());assertFalse(retry(form).isVisible());return null;});
        } finally {fx(()->{form.controller.dispose();return null;});}
    }

    @Test void logoutClosesSavedMapAndCancelsPendingRoute() throws Exception {
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);var returned=new CountDownLatch(1);
        CoursePlannerController main=fx(()->{
            var loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));loader.load();
            CoursePlannerController controller=loader.getController();invoke(controller,"prepareSavedPage",null,null);
            var saved=(SavedCourseController)field(controller,"savedController");CourseMapView map=(CourseMapView)field(saved,"map");
            set(map,"loaded",true);
            map.setRouteRouter((stops,mode)->{
                started.countDown();while(release.getCount()>0){try{release.await();}catch(InterruptedException ignored){}}
                returned.countDown();return route(stops);
            });
            invoke(saved,"showDetail",SavedCourse.class,new SavedCourse(1,7,PLAN));return controller;
        });
        try {
            var saved=(SavedCourseController)fx(()->field(main,"savedController"));
            awaitFx(()->((javafx.scene.image.ImageView)field(saved,"coursePreview")).getImage()!=null);
            fx(()->{CourseMapView map=(CourseMapView)field(saved,"map");set(map,"ready",true);invoke(saved,"onRequestRoute",null,null);return null;});
            assertTrue(started.await(5,TimeUnit.SECONDS));
            CourseMapView map=fx(()->(CourseMapView)field(saved,"map"));
            fx(()->{
                invoke(main,"logoutNow",null,null);
                assertTrue((boolean)field(field(map,"viewport"),"closed"));
                assertFalse(map.isReady());assertNull(field(map,"plan"));return null;
            });
            release.countDown();assertTrue(returned.await(5,TimeUnit.SECONDS));
            fx(()->{assertNull(field(map,"walkingRoute"));assertFalse(((Label)field(saved,"mapStatus")).isVisible());return null;});
        } finally {release.countDown();}
    }

    @Test void liveMapUsesSavedSnapshotAndKeepsCourseListCache() throws Exception {
        try(var pool=pool()) {
            var repository=new com.datecourse.db.SavedCourseRepository(pool);var owner=new com.datecourse.model.UserSession(7,"Demo");
            var saved=repository.saveWithoutPreview(owner,null,PLAN);var loads=new AtomicInteger();
            try(var context=new UserContext(owner,null,()->{loads.incrementAndGet();return repository.findAll(owner);})) {
                context.courses().get(5,TimeUnit.SECONDS);Form form=form();
                var opened=new java.util.concurrent.atomic.AtomicReference<SavedCourse>();
                try {
                    fx(()->{ready(form);form.map.setRouteRouter((stops,mode)->route(stops));form.controller.configure(repository,context,opened::set);return null;});
                    awaitFx(()->((ListView<?>)field(form.controller,"courseList")).getSelectionModel().getSelectedItem()!=null);
                    awaitPreview(form);
                    fx(()->{
                        assertEquals(PLAN,field(form.map,"plan"));
                        assertEquals(PLAN.places().size(),((TableView<?>)field(form.controller,"placeTable")).getItems().size());
                        invoke(form.controller,"onOpenClick",null,null);assertEquals(saved,opened.get());
                        form.controller.configure(repository,context,opened::set);return null;
                    });
                    awaitFx(()->!((boolean)field(form.controller,"busy")));
                    assertEquals(1,loads.get());assertEquals(List.of(saved),repository.findAll(owner));
                } finally {fx(()->{form.controller.dispose();return null;});}
            }
        }
    }

    @Test void storedImageSkipsMapAndRoutingAndLateImageCannotRestoreClearedCourse() throws Exception {
        try(var pool=pool()) {
            var repository=new com.datecourse.db.SavedCourseRepository(pool);var owner=new com.datecourse.model.UserSession(7,"Demo");
            byte[] png=new com.datecourse.service.CoursePreviewImage().render(PLAN);
            var saved=repository.saveWithPreview(owner,null,PLAN,png);
            try(var context=new UserContext(owner,null,()->repository.findAll(owner))) {
                context.courses().get(5,TimeUnit.SECONDS);Form form=form();var calls=new AtomicInteger();
                try {
                    fx(()->{form.map.setRouteRouter((stops,mode)->{calls.incrementAndGet();return route(stops);});
                        form.controller.configure(repository,context,c->{});return null;});
                    awaitPreview(form);
                    fx(()->{
                        assertTrue((boolean)field(form.controller,"storedPreview"));assertFalse(form.view.isVisible());
                        assertFalse((boolean)field(form.controller,"showingMap"));assertNull(field(form.map,"plan"));assertEquals(0,calls.get());
                        assertTrue(((Label)field(form.controller,"previewCaption")).getText().contains("저장된"));return null;
                    });
                    var started=new CountDownLatch(1);var release=new CountDownLatch(1);
                    context.invalidateCourses();
                    var pending=context.preview(saved,()->{started.countDown();release.await(5,TimeUnit.SECONDS);return Optional.of(png);});
                    try {
                        assertTrue(started.await(5,TimeUnit.SECONDS));
                        fx(()->{form.controller.configure(repository,context,c->{});return null;});
                        awaitFx(()->!((boolean)field(form.controller,"busy")));
                        fx(()->{form.controller.clear();return null;});release.countDown();pending.get(5,TimeUnit.SECONDS);
                        // Wait for the decoding callback as well as the original BLOB read.
                        ((CompletableFuture<?>)fx(()->field(form.controller,"previewTask"))).handle((v,e)->null).get(5,TimeUnit.SECONDS);
                        fx(()->{assertNull(((javafx.scene.image.ImageView)field(form.controller,"coursePreview")).getImage());
                            assertNull(field(form.controller,"previewCourse"));assertFalse(form.view.isVisible());return null;});
                    } finally {release.countDown();}
                } finally {fx(()->{form.controller.dispose();return null;});}
            }
        }
    }

    @Test void missingCorruptAndUnreadablePreviewStillAllowsCourseOpenWithoutRouting() throws Exception {
        try(var pool=pool()) {
            var repository=new com.datecourse.db.SavedCourseRepository(pool);var owner=new com.datecourse.model.UserSession(7,"Demo");
            var saved=repository.saveWithoutPreview(owner,null,PLAN);
            for(String scenario:List.of("missing","corrupt","readFailure")) {
                try(var context=new UserContext(owner,null,()->repository.findAll(owner))) {
                    context.courses().get(5,TimeUnit.SECONDS);
                    var cached=context.preview(saved,()->{
                        if(scenario.equals("readFailure"))throw new java.sql.SQLException("simulated preview read failure");
                        return scenario.equals("corrupt")?Optional.of(new byte[]{1,2,3}):Optional.empty();
                    });
                    cached.handle((v,e)->null).get(5,TimeUnit.SECONDS);
                    Form form=form();var calls=new AtomicInteger();var opened=new java.util.concurrent.atomic.AtomicReference<SavedCourse>();
                    try {
                        fx(()->{form.map.setRouteRouter((stops,mode)->{calls.incrementAndGet();return route(stops);});
                            form.controller.configure(repository,context,opened::set);return null;});
                        awaitPreview(form);
                        fx(()->{
                            assertFalse((boolean)field(form.controller,"storedPreview"),scenario);
                            assertTrue(((javafx.scene.image.ImageView)field(form.controller,"coursePreview")).isVisible(),scenario);
                            assertEquals(0,calls.get(),scenario);
                            assertEquals(PLAN.places().size(),((TableView<?>)field(form.controller,"placeTable")).getItems().size(),scenario);
                            invoke(form.controller,"onOpenClick",null,null);assertEquals(saved,opened.get(),scenario);return null;
                        });
                    } finally {fx(()->{form.controller.dispose();return null;});}
                }
            }
        }
    }

    @Test void fallbackAndRetryControlsFitSmallAndLargeWindows() throws Exception {
        Form form=form();
        try {
            fx(()->{form.map.setRouteRouter((stops,mode)->{fail("Window layout must not request routes");return null;});select(form);return null;});
            awaitPreview(form);
            fx(()->{
                var scene=new Scene(form.root,920,620);scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
                invoke(form.controller,"showMapError",String.class,"지도를 불러오지 못했어요. 네트워크 연결을 확인한 뒤 다시 시도해 주세요.");
                for(int[] size:List.of(new int[]{920,620},new int[]{1100,760},new int[]{1600,1000})) {
                    form.root.resize(size[0],size[1]);form.root.applyCss();form.root.layout();form.root.layout();
                    assertTrue(retry(form).getWidth()>=retry(form).prefWidth(-1)-1,"Retry label at "+size[0]);
                    for(String id:List.of("#mapPane","#bottomScroll","#toggleMapButton","#mapRetry","#openButton")) {
                        var node=form.root.lookup(id);var bounds=node.localToScene(node.getBoundsInLocal());
                        assertTrue(bounds.getMinX()>=0&&bounds.getMaxX()<=size[0]+1,id+" at "+size[0]);
                        assertTrue(bounds.getMinY()>=0&&bounds.getMaxY()<=size[1]+1,id+" at "+size[1]);
                    }
                    var target=Path.of("build/ui-preview/verified-preview-"+size[0]+"x"+size[1]+".png");Files.createDirectories(target.getParent());
                    javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(form.root.snapshot(null,null),null),"png",target.toFile());
                }
                return null;
            });
        } finally {fx(()->{form.controller.dispose();return null;});}
    }

    @Test void masterNewCourseActionAndPlaceFocusPreserveOptInRouting() throws Exception {
        try(var pool=pool()) {
            var repository=new com.datecourse.db.SavedCourseRepository(pool);var owner=new com.datecourse.model.UserSession(7,"Demo");
            var saved=repository.saveWithPreview(owner,null,PLAN,new com.datecourse.service.CoursePreviewImage().render(PLAN));
            try(var context=new UserContext(owner,null,()->List.of(saved))) {
                context.courses().get(5,TimeUnit.SECONDS);Form form=form();var routes=new AtomicInteger();var newCourses=new AtomicInteger();
                try {
                    fx(()->{form.map.setRouteRouter((stops,mode)->{routes.incrementAndGet();return route(stops);});
                        form.controller.configure(repository,context,c->{},newCourses::incrementAndGet);return null;});
                    awaitPreview(form);
                    fx(()->{
                        assertTrue((boolean)field(form.controller,"storedPreview"));assertFalse(form.view.isVisible());
                        invoke(form.controller,"onNewCourseClick",null,null);assertEquals(1,newCourses.get());
                        var scene=new Scene(form.root,1100,760);scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());form.root.resize(1100,760);form.root.applyCss();form.root.layout();
                        long imageVersion=(long)field(form.controller,"previewLoadVersion");
                        var table=(TableView<?>)field(form.controller,"placeTable");
                        var row=table.lookupAll(".table-row-cell").stream().filter(n->n instanceof TableRow<?> r&&!r.isEmpty()).map(n->(TableRow<?>)n).findFirst().orElseThrow();
                        row.getOnMouseClicked().handle(null);
                        assertEquals(imageVersion,field(form.controller,"previewLoadVersion"));
                        assertFalse((boolean)field(form.controller,"showingMap"));
                        assertTrue(form.root.lookup("#mapLoadPrompt").isVisible());
                        assertTrue(form.root.lookup("#previewViewControls").isVisible());
                        invoke(form.controller,"onLoadMap",null,null);
                        assertTrue((boolean)field(form.controller,"showingMap"));assertEquals(PLAN,field(form.map,"plan"));
                        ((MapEventBridge)field(form.map,"bridge")).onMapReady();
                        assertEquals(1,((Number)form.view.getEngine().executeScript("window.focusedPlace")).intValue());
                        assertEquals(0,routes.get());return null;
                    });
                    Thread.sleep(220); // The delayed whole-course fit must not override a place the user clicked.
                    fx(()->{assertEquals(0,((Number)form.view.getEngine().executeScript("window.fitCalls||0")).intValue());return null;});
                } finally {fx(()->{form.controller.dispose();return null;});}
            }
        }
    }

    @Test void imageOnlyCourseNeedsExplicitLoadAndPreparedStateSurvivesReselection() throws Exception {
        try(var pool=pool()) {
            var repository=new com.datecourse.db.SavedCourseRepository(pool);var owner=new UserSession(7,"Demo");
            var saved=repository.saveWithPreview(owner,null,PLAN,new com.datecourse.service.CoursePreviewImage().render(PLAN));
            try(var context=new UserContext(owner,null,()->List.of(saved))) {
                context.courses().get(5,TimeUnit.SECONDS);Form form=form();
                try {
                    fx(()->{form.controller.configure(repository,context,c->{});return null;});awaitPreview(form);
                    fx(()->{
                        assertTrue(form.root.lookup("#previewViewControls").isVisible());
                        invoke(form.controller,"onSelectMap",null,null);
                        assertTrue(form.root.lookup("#mapLoadPrompt").isVisible());
                        assertNull(field(form.map,"plan"));
                        invoke(form.controller,"onSelectImage",null,null);
                        assertFalse(form.root.lookup("#mapLoadPrompt").isVisible());
                        invoke(form.controller,"onSelectMap",null,null);invoke(form.controller,"onLoadMap",null,null);ready(form);
                        return null;
                    });
                    awaitFx(()->(boolean)field(form.controller,"mapPrepared"));
                    assertTrue(context.isMapPrepared(saved));
                    fx(()->{var list=(ListView<?>)field(form.controller,"courseList");list.getSelectionModel().clearSelection();list.getSelectionModel().selectFirst();return null;});
                    awaitPreview(form);
                    fx(()->{
                        assertTrue((boolean)field(form.controller,"mapPrepared"));
                        invoke(form.controller,"onSelectMap",null,null);
                        assertFalse(form.root.lookup("#mapLoadPrompt").isVisible());
                        assertTrue((boolean)field(form.controller,"showingMap"));
                        assertEquals(PLAN,field(form.map,"plan"));return null;
                    });
                } finally {fx(()->{form.controller.dispose();return null;});}
            }
        }
    }

    @Test void keyboardSelectionRequiresEnterAndStillRespectsExplicitMapLoad() throws Exception {
        var second = new Place(2,"b","두 번째 장소","CAFE",35.159,129.061,5000,4);
        var plan = new RoutePlan(PLAN.startLat(),PLAN.startLng(),2,1000,"키보드",List.of(PLAN.places().get(0),second));
        try(var pool=pool()) {
            var repository=new SavedCourseRepository(pool);var owner=new UserSession(7,"Demo");
            var saved=repository.saveWithPreview(owner,null,plan,new com.datecourse.service.CoursePreviewImage().render(plan));
            try(var context=new UserContext(owner,null,()->List.of(saved))) {
                context.courses().get(5,TimeUnit.SECONDS);Form form=form();var calls=new AtomicInteger();
                try {
                    fx(()->{form.map.setRouteRouter((stops,mode)->{calls.incrementAndGet();return route(stops);});
                        form.controller.configure(repository,context,c->{});return null;});
                    awaitPreview(form);
                    fx(()->{
                        new Scene(form.root,1100,760);form.root.resize(1100,760);form.root.applyCss();form.root.layout();
                        var table=(TableView<?>)field(form.controller,"placeTable");
                        table.getSelectionModel().selectFirst();
                        table.fireEvent(new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,"","",
                                javafx.scene.input.KeyCode.DOWN,false,false,false,false));
                        assertEquals(1,table.getSelectionModel().getSelectedIndex());
                        assertNull(field(form.map,"plan"));assertNull(field(form.controller,"pendingFocusPlace"));
                        table.fireEvent(new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,"","",
                                javafx.scene.input.KeyCode.ENTER,false,false,false,false));
                        assertFalse((boolean)field(form.controller,"showingMap"));
                        assertEquals(second,field(form.controller,"pendingFocusPlace"));
                        assertTrue(form.root.lookup("#mapLoadPrompt").isVisible());
                        invoke(form.controller,"onLoadMap",null,null);ready(form);
                        assertEquals(2,((Number)form.view.getEngine().executeScript("window.focusedPlace")).intValue());
                        assertEquals(0,calls.get());return null;
                    });
                } finally {fx(()->{form.controller.dispose();return null;});}
            }
        }
    }

    @Test void delayedPreviewDoesNotBypassExplicitMapLoading() throws Exception {
        var release=new CountDownLatch(1);var started=new CountDownLatch(1);
        try(var pool=pool()) {
            var repository=new com.datecourse.db.SavedCourseRepository(pool);var owner=new UserSession(7,"Demo");
            var png=new com.datecourse.service.CoursePreviewImage().render(PLAN);
            var saved=repository.saveWithPreview(owner,null,PLAN,png);
            try(var context=new UserContext(owner,null,()->List.of(saved))) {
                context.courses().get(5,TimeUnit.SECONDS);
                context.preview(saved,()->{started.countDown();release.await(10,TimeUnit.SECONDS);return Optional.of(png);});
                assertTrue(started.await(5,TimeUnit.SECONDS));Form form=form();
                try {
                    fx(()->{form.controller.configure(repository,context,c->{});return null;});
                    awaitFx(()->field(form.controller,"previewCourse")!=null);
                    fx(()->{
                        new Scene(form.root,1100,760);form.root.resize(1100,760);form.root.applyCss();form.root.layout();
                        var table=(TableView<?>)field(form.controller,"placeTable");
                        var row=(TableRow<?>)table.lookupAll(".table-row-cell").stream().filter(n->n instanceof TableRow<?> r&&!r.isEmpty()).findFirst().orElseThrow();
                        row.getOnMouseClicked().handle(null);
                        assertFalse((boolean)field(form.controller,"showingMap"));assertNull(field(form.map,"plan"));
                        assertTrue(form.root.lookup("#mapLoadPrompt").isVisible());assertTrue(((Button)field(form.controller,"mapLoadButton")).isDisabled());
                        invoke(form.controller,"onLoadMap",null,null);assertNull(field(form.map,"plan"));return null;
                    });
                    release.countDown();awaitPreview(form);
                    fx(()->{
                        assertFalse((boolean)field(form.controller,"showingMap"));
                        assertFalse(((Button)field(form.controller,"mapLoadButton")).isDisabled());
                        invoke(form.controller,"onLoadMap",null,null);ready(form);
                        assertEquals(PLAN,field(form.map,"plan"));
                        assertEquals(1,((Number)form.view.getEngine().executeScript("window.focusedPlace")).intValue());return null;
                    });
                } finally {release.countDown();fx(()->{form.controller.dispose();return null;});}
            }
        } finally {release.countDown();}
    }

    @Test void savedTimeCacheRejectsOldWalkingOptionButKeepsDrivingTime(@org.junit.jupiter.api.io.TempDir Path root) throws Exception {
        Form form=form();
        var walking=new SavedCourse(1,7,PLAN);
        var driving=new SavedCourse(2,7,PLAN.withTravelMode(TravelMode.DRIVING));
        var route=new TravelRoute(List.of(),100,60);
        try {
            fx(()->{((ListView<SavedCourse>)field(form.controller,"courseList")).getItems().setAll(walking,driving);
                ((ListView<?>)field(form.controller,"courseList")).getSelectionModel().selectFirst();return null;});
            awaitPreview(form);
            fx(()->{((java.util.function.BiConsumer<TravelRoute,String>)field(form.map,"walkingResult")).accept(route,"");
                assertFalse(((Label)field(form.controller,"travelSummaryLabel")).getText().contains("조회 전"));
                ((ListView<?>)field(form.controller,"courseList")).getSelectionModel().select(1);return null;});awaitPreview(form);
            fx(()->{((java.util.function.BiConsumer<TravelRoute,String>)field(form.map,"walkingResult")).accept(route,"");return null;});
            var save=com.datecourse.service.TmapSettings.class.getDeclaredMethod("saveSearchOption",Path.class,int.class);
            save.setAccessible(true);save.invoke(null,root,30);
            fx(()->{((ListView<?>)field(form.controller,"courseList")).getSelectionModel().selectFirst();return null;});awaitPreview(form);
            fx(()->{assertTrue(((Label)field(form.controller,"travelSummaryLabel")).getText().contains("조회 전"));
                ((ListView<?>)field(form.controller,"courseList")).getSelectionModel().select(1);return null;});awaitPreview(form);
            fx(()->{assertFalse(((Label)field(form.controller,"travelSummaryLabel")).getText().contains("조회 전"));return null;});
        } finally {fx(()->{form.controller.dispose();return null;});}
    }

    private static com.zaxxer.hikari.HikariDataSource pool() throws Exception {
        var config=new com.zaxxer.hikari.HikariConfig();config.setJdbcUrl("jdbc:h2:mem:"+UUID.randomUUID()+";MODE=MySQL");
        config.setUsername("sa");config.setPassword("");var pool=new com.zaxxer.hikari.HikariDataSource(config);
        try(var connection=pool.getConnection()) {
            for(String name:List.of("02_create_tables.sql","07_saved_courses.sql","08_saved_course_price_estimates.sql"))
                try(var reader=Files.newBufferedReader(Path.of("data/sql",name))){org.h2.tools.RunScript.execute(connection,reader);}
            connection.createStatement().executeUpdate("INSERT INTO members(member_id,email,password_hash,name) VALUES(7,'map@test.test','test-only','Demo')");
        }
        com.datecourse.maintenance.DatabaseSetup.preparePreviews(pool);
        com.datecourse.maintenance.DatabaseSetup.prepareTransport(pool);
        return pool;
    }

    @Test void mapAndInformationTablesRemainVisibleInCompactWindow() throws Exception {
        Form form=form();
        try {
            fx(()->{form.map.setRouteRouter((stops,mode)->route(stops));select(form);return null;});
            awaitPreview(form);
            fx(()->{
                var scene=new Scene(form.root,1100,760);scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());form.root.resize(1100,760);form.root.applyCss();form.root.layout();
                var map=form.root.lookup("#mapPane").localToScene(form.root.lookup("#mapPane").getBoundsInLocal());
                var info=form.root.lookup("#bottomScroll").localToScene(form.root.lookup("#bottomScroll").getBoundsInLocal());
                assertTrue(map.getHeight()>=180);assertTrue(map.getWidth()>400);
                assertTrue(map.getMaxX()<=1100);
                assertTrue(map.getMaxY()<=info.getMinY());assertTrue(info.getMaxY()<760);
                assertTrue(form.root.lookup("#summaryTable").isVisible());assertTrue(form.root.lookup("#placeTable").isVisible());
                assertEquals(0,javafx.scene.layout.GridPane.getRowIndex(form.root.lookup("#placeTable")));
                assertEquals(1,javafx.scene.layout.GridPane.getRowIndex(form.root.lookup("#summaryTable")));
                var places=form.root.lookup("#placeTable").localToScene(form.root.lookup("#placeTable").getBoundsInLocal());
                assertTrue(places.getWidth()>600);
                var scroll=(ScrollPane)form.root.lookup("#bottomScroll");
                scroll.setVvalue(1);form.root.layout();
                var summary=form.root.lookup("#summaryTable").localToScene(form.root.lookup("#summaryTable").getBoundsInLocal());
                assertTrue(summary.getMaxY()<=info.getMaxY()+1,"요약 표는 아래로 스크롤해서 읽을 수 있어야 함");
                scroll.setVvalue(0);form.root.layout();

                var screenshot=Path.of("build/ui-preview/saved-preview-policy.png");Files.createDirectories(screenshot.getParent());
                javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(form.root.snapshot(null,null),null),"png",screenshot.toFile());
                return null;
            });
        } finally {fx(()->{form.controller.dispose();return null;});}
    }

    private static Object field(Object target,String name) throws Exception {var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private static void set(Object target,String name,Object value) throws Exception {var f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    private static void invoke(Object target,String name,Class<?> type,Object value) throws Exception {
        var method=type==null?target.getClass().getDeclaredMethod(name):target.getClass().getDeclaredMethod(name,type);
        method.setAccessible(true);if(type==null)method.invoke(target);else method.invoke(target,value);
    }
    private static <T>T fx(Callable<T> action) throws Exception {var task=new FutureTask<>(action);Platform.runLater(task);return task.get(10,TimeUnit.SECONDS);}
    private static void awaitFx(Callable<Boolean> condition) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(6);
        while(System.nanoTime()<end){if(fx(condition))return;Thread.sleep(20);}fail("화면 처리 시간 초과");
    }
}
