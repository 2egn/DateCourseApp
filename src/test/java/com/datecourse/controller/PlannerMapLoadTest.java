package com.datecourse.controller;

import com.datecourse.model.RoutePlan;
import com.sun.net.httpserver.HttpServer;
import javafx.application.Platform;
import javafx.scene.web.WebView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;

class PlannerMapLoadTest {
    @BeforeAll static void startFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try { Platform.startup(() -> { Platform.setImplicitExit(false); started.countDown(); }); }
        catch (IllegalStateException alreadyStarted) { Platform.runLater(started::countDown); }
        assertTrue(started.await(10, TimeUnit.SECONDS));
    }

    @Test void retriesServerAndSdkFailuresAndRestoresTheCurrentRoute() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/map.html", exchange -> {
            byte[] html = """
                    <!doctype html><html><body><script>
                    window.presentRoute=function(plan){window.presented=plan;};
                    window.connectJavaMapBridge=function(){window.javaMapBridge.onMapReady();};
                    </script></body></html>
                    """.getBytes(StandardCharsets.UTF_8);
            try (exchange) {
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, html.length);
                exchange.getResponseBody().write(html);
            }
        });
        server.start();
        try {
            AtomicInteger attempts = new AtomicInteger();
            AtomicReference<String> error = new AtomicReference<>();
            AtomicReference<WebView> view = new AtomicReference<>();
            PlannerMap map = fx(() -> {
                view.set(new WebView());
                var stage=new javafx.stage.Stage();stage.setScene(new javafx.scene.Scene(new javafx.scene.layout.StackPane(view.get()),800,600));stage.show();
                view.get().getProperties().put("testStage",stage);
                PlannerMap result = new PlannerMap(view.get(), (lat, lng) -> {}, error::set, () -> {
                    if (attempts.incrementAndGet() == 1) throw new IOException("server unavailable");
                    return URI.create("http://localhost:" + server.getAddress().getPort() + "/map.html");
                });
                result.present(new RoutePlan(35.1796, 129.0756, 2, 500, "test", List.of()), null, 500, false, true);
                result.load();
                return result;
            });
            assertTrue(error.get().contains("지도 새로고침"));
            assertFalse(error.get().contains("8080"));
            fx(() -> { map.reload(); return null; });
            awaitPresented(view.get());
            assertEquals(2, attempts.get());
            fx(() -> {
                view.get().getEngine().executeScript("javaMapBridge.onMapError('SDK failed')");
                return null;
            });
            assertTrue(error.get().contains("지도 새로고침"));
            assertFalse(error.get().contains("SDK"));
            fx(() -> { map.load(); return null; });
            awaitPresented(view.get());
            assertEquals(3, attempts.get());
            fx(() -> { map.dispose();((javafx.stage.Stage)view.get().getProperties().get("testStage")).close(); return null; });
        } finally { server.stop(0); }
    }

    @Test void mapPageWaitsForHiddenAncestorToBecomeVisible() throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        AtomicInteger requests=new AtomicInteger();
        server.createContext("/map.html",exchange->{
            requests.incrementAndGet();byte[] html="<html><script>window.presentRoute=function(p){window.presented=p;};window.connectJavaMapBridge=function(){javaMapBridge.onMapReady();};</script></html>".getBytes(StandardCharsets.UTF_8);
            try(exchange){exchange.getResponseHeaders().set("Content-Type","text/html");exchange.sendResponseHeaders(200,html.length);exchange.getResponseBody().write(html);}
        });server.start();
        record Context(javafx.stage.Stage stage,javafx.scene.layout.StackPane pane,WebView view,PlannerMap map){}
        Context context=fx(()->{
            WebView view=new WebView();var pane=new javafx.scene.layout.StackPane(view);pane.setVisible(false);
            var stage=new javafx.stage.Stage();stage.setScene(new javafx.scene.Scene(pane,800,600));stage.show();
            var map=new PlannerMap(view,(lat,lng)->{},message->{},()->URI.create("http://localhost:"+server.getAddress().getPort()+"/map.html"));
            map.present(new RoutePlan(35.1796,129.0756,2,1000,"",List.of()),null,500,false,true);map.load();
            return new Context(stage,pane,view,map);
        });
        try{
            fx(()->null);assertEquals(0,requests.get());
            assertEquals(javafx.concurrent.Worker.State.READY,fx(()->context.view().getEngine().getLoadWorker().getState()));
            fx(()->{context.pane().setVisible(true);return null;});awaitPresented(context.view());assertEquals(1,requests.get());
        }finally{fx(()->{context.map().dispose();context.stage().close();return null;});server.stop(0);}
    }

    private void awaitPresented(WebView view) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (fx(() -> Boolean.TRUE.equals(view.getEngine().executeScript("!!window.presented")))) {
                assertEquals(35.1796, ((Number) fx(() -> view.getEngine().executeScript("window.presented.startLat"))).doubleValue(), 0.000001);
                return;
            }
            Thread.sleep(50);
        }
        fail("The map did not reconnect and restore its route");
    }

    private static <T> T fx(Supplier<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work::get);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
