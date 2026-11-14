package com.datecourse.model;

import java.util.Map;
import java.util.List;

/** Public-source facts and identifiers. External API price/review payloads do not belong here. */
public record PlaceDetails(String address, String subcategory, String phone, String website,
                           String sourceUrl, String sourceDate, String identityCheckedAt,
                           String verificationUrl, Map<String, String> externalIds, List<TagFact> tags) {
    public record TagFact(PlaceTag tag, String basis, String sourceUrl, String checkedAt) {
        public TagFact { if (tag == null) throw new IllegalArgumentException("태그가 필요합니다."); }
    }
    public PlaceDetails(String address, String subcategory, String phone, String website,
                        String sourceUrl, String sourceDate, String identityCheckedAt,
                        String verificationUrl, Map<String, String> externalIds) {
        this(address, subcategory, phone, website, sourceUrl, sourceDate, identityCheckedAt, verificationUrl, externalIds, List.of());
    }
    public static final PlaceDetails EMPTY = new PlaceDetails("", "", "", "", "", "", "", "", Map.of());
    public PlaceDetails {
        address = clean(address); subcategory = clean(subcategory); phone = clean(phone); website = clean(website);
        sourceUrl = clean(sourceUrl); sourceDate = clean(sourceDate); identityCheckedAt = clean(identityCheckedAt);
        verificationUrl = clean(verificationUrl);
        externalIds = externalIds == null ? Map.of() : Map.copyOf(externalIds);
        tags = tags == null ? List.of() : List.copyOf(tags);
    }
    public boolean hasTag(PlaceTag tag) { return tags.stream().anyMatch(fact -> fact.tag() == tag); }
    public String identityText() {
        if (!identityCheckedAt.isBlank()) return "장소 대조 기록 · " + identityCheckedAt.substring(0,Math.min(10,identityCheckedAt.length()));
        return sourceUrl.isBlank() ? "장소 확인 기록 없음" : "공공자료 등록 · 지도 대조 전";
    }
    private static String clean(String value) { return value == null ? "" : value; }
}
