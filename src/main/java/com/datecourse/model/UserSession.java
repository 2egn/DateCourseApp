package com.datecourse.model;

/**
 * - 로그인한 회원의 식별값 전달용 모델
 * - 회원별 조회, 코스 저장 및 소유권 확인에 사용
 */
public record UserSession(long memberId, String name) {

}
