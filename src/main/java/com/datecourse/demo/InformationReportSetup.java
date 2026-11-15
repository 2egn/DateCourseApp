package com.datecourse.demo;

import kr.ac.university.localdb.db.MemberRepository;

/** Prepares only additive reporting tables using the existing team DB settings. */
public final class InformationReportSetup {
    public static void main(String[] args) {
        try(var members=new MemberRepository()) {
            var reports=members.informationReports();reports.initialize();
            reports.apply(java.util.List.of());
            System.out.println("제보·정정 테이블 준비 및 조회 성공. 기존 회원·장소·코스 데이터는 수정하지 않았습니다.");
        } catch(Exception failure) {
            // Connection exceptions can contain credentials/addresses; expose only a safe status.
            System.err.println("제보 테이블 준비 실패. 공통 접속 설정·DB 연결·테이블 생성 권한을 확인하세요.");
            System.exit(1);
        }
    }
}
