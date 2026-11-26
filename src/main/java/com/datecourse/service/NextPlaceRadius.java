package com.datecourse.service;

import com.datecourse.model.RoutePlan;

/**
 * - 전체 반경과 직전 장소 위치로 다음 장소의 반경 상한을 계산하는 클래스
 * - 전체 원을 덮는 데 필요한 범위와 이동수단별 상한을 적용
 */
public final class NextPlaceRadius {
    private NextPlaceRadius() {}

    public static int maximum(RoutePlan plan) {
        return maximum(plan,plan.places().size());
    }

    public static int maximum(RoutePlan plan,int nextIndex) {
        if(nextIndex<0||nextIndex>plan.places().size())throw new IllegalArgumentException("구간 순서를 확인해 주세요.");
        var previous = nextIndex==0 ? null : plan.places().get(nextIndex-1);
        double offset = previous == null ? 0 : new PlaceCalculationService().calculateDistance(
                plan.startLat(), plan.startLng(), previous.lat(), previous.lng());
        // Include the far edge without expanding beyond the selected mode's useful limit.
        return Math.max(com.datecourse.model.TravelMode.MIN_RADIUS, Math.min(plan.travelMode().maximumLegRadius(), (int) Math.ceil((plan.radius() + offset) / 100) * 100));
    }
}
