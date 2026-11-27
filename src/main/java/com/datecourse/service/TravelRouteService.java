package com.datecourse.service;

import com.datecourse.model.*;
import com.datecourse.model.TravelRoute.Stop;
import java.util.List;

/**
 * - 선택한 이동수단을 실제 경로 조회 서비스에 연결하는 클래스
 * - 화면은 수단만 전달, 제공사별 요청과 응답 처리는 각 서비스에서 담당
 */
public final class TravelRouteService {
    private final WalkingRouteService walking=new WalkingRouteService();
    private final KakaoRouteService kakao=new KakaoRouteService();
    /**
     * 화면에서 선택한 이동수단을 제공사별 경로 조회로 연결하는 공통 진입점
     * 도보는 TMAP, 자동차는 카카오, 혼합은 두 제공사의 구간 조회 함수를 TransitWalkingRouteService에 전달
     * 설정은 요청 시 불러오고, 실제 경로 응답 검증·변환은 각 제공사 서비스에서 담당
     */
    public TravelRoute route(List<Stop> stops,TravelMode mode)throws Exception {
        if(mode==TravelMode.WALKING)return walking.route(stops,TmapSettings.load());
        var kakaoSettings=KakaoRouteSettings.load();
        if(mode==TravelMode.DRIVING)return kakao.route(stops,mode,kakaoSettings);
        var walkSettings=TmapSettings.load();
        return new TransitWalkingRouteService(
                (from,to)->walking.route(List.of(from,to),walkSettings).legs().getFirst(),
                (from,to)->kakao.route(List.of(from,to),TravelMode.TRANSIT,kakaoSettings).legs().getFirst()).route(stops);
    }
}
