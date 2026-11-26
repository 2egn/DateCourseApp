package com.datecourse.service;

import com.datecourse.model.Place;
import com.datecourse.model.RoutePlan;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Objects;

/**
 * - 장소 모델을 지도 표시용 데이터로 변환하는 연결용 클래스
 * - 화면 표시 값은 영구 저장용 Place 모델에 추가하지 않음
 */
public final class PlaceMapPayload {
    private final ObjectMapper json;
    private final PlacePresenter presenter;
    public PlaceMapPayload(ObjectMapper json,PlacePresenter presenter) {
        this.json=Objects.requireNonNull(json);this.presenter=Objects.requireNonNull(presenter);
    }
    public ObjectNode place(Place place) {
        if(place==null)return null;
        ObjectNode value=json.valueToTree(place);
        value.put("identityKey",place.stableKey());
        value.set("presentation",json.valueToTree(presenter.present(place)));
        return value;
    }
    public ArrayNode places(List<Place> places) {
        ArrayNode array=json.createArrayNode();places.forEach(p->array.add(place(p)));return array;
    }
    public ObjectNode plan(RoutePlan plan) {
        ObjectNode value=json.valueToTree(plan);value.set("places",places(plan.places()));
        value.put("maximumRadius",com.datecourse.model.TravelMode.MAX_RADIUS);
        value.put("maximumLegRadius",com.datecourse.model.TravelMode.MAX_LEG_RADIUS);
        return value;
    }
}
