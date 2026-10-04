package com.datecourse.data;

import com.datecourse.domain.*;
import java.util.List;

/** 모두 가상 장소이며 가격과 평점은 테스트용이다. */
public final class DemoPlaces {
    private DemoPlaces() {}

    public static List<Place> all() {
        return List.of(
                place("m1", "샘플 한식집", Category.MEAL, 35.1584, 129.0586, 8000, 10000, 4.4, 30),
                place("m2", "샘플 파스타집", Category.MEAL, 35.1554, 129.0606, 10000, 12000, 4.7, 20),
                place("m3", "샘플 분식집", Category.MEAL, 35.1594, 129.0556, 6000, 8000, 4.0, 15),
                place("c1", "샘플 커피집", Category.CAFE, 35.1574, 129.0596, 4000, 6000, 4.5, 25),
                place("c2", "샘플 디저트집", Category.CAFE, 35.1544, 129.0576, 6000, 8000, 4.8, 10),
                place("u1", "샘플 무료 전시", Category.CULTURE, 35.1564, 129.0566, 0, 0, 4.3, 12),
                place("u2", "샘플 유료 전시", Category.CULTURE, 35.1514, 129.0616, 8000, 10000, 4.7, 40),
                place("e1", "샘플 보드게임", Category.ENTERTAINMENT, 35.1554, 129.0596, 5000, 7000, 4.4, 20),
                place("e2", "샘플 오락실", Category.ENTERTAINMENT, 35.1594, 129.0616, 3000, 5000, 4.1, 18));
    }

    private static Place place(String id, String name, Category category, double lat, double lon,
                               long lower, long upper, double rating, int reviews) {
        return new Place(id, name, category, new GeoPoint(lat, lon), lower, upper,
                Place.PriceUnit.PER_PERSON, true, rating, reviews);
    }
}
