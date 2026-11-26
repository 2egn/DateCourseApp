package com.datecourse.service;

import com.datecourse.model.Place;
import java.io.IOException;
import java.util.List;

/**
 * - 공공 장소 ID와 명시적인 추정 가격을 사용하는 시연 자료 조회용 클래스
 * - 기존 9곳 파일럿 스냅샷과 별도 자료로 관리
 */
public final class PublicDemoPlaceRepository implements PlaceSource {
    public static final String RESOURCE="/datasets/seomyeon-public-estimates.json";
    public static final String FACTS_RESOURCE="/datasets/appeal/demo_appeal_facts.json";
    @Override public List<Place> findAll() throws IOException {
        try(var input=getClass().getResourceAsStream(RESOURCE);
            var facts=getClass().getResourceAsStream(FACTS_RESOURCE)) {
            if(input==null)throw new IOException("공공자료·추정 목록을 찾지 못했어요.");
            if(facts==null)throw new IOException("공공 속성 자료를 찾지 못했어요.");
            return PublicPlaceFacts.read(input.readAllBytes(),facts);
        }
    }
}
