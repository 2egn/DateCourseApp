package kr.ac.university.localdb.ui;

import javax.swing.*;
import java.awt.*;

/** Keeps signup fields as wide as the viewport and scrolls instead of squeezing their height. */
final class SignupForm extends JPanel implements Scrollable {
    SignupForm() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 10));
    }

    @Override public Dimension getPreferredScrollableViewportSize() { return new Dimension(440, 500); }
    @Override public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) { return 24; }
    @Override public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
        return Math.max(24, (orientation == SwingConstants.VERTICAL ? visible.height : visible.width) - 24);
    }
    @Override public boolean getScrollableTracksViewportWidth() { return true; }
    @Override public boolean getScrollableTracksViewportHeight() { return false; }
}