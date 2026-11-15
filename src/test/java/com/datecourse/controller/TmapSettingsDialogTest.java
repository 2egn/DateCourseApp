package com.datecourse.controller;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class TmapSettingsDialogTest {
    @BeforeAll static void startFx() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        try { Platform.startup(() -> { Platform.setImplicitExit(false); started.countDown(); }); }
        catch (IllegalStateException alreadyStarted) { Platform.runLater(started::countDown); }
        assertTrue(started.await(10,TimeUnit.SECONDS));
    }

    @Test void usersChooseFourRouteCategoriesWithoutSeeingOrChangingAnApiKey() throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            AtomicInteger saved = new AtomicInteger(-1);
            Dialog<ButtonType> dialog = TmapSettingsDialog.create(null,30,saved::set);
            dialog.show();
            try {
                var pane=dialog.getDialogPane();
                pane.applyCss();pane.layout();
                assertTrue(pane.getHeight()<700,"Route choices should fit in a compact dialog");
                assertEquals(4,pane.lookupAll(".radio-button").size());
                assertTrue(pane.lookupAll(".text-input").isEmpty());
                assertTrue(((RadioButton)pane.lookup("#route-option-30")).isSelected());
                Path preview=Path.of("build/ui-preview/route-options.png");Files.createDirectories(preview.getParent());
                ImageIO.write(SwingFXUtils.fromFXImage(pane.snapshot(null,null),null),"png",preview.toFile());
                ((RadioButton)pane.lookup("#route-option-10")).fire();
                ((Button)pane.lookupButton(ButtonType.APPLY)).fire();
                assertEquals(10,saved.get());
                assertFalse(dialog.isShowing());
            } finally {dialog.setResult(ButtonType.CANCEL);dialog.close();}
            return null;
        });
        Platform.runLater(task);task.get(10,TimeUnit.SECONDS);
    }
}
