package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.NextPlaceCondition;
import java.util.List;

/** 다음 장소 후보를 거리순으로 찾고, 전체 원형 범위 밖의 장소와 이미 확정한 장소를 제외한다. */
public final class NextPlaceService {
    private final PlaceSearchService search = new PlaceSearchService();
    private final PlaceCalculationService calculation = new PlaceCalculationService();

    /**
     * 조건에 맞는 후보 중 화면에 표시할 개수만 반환
     * @param places 장소 전체 목록
     * @param confirmed 코스 편집에서 확정된 장소 목록
     * @param startLat 출발지의 위도
     * @param startLng 출발지의 경도
     * @param people 인원수
     * @param routeRadius 탐색 반경(공통 조건 - 전체 범위 기준)
     * @param condition 활동·가격·구간 반경·특징·표시 개수 조건
     * @return 필터 적용 후 거리순 후보, 표시 개수 이내
     */
    public List<PlaceSearchService.NearbyPlace> candidates(List<Place> places, List<Place> confirmed,
            double startLat, double startLng, int people, int routeRadius, NextPlaceCondition condition) {
        return matching(places, confirmed, startLat, startLng, people, routeRadius, condition).stream()
                .limit(condition.displayLimit()).toList();
    }

    // 전체 범위 → 다음 반경 → 가격·중복·특징 순으로 조건에 맞는 전체 후보 조회
    public List<PlaceSearchService.NearbyPlace> matching(List<Place> places, List<Place> confirmed,
            double startLat, double startLng, int people, int routeRadius, NextPlaceCondition condition) {
        int maxPricePerPerson = condition.maxPrice();
        int radius = condition.radius();
        String category = condition.activity();
        boolean includeUncertainPrice = condition.includeUnknown();
        if (people < 1 || people > 20 || routeRadius < com.datecourse.model.TravelMode.MIN_RADIUS || routeRadius > com.datecourse.model.TravelMode.MAX_RADIUS || maxPricePerPerson < 0)
            throw new IllegalArgumentException("가격·인원·전체 반경을 확인해 주세요.");
        Place last = confirmed.isEmpty() ? null : confirmed.get(confirmed.size() - 1);
        // 전체 범위에서 끝낸 유효성·중복 검사는 재사용하고, 필요한 거리만 다시 구한다.
        var nearby = search.nearby(places, startLat, startLng, routeRadius, List.of(category)).stream();
        if (last != null) nearby = nearby
                .map(candidate -> new PlaceSearchService.NearbyPlace(candidate.place(),
                        calculation.calculateDistance(last.lat(), last.lng(), candidate.place().lat(), candidate.place().lng())))
                .filter(candidate -> candidate.distanceMeters() <= radius)
                .sorted(java.util.Comparator.comparingDouble(PlaceSearchService.NearbyPlace::distanceMeters)
                        .thenComparingLong(candidate -> candidate.place().recordId()));
        else nearby = nearby.filter(candidate -> candidate.distanceMeters() <= radius);
        return nearby
                .filter(candidate -> candidate.place().priceEstimate().withinCap(maxPricePerPerson)
                    || includeUncertainPrice && !candidate.place().priceEstimate().finite()
                        && (!candidate.place().priceEstimate().usablePerPerson()
                            || candidate.place().priceEstimate().minimum() <= maxPricePerPerson))
                .filter(candidate -> confirmed.stream().noneMatch(p -> candidate.place().sameIdentityAs(p)))
                .filter(candidate -> condition.subtypes().isEmpty() || condition.subtypes().contains(PlaceFacets.subtype(candidate.place())))
                .filter(candidate -> condition.detailTypes().isEmpty() || condition.detailTypes().contains(PlaceFacets.detailType(candidate.place())))
                .filter(candidate -> condition.priceFilter().accepts(candidate.place().priceEstimate()))
                .filter(candidate -> condition.tags().stream().allMatch(candidate.place().details()::hasTag))
                .toList();
    }

}
