package com.datecourse.model;

/** 장소 속성의 확인 상태와 출처 근거를 함께 보관하는 모델 */
public record PlaceAttributeFact(PlaceFeature feature, FactState state, SourceEvidence evidence) {
    public PlaceAttributeFact {
        if (feature == null || state == null) throw new IllegalArgumentException("속성과 확인 상태가 필요합니다.");
        if (evidence == null) evidence = SourceEvidence.UNKNOWN;
        if (state != FactState.UNKNOWN && evidence.origin() == SourceEvidence.Origin.UNKNOWN)
            throw new IllegalArgumentException("확인된 속성에는 근거가 필요합니다.");
    }
}
