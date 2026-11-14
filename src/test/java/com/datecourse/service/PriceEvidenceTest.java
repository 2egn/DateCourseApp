package com.datecourse.service;
import com.datecourse.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PriceEvidenceTest {
    @Test void oldJsonDefaultsWithoutInventingGoogleVerification() throws Exception {
        var p=new ObjectMapper().readValue("{\"minimum\":5000,\"maximum\":5000,\"unit\":\"PER_PERSON\",\"basis\":\"기존\"}",PriceEstimate.class);
        assertEquals(PriceEstimate.Evidence.LEGACY_REFERENCE,p.evidence());assertFalse(p.estimated());
        var details=new ObjectMapper().readValue("{}",PlaceDetails.class);assertTrue(details.tags().isEmpty());
    }
    @Test void numericEstimateCalculatesButCannotBecomeConfirmedTotal() throws Exception {
        var e=new PriceEstimate(12000,12000,PriceEstimate.Unit.PER_PERSON,"139곳 기준",PriceEstimate.Evidence.SIMILAR_ESTIMATE,"https://example.test","2026-11-14");
        var p=new Place(1,"x","장소","MEAL",35,129,e,0,PlaceDetails.EMPTY);
        var cost=CostEstimate.of(List.of(p),2);assertEquals(24000,cost.minimum());assertEquals(1,cost.estimatedPlaces());
        assertTrue(cost.display().contains("추정 1곳 포함"));assertTrue(p.priceText().startsWith("약 "));
        assertThrows(IllegalStateException.class,cost::exactTotal);
        var json=new ObjectMapper();assertEquals(e,json.readValue(json.writeValueAsString(e),PriceEstimate.class));
    }
}
