package com.datecourse.model;

import java.util.Set;

/** Filters are evaluated before the display limit. Empty facets mean no restriction. */
public record NextPlaceCondition(String activity, int maxPrice, int radius, boolean includeUnknown,
                                 Set<String> subtypes, Set<PlaceTag> tags, int displayLimit,
                                 Set<String> detailTypes, PlacePriceFilter priceFilter) {
    public NextPlaceCondition(String activity,int maxPrice,int radius,boolean includeUnknown,
                              Set<String> subtypes,Set<PlaceTag> tags,int displayLimit) {
        this(activity,maxPrice,radius,includeUnknown,subtypes,tags,displayLimit,Set.of(),PlacePriceFilter.ANY);
    }
    public NextPlaceCondition {
        detailTypes=detailTypes==null?Set.of():Set.copyOf(detailTypes);
        priceFilter=priceFilter==null?PlacePriceFilter.ANY:priceFilter;
        subtypes = subtypes == null ? Set.of() : Set.copyOf(subtypes);
        tags = tags == null ? Set.of() : Set.copyOf(tags);
        if (activity == null || !Set.of("식사", "카페", "문화").contains(activity) || maxPrice < 0 || radius < 200 || radius > 2000
                || !Set.of(5, 10, 20, 30).contains(displayLimit))
            throw new IllegalArgumentException("활동·가격·반경·표시 수를 확인해 주세요.");
    }
}
