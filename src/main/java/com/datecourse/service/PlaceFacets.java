package com.datecourse.service;

import com.datecourse.model.Place;

/** User-facing groups over the public source's detailed business types. */
public final class PlaceFacets {
    private PlaceFacets() {}
    /** Keep the public business type; never guess a menu from the venue name. */
    public static String detailType(Place p) {
        String value=p.details().subcategory().trim();
        return switch(value) {
            case "전국도시공원정보표준데이터" -> "도시공원";
            case "전국박물관미술관정보표준데이터" -> "박물관·미술관";
            case "전국관광지정보표준데이터" -> "관광지";
            case "" -> subtype(p);
            default -> value;
        };
    }
    public static String subtype(Place p) {
        String s = p.details().subcategory();
        if ("CAFE".equals(p.category())) return s.contains("디저트") || s.contains("빵") ? "디저트 카페" : "카페";
        if ("CULTURE".equals(p.category())) {
            if (s.contains("도서관")) return "도서관";
            if (s.contains("공원")) return "공원";
            if (s.contains("박물관") || s.contains("미술관") || s.contains("전시")) return "전시·관람";
            return "기타 문화";
        }
        if (s.contains("일식") || s.contains("초밥") || s.contains("돈가스")) return "일식";
        if (s.contains("중국") || s.contains("중식") || s.contains("마라")) return "중식";
        if (s.contains("경양식") || s.contains("양식") || s.contains("피자") || s.contains("버거")) return "양식";
        if (s.contains("분식") || s.contains("김밥")) return "분식";
        if (s.contains("백반") || s.contains("한식") || s.contains("국/") || s.contains("고기")
                || s.contains("밀면") || s.contains("국수") || s.contains("해산물") || s.contains("족발")) return "한식";
        return "기타 식사";
    }
}
