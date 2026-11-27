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
        assertEquals("식사 1 + 카페 1 + 문화 1",detail(summary,"카테고리"));
        assertEquals("1000m",detail(summary,"전체 범위"));
        assertEquals("참고 1 + 추정 1 + 미확인 1",detail(summary,"가격 상태"));
        assertEquals("2026.11.15 10:00",detail(summary,"최근 수정"));
        assertEquals(3,summary.places().size());
        assertEquals("2",summary.places().get(1).order());
        assertEquals("카페(카페)",summary.places().get(1).name());
        assertTrue(summary.places().get(2).price().contains("가격 미확인"));
        assertTrue(summary.details().stream().noneMatch(row->row.label().contains("소요시간")));
    }
    private static String detail(SavedCoursePresenter.Summary summary,String label) {
        return summary.details().stream().filter(row->row.label().equals(label)).findFirst().orElseThrow().value();
    }

    @Test void punctuationAndLineBreaksRemainInsideTheirOwnColumns() {
        String name="첫. 장소:\n둘째 줄",address="부산: 중구\n상세: 2층";
        var details=new PlaceDetails(address,"카페","","","","","","",Map.of());
        var place=new Place(1,"a",name,"CAFE",35.15,129.05,PriceEstimate.unknown(),0,details);
        var summary=new SavedCoursePresenter().present(new SavedCourse(1,7,
                new RoutePlan(35.15,129.05,2,1000,"",List.of(place))));
        var row=summary.places().getFirst();
        assertEquals("1",row.order());assertEquals(name+"(카페)",row.name());
        assertEquals(address,row.address());assertTrue(row.price().contains("가격 미확인"));
        assertEquals("조회 전",row.travel());
        assertThrows(UnsupportedOperationException.class,()->summary.details().clear());
        assertThrows(UnsupportedOperationException.class,()->summary.places().clear());
    }
}
