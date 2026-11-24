package com.datecourse.page.account;

import com.datecourse.page._01step_planner.CoursePlannerController;
import javafx.application.Platform;
import javax.swing.*;
import java.awt.event.WindowEvent;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MembershipCloseTest {
    private static <T>T edt(Callable<T> action)throws Exception {
        var task=new FutureTask<>(action);SwingUtilities.invokeLater(task);return task.get(5,TimeUnit.SECONDS);
    }
    private static void set(Object target,String name,Object value)throws Exception {
        var field=MembershipApp.class.getDeclaredField(name);field.setAccessible(true);field.set(target,value);
    }
    private static Object get(Object target,String name)throws Exception {
        var field=MembershipApp.class.getDeclaredField(name);field.setAccessible(true);return field.get(target);
    }
    @Test void nativeCloseUsesFxDecisionAndCancelKeepsTheWindowOpen()throws Exception {
        var ready=new CountDownLatch(1);
        try{Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException started){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(5,TimeUnit.SECONDS));
        var allowed=new AtomicBoolean();var decisions=new AtomicInteger();var exits=new AtomicInteger();
        var controller=new CoursePlannerController(){
            @Override public boolean confirmWindowClose(){assertTrue(Platform.isFxApplicationThread());decisions.incrementAndGet();return allowed.get();}
        };
        var app=edt(()->{var a=new MembershipApp();a.installCloseHandling();set(a,"plannerController",controller);
            set(a,"exitApplication",(Runnable)exits::incrementAndGet);return a;});
        var frame=edt(()->(JFrame)get(app,"home"));
        try {
            assertEquals(JFrame.DO_NOTHING_ON_CLOSE,edt(frame::getDefaultCloseOperation));
            edt(()->{frame.dispatchEvent(new WindowEvent(frame,WindowEvent.WINDOW_CLOSING));return null;});
            awaitDecision(app,decisions,1);
            assertEquals(0,exits.get());
            allowed.set(true);
            edt(()->{frame.dispatchEvent(new WindowEvent(frame,WindowEvent.WINDOW_CLOSING));return null;});
            awaitDecision(app,decisions,2);
            assertEquals(1,exits.get());
        } finally {edt(()->{frame.dispose();return null;});}
    }
    private static void awaitDecision(MembershipApp app,AtomicInteger decisions,int expected)throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<end) {
            if(edt(()->decisions.get()>=expected && !(boolean)get(app,"closePending")))return;
            Thread.sleep(10);
        }
        fail("종료 판정 대기 시간 초과");
    }
}
