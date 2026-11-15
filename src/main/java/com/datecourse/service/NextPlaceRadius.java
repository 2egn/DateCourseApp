package com.datecourse.service;

import com.datecourse.model.RoutePlan;

/** A larger next-place circle is redundant once it contains the whole route circle. */
public final class NextPlaceRadius {
    private NextPlaceRadius() {}

    public static int maximum(RoutePlan plan) {
        var previous = plan.places().isEmpty() ? null : plan.places().getLast();
        double offset = previous == null ? 0 : new PlaceCalculationService().calculateDistance(
                plan.startLat(), plan.startLng(), previous.lat(), previous.lng());
        // Round up so the final 100m step includes the far edge. Keep the model's 2km limit.
        return Math.max(200, Math.min(2000, (int) Math.ceil((plan.radius() + offset) / 100) * 100));
    }
}
