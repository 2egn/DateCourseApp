package com.datecourse.model;

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
    /** Existing demos explicitly provide an exact per-person price, including a confirmed zero. */
    public Place(long recordId, String placeId, String name, String category, double lat, double lng,
                 int price, double rating) {
        this(recordId, placeId, name, category, lat, lng, PriceEstimate.exact(price), rating, PlaceDetails.EMPTY);
    }

    public Place {
        if (priceEstimate == null) throw new IllegalArgumentException("가격 상태가 필요합니다.");
        if (details == null) details = PlaceDetails.EMPTY;
    }

    /** Public identity survives demo/DB identifiers, corrections, and saved snapshots. */
    public String stableKey() {
        String publicId = details.externalIds().get("public");
        return publicId != null && !publicId.isBlank() ? publicId
                : placeId != null && !placeId.isBlank() ? placeId : "record:" + recordId;
    }
    public boolean sameIdentityAs(Place other) {
        if (other == null) return false;
        if (stableKey().equals(other.stableKey())) return true;
        // Distinct public source IDs must not be merged just because names and coordinates coincide.
        if (hasPublicIdentity() && other.hasPublicIdentity()) return false;
        return name != null && name.equals(other.name) && lat == other.lat && lng == other.lng;
    }

    private boolean hasPublicIdentity() {
        String id = details.externalIds().get("public");
        return id != null && !id.isBlank();
    }

    public int price() { return priceEstimate.exactAmount(); }

    @com.fasterxml.jackson.annotation.JsonProperty(value = "priceText", access = com.fasterxml.jackson.annotation.JsonProperty.Access.READ_ONLY)
    public String priceText() { return priceEstimate.display(); }
}
