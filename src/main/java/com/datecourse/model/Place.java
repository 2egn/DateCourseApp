package com.datecourse.model;

/**
 * - 추천, 지도 및 코스 저장에서 공통으로 사용하는 장소 모델
 * - 장소 식별값, 분류, 좌표, 가격 및 상세정보를 한 단위로 전달
 */
public record Place (
    long recordId,
    String placeId,
    String name,
    String category,
    double lat,
    double lng,
    PriceEstimate priceEstimate,
    double rating,
    PlaceDetails details
){
    /** 기존 시연 코드의 1인 정액 가격 전달용 생성자. 명시된 0원도 실제 값으로 처리 */
    public Place(long recordId, String placeId, String name, String category, double lat, double lng,
                 int price, double rating) {
        this(recordId, placeId, name, category, lat, lng, PriceEstimate.exact(price), rating, PlaceDetails.EMPTY);
    }

    public Place {
        if (priceEstimate == null) throw new IllegalArgumentException("가격 상태가 필요합니다.");
        if (details == null) details = PlaceDetails.EMPTY;
    }

    /** 시연 번호, DB 번호 또는 정정값이 달라도 유지할 공공 ID 우선 식별값 구성 */
    public String stableKey() {
        String publicId = details.externalIds().get("public");
        return publicId != null && !publicId.isBlank() ? publicId
                : placeId != null && !placeId.isBlank() ? placeId : "record:" + recordId;
    }
    // 장소 ID와 기존 자료의 호환 조건으로 같은 장소인지 확인
    public boolean sameIdentityAs(Place other) {
        if (other == null) return false;
        if (stableKey().equals(other.stableKey())) return true;
        // Distinct public source IDs must not be merged just because names and coordinates coincide.
        if (hasPublicIdentity() && other.hasPublicIdentity()) return false;
        return name != null && !name.isBlank() && name.equals(other.name) && lat == other.lat && lng == other.lng;
    }

    public boolean hasPublicIdentity() {
        String id = details.externalIds().get("public");
        return id != null && !id.isBlank();
    }

    public int price() { return priceEstimate.exactAmount(); }

    @com.fasterxml.jackson.annotation.JsonProperty(value = "priceText", access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public String priceText() { return priceEstimate.display(); }
}
