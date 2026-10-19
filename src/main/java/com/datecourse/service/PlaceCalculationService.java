package com.datecourse.service;

import com.datecourse.dto.PlaceDto;
import java.util.List;
import java.util.stream.Collectors;

public class PlaceCalculationService {

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        double a = Math.pow(Math.sin(Math.toRadians(lat2 - lat1) / 2), 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.pow(Math.sin(Math.toRadians(lon2 - lon1) / 2), 2);
        return 6371.0 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public List<PlaceDto> filterPlacesByRadius(List<PlaceDto> allPlaces, double targetLat, double targetLon, double radiusKm) {
        return allPlaces.stream()
                .filter(p -> radiusKm <= 0 || calculateDistance(targetLat, targetLon, p.getLat(), p.getLon()) <= radiusKm)
                .collect(Collectors.toList());
    }

    public int calculateTotalCost(PlaceDto meal, PlaceDto cafe, PlaceDto culture, int people) {
        if (people <= 0) {
            throw new IllegalArgumentException("인원수는 1명 이상이어야 합니다.");
        }

        int unitPriceSum = meal.getPrice() + cafe.getPrice() + culture.getPrice();
        return unitPriceSum * people;
    }
}