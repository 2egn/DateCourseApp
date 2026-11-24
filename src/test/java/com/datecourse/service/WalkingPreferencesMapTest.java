package com.datecourse.service;

import com.datecourse.model.*;
import com.datecourse.page.common.map.CourseMapView;
import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

/** 실제 키·설정 파일 대신 임시 설정과 모의 경로로 지도 사이의 무효화를 검사한다. */
class WalkingPreferencesMapTest {
    @TempDir Path root;
    private record Form(CourseMapView map,WebView view) {}
    private static final RoutePlan PLAN=new RoutePlan(35.157,129.06,2,1000,"도보",
            List.of(new Place(1,"a","장소","MEAL",35.158,129.06,9000,4)));
    @BeforeAll static void startFx() throws Exception {
        var ready=new CountDownLatch(1);
        try {Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException started){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(10,TimeUnit.SECONDS));
    }
    private Form form(boolean automatic,AtomicInteger calls,AtomicInteger option) throws Exception {
        Form form=fx(()->{
            WebView view=new WebView();var map=new CourseMapView(view,(lat,lng)->{},message->fail(message));
            map.setAutomaticWalking(automatic);
            map.setRouteRouter((stops,mode)->{calls.incrementAndGet();return result(60+option.get());});
            set(map,"loaded",true);
            view.getEngine().loadContent("<html><script>window.presentRoute=function(){};window.showWalkingRoute=function(){};</script></html>");
            return new Form(map,view);
        });
        await(()->form.view.getEngine().getLoadWorker().getState()==Worker.State.SUCCEEDED);
        fx(()->{set(form.map,"ready",true);return null;});return form;
    }
    @Test void sameStopsOnAnotherMapUseNewOptionWhenPresentedAgain() throws Exception {
        var calls=new AtomicInteger();var option=new AtomicInteger();
        Form first=form(true,calls,option),second=form(true,calls,option);
        try {
            fx(()->{first.map.presentCourse(PLAN,true);second.map.presentCourse(PLAN,true);return null;});
            await(()->route(first)!=null && route(second)!=null);assertEquals(2,calls.get());
            option.set(10);TmapSettings.saveSearchOption(root,10);
            fx(()->{first.map.refreshWalkingPreferences();second.map.presentCourse(PLAN,true);return null;});
            await(()->route(first)!=null && route(second)!=null && route(first).totalTime()==70 && route(second).totalTime()==70);
            assertEquals(4,calls.get());
            fx(()->{second.map.presentCourse(PLAN,true);return null;});assertEquals(4,calls.get());
        } finally {dispose(first,second);}
    }
    @Test void savedPreviewInvalidatesOldRouteWithoutRequestingAutomatically() throws Exception {
        var calls=new AtomicInteger();var option=new AtomicInteger();Form form=form(false,calls,option);
        try {
            fx(()->{form.map.presentCourse(PLAN,true);form.map.requestWalkingRoute();return null;});await(()->route(form)!=null);
            option.set(4);TmapSettings.saveSearchOption(root,4);
            fx(()->{form.map.presentCourse(PLAN,true);assertNull(route(form));return null;});assertEquals(1,calls.get());
            fx(()->{form.map.requestWalkingRoute();return null;});await(()->route(form)!=null && route(form).totalTime()==64);
            assertEquals(2,calls.get());
        } finally {dispose(form);}
    }
    @Test void lateResponseFromAnotherScreensOldOptionIsDiscarded() throws Exception {checkLateResponse(false);}
    @Test void automaticMapRetriesAfterDiscardingAnotherScreensOldOption() throws Exception {checkLateResponse(true);}
    private void checkLateResponse(boolean automatic) throws Exception {
        var calls=new AtomicInteger();var option=new AtomicInteger();var release=new CountDownLatch(1);var started=new CountDownLatch(1);
        Form form=form(automatic,calls,option);var published=new ArrayList<TravelRoute>();
        try {
            fx(()->{
                form.map.setWalkingResultListener((route,message)->{if(route!=null)published.add(route);});
                form.map.setRouteRouter((stops,mode)->{
                    calls.incrementAndGet();int captured=option.get();started.countDown();
                    while(release.getCount()>0){try{release.await();}catch(InterruptedException ignored){}}
                    return result(60+captured);
                });
                form.map.presentCourse(PLAN,true);if(!automatic)form.map.requestWalkingRoute();return null;
            });assertTrue(started.await(5,TimeUnit.SECONDS));
            option.set(30);TmapSettings.saveSearchOption(root,30);release.countDown();
            if (!automatic) {
                await(()->field(form.map,"walkingTask")==null);
                fx(()->{assertNull(route(form));assertTrue(published.isEmpty());form.map.requestWalkingRoute();return null;});
            }
            await(()->route(form)!=null);
            fx(()->{assertEquals(90,route(form).totalTime());assertEquals(1,published.size());return null;});assertEquals(2,calls.get());
        } finally {release.countDown();dispose(form);}
    }
    @Test void walkingOptionDoesNotInvalidateDrivingRoute() throws Exception {
        var calls=new AtomicInteger();var option=new AtomicInteger();Form form=form(true,calls,option);
        var plan=PLAN.withTravelMode(TravelMode.DRIVING);
        try {
            fx(()->{form.map.presentCourse(plan,true);return null;});await(()->route(form)!=null);
            TmapSettings.saveSearchOption(root,30);
            fx(()->{form.map.presentCourse(plan,true);assertNotNull(route(form));return null;});assertEquals(1,calls.get());
        } finally {dispose(form);}
    }
    private static TravelRoute result(int seconds){return new TravelRoute(List.of(),100,seconds);}
    private static TravelRoute route(Form form)throws Exception{return (TravelRoute)field(form.map,"walkingRoute");}
    private static Object field(Object target,String name)throws Exception{var f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private static void set(Object target,String name,Object value)throws Exception{var f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    private static void dispose(Form... forms)throws Exception{fx(()->{for(Form form:forms)form.map.dispose();return null;});}
    private static <T>T fx(Callable<T> action)throws Exception{var task=new FutureTask<>(action);Platform.runLater(task);return task.get(10,TimeUnit.SECONDS);}
    private static void await(Callable<Boolean> condition)throws Exception{
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<deadline){if(fx(condition))return;Thread.sleep(20);}fail("지도 경로 상태 대기 시간 초과");
    }
}
