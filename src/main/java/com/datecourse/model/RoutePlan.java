package com.datecourse.model;

import java.util.List;
import java.util.Map;

/** 확정본과 편집본 사이에 전달하는 불변 상태. 장소 가격은 항상 1인 기준이다. */
public record RoutePlan(double startLat, double startLng, int people, int radius,
                        String title, List<Place> places, Map<Long, Integer> arrivalRadii) {
    // Older callers have no recorded per-place condition; do not invent one.
    public RoutePlan(double startLat, double startLng, int people, int radius, String title, List<Place> places) {
        this(startLat, startLng, people, radius, title, places, Map.of());
    }

    public RoutePlan {
        places = List.copyOf(places);
        arrivalRadii = Map.copyOf(arrivalRadii);
        if (arrivalRadii.values().stream().anyMatch(value -> value < 200 || value > 2000))
            throw new IllegalArgumentException("장소별 이동 반경은 200~2000m여야 합니다.");
    }

    public CostEstimate costEstimate() { return CostEstimate.of(places, people); }
    public long totalCost() { return costEstimate().exactTotal(); }
}
