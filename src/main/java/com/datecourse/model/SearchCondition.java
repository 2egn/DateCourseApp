package com.datecourse.model;

import java.util.List;

public record SearchCondition(
        int budget,
        int people,
        List<String> categories,
        double lat,
        double lng,
        int radius,
        int minPlaces,
        int maxPlaces
) {
    /** Existing callers retain the original one-place-per-category behavior. */
    public SearchCondition(int budget, int people, List<String> categories, double lat, double lng, int radius) {
        this(budget, people, categories, lat, lng, radius,
                Math.max(1, new java.util.HashSet<>(categories).size()),
                Math.max(1, new java.util.HashSet<>(categories).size()));
    }
}
