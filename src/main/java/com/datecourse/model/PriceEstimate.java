package com.datecourse.model;

import java.util.Locale;

/** Amounts are KRW. Null means unknown, never free. */
public record PriceEstimate(Integer minimum, Integer maximum, Unit unit, String basis,
                            Evidence evidence, String sourceUrl, String checkedAt) {
    public enum Unit { PER_PERSON, PER_DRINK, ADULT_TICKET, UNKNOWN }
    public enum Evidence {
        LEGACY_REFERENCE("기존 참고 가격"), REFERENCE("공개 자료 참고"), GOOGLE_PROVIDED("Google 제공"),
        SIMILAR_ESTIMATE("유사 장소 기준 추정"), DEMO_ASSUMPTION("시연용 추정"),
        ADMIN_REVIEWED("관리자 검토"), UNKNOWN("가격 미확인");
        private final String label;
        Evidence(String label) { this.label = label; }
        public String label() { return label; }
    }
    public PriceEstimate(Integer minimum, Integer maximum, Unit unit, String basis) {
        this(minimum, maximum, unit, basis, minimum == null ? Evidence.UNKNOWN : Evidence.LEGACY_REFERENCE, "", "");
    }

    public PriceEstimate {
        if (unit == null || minimum == null && maximum != null
                || minimum != null && minimum < 0 || maximum != null && maximum < minimum)
            throw new IllegalArgumentException("가격 범위·단위를 확인해 주세요.");
        basis = basis == null ? "" : basis;
        evidence = evidence == null ? (minimum == null ? Evidence.UNKNOWN : Evidence.LEGACY_REFERENCE) : evidence;
        sourceUrl = sourceUrl == null ? "" : sourceUrl;
        checkedAt = checkedAt == null ? "" : checkedAt;
    }

    public static PriceEstimate unknown() { return new PriceEstimate(null, null, Unit.UNKNOWN, "가격 미확인"); }
    public static PriceEstimate exact(int amount) { return new PriceEstimate(amount, amount, Unit.PER_PERSON, "1인 가격"); }
    public boolean usablePerPerson() { return minimum != null && unit != Unit.UNKNOWN; }
    public boolean finite() { return usablePerPerson() && maximum != null; }
    public boolean exact() { return finite() && minimum.equals(maximum); }
    public boolean estimated() { return evidence == Evidence.SIMILAR_ESTIMATE || evidence == Evidence.DEMO_ASSUMPTION; }
    public String evidenceText() { return evidence.label(); }
    public String briefBasis() {
        if (basis.contains("착한가격업소")) return "부산 착한가격업소";
        return switch(evidence) {
            case GOOGLE_PROVIDED -> "Google Places";
            case REFERENCE -> "공공자료";
            case SIMILAR_ESTIMATE -> "유사 장소";
            case DEMO_ASSUMPTION -> "시연용 가정";
            case ADMIN_REVIEWED -> "관리자 검토";
            case LEGACY_REFERENCE -> "기존 참고 정보";
            case UNKNOWN -> "가격 미확인";
        };
    }
    public String explanation() { return briefBasis() + (estimated() ? " 기준 추정 가격" : " · " + evidenceText()); }
    public boolean withinCap(int cap) { return finite() && maximum <= cap; }
    public long sortUpperBound() { return finite() ? maximum : Long.MAX_VALUE; }

    /** Compatibility for old screens accepting only an exact per-person integer. */
    public int exactAmount() {
        if (!exact()) throw new IllegalStateException("가격이 미확인이거나 범위입니다. 가격 범위 모델을 사용해 주세요.");
        return minimum;
    }

    public String display() {
        if (!usablePerPerson()) return "가격 미확인";
        String amount = maximum == null ? won(minimum) + " 이상"
                : minimum.equals(maximum) ? won(minimum) : won(minimum) + "~" + won(maximum);
        String suffix = switch (unit) { case PER_PERSON -> "/인"; case PER_DRINK -> "/잔";
            case ADULT_TICKET -> "/성인"; case UNKNOWN -> ""; };
        return (estimated() ? "약 " : "") + amount + suffix;
    }

    private static String won(long amount) { return String.format(Locale.KOREA, "%,d원", amount); }
}
