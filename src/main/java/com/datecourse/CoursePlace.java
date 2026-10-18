package com.datecourse;

public record CoursePlace(
        String placeId,
        String name,
        String category,   // 식사 / 카페 / 문화
        String address,
        double lat,
        double lng,
        int price
) {}