package kr.ac.university.localdb;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;

/** 여백·둥근 모서리와 사용자 지정 코랄 핑크를 사용하는 공통 스타일. */
public final class UiTheme {
    public static final Color ACCENT=new Color(0xFF6B81), TEXT=new Color(0x191F28),
            MUTED=new Color(0x6B7684), INPUT=new Color(0xF2F4F6), SOFT=new Color(0xFFF0F3);
    public static final Font BODY=new Font("Malgun Gothic",Font.PLAIN,15);
    private UiTheme() {}
    public static void install() {
        UIManager.put("defaultFont",BODY);
        for(String key:new String[]{"Label.font","Button.font","TextField.font","PasswordField.font","Table.font","TableHeader.font","ComboBox.font","OptionPane.messageFont","OptionPane.buttonFont"}) UIManager.put(key,BODY);
        UIManager.put("Panel.background",Color.WHITE); UIManager.put("Label.foreground",TEXT);
        UIManager.put("ScrollPane.background",Color.WHITE); UIManager.put("Viewport.background",Color.WHITE);
    }
    public static void field(JTextField field) {
        field.setFont(BODY); field.setForeground(TEXT); field.setCaretColor(ACCENT);
        field.setBackground(INPUT); field.setOpaque(false); field.setBorder(new FieldBorder());
        field.setPreferredSize(new Dimension(300,54));
        field.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e){field.repaint();}
            public void focusLost(FocusEvent e){field.repaint();}
        });
    }
    public static class TextField extends JTextField {
        public TextField(int columns){super(columns); UiTheme.field(this);}
        protected void paintComponent(Graphics g){background(this,g);super.paintComponent(g);}
    }
    public static class PasswordField extends JPasswordField {
        public PasswordField(int columns){super(columns);UiTheme.field(this);setEchoChar('●');}
        protected void paintComponent(Graphics g){background(this,g);super.paintComponent(g);}
    }
    private static void background(JComponent c,Graphics g) {
        Graphics2D p=(Graphics2D)g.create(); p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
        p.setColor(INPUT);p.fillRoundRect(0,0,c.getWidth(),c.getHeight(),18,18);p.dispose();
    }
    private static class FieldBorder extends AbstractBorder {
        public Insets getBorderInsets(Component c){return new Insets(12,16,12,16);}
        public Insets getBorderInsets(Component c,Insets i){i.set(12,16,12,16);return i;}
        public void paintBorder(Component c,Graphics g,int x,int y,int w,int h){
            if(!c.hasFocus())return;
            Graphics2D p=(Graphics2D)g.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            p.setColor(ACCENT);p.setStroke(new BasicStroke(2));p.drawRoundRect(x+1,y+1,w-3,h-3,18,18);p.dispose();
        }
    }
    public static void button(JButton button,boolean primary) {
        button.setFont(BODY.deriveFont(Font.BOLD));button.setFocusPainted(false);button.setBorderPainted(false);
        button.setContentAreaFilled(false);button.setOpaque(false);button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(new Dimension(200,54));
        button.setBorder(BorderFactory.createEmptyBorder(12,20,12,20));
        button.setForeground(TEXT); // 핑크 위에서도 읽기 쉬운 진한 글자
        button.setUI(new BasicButtonUI(){
            public void paint(Graphics g,JComponent c){
                Graphics2D p=(Graphics2D)g.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                Color fill=!button.isEnabled()?new Color(0xE5E8EB):(primary?ACCENT:SOFT);
                if(button.getModel().isPressed())fill=fill.darker();
                else if(button.getModel().isRollover()&&button.isEnabled())fill=primary?new Color(0xFF8294):new Color(0xFFE4E9);
                p.setColor(fill);p.fillRoundRect(0,0,c.getWidth(),c.getHeight(),18,18);p.dispose();super.paint(g,c);
            }
        });
    }
    public static JPanel heading(String title,String subtitle) {
        JPanel panel=new JPanel();panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
        JLabel main=new JLabel(title);main.setFont(BODY.deriveFont(Font.BOLD,30f));
        JLabel brand=new JLabel("데이트 코스 추천 앱");brand.setFont(BODY.deriveFont(Font.BOLD,14f));brand.setForeground(MUTED);
        JLabel sub=new JLabel(subtitle);sub.setFont(BODY.deriveFont(14f));sub.setForeground(MUTED);
        panel.add(brand);panel.add(Box.createVerticalStrut(24));panel.add(main);panel.add(Box.createVerticalStrut(12));panel.add(sub);
        return panel;
    }
    public static JPanel loginPanel(JTextField id,JPasswordField password,JButton login,JButton signupLink) {
        JPanel panel=new JPanel(new BorderLayout(0,30));
        panel.add(heading("다시 만나서 반가워요","아이디와 비밀번호로 로그인해 주세요"),BorderLayout.NORTH);
        JPanel fields=new JPanel();fields.setLayout(new BoxLayout(fields,BoxLayout.Y_AXIS));
        fields.add(inputRow("아이디",id));fields.add(Box.createVerticalStrut(22));fields.add(inputRow("비밀번호",password));
        fields.add(Box.createVerticalGlue());panel.add(fields,BorderLayout.CENTER);
        button(login,true);
        signupLink.setForeground(MUTED);signupLink.setFont(BODY.deriveFont(14f));
        signupLink.setContentAreaFilled(false);signupLink.setBorderPainted(false);signupLink.setFocusPainted(false);
        signupLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        JPanel actions=new JPanel(new GridLayout(0,1,0,12));actions.add(login);actions.add(signupLink);
        panel.add(actions,BorderLayout.SOUTH);return panel;
    }
    private static JPanel inputRow(String text,JTextField input) {
        JPanel row=new JPanel(new BorderLayout(0,10));JLabel label=new JLabel(text);label.setForeground(MUTED);label.setFont(BODY.deriveFont(14f));
        row.add(label,BorderLayout.NORTH);row.add(input,BorderLayout.CENTER);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE,85));return row;
    }
}
