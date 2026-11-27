package com.datecourse.service;

import com.datecourse.model.*;
import java.util.*;

/**
 * - 검색 결과가 없을 때 한 가지 조건을 바꿔 후보가 생기는지 확인하는 클래스
 * - NextPlaceService로 대안을 재검색, 현재 필터는 유지하고 변경안만 반환
 */
public final class CandidateRecoveryService {
    public enum Change { INCLUDE_UNKNOWN, CLEAR_FACETS, EXPAND_RADIUS, REVIEW_PRICE }
    public record Suggestion(Change change, int matches) {}
    public record Advice(String message, List<Suggestion> suggestions) {
        public Advice { suggestions=List.copyOf(suggestions); }
    }
    private final NextPlaceService search=new NextPlaceService();
    /**
     * 현재 조건에 후보가 없을 때 미확인 가격 포함·세부 조건 해제·반경 확대·가격 완화를 각각 재검색
     * 이전 대안에 다음 완화를 누적하지 않고 원래 조건에서 독립적으로 검증
     * 전체 검색 범위와 확정 장소 제외는 유지, 화면에는 실제 후보가 생긴 대안과 개수만 전달
     */
    public Advice analyze(List<Place> places, RoutePlan route, NextPlaceCondition condition, int maximumRadius) {
        if (!matches(places,route,condition).isEmpty()) return new Advice("",List.of());
        // 이동수단·현재 코스의 반경 상한과 호출부 제한을 함께 적용, 기존 반경은 축소하지 않음
        int radius=Math.max(condition.radius(),Math.min(NextPlaceRadius.maximum(route),maximumRadius));
        var suggestions=new ArrayList<Suggestion>();
        if(!condition.includeUnknown()) offer(suggestions,Change.INCLUDE_UNKNOWN,places,route,
                copy(condition,condition.maxPrice(),condition.radius(),true,condition.subtypes(),condition.tags()));
        if(!condition.subtypes().isEmpty() || !condition.tags().isEmpty() || !condition.detailTypes().isEmpty() || condition.priceFilter()!=PlacePriceFilter.ANY) offer(suggestions,Change.CLEAR_FACETS,places,route,
                new NextPlaceCondition(condition.activity(),condition.maxPrice(),condition.radius(),condition.includeUnknown(),Set.of(),Set.of(),condition.displayLimit()));
        if(radius>condition.radius()) offer(suggestions,Change.EXPAND_RADIUS,places,route,
                copy(condition,condition.maxPrice(),radius,condition.includeUnknown(),condition.subtypes(),condition.tags()));
        if(condition.maxPrice()<100000000) offer(suggestions,Change.REVIEW_PRICE,places,route,
                copy(condition,100000000,condition.radius(),condition.includeUnknown(),condition.subtypes(),condition.tags()));
        String message;
        if(!suggestions.isEmpty()) message=suggestions.stream().map(suggestion->switch(suggestion.change()) {
            case INCLUDE_UNKNOWN->"가격 미확인 장소를 포함하면 "+suggestion.matches()+"곳을 볼 수 있어요.";
            case CLEAR_FACETS->"세부 조건을 해제하면 "+suggestion.matches()+"곳을 볼 수 있어요.";
            case EXPAND_RADIUS->"반경을 "+radius+"m로 넓히면 "+suggestion.matches()+"곳을 볼 수 있어요.";
            case REVIEW_PRICE->"현재 가격 상한 때문에 제외된 장소가 "+suggestion.matches()+"곳 있어요.";
        }).collect(java.util.stream.Collectors.joining("\n"));
        // 단일 완화로 해결되지 않으면 넓힌 조건으로 재확인, 장소 부재와 복합 조건 충돌을 구분
        else if(matches(places,route,new NextPlaceCondition(condition.activity(),100000000,radius,true,Set.of(),Set.of(),condition.displayLimit())).isEmpty())
            message="전체 범위 안에 추가할 수 있는 '"+condition.activity()+"' 장소가 없어요. 활동을 바꾸거나 공통 조건의 전체 범위를 확인해 주세요.";
        else message="여러 조건에 함께 걸려 있어요. 활동·가격·반경·세부 조건을 함께 확인해 주세요.";
        return new Advice(message,suggestions);
    }
    // 바꾼 조건으로 실제 후보가 생기는 경우만 대안에 포함
    private void offer(List<Suggestion> result,Change change,List<Place> places,RoutePlan route,NextPlaceCondition condition) {
        int count=matches(places,route,condition).size();if(count>0)result.add(new Suggestion(change,count));
    }
    // 실제 추천과 같은 matching() 사용, 전체 범위·중복 제외 규칙 유지
    private List<PlaceSearchService.NearbyPlace> matches(List<Place> places,RoutePlan route,NextPlaceCondition condition) {
        return search.matching(places,route.places(),route.startLat(),route.startLng(),route.people(),route.radius(),condition);
    }
    // 변경 대상만 바꾸고 활동·표시 개수·나머지 세부 조건은 유지
    private NextPlaceCondition copy(NextPlaceCondition c,int price,int radius,boolean unknown,Set<String> subtypes,Set<PlaceTag> tags) {
        return new NextPlaceCondition(c.activity(),price,radius,unknown,subtypes,tags,c.displayLimit(),c.detailTypes(),c.priceFilter());
    }
}
