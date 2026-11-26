package com.datecourse.model;

/**
 * - 공공 장소와 외부 장소 ID 연결 상태 보관용 모델
 * - 일치 점수, 검토 승인 여부, 실제 장소 여부를 따로 구분
 */
public record ExternalPlaceLink(Provider provider, String externalId, Confidence confidence,
                                ReviewStatus reviewStatus, EntityKind entityKind,
                                String matchMethod, String checkedAt, String reviewNote) {
    public enum Provider { GOOGLE_PLACES, KAKAO }
    public enum Confidence { HIGH, MEDIUM, UNSPECIFIED }
    public enum ReviewStatus { NEEDS_REVIEW, CONFIRMED, REJECTED }
    public enum EntityKind { VENUE, ADDRESS, UNKNOWN }
    public ExternalPlaceLink {
        if (provider == null || confidence == null || reviewStatus == null || entityKind == null)
            throw new IllegalArgumentException("외부 연결의 상태를 확인해 주세요.");
        if (externalId == null || !externalId.matches("[A-Za-z0-9_-]{1,256}"))
            throw new IllegalArgumentException("외부 장소 ID를 확인해 주세요.");
        matchMethod = clean(matchMethod); checkedAt = clean(checkedAt); reviewNote = clean(reviewNote);
        try { if (!checkedAt.isEmpty()) java.time.Instant.parse(checkedAt); }
        catch (java.time.DateTimeException error) { throw new IllegalArgumentException("연결 확인 시각을 확인해 주세요.", error); }
        if (reviewStatus == ReviewStatus.CONFIRMED && (entityKind != EntityKind.VENUE
                || matchMethod.isEmpty() || checkedAt.isEmpty() || reviewNote.isEmpty()))
            throw new IllegalArgumentException("가게 확인·대조 방법·검토 기록 없이 연결을 확정할 수 없습니다.");
    }
    public boolean usable() { return reviewStatus == ReviewStatus.CONFIRMED && entityKind == EntityKind.VENUE; }
    public ExternalPlaceLink identityChanged() {
        return reviewStatus == ReviewStatus.REJECTED ? this : new ExternalPlaceLink(provider, externalId, confidence,
                ReviewStatus.NEEDS_REVIEW, entityKind, matchMethod, checkedAt, "장소명 또는 주소 정정으로 재검토 필요");
    }
    public static ExternalPlaceLink unreviewedGoogle(String id) {
        return new ExternalPlaceLink(Provider.GOOGLE_PLACES, id, Confidence.UNSPECIFIED,
                ReviewStatus.NEEDS_REVIEW, EntityKind.UNKNOWN, "legacy_external_id", "", "");
    }
    private static String clean(String value) { return value == null ? "" : value.strip(); }
}
