package kr.ac.university.localdb.ui;

import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

class PlannerLoadingDialogTest {
    @Test void loadingMessageAppearsAndClosingRestoresTheOwnerOnBothCompletionPaths() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JFrame owner = new JFrame("loading test");owner.setSize(500,400);owner.setLocationRelativeTo(null);owner.setVisible(true);
            try {
                PlannerLoadingDialog dialog=new PlannerLoadingDialog(owner);
                try {
                    dialog.open();
                    assertTrue(dialog.isShowing());assertFalse(owner.isEnabled());
                    JPanel body=(JPanel)dialog.getContentPane();
                    assertEquals("화면과 장소 목록을 준비하고 있어요…",((JLabel)body.getComponent(0)).getText());
                    assertTrue(((JProgressBar)body.getComponent(2)).isIndeterminate());
                    BufferedImage image=new BufferedImage(body.getWidth(),body.getHeight(),BufferedImage.TYPE_INT_ARGB);
                    Graphics2D graphics=image.createGraphics();body.printAll(graphics);graphics.dispose();
                    Path preview=Path.of("build/ui-preview/planner-loading.png");Files.createDirectories(preview.getParent());ImageIO.write(image,"png",preview.toFile());
                } catch(Exception failure) {throw new RuntimeException(failure);}
                finally {dialog.close();}
                assertFalse(dialog.isShowing());assertTrue(owner.isEnabled());
                PlannerLoadingDialog failing=new PlannerLoadingDialog(owner);failing.open();
                try { throw new IllegalStateException("screen load failure"); }
                catch(IllegalStateException expected) {failing.close();}
                assertFalse(failing.isShowing());assertTrue(owner.isEnabled());
            } finally {owner.dispose();}
        });
    }
}
