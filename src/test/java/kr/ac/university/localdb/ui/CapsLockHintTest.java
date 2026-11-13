package kr.ac.university.localdb.ui;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.event.FocusEvent;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class CapsLockHintTest {
    @Test void focusShowsCapsWarningAndFocusLossClearsItWithoutReadingPassword() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            AtomicBoolean focused=new AtomicBoolean(true), caps=new AtomicBoolean(true);
            JPasswordField field=new JPasswordField() {
                @Override public boolean isFocusOwner() { return focused.get(); }
                @Override public char[] getPassword() { throw new AssertionError("Hint must not read the password"); }
            };
            CapsLockHint hint=new CapsLockHint(field,caps::get);
            try {
                for(var listener:field.getFocusListeners()) listener.focusGained(new FocusEvent(field,FocusEvent.FOCUS_GAINED));
                assertTrue(hint.getText().contains("Caps Lock"));
                focused.set(false);
                for(var listener:field.getFocusListeners()) listener.focusLost(new FocusEvent(field,FocusEvent.FOCUS_LOST));
                assertTrue(hint.getText().isBlank());
                caps.set(false);focused.set(true);
                for(var listener:field.getFocusListeners()) listener.focusGained(new FocusEvent(field,FocusEvent.FOCUS_GAINED));
                assertTrue(hint.getText().isBlank());
            } finally {
                for(var listener:field.getFocusListeners()) listener.focusLost(new FocusEvent(field,FocusEvent.FOCUS_LOST));
            }
        });
    }
}
