package com.datecourse.service;

import com.datecourse.model.Place;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/** Reads only the explicitly packaged demonstration snapshot. Never writes/imports into DB. */
public final class PilotPlaceRepository implements PlaceSource {
    public static final String RESOURCE = "/datasets/seomyeon-jeonpo-demo.json";
    public static final double START_LAT = 35.1578, START_LNG = 129.0600;

    @Override public List<Place> findAll() throws IOException {
        try (InputStream input = getClass().getResourceAsStream(RESOURCE)) {
            if (input == null) throw new IOException("시연용 장소 목록을 찾지 못했어요.");
            return read(input);
        }
    }

    public static List<Place> read(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode document = mapper.readTree(input);
        if (document == null || !document.path("schemaVersion").isIntegralNumber()
                || !document.path("schemaVersion").canConvertToInt()
                || document.path("schemaVersion").asInt() != 1 || !document.path("places").isArray())
            throw new IOException("시연용 장소 목록 형식이 올바르지 않아요.");
        List<Place> places = new ArrayList<>();
        var ids = new HashSet<String>();
        var records = new HashSet<Long>();
        for (JsonNode row : document.path("places")) {
            // Numeric defaults must never silently become a free price or (0,0) location.
            for (String field : List.of("recordId", "lat", "lng", "rating"))
                if (!row.path(field).isNumber()) throw new IOException("장소의 숫자 항목이 누락됐어요: " + field);
            if (!row.path("priceEstimate").isObject() || !row.path("details").isObject())
                throw new IOException("장소 가격 상태와 출처가 필요해요.");
            if (!row.path("recordId").isIntegralNumber() || !row.path("recordId").canConvertToLong())
                throw new IOException("장소 번호는 정수여야 해요.");
            for (String amount : List.of("minimum", "maximum")) {
                JsonNode value = row.path("priceEstimate").path(amount);
                if (!value.isNull() && (!value.isIntegralNumber() || !value.canConvertToInt()))
                    throw new IOException("가격은 정수 또는 미확인(null)이어야 해요.");
            }
            Place place = mapper.treeToValue(row, Place.class);
            if (place.recordId() <= 0 || place.placeId() == null || !place.placeId().startsWith("DCA-DEMO-")
                    || !ids.add(place.placeId()) || !records.add(place.recordId())
                    || place.name() == null || place.name().isBlank()
                    || !("MEAL".equals(place.category()) || "CAFE".equals(place.category()) || "CULTURE".equals(place.category()))
                    || !Double.isFinite(place.lat()) || !Double.isFinite(place.lng())
                    || place.lat() < -90 || place.lat() > 90 || place.lng() < -180 || place.lng() > 180
                    || place.lat() == 0 && place.lng() == 0 || place.details().sourceUrl().isBlank()
                    || !Double.isFinite(place.rating()) || place.rating() < 0 || place.rating() > 5)
                throw new IOException("장소 ID·좌표·분류·출처를 확인해 주세요.");
            places.add(place);
        }
        if (places.isEmpty()) throw new IOException("시연용 장소 목록이 비어 있어요.");
        return List.copyOf(places);
    }
}
