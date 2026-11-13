package com.datecourse.service;

import com.datecourse.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PilotPriceFlowTest {
    private Place place(long id, PriceEstimate price) {
        return new Place(id, "test:" + id, "설명용 장소 " + id, "CAFE", 35.1578, 129.06,
                price, 0, PlaceDetails.EMPTY);
    }

    @Test void rangeUnknownAndConfirmedFreeAreNotConfused() {
        var range = new PriceEstimate(10000, 15000, PriceEstimate.Unit.PER_PERSON, "설명용");
        var drink = new PriceEstimate(4000, 6000, PriceEstimate.Unit.PER_DRINK, "설명용 음료");
        var cost = CostEstimate.of(List.of(place(1, range), place(2, drink),
                place(3, PriceEstimate.exact(0)), place(4, PriceEstimate.unknown())), 2);
        assertEquals(28000, cost.minimum());
        assertEquals(42000L, cost.maximum());
        assertEquals(1, cost.unknownPlaces());
        assertEquals("확인된 비용 28,000원~42,000원 + 가격 미확인 1곳", cost.display());
        assertThrows(IllegalStateException.class, cost::exactTotal);
        assertEquals("가격 미확인 1곳", CostEstimate.of(List.of(place(1, PriceEstimate.unknown())), 2).display());
        assertEquals("예상 총비용 0원", CostEstimate.of(List.of(place(1, PriceEstimate.exact(0))), 2).display());
    }

    @Test void openUpperBoundAndUnknownUnitDoNotBecomeFiniteWholeCost() {
        var open = new PriceEstimate(10000, null, PriceEstimate.Unit.PER_PERSON, "상한 미확인");
        var group = new PriceEstimate(30000, 40000, PriceEstimate.Unit.UNKNOWN, "분량 미확인 세트");
        var cost = CostEstimate.of(List.of(place(1, open), place(2, group)), 3);
        assertEquals(30000, cost.minimum());
        assertNull(cost.maximum());
        assertEquals(1, cost.unknownPlaces());
        assertTrue(cost.display().contains("이상"));
        assertFalse(open.withinCap(20000));
    }

    @Test void invalidRangeAndUnknownExactAccessAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> PriceEstimate.exact(-1));
        assertThrows(IllegalArgumentException.class, () -> new PriceEstimate(10000, 9000, PriceEstimate.Unit.PER_PERSON, ""));
        assertThrows(IllegalArgumentException.class, () -> new PriceEstimate(null, 9000, PriceEstimate.Unit.UNKNOWN, ""));
        assertThrows(IllegalStateException.class, () -> place(1, PriceEstimate.unknown()).price());
        assertThrows(IllegalArgumentException.class, () -> CostEstimate.of(List.of(), 0));
    }

    @Test void uncertaintyRequiresOptInAndRangesUseTheirUpperBound() {
        Place known = place(1, new PriceEstimate(8000, 10000, PriceEstimate.Unit.PER_PERSON, ""));
        Place exceeds = place(2, new PriceEstimate(8000, 15000, PriceEstimate.Unit.PER_PERSON, ""));
        Place unknown = place(3, PriceEstimate.unknown());
        var service = new NextPlaceService();
        var places = List.of(unknown, exceeds, known);
        assertEquals(List.of(known), service.candidates(places, List.of(), 35.1578, 129.06, 500,
                "카페", 10000, 2, 2000).stream().map(PlaceSearchService.NearbyPlace::place).toList());
        assertEquals(List.of(known, unknown), service.candidates(places, List.of(), 35.1578, 129.06, 500,
                "카페", 10000, 2, 2000, true).stream().map(PlaceSearchService.NearbyPlace::place).toList());
        var legacy = new PlaceSearchService().recommend(List.of(unknown),
                new SearchCondition(10000, 2, List.of("카페"), 35.1578, 129.06, 500));
        assertTrue(legacy.course().isEmpty());
    }

    @Test void packagedPilotUsesOwnStableIdsUnknownPricesAndSourceMetadata() throws Exception {
        var repository = new PilotPlaceRepository();
        var places = repository.findAll();
        assertEquals(9, places.size());
        assertEquals(places, repository.findAll());
        assertEquals(9, places.stream().map(Place::placeId).distinct().count());
        assertEquals(9, places.stream().map(Place::recordId).distinct().count());
        assertTrue(places.stream().allMatch(p -> p.placeId().startsWith("DCA-DEMO-")
                && !p.priceEstimate().usablePerPerson() && !p.details().sourceUrl().isBlank()
                && p.details().externalIds().containsKey("public")));
        assertFalse(places.stream().anyMatch(p -> p.name().contains("부전도서관")));
        assertThrows(UnsupportedOperationException.class, places::clear);
    }

    @Test void malformedSnapshotCannotInventCoordinatesOrPricesOrDuplicateIds() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        var good = mapper.readTree(getClass().getResourceAsStream(PilotPlaceRepository.RESOURCE));
        for (String field : List.of("lat", "category", "priceEstimate", "details")) {
            var bad = good.deepCopy();
            ((ObjectNode) bad.path("places").get(0)).remove(field);
            assertThrows(java.io.IOException.class, () -> PilotPlaceRepository.read(new ByteArrayInputStream(mapper.writeValueAsBytes(bad))));
        }
        var duplicate = good.deepCopy();
        ((ObjectNode) duplicate.path("places").get(1)).put("placeId", duplicate.path("places").get(0).path("placeId").asText());
        assertThrows(java.io.IOException.class, () -> PilotPlaceRepository.read(new ByteArrayInputStream(mapper.writeValueAsBytes(duplicate))));
    }

    @Test void mapJsonContainsUnknownLabelInsteadOfFabricatedNumericZero() throws Exception {
        var mapper = new ObjectMapper();
        var unknown = place(1, PriceEstimate.unknown());
        var json = mapper.readTree(mapper.writeValueAsBytes(unknown));
        assertEquals("가격 미확인", json.path("priceText").asText());
        assertTrue(json.path("priceEstimate").path("minimum").isNull());
        assertFalse(json.has("price"));
        assertEquals(unknown, mapper.treeToValue(json, Place.class));
    }
}
