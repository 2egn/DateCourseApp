package kr.ac.university.localdb.ui;

import javax.swing.*;
import java.awt.*;

/** Short, nonblocking handoff between authenticated login and the JavaFX planner. */
final class PlannerLoadingDialog extends JDialog implements AutoCloseable {
    private final JFrame owner;
    private final boolean ownerWasEnabled;

    PlannerLoadingDialog(JFrame owner) {
        super(owner, "앱 실행 중", false);
        this.owner = owner;
        ownerWasEnabled = owner.isEnabled();
        setUndecorated(true);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE5E8EB)),
                BorderFactory.createEmptyBorder(28, 38, 28, 38)));
        JLabel message = new JLabel("화면과 장소 목록을 준비하고 있어요…");
        message.setFont(UiTheme.BODY.deriveFont(Font.BOLD, 18f));
        message.setForeground(UiTheme.TEXT);
        message.setAlignmentX(Component.CENTER_ALIGNMENT);
        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setForeground(UiTheme.ACCENT);
        progress.setBackground(UiTheme.INPUT);
        progress.setBorderPainted(false);
        progress.setPreferredSize(new Dimension(250, 7));
        progress.setMaximumSize(new Dimension(250, 7));
        progress.setAlignmentX(Component.CENTER_ALIGNMENT);
        body.add(message);
        body.add(Box.createVerticalStrut(20));
        body.add(progress);
        setContentPane(body);
        pack();
        setLocationRelativeTo(owner);
    }

    void open() {
        owner.setEnabled(false);
        setVisible(true);
        // Paint the message before first-time JavaFX toolkit initialization can occupy the EDT.
        getRootPane().paintImmediately(0, 0, getWidth(), getHeight());
    }

    @Override public void close() {
        dispose();
        owner.setEnabled(ownerWasEnabled);
    }
}
