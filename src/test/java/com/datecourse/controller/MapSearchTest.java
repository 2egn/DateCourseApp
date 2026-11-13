package com.datecourse.controller;

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

class MapSearchTest {
    @Test
    void myPageUsesSharedLoadingAndMenuLogoutClearsPlannerSession() throws Exception {
        CountDownLatch loaded=new CountDownLatch(1);
        var opened=new java.util.concurrent.atomic.AtomicInteger();
        MyPageController page=fx(() -> {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/my-page-view.fxml"));
            Parent root=loader.load();
            StackPane host=new StackPane(root);new Scene(host,960,680);
            host.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            MyPageController controller=loader.getController();
            controller.useFeedback(new PageFeedback(host));
            ((javafx.scene.control.Label)field(controller,"statusLabel")).textProperty()
                    .addListener((o,old,text)->{if(text.isEmpty())loaded.countDown();});
            controller.configure(()->new kr.ac.university.localdb.db.MemberRepository.Profile("me@test","내 이름",null),
                    opened::incrementAndGet,opened::incrementAndGet);
            assertTrue(controller.isLoading());return controller;
        });
        assertTrue(loaded.await(10,TimeUnit.SECONDS));
        fx(() -> {
            assertEquals("내 이름",((javafx.scene.control.Label)field(page,"nameLabel")).getText());
            assertEquals("me@test",((javafx.scene.control.Label)field(page,"emailLabel")).getText());
            assertEquals("미등록",((javafx.scene.control.Label)field(page,"phoneLabel")).getText());
            invoke(page,"onSavedCourses");invoke(page,"onBack");assertEquals(2,opened.get());
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/condition-view.fxml"));
            Parent root=loader.load();new Scene(root,1440,960);
            ConditionController main=loader.getController();
            stubPlannerMap(main,"mapView");stubPlannerMap(field(main,"routeEditorController"),"editMapView");
            main.setUserSession(new kr.ac.university.localdb.UserSession(7,"내 이름"));
            var loggedOut=new java.util.concurrent.atomic.AtomicBoolean();
            main.setAccountActions(null,()->loggedOut.set(true));
            var menu=(javafx.scene.control.MenuButton)field(main,"userLabel");
            assertEquals("내 이름님",menu.getText());assertFalse(menu.isDisabled());
            assertEquals("내 페이지",menu.getItems().get(0).getText());
            menu.getItems().get(2).fire();
            assertTrue(loggedOut.get());assertNull(field(main,"userSession"));assertTrue(menu.isDisabled());
            assertNull(field(main,"appliedPlan"));
            return null;
        });
    }

    @Test
    void clickedWalkingPopupSurvivesMouseoutAndRouteClicksDoNotMoveOrigin() throws Exception {
        WebEngine engine=loadMap();
        script(engine,"""
            window.blocked=0;javaMapBridge.onOriginChangeBlocked=function(){blocked++;};
            window.selectMapLocation(35.1579,129.059,'출발');window.setOriginLocked(true);
            window.testPath=[[{lat:35.1579,lng:129.059},{lat:35.158,lng:129.059}]];
            window.showWalkingRoute({totalDistance:200,totalTime:160,legs:[
              {from:{name:'출발'},to:{name:'첫 장소'},distance:100,time:80,paths:testPath,steps:[]},
              {from:{name:'첫 장소'},to:{name:'둘째 장소'},distance:100,time:80,paths:testPath,steps:[]}
            ]},'',false);
            routeLines[1].onclick({latLng:new kakao.maps.LatLng(35.158,129.059)});
            testMap.onclick({latLng:new kakao.maps.LatLng(35.158,129.059)});
            routeLines[1].onmouseout();routeLines[3].onmouseover({latLng:new kakao.maps.LatLng(35.158,129.059)});
            """);
        assertEquals(true,script(engine,"blocked===0 && routeOverlays[0].attached && routeOverlays[0].options.content.textContent.includes('1구간')"));
        assertEquals(true,script(engine,"routeLines[3].zIndex===12 && routeLines[1].zIndex===4"));
        script(engine,"routeLines[3].onmouseout()");
        assertEquals(true,script(engine,"routeLines[1].zIndex===12 && routeLines[3].zIndex===4 && routeOverlays[0].attached"));
        script(engine,"routeLines[3].onclick();routeLines[3].onmouseout()");
        assertEquals(true,script(engine,"routeOverlays[0].attached && routeOverlays[0].options.content.textContent.includes('2구간')"));
        Thread.sleep(50);
        script(engine,"testMap.onclick({latLng:new kakao.maps.LatLng(35.16,129.06)})");
        assertEquals(true,script(engine,"!routeOverlays[0].attached && blocked===0"));
        script(engine,"routeLines[1].onmouseover();testMap.onclick({latLng:new kakao.maps.LatLng(35.16,129.06)})");
        assertEquals(true,script(engine,"!routeOverlays[0].attached && blocked===0"));
    }

