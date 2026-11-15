package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.NextPlaceCondition;
import java.util.List;

/** 다음 장소 후보를 거리순으로 찾고, 전체 원형 범위 밖의 장소와 이미 확정한 장소를 제외한다. */
public final class NextPlaceService {
    public static final int DEFAULT_CANDIDATES = NextPlaceCondition.DEFAULT_DISPLAY_LIMIT;
    private final PlaceSearchService search = new PlaceSearchService();

    public List<PlaceSearchService.NearbyPlace> candidates(List<Place> places, List<Place> confirmed,
            double startLat, double startLng, int radius, String category,
            int maxPricePerPerson, int people, int routeRadius) {
        return candidates(places, confirmed, startLat, startLng, radius, category,
                maxPricePerPerson, people, routeRadius, false);
    }

    public List<PlaceSearchService.NearbyPlace> candidates(List<Place> places, List<Place> confirmed,
            double startLat, double startLng, int radius, String category,
            int maxPricePerPerson, int people, int routeRadius, boolean includeUncertainPrice) {
        return candidates(places, confirmed, startLat, startLng, people, routeRadius,
                new NextPlaceCondition(category, maxPricePerPerson, radius, includeUncertainPrice,
                        java.util.Set.of(), java.util.Set.of(), DEFAULT_CANDIDATES));
    }

    public List<PlaceSearchService.NearbyPlace> candidates(List<Place> places, List<Place> confirmed,
            double startLat, double startLng, int people, int routeRadius, NextPlaceCondition condition) {
        return matching(places, confirmed, startLat, startLng, people, routeRadius, condition).stream()
                .limit(condition.displayLimit()).toList();
    }

    public List<PlaceSearchService.NearbyPlace> matching(List<Place> places, List<Place> confirmed,
            double startLat, double startLng, int people, int routeRadius, NextPlaceCondition condition) {
        int maxPricePerPerson = condition.maxPrice();
        int radius = condition.radius();
        String category = condition.activity();
        boolean includeUncertainPrice = condition.includeUnknown();
        if (people < 1 || people > 20 || routeRadius < 200 || routeRadius > 2000 || maxPricePerPerson < 0)
            throw new IllegalArgumentException("가격·인원·전체 반경을 확인해 주세요.");
        Place last = confirmed.isEmpty() ? null : confirmed.get(confirmed.size() - 1);
        double lat = last == null ? startLat : last.lat();
        double lng = last == null ? startLng : last.lng();
        List<Place> inRouteRange = search.nearby(places, startLat, startLng, routeRadius, List.of(category))
                .stream().map(PlaceSearchService.NearbyPlace::place).toList();
        return search.nearby(inRouteRange, lat, lng, radius, List.of(category)).stream()
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
