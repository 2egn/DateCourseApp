package com.datecourse.service;

import com.datecourse.model.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** Formats only the saved snapshot: no provider requests or invented travel times. */
public final class SavedCoursePresenter {
    public record Summary(String overview, String details, List<String> places) {
        public Summary { places=List.copyOf(places); }
    }
    private final PlacePresenter placePresenter=new PlacePresenter();
    public Summary present(SavedCourse course) {
        var plan=course.plan();var cost=plan.costEstimate();
        var categories=new LinkedHashMap<String,Long>();
        for(var place:plan.places())categories.merge(PlaceSearchService.categoryName(place.category()),1L,Long::sum);
        String types=categories.entrySet().stream().map(e->e.getKey()+" "+e.getValue()+"곳").collect(Collectors.joining(" · "));
        String updated=course.updatedAt()==null?"시각 미확인":DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
                .withZone(ZoneId.of("Asia/Seoul")).format(course.updatedAt());
        int reference=cost.placeCount()-cost.unknownPlaces()-cost.estimatedPlaces();
        String details="코스 구성 · "+plan.places().size()+"곳 · "+plan.people()+"명"
                +(types.isEmpty()?"":"\n"+types)
                +"\n이동 방법 · "+plan.travelMode().label()+"\n전체 범위 · 출발지 중심 "+plan.radius()+"m (직선)"
                +"\n가격 상태 · 참고 "+reference+" · 추정 "+cost.estimatedPlaces()+" · 미확인 "+cost.unknownPlaces()+"곳"
                +"\n최근 수정 · "+updated+" (한국 시간)";
        var features=plan.places().stream().flatMap(p->placePresenter.present(p).badges().stream())
                .map(PlacePresenter.Badge::label).distinct().limit(3).collect(Collectors.joining(" · "));
        if(!features.isBlank())details+="\n특징 · "+features;
        var rows=new ArrayList<String>();
        for(int i=0;i<plan.places().size();i++) {
            var place=plan.places().get(i);var presentation=placePresenter.present(place);
            String row=(i+1)+". "+place.name()+" · "+placePresenter.candidateType(place)
                    +"\n"+place.priceText()+" · "+presentation.priceLabel();
            String badges=presentation.badges().stream().map(PlacePresenter.Badge::label).limit(2).collect(Collectors.joining(" · "));
            if(!badges.isBlank())row+=" · "+badges;
            if(!place.details().address().isBlank())row+="\n"+place.details().address();
            rows.add(row);
        }
        return new Summary(plan.places().size()+"곳 · "+plan.people()+"명 · "+cost.display(),details,rows);
    }
}
