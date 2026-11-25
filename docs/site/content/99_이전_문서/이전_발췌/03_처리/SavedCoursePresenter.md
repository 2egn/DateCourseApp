---
aliases:
  - "03_처리/SavedCoursePresenter"
document_status: archived
archived_at: 2026-11-26
title: "보관 · SavedCoursePresenter"
source: src/main/java/com/datecourse/service/SavedCoursePresenter.java
source_commit: b69d00e
source_sha256: 84843ddf2e45b5beb2b578ac331fb3540723548de4b72e7abc62fc7a964808bd
readability_updated: 2026-11-15
---

> **보관 문서 · 현재 구현 안내가 아닙니다.** 현재 소스와 다른 버전의 코드 발췌입니다. 원래 source_commit·source_sha256과 발췌는 당시 근거로 보존합니다. 현재 설명은 [[기능별_구현/목차|기능별 구현 문서]], 보관 사유는 [[99_이전_문서/목차|이전 문서 목록]]에서 확인하세요.


## 하는 일

저장 목록에 보여줄 코스 요약과 방문 장소 설명을 만들어요.

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `present()` | 장소 수·인원·가격·범위와 방문 순서 설명을 묶어요. |

## 함께 확인할 점

이미지와 요약은 보조 표시이고 저장된 RoutePlan이 코스 원본이에요.

[[현재_구현_흐름|사용 흐름]] · [[99_이전_문서/이전_안내/최종_검토_2026_11_15|검토 범위]]
