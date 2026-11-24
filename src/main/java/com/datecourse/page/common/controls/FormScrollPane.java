package com.datecourse.page.common.controls;

/** Vertically scrolling forms inherit the common scroll surface. */
public final class FormScrollPane extends ModernScrollPane {
    public FormScrollPane() {
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
    }
}
