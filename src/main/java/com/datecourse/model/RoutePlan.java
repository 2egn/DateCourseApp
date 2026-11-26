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
    // 이전 호출부에는 장소별 반경 기록이 없으므로 임의의 조건을 추가하지 않음
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

    // 현재 방문 장소와 인원으로 예상 비용 범위 계산
    public CostEstimate costEstimate() { return CostEstimate.of(places, people); }
    // 현재 코스를 확정된 단일 비용으로 계산할 수 있는지 확인 후 반환
    public long totalCost() { return costEstimate().exactTotal(); }
}
