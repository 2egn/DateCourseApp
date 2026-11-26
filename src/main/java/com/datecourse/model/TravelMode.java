package com.datecourse.model;

/** 이동수단별 전체 반경, 구간 반경 상한 및 시간 안내 문구 관리용 enum */
public enum TravelMode {
    WALKING("도보",2000,2000), DRIVING("자동차",30000,60000), TRANSIT("대중교통 + 도보",20000,40000);
    public static final int MIN_RADIUS=200;
    public static final int MAX_RADIUS=java.util.Arrays.stream(values()).mapToInt(TravelMode::maximumRadius).max().orElseThrow();
    public static final int MAX_LEG_RADIUS=java.util.Arrays.stream(values()).mapToInt(TravelMode::maximumLegRadius).max().orElseThrow();
    private final String label;
    private final int maximumRadius,maximumLegRadius;
    TravelMode(String label,int maximumRadius,int maximumLegRadius){this.label=label;this.maximumRadius=maximumRadius;this.maximumLegRadius=maximumLegRadius;}
    public int maximumRadius(){return maximumRadius;}
    public int maximumLegRadius(){return maximumLegRadius;}
    public String label(){return label;}
    public String timeNote(){return switch(this){case WALKING->"체류시간 제외";case DRIVING->"주차·주차 후 도보·체류시간 제외 · 교통비 별도";case TRANSIT->"구간별 빠른 이동 선택 · 도보 포함 · 체류시간 제외 · 교통비 별도";};}
    @Override public String toString(){return label;}
}
