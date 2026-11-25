---
aliases:
  - "05_회원_DB/MemberAdminPanel"
document_status: archived
archived_at: 2026-11-26
title: "보관 · MemberAdminPanel"
source: src/main/java/kr/ac/university/localdb/ui/MemberAdminPanel.java
current_source: src/main/java/com/datecourse/page/admin/MemberAdminPanel.java
source_commit: b69d00e
source_sha256: 16567581a228eae0a6f4395b98c679018832bf06163fa94766be9722b50c2a5f
readability_updated: 2026-11-15
---

> **보관 문서 · 현재 구현 안내가 아닙니다.** 현재 소스와 다른 버전의 코드 발췌입니다. 원래 source_commit·source_sha256과 발췌는 당시 근거로 보존합니다. 현재 설명은 [[기능별_구현/목차|기능별 구현 문서]], 보관 사유는 [[99_이전_문서/목차|이전 문서 목록]]에서 확인하세요.


## 하는 일

Swing의 회원 표·선택한 회원 편집·확인창·백그라운드 작업을 한 패널로 묶어요.

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `save()` | 이름·전화·상태의 변경 전후를 확인한 다음 저장해요. |
| `withdraw()` | 확인 후 회원 상태만 탈퇴로 바꿔요. |
| `delete()` | 삭제 제한을 조회하고 대상 확인 후 영구 삭제해요. |
| `confirmLeave()` | 작업 중에는 나가기를 막고, 미저장 변경은 버릴지 확인해요. |

## 함께 확인할 점

정렬된 표의 화면 행을 원본 회원으로 변환해요. 표와 편집란은 하나의 회원 선택을 공유하고 작업 중에는 잠가요.

[[현재_구현_흐름|사용 흐름]] · [[99_이전_문서/이전_안내/최종_검토_2026_11_15|검토 범위]]
