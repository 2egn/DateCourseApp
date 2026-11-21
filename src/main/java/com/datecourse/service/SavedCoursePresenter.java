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

    public String costSummary(SavedCourse course) {
        var cost = course.plan().costEstimate();
        String amount = cost.placeCount() == 0 ? "예상 비용 없음"
                : cost.unknownPlaces() == cost.placeCount() ? "예상 비용 확인 필요"
                : "예상 비용 " + cost.amountDisplay();
        var priceNotes = new ArrayList<String>();
        if (cost.estimatedPlaces() > 0) priceNotes.add("추정 " + cost.estimatedPlaces() + "곳");
        if (cost.unknownPlaces() > 0) priceNotes.add("가격 미확인 " + cost.unknownPlaces() + "곳");
        if (!priceNotes.isEmpty()) amount += "(" + String.join(" · ", priceNotes) + ")";
        return amount;
    }

    public Summary present(SavedCourse course) {
        var plan=course.plan();var cost=plan.costEstimate();
        var categories=new LinkedHashMap<String,Long>();
        for(var place:plan.places())categories.merge(PlaceSearchService.categoryName(place.category()),1L,Long::sum);
        String types=categories.entrySet().stream().map(e->e.getKey()+" "+e.getValue()).collect(Collectors.joining(" + "));
        String updated=course.updatedAt()==null?"시각 미확인":DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
                .withZone(ZoneId.of("Asia/Seoul")).format(course.updatedAt());
        int reference=cost.placeCount()-cost.unknownPlaces()-cost.estimatedPlaces();
        String details="코스 구성: "+plan.places().size()+"곳 · "+plan.people()+"명"
                +(types.isEmpty()?"":"\n"+types)
                +"\n이동 방법: "+plan.travelMode().label()+"\n전체 범위: "+plan.radius()+"m"
                +"\n가격 상태: 참고 "+reference+" + 추정 "+cost.estimatedPlaces()+" + 미확인 "+cost.unknownPlaces();
        var features=plan.places().stream().flatMap(p->placePresenter.present(p).badges().stream())
                .map(PlacePresenter.Badge::label).distinct().limit(3).collect(Collectors.joining(" · "));
        if(!features.isBlank())details+="\n특징: "+features;
        details+="\n최근 수정: "+updated;
        var rows=new ArrayList<String>();
        for(int i=0;i<plan.places().size();i++) {
            var place=plan.places().get(i);var presentation=placePresenter.present(place);
            String row=(i+1)+". "+place.name()+"("+placePresenter.candidateType(place)+")"
                    +"\n가격: "+place.priceText()+"("+presentation.priceLabel();
            String priceBadges=presentation.badges().stream()
                    .filter(b->b.code().equals(PlaceTag.GOOD_PRICE.name())||b.code().equals(PlaceTag.FREE_ADMISSION.name()))
                    .map(PlacePresenter.Badge::label).collect(Collectors.joining(" · "));
            if(!priceBadges.isBlank())row+=" · "+priceBadges;
            row+=")";
            if(!place.details().address().isBlank()) {
                String proximity=presentation.badges().stream()
                        .filter(b->b.code().equals(PlaceTag.NEAR_SUBWAY.name())||b.code().equals(PlaceTag.NEAR_BUS.name()))
                        .map(PlacePresenter.Badge::label).collect(Collectors.joining(" · "));
                row+="\n주소: "+place.details().address()+(proximity.isBlank()?"":"("+proximity+")");
            }
            rows.add(row);
        }
        return new Summary(plan.places().size()+"곳 · "+plan.people()+"명 · "+costSummary(course),details,rows);
    }
}
