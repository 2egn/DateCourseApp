package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RouteRangeValidatorTest {
    private final RouteRangeValidator validator = new RouteRangeValidator();
    private Place place(long id, double latitude) {
        return new Place(id, "p" + id, "장소" + id, "CAFE", latitude, 129, 3000, 4);
    }
    private RoutePlan plan(double origin, int radius, List<Place> places) {
        return new RoutePlan(origin, 129, 2, radius, "", places);
    }

    @Test void shrinkingRadiusAndMovingOriginRevalidateExistingPlaces() {
        Place near = place(1, 35.001), far = place(2, 35.003);
        assertTrue(validator.outsideRange(plan(35, 500, List.of(near,far))).isEmpty());
        assertEquals(List.of(far), validator.outsideRange(plan(35, 200, List.of(near,far))));
        assertEquals(List.of(near,far), validator.outsideRange(plan(35.01, 500, List.of(near,far))));
    }

    @Test void includesExactBoundaryAndDoesNotDependOnOrder() {
        double boundary = Math.toDegrees(200.0 / 6_371_000.0);
        Place edge = place(1, boundary), outside = place(2, boundary + .000001);
        assertEquals(List.of(outside), validator.outsideRange(plan(0, 200, List.of(edge,outside))));
        assertEquals(List.of(outside), validator.outsideRange(plan(0, 200, List.of(outside,edge))));
        assertTrue(validator.outsideRange(plan(0,200,List.of())).isEmpty());
    }

    @Test void limitsFollowDestinationAfterReorderingDeletionAndDepartureChange() {
        Place a=place(1,35.001), b=place(2,35.002), c=place(3,35.003);
        var limits=new java.util.HashMap<>(java.util.Map.of(1L,200,2L,300,3L,200));
        var original=new RoutePlan(35,129,2,1000,"",List.of(a,b,c),limits);
        limits.put(3L,2000);
        assertTrue(validator.legs(original).stream().noneMatch(RouteRangeValidator.Leg::exceeded));
        assertEquals(200,original.arrivalRadii().get(3L));
        assertThrows(UnsupportedOperationException.class,()->original.arrivalRadii().put(3L,500));
        var reordered=new RoutePlan(35,129,2,1000,"",List.of(c,a,b),original.arrivalRadii());
        assertEquals(List.of(c,a),validator.legs(reordered).stream().filter(RouteRangeValidator.Leg::exceeded).map(RouteRangeValidator.Leg::to).toList());
        var deleted=new RoutePlan(35,129,2,1000,"",List.of(a,c),original.arrivalRadii());
        assertEquals(List.of(c),validator.legs(deleted).stream().filter(RouteRangeValidator.Leg::exceeded).map(RouteRangeValidator.Leg::to).toList());
        var moved=new RoutePlan(34.999,129,2,1000,"",List.of(a,b,c),original.arrivalRadii());
        assertTrue(validator.legs(moved).get(0).exceeded());
        assertNull(validator.legs(plan(35,1000,List.of(a))).get(0).radius());
    }

    @Test void legBoundaryIsIncluded() {
        Place edge=place(1,Math.toDegrees(200.0/6_371_000.0));
        var plan=new RoutePlan(0,129,2,1000,"",List.of(edge),java.util.Map.of(1L,200));
        assertFalse(validator.legs(plan).get(0).exceeded());
    }
}
