package com.datecourse.demo;

import com.datecourse.model.Course;
import com.datecourse.model.CoursePlace;

import java.util.ArrayList;
import java.util.List;

// 백엔드 연동 전까지 쓰는 가짜 저장 코스
public class FakeCourseData {

    public static List<Course> getSavedCourses() {
        List<Course> list = new ArrayList<>();

        list.add(make("c1", "수원 화성 산책 코스", List.of(
                new CoursePlace("p1", "할매순대국", "식사", "수원시 팔달구 1-1", 37.28, 127.01, 9000),
                new CoursePlace("p2", "모닝커피", "카페", "수원시 팔달구 2-2", 37.29, 127.02, 6000),
                new CoursePlace("p3", "화성 전시관", "문화", "수원시 팔달구 3-3", 37.29, 127.01, 10000))));

        list.add(make("c2", "파스타 데이트", List.of(
                new CoursePlace("p4", "파스타하우스", "식사", "수원시 영통구 4-4", 37.25, 127.07, 18000),
                new CoursePlace("p5", "브런치카페", "카페", "수원시 영통구 5-5", 37.26, 127.07, 9000))));

        list.add(make("c3", "영화 보는 날", List.of(
                new CoursePlace("p6", "라멘집", "식사", "수원시 장안구 6-6", 37.30, 127.00, 11000),
                new CoursePlace("p7", "시네마극장", "문화", "수원시 장안구 7-7", 37.31, 127.00, 14000),
                new CoursePlace("p8", "달빛카페", "카페", "수원시 장안구 8-8", 37.31, 127.01, 7000))));

        return list;
    }

    // 총 금액을 자동으로 더해서 Course 생성
    private static Course make(String id, String title, List<CoursePlace> coursePlaces) {
        int total = coursePlaces.stream().mapToInt(CoursePlace::price).sum();
        return new Course(id, title, coursePlaces, total);
    }
}