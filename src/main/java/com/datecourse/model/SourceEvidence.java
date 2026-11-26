package com.datecourse.model;

import java.time.LocalDate;
import java.time.Instant;

/**
 * - 장소 정보의 출처와 기준일 보관용 모델
 * - 원본 자료의 기준일과 실제 조회 또는 계산한 시점은 따로 관리
 */
public record SourceEvidence(Origin origin, String basis, String sourceUrl,
                             String referenceDate, String checkedAt) {
    public enum Origin { PUBLIC_DATA, OFFICIAL_SITE, ADMIN_REVIEW, CALCULATED,
                         MATCH_VALIDATION, LEGACY, GOOGLE_PLACES, UNKNOWN }
    public static final SourceEvidence UNKNOWN = new SourceEvidence(Origin.UNKNOWN, "", "", "", "");
    public SourceEvidence {
        if (origin == null) throw new IllegalArgumentException("정보 근거의 종류가 필요합니다.");
        basis = clean(basis); sourceUrl = clean(sourceUrl);
        referenceDate = clean(referenceDate); checkedAt = clean(checkedAt);
        try {
            if (!referenceDate.isEmpty()) LocalDate.parse(referenceDate);
            if (!checkedAt.isEmpty()) {
                try { Instant.parse(checkedAt); }
                catch (java.time.format.DateTimeParseException e) { LocalDate.parse(checkedAt); }
            }
        } catch (java.time.DateTimeException error) {
            throw new IllegalArgumentException("자료 기준일과 확인일을 확인해 주세요.", error);
        }
        if (origin != Origin.UNKNOWN && basis.isEmpty())
            throw new IllegalArgumentException("정보의 근거 설명이 필요합니다.");
    }
    private static String clean(String value) { return value == null ? "" : value.strip(); }
}
