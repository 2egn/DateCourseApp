package com.datecourse.model;

import java.util.Locale;

/** Amounts are KRW. Null means unknown, never free. */
public record PriceEstimate(Integer minimum, Integer maximum, Unit unit, String basis) {
    public enum Unit { PER_PERSON, PER_DRINK, ADULT_TICKET, UNKNOWN }

    public PriceEstimate {
        if (unit == null || minimum == null && maximum != null
                || minimum != null && minimum < 0 || maximum != null && maximum < minimum)
            throw new IllegalArgumentException("가격 범위·단위를 확인해 주세요.");
        basis = basis == null ? "" : basis;
    }

    public static PriceEstimate unknown() { return new PriceEstimate(null, null, Unit.UNKNOWN, "가격 미확인"); }
    public static PriceEstimate exact(int amount) { return new PriceEstimate(amount, amount, Unit.PER_PERSON, "1인 가격"); }
    public boolean usablePerPerson() { return minimum != null && unit != Unit.UNKNOWN; }
    public boolean finite() { return usablePerPerson() && maximum != null; }
    public boolean exact() { return finite() && minimum.equals(maximum); }
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
        return amount + suffix;
    }

    private static String won(long amount) { return String.format(Locale.KOREA, "%,d원", amount); }
}
