package com.datecourse.service;

import com.datecourse.model.*;
import java.util.*;
import java.util.stream.Collectors;

/** Builds display-only values. Does not query providers, mutate places or decide search eligibility. */
public final class PlacePresenter {
    public record Badge(String code, String label, String description) {}
    public record DetailLine(String label, String value, String description) {}
    public record Presentation(String priceLabel, String priceText, String priceSummary,
                               String candidateSummary, String candidateDescription,
                               String informationSummary, List<Badge> badges, List<DetailLine> lines) {
        public Presentation { badges=List.copyOf(badges); lines=List.copyOf(lines); }
    }
    private static final List<PlaceTag> PRIORITY = List.of(PlaceTag.FREE_ADMISSION, PlaceTag.MODEL_RESTAURANT,
            PlaceTag.GOOD_PRICE, PlaceTag.RESERVABLE, PlaceTag.VEGETARIAN_FOOD, PlaceTag.OUTDOOR_SEATING,
            PlaceTag.ALLOWS_DOGS, PlaceTag.PARKING, PlaceTag.WHEELCHAIR_ACCESSIBLE_ENTRANCE,
            PlaceTag.GOOD_FOR_GROUPS, PlaceTag.GOOD_FOR_CHILDREN, PlaceTag.TAKEOUT, PlaceTag.NEAR_SUBWAY, PlaceTag.NEAR_BUS);

    public Presentation present(Place place) {
        Objects.requireNonNull(place);
        var details=place.details(); var price=place.priceEstimate();
        List<Badge> badges=PRIORITY.stream().filter(details::hasTag).map(tag->badge(details,tag)).toList();
        var lines=new ArrayList<DetailLine>();
        line(lines,"주소",details.address(),details.evidenceFor(PlaceField.ADDRESS));
        line(lines,"세부 분류",details.subcategory().replace("전국박물관미술관정보표준데이터","박물관·미술관"),details.evidenceFor(PlaceField.SUBCATEGORY));
        line(lines,"연락처",details.phone(),details.evidenceFor(PlaceField.PHONE));
        String priceLabel=price.estimated()?"추정 가격":price.finite()?"참고 가격":"가격";
        String priceSummary=price.usablePerPerson()?price.explanation()+"이에요.":"가격은 아직 확인되지 않았어요.";
        String evidenceLabel=price.estimated()?"추정 가격":price.finite()?price.evidenceText():"가격 미확인";
        String shortBadges=badges.stream().limit(2).map(Badge::label).collect(Collectors.joining(" · "));
        String candidateSummary=evidenceLabel+(shortBadges.isEmpty()?"":" · "+shortBadges);
        String description=priceSummary+badges.stream().map(b->"\n"+b.label()+" · "+b.description()).collect(Collectors.joining());
        String informationSummary=!details.identityCheckedAt().isBlank() || details.externalLinks().stream().anyMatch(ExternalPlaceLink::usable)
                ?"장소 대조 기록이 있어요. 영업 여부는 방문 전 확인해 주세요."
                :"현재 영업 여부는 확인 전이에요. 방문 전 지도에서 확인해 주세요.";
        return new Presentation(priceLabel,place.priceText(),priceSummary,candidateSummary,description,
                informationSummary,badges,lines);
    }
    private Badge badge(PlaceDetails details,PlaceTag tag) {
        var typed=details.attributeFacts().stream().filter(f->f.feature()==PlaceFeature.from(tag)).findFirst();
        String description;
        if(typed.isPresent()) description=evidenceDescription(typed.get().evidence());
        else description=details.tags().stream().filter(t->t.tag()==tag).findFirst()
                .map(t->Objects.toString(t.basis(),"")+(t.checkedAt()==null||t.checkedAt().isBlank()?"":" · 자료 "+t.checkedAt()))
                .orElse("");
        return new Badge(tag.name(),tag.label(),description);
    }
    private void line(List<DetailLine> lines,String label,String value,SourceEvidence evidence) {
        if(value!=null&&!value.isBlank()) lines.add(new DetailLine(label,value,evidenceDescription(evidence)));
    }
    private String evidenceDescription(SourceEvidence evidence) {
        return evidence.basis()+(evidence.referenceDate().isBlank()?"":" · 자료 "+evidence.referenceDate())
                +(evidence.checkedAt().isBlank()?"":" · "+(evidence.origin()==SourceEvidence.Origin.CALCULATED?"계산 ":"확인 ")+evidence.checkedAt());
    }
    public String datasetSummary(List<Place> places) {
        long estimated=places.stream().filter(p->p.priceEstimate().estimated()).count();
        long unknown=places.stream().filter(p->!p.priceEstimate().finite()).count();
        return places.size()+"곳 · 추정 가격 "+estimated+"곳 · 가격 상한 미확인 "+unknown+"곳";
    }
}
