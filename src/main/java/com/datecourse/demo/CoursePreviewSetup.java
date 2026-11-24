package com.datecourse.demo;

import com.datecourse.db.MemberRepository;

/** Adds only the optional PNG table, without touching saved courses. */
public final class CoursePreviewSetup {
    public static void main(String[] args) {
        try (var members = new MemberRepository()) {
            members.savedCourses().initializePreviews();
            System.out.println("코스 미리보기 테이블 준비 완료. 기존 코스 데이터는 변경하지 않았습니다.");
        } catch (Exception failure) {
            System.err.println("미리보기 테이블 준비 실패. DB 연결 및 생성 권한을 확인해 주세요.");
            System.exit(1);
        }
    }
}
