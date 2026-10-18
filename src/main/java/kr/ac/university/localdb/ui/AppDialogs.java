package kr.ac.university.localdb.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.RoundRectangle2D;

/** 알림·확인·동의 창에서 함께 사용하는 코랄 핑크 모달. */
public final class AppDialogs {
    private AppDialogs() {}

    public static void showMessageDialog(Component parent,Object message) {
        showMessageDialog(parent,message,"알림",JOptionPane.INFORMATION_MESSAGE);
    }
    public static void showMessageDialog(Component parent,Object message,String title,int type) {
        display(parent,message,title,type,new String[]{"확인"},0);
    }
    public static int showConfirmDialog(Component parent,Object message,String title,int options) {
        return showConfirmDialog(parent,message,title,options,JOptionPane.QUESTION_MESSAGE);
    }
    public static int showConfirmDialog(Component parent,Object message,String title,int options,int type) {
        return display(parent,message,title,type,new String[]{"확인","취소"},1);
    }
    public static int showOptionDialog(Component parent,Object message,String title,int options,int type,
                                       Icon icon,Object[] choices,Object initial) {
        String[] labels=new String[choices.length]; int selected=0;
        for(int i=0;i<choices.length;i++){labels[i]=String.valueOf(choices[i]);if(choices[i].equals(initial))selected=i;}
        return display(parent,message,title,type,labels,selected);
    }

    private static int display(Component parent,Object message,String title,int type,String[] choices,int defaultChoice) {
        Window owner=parent instanceof Window w ? w : SwingUtilities.getWindowAncestor(parent);
        JDialog dialog=new JDialog(owner,title,Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        GraphicsDevice device=dialog.getGraphicsConfiguration().getDevice();
        if(device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSLUCENT)) {
            dialog.setBackground(new Color(0,0,0,0));
        }
        int[] result={JOptionPane.CLOSED_OPTION};
        JPanel body=content(String.valueOf(message),title,type,choices,index -> {result[0]=index;dialog.dispose();});
        dialog.setContentPane(body);dialog.pack();
        if(device.isWindowTranslucencySupported(GraphicsDevice.WindowTranslucency.PERPIXEL_TRANSPARENT))
            dialog.setShape(new RoundRectangle2D.Double(0,0,dialog.getWidth(),dialog.getHeight(),28,28));
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),KeyStroke.getKeyStroke("ESCAPE"),JComponent.WHEN_IN_FOCUSED_WINDOW);
        JButton defaultButton=(JButton)((JPanel)body.getComponent(2)).getComponent(defaultChoice);
        dialog.getRootPane().setDefaultButton(defaultButton);
        dialog.setLocationRelativeTo(parent);defaultButton.requestFocusInWindow();dialog.setVisible(true);
        return result[0];
    }

    /** 실제 창과 미리보기에서 같은 화면 컴포넌트를 사용합니다. */
    static JPanel content(String message,String title,int type,String[] choices,java.util.function.IntConsumer action) {
        JPanel body=new JPanel(new BorderLayout(0,24)) {
            protected void paintComponent(Graphics g) {
                Graphics2D p=(Graphics2D)g.create();
                p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                p.setColor(Color.WHITE);p.fillRoundRect(0,0,getWidth(),getHeight(),28,28);
                p.setColor(new Color(0xF1DDE1));p.drawRoundRect(0,0,getWidth()-1,getHeight()-1,28,28);p.dispose();
            }
        };
        body.setOpaque(false);body.setBorder(BorderFactory.createEmptyBorder(28,28,28,28));
        JPanel header=new JPanel(new BorderLayout(12,0));header.setOpaque(false);
        JLabel badge=new JLabel(type==JOptionPane.QUESTION_MESSAGE ? "?" : type==JOptionPane.INFORMATION_MESSAGE ? "✓" : "!",SwingConstants.CENTER) {
            protected void paintComponent(Graphics g){
                Graphics2D p=(Graphics2D)g.create();p.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                p.setColor(UiTheme.SOFT);p.fillOval(0,0,getWidth(),getHeight());p.dispose();super.paintComponent(g);
            }
        };
        badge.setPreferredSize(new Dimension(42,42));badge.setForeground(new Color(0xB4233F));badge.setFont(UiTheme.BODY.deriveFont(Font.BOLD,22f));
        JLabel heading=new JLabel(title);heading.setFont(UiTheme.BODY.deriveFont(Font.BOLD,22f));heading.setForeground(UiTheme.TEXT);
        header.add(badge,BorderLayout.WEST);header.add(heading,BorderLayout.CENTER);body.add(header,BorderLayout.NORTH);
        JTextArea text=new JTextArea(message);text.setFont(UiTheme.BODY);text.setForeground(UiTheme.MUTED);
        text.setEditable(false);text.setFocusable(false);text.setLineWrap(true);text.setWrapStyleWord(true);text.setOpaque(false);
        text.setSize(384,Short.MAX_VALUE);
        int height=Math.max(46,Math.min(240,text.getPreferredSize().height));
        JScrollPane scroll=new JScrollPane(text);scroll.setBorder(null);scroll.setOpaque(false);scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(new Dimension(384,height));scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        body.add(scroll,BorderLayout.CENTER);
        JPanel buttons=new JPanel(new GridLayout(1,choices.length,12,0));buttons.setOpaque(false);
        for(int i=0;i<choices.length;i++) {
            final int index=i;JButton button=new JButton(choices[i]);UiTheme.button(button,i==0);
            button.setPreferredSize(new Dimension(120,50));button.addActionListener(e -> action.accept(index));buttons.add(button);
        }
        body.add(buttons,BorderLayout.SOUTH);return body;
    }
}
