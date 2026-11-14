package com.datecourse.service;
import com.datecourse.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.stream.*;
import static org.junit.jupiter.api.Assertions.*;

class NextPlaceConditionsTest {
    private final NextPlaceService search=new NextPlaceService();
    private List<Place> pool() {
        return IntStream.rangeClosed(1,60).mapToObj(i->new Place(i,"p"+i,"장소 "+i,"MEAL",35.1578+i*.00002,129.06,
            new PriceEstimate(12000,12000,PriceEstimate.Unit.PER_PERSON,"추정",PriceEstimate.Evidence.SIMILAR_ESTIMATE,"",""),0,
            new PlaceDetails("",i<=40?"백반/한정식":"일식 회/초밥","","","source","","","",Map.of(),
                i%10==0?List.of(new PlaceDetails.TagFact(PlaceTag.GOOD_PRICE,"지정","source","")):List.of()))).toList();
    }
    private NextPlaceCondition condition(Set<String> kinds,Set<PlaceTag> tags,int n) {return new NextPlaceCondition("식사",20000,2000,false,kinds,tags,n);}
    @Test void limitsApplyAfterSubtypeAndTagIntersection() {
        var c=condition(Set.of("일식"),Set.of(PlaceTag.GOOD_PRICE),30);
        assertEquals(List.of(50L,60L),search.candidates(pool(),List.of(),35.1578,129.06,2,2000,c).stream().map(p->p.place().recordId()).toList());
        assertEquals(20,search.matching(pool(),List.of(),35.1578,129.06,2,2000,condition(Set.of("일식"),Set.of(),5)).size());
    }
    @Test void acceptsOnlySupportedDisplayLimitsAndPreservesNearestOrder() {
        for(int n:List.of(5,10,20,30)) {
            var result=search.candidates(pool(),List.of(),35.1578,129.06,2,2000,condition(Set.of(),Set.of(),n));
            assertEquals(n,result.size());assertEquals(1,result.getFirst().place().recordId());
        }
        assertThrows(IllegalArgumentException.class,()->condition(Set.of(),Set.of(),31));
    }
    @Test void multiSubtypeIsOrWhileTagsAndPriceAreRequired() {
        assertEquals(6,search.matching(pool(),List.of(),35.1578,129.06,2,2000,condition(Set.of("한식","일식"),Set.of(PlaceTag.GOOD_PRICE),30)).size());
        assertTrue(search.candidates(pool(),List.of(),35.1578,129.06,2,2000,new NextPlaceCondition("식사",10000,2000,false,Set.of(),Set.of(),30)).isEmpty());
    }
    @Test void packagedPublicDatasetKeepsFactsAndEstimatesSeparate() throws Exception {
        var rows=new PublicDemoPlaceRepository().findAll();
        assertTrue(rows.stream().filter(p->p.category().equals("MEAL")).count()>30);
        assertTrue(rows.stream().filter(p->p.category().equals("CAFE")).count()>30);
        assertTrue(rows.stream().anyMatch(p->p.details().hasTag(PlaceTag.GOOD_PRICE)));
        assertTrue(rows.stream().anyMatch(p->p.priceEstimate().estimated()));
        assertTrue(rows.stream().allMatch(p->!p.details().sourceUrl().isBlank()&&p.details().externalIds().containsKey("public")));
        assertTrue(rows.stream().noneMatch(p->p.priceEstimate().evidence()==PriceEstimate.Evidence.GOOGLE_PROVIDED));
        var unique=new HashSet<String>();assertTrue(rows.stream().allMatch(p->unique.add(p.placeId())));
    }
}
