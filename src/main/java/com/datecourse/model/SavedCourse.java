package com.datecourse.model;

/**
 * - DB에 저장된 코스의 번호, 소유 회원 및 계획 보관용 모델
 * - 편집에 사용하는 RoutePlan과 DB 식별값은 분리해서 처리
 */
public record SavedCourse(long courseId, long memberId, RoutePlan plan, java.time.Instant updatedAt) {
    public SavedCourse(long courseId, long memberId, RoutePlan plan) { this(courseId, memberId, plan, null); }
}
