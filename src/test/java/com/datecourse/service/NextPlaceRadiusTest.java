package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class NextPlaceRadiusTest {
    @Test void firstPlaceNeedsOnlyTheGlobalRadius() {
        assertEquals(500, NextPlaceRadius.maximum(new RoutePlan(35,129,2,500,"",List.of())));
        assertEquals(200, NextPlaceRadius.maximum(new RoutePlan(35,129,2,200,"",List.of())));
    }

    @Test void shiftedCenterIncludesFarEdgeWithoutExceedingModelLimit() {
        var previous = new Place(1,"p1","카페","CAFE",35.002,129,5000,4);
        var plan = new RoutePlan(35,129,2,500,"",List.of(previous));
        double offset = new PlaceCalculationService().calculateDistance(35,129,previous.lat(),previous.lng());
        int maximum = NextPlaceRadius.maximum(plan);
        assertTrue(maximum >= 500 + offset);
        assertTrue(maximum - 100 < 500 + offset);
        assertEquals(2000, NextPlaceRadius.maximum(new RoutePlan(35,129,2,2000,"",List.of(previous))));
    }
    @Test void motorizedModesExpandAndStillStopAtTheFarEdge() {
        var car=new RoutePlan(35,129,2,30000,"",List.of()).withTravelMode(com.datecourse.model.TravelMode.DRIVING);
        var transit=new RoutePlan(35,129,2,20000,"",List.of()).withTravelMode(com.datecourse.model.TravelMode.TRANSIT);
        assertEquals(30000,NextPlaceRadius.maximum(car));assertEquals(20000,NextPlaceRadius.maximum(transit));
        var previous=new Place(1,"far","먼 장소","CAFE",35.1,129,5000,0);
        var shifted=new RoutePlan(35,129,2,20000,"",List.of(previous)).withTravelMode(com.datecourse.model.TravelMode.TRANSIT);
        int maximum=NextPlaceRadius.maximum(shifted);
        assertTrue(maximum>30000&&maximum<=40000);assertEquals(20000,NextPlaceRadius.maximum(shifted,0));
        assertEquals(2000,com.datecourse.model.TravelMode.WALKING.maximumRadius());
    }
    @Test void searchAndRecoverySupportPlacesBeyondTheFormerTwoKilometerLimit() {
        var far=new Place(1,"far","먼 카페","CAFE",35.1,129,5000,0);
        var route=new RoutePlan(35,129,2,20000,"",List.of()).withTravelMode(com.datecourse.model.TravelMode.TRANSIT);
        var narrow=new com.datecourse.model.NextPlaceCondition("카페",10000,500,false,java.util.Set.of(),java.util.Set.of(),5);
        var advice=new CandidateRecoveryService().analyze(List.of(far),route,narrow,NextPlaceRadius.maximum(route));
        assertTrue(advice.suggestions().stream().anyMatch(x->x.change()==CandidateRecoveryService.Change.EXPAND_RADIUS));
        var expanded=new com.datecourse.model.NextPlaceCondition("카페",10000,20000,false,java.util.Set.of(),java.util.Set.of(),5);
        assertEquals(1,new NextPlaceService().matching(List.of(far),List.of(),35,129,2,20000,expanded).size());
        assertThrows(IllegalArgumentException.class,()->new com.datecourse.model.NextPlaceCondition("카페",10000,60100,false,java.util.Set.of(),java.util.Set.of(),5));
    }
}
