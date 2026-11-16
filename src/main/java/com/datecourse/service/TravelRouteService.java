package com.datecourse.service;

import com.datecourse.model.*;
import com.datecourse.model.TravelRoute.Stop;
import java.util.List;

/** Minimal routing boundary: UI selects a mode, adapters own provider details. */
public final class TravelRouteService {
    private final WalkingRouteService walking=new WalkingRouteService();
    private final KakaoRouteService kakao=new KakaoRouteService();
    public TravelRoute route(List<Stop> stops,TravelMode mode)throws Exception {
        return mode==TravelMode.WALKING?walking.route(stops,TmapSettings.load()):kakao.route(stops,mode,KakaoRouteSettings.load());
    }
}
