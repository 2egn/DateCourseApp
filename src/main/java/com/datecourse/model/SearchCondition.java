package com.datecourse.model;

import java.util.List;

/**
 * - 이전 일괄 추천 방식의 전체 예산, 방문 수 및 활동 조건 전달용 모델
 * - 현재 다음 장소 편집 화면의 조건은 NextPlaceCondition에서 처리
 */
public record SearchCondition(
        int budget,
        int people,
        List<String> categories,
        double lat,
        double lng,
        int radius,
        int minPlaces,
        int maxPlaces
) {
    /** 이전 호출부의 활동별 한 장소 추천 방식을 유지하는 호환용 생성자 */
    public SearchCondition(int budget, int people, List<String> categories, double lat, double lng, int radius) {
        this(budget, people, categories, lat, lng, radius,
                Math.max(1, new java.util.HashSet<>(categories).size()),
                Math.max(1, new java.util.HashSet<>(categories).size()));
    }
}
