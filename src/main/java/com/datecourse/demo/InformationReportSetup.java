package com.datecourse.demo;

/** 기존 직접 실행 경로를 보존하는 호환 진입점. 실제 DB 준비는 maintenance에서 수행한다. */
@Deprecated
public final class InformationReportSetup {
    public static void main(String[] args) {
        com.datecourse.maintenance.DatabaseSetup.main(new String[]{"--reports"});
    }
}
