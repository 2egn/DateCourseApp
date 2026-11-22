package kr.ac.university.localdb.ui;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;

/** 여백·둥근 모서리와 버건디 & 아이보리 테마를 사용하는 공통 스타일. */
public final class LoginUI {

    public static final Color BG = new Color(0xF8F5EF);      // 전체 크림 아이보리 배경
    public static final Color SURFACE = new Color(0xFFFFFF); // 순백색 입력칸/카드 배경
    public static final Color SOFT = new Color(0xE9DFD2);    // 따뜻한 베이지 (보조 버튼, Hover, 테두리)
    public static final Color INPUT = SOFT;                  // 기존 코드 호환용 변수 추가 (로딩바 배경 등)
    public static final Color ACCENT = new Color(0x7A263A);  // 메인 버건디 (강조 버튼, 포커스, 체크박스)
    public static final Color TEXT = new Color(0x4A1827);    // 딥 다크 버건디 (선명한 기본 글씨)
    public static final Color MUTED = new Color(0x7A5C64);   // 보조 안내 글씨용 차분한 톤
    public static final Font BODY = new Font("Malgun Gothic", Font.PLAIN, 15);
    private LoginUI() {}

    public static void install() {
        UIManager.put("defaultFont", BODY);
        for (String key : new String[]{"Label.font","Button.font","TextField.font","PasswordField.font","Table.font","TableHeader.font","ComboBox.font","OptionPane.messageFont","OptionPane.buttonFont"})
            UIManager.put(key, BODY);
        UIManager.put("Panel.background", BG);
        UIManager.put("Label.foreground", TEXT);
        UIManager.put("ScrollPane.background", BG);
        UIManager.put("Viewport.background", BG);
        UIManager.put("CheckBox.background", BG);
    }

