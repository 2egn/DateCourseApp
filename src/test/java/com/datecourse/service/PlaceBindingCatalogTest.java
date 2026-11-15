package com.datecourse.service;

import com.datecourse.model.Place;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PlaceBindingCatalogTest {
    @TempDir Path temporary;
    private Path dataset;
    private Path file;

    @BeforeEach
    void setup() throws Exception {
        dataset = temporary.resolve("places.csv");
        Files.writeString(dataset, "place_id,name\nSBIZ:one,가상카페\n");
        file = temporary.resolve("google-bindings.json");
        write(List.of(binding("SBIZ:one", "test_Google-one")));
    }

    @Test
    void resolvesExactSourceIdWithoutNetworkOrNameFallback() throws Exception {
        var catalog = PlaceBindingCatalog.load(file, dataset);
        assertEquals("test_Google-one", catalog.googlePlaceId("SBIZ:one").orElseThrow());
        Place wrongSource = new Place(1, "CAFE_0464", "가상카페", "CAFE", 35.15, 129.06, 0, 0);
        assertTrue(catalog.googlePlaceId(wrongSource).isEmpty());
        assertTrue(catalog.googlePlaceId("SBIZ:pending").isEmpty());
        assertTrue(catalog.googlePlaceId((String) null).isEmpty());
        assertTrue(catalog.googlePlaceId((Place) null).isEmpty());
    }

    @Test
    void changedDatasetCannotReusePreviouslyVerifiedBindings() throws Exception {
        Files.writeString(dataset, "place_id,name\nSBIZ:one,다른가게\n");
        assertThrows(IOException.class, () -> PlaceBindingCatalog.load(file, dataset));
    }

    @Test
    void duplicateLocalOrGoogleIdsRejectEntireCatalog() throws Exception {
        write(List.of(binding("SBIZ:one", "googleA"), binding("SBIZ:one", "googleB")));
        assertThrows(IOException.class, () -> PlaceBindingCatalog.load(file, dataset));
        write(List.of(binding("SBIZ:one", "googleA"), binding("SBIZ:two", "googleA")));
        assertThrows(IOException.class, () -> PlaceBindingCatalog.load(file, dataset));
    }

    @Test
    void missingOrMalformedFieldsRejectEntireCatalog() throws Exception {
        for (Map<String, String> bad : List.of(Map.of("local_place_id", "SBIZ:one"),
                binding("SBIZ:one", "https://invalid.test/place"), binding("", "googleA"))) {
            write(List.of(bad));
            assertThrows(IOException.class, () -> PlaceBindingCatalog.load(file, dataset));
        }
        Files.writeString(file, "null");
        assertThrows(IOException.class, () -> PlaceBindingCatalog.load(file, dataset));
    }

    @Test
    void unknownSchemaOrMissingCatalogCannotBecomeAnEmptySuccess() throws Exception {
        Files.writeString(file, "{\"schema_version\":2,\"bindings\":[]}");
        assertThrows(IOException.class, () -> PlaceBindingCatalog.load(file, dataset));
        Files.delete(file);
        assertThrows(IOException.class, () -> PlaceBindingCatalog.load(file, dataset));
    }

    @Test
    void realPilotCatalogMatchesDatasetAndExcludesReviewCandidates() throws Exception {
        var directory = PlaceBindingCatalog.PILOT_DIRECTORY;
        var catalog = PlaceBindingCatalog.load(directory.resolve("google-bindings.json"), directory.resolve("places.csv"));
        assertEquals(2, catalog.size());
        assertTrue(catalog.googlePlaceId("SBIZ:MA0101202607A0151351").isPresent());
        assertTrue(catalog.googlePlaceId("SBIZ:MA0101202406A0012647").isPresent());
        assertTrue(catalog.googlePlaceId("SBIZ:MA010120220813751543").isEmpty());
        assertTrue(catalog.googlePlaceId("CAFE_0464").isEmpty());
    }

    @Test
    void expandedAndRefinedCatalogsPreserveClosedAndUncertainExclusions() throws Exception {
        var directory = PlaceBindingCatalog.PILOT_DIRECTORY;
        var expanded = PlaceBindingCatalog.load(directory.resolve("google-bindings-v2.json"), directory.resolve("places.csv"));
        var refined = PlaceBindingCatalog.load(directory.resolve("google-bindings-v3.json"), directory.resolve("places.csv"));
        assertEquals(6, expanded.size());
        assertEquals(8, refined.size());
        assertTrue(refined.googlePlaceId("SBIZ:MA010120220813751543").isPresent());
        assertTrue(refined.googlePlaceId("SBIZ:MA010120220807078377").isPresent());
        assertTrue(refined.googlePlaceId("STD:15017323:e33f9aadac190db685befd1a").isEmpty());
        assertTrue(refined.googlePlaceId("SBIZ:MA0106202201A2364418").isEmpty());
        assertTrue(refined.googlePlaceId("CAFE_0464").isEmpty());
    }

    @Test
    void newCatalogRequiresExplicitEligibilityAndCorrectRuleForItsVersion() throws Exception {
        var mapper = new ObjectMapper();
        var directory = PlaceBindingCatalog.PILOT_DIRECTORY;
        var catalog = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(directory.resolve("google-bindings-v3.json").toFile());
        // Use the fixture dataset hash so every rejection checks the rule/eligibility itself.
        catalog.put("dataset_sha256", HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(dataset))));
        var first = (com.fasterxml.jackson.databind.node.ObjectNode) catalog.path("bindings").get(0);
        first.remove("eligibility");
        mapper.writeValue(file.toFile(), catalog);
        assertThrows(IOException.class, () -> PlaceBindingCatalog.load(file, dataset));
        first.put("eligibility", "EXCLUDED");
        mapper.writeValue(file.toFile(), catalog);
        assertThrows(IOException.class, () -> PlaceBindingCatalog.load(file, dataset));
        first.put("eligibility", "ELIGIBLE_AT_CHECK");
        catalog.put("schema_version", 1);
        mapper.writeValue(file.toFile(), catalog);
        assertThrows(IOException.class, () -> PlaceBindingCatalog.load(file, dataset));
    }

    @Test
    void officialContactCatalogDependsOnVerifiedCorrectionsAndExcludesClosedLibrary() throws Exception {
        var directory = PlaceBindingCatalog.PILOT_DIRECTORY;
        var catalog = PlaceBindingCatalog.load(directory.resolve("google-bindings-v5.json"), directory.resolve("places.csv"));
        assertEquals(9, catalog.size());
        assertTrue(catalog.googlePlaceId("LIB:69").isPresent());
        assertTrue(catalog.googlePlaceId("LIB:73").isPresent());
        assertTrue(catalog.googlePlaceId("LIB:21").isEmpty());
        Path localCatalog = temporary.resolve("google-bindings-v5.json");
        Path corrections = temporary.resolve("contact-source-overrides.json");
        Files.copy(directory.resolve("google-bindings-v5.json"), localCatalog);
        assertThrows(IOException.class, () -> PlaceBindingCatalog.load(localCatalog, directory.resolve("places.csv")));
        Files.copy(directory.resolve("contact-source-overrides.json"), corrections);
        assertEquals(9, PlaceBindingCatalog.load(localCatalog, directory.resolve("places.csv")).size());
        Files.writeString(corrections, "\n", java.nio.file.StandardOpenOption.APPEND);
        assertThrows(IOException.class, () -> PlaceBindingCatalog.load(localCatalog, directory.resolve("places.csv")));
    }

    private static Map<String, String> binding(String local, String google) {
        return Map.of("local_place_id", local, "google_place_id", google);
    }

    private void write(List<Map<String, String>> bindings) throws Exception {
        String digest = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(dataset)));
        new ObjectMapper().writeValue(file.toFile(), Map.of("schema_version", 1,
                "dataset_sha256", digest, "checked_at", "2026-11-13T12:30:00Z",
                "match_rule", "exact_normalized_name_road_building_district_within_200m_operational_activity",
                "bindings", bindings));
    }
}
