package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.ExternalPlaceLink;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/**
 * - 공공 장소 ID와 확인된 Google 장소 ID 연결표 조회용 클래스
 * - 가게 이름 검색으로 연결 관계를 새로 추정하지 않음
 */
public final class PlaceBindingCatalog {
    public static final Path PILOT_DIRECTORY = Path.of("data", "places", "pilot", "seomyeon-jeonpo-2026-11-13");
    private static final Map<Integer, String> MATCH_RULES = Map.of(
            1, "exact_normalized_name_road_building_district_within_200m_operational_activity",
            2, "name_address_identity_with_separate_visit_eligibility_v2",
            3, "name_address_identity_with_typed_retrieval_and_safe_suffixes_v3",
            4, "contact_or_scoped_website_identity_v4",
            5, "official_source_corrected_contact_identity_v5");
    private final Map<String, String> googleIds;
    private final String matchMethod;
    private final String checkedAt;

    private PlaceBindingCatalog(Map<String, String> googleIds, String matchMethod, String checkedAt) {
        this.googleIds = Map.copyOf(googleIds);
        this.matchMethod = matchMethod;
        this.checkedAt = checkedAt;
    }

    /** 연결표가 현재 장소 자료와 다른 버전이면 전체 사용을 거부하는 검증 */
    public static PlaceBindingCatalog load(Path catalogFile, Path datasetFile) throws IOException {
        JsonNode catalog = new ObjectMapper().readTree(catalogFile.toFile());
        if (catalog == null || !catalog.isObject() || !catalog.path("schema_version").isIntegralNumber()
                || !catalog.path("schema_version").canConvertToInt()
                || !catalog.path("match_rule").asText().equals(MATCH_RULES.get(catalog.path("schema_version").asInt()))
                || !catalog.path("bindings").isArray()) {
            throw new IOException("장소 연결표 형식을 확인해 주세요.");
        }
        try { Instant.parse(requiredText(catalog, "checked_at")); }
        catch (DateTimeParseException failure) { throw new IOException("장소 연결표 확인일을 확인해 주세요."); }
        if (!sha256(datasetFile).equals(requiredText(catalog, "dataset_sha256"))) {
            throw new IOException("장소 자료가 변경되었습니다. 연결표를 다시 검증해 주세요.");
        }
        if (catalog.path("schema_version").asInt() == 5
                && !sha256(catalogFile.resolveSibling("contact-source-overrides.json"))
                    .equals(requiredText(catalog, "verification_overrides_sha256"))) {
            throw new IOException("공식 출처 보정 자료가 변경되었습니다. 장소 연결을 다시 검증해 주세요.");
        }
        Map<String, String> ids = new HashMap<>();
        var providerIds = new HashSet<String>();
        for (JsonNode binding : catalog.path("bindings")) {
            if (catalog.path("schema_version").asInt() >= 2
                    && !"ELIGIBLE_AT_CHECK".equals(binding.path("eligibility").asText())) {
                throw new IOException("방문 가능 여부가 확인되지 않은 장소 연결입니다.");
            }
            String localId = requiredText(binding, "local_place_id");
            String googleId = requiredText(binding, "google_place_id");
            if (!googleId.matches("[A-Za-z0-9_-]+") || ids.putIfAbsent(localId, googleId) != null
                    || !providerIds.add(googleId)) {
                throw new IOException("중복되거나 잘못된 장소 연결 ID가 있습니다.");
            }
        }
        return new PlaceBindingCatalog(ids, requiredText(catalog, "match_rule"), requiredText(catalog, "checked_at"));
    }

    public Optional<String> googlePlaceId(String localPlaceId) {
        return Optional.ofNullable(localPlaceId == null ? null : googleIds.get(localPlaceId));
    }

    /** 장소 연결의 식별값 조회용 함수. DB 번호나 장소명으로 대체 식별값을 만들지 않음 */
    public Optional<String> googlePlaceId(Place place) {
        return place == null ? Optional.empty() : googlePlaceId(place.placeId());
    }

    /** 자료, 규칙 및 사용 가능 여부 검사를 통과한 연결표만 확인된 연결로 변환 */
    public Optional<ExternalPlaceLink> reviewedGoogleLink(String localPlaceId) {
        return googlePlaceId(localPlaceId).map(id -> new ExternalPlaceLink(ExternalPlaceLink.Provider.GOOGLE_PLACES,
                id, ExternalPlaceLink.Confidence.UNSPECIFIED, ExternalPlaceLink.ReviewStatus.CONFIRMED,
                ExternalPlaceLink.EntityKind.VENUE, matchMethod, checkedAt, "자료 지문·장소 동일성·방문 가능 여부 검증 기록"));
    }

    public int size() { return googleIds.size(); }

    private static String requiredText(JsonNode node, String field) throws IOException {
        JsonNode value = node.get(field);
        if (value == null || !value.isTextual() || value.asText().isBlank()) {
            throw new IOException("장소 연결표에 필요한 값이 없습니다: " + field);
        }
        return value.asText();
    }

    private static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file)) {
                byte[] buffer = new byte[8192];
                int length;
                while ((length = input.read(buffer)) != -1) digest.update(buffer, 0, length);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by Java", impossible);
        }
    }
}
