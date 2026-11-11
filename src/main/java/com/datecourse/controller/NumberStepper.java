package com.datecourse.controller;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;

/** 입력값 단위는 화면 라벨에서 정한다. 가격 필드의 한 단계는 1만원이다. */
final class NumberStepper {
    private NumberStepper() {}

    static void install(TextField field, Button up, Button down, int minimum, int maximum, int fallback) {
        Runnable update = () -> {
            int value = value(field, fallback);
            up.setDisable(value >= maximum);
            down.setDisable(value <= minimum);
        };
        up.setOnAction(event -> step(field, 1, minimum, maximum, fallback));
        down.setOnAction(event -> step(field, -1, minimum, maximum, fallback));
        field.textProperty().addListener((obs, old, text) -> update.run());
        field.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                step(field, event.getCode() == KeyCode.UP ? 1 : -1, minimum, maximum, fallback);
                event.consume();
            }
        });
        field.getParent().addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() != 0) {
                step(field, event.getDeltaY() > 0 ? 1 : -1, minimum, maximum, fallback);
                event.consume();
            }
        });
        update.run();
    }

    private static int value(TextField field, int fallback) {
        try { return Integer.parseInt(field.getText().trim()); }
        catch (NumberFormatException invalid) { return fallback; }
    }

    private static void step(TextField field, int direction, int minimum, int maximum, int fallback) {
        long next = (long) value(field, fallback) + direction;
        field.setText(Long.toString(Math.max(minimum, Math.min(maximum, next))));
        field.requestFocus();
        field.positionCaret(field.getText().length());
    }

    static int read(TextField field, int minimum, int maximum, String label) {
        try {
            int value = Integer.parseInt(field.getText().trim());
            if (value >= minimum && value <= maximum) return value;
        } catch (NumberFormatException ignored) { }
        throw new IllegalArgumentException(label + "은(는) " + minimum + "~" + maximum + " 사이의 정수로 입력해 주세요.");
    }
}
