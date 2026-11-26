package com.datecourse.model;

import java.time.LocalDate;

/**
 * - 회원의 식별값, 역할 및 정지 상태 전달용 모델
 * - 로그인 DB 조회에 사용하는 MemberRepository 내부 레코드와 별도
 */
public record Member(
        String memberId,
        String name,
        String role,          // "USER" 또는 "ADMIN"
        LocalDate joinedAt,
        boolean suspended     // true면 정지 상태
) {}