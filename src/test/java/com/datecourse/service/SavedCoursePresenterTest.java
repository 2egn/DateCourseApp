package com.datecourse.service;

import com.datecourse.model.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class SavedCoursePresenterTest {
    @Test void savedSummaryShowsCompositionAndPriceUncertaintyWithoutInventingMissingCosts() {
        var places=List.of(new Place(1,"a","식당","MEAL",35.15,129.05,9000,4),
                new Place(2,"b","카페","CAFE",35.15,129.05,
                        new PriceEstimate(4000,6000,PriceEstimate.Unit.PER_PERSON,"유사 장소",PriceEstimate.Evidence.SIMILAR_ESTIMATE,"",""),4,PlaceDetails.EMPTY),
                new Place(3,"c","전시관","CULTURE",35.15,129.05,PriceEstimate.unknown(),0,PlaceDetails.EMPTY));
        var course=new SavedCourse(1,7,new RoutePlan(35.15,129.05,2,1000,"나의 코스",places),Instant.parse("2026-11-15T01:00:00Z"));
        var summary=new SavedCoursePresenter().present(course);
        assertTrue(summary.overview().contains("3곳 · 2명"));
        assertTrue(summary.overview().contains("가격 미확인 1곳"));
        assertTrue(summary.details().contains("식사 1곳 · 카페 1곳 · 문화 1곳"));
        assertTrue(summary.details().contains("출발지 중심 1000m (직선)"));
        assertTrue(summary.details().contains("참고 1 · 추정 1 · 미확인 1곳"));
        assertTrue(summary.details().contains("2026.11.15 10:00"));
        assertEquals(3,summary.places().size());
        assertTrue(summary.places().get(1).contains("2. 카페 · 카페"));
        assertTrue(summary.places().get(2).contains("가격 미확인"));
        assertFalse(summary.details().contains("소요시간"));
    }
}
