package com.datecourse.service;

import com.datecourse.model.RoutePlan;
import java.io.IOException;

/**
 * - 브라우저나 지도 API 없이 코스 연결 이미지를 만드는 인터페이스
 * - 지도 타일과 실제 이동 경로 조회를 요구하지 않는 로컬 이미지 처리용
 */
@FunctionalInterface
public interface CoursePreviewRenderer {
    byte[] render(RoutePlan plan) throws IOException;
}
