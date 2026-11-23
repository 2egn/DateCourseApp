package kr.ac.university.localdb.ui;

import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * style.css의 베이지·버건디 색을 Swing 화면에 똑같이 적용하는 도우미 클래스입니다.
 * 사용법: 각 화면에서 AdminStyle.tabs(...), scroll(...), table(...), combo(...)를 호출합니다.
 */
final class AdminStyle {

    // ===== 색상 상수 =====
    static final Color BG = new Color(0xF8F5EF);          // 바탕
    static final Color BEIGE = new Color(0xE9DFD2);       // 버튼·머리글·선택
    static final Color BEIGE_DARK = new Color(0xDFD2C0);  // 호버·스크롤 막대
    static final Color BURGUNDY = new Color(0x7A263A);    // 포인트
    static final Color DARK = new Color(0x4A1827);        // 글자
    static final Color STRIPE = new Color(0xFBF9F5);      // 표 줄무늬

    /** 유틸리티 클래스이므로 객체를 만들 수 없게 막습니다. */
    private AdminStyle() {
    }

    // ===== 컴포넌트별 스타일 적용 =====

    /**
     * 탭 바: 선택된 탭은 버건디, 나머지는 베이지로 납작하게 그립니다.
     */
    static void tabs(JTabbedPane tabs) {
        tabs.setOpaque(true);
        tabs.setBackground(BG);
        tabs.setForeground(DARK);
        tabs.setFont(LoginUI.BODY.deriveFont(Font.BOLD, 13f));
        tabs.setUI(new BasicTabbedPaneUI() {
            /** 탭 안쪽 여백과 내용 테두리 여백을 지정합니다. */
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(6, 18, 6, 18);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                tabAreaInsets = new Insets(4, 0, 0, 0);
                contentBorderInsets = new Insets(0, 0, 0, 0);
            }

            /** 탭 배경: 선택되면 버건디, 아니면 베이지 둥근 사각형. */
            @Override
            protected void paintTabBackground(Graphics g, int placement, int index,
                                              int x, int y, int w, int h, boolean selected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(selected ? BURGUNDY : BEIGE);
                g2.fillRoundRect(x + 1, y, w - 2, h + 10, 10, 10);
                g2.dispose();
            }

            /** 탭 테두리: 그리지 않습니다(납작한 모양). */
            @Override
            protected void paintTabBorder(Graphics g, int placement, int index,
                                          int x, int y, int w, int h, boolean selected) {
            }

            /** 포커스 점선: 그리지 않습니다. */
            @Override
            protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects,
                                               int index, Rectangle icon, Rectangle text, boolean selected) {
            }

            /** 탭 내용 영역 테두리: 그리지 않습니다. */
            @Override
            protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
            }

            /** 탭 글자: 선택되면 흰색, 아니면 진한 글자색. */
            @Override
            protected void paintText(Graphics g, int placement, Font font, FontMetrics metrics,
                                     int index, String title, Rectangle text, boolean selected) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                g2.setFont(font);
                g2.setColor(selected ? Color.WHITE : DARK);
                g2.drawString(title, text.x, text.y + metrics.getAscent());
            }
        });
    }

    /**
     * 화살표 없는 얇은 스크롤바.
     */
    static void scroll(JScrollPane pane) {
        pane.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        pane.getHorizontalScrollBar().setUI(new ThinScrollBarUI());
        pane.getVerticalScrollBar().setPreferredSize(new Dimension(10, 0));
        pane.getHorizontalScrollBar().setPreferredSize(new Dimension(0, 10));
        pane.getVerticalScrollBar().setBackground(Color.WHITE);
        pane.getHorizontalScrollBar().setBackground(Color.WHITE);
        pane.setBorder(BorderFactory.createLineBorder(BEIGE, 2));
        pane.getViewport().setBackground(Color.WHITE);
        // 두 스크롤바가 만나는 오른쪽 위 모서리도 베이지로 채웁니다.
        JPanel corner = new JPanel();
        corner.setBackground(BEIGE);
        pane.setCorner(JScrollPane.UPPER_RIGHT_CORNER, corner);
    }

    /**
     * 표 머리글을 베이지로 바꿉니다. 줄무늬·선택색은 MemberAdminPanel의 렌더러에서 지정합니다.
     */
    static void table(JTable table) {
        table.setSelectionBackground(BEIGE);
        table.setSelectionForeground(DARK);
        JTableHeader header = table.getTableHeader();
        header.setBackground(BEIGE);
        // 머리글 칸을 직접 그리는 렌더러: 베이지 배경, 굵은 가운데 정렬 글자
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                                                           boolean selected, boolean focus, int row, int column) {
                super.getTableCellRendererComponent(t, value, false, false, row, column);
                setOpaque(true);
                setBackground(BEIGE);
                setForeground(DARK);
                setFont(LoginUI.BODY.deriveFont(Font.BOLD, 13f));
                setHorizontalAlignment(SwingConstants.CENTER);
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
    }

    /**
     * 콤보박스: 베이지 테두리, 화살표 버튼도 같은 톤으로.
     */
    static void combo(JComboBox<?> box) {
        UIManager.put("ComboBox.disabledForeground", new Color(0xA8969A));
        UIManager.put("ComboBox.disabledBackground", Color.WHITE);
        box.setUI(new BasicComboBoxUI() {
            /** 흰 배경에 진한 화살표만 있는 버튼으로 교체합니다. */
            @Override
            protected JButton createArrowButton() {
                BasicArrowButton b = new BasicArrowButton(SwingConstants.SOUTH, Color.WHITE, Color.WHITE, DARK, Color.WHITE);
                b.setBorder(BorderFactory.createEmptyBorder());
                return b;
            }
        });
        box.setBackground(Color.WHITE);
        box.setForeground(DARK);
        box.setFont(LoginUI.BODY.deriveFont(Font.BOLD, 14f));
        box.setBorder(BorderFactory.createLineBorder(BEIGE, 2));
    }

    // ===== 내부 클래스 =====

    /** 위·아래 화살표 버튼이 없고 둥근 막대만 보이는 스크롤바 모양. */
    private static final class ThinScrollBarUI extends BasicScrollBarUI {
        /** 위(왼쪽) 화살표 버튼: 크기 0으로 숨깁니다. */
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return empty();
        }

        /** 아래(오른쪽) 화살표 버튼: 크기 0으로 숨깁니다. */
        @Override
        protected JButton createIncreaseButton(int orientation) {
            return empty();
        }

        /** 스크롤바가 움직이는 길(트랙): 흰색으로 채웁니다. */
        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(Color.WHITE);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        /** 스크롤바 막대(썸): 마우스를 올리면 버건디, 평소엔 진한 베이지. */
        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? BURGUNDY : BEIGE_DARK);
            g2.fillRoundRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 8, 8);
            g2.dispose();
        }

        /** 크기가 0인 빈 버튼을 만듭니다(화살표 숨김용). */
        private JButton empty() {
            JButton b = new JButton();
            Dimension zero = new Dimension(0, 0);
            b.setPreferredSize(zero);
            b.setMinimumSize(zero);
            b.setMaximumSize(zero);
            return b;
        }
    }
}