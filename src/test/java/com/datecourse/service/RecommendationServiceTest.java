package com.datecourse.service;

import com.datecourse.domain.*;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RecommendationServiceTest {
    private final RecommendationService service = new RecommendationService();
    private final GeoPoint center = new GeoPoint(37.5563, 126.9236);

    private Place place(String id, Category category, long lower, long upper, double rating) {
        return new Place(id, id, category, center, lower, upper,
                Place.PriceUnit.PER_PERSON, false, rating, 20);
    }

    private RecommendationRequest request(long budget, int people, Category... categories) {
        return new RecommendationRequest(budget, people, center, 1500, List.of(categories));
    }

    @Test void checksUpperPriceAndPeopleIncludingExactBudgetBoundary() {
        var places = List.of(place("meal", Category.MEAL, 8000, 10000, 4.5),
                place("cafe", Category.CAFE, 4000, 6000, 4.5));
        assertTrue(service.recommend(request(31999, 2, Category.MEAL, Category.CAFE), places).isEmpty());
        Course course = service.recommend(request(32000, 2, Category.MEAL, Category.CAFE), places).getFirst();
        assertEquals(24000, course.lowerCostWon());
        assertEquals(32000, course.upperCostWon());
    }

    @Test void returnsThreeDistinctResultsSortedByScore() {
        var places = List.of(place("a", Category.MEAL, 100, 100, 4.0),
                place("b", Category.MEAL, 100, 100, 4.8),
                place("c", Category.MEAL, 100, 100, 4.5),
                place("d", Category.MEAL, 100, 100, 3.0));
        var result = service.recommend(request(100, 1, Category.MEAL), places);
        assertEquals(List.of("b", "c", "a"), result.stream().map(c -> c.stops().getFirst().id()).toList());
    }

    @Test void excludesPlacesOutsideCircleAndRequiresEveryCategory() {
        var far = new Place("far", "far", Category.MEAL, new GeoPoint(37.6, 127.0),
                100, 100, Place.PriceUnit.PER_PERSON, false, 5, 100);
        assertTrue(service.recommend(request(1000, 1, Category.MEAL), List.of(far)).isEmpty());
        assertTrue(service.recommend(request(1000, 1, Category.MEAL, Category.CAFE),
                List.of(place("meal", Category.MEAL, 100, 100, 4))).isEmpty());
    }

    @Test void groupPriceIsNotMultipliedAndFreePlacesAreAllowed() {
        var group = new Place("group", "group", Category.ENTERTAINMENT, center,
                5000, 6000, Place.PriceUnit.PER_GROUP, true, 4.5, 10);
        var result = service.recommend(request(6000, 4, Category.ENTERTAINMENT, Category.CULTURE),
                List.of(group, place("free", Category.CULTURE, 0, 0, 4)));
        assertEquals(6000, result.getFirst().upperCostWon());
        assertEquals(5000, result.getFirst().lowerCostWon());
    }

    @Test void shrinksSmallReviewSamplesTowardPrior() {
        var oneReview = new Place("one", "one", Category.CAFE, center, 0, 0,
                Place.PriceUnit.PER_PERSON, false, 5, 1);
        var established = place("many", Category.CAFE, 0, 0, 4.5);
        assertTrue(established.adjustedRating() > oneReview.adjustedRating());
    }

    @Test void rejectsInvalidInputAndDuplicateIds() {
        assertThrows(IllegalArgumentException.class, () -> request(0, 1, Category.MEAL));
        assertThrows(IllegalArgumentException.class, () -> request(100, 0, Category.MEAL));
        assertThrows(IllegalArgumentException.class, () -> request(100, 1));
        assertThrows(IllegalArgumentException.class, () -> request(100, 1, Category.MEAL, Category.MEAL));
        assertThrows(IllegalArgumentException.class, () -> new GeoPoint(Double.NaN, 127));
        assertThrows(IllegalArgumentException.class, () -> new RecommendationRequest(100, 1, center, 20001, List.of(Category.MEAL)));
        var meal = place("same", Category.MEAL, 0, 0, 4);
        assertThrows(IllegalArgumentException.class, () -> service.recommend(request(100, 1, Category.MEAL), List.of(meal, meal)));
    }

    @Test void resultHasImmutableStopsAndHonorsRequestedOrder() {
        var course = service.recommend(request(100, 1, Category.CAFE, Category.MEAL),
                List.of(place("meal", Category.MEAL, 0, 0, 4), place("cafe", Category.CAFE, 0, 0, 4))).getFirst();
        assertEquals(List.of(Category.CAFE, Category.MEAL), course.stops().stream().map(Place::category).toList());
        assertThrows(UnsupportedOperationException.class, () -> course.stops().clear());
    }

    @Test void distancePenaltyPrefersNearerCourseWithSameRatings() {
        var near = place("near", Category.MEAL, 0, 0, 4);
        var far = new Place("far", "far", Category.MEAL, new GeoPoint(37.5663, 126.9236),
                0, 0, Place.PriceUnit.PER_PERSON, false, 4, 20);
        var result = service.recommend(request(100, 1, Category.MEAL), List.of(far, near));
        assertEquals("near", result.getFirst().stops().getFirst().id());
        assertEquals(0, result.getFirst().distanceMeters());
        assertTrue(result.get(1).distanceMeters() > 1000);
    }

    @Test void keepsCostArithmeticWithinBudgetEvenNearLongLimit() {
        var a = place("a", Category.MEAL, Long.MAX_VALUE - 100, Long.MAX_VALUE - 100, 4);
        var b = place("b", Category.CAFE, 200, 200, 4);
        assertTrue(service.recommend(request(Long.MAX_VALUE, 1, Category.MEAL, Category.CAFE), List.of(a, b)).isEmpty());
    }
}
