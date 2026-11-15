package com.datecourse.demo;

import com.datecourse.service.PlaceBindingCatalog;

/** Offline check of the pilot bindings; no API key, DB or map required. */
public final class PlaceBindingDemo {
    public static void main(String[] args) throws Exception {
        var directory = PlaceBindingCatalog.PILOT_DIRECTORY;
        var catalog = PlaceBindingCatalog.load(directory.resolve("google-bindings-v5.json"), directory.resolve("places.csv"));
        System.out.println("확인된 Google 장소 연결: " + catalog.size() + "곳");
        System.out.println("대모골 연결: " + catalog.googlePlaceId("SBIZ:MA0101202607A0151351").isPresent());
        System.out.println("부가 설명을 구분한 부잣집밀면 연결: " + catalog.googlePlaceId("SBIZ:MA010120220813751543").isPresent());
        System.out.println("방문 제외 박물관 연결: " + catalog.googlePlaceId("STD:15017323:e33f9aadac190db685befd1a").isPresent());
        System.out.println("공식 연락처를 확인한 범천도서관 연결: " + catalog.googlePlaceId("LIB:69").isPresent());
        System.out.println("휴관 중인 부전도서관 연결: " + catalog.googlePlaceId("LIB:21").isPresent());
        System.out.println("기존 앱 CAFE_0464 연결: " + catalog.googlePlaceId("CAFE_0464").isPresent());
    }
}
