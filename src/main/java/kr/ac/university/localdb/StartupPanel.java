package kr.ac.university.localdb;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/** Startup loading and retry screen. Database work always runs outside the Swing event thread. */
final class StartupPanel extends JPanel {
    private final JLabel title = new JLabel("", SwingConstants.CENTER);
    private final JLabel description = new JLabel("", SwingConstants.CENTER);
    private final JProgressBar progress = new JProgressBar();
    private final JPanel actions = new JPanel(new GridLayout(1, 2, 12, 0));
    private final JButton retry = new JButton("다시 시도");
    private final Callable<Void> prepare;
    private final Runnable onReady;
    private SwingWorker<Void, Void> worker;

    StartupPanel(Callable<Void> prepare, Runnable onReady, Runnable onExit) {
        super(new GridBagLayout());
        this.prepare = prepare;
        this.onReady = onReady;
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("데이트 코스 추천 앱", SwingConstants.CENTER);
        brand.setForeground(UiTheme.ACCENT);
        brand.setFont(UiTheme.BODY.deriveFont(Font.BOLD, 16f));
        brand.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(UiTheme.BODY.deriveFont(Font.BOLD, 22f));
        title.setForeground(UiTheme.TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        description.setFont(UiTheme.BODY.deriveFont(14f));
        description.setForeground(UiTheme.MUTED);
        description.setAlignmentX(Component.CENTER_ALIGNMENT);
        progress.setForeground(UiTheme.ACCENT);
        progress.setBackground(UiTheme.INPUT);
        progress.setBorderPainted(false);
        progress.setPreferredSize(new Dimension(320, 8));
        progress.setMaximumSize(new Dimension(320, 8));
        progress.setAlignmentX(Component.CENTER_ALIGNMENT);
        JButton exit = new JButton("종료");
        UiTheme.button(retry, true);
        UiTheme.button(exit, false);
        actions.add(retry);
        actions.add(exit);
        actions.setMaximumSize(new Dimension(360, 54));
        actions.setAlignmentX(Component.CENTER_ALIGNMENT);
        retry.addActionListener(e -> start());
        exit.addActionListener(e -> onExit.run());
        body.add(brand);
        body.add(Box.createVerticalStrut(28));
        body.add(title);
        body.add(Box.createVerticalStrut(14));
        body.add(description);
        body.add(Box.createVerticalStrut(28));
        body.add(progress);
        body.add(Box.createVerticalStrut(28));
        body.add(actions);
        add(body);
    }

    void start() {
        if (worker != null) return;
        title.setText("로그인 서비스를 준비하고 있어요");
        description.setText("서버에 연결하고 있어요. 잠시만 기다려 주세요.");
        actions.setVisible(false);
        progress.setVisible(true);
        progress.setIndeterminate(true);
        worker = new SwingWorker<>() {
            @Override protected Void doInBackground() throws Exception {
                return prepare.call();
            }

            @Override protected void done() {
                progress.setIndeterminate(false);
                worker = null;
                try {
                    get();
                    onReady.run();
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    showFailure();
                } catch (ExecutionException failure) {
                    showFailure();
                }
            }
        };
        worker.execute();
        revalidate();
        repaint();
    }

    private void showFailure() {
        title.setText("로그인 서비스를 준비하지 못했어요");
        description.setText("<html><div style='text-align:center'>인터넷 연결과 DB 설정, 서버 상태를 확인한 뒤<br>다시 시도해 주세요.</div></html>");
        progress.setVisible(false);
        actions.setVisible(true);
        revalidate();
        repaint();
        retry.requestFocusInWindow();
    }
}