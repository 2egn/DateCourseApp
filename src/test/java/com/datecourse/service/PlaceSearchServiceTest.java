package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.SearchCondition;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PlaceSearchServiceTest {
    private final PlaceSearchService service = new PlaceSearchService();
    private Place place(long id, String category, double lat, int price) {
        return new Place(id, "p" + id, "장소 " + id, category, lat, 129.06, price, 4.0);
    }
    private SearchCondition condition(int budget, int people, String... categories) {
        return new SearchCondition(budget, people, List.of(categories), 35.157, 129.06, 500);
    }

    @Test void filtersActualCategoryCodesAndMeterRadiusAndSortsByDistance() {
        Place near = place(1, "CAFE", 35.1571, 5000);
        Place far = place(2, "CAFE", 35.17, 5000);
        Place meal = place(3, "MEAL", 35.157, 10000);
        assertEquals(List.of(near), service.nearby(List.of(far, meal, near), 35.157, 129.06, 500, List.of("카페"))
                .stream().map(PlaceSearchService.NearbyPlace::place).toList());
        assertEquals(2, service.nearby(List.of(far, meal, near), 35.157, 129.06, 500, List.of()).size());
    }

    @Test void recommendsOnePerCategoryWithinWholeGroupBudget() {
        Place expensive = place(1, "MEAL", 35.157, 20000);
        Place meal = place(2, "MEAL", 35.158, 10000);
        Place cafe = place(3, "CAFE", 35.157, 5000);
        var result = service.recommend(List.of(expensive, meal, cafe), condition(30000, 2, "식사", "카페"));
        assertEquals(List.of(meal, cafe), result.course().stream().map(PlaceSearchService.NearbyPlace::place).toList());
        assertEquals(30000, result.totalCost());
        assertFalse(result.overBudget());
    }

    @Test void reportsMissingCategoryInsteadOfPresentingPartialCourse() {
        var result = service.recommend(List.of(place(1, "MEAL", 35.157, 10000)), condition(50000, 2, "식사", "문화"));
        assertEquals(List.of("문화"), result.missingCategories());
        assertTrue(result.course().isEmpty());
    }

    @Test void doesNotPresentAnOverBudgetCourse() {
        var result = service.recommend(List.of(place(1, "MEAL", 35.157, 10000)), condition(19999, 2, "식사"));
        assertTrue(result.overBudget());
        assertEquals(20000, result.totalCost());
        assertTrue(result.course().isEmpty());
    }

    @Test void excludesBrokenCoordinatesAndRejectsNegativePricesAtConstruction() {
        assertTrue(service.nearby(List.of(place(1, "MEAL", Double.NaN, 100),
                place(2, "MEAL", 100, 100)),
                35.157, 129.06, 500, List.of()).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> place(3, "MEAL", 35.157, -1));
    }

    @Test void groupCostDoesNotOverflowInteger() {
        var result = service.recommend(List.of(place(1, "MEAL", 35.157, Integer.MAX_VALUE)),
                condition(Integer.MAX_VALUE, 20, "식사"));
        assertEquals(42949672940L, result.totalCost());
        assertTrue(result.overBudget());
    }

    private SearchCondition range(int budget, int min, int max, String... categories) {
        return new SearchCondition(budget, 2, List.of(categories), 35.157, 129.06, 500, min, max);
    }

    @Test void fillsRequestedRangeWithDistinctAffordablePlacesAndCoversSelectedActivities() {
        var result = service.recommend(List.of(place(1,"MEAL",35.157,10000),
                place(2,"CAFE",35.157,2000), place(3,"CAFE",35.158,3000),
                place(4,"CULTURE",35.157,0), place(5,"CULTURE",35.158,1000),
                place(6,"CAFE",35.157,4000)), range(50000,2,5,"식사","카페","문화"));
        assertEquals(5,result.course().size());
        assertEquals(32000,result.totalCost());
        assertEquals(3,result.course().stream().map(p -> p.place().category()).distinct().count());
        assertEquals(5,result.course().stream().map(p -> p.place().recordId()).distinct().count());
    }

    @Test void stopsAtBudgetWhileStillSatisfyingMinimumCount() {
        var result=service.recommend(List.of(place(1,"CAFE",35.157,2000),
                place(2,"CAFE",35.158,3000),place(3,"CAFE",35.159,4000)),range(11000,2,5,"카페"));
        assertEquals(2,result.course().size());
        assertEquals(10000,result.totalCost());
        assertFalse(result.overBudget());
    }

    @Test void reportsInsufficientPlacesRatherThanReturningLessThanMinimum() {
        var result=service.recommend(List.of(place(1,"CAFE",35.157,2000)),range(50000,2,5,"카페"));
        assertTrue(result.insufficientPlaces());
        assertTrue(result.course().isEmpty());
    }

    @Test void minimumCountParticipatesInBudgetCheck() {
        var result=service.recommend(List.of(place(1,"CAFE",35.157,2000),
                place(2,"CAFE",35.158,3000)),range(9000,2,5,"카페"));
        assertTrue(result.overBudget());
        assertEquals(10000,result.totalCost());
        assertTrue(result.course().isEmpty());
    }

    @Test void exactCountAndDuplicateRowsDoNotRepeatAPlace() {
        Place cafe=place(1,"CAFE",35.157,2000);
        var result=service.recommend(List.of(cafe,cafe,place(2,"CAFE",35.158,3000),
                place(3,"CAFE",35.159,4000)),range(50000,2,2,"카페"));
        assertEquals(2,result.course().size());
        assertEquals(2,result.course().stream().map(p -> p.place().recordId()).distinct().count());
    }

    @Test void differentIdsAtSameNameAndCoordinatesUseCheapestRowInNearbyAndCourse() {
        Place expensive = new Place(1, "old", "같은 카페", "CAFE", 35.157, 129.06, 6000, 4);
        Place cheap = new Place(2, "new", "같은 카페", "CAFE", 35.157, 129.06, 3000, 4);
        Place branch = new Place(3, "branch", "같은 카페", "CAFE", 35.158, 129.06, 4000, 4);
        Place neighbor = new Place(4, "neighbor", "다른 카페", "CAFE", 35.157, 129.06, 5000, 4);
        for (List<Place> input : List.of(List.of(expensive, cheap, branch, neighbor),
                List.of(neighbor, branch, cheap, expensive))) {
            var result = service.recommend(input, range(50000, 2, 5, "카페"));
            assertEquals(List.of(cheap, neighbor, branch), result.nearby().stream().map(PlaceSearchService.NearbyPlace::place).toList());
            assertEquals(List.of(cheap, neighbor, branch), result.course().stream().map(PlaceSearchService.NearbyPlace::place).toList());
            assertEquals(24000, result.totalCost());
        }
    }

    @Test void duplicateRowsCannotSatisfyMinimumVisitCount() {
        Place first = new Place(1, "first", "같은 카페", "CAFE", 35.157, 129.06, 3000, 4);
        Place duplicate = new Place(2, "second", "같은 카페", "CAFE", 35.157, 129.06, 3000, 4);
        var result = service.recommend(List.of(first, duplicate), range(50000, 2, 5, "카페"));
        assertEquals(1, result.nearby().size());
        assertTrue(result.insufficientPlaces());
        assertTrue(result.course().isEmpty());
    }

    @Test void matchingIdAlsoDeduplicatesRowsWithDifferentNamesOrCoordinates() {
        Place first = new Place(1, "same", "카페", "CAFE", 35.157, 129.06, 3000, 4);
        Place duplicate = new Place(2, "same", "수정된 이름", "CAFE", 35.158, 129.06, 5000, 4);
        assertEquals(List.of(first), service.nearby(List.of(duplicate, first), 35.157, 129.06, 500, List.of())
                .stream().map(PlaceSearchService.NearbyPlace::place).toList());
    }

    @Test void rejectsReversedOutOfBoundsOrCategoryIncompatibleRanges() {
        for(int[] bounds:List.of(new int[]{0,5},new int[]{5,2},new int[]{2,11},new int[]{1,1})) {
            assertThrows(IllegalArgumentException.class,() -> service.recommend(List.of(),
                    range(50000,bounds[0],bounds[1],"식사","카페")));
        }
    }
}
