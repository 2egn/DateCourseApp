package com.datecourse.page._03step_saved;

import com.datecourse.page.common.map.CourseMapView;

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
    private record Context(Stage stage,WebView view,CourseMapView map) {}
    private static <T>T fx(Callable<T> action)throws Exception {
        var task=new FutureTask<>(action);Platform.runLater(task);return task.get(10,TimeUnit.SECONDS);
    }
    @Test void coldFirstCaptureAndImmediateSecondCaptureBothProduceImages() throws Exception {
        var ready=new CountDownLatch(1);
        try {Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException started){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(10,TimeUnit.SECONDS));
        var plan=new RoutePlan(35.1578,129.0601,2,1000,"첫 캡처",List.of(
                new Place(1,"a","첫 장소","MEAL",35.1555,129.058,9000,4)));
        var context=fx(()->{
            var view=new WebView();var map=new CourseMapView(view,(lat,lng)->{},message->{});
            var stage=new Stage();stage.setScene(new Scene(new StackPane(view),1000,520));stage.show();
            map.present(plan,null,500,false,false);map.load();return new Context(stage,view,map);
        });
        try {
            var first=fx(()->context.map.capturePreview(plan)).get(25,TimeUnit.SECONDS);
            assertEquals(1000,first.getWidth());
            var second=fx(()->context.map.capturePreview(plan)).get(25,TimeUnit.SECONDS);
            assertEquals(520,second.getHeight());
        } finally {fx(()->{context.map.dispose();context.stage.close();return null;});}
    }

    @Test void previewDialogGeneratesActualMapAndClosesItsResources() throws Exception {
        var ready=new CountDownLatch(1);
        try{Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException started){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(10,TimeUnit.SECONDS));
        var owner=fx(()->{var stage=new Stage();stage.setScene(new Scene(new StackPane(),900,620));stage.show();return stage;});
        try {
            var plan=new RoutePlan(35.1578,129.0601,2,1000,"미리보기 재생성",List.of(new Place(1,"a","첫 장소","MEAL",35.1555,129.058,9000,4)));
            byte[] png=fx(()->CoursePreviewDialog.generate(owner,plan)).get(25,TimeUnit.SECONDS);
            var image=javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));assertEquals(1000,image.getWidth());assertEquals(650,image.getHeight());
            long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
            while(System.nanoTime()<end && fx(()->javafx.stage.Window.getWindows().size()>1))Thread.sleep(20);
            assertEquals(1,fx(()->javafx.stage.Window.getWindows().size()));
        } finally {fx(()->{owner.close();return null;});}
    }

    @Test void cancellingPreviewDialogKeepsOwnerOpenAndDoesNotLeaveACaptureRunning() throws Exception {
        var ready=new CountDownLatch(1);
        try{Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException started){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(10,TimeUnit.SECONDS));
        var owner=fx(()->{var stage=new Stage();stage.setScene(new Scene(new StackPane(),900,620));stage.show();return stage;});
        try {
            var pending=fx(()->{
                var future=CoursePreviewDialog.generate(owner,new RoutePlan(35.1578,129.06,2,1000,"취소",List.of()));
                var dialog=javafx.stage.Window.getWindows().stream().filter(w->w!=owner).map(w->(Stage)w).findFirst().orElseThrow();
                dialog.close();return future;
            });
            assertTrue(pending.isCancelled());assertTrue(fx(owner::isShowing));
            assertEquals(1,fx(()->javafx.stage.Window.getWindows().size()));
        } finally {fx(()->{owner.close();return null;});}
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
            var view=new WebView();var map=new CourseMapView(view,(lat,lng)->{},errors::add);
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
            verifyBufferedEdgeAndAttribution(context,plan);
        } finally {fx(()->{context.map.dispose();context.stage.close();return null;});}
    }
    /** 실제 SDK에서 크롭 복원 뒤 경계 마커·호버·짧은 카메라 이동과 출처 위치를 확인한다. */
    private void verifyBufferedEdgeAndAttribution(Context context,RoutePlan plan) throws Exception {
        String payload=new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(plan);
        String expectedScale=System.getProperty("datecourse.preview.expectedScale");
        fx(()->{
            if(expectedScale!=null)assertEquals(Double.parseDouble(expectedScale),context.stage.getOutputScaleX(),0.01,"검사 배율이 실제 창에 적용되어야 합니다.");
            context.view.getEngine().executeScript("window.livePlan="+payload+";");
            context.view.getEngine().executeScript("""
                window.livePins=[];window.liveImageSources=new WeakMap();
                window.NativeMarkerImage=kakao.maps.MarkerImage;window.NativeMarker=kakao.maps.Marker;
                kakao.maps.MarkerImage=function(src,size,options){var image=new NativeMarkerImage(src,size,options);liveImageSources.set(image,src);return image;};
                kakao.maps.Marker=function(options){
                    var pin=new NativeMarker(options),record={pin:pin,source:liveImageSources.get(options.image)},nativeSet=pin.setImage;
                    pin.setImage=function(image){record.source=liveImageSources.get(image);return nativeSet.call(pin,image);};livePins.push(record);return pin;
                };
                window.liveViewport=document.getElementById('map');
                var state=mapViewport.inspect();
                if(state.renderWidth<=state.width || state.renderHeight<=state.height)throw Error('Buffer was not restored');
                window.liveEdge=mapViewport.projection().coordsFromContainerPoint(new kakao.maps.Point(liveViewport.clientWidth-16,liveViewport.clientHeight/2));
                livePlan.places=[Object.assign({},livePlan.places[0],{recordId:99001,placeId:'live-edge',name:'경계 확인용 마커',lat:liveEdge.getLat(),lng:liveEdge.getLng()})];
                window.setCoursePreviewMode(true);window.presentRoute(livePlan,null,500,true,false);
                window.liveMove=function(x,y){var rect=liveViewport.getBoundingClientRect();liveViewport.dispatchEvent(new MouseEvent('mousemove',{bubbles:true,clientX:rect.left+x,clientY:rect.top+y}));};
                window.liveSvg=function(){return decodeURIComponent(livePins[0].source);};
                """);
            assertEquals(true,context.view.getEngine().executeScript("liveSvg().includes('opacity=\"0.55\"')"));
            context.view.getEngine().executeScript("liveMove(liveViewport.clientWidth-16,liveViewport.clientHeight/2-24)");
            return null;
        });
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<end && !fx(()->Boolean.TRUE.equals(context.view.getEngine().executeScript("liveSvg().includes('opacity=\"1\"')"))))Thread.sleep(30);
        fx(()->{
            assertEquals(true,context.view.getEngine().executeScript("liveSvg().includes('opacity=\"1\"')"));
            assertEquals(true,context.view.getEngine().executeScript("document.getElementById('map-surface').classList.contains('map-interactive')"));
            context.view.getEngine().executeScript("window.liveInitialCamera=inspectMapCamera();liveMove(10,10);var shifted=mapViewport.projection().coordsFromContainerPoint(new kakao.maps.Point(liveViewport.clientWidth/2+30,liveViewport.clientHeight/2));window.focusResultPlace(shifted.getLat(),shifted.getLng());");
            return null;
        });
        end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<end && fx(()->Boolean.TRUE.equals(context.view.getEngine().executeScript("inspectMapCamera().moving||inspectMapCamera().pending"))))Thread.sleep(30);
        fx(()->{
            assertEquals(true,context.view.getEngine().executeScript("Math.abs(inspectMapCamera().lng-liveInitialCamera.lng)>0.0000001 || Math.abs(inspectMapCamera().lat-liveInitialCamera.lat)>0.0000001"));
            assertEquals(true,context.view.getEngine().executeScript("""
                (function(){var bounds=liveViewport.getBoundingClientRect(),footers=Array.from(document.getElementById('map-canvas').children).filter(function(node){return node.style.position==='absolute' && node.style.bottom && node.style.bottom!=='auto' && (!node.style.top || node.style.top==='auto');});
                var marks=[];footers.forEach(function(node){marks=marks.concat(Array.from(node.querySelectorAll('a,img')));});
                return marks.length>0 && marks.every(function(node){var r=node.getBoundingClientRect();return !r.width || r.left>=bounds.left-1 && r.right<=bounds.right+1 && r.bottom<=bounds.bottom+1;});})()
                """));
            var point=(netscape.javascript.JSObject)context.view.getEngine().executeScript("mapViewport.projection().containerPointFromCoords(liveEdge)");
            assertTrue(((Number)point.getMember("x")).doubleValue()>context.view.getWidth()-100);
            assertTrue(((Number)point.getMember("x")).doubleValue()<context.view.getWidth());
            String suffix=expectedScale==null?"":"-scale-"+expectedScale.replace('.', '_');
            var snapshot=context.view.snapshot(null,null);var target=Path.of("build/ui-preview/map-buffer-edge-live"+suffix+".png");
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(snapshot,null),"png",target.toFile());
            var scaleReport=new LinkedHashMap<String,Object>();
            scaleReport.put("javaVersion",System.getProperty("java.version"));
            scaleReport.put("javafxVersion",System.getProperty("javafx.runtime.version"));
            scaleReport.put("expectedScale",expectedScale==null?"native":expectedScale);
            scaleReport.put("outputScaleX",context.stage.getOutputScaleX());scaleReport.put("outputScaleY",context.stage.getOutputScaleY());
            scaleReport.put("renderScaleX",context.stage.getRenderScaleX());scaleReport.put("renderScaleY",context.stage.getRenderScaleY());
            scaleReport.put("webViewWidth",context.view.getWidth());scaleReport.put("webViewHeight",context.view.getHeight());
            scaleReport.put("webDevicePixelRatio",context.view.getEngine().executeScript("window.devicePixelRatio"));
            new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter()
                    .writeValue(Path.of("build/ui-preview/map-buffer-edge-live"+suffix+".json").toFile(),scaleReport);
            return null;
        });
    }

}
