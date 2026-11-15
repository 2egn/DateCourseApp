package com.datecourse.service;

import com.datecourse.model.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ExpandedPlaceFiltersTest {
    private Place place(long id,String type,PriceEstimate price,PlaceTag...tags){
        var facts=Arrays.stream(tags).map(t->new PlaceDetails.TagFact(t,"확인 자료","","2026-11-15")).toList();
        var details=new PlaceDetails("",type,"","","","","","",Map.of(),facts);
        return new Place(id,"p"+id,"메뉴를 추측하지 않는 가게 "+id,"MEAL",35.1578,129.06,price,0,details);
    }
    private NextPlaceCondition condition(Set<String> types,Set<PlaceTag> tags,PlacePriceFilter price){
        return new NextPlaceCondition("식사",20000,500,true,Set.of(),tags,5,types,price);
    }
    private List<Place> search(List<Place> places,NextPlaceCondition condition){
        return new NextPlaceService().matching(places,List.of(),35.1578,129.06,2,2000,condition).stream().map(PlaceSearchService.NearbyPlace::place).toList();
    }
    @Test void detailedTypesSeparateBusinessesWithinTheSameBroadCuisine(){
        var soup=place(1,"국/탕/찌개류",PriceEstimate.exact(8000));
        var noodles=place(2,"냉면/밀면",PriceEstimate.exact(8000));
        assertEquals("한식",PlaceFacets.subtype(soup));assertEquals("한식",PlaceFacets.subtype(noodles));
        assertEquals(List.of(noodles),search(List.of(soup,noodles),condition(Set.of("냉면/밀면"),Set.of(),PlacePriceFilter.ANY)));
    }
    @Test void allSelectedFeaturesMustBeConfirmedAndUnknownDoesNotPass(){
        var both=place(1,"백반/한정식",PriceEstimate.exact(8000),PlaceTag.GOOD_PRICE,PlaceTag.NEAR_SUBWAY);
        var one=place(2,"백반/한정식",PriceEstimate.exact(8000),PlaceTag.GOOD_PRICE);
        var none=place(3,"백반/한정식",PriceEstimate.exact(8000));
        assertEquals(List.of(both),search(List.of(both,one,none),condition(Set.of(),Set.of(PlaceTag.GOOD_PRICE,PlaceTag.NEAR_SUBWAY),PlacePriceFilter.ANY)));
    }
    @Test void priceEvidenceFiltersNeverTreatUnknownAsFreeOrReference(){
        var reference=place(1,"한식",PriceEstimate.exact(8000));
        var estimate=place(2,"한식",new PriceEstimate(6000,9000,PriceEstimate.Unit.PER_PERSON,"유사 업종",PriceEstimate.Evidence.SIMILAR_ESTIMATE,"",""));
        var unknown=place(3,"한식",PriceEstimate.unknown());var free=place(4,"한식",PriceEstimate.exact(0));
        var estimatedZero=place(5,"한식",new PriceEstimate(0,0,PriceEstimate.Unit.PER_PERSON,"가정",PriceEstimate.Evidence.DEMO_ASSUMPTION,"",""));
        assertFalse(PlacePriceFilter.FREE.accepts(estimatedZero.priceEstimate()));
        var all=List.of(reference,estimate,unknown,free);
        assertEquals(List.of(free),search(all,condition(Set.of(),Set.of(),PlacePriceFilter.FREE)));
        assertEquals(List.of(estimate),search(all,condition(Set.of(),Set.of(),PlacePriceFilter.ESTIMATED)));
        assertEquals(Set.of(reference,free),new HashSet<>(search(all,condition(Set.of(),Set.of(),PlacePriceFilter.REFERENCE))));
    }
    @Test void recoveryKeepsNewFiltersUnlessExplicitlyClearingAdvancedConditions(){
        var place=place(1,"국/탕/찌개류",PriceEstimate.exact(8000));
        var wrong=condition(Set.of("냉면/밀면"),Set.of(),PlacePriceFilter.ESTIMATED);
        var advice=new CandidateRecoveryService().analyze(List.of(place),new RoutePlan(35.1578,129.06,2,2000,"",List.of()),wrong,2000);
        assertEquals(List.of(new CandidateRecoveryService.Suggestion(CandidateRecoveryService.Change.CLEAR_FACETS,1)),advice.suggestions());
    }
    @Test void publicDatasetProvidesRealDetailedChoicesAndStableFriendlyCultureLabels()throws Exception{
        var places=new PublicDemoPlaceRepository().findAll();
        assertTrue(places.stream().filter(p->p.category().equals("MEAL")).map(PlaceFacets::detailType).distinct().count()>=15);
        assertTrue(places.stream().anyMatch(p->PlaceFacets.detailType(p).equals("도시공원")));
        assertTrue(places.stream().anyMatch(p->PlaceFacets.detailType(p).equals("박물관·미술관")));
    }
}
