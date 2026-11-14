package com.datecourse.model;

/** Stable public facts and optional amenities; not all are search filters. */
public enum PlaceFeature {
    GOOD_PRICE, MODEL_RESTAURANT, FREE_ADMISSION, NEAR_SUBWAY, NEAR_BUS,
    GOOD_FOR_GROUPS, RESERVABLE, TAKEOUT, VEGETARIAN_FOOD, OUTDOOR_SEATING,
    ALLOWS_DOGS, GOOD_FOR_CHILDREN, PARKING, WHEELCHAIR_ACCESSIBLE_ENTRANCE;

    public static PlaceFeature from(PlaceTag tag) { return valueOf(tag.name()); }
}
