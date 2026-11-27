package com.datecourse.service;

import com.datecourse.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

/**
 * - 공공 속성 파일의 근거를 해당 장소 스냅샷에 연결하는 클래스
 * - 계산에 사용한 원본과 지문이 일치할 때만 적용
 */
public final class PublicPlaceFacts {
    private PublicPlaceFacts() {}
    // 장소와 속성 파일의 ID·지문·기준 정보를 확인한 뒤 공공 속성 연결
    public static List<Place> read(byte[] dataset, InputStream factsInput) throws IOException {
        List<Place> places = readPlaces(new ByteArrayInputStream(dataset));
        JsonNode facts = new ObjectMapper().readTree(factsInput);
        if (facts == null || !facts.path("schemaVersion").isIntegralNumber() || !facts.path("schemaVersion").canConvertToInt()
                || facts.path("schemaVersion").asInt() != 1
                || !facts.path("places").isArray() || !sha256(dataset).equals(facts.path("datasetSha256").asText()))
            throw new IOException("공공 속성 자료의 버전과 원본 장소 목록을 확인해 주세요.");
        Map<String, JsonNode> indexed = new HashMap<>();
        for (JsonNode row : facts.path("places")) {
            String id = required(row, "placeId");
            if (indexed.putIfAbsent(id, row) != null) throw new IOException("공공 속성의 장소 ID가 중복됐어요.");
        }
        if (indexed.size() != places.size()) throw new IOException("공공 속성 자료의 장소 수가 일치하지 않아요.");
        List<Place> result = new ArrayList<>();
        for (Place place : places) {
            JsonNode row = indexed.remove(place.placeId());
            if (row == null || !required(row, "publicPlaceId").equals(place.details().externalIds().get("public"))
                    || !row.path("tags").isArray()) throw new IOException("공공 속성의 장소 연결을 확인해 주세요.");
            var tags = new ArrayList<PlaceDetails.TagFact>();
            var attributes = new ArrayList<PlaceAttributeFact>();
            var seen = new HashSet<PlaceTag>();
            for (JsonNode value : row.path("tags")) {
                try {
                    PlaceTag tag = PlaceTag.valueOf(required(value, "tag"));
                    if (!seen.add(tag)) throw new IllegalArgumentException("중복된 공공 태그입니다.");
                    String basis = required(value, "basis"), url = required(value, "sourceUrl");
                    String referenceDate = required(value, "checkedAt");
                    String calculatedAt = value.path("calculatedAt").asText("");
                    boolean calculated = tag == PlaceTag.NEAR_SUBWAY || tag == PlaceTag.NEAR_BUS;
                    if (calculated && calculatedAt.isBlank()) throw new IllegalArgumentException("교통 접근성의 계산일이 필요합니다.");
                    var evidence = new SourceEvidence(calculated ? SourceEvidence.Origin.CALCULATED : SourceEvidence.Origin.PUBLIC_DATA,
                            basis, url, referenceDate, calculatedAt);
                    tags.add(new PlaceDetails.TagFact(tag, basis, url, referenceDate));
                    attributes.add(new PlaceAttributeFact(PlaceFeature.from(tag), FactState.AVAILABLE, evidence));
                } catch (IllegalArgumentException ex) { throw new IOException("공공 태그의 상태·근거·날짜를 확인해 주세요.", ex); }
            }
            if (seen.contains(PlaceTag.FREE_ADMISSION) && place.priceEstimate().minimum() != null && place.priceEstimate().minimum() > 0)
                throw new IOException("무료입장 자료와 장소 가격이 충돌합니다.");
            var original = place.details();
            var fieldEvidence = new EnumMap<PlaceField, SourceEvidence>(PlaceField.class);
            fieldEvidence.putAll(original.fieldEvidence());
            var publicSource = new SourceEvidence(SourceEvidence.Origin.PUBLIC_DATA, "공공 장소 자료", original.sourceUrl(), original.sourceDate(), "");
            for (var field : List.of(PlaceField.NAME, PlaceField.ADDRESS, PlaceField.SUBCATEGORY, PlaceField.LOCATION))
                fieldEvidence.putIfAbsent(field, publicSource);
            var enriched = original.withPublicFacts(tags, attributes);
            enriched = new PlaceDetails(enriched.address(), enriched.subcategory(), enriched.phone(), enriched.website(),
                    enriched.sourceUrl(), enriched.sourceDate(), enriched.identityCheckedAt(), enriched.verificationUrl(),
                    enriched.externalIds(), enriched.tags(), enriched.attributeFacts(), enriched.externalLinks(), fieldEvidence);
            result.add(new Place(place.recordId(), place.placeId(), place.name(), place.category(), place.lat(), place.lng(),
                    place.priceEstimate(), place.rating(), enriched));
        }
        if (!indexed.isEmpty()) throw new IOException("목록에 없는 장소의 공공 속성이 포함됐어요.");
        return List.copyOf(result);
    }
    /** 장소 스냅샷의 필수값·식별자·좌표·가격 검증 */
    public static List<Place> readPlaces(InputStream input) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        JsonNode document = mapper.readTree(input);
        if (document == null || !document.path("schemaVersion").isIntegralNumber()
                || !document.path("schemaVersion").canConvertToInt()
                || document.path("schemaVersion").asInt() != 1 || !document.path("places").isArray())
            throw new IOException("장소 목록 형식이 올바르지 않아요.");
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
            if (place.recordId() <= 0 || place.placeId() == null || place.placeId().isBlank()
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
        if (places.isEmpty()) throw new IOException("장소 목록이 비어 있어요.");
        return List.copyOf(places);
    }
    private static String required(JsonNode row, String field) throws IOException {
        if (!row.path(field).isTextual() || row.path(field).asText().isBlank())
            throw new IOException("공공 속성 필수 항목이 필요해요: " + field);
        return row.path(field).asText();
    }
    private static String sha256(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
}
