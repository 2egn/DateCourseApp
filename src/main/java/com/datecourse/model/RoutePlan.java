package com.datecourse.model;

import java.util.List;
import java.util.Map;

/** 확정본과 편집본 사이에 전달하는 불변 상태. 장소 가격은 항상 1인 기준이다. */
public record RoutePlan(double startLat, double startLng, int people, int radius,
                        String title, List<Place> places, Map<Long, Integer> arrivalRadii, TravelMode travelMode) {
    public RoutePlan(double startLat,double startLng,int people,int radius,String title,List<Place> places,Map<Long,Integer> arrivalRadii) {
        this(startLat,startLng,people,radius,title,places,arrivalRadii,TravelMode.WALKING);
    }
    public RoutePlan withTravelMode(TravelMode mode) {return new RoutePlan(startLat,startLng,people,radius,title,places,arrivalRadii,mode);}
    // Older callers have no recorded per-place condition; do not invent one.
    public RoutePlan(double startLat, double startLng, int people, int radius, String title, List<Place> places) {
        this(startLat, startLng, people, radius, title, places, Map.of());
    }

    public RoutePlan {
        travelMode=travelMode==null?TravelMode.WALKING:travelMode;
        places = List.copyOf(places);
        arrivalRadii = Map.copyOf(arrivalRadii);
        if (arrivalRadii.values().stream().anyMatch(value -> value < TravelMode.MIN_RADIUS || value > TravelMode.MAX_LEG_RADIUS))
            throw new IllegalArgumentException("장소별 이동 반경이 지원 범위를 벗어났습니다.");
    }

    public CostEstimate costEstimate() { return CostEstimate.of(places, people); }
    public long totalCost() { return costEstimate().exactTotal(); }
}
