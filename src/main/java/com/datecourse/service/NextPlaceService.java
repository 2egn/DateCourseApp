package com.datecourse.service;

import com.datecourse.model.Place;
import java.util.List;

/** 다음 장소 후보를 거리순으로 찾고, 전체 원형 범위 밖의 장소와 이미 확정한 장소를 제외한다. */
public final class NextPlaceService {
    public static final int MAX_CANDIDATES = 5;
    private final PlaceSearchService search = new PlaceSearchService();

    public List<PlaceSearchService.NearbyPlace> candidates(List<Place> places, List<Place> confirmed,
            double startLat, double startLng, int radius, String category,
            int maxPricePerPerson, int people, int routeRadius) {
        if (people < 1 || people > 20 || routeRadius < 200 || routeRadius > 2000 || maxPricePerPerson < 0)
            throw new IllegalArgumentException("가격·인원·전체 반경을 확인해 주세요.");
        Place last = confirmed.isEmpty() ? null : confirmed.get(confirmed.size() - 1);
        double lat = last == null ? startLat : last.lat();
        double lng = last == null ? startLng : last.lng();
        List<Place> inRouteRange = search.nearby(places, startLat, startLng, routeRadius, List.of(category))
                .stream().map(PlaceSearchService.NearbyPlace::place).toList();
        return search.nearby(inRouteRange, lat, lng, radius, List.of(category)).stream()
                .filter(candidate -> candidate.place().price() <= maxPricePerPerson)
                .filter(candidate -> confirmed.stream().noneMatch(p -> samePlace(candidate.place(), p)))
                .limit(MAX_CANDIDATES)
                .toList();
    }

    private boolean samePlace(Place candidate, Place confirmed) {
        return identity(candidate).equals(identity(confirmed)) || (candidate.name() != null
                && candidate.name().equals(confirmed.name())
                && candidate.lat() == confirmed.lat() && candidate.lng() == confirmed.lng());
    }

    private String identity(Place place) {
        return place.placeId() == null || place.placeId().isBlank() ? "record:" + place.recordId() : place.placeId();
    }
}
