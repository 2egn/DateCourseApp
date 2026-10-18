package kr.ac.university.localdb;

import org.junit.jupiter.api.Test;
import javax.swing.*;
import java.awt.*;
import java.sql.SQLException;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;

class StartupPanelTest {
    @Test
    void initializationRunsOffEdtAndDuplicateStartsAreIgnored() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch ready = new CountDownLatch(1);
        AtomicInteger attempts = new AtomicInteger();
        AtomicBoolean background = new AtomicBoolean();
        AtomicBoolean completedOnEdt = new AtomicBoolean();
        SwingUtilities.invokeAndWait(() -> {
            StartupPanel panel = new StartupPanel(() -> {
                attempts.incrementAndGet();
                background.set(!SwingUtilities.isEventDispatchThread());
                entered.countDown();
                if (!release.await(3, TimeUnit.SECONDS)) throw new IllegalStateException("Timed out");
                return null;
            }, () -> {
                completedOnEdt.set(SwingUtilities.isEventDispatchThread());
                ready.countDown();
            }, () -> {});
            panel.start();
            panel.start();
        });
        try {
            assertTrue(entered.await(3, TimeUnit.SECONDS));
            assertEquals(1, attempts.get());
            assertTrue(background.get());
        } finally {
            release.countDown();
        }
        assertTrue(ready.await(3, TimeUnit.SECONDS));
        assertTrue(completedOnEdt.get());
    }

    @Test
    void failedInitializationShowsRetryAndCanRecover() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        AtomicInteger exits = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(1);
        AtomicReference<StartupPanel> panelRef = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            StartupPanel panel = new StartupPanel(() -> {
                if (attempts.incrementAndGet() == 1) throw new SQLException("Database unavailable");
                return null;
            }, ready::countDown, exits::incrementAndGet);
            panelRef.set(panel);
            panel.start();
        });
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        AtomicBoolean failed = new AtomicBoolean();
        while (!failed.get() && System.nanoTime() < deadline) {
            SwingUtilities.invokeAndWait(() -> {
                JButton retry = button(panelRef.get(), "다시 시도");
                failed.set(retry.getParent().isVisible());
            });
            if (!failed.get()) Thread.sleep(10);
        }
        assertTrue(failed.get(), "Failure must expose retry controls");
        assertEquals(1, ready.getCount(), "Failure must not open the login screen");
        SwingUtilities.invokeAndWait(() -> {
            button(panelRef.get(), "종료").doClick();
            button(panelRef.get(), "다시 시도").doClick();
        });
        assertEquals(1, exits.get());
        assertTrue(ready.await(3, TimeUnit.SECONDS));
        assertEquals(2, attempts.get());
    }

    private static JButton button(Container parent, String text) {
        for (Component child : parent.getComponents()) {
            if (child instanceof JButton b && text.equals(b.getText())) return b;
            if (child instanceof Container container) {
                JButton found = button(container, text);
                if (found != null) return found;
            }
        }
        return null;
    }
}