package kr.ac.university.localdb.ui;

import javax.swing.*;
import java.awt.*;

/** 높이가 늘어나면서 서서히 나타나는 입력 행. 모든 동작은 Swing 스레드에서 수행합니다. */
public final class RevealPanel extends JPanel {
    private float progress;
    private Timer timer;
    private int revealedHeight() {
        // Include the actual font, label, field and padding sizes (also on scaled displays).
        return Math.round(Math.max(98, super.getPreferredSize().height) * progress);
    }
    public RevealPanel(String label,JComponent input) {
        super(new BorderLayout(0,6));
        setBorder(BorderFactory.createEmptyBorder(10,0,10,0));
        JLabel heading=new JLabel(label);heading.setForeground(UiTheme.MUTED);heading.setFont(UiTheme.BODY.deriveFont(13f));
        add(heading,BorderLayout.NORTH); add(input,BorderLayout.CENTER);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setVisible(false);
    }
    public void reveal(Runnable finished) {
        if(timer!=null) timer.stop();
        progress=0; setVisible(true);
        long start=System.nanoTime();
        timer=new Timer(15,e -> {
            float t=Math.min(1f,(System.nanoTime()-start)/280_000_000f);
            progress=1f-(1f-t)*(1f-t);
            revalidate(); repaint();
            if(t>=1f) { timer.stop(); finished.run(); }
        });
        timer.start();
    }
    public void reset() { if(timer!=null) timer.stop(); progress=0; setVisible(false); }
    @Override public Dimension getPreferredSize() { return new Dimension(super.getPreferredSize().width,revealedHeight()); }
    @Override public Dimension getMaximumSize() { return new Dimension(Integer.MAX_VALUE,revealedHeight()); }
    @Override public Dimension getMinimumSize() { return new Dimension(0,revealedHeight()); }
    @Override public void paint(Graphics g) {
        Graphics2D copy=(Graphics2D)g.create();
        copy.setComposite(AlphaComposite.SrcOver.derive(progress)); super.paint(copy); copy.dispose();
    }
}
