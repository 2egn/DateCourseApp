package com.datecourse.service;

import com.datecourse.model.RoutePlan;

/** A larger next-place circle is redundant once it contains the whole route circle. */
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
