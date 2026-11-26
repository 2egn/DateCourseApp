package com.datecourse.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * - 외부 API에서 한 번 조회한 장소 상세정보 보관용 모델
 * - 현재 메모리에서만 사용, Place 및 저장 코스의 영구 데이터에는 포함하지 않음
 */
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
    /** 영업 구간이 없으면 미확인으로 처리. 전체 시간표가 있어야 구간 밖을 영업 종료로 판단 */
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
