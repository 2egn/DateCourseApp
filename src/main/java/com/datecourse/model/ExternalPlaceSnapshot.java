package com.datecourse.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/** A single retrieval's in-memory details. Never attach this to persisted Place or saved courses. */
@JsonSerialize(using = ExternalPlaceSnapshot.NoPersistence.class)
public record ExternalPlaceSnapshot(ExternalPlaceLink link, Instant fetchedAt,
        ProviderPriceRange priceRange, Double rating, Integer ratingCount,
        Map<PlaceFeature, FactState> features, List<OpeningInterval> openingIntervals) {
    public record OpeningInterval(Instant opensAt, Instant closesAt) {
        public OpeningInterval {
            if (opensAt == null || closesAt == null || !closesAt.isAfter(opensAt))
                throw new IllegalArgumentException("영업 구간을 확인해 주세요.");
        }
        public boolean contains(Instant arrival) {
            return !arrival.isBefore(opensAt) && arrival.isBefore(closesAt);
        }
    }
    public ExternalPlaceSnapshot {
        if (link == null || !link.usable() || fetchedAt == null)
            throw new IllegalArgumentException("검토 완료 연결과 조회 시각이 필요합니다.");
        if (rating != null && (!Double.isFinite(rating) || rating < 0 || rating > 5)
                || ratingCount != null && ratingCount < 0)
            throw new IllegalArgumentException("외부 평가 정보를 확인해 주세요.");
        features = features == null ? Map.of() : Map.copyOf(features);
        openingIntervals = openingIntervals == null ? List.of() : List.copyOf(openingIntervals);
    }
    public FactState featureState(PlaceFeature feature) { return features.getOrDefault(feature, FactState.UNKNOWN); }
    /** No known interval means unknown, not closed. Gaps require a complete schedule to prove closure. */
    public FactState openAt(Instant arrival) {
        return openingIntervals.stream().anyMatch(p -> p.contains(arrival)) ? FactState.AVAILABLE : FactState.UNKNOWN;
    }
    @Override public String toString() { return "ExternalPlaceSnapshot[provider=" + link.provider() + ", fetchedAt=" + fetchedAt + "]"; }
    public static final class NoPersistence extends JsonSerializer<ExternalPlaceSnapshot> {
        @Override public void serialize(ExternalPlaceSnapshot value, JsonGenerator output, SerializerProvider provider) throws IOException {
            throw new IOException("외부 상세 조회 결과는 영구 JSON 데이터로 저장할 수 없습니다.");
        }
    }
}
