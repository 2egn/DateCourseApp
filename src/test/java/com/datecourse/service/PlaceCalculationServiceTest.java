package com.datecourse.service;

import com.datecourse.model.Place;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlaceCalculationServiceTest {
    private final PlaceCalculationService service = new PlaceCalculationService();

    @Test
    void distanceUsesMeters() {
        assertEquals(0, service.calculateDistance(35.1, 129.1, 35.1, 129.1), 0.001);
        assertEquals(1111.95, service.calculateDistance(0, 0, 0.01, 0), 0.1);
    }

    @Test
    void filtersRepositoryPlacesUsingScreenRadiusInMeters() {
        Place near = place("near", 0.001, 1000);
        Place far = place("far", 0.01, 2000);
        assertEquals(List.of(near), service.filterPlacesByRadius(List.of(near, far), 0, 0, 200));
        assertEquals(List.of(near, far), service.filterPlacesByRadius(List.of(near, far), 0, 0, 2000));
    }

    @Test
    void zeroRadiusDoesNotDisableFiltering() {
        Place origin = place("origin", 0, 1000);
        Place other = place("other", 0.001, 1000);
        assertEquals(List.of(origin), service.filterPlacesByRadius(List.of(origin, other), 0, 0, 0));
    }

    @Test
    void rejectsInvalidRadiusAndCoordinates() {
        for (double radius : new double[]{-1, Double.NaN, Double.POSITIVE_INFINITY}) {
            assertThrows(IllegalArgumentException.class,
                    () -> service.filterPlacesByRadius(List.of(), 0, 0, radius));
        }
        assertThrows(IllegalArgumentException.class, () -> service.calculateDistance(100, 0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.calculateDistance(0, Double.NaN, 0, 0));
    }

    @Test
    void calculatesTotalForAllPeople() {
        assertEquals(46000, service.calculateTotalCost(
                place("meal", 0, 11000), place("cafe", 0, 5000), place("culture", 0, 7000), 2));
    }

    @Test
    void rejectsNonPositivePeople() {
        Place place = place("sample", 0, 1000);
        assertThrows(IllegalArgumentException.class, () -> service.calculateTotalCost(place, place, place, 0));
        assertThrows(IllegalArgumentException.class, () -> service.calculateTotalCost(place, place, place, -1));
    }

    @Test
    void excessiveCostDoesNotWrapIntoANegativeBudget() {
        Place place = place("sample", 0, 1_000_000_000);
        assertThrows(ArithmeticException.class, () -> service.calculateTotalCost(place, place, place, 2));
    }

    private Place place(String id, double lat, int price) {
        return new Place(1, id, id, "MEAL", lat, 0, price, 4.5);
    }
}