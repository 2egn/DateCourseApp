package com.datecourse.ui;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.scene.paint.Color;
import org.junit.jupiter.api.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class DialogThemeTest {
    @BeforeAll static void start()throws Exception {
        var ready=new CountDownLatch(1);
        try{Platform.startup(()->{Platform.setImplicitExit(false);ready.countDown();});}
        catch(IllegalStateException running){Platform.runLater(ready::countDown);}
        assertTrue(ready.await(5,TimeUnit.SECONDS));
    }
    @Test void commonRolesHaveMatchingGeometryAndNativeDialogsUseTheSameTheme()throws Exception {
        var task=new FutureTask<Void>(()->{
            var title=new Label("화면 제목");title.getStyleClass().add("title");
            var primary=new Button("저장");primary.getStyleClass().add("primary");
            var secondary=new Button("새로고침");secondary.getStyleClass().add("secondary");
            var danger=new Button("삭제");danger.getStyleClass().add("danger");
            var folded=new TitledPane("추가 설정",new Label("설정 내용"));folded.setExpanded(false);
            var field=new TextField("입력값");var combo=new ComboBox<String>();combo.getItems().add("선택값");combo.getSelectionModel().selectFirst();
            var root=new VBox(12,title,new HBox(8,primary,secondary,danger),field,combo,folded);
            root.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());new Scene(root,700,460);
            root.resize(700,460);root.applyCss();root.layout();
            assertEquals(primary.getHeight(),secondary.getHeight(),.1);assertEquals(primary.getHeight(),danger.getHeight(),.1);
            assertEquals(field.getHeight(),combo.getHeight(),.1);
            assertNotEquals(primary.getBackground().getFills().getFirst().getFill(),secondary.getBackground().getFills().getFirst().getFill());
            assertEquals(Color.WHITE,primary.getTextFill());
            assertTrue(((Text)folded.lookup(".title .text")).getFont().getSize()<title.getFont().getSize());
            var alert=new Alert(Alert.AlertType.CONFIRMATION,"저장하지 않은 내용이 있어요.",ButtonType.OK,ButtonType.CANCEL);
            DialogTheme.apply(alert);int count=alert.getDialogPane().getStylesheets().size();DialogTheme.apply(alert);
            assertEquals(count,alert.getDialogPane().getStylesheets().size());
            var pane=alert.getDialogPane();pane.applyCss();pane.autosize();pane.layout();
            assertEquals(primary.prefHeight(-1),pane.lookupButton(ButtonType.OK).prefHeight(-1),.1);
            var file=java.nio.file.Path.of("build/ui-preview/common-ui-controls.png");java.nio.file.Files.createDirectories(file.getParent());
            javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(root.snapshot(null,null),null),"png",file.toFile());
            alert.close();return null;
        });Platform.runLater(task);task.get(10,TimeUnit.SECONDS);
    }
}
