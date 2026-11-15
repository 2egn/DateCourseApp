package kr.ac.university.localdb.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;

class PasswordFieldTest {
    @Test void visibilityTogglePreservesInputCaretAndEnterActionAndResetsOnClear() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            UiTheme.PasswordField field = new UiTheme.PasswordField(24);
            JToggleButton button = (JToggleButton) field.getComponent(0);
            field.setText("sample-pass-123");
            field.setCaretPosition(4);
            char[] expected = field.getPassword();
            try {
                assertTrue(field.echoCharIsSet());
                button.doClick(0);
                assertFalse(field.echoCharIsSet());
                assertEquals(4, field.getCaretPosition());
                char[] actual = field.getPassword();
                try { assertArrayEquals(expected, actual); } finally { Arrays.fill(actual, '\0'); }
                assertEquals("비밀번호 숨기기", button.getAccessibleContext().getAccessibleName());
                AtomicInteger submitted = new AtomicInteger();
                field.addActionListener(event -> submitted.incrementAndGet());
                field.postActionEvent();
                assertEquals(1, submitted.get());
                button.doClick(0);
                assertTrue(field.echoCharIsSet());
                button.doClick(0);
                field.setText("");
                assertTrue(field.echoCharIsSet());
                assertFalse(button.isSelected());
                assertEquals("비밀번호 보기", button.getAccessibleContext().getAccessibleName());
            } finally { Arrays.fill(expected, '\0'); }
        });
    }

    @Test void buttonFitsInsideFieldAndFollowsDisabledState() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            UiTheme.PasswordField field = new UiTheme.PasswordField(24);
            field.setSize(320,54);
            field.doLayout();
            JToggleButton button = (JToggleButton) field.getComponent(0);
            assertTrue(button.getX() >= field.getWidth() - field.getInsets().right);
            assertTrue(button.getX() + button.getWidth() <= field.getWidth());
            assertTrue(button.getHeight() >= 40);
            field.setEnabled(false);
            assertFalse(button.isEnabled());
            button.doClick(0);
            assertTrue(field.echoCharIsSet());
            field.setEnabled(true);
            assertTrue(button.isEnabled());
        });
    }

    @Test void rendersMaskedAndVisibleEyeButtons() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JPanel panel = new JPanel(new GridLayout(2,1,0,12));
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
            UiTheme.PasswordField masked = new UiTheme.PasswordField(24);
            UiTheme.PasswordField visible = new UiTheme.PasswordField(24);
            masked.setText("sample-pass-123"); visible.setText("sample-pass-123");
            panel.add(masked); panel.add(visible);
            panel.setSize(380,144);panel.doLayout();masked.doLayout();visible.doLayout();
            ((JToggleButton)visible.getComponent(0)).doClick(0);
            BufferedImage image = new BufferedImage(380,144,BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            try { panel.printAll(g); } finally { g.dispose(); }
            try {
                Path out=Path.of("build/ui-preview/password-visibility.png");
                Files.createDirectories(out.getParent());
                ImageIO.write(image,"png",out.toFile());
            } catch (java.io.IOException failure) { throw new java.io.UncheckedIOException(failure); }
        });
    }
}
