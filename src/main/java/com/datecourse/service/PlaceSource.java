package com.datecourse.service;

import com.datecourse.model.Place;
import java.util.List;

/** 화면에 전달할 장소 목록의 조회 방식을 연결하는 인터페이스 */
@FunctionalInterface
public interface PlaceSource {
    List<Place> findAll() throws Exception;
}
