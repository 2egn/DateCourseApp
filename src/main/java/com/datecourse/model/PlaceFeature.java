package com.datecourse.model;

/**
 * - 공공 지정 및 시설 정보의 속성 종류 구분용 enum
 * - 일부는 표시용으로만 사용하며 전부 검색 필터로 사용하지는 않음
 */
public enum PlaceFeature {
    GOOD_PRICE, MODEL_RESTAURANT, FREE_ADMISSION, NEAR_SUBWAY, NEAR_BUS,
    GOOD_FOR_GROUPS, RESERVABLE, TAKEOUT, VEGETARIAN_FOOD, OUTDOOR_SEATING,
    ALLOWS_DOGS, GOOD_FOR_CHILDREN, PARKING, WHEELCHAIR_ACCESSIBLE_ENTRANCE;

    public static PlaceFeature from(PlaceTag tag) { return valueOf(tag.name()); }
}
