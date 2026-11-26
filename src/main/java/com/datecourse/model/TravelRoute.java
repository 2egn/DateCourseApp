package com.datecourse.model;

import java.util.List;

/**
 * - API에서 조회한 실제 이동 경로 전달용 모델
 * - WGS84 좌표, 미터 거리, 초 단위 시간 사용. 영구 저장용 스냅샷과는 별도
 */
public record TravelRoute(List<Leg> legs, long totalDistance, long totalTime, TravelMode mode, String provider) {
    public TravelRoute(List<Leg> legs,long totalDistance,long totalTime) {this(legs,totalDistance,totalTime,TravelMode.WALKING,"TMAP");}
    public TravelRoute(List<Leg> legs,long totalDistance,long totalTime,TravelMode mode) {this(legs,totalDistance,totalTime,mode,"");}
    public TravelRoute {
        legs = List.copyOf(legs);mode=mode==null?TravelMode.WALKING:mode;provider=provider==null?"":provider;
        if(totalDistance<0||totalTime<0)throw new IllegalArgumentException("경로 거리·시간이 올바르지 않습니다.");
    }
    public static String durationText(long seconds) {
        if(seconds<0)throw new IllegalArgumentException("이동 시간은 0 이상이어야 합니다.");
        return (seconds==0?"0":"약 "+(seconds/60+(seconds%60==0?0:1)))+"분";
    }

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

    public record TransitStage(String type,String guidance,long distance,long time,List<Coordinate> path) {
        public TransitStage {path=List.copyOf(path);if(distance<0||time<0)throw new IllegalArgumentException("대중교통 단계가 올바르지 않습니다.");}
    }

    public record Leg(Stop from, Stop to, long distance, long time,
                      List<List<Coordinate>> paths, List<Step> steps, List<TransitStage> transitStages,
                      Integer fare, Integer transfers) {
        public Leg(Stop from,Stop to,long distance,long time,List<List<Coordinate>> paths,List<Step> steps) {
            this(from,to,distance,time,paths,steps,List.of(),null,null);
        }
        @com.fasterxml.jackson.annotation.JsonProperty("movementSummary")
        public String movementSummary() {
            return transitStages.stream().map(stage->switch(stage.type()) {
                case "WALKING"->"도보";case "BUS"->"버스";case "SUBWAY"->"지하철";default->"이동";
            }+" "+durationText(stage.time())).collect(java.util.stream.Collectors.joining(" → "));
        }
        public Leg {
            paths = paths.stream().map(List::copyOf).toList();
            steps = List.copyOf(steps);transitStages=List.copyOf(transitStages);
            if(distance<0||time<0||fare!=null&&fare<0||transfers!=null&&transfers<0)
                throw new IllegalArgumentException("구간 거리·시간·요금이 올바르지 않습니다.");
        }
    }
}
