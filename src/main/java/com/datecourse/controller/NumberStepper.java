package com.datecourse.controller;

import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.ScrollEvent;
import java.math.BigDecimal;
import java.util.function.IntSupplier;
import java.util.function.IntConsumer;

/** 인원은 정수, 가격은 만원 표시를 유지하면서 천원 단위로 조절한다. */
final class NumberStepper {
    private NumberStepper() {}

    static void install(TextField field, Button up, Button down, int minimum, int maximum, int fallback) {
        bind(field,up,down,minimum,maximum,()->value(field,fallback),
                direction->step(field,direction,minimum,maximum,fallback));
    }

    static void installPrice(TextField field,Button up,Button down) {
        IntSupplier current=()->{try{return readWon(field);}catch(IllegalArgumentException invalid){return 20000;}};
        bind(field,up,down,0,100000000,current,direction->{
            int next=Math.max(0,Math.min(100000000,current.getAsInt()+direction*1000));
            field.setText(BigDecimal.valueOf(next,4).stripTrailingZeros().toPlainString());
            field.requestFocus();field.positionCaret(field.getText().length());
        });
    }

    static int readWon(TextField field) {
        String text=field.getText().trim();
        if(text.matches("(?:\\d+(?:\\.\\d{1,4})?|\\.\\d{1,4})")) try {
            int won=new BigDecimal(text).movePointRight(4).intValueExact();
            if(won>=0&&won<=100000000)return won;
        } catch(ArithmeticException ignored) { }
        throw new IllegalArgumentException("1인 가격 상한은 0~10,000만원으로 입력해 주세요. 예: 0.7만원 = 7,000원");
    }

    private static void bind(TextField field,Button up,Button down,int minimum,int maximum,
                             IntSupplier current,IntConsumer step) {
        Runnable update = () -> {
            int value = current.getAsInt();
            up.setDisable(value >= maximum);
            down.setDisable(value <= minimum);
        };
        up.setOnAction(event -> step.accept(1));
        down.setOnAction(event -> step.accept(-1));
        field.textProperty().addListener((obs, old, text) -> update.run());
        field.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.UP || event.getCode() == KeyCode.DOWN) {
                step.accept(event.getCode() == KeyCode.UP ? 1 : -1);
                event.consume();
            }
        });
        field.getParent().addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() != 0) {
                step.accept(event.getDeltaY() > 0 ? 1 : -1);
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
