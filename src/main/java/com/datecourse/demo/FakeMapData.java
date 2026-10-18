package com.datecourse.demo;

import java.util.List;

// 지도 API 연동 전까지 쓰는 가짜 데이터
public class FakeMapData {

    // x, y는 가짜 지도 위의 픽셀 위치
    public record Place(String name, String category, double x, double y, int avgPrice) {}

    public static List<Place> getPlaces() {
        return List.of(
                new Place("할매순대국", "식사", 100, 80, 9000),
                new Place("파스타하우스", "식사", 180, 150, 18000),
                new Place("스시로", "식사", 260, 100, 25000),
                new Place("홍콩반점", "식사", 340, 190, 12000),
                new Place("모닝커피", "카페", 220, 230, 6000),
                new Place("달빛포차", "식사", 420, 120, 20000),
                new Place("시네마극장", "문화", 300, 300, 14000),
                new Place("브런치카페", "카페", 120, 260, 9000),
                new Place("고깃집 불꽃", "식사", 460, 280, 22000),
                new Place("이탈리안 바", "식사", 400, 350, 30000),
                new Place("라멘집", "식사", 70, 350, 11000),
                new Place("전시관", "문화", 500, 60, 10000)
        );
    }
}