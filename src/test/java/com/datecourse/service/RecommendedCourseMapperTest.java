package com.datecourse.service;

import com.datecourse.model.Place;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecommendedCourseMapperTest {
    @Test
    void preservesOrderIdentityCoordinatesAndGroupCost() {
        Place cafe = new Place(12, "cafe", "카페", "CAFE", 35.1, 129.1, 5000, 4);
        Place meal = new Place(3, null, "식당", "MEAL", 35.2, 129.2, 10000, 4);
        var course = RecommendedCourseMapper.toCourse(List.of(cafe, meal), 2);
        assertEquals(List.of("cafe", "record:3"), course.coursePlaces().stream().map(p -> p.placeId()).toList());
        assertEquals(List.of("카페", "식사"), course.coursePlaces().stream().map(p -> p.category()).toList());
        assertEquals(35.1, course.coursePlaces().get(0).lat());
        assertEquals(129.2, course.coursePlaces().get(1).lng());
        assertEquals(10000, course.coursePlaces().get(0).price());
        assertEquals(30000, course.totalPrice());
        assertEquals(5000, cafe.price());
    }
}
