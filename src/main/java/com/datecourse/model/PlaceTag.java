package com.datecourse.model;

/** Tags are attached only when their source or calculation is available. */
public enum PlaceTag {
    GOOD_PRICE("착한가격"), MODEL_RESTAURANT("모범음식점"), FREE_ADMISSION("무료입장"),
    NEAR_SUBWAY("역 가까움"), NEAR_BUS("정류장 가까움");
    private final String label;
    PlaceTag(String label) { this.label = label; }
    public String label() { return label; }
    @Override public String toString() { return label; }
}
