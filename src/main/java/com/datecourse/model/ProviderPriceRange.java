package com.datecourse.model;

import java.math.BigDecimal;

/** Provider-reported range. Deliberately has no conversion to a per-person course budget. */
public record ProviderPriceRange(BigDecimal minimum, BigDecimal maximum, String currencyCode) {
    public ProviderPriceRange {
        if (minimum == null || minimum.signum() < 0 || maximum != null && maximum.compareTo(minimum) < 0
                || currencyCode == null || !currencyCode.matches("[A-Z]{3}"))
            throw new IllegalArgumentException("외부 제공 가격 범위와 통화를 확인해 주세요.");
    }
}
