---
aliases:
  - "03_처리/CoursePreviewImage"
document_status: archived
archived_at: 2026-11-26
title: "보관 · CoursePreviewImage"
source: src/main/java/com/datecourse/service/CoursePreviewImage.java
source_commit: b69d00e
source_sha256: 072b2e035c5df405b44c7e7d30e5c5da0b6f313ad8b4cb42c3f2458023cced08
readability_updated: 2026-11-15
---

> **보관 문서 · 현재 구현 안내가 아닙니다.** 현재 소스와 다른 버전의 코드 발췌입니다. 원래 source_commit·source_sha256과 발췌는 당시 근거로 보존합니다. 현재 설명은 [[기능별_구현/목차|기능별 구현 문서]], 보관 사유는 [[99_이전_문서/목차|이전 문서 목록]]에서 확인하세요.


## 하는 일

실제 지도 캡처에 코스 요약과 출처 안내를 합성해 저장 PNG를 만들어요.

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `renderWithMap()` | 준비된 SDK 지도 이미지와 코스 요약을 PNG로 만들어요. |
| `fingerprint()` | 미리보기가 어떤 코스 내용으로 생성됐는지 구분해요. |

## 함께 확인할 점

저장 이미지는 직선 연결이에요. 도보 경로를 정확하게 나타내는 자료로 쓰지 않아요.

[[현재_구현_흐름|사용 흐름]] · [[99_이전_문서/이전_안내/최종_검토_2026_11_15|검토 범위]]
