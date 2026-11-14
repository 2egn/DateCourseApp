package com.datecourse.service;

import com.datecourse.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlacePresenterTest {
    final PlacePresenter presenter=new PlacePresenter();
    static Place facility(String id,FactState state) {
        var evidence=new SourceEvidence(SourceEvidence.Origin.OFFICIAL_SITE,"운영 안내","https://example.test","2026-11-14","");
        var details=new PlaceDetails("주소","한식","","","https://example.test","2026-11-14","","",Map.of("public",id),List.of(),
                state==FactState.UNKNOWN?List.of():List.of(new PlaceAttributeFact(PlaceFeature.RESERVABLE,state,evidence)),List.of());
        return new Place(id.hashCode(),id,"가게","MEAL",35.1578,129.06,PriceEstimate.exact(5000),0,details);
    }
    @Test void publicFactsHaveConciseLabelsAndPreserveCalculationContext() throws Exception {
        var place=new PublicDemoPlaceRepository().findAll().getFirst();
        var view=presenter.present(place);
        assertEquals("추정 가격",view.priceLabel());
        assertTrue(view.priceSummary().contains("부산 착한가격업소"));
        assertFalse(view.priceSummary().contains("115곳"));assertFalse(view.priceSummary().contains("https:"));
        var station=view.badges().stream().filter(b->b.code().equals("NEAR_SUBWAY")).findFirst().orElseThrow();
        assertTrue(station.description().contains("직선"));assertTrue(station.description().contains("자료 2022-11-30"));
        assertTrue(station.description().contains("계산 2026-11-14"));
        assertTrue(view.candidateSummary().contains("착한가격"));
    }
    @Test void typedFeaturesUseSameDomainInterpretationForBadgesAndFiltering() {
        var positive=facility("P:A",FactState.AVAILABLE);var negative=facility("P:B",FactState.UNAVAILABLE);var unknown=facility("P:C",FactState.UNKNOWN);
        assertEquals(List.of(PlaceTag.RESERVABLE),positive.details().availableTags());
        assertTrue(negative.details().availableTags().isEmpty());assertTrue(unknown.details().availableTags().isEmpty());
        assertEquals("예약 가능",presenter.present(positive).badges().getFirst().label());
        assertTrue(presenter.present(negative).badges().isEmpty());assertTrue(presenter.present(unknown).badges().isEmpty());
        var condition=new NextPlaceCondition("식사",10000,500,false,Set.of(),Set.of(PlaceTag.RESERVABLE),20);
        assertEquals(List.of(positive),new NextPlaceService().matching(List.of(positive,negative,unknown),List.of(),35.1578,129.06,2,500,condition)
                .stream().map(PlaceSearchService.NearbyPlace::place).toList());
    }
    @Test void mapPresentationIsNotPartOfPersistedPlaceData() throws Exception {
        var place=facility("P:A",FactState.AVAILABLE);var json=new ObjectMapper();
        var payload=new PlaceMapPayload(json,presenter);
        assertTrue(payload.place(place).has("presentation"));
        assertFalse(json.valueToTree(place).has("presentation"));
        assertEquals(place,json.readValue(json.writeValueAsBytes(place),Place.class));
        assertEquals("예약 가능",payload.plan(new RoutePlan(35.1578,129.06,2,500,"",List.of(place))).path("places").get(0)
                .path("presentation").path("badges").get(0).path("label").asText());
    }
    @Test void publicIdentitySurvivesSourceChangesAndReviewedNameChanges() {
        var old=facility("PUBLIC:A",FactState.AVAILABLE);
        var current=new Place(999,"canonical-id","정정된 가게","MEAL",35.1579,129.06,old.priceEstimate(),0,old.details());
        assertTrue(old.sameIdentityAs(current));
        assertTrue(new NextPlaceService().candidates(List.of(current),List.of(old),35.1578,129.06,500,"식사",10000,2,500).isEmpty());
        assertFalse(old.sameIdentityAs(facility("PUBLIC:B",FactState.AVAILABLE)));
    }
    @Test void unknownPriceNeverBecomesFreeOrAProviderVerificationClaim() {
        var place=new Place(1,"unknown","장소","CULTURE",35,129,PriceEstimate.unknown(),0,PlaceDetails.EMPTY);
        var view=presenter.present(place);
        assertEquals("가격 미확인",view.priceText());assertTrue(view.candidateSummary().contains("미확인"));
        assertFalse(view.informationSummary().contains("Google"));assertTrue(view.informationSummary().contains("확인 전"));
    }
}
