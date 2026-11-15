---
title: InformationReportRepository
source: src/main/java/kr/ac/university/localdb/db/InformationReportRepository.java
source_commit: b69d00e
source_sha256: 25a94701284327ca9c9278b94cc968a4dead547faadb12ddb4ac7c31ca556e2c
readability_updated: 2026-11-15
---

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

[[현재_구현_흐름|사용 흐름]] · [[최종_검토_2026_11_15|검토 범위]]
