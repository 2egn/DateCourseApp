package com.datecourse.ui;

import javafx.scene.control.ScrollPane;

/** Shared scroll surface with a narrow gutter that never covers the content. */
public class ModernScrollPane extends ScrollPane {
    public ModernScrollPane() {
        getStyleClass().add("modern-scroll");
        getStylesheets().add(ModernScrollPane.class.getResource("/modern-scroll.css").toExternalForm());
    }
}
