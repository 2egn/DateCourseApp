---
aliases:
  - "05_회원_DB/PlaceCatalogRepository"
document_status: archived
archived_at: 2026-11-26
title: "보관 · PlaceCatalogRepository"
source: src/main/java/kr/ac/university/localdb/db/PlaceCatalogRepository.java
current_source: src/main/java/com/datecourse/db/PlaceCatalogRepository.java
source_commit: b69d00e
source_sha256: 3fdfdbb8c706a33225aa592030f16ce435d498443f8bc21c19b3e9e8da6ac1f6
readability_updated: 2026-11-15
---

> **보관 문서 · 현재 구현 안내가 아닙니다.** 현재 소스와 다른 버전의 코드 발췌입니다. 원래 source_commit·source_sha256과 발췌는 당시 근거로 보존합니다. 현재 설명은 [[기능별_구현/목차|기능별 구현 문서]], 보관 사유는 [[99_이전_문서/목차|이전 문서 목록]]에서 확인하세요.


> **현재 변경 · 2026-11-24:** 아래 과거 발췌의 자동 생성 동작은 현재와 달라요. 기존 `initialize()` 계열은 구조 확인만 하며 생성·변경은 `maintenance.DatabaseSetup`으로 모았어요. [[05_회원_DB/DB_준비와_일반_실행_분리_2026_11_24|현재 준비 방법과 보존 범위]]를 확인하세요.

## 하는 일

공공 장소 배치를 검증하고 기존 장소 카탈로그와 동기화해요.

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `prepare()` | 배치와 장소 식별·자료 지문을 검증하고 저장할 내용을 준비해요. |
| `initialize()` | 카탈로그 관리 스키마를 준비해요. |
| `synchronize()` | 같은 출처의 추가·변경·재활성·비활성을 트랜잭션으로 반영해요. |

## 함께 확인할 점

운영 적재는 별도의 명시적 작업이에요. 앱 시작이나 문서 빌드가 기존 장소 목록을 교체하지 않아요.

[[현재_구현_흐름|사용 흐름]] · [[99_이전_문서/이전_안내/최종_검토_2026_11_15|검토 범위]]
