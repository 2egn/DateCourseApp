package com.datecourse.service;

import com.datecourse.model.TravelMode;
import com.datecourse.model.TravelRoute;
import com.datecourse.model.TravelRoute.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Compares door-to-door routes per leg, retaining the selected provider geometry and total time. */
final class TransitWalkingRouteService {
    @FunctionalInterface interface LegRouter {
        Leg route(Stop from,Stop to)throws IOException,InterruptedException;
    }
    private final LegRouter walking,transit;
    TransitWalkingRouteService(LegRouter walking,LegRouter transit){this.walking=walking;this.transit=transit;}

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
