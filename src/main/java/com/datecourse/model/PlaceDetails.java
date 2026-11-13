package com.datecourse.model;

import java.util.Map;

/** Public-source facts and identifiers. External API price/review payloads do not belong here. */
public record PlaceDetails(String address, String subcategory, String phone, String website,
                           String sourceUrl, String sourceDate, String identityCheckedAt,
                           String verificationUrl, Map<String, String> externalIds) {
    public static final PlaceDetails EMPTY = new PlaceDetails("", "", "", "", "", "", "", "", Map.of());
    public PlaceDetails {
        address = clean(address); subcategory = clean(subcategory); phone = clean(phone); website = clean(website);
        sourceUrl = clean(sourceUrl); sourceDate = clean(sourceDate); identityCheckedAt = clean(identityCheckedAt);
        verificationUrl = clean(verificationUrl);
        externalIds = externalIds == null ? Map.of() : Map.copyOf(externalIds);
    }
    private static String clean(String value) { return value == null ? "" : value; }
}
