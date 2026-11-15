package com.datecourse.service;

import com.datecourse.model.Course;
import com.datecourse.model.CoursePlace;
import com.datecourse.model.Place;

import java.util.List;

/** Converts a recommendation to the editor model without changing visit order. */
public final class RecommendedCourseMapper {
    private RecommendedCourseMapper() {}

    public static Course toCourse(List<Place> places, int people) {
        if (places.isEmpty() || people < 1) {
            throw new IllegalArgumentException("추천 장소와 인원을 확인해 주세요.");
        }
        if (places.stream().anyMatch(p -> !p.priceEstimate().exact()))
            throw new IllegalArgumentException("이전 편집 모델에는 가격 미확인·범위를 전달할 수 없습니다.");
        List<CoursePlace> coursePlaces = places.stream()
                .map(place -> new CoursePlace(
                        place.placeId() == null || place.placeId().isBlank()
                                ? "record:" + place.recordId() : place.placeId(),
                        place.name(), PlaceSearchService.categoryName(place.category()),
                        "주소 정보 없음", place.lat(), place.lng(),
                        Math.multiplyExact(place.price(), people)))
                .toList();
        int total = Math.toIntExact(coursePlaces.stream().mapToLong(CoursePlace::price).sum());
        return new Course(null, "추천 코스", coursePlaces, total);
    }
}
