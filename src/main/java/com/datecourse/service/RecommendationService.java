package com.datecourse.service;

import com.datecourse.domain.*;
import java.util.*;

/** 외부 시스템과 독립적인 추천 엔진. 각 카테고리 최대 20개 후보 안에서 탐색한다. */
public final class RecommendationService {
    private static final int CANDIDATE_LIMIT = 20;
    private static final int RESULT_LIMIT = 3;
    private static final Comparator<Course> BEST_FIRST = Comparator
            .comparingDouble(Course::score).reversed()
            .thenComparingLong(Course::upperCostWon)
            .thenComparing(c -> c.stops().stream().map(Place::id).toList().toString());

    public List<Course> recommend(RecommendationRequest request, List<Place> places) {
        Set<String> ids = new HashSet<>();
        for (Place place : places) {
            if (!ids.add(place.id())) throw new IllegalArgumentException("장소 ID가 중복됩니다: " + place.id());
        }
        List<List<Place>> pools = new ArrayList<>();
        for (Category category : request.categories()) {
            List<Place> pool = places.stream()
                    .filter(p -> p.category() == category)
                    .filter(p -> request.center().distanceMetersTo(p.location()) <= request.radiusMeters())
                    .filter(p -> p.upperCost(request.people()) <= request.budgetWon())
                    .sorted(Comparator.comparingDouble(Place::adjustedRating).reversed().thenComparing(Place::id))
                    .limit(CANDIDATE_LIMIT).toList();
            if (pool.isEmpty()) return List.of();
            pools.add(pool);
        }
        PriorityQueue<Course> best = new PriorityQueue<>(BEST_FIRST.reversed());
        search(request, pools, 0, new ArrayList<>(), 0, 0, best);
        return best.stream().sorted(BEST_FIRST).toList();
    }

    private void search(RecommendationRequest request, List<List<Place>> pools, int depth,
                        List<Place> stops, long lower, long upper, PriorityQueue<Course> best) {
        if (depth == pools.size()) {
            double distance = 0;
            GeoPoint previous = request.center();
            for (Place stop : stops) {
                distance += previous.distanceMetersTo(stop.location());
                previous = stop.location();
            }
            double rating = stops.stream().mapToDouble(Place::adjustedRating).average().orElseThrow();
            double uncertainty = stops.stream().filter(Place::estimatedPrice).count() / (double) stops.size();
            double score = rating - 0.15 * distance / 1000 - 0.1 * uncertainty;
            best.add(new Course(stops, lower, upper, distance, score));
            if (best.size() > RESULT_LIMIT) best.poll();
            return;
        }
        for (Place place : pools.get(depth)) {
            long cost = place.upperCost(request.people());
            if (cost > request.budgetWon() - upper) continue;
            stops.add(place);
            search(request, pools, depth + 1, stops,
                    lower + place.lowerCost(request.people()), upper + cost, best);
            stops.remove(stops.size() - 1);
        }
    }
}
