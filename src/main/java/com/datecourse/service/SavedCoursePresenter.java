package com.datecourse.service;

import com.datecourse.model.*;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * - 저장 코스의 스냅샷을 목록 및 상세 문구로 변환하는 클래스
 * - 외부 API를 호출하거나 없는 이동시간을 만들어 표시하지 않음
 */
public final class SavedCoursePresenter {
    /** 저장 코스 요약 표의 제목·값 전달용 행 */
    public record CourseDetailRow(String label, String value) {}
    /** 저장 장소 표의 순서·이름·가격·주소·이동 정보 전달용 행 */
    public record PlaceInfoRow(String order, String name, String price, String address, String travel) {}
    /** 문자열 재분해 없이 목록·요약 표·장소 표에 전달할 표시값 */
    public record Summary(String overview, List<CourseDetailRow> details, List<PlaceInfoRow> places) {
        public Summary { details=List.copyOf(details); places=List.copyOf(places); }
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
        var details=new ArrayList<CourseDetailRow>();
        details.add(new CourseDetailRow("코스 구성",plan.places().size()+"곳 · "+plan.people()+"명"));
        if(!types.isEmpty())details.add(new CourseDetailRow("카테고리",types));
        details.add(new CourseDetailRow("이동 방법",plan.travelMode().label()));
        details.add(new CourseDetailRow("전체 범위",plan.radius()+"m"));
        details.add(new CourseDetailRow("가격 상태","참고 "+reference+" + 추정 "+cost.estimatedPlaces()+" + 미확인 "+cost.unknownPlaces()));
        var features=plan.places().stream().flatMap(p->placePresenter.present(p).badges().stream())
                .map(PlacePresenter.Badge::label).distinct().limit(3).collect(Collectors.joining(" · "));
        if(!features.isBlank())details.add(new CourseDetailRow("특징",features));
        details.add(new CourseDetailRow("최근 수정",updated));
        var rows=new ArrayList<PlaceInfoRow>();
        for(int i=0;i<plan.places().size();i++) {
            var place=plan.places().get(i);var presentation=placePresenter.present(place);
            String name=place.name()+"("+placePresenter.candidateType(place)+")";
            String price=place.priceText()+"("+presentation.priceLabel();
            String priceBadges=presentation.badges().stream()
                    .filter(b->b.code().equals(PlaceTag.GOOD_PRICE.name())||b.code().equals(PlaceTag.FREE_ADMISSION.name()))
                    .map(PlacePresenter.Badge::label).collect(Collectors.joining(" · "));
            if(!priceBadges.isBlank())price+=" · "+priceBadges;
            price+=")";
            String address="주소 정보 없음";
            if(!place.details().address().isBlank()) {
                String proximity=presentation.badges().stream()
                        .filter(b->b.code().equals(PlaceTag.NEAR_SUBWAY.name())||b.code().equals(PlaceTag.NEAR_BUS.name()))
                        .map(PlacePresenter.Badge::label).collect(Collectors.joining(" · "));
                address=place.details().address()+(proximity.isBlank()?"":"("+proximity+")");
            }
            rows.add(new PlaceInfoRow(Integer.toString(i+1),name,price,address,"조회 전"));
        }
        return new Summary(plan.places().size()+"곳 · "+plan.people()+"명 · "+plan.travelMode().label()+" · "+costSummary(course),details,rows);
    }
}
