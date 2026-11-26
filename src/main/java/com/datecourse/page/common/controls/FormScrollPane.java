package com.datecourse.page.common.controls;

/** 입력 폼의 세로 스크롤과 공통 스타일을 처리하는 부품 */
public final class FormScrollPane extends ModernScrollPane {
    public FormScrollPane() {
        setFitToWidth(true);
        setHbarPolicy(ScrollBarPolicy.NEVER);
    }
}
