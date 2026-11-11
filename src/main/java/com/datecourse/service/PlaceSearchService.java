package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.SearchCondition;
import java.util.*;

/** Includes each selected activity, then adds affordable distinct places within the requested count. */
public final class PlaceSearchService {
    private final PlaceCalculationService calculation = new PlaceCalculationService();
    public record NearbyPlace(Place place, double distanceMeters) {}
    public record Result(List<NearbyPlace> nearby, List<NearbyPlace> course,
                         long totalCost, List<String> missingCategories, boolean overBudget,
                         boolean insufficientPlaces) {}

    public List<NearbyPlace> nearby(List<Place> places, double lat, double lng,
                                    int radius, List<String> categories) {
        List<NearbyPlace> candidates = places.stream()
                .filter(p -> valid(p) && (categories.isEmpty() || categories.contains(categoryName(p.category()))))
                .map(p -> new NearbyPlace(p, calculation.calculateDistance(lat, lng, p.lat(), p.lng())))
                .filter(p -> p.distanceMeters() <= radius)
                .sorted(Comparator.comparingInt((NearbyPlace p) -> p.place().price())
                        .thenComparingDouble(NearbyPlace::distanceMeters)
                        .thenComparingLong(p -> p.place().recordId()))
                .toList();
        Set<String> ids = new HashSet<>();
        Set<PlaceLocation> locations = new HashSet<>();
        List<NearbyPlace> distinct = new ArrayList<>();
        for (NearbyPlace candidate : candidates) {
            Place place = candidate.place();
            PlaceLocation location = place.name() == null || place.name().isBlank() ? null
                    : new PlaceLocation(place.name(), place.lat(), place.lng());
            boolean duplicate = ids.contains(identity(place))
                    || (location != null && locations.contains(location));
            // 중복 행의 ID도 기억해 이후 같은 ID가 다시 들어오는 것을 막는다.
            ids.add(identity(place));
            if (location != null) locations.add(location);
            if (!duplicate) distinct.add(candidate);
        }
        return distinct.stream()
                .sorted(Comparator.comparingDouble(NearbyPlace::distanceMeters)
                        .thenComparingLong(p -> p.place().recordId()))
                .toList();
    }

    private record PlaceLocation(String name, double lat, double lng) {}

    public Result recommend(List<Place> places, SearchCondition condition) {
        if (condition.people() < 1 || condition.budget() < 1 || condition.categories().isEmpty())
            throw new IllegalArgumentException("예산·인원·분류를 확인해 주세요.");
        List<String> categories = new ArrayList<>(new LinkedHashSet<>(condition.categories()));
        if (condition.minPlaces() < 1 || condition.maxPlaces() > 10
                || condition.minPlaces() > condition.maxPlaces() || condition.maxPlaces() < categories.size())
            throw new IllegalArgumentException("방문 장소 수는 1~10곳, 최소≤최대이며 최대는 선택한 분류 수 이상이어야 합니다.");
        List<NearbyPlace> nearby = nearby(places, condition.lat(), condition.lng(), condition.radius(), condition.categories());
        Comparator<NearbyPlace> cheapest = Comparator.comparingInt((NearbyPlace p) -> p.place().price())
                .thenComparingDouble(NearbyPlace::distanceMeters).thenComparingLong(p -> p.place().recordId());
        Map<String, NearbyPlace> unique = new LinkedHashMap<>();
        nearby.stream().sorted(cheapest).forEach(p -> unique.putIfAbsent(identity(p.place()), p));
        List<NearbyPlace> course = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        Set<String> chosen = new HashSet<>();
        for (String category : categories) {
            Optional<NearbyPlace> choice = unique.values().stream()
                    .filter(p -> category.equals(categoryName(p.place().category())))
                    .min(cheapest);
            if (choice.isPresent()) {
                course.add(choice.get()); chosen.add(identity(choice.get().place()));
            } else missing.add(category);
        }
        List<NearbyPlace> remaining = unique.values().stream()
                .filter(p -> !chosen.contains(identity(p.place()))).sorted(cheapest).toList();
        int next = 0;
        int minimum = Math.max(condition.minPlaces(), categories.size());
        while (course.size() < minimum && next < remaining.size()) course.add(remaining.get(next++));
        long total = course.stream().mapToLong(p -> (long) p.place().price() * condition.people()).sum();
        boolean insufficient = course.size() < minimum;
        if (!missing.isEmpty() || insufficient || total > condition.budget()) {
            return new Result(nearby, List.of(), total, List.copyOf(missing), total > condition.budget(), insufficient);
        }
        while (course.size() < condition.maxPlaces() && next < remaining.size()) {
            NearbyPlace candidate = remaining.get(next++);
            long cost = (long) candidate.place().price() * condition.people();
            if (total + cost > condition.budget()) break;
            course.add(candidate); total += cost;
        }
        course.sort(Comparator.comparingInt((NearbyPlace p) -> categories.indexOf(categoryName(p.place().category())))
                .thenComparingDouble(NearbyPlace::distanceMeters).thenComparingLong(p -> p.place().recordId()));
        return new Result(nearby, List.copyOf(course), total, List.of(), false, false);
    }

    private String identity(Place place) {
        return place.placeId() != null && !place.placeId().isBlank() ? place.placeId() : "record:" + place.recordId();
    }

    public static String categoryName(String code) {
        return switch (code) { case "MEAL" -> "식사"; case "CAFE" -> "카페"; case "CULTURE" -> "문화"; default -> code; };
    }

    private boolean valid(Place p) {
        return Double.isFinite(p.lat()) && Double.isFinite(p.lng())
                && p.lat() >= -90 && p.lat() <= 90 && p.lng() >= -180 && p.lng() <= 180
                && !(p.lat() == 0 && p.lng() == 0) && p.price() >= 0;
    }
}
