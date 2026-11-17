package com.datecourse.service;

import com.datecourse.model.*;
import com.datecourse.model.TravelRoute.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TransitWalkingRouteServiceTest {
    final Stop start=new Stop(35.15,129.11,"출발"),near=new Stop(35.151,129.111,"근처"),far=new Stop(35.17,129.13,"다음 장소");
    Leg walk(Stop from,Stop to,long seconds){return new Leg(from,to,100,seconds,
            List.of(List.of(new Coordinate(from.lat(),from.lng()),new Coordinate(to.lat(),to.lng()))),List.of(new Step("횡단보도 이용",null)));}
    Leg ride(Stop from,Stop to){return new Leg(from,to,1000,600,walk(from,to,0).paths(),List.of(),
            List.of(new TransitStage("WALKING","승차 정류장까지",100,60,List.of()),
                    new TransitStage("BUS","54번",800,420,List.of()),
                    new TransitStage("WALKING","하차 후 목적지까지",100,120,List.of())),1550,0);}

    @Test void selectsWalkingAndTransitPerLegAndCountsAccessWalkOnlyOnce()throws Exception {
        var transit=ride(near,far);
        var service=new TransitWalkingRouteService((a,b)->walk(a,b,b.equals(near)?180:1200),
                (a,b)->b.equals(far)?transit:ride(a,b));
        var route=service.route(List.of(start,near,far));
        assertEquals(TravelMode.TRANSIT,route.mode());assertEquals("TMAP·카카오",route.provider());
        assertEquals(780,route.totalTime());assertEquals(1100,route.totalDistance());
        var first=route.legs().getFirst();assertEquals(0,first.fare());assertEquals(0,first.transfers());
        assertEquals("WALKING",first.transitStages().getFirst().type());
        assertEquals(walk(start,near,180).paths(),first.paths());assertSame(transit,route.legs().getLast());
        assertEquals("도보 약 1분 → 버스 약 7분 → 도보 약 2분",transit.movementSummary());
        assertTrue(new ObjectMapper().valueToTree(route).path("legs").get(1).path("movementSummary").asText().contains("버스"));
    }
    @Test void noTransitResultUsesVerifiedWalkingGeometry()throws Exception {
        var route=new TransitWalkingRouteService((a,b)->walk(a,b,180),(a,b)->{throw new KakaoRouteService.NoTransitRouteException();}).route(List.of(start,near));
        assertEquals(180,route.totalTime());assertEquals("TMAP",route.provider());
        assertEquals(walk(start,near,180).paths(),route.legs().getFirst().paths());
    }
    @Test void equalTimePrefersWalkingWithoutInventingDistanceCutoff()throws Exception {
        var route=new TransitWalkingRouteService((a,b)->walk(a,b,600),this::ride).route(List.of(start,far));
        assertEquals("TMAP",route.provider());assertEquals(0,route.legs().getFirst().fare());
    }
    @Test void providerAuthQuotaMalformedAndNetworkFailuresAreNotTreatedAsNoTransit() {
        for(IOException failure:List.of(new AppDiagnostics.HttpFailure(403,"권한"),new AppDiagnostics.HttpFailure(429,"한도"),new IOException("네트워크"))) {
            var service=new TransitWalkingRouteService((a,b)->walk(a,b,180),(a,b)->{throw failure;});
            assertSame(failure,assertThrows(IOException.class,()->service.route(List.of(start,near))));
        }
        var failure=new IOException("도보 조회 실패");
        assertSame(failure,assertThrows(IOException.class,()->new TransitWalkingRouteService((a,b)->{throw failure;},this::ride).route(List.of(start,near))));
    }
    @Test void cancellationStopsBeforeOtherProviderAndIdenticalStopsNeedNoRequests()throws Exception {
        var calls=new AtomicInteger();
        var same=new TransitWalkingRouteService((a,b)->{calls.incrementAndGet();return walk(a,b,0);},(a,b)->{calls.incrementAndGet();return ride(a,b);}).route(List.of(start,start));
        assertEquals(0,same.totalTime());assertEquals(0,calls.get());
        try {
            var service=new TransitWalkingRouteService((a,b)->{Thread.currentThread().interrupt();return walk(a,b,180);},(a,b)->{fail("Cancelled before transit request");return null;});
            assertThrows(InterruptedException.class,()->service.route(List.of(start,near)));
        }finally{Thread.interrupted();}
    }
    @Test void onlyDocumentedUnavailableStatusesPermitWalkingFallback()throws Exception {
        var service=new KakaoRouteService();var json=new ObjectMapper();
        for(String status:List.of("NO_RESULTS","STARTNODES_NULL","ENDNODES_NULL","EQUAL_POINTS"))
            assertThrows(KakaoRouteService.NoTransitRouteException.class,()->service.transit(json.readTree("{\"status\":\""+status+"\"}"),start,near));
        for(String status:List.of("INVALID_REQUEST","UNKNOWN","")) {
            var failure=assertThrows(IOException.class,()->service.transit(json.readTree("{\"status\":\""+status+"\"}"),start,near));
            assertFalse(failure instanceof KakaoRouteService.NoTransitRouteException);
        }
    }
}
