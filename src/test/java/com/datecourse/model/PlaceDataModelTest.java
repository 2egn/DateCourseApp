package com.datecourse.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PlaceDataModelTest {
    private final SourceEvidence evidence = new SourceEvidence(SourceEvidence.Origin.ADMIN_REVIEW, "운영기관 확인", "https://example.test", "2026-11-14", "2026-11-15");
    private ExternalPlaceLink confirmed() {
        return new ExternalPlaceLink(ExternalPlaceLink.Provider.GOOGLE_PLACES, "id", ExternalPlaceLink.Confidence.MEDIUM,
                ExternalPlaceLink.ReviewStatus.CONFIRMED, ExternalPlaceLink.EntityKind.VENUE,
                "name_address_phone", "2026-11-14T12:00:00Z", "연락처 대조");
    }
    private PlaceDetails details(List<PlaceAttributeFact> facts) {
        return new PlaceDetails("", "", "", "", "", "", "", "", Map.of(), List.of(), facts, List.of());
    }
    @Test void missingNegativeAndPositiveAreDistinctAndNeedEvidence() {
        assertEquals(FactState.UNKNOWN, PlaceDetails.EMPTY.featureState(PlaceFeature.PARKING));
        assertEquals(FactState.UNAVAILABLE, details(List.of(new PlaceAttributeFact(PlaceFeature.PARKING, FactState.UNAVAILABLE, evidence))).featureState(PlaceFeature.PARKING));
        assertThrows(IllegalArgumentException.class, () -> new PlaceAttributeFact(PlaceFeature.PARKING, FactState.AVAILABLE, null));
        assertThrows(IllegalArgumentException.class, () -> new SourceEvidence(SourceEvidence.Origin.PUBLIC_DATA, "근거", "", "2026-02-30", ""));
    }
    @Test void highResolutionIsNotApprovalAndAddressCannotBeConfirmed() {
        var pending = new ExternalPlaceLink(ExternalPlaceLink.Provider.GOOGLE_PLACES, "id", ExternalPlaceLink.Confidence.HIGH,
                ExternalPlaceLink.ReviewStatus.NEEDS_REVIEW, ExternalPlaceLink.EntityKind.ADDRESS, "resolveNames", "", "");
        assertFalse(pending.usable());
        assertThrows(IllegalArgumentException.class, () -> new ExternalPlaceLink(pending.provider(), pending.externalId(), pending.confidence(),
                ExternalPlaceLink.ReviewStatus.CONFIRMED, pending.entityKind(), "resolveNames", "2026-11-14T12:00:00Z", "검토"));
        assertTrue(confirmed().usable());
        assertFalse(confirmed().identityChanged().usable());
    }
    @Test void oldJsonDoesNotUpgradeExternalIdsToReviewedLinks() throws Exception {
        var mapper = new ObjectMapper();
        var old = mapper.readValue("{\"externalIds\":{\"google\":\"old_id\"}}", PlaceDetails.class);
        assertFalse(old.externalLinks().getFirst().usable());
        assertEquals(ExternalPlaceLink.ReviewStatus.NEEDS_REVIEW, old.externalLinks().getFirst().reviewStatus());
        assertEquals(old, mapper.readValue(mapper.writeValueAsString(old), PlaceDetails.class));
        assertTrue(mapper.readValue("{}", PlaceDetails.class).externalLinks().isEmpty());
    }
    @Test void persistentFactsRejectProviderDetailsAndConflicts() {
        var google = new SourceEvidence(SourceEvidence.Origin.GOOGLE_PLACES, "Google", "", "", "2026-11-15");
        assertThrows(IllegalArgumentException.class, () -> details(List.of(new PlaceAttributeFact(PlaceFeature.PARKING, FactState.AVAILABLE, google))));
        var negative = new PlaceAttributeFact(PlaceFeature.GOOD_PRICE, FactState.UNAVAILABLE, evidence);
        assertThrows(IllegalArgumentException.class, () -> new PlaceDetails("", "", "", "", "", "", "", "", Map.of(),
                List.of(new PlaceDetails.TagFact(PlaceTag.GOOD_PRICE, "지정", "", "")), List.of(negative), List.of()));
        assertThrows(IllegalArgumentException.class, () -> details(List.of(negative, negative)));
    }
    @Test void transientPriceAndHoursCannotLeakToSavedJsonOrBecomePerPersonCost() throws Exception {
        var fetched = Instant.parse("2026-11-15T00:00:00Z");
        var range = new ProviderPriceRange(new BigDecimal("10000"), new BigDecimal("20000"), "KRW");
        var snapshot = new ExternalPlaceSnapshot(confirmed(), fetched, range, null, null, Map.of(),
                List.of(new ExternalPlaceSnapshot.OpeningInterval(fetched.plusSeconds(3600), fetched.plusSeconds(7200))));
        assertEquals(FactState.UNKNOWN, snapshot.openAt(fetched));
        assertEquals(FactState.AVAILABLE, snapshot.openAt(fetched.plusSeconds(3600)));
        assertEquals(FactState.UNKNOWN, snapshot.openAt(fetched.plusSeconds(7200)));
        assertThrows(java.io.IOException.class, () -> new ObjectMapper().writeValueAsString(snapshot));
        assertThrows(java.io.IOException.class, () -> new ObjectMapper().writeValueAsString(Map.of("details", snapshot)));
        assertFalse(snapshot.toString().contains("10000"));
        assertEquals(1, CostEstimate.of(List.of(new Place(1,"own","식당","MEAL",35,129,PriceEstimate.unknown(),0,PlaceDetails.EMPTY)),2).unknownPlaces());
    }
    @Test void reviewedBindingAndFactsSurviveJsonRoundTrip() throws Exception {
        var value = new PlaceDetails("주소", "한식", "", "", "", "", "", "", Map.of("google", "id"), List.of(),
                List.of(new PlaceAttributeFact(PlaceFeature.RESERVABLE, FactState.AVAILABLE, evidence)), List.of(confirmed()));
        var mapper = new ObjectMapper();
        assertEquals(value, mapper.readValue(mapper.writeValueAsString(value), PlaceDetails.class));
    }
}
