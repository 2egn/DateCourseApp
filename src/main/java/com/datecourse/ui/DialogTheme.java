package com.datecourse.ui;

import javafx.scene.control.Dialog;

/** Applies the existing stylesheet to native JavaFX dialogs. */
public final class DialogTheme {
    private DialogTheme() {}
    public static void apply(Dialog<?> dialog) {
        String stylesheet=DialogTheme.class.getResource("/style.css").toExternalForm();
        if(!dialog.getDialogPane().getStylesheets().contains(stylesheet))
            dialog.getDialogPane().getStylesheets().add(stylesheet);
    }
}
