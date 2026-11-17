package com.datecourse.service;

import com.datecourse.model.*;
import com.datecourse.model.TravelRoute.Stop;
import java.util.List;

/** Minimal routing boundary: UI selects a mode, adapters own provider details. */
public final class TravelRouteService {
    private final WalkingRouteService walking=new WalkingRouteService();
    private final KakaoRouteService kakao=new KakaoRouteService();
    public TravelRoute route(List<Stop> stops,TravelMode mode)throws Exception {
        if(mode==TravelMode.WALKING)return walking.route(stops,TmapSettings.load());
        var kakaoSettings=KakaoRouteSettings.load();
        if(mode==TravelMode.DRIVING)return kakao.route(stops,mode,kakaoSettings);
        var walkSettings=TmapSettings.load();
        return new TransitWalkingRouteService(
                (from,to)->walking.route(List.of(from,to),walkSettings).legs().getFirst(),
                (from,to)->kakao.route(List.of(from,to),TravelMode.TRANSIT,kakaoSettings).legs().getFirst()).route(stops);
    }
}
