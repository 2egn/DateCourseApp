package com.datecourse;

import java.time.LocalDate;

public record Review(
        String reviewId,
        String author,
        Course course,       // 리뷰 대상 코스 (가게 목록 포함)
        int rating,          // 1~5
        String content,
        LocalDate createdAt
) {}