package com.datecourse.controller;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import com.datecourse.model.TravelRoute;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

class WalkingRouteUiTest {
    @Test void automaticTimeCalculationDoesNotDependOnMapSdkReadiness() throws Exception {
        var complete=new CountDownLatch(1);var route=new AtomicReference<TravelRoute>();
        PlannerMap map=fx(()->{
            PlannerMap m=new PlannerMap(new WebView(),(lat,lng)->{},e->fail(e));
            m.setAutomaticWalking(true);m.setWalkingRouter(stops->new TravelRoute(List.of(),300,122));
            m.setWalkingResultListener((r,message)->{if(r!=null){route.set(r);complete.countDown();}});
            m.present(plan("saved",35.18),null,500,true,false);return m;
        });
        try{assertTrue(complete.await(5,TimeUnit.SECONDS));assertEquals(122,route.get().totalTime());}
        finally{fx(()->{map.dispose();return null;});}
    }
    @Test void automaticConfirmedRoutesIgnoreCandidateBrowsingAndRejectLateResults() throws Exception {
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);var returned=new CountDownLatch(1);
        var calls=new java.util.concurrent.atomic.AtomicInteger();var view=new AtomicReference<WebView>();
        PlannerMap map=fx(()->{
            view.set(new WebView());PlannerMap m=new PlannerMap(view.get(),(lat,lng)->{},e->fail(e),()->URI.create("http://localhost/unused"));
            m.setAutomaticWalking(true);
            m.setWalkingRouter(stops->{calls.incrementAndGet();boolean old=stops.getLast().name().equals("old");
                if(old){started.countDown();while(release.getCount()>0){try{release.await(50,TimeUnit.MILLISECONDS);}catch(InterruptedException ignored){}}returned.countDown();}
                return new TravelRoute(List.of(),old?100:200,60);
            });
            view.get().getEngine().loadContent("<html><script>window.presentRoute=function(){};window.showWalkingRoute=function(r,m,f){window.walking=r;};</script></html>");return m;
        });
        try {
            await(view.get(),"typeof window.presentRoute==='function'");
            fx(()->{
                try{var field=PlannerMap.class.getDeclaredField("bridge");field.setAccessible(true);((MapBridge)field.get(map)).onMapReady();}
                catch(Exception e){throw new RuntimeException(e);}
                map.present(plan("old",35.18),null,500,true,false);return null;
            });
            assertTrue(started.await(5,TimeUnit.SECONDS));
            fx(()->{map.present(plan("new",35.181),null,500,true,false);return null;});
            await(view.get(),"!!window.walking && window.walking.totalDistance===200");
            fx(()->{var candidate=new Place(99,"c","미선택 후보","CAFE",35.182,129.0797,5000,0);
                map.presentCandidates(plan("new",35.181),List.of(candidate),candidate,500,true,false);return null;});
            assertEquals(2,calls.get());release.countDown();assertTrue(returned.await(5,TimeUnit.SECONDS));
            fx(()->{assertEquals(200,((Number)view.get().getEngine().executeScript("window.walking.totalDistance")).intValue());
                map.present(new RoutePlan(35.1796,129.0756,2,1000,"",List.of()),null,500,true,false);
                assertEquals(true,view.get().getEngine().executeScript("window.walking===null"));return null;});
        } finally {release.countDown();fx(()->{map.dispose();return null;});}
    }
    @Test void automaticWalkingWaitsForMapAndResetCancelsQueuedWorkWithoutZooming() throws Exception {
        for (boolean resetBeforeReady : new boolean[]{false,true}) {
            AtomicReference<WebView> view = new AtomicReference<>();
            var calls = new java.util.concurrent.atomic.AtomicInteger();
            PlannerMap map = fx(() -> {
                view.set(new WebView());
                var stage=new javafx.stage.Stage();stage.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane(view.get()),800,600));stage.show();
                view.get().getProperties().put("testStage",stage);
                PlannerMap result = new PlannerMap(view.get(),(lat,lng)->{},error -> fail(error),
                        () -> URI.create("http://localhost/not-used"));
                result.setWalkingRouter(stops -> {
                    calls.incrementAndGet();
                    return new TravelRoute(List.of(),300,100);
                });
                view.get().getEngine().loadContent("""
                    <html><script>
                    window.presentRoute=function(){};
                    window.showWalkingRoute=function(route,message,fit){window.walking=route;window.walkingFit=fit;};
                    </script></html>
                    """);
                return result;
            });
            try {
                await(view.get(),"typeof window.presentRoute==='function'");
                fx(() -> {
                    map.present(plan("auto",35.18),null,500,false,false);
                    map.requestWalkingRouteWhenReady();
                    assertEquals(0,calls.get());
                    if(resetBeforeReady) map.present(new RoutePlan(35.1796,129.0756,2,1000,"",List.of()),null,500,false,false);
                    try {
                        var bridge = PlannerMap.class.getDeclaredField("bridge");bridge.setAccessible(true);
                        ((MapBridge)bridge.get(map)).onMapReady();
                    } catch (ReflectiveOperationException failure) { throw new RuntimeException(failure); }
                    return null;
                });
                if(resetBeforeReady) assertEquals(0,calls.get());
                else {
                    await(view.get(),"!!window.walking && window.walking.totalDistance===300");
                    assertEquals(1,calls.get());
                    assertEquals(false,fx(() -> view.get().getEngine().executeScript("window.walkingFit")));
                }
            } finally { fx(() -> { view.get().getEngine().load(null);return null; }); }
        }
    }

    @BeforeAll static void startFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try { Platform.startup(() -> { Platform.setImplicitExit(false); started.countDown(); }); }
        catch (IllegalStateException alreadyStarted) { Platform.runLater(started::countDown); }
        assertTrue(started.await(10, TimeUnit.SECONDS));
    }

    @Test void oldWalkingResponsesCannotReplaceANewerCourseAndCourseChangesClearTheOverlay() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/map.html", exchange -> {
            byte[] html = """
                    <!doctype html><html><body><script>
                    window.presentRoute=function(){};
                    window.showWalkingRoute=function(route,message){window.walking=route;window.walkingMessage=message;};
                    window.connectJavaMapBridge=function(){javaMapBridge.onMapReady();window.connected=true;};
                    </script></body></html>
                    """.getBytes(StandardCharsets.UTF_8);
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, html.length); exchange.getResponseBody().write(html);
            }
        });
        server.start();
        CountDownLatch oldStarted = new CountDownLatch(1), releaseOld = new CountDownLatch(1), oldReturned = new CountDownLatch(1);
        AtomicReference<WebView> view = new AtomicReference<>();
        try {
            PlannerMap map = fx(() -> {
                view.set(new WebView());
                var stage=new javafx.stage.Stage();stage.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane(view.get()),800,600));stage.show();
                view.get().getProperties().put("testStage",stage);
                PlannerMap result = new PlannerMap(view.get(), (lat,lng)->{}, error -> fail(error),
                        () -> URI.create("http://localhost:" + server.getAddress().getPort() + "/map.html"));
                result.setWalkingRouter(stops -> {
                    boolean old = stops.get(1).name().equals("old");
                    if (old) {
                        oldStarted.countDown();
                        boolean released = false;
                        while (!released) {
                            try { released = releaseOld.await(10, TimeUnit.SECONDS); }
                            catch (InterruptedException cancellation) { /* Simulate an API callback arriving despite cancellation. */ }
                        }
                        oldReturned.countDown();
                    }
                    return new TravelRoute(List.of(), old ? 100 : 200, 60);
                });
                result.load();
                return result;
            });
            await(view.get(), "window.connected===true");
            fx(() -> { map.present(plan("old",35.18), null,500,false,false); map.requestWalkingRoute(); return null; });
            assertTrue(oldStarted.await(5,TimeUnit.SECONDS));
            fx(() -> { map.present(plan("new",35.181), null,500,false,false); map.requestWalkingRoute(); return null; });
            await(view.get(), "!!window.walking && window.walking.totalDistance===200");
            releaseOld.countDown();
            assertTrue(oldReturned.await(5,TimeUnit.SECONDS));
            assertEquals(200, ((Number)fx(() -> view.get().getEngine().executeScript("window.walking.totalDistance"))).intValue());
            fx(() -> { map.present(plan("changed",35.182),null,500,false,false); return null; });
            assertEquals(true, fx(() -> view.get().getEngine().executeScript("window.walking===null && window.walkingMessage.includes('코스')")));
        } finally {
            releaseOld.countDown();
            if (view.get()!=null) fx(() -> {view.get().getEngine().load(null);((javafx.stage.Stage)view.get().getProperties().get("testStage")).close();return null;});
            server.stop(0);
        }
    }

    private RoutePlan plan(String name, double lat) {
        return new RoutePlan(35.1796,129.0756,2,1000,"",List.of(new Place(1,name,name,"CAFE",lat,129.0797,5000,4)));
    }

    private void await(WebView view, String condition) throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
        while(System.nanoTime()<deadline) {
            if(Boolean.TRUE.equals(fx(() -> view.getEngine().executeScript(condition)))) return;
            Thread.sleep(50);
        }
        String diagnostic=fx(() -> view.getEngine().getLocation()+" / "+view.getEngine().getLoadWorker().getState()
                +" / "+view.getEngine().executeScript("document.body ? document.body.textContent.slice(0,500) : 'no body'"));
        fail("Timed out waiting for " + condition+" / "+diagnostic);
    }

    private static <T> T fx(Supplier<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action::get);
        Platform.runLater(task);
        return task.get(10,TimeUnit.SECONDS);
    }
}
