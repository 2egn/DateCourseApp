package com.datecourse.page.common.controls;

import javafx.scene.control.Dialog;

/** JavaFX 기본 대화상자에 공통 스타일을 적용하는 도우미 클래스 */
public final class DialogTheme {
    private DialogTheme() {}
    public static void apply(Dialog<?> dialog) {
        String stylesheet=DialogTheme.class.getResource("/style.css").toExternalForm();
        if(!dialog.getDialogPane().getStylesheets().contains(stylesheet))
            dialog.getDialogPane().getStylesheets().add(stylesheet);
    }
}
