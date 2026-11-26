package com.datecourse.model;

import java.util.Set;

/**
 * - 다음 장소 검색에 사용할 활동, 가격, 반경 및 필터 조건 보관용 모델
 * - 필터 적용 후 표시 개수 제한, 비어 있는 세부 조건은 제한 없이 처리
 */
public record NextPlaceCondition(String activity, int maxPrice, int radius, boolean includeUnknown,
                                 Set<String> subtypes, Set<PlaceTag> tags, int displayLimit,
                                 Set<String> detailTypes, PlacePriceFilter priceFilter) {
    public static final int DEFAULT_DISPLAY_LIMIT = 5;
    public static final java.util.List<Integer> DISPLAY_LIMITS = java.util.List.of(DEFAULT_DISPLAY_LIMIT,10,20,30);

    public NextPlaceCondition(String activity,int maxPrice,int radius,boolean includeUnknown,
                              Set<String> subtypes,Set<PlaceTag> tags,int displayLimit) {
        this(activity,maxPrice,radius,includeUnknown,subtypes,tags,displayLimit,Set.of(),PlacePriceFilter.ANY);
    }
    public NextPlaceCondition {
        detailTypes=detailTypes==null?Set.of():Set.copyOf(detailTypes);
        priceFilter=priceFilter==null?PlacePriceFilter.ANY:priceFilter;
        subtypes = subtypes == null ? Set.of() : Set.copyOf(subtypes);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        if (activity == null || !Set.of("식사", "카페", "문화").contains(activity) || maxPrice < 0 || radius < TravelMode.MIN_RADIUS || radius > TravelMode.MAX_LEG_RADIUS
                || !DISPLAY_LIMITS.contains(displayLimit))
            throw new IllegalArgumentException("활동·가격·반경·표시 수를 확인해 주세요.");
    }
}
