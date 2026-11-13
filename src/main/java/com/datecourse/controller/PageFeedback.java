package com.datecourse.controller;

import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.util.Duration;

/** Shared overlay: does not take space from the map or resize the page. JavaFX thread only. */
public final class PageFeedback {
    public enum Kind { INFO, SUCCESS, ERROR }
    private final StackPane layer = new StackPane();
    private final StackPane busy = new StackPane();
    private final VBox toast = new VBox(9);
    private final Label message = new Label(), busyMessage = new Label();
    private final ProgressBar remaining = new ProgressBar(1);
    private final Timeline countdown;
    private final FadeTransition fade;
    private int pending;

    public PageFeedback(StackPane host) {
        layer.setPickOnBounds(false);
        layer.getStyleClass().add("page-feedback");
        ProgressIndicator indicator = new ProgressIndicator();
        indicator.setMaxSize(36, 36);
        VBox card = new VBox(14, indicator, busyMessage);
        card.setAlignment(Pos.CENTER);
        card.setMaxSize(360, 140);
        card.getStyleClass().add("loading-card");
        busy.getChildren().add(card);
        busy.getStyleClass().add("loading-shade");
        busy.setVisible(false);
        busy.setManaged(false);
        message.setWrapText(true);
        message.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(message, Priority.ALWAYS);
        Button close = new Button("×");
        close.setAccessibleText("알림 닫기");
        close.getStyleClass().add("feedback-close");
        close.setMinSize(22, 22);
        close.setMaxSize(22, 22);
        close.setOnAction(event -> hide());
        HBox body = new HBox(8, message, close);
        body.setAlignment(Pos.TOP_RIGHT);
        remaining.setMaxWidth(Double.MAX_VALUE);
        remaining.getStyleClass().add("feedback-countdown");
        remaining.setMouseTransparent(true);
        toast.getChildren().addAll(body, remaining);
        toast.setMaxSize(390, Region.USE_PREF_SIZE);
        toast.setVisible(false);
        StackPane.setAlignment(toast, Pos.TOP_RIGHT);
        StackPane.setMargin(toast, new Insets(82, 24, 0, 24));
        layer.getChildren().addAll(busy, toast);
        host.getChildren().add(layer);
        countdown = new Timeline(new KeyFrame(Duration.ZERO, new KeyValue(remaining.progressProperty(), 1)),
                new KeyFrame(Duration.seconds(5), new KeyValue(remaining.progressProperty(), 0, Interpolator.LINEAR)));
        fade = new FadeTransition(Duration.millis(250), toast);
        fade.setFromValue(1);
        fade.setToValue(0);
        countdown.setOnFinished(event -> fade.playFromStart());
        fade.setOnFinished(event -> toast.setVisible(false));
    }

    public void show(Kind kind, String text) {
        if (text == null || text.isBlank()) return;
        String brief = text.replaceAll("\\s+", " ").trim();
        int sentence = brief.indexOf(". ");
        if (sentence >= 0) brief = brief.substring(0, sentence + 1);
        message.setText(brief.length() <= 90 ? brief : brief.substring(0, 89) + "…");
        message.setTooltip(new Tooltip(text));
        toast.getStyleClass().setAll("feedback-toast", "feedback-" + kind.name().toLowerCase(java.util.Locale.ROOT));
        toast.setAccessibleText(text);
        countdown.stop();
        fade.stop();
        remaining.setProgress(1);
        toast.setOpacity(1);
        toast.setVisible(true);
        layer.toFront();
        countdown.playFromStart();
    }

    private void hide() {
        countdown.stop();
        fade.stop();
        toast.setVisible(false);
    }

    public Runnable beginLoading(String text) {
        pending++;
        busyMessage.setText(text);
        busy.setVisible(true);
        busy.setManaged(true);
        layer.toFront();
        boolean[] ended = {false};
        return () -> {
            if (ended[0]) return;
            ended[0] = true;
            pending--;
            busy.setVisible(pending > 0);
            busy.setManaged(pending > 0);
        };
    }
    public boolean isBusy() { return pending > 0; }
    public void dispose() { countdown.stop(); fade.stop(); layer.setVisible(false); }
}