    public static void field(JTextField field) {
        field.setFont(BODY);
        field.setForeground(TEXT);
        field.setCaretColor(ACCENT);
        field.setBackground(SURFACE);
        field.setOpaque(false);
        field.setBorder(new FieldBorder());
        field.setPreferredSize(new Dimension(300, 54));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) { field.repaint(); }
            public void focusLost(FocusEvent e) { field.repaint(); }
        });
    }

    public static class TextField extends JTextField {
        public TextField(int columns) { super(columns); LoginUI.field(this); }
        protected void paintComponent(Graphics g) { background(this, g); super.paintComponent(g); }
    }

    public static class PasswordField extends JPasswordField {
        private final JToggleButton visibilityButton = new JToggleButton();

        public PasswordField(int columns) {
            super(columns);
            LoginUI.field(this);
            setEchoChar('●');
            setBorder(new FieldBorder(60));
            setLayout(null);
            visibilityButton.setIcon(new EyeIcon(false));
            visibilityButton.setSelectedIcon(new EyeIcon(true));
            visibilityButton.setBorder(BorderFactory.createEmptyBorder());
            visibilityButton.setContentAreaFilled(false);
            visibilityButton.setOpaque(false);
            visibilityButton.setFocusPainted(false);
            visibilityButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            visibilityButton.setUI(new BasicButtonUI() {
                @Override public void paint(Graphics graphics, JComponent component) {
                    Graphics2D g = (Graphics2D) graphics.create();
                    g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    if (visibilityButton.isEnabled() && (visibilityButton.hasFocus() || visibilityButton.getModel().isRollover())) {
                        g.setColor(SOFT); // 마우스 올렸을 때 연베이지 하이라이트
                        g.fillRoundRect(1, 1, component.getWidth() - 2, component.getHeight() - 2, 12, 12);
                    }
                    g.dispose();
                    super.paint(graphics, component);
                }
            });
            updateVisibilityLabel();
            visibilityButton.addItemListener(event -> {
                setEchoChar(visibilityButton.isSelected() ? '\0' : '●');
                updateVisibilityLabel();
            });
            visibilityButton.addActionListener(event -> requestFocusInWindow());
            add(visibilityButton);
            addPropertyChangeListener("enabled", event -> visibilityButton.setEnabled(isEnabled()));
            getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
                private void resetIfEmpty() { if (getDocument().getLength() == 0) visibilityButton.setSelected(false); }
                public void insertUpdate(javax.swing.event.DocumentEvent event) { resetIfEmpty(); }
                public void removeUpdate(javax.swing.event.DocumentEvent event) { resetIfEmpty(); }
                public void changedUpdate(javax.swing.event.DocumentEvent event) { resetIfEmpty(); }
            });
            addHierarchyListener(event -> {
                if ((event.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0 && !isShowing())
                    visibilityButton.setSelected(false);
            });
        }

        private void updateVisibilityLabel() {
            String label = visibilityButton.isSelected() ? "비밀번호 숨기기" : "비밀번호 보기";
            visibilityButton.setToolTipText(label);
            visibilityButton.getAccessibleContext().setAccessibleName(label);
        }

        @Override public void doLayout() {
            int size = Math.max(0, Math.min(42, getHeight() - 8));
            visibilityButton.setBounds(Math.max(0, getWidth() - size - 8), (getHeight() - size) / 2, size, size);
        }

        protected void paintComponent(Graphics g) { background(this, g); super.paintComponent(g); }
    }

    private static final class EyeIcon implements Icon {
        private final boolean crossed;
        private EyeIcon(boolean crossed) { this.crossed = crossed; }
        public int getIconWidth() { return 24; }
        public int getIconHeight() { return 24; }
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setColor(component.isEnabled() ? TEXT : MUTED);
                g.setStroke(new BasicStroke(1.7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                java.awt.geom.Path2D eye = new java.awt.geom.Path2D.Float();
                eye.moveTo(2, 12); eye.quadTo(12, -1, 22, 12); eye.quadTo(12, 25, 2, 12);
                g.draw(eye); g.drawOval(9, 9, 6, 6);
                if (crossed) g.drawLine(3, 3, 21, 21);
            } finally { g.dispose(); }
        }
    }

    // 입력칸 배경: 순백색(#FFFFFF) 채움 + 평소에도 구분되는 베이지(#E9DFD2) 테두리
    private static void background(JComponent c, Graphics g) {
        Graphics2D p = (Graphics2D) g.create();
        p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        p.setColor(SURFACE);
        p.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 16, 16);
        p.setColor(SOFT);
        p.setStroke(new BasicStroke(1.5f));
        p.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, 16, 16);
        p.dispose();
    }

    private static class FieldBorder extends AbstractBorder {
        private final int right;
        FieldBorder() { this(16); }
        FieldBorder(int right) { this.right = right; }
        public Insets getBorderInsets(Component c) { return new Insets(12, 16, 12, right); }
        public Insets getBorderInsets(Component c, Insets i) { i.set(12, 16, 12, right); return i; }
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            if (!c.hasFocus()) return;
            Graphics2D p = (Graphics2D) g.create();
            p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(ACCENT); // 클릭(포커스) 시 버건디 테두리 강조
            p.setStroke(new BasicStroke(2f));
            p.drawRoundRect(x + 1, y + 1, w - 3, h - 3, 16, 16);
            p.dispose();
        }
    }

    public static void button(JButton button, boolean primary) {
        button.setFont(BODY.deriveFont(Font.BOLD)); // 모든 버튼 글씨를 볼드체로 선명하게
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(200, 54));
        button.setBorder(BorderFactory.createEmptyBorder(12, 20, 12, 20));
        // 메인 버튼(로그인/가입완료)은 흰색 글씨, 보조 버튼(회원가입 이동/취소)은 딥 버건디 글씨
        button.setForeground(primary ? SURFACE : TEXT);
        button.setUI(new BasicButtonUI() {
            public void paint(Graphics g, JComponent c) {
                Graphics2D p = (Graphics2D) g.create();
                p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill = !button.isEnabled() ? SOFT : (primary ? ACCENT : SOFT);
                if (button.getModel().isPressed() && button.isEnabled()) {
                    fill = primary ? TEXT : new Color(0xD8CBB8); // 클릭 시 더 진하게
                } else if (button.getModel().isRollover() && button.isEnabled()) {
                    fill = primary ? TEXT : new Color(0xDFD3C3); // 마우스 올렸을 때 반응
                }
                p.setColor(fill);
                p.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), 16, 16);
                p.dispose();
                super.paint(g, c);
            }
        });
    }

    public static JPanel heading(String title, String subtitle) {
        JPanel panel = new JPanel();
        panel.setBackground(BG);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel main = new JLabel(title);
        main.setFont(BODY.deriveFont(Font.BOLD, 28f));
        main.setForeground(TEXT);
        JLabel brand = new JLabel("데이트 코스 추천 앱");
        brand.setFont(BODY.deriveFont(Font.BOLD, 14f));
        brand.setForeground(ACCENT); // 브랜드명은 포인트 버건디로 강조
        JLabel sub = new JLabel(subtitle);
        sub.setFont(BODY.deriveFont(14f));
        sub.setForeground(MUTED);
        panel.add(brand);
        panel.add(Box.createVerticalStrut(20));
        panel.add(main);
        panel.add(Box.createVerticalStrut(10));
        panel.add(sub);
        return panel;
    }

    public static JPanel loginPanel(JTextField id, JPasswordField password, JCheckBox rememberId, JButton login, JButton signupLink) {
        JPanel panel = new JPanel(new BorderLayout(0, 30));
        panel.setBackground(BG);
        panel.add(heading("다시 만나서 반가워요", "아이디와 비밀번호로 로그인해 주세요"), BorderLayout.NORTH);

        JPanel fields = new JPanel();
        fields.setBackground(BG);
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.add(inputRow("아이디", id));
        fields.add(Box.createVerticalStrut(22));

        JPanel passwordRow = inputRow("비밀번호", password);
        passwordRow.add(new CapsLockHint(password), BorderLayout.SOUTH);
        passwordRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 110));
        fields.add(passwordRow);

        rememberId.setFont(BODY.deriveFont(Font.BOLD, 14f));
        rememberId.setForeground(TEXT);
        rememberId.setOpaque(false);
        rememberId.setAlignmentX(Component.LEFT_ALIGNMENT);
        rememberId.setIcon(new RememberIdIcon());
        rememberId.setSelectedIcon(new RememberIdIcon());
        rememberId.setIconTextGap(9);
        rememberId.setBorder(BorderFactory.createEmptyBorder(3, 2, 3, 2));
        rememberId.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        rememberId.setFocusPainted(false);
        rememberId.setRolloverEnabled(true);
        rememberId.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent event) { rememberId.repaint(); }
            public void focusLost(FocusEvent event) { rememberId.repaint(); }
        });

        JPanel rememberRow = new JPanel(new BorderLayout());
        rememberRow.setBackground(BG);
        rememberRow.add(rememberId, BorderLayout.WEST);
        rememberRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        fields.add(Box.createVerticalStrut(12));
        fields.add(rememberRow);
        fields.add(Box.createVerticalGlue());
        panel.add(fields, BorderLayout.CENTER);

        button(login, true);
        button(signupLink, false);
        JPanel actions = new JPanel(new GridLayout(0, 1, 0, 12));
        actions.setBackground(BG);
        actions.add(login);
        actions.add(signupLink);
        panel.add(actions, BorderLayout.SOUTH);
        return panel;
    }

    /** Rounded checkbox with the same accent as the login button. */
    private static final class RememberIdIcon implements Icon {
        public int getIconWidth() { return 24; }
        public int getIconHeight() { return 24; }

        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            JCheckBox checkbox = (JCheckBox) component;
            Graphics2D g = (Graphics2D) graphics.create();
            try {
                g.translate(x, y);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean selected = checkbox.isSelected();
                boolean enabled = checkbox.isEnabled();
                if (checkbox.hasFocus() && enabled) {
                    g.setColor(SOFT);
                    g.fillRoundRect(0, 0, 24, 24, 10, 10);
                }
                Color fill = !enabled ? SOFT
                        : selected ? ACCENT : checkbox.getModel().isRollover() ? SOFT : SURFACE;
                if (checkbox.getModel().isPressed() && enabled) fill = fill.darker();
                g.setColor(fill);
                g.fillRoundRect(3, 3, 18, 18, 7, 7);
                if (!selected) {
                    g.setColor(enabled ? MUTED : SOFT);
                    g.setStroke(new BasicStroke(1.4f));
                    g.drawRoundRect(3, 3, 18, 18, 7, 7);
                } else {
                    g.setColor(SURFACE); // 체크 표시를 흰색으로 선명하게
                    g.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    java.awt.geom.Path2D check = new java.awt.geom.Path2D.Float();
                    check.moveTo(7, 12);
                    check.lineTo(10.5, 15.5);
                    check.lineTo(17, 9);
                    g.draw(check);
                }
            } finally {
                g.dispose();
            }
        }
    }

    private static JPanel inputRow(String text, JTextField input) {
        JPanel row = new JPanel(new BorderLayout(0, 8));
        row.setBackground(BG);
        JLabel label = new JLabel(text);
        label.setForeground(TEXT);
        label.setFont(BODY.deriveFont(Font.BOLD, 14f)); // 입력칸 제목도 볼드체로 통일
        row.add(label, BorderLayout.NORTH);
        row.add(input, BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
        return row;
    }
}