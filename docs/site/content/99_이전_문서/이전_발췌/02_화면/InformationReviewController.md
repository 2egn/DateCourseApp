---
aliases:
  - "02_화면/InformationReviewController"
document_status: archived
archived_at: 2026-11-26
current_source: src/main/java/com/datecourse/page/review/InformationReviewController.java
layout_updated: 2026-11-24
title: "보관 · InformationReviewController"
source: src/main/java/com/datecourse/controller/InformationReviewController.java
source_commit: b69d00e
source_sha256: 1c53f4088ed4050c65c6871ab46e56c6e94d2a47cf95424f9fdba7347b9966ca
readability_updated: 2026-11-15
---

> **보관 문서 · 현재 구현 안내가 아닙니다.** 현재 소스와 다른 버전의 코드 발췌입니다. 원래 source_commit·source_sha256과 발췌는 당시 근거로 보존합니다. 현재 설명은 [[기능별_구현/목차|기능별 구현 문서]], 보관 사유는 [[99_이전_문서/목차|이전 문서 목록]]에서 확인하세요.


## 현재 이름·경로 · 2026-11-24

`InformationReviewController` → `src/main/java/com/datecourse/page/review/InformationReviewController.java`. [[09_구조와_검토/페이지_구조와_이름_2026_11_24|페이지별 배치]]를 확인하세요. 아래 원본 링크·코드 발췌는 기존 `source_commit`의 기록이며 이름 변경 전 코드예요.


## 하는 일

관리자의 장소 정정 제보 목록과 승인·반려를 연결해요.

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `configure()` | 인증된 AdminSession을 전달받아 검토 목록을 읽어요. |
| `loadDetail()` | 접수 당시 정보와 현재 승인된 정정을 구분해서 읽어요. |
| `review()` | 메모와 확인창을 거쳐 승인 또는 반려해요. |
| `clear()` | 로그아웃 시 이전 계정의 검토 상태를 지워요. |

## 함께 확인할 점

최신 승인값을 읽지 못한 상태에서는 승인할 수 없어요. 처리 중에는 선택과 중복 작업을 잠가요.

[[현재_구현_흐름|사용 흐름]] · [[99_이전_문서/이전_안내/최종_검토_2026_11_15|검토 범위]]
