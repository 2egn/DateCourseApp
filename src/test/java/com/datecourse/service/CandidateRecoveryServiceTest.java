package com.datecourse.service;

import com.datecourse.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CandidateRecoveryServiceTest {
    private final CandidateRecoveryService recovery=new CandidateRecoveryService();
    private final RoutePlan route=new RoutePlan(35.1578,129.06,2,2000,"",List.of());
    private NextPlaceCondition condition(boolean unknown){return new NextPlaceCondition("식사",10000,500,unknown,Set.of(),Set.of(),5);}
    private Place unknown(double lat){return new Place(1,"unknown","가격 없는 식당","MEAL",lat,129.06,PriceEstimate.unknown(),0,PlaceDetails.EMPTY);}
    @Test void unknownRecoveryIsOfferedOnlyWhenItProducesMatchesWithoutChangingCondition() {
        var filter=condition(false);var advice=recovery.analyze(List.of(unknown(35.158)),route,filter,2000);
        assertEquals(List.of(new CandidateRecoveryService.Suggestion(CandidateRecoveryService.Change.INCLUDE_UNKNOWN,1)),advice.suggestions());
        assertFalse(filter.includeUnknown());
    }
    @Test void doesNotBlamePriceWhenPriceAndRadiusWouldBothNeedToChange() {
        var advice=recovery.analyze(List.of(unknown(35.167)),route,condition(false),2000);
        assertTrue(advice.suggestions().isEmpty());assertTrue(advice.message().contains("여러 조건"));
    }
    @Test void radiusRecoveryHonorsAvailableLimitAndNeverIncludesAlreadyChosenPlaces() {
        var place=new Place(2,"known","식당","MEAL",35.167,129.06,8000,0);
        assertEquals(List.of(new CandidateRecoveryService.Suggestion(CandidateRecoveryService.Change.EXPAND_RADIUS,1)),
                recovery.analyze(List.of(place),route,condition(false),2000).suggestions());
        assertTrue(recovery.analyze(List.of(place),route,condition(false),500).suggestions().isEmpty());
        var chosen=new RoutePlan(route.startLat(),route.startLng(),2,2000,"",List.of(place));
        assertTrue(recovery.analyze(List.of(place),chosen,condition(true),2000).suggestions().isEmpty());
    }
    @Test void noRecoveryIsShownForSuccessfulSearchOrUnavailableActivity() {
        var place=new Place(2,"known","식당","MEAL",35.158,129.06,8000,0);
        assertTrue(recovery.analyze(List.of(place),route,condition(false),2000).message().isEmpty());
        var advice=recovery.analyze(List.of(),route,condition(false),2000);
        assertTrue(advice.message().contains("전체 범위"));assertTrue(advice.suggestions().isEmpty());
    }
}
