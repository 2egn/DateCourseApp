---
aliases:
  - "05_회원_DB/InformationReportRepository"
document_status: archived
archived_at: 2026-11-26
title: "보관 · InformationReportRepository"
source: src/main/java/kr/ac/university/localdb/db/InformationReportRepository.java
current_source: src/main/java/com/datecourse/db/InformationReportRepository.java
source_commit: b69d00e
source_sha256: 25a94701284327ca9c9278b94cc968a4dead547faadb12ddb4ac7c31ca556e2c
readability_updated: 2026-11-15
---

> **보관 문서 · 현재 구현 안내가 아닙니다.** 현재 소스와 다른 버전의 코드 발췌입니다. 원래 source_commit·source_sha256과 발췌는 당시 근거로 보존합니다. 현재 설명은 [[기능별_구현/목차|기능별 구현 문서]], 보관 사유는 [[99_이전_문서/목차|이전 문서 목록]]에서 확인하세요.


> **현재 변경 · 2026-11-24:** 아래 과거 발췌의 자동 생성 동작은 현재와 달라요. 기존 `initialize()` 계열은 구조 확인만 하며 생성·변경은 `maintenance.DatabaseSetup`으로 모았어요. [[05_회원_DB/DB_준비와_일반_실행_분리_2026_11_24|현재 준비 방법과 보존 범위]]를 확인하세요.

## 하는 일

제보 접수와 검토, 승인된 정정을 앱 장소 목록에 적용해요.

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `submit()` | 현재 장소·필드·제안·이유를 제보로 저장해요. |
| `reviewContext()` | 제보와 현재 승인된 정정을 함께 읽어요. |
| `review()` | 미처리 상태와 최신 정정을 트랜잭션에서 확인한 뒤 승인·반려해요. |
| `apply()` | 새로 읽은 장소 목록에 승인된 값을 적용해요. |

## 함께 확인할 점

이미 저장한 코스 스냅샷을 소급 변경하지 않아요. 이름·주소 정정은 외부 장소 동일성 연결 재검토로 이어져요.

[[현재_구현_흐름|사용 흐름]] · [[99_이전_문서/이전_안내/최종_검토_2026_11_15|검토 범위]]
