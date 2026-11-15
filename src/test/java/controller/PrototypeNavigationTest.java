package controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.DialogPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.jupiter.api.Assertions.*;

class PrototypeNavigationTest {
    @BeforeAll static void startFx()throws Exception {
        var ready=new CountDownLatch(1);
        try{Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException running){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(10,TimeUnit.SECONDS));
    }
    @Test void allMasterPrototypeScreensLoadWithTheProductionClasspath()throws Exception {
        fx(()->{
            for(String name:new String[]{"main.fxml","myPage.fxml","feedBack.fxml","courseRecommend.fxml"}) {
                var url=getClass().getResource("/fxml/"+name);assertNotNull(url,name);
                assertNotNull(new FXMLLoader(url).load(),name);
            }
            return null;
        });
    }
    @Test void missingPrototypeActionsKeepTheirSceneAndDoNotClaimSuccessfulLogout()throws Exception {
        fx(()->{
            var source=new Button("test");var scene=new Scene(new StackPane(source));var stage=new Stage();stage.setScene(scene);
            try {
                var home=new Main();
                assertTrue(notice(()->home.goSavedCourseAction(new ActionEvent(source,source))).contains("연결되지 않은"));
                assertSame(scene,stage.getScene());
                assertTrue(notice(()->home.goCommunityButton(new ActionEvent(source,source))).contains("연결되지 않은"));
                assertSame(scene,stage.getScene());
                String logout=notice(()->{
                    var method=MyPage.class.getDeclaredMethod("logout",ActionEvent.class);method.setAccessible(true);
                    method.invoke(new MyPage(),new ActionEvent(source,source));
                });
                assertTrue(logout.contains("로그인 연결이 없어요"));assertFalse(logout.contains("로그아웃 되었습니다"));
                assertSame(scene,stage.getScene());
            }finally{stage.close();}
            return null;
        });
    }
    interface Action {void run()throws Exception;}
    private static String notice(Action action)throws Exception {
        var text=new AtomicReference<String>();
        Platform.runLater(()->{
            for(Window window:java.util.List.copyOf(Window.getWindows())) {
                if(window instanceof Stage dialog && "예제 화면 안내".equals(dialog.getTitle())) {
                    var pane=(DialogPane)dialog.getScene().getRoot().lookup(".dialog-pane");
                    text.set(pane.getContentText());dialog.hide();
                }
            }
        });
        action.run();assertNotNull(text.get(),"Expected a prototype notice");return text.get();
    }
    private static <T>T fx(Callable<T> action)throws Exception {
        var work=new FutureTask<T>(action);Platform.runLater(work);return work.get(15,TimeUnit.SECONDS);
    }
}
