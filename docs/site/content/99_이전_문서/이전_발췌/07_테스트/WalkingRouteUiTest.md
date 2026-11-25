---
aliases:
  - "07_테스트/WalkingRouteUiTest"
document_status: archived
archived_at: 2026-11-26
current_source: src/test/java/com/datecourse/page/common/map/WalkingRouteUiTest.java
layout_updated: 2026-11-24
title: "보관 · WalkingRouteUiTest"
source: src/test/java/com/datecourse/controller/WalkingRouteUiTest.java
source_commit: ea2242b
source_sha256: 4dc42b5cee9aa1d63b825a0de2ff4a21e95b2cfa4ea687516b65d8c978e2d9fe
readability_updated: 2026-11-14
---

> **보관 문서 · 현재 구현 안내가 아닙니다.** 현재 소스와 다른 버전의 코드 발췌입니다. 원래 source_commit·source_sha256과 발췌는 당시 근거로 보존합니다. 현재 설명은 [[기능별_구현/목차|기능별 구현 문서]], 보관 사유는 [[99_이전_문서/목차|이전 문서 목록]]에서 확인하세요.


현재 테스트 위치: `src/test/java/com/datecourse/page/common/map/WalkingRouteUiTest.java`. 아래 발췌는 기존 커밋 기준이며, [[09_구조와_검토/페이지_구조와_이름_2026_11_24|페이지·이름 변경]]을 함께 확인하세요.


# WalkingRouteUiTest

`ea2242b`의 자동 검사예요. 실제 MySQL·외부 지도 API·Windows 포커스의 수동 확인과 구분해요. 테스트 DB와 모의 지도를 사용해 실패·취소·오래된 응답·상태 보존을 재현해요.

## 검사 이름으로 읽기

- `automaticWalkingWaitsForMapAndResetCancelsQueuedWorkWithoutZooming()`
- `oldWalkingResponsesCannotReplaceANewerCourseAndCourseChangesClearTheOverlay()`

[검사 코드](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/test/java/com/datecourse/controller/WalkingRouteUiTest.java) · [[현재_구현_흐름|현재 전체 흐름]]
