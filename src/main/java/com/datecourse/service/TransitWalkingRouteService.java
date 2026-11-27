package com.datecourse.service;

import com.datecourse.model.TravelMode;
import com.datecourse.model.TravelRoute;
import com.datecourse.model.TravelRoute.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * - 각 구간의 도보와 대중교통 시간을 비교해 이동 경로를 고르는 클래스
 * - TravelRouteService에서 도보·대중교통 조회 함수를 전달받아 구간별 비교
 */
final class TransitWalkingRouteService {
    @FunctionalInterface interface LegRouter {
        // 두 지점 사이의 경로 한 구간 조회용 함수
        Leg route(Stop from,Stop to)throws IOException,InterruptedException;
    }
    private final LegRouter walking,transit;
    TransitWalkingRouteService(LegRouter walking,LegRouter transit){this.walking=walking;this.transit=transit;}

    /**
     * 각 구간의 TMAP 도보 경로와 카카오 대중교통 경로를 총시간 기준으로 비교
     * 동률 또는 대중교통 경로 없음이면 도보 선택, 선택한 경로의 좌표·안내·거리·시간을 그대로 사용
     * 코스 전체 시간을 평균 내지 않고 선택된 구간별 시간을 합산, 이용한 제공사도 결과에 기록
     */
    TravelRoute route(List<Stop> input)throws IOException,InterruptedException {
        var stops=List.copyOf(input);
        if(stops.size()<2)throw new IllegalArgumentException("출발 위치와 방문할 장소를 선택해 주세요.");
        var legs=new ArrayList<Leg>();long distance=0,time=0;boolean usedWalking=false,usedTransit=false;
        for(int i=1;i<stops.size();i++) {
            if(Thread.currentThread().isInterrupted())throw new InterruptedException();
            Stop from=stops.get(i-1),to=stops.get(i);Leg selected;
            if(from.lat()==to.lat()&&from.lng()==to.lng())selected=new Leg(from,to,0,0,List.of(),List.of());
            else {
                Leg walk=walking.route(from,to),ride;
                if(Thread.currentThread().isInterrupted())throw new InterruptedException();
                try{ride=transit.route(from,to);}
                // 대중교통 경로 없음만 도보로 대체, 인증·통신 오류는 호출부에 전달
                catch(KakaoRouteService.NoTransitRouteException noRoute){ride=null;}
                if(ride==null||walk.time()<=ride.time()) {
                    selected=new Leg(from,to,walk.distance(),walk.time(),walk.paths(),walk.steps(),
                            List.of(new TransitStage("WALKING","도보로 이동",walk.distance(),walk.time(),List.of())),0,0);
                    usedWalking=true;
                }else {selected=ride;usedTransit=true;}
            }
            legs.add(selected);distance=Math.addExact(distance,selected.distance());time=Math.addExact(time,selected.time());
        }
        String provider=usedWalking?(usedTransit?"TMAP·카카오":"TMAP"):usedTransit?"카카오":"";
        return new TravelRoute(legs,distance,time,TravelMode.TRANSIT,provider);
    }
}
