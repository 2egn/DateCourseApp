package com.datecourse.model;

import java.util.Locale;

/**
 * - 가격의 범위, 단위, 출처 및 근거 보관용 모델
 * - minimum이 null이면 가격 미확인, maximum만 null이면 최소 금액만 확인된 상태
 * - 무료는 금액 0으로 표현, 추정 여부는 금액이 아닌 Evidence로 판단
 * - NextPlaceService의 가격 필터와 CostEstimate의 인원별 합산에서 공통 사용
 */
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

    // 음수·역전된 범위·최소 없는 최대값 차단, 누락된 근거는 기존 참고값 또는 미확인으로 분류
    public PriceEstimate {
        if (unit == null || minimum == null && maximum != null
                || minimum != null && minimum < 0 || maximum != null && maximum < minimum)
            throw new IllegalArgumentException("가격 범위·단위를 확인해 주세요.");
        basis = basis == null ? "" : basis;
        evidence = evidence == null ? (minimum == null ? Evidence.UNKNOWN : Evidence.LEGACY_REFERENCE) : evidence;
        sourceUrl = sourceUrl == null ? "" : sourceUrl;
        checkedAt = checkedAt == null ? "" : checkedAt;
    }

    // 가격을 모르는 상태 생성, 무료 가격 0과 구분
    public static PriceEstimate unknown() { return new PriceEstimate(null, null, Unit.UNKNOWN, "가격 미확인"); }
    // 1인 정액을 최소·최대가 같은 가격 모델로 생성
    public static PriceEstimate exact(int amount) { return new PriceEstimate(amount, amount, Unit.PER_PERSON, "1인 가격"); }
    // 최소 금액과 지원 단위가 있으면 인원별 계산에 사용 가능, 매장 실값 여부와는 별도
    public boolean usablePerPerson() { return minimum != null && unit != Unit.UNKNOWN; }
    // 최소·최대가 모두 있는 범위인지 확인, 금액 확정 여부는 exact()에서 판단
    public boolean finite() { return usablePerPerson() && maximum != null; }
    // 가격 범위의 양 끝이 같은 숫자인지 확인. 실제 메뉴 검증 여부와는 별도
    public boolean exact() { return finite() && minimum.equals(maximum); }
    // 통계 추정 또는 시연용 가정 가격인지 확인
    public boolean estimated() { return evidence == Evidence.SIMILAR_ESTIMATE || evidence == Evidence.DEMO_ASSUMPTION; }
    public String evidenceText() { return evidence.label(); }
    // 자료별 구체적인 근거를 우선 표시, 일치하지 않으면 Evidence의 출처 분류 사용
    public String briefBasis() {
        if (basis.startsWith("부산 지역별 외식비 ·")) return "부산 지역별 외식비";
        if (basis.startsWith("외식업체 조사 ·")) return "외식업체 조사";
        if (basis.startsWith("매장 공개 메뉴 ·")) return "매장 공개 메뉴";
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
    // 최댓값이 가격 상한 이하여야 통과, 미확인·상한 없는 가격의 포함 여부는 NextPlaceService에서 별도 판단
    public boolean withinCap(int cap) { return finite() && maximum <= cap; }
    // 상한 없는 가격은 정렬 시 뒤쪽으로 배치
    public long sortUpperBound() { return finite() ? maximum : Long.MAX_VALUE; }

    /** 1인 정액 정수만 받는 이전 화면과의 호환용 함수 */
    public int exactAmount() {
        if (!exact()) throw new IllegalStateException("가격이 미확인이거나 범위입니다. 가격 범위 모델을 사용해 주세요.");
        return minimum;
    }

    // 최소·최대와 단위를 표시, 통계 추정은 ‘약’으로 구분하고 근거는 explanation()으로 제공
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
