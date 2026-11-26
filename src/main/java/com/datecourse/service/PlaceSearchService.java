package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.SearchCondition;
import java.util.*;

/**
 * - 전체 범위 안의 장소 조회와 동일 장소 중복 정리용 클래스
 * - 현재 편집은 nearby 결과 사용, 이전 전체 예산 기반 일괄 추천은 recommend에서 별도 처리
 */
public final class PlaceSearchService {
    private final PlaceCalculationService calculation = new PlaceCalculationService();
    public record NearbyPlace(Place place, double distanceMeters) {}
    public record Result(List<NearbyPlace> nearby, List<NearbyPlace> course,
                         long totalCost, List<String> missingCategories, boolean overBudget,
                         boolean insufficientPlaces) {}

    // 전체 반경 안의 유효한 장소를 조회하고 공공 ID 우선으로 중복 정리
    public List<NearbyPlace> nearby(List<Place> places, double lat, double lng,
                                    int radius, List<String> categories) {
        List<NearbyPlace> candidates = places.stream()
                .filter(p -> valid(p) && (categories.isEmpty() || categories.contains(categoryName(p.category()))))
                .map(p -> new NearbyPlace(p, calculation.calculateDistance(lat, lng, p.lat(), p.lng())))
                .filter(p -> p.distanceMeters() <= radius)
                .sorted(Comparator.comparing((NearbyPlace p) -> !p.place().hasPublicIdentity())
                        .thenComparingLong(p -> p.place().priceEstimate().sortUpperBound())
                        .thenComparingDouble(NearbyPlace::distanceMeters)
                        .thenComparingLong(p -> p.place().recordId()))
                .toList();
        Map<String,List<Place>> identities = new HashMap<>();
        Map<PlaceLocation,List<Place>> locations = new HashMap<>();
        List<NearbyPlace> distinct = new ArrayList<>();
        for (NearbyPlace candidate : candidates) {
            Place place = candidate.place();
            PlaceLocation location = new PlaceLocation(place.name(), place.lat(), place.lng());
            boolean duplicate = identities.getOrDefault(place.stableKey(),List.of()).stream().anyMatch(place::sameIdentityAs)
                    || locations.getOrDefault(location,List.of()).stream().anyMatch(place::sameIdentityAs);
            // 공공 ID를 먼저 확인해 이전 자료의 별칭으로 서로 다른 공공 장소가 합쳐지지 않도록 처리
            identities.computeIfAbsent(place.stableKey(),key->new ArrayList<>()).add(place);
            locations.computeIfAbsent(location,key->new ArrayList<>()).add(place);
            if (!duplicate) distinct.add(candidate);
        }
        return distinct.stream()
                .sorted(Comparator.comparingDouble(NearbyPlace::distanceMeters)
                        .thenComparingLong(p -> p.place().recordId()))
                .toList();
    }

    private record PlaceLocation(String name, double lat, double lng) {}

    // 이전 일괄 추천용 함수. 전체 예산과 활동별 방문 수를 기준으로 코스 구성
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
        // This older batch recommendation model has one exact total only.
        // Do not pretend an unknown/range price satisfies its whole-group budget.
        nearby.stream().filter(p -> p.place().priceEstimate().exact()).sorted(cheapest)
                .forEach(p -> unique.putIfAbsent(identity(p.place()), p));
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
        return place.stableKey();
    }

    public static String categoryName(String code) {
        return switch (code) { case "MEAL" -> "식사"; case "CAFE" -> "카페"; case "CULTURE" -> "문화"; default -> code; };
    }

    private boolean valid(Place p) {
        return Double.isFinite(p.lat()) && Double.isFinite(p.lng())
                && p.lat() >= -90 && p.lat() <= 90 && p.lng() >= -180 && p.lng() <= 180
                && !(p.lat() == 0 && p.lng() == 0);
    }
}
