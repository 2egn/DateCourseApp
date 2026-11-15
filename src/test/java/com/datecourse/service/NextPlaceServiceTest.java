package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NextPlaceServiceTest {
    private final NextPlaceService service = new NextPlaceService();
    private Place place(long id, String name, double lat, int price) {
        return new Place(id, "p" + id, name, "CAFE", lat, 129.06, price, 4);
    }

    @Test void usesLastConfirmedPlaceAndExcludesDuplicateIdentityAndLocation() {
        Place last = place(1, "선택한 카페", 35.173, 5000);
        Place duplicate = place(2, "선택한 카페", 35.173, 4000);
        Place next = place(3, "다음 카페", 35.174, 4000);
        Place nearOrigin = place(4, "출발지 근처", 35.157, 4000);
        Place outside = place(5, "전체 범위 밖", 35.176, 1000);
        var result = service.candidates(List.of(last, duplicate, next, nearOrigin, outside), List.of(last),
                35.157, 129.06, 500, "카페", 10000, 2, 2000);
        assertEquals(List.of(next), result.stream().map(PlaceSearchService.NearbyPlace::place).toList());
    }

    @Test void appliesPerPersonCapWithoutTotalBudgetLimit() {
        Place first = place(1, "첫 장소", 35.157, 10000);
        Place affordable = place(2, "가능", 35.157, 5000);
        Place overBudget = place(3, "예산 초과", 35.157, 6000);
        var result = service.candidates(List.of(first, affordable, overBudget), List.of(first),
                35.157, 129.06, 500, "카페", 10000, 2, 500);
        assertEquals(List.of(affordable, overBudget), result.stream().map(PlaceSearchService.NearbyPlace::place).toList());
        assertTrue(service.candidates(List.of(affordable), List.of(), 35.157, 129.06, 500,
                "카페", 0, 2, 500).isEmpty());
        Place free = place(4, "무료", 35.157, 0);
        assertEquals(1, service.candidates(List.of(free), List.of(), 35.157, 129.06, 500, "카페", 0, 2, 500).size());
    }

    @Test void onlyIntersectionOfGlobalAndPreviousPlaceRangesIsEligible() {
        Place previous = place(1, "이전 장소", 35.160, 5000);
        Place globalOnly = place(2, "전체 범위만", 35.157, 3000);
        Place both = place(3, "두 범위 안", 35.161, 3000);
        Place previousOnly = place(4, "이전 장소 범위만", 35.162, 3000);
        Place neither = place(5, "두 범위 밖", 35.17, 3000);
        var result = service.candidates(List.of(previous, globalOnly, both, previousOnly, neither),
                List.of(previous), 35.157, 129.06, 300, "카페", 10000, 2, 500);
        assertEquals(List.of(both), result.stream().map(PlaceSearchService.NearbyPlace::place).toList());
    }

    @Test void firstPlaceUsesDepartureForBothRangesAndHonorsSmallerRadius() {
        Place close = place(1, "공통 범위 안", 35.158, 3000);
        Place far = place(2, "전체 범위만", 35.160, 3000);
        var result = service.candidates(List.of(close, far), List.of(),
                35.157, 129.06, 200, "카페", 10000, 2, 500);
        assertEquals(List.of(close), result.stream().map(PlaceSearchService.NearbyPlace::place).toList());
    }

    @Test void snapshotDoesNotShareMutableListAndKeepsUnitPrices() {
        Place first = place(1, "첫 장소", 35.157, 5000);
        var draft = new ArrayList<>(List.of(first));
        RoutePlan plan = new RoutePlan(35.157, 129.06, 2, 2000, "코스", draft);
        draft.clear();
        assertEquals(List.of(first), plan.places());
        assertEquals(10000, plan.totalCost());
        assertEquals(5000, plan.places().get(0).price());
        assertThrows(UnsupportedOperationException.class, () -> plan.places().clear());
    }

    @Test void returnsFiveNearestEligibleCandidatesAfterPriceAndDuplicateFiltering() {
        List<Place> places = new ArrayList<>();
        places.add(place(90, "가격 초과", 35.157, 50000));
        for (int i = 8; i >= 1; i--) places.add(place(i, "후보 " + i, 35.157 + i * 0.0001, 5000));
        places.add(new Place(100, "duplicate", "후보 1", "CAFE", 35.1571, 129.06, 5000, 4));
        var result = service.candidates(places, List.of(), 35.157, 129.06, 500, "카페", 10000, 2, 500);
        assertEquals(List.of(1L,2L,3L,4L,5L),result.stream().map(p -> p.place().recordId()).toList());
    }
}
