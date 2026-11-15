package com.datecourse.model;

/** Values supported by the TMAP pedestrian routing API. */
public enum WalkingSearchOption {
    RECOMMENDED(0, "추천 경로", "걷기 좋은 추천 경로를 안내해요."),
    MAIN_ROADS(4, "대로 우선", "추천 경로 중 큰길을 우선해요."),
    SHORTEST(10, "최단 경로", "이동 거리가 짧은 경로를 찾아요."),
    WITHOUT_STAIRS(30, "계단 제외", "계단을 제외한 최단 경로를 찾아요.");

    private final int code;
    private final String title, description;
    WalkingSearchOption(int code, String title, String description) {
        this.code = code; this.title = title; this.description = description;
    }
    public int code() { return code; }
    public String title() { return title; }
    public String description() { return description; }
}
