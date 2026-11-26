package com.datecourse.model;

/**
 * - 장소 속성의 가능, 불가능, 미확인 상태 구분용 enum
 * - 정보가 없으면 미확인으로 처리, 불가능으로 단정하지 않음
 */
public enum FactState { AVAILABLE, UNAVAILABLE, UNKNOWN }
