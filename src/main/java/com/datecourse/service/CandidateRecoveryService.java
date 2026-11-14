package com.datecourse.service;

import com.datecourse.model.*;
import java.util.*;

/** Offers only single-condition changes that actually produce eligible candidates. Never mutates filters. */
public final class CandidateRecoveryService {
    public enum Change { INCLUDE_UNKNOWN, CLEAR_FACETS, EXPAND_RADIUS, REVIEW_PRICE }
    public record Suggestion(Change change, int matches) {}
    public record Advice(String message, List<Suggestion> suggestions) {
        public Advice { suggestions=List.copyOf(suggestions); }
    }
    private final NextPlaceService search=new NextPlaceService();
    public Advice analyze(List<Place> places, RoutePlan route, NextPlaceCondition condition, int maximumRadius) {
        if (!matches(places,route,condition).isEmpty()) return new Advice("",List.of());
        int radius=Math.max(condition.radius(),Math.min(2000,maximumRadius));
        var suggestions=new ArrayList<Suggestion>();
        if(!condition.includeUnknown()) offer(suggestions,Change.INCLUDE_UNKNOWN,places,route,
                copy(condition,condition.maxPrice(),condition.radius(),true,condition.subtypes(),condition.tags()));
        if(!condition.subtypes().isEmpty() || !condition.tags().isEmpty()) offer(suggestions,Change.CLEAR_FACETS,places,route,
                copy(condition,condition.maxPrice(),condition.radius(),condition.includeUnknown(),Set.of(),Set.of()));
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
        else if(matches(places,route,copy(condition,100000000,radius,true,Set.of(),Set.of())).isEmpty())
            message="전체 범위 안에 추가할 수 있는 '"+condition.activity()+"' 장소가 없어요. 활동을 바꾸거나 공통 조건의 전체 범위를 확인해 주세요.";
        else message="여러 조건에 함께 걸려 있어요. 활동·가격·반경·세부 조건을 함께 확인해 주세요.";
        return new Advice(message,suggestions);
    }
    private void offer(List<Suggestion> result,Change change,List<Place> places,RoutePlan route,NextPlaceCondition condition) {
        int count=matches(places,route,condition).size();if(count>0)result.add(new Suggestion(change,count));
    }
    private List<PlaceSearchService.NearbyPlace> matches(List<Place> places,RoutePlan route,NextPlaceCondition condition) {
        return search.matching(places,route.places(),route.startLat(),route.startLng(),route.people(),route.radius(),condition);
    }
    private NextPlaceCondition copy(NextPlaceCondition c,int price,int radius,boolean unknown,Set<String> subtypes,Set<PlaceTag> tags) {
        return new NextPlaceCondition(c.activity(),price,radius,unknown,subtypes,tags,c.displayLimit());
    }
}
