package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import java.util.List;

/** Reuses the candidate search distance calculation; the circle boundary is included. */
public final class RouteRangeValidator {
    private final PlaceCalculationService calculation = new PlaceCalculationService();

    public record Leg(int index, String from, Place to, double distanceMeters, Integer radius) {
        public boolean exceeded() { return radius != null && distanceMeters > radius; }
    }

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

    public List<Place> outsideRange(RoutePlan plan) {
        return plan.places().stream()
                .filter(place -> calculation.calculateDistance(plan.startLat(), plan.startLng(),
                        place.lat(), place.lng()) > plan.radius())
                .toList();
    }
}
