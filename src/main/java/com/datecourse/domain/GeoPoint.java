package com.datecourse.domain;

public record GeoPoint(double latitude, double longitude) {
    public GeoPoint {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("올바른 위도·경도를 입력하세요.");
        }
    }

    public double distanceMetersTo(GeoPoint other) {
        double lat = Math.toRadians(other.latitude - latitude);
        double lon = Math.toRadians(other.longitude - longitude);
        double a = Math.pow(Math.sin(lat / 2), 2)
                + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(other.latitude))
                * Math.pow(Math.sin(lon / 2), 2);
        return 6_371_000 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, a))));
    }
}
