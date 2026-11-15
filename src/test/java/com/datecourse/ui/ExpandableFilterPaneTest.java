package com.datecourse.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.*;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ExpandableFilterPaneTest {
    @BeforeAll static void startFx()throws Exception{
        CountDownLatch ready=new CountDownLatch(1);
        try{Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException started){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(10,TimeUnit.SECONDS));
    }
    @Test void registeringALargeNewCategoryKeepsTheEditorBoundedAndSelectionsIntact()throws Exception{
        FutureTask<Void> task=new FutureTask<>(()->{
            var pane=new ExpandableFilterPane();
            var searches=new java.util.concurrent.atomic.AtomicInteger();pane.setOnSearch(searches::incrementAndGet);
            var first=new CheckBox("원래 조건");first.setSelected(true);
            VBox future=new VBox(8);for(int i=0;i<40;i++)future.getChildren().add(new CheckBox("추가 조건 "+i));
            pane.setSections(List.of(
                new ExpandableFilterPane.Section("original","기존 그룹","기존 조건",first,()->first.isSelected()?1:0),
                new ExpandableFilterPane.Section("future","추가 그룹","새 조건도 같은 편집 영역에서 제공해요.",future,()->0)));
            pane.setSelections(List.of(new ExpandableFilterPane.Selection("original","원래 조건",()->first.setSelected(false))));
            VBox root=new VBox(pane);root.getStyleClass().add("condition-screen");
            root.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            new Scene(root,520,600);root.resize(520,600);pane.setExpanded(true);root.applyCss();root.layout();
            double height=pane.getHeight();pane.showSection("future");root.applyCss();root.layout();
            var scroll=(ScrollPane)pane.lookup(".filter-scroll");
            assertTrue(scroll.getHeight()<=280);assertTrue(future.getHeight()>scroll.getViewportBounds().getHeight());
            assertTrue(Math.abs(height-pane.getHeight())<45);assertTrue(first.isSelected());
            pane.showSection("original");assertTrue(first.isSelected());assertEquals(1,pane.selections().size());
            ((Button)pane.lookup(".filter-search")).fire();assertEquals(1,searches.get());assertFalse(pane.isExpanded());
            return null;
        });Platform.runLater(task);task.get(10,TimeUnit.SECONDS);
    }
}
