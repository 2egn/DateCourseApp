package com.datecourse.model;

import java.math.BigDecimal;

/**
 * - 외부 API에서 제공한 가격 범위 보관용 모델
 * - 제공사 기준 금액이므로 코스의 1인 예산으로 자동 환산하지 않음
 */
public record ProviderPriceRange(BigDecimal minimum, BigDecimal maximum, String currencyCode) {
    public ProviderPriceRange {
        if (minimum == null || minimum.signum() < 0 || maximum != null && maximum.compareTo(minimum) < 0
                || currencyCode == null || !currencyCode.matches("[A-Z]{3}"))
            throw new IllegalArgumentException("외부 제공 가격 범위와 통화를 확인해 주세요.");
    }
}
