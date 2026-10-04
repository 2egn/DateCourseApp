package com.datecourse.domain;

public enum Category {
    MEAL("식사"), CAFE("카페"), CULTURE("문화"), ENTERTAINMENT("오락");

    private final String label;

    Category(String label) { this.label = label; }

    public String label() { return label; }
}
