package com.datecourse.service;

import com.datecourse.model.Place;
import java.util.List;

public class PlaceCalculationService {
    private static final double EARTH_RADIUS_METERS = 6_371_000.0;

    /** Great-circle distance in meters, matching SearchCondition.radius and the condition screen. */
    public double calculateDistance(double lat1, double lng1, double lat2, double lng2) {
        validateCoordinates(lat1, lng1);
        validateCoordinates(lat2, lng2);
        double a = Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.pow(Math.sin(Math.toRadians(lng2 - lng1) / 2), 2);
        a = Math.max(0, Math.min(1, a));
        return EARTH_RADIUS_METERS * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    /** A zero radius includes only the selected location; negative radii are invalid. */
    public List<Place> filterPlacesByRadius(List<Place> allPlaces, double targetLat,
                                          double targetLng, double radiusMeters) {
        validateCoordinates(targetLat, targetLng);
        if (!Double.isFinite(radiusMeters) || radiusMeters < 0) {
            throw new IllegalArgumentException("반경은 0 이상의 유한한 미터 값이어야 합니다.");
        }
        return allPlaces.stream()
                .filter(place -> calculateDistance(targetLat, targetLng, place.lat(), place.lng()) <= radiusMeters)
                .toList();
    }

    /** Prices are per person; the result is the total for the whole group. */
    public int calculateTotalCost(Place meal, Place cafe, Place culture, int people) {
        if (people <= 0) {
            throw new IllegalArgumentException("인원수는 1명 이상이어야 합니다.");
        }
        int unitPriceSum = Math.addExact(Math.addExact(meal.price(), cafe.price()), culture.price());
        return Math.multiplyExact(unitPriceSum, people);
    }

    private void validateCoordinates(double lat, double lng) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180) {
            throw new IllegalArgumentException("유효한 위도·경도를 입력해 주세요.");
        }
    }
}