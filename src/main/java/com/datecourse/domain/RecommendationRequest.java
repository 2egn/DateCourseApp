package com.datecourse.domain;

import java.util.List;
import java.util.Objects;

public record RecommendationRequest(long budgetWon, int people, GeoPoint center,
                                    double radiusMeters, List<Category> categories) {
    public RecommendationRequest {
        Objects.requireNonNull(center);
        categories = List.copyOf(categories);
        if (budgetWon <= 0 || people < 1 || !Double.isFinite(radiusMeters)
                || radiusMeters <= 0 || radiusMeters > 20_000
                || categories.isEmpty() || categories.stream().distinct().count() != categories.size()) {
            throw new IllegalArgumentException("예산·인원은 양수, 반경은 0 초과 20,000m 이하이며 카테고리를 하나 이상 선택해야 합니다.");
        }
    }
}
