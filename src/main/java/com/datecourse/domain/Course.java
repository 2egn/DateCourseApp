package com.datecourse.domain;

import java.util.List;

public record Course(List<Place> stops, long lowerCostWon, long upperCostWon,
                     double distanceMeters, double score) {
    public Course { stops = List.copyOf(stops); }
}
