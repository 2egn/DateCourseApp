package com.datecourse.page._01step_planner;

import com.datecourse.page.account.MyPageController;
import com.datecourse.page.common.BasePageController;
import com.datecourse.page.common.NumberStepper;
import com.datecourse.page.common.PageFeedback;
import com.datecourse.page.common.map.CourseMapView;
import com.datecourse.page.common.map.MapEventBridge;
import com.datecourse.page._02step_editor.CourseEditorController;
import com.datecourse.page.review.InformationReviewController;
import com.datecourse.page._03step_saved.SavedCourseController;

import javafx.application.Platform;
import javafx.concurrent.Worker;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.TextField;
import javafx.embed.swing.SwingFXUtils;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URL;
import java.util.ResourceBundle;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class CoursePlannerUiTest {
    private final MapEventBridge cameraPolicyBridge = new MapEventBridge(()->{},(lat,lng)->{},message->{});
    @Test void freeFiltersBelongOnlyToCultureAndRestoreWithoutLeakingAcrossActivities() throws Exception {
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();
            CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            var details=new com.datecourse.model.PlaceDetails("","","","","","","","",java.util.Map.of(),
                    java.util.List.of(new com.datecourse.model.PlaceDetails.TagFact(com.datecourse.model.PlaceTag.FREE_ADMISSION,"확인","","")));
            var places=new java.util.ArrayList<>(new com.datecourse.service.PublicDemoPlaceRepository().findAll());
            // 비문화 자료에 무료 태그가 섞여도 화면의 활동 정책을 따라야 한다.
            places.add(new com.datecourse.model.Place(99999,"free-meal","무료 태그 식당","MEAL",35.1578,129.06,com.datecourse.model.PriceEstimate.exact(0),0,details));
            editor.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,2000,"",java.util.List.of()),places,p->{},()->{});
            try {
                var activity=(javafx.scene.control.ComboBox<String>)field(editor,"activityBox");
                var price=(javafx.scene.control.ComboBox<com.datecourse.model.PlacePriceFilter>)field(editor,"priceFilterBox");
                var features=(com.datecourse.page.common.controls.PlaceFeatureSelector)field(editor,"featureSelector");
                var pane=(com.datecourse.page.common.controls.ExpandableFilterPane)field(editor,"filterPane");
                assertFalse(price.getItems().contains(com.datecourse.model.PlacePriceFilter.FREE));
                assertFalse(features.availableTags().contains(com.datecourse.model.PlaceTag.FREE_ADMISSION));
                price.setValue(com.datecourse.model.PlacePriceFilter.ESTIMATED);
                activity.setValue("문화");
                assertTrue(price.getItems().contains(com.datecourse.model.PlacePriceFilter.FREE));
                price.setValue(com.datecourse.model.PlacePriceFilter.FREE);
                features.selectedTags().add(com.datecourse.model.PlaceTag.FREE_ADMISSION);
                activity.setValue("카페");
                assertEquals(com.datecourse.model.PlacePriceFilter.ANY,price.getValue());
                assertFalse(price.getItems().contains(com.datecourse.model.PlacePriceFilter.FREE));
                assertTrue(features.selectedTags().isEmpty());
                assertTrue(pane.selections().isEmpty());
                assertFalse(price.getTooltip().getText().contains("무료"));
                assertFalse(((CheckBox)field(editor,"includeUncertainPrice")).isDisabled());
                activity.setValue("식사");
                assertEquals(com.datecourse.model.PlacePriceFilter.ESTIMATED,price.getValue());
                assertFalse(features.availableTags().contains(com.datecourse.model.PlaceTag.FREE_ADMISSION));
                activity.setValue("문화");
                assertEquals(com.datecourse.model.PlacePriceFilter.FREE,price.getValue());
                assertTrue(features.selectedTags().contains(com.datecourse.model.PlaceTag.FREE_ADMISSION));
                assertEquals(2,pane.selections().size());
                assertTrue(((CheckBox)field(editor,"includeUncertainPrice")).isDisabled());
            } finally {editor.dispose();}
            return null;
        });
    }

    @Test void filterEntryShowsRemovableSelectionsWithoutOpeningTheEditor() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));Parent root=loader.load();
            CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            editor.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,2000,"",java.util.List.of()),
                    new com.datecourse.service.PublicDemoPlaceRepository().findAll(),p->{},()->{});
            var pane=(com.datecourse.page.common.controls.ExpandableFilterPane)field(editor,"filterPane");
            var subtype=(javafx.scene.control.ComboBox<String>)field(editor,"subtypeBox");
            var detail=(javafx.scene.control.ComboBox<String>)field(editor,"detailTypeBox");
            var price=(javafx.scene.control.ComboBox<com.datecourse.model.PlacePriceFilter>)field(editor,"priceFilterBox");
            var features=(com.datecourse.page.common.controls.PlaceFeatureSelector)field(editor,"featureSelector");
            subtype.setValue("한식");detail.setValue("냉면/밀면");price.setValue(com.datecourse.model.PlacePriceFilter.ESTIMATED);
            features.selectedTags().addAll(java.util.Set.of(com.datecourse.model.PlaceTag.GOOD_PRICE,com.datecourse.model.PlaceTag.NEAR_SUBWAY));
            assertFalse(pane.isExpanded());assertEquals(5,pane.selections().size());
            new Scene(root,1440,960);root.resize(1440,960);root.applyCss();root.layout();
            var entry=(javafx.scene.control.Button)pane.lookup(".filter-entry");
            assertTrue(entry.getText().contains("5개"));assertNotNull(entry.getGraphic());
            pane.lookupAll(".filter-chip").stream().map(n->(javafx.scene.control.Button)n)
                .filter(b->b.getText().startsWith("업종:")).findFirst().orElseThrow().fire();
            assertNull(detail.getValue());assertEquals("한식",subtype.getValue());assertEquals(4,pane.selections().size());
            pane.lookupAll(".filter-chip").stream().map(n->(javafx.scene.control.Button)n)
                .filter(b->b.getText().contains("더 보기")).findFirst().orElseThrow().fire();
            assertTrue(pane.isExpanded());assertEquals("selected",pane.currentSection());
            pane.fireEvent(new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,"","",javafx.scene.input.KeyCode.ESCAPE,false,false,false,false));
            assertFalse(pane.isExpanded());assertEquals(4,pane.selections().size());
            Path directory=Path.of("build/ui-preview");Files.createDirectories(directory);
            root.applyCss();root.layout();
            ImageIO.write(SwingFXUtils.fromFXImage(root.lookup(".route-edit-form").snapshot(null,null),null),"png",directory.resolve("filter-entry-and-chips.png").toFile());
            pane.setExpanded(true);pane.showSection("features");root.applyCss();root.layout();
            ImageIO.write(SwingFXUtils.fromFXImage(root.lookup(".route-edit-form").snapshot(null,null),null),"png",directory.resolve("filter-features-editor.png").toFile());
            root.resize(1100,760);root.applyCss();root.layout();
            var scroller=(javafx.scene.control.ScrollPane)pane.lookup(".filter-scroll");
            assertTrue(scroller.getHeight()<=280);
            var entryBounds=entry.localToScene(entry.getBoundsInLocal());
            assertTrue(entryBounds.getMinX()>=0 && entryBounds.getMaxX()<=1100);
            ImageIO.write(SwingFXUtils.fromFXImage(root.lookup(".route-edit-form").snapshot(null,null),null),"png",directory.resolve("filter-compact-editor.png").toFile());
            editor.dispose();return null;
        });Platform.runLater(task);task.get(15,TimeUnit.SECONDS);
    }

    @Test void expandedFiltersRememberActivitySelectionsAndClearOnlyAdvancedConditions() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));Parent root=loader.load();
            CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            var places=new com.datecourse.service.PublicDemoPlaceRepository().findAll();
            editor.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,2000,"",java.util.List.of()),places,p->{},()->{});
            var subtype=(javafx.scene.control.ComboBox<String>)field(editor,"subtypeBox");
            var detail=(javafx.scene.control.ComboBox<String>)field(editor,"detailTypeBox");
            var price=(javafx.scene.control.ComboBox<com.datecourse.model.PlacePriceFilter>)field(editor,"priceFilterBox");
            var features=(com.datecourse.page.common.controls.PlaceFeatureSelector)field(editor,"featureSelector");
            assertTrue(detail.getItems().contains("냉면/밀면"));assertTrue(detail.getItems().contains("국/탕/찌개류"));
            subtype.setValue("한식");detail.setValue("냉면/밀면");
            features.selectedTags().addAll(java.util.Set.of(com.datecourse.model.PlaceTag.GOOD_PRICE,com.datecourse.model.PlaceTag.NEAR_SUBWAY));
            price.setValue(com.datecourse.model.PlacePriceFilter.ESTIMATED);
            assertTrue(((CheckBox)field(editor,"includeUncertainPrice")).isDisabled());
            var activity=(javafx.scene.control.ComboBox<String>)field(editor,"activityBox");
            activity.setValue("카페");assertNull(detail.getValue());assertTrue(features.selectedTags().isEmpty());
            activity.setValue("식사");assertEquals("한식",subtype.getValue());assertEquals("냉면/밀면",detail.getValue());
            assertEquals(2,features.selectedTags().size());assertEquals(com.datecourse.model.PlacePriceFilter.ESTIMATED,price.getValue());
            subtype.setValue("중식");assertNull(detail.getValue());
            assertFalse(detail.getItems().contains("냉면/밀면"));
            ((TextField)field(editor,"placePriceField")).setText("1.2");
            invoke(editor,"onClearAdvancedFilters");
            assertNull(subtype.getValue());assertNull(detail.getValue());assertTrue(features.selectedTags().isEmpty());
            assertEquals(com.datecourse.model.PlacePriceFilter.ANY,price.getValue());
            assertFalse(((CheckBox)field(editor,"includeUncertainPrice")).isDisabled());
            assertEquals("1.2",((TextField)field(editor,"placePriceField")).getText());
            ((com.datecourse.page.common.controls.ExpandableFilterPane)field(editor,"filterPane")).setExpanded(true);
            new Scene(root,1440,960);root.resize(1440,960);root.applyCss();root.layout();
            var form=root.lookup(".route-edit-form");
            Path directory=Path.of("build/ui-preview");Files.createDirectories(directory);
            ImageIO.write(SwingFXUtils.fromFXImage(form.snapshot(null,null),null),"png",directory.resolve("expanded-place-filters.png").toFile());
            ((CourseMapView)field(editor,"map")).dispose();return null;
        });Platform.runLater(task);task.get(15,TimeUnit.SECONDS);
    }

    @Test void emptySearchOffersExplicitRecoveryAndPriceConversionStaysVisible() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();
            CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            var place=new com.datecourse.model.Place(1,"unknown","미확인 식당","MEAL",35.1579,129.06,
                    com.datecourse.model.PriceEstimate.unknown(),0,com.datecourse.model.PlaceDetails.EMPTY);
            editor.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,1000,"",java.util.List.of()),java.util.List.of(place),p->{},()->{});
            ((TextField)field(editor,"placePriceField")).setText("0.82");
            assertEquals("8,200원 / 1인",((javafx.scene.control.Label)field(editor,"priceWonLabel")).getText());
            invoke(editor,"onRecommendNext");
            assertTrue(((javafx.scene.layout.VBox)field(editor,"emptyResults")).isVisible());
            assertFalse(((CheckBox)field(editor,"includeUncertainPrice")).isSelected());
            assertTrue(((javafx.scene.control.Button)field(editor,"includeUnknownRecovery")).isVisible());
            assertTrue(((javafx.scene.control.Label)field(editor,"emptyResultMessage")).getText().contains("가격 미확인"));
            invoke(editor,"onIncludeUnknownRecovery");
            assertTrue(((CheckBox)field(editor,"includeUncertainPrice")).isSelected());
            assertEquals("CHOICE",field(editor,"state").toString());
            assertFalse(((javafx.scene.layout.VBox)field(editor,"emptyResults")).isVisible());
            assertEquals(1,((javafx.scene.control.ListView<?>)field(editor,"candidateList")).getItems().size());
            return null;
        });Platform.runLater(task);task.get(15,TimeUnit.SECONDS);
    }

    @Test void completedCoursePrioritizesReviewAndKeepsMapOnTheLeftInCompactWindow() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));Parent root=loader.load();
            CoursePlannerController main=loader.getController();stubPlannerMap(main,"mapView");
            var a=new com.datecourse.model.Place(1,"a","첫 식당","MEAL",35.1579,129.06,8000,4);
            var b=new com.datecourse.model.Place(2,"b","다음 카페","CAFE",35.158,129.06,4000,4);
            callWith(main,"presentPlan",com.datecourse.model.RoutePlan.class,new com.datecourse.model.RoutePlan(35.1578,129.06,2,1000,"검토할 코스",java.util.List.of(a,b)));
            assertTrue(((javafx.scene.layout.VBox)field(main,"courseOverview")).isVisible());
            assertFalse(((javafx.scene.control.TitledPane)field(main,"commonConditionsPane")).isExpanded());
            assertTrue(((javafx.scene.control.Button)field(main,"saveCourseButton")).isVisible());
            String summary=((javafx.scene.control.Label)field(main,"completedStopsLabel")).getText();
            assertTrue(summary.indexOf("첫 식당")<summary.indexOf("다음 카페"));
            ((TextField)field(main,"peopleField")).setText("3");
            assertTrue(((javafx.scene.control.Label)field(main,"resultLabel")).getText().contains("36,000"));
            assertEquals(2,((com.datecourse.model.RoutePlan)field(main,"appliedPlan")).people());
            ((TextField)field(main,"peopleField")).setText("2");
            assertFalse(((String)field(field(main,"map"),"uiStatus")).contains("출발 위치를 선택"));
            new Scene(root,1100,760);root.resize(1100,760);root.applyCss();root.layout();
            var sidebar=root.lookup("#mainSidebar").localToScene(root.lookup("#mainSidebar").getBoundsInLocal());
            var map=root.lookup("#mapView").localToScene(root.lookup("#mapView").getBoundsInLocal());
            assertTrue(map.getMaxX()<=sidebar.getMinX()+1);assertTrue(map.getWidth()>400);
            var save=root.lookup("#saveCourseButton").localToScene(root.lookup("#saveCourseButton").getBoundsInLocal());
            assertTrue(save.getMinX()>=sidebar.getMinX() && save.getMaxX()<=1100 && save.getMaxY()<760);
            Path directory=Path.of("build/ui-preview");Files.createDirectories(directory);
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",directory.resolve("ux-course-review-compact.png").toFile());
            return null;
        });Platform.runLater(task);task.get(15,TimeUnit.SECONDS);
    }

    @Test void cameraDoesNotFitHiddenViewportAndReplaysLatestGoalAfterLayout() throws Exception {
        WebEngine engine=loadMap(1100,720,false);
        script(engine,"""
            window.viewportWidth=0;window.viewportHeight=0;
            window.sizedElement=document.createElement('div');
            Object.defineProperty(sizedElement,'clientWidth',{get:function(){return viewportWidth;}});
            Object.defineProperty(sizedElement,'clientHeight',{get:function(){return viewportHeight;}});
            testMap.setCenter(new kakao.maps.LatLng(35.1578,129.06));testMap.setLevel(5,{});zoomCalls=[];
            window.sizeCamera=createMapCamera(testMap,sizedElement,function(f,i){return JSON.parse(cameraPolicyBridge.decideCameraTransition(i,f.level,f.fitLevel,f.screenDistance,f.overflowRatio,f.reducedMotion));});
            window.circleGoal=function(lat){return {intent:'FIT',points:[new kakao.maps.LatLng(lat-.009,129.049),new kakao.maps.LatLng(lat+.009,129.071)]};};
            sizeCamera.request(circleGoal(35.1578));sizeCamera.request(circleGoal(35.1598));
            """);
        assertEquals(5,((Number)script(engine,"testMap.getLevel()")).intValue());
        assertEquals(0,((Number)script(engine,"zoomCalls.length")).intValue());
        assertEquals(true,script(engine,"sizeCamera.inspect().pending"));
        script(engine,"viewportWidth=1400;viewportHeight=800;sizeCamera.resize()");
        Thread.sleep(150);
        assertEquals(35.1598,((Number)script(engine,"testMap.getCenter().getLat()")).doubleValue(),1e-8);
        assertTrue(((Number)script(engine,"testMap.getLevel()")).intValue()<10);
        assertEquals(false,script(engine,"sizeCamera.inspect().pending"));
        script(engine,"sizeCamera.dispose()");
    }

    @Test void cameraKeepsOnlyLatestGoalAndManualInputCancelsWaitingMove() throws Exception {
        WebEngine engine=loadMap(1100,720,false);
        script(engine,"""
            testMap.setCenter(new kakao.maps.LatLng(35.1578,129.06));mapPans=[];
            testMap.panTo=function(p){this.setCenter(p);mapPans.push([p.getLat(),p.getLng()]);};
            focusResultPlace(35.1588,129.06);
            focusResultPlace(35.1598,129.06);focusResultPlace(35.1608,129.06);
            """);
        assertEquals(1,((Number)script(engine,"mapPans.length")).intValue());
        assertEquals(true,script(engine,"inspectMapCamera().moving && inspectMapCamera().pending"));
        script(engine,"testMap.onidle()");
        assertEquals(2,((Number)script(engine,"mapPans.length")).intValue());
        assertEquals(35.1608,((Number)script(engine,"testMap.getCenter().getLat()")).doubleValue(),1e-8);
        script(engine,"focusResultPlace(35.1618,129.06);document.getElementById('map').dispatchEvent(new WheelEvent('wheel',{deltaY:120}));testMap.onidle()");
        assertEquals(2,((Number)script(engine,"mapPans.length")).intValue());
        assertEquals(false,script(engine,"inspectMapCamera().pending"));
        script(engine,"focusResultPlace(35.1618,129.06);focusResultPlace(35.1628,129.06);testMap.ondragstart();testMap.onidle()");
        assertEquals(3,((Number)script(engine,"mapPans.length")).intValue());
        assertEquals(false,script(engine,"inspectMapCamera().moving || inspectMapCamera().pending"));
    }

    @Test void radiusDragUpdatesCircleWithoutZoomingInAndFitsOnRelease() throws Exception {
        WebEngine engine=loadMap(1100,720);
        script(engine,"""
            window.mapViewport.setBuffered(false);testMap.relayout();testMap.getProjection=function(){
                var unit=100000*Math.pow(2,5-testMap.getLevel()),element=document.getElementById('map');
                return {containerPointFromCoords:function(p){var c=testMap.getCenter();return {
                    x:(p.getLng()-c.getLng())*unit+element.clientWidth/2,
                    y:(c.getLat()-p.getLat())*unit+element.clientHeight/2};}};
            };
            testMap.setCenter(new kakao.maps.LatLng(35.1578,129.06));testMap.setLevel(8,{});
            window.plan={startLat:35.1578,startLng:129.06,radius:2000,places:[]};
            presentRoute(plan,null,2000,false,false,[]);
            setCameraRangeAdjusting(true);plan.radius=200;presentRoute(plan,null,200,false,true,[]);
            """);
        assertEquals(8,((Number)script(engine,"testMap.getLevel()")).intValue());
        assertEquals(true,script(engine,"pins.some(function(p){return p.attached && p.radius===200 && p.options.strokeColor==='#2563eb';})"));
        script(engine,"setCameraRangeAdjusting(false);fitRouteView(false)");
        assertTrue(((Number)script(engine,"testMap.getLevel()")).intValue()<8);
        assertEquals("FIT",script(engine,"inspectMapCamera().last.intent"));
    }

    @Test void addedLegFocusExcludesRangesAndOlderStopsButIncludesItsWalkingGeometry() throws Exception {
        WebEngine engine=loadMap(1100,720);
        script(engine,"""
            window.mapViewport.setBuffered(false);testMap.relayout();testMap.getProjection=function(){
                var unit=100000*Math.pow(2,5-testMap.getLevel()),element=document.getElementById('map');
                return {containerPointFromCoords:function(p){var c=testMap.getCenter();return {
                    x:(p.getLng()-c.getLng())*unit+element.clientWidth/2,
                    y:(c.getLat()-p.getLat())*unit+element.clientHeight/2};}};
            };
            window.plan={startLat:35,startLng:129,radius:2000,places:[
                {recordId:1,name:'이전 장소',lat:35.01,lng:129},
                {recordId:2,name:'추가 장소',lat:35.011,lng:129}]};
            presentRoute(plan,null,2000,true,true,[{recordId:3,name:'다른 후보',lat:35.03,lng:129}]);
            window.overviewLevel=testMap.getLevel();fitRouteLeg(1);
            """);
        assertEquals(true,script(engine,"Math.abs(testMap.getCenter().getLat()-35.0105)<0.0000001 && testMap.getLevel()<overviewLevel"));
        script(engine,"""
            showWalkingRoute({legs:[
                {from:{name:'출발지'},to:{name:'이전 장소'},distance:100,time:60,paths:[],steps:[]},
                {from:{name:'이전 장소'},to:{name:'추가 장소'},distance:200,time:120,
                 paths:[[{lat:35.01,lng:129},{lat:35.0105,lng:129.008},{lat:35.011,lng:129}]],steps:[]}
            ],totalDistance:300,totalTime:180},'',false);
            fitRouteLeg(1);
            """);
        assertEquals(true,script(engine,"Math.abs(testMap.getCenter().getLng()-129.004)<0.0000001"));
        script(engine,"fitRouteLeg(0)");
        assertEquals(true,script(engine,"Math.abs(testMap.getCenter().getLat()-35.005)<0.0000001"));
        script(engine,"window.beforeInvalid=JSON.stringify(inspectMapCamera());fitRouteLeg(-1);fitRouteLeg(20)");
        assertEquals(true,script(engine,"beforeInvalid===JSON.stringify(inspectMapCamera())"));
    }

    @Test void resetDiscardsWaitingGoalAndReducedMotionDoesNotAnimate() throws Exception {
        WebEngine engine=loadMap(1100,720,false);
        script(engine,"""
            testMap.setCenter(new kakao.maps.LatLng(35.1578,129.06));mapPans=[];
            testMap.panTo=function(p){this.setCenter(p);mapPans.push([p.getLat(),p.getLng()]);};
            focusResultPlace(35.1588,129.06);focusResultPlace(35.1598,129.06);resetPlannerMap();testMap.onidle();
            """);
        assertEquals(35.1796,((Number)script(engine,"testMap.getCenter().getLat()")).doubleValue(),1e-8);
        assertEquals(false,script(engine,"inspectMapCamera().pending"));
        script(engine,"window.matchMedia=function(){return {matches:true};};focusResultPlace(35.1806,129.0756)");
        assertEquals(1,((Number)script(engine,"mapPans.length")).intValue());
        assertEquals("IMMEDIATE",script(engine,"inspectMapCamera().last.movement"));
        assertEquals(35.1806,((Number)script(engine,"testMap.getCenter().getLat()")).doubleValue(),1e-8);
    }

    @Test void gestureStateResetsOnFreshInputWithoutTimeWindow() throws Exception {
        WebEngine engine=loadMap(850,720);
        assertEquals(true,script(engine,"""
            (function(){
                var gesture=new PinPointerGesture({movementTolerance:4}),pin={};
                for(var i=0;i<8;i++) {
                    gesture.begin({x:10,y:10},pin);
                    if(gesture.consumesClick({x:11,y:11}) || !gesture.blocksSdkClick(pin))return false;
                    if(gesture.finish({x:11,y:11},function(){return true;})!==pin)return false;
                    if(!gesture.blocksSdkClick(pin) || !gesture.consumesClick({x:11,y:11}))return false;
                }
                gesture.begin({x:10,y:10},pin);gesture.move({x:20,y:10});gesture.move({x:10,y:10});
                if(!gesture.blocksSdkClick(pin))return false;
                if(gesture.finish({x:10,y:10},function(){return true;})!==null)return false;
                if(!gesture.blocksSdkClick(pin))return false;
                gesture.reset();return !gesture.blocksSdkClick(pin) && !gesture.consumesClick({x:10,y:10});
            }())
            """));
    }

    @Test void pressReleaseOpensDetailsWhenHoverChangesTargetAndClickIsMissing() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.releasePlace={recordId:1,placeId:'release',name:'손 뗄 때 상세 열기',lat:35.1578,lng:129.06};
            window.presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:[releasePlace]},null,500,true,false,[]);
            testMap.setCenter(new kakao.maps.LatLng(35.1578,129.06));
            window.releasePin=pins.find(p=>p.coursePin && p.attached);
            window.pressTarget=document.createElement('div');window.releaseTarget=document.createElement('div');
            document.getElementById('map').appendChild(pressTarget);document.getElementById('map').appendChild(releaseTarget);
            window.gesturePoint=window.mapViewport.projection().containerPointFromCoords(new kakao.maps.LatLng(releasePlace.lat,releasePlace.lng));
            var viewport=document.getElementById('map'),rect=viewport.getBoundingClientRect();
            window.gestureX=rect.left+gesturePoint.x*rect.width/viewport.clientWidth;
            window.gestureY=rect.top+(gesturePoint.y-30)*rect.height/viewport.clientHeight;
            window.gestureEvent=function(type,target,dx){target.dispatchEvent(new MouseEvent(type,{bubbles:true,cancelable:true,button:0,
                buttons:type==='mousedown'||type==='mousemove'?1:0,detail:1,clientX:gestureX+(dx||0),clientY:gestureY}));};
            releasePin.onmouseover();window.releasePreview=routeOverlays.find(p=>p.options.content.classList.contains('place-pin-preview'));
            window.releasePanCount=mapPans.length;
            gestureEvent('mousedown',pressTarget);gestureEvent('mousemove',pressTarget,1);releasePin.onmouseout();
            """);
        Thread.sleep(220);
        assertEquals(true,script(engine,"releasePreview.attached"));
        script(engine,"pressTarget.remove();gestureEvent('mouseup',releaseTarget,1)");
        assertEquals("손 뗄 때 상세 열기",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(false,script(engine,"document.getElementById('place-detail').hidden"));
        assertEquals(0,((Number)script(engine,"mapPans.length-releasePanCount")).intValue());
        script(engine,"releasePin.onclick();gestureEvent('click',releaseTarget,1)");
        assertEquals(0,((Number)script(engine,"mapPans.length-releasePanCount")).intValue());
    }
    @Test void pinPressDraggedBeforeReleaseDoesNotOpenDetails() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.dragPlace={recordId:1,placeId:'drag',name:'드래그 장소',lat:35.1578,lng:129.06};
            window.presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:[dragPlace]},null,500,true,false,[]);
            testMap.setCenter(new kakao.maps.LatLng(35.1578,129.06));
            window.dragPin=pins.find(p=>p.coursePin && p.attached);
            var viewport=document.getElementById('map'),rect=viewport.getBoundingClientRect();
            var point=window.mapViewport.projection().containerPointFromCoords(new kakao.maps.LatLng(dragPlace.lat,dragPlace.lng));
            var x=rect.left+point.x*rect.width/viewport.clientWidth,y=rect.top+(point.y-30)*rect.height/viewport.clientHeight;
            viewport.dispatchEvent(new MouseEvent('mousedown',{bubbles:true,button:0,buttons:1,clientX:x,clientY:y}));
            viewport.dispatchEvent(new MouseEvent('mousemove',{bubbles:true,buttons:1,clientX:x+40,clientY:y}));
            viewport.dispatchEvent(new MouseEvent('mousemove',{bubbles:true,buttons:1,clientX:x,clientY:y}));
            viewport.dispatchEvent(new MouseEvent('mouseup',{bubbles:true,button:0,clientX:x,clientY:y}));
            dragPin.onclick();
            viewport.dispatchEvent(new MouseEvent('click',{bubbles:true,button:0,detail:1,clientX:x,clientY:y}));
            """);
        assertEquals(true,script(engine,"document.getElementById('place-detail').hidden"));
    }

    @Test void oneDomClickOnHoveredPinOpensDetailsThroughSdkOverlayWrapper() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.singleClickPlaces=[{recordId:1,placeId:'a',name:'한 번 클릭 장소',lat:35.1578,lng:129.06},
                {recordId:2,placeId:'b',name:'다른 핀 장소',lat:35.159,lng:129.06}];
            window.presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:singleClickPlaces},null,500,true,false,[]);
            testMap.setCenter(new kakao.maps.LatLng(35.1578,129.06));
            window.wrapperClicks=0;window.clickShield=document.createElement('div');clickShield.className='sdk-overlay-wrapper';
            clickShield.addEventListener('click',function(){wrapperClicks++;if(!window.simulatingDrag)testMap.onclick({latLng:new kakao.maps.LatLng(35.1578,129.06)});});
            document.getElementById('map').appendChild(clickShield);
            window.dispatchAtPin=function(place,type,target,offsetX){
                var mapElement=document.getElementById('map'),rect=mapElement.getBoundingClientRect();
                var point=window.mapViewport.projection().containerPointFromCoords(new kakao.maps.LatLng(place.lat,place.lng));
                (target||clickShield).dispatchEvent(new MouseEvent(type,{bubbles:true,cancelable:true,button:0,detail:type==='click'?1:0,
                    clientX:rect.left+(point.x+(offsetX||0))*rect.width/mapElement.clientWidth,
                    clientY:rect.top+(point.y-30)*rect.height/mapElement.clientHeight}));
            };
            dispatchAtPin(singleClickPlaces[0],'mousemove');window.panCountBefore=mapPans.length;
            dispatchAtPin(singleClickPlaces[0],'click');
            """);
        assertEquals("한 번 클릭 장소",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(false,script(engine,"document.getElementById('place-detail').hidden"));
        assertEquals(0,((Number)script(engine,"wrapperClicks")).intValue());
        assertEquals(0,((Number)script(engine,"mapPans.length-panCountBefore")).intValue());
        script(engine,"dispatchAtPin(singleClickPlaces[1],'mousemove');dispatchAtPin(singleClickPlaces[1],'click')");
        assertEquals("다른 핀 장소",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(0,((Number)script(engine,"mapPans.length-panCountBefore")).intValue());
        script(engine,"window.simulatingDrag=true;dispatchAtPin(singleClickPlaces[0],'mousedown',clickShield,-100);dispatchAtPin(singleClickPlaces[0],'click');window.simulatingDrag=false");
        assertEquals("다른 핀 장소",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(0,((Number)script(engine,"mapPans.length-panCountBefore")).intValue());
        script(engine,"window.routeControl=document.createElement('button');routeControl.className='walking-leg-badge';window.routeControlClicks=0;routeControl.onclick=function(){routeControlClicks++;};document.getElementById('map').appendChild(routeControl);dispatchAtPin(singleClickPlaces[1],'click',routeControl)");
        assertEquals(1,((Number)script(engine,"routeControlClicks")).intValue());
        assertEquals(0,((Number)script(engine,"mapPans.length-panCountBefore")).intValue());
    }

    @Test void pointerInsidePinKeepsPreviewDespiteRepeatedSdkExitEvents() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.coordinatePlace={recordId:1,placeId:'a',name:'좌표 판정 장소',lat:35.1578,lng:129.06};
            window.presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:[coordinatePlace]},null,500,true,false,[]);
            testMap.setCenter(new kakao.maps.LatLng(35.1578,129.06));
            window.coordinatePin=pins.find(p=>p.coursePin && p.attached);
            window.movePinPointer=function(place,dx,dy,target){
                var element=document.getElementById('map'),rect=element.getBoundingClientRect();
                var point=window.mapViewport.projection().containerPointFromCoords(new kakao.maps.LatLng(place.lat,place.lng));
                (target||element).dispatchEvent(new MouseEvent('mousemove',{bubbles:true,clientX:rect.left+(point.x+dx)*rect.width/element.clientWidth,
                    clientY:rect.top+(point.y+dy)*rect.height/element.clientHeight}));
            };
            coordinatePin.onmouseover();movePinPointer(coordinatePlace,0,-30);
            window.coordinatePreview=routeOverlays.find(p=>p.options.content.classList.contains('place-pin-preview'));
            coordinatePin.onmouseout();
            """);
        Thread.sleep(250);
        assertEquals(true,script(engine,"coordinatePreview.attached"));
        for(int x:new int[]{-15,12,-8,18}) {
            script(engine,"movePinPointer(coordinatePlace,"+x+",-25);coordinatePin.onmouseout()");Thread.sleep(180);
            assertEquals(true,script(engine,"coordinatePreview.attached"));
        }
        script(engine,"movePinPointer(coordinatePlace,100,-30)");Thread.sleep(250);
        assertEquals(false,script(engine,"coordinatePreview.attached"));
    }
    @Test void pointerCoordinatesCanEnterAndSwitchPinsWithoutSdkMouseover() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.coordinatePlaces=[{recordId:1,placeId:'a',name:'첫 좌표 장소',lat:35.1578,lng:129.06},
                {recordId:2,placeId:'b',name:'두 번째 좌표 장소',lat:35.159,lng:129.06}];
            window.presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:coordinatePlaces},null,500,true,false,[]);
            testMap.setCenter(new kakao.maps.LatLng(35.1578,129.06));
            document.getElementById('map').style.transform='scale(1.1)';
            window.moveCoordinates=function(place,dx,dy){
                var element=document.getElementById('map'),rect=element.getBoundingClientRect();
                var point=window.mapViewport.projection().containerPointFromCoords(new kakao.maps.LatLng(place.lat,place.lng));
                element.dispatchEvent(new MouseEvent('mousemove',{bubbles:true,clientX:rect.left+(point.x+dx)*rect.width/element.clientWidth,
                    clientY:rect.top+(point.y+dy)*rect.height/element.clientHeight}));
            };
            moveCoordinates(coordinatePlaces[0],0,-30);
            window.coordinatePreview=routeOverlays.find(p=>p.options.content.classList.contains('place-pin-preview'));
            """);
        assertEquals(true,script(engine,"!!coordinatePreview && coordinatePreview.attached"));
        script(engine,"moveCoordinates(coordinatePlaces[0],31,-30)");Thread.sleep(220);
        assertEquals(true,script(engine,"coordinatePreview.attached"));
        script(engine,"moveCoordinates(coordinatePlaces[1],0,-30)");
        assertTrue(((String)script(engine,"coordinatePreview.options.content.textContent")).contains("두 번째 좌표 장소"));
        script(engine,"moveCoordinates(coordinatePlaces[1],100,-30)");Thread.sleep(250);
        assertEquals(false,script(engine,"coordinatePreview.attached"));
        script(engine,"moveCoordinates(coordinatePlaces[0],0,-30);window.resetPlannerMap()");Thread.sleep(220);
        assertEquals(false,script(engine,"coordinatePreview.attached"));
    }

    @Test void placePinHitAreaAndAnchorStayFixedAcrossSelection() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:[
                {recordId:1,placeId:'a',name:'첫 장소',lat:35.1578,lng:129.06},
                {recordId:2,placeId:'b',name:'두 번째 장소',lat:35.1579,lng:129.06}]},null,500,true,false,[]);
            window.paddedPins=pins.filter(p=>p.coursePin && p.attached);
            """);
        assertEquals(true,script(engine,"paddedPins.every(p=>p.normalImage.size.width===56 && p.normalImage.size.height===68 && p.activeImage.size.width===56 && p.activeImage.size.height===68)"));
        assertEquals(true,script(engine,"paddedPins.every(p=>p.normalImage.options.shape==='rect' && p.normalImage.options.coords==='0,0,56,68' && JSON.stringify(p.normalImage.options)===JSON.stringify(p.activeImage.options))"));
        assertEquals(true,script(engine,"paddedPins.every(p=>p.normalImage.options.offset.x===28 && p.normalImage.options.offset.y===60)"));
        script(engine,"paddedPins[1].onclick();paddedPins[1].onmouseover()");
        assertEquals(true,script(engine,"paddedPins[1].image===paddedPins[1].activeImage && paddedPins[1].image.size.width===56"));
    }
    @Test void pinExitImmediatelyHidesHoverAndOldLeaveCannotCloseAnotherPreview() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:[
                {recordId:1,placeId:'a',name:'첫 장소',lat:35.1578,lng:129.06},
                {recordId:2,placeId:'b',name:'두 번째 장소',lat:35.1579,lng:129.06}]},null,500,true,false,[]);
            window.hoverPins=pins.filter(p=>p.coursePin && p.attached);hoverPins[0].onmouseover();
            window.stablePreview=routeOverlays.find(p=>p.options.content.classList.contains('place-pin-preview'));
            hoverPins[0].onmouseout();
            """);
        assertEquals(false,script(engine,"stablePreview.attached"));
        Thread.sleep(50);script(engine,"hoverPins[0].onmouseover()");Thread.sleep(220);
        assertEquals(true,script(engine,"stablePreview.attached"));
        script(engine,"hoverPins[0].onmouseout();hoverPins[1].onmouseover();hoverPins[0].onmouseout()");Thread.sleep(220);
        assertEquals(true,script(engine,"stablePreview.attached && stablePreview.options.content.textContent.includes('두 번째 장소')"));
        script(engine,"hoverPins[1].onmouseout()");Thread.sleep(250);
        assertEquals(false,script(engine,"stablePreview.attached"));
        script(engine,"hoverPins[1].onmouseover();hoverPins[1].onmouseout();window.resetPlannerMap()");Thread.sleep(220);
        assertEquals(false,script(engine,"stablePreview.attached"));
    }

    @Test void clickedPlaceDetailsSurviveHoverAndHiddenRailScrollUntilExplicitDismissal() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.stickyPlan={startLat:35.1578,startLng:129.06,radius:1000,places:[
                {recordId:1,placeId:'a',name:'첫 장소',lat:35.1578,lng:129.06},
                {recordId:2,placeId:'b',name:'두 번째 장소',lat:35.1579,lng:129.06}
            ]};
            window.presentRoute(stickyPlan,null,500,true,false,[]);
            window.stickyPins=pins.filter(p=>p.coursePin && p.attached);stickyPins[1].onclick();
            stickyPins[1].onmouseout();stickyPins[0].onmouseover();
            document.getElementById('itinerary-viewport').dispatchEvent(new Event('scroll'));
            """);
        Thread.sleep(400);
        assertEquals("두 번째 장소",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(false,script(engine,"document.getElementById('place-detail').hidden"));
        assertEquals(true,script(engine,"document.getElementById('place-detail').classList.contains('is-open')"));
        script(engine,"stickyPins[0].onclick();stickyPins[0].onmouseout();stickyPins[1].onmouseover()");
        Thread.sleep(250);
        assertEquals("첫 장소",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(false,script(engine,"document.getElementById('place-detail').hidden"));
        script(engine,"testMap.onclick({latLng:new kakao.maps.LatLng(35.16,129.061)})");
        Thread.sleep(250);
        assertEquals(true,script(engine,"document.getElementById('place-detail').hidden"));
    }
    @Test void candidateFocusRefreshDoesNotDismissClickedCourseDetails() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.focusPlan={startLat:35.1578,startLng:129.06,radius:1000,places:[
                {recordId:1,placeId:'a',name:'확정 장소',lat:35.1578,lng:129.06}]};
            window.focusCandidate={recordId:2,placeId:'b',name:'후보',lat:35.1579,lng:129.06};
            window.presentRoute(focusPlan,focusCandidate,500,true,false,[focusCandidate]);
            pins.find(p=>p.coursePin && p.attached).onclick();
            window.presentRoute(focusPlan,focusCandidate,500,true,false,[focusCandidate]);
            """);
        Thread.sleep(250);
        assertEquals("확정 장소",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(false,script(engine,"document.getElementById('place-detail').hidden"));
    }

    @Test void confirmedPinPreviewIsPassiveAndClickOpensDetailsWithWalkingRoutePresent() throws Exception {
        WebEngine engine=loadMap(850,720);
        var place=new com.datecourse.service.PublicDemoPlaceRepository().findAll().getFirst();
        var payload=new com.datecourse.service.PlaceMapPayload(new com.fasterxml.jackson.databind.ObjectMapper(),new com.datecourse.service.PlacePresenter());
        script(engine,"window.previewPlace="+payload.place(place)+";window.presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:[previewPlace]},null,500,false,false,[]);");
        script(engine,"""
            window.showWalkingRoute({totalDistance:100,totalTime:90,legs:[{from:{name:'출발'},to:{name:'대모골'},distance:100,time:90,
                paths:[[{lat:35.1578,lng:129.06},{lat:35.1592,lng:129.0588}]],steps:[{description:'길을 따라 이동'}]}]},'',false);
            window.redPin=pins.find(p=>p.coursePin && p.attached);window.beforePreviewSummary=document.getElementById('walking-summary').textContent;
            redPin.onmouseover();window.pinPreview=routeOverlays.find(p=>p.options.content.classList.contains('place-pin-preview'));
            """);
        assertEquals(true,script(engine,"pinPreview.attached"));
        assertEquals("대모골",script(engine,"pinPreview.options.content.querySelector('.place-pin-preview-title').textContent"));
        assertTrue(((String)script(engine,"pinPreview.options.content.textContent")).contains("약 7,000원/인"));
        assertTrue(((String)script(engine,"pinPreview.options.content.textContent")).contains("클릭해서 장소 상세 보기"));
        assertEquals(true,script(engine,"document.getElementById('place-detail').hidden"));
        assertEquals(true,script(engine,"document.getElementById('walking-summary').textContent===beforePreviewSummary"));
        script(engine,"document.getElementById('map-surface').appendChild(pinPreview.options.content);Object.assign(pinPreview.options.content.style,{position:'absolute',right:'24px',top:'140px',transform:'none'});");
        var previewWindow=fx(()->{var stage=new javafx.stage.Stage();stage.setScene(previewView.getScene());stage.show();return stage;});
        try {
            Thread.sleep(200);
            fx(()->{Path image=Path.of("build/ui-preview/place-pin-hover.png");Files.createDirectories(image.getParent());
                ImageIO.write(SwingFXUtils.fromFXImage(previewView.snapshot(null,null),null),"png",image.toFile());return null;});
        } finally {fx(()->{previewWindow.close();return null;});}
        script(engine,"pinPreview.options.content.remove();redPin.onmouseout()");
        Thread.sleep(250);assertEquals(false,script(engine,"pinPreview.attached"));
        script(engine,"redPin.onmouseover();redPin.onclick()");
        assertEquals(false,script(engine,"pinPreview.attached"));
        assertEquals("대모골",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(false,script(engine,"document.getElementById('place-detail').hidden"));
        assertEquals(false,script(engine,"routeOverlays.some(p=>p.attached && p.options.content.classList.contains('walking-route-tooltip'))"));
        script(engine,"routeLines.find(p=>p.onclick).onclick()");
        assertEquals(true,script(engine,"!document.getElementById('walking-details').hidden && document.getElementById('detail-title').textContent.startsWith('1구간')"));
    }
    @Test void removedPinCannotReopenPreviewAndOldMouseoutCannotHideNewPin() throws Exception {
        WebEngine engine=loadMap(420,680);
        script(engine,"""
            window.presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:[
                {recordId:1,placeId:'a',name:'첫 장소',lat:35.1578,lng:129.06},
                {recordId:2,placeId:'b',name:'<img src=x onerror=alert(1)> 두 번째 장소',lat:35.1579,lng:129.06}
            ]},null,500,false,false,[]);
            window.redPins=pins.filter(p=>p.coursePin && p.attached);
            redPins[0].onmouseover();redPins[1].onmouseover();redPins[0].onmouseout();
            window.pinPreview=routeOverlays.find(p=>p.options.content.classList.contains('place-pin-preview'));
            """);
        assertEquals(true,script(engine,"pinPreview.attached"));
        assertEquals(0,((Number)script(engine,"pinPreview.options.content.querySelectorAll('img').length")).intValue());
        assertTrue(((String)script(engine,"pinPreview.options.content.textContent")).contains("두 번째 장소"));
        script(engine,"window.lateHover=redPins[1].onmouseover;window.resetPlannerMap();lateHover()");
        assertEquals(false,script(engine,"pinPreview.attached"));
        assertEquals("undefined",script(engine,"typeof redPins[1].onmouseover"));
    }

    @Test void javaPresentationFlowsIntoMapCardWithoutOverflowOrSourceFields() throws Exception {
        var place=new com.datecourse.service.PublicDemoPlaceRepository().findAll().getFirst();
        var payload=new com.datecourse.service.PlaceMapPayload(new com.fasterxml.jackson.databind.ObjectMapper(),new com.datecourse.service.PlacePresenter());
        for(int width:new int[]{850,420}) {
            WebEngine engine=loadMap(width,720);
            script(engine,"window.typedPlace="+payload.place(place)+";window.presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:[]},typedPlace,500,true,true,[typedPlace]);window.openRoutePlace("+place.recordId()+");");
            assertEquals(3,((Number)script(engine,"document.querySelectorAll('#detail-extra .detail-badge').length")).intValue());
            assertTrue(((String)script(engine,"document.querySelector('#detail-facts dt').textContent")).contains("추정"));
            assertTrue(((String)script(engine,"document.getElementById('detail-extra').textContent")).contains("부산 착한가격업소"));
            assertFalse(((String)script(engine,"document.getElementById('detail-extra').textContent")).contains("https://"));
            assertEquals(true,script(engine,"Array.from(document.querySelectorAll('.detail-badge')).some(function(n){return n.title.includes('계산 2026-11-16');})"));
            assertEquals(true,script(engine,"document.getElementById('detail-extra').scrollWidth<=document.getElementById('detail-extra').clientWidth+1"));
            assertEquals(true,script(engine,"document.getElementById('place-detail').getBoundingClientRect().left>=0 && document.getElementById('place-detail').getBoundingClientRect().right<=window.innerWidth+1"));
            assertEquals(true,script(engine,"document.getElementById('detail-add').getBoundingClientRect().bottom<=document.getElementById('place-detail').getBoundingClientRect().bottom"));
            var window=fx(()->{var stage=new javafx.stage.Stage();stage.setScene(previewView.getScene());stage.show();return stage;});
            try {
                Thread.sleep(300);
                fx(()->{Path image=Path.of("build/ui-preview/public-data-detail-"+width+".png");Files.createDirectories(image.getParent());
                    ImageIO.write(SwingFXUtils.fromFXImage(previewView.snapshot(null,null),null),"png",image.toFile());return null;});
            } finally {fx(()->{window.close();return null;});}
        }
    }
    @Test void editorDiscoversOnlyConfirmedFacilityOptionsFromTypedFacts() throws Exception {
        fx(()->{
            var evidence=new com.datecourse.model.SourceEvidence(com.datecourse.model.SourceEvidence.Origin.OFFICIAL_SITE,"운영 안내","https://example.test","2026-11-14","");
            var fact=new com.datecourse.model.PlaceAttributeFact(com.datecourse.model.PlaceFeature.RESERVABLE,com.datecourse.model.FactState.AVAILABLE,evidence);
            var details=new com.datecourse.model.PlaceDetails("주소","한식","","","https://example.test","2026-11-14","","",java.util.Map.of(),java.util.List.of(),java.util.List.of(fact),java.util.List.of());
            var place=new com.datecourse.model.Place(1,"facility","예약 장소","MEAL",35.1578,129.06,com.datecourse.model.PriceEstimate.exact(5000),0,details);
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            editor.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,1000,"",java.util.List.of()),java.util.List.of(place),p->{},()->{});
            var tags=(com.datecourse.page.common.controls.PlaceFeatureSelector)field(editor,"featureSelector");
            assertTrue(tags.availableTags().contains(com.datecourse.model.PlaceTag.RESERVABLE));
            assertFalse(tags.availableTags().contains(com.datecourse.model.PlaceTag.PARKING));
            tags.selectedTags().add(com.datecourse.model.PlaceTag.RESERVABLE);invoke(editor,"onRecommendNext");
            assertEquals(1,((javafx.scene.control.ListView<?>)field(editor,"candidateList")).getItems().size());
            return null;
        });
    }

    @Test
    void myPageUsesSharedLoadingAndMenuLogoutClearsPlannerSession() throws Exception {
        CountDownLatch loaded=new CountDownLatch(1);
        CountDownLatch releaseProfile=new CountDownLatch(1);
        var opened=new java.util.concurrent.atomic.AtomicInteger();
        MyPageController page=fx(() -> {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/account/my-page-view.fxml"));
            Parent root=loader.load();
            StackPane host=new StackPane(root);new Scene(host,960,680);
            host.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            MyPageController controller=loader.getController();
            controller.useFeedback(new PageFeedback(host));
            ((javafx.scene.control.Label)field(controller,"statusLabel")).textProperty()
                    .addListener((o,old,text)->{if(text.isEmpty())loaded.countDown();});
            controller.configure(new com.datecourse.service.UserContext(new com.datecourse.model.UserSession(7,"내 이름"),
                    ()->{releaseProfile.await();return new com.datecourse.db.MemberRepository.Profile("me@test","내 이름",null);},java.util.List::of),
                    opened::incrementAndGet,opened::incrementAndGet);
            assertTrue(controller.isLoading());return controller;
        });
        releaseProfile.countDown();
        assertTrue(loaded.await(10,TimeUnit.SECONDS));
        fx(() -> {
            assertEquals("내 이름",((javafx.scene.control.Label)field(page,"nameLabel")).getText());
            assertEquals("me@test",((javafx.scene.control.Label)field(page,"emailLabel")).getText());
            assertEquals("미등록",((javafx.scene.control.Label)field(page,"phoneLabel")).getText());
            invoke(page,"onSavedCourses");invoke(page,"onBack");assertEquals(2,opened.get());
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));
            Parent root=loader.load();new Scene(root,1440,960);
            CoursePlannerController main=loader.getController();
            stubPlannerMap(main,"mapView");stubPlannerMap(field(main,"routeEditorController"),"editMapView");
            main.setUserSession(new com.datecourse.model.UserSession(7,"내 이름"));
            var loggedOut=new java.util.concurrent.atomic.AtomicBoolean();
            main.setAccountActions(null,()->loggedOut.set(true));
            var menu=(javafx.scene.control.MenuButton)field(main,"userLabel");
            assertEquals("내 이름",menu.getText().replaceFirst("\\s*님\\s*$", ""));assertFalse(menu.isDisabled());
            assertEquals("내 페이지",menu.getItems().get(0).getText());
            menu.getItems().get(2).fire();
            assertTrue(loggedOut.get());assertNull(field(main,"userSession"));assertTrue(menu.isDisabled());
            assertNull(field(main,"appliedPlan"));
            return null;
        });
    }

    @Test void routeFocusSurvivesHoverAndMovesOnlyOnClickWithoutMovingOrigin() throws Exception {
        WebEngine engine=loadMap();
        script(engine,"""
            window.blocked=0;javaMapBridge.onOriginChangeBlocked=function(){blocked++;};
            window.selectMapLocation(35.1579,129.059,'출발');window.setOriginLocked(true);
            window.testPath=[[{lat:35.1579,lng:129.059},{lat:35.158,lng:129.059}]];
            window.showWalkingRoute({totalDistance:200,totalTime:160,legs:[
              {from:{name:'출발'},to:{name:'첫 장소'},distance:100,time:80,paths:testPath,steps:[]},
              {from:{name:'첫 장소'},to:{name:'둘째 장소'},distance:100,time:80,paths:testPath,steps:[]}
            ]},'',false);
            """);
        assertEquals(true,script(engine,"routeLines[1].options.strokeOpacity===.55 && routeLines[3].options.strokeOpacity===.55"));
        script(engine,"routeLines[1].onclick();testMap.onclick({latLng:new kakao.maps.LatLng(35.158,129.059)});routeLines[1].onmouseout();routeLines[3].onmouseover()");
        assertEquals(true,script(engine,"blocked===0 && routeOverlays[0].attached && routeOverlays[0].options.content.textContent.includes('2구간')"));
        assertEquals(true,script(engine,"routeLines[1].options.strokeOpacity===1 && routeLines[3].options.strokeOpacity===1"));
        script(engine,"routeLines[3].onmouseout()");
        assertEquals(true,script(engine,"routeLines[1].options.strokeOpacity===1 && routeLines[3].options.strokeOpacity===.55 && !routeOverlays[0].attached"));
        script(engine,"routeLines[3].onclick();routeLines[3].onmouseout()");
        assertEquals(true,script(engine,"routeLines[1].options.strokeOpacity===.55 && routeLines[3].options.strokeOpacity===1 && document.getElementById('detail-title').textContent.startsWith('2구간')"));
        Thread.sleep(50);
        script(engine,"testMap.onclick({latLng:new kakao.maps.LatLng(35.16,129.06)})");
        assertEquals(true,script(engine,"!routeOverlays[0].attached && blocked===0 && routeLines[3].options.strokeOpacity===.55"));
    }

    @Test void accountPagesSharePrefetchedDataAndDoNotQueryOnRepeatedVisits() throws Exception {
        var profileQueries=new java.util.concurrent.atomic.AtomicInteger();
        var courseQueries=new java.util.concurrent.atomic.AtomicInteger();
        var profile=new com.datecourse.db.MemberRepository.Profile("cached@test","캐시 회원",null);
        var plan=new com.datecourse.model.RoutePlan(35,129,2,500,"저장된 코스",java.util.List.of());
        var saved=new com.datecourse.model.SavedCourse(1,7,plan);
        try(var context=new com.datecourse.service.UserContext(new com.datecourse.model.UserSession(7,"캐시 회원"),
                ()->{profileQueries.incrementAndGet();return profile;},
                ()->{courseQueries.incrementAndGet();return java.util.List.of(saved);})) {
            context.preload();context.profile().get(5,TimeUnit.SECONDS);context.courses().get(5,TimeUnit.SECONDS);
            var savedPage=fx(()->{
                FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_03step_saved/saved-course-view.fxml"));loader.load();
                SavedCourseController controller=loader.getController();
                var repository=new com.datecourse.db.SavedCourseRepository(new org.h2.jdbcx.JdbcDataSource());
                controller.configure(repository,context,c->{},()->{});controller.configure(repository,context,c->{},()->{});
                assertSame(context,field(controller,"userContext"));assertFalse(controller.isLoading());
                assertEquals(1,((javafx.scene.control.ListView<?>)field(controller,"courseList")).getItems().size());
                FXMLLoader profileLoader=new FXMLLoader(getClass().getResource("/com/datecourse/page/account/my-page-view.fxml"));profileLoader.load();
                MyPageController page=profileLoader.getController();
                page.configure(context,()->{},()->{});page.configure(context,()->{},()->{});
                assertSame(context,field(page,"userContext"));assertFalse(page.isLoading());
                assertEquals("캐시 회원",((javafx.scene.control.Label)field(page,"nameLabel")).getText());
                return controller;
            });
            assertEquals(1,profileQueries.get());assertEquals(1,courseQueries.get());
            CountDownLatch refreshed=new CountDownLatch(1);
            fx(()->{
                ((javafx.scene.control.Label)field(savedPage,"messageLabel")).textProperty().addListener((o,a,b)->{if(b.isEmpty())refreshed.countDown();});
                invoke(savedPage,"onRefreshClick");return null;
            });
            assertTrue(refreshed.await(5,TimeUnit.SECONDS));
            assertEquals(2,courseQueries.get());assertEquals(1,profileQueries.get());
        }
    }

    @Test void oldAccountProfileCannotRepopulateThePageAfterLogout() throws Exception {
        CountDownLatch release=new CountDownLatch(1);
        var oldContext=new com.datecourse.service.UserContext(new com.datecourse.model.UserSession(1,"이전 회원"),()->{
            release.await(5,TimeUnit.SECONDS);return new com.datecourse.db.MemberRepository.Profile("old@test","이전 회원",null);
        },null);
        var oldFuture=oldContext.profile();
        try(var next=new com.datecourse.service.UserContext(new com.datecourse.model.UserSession(2,"새 회원"),
                ()->new com.datecourse.db.MemberRepository.Profile("new@test","새 회원",null),null)) {
            next.profile().get(5,TimeUnit.SECONDS);
            var page=fx(()->{
                FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/account/my-page-view.fxml"));loader.load();
                MyPageController controller=loader.getController();controller.configure(oldContext,()->{},()->{});
                oldContext.close();controller.clear();controller.configure(next,()->{},()->{});return controller;
            });
            release.countDown();oldFuture.get(5,TimeUnit.SECONDS);
            fx(()->{assertEquals("새 회원",((javafx.scene.control.Label)field(page,"nameLabel")).getText());return null;});
        } finally {release.countDown();oldContext.close();}
    }

    @Test void editingLegRangeFocusAndCandidateCountFollowTheActiveSection() throws Exception {
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();
            CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            var a=new com.datecourse.model.Place(1,"a","A","MEAL",35.001,129,3000,4);
            var b=new com.datecourse.model.Place(2,"b","B","MEAL",35.002,129,3000,4);
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of(a,b),java.util.Map.of(1L,500,2L,500)),java.util.List.of(a,b),p->{},()->{});
            var list=(javafx.scene.control.ListView<?>)field(editor,"placeList");
            var map=field(editor,"map");
            list.getSelectionModel().select(1);
            ((javafx.scene.control.Slider)field(editor,"selectedRadiusSlider")).setValue(700);
            assertEquals(1,field(map,"editingLegIndex"));assertEquals(700,field(map,"editingLegRadius"));
            assertEquals(700,editor.draftSnapshot().arrivalRadii().get(2L));
            invoke(editor,"onModifyConditions");
            assertEquals(-1,list.getSelectionModel().getSelectedIndex());
            assertNull(field(map,"editingLegIndex"));assertNull(field(map,"editingLegRadius"));
            var conditionCount=(javafx.scene.control.ComboBox<Integer>)field(editor,"conditionCandidateLimitBox");
            var resultCount=(javafx.scene.control.ComboBox<Integer>)field(editor,"candidateLimitBox");
            conditionCount.setValue(10);assertEquals(10,resultCount.getValue());
            resultCount.setValue(20);assertEquals(20,conditionCount.getValue());
            return null;
        });
        WebEngine engine=loadMap(640,600);
        script(engine,"""
            window.presentRoute({startLat:35,startLng:129,radius:1000,places:[
                {recordId:1,placeId:'a',name:'A',category:'MEAL',lat:35.001,lng:129},
                {recordId:2,placeId:'b',name:'B',category:'MEAL',lat:35.002,lng:129}
            ]},null,500,true,false,[]);
            window.green=pins.find(function(p){return p.options.strokeColor==='#15803d';});
            window.focusRouteLeg(1,700);
            """);
        assertEquals(true,script(engine,"green.position.getLat()===35.001 && green.radius===700"));
        script(engine,"window.focusRouteLeg(0,300)");
        assertEquals(true,script(engine,"green.position.getLat()===35 && green.radius===300"));
        script(engine,"window.focusRouteLeg(null,null)");
        assertEquals(true,script(engine,"green.position.getLat()===35.002 && green.radius===500"));
    }

    @Test void commonResetRestoresDefaultsAndClearsSelectedOriginAndMapSnapshot() throws Exception {
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));loader.load();
            CoursePlannerController main=loader.getController();stubPlannerMap(main,"mapView");
            stubPlannerMap(field(main,"routeEditorController"),"editMapView");
            var people=(TextField)field(main,"peopleField");
            var radius=(javafx.scene.control.Slider)field(main,"radiusSlider");
            var source=(javafx.scene.control.ComboBox<?>)field(main,"sourceBox");
            Object originalSource=source.getValue();
            people.setText("7");radius.setValue(1600);
            setField(main,"selectedLatitude",35.0);setField(main,"selectedLongitude",129.0);
            invoke(main,"updateRange");
            assertNotNull(field(field(main,"map"),"plan"));
            assertFalse(((javafx.scene.control.Button)field(main,"newCourseButton")).isDisabled());
            invoke(main,"onNewCourse");
            assertEquals("2",people.getText());assertEquals(500,radius.getValue());
            assertNull(field(main,"selectedLatitude"));assertNull(field(main,"selectedLongitude"));
            assertNull(field(main,"appliedPlan"));assertNull(field(main,"savedCourseId"));
            assertNull(field(field(main,"map"),"plan"));
            assertFalse((Boolean)field(field(main,"map"),"originLocked"));
            assertEquals(originalSource,source.getValue());
            assertTrue(((javafx.scene.control.Button)field(main,"newCourseButton")).isDisabled());
            setField(main,"placesLoaded",true);invoke(main,"onRecommendClick");
            assertTrue(((javafx.scene.control.Label)field(main,"errorLabel")).getText().contains("출발 장소"));
            return null;
        });
    }

    @Test void mapResetRemovesOriginShapesAndDoesNotRestoreOriginOnReconnect() throws Exception {
        WebEngine engine=loadMap(640,600);
        script(engine,"""
            window.presentRoute({startLat:35,startLng:129,radius:1600,places:[
                {recordId:1,placeId:'a',name:'식당',category:'MEAL',lat:35.001,lng:129,price:5000}
            ]},null,500,true,false,[]);
            window.openRoutePlace(1);window.setOriginLocked(true);
            window.showWalkingRoute({totalDistance:100,totalTime:60,legs:[
                {from:{name:'출발'},to:{name:'식당'},distance:100,time:60,
                 paths:[[{lat:35,lng:129},{lat:35.001,lng:129}]],steps:[]}
            ]},'',false);
            window.resetPlannerMap();window.connectJavaMapBridge();
            """);
        assertEquals(true,script(engine,"pins.every(function(p){return !p.attached;}) && routeOverlays.every(function(o){return !o.attached;}) && routeLines.every(function(l){return !l.attached;})"));
        assertEquals("[]",script(engine,"JSON.stringify(bridgeSelections)"));
        assertEquals("true",script(engine,"document.getElementById('place-detail').getAttribute('aria-hidden')"));
        assertEquals(true,script(engine,"document.getElementById('walking-panel').hidden && !pins.some(c=>c.attached && c.options.strokeColor==='#15803d')"));
        assertEquals(35.1796,((Number)script(engine,"testMap.getCenter().getLat()")).doubleValue(),0.000001);
        assertEquals(5,((Number)script(engine,"testMap.getLevel()")).intValue());
        script(engine,"testMap.onclick({latLng:new kakao.maps.LatLng(35.16,129.06)})");
        assertEquals("[[35.16,129.06]]",script(engine,"JSON.stringify(bridgeSelections)"));
    }

    @Test void overlappingPinWinsRouteHoverAndClickAndOnlyOneTooltipExists() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:[
                {recordId:1,placeId:'a',name:'겹친 장소',lat:35.158,lng:129.06}]},null,500,true,false,[]);
            window.path=[[{lat:35.1578,lng:129.06},{lat:35.158,lng:129.06}]];
            window.showWalkingRoute({totalDistance:100,totalTime:80,legs:[
                {from:{name:'출발'},to:{name:'겹친 장소'},distance:100,time:80,paths:path,steps:[]}
            ]},'',false);
            window.pin=pins.find(p=>p.coursePin && p.attached);
            testMap.setCenter(new kakao.maps.LatLng(35.158,129.06));
            window.point=window.mapViewport.projection().containerPointFromCoords(new kakao.maps.LatLng(35.158,129.06));
            window.rect=document.getElementById('map').getBoundingClientRect();
            document.getElementById('map').dispatchEvent(new MouseEvent('mousemove',{bubbles:true,clientX:rect.left+point.x,clientY:rect.top+point.y-20}));
            routeLines[1].onmouseover();routeLines[1].onclick();
            """);
        assertEquals(true,script(engine,"pin.image===pin.activeImage && document.getElementById('place-detail').hidden && document.getElementById('walking-details').hidden"));
        assertEquals(1,((Number)script(engine,"routeOverlays.filter(o=>o.attached && (o.options.content.classList.contains('place-pin-preview') || o.options.content.classList.contains('walking-route-tooltip'))).length")).intValue());
        assertEquals(true,script(engine,"document.getElementById('map-surface').classList.contains('map-interactive')"));
        script(engine,"pin.onclick();document.getElementById('map').dispatchEvent(new MouseEvent('mousemove',{bubbles:true,clientX:rect.left+10,clientY:rect.top+10}));routeLines[1].onclick()");
        assertEquals(true,script(engine,"pin.image===pin.normalImage && routeLines[1].options.strokeOpacity===1 && document.getElementById('detail-title').textContent.startsWith('1구간')"));
        script(engine,"pin.onclick()");
        assertEquals(true,script(engine,"pin.image===pin.activeImage && routeLines[1].options.strokeOpacity===.55 && document.getElementById('detail-title').textContent==='겹친 장소'"));
    }

    @Test void numberedBadgeHoverIsBriefAndClickedDetailsStayInCards() throws Exception {
        WebEngine engine=loadMap(640,600);
        script(engine,"""
            window.path=[[{lat:35,lng:129},{lat:35.001,lng:129}]];
            window.showWalkingRoute({totalDistance:200,totalTime:120,legs:[
                {from:{name:'출발'},to:{name:'첫 장소'},distance:100,time:60,paths:path,steps:[]},
                {from:{name:'첫 장소'},to:{name:'둘째 장소'},distance:100,time:60,paths:path,steps:[]}
            ]},'',false);
            window.badges=routeOverlays.filter(o=>o.options.content.className==='walking-leg-badge');
            badges[1].options.content.dispatchEvent(new Event('mouseenter'));
            """);
        assertEquals(true,script(engine,"routeLines[3].options.strokeOpacity===1 && routeLines[1].options.strokeOpacity===.55 && routeOverlays[0].attached"));
        script(engine,"badges[1].options.content.dispatchEvent(new Event('mouseleave'))");
        assertEquals(true,script(engine,"!routeOverlays[0].attached && routeLines[3].options.strokeOpacity===.55"));
        script(engine,"badges[1].options.content.click();badges[0].options.content.dispatchEvent(new Event('mouseenter'))");
        assertEquals(true,script(engine,"document.getElementById('detail-title').textContent.startsWith('2구간') && !document.getElementById('walking-details').hidden && routeOverlays[0].options.content.textContent.includes('1구간')"));
        script(engine,"badges[0].options.content.dispatchEvent(new Event('mouseleave'))");
        assertEquals(true,script(engine,"!routeOverlays[0].attached && routeLines[3].options.strokeOpacity===1 && routeLines[1].options.strokeOpacity===.55"));
    }

    @Test void routeBadgeUsesHalfDistanceAndBlankMapDismissesBothCards() throws Exception {
        WebEngine engine=loadMap(640,600);
        script(engine,"""
            window.presentRoute({startLat:35,startLng:129,radius:2000,places:[
                {recordId:1,placeId:'a',name:'도착 가게',category:'CAFE',lat:35.01,lng:129,price:5000}
            ]},null,500,true,false,[]);
            window.showWalkingRoute({totalDistance:1112,totalTime:900,legs:[
                {from:{name:'출발'},to:{name:'도착 가게'},distance:1112,time:900,
                 paths:[[{lat:35,lng:129},{lat:35.0001,lng:129},{lat:35.0002,lng:129}],
                        [{lat:35.0002,lng:129},{lat:35.01,lng:129}]],
                 steps:[{description:'도착 가게까지 직진하세요.'}]}
            ]},'',false);
            window.badge=routeOverlays.find(function(o){return o.options.content.className==='walking-leg-badge';});
            routeLines[1].onmouseover();window.popup=routeOverlays[0].options.content;
            """);
        assertEquals(35.005,((Number)script(engine,"badge.options.position.getLat()")).doubleValue(),0.0000001);
        assertEquals(0.5,((Number)script(engine,"badge.options.yAnchor")).doubleValue());
        assertEquals(true,script(engine,"!popup.querySelector('.walking-tooltip-steps') && popup.style.width==='260px'"));
        script(engine,"routeLines[1].onclick()");
        assertEquals(true,script(engine,"!routeOverlays[0].attached && document.getElementById('detail-extra').textContent.includes('도착 가게까지 직진하세요.')"));
        script(engine,"window.openRoutePlace(1)");
        assertEquals("false",script(engine,"document.getElementById('place-detail').getAttribute('aria-hidden')"));
        Thread.sleep(50);
        script(engine,"testMap.onclick({latLng:new kakao.maps.LatLng(36,130)})");
        assertEquals(true,script(engine,"!routeOverlays[0].attached && document.getElementById('place-detail').getAttribute('aria-hidden')==='true' && bridgeSelections.length===0"));
        // A place card alone consumes the dismissal click in the unlocked main map as well.
        script(engine,"window.setOriginLocked(false);window.openRoutePlace(1);testMap.onclick({latLng:new kakao.maps.LatLng(36,130)})");
        assertEquals(true,script(engine,"document.getElementById('place-detail').getAttribute('aria-hidden')==='true' && bridgeSelections.length===0"));
        script(engine,"routeLines[1].onmouseover();routeLines[1].onmouseout()");
        Thread.sleep(170);
        assertEquals(false,script(engine,"routeOverlays[0].attached"));
        script(engine,"routeLines[1].onclick();document.getElementById('detail-close').click()");
        assertEquals(false,script(engine,"routeOverlays[0].attached"));
    }

    @Test
    void emptyPlannerDisablesResetAndLoadingSavedCourseRequestsWalking() throws Exception {
        CountDownLatch requested=new CountDownLatch(1);
        fx(() -> {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));
            Parent root=loader.load();new Scene(root,1440,960);
            CoursePlannerController main=loader.getController();stubPlannerMap(main,"mapView");
            var reset=(javafx.scene.control.Button)field(main,"newCourseButton");
            assertTrue(reset.isDisabled());
            ((TextField)field(main,"peopleField")).setText("3");assertFalse(reset.isDisabled());
            invoke(main,"resetCourse");assertTrue(reset.isDisabled());
            CourseMapView map=(CourseMapView)field(main,"map");
            map.setWalkingRouter(stops -> { requested.countDown();return new com.datecourse.model.TravelRoute(java.util.List.of(),100,60); });
            var p=new com.datecourse.model.Place(1,"a","식당","MEAL",35.158,129.059,1000,0);
            var plan=new com.datecourse.model.RoutePlan(35.1579,129.059,2,1000,"저장 코스",java.util.List.of(p));
            callWith(main,"restoreSavedCourse",com.datecourse.model.SavedCourse.class,new com.datecourse.model.SavedCourse(1,7,plan));
            assertFalse(reset.isDisabled());return null;
        });
        assertTrue(requested.await(10,TimeUnit.SECONDS));
    }

    @Test
    void mapAndSearchCannotMoveLockedOriginAndResetUnlocksIt() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
            window.blockedOrigins=0; javaMapBridge.onOriginChangeBlocked=function(){blockedOrigins++;};
            window.selectMapLocation(35.1579,129.059,'기존 출발지');
            window.originCount=bridgeSelections.length;
            window.setOriginLocked(true);
            testMap.onclick({latLng:new kakao.maps.LatLng(35.16,129.06)});
            window.submitQuery('부산역');
            requests[0].callback([{place_name:'부산역',y:'35.115',x:'129.04',address_name:'부산 동구'}],kakao.maps.services.Status.OK);
            document.querySelector('#search-results button').click();
            """);
        assertEquals(true, script(engine, "blockedOrigins===2 && bridgeSelections.length===originCount"));
        assertEquals("기존 출발지", script(engine, "document.getElementById('map-status').textContent"));
        script(engine, "window.setOriginLocked(false);testMap.onclick({latLng:new kakao.maps.LatLng(35.16,129.06)})");
        assertEquals(true, script(engine, "bridgeSelections.length===originCount+1"));

        fx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));
            Parent root = loader.load(); new Scene(root, 1440, 960);
            CoursePlannerController main = loader.getController();
            CourseEditorController editor = (CourseEditorController) field(main, "routeEditorController");
            stubPlannerMap(main, "mapView"); stubPlannerMap(editor, "editMapView");
            var place = new com.datecourse.model.Place(1,"a","식당","MEAL",35.158,129.059,1000,0);
            var plan = new com.datecourse.model.RoutePlan(35.1579,129.059,2,1000,"",java.util.List.of());
            callWith(main,"presentPlan",com.datecourse.model.RoutePlan.class,plan);
            setField(main,"placesLoaded",true);setField(main,"allPlaces",java.util.List.of(place));
            invoke(main,"onRecommendClick");invoke(editor,"onRecommendNext");invoke(editor,"onAddCandidate");invoke(main,"onHome");
            CourseMapView map = (CourseMapView) field(main,"map");
            MapEventBridge bridge = (MapEventBridge) field(map,"bridge");
            assertEquals(true,field(map,"originLocked"));
            bridge.onLocationSelected(35.16,129.06);
            assertEquals(35.1579,field(main,"selectedLatitude"));
            assertEquals(129.059,field(main,"selectedLongitude"));
            assertTrue(((javafx.scene.control.Label)field(field(main,"feedback"),"message")).getText().contains("초기화"));
            invoke(main,"resetCourse");
            assertEquals(false,field(map,"originLocked"));
            bridge.onLocationSelected(35.16,129.06);
            assertEquals(35.16,field(main,"selectedLatitude"));
            return null;
        });
    }

    @Test
    void routeHoverShowsSummaryAndClicksOpenRouteOrPlaceCards() throws Exception {
        WebEngine engine = loadMap(640,600);
        script(engine,"""
            window.presentRoute({startLat:35.1579,startLng:129.059,radius:1000,people:2,places:[
              {recordId:1,placeId:'a',name:'첫 장소',category:'MEAL',lat:35.158,lng:129.059},
              {recordId:2,placeId:'b',name:'두 번째 장소',category:'CAFE',lat:35.1582,lng:129.059}
            ]},null,500,false,false,[]);
            window.pathOne=[[{lat:35.1579,lng:129.059},{lat:35.158,lng:129.059}]];
            window.showWalkingRoute({totalDistance:200,totalTime:160,legs:[
              {from:{name:'출발'},to:{name:'첫 장소'},distance:100,time:80,paths:pathOne,steps:[{description:'첫 구간 안내'}]},
              {from:{name:'첫 장소'},to:{name:'긴 이름이 있는 두 번째 장소'},distance:100,time:80,paths:pathOne,steps:[{description:'둘째 구간 안내'}]}
            ]},'',false);
            routeLines[3].onclick();
            """);
        assertEquals(false,script(engine,"document.getElementById('walking-details').hidden"));
        assertEquals(true,script(engine,"document.getElementById('walking-summary').textContent.startsWith('2구간')"));
        script(engine,"pins.filter(p=>p.coursePin && p.attached)[0].onclick()");
        assertEquals(true,script(engine,"document.getElementById('walking-details').hidden"));
        assertEquals(true,script(engine,"document.getElementById('walking-summary').textContent.startsWith('2구간')"));
        assertEquals("첫 장소",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(false,script(engine,"document.getElementById('place-detail').hidden"));
        assertEquals(1,((Number)script(engine,"document.querySelectorAll('#walking-directions > li:not([hidden])').length")).intValue());
        script(engine,"""
            pins.filter(p=>p.coursePin && p.attached)[1].onclick();
            routeLines[3].onmouseover({latLng:new kakao.maps.LatLng(35.16,129.062)});
            window.tooltip=routeOverlays[0].options.content;
            document.getElementById('map-surface').appendChild(tooltip);
            tooltip.style.position='absolute';tooltip.style.right='16px';tooltip.style.top='180px';
            """);
        assertEquals("두 번째 장소",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(true,script(engine,"document.getElementById('walking-summary').textContent.startsWith('2구간')"));
        assertEquals(260,((Number)script(engine,"tooltip.getBoundingClientRect().width")).intValue());
        assertEquals(true,script(engine,"tooltip.getBoundingClientRect().height<180 && tooltip.querySelector('.walking-tooltip-facts').textContent.includes('분')"));
        assertEquals(true,script(engine,"!tooltip.querySelector('.walking-tooltip-steps') && getComputedStyle(tooltip).pointerEvents==='none'"));
        // Show the test scene briefly: WebKit may skip painting in a detached Scene.
        var previewWindow = fx(() -> {
            var window = new javafx.stage.Stage();window.setScene(previewView.getScene());window.show();return window;
        });
        try {
            Thread.sleep(150);
            fx(() -> {
                Path preview=Path.of("build/ui-preview/walking-leg-tooltip.png");Files.createDirectories(preview.getParent());
                ImageIO.write(SwingFXUtils.fromFXImage(previewView.snapshot(null,null),null),"png",preview.toFile());
                return null;
            });
        } finally { fx(() -> { previewWindow.close();return null; }); }
    }

    @Test
    void noticeCountsDownFadesAndNewNoticeRestartsItsLifetime() throws Exception {
        CountDownLatch hidden = new CountDownLatch(1);
        fx(() -> {
            StackPane root = new StackPane();
            new Scene(root, 960, 680);
            PageFeedback feedback = new PageFeedback(root);
            feedback.show(PageFeedback.Kind.INFO, "저장했어요.");
            var timer = (javafx.animation.Timeline) field(feedback, "countdown");
            var fade = (javafx.animation.FadeTransition) field(feedback, "fade");
            var toast = (javafx.scene.layout.VBox) field(feedback, "toast");
            var bar = (javafx.scene.control.ProgressBar) field(feedback, "remaining");
            timer.jumpTo(javafx.util.Duration.seconds(2.5));
            assertEquals(0.5, bar.getProgress(), 0.05);
            timer.stop();
            timer.getOnFinished().handle(new javafx.event.ActionEvent());
            fade.jumpTo(javafx.util.Duration.millis(125));
            assertTrue(toast.getOpacity() < 1);
            feedback.show(PageFeedback.Kind.ERROR, "다시 시도해 주세요.");
            assertEquals(1, toast.getOpacity());
            assertEquals(1, bar.getProgress());
            feedback.show(PageFeedback.Kind.ERROR, "요청 실패. 연결을 확인한 후 다시 시도해 주세요.");
            var copy = (javafx.scene.control.Label) field(feedback, "message");
            assertEquals("요청 실패. 연결을 확인한 후 다시 시도해 주세요.", copy.getText());
            assertTrue(copy.getTooltip().getText().contains("연결을 확인"));
            ((javafx.scene.control.Button) toast.lookup(".feedback-close")).fire();
            assertFalse(toast.isVisible());
            assertEquals(javafx.animation.Animation.Status.STOPPED, timer.getStatus());
            feedback.show(PageFeedback.Kind.ERROR, "오류 알림도 자동으로 닫혀요.");
            toast.visibleProperty().addListener((o, old, visible) -> { if (!visible) hidden.countDown(); });
            timer.setRate(100);
            fade.setRate(100);
            return null;
        });
        assertTrue(hidden.await(5, TimeUnit.SECONDS));
    }

    @Test
    void overlappingWalkingLegsCanBeSelectedHoveredAndRemovedWithoutResizingMap() throws Exception {
        WebEngine engine = loadMap(640, 600);
        script(engine, """
            window.surfaceBefore = document.getElementById('map-surface').clientHeight;
            window.sharedPath = [[{lat:35.1796,lng:129.0756},{lat:35.1801,lng:129.076}]];
            window.showWalkingRoute({totalDistance:200,totalTime:160,legs:[
              {from:{name:'출발'},to:{name:'첫 장소'},distance:100,time:80,paths:sharedPath,steps:[]},
              {from:{name:'첫 장소'},to:{name:'<img src=x>'},distance:100,time:80,paths:sharedPath,steps:[]}
            ]},'',false);
            """);
        assertEquals(true, script(engine, "routeLines[1].options.strokeColor!==routeLines[3].options.strokeColor"));
        assertEquals(2, ((Number) script(engine, "document.querySelectorAll('.walking-leg-button').length")).intValue());
        script(engine, "document.querySelectorAll('.walking-leg-button')[1].click()");
        assertEquals(true, script(engine, "routeLines[3].zIndex===12 && routeLines[1].zIndex===4"));
        assertEquals("true", script(engine, "document.querySelectorAll('.walking-leg-button')[1].getAttribute('aria-pressed')"));
        script(engine, "routeLines[3].onmouseover({latLng:new kakao.maps.LatLng(35.1801,129.076)})");
        assertEquals(true, script(engine, "routeOverlays[0].attached && routeOverlays[0].options.content.textContent.includes('2구간')"));
        assertEquals(0, ((Number) script(engine, "routeOverlays[0].options.content.querySelectorAll('img').length")).intValue());
        assertEquals(true, script(engine, "document.getElementById('map-surface').clientHeight===surfaceBefore"));
        script(engine, "routeLines[3].onmouseout(); window.showWalkingRoute(null,'',false)");
        assertEquals(true, script(engine, "routeOverlays.every(o=>!o.attached) && routeLines.every(l=>!l.attached && !l.onmouseover)"));
        assertEquals(0, ((Number) script(engine, "document.querySelectorAll('.walking-leg-button').length")).intValue());
    }

    @Test
    void navigationPreservesCandidateAndConditionsAndOnlyExplicitResetClearsDraft() throws Exception {
        fx(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));
            Parent root = loader.load(); new Scene(root, 1440, 960);
            CoursePlannerController main = loader.getController();
            CourseEditorController editor = (CourseEditorController) field(main, "routeEditorController");
            stubPlannerMap(main, "mapView"); stubPlannerMap(editor, "editMapView");
            var a = new com.datecourse.model.Place(1, "a", "식당 A", "MEAL", 35.158, 129.059, 1000, 0);
            var b = new com.datecourse.model.Place(2, "b", "식당 B", "MEAL", 35.1582, 129.059, 1000, 0);
            var plan = new com.datecourse.model.RoutePlan(35.1579, 129.059, 2, 1000, "초안", java.util.List.of());
            callWith(main, "presentPlan", com.datecourse.model.RoutePlan.class, plan);
            setField(main, "placesLoaded", true); setField(main, "allPlaces", java.util.List.of(a,b));
            invoke(main, "onRecommendClick");
            invoke(editor, "onRecommendNext"); invoke(editor, "onAddCandidate");
            invoke(editor, "onRecommendNext");
            var candidates = (javafx.scene.control.ListView<?>) field(editor, "candidateList");
            candidates.getSelectionModel().selectFirst();
            Object choice = candidates.getSelectionModel().getSelectedItem();
            invoke(main, "onHome");
            assertEquals(true, field(main, "editorPaused"));
            assertEquals(plan, field(main, "appliedPlan"));
            assertFalse(((javafx.scene.layout.HBox) field(main, "headerBar")).isDisabled());
            invoke(main, "onRecommendClick");
            assertEquals(java.util.List.of(a), editor.draftSnapshot().places());
            assertSame(choice, candidates.getSelectionModel().getSelectedItem());
            assertTrue(((javafx.scene.control.TitledPane) field(editor, "choicePane")).isExpanded());
            invoke(editor,"resetDraft");
            assertTrue(editor.draftSnapshot().places().isEmpty());
            assertTrue(editor.draftSnapshot().arrivalRadii().isEmpty());
            assertTrue(candidates.getItems().isEmpty());
            assertEquals(plan, field(main, "appliedPlan"));
            ((javafx.animation.Timeline) field(main, "screenAnimation")).stop();
            return null;
        });
    }

    @Test
    void startupWaitsForFirstPlaceReadAndCompletesOnSuccessOrFailure() throws Exception {
        for (boolean failRead : new boolean[]{false, true}) {
            CountDownLatch release = new CountDownLatch(1), ready = new CountDownLatch(1);
            var source = new com.datecourse.db.PlaceRepository(null) {
                @Override public java.util.List<com.datecourse.model.Place> findAll() throws java.sql.SQLException {
                    try { release.await(10, TimeUnit.SECONDS); }
                    catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new java.sql.SQLException(failure); }
                    if (failRead) throw new java.sql.SQLException("테스트용 실패");
                    return java.util.List.of();
                }
            };
            var main = fx(() -> {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));
                Parent root = loader.load();
                new Scene(root, 960, 680);
                CoursePlannerController controller = loader.getController();
                stubPlannerMap(controller, "mapView");
                stubPlannerMap(field(controller, "routeEditorController"), "editMapView");
                controller.setPlaceRepository(source);
                controller.prepareStartup(ready::countDown);
                assertNotNull(field(controller, "savedPage"));
                assertTrue(controller.isLoading());
                assertEquals(1, ready.getCount());
                return controller;
            });
            try {
                release.countDown();
                assertTrue(ready.await(10, TimeUnit.SECONDS));
                fx(() -> { assertFalse(main.isLoading()); assertEquals(!failRead, field(main, "placesLoaded")); return null; });
            } finally { release.countDown(); }
        }
    }

    @Test
    void commonLoadingKeepsOverlayUntilAllWorkEndsAndShowsSharedNotice() throws Exception {
        CountDownLatch releaseFirst = new CountDownLatch(1), releaseSecond = new CountDownLatch(1);
        CountDownLatch firstDone = new CountDownLatch(1), secondDone = new CountDownLatch(1);
        Object[] fixture = fx(() -> {
            StackPane root = new StackPane();
            new Scene(root, 960, 680);
            root.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            root.resize(960, 680);
            PageFeedback feedback = new PageFeedback(root);
            var page = new BasePageController() {
                <T> void startWork(String label, java.util.concurrent.Callable<T> work,
                                   java.util.function.Consumer<T> success, java.util.function.Consumer<Throwable> failure) {
                    runWork(label,work,success,failure);
                }
                void showFailure() { notifyUser(PageFeedback.Kind.ERROR,"작업에 실패했어요. 다시 시도해 주세요."); }
            };
            page.useFeedback(feedback);
            page.startWork("첫 작업", () -> { releaseFirst.await(); return true; }, ignored -> firstDone.countDown(), error -> fail(error));
            page.startWork("두 번째 작업", () -> { releaseSecond.await(); throw new java.io.IOException("확인용 실패"); },
                    ignored -> fail("실패해야 함"), error -> { page.showFailure(); secondDone.countDown(); });
            assertTrue(page.loadingProperty().get());
            return new Object[]{root, page};
        });
        try {
            releaseFirst.countDown();
            assertTrue(firstDone.await(10, TimeUnit.SECONDS));
            fx(() -> { assertTrue(((BasePageController) fixture[1]).isLoading()); return null; });
            releaseSecond.countDown();
            assertTrue(secondDone.await(10, TimeUnit.SECONDS));
            fx(() -> {
                StackPane root = (StackPane) fixture[0];
                assertFalse(((BasePageController) fixture[1]).isLoading());
                root.applyCss(); root.layout();
                assertFalse(root.lookup(".loading-shade").isVisible());
                assertTrue(root.lookup(".feedback-toast").isVisible());
                assertEquals(960, root.getWidth()); assertEquals(680, root.getHeight());
                Path preview = Path.of("build/ui-preview/shared-notice.png");
                Files.createDirectories(preview.getParent());
                ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null, null), null), "png", preview.toFile());
                return null;
            });
        } finally { releaseFirst.countDown(); releaseSecond.countDown(); }
    }

    @Test
    void savesLoadsAndUpdatesSameCourseThroughRealFxmlControllers() throws Exception {
        var config = new com.zaxxer.hikari.HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:" + java.util.UUID.randomUUID() + ";MODE=MySQL");
        config.setUsername("sa");
        config.setMaximumPoolSize(2);
        try (var pool = new com.zaxxer.hikari.HikariDataSource(config)) {
            try (var connection = pool.getConnection()) {
                for (String sql : java.util.List.of("02_create_tables.sql", "07_saved_courses.sql", "08_saved_course_price_estimates.sql")) {
                    try (var reader = Files.newBufferedReader(Path.of("data/sql", sql))) {
                        org.h2.tools.RunScript.execute(connection, reader);
                    }
                }
                connection.createStatement().execute("INSERT INTO members(member_id,email,password_hash,name) VALUES(7,'flow@test','test','Flow')");
            }
        com.datecourse.maintenance.DatabaseSetup.prepareReports(pool);
        com.datecourse.maintenance.DatabaseSetup.prepareCatalog(pool);
        com.datecourse.maintenance.DatabaseSetup.preparePreviews(pool);
        com.datecourse.maintenance.DatabaseSetup.prepareTransport(pool);
            var repository = new com.datecourse.db.SavedCourseRepository(pool);
            var session = new com.datecourse.model.UserSession(7, "Flow");
            var place = new com.datecourse.model.Place(11, "test-one", "미확인 식당", "MEAL", 35.158, 129.059,
                    com.datecourse.model.PriceEstimate.unknown(), 0, com.datecourse.model.PlaceDetails.EMPTY);
            var plan = new com.datecourse.model.RoutePlan(35.1579, 129.059, 2, 1000, "흐름 검증", java.util.List.of(place));
            var main = fx(() -> {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));
                Parent root = loader.load();
                new Scene(root, 1440, 960);
                CoursePlannerController controller = loader.getController();
                stubPlannerMap(controller, "mapView");
                controller.setUserSession(session);
                controller.setSavedCourseRepository(repository);
                callWith(controller, "presentPlan", com.datecourse.model.RoutePlan.class, plan);
                return controller;
            });
            fx(() -> {
                setField(main, "titlePrompt", (java.util.function.Function<com.datecourse.model.RoutePlan, java.util.Optional<String>>) p -> java.util.Optional.empty());
                invoke(main, "onSaveCourse");
                return null;
            });
            assertTrue(repository.findAll(session).isEmpty());
            fx(() -> {
                setField(main, "titlePrompt", (java.util.function.Function<com.datecourse.model.RoutePlan, java.util.Optional<String>>) p -> java.util.Optional.of(" "));
                invoke(main, "onSaveCourse");
                assertTrue(((javafx.scene.control.Label) field(main, "saveStatusLabel")).getText().contains("1~20자"));
                return null;
            });
            assertTrue(repository.findAll(session).isEmpty());
            saveAndAwait(main);
            long id = repository.findAll(session).get(0).courseId();
            assertEquals(plan, repository.findAll(session).get(0).plan());
            CountDownLatch listed = new CountDownLatch(1);
            var list = fx(() -> {
                invoke(main, "prepareSavedPage");
                Object page = field(main, "savedPage");
                invoke(main, "prepareSavedPage");
                assertSame(page, field(main, "savedPage"));
                SavedCourseController controller = (SavedCourseController) field(main, "savedController");
                ((javafx.scene.control.Label) field(controller, "messageLabel")).textProperty()
                        .addListener((o, old, text) -> { if (text.isEmpty()) listed.countDown(); });
                invoke(main, "onSavedCourses");
                assertTrue(((StackPane) field(main, "savedPageHost")).isVisible());
                return controller;
            });
            assertTrue(listed.await(10, TimeUnit.SECONDS));
            fx(() -> {
                invoke(list, "onOpenClick");
                assertFalse(((StackPane) field(main, "savedPageHost")).isVisible());
                assertEquals(id, field(main, "savedCourseId"));
                assertEquals(plan, field(main, "appliedPlan"));
                var summary=(javafx.scene.control.TableView<?>)field(list,"summaryTable");
                assertTrue(summary.getItems().stream().anyMatch(row->row.toString().contains("미확인")));
                assertTrue(summary.getItems().stream().anyMatch(row->row.toString().contains("최근 수정")));
                assertEquals(plan.places().size(),((javafx.scene.control.TableView<?>)field(list,"placeTable")).getItems().size());
                ((TextField) field(main, "peopleField")).setText("3");
                return null;
            });
            saveAndAwait(main);
            assertEquals(1, repository.findAll(session).size());
            assertEquals(id, repository.findAll(session).get(0).courseId());
            assertEquals(3, repository.findAll(session).get(0).plan().people());
            fx(() -> {
                invoke(main, "onNewCourse");
                assertNull(field(main, "savedCourseId"));
                callWith(main, "presentPlan", com.datecourse.model.RoutePlan.class, plan);
                return null;
            });
            saveAndAwait(main);
            assertEquals(2, repository.findAll(session).size());
            CountDownLatch failed = new CountDownLatch(1);
            fx(() -> {
                ((javafx.scene.control.Label) field(main, "saveStatusLabel")).textProperty()
                        .addListener((o, old, text) -> { if (text.startsWith("코스를 저장하지 못했어요")) failed.countDown(); });
                pool.close();
                invoke(main, "onSaveCourse");
                return null;
            });
            assertTrue(failed.await(10, TimeUnit.SECONDS));
            fx(() -> { assertEquals(plan, field(main, "appliedPlan")); assertEquals(false, field(main, "saving")); return null; });
        }
    }

    @Test void compactFiltersAndDisplayLimitPreserveSelectionAndBlockStaleAdds() throws Exception {
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));
            Parent root=loader.load();CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            var rows=new com.datecourse.service.PublicDemoPlaceRepository().findAll();
            editor.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,2000,"",java.util.List.of()),rows,p->{},()->{});
            ((javafx.scene.control.Slider)field(editor,"radiusSlider")).setValue(2000);
            new Scene(root,1280,800);root.resize(1280,800);root.applyCss();root.layout();
            invoke(editor,"onRecommendNext");
            var list=(javafx.scene.control.ListView<?>)field(editor,"candidateList");assertEquals(5,list.getItems().size());
            list.getSelectionModel().select(2);Object selected=list.getSelectionModel().getSelectedItem();
            var limit=(javafx.scene.control.ComboBox<Integer>)field(editor,"candidateLimitBox");limit.setValue(30);
            assertEquals(30,list.getItems().size());assertEquals(selected,list.getSelectionModel().getSelectedItem());
            TextField price=(TextField)field(editor,"placePriceField");price.setText("1");
            assertEquals(30,list.getItems().size());invoke(editor,"onAddCandidate");
            assertTrue(((javafx.scene.control.ListView<?>)field(editor,"placeList")).getItems().isEmpty());
            var subtype=(javafx.scene.control.ComboBox<String>)field(editor,"subtypeBox");subtype.setValue("일식");
            invoke(editor,"onRecommendNext");assertFalse(list.getItems().isEmpty());
            assertTrue(list.getItems().stream().allMatch(v->com.datecourse.service.PlaceFacets.subtype(((com.datecourse.service.PlaceSearchService.NearbyPlace)v).place()).equals("일식")));
            editor.resume(new com.datecourse.model.RoutePlan(35.1578,129.06,2,2000,"",java.util.List.of()),rows);
            assertEquals("일식",subtype.getValue());
            root.applyCss();root.layout();
            Path preview=Path.of("build","ui-preview","compact-public-candidates.png");Files.createDirectories(preview.getParent());
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",preview.toFile());
            return null;
        });
    }

    @Test void staleBackgroundSearchCannotRestoreCandidatesAfterReset() throws Exception {
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);var finished=new CountDownLatch(1);
        CourseEditorController editor=fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();
            CourseEditorController e=loader.getController();stubPlannerMap(e,"editMapView");
            var p=new com.datecourse.model.Place(1,"p","식당","MEAL",35.1578,129.06,5000,0);
            e.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,2000,"",java.util.List.of()),java.util.List.of(p),v->{},()->{});
            e.setPlaceRefresh(rows->{started.countDown();release.await(5,TimeUnit.SECONDS);return rows;});
            e.loadingProperty().addListener((o,a,b)->{if(!b)finished.countDown();});invoke(e,"onRecommendNext");return e;
        });
        assertTrue(started.await(5,TimeUnit.SECONDS));
        fx(()->{invoke(editor,"resetDraft");return null;});release.countDown();assertTrue(finished.await(5,TimeUnit.SECONDS));
        fx(()->{assertTrue(((javafx.scene.control.ListView<?>)field(editor,"candidateList")).getItems().isEmpty());return null;});
    }

    @Test void reviewScreenLoadsWithoutGrantingAnonymousManagementActions() throws Exception {
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/review/information-review-view.fxml"));
            Parent root=loader.load();new Scene(root,1020,690);root.resize(1020,690);root.applyCss();root.layout();
            assertTrue(((javafx.scene.control.Button)root.lookup("#approveButton")).isDisabled());
            assertTrue(((javafx.scene.control.Button)root.lookup("#rejectButton")).isDisabled());
            assertThrows(NullPointerException.class,()->((InformationReviewController)loader.getController()).configure(null));return null;
        });
    }

    @Test void fractionalWonCapsDistinguishPublicPriceGroupsAndKeepExactWonAmounts() throws Exception {
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();
            CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            var rows=new com.datecourse.service.PublicDemoPlaceRepository().findAll();
            editor.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,2000,"",java.util.List.of()),rows,p->{},()->{});
            ((javafx.scene.control.Slider)field(editor,"radiusSlider")).setValue(2000);
            var price=(TextField)field(editor,"placePriceField");
            var subtype=(javafx.scene.control.ComboBox<String>)field(editor,"subtypeBox");subtype.setValue("일식");
            price.setText("0.7");invoke(editor,"onRecommendNext");
            var list=(javafx.scene.control.ListView<?>)field(editor,"candidateList");assertTrue(list.getItems().isEmpty());
            price.setText("0.82");assertEquals(8200,NumberStepper.readWon(price));invoke(editor,"onRecommendNext");assertFalse(list.getItems().isEmpty());
            ((javafx.scene.control.Button)field(editor,"priceUp")).fire();assertEquals("0.92",price.getText());
            ((javafx.scene.control.Button)field(editor,"priceDown")).fire();assertEquals("0.82",price.getText());
            price.setText("0.0001");assertEquals(1,NumberStepper.readWon(price));
            for(String invalid:java.util.List.of("0.00001","NaN","1e2","-0.1","10000.01","999999999999999999999999")) {
                price.setText(invalid);assertThrows(IllegalArgumentException.class,()->NumberStepper.readWon(price));
            }
            return null;
        });
    }

    @Test void reportFormRetainsInvalidInputAndReturnsOnlyAnExplicitValidSubmission() throws Exception {
        fx(()->{
            var place=new com.datecourse.model.Place(1,"p","제보할 식당","MEAL",35,129,7000,0);
            var dialog=InformationReportDialog.create(null,place);
            try {
                dialog.show();var pane=dialog.getDialogPane();
                var value=(TextField)pane.lookup("#reportValue");
                var reason=(javafx.scene.control.TextArea)pane.lookup("#reportReason");
                var source=(TextField)pane.lookup("#reportSource");
                var send=(javafx.scene.control.Button)pane.lookupButton(pane.getButtonTypes().stream()
                    .filter(b->b.getButtonData()==javafx.scene.control.ButtonBar.ButtonData.OK_DONE).findFirst().orElseThrow());
                value.setText("-1");reason.setText("메뉴판의 1인 기본 가격 확인");send.fire();
                assertTrue(dialog.isShowing());assertEquals("-1",value.getText());assertNull(dialog.getResult());
                value.setText("9500");source.setText("javascript:alert(1)");send.fire();
                assertTrue(dialog.isShowing());assertTrue(((javafx.scene.control.Label)pane.lookup("#reportError")).getText().contains("https"));
                source.setText("https://example.test/menu");
                answerReportConfirmation(javafx.scene.control.ButtonBar.ButtonData.CANCEL_CLOSE);send.fire();
                assertTrue(dialog.isShowing());assertNull(dialog.getResult());assertEquals("9500",value.getText());
                answerReportConfirmation(javafx.scene.control.ButtonBar.ButtonData.OK_DONE);send.fire();
                assertFalse(dialog.isShowing());assertNotNull(dialog.getResult());assertEquals("9500",dialog.getResult().value());
                assertEquals(com.datecourse.model.InformationReport.Field.PRICE,dialog.getResult().field());
            } finally {dialog.close();}
            var cancelled=InformationReportDialog.create(null,place);
            try{cancelled.show();((javafx.scene.control.Button)cancelled.getDialogPane().lookupButton(javafx.scene.control.ButtonType.CANCEL)).fire();
                assertFalse(cancelled.isShowing());assertNull(cancelled.getResult());}finally{cancelled.close();}
            return null;
        });
    }

    private static void answerReportConfirmation(javafx.scene.control.ButtonBar.ButtonData answer) {
        Platform.runLater(()->{
            var window=javafx.stage.Window.getWindows().stream()
                .filter(w->w instanceof javafx.stage.Stage stage && "제보 확인".equals(stage.getTitle()))
                .findFirst().orElseThrow();
            var pane=(javafx.scene.control.DialogPane)window.getScene().getRoot();
            var type=pane.getButtonTypes().stream().filter(b->b.getButtonData()==answer).findFirst().orElseThrow();
            ((javafx.scene.control.Button)pane.lookupButton(type)).fire();
        });
    }

    @Test void approvedNewSubtypeAppearsInFacetsAfterBackgroundRefresh() throws Exception {
        var finished=new CountDownLatch(1);
        CourseEditorController editor=fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();
            CourseEditorController e=loader.getController();stubPlannerMap(e,"editMapView");
            var old=new com.datecourse.model.Place(1,"p","식당","MEAL",35.1578,129.06,5000,0);
            var updated=new com.datecourse.model.Place(1,"p","식당","MEAL",35.1578,129.06,old.priceEstimate(),0,
                new com.datecourse.model.PlaceDetails("","중국집","","","source","","","",java.util.Map.of()));
            e.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,2000,"",java.util.List.of()),java.util.List.of(old),p->{},()->{});
            e.setPlaceRefresh(rows->java.util.List.of(updated));
            e.loadingProperty().addListener((o,a,b)->{if(!b)finished.countDown();});invoke(e,"onRecommendNext");return e;
        });
        assertTrue(finished.await(5,TimeUnit.SECONDS));
        fx(()->{
            var subtype=(javafx.scene.control.ComboBox<String>)field(editor,"subtypeBox");assertTrue(subtype.getItems().contains("중식"));
            assertNull(subtype.getValue());
            var list=(javafx.scene.control.ListView<?>)field(editor,"candidateList");
            assertEquals("중식",com.datecourse.service.PlaceFacets.subtype(((com.datecourse.service.PlaceSearchService.NearbyPlace)list.getItems().getFirst()).place()));
            return null;
        });
    }

    @Test void addingPlacesAutomaticallyShowsPerLegAndCumulativeWalkingTime() throws Exception {
        var calls=new java.util.concurrent.atomic.AtomicInteger();
        CourseEditorController editor=fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();
            CourseEditorController e=loader.getController();stubPlannerMap(e,"editMapView");
            ((CourseMapView)field(e,"map")).setWalkingRouter(stops->{
                calls.incrementAndGet();var legs=new java.util.ArrayList<com.datecourse.model.TravelRoute.Leg>();
                for(int i=1;i<stops.size();i++){
                    assertFalse(stops.get(i).name().contains("후보"));
                    legs.add(new com.datecourse.model.TravelRoute.Leg(stops.get(i-1),stops.get(i),100,61,java.util.List.of(),java.util.List.of()));
                }
                return new com.datecourse.model.TravelRoute(legs,legs.size()*100L,legs.size()*61L);
            });
            var a=new com.datecourse.model.Place(1,"a","식당 A","MEAL",35.1579,129.06,7000,0);
            var b=new com.datecourse.model.Place(2,"b","식당 B","MEAL",35.158,129.06,7000,0);
            e.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,2000,"",java.util.List.of()),java.util.List.of(a,b),p->{},()->{});
            invoke(e,"onRecommendNext");assertEquals(0,calls.get());invoke(e,"onAddCandidate");return e;
        });
        awaitEditorTime(editor,"도보 · 약 2분");assertEquals(1,calls.get());
        fx(()->{invoke(editor,"onRecommendNext");assertEquals(1,calls.get());invoke(editor,"onAddCandidate");return null;});
        awaitEditorTime(editor,"도보 · 약 3분");assertEquals(2,calls.get());
        fx(()->{
            var description=CourseEditorController.class.getDeclaredMethod("walkingDescription",int.class);description.setAccessible(true);
            assertTrue(description.invoke(editor,0).toString().contains("도보 약 2분 · 누적 약 2분"));
            assertTrue(description.invoke(editor,1).toString().contains("도보 약 2분 · 누적 약 3분"));
            ((CourseMapView)field(editor,"map")).setWalkingRouter(stops->{throw new java.io.IOException("시험 조회 실패");});
            var list=(javafx.scene.control.ListView<?>)field(editor,"placeList");list.getSelectionModel().selectLast();invoke(editor,"onRemove");return null;
        });
        awaitEditorTime(editor,"미확인");
        fx(()->{assertNull(field(editor,"walkingRoute"));invoke(editor,"resetDraft");
            assertTrue(((javafx.scene.control.Label)field(editor,"walkingTimeLabel")).getText().contains("장소를 추가"));return null;});
    }
    @Test void changingTransportRejectsLateOldModeAndKeepsDraftMode() throws Exception {
        var drivingStarted=new CountDownLatch(1);var releaseDriving=new CountDownLatch(1);
        var requested=new java.util.concurrent.CopyOnWriteArrayList<com.datecourse.model.TravelMode>();
        CourseEditorController editor=fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();
            CourseEditorController e=loader.getController();stubPlannerMap(e,"editMapView");
            ((CourseMapView)field(e,"map")).setRouteRouter((stops,mode)->{
                requested.add(mode);
                if(mode==com.datecourse.model.TravelMode.DRIVING){
                    drivingStarted.countDown();
                    try{releaseDriving.await(5,TimeUnit.SECONDS);}catch(InterruptedException ignored){}
                }
                var leg=new com.datecourse.model.TravelRoute.Leg(stops.get(0),stops.get(1),100,60,java.util.List.of(),java.util.List.of());
                return new com.datecourse.model.TravelRoute(java.util.List.of(leg),100,60,mode);
            });
            var place=new com.datecourse.model.Place(1,"a","장소","MEAL",35.1579,129.06,7000,0);
            e.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,1000,"",java.util.List.of(place)),java.util.List.of(place),p->{},()->{});
            return e;
        });
        try {
            awaitEditorTime(editor,"도보 · 약 1분");
            fx(()->{((javafx.scene.control.ComboBox<com.datecourse.model.TravelMode>)field(editor,"travelModeBox")).setValue(com.datecourse.model.TravelMode.DRIVING);return null;});
            assertTrue(drivingStarted.await(5,TimeUnit.SECONDS));
            fx(()->{((javafx.scene.control.ComboBox<com.datecourse.model.TravelMode>)field(editor,"travelModeBox")).setValue(com.datecourse.model.TravelMode.TRANSIT);return null;});
            awaitEditorTime(editor,"대중교통 + 도보 · 약 1분");releaseDriving.countDown();
            fx(()->{
                assertEquals(com.datecourse.model.TravelMode.TRANSIT,editor.draftSnapshot().travelMode());
                assertEquals(com.datecourse.model.TravelMode.TRANSIT,((com.datecourse.model.TravelRoute)field(editor,"walkingRoute")).mode());
                assertFalse(((javafx.scene.control.Button)field(editor,"walkingSettingsButton")).isVisible());
                editor.dispose();return null;
            });
            assertTrue(requested.containsAll(java.util.List.of(com.datecourse.model.TravelMode.WALKING,com.datecourse.model.TravelMode.DRIVING,com.datecourse.model.TravelMode.TRANSIT)));
        }finally {releaseDriving.countDown();}
    }

    private static void awaitEditorTime(CourseEditorController editor,String text) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<end) {
            if(fx(()->(((javafx.scene.control.Label)field(editor,"metricTransportLabel")).getText()+" · "+((javafx.scene.control.Label)field(editor,"metricTimeLabel")).getText()+" · "+((javafx.scene.control.Label)field(editor,"walkingTimeLabel")).getText()).contains(text)))return;
            Thread.sleep(20);
        }
        fail("Missing editor walking status: "+text);
    }

    @Test void conciseDetailCardOwnsTheReportActionAndOmitsSourceFields() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.reports=[];javaMapBridge.reportPlace=function(id){reports.push(id);};
            window.shortPlace={recordId:77,placeId:'short',name:'간략 카드',category:'MEAL',lat:35.158,lng:129.06,
                priceText:'약 7,000원/인',priceEstimate:{evidence:'SIMILAR_ESTIMATE',briefBasis:'부산 착한가격업소',
                basis:'부산 착한가격업소 한식 고유장소 115곳 자세한 계산',sourceUrl:'https://example.test/source'},
                details:{address:'부산 주소',identityCheckedAt:'2026-11-14',sourceUrl:'https://example.test/public'}};
            window.presentRoute({startLat:35.157,startLng:129.06,radius:1000,places:[]},shortPlace,500,true,true,[shortPlace]);
            window.openRoutePlace(77);document.getElementById('detail-report').click();
            """);
        assertEquals("[\"77\"]",script(engine,"JSON.stringify(reports)"));
        assertEquals(false,script(engine,"document.getElementById('detail-report').hidden"));
        assertEquals("A",script(engine,"document.getElementById('detail-report').tagName"));
        script(engine,"window.cardAdds=[];javaMapBridge.addCandidate=function(id){cardAdds.push(id);};document.getElementById('detail-add').click()");
        assertEquals("[\"77\"]",script(engine,"JSON.stringify(cardAdds)"));
        script(engine,"window.setCandidateActionsEnabled(false);document.getElementById('detail-add').click()");
        assertEquals(true,script(engine,"document.getElementById('detail-add').disabled"));
        assertEquals("[\"77\"]",script(engine,"JSON.stringify(cardAdds)"));
        script(engine,"window.setCandidateActionsEnabled(true)");
        String text=(String)script(engine,"document.getElementById('place-detail').textContent");
        assertTrue(text.contains("부산 착한가격업소 기준 추정 가격"));
        assertFalse(text.contains("115"));assertFalse(text.contains("자료 출처"));assertFalse(text.contains("https://example.test"));
        assertEquals(false,script(engine,"document.getElementById('detail-facts').textContent.includes('출처') || document.getElementById('detail-facts').textContent.includes('장소 확인')"));
        javafx.stage.Stage preview=fx(()->{var stage=new javafx.stage.Stage();stage.setScene(previewView.getScene());stage.show();return stage;});
        try {
            Thread.sleep(250);
            fx(()->{
                Path file=Path.of("build/ui-preview/detail-card-concise.png");Files.createDirectories(file.getParent());
                ImageIO.write(SwingFXUtils.fromFXImage(previewView.snapshot(null,null),null),"png",file.toFile());return null;
            });
        } finally {fx(()->{preview.close();return null;});}
    }

    @Test void editResetAppliesAnEmptyCourseAndReturnsToCommonConditionsWithoutDialog() throws Exception {
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));Parent root=loader.load();new Scene(root,1440,960);
            CoursePlannerController main=loader.getController();CourseEditorController editor=(CourseEditorController)field(main,"routeEditorController");
            stubPlannerMap(main,"mapView");stubPlannerMap(editor,"editMapView");
            var place=new com.datecourse.model.Place(1,"a","식당","MEAL",35.158,129.059,7000,0);
            var before=new com.datecourse.model.RoutePlan(35.1579,129.059,2,1000,"기존 코스",java.util.List.of(place),java.util.Map.of(1L,500));
            callWith(main,"presentPlan",com.datecourse.model.RoutePlan.class,before);
            setField(main,"placesLoaded",true);setField(main,"allPlaces",java.util.List.of(place));setField(main,"savedCourseId",77L);
            invoke(main,"onRecommendClick");assertEquals(true,field(main,"editorOpen"));invoke(editor,"onResetDraft");
            var after=(com.datecourse.model.RoutePlan)field(main,"appliedPlan");
            assertTrue(after.places().isEmpty());assertTrue(after.arrivalRadii().isEmpty());
            assertEquals(before.startLat(),after.startLat());assertEquals(before.people(),after.people());assertEquals(before.radius(),after.radius());
            assertEquals(false,field(main,"editorOpen"));assertEquals(false,field(main,"editorPaused"));assertEquals(77L,field(main,"savedCourseId"));
            assertEquals(java.util.List.of(place),before.places());
            var animation=(javafx.animation.Timeline)field(main,"screenAnimation");animation.stop();animation.getOnFinished().handle(new javafx.event.ActionEvent());
            assertTrue(root.lookup("#mainPage").isVisible());assertFalse(root.lookup("#routeEditor").isVisible());
            assertFalse(root.lookupAll(".button").stream().filter(n->n instanceof javafx.scene.control.Button)
                .map(n->((javafx.scene.control.Button)n).getText()).anyMatch(t->"도보 경로".equals(t)||"경로 설명".equals(t)));
            return null;
        });
    }

    @Test void clickedRouteInstructionsScrollInLeftCardAndHoverNeverHoldsThem() throws Exception {
        WebEngine engine=loadMap(640,600);
        script(engine,"""
            window.path=[[{lat:35.157,lng:129.06},{lat:35.158,lng:129.06}]];
            window.showWalkingRoute({totalDistance:200,totalTime:120,legs:[
                {from:{name:'출발'},to:{name:'첫 장소'},distance:100,time:60,paths:path,steps:Array.from({length:30},(_,i)=>({description:'첫 구간 안내 '+i+' · 횡단보도를 건넌 후 왼쪽 보행자 도로를 따라 350m 이동하세요.'}))},
                {from:{name:'첫 장소'},to:{name:'둘째 장소'},distance:100,time:60,paths:path,steps:[{description:'둘째 구간 전용 안내'}]}
            ]},'',false);
            routeLines[1].onmouseover();window.popup=routeOverlays[0].options.content;
            routeLines[1].onclick();routeLines[1].onmouseout();popup.dispatchEvent(new Event('mouseenter'));
            """);
        assertEquals(false,script(engine,"routeOverlays[0].attached"));
        assertEquals(30,((Number)script(engine,"document.querySelectorAll('#detail-extra li').length")).intValue());
        assertEquals("none",script(engine,"getComputedStyle(document.querySelector('.detail-actions')).display"));
        var previewWindow=fx(()->{var stage=new javafx.stage.Stage();stage.setScene(previewView.getScene());stage.show();return stage;});
        try {
            Thread.sleep(150);
            fx(()->{var path=Path.of("build/ui-preview/route-left-detail.png");Files.createDirectories(path.getParent());
                ImageIO.write(SwingFXUtils.fromFXImage(previewView.snapshot(null,null),null),"png",path.toFile());return null;});
        } finally {fx(()->{previewWindow.close();return null;});}

        assertEquals(false,script(engine,"document.getElementById('detail-extra').textContent.includes('둘째 구간 전용 안내')"));
        assertEquals(true,script(engine,"document.getElementById('place-detail').getBoundingClientRect().left<30 && document.getElementById('place-detail').getBoundingClientRect().bottom<=window.innerHeight"));
        assertEquals(true,script(engine,"document.getElementById('place-detail').scrollHeight>document.getElementById('place-detail').clientHeight"));
        assertEquals("13px",script(engine,"getComputedStyle(document.getElementById('detail-extra')).fontSize"));
        script(engine,"document.getElementById('place-detail').scrollTop=120");
        assertTrue(((Number)script(engine,"document.getElementById('place-detail').scrollTop")).intValue()>0);

        script(engine,"routeLines[3].onmouseover()");
        assertEquals(true,script(engine,"document.getElementById('detail-extra').textContent.includes('첫 구간 안내') && !popup.querySelector('ol')"));
        script(engine,"routeLines[3].onclick()");
        assertEquals(true,script(engine,"document.getElementById('detail-extra').textContent.includes('둘째 구간 전용 안내') && !document.getElementById('detail-extra').textContent.includes('첫 구간 안내')"));
    }

    @Test void mapSelectionFocusesCandidateAndCardAdditionUsesTheSameValidatedFlow() throws Exception {
        var rootRef=new java.util.concurrent.atomic.AtomicReference<Parent>();
        CourseEditorController editor=fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));Parent root=loader.load();rootRef.set(root);
            CourseEditorController e=loader.getController();stubPlannerMap(e,"editMapView");new Scene(root,1440,960);root.resize(1440,960);
            var rows=new java.util.ArrayList<com.datecourse.model.Place>();
            for(int i=1;i<=30;i++)rows.add(new com.datecourse.model.Place(i,"p"+i,"장소 "+i,"MEAL",35.1578+i*.00001,129.06,5000,0));
            e.begin(new com.datecourse.model.RoutePlan(35.1578,129.06,2,2000,"",java.util.List.of()),rows,p->{},()->{});
            ((javafx.scene.control.ComboBox<Integer>)field(e,"candidateLimitBox")).setValue(30);invoke(e,"onRecommendNext");
            root.applyCss();root.layout();
            ((MapEventBridge)field(field(e,"map"),"bridge")).onPlaceSelected("30");return e;
        });
        fx(()->{
            var list=(javafx.scene.control.ListView<?>)field(editor,"candidateList");assertEquals(29,list.getSelectionModel().getSelectedIndex());
            assertEquals(29,list.getFocusModel().getFocusedIndex());assertEquals(list,rootRef.get().getScene().getFocusOwner());
            ((MapEventBridge)field(field(editor,"map"),"bridge")).addCandidate("30");return null;
        });
        fx(()->{
            assertEquals(java.util.List.of(30L),editor.draftSnapshot().places().stream().map(p->p.recordId()).toList());
            ((MapEventBridge)field(field(editor,"map"),"bridge")).addCandidate("30");return null;
        });
        fx(()->{
            assertEquals(1,editor.draftSnapshot().places().size());invoke(editor,"onRecommendNext");
            ((TextField)field(editor,"placePriceField")).setText("0.1");
            ((MapEventBridge)field(field(editor,"map"),"bridge")).addCandidate("29");return null;
        });
        fx(()->{assertEquals(1,editor.draftSnapshot().places().size());return null;});
    }

    @Test void userMenuTextFitsAndInitialScreenHidesActionsThatNeedACourse() throws Exception {
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));Parent root=loader.load();new Scene(root,1200,850);root.resize(1200,850);
            CoursePlannerController main=loader.getController();stubPlannerMap(main,"mapView");stubPlannerMap(field(main,"routeEditorController"),"editMapView");
            main.setUserSession(new com.datecourse.model.UserSession(7,"테스트 사용자"));
            root.applyCss();root.layout();
            var menu=(javafx.scene.control.MenuButton)field(main,"userLabel");var label=(javafx.scene.control.Label)menu.lookup(".label");
            assertNotNull(label);assertTrue(label.getHeight()>=label.prefHeight(-1)-1);
            assertFalse(((javafx.scene.control.Button)field(main,"saveCourseButton")).isVisible());
            assertFalse(((javafx.scene.layout.HBox)field(main,"walkingToolbar")).isVisible());
            var file=Path.of("build/ui-preview/user-menu-and-initial-actions.png");Files.createDirectories(file.getParent());
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",file.toFile());return null;
        });
    }

    private static <T> T fx(java.util.concurrent.Callable<T> work) throws Exception {
        FutureTask<T> task = new FutureTask<>(work);
        Platform.runLater(task);
        return task.get(15, TimeUnit.SECONDS);
    }

    private static void callWith(Object target, String name, Class<?> type, Object value) throws Exception {
        var method = target.getClass().getDeclaredMethod(name, type);
        method.setAccessible(true);
        method.invoke(target, value);
    }

    private static void saveAndAwait(CoursePlannerController main) throws Exception {
        CountDownLatch saved = new CountDownLatch(1);
        fx(() -> {
            setField(main, "titlePrompt", (java.util.function.Function<com.datecourse.model.RoutePlan, java.util.Optional<String>>)
                    plan -> java.util.Optional.of(plan.title()));
            ((javafx.scene.control.Label) field(main, "saveStatusLabel")).textProperty()
                    .addListener((o, old, text) -> { if (text.equals("코스를 저장했어요.") || text.startsWith("코스는 저장됐어요.")) saved.countDown(); });
            invoke(main, "onSaveCourse");
            return null;
        });
        assertTrue(saved.await(10, TimeUnit.SECONDS));
    }

    @Test
    void pilotSourceWinsOverLateDatabaseLoadAndUnknownPriceSurvivesEditing() throws Exception {
        CountDownLatch dbStarted = new CountDownLatch(1), releaseDb = new CountDownLatch(1), dbFinished = new CountDownLatch(1);
        CountDownLatch pilotLoaded = new CountDownLatch(1);
        var delayed = new com.datecourse.db.PlaceRepository(null) {
            @Override public java.util.List<com.datecourse.model.Place> findAll() {
                dbStarted.countDown();
                while (releaseDb.getCount() > 0) {
                    try { releaseDb.await(20, TimeUnit.MILLISECONDS); }
                    catch (InterruptedException ignored) { /* Model JDBC work that cannot stop immediately. */ }
                }
                dbFinished.countDown();
                return java.util.List.of(new com.datecourse.model.Place(7, "old", "늦은 DB 결과", "MEAL", 35.1578, 129.06, 1000, 0));
            }
        };
        FutureTask<Object[]> create = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));
            Parent root = loader.load();
            new Scene(root, 1440, 960); root.resize(1440, 960);
            CoursePlannerController main = loader.getController();
            CourseEditorController editor = (CourseEditorController) loader.getNamespace().get("routeEditorController");
            stubPlannerMap(main, "mapView"); stubPlannerMap(editor, "editMapView");
            var status = (javafx.scene.control.Label) field(main, "sourceStatusLabel");
            status.textProperty().addListener((o, old, value) -> { if (value.startsWith("9곳")) pilotLoaded.countDown(); });
            main.setPlaceRepository(delayed);
            return new Object[]{main, editor, root};
        });
        Platform.runLater(create);
        Object[] fixture = create.get(10, TimeUnit.SECONDS);
        try {
            assertTrue(dbStarted.await(5, TimeUnit.SECONDS));
            FutureTask<Void> choose = new FutureTask<>(() -> { ((CoursePlannerController) fixture[0]).usePilotPlaces(); return null; });
            Platform.runLater(choose); choose.get(10, TimeUnit.SECONDS);
            assertTrue(pilotLoaded.await(5, TimeUnit.SECONDS));
        } finally { releaseDb.countDown(); }
        assertTrue(dbFinished.await(5, TimeUnit.SECONDS));
        FutureTask<Void> inspect = new FutureTask<>(() -> {
            CoursePlannerController main = (CoursePlannerController) fixture[0];
            CourseEditorController editor = (CourseEditorController) fixture[1];
            var places = (java.util.List<com.datecourse.model.Place>) field(main, "allPlaces");
            assertEquals(9, places.size());
            assertTrue(places.stream().allMatch(p -> p.placeId().startsWith("DCA-DEMO-")));
            invoke(main, "onRecommendClick"); invoke(editor, "onRecommendNext");
            assertTrue(((java.util.List<?>) field(editor, "candidates")).isEmpty());
            ((CheckBox) field(editor, "includeUncertainPrice")).setSelected(true);
            invoke(editor, "onRecommendNext");
            assertFalse(((java.util.List<?>) field(editor, "candidates")).isEmpty());
            invoke(editor, "onAddCandidate");
            assertTrue(((javafx.scene.control.Label) field(editor, "summaryLabel")).getText().contains("가격 미확인 1곳"));
            invoke(editor, "onFinish");
            var applied = (com.datecourse.model.RoutePlan) field(main, "appliedPlan");
            assertEquals(1, applied.costEstimate().unknownPlaces());
            String summary = ((javafx.scene.control.Label) field(main, "resultLabel")).getText();
            assertTrue(summary.contains("가격 미확인 1곳")); assertFalse(summary.contains("0원"));
            WebEngine engine = ((WebView) field(main, "mapView")).getEngine();
            assertEquals("가격 미확인", engine.executeScript("presentCalls[presentCalls.length-1][0].places[0].priceText"));
            return null;
        });
        Platform.runLater(inspect); inspect.get(15, TimeUnit.SECONDS);
    }

    @Test
    void placeDetailShowsUnknownPriceAndPublicSourceAsText() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
            window.presentRoute({startLat:35.1578,startLng:129.06,radius:500,people:2,places:[
              {recordId:1000001,placeId:'DCA-DEMO-001',name:'자료 확인 장소',category:'CAFE',lat:35.158,lng:129.06,
               priceText:'가격 미확인',priceEstimate:{minimum:null,maximum:null,unit:'UNKNOWN',basis:'1인 가격 미확인'},
               details:{address:'공개 원본 주소',sourceDate:'2026-09-30',sourceUrl:'https://example.test/<img src=x>'}}
            ]},null,500,false,true,[]);
            window.openRoutePlace(1000001);
            """);
        assertEquals(true, script(engine, "document.getElementById('detail-facts').textContent.includes('가격 미확인')"));
        assertEquals(true, script(engine, "document.getElementById('detail-extra').textContent.includes('공개 원본 주소')"));
        assertEquals(0, ((Number) script(engine, "document.querySelectorAll('#detail-extra img').length")).intValue());
    }

    @Test
    void onlyTmapWalkingGeometryIsShownAndFailuresDoNotRestoreStraightRoutes() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
                window.selectMapLocation(35.1796,129.0756,'출발');
                window.showResultPlaces([{recordId:1,placeId:'one',lat:35.1813,lng:129.0797,name:'도착',category:'CAFE',price:1000,rating:4}],true,false);
                window.showWalkingRoute({totalDistance:619,totalTime:514,legs:[{
                  from:{name:'출발'},to:{name:'<img src=x onerror=alert(1)>'},distance:619,time:514,
                  paths:[[{lat:35.1796,lng:129.0756},{lat:35.1789,lng:129.0760}],
                         [{lat:35.1789,lng:129.0760},{lat:35.1813,lng:129.0797}]],
                  steps:[{description:'<script>alert(1)</script>'}]
                }]},'',true);
                """);
        assertEquals(true, script(engine, "routeLines.filter(l=>l.attached).length===4 && routeLines.filter(l=>l.attached).every(l=>l.options.strokeStyle==='solid')"));
        assertEquals(true, script(engine, "routeLines[routeLines.length-1].options.path[0].getLat()===35.1789"));
        assertEquals(true, script(engine, "document.getElementById('walking-summary').textContent.includes('619m') && document.getElementById('walking-summary').textContent.includes('9분')"));
        assertEquals(0, ((Number)script(engine, "document.querySelectorAll('#walking-directions img,#walking-directions script').length")).intValue());
        script(engine, "window.showWalkingRoute(null,'연결 실패',false)");
        assertEquals(true, script(engine, "routeLines.filter(l=>l.attached).length===0"));
        assertEquals("연결 실패", script(engine, "document.getElementById('walking-summary').textContent"));
    }

    @Test void commonRangeMaximumTracksTransportAndResetRestoresWalkingDefaults() throws Exception {
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));loader.load();
            CoursePlannerController main=loader.getController();stubPlannerMap(main,"mapView");
            stubPlannerMap(field(main,"routeEditorController"),"editMapView");
            var modes=(javafx.scene.control.ComboBox<com.datecourse.model.TravelMode>)field(main,"travelModeBox");
            var range=(javafx.scene.control.Slider)field(main,"radiusSlider");
            modes.setValue(com.datecourse.model.TravelMode.DRIVING);assertEquals(30000,range.getMax());range.setValue(30000);
            modes.setValue(com.datecourse.model.TravelMode.TRANSIT);assertEquals(20000,range.getMax());assertEquals(20000,range.getValue());
            assertEquals("20km",((javafx.scene.control.Label)field(main,"radiusMaximumLabel")).getText());
            invoke(main,"onNewCourse");assertEquals(2000,range.getMax());assertEquals(500,range.getValue());
            assertEquals(com.datecourse.model.TravelMode.WALKING,modes.getValue());return null;
        });
    }

    @Test void extendedRangesReachMapAndEditorWithoutTwoKilometerClamping() throws Exception {
        WebEngine engine=loadMap();
        script(engine,"""
            window.presentRoute({startLat:35.15,startLng:129.11,radius:30000,maximumRadius:30000,maximumLegRadius:60000,people:2,places:[]},null,30000,true,false,[]);
            window.setMapRadius(30000);
            """);
        assertEquals(true,script(engine,"pins.some(p=>p.attached&&p.radius===30000)"));
        fx(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();
            CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            var plan=new com.datecourse.model.RoutePlan(35.15,129.11,2,30000,"",java.util.List.of()).withTravelMode(com.datecourse.model.TravelMode.DRIVING);
            editor.begin(plan,java.util.List.of(),p->{},()->{});
            assertEquals(30000,((javafx.scene.control.Slider)field(editor,"radiusSlider")).getMax());
            editor.dispose();return null;
        });
    }

    @Test void rangeHintsAppearOnlyNearVisibleCircleBorders() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            presentRoute({startLat:35.1578,startLng:129.06,radius:1000,places:[]},null,500,true,false,[]);
            testMap.setCenter(new kakao.maps.LatLng(35.1578,129.06));
            window.moveToRange=function(meters,buttons){
                var viewport=document.getElementById('map'),rect=viewport.getBoundingClientRect();
                var point=window.mapViewport.projection().containerPointFromCoords(new kakao.maps.LatLng(35.1578+meters/6378137*180/Math.PI,129.06));
                viewport.dispatchEvent(new MouseEvent('mousemove',{bubbles:true,buttons:buttons||0,
                    clientX:rect.left+point.x*rect.width/viewport.clientWidth,
                    clientY:rect.top+point.y*rect.height/viewport.clientHeight}));
            };
            moveToRange(0);
            """);
        assertNull(script(engine,"document.getElementById('map-legend')"));
        assertEquals(true,script(engine,"document.getElementById('range-tooltip').hidden"));
        script(engine,"moveToRange(1000)");
        assertEquals(false,script(engine,"document.getElementById('range-tooltip').hidden"));
        assertEquals("전체 허용 범위 · 1,000m",script(engine,"document.getElementById('range-tooltip').textContent"));
        script(engine,"moveToRange(500)");
        assertEquals("다음 장소의 검색 범위 · 500m",script(engine,"document.getElementById('range-tooltip').textContent"));
        script(engine,"moveToRange(500,1)");
        assertEquals(true,script(engine,"document.getElementById('range-tooltip').hidden"));
        script(engine,"document.getElementById('map').style.transform='scale(.8)';moveToRange(1000)");
        assertEquals(false,script(engine,"document.getElementById('range-tooltip').hidden"));
        script(engine,"document.getElementById('map-surface').classList.add('has-route-popup');moveToRange(1000)");
        assertEquals(true,script(engine,"document.getElementById('range-tooltip').hidden"));
        script(engine,"document.getElementById('map-surface').classList.remove('has-route-popup');setCoursePreviewMode(true);moveToRange(1000)");
        assertEquals(true,script(engine,"document.getElementById('range-tooltip').hidden"));
    }

    @Test void routeImagesRejectOldCoursesAndCancelledRequests() throws Exception {
        WebEngine engine=loadMap();
        script(engine,"""
            window.javaMapBridge.requestRouteImage=function(){};
            window.imagePlan={startLat:35.15,startLng:129.06,radius:1000,places:[
                {recordId:1,placeId:'first',name:'첫 코스',lat:35.16,lng:129.07}]};
            presentRoute(imagePlan,null,500,true,false,[]);
            var canvas=document.createElement('canvas');canvas.width=canvas.height=32;
            window.imageData=canvas.toDataURL('image/png');
            document.getElementById('route-image-button').click();
            window.firstImageRequest=routeImageRequestVersion;
            showRouteImage(imageData,firstImageRequest);
            """);
        assertEquals(false,script(engine,"document.getElementById('route-image-overlay').hidden"));
        script(engine,"""
            imagePlan.places=[{recordId:2,placeId:'second',name:'다른 코스',lat:35.17,lng:129.08}];
            presentRoute(imagePlan,null,500,true,false,[]);
            showRouteImage(imageData,firstImageRequest);
            """);
        assertEquals(true,script(engine,"document.getElementById('route-image-overlay').hidden && !document.getElementById('route-image').hasAttribute('src')"));
        script(engine,"""
            document.getElementById('route-image-button').click();window.cancelledRequest=routeImageRequestVersion;
            document.getElementById('route-map-button').click();showRouteImage(imageData,cancelledRequest);
            """);
        assertEquals(true,script(engine,"document.getElementById('route-image-overlay').hidden"));
        script(engine,"document.getElementById('route-image-button').click();showRouteImage(imageData,routeImageRequestVersion)");
        assertEquals(false,script(engine,"document.getElementById('route-image-overlay').hidden"));
        script(engine,"resetPlannerMap();showRouteImage(imageData,cancelledRequest)");
        assertEquals(true,script(engine,"document.getElementById('route-image-overlay').hidden"));
    }

    @Test void compactSearchOptionsKeepTheirValuesAndWrapInsideTheForm() throws Exception {
        fx(()->{
            var loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));Parent root=loader.load();
            CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of()),java.util.List.of(),p->{},()->{});
            new Scene(root,960,680);root.resize(960,680);root.applyCss();root.layout();
            var count=(javafx.scene.control.ComboBox<?>)field(editor,"conditionCandidateLimitBox");
            var cell=(javafx.scene.control.ListCell<?>)count.lookup(".combo-box > .list-cell");
            assertEquals(count.getValue().toString(),cell.getText());
            assertEquals(count.getValue().toString(),((javafx.scene.text.Text)cell.lookup(".text")).getText(),"후보 수 숫자가 잘리지 않아야 합니다.");
            var include=(CheckBox)field(editor,"includeUncertainPrice");
            assertTrue(include.getWidth()>=include.prefWidth(-1),"가격 미확인 옵션이 잘리지 않아야 합니다.");
            var form=root.lookup(".route-edit-form").localToScene(root.lookup(".route-edit-form").getBoundsInLocal());
            for(var node:java.util.List.of(count,include,root.lookup("#recommendNextButton"))) {
                var bounds=node.localToScene(node.getBoundsInLocal());
                assertTrue(bounds.getMinX()>=form.getMinX()&&bounds.getMaxX()<=form.getMaxX());
            }
            Path file=Path.of("build/ui-preview/compact-search-options.png");Files.createDirectories(file.getParent());
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",file.toFile());
            editor.dispose();return null;
        });
    }

    @Test void masterRouteImageControlsRemainConnected() throws Exception {
        WebEngine engine=loadMap();
        script(engine,"""
            window.imageRequests=0;window.javaMapBridge.requestRouteImage=function(){imageRequests++;};
            window.setRouteViewVisible(true);document.getElementById('route-image-button').click();
            var canvas=document.createElement('canvas');canvas.width=canvas.height=32;
            window.showRouteImage(canvas.toDataURL('image/png'));
            """);
        assertEquals(1,((Number)script(engine,"imageRequests")).intValue());
        assertEquals(true,script(engine,"!document.getElementById('route-controls').hidden && !document.getElementById('route-image-overlay').hidden"));
        script(engine,"document.getElementById('route-image').focus();document.getElementById('route-image').dispatchEvent(new KeyboardEvent('keydown',{key:'Enter',bubbles:true,cancelable:true}))");
        assertEquals(false,script(engine,"document.getElementById('route-image-zoom').hidden"));
        assertEquals(true,script(engine,"document.activeElement===document.getElementById('route-image-zoom-close')"));
        script(engine,"document.getElementById('route-image-zoom-close').dispatchEvent(new KeyboardEvent('keydown',{key:'Tab',shiftKey:true,bubbles:true,cancelable:true}))");
        assertEquals(true,script(engine,"document.activeElement===document.getElementById('route-image-zoom-close')"));
        script(engine,"document.getElementById('route-image-zoom-close').dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true,cancelable:true}))");
        assertEquals(true,script(engine,"document.activeElement===document.getElementById('route-image') && document.getElementById('route-image-zoom').hidden"));
        script(engine,"document.getElementById('route-map-button').click()");
        assertEquals(true,script(engine,"document.getElementById('route-image-zoom').hidden && document.getElementById('route-image-overlay').hidden"));
    }

    @Test void savedPreviewConnectionsNeverReplaceMainRoutesAndReturnAfterRouteFailure() throws Exception {
        WebEngine engine=loadMap();
        script(engine,"""
            window.setCoursePreviewMode(true);
            window.presentRoute({startLat:35.15,startLng:129.11,radius:1000,people:2,places:[
              {recordId:1,placeId:'one',name:'도착',category:'CAFE',lat:35.16,lng:129.12}
            ]},null,1000,true,false,[]);
            """);
        assertEquals(true,script(engine,"routeLines.filter(l=>l.attached).length===1 && routeLines.filter(l=>l.attached)[0].options.strokeStyle==='dash'"));
        assertEquals(true,script(engine,"!pins.some(c=>c.attached && c.options.strokeColor==='#15803d') && document.getElementById('place-search').hidden"));
        script(engine,"""
            window.showWalkingRoute({mode:'WALKING',totalDistance:100,totalTime:60,legs:[{
              from:{name:'출발'},to:{name:'도착'},distance:100,time:60,
              paths:[[{lat:35.15,lng:129.11},{lat:35.16,lng:129.12}]],steps:[]
            }]},'',false);
            """);
        assertEquals(true,script(engine,"routeLines.filter(l=>l.attached).length===2 && routeLines.filter(l=>l.attached).every(l=>l.options.strokeStyle==='solid')"));
        script(engine,"window.showWalkingRoute(null,'조회 실패',false)");
        assertEquals(true,script(engine,"routeLines.filter(l=>l.attached).length===1 && routeLines.filter(l=>l.attached)[0].options.strokeStyle==='dash'"));
        script(engine,"window.resetPlannerMap();window.setCoursePreviewMode(false);window.showWalkingRoute(null,'',false)");
        assertEquals(0,((Number)script(engine,"routeLines.filter(l=>l.attached).length")).intValue());
    }

    @Test void transitWalkingStagesHaveDistinctLinesAndKeepRouteInteraction() throws Exception {
        WebEngine engine=loadMap();
        script(engine,"""
            window.showWalkingRoute({mode:'TRANSIT',provider:'TMAP·카카오',totalDistance:1000,totalTime:600,legs:[{
              from:{name:'출발'},to:{name:'도착'},distance:1000,time:600,fare:1550,transfers:0,
              movementSummary:'도보 약 2분 → 버스 약 8분',
              paths:[[{lat:35.15,lng:129.11},{lat:35.151,lng:129.111}], [{lat:35.152,lng:129.112},{lat:35.16,lng:129.12}]],
              transitStages:[{type:'WALKING',path:[{lat:35.15,lng:129.11},{lat:35.151,lng:129.111}]},
                {type:'BUS',path:[{lat:35.152,lng:129.112},{lat:35.16,lng:129.12}]}],steps:[{description:'정류장까지 도보'}]
            }]},'',false);
            """);
        assertEquals(true,script(engine,"routeLines.filter(l=>l.attached&&l.options.zIndex===4).map(l=>l.options.strokeStyle).join(',')==='shortdash,solid'"));
        assertEquals(true,script(engine,"document.getElementById('walking-summary').textContent.includes('대중교통 + 도보')"));
        script(engine,"routeLines[1].onmouseover({});routeLines[1].onclick({});");
        assertEquals(true,script(engine,"document.getElementById('walking-detail-facts').textContent.includes('도보 약 2분 → 버스 약 8분')"));
        assertEquals(true,script(engine,"document.getElementById('walking-detail-facts').textContent.includes('1,550원')"));
        assertEquals(true,script(engine,"!document.getElementById('walking-details').hidden && document.getElementById('walking-details').open"));
        script(engine,"""
            window.showWalkingRoute({mode:'TRANSIT',provider:'TMAP',totalDistance:100,totalTime:120,legs:[{
              from:{name:'출발'},to:{name:'근처'},distance:100,time:120,fare:0,transfers:0,
              movementSummary:'도보 약 2분',transitStages:[{type:'WALKING',path:[]}],
              paths:[[{lat:35.15,lng:129.11},{lat:35.151,lng:129.111}]],steps:[{description:'횡단보도 이용'}]
            }]},'',false);
            """);
        assertEquals(true,script(engine,"routeLines.filter(l=>l.attached&&l.options.strokeColor!=='#ffffff').every(l=>l.options.strokeStyle==='shortdash')"));
        assertEquals(2,((Number)script(engine,"routeLines.filter(l=>l.attached).length")).intValue());
    }

    private static Object field(Object target, String name) throws Exception {
        var member = target.getClass().getDeclaredField(name);
        member.setAccessible(true);
        return member.get(target);
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        var member = target.getClass().getDeclaredField(name);
        member.setAccessible(true);
        member.set(target, value);
    }

    private static void stubPlannerMap(Object controller, String viewName) throws Exception {
        WebView view = (WebView) field(controller, viewName);
        view.getEngine().getLoadWorker().cancel();
        view.getEngine().executeScript("window.presentCalls=[];window.fitCalls=[];window.fitLegCalls=[];window.fitRouteLeg=function(index){fitLegCalls.push(index);};window.detailCalls=[];window.ensureCalls=0;window.ensureRouteRangesVisible=function(){ensureCalls++;};window.openRoutePlace=function(id){detailCalls.push(id);};window.presentRoute=function(plan,candidate,radius,locked,fit){presentCalls.push([plan,candidate,radius,locked,fit]);};window.fitRouteView=function(whole){fitCalls.push(whole);};");
        Object map = field(controller, "map");
        ((CourseMapView) map).setWalkingRouter(stops -> new com.datecourse.model.TravelRoute(java.util.List.of(), 0, 0));
        setField(map, "ready", true);
        setField(map, "loaded", true);
    }

    @Test
    void oneByOneEditingIsIsolatedUntilCompleteAndReturningKeepsDraft() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));
            Parent root = loader.load();
            CoursePlannerController main = loader.getController();
            CourseEditorController editor = (CourseEditorController) loader.getNamespace().get("routeEditorController");
            Parent editRoot = (Parent) loader.getNamespace().get("routeEditor");
            stubPlannerMap(main, "mapView");
            stubPlannerMap(editor, "editMapView");
            var first = new com.datecourse.model.Place(1,"a","첫 식당","MEAL",35,129,10000,4);
            var second = new com.datecourse.model.Place(2,"b","두 번째 식당","MEAL",35.0001,129,5000,4);
            setField(main,"allPlaces", java.util.List.of(first,second));
            setField(main,"placesLoaded",true);
            setField(main,"selectedLatitude",35.0);
            setField(main,"selectedLongitude",129.0);
            var nodes = loader.getNamespace();
            assertNull(nodes.get("budgetField"));
            assertNull(nodes.get("editCourseButton"));
            var range=(javafx.scene.control.Slider)nodes.get("radiusSlider");
            range.setValue(1200);
            ((WebView)nodes.get("mapView")).getEngine().executeScript("presentCalls=[]");
            ((javafx.scene.control.Button)nodes.get("recommendButton")).fire();
            assertNull(field(main,"appliedPlan"));
            assertNotSame(field(main,"map"),field(editor,"map"));
            assertEquals(0, ((Number)((WebView)nodes.get("mapView")).getEngine().executeScript("presentCalls.length")).intValue());
            invoke(editor,"onRecommendNext");
            assertTrue(((javafx.scene.control.ListView<?>)field(editor,"placeList")).getItems().isEmpty());
            invoke(editor,"onAddCandidate");
            var list=(javafx.scene.control.ListView<?>)field(editor,"placeList");
            assertEquals(java.util.List.of(first),list.getItems());
            assertNull(field(main,"appliedPlan"));
            invoke(editor,"onRecommendNext");
            invoke(editor,"onAddCandidate");
            assertEquals(java.util.List.of(first,second),list.getItems());
            list.getSelectionModel().select(1);
            invoke(editor,"onMoveUp");
            assertEquals(java.util.List.of(second,first),list.getItems());
            var animation=(javafx.animation.Timeline)field(main,"screenAnimation");
            animation.jumpTo(animation.getTotalDuration());
            animation.pause();
            new Scene(root,1440,960); root.resize(1440,960);root.applyCss();root.layout();
            Path preview=Path.of("build","ui-preview","route-editor.png");Files.createDirectories(preview.getParent());
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",preview.toFile());
            root.resize(960,680);root.applyCss();root.layout();
            root.layout();
            var compactSummary=(javafx.scene.control.Label)field(editor,"compactSummaryLabel");
            assertTrue(compactSummary.isVisible());
            assertFalse(((javafx.scene.layout.HBox)field(editor,"editorSummaryBar")).isManaged());
            assertTrue(compactSummary.getWidth()>300);
            assertTrue(compactSummary.getText().contains("30,000원"));
            WebView editMap=(WebView)field(editor,"editMapView");
            assertTrue(editMap.getWidth() >= root.getLayoutBounds().getWidth() * .4);
            assertTrue(editMap.getHeight() >= root.getLayoutBounds().getHeight() / 2, "지도 높이: " + editMap.getHeight());
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",
                    Path.of("build","ui-preview","route-editor-small.png").toFile());
            invoke(editor,"onFinish");
            var applied=(com.datecourse.model.RoutePlan)field(main,"appliedPlan");
            assertEquals(java.util.List.of(second,first),applied.places());
            assertEquals(30000,applied.totalCost());
            assertEquals(1200,applied.radius());
            assertEquals(java.util.Map.of(1L,500,2L,500),applied.arrivalRadii());
            var returnAnimation=(javafx.animation.Timeline)field(main,"screenAnimation");
            returnAnimation.stop();
            returnAnimation.getOnFinished().handle(new javafx.event.ActionEvent());
            assertEquals("[false]",((WebView)nodes.get("mapView")).getEngine().executeScript("JSON.stringify(fitCalls)"));
            assertEquals(1, ((Number)((WebView)nodes.get("mapView")).getEngine().executeScript("presentCalls.length")).intValue());
            invoke(main,"onRecommendClick");
            assertEquals(applied.arrivalRadii(),((com.datecourse.model.RoutePlan)field(editor,"original")).arrivalRadii());
            list.getSelectionModel().select(0);invoke(editor,"onRemove");
            invoke(editor,"onCancel");
            assertSame(applied,field(main,"appliedPlan"));
            assertEquals(2,applied.places().size());
            assertEquals(1, ((Number)((WebView)nodes.get("mapView")).getEngine().executeScript("presentCalls.length")).intValue());
            invoke(main,"onRecommendClick");
            assertEquals(java.util.List.of(first),list.getItems()); // Returning keeps the edited draft.
            TextField price=(TextField)field(editor,"placePriceField");
            price.setText("0");
            ((javafx.scene.control.Button)field(editor,"priceDown")).fire();assertEquals("0",price.getText());
            price.getParent().fireEvent(new javafx.scene.input.ScrollEvent(javafx.scene.input.ScrollEvent.SCROLL,
                    0,0,0,0,false,false,false,false,false,false,0,40,0,40,
                    javafx.scene.input.ScrollEvent.HorizontalTextScrollUnits.NONE,0,
                    javafx.scene.input.ScrollEvent.VerticalTextScrollUnits.NONE,0,0,null));
            assertEquals("0.1",price.getText());
            ((javafx.animation.Timeline)field(main,"screenAnimation")).stop();
            return null;
        });
        Platform.runLater(task);task.get(15,TimeUnit.SECONDS);
    }

    @Test
    void outsideGlobalRangeBlocksCompletionUntilRemovedOrRangeAdjusted() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));
            loader.load();
            CourseEditorController editor = loader.getController();
            stubPlannerMap(editor, "editMapView");
            var near = new com.datecourse.model.Place(1,"a","범위 안", "CAFE",35.001,129,3000,4);
            var far = new com.datecourse.model.Place(2,"b","먼 카페", "CAFE",35.003,129,3000,4);
            var completed = new java.util.concurrent.atomic.AtomicReference<com.datecourse.model.RoutePlan>();
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,200,"",java.util.List.of(near,far)),
                    java.util.List.of(near,far),completed::set,()->{});
            var finish=(javafx.scene.control.Button)field(editor,"finishButton");
            var warning=(javafx.scene.control.Label)field(editor,"rangeWarning");
            assertTrue(finish.isDisabled());
            assertTrue(warning.getText().contains("먼 카페"));
            invoke(editor,"onFinish");
            assertNull(completed.get());
            invoke(editor,"onModifyConditions");
            assertFalse(warning.getText().isEmpty());
            var list=(javafx.scene.control.ListView<?>)field(editor,"placeList");
            list.getSelectionModel().select(1); invoke(editor,"onMoveUp");
            assertTrue(finish.isDisabled());
            invoke(editor,"onRemove");
            assertEquals("",warning.getText());
            assertFalse(finish.isDisabled());
            invoke(editor,"onFinish");
            assertEquals(java.util.List.of(near),completed.get().places());
            completed.set(null);
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,500,"",java.util.List.of(near,far)),
                    java.util.List.of(near,far),completed::set,()->{});
            assertFalse(finish.isDisabled());
            invoke(editor,"onFinish");
            assertEquals(java.util.List.of(near,far),completed.get().places());
            return null;
        });
        Platform.runLater(task); task.get(15,TimeUnit.SECONDS);
    }

    @Test
    void clearingCourseCanBeCancelledOrCommittedWhileCommonConditionsRemain() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));loader.load();
            CoursePlannerController main=loader.getController();
            CourseEditorController editor=(CourseEditorController)loader.getNamespace().get("routeEditorController");
            stubPlannerMap(main,"mapView");stubPlannerMap(editor,"editMapView");
            var place=new com.datecourse.model.Place(1,"a","A","MEAL",35.0001,129,3000,4);
            setField(main,"allPlaces",java.util.List.of(place));setField(main,"placesLoaded",true);
            setField(main,"selectedLatitude",35.0);setField(main,"selectedLongitude",129.0);
            invoke(main,"onRecommendClick");invoke(editor,"onRecommendNext");invoke(editor,"onAddCandidate");invoke(editor,"onFinish");
            var saved=(com.datecourse.model.RoutePlan)field(main,"appliedPlan");
            assertEquals(1,saved.places().size());
            invoke(main,"onRecommendClick");
            var list=(javafx.scene.control.ListView<?>)field(editor,"placeList");
            list.getSelectionModel().select(0);invoke(editor,"onRemove");
            assertFalse(((javafx.scene.control.Button)field(editor,"finishButton")).isDisabled());
            invoke(editor,"onCancel");assertSame(saved,field(main,"appliedPlan"));
            invoke(main,"onRecommendClick");list.getSelectionModel().select(0);invoke(editor,"onRemove");invoke(editor,"onFinish");
            var cleared=(com.datecourse.model.RoutePlan)field(main,"appliedPlan");
            assertTrue(cleared.places().isEmpty());assertTrue(cleared.arrivalRadii().isEmpty());assertEquals(0,cleared.totalCost());
            assertEquals(saved.startLat(),cleared.startLat());assertEquals(saved.startLng(),cleared.startLng());
            assertEquals(saved.people(),cleared.people());assertEquals(saved.radius(),cleared.radius());
            WebEngine engine=((WebView)field(main,"mapView")).getEngine();
            assertEquals(0,((Number)engine.executeScript("presentCalls[presentCalls.length-1][0].places.length")).intValue());
            assertTrue(((javafx.scene.control.Label)field(main,"resultLabel")).getText().contains("선택한 장소 없음"));
            assertEquals("코스 만들기 →",((javafx.scene.control.Button)field(main,"recommendButton")).getText());
            // Reopen the empty course and complete without adding a place.
            invoke(main,"onRecommendClick");invoke(editor,"onFinish");
            assertTrue(((com.datecourse.model.RoutePlan)field(main,"appliedPlan")).places().isEmpty());
            return null;
        });
        Platform.runLater(task);task.get(15,TimeUnit.SECONDS);
    }

    @Test
    void arrivalLimitsAreCapturedAndSurviveReopeningWhileInvalidNewLegsBlockFinish() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();
            CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            var a=new com.datecourse.model.Place(1,"a","A","CAFE",35.001,129,3000,4);
            var b=new com.datecourse.model.Place(2,"b","B","CAFE",35.002,129,3000,4);
            var c=new com.datecourse.model.Place(3,"c","C","CAFE",35.003,129,3000,4);
            var all=java.util.List.of(a,b,c);
            var result=new java.util.concurrent.atomic.AtomicReference<com.datecourse.model.RoutePlan>();
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of()),all,result::set,()->{});
            ((javafx.scene.control.ComboBox<?>)field(editor,"activityBox")).getSelectionModel().select(1);
            var radius=(javafx.scene.control.Slider)field(editor,"radiusSlider");
            for(int limit:new int[]{200,300,200}){
                radius.setValue(limit);invoke(editor,"onRecommendNext");invoke(editor,"onAddCandidate");
            }
            invoke(editor,"onFinish");
            var saved=result.get();assertNotNull(saved);
            assertEquals(all,saved.places());
            assertEquals(java.util.Map.of(1L,200,2L,300,3L,200),saved.arrivalRadii());
            result.set(null);
            editor.begin(saved,all,result::set,()->{});
            var list=(javafx.scene.control.ListView<?>)field(editor,"placeList");
            var finish=(javafx.scene.control.Button)field(editor,"finishButton");
            list.getSelectionModel().select(2);invoke(editor,"onMoveUp");invoke(editor,"onMoveUp");
            assertTrue(finish.isDisabled());invoke(editor,"onFinish");assertNull(result.get());
            String warning=((javafx.scene.control.Label)field(editor,"rangeWarning")).getText();
            assertTrue(warning.contains("출발지 → C"));
            assertTrue(warning.contains("허용 200m"));
            invoke(editor,"onMoveDown");invoke(editor,"onMoveDown");assertFalse(finish.isDisabled());
            list.getSelectionModel().select(1);invoke(editor,"onRemove");
            assertTrue(finish.isDisabled());invoke(editor,"onFinish");assertNull(result.get());
            assertEquals(all,saved.places());assertEquals(3,saved.arrivalRadii().size());
            invoke(editor,"onCancel");
            editor.begin(saved,all,result::set,()->{});assertFalse(finish.isDisabled());
            list.getSelectionModel().select(2);invoke(editor,"onRemove");invoke(editor,"onFinish");
            assertEquals(java.util.Map.of(1L,200,2L,300),result.get().arrivalRadii());
            return null;
        });
        Platform.runLater(task);task.get(15,TimeUnit.SECONDS);
    }

    @Test
    void selectedPlaceRadiusEditsRecoverValidationWithoutChangingNextConditionsOrOriginal() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));loader.load();
            CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            var a=new com.datecourse.model.Place(1,"a","A","CAFE",35.001,129,3000,4);
            var b=new com.datecourse.model.Place(2,"b","B","CAFE",35.002,129,3000,4);
            var c=new com.datecourse.model.Place(3,"c","C","CAFE",35.003,129,3000,4);
            var saved=new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of(a,c),java.util.Map.of(1L,200,3L,200));
            var result=new java.util.concurrent.atomic.AtomicReference<com.datecourse.model.RoutePlan>();
            editor.begin(saved,java.util.List.of(a,b,c),result::set,()->{});
            var list=(javafx.scene.control.ListView<?>)field(editor,"placeList");
            var slider=(javafx.scene.control.Slider)field(editor,"selectedRadiusSlider");
            var next=(javafx.scene.control.Slider)field(editor,"radiusSlider");
            var finish=(javafx.scene.control.Button)field(editor,"finishButton");
            assertTrue(slider.isDisabled());assertTrue(finish.isDisabled());
            ((javafx.scene.control.ComboBox<?>)field(editor,"activityBox")).getSelectionModel().select(1);
            invoke(editor,"onRecommendNext");
            Object candidates=field(editor,"candidates");
            assertEquals("CHOICE",field(editor,"state").toString());
            list.getSelectionModel().select(1);assertEquals(200,slider.getValue());
            WebEngine engine=((WebView)field(editor,"editMapView")).getEngine();
            engine.executeScript("presentCalls=[];fitCalls=[];ensureCalls=0");
            slider.setValue(300);
            assertFalse(finish.isDisabled());
            assertTrue(((javafx.scene.control.Label)field(editor,"rangeWarning")).getText().isEmpty());
            assertEquals(500,next.getValue());assertSame(candidates,field(editor,"candidates"));
            assertEquals("COURSE",field(editor,"state").toString());
            assertEquals(true,engine.executeScript("presentCalls.length>0 && presentCalls.every(function(c){return c[4]===false;}) && fitCalls.length===0 && ensureCalls===1"));
            list.getSelectionModel().select(0);assertEquals(200,slider.getValue());
            list.getSelectionModel().select(1);assertEquals(300,slider.getValue());
            assertEquals(200,saved.arrivalRadii().get(3L));
            slider.setValue(200);assertTrue(finish.isDisabled());
            slider.setValue(300);invoke(editor,"onFinish");
            assertEquals(300,result.get().arrivalRadii().get(3L));
            invoke(editor,"onCancel");editor.begin(saved,java.util.List.of(a,b,c),result::set,()->{});
            list.getSelectionModel().select(1);assertEquals(200,slider.getValue());
            // Local relaxation must not bypass the separate global-range check.
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,200,"",java.util.List.of(c),java.util.Map.of(3L,200)),
                    java.util.List.of(c),result::set,()->{});
            list.getSelectionModel().select(0);slider.setValue(500);assertTrue(finish.isDisabled());
            invoke(editor,"onRemove");assertTrue(slider.isDisabled());
            // Loading a legacy place must not silently invent a condition on selection.
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of(a)),java.util.List.of(a),result::set,()->{});
            list.getSelectionModel().select(0);
            var record=(javafx.scene.control.Button)field(editor,"recordRadiusButton");assertTrue(record.isVisible());
            record.fire();assertFalse(record.isVisible());invoke(editor,"onFinish");
            assertEquals(500,result.get().arrivalRadii().get(1L));
            return null;
        });
        Platform.runLater(task);task.get(15,TimeUnit.SECONDS);
    }

    private static void invoke(Object target, String name) throws Exception {
        var method=target.getClass().getDeclaredMethod(name);method.setAccessible(true);method.invoke(target);
    }

    @Test
    void editorGeometryIsStableAcrossStatesAndAddingPreservesCamera() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));
            Parent root=loader.load();CourseEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            var first=new com.datecourse.model.Place(1,"p1","첫 식당","MEAL",35,129,5000,4);
            var next=new com.datecourse.model.Place(2,"p2","다음 식당","MEAL",35.0001,129,6000,4);
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of(first)),java.util.List.of(first,next),p->{},()->{});
            new Scene(root,1440,960);root.resize(1440,960);root.applyCss();root.layout();
            var footer=root.lookup("#editorFooter");var map=(WebView)field(editor,"editMapView");
            double mapHeight=map.getHeight(),footerY=footer.localToScene(footer.getBoundsInLocal()).getMinY();
            assertEquals(root.getLayoutBounds().getHeight()-footer.getBoundsInParent().getHeight(),footerY,0.1);
            var course=(javafx.scene.control.TitledPane)field(editor,"coursePane");course.setExpanded(true);root.applyCss();root.layout();
            assertEquals(mapHeight,map.getHeight(),0.1);assertEquals(footerY,footer.localToScene(footer.getBoundsInLocal()).getMinY(),0.1);
            assertTrue(root.lookupAll(".list-cell").stream().anyMatch(node->node instanceof javafx.scene.control.ListCell<?> cell
                    && first.equals(cell.getItem()) && cell.getText().contains("5,000원/인") && cell.getText().contains("2명 예상 총비용 10,000원")));
            invoke(editor,"onRecommendNext");root.applyCss();root.layout();
            assertEquals(mapHeight,map.getHeight(),0.1);assertEquals(footerY,footer.localToScene(footer.getBoundsInLocal()).getMinY(),0.1);
            course.setExpanded(false);root.layout();assertEquals(mapHeight,map.getHeight(),0.1);
            map.getEngine().executeScript("presentCalls=[]");invoke(editor,"onAddCandidate");
            assertEquals(true,map.getEngine().executeScript("presentCalls.length>0 && presentCalls.every(function(call){return call[4]===false;})"));
            root.applyCss();root.layout();assertEquals(mapHeight,map.getHeight(),0.1);
            root.resize(960,680);root.applyCss();root.layout();
            double smallHeight=map.getHeight();double smallFooter=footer.localToScene(footer.getBoundsInLocal()).getMinY();
            assertEquals(root.getLayoutBounds().getHeight()-footer.getBoundsInParent().getHeight(),smallFooter,0.1);
            course.setExpanded(true);invoke(editor,"onRecommendNext");root.applyCss();root.layout();
            assertEquals(smallHeight,map.getHeight(),0.1);assertEquals(smallFooter,footer.localToScene(footer.getBoundsInLocal()).getMinY(),0.1);
            return null;
        });Platform.runLater(task);task.get(10,TimeUnit.SECONDS);
    }

    @Test
    void conditionChoiceTransitionsPreserveInputsAndKeepEmptyResultsInCondition() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));
            Parent root=loader.load();CourseEditorController editor=loader.getController();
            stubPlannerMap(editor,"editMapView");
            var place=new com.datecourse.model.Place(1,"cafe","카페","CAFE",35.0001,129,5000,4);
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of()),java.util.List.of(place),p->{},()->{});
            var condition=(javafx.scene.control.TitledPane)field(editor,"conditionPane");
            var choice=(javafx.scene.control.TitledPane)field(editor,"choicePane");
            var activity=(javafx.scene.control.ComboBox<?>)field(editor,"activityBox");
            TextField price=(TextField)field(editor,"placePriceField");
            var radius=(javafx.scene.control.Slider)field(editor,"radiusSlider");
            var searchButton=(javafx.scene.control.Button)field(editor,"recommendNextButton");
            assertEquals("후보 검색",searchButton.getText());
            assertEquals("CONDITION",field(editor,"state").toString());
            assertTrue(condition.isExpanded());assertFalse(choice.isExpanded());
            activity.getSelectionModel().select(1);price.setText("1");radius.setValue(700);
            new Scene(root,1440,960);root.resize(1440,960);root.applyCss();root.layout();
            assertEquals(javafx.scene.Cursor.DEFAULT,condition.getChildrenUnmodifiable().stream().filter(node->node.getStyleClass().contains("title")).findFirst().orElseThrow().getCursor());
            root.resize(960,680);root.applyCss();root.layout();
            var countControl=(javafx.scene.control.ComboBox<?>)field(editor,"conditionCandidateLimitBox");
            assertTrue(searchButton.localToScene(searchButton.getBoundsInLocal()).getMaxX()<=960);
            assertTrue(countControl.localToScene(countControl.getBoundsInLocal()).getMaxX()<=960);
            root.resize(1440,960);root.applyCss();root.layout();
            Path dir=Path.of("build","ui-preview");Files.createDirectories(dir);
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",dir.resolve("editor-condition.png").toFile());
            invoke(editor,"onRecommendNext");
            assertEquals("CHOICE",field(editor,"state").toString());
            assertFalse(condition.isExpanded());assertTrue(choice.isExpanded());
            root.applyCss();root.layout();
            assertEquals("후보 업데이트",searchButton.getText());
            assertEquals(javafx.scene.Cursor.DEFAULT,choice.getChildrenUnmodifiable().stream().filter(node->node.getStyleClass().contains("title")).findFirst().orElseThrow().getCursor());
            assertEquals(javafx.scene.Cursor.HAND,condition.getChildrenUnmodifiable().stream().filter(node->node.getStyleClass().contains("title")).findFirst().orElseThrow().getCursor());
            invoke(editor,"onCandidateDetail");assertEquals("CHOICE",field(editor,"state").toString());
            invoke(editor,"onModifyConditions");
            assertEquals("CONDITION",field(editor,"state").toString());
            assertEquals("카페",activity.getValue());assertEquals("1",price.getText());assertEquals(700,radius.getValue());
            invoke(editor,"onRecommendNext");assertEquals("CHOICE",field(editor,"state").toString());
            root.applyCss();root.layout();
            assertNull(root.lookup(".candidate-row-add"));assertNull(root.lookup(".candidate-row-detail"));
            invoke(editor,"onAddCandidate");
            assertEquals(0,((Number)((javafx.scene.web.WebView)field(editor,"editMapView")).getEngine().executeScript("fitLegCalls[fitLegCalls.length-1]")).intValue());
            assertEquals("CONDITION",field(editor,"state").toString());
            assertEquals(java.util.List.of(place),((javafx.scene.control.ListView<?>)field(editor,"placeList")).getItems());
            assertFalse(choice.isExpanded());assertEquals("1",price.getText());
            assertEquals("후보 검색",searchButton.getText());
            var course=(javafx.scene.control.TitledPane)field(editor,"coursePane");
            course.setExpanded(true);assertTrue(course.isExpanded());assertFalse(condition.isExpanded());assertFalse(choice.isExpanded());
            course.setExpanded(false);assertTrue(course.isExpanded());
            condition.setExpanded(true);assertFalse(course.isExpanded());assertTrue(condition.isExpanded());
            condition.setExpanded(false);assertTrue(condition.isExpanded());
            invoke(editor,"onRecommendNext");
            assertEquals("CONDITION",field(editor,"state").toString());
            assertTrue(((javafx.scene.control.Label)field(editor,"emptyResultMessage")).getText().contains("없어요"));
            assertTrue(choice.isDisabled());
            return null;
        });Platform.runLater(task);task.get(10,TimeUnit.SECONDS);
    }


    @Test void nextRadiusClampsOnContextChangeWithoutRewritingSavedLegLimits() throws Exception {
        fx(() -> {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));
            Parent root=loader.load();CourseEditorController editor=loader.getController();
            stubPlannerMap(editor,"editMapView");
            var place=new com.datecourse.model.Place(1,"p1","카페","CAFE",35,129,5000,4);
            var plan=new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of(place),java.util.Map.of(1L,1500));
            editor.begin(plan,java.util.List.of(place),p->{},()->{});
            var radius=(javafx.scene.control.Slider)field(editor,"radiusSlider");
            radius.setValue(900);
            editor.resume(new com.datecourse.model.RoutePlan(35,129,2,300,"",plan.places(),plan.arrivalRadii()),java.util.List.of(place));
            assertEquals(300,radius.getMax());assertEquals(300,radius.getValue());
            assertEquals(1500,editor.draftSnapshot().arrivalRadii().get(1L));
            var list=(javafx.scene.control.ListView<?>)field(editor,"placeList");list.getSelectionModel().selectFirst();
            assertTrue(((javafx.scene.control.Label)field(editor,"selectedRadiusLabel")).getText().contains("다음 장소의 조건"));
            assertInstanceOf(com.datecourse.page.common.controls.ModernScrollPane.class,field(editor,"candidateScroll"));
            return null;
        });
    }

    @Test
    void topFiveSelectionAndDetailDoNotAddPlaceUntilConfirmed() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_02step_editor/route-editor-view.fxml"));
            Parent root=loader.load(); CourseEditorController editor=loader.getController();
            stubPlannerMap(editor,"editMapView");
            var places=new java.util.ArrayList<com.datecourse.model.Place>();
            for(int i=1;i<=8;i++) places.add(new com.datecourse.model.Place(i,"p"+i,"후보 "+i,"MEAL",35+i*0.0001,129,5000,4));
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of()),places,p->{},()->{});
            invoke(editor,"onRecommendNext");
            var candidates=(javafx.scene.control.ListView<?>)field(editor,"candidateList");
            var confirmed=(javafx.scene.control.ListView<?>)field(editor,"placeList");
            assertEquals(5,candidates.getItems().size()); assertTrue(confirmed.getItems().isEmpty());
            new Scene(root,1440,960);root.resize(1440,960);root.applyCss();root.layout();
            var fourthCell = root.lookupAll(".list-cell").stream()
                .filter(node -> node instanceof javafx.scene.control.ListCell<?> cell
                    && cell.getListView() == candidates && cell.getIndex() == 3)
                .findFirst().orElseThrow();
            fourthCell.fireEvent(new javafx.scene.input.MouseEvent(javafx.scene.input.MouseEvent.MOUSE_CLICKED,
                4,4,4,4,javafx.scene.input.MouseButton.PRIMARY,1,false,false,false,false,
                false,false,false,false,false,true,null));
            assertEquals(3,candidates.getSelectionModel().getSelectedIndex());
            WebEngine engine=((WebView)field(editor,"editMapView")).getEngine();
            assertEquals("[4]",engine.executeScript("JSON.stringify(detailCalls)"));
            assertTrue(confirmed.getItems().isEmpty());
            root.applyCss();root.layout();
            ((javafx.scene.control.ScrollPane)root.lookup(".route-form-scroll")).setVvalue(1);
            root.layout();
            Path candidatePreview=Path.of("build","ui-preview","top-five-candidates.png");
            Files.createDirectories(candidatePreview.getParent());
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",candidatePreview.toFile());
            invoke(editor,"onAddCandidate");
            assertEquals(java.util.List.of(places.get(3)),confirmed.getItems());
            assertTrue(candidates.getItems().isEmpty());
            confirmed.getSelectionModel().selectFirst();
            confirmed.fireEvent(new javafx.scene.input.KeyEvent(javafx.scene.input.KeyEvent.KEY_PRESSED,"","",
                    javafx.scene.input.KeyCode.ENTER,false,false,false,false));
            assertEquals("[4,4]",engine.executeScript("JSON.stringify(detailCalls)"));
            return null;
        });Platform.runLater(task);task.get(10,TimeUnit.SECONDS);
    }

    @Test
    void routeMarkersAndButtonsOpenDetailsWithoutShowingLegacyRailOrChangingRoute() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.selectedRecords=[];javaMapBridge.onPlaceSelected=function(id){selectedRecords.push(id);};
            window.testPlan={startLat:35.157,startLng:129.06,radius:1000,places:[{recordId:1,placeId:'a',name:'확정 식당',category:'MEAL',price:12000,rating:4.2,lat:35.157,lng:129.06}]};
            window.testCandidates=[{recordId:2,placeId:'b',name:'후보 카페',category:'CAFE',price:5000,rating:4.5,lat:35.158,lng:129.06},{recordId:3,placeId:'c',name:'다른 후보',category:'CAFE',price:6000,rating:4.1,lat:35.159,lng:129.06}];
            window.presentRoute(testPlan,testCandidates[0],500,true,true,testCandidates);
            window.oldRouteLines=routeLines.length;
            pins.find(function(p){return p.attached && p.options.title==='추천 후보. 다른 후보';}).onclick();
            """);
        assertEquals("다른 후보",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals("[\"3\"]",script(engine,"JSON.stringify(selectedRecords)"));
        assertEquals(false,script(engine,"document.getElementById('place-detail').hidden"));
        assertEquals(true,script(engine,"document.getElementById('itinerary-rail').hidden"));
        assertTrue(((String)script(engine,"document.getElementById('detail-facts').textContent")).contains("6,000원"));
        assertEquals(true,script(engine,"document.getElementById('place-detail').getBoundingClientRect().width>0"));
        script(engine,"window.openRoutePlace(1)");
        assertEquals("확정 식당",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(true,script(engine,"routeLines.length===oldRouteLines && testPlan.places.length===1"));
        assertEquals("[]",script(engine,"JSON.stringify(bridgeSelections)"));
        script(engine,"window.presentRoute(testPlan,null,500,false,true,[]);pins.find(function(p){return p.attached && p.options.title==='1. 확정 식당';}).onclick()");
        assertEquals(false,script(engine,"document.getElementById('place-detail').hidden"));
        assertEquals(true,script(engine,"document.getElementById('itinerary-rail').hidden"));
    }

    @Test void thirtyCandidatePinsKeepTheirRanksAndShowEvidenceWithoutAddingStops() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.selectedRecords=[];javaMapBridge.onPlaceSelected=function(id){selectedRecords.push(id);};
            window.largeCandidates=Array.from({length:30},function(_,i){return {recordId:i+1,placeId:'p'+i,
                name:'후보 '+(i+1),category:'MEAL',lat:35.157+i*0.00001,lng:129.06,priceText:'약 7,000원/인',
                priceEstimate:{evidence:'SIMILAR_ESTIMATE',basis:'동종 업종 기준 메뉴 평균'},
                details:{tags:[{tag:'GOOD_PRICE',basis:'공공 지정자료'}]}};});
            window.emptyPlan={startLat:35.157,startLng:129.06,radius:2000,places:[]};
            window.presentRoute(emptyPlan,largeCandidates[0],2000,true,true,largeCandidates);
            """);
        assertEquals(30,((Number)script(engine,"pins.filter(function(p){return p.attached && p.options.title && p.options.title.indexOf('추천 후보.')===0;}).length")).intValue());
        assertEquals(true,script(engine,"pins.some(function(p){return p.attached && p.options.title==='추천 후보. 후보 30' && decodeURIComponent(p.normalImage.src).indexOf('>30</text>')>=0;})"));
        script(engine,"pins.find(function(p){return p.attached && p.options.title==='추천 후보. 후보 30';}).onclick()");
        assertEquals("후보 30",script(engine,"document.getElementById('detail-title').textContent"));
        assertTrue(((String)script(engine,"document.getElementById('detail-extra').textContent")).contains("유사 장소 기준 추정"));
        assertTrue(((String)script(engine,"document.getElementById('detail-extra').textContent")).contains("착한가격"));
        assertEquals("[\"30\"]",script(engine,"JSON.stringify(selectedRecords)"));
        assertEquals(0,((Number)script(engine,"emptyPlan.places.length")).intValue());
    }

    @Test
    void editorMapFitsWholeRadiusSeparatesCandidateAndDoesNotAffectOtherMap() throws Exception {
        WebEngine main=loadMap(700,700);
        WebEngine edit=loadMap(700,700);
        String plan="{startLat:35.157,startLng:129.06,radius:1200,places:[{recordId:1,placeId:'a',name:'확정',lat:35.158,lng:129.061}]}";
        script(main,"window.presentRoute("+plan+",null,500,false,true)");
        String mainPins=(String)script(main,"JSON.stringify(pins.filter(function(p){return p.attached && p.options.title;}).map(function(p){return p.options.title;}))");
        script(edit,"window.presentRoute("+plan+",{recordId:2,placeId:'b',name:'후보',lat:35.159,lng:129.062},1000,true,true)");
        assertEquals(true,script(edit,"document.getElementById('itinerary-rail').hidden && document.getElementById('place-detail').hidden"));
        assertEquals(true,script(edit,"document.getElementById('place-search').hidden"));
        assertEquals(true,script(edit,"pins.some(function(p){return p.attached && p.options.title==='추천 후보. 후보' && decodeURIComponent(p.image.src).indexOf('#2563eb')>=0;})"));
        assertEquals(true,script(edit,"pins.some(function(p){if(!p.attached || p.options.title!=='추천 후보. 후보')return false;var svg=decodeURIComponent(p.normalImage.src);return svg.indexOf('opacity=\"0.55\"')>=0 && svg.indexOf('>1</text>')>=0 && svg.indexOf('?')<0;})"));
        assertEquals(true,script(edit,"testMap.getLevel()>5 && Math.abs(testMap.getCenter().getLat()-35.157)<.001"));
        assertEquals(true, script(edit,"pins.some(function(p){return p.attached && p.options.strokeColor==='#2563eb' && p.radius===1200 && p.position.getLat()===35.157;})"));
        assertEquals(true, script(edit,"pins.some(function(p){return p.attached && p.options.strokeColor==='#15803d' && p.options.strokeStyle==='shortdash' && p.radius===1000 && p.position.getLat()===35.158;})"));
        assertEquals(false,script(edit,"!pins.some(c=>c.attached && c.options.strokeColor==='#15803d')"));
        assertEquals("FIT",script(edit,"inspectMapCamera().last.intent"));
        assertEquals("[]",script(edit,"JSON.stringify(bridgeSelections)"));
        script(edit,"testMap.onclick({latLng:new kakao.maps.LatLng(36,130)})");
        assertEquals("[]",script(edit,"JSON.stringify(bridgeSelections)"));
        assertEquals(mainPins,script(main,"JSON.stringify(pins.filter(function(p){return p.attached && p.options.title;}).map(function(p){return p.options.title;}))"));
        script(edit,"window.fitRouteView(true)");
        assertEquals(true,script(edit,"Math.abs(testMap.getCenter().getLat()-35.158)<1e-8 && Math.abs(testMap.getCenter().getLng()-129.061)<1e-8"));
        script(edit,"window.presentRoute({startLat:35.157,startLng:129.06,radius:800,places:[]},null,200,true,true)");
        assertEquals(true,script(edit,"pins.some(function(p){return p.attached && p.options.strokeColor==='#15803d' && p.radius===200 && p.position.getLat()===35.157;})"));
        script(edit,"window.presentRoute("+plan+",null,500,false,true)");
        assertEquals(true,script(edit,"!pins.some(c=>c.attached && c.options.strokeColor==='#15803d')"));
        assertEquals(false,script(edit,"pins.some(function(p){return p.attached && p.options.strokeColor==='#15803d';})"));
        assertEquals(true,script(edit,"Math.abs(testMap.getCenter().getLat()-35.157)<1e-8 && Math.abs(testMap.getCenter().getLng()-129.06)<1e-8"));
    }

    @BeforeAll
    static void startJavaFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try {
            Platform.startup(() -> {
                Platform.setImplicitExit(false);
                started.countDown();
            });
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(started::countDown);
        }
        assertTrue(started.await(10, TimeUnit.SECONDS));
    }

    private static Object script(WebEngine engine, String code) throws Exception {
        FutureTask<Object> task = new FutureTask<>(() -> engine.executeScript(code));
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }

    @Test void mapControlsWinPointerInputWhenTheyOverlapMarkers() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.presentRoute({startLat:35.1796,startLng:129.0756,radius:1000,places:[]},null,500,true,false,[]);
            window.setRouteViewVisible(true);window.imageRequests=0;javaMapBridge.requestRouteImage=function(){imageRequests++;};
            var target=document.getElementById('route-image-button'),button=target.getBoundingClientRect(),view=document.getElementById('map').getBoundingClientRect();
            var x=button.left+button.width/2,y=button.top+button.height/2;
            var coords=mapViewport.projection().coordsFromContainerPoint(new kakao.maps.Point(x-view.left,y-view.top+30));
            window.presentRoute({startLat:35.1796,startLng:129.0756,radius:1000,places:[{recordId:1,placeId:'under-button',name:'버튼 아래 마커',lat:coords.getLat(),lng:coords.getLng()}]},null,500,true,false,[]);
            ['mousemove','mousedown','mouseup','click'].forEach(type=>target.dispatchEvent(new MouseEvent(type,{bubbles:true,cancelable:true,button:0,detail:1,clientX:x,clientY:y})));
            """);
        assertEquals(1,((Number)script(engine,"imageRequests")).intValue());
        assertEquals(true,script(engine,"document.getElementById('place-detail').hidden"));
        assertEquals(false,script(engine,"routeOverlays.some(o=>o.attached && o.options.content.classList.contains('place-pin-preview'))"));
    }

    @Test void standaloneDetailsReturnFocusToOpenerOrVisibleMapWhenOpenerDisappears() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.presentRoute({startLat:35.157,startLng:129.06,radius:1000,places:[
                {recordId:1,placeId:'a',name:'식당',category:'MEAL',price:9000,lat:35.158,lng:129.06}]},null,500,true,true);
            window.opener=document.createElement('button');opener.textContent='상세 열기';document.getElementById('map-surface').appendChild(opener);
            opener.focus();window.openRoutePlace(1);
            """);
        assertEquals(true,script(engine,"document.activeElement===document.getElementById('detail-close')"));
        script(engine,"document.getElementById('detail-close').click()");
        assertEquals(true,script(engine,"document.activeElement===opener"));
        script(engine,"opener.focus();window.openRoutePlace(1);opener.remove();document.getElementById('detail-close').click()");
        assertEquals(true,script(engine,"document.activeElement===document.getElementById('map-surface')"));
    }

    @Test void routeDetailsRestoreKeyboardCloseAfterReopening() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.showWalkingRoute({totalDistance:100,totalTime:60,legs:[{from:{name:'출발'},to:{name:'도착'},distance:100,time:60,
                paths:[[{lat:35.1796,lng:129.0756},{lat:35.18,lng:129.0756}]],steps:[{description:'직진'}]}]},'',false);
            routeLines[1].onclick();
            """);
        assertEquals(0,((Number)script(engine,"document.getElementById('detail-close').tabIndex")).intValue());
        script(engine,"document.getElementById('detail-close').focus();document.getElementById('detail-close').dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true,cancelable:true}))");
        assertEquals("true",script(engine,"document.getElementById('place-detail').getAttribute('aria-hidden')"));
        script(engine,"routeLines[1].onclick();document.getElementById('detail-close').focus()");
        assertEquals(true,script(engine,"document.activeElement===document.getElementById('detail-close') && document.getElementById('detail-close').tabIndex===0"));
        script(engine,"document.getElementById('detail-close').click()");
        assertEquals("true",script(engine,"document.getElementById('place-detail').getAttribute('aria-hidden')"));
    }

    @Test void bufferedMapKeepsEdgeCoordinatesAndNativeCopyrightInsideVisibleViewport() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            window.viewport=document.getElementById('map');window.canvas=document.getElementById('map-canvas');
            testMap.setCenter(new kakao.maps.LatLng(35.158,129.06));
            window.edge=mapViewport.projection().coordsFromContainerPoint(new kakao.maps.Point(viewport.clientWidth-8,viewport.clientHeight/2));
            window.presentRoute({startLat:35.158,startLng:129.06,radius:1000,places:[
                {recordId:9,placeId:'edge',name:'경계 장소',lat:edge.getLat(),lng:edge.getLng()}]},null,500,true,false,[]);
            window.footer=document.createElement('div');footer.textContent='Kakao · 지도 출처';
            Object.assign(footer.style,{position:'absolute',bottom:'0px',right:'0px',width:'160px',height:'20px'});canvas.appendChild(footer);
            window.tileLayer=document.createElement('div');Object.assign(tileLayer.style,{position:'absolute',top:'0px',bottom:'0px'});canvas.appendChild(tileLayer);
            """);
        Thread.sleep(60);
        assertEquals(true,script(engine,"canvas.clientWidth>viewport.clientWidth && canvas.clientHeight>viewport.clientHeight && getComputedStyle(viewport).overflow==='hidden'"));
        assertEquals(true,script(engine,"(function(){var visible=viewport.getBoundingClientRect(),copy=footer.getBoundingClientRect();return copy.right<=visible.right+1 && copy.bottom<=visible.bottom+1 && copy.left>=visible.left && footer.parentElement===canvas && !tileLayer.style.marginBottom;})()"));
        script(engine,"""
            window.p=mapViewport.projection().containerPointFromCoords(edge);window.rect=viewport.getBoundingClientRect();
            viewport.dispatchEvent(new MouseEvent('mousemove',{bubbles:true,clientX:rect.left+p.x,clientY:rect.top+p.y-20}));
            window.pin=pins.find(p=>p.coursePin && p.attached);
            """);
        assertEquals(true,script(engine,"pin.image===pin.activeImage && Math.abs(p.x-(viewport.clientWidth-8))<1"));
        script(engine,"pin.onclick();testMap.onbounds_changed()");
        assertEquals("경계 장소",script(engine,"document.getElementById('detail-title').textContent"));
        assertEquals(true,script(engine,"pin.image===pin.activeImage && (function(){var raw=testMap.getProjection().containerPointFromCoords(edge);return raw.x>0 && raw.x<canvas.clientWidth;})()"));
    }

    @Test void resizingBufferedMapPreservesCenterAndZoomAndFitsVisibleArea() throws Exception {
        WebEngine engine=loadMap(850,720);
        script(engine,"""
            testMap.setCenter(new kakao.maps.LatLng(35.158,129.06));
            document.getElementById('map').style.width='400px';document.getElementById('map').style.height='280px';
            window.dispatchEvent(new Event('resize'));
            """);
        Thread.sleep(180);
        assertEquals(true,script(engine,"testMap.getCenter().getLat()===35.158 && testMap.getCenter().getLng()===129.06 && testMap.getLevel()===5"));
        assertEquals(true,script(engine,"(function(){var state=mapViewport.inspect(),point=mapViewport.projection().containerPointFromCoords(testMap.getCenter()),camera=inspectMapCamera();return state.width===400 && state.height===280 && state.renderWidth>400 && state.renderHeight>280 && camera.width===400 && camera.height===280 && Math.abs(point.x-200)<1 && Math.abs(point.y-140)<1;})()"));
    }

    @Test void cachedMapTilesCanBeCapturedWithoutANewTilesLoadedEvent() throws Exception {
        WebEngine engine=loadMap();
        fx(()->{
            engine.executeScript("""
                    [kakao.maps.Marker,kakao.maps.Circle,kakao.maps.Polyline,kakao.maps.CustomOverlay].forEach(function(type){
                        type.prototype.getMap=function(){return window.testMap;};
                    });
                    var canvas=document.createElement('canvas');canvas.width=canvas.height=256;
                    var tile=new Image();tile.src=canvas.toDataURL();document.getElementById('map').appendChild(tile);
                    window.beginCourseCapture({startLat:35.1578,startLng:129.06,places:[]});
                    """);
            return null;
        });
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(4);
        boolean captured=false;
        try {
            while(System.nanoTime()<deadline) {
                captured=fx(()->Boolean.TRUE.equals(engine.executeScript("window.courseCaptureReady()")));
                if(captured)break;Thread.sleep(50);
            }
            assertTrue(captured,"이미 캐시된 타일에는 새 tilesloaded 이벤트가 없어도 준비 판정이 필요합니다.");
            assertEquals(true,script(engine,"mapViewport.inspect().padX===0 && mapViewport.inspect().padY===0"));
            // An incomplete tile must still block capture.
            fx(()->{
                engine.executeScript("var pending=new Image();pending.src='data:image/png;base64,broken';document.getElementById('map').appendChild(pending)");
                assertEquals(false,engine.executeScript("window.courseCaptureReady()"));return null;
            });
        } finally {fx(()->{engine.executeScript("window.endCourseCapture()");return null;});}
        assertEquals(true,script(engine,"mapViewport.inspect().padX>0 && mapViewport.inspect().padY>0"));
    }

    @Test void closingTheWindowChecksUnsavedChangesAndBlocksAnActiveSave() throws Exception {
        fx(()->{
            var loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));Parent root=loader.load();new Scene(root,1200,800);
            CoursePlannerController main=loader.getController();stubPlannerMap(main,"mapView");stubPlannerMap(field(main,"routeEditorController"),"editMapView");
            assertTrue(main.confirmWindowClose());
            var plan=new com.datecourse.model.RoutePlan(35.1578,129.06,2,1000,"미저장 코스",java.util.List.of(
                    new com.datecourse.model.Place(1,"first","장소","MEAL",35.1578,129.06,9000,4)));
            callWith(main,"presentPlan",com.datecourse.model.RoutePlan.class,plan);
            var asked=new java.util.concurrent.atomic.AtomicInteger();
            setField(main,"closeConfirmation",(java.util.function.BooleanSupplier)()->{asked.incrementAndGet();return false;});
            assertFalse(main.confirmWindowClose());assertEquals(plan,field(main,"appliedPlan"));assertEquals(1,asked.get());
            setField(main,"closeConfirmation",(java.util.function.BooleanSupplier)()->true);
            assertTrue(main.confirmWindowClose());
            setField(main,"saving",true);assertFalse(main.confirmWindowClose());setField(main,"saving",false);
            setField(main,"lastSavedPlan",plan);assertTrue(main.confirmWindowClose());
            return null;
        });
    }

    @Test void mapPinsKeepDifferentPublicPlacesDistinctEvenWithASharedProviderId() throws Exception {
        WebEngine engine=loadMap();
        fx(()->{
            engine.executeScript("""
                    window.presentRoute({startLat:35.157,startLng:129.06,radius:1000,places:[
                        {recordId:100,placeId:'shared',identityKey:'source-a',name:'같은 이름',category:'MEAL',lat:35.157,lng:129.06,priceText:'5,000원/인'},
                        {recordId:101,placeId:'shared',identityKey:'source-b',name:'같은 이름',category:'MEAL',lat:35.157,lng:129.06,priceText:'5,000원/인'}
                    ]},null,500,true,false,[]);
                    window.openRoutePlace(101);
                    """);
            assertEquals("true",engine.executeScript("document.querySelector('[data-visit-id=\"source-b:1\"]').getAttribute('aria-expanded')"));
            return null;
        });
    }

    @Test void userNoticesKeepRecoveryActionsAndTechnicalSourceSettingsStartCollapsed() throws Exception {
        fx(()->{
            var loader=new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));Parent root=loader.load();new Scene(root,1100,760);
            CoursePlannerController main=loader.getController();stubPlannerMap(main,"mapView");stubPlannerMap(field(main,"routeEditorController"),"editMapView");
            assertFalse(((javafx.scene.control.TitledPane)loader.getNamespace().get("sourceSettings")).isExpanded());
            var feedback=(PageFeedback)field(main,"feedback");
            String partial="코스는 저장됐어요. 미리보기는 ‘저장한 코스’에서 다시 만들 수 있어요.";
            feedback.show(PageFeedback.Kind.INFO,partial);
            assertEquals(partial,((javafx.scene.control.Label)field(feedback,"message")).getText());
            assertTrue(((javafx.scene.layout.VBox)field(feedback,"toast")).getStyleClass().contains("feedback-info"));
            var finish=feedback.beginLoading("지도를 준비하고 있어요…");feedback.updateLoading("미리보기를 만들고 있어요…");
            assertEquals("미리보기를 만들고 있어요…",((javafx.scene.control.Label)field(feedback,"busyMessage")).getText());
            finish.run();assertFalse(feedback.isBusy());
            root.resize(1100,760);root.applyCss();root.layout();
            Path file=Path.of("build/ui-preview/user-first-feedback.png");Files.createDirectories(file.getParent());
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",file.toFile());return null;
        });
    }

    private WebEngine loadMap() throws Exception {
        return loadMap(420, 500);
    }

    private WebView previewView;
    private WebEngine loadMap(int width, int height) throws Exception {
        return loadMap(width, height, true);
    }

    private WebEngine loadMap(int width, int height, boolean customZoom) throws Exception {
        String html;
        String js;
        try (var page = getClass().getResourceAsStream("/map/map.html");
             var source = getClass().getResourceAsStream("/map/map.js")) {
            html = new String(page.readAllBytes(), StandardCharsets.UTF_8);
            js = new String(source.readAllBytes(), StandardCharsets.UTF_8);
            // Keep explicit coverage of the fallback SDK mode; the fractional test installs its own handler.
            js = js.replace("const USE_FRACTIONAL_ZOOM = true;", "const USE_FRACTIONAL_ZOOM = false;");
            if (customZoom) js = js.replace("const USE_INSTANT_ZOOM = false;", "const USE_INSTANT_ZOOM = true;");
            else js = js.replace("const USE_INSTANT_ZOOM = true;", "const USE_INSTANT_ZOOM = false;");
        }
        // Exercise the real page in JavaFX WebKit without credentials or network requests.
        String mock = """
            window.KAKAO_MAP_KEY = 'test';
            window.requests = [];
            window.categoryRequests = [];
            window.addressRequests = [];
            window.bridgeSelections = [];
            window.externalLinks = [];
            window.routeLines = [];
            window.routeOverlays = [];
            window.pins = [];
            window.mapPans = [];
            window.zoomCalls = [];
            window.nativeWheelEvents = 0;
            window.javaMapBridge = {
                onMapReady: function () {}, onMapError: function () {},
                openExternalLink: function (address) { externalLinks.push(address); },
                onLocationSelected: function (lat, lng) { bridgeSelections.push([lat, lng]); }
            };
            function Position(lat, lng) { this.getLat = function () { return lat; }; this.getLng = function () { return lng; }; }
            function Shape(options) {
                this.options = options; window.pins.push(this);
                this.setPosition = function (position) { this.position = position; };
                this.setRadius = function (radius) { this.radius = radius; };
                this.setMap = function (map) { this.attached = map !== null; };
                this.setImage = function (image) { this.image = image; };
                this.setZIndex = function (z) { this.zIndex = z; };
                this.setOptions = function (options) { Object.assign(this.options,options); };
            }
            window.kakao = { maps: {
                LatLng: Position, Marker: Shape, Circle: Shape,
                LatLngBounds: function () { this.points=[];this.extend=function(p){this.points.push([p.getLat(),p.getLng()]);}; },
                Size: function (w, h) { this.width = w; this.height = h; },
                Point: function (x, y) { this.x = x; this.y = y; },
                MarkerImage: function (src, size, options) { this.src = src; this.size = size; this.options = options; },
                Polyline: function (options) {
                    this.options = options;
                    this.setMap = function (map) { this.attached = map !== null; };
                    this.setOptions = function (value) { Object.assign(this.options, value); };
                    this.setZIndex = function (value) { this.zIndex = value; };
                    routeLines.push(this);
                },
                CustomOverlay: function (options) {
                    this.options = options;
                    this.setMap = function (map) { this.attached = map !== null; };
                    this.setPosition = function (position) { this.position = position; };
                    routeOverlays.push(this);
                },
                Map: function (element, options) {
                    window.testMap = this;
                    window.testMapOptions = options;
                    var level = options.level;
                    this.getLevel = function () { return level; };
                    this.setLevel = function (value, settings) { level = value; zoomCalls.push({ level:value, settings:settings }); };
                    element.addEventListener('wheel', function () { nativeWheelEvents++; });
                    var center = options.center;
                    this.getCenter = function () { return center; };
                    this.setCenter = function (p) { center = p; window.lastCenter = [p.getLat(), p.getLng()]; };
                    this.panTo = function (p) { center = p; window.mapPans.push([p.getLat(),p.getLng()]); if(this.onidle)this.onidle(); };
                    this.getProjection = function () { return {
                        containerPointFromCoords: function(p) { return {x:(p.getLng()-center.getLng())*100000+element.clientWidth/2,y:(center.getLat()-p.getLat())*100000+element.clientHeight/2}; },
                        coordsFromContainerPoint: function(p) { return new Position(center.getLat()-(p.y-element.clientHeight/2)/100000,center.getLng()+(p.x-element.clientWidth/2)/100000); }
                    }; };
                    this.relayout = function () {};
                    this.setBounds = function(bounds,top,right,bottom,left){window.lastBounds=bounds.points;window.boundsPadding=[top,right,bottom,left];};
                },
                event: {
                    addListener: function (target, name, callback) { target['on' + name] = callback; },
                    removeListener: function (target, name, callback) { if (target['on' + name] === callback) delete target['on' + name]; }
                },
                services: {
                    SortBy: { DISTANCE: 'distance' },
                    Status: { OK: 'OK', ZERO_RESULT: 'ZERO_RESULT', ERROR: 'ERROR' },
                    Geocoder: function () { this.coord2Address = function (lng, lat, callback) { addressRequests.push({ lat: lat, lng: lng, callback: callback }); }; },
                    Places: function () {
                        this.keywordSearch = function (query, callback, options) { requests.push({ query: query, callback: callback, options: options }); };
                        this.categorySearch = function (category, callback, options) { categoryRequests.push({ category: category, callback: callback, options: options }); };
                    }
                },
                load: function (callback) { callback(); }
            }};
            const append = document.head.appendChild.bind(document.head);
            document.head.appendChild = function (node) {
                if (node.tagName === 'SCRIPT') { setTimeout(function () { node.onload(); }, 0); return node; }
                return append(node);
            };
            window.submitQuery = function (query) {
                document.getElementById('place-query').value = query;
                document.getElementById('search-form').dispatchEvent(new Event('submit', { cancelable: true }));
            };
            """;
        html = html.replace("<script src=\"./kakao-config.js\"></script>", "<script>" + mock + "</script>")
                .replace("<script src=\"./map.js\"></script>", "<script>" + js + "</script>");
        try (var railJs = getClass().getResourceAsStream("/map/itinerary-rail.js");
             var railCss = getClass().getResourceAsStream("/map/itinerary-rail.css")) {
            html = html.replace("<script src=\"./itinerary-rail.js\"></script>", "<script>" + new String(railJs.readAllBytes(), StandardCharsets.UTF_8) + "</script>")
                    .replace("<link rel=\"stylesheet\" href=\"./itinerary-rail.css\">", "<style>" + new String(railCss.readAllBytes(), StandardCharsets.UTF_8) + "</style>");
        }
        try (var gesture = getClass().getResourceAsStream("/map/pin-pointer-gesture.js")) {
            html = html.replace("<script src=\"./pin-pointer-gesture.js\"></script>",
                    "<script>" + new String(gesture.readAllBytes(), StandardCharsets.UTF_8) + "</script>");
        }
        try (var viewport = getClass().getResourceAsStream("/map/map-viewport.js")) {
            html = html.replace("<script src=\"./map-viewport.js\"></script>",
                    "<script>" + new String(viewport.readAllBytes(), StandardCharsets.UTF_8) + "</script>");
        }
        try (var camera = getClass().getResourceAsStream("/map/camera-motion.js")) {
            html = html.replace("<script src=\"./camera-motion.js\"></script>",
                    "<script>" + new String(camera.readAllBytes(), StandardCharsets.UTF_8) + "</script>");
        }
        try (var instantZoom = getClass().getResourceAsStream("/map/instant-zoom.js")) {
            html = html.replace("<script src=\"./instant-zoom.js\"></script>",
                    "<script>" + new String(instantZoom.readAllBytes(), StandardCharsets.UTF_8) + "</script>");
        }
        String content = html;
        CountDownLatch loaded = new CountDownLatch(1);
        FutureTask<WebEngine> create = new FutureTask<>(() -> {
            WebView view = new WebView();
            previewView = view;
            StackPane root = new StackPane(view);
            new Scene(root, width, height);
            root.resize(width, height);
            root.applyCss();
            root.layout();
            view.resize(width, height);
            view.snapshot(null, null);
            WebEngine engine = view.getEngine();
            engine.getLoadWorker().stateProperty().addListener((o, previous, state) -> {
                if (state == Worker.State.SUCCEEDED) loaded.countDown();
            });
            engine.loadContent(content);
            return engine;
        });
        Platform.runLater(create);
        WebEngine engine = create.get(10, TimeUnit.SECONDS);
        assertTrue(loaded.await(10, TimeUnit.SECONDS));
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while ((Boolean) script(engine, "document.getElementById('search-submit').disabled")) {
            assertTrue(System.nanoTime() < deadline, "Search SDK initialization failed");
            Thread.sleep(20);
        }
        FutureTask<Void> connectPolicy = new FutureTask<>(() -> {
            ((netscape.javascript.JSObject)engine.executeScript("window")).setMember("cameraPolicyBridge",cameraPolicyBridge);
            engine.executeScript("javaMapBridge.decideCameraTransition=function(i,l,f,d,o,r){return cameraPolicyBridge.decideCameraTransition(i,l,f,d,o,r);}");
            return null;
        });
        Platform.runLater(connectPolicy);connectPolicy.get(10,TimeUnit.SECONDS);
        return engine;
    }

    @Test
    void plannerTabsShareDetailsAndKeepRecommendedRouteWhileBrowsingNearby() throws Exception {
        WebEngine engine=loadMap(1100,720);
        seedItinerary(engine);
        script(engine,"""
            window.extraPlace={recordId:104,placeId:'CAFE_2',name:'주변 카페',category:'CAFE',lat:35.1595,lng:129.0615,price:4000,rating:4.2};
            window.updatePlannerPlaces(testCourse,testCourse.concat([extraPlace]));
            """);
        assertEquals("true",script(engine,"document.getElementById('course-tab').getAttribute('aria-selected')"));
        script(engine,"document.getElementById('nearby-tab').click()");
        assertEquals(4,((Number)script(engine,"document.querySelectorAll('.mini-place-card').length")).intValue());
        script(engine,"document.querySelectorAll('.mini-place-card')[3].click()");
        assertEquals(0,((Number)script(engine,"mapPans.length")).intValue());
        assertEquals(4,((Number)script(engine,"pins.filter(function(p){return p.visitId && p.attached;}).length")).intValue());
        assertEquals(0,((Number)script(engine,"routeLines.length")).intValue());
        assertEquals(true,script(engine,"routeLines.every(function(p){return p.attached;})"));
        script(engine,"document.querySelectorAll('.mini-place-card')[3].click()");
        assertEquals("주변 카페",script(engine,"document.getElementById('detail-title').textContent"));
        script(engine,"document.getElementById('course-tab').click()");
        Thread.sleep(250);
        assertEquals(3,((Number)script(engine,"document.querySelectorAll('.mini-place-card').length")).intValue());
        assertEquals(true,script(engine,"document.getElementById('place-detail').hidden"));
        assertEquals(0,((Number)script(engine,"routeLines.length")).intValue());
    }

    @Test
    void plannerEmptyStatesAndRadiusEmphasisFollowEditingAndExploration() throws Exception {
        WebEngine engine=loadMap(1100,720);
        script(engine,"window.updatePlannerPlaces([],[]);window.setPlanningMode(true)");
        assertEquals(false,script(engine,"document.getElementById('itinerary-rail').hidden"));
        assertTrue(((String)script(engine,"document.getElementById('itinerary-empty').textContent")).contains("출발 위치"));
        script(engine,"document.getElementById('course-tab').click()");
        assertTrue(((String)script(engine,"document.getElementById('itinerary-empty').textContent")).contains("아직 추천 코스"));
        assertEquals(0.25,((Number)script(engine,"pins.find(function(p){return p.options && p.options.fillOpacity===0 && p.options.strokeColor==='#2563eb';}).options.strokeOpacity")).doubleValue());
        script(engine,"window.setPlanningMode(true)");
        assertEquals(0.85,((Number)script(engine,"pins.find(function(p){return p.options && p.options.fillOpacity===0 && p.options.strokeColor==='#2563eb';}).options.strokeOpacity")).doubleValue());
    }

    @Test
    void gentleWheelZoomPreservesPointerAnchorAndCourseObjects() throws Exception {
        WebEngine engine=loadMap(1100,720);
        seedItinerary(engine);
        script(engine,"""
            window.mapElement=document.getElementById('map');window.bounds=mapElement.getBoundingClientRect();
            window.oldPinCount=pins.length;window.oldLines=routeLines.length;
            window.expectedAnchor=window.mapViewport.projection().coordsFromContainerPoint(new kakao.maps.Point(100,120));
            window.sendWheel=function(delta){mapElement.dispatchEvent(new WheelEvent('wheel',{deltaY:delta,clientX:bounds.left+100,clientY:bounds.top+120,bubbles:true,cancelable:true}));};
            sendWheel(-120);
            """);
        assertEquals(4,((Number)script(engine,"testMap.getLevel()")).intValue());
        assertEquals(0,((Number)script(engine,"mapElement.getAnimations().length")).intValue());
        assertEquals(true,script(engine,"zoomCalls[0].settings.animate.duration===300 && zoomCalls[0].settings.anchor.getLat()===expectedAnchor.getLat() && zoomCalls[0].settings.anchor.getLng()===expectedAnchor.getLng()"));
        assertEquals(true,script(engine,"mapElement.style.transform==='' && !mapElement.dataset.zoomScale"));
        assertEquals(true,script(engine,"nativeWheelEvents===0 && testMapOptions.disableDoubleClickZoom && pins.length===oldPinCount && routeLines.length===oldLines"));
        assertEquals("[[35.157,129.059]]",script(engine,"JSON.stringify(bridgeSelections)"));
        assertEquals("true",script(engine,"document.querySelector('.mini-place-card').getAttribute('aria-selected')"));
    }

    @Test
    void defaultMapLeavesZoomInputToSdkWithoutScalingPageOrMap() throws Exception {
        WebEngine engine = loadMap(1100, 720, false);
        assertEquals(false, script(engine, "testMapOptions.disableDoubleClickZoom"));
        assertEquals(true, script(engine, "typeof window.disposeMapZoom==='undefined' && typeof window.resetMapZoom==='undefined'"));
        assertEquals(true, script(engine, """
            (function(){
                var element=document.getElementById('map-canvas');
                var wheel=new WheelEvent('wheel',{deltaY:-120,bubbles:true,cancelable:true});
                element.dispatchEvent(wheel);
                var dbl=new MouseEvent('dblclick',{bubbles:true,cancelable:true});
                element.dispatchEvent(dbl);
                return !wheel.defaultPrevented && !dbl.defaultPrevented && nativeWheelEvents===1
                    && element.style.transform==='' && !element.dataset.zoomScale;
            })()
            """));
    }

    @Test
    void emptyAppliedCourseRemovesOldPlacePinsAndRouteLines() throws Exception {
        WebEngine engine=loadMap(1100,720);
        script(engine,"""
            window.first={recordId:1,placeId:'a',name:'A',category:'CAFE',lat:35.158,lng:129.06,price:3000,rating:4};
            presentRoute({startLat:35.157,startLng:129.06,radius:500,people:2,places:[first]},null,500,false,false,[]);
            window.oldCoursePins=pins.filter(function(p){return p.coursePin;});
            window.oldRouteLines=routeLines.slice();
            presentRoute({startLat:35.157,startLng:129.06,radius:500,people:2,places:[]},null,500,false,false,[]);
            """);
        assertEquals(true,script(engine,"oldCoursePins.length>0 && oldCoursePins.every(function(p){return !p.attached;})"));
        assertEquals(true,script(engine,"oldRouteLines.length===0 && routeLines.length===0"));
        assertEquals(true,script(engine,"document.getElementById('place-detail').hidden && !pins.some(c=>c.attached && c.options.strokeColor==='#15803d')"));
    }

    @Test
    void radiusEditsFitActiveCircleAndOverviewStillShowsBothRanges() throws Exception {
        WebEngine engine=loadMap(1100,720);
        script(engine,"""
            window.mapElement=document.getElementById('map');
            window.mapViewport.setBuffered(false);testMap.relayout();testMap.getProjection=function(){
                var unit=100000*Math.pow(2,5-testMap.getLevel());
                return {containerPointFromCoords:function(p){var c=testMap.getCenter();return {
                    x:(p.getLng()-c.getLng())*unit+mapElement.clientWidth/2,
                    y:(c.getLat()-p.getLat())*unit+mapElement.clientHeight/2};}};
            };
            window.plan={startLat:35.157,startLng:129.06,radius:200,places:[]};
            presentRoute(plan,null,200,true,false,[]);
            testMap.setCenter(new kakao.maps.LatLng(plan.startLat,plan.startLng));
            zoomCalls=[];
            ensureRouteRangesVisible();
            """);
        assertEquals(0,((Number)script(engine,"zoomCalls.length")).intValue());
        script(engine,"presentRoute(plan,null,2000,true,false,[]);ensureRouteRangesVisible()");
        assertTrue(((Number)script(engine,"testMap.getLevel()")).intValue()>5);
        assertEquals(true,script(engine,"testMap.getCenter().getLat()===plan.startLat && testMap.getCenter().getLng()===plan.startLng"));
        int calls=((Number)script(engine,"zoomCalls.length")).intValue();
        script(engine,"ensureRouteRangesVisible()");
        assertEquals(calls,((Number)script(engine,"zoomCalls.length")).intValue());
        script(engine,"window.expandedLevel=testMap.getLevel();presentRoute(plan,null,200,true,false,[]);ensureRouteRangesVisible()");
        assertEquals(true,script(engine,"testMap.getLevel()<expandedLevel && testMap.getLevel()===5"));
        calls=((Number)script(engine,"zoomCalls.length")).intValue();
        script(engine,"ensureRouteRangesVisible()");
        assertEquals(calls,((Number)script(engine,"zoomCalls.length")).intValue());
        // Adjusting the local circle fits it even if the global circle is larger; overview remains separate.
        script(engine,"plan.radius=2000;presentRoute(plan,null,200,true,false,[]);fitRouteView(false);window.globalLevel=testMap.getLevel();zoomCalls=[];fitRouteView(false)");
        assertEquals(0,((Number)script(engine,"zoomCalls.length")).intValue());
        assertEquals(true,script(engine,"testMap.getLevel()===globalLevel && globalLevel>5"));
        script(engine,"plan.radius=200");
        script(engine,"testMap.setLevel(5,{});plan.places=[{recordId:1,name:'멀리 있는 장소',lat:35.164,lng:129.06}];presentRoute(plan,null,200,true,false,[]);zoomCalls=[];ensureRouteRangesVisible()");
        assertEquals(true,script(engine,"testMap.getCenter().getLat()===plan.places[0].lat && testMap.getLevel()===5"));
    }

    @Test
    void fractionalMapOnlyZoomKeepsPointerAndControlsAcrossSdkBoundary() throws Exception {
        WebEngine engine = loadMap(1100, 720);
        seedItinerary(engine);
        FutureTask<Void> setup = new FutureTask<>(() -> {
            engine.executeScript("""
                disposeMapZoom();
                window.controlIds=['place-search','range-tooltip','map-status','place-detail'];
                window.controlSizes=controlIds.map(function(id){var r=document.getElementById(id).getBoundingClientRect();return [r.width,r.height];});
                window.mapViewport.setBuffered(false);testMap.relayout();testMap.getProjection=function(){
                    var unit=100000*Math.pow(2,5-testMap.getLevel());
                    return {
                        containerPointFromCoords:function(p){var c=testMap.getCenter();return {x:(p.getLng()-c.getLng())*unit+mapElement.clientWidth/2,y:(c.getLat()-p.getLat())*unit+mapElement.clientHeight/2};},
                        coordsFromContainerPoint:function(p){var c=testMap.getCenter();return new kakao.maps.LatLng(c.getLat()-(p.y-mapElement.clientHeight/2)/unit,c.getLng()+(p.x-mapElement.clientWidth/2)/unit);}
                    };
                };
                window.mapElement=document.getElementById('map');
                window.originalAnchor=window.mapViewport.projection().coordsFromContainerPoint(new kakao.maps.Point(100,120));
                window.coursePins=pins.length;
                disposeMapZoom=installInstantZoom(mapElement,testMap,{fractional:true});
                window.microWheel=function(delta){var b=mapElement.getBoundingClientRect();mapElement.dispatchEvent(new WheelEvent('wheel',{deltaY:delta,clientX:b.left+100,clientY:b.top+120,bubbles:true,cancelable:true}));};
                window.anchorError=function(){var p=window.mapViewport.projection().containerPointFromCoords(originalAnchor);var b=mapElement.getBoundingClientRect();return Math.abs(p.x*b.width/mapElement.clientWidth-100)+Math.abs(p.y*b.height/mapElement.clientHeight-120);};
                microWheel(-120);
                """);
            assertEquals(5, ((Number) engine.executeScript("testMap.getLevel()")).intValue());
            assertEquals(Math.pow(2, .1), ((Number) engine.executeScript("Number(mapElement.dataset.zoomScale)")).doubleValue(), 1e-8);
            assertTrue(((Number) engine.executeScript("anchorError()")).doubleValue() < .01);
            engine.executeScript("for(var i=0;i<11;i++)microWheel(-120)");
            assertEquals(4, ((Number) engine.executeScript("testMap.getLevel()")).intValue());
            assertTrue(((Number) engine.executeScript("anchorError()")).doubleValue() < .01);
            assertEquals(true, engine.executeScript("controlIds.every(function(id,i){var r=document.getElementById(id).getBoundingClientRect();return r.width===controlSizes[i][0] && r.height===controlSizes[i][1];})"));
            assertEquals(true, engine.executeScript("pins.length===coursePins && nativeWheelEvents===0"));
            engine.executeScript("for(var i=0;i<12;i++)microWheel(120)");
            assertEquals(1, ((Number) engine.executeScript("Number(mapElement.dataset.zoomScale)")).doubleValue(), 1e-8);
            assertTrue(((Number) engine.executeScript("anchorError()")).doubleValue() < .01);
            engine.executeScript("presentRoute({startLat:35.15,startLng:129.06,radiusMeter:500,places:testCourse},null,500,true,false,[]);microWheel(-120);fitRouteView(false)");
            assertEquals(1, ((Number) engine.executeScript("Number(mapElement.dataset.zoomScale)")).doubleValue(), 1e-8);
            engine.executeScript("testMap.setLevel(1,{});microWheel(-120)");
            assertEquals(1, ((Number) engine.executeScript("Number(mapElement.dataset.zoomScale)")).doubleValue(), 1e-8);
            assertEquals(1, ((Number) engine.executeScript("testMap.getLevel()")).intValue());
            engine.executeScript("testMap.setLevel(14,{});microWheel(120)");
            assertEquals(1, ((Number) engine.executeScript("Number(mapElement.dataset.zoomScale)")).doubleValue(), 1e-8);
            assertEquals(14, ((Number) engine.executeScript("testMap.getLevel()")).intValue());
            engine.executeScript("microWheel(-120)");
            return null;
        });
        Platform.runLater(setup);
        setup.get(15, TimeUnit.SECONDS);
        Thread.sleep(200); // Let WebKit resize and its delayed map relayout complete.
        assertEquals(Math.pow(2, .1), ((Number) script(engine, "Number(mapElement.dataset.zoomScale)")).doubleValue(), 1e-8);
    }

    @Test
    void gentleZoomGroupsTrackpadBurstsAndClampsDoubleClicksToMapLevels() throws Exception {
        WebEngine engine=loadMap(1100,720);
        script(engine,"""
            window.mapElement=document.getElementById('map');window.bounds=mapElement.getBoundingClientRect();
            window.sendWheel=function(delta){mapElement.dispatchEvent(new WheelEvent('wheel',{deltaY:delta,clientX:bounds.left+100,clientY:bounds.top+120,bubbles:true,cancelable:true}));};
            sendWheel(-10);sendWheel(-10);
            """);
        assertEquals(0,((Number)script(engine,"zoomCalls.length")).intValue());
        script(engine,"for(var i=0;i<36;i++)sendWheel(-30)");
        Thread.sleep(360);
        assertEquals(2,((Number)script(engine,"zoomCalls.length")).intValue());
        assertEquals(3,((Number)script(engine,"testMap.getLevel()")).intValue());
        script(engine,"""
            window.doubleZoom=function(shift){mapElement.dispatchEvent(new MouseEvent('dblclick',{clientX:bounds.left+120,clientY:bounds.top+120,shiftKey:shift,bubbles:true,cancelable:true}));};
            testMap.setLevel(1,{});window.beforeLimit=zoomCalls.length;doubleZoom(false);
            """);
        assertEquals(true,script(engine,"zoomCalls.length===beforeLimit && testMap.getLevel()===1"));
        script(engine,"testMap.setLevel(14,{});window.beforeLimit=zoomCalls.length;doubleZoom(true)");
        assertEquals(true,script(engine,"zoomCalls.length===beforeLimit && testMap.getLevel()===14"));
        Thread.sleep(340);
        script(engine,"doubleZoom(false)");
        assertEquals(true,script(engine,"testMap.getLevel()===13 && zoomCalls[zoomCalls.length-1].settings.animate.duration===300"));
    }

    @Test
    void detailLinkResolvesToActualPlaceAndCachesMatchingLookup() throws Exception {
        WebEngine engine = loadMap(1100,720);
        seedItinerary(engine);
        script(engine, "document.querySelector('.mini-place-card').click()");
        assertEquals("식당 테스트", script(engine,"requests[0].query"));
        assertEquals(500, ((Number)script(engine,"requests[0].options.radius")).intValue());
        script(engine,"""
            requests[0].callback([
                {place_name:'식당 테스트',y:'35.18',x:'129.08',place_url:'https://place.map.kakao.com/999'},
                {place_name:'식당 테스트',y:'35.158',x:'129.06',place_url:'http://place.map.kakao.com/12345'}
            ],'OK');
            """);
        Thread.sleep(100);
        assertEquals("https://place.map.kakao.com/12345",script(engine,"document.getElementById('detail-map-link').href"));
        script(engine,"document.getElementById('detail-map-link').click()");
        assertEquals("https://place.map.kakao.com/12345",script(engine,"externalLinks[0]"));
        script(engine,"document.querySelector('.mini-place-card').click();document.querySelector('.mini-place-card').click()");
        Thread.sleep(100);
        assertEquals(1,((Number)script(engine,"requests.length")).intValue());
        assertEquals("https://place.map.kakao.com/12345",script(engine,"document.getElementById('detail-map-link').href"));
    }

    @Test
    void outdatedPlaceLookupsCannotChangeAnotherPlacesLinkAndWrongBranchesFallBackToSearch() throws Exception {
        WebEngine engine=loadMap(1100,720);
        seedItinerary(engine);
        script(engine,"""
            document.querySelector('.mini-place-card').click();
            document.querySelectorAll('.mini-place-card')[1].click();
            document.querySelectorAll('.mini-place-card')[1].click();
            requests[0].callback([{place_name:'식당 테스트',y:'35.158',x:'129.06',place_url:'https://place.map.kakao.com/12345'}],'OK');
            requests[1].callback([{place_name:'카페 테스트 다른지점',y:'35.159',x:'129.061',place_url:'https://place.map.kakao.com/67890'}],'OK');
            categoryRequests[0].callback([], 'ZERO_RESULT');
            """);
        Thread.sleep(100);
        assertEquals("https://map.kakao.com/link/search/"+java.net.URLEncoder.encode("부산 카페 테스트",StandardCharsets.UTF_8).replace("+","%20"),
                script(engine,"document.getElementById('detail-map-link').href"));
        assertTrue(((String)script(engine,"document.getElementById('detail-map-link').textContent")).contains("검색"));
        assertEquals("false",script(engine,"document.getElementById('detail-map-link').getAttribute('aria-busy')"));
    }

    @Test
    void abbreviatedCafeNamesCanResolveThroughNearbyCategoryLookup() throws Exception {
        WebEngine engine=loadMap(1100,720);
        seedItinerary(engine);
        script(engine,"""
            testCourse[1].name='컴포즈파크에비뉴';window.showResultPlaces(testCourse,true);
            document.querySelectorAll('.mini-place-card')[1].click();document.querySelectorAll('.mini-place-card')[1].click();
            requests[0].callback([], 'ZERO_RESULT');
            categoryRequests[0].callback([{place_name:'컴포즈커피 부산파크에비뉴점',y:'35.159',x:'129.061',place_url:'https://place.map.kakao.com/54321'}],'OK');
            """);
        Thread.sleep(100);
        assertEquals("CE7",script(engine,"categoryRequests[0].category"));
        assertEquals("https://place.map.kakao.com/54321",script(engine,"document.getElementById('detail-map-link').href"));
    }

    @Test
    void radiusBorderAndWhiteOutlineFollowLocationAndRadiusWithoutFillingMap() throws Exception {
        WebEngine engine=loadMap();
        script(engine,"window.selectMapLocation(35.157,129.059,'서면역');window.setMapRadius(1200)");
        assertEquals(true,script(engine,"""
            (function(){var circles=pins.filter(function(p){return p.options && p.options.fillOpacity===0;});
            return circles.length===2 && circles.every(function(p){return p.radius===1200 && p.position.getLat()===35.157 && p.attached;})
            && circles.some(function(p){return p.options.strokeColor==='#ffffff' && p.options.strokeWeight===6;})
            && circles.some(function(p){return p.options.strokeColor==='#2563eb' && p.options.strokeWeight===2.5;});})()
            """));
    }

    @Test
    void itineraryCardsSeparateSelectionFromDetailsAndKeepMapObjects() throws Exception {
        WebEngine engine = loadMap(1100, 720);
        seedItinerary(engine);
        assertEquals(3, ((Number) script(engine, "document.querySelectorAll('.mini-place-card').length")).intValue());
        assertEquals(true, script(engine, "document.getElementById('itinerary-viewport').clientWidth===document.getElementById('itinerary-viewport').offsetWidth"));
        assertEquals(0, ((Number) script(engine, "mapPans.length")).intValue());
        script(engine, "window.pinCount=pins.length; window.lineCount=routeLines.length; document.querySelectorAll('.mini-place-card')[1].click()");
        Thread.sleep(350);
        assertEquals("true", script(engine, "document.querySelectorAll('.mini-place-card')[1].getAttribute('aria-selected')"));
        assertEquals(true, script(engine, "document.getElementById('place-detail').hidden"));
        assertEquals(0, ((Number) script(engine, "mapPans.length")).intValue());
        assertEquals(true, script(engine, "Math.abs(document.querySelectorAll('.mini-place-card')[1].parentElement.getBoundingClientRect().top + 39 - (document.getElementById('itinerary-viewport').getBoundingClientRect().top + document.getElementById('itinerary-viewport').clientHeight/2)) < 3"));
        script(engine, "window.railBefore=document.getElementById('itinerary-rail').getBoundingClientRect().toJSON(); document.querySelectorAll('.mini-place-card')[1].click()");
        Thread.sleep(250);
        assertEquals("카페 테스트", script(engine, "document.getElementById('detail-title').textContent"));
        assertEquals(1, ((Number) script(engine, "mapPans.length")).intValue());
        assertEquals(true, script(engine, """
            (function(){var map=document.getElementById('map').getBoundingClientRect(), detail=document.getElementById('place-detail').getBoundingClientRect();
            var p=window.mapViewport.projection().containerPointFromCoords(new kakao.maps.LatLng(testCourse[1].lat,testCourse[1].lng));
            var rail=document.getElementById('itinerary-rail').getBoundingClientRect();
            return p.x+map.left>detail.right+15 && p.x+map.left<rail.left && detail.left>map.left && detail.left<map.left+30 && detail.right<rail.left && rail.width===railBefore.width && rail.left===railBefore.left;})()
            """));
        assertEquals(true, script(engine, "pins.filter(function(p){return p.visitId && p.zIndex===36;}).length===1"));
        script(engine, "window.showResultPlaces(testCourse,true)");
        assertEquals(true, script(engine, "pins.length===pinCount && routeLines.length===lineCount"));
        script(engine, "document.getElementById('detail-close').click()");
        Thread.sleep(250);
        assertEquals(true, script(engine, "document.getElementById('place-detail').hidden"));
        assertEquals(1, ((Number) script(engine, "mapPans.length")).intValue());
    }

    private void seedItinerary(WebEngine engine) throws Exception {
        script(engine, """
            window.selectMapLocation(35.157,129.059,'서면역');
            window.testCourse = [
                {recordId:101,placeId:'MEAL_1',name:'식당 테스트',category:'MEAL',lat:35.158,lng:129.06,price:10000,rating:4.3},
                {recordId:102,placeId:'CAFE_1',name:'카페 테스트',category:'CAFE',lat:35.159,lng:129.061,price:5000,rating:4.5},
                {recordId:103,placeId:'CULTURE_1',name:'문화 공간 테스트',category:'CULTURE',lat:35.16,lng:129.062,price:0,rating:0}
            ];
            window.showResultPlaces(testCourse,true);
            """);
        Thread.sleep(80);
        script(engine,"testMap.setLevel(5,{});zoomCalls=[];mapPans=[];");
    }

    @Test
    void itineraryPinClicksScrollAndUserScrollingDoesNotPan() throws Exception {
        WebEngine engine = loadMap(1100, 720);
        seedItinerary(engine);
        script(engine, "pins.filter(function(p){return p.visitId;})[2].onclick()");
        Thread.sleep(450);
        assertEquals("문화 공간 테스트", script(engine, "document.getElementById('detail-title').textContent"));
        assertEquals("true", script(engine, "document.querySelectorAll('.mini-place-card')[2].getAttribute('aria-selected')"));
        assertEquals(1, ((Number) script(engine, "mapPans.length")).intValue());
        script(engine, """
            var viewport=document.getElementById('itinerary-viewport');
            viewport.dispatchEvent(new Event('wheel'));
            viewport.scrollTop=0;
            viewport.dispatchEvent(new Event('scroll'));
            viewport.scrollTop=86;
            viewport.dispatchEvent(new Event('scroll'));
            viewport.scrollTop=0;
            viewport.dispatchEvent(new Event('scroll'));
            """);
        Thread.sleep(650);
        assertEquals("true", script(engine, "document.querySelectorAll('.mini-place-card')[0].getAttribute('aria-selected')"));
        assertEquals(true, script(engine, "document.getElementById('place-detail').hidden"));
        assertEquals(1, ((Number) script(engine, "mapPans.length")).intValue());
        assertEquals(0, ((Number) script(engine, "routeLines.length")).intValue());
    }

    @Test
    void itineraryCourseReplacementCancelsAnimationsAndUsesUniqueVisitIds() throws Exception {
        WebEngine engine = loadMap(1100, 720);
        seedItinerary(engine);
        script(engine, """
            pins.filter(function(p){return p.visitId;})[2].onclick();
            window.oldPin=pins.filter(function(p){return p.visitId;})[2];
            window.showResultPlaces([testCourse[0],testCourse[0]],true);
            """);
        Thread.sleep(500);
        assertEquals(2, ((Number) script(engine, "document.querySelectorAll('.mini-place-card').length")).intValue());
        assertEquals(true, script(engine, "document.querySelectorAll('.mini-place-card')[0].dataset.visitId!==document.querySelectorAll('.mini-place-card')[1].dataset.visitId"));
        assertEquals(true, script(engine, "document.getElementById('place-detail').hidden && !oldPin.onclick"));
        assertEquals("true", script(engine, "document.querySelectorAll('.mini-place-card')[0].getAttribute('aria-selected')"));
        script(engine, "window.showResultPlaces([],false)");
        Thread.sleep(250);
        assertEquals(true, script(engine, "document.getElementById('itinerary-rail').hidden && document.getElementById('place-detail').hidden"));
    }

    @Test
    void itineraryKeyboardAndNarrowSheetStayInsideMap() throws Exception {
        WebEngine engine = loadMap(420, 680);
        seedItinerary(engine);
        script(engine, "document.querySelector('.mini-place-card').dispatchEvent(new KeyboardEvent('keydown',{key:'End',bubbles:true}));");
        Thread.sleep(350);
        assertEquals("true", script(engine, "document.querySelectorAll('.mini-place-card')[2].getAttribute('aria-selected')"));
        script(engine, "document.querySelectorAll('.mini-place-card')[2].click()");
        Thread.sleep(250);
        assertEquals(true, script(engine, """
            (function(){var map=document.getElementById('map').getBoundingClientRect(),detail=document.getElementById('place-detail').getBoundingClientRect(),rail=document.getElementById('itinerary-rail').getBoundingClientRect();
            return detail.left>=map.left && detail.right<=map.right && detail.bottom<=map.bottom && detail.top>=rail.bottom;})()
            """));
        assertEquals(false, script(engine, "document.getElementById('detail-facts').textContent.includes('평점')"));
        assertEquals(2, ((Number) script(engine, "document.getElementById('detail-facts').children.length")).intValue());
        assertTrue(((String)script(engine,"document.getElementById('detail-extra').textContent")).contains("확인 전"));
        script(engine, "document.getElementById('detail-close').dispatchEvent(new KeyboardEvent('keydown',{key:'Escape',bubbles:true}))");
        Thread.sleep(250);
        assertEquals(true, script(engine, "document.getElementById('place-detail').hidden && document.activeElement===document.querySelectorAll('.mini-place-card')[2]"));
    }

    @Test
    void textChangesSearchAutomaticallyAndWhitespaceCancelsPendingResults() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
            window.typeQuery = function (text) {
                document.getElementById('place-query').value = text;
                document.getElementById('place-query').dispatchEvent(new Event('input'));
            };
            typeQuery('서'); typeQuery('서면역');
            """);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (((Number) script(engine, "requests.length")).intValue() == 0) {
            assertTrue(System.nanoTime() < deadline, "Automatic search did not start");
            Thread.sleep(20);
        }
        assertEquals(1, ((Number) script(engine, "requests.length")).intValue());
        assertEquals("부산 서면역", script(engine, "requests[0].query"));
        script(engine, """
            typeQuery('부산역'); typeQuery('   ');
            requests[0].callback([{place_name:'서면역',address_name:'부산 부산진구',y:'35.157',x:'129.059'}], 'OK');
            """);
        Thread.sleep(450);
        assertEquals(1, ((Number) script(engine, "requests.length")).intValue());
        assertEquals(0, ((Number) script(engine, "document.querySelectorAll('#search-results button').length")).intValue());
        assertEquals("", script(engine, "document.getElementById('search-message').textContent"));
    }

    @Test
    void busanResultsSelectMapPositionAndNotifyJava() throws Exception {
        WebEngine engine = loadMap();
        script(engine, "submitQuery('서면역')");
        assertEquals("부산 서면역", script(engine, "requests[0].query"));
        script(engine, """
            requests[0].callback([
                {place_name:'서면역', address_name:'부산 부산진구 부전동', y:'35.157', x:'129.059'},
                {place_name:'다른 지역', address_name:'서울 중구', y:'37.5', x:'127.0'}
            ], 'OK');
            """);
        assertEquals(1, ((Number) script(engine, "document.querySelectorAll('#search-results button').length")).intValue());
        script(engine, "document.querySelector('#search-results button').click()");
        assertEquals("[35.157,129.059]", script(engine, "JSON.stringify([testMap.getCenter().getLat(),testMap.getCenter().getLng()])"));
        assertEquals("[[35.157,129.059]]", script(engine, "JSON.stringify(bridgeSelections)"));
        assertEquals("서면역", script(engine, "document.getElementById('map-status').textContent"));
        script(engine, "window.setMapRadius(1000)");
        assertEquals("서면역", script(engine, "document.getElementById('map-status').textContent"));
        assertEquals(0, ((Number) script(engine, "document.querySelectorAll('#search-results button').length")).intValue());
    }

    @Test
    void outdatedResponsesCannotOverwriteLatestSearchAndErrorsKeepMapUsable() throws Exception {
        WebEngine engine = loadMap();
        script(engine, "submitQuery('서면역'); submitQuery('부산역'); requests[1].callback([], 'ZERO_RESULT');");
        String message = (String) script(engine, "document.getElementById('search-message').textContent");
        assertTrue(message.contains("검색 결과가 없어요"));
        script(engine, "requests[0].callback([], 'ERROR')");
        assertEquals(message, script(engine, "document.getElementById('search-message').textContent"));
        script(engine, "submitQuery('광안리'); requests[2].callback([], 'ERROR'); window.selectMapLocation(35.15, 129.12);");
        assertTrue(((String) script(engine, "document.getElementById('search-message').textContent")).contains("검색하지 못했어요"));
        assertEquals("[[35.15,129.12]]", script(engine, "JSON.stringify(bridgeSelections)"));
    }

    @Test
    void searchResultsOverlayMapWithoutResizingItAndCanBeDismissed() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
            window.mapBeforeSearch = document.getElementById('map').getBoundingClientRect().toJSON();
            submitQuery('서면역');
            requests[0].callback(Array.from({length:15}, function () {
                return {place_name:'서면역', address_name:'부산 부산진구 부전동', y:'35.157', x:'129.059'};
            }), 'OK');
            """);
        String geometry = (String) script(engine, "JSON.stringify(['place-search','map','map-status'].map(function(id){var r=document.getElementById(id).getBoundingClientRect();return {id:id,top:r.top,bottom:r.bottom,height:r.height};}))");
        assertEquals(true, script(engine, """
            (function () {
                const search = document.getElementById('place-search').getBoundingClientRect();
                const map = document.getElementById('map').getBoundingClientRect();
                const footer = document.getElementById('map-status').getBoundingClientRect();
                const popup = document.getElementById('search-popup').getBoundingClientRect();
                return search.bottom <= map.top && map.bottom <= footer.top && map.height > 200
                    && map.top === mapBeforeSearch.top && map.height === mapBeforeSearch.height
                    && map.width === mapBeforeSearch.width && popup.bottom > map.top;
            })()
            """), geometry);
        script(engine, "document.getElementById('search-close').click()");
        assertEquals(0, ((Number) script(engine, "document.querySelectorAll('#search-results button').length")).intValue());
        assertEquals("", script(engine, "document.getElementById('search-message').textContent"));
        assertEquals(true, script(engine, "document.getElementById('search-popup').hidden"));
    }

    @Test
    void searchArrowKeysNavigateResultsAndEnterSelectsWithoutAnotherSearch() throws Exception {
        WebEngine engine=loadMap();
        script(engine,"""
            submitQuery('서면');
            requests[0].callback(Array.from({length:15},function(_,i){return {place_name:'서면 '+i,address_name:'부산 부산진구',y:'35.157',x:'129.059'};}),'OK');
            window.queryField=document.getElementById('place-query');queryField.focus();
            window.searchKey=function(key,composing){document.activeElement.dispatchEvent(new KeyboardEvent('keydown',{key:key,isComposing:!!composing,bubbles:true,cancelable:true}));};
            searchKey('ArrowDown',true);
            """);
        assertEquals(null,script(engine,"queryField.getAttribute('aria-activedescendant')"));
        script(engine,"searchKey('ArrowUp')");
        assertEquals("서면 14",script(engine,"document.querySelector('#search-results [aria-selected=true] .place-name').textContent"));
        assertEquals(true,script(engine,"document.activeElement===document.querySelectorAll('#search-results button')[14] && document.getElementById('search-results').scrollTop>0"));
        script(engine,"searchKey('ArrowDown');searchKey('ArrowUp')");
        assertEquals("서면 13",script(engine,"document.querySelector('#search-results [aria-selected=true] .place-name').textContent"));
        script(engine,"searchKey('Enter')");
        assertEquals("서면 13",script(engine,"document.getElementById('map-status').textContent"));
        assertEquals("서면 13",script(engine,"queryField.value"));
        assertEquals(1,((Number)script(engine,"requests.length")).intValue());
        assertEquals("[[35.157,129.059]]",script(engine,"JSON.stringify(bridgeSelections)"));
        assertEquals(true,script(engine,"document.getElementById('search-popup').hidden && queryField.getAttribute('aria-expanded')==='false' && !queryField.hasAttribute('aria-activedescendant')"));
        script(engine,"submitQuery('부산역');requests[1].callback([{place_name:'부산역',address_name:'부산 동구',y:'35.115',x:'129.042'}],'OK');searchKey('ArrowDown');searchKey('Escape')");
        assertEquals(true,script(engine,"document.getElementById('search-popup').hidden"));
        assertEquals("[[35.157,129.059]]",script(engine,"JSON.stringify(bridgeSelections)"));
        script(engine,"submitQuery('서면');requests[2].callback([{place_name:'서면역',address_name:'부산 부산진구',y:'35.157',x:'129.059'}],'OK');searchKey('Down')");
        assertEquals(true,script(engine,"document.activeElement===document.querySelector('#search-results button')"));
        script(engine,"searchKey('Up')");
        assertEquals(true,script(engine,"document.activeElement===queryField && !queryField.hasAttribute('aria-activedescendant')"));
        script(engine,"queryField.dispatchEvent(new KeyboardEvent('keydown',{keyCode:40,bubbles:true,cancelable:true}))");
        assertEquals(true,script(engine,"document.activeElement===document.querySelector('#search-results button')"));
        script(engine,"searchKey('Escape')");
    }

    @Test
    void attributionLinksAndPopupsLeaveEmbeddedMapInPlace() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
            const link = document.createElement('a');
            link.href = 'https://map.kakao.com/';
            link.target = '_blank';
            link.textContent = 'Kakao';
            document.body.appendChild(link);
            link.click();
            window.open('https://map.kakao.com/link/map/35.1,129.1');
            """);
        assertEquals("[\"https://map.kakao.com/\",\"https://map.kakao.com/link/map/35.1,129.1\"]",
                script(engine, "JSON.stringify(externalLinks)"));
        assertEquals(true, script(engine, "document.getElementById('map') !== null"));
    }

    @Test
    void manualSelectionShowsAddressAndOldLookupsCannotReplaceNewPlaceName() throws Exception {
        WebEngine engine = loadMap();
        script(engine, "window.selectMapLocation(35.15,129.12)");
        assertEquals(129.12, ((Number) script(engine, "addressRequests[0].lng")).doubleValue());
        script(engine, "addressRequests[0].callback([{road_address:{address_name:'부산 수영구 광안해변로'}}], 'OK')");
        assertEquals("부산 수영구 광안해변로", script(engine, "document.getElementById('map-status').textContent"));
        script(engine, "window.selectMapLocation(35.16,129.13); window.selectMapLocation(35.157,129.059,'서면역'); addressRequests[1].callback([{address:{address_name:'이전 주소'}}], 'OK')");
        assertEquals("서면역", script(engine, "document.getElementById('map-status').textContent"));
        script(engine, "window.selectMapLocation(35.17,129.14); addressRequests[2].callback([], 'ZERO_RESULT')");
        assertEquals("선택한 위치", script(engine, "document.getElementById('map-status').textContent"));
    }

    @Test
    void databaseResultMarkersAndFocusPreserveStartingPoint() throws Exception {
        WebEngine engine = loadMap();
        script(engine, """
            window.resultPins = [];
            kakao.maps.Marker = function (options) {
                this.options = options;
                this.setMap = function (map) { this.attached = map !== null; };
                this.setImage = this.setZIndex = function () {};
                resultPins.push(this);
            };
            window.selectMapLocation(35.157, 129.059, '출발 위치');
            window.showResultPlaces([{lat:35.158,lng:129.06,name:'식당'}, {lat:35.159,lng:129.061,name:'카페'}], true);
            window.focusResultPlace(35.158,129.06);
            """);
        assertEquals(2, ((Number) script(engine, "resultPins.length")).intValue());
        assertEquals("[35.158,129.06]", script(engine, "JSON.stringify([testMap.getCenter().getLat(),testMap.getCenter().getLng()])"));
        assertEquals("[[35.157,129.059]]", script(engine, "JSON.stringify(bridgeSelections)"));
        assertEquals(0, ((Number) script(engine, "routeLines.length")).intValue());
        script(engine, "window.showResultPlaces([])");
        assertEquals(false, script(engine, "resultPins[0].attached || resultPins[1].attached"));
        script(engine, "window.showResultPlaces([{lat:35.158,lng:129.06,name:'주변 장소'}], false)");
        assertEquals(0, ((Number) script(engine, "routeLines.length")).intValue());
    }

    @Test
    void plannerLayoutLoadsAndKeepsFieldsInsideSidebar() throws Exception {
        WebEngine fixture = loadMap();
        String mapPage = (String) script(fixture, "document.documentElement.outerHTML");
        CountDownLatch loaded = new CountDownLatch(1);
        FutureTask<Parent> create = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/datecourse/page/_01step_planner/condition-view.fxml"));
            // Layout inspection must not start the app's localhost server or use its real key.
            loader.setControllerFactory(type -> type == CourseEditorController.class ? new CourseEditorController() : new CoursePlannerController() {
                @Override public void initialize() {}
            });
            Parent root = loader.load();
            new Scene(root, 960, 680);
            root.resize(960, 680);
            WebView map = (WebView) root.lookup("#mapView");
            map.getEngine().getLoadWorker().stateProperty().addListener((o, previous, state) -> {
                if (state == Worker.State.SUCCEEDED) loaded.countDown();
            });
            root.applyCss();
            root.layout();
            map.snapshot(null, null);
            map.getEngine().loadContent(mapPage);
            return root;
        });
        Platform.runLater(create);
        Parent root = create.get(10, TimeUnit.SECONDS);
        assertTrue(loaded.await(10, TimeUnit.SECONDS));
        FutureTask<Void> inspect = new FutureTask<>(() -> {
            root.applyCss();
            root.layout();
            assertNull(root.lookup("#budgetField"));
            assertNull(root.lookup("#editCourseButton"));
            assertNotNull(root.lookup("#radiusSlider"));
            TextField people = (TextField) root.lookup("#peopleField");
            assertTrue(people.localToScene(people.getBoundsInLocal()).getMinX()
                    > root.lookup("#mapView").localToScene(root.lookup("#mapView").getBoundsInLocal()).getMaxX());
            assertTrue(root.lookup("#mapView").getBoundsInLocal().getWidth() > 500);
            assertFalse(root.lookup("#routeEditor").isVisible());
            assertTrue(root.lookup("#saveStatusLabel").getBoundsInLocal().getHeight() < 100);
            Path image = Path.of("build", "ui-preview", "condition-planner.png");
            Files.createDirectories(image.getParent());
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null, null), null), "png", image.toFile());
            return null;
        });
        Platform.runLater(inspect);
        inspect.get(10, TimeUnit.SECONDS);
    }
}
