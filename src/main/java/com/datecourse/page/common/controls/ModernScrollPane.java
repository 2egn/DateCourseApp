package com.datecourse.page.common.controls;

import javafx.scene.control.ScrollPane;

/** 본문을 가리지 않는 좁은 스크롤 영역 구성용 공통 부품 */
public class ModernScrollPane extends ScrollPane {
    public ModernScrollPane() {
        getStyleClass().add("modern-scroll");
        getStylesheets().add(ModernScrollPane.class.getResource("/modern-scroll.css").toExternalForm());
    }
}
