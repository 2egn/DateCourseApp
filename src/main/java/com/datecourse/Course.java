package com.datecourse;

import java.util.List;

public record Course(
        String courseId,
        String title,
        List<CoursePlace> coursePlaces,   // 방문 순서대로
        int totalPrice
) {}