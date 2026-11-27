package com.datecourse.service;

import com.datecourse.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import java.io.*;
import java.nio.file.*;
import static org.junit.jupiter.api.Assertions.*;

class PublicPlaceFactsTest {
    private final byte[] source;
    private final ObjectMapper mapper = new ObjectMapper();
    private final JsonNode current;
    PublicPlaceFactsTest() throws IOException {
        current=mapper.readTree(Path.of("data/places/expansion/current.json").toFile());
        source=Files.readAllBytes(Path.of(current.path("dataset").asText()));
    }
    private ObjectNode facts() throws IOException {
        return (ObjectNode) mapper.readTree(Path.of(current.path("facts").asText()).toFile());
    }
    private InputStream stream(ObjectNode facts) throws IOException { return new ByteArrayInputStream(mapper.writeValueAsBytes(facts)); }
    @Test void currentFingerprintUsesLfSnapshotAndStillRejectsChangedContent() throws Exception {
        assertFalse(new String(source, java.nio.charset.StandardCharsets.UTF_8).contains("\r\n"));
        assertNull(getClass().getResourceAsStream("/datasets/seomyeon-public-estimates.json"));
        assertEquals(current.path("places").asInt(),PublicPlaceFacts.read(source,stream(facts())).size());
        var changed=mapper.readTree(source);((ObjectNode)changed.path("places").get(0)).put("name","changed");
        assertThrows(IOException.class,()->PublicPlaceFacts.read(mapper.writeValueAsBytes(changed),stream(facts())));
    }
    @Test void currentPublicDataLoadsTypedFactsWithoutApprovingLegacyIds() throws Exception {
        var places = com.datecourse.demo.PublicPlaceImport.batch().places();
        assertEquals(current.path("places").asInt(), places.size());
        assertTrue(places.stream().filter(p -> p.details().hasTag(PlaceTag.MODEL_RESTAURANT)).count() >= 3);
        assertTrue(places.stream().filter(p -> p.details().hasTag(PlaceTag.NEAR_SUBWAY)).count() >= 368);
        var nearest = places.getFirst().details().attributeFacts().stream().filter(f -> f.feature() == PlaceFeature.NEAR_SUBWAY).findFirst().orElseThrow();
        assertEquals("2022-11-30", nearest.evidence().referenceDate());
        assertEquals("2026-11-16", nearest.evidence().checkedAt());
        assertEquals(SourceEvidence.Origin.CALCULATED, nearest.evidence().origin());
        assertTrue(places.stream().flatMap(p -> p.details().externalLinks().stream()).noneMatch(ExternalPlaceLink::usable));
        assertEquals(FactState.UNKNOWN, places.getFirst().details().featureState(PlaceFeature.ALLOWS_DOGS));
    }
    @Test void changedDatasetAndDuplicateOrUnknownIdentitiesAreRejected() throws Exception {
        var version = facts(); version.put("schemaVersion", 4294967297L);
        assertThrows(IOException.class, () -> PublicPlaceFacts.read(source, stream(version)));
        var facts = facts(); facts.put("datasetSha256", "bad");
        assertThrows(IOException.class, () -> PublicPlaceFacts.read(source, stream(facts)));
        var duplicate = facts(); ((com.fasterxml.jackson.databind.node.ArrayNode)duplicate.path("places")).add(duplicate.path("places").get(0));
        assertThrows(IOException.class, () -> PublicPlaceFacts.read(source, stream(duplicate)));
        var unknown = facts(); ((ObjectNode) unknown.path("places").get(0)).put("publicPlaceId", "another");
        assertThrows(IOException.class, () -> PublicPlaceFacts.read(source, stream(unknown)));
    }
    @Test void wrongFeatureAndInvalidDatesDoNotSilentlyDefault() throws Exception {
        var facts = facts(); ((ObjectNode) facts.path("places").get(0).path("tags").get(0)).put("tag", "UNKNOWN_NEW_TAG");
        assertThrows(IOException.class, () -> PublicPlaceFacts.read(source, stream(facts)));
        var dates = facts(); ((ObjectNode) dates.path("places").get(0).path("tags").get(0)).put("checkedAt", "bad-date");
        assertThrows(IOException.class, () -> PublicPlaceFacts.read(source, stream(dates)));
    }
    @Test void oldDatasetRetainsItsCostAndCoordinates() throws Exception {
        var old = PublicPlaceFacts.readPlaces(new ByteArrayInputStream(source));
        var updated = com.datecourse.demo.PublicPlaceImport.batch().places();
        for (int i=0; i<old.size(); i++) {
            assertEquals(old.get(i).priceEstimate(), updated.get(i).priceEstimate());
            assertEquals(old.get(i).lat(), updated.get(i).lat());
            assertEquals(old.get(i).lng(), updated.get(i).lng());
        }
    }
}