    @Test
    void emptyPlannerDisablesResetAndLoadingSavedCourseRequestsWalking() throws Exception {
        CountDownLatch requested=new CountDownLatch(1);
        fx(() -> {
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/condition-view.fxml"));
            Parent root=loader.load();new Scene(root,1440,960);
            ConditionController main=loader.getController();stubPlannerMap(main,"mapView");
            var reset=(javafx.scene.control.Button)field(main,"newCourseButton");
            assertTrue(reset.isDisabled());
            ((TextField)field(main,"peopleField")).setText("3");assertFalse(reset.isDisabled());
            invoke(main,"resetCourse");assertTrue(reset.isDisabled());
            PlannerMap map=(PlannerMap)field(main,"map");
            map.setWalkingRouter(stops -> { requested.countDown();return new com.datecourse.model.WalkingRoute(java.util.List.of(),100,60); });
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/condition-view.fxml"));
            Parent root = loader.load(); new Scene(root, 1440, 960);
            ConditionController main = loader.getController();
            RouteEditorController editor = (RouteEditorController) field(main, "routeEditorController");
            stubPlannerMap(main, "mapView"); stubPlannerMap(editor, "editMapView");
            var place = new com.datecourse.model.Place(1,"a","식당","MEAL",35.158,129.059,1000,0);
            var plan = new com.datecourse.model.RoutePlan(35.1579,129.059,2,1000,"",java.util.List.of());
            callWith(main,"presentPlan",com.datecourse.model.RoutePlan.class,plan);
            setField(main,"placesLoaded",true);setField(main,"allPlaces",java.util.List.of(place));
            invoke(main,"onRecommendClick");invoke(editor,"onRecommendNext");invoke(editor,"onAddCandidate");invoke(main,"onHome");
            PlannerMap map = (PlannerMap) field(main,"map");
            MapBridge bridge = (MapBridge) field(map,"bridge");
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
    void routeDescriptionIsOptInAndPinsSelectTheirIncomingWalkingLeg() throws Exception {
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
        assertEquals(true,script(engine,"document.getElementById('walking-panel').hidden"));
        assertEquals(true,script(engine,"document.getElementById('walking-summary').textContent.startsWith('2구간')"));
        script(engine,"window.toggleWalkingExplanation();pins.filter(p=>p.coursePin && p.attached)[0].onclick()");
        assertEquals(false,script(engine,"document.getElementById('walking-panel').hidden"));
        assertEquals(true,script(engine,"document.getElementById('walking-summary').textContent.startsWith('1구간')"));
        assertEquals(1,((Number)script(engine,"document.querySelectorAll('#walking-directions > li:not([hidden])').length")).intValue());
        script(engine,"""
            pins.filter(p=>p.coursePin && p.attached)[1].onclick();
            routeLines[3].onmouseover({latLng:new kakao.maps.LatLng(35.16,129.062)});
            window.tooltip=routeOverlays[0].options.content;
            document.getElementById('map-surface').appendChild(tooltip);
            tooltip.style.position='absolute';tooltip.style.right='16px';tooltip.style.top='180px';
            """);
        assertEquals(true,script(engine,"document.getElementById('walking-summary').textContent.startsWith('2구간')"));
        assertEquals(248,((Number)script(engine,"tooltip.getBoundingClientRect().width")).intValue());
        assertEquals(true,script(engine,"tooltip.getBoundingClientRect().height<120 && tooltip.querySelector('.walking-tooltip-facts').textContent.includes('분')"));
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
            assertEquals("요청 실패.", copy.getText());
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/condition-view.fxml"));
            Parent root = loader.load(); new Scene(root, 1440, 960);
            ConditionController main = loader.getController();
            RouteEditorController editor = (RouteEditorController) field(main, "routeEditorController");
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
            editor.resetDraft();
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
            var source = new kr.ac.university.localdb.db.PlaceRepository(null) {
                @Override public java.util.List<com.datecourse.model.Place> findAll() throws java.sql.SQLException {
                    try { release.await(10, TimeUnit.SECONDS); }
                    catch (InterruptedException failure) { Thread.currentThread().interrupt(); throw new java.sql.SQLException(failure); }
                    if (failRead) throw new java.sql.SQLException("테스트용 실패");
                    return java.util.List.of();
                }
            };
            var main = fx(() -> {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/condition-view.fxml"));
                Parent root = loader.load();
                new Scene(root, 960, 680);
                ConditionController controller = loader.getController();
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
            BasePageController page = new BasePageController() {};
            page.useFeedback(feedback);
            page.runWork("첫 작업", () -> { releaseFirst.await(); return true; }, ignored -> firstDone.countDown(), error -> fail(error));
            page.runWork("두 번째 작업", () -> { releaseSecond.await(); throw new java.io.IOException("확인용 실패"); },
                    ignored -> fail("실패해야 함"), error -> { page.notifyUser(PageFeedback.Kind.ERROR, "작업에 실패했어요. 다시 시도해 주세요."); secondDone.countDown(); });
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
            var repository = new kr.ac.university.localdb.db.SavedCourseRepository(pool);
            var session = new kr.ac.university.localdb.UserSession(7, "Flow");
            var place = new com.datecourse.model.Place(11, "test-one", "미확인 식당", "MEAL", 35.158, 129.059,
                    com.datecourse.model.PriceEstimate.unknown(), 0, com.datecourse.model.PlaceDetails.EMPTY);
            var plan = new com.datecourse.model.RoutePlan(35.1579, 129.059, 2, 1000, "흐름 검증", java.util.List.of(place));
            var main = fx(() -> {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/condition-view.fxml"));
                Parent root = loader.load();
                new Scene(root, 1440, 960);
                ConditionController controller = loader.getController();
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
                assertTrue(((javafx.scene.control.Label) field(list, "detailSummary")).getText().contains("가격 미확인"));
                assertTrue(((javafx.scene.control.Label) field(list, "detailSummary")).getText().contains("최근 수정"));
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
                        .addListener((o, old, text) -> { if (text.startsWith("저장 실패")) failed.countDown(); });
                pool.close();
                invoke(main, "onSaveCourse");
                return null;
            });
            assertTrue(failed.await(10, TimeUnit.SECONDS));
            fx(() -> { assertEquals(plan, field(main, "appliedPlan")); assertEquals(false, field(main, "saving")); return null; });
        }
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

    private static void saveAndAwait(ConditionController main) throws Exception {
        CountDownLatch saved = new CountDownLatch(1);
        fx(() -> {
            setField(main, "titlePrompt", (java.util.function.Function<com.datecourse.model.RoutePlan, java.util.Optional<String>>)
                    plan -> java.util.Optional.of(plan.title()));
            ((javafx.scene.control.Label) field(main, "saveStatusLabel")).textProperty()
                    .addListener((o, old, text) -> { if (text.equals("코스를 저장했어요.")) saved.countDown(); });
            invoke(main, "onSaveCourse");
            return null;
        });
        assertTrue(saved.await(10, TimeUnit.SECONDS));
    }

    @Test
    void pilotSourceWinsOverLateDatabaseLoadAndUnknownPriceSurvivesEditing() throws Exception {
        CountDownLatch dbStarted = new CountDownLatch(1), releaseDb = new CountDownLatch(1), dbFinished = new CountDownLatch(1);
        CountDownLatch pilotLoaded = new CountDownLatch(1);
        var delayed = new kr.ac.university.localdb.db.PlaceRepository(null) {
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/condition-view.fxml"));
            Parent root = loader.load();
            new Scene(root, 1440, 960); root.resize(1440, 960);
            ConditionController main = loader.getController();
            RouteEditorController editor = (RouteEditorController) loader.getNamespace().get("routeEditorController");
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
            FutureTask<Void> choose = new FutureTask<>(() -> { ((ConditionController) fixture[0]).usePilotPlaces(); return null; });
            Platform.runLater(choose); choose.get(10, TimeUnit.SECONDS);
            assertTrue(pilotLoaded.await(5, TimeUnit.SECONDS));
        } finally { releaseDb.countDown(); }
        assertTrue(dbFinished.await(5, TimeUnit.SECONDS));
        FutureTask<Void> inspect = new FutureTask<>(() -> {
            ConditionController main = (ConditionController) fixture[0];
            RouteEditorController editor = (RouteEditorController) fixture[1];
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
        view.getEngine().executeScript("window.presentCalls=[];window.fitCalls=[];window.detailCalls=[];window.ensureCalls=0;window.ensureRouteRangesVisible=function(){ensureCalls++;};window.openRoutePlace=function(id){detailCalls.push(id);};window.presentRoute=function(plan,candidate,radius,locked,fit){presentCalls.push([plan,candidate,radius,locked,fit]);};window.fitRouteView=function(whole){fitCalls.push(whole);};");
        Object map = field(controller, "map");
        ((PlannerMap) map).setWalkingRouter(stops -> new com.datecourse.model.WalkingRoute(java.util.List.of(), 0, 0));
        setField(map, "ready", true);
        setField(map, "loaded", true);
    }

    @Test
    void oneByOneEditingIsIsolatedUntilCompleteAndReturningKeepsDraft() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/condition-view.fxml"));
            Parent root = loader.load();
            ConditionController main = loader.getController();
            RouteEditorController editor = (RouteEditorController) loader.getNamespace().get("routeEditorController");
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
            WebView editMap=(WebView)field(editor,"editMapView");
            assertTrue(editMap.getWidth() >= 500);
            assertTrue(editMap.getHeight() >= 400, "지도 높이: " + editMap.getHeight());
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
            assertEquals("1",price.getText());
            ((javafx.animation.Timeline)field(main,"screenAnimation")).stop();
            return null;
        });
        Platform.runLater(task);task.get(15,TimeUnit.SECONDS);
    }

    @Test
    void outsideGlobalRangeBlocksCompletionUntilRemovedOrRangeAdjusted() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/route-editor-view.fxml"));
            loader.load();
            RouteEditorController editor = loader.getController();
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
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/condition-view.fxml"));loader.load();
            ConditionController main=loader.getController();
            RouteEditorController editor=(RouteEditorController)loader.getNamespace().get("routeEditorController");
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
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/route-editor-view.fxml"));loader.load();
            RouteEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
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
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/route-editor-view.fxml"));loader.load();
            RouteEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
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
            assertEquals("CHOICE",field(editor,"state").toString());
            assertEquals(true,engine.executeScript("presentCalls.length===0 && fitCalls.length===0 && ensureCalls===0"));
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
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/route-editor-view.fxml"));
            Parent root=loader.load();RouteEditorController editor=loader.getController();stubPlannerMap(editor,"editMapView");
            var first=new com.datecourse.model.Place(1,"p1","첫 식당","MEAL",35,129,5000,4);
            var next=new com.datecourse.model.Place(2,"p2","다음 식당","MEAL",35.0001,129,6000,4);
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of(first)),java.util.List.of(first,next),p->{},()->{});
            new Scene(root,1440,960);root.resize(1440,960);root.applyCss();root.layout();
            var footer=root.lookup("#editorFooter");var map=(WebView)field(editor,"editMapView");
            double mapHeight=map.getHeight(),footerY=footer.localToScene(footer.getBoundsInLocal()).getMinY();
            assertEquals(960-76,footerY,0.1);
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
            assertEquals(680-76,smallFooter,0.1);
            course.setExpanded(true);invoke(editor,"onRecommendNext");root.applyCss();root.layout();
            assertEquals(smallHeight,map.getHeight(),0.1);assertEquals(smallFooter,footer.localToScene(footer.getBoundsInLocal()).getMinY(),0.1);
            return null;
        });Platform.runLater(task);task.get(10,TimeUnit.SECONDS);
    }

    @Test
    void conditionChoiceTransitionsPreserveInputsAndKeepEmptyResultsInCondition() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/route-editor-view.fxml"));
            Parent root=loader.load();RouteEditorController editor=loader.getController();
            stubPlannerMap(editor,"editMapView");
            var place=new com.datecourse.model.Place(1,"cafe","카페","CAFE",35.0001,129,5000,4);
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of()),java.util.List.of(place),p->{},()->{});
            var condition=(javafx.scene.control.TitledPane)field(editor,"conditionPane");
            var choice=(javafx.scene.control.TitledPane)field(editor,"choicePane");
            var activity=(javafx.scene.control.ComboBox<?>)field(editor,"activityBox");
            TextField price=(TextField)field(editor,"placePriceField");
            var radius=(javafx.scene.control.Slider)field(editor,"radiusSlider");
            assertEquals("CONDITION",field(editor,"state").toString());
            assertTrue(condition.isExpanded());assertFalse(choice.isExpanded());
            activity.getSelectionModel().select(1);price.setText("1");radius.setValue(700);
            new Scene(root,1440,960);root.resize(1440,960);root.applyCss();root.layout();
            Path dir=Path.of("build","ui-preview");Files.createDirectories(dir);
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",dir.resolve("editor-condition.png").toFile());
            invoke(editor,"onRecommendNext");
            assertEquals("CHOICE",field(editor,"state").toString());
            assertFalse(condition.isExpanded());assertTrue(choice.isExpanded());
            invoke(editor,"onCandidateDetail");assertEquals("CHOICE",field(editor,"state").toString());
            invoke(editor,"onModifyConditions");
            assertEquals("CONDITION",field(editor,"state").toString());
            assertEquals("카페",activity.getValue());assertEquals("1",price.getText());assertEquals(700,radius.getValue());
            choice.setExpanded(true);assertEquals("CHOICE",field(editor,"state").toString());
            root.applyCss();root.layout();
            var add=(javafx.scene.control.Button)root.lookup(".candidate-row-add");assertNotNull(add);add.fire();
            assertEquals("CONDITION",field(editor,"state").toString());
            assertEquals(java.util.List.of(place),((javafx.scene.control.ListView<?>)field(editor,"placeList")).getItems());
            assertFalse(choice.isExpanded());assertEquals("1",price.getText());
            invoke(editor,"onRecommendNext");
            assertEquals("CONDITION",field(editor,"state").toString());
            assertTrue(((javafx.scene.control.Label)field(editor,"editorError")).getText().contains("없어요"));
            assertTrue(choice.isDisabled());
            return null;
        });Platform.runLater(task);task.get(10,TimeUnit.SECONDS);
    }

    @Test
    void topFiveSelectionAndDetailDoNotAddPlaceUntilConfirmed() throws Exception {
        FutureTask<Void> task=new FutureTask<>(()->{
            FXMLLoader loader=new FXMLLoader(getClass().getResource("/route-editor-view.fxml"));
            Parent root=loader.load(); RouteEditorController editor=loader.getController();
            stubPlannerMap(editor,"editMapView");
            var places=new java.util.ArrayList<com.datecourse.model.Place>();
            for(int i=1;i<=8;i++) places.add(new com.datecourse.model.Place(i,"p"+i,"후보 "+i,"MEAL",35+i*0.0001,129,5000,4));
            editor.begin(new com.datecourse.model.RoutePlan(35,129,2,1000,"",java.util.List.of()),places,p->{},()->{});
            invoke(editor,"onRecommendNext");
            var candidates=(javafx.scene.control.ListView<?>)field(editor,"candidateList");
            var confirmed=(javafx.scene.control.ListView<?>)field(editor,"placeList");
            assertEquals(5,candidates.getItems().size()); assertTrue(confirmed.getItems().isEmpty());
            candidates.getSelectionModel().select(3);
            invoke(editor,"onCandidateDetail");
            WebEngine engine=((WebView)field(editor,"editMapView")).getEngine();
            assertEquals("[4]",engine.executeScript("JSON.stringify(detailCalls)"));
            assertTrue(confirmed.getItems().isEmpty());
            new Scene(root,1440,960);root.resize(1440,960);root.applyCss();root.layout();
            ((javafx.scene.control.ScrollPane)root.lookup(".route-form-scroll")).setVvalue(1);
            root.layout();
            Path candidatePreview=Path.of("build","ui-preview","top-five-candidates.png");
            Files.createDirectories(candidatePreview.getParent());
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",candidatePreview.toFile());
            invoke(editor,"onAddCandidate");
            assertEquals(java.util.List.of(places.get(3)),confirmed.getItems());
            assertTrue(candidates.getItems().isEmpty());
            confirmed.getSelectionModel().selectFirst();invoke(editor,"onPlaceDetail");
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
        assertEquals(true,script(edit,"lastBounds[0][0]<35.157-1200/111320 && lastBounds[1][0]>35.157+1200/111320"));
        assertEquals(4, ((Number)script(edit,"lastBounds.length")).intValue());
        assertEquals(true, script(edit,"pins.some(function(p){return p.attached && p.options.strokeColor==='#2563eb' && p.radius===1200 && p.position.getLat()===35.157;})"));
        assertEquals(true, script(edit,"pins.some(function(p){return p.attached && p.options.strokeColor==='#15803d' && p.options.strokeStyle==='shortdash' && p.radius===1000 && p.position.getLat()===35.158;})"));
        assertEquals(false,script(edit,"document.getElementById('next-range-legend').hidden"));
        assertEquals("[48,48,48,48]",script(edit,"JSON.stringify(boundsPadding)"));
        assertEquals("[]",script(edit,"JSON.stringify(bridgeSelections)"));
        script(edit,"testMap.onclick({latLng:new kakao.maps.LatLng(36,130)})");
        assertEquals("[]",script(edit,"JSON.stringify(bridgeSelections)"));
        assertEquals(mainPins,script(main,"JSON.stringify(pins.filter(function(p){return p.attached && p.options.title;}).map(function(p){return p.options.title;}))"));
        script(edit,"window.fitRouteView(true)");
        assertEquals("[[35.157,129.06],[35.158,129.061],[35.159,129.062]]",script(edit,"JSON.stringify(lastBounds)"));
        script(edit,"window.presentRoute({startLat:35.157,startLng:129.06,radius:800,places:[]},null,200,true,true)");
        assertEquals(true,script(edit,"pins.some(function(p){return p.attached && p.options.strokeColor==='#15803d' && p.radius===200 && p.position.getLat()===35.157;})"));
        script(edit,"window.presentRoute("+plan+",null,500,false,true)");
        assertEquals(true,script(edit,"document.getElementById('next-range-legend').hidden"));
        assertEquals(false,script(edit,"pins.some(function(p){return p.attached && p.options.strokeColor==='#15803d';})"));
        assertEquals(2, ((Number)script(edit,"lastBounds.length")).intValue());
    }

    @Test
    void recommendationOpensInEditorWithEditablePlacesInOrder() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/course-edit-view.fxml"));
            Parent root = loader.load();
            CourseEditController controller = loader.getController();
            var course = com.datecourse.service.RecommendedCourseMapper.toCourse(java.util.List.of(
                    new com.datecourse.model.Place(8, "b", "두 번째 ID", "CAFE", 35, 129, 5000, 4),
                    new com.datecourse.model.Place(1, "a", "첫 번째 ID", "MEAL", 35.1, 129.1, 10000, 4)), 2);
            controller.setRecommendedCourse(course, 40000);
            javafx.scene.control.ListView<?> places = (javafx.scene.control.ListView<?>) root.lookup("#placeList");
            assertEquals(course.coursePlaces(), places.getItems());
            assertEquals("총 30,000원 · 2곳", ((javafx.scene.control.Label) root.lookup("#summaryLabel")).getText());
            assertFalse(((javafx.scene.control.Button) root.lookup("#clearButton")).isDisabled());
            controller.setRecommendedCourse(course, 40000);
            assertEquals(2, places.getItems().size());
            return null;
        });
        Platform.runLater(task);
        task.get(10, TimeUnit.SECONDS);
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
                    this.panTo = function (p) { center = p; window.mapPans.push([p.getLat(),p.getLng()]); };
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
            window.expectedAnchor=testMap.getProjection().coordsFromContainerPoint(new kakao.maps.Point(100,120));
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
                var element=document.getElementById('map');
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
        assertEquals(true,script(engine,"document.getElementById('place-detail').hidden && document.getElementById('next-range-legend').hidden"));
    }

    @Test
    void radiusEditsZoomBothWaysWhileKeepingBothCirclesAndCenterVisible() throws Exception {
        WebEngine engine=loadMap(1100,720);
        script(engine,"""
            window.mapElement=document.getElementById('map');
            testMap.getProjection=function(){
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
        // The larger global circle must prevent zooming in even when the local circle is tiny.
        script(engine,"plan.radius=2000;presentRoute(plan,null,200,true,false,[]);ensureRouteRangesVisible();window.globalLevel=testMap.getLevel();zoomCalls=[];ensureRouteRangesVisible()");
        assertEquals(0,((Number)script(engine,"zoomCalls.length")).intValue());
        assertEquals(true,script(engine,"testMap.getLevel()===globalLevel && globalLevel>5"));
        script(engine,"plan.radius=200");
        script(engine,"testMap.setLevel(5,{});plan.places=[{recordId:1,name:'멀리 있는 장소',lat:35.164,lng:129.06}];presentRoute(plan,null,200,true,false,[]);zoomCalls=[];ensureRouteRangesVisible()");
        assertTrue(((Number)script(engine,"zoomCalls.length")).intValue()>0);
        assertEquals(true,script(engine,"testMap.getCenter().getLat()===plan.startLat"));
    }

    @Test
    void fractionalMapOnlyZoomKeepsPointerAndControlsAcrossSdkBoundary() throws Exception {
        WebEngine engine = loadMap(1100, 720);
        seedItinerary(engine);
        FutureTask<Void> setup = new FutureTask<>(() -> {
            engine.executeScript("""
                disposeMapZoom();
                window.controlIds=['place-search','map-legend','map-status','place-detail'];
                window.controlSizes=controlIds.map(function(id){var r=document.getElementById(id).getBoundingClientRect();return [r.width,r.height];});
                testMap.getProjection=function(){
                    var unit=100000*Math.pow(2,5-testMap.getLevel());
                    return {
                        containerPointFromCoords:function(p){var c=testMap.getCenter();return {x:(p.getLng()-c.getLng())*unit+mapElement.clientWidth/2,y:(c.getLat()-p.getLat())*unit+mapElement.clientHeight/2};},
                        coordsFromContainerPoint:function(p){var c=testMap.getCenter();return new kakao.maps.LatLng(c.getLat()-(p.y-mapElement.clientHeight/2)/unit,c.getLng()+(p.x-mapElement.clientWidth/2)/unit);}
                    };
                };
                window.mapElement=document.getElementById('map');
                window.originalAnchor=testMap.getProjection().coordsFromContainerPoint(new kakao.maps.Point(100,120));
                window.coursePins=pins.length;
                disposeMapZoom=installInstantZoom(mapElement,testMap,{fractional:true});
                window.microWheel=function(delta){var b=mapElement.getBoundingClientRect();mapElement.dispatchEvent(new WheelEvent('wheel',{deltaY:delta,clientX:b.left+100,clientY:b.top+120,bubbles:true,cancelable:true}));};
                window.anchorError=function(){var p=testMap.getProjection().containerPointFromCoords(originalAnchor);var b=mapElement.getBoundingClientRect();return Math.abs(p.x*b.width/mapElement.clientWidth-100)+Math.abs(p.y*b.height/mapElement.clientHeight-120);};
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
            var p=testMap.getProjection().containerPointFromCoords(new kakao.maps.LatLng(testCourse[1].lat,testCourse[1].lng));
            var rail=document.getElementById('itinerary-rail').getBoundingClientRect();
            return p.x>20 && p.x+map.left<detail.left-15 && detail.left>map.left && detail.right<rail.left && rail.width===railBefore.width && rail.left===railBefore.left;})()
            """));
        assertEquals(true, script(engine, "pins.filter(function(p){return p.visitId && p.zIndex===6;}).length===1"));
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
        assertEquals("[35.157,129.059]", script(engine, "JSON.stringify(lastCenter)"));
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
        assertEquals("[35.158,129.06]", script(engine, "JSON.stringify(lastCenter)"));
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/condition-view.fxml"));
            // Layout inspection must not start the app's localhost server or use its real key.
            loader.setControllerFactory(type -> type == RouteEditorController.class ? new RouteEditorController() : new ConditionController() {
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
            assertTrue(people.localToScene(people.getBoundsInLocal()).getMaxX() <= 300);
            assertTrue(root.lookup("#mapView").getBoundsInLocal().getWidth() > 500);
            assertFalse(root.lookup("#routeEditor").isVisible());
            assertTrue(root.lookup("#resultSummary").getBoundsInLocal().getHeight() < 100);
            Path image = Path.of("build", "ui-preview", "condition-planner.png");
            Files.createDirectories(image.getParent());
            ImageIO.write(SwingFXUtils.fromFXImage(root.snapshot(null, null), null), "png", image.toFile());
            return null;
        });
        Platform.runLater(inspect);
        inspect.get(10, TimeUnit.SECONDS);
    }
}
