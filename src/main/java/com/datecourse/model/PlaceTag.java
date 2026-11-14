package com.datecourse.model;

/** Tags are attached only when their source or calculation is available. */
public enum PlaceTag {
    GOOD_PRICE("착한가격"), MODEL_RESTAURANT("모범음식점"), FREE_ADMISSION("무료입장"),
    NEAR_SUBWAY("역 가까움"), NEAR_BUS("정류장 가까움"),
    GOOD_FOR_GROUPS("단체 이용"), RESERVABLE("예약 가능"), TAKEOUT("포장 가능"),
    VEGETARIAN_FOOD("채식 메뉴"), OUTDOOR_SEATING("야외 좌석"), ALLOWS_DOGS("반려견 동반"),
    GOOD_FOR_CHILDREN("어린이 이용"), PARKING("주차 가능"), WHEELCHAIR_ACCESSIBLE_ENTRANCE("휠체어 출입");
    private final String label;
    PlaceTag(String label) { this.label = label; }
    public String label() { return label; }
    @Override public String toString() { return label; }
}
