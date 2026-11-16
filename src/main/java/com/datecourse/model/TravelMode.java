package com.datecourse.model;

public enum TravelMode {
    WALKING("도보"), DRIVING("자동차"), TRANSIT("대중교통");
    private final String label;
    TravelMode(String label){this.label=label;}
    public String label(){return label;}
    public String timeNote(){return switch(this){case WALKING->"체류시간 제외";case DRIVING->"주차·주차 후 도보·체류시간 제외 · 교통비 별도";case TRANSIT->"체류시간 제외 · 교통비 별도";};}
    @Override public String toString(){return label;}
}
