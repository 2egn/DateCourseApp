package kr.ac.university.localdb.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/** Only checks the keyboard toggle state; never reads password contents. */
final class CapsLockHint extends JLabel {
    private final Timer timer;
    private final java.util.function.BooleanSupplier capsLockState;
    CapsLockHint(JPasswordField field) {
        this(field, () -> Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK));
    }
    CapsLockHint(JPasswordField field, java.util.function.BooleanSupplier capsLockState) {
        super(" ");
        this.capsLockState = capsLockState;
        setFont(UiTheme.BODY.deriveFont(12f));
        setForeground(new Color(0xB45309));
        timer = new Timer(200, event -> refresh(field));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent event) { refresh(field); timer.start(); }
            @Override public void focusLost(FocusEvent event) { timer.stop(); setText(" "); }
        });
        field.addHierarchyListener(event -> { if (!field.isShowing()) { timer.stop(); setText(" "); } });
    }
    private void refresh(JPasswordField field) {
        boolean on = false;
        try { on = field.isFocusOwner() && capsLockState.getAsBoolean(); }
        catch (UnsupportedOperationException | SecurityException ignored) { }
        setText(on ? "Caps Lock이 켜져 있어요." : " ");
    }
}
