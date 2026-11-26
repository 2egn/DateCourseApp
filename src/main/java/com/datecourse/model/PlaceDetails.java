package com.datecourse.model;

import java.util.Map;
import java.util.List;

/**
 * - 장소 주소, 세부 업종, 공공 속성 및 외부 ID 보관용 모델
 * - 외부 API에서 조회한 가격이나 리뷰 응답은 포함하지 않음
 */
public record PlaceDetails(String address, String subcategory, String phone, String website,
                           String sourceUrl, String sourceDate, String identityCheckedAt,
                           String verificationUrl, Map<String, String> externalIds, List<TagFact> tags,
                           List<PlaceAttributeFact> attributeFacts, List<ExternalPlaceLink> externalLinks,
                           Map<PlaceField, SourceEvidence> fieldEvidence) {
    public record TagFact(PlaceTag tag, String basis, String sourceUrl, String checkedAt) {
        public TagFact { if (tag == null) throw new IllegalArgumentException("태그가 필요합니다."); }
    }
    public PlaceDetails(String address, String subcategory, String phone, String website,
                        String sourceUrl, String sourceDate, String identityCheckedAt,
                        String verificationUrl, Map<String, String> externalIds) {
        this(address, subcategory, phone, website, sourceUrl, sourceDate, identityCheckedAt, verificationUrl, externalIds, List.of());
    }
    /** 기존 스냅샷 및 관리자 수정과의 호환용 생성자. 외부 ID만으로 검토 승인을 간주하지 않음 */
    public PlaceDetails(String address, String subcategory, String phone, String website,
                        String sourceUrl, String sourceDate, String identityCheckedAt,
                        String verificationUrl, Map<String, String> externalIds, List<TagFact> tags) {
        this(address, subcategory, phone, website, sourceUrl, sourceDate, identityCheckedAt,
                verificationUrl, externalIds, tags, List.of(), List.of());
    }
    public PlaceDetails(String address, String subcategory, String phone, String website,
                        String sourceUrl, String sourceDate, String identityCheckedAt,
                        String verificationUrl, Map<String, String> externalIds, List<TagFact> tags,
                        List<PlaceAttributeFact> attributeFacts, List<ExternalPlaceLink> externalLinks) {
        this(address, subcategory, phone, website, sourceUrl, sourceDate, identityCheckedAt,
                verificationUrl, externalIds, tags, attributeFacts, externalLinks, Map.of());
    }
    public static final PlaceDetails EMPTY = new PlaceDetails("", "", "", "", "", "", "", "", Map.of());
    public PlaceDetails {
        address = clean(address); subcategory = clean(subcategory); phone = clean(phone); website = clean(website);
        sourceUrl = clean(sourceUrl); sourceDate = clean(sourceDate); identityCheckedAt = clean(identityCheckedAt);
        verificationUrl = clean(verificationUrl);
        externalIds = externalIds == null ? Map.of() : Map.copyOf(externalIds);
        tags = tags == null ? List.of() : List.copyOf(tags);
        attributeFacts = attributeFacts == null ? List.of() : List.copyOf(attributeFacts);
        fieldEvidence = fieldEvidence == null ? Map.of() : Map.copyOf(fieldEvidence);
        if (fieldEvidence.values().stream().anyMatch(e -> e.origin() == SourceEvidence.Origin.GOOGLE_PLACES))
            throw new IllegalArgumentException("외부 상세 항목은 영구 장소 정보와 분리해 주세요.");
        var seen = new java.util.HashSet<PlaceFeature>();
        for (var fact : attributeFacts) {
            if (!seen.add(fact.feature())) throw new IllegalArgumentException("중복된 장소 속성입니다.");
            if (fact.evidence().origin() == SourceEvidence.Origin.GOOGLE_PLACES)
                throw new IllegalArgumentException("Google 상세 속성은 영구 장소 정보와 분리해 주세요.");
            if (fact.state() != FactState.AVAILABLE && tags.stream().anyMatch(t -> PlaceFeature.from(t.tag()) == fact.feature()))
                throw new IllegalArgumentException("기존 태그와 속성 상태가 충돌합니다.");
        }
        var links = new java.util.ArrayList<>(externalLinks == null ? List.<ExternalPlaceLink>of() : externalLinks);
        String googleId = externalIds.get("google");
        if (googleId != null && links.stream().noneMatch(l -> l.provider() == ExternalPlaceLink.Provider.GOOGLE_PLACES))
            links.add(ExternalPlaceLink.unreviewedGoogle(googleId));
        var providers = new java.util.HashSet<ExternalPlaceLink.Provider>();
        for (var link : links) {
            if (!providers.add(link.provider())) throw new IllegalArgumentException("한 제공자에 여러 연결을 확정할 수 없습니다.");
            if (link.provider() == ExternalPlaceLink.Provider.GOOGLE_PLACES && googleId != null && !googleId.equals(link.externalId()))
                throw new IllegalArgumentException("기존 외부 ID와 새 연결이 충돌합니다.");
        }
        externalLinks = List.copyOf(links);
    }
    public SourceEvidence evidenceFor(PlaceField field) { return fieldEvidence.getOrDefault(field, SourceEvidence.UNKNOWN); }
    public FactState featureState(PlaceFeature feature) {
        return attributeFacts.stream().filter(f -> f.feature() == feature).map(PlaceAttributeFact::state).findFirst()
                .orElseGet(() -> tags.stream().anyMatch(t -> PlaceFeature.from(t.tag()) == feature)
                        ? FactState.AVAILABLE : FactState.UNKNOWN);
    }
    /** 이전 태그와 근거 있는 속성의 확인 상태를 공통 기준으로 해석. 누락은 불가능과 구분 */
    public List<PlaceTag> availableTags() {
        return java.util.Arrays.stream(PlaceTag.values()).filter(this::hasTag).toList();
    }
    public boolean hasTag(PlaceTag tag) { return featureState(PlaceFeature.from(tag)) == FactState.AVAILABLE; }
    public PlaceDetails withPublicFacts(List<TagFact> publicTags, List<PlaceAttributeFact> facts) {
        return new PlaceDetails(address, subcategory, phone, website, sourceUrl, sourceDate,
                identityCheckedAt, verificationUrl, externalIds, publicTags, facts, externalLinks, fieldEvidence);
    }
    public String identityText() {
        if (!identityCheckedAt.isBlank()) return "장소 대조 기록 · " + identityCheckedAt.substring(0,Math.min(10,identityCheckedAt.length()));
        return sourceUrl.isBlank() ? "장소 확인 기록 없음" : "공공자료 등록 · 지도 대조 전";
    }
    private static String clean(String value) { return value == null ? "" : value; }
}
