package com.datecourse.model;

public record Place (
    long recordId,
    String placeId,
    String name,
    String category,
    double lat,
    double lng,
    int price,
    double rating
){}
