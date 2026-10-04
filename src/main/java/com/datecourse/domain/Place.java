package com.datecourse.domain;

import java.util.Objects;

public record Place(String id, String name, Category category, GeoPoint location,
                    long priceLowerWon, long priceUpperWon, PriceUnit priceUnit,
                    boolean estimatedPrice, double rating, int reviewCount) {
    public enum PriceUnit { PER_PERSON, PER_GROUP }

    public Place {
        Objects.requireNonNull(id);
        Objects.requireNonNull(name);
        Objects.requireNonNull(category);
        Objects.requireNonNull(location);
        Objects.requireNonNull(priceUnit);
        if (id.isBlank() || name.isBlank() || priceLowerWon < 0 || priceUpperWon < priceLowerWon
                || !Double.isFinite(rating) || rating < 0 || rating > 5 || reviewCount < 0) {
            throw new IllegalArgumentException("장소 정보가 올바르지 않습니다.");
        }
    }

    public long lowerCost(int people) { return cost(priceLowerWon, people); }
    public long upperCost(int people) { return cost(priceUpperWon, people); }

    private long cost(long price, int people) {
        if (people < 1) throw new IllegalArgumentException("인원은 1명 이상이어야 합니다.");
        return Math.multiplyExact(price, priceUnit == PriceUnit.PER_PERSON ? people : 1);
    }

    // 리뷰가 적은 장소의 점수는 초기 기준 평점 3.5 쪽으로 보정한다.
    public double adjustedRating() {
        return (rating * reviewCount + 3.5 * 5) / (reviewCount + 5.0);
    }
}
