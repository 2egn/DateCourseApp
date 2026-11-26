package com.datecourse.model;

import java.util.List;
import java.util.Locale;

/**
 * - 장소별 가격에 인원을 적용한 예상 비용 계산용 모델
 * - 1인 기준으로 환산 가능한 금액만 합산, 미확인 및 추정 장소 수는 따로 표시
 */
public record CostEstimate(long minimum, Long maximum, int unknownPlaces, int placeCount, int estimatedPlaces) {
    public CostEstimate(long minimum, Long maximum, int unknownPlaces, int placeCount) {
        this(minimum, maximum, unknownPlaces, placeCount, 0);
    }
    // 장소별 가격 범위에 인원을 곱하고 미확인·추정 장소 수를 같이 계산
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
            lower = Math.addExact(lower, Math.multiplyExact((long) price.minimum(), people));
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

    public String display() {
        if (placeCount == 0) return "선택한 장소 없음";
        if (unknownPlaces == placeCount) return "가격 미확인 " + unknownPlaces + "곳";
        String amount = amountDisplay();
        return (unknownPlaces == 0 ? "예상 총비용 " : estimatedPlaces > 0 ? "계산 가능한 예상 비용 " : "확인된 비용 ") + amount
                + (unknownPlaces == 0 ? "" : " + 가격 미확인 " + unknownPlaces + "곳")
                + (estimatedPlaces == 0 ? "" : " · 추정 " + estimatedPlaces + "곳 포함");
    }

    public String amountDisplay() {
        return maximum == null ? won(minimum) + " 이상"
                : minimum == maximum ? won(minimum) : won(minimum) + "~" + won(maximum);
    }

    private static String won(long amount) { return String.format(Locale.KOREA, "%,d원", amount); }
}
