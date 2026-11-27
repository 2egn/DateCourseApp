package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import java.util.List;

/**
 * - 전체 원형 범위와 장소별 도착 반경 유효성 검사 클래스
 * - PlaceCalculationService의 직선 거리 사용, API의 도로 거리·이동시간과 구분
 * - RoutePlan의 전체 반경과 arrivalRadii의 장소별 도착 반경을 각각 검사
 * - 경계와 같은 거리는 허용, 범위를 벗어난 항목만 반환하거나 초과로 판정
 */
public final class RouteRangeValidator {
    private final PlaceCalculationService calculation = new PlaceCalculationService();

    // 출발 지점·도착 장소·직선 거리·해당 구간의 반경을 묶은 검증 결과
    public record Leg(int index, String from, Place to, double distanceMeters, Integer radius) {
        // 과거 코스 등 구간 반경 기록이 없는 경우에는 임의로 초과 판정하지 않음
        public boolean exceeded() { return radius != null && distanceMeters > radius; }
    }

    // 방문 순서대로 구간 생성, 첫 구간은 출발지 기준, 이후는 직전 장소 기준
    // 거리와 저장 반경을 반환하고 개별 초과 여부는 Leg.exceeded()로 확인
    public List<Leg> legs(RoutePlan plan) {
        List<Leg> result = new java.util.ArrayList<>();
        for (int index = 0; index < plan.places().size(); index++) {
            Place current = plan.places().get(index);
            Place previous = index == 0 ? null : plan.places().get(index - 1);
            double distance = calculation.calculateDistance(previous == null ? plan.startLat() : previous.lat(),
                    previous == null ? plan.startLng() : previous.lng(), current.lat(), current.lng());
            result.add(new Leg(index, previous == null ? "출발지" : previous.name(), current, distance,
                    plan.arrivalRadii().get(current.recordId())));
        }
        return List.copyOf(result);
    }

    // 모든 장소를 출발지 중심 전체 반경과 비교, 구간 반경 통과 여부와 독립적으로 확인
    public List<Place> outsideRange(RoutePlan plan) {
        return plan.places().stream()
                .filter(place -> calculation.calculateDistance(plan.startLat(), plan.startLng(),
                        place.lat(), place.lng()) > plan.radius())
                .toList();
    }
}
