package com.datecourse;

import java.util.List;

// 위치/반경/카테고리에 해당하는 가게 수를 알려주는 곳
// TODO: 백엔드 "가게 수 조회" API 호출로 교체 (이 메서드 안만 바꾸면 됨)
public class PlaceCountService {

    public int count(double lat, double lng, int radiusMeter, List<String> categories) {
        // 가짜 계산: 반경이 넓을수록, 카테고리를 많이 고를수록 많아짐
        int perCategory = Math.max(1, radiusMeter / 50);
        return categories.isEmpty() ? perCategory * 3 : perCategory * categories.size();
    }
}