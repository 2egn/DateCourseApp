package kr.ac.university.localdb.ui;

import javax.swing.*;
import javax.swing.plaf.basic.*;
import javax.swing.table.*;
import java.awt.*;

/**
 * style.css의 베이지·버건디 색을 Swing 화면에 똑같이 적용하는 도우미 클래스입니다.
 */
final class AdminStyle {
    static final Color BG = new Color(0xF8F5EF);          // 바탕
    static final Color BEIGE = new Color(0xE9DFD2);       // 버튼·머리글·선택
    static final Color BEIGE_DARK = new Color(0xDFD2C0);  // 호버·스크롤 막대
    static final Color BURGUNDY = new Color(0x7A263A);    // 포인트
    static final Color DARK = new Color(0x4A1827);        // 글자
    static final Color STRIPE = new Color(0xFBF9F5);      // 표 줄무늬

    private AdminStyle() {
    }

    /**
     * 탭 바: 선택된 탭은 버건디, 나머지는 베이지로 납작하게 그립니다.
     */
    static void tabs(JTabbedPane tabs) {
        tabs.setOpaque(true);
        tabs.setBackground(BG);
        tabs.setForeground(DARK);
        tabs.setFont(LoginUI.BODY.deriveFont(Font.BOLD, 13f));
        tabs.setUI(new BasicTabbedPaneUI() {
            @Override
            protected void installDefaults() {
                super.installDefaults();
                tabInsets = new Insets(6, 18, 6, 18);
                selectedTabPadInsets = new Insets(0, 0, 0, 0);
                tabAreaInsets = new Insets(4, 0, 0, 0);
                contentBorderInsets = new Insets(0, 0, 0, 0);
            }

            @Override
            protected void paintTabBackground(Graphics g, int placement, int index,
                                              int x, int y, int w, int h, boolean selected) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(selected ? BURGUNDY : BEIGE);
                g2.fillRoundRect(x + 1, y, w - 2, h + 10, 10, 10);
                g2.dispose();
            }

            @Override
            protected void paintTabBorder(Graphics g, int placement, int index,
                                          int x, int y, int w, int h, boolean selected) {
            }

            @Override
            protected void paintFocusIndicator(Graphics g, int placement, Rectangle[] rects,
                                               int index, Rectangle icon, Rectangle text, boolean selected) {
            }

            @Override
            protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
            }

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

    private static final class ThinScrollBarUI extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return empty();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return empty();
        }

        private JButton empty() {
            JButton b = new JButton();
            Dimension zero = new Dimension(0, 0);
            b.setPreferredSize(zero);
            b.setMinimumSize(zero);
            b.setMaximumSize(zero);
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(Color.WHITE);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(isThumbRollover() ? BURGUNDY : BEIGE_DARK);
            g2.fillRoundRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2, 8, 8);
            g2.dispose();
        }
    }
}