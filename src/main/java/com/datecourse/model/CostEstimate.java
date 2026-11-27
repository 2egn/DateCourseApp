package com.datecourse.model;

import java.util.List;
import java.util.Locale;

/**
 * - 장소별 가격에 인원을 적용한 예상 비용 계산용 모델
 * - Place의 PriceEstimate를 기준으로 합산, 미확인 및 추정 장소 수는 별도 보존
 */
public record CostEstimate(long minimum, Long maximum, int unknownPlaces, int placeCount, int estimatedPlaces) {
    public CostEstimate(long minimum, Long maximum, int unknownPlaces, int placeCount) {
        this(minimum, maximum, unknownPlaces, placeCount, 0);
    }
    /**
     * PriceEstimate의 최소·최대 금액에 인원을 곱해 합산, 미확인은 금액 대신 장소 수로 보존
     * 추정 금액도 계산에 포함하되 estimatedPlaces로 구분, 총액의 근거가 확정값처럼 보이지 않게 처리
     * 가격 보완은 사전 데이터 도구에서 수행, 여기서는 전달받은 가격만 계산
     */
    public static CostEstimate of(List<Place> places, int people) {
        if (people < 1 || people > 20) throw new IllegalArgumentException("인원은 1~20명이어야 합니다.");
        long lower = 0, upper = 0;
        int unknown = 0;
        int estimated = 0;
        boolean open = false;
        for (Place place : places) {
            PriceEstimate price = place.priceEstimate();
            if (!price.usablePerPerson()) { unknown++; continue; }
            if (price.estimated()) estimated++;
            // long 연산과 Exact 계열로 금액 초과 시 잘못된 합계 대신 예외 발생
            lower = Math.addExact(lower, Math.multiplyExact((long) price.minimum(), people));
            // 상한 없는 가격이 포함되면 총비용 상한도 미확정 처리
            if (price.maximum() == null) open = true;
            else upper = Math.addExact(upper, Math.multiplyExact((long) price.maximum(), people));
        }
        return new CostEstimate(lower, open ? null : upper, unknown, places.size(), estimated);
    }

    // 미확인·추정·열린 범위가 없는 경우에만 단일 합계 반환
    public long exactTotal() {
        if (estimatedPlaces != 0 || unknownPlaces != 0 || maximum == null || minimum != maximum)
            throw new IllegalStateException("전체 비용을 하나의 금액으로 확정할 수 없습니다.");
        return minimum;
    }

    // 계산된 금액과 미확인·추정 장소 수를 구분해 표시
    public String display() {
        if (placeCount == 0) return "선택한 장소 없음";
        if (unknownPlaces == placeCount) return "가격 미확인 " + unknownPlaces + "곳";
        String amount = amountDisplay();
        return (unknownPlaces == 0 ? "예상 총비용 " : estimatedPlaces > 0 ? "계산 가능한 예상 비용 " : "확인된 비용 ") + amount
                + (unknownPlaces == 0 ? "" : " + 가격 미확인 " + unknownPlaces + "곳")
                + (estimatedPlaces == 0 ? "" : " · 추정 " + estimatedPlaces + "곳 포함");
    }

    // 단일 금액·최소~최대·최소 이상 중 가격 범위에 맞는 표현 반환
    public String amountDisplay() {
        return maximum == null ? won(minimum) + " 이상"
                : minimum == maximum ? won(minimum) : won(minimum) + "~" + won(maximum);
    }

    private static String won(long amount) { return String.format(Locale.KOREA, "%,d원", amount); }
}
