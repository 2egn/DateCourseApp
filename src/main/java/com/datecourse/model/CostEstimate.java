package com.datecourse.model;

import java.util.List;
import java.util.Locale;

/** Sum only amounts with a confirmed per-person basis; report missing places explicitly. */
public record CostEstimate(long minimum, Long maximum, int unknownPlaces, int placeCount) {
    public static CostEstimate of(List<Place> places, int people) {
        if (people < 1 || people > 20) throw new IllegalArgumentException("인원은 1~20명이어야 합니다.");
        long lower = 0, upper = 0;
        int unknown = 0;
        boolean open = false;
        for (Place place : places) {
            PriceEstimate price = place.priceEstimate();
            if (!price.usablePerPerson()) { unknown++; continue; }
            lower = Math.addExact(lower, Math.multiplyExact((long) price.minimum(), people));
            if (price.maximum() == null) open = true;
            else upper = Math.addExact(upper, Math.multiplyExact((long) price.maximum(), people));
        }
        return new CostEstimate(lower, open ? null : upper, unknown, places.size());
    }

    public long exactTotal() {
        if (unknownPlaces != 0 || maximum == null || minimum != maximum)
            throw new IllegalStateException("전체 비용을 하나의 금액으로 확정할 수 없습니다.");
        return minimum;
    }

    public String display() {
        if (placeCount == 0) return "선택한 장소 없음";
        if (unknownPlaces == placeCount) return "가격 미확인 " + unknownPlaces + "곳";
        String amount = maximum == null ? won(minimum) + " 이상"
                : minimum == maximum ? won(minimum) : won(minimum) + "~" + won(maximum);
        return (unknownPlaces == 0 ? "예상 총비용 " : "확인된 비용 ") + amount
                + (unknownPlaces == 0 ? "" : " + 가격 미확인 " + unknownPlaces + "곳");
    }

    private static String won(long amount) { return String.format(Locale.KOREA, "%,d원", amount); }
}
