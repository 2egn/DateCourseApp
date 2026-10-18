package com.datecourse;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// 백엔드 연동 전까지 쓰는 가짜 리뷰
public class FakeReviewData {

    // true로 바꾸면 "리뷰가 하나도 없는 상태"를 테스트할 수 있음
    private static final boolean EMPTY_TEST = false;

    public static List<Review> getReviews() {
        List<Review> list = new ArrayList<>();
        if (EMPTY_TEST) return list;

        List<Course> courses = FakeCourseData.getSavedCourses();
        Course c1 = courses.get(0);
        Course c2 = courses.get(1);
        Course c3 = courses.get(2);

        list.add(new Review("r1", "민지", c1, 5,
                "산책하고 카페까지 동선이 정말 좋았어요. 예산도 딱 맞았어요!", LocalDate.of(2026, 10, 8)));
        list.add(new Review("r2", "준호", c2, 4,
                "파스타집이 맛있었는데 주말엔 웨이팅이 좀 길어요.", LocalDate.of(2026, 10, 6)));
        list.add(new Review("r3", "서연", c3, 3,
                "영화관에서 카페까지 걸어서 이동이 조금 멀었어요. 그래도 무난했어요.", LocalDate.of(2026, 10, 3)));
        list.add(new Review("r4", "하늘", c1, 5,
                "전시관이 생각보다 알차서 만족했어요. 부모님과 가도 좋을 듯해요.", LocalDate.of(2026, 9, 29)));
        list.add(new Review("r5", "도윤", c2, 2,
                "추천된 가격보다 실제로는 조금 더 나왔어요.", LocalDate.of(2026, 9, 25)));
        return list;
    }
}