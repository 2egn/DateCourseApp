package com.datecourse.model;

/**
 * - 가격 자료의 근거 종류를 선택하는 필터
 * - 참고값이나 추정값 구분용이며 현재 메뉴 가격의 검증 여부와는 별도
 */
public enum PlacePriceFilter {
    ANY("제한 없음"), REFERENCE("참고 가격만"), ESTIMATED("추정 가격만"), FREE("무료만");
    private final String label;
    PlacePriceFilter(String label){this.label=label;}
    public boolean accepts(PriceEstimate price){return switch(this){
        case ANY->true;
        case REFERENCE->price.finite() && !price.estimated() && price.evidence()!=PriceEstimate.Evidence.UNKNOWN;
        case ESTIMATED->price.finite() && price.estimated();
        case FREE->price.finite() && !price.estimated() && price.evidence()!=PriceEstimate.Evidence.UNKNOWN && price.maximum()==0;
    };}
    @Override public String toString(){return label;}
}
