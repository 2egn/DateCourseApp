package com.datecourse.page.account;

import javax.swing.*;
import java.awt.*;

/**
 * - 회원가입 입력 폼 구성용 클래스
 * - 창 너비에 맞춰 표시하고 세로 공간이 부족하면 스크롤로 처리
 */
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