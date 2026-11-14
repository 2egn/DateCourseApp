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
}
