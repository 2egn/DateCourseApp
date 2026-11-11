package com.datecourse.model;

import java.util.List;

/** TMAP walking geometry uses WGS84 degrees; distance is meters and time is seconds. */
public record WalkingRoute(List<Leg> legs, long totalDistance, long totalTime) {
    public WalkingRoute { legs = List.copyOf(legs); }

    public record Coordinate(double lat, double lng) {
        public Coordinate {
            if (!Double.isFinite(lat) || !Double.isFinite(lng) || lat < -90 || lat > 90 || lng < -180 || lng > 180)
                throw new IllegalArgumentException("유효한 위도·경도가 필요합니다.");
        }
    }

    public record Stop(double lat, double lng, String name) {
        public Stop {
            new Coordinate(lat, lng);
            name = name == null || name.isBlank() ? "선택 위치" : name;
        }
    }

    public record Step(String description, Coordinate position) {}

    public record Leg(Stop from, Stop to, long distance, long time,
                      List<List<Coordinate>> paths, List<Step> steps) {
        public Leg {
            paths = paths.stream().map(List::copyOf).toList();
            steps = List.copyOf(steps);
        }
    }
}
