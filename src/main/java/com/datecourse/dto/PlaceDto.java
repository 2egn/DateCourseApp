package com.datecourse.dto;

public class PlaceDto {
    private String id;
    private String name;
    private String category;
    private double lat;
    private double lon;
    private int price;
    private double rating;

    public PlaceDto(String id, String name, String category, double lat, double lon, int price, double rating) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.lat = lat;
        this.lon = lon;
        this.price = price;
        this.rating = rating;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getLat() { return lat; }
    public double getLon() { return lon; }
    public int getPrice() { return price; }
    public double getRating() { return rating; }
}