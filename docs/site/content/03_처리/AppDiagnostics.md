---
title: AppDiagnostics
source: src/main/java/com/datecourse/service/AppDiagnostics.java
source_commit: b69d00e
source_sha256: 321d0cad40325fae7bccb76b992a16b967938930a3f07cbb1352ad6b438017df
readability_updated: 2026-11-15
---

## 하는 일

화면에는 행동할 수 있는 안내를 보여주고 콘솔에는 작업·단계·결과를 간략히 남겨요.

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `begin()` | 작업 단위의 진단 기록을 시작해요. |
| `failure()` | 실패한 작업과 단계를 안전한 형태로 기록해요. |
| `root()` | 중첩된 예외에서 실제 원인을 찾아요. |

## 함께 확인할 점

비밀번호·키·쿼리·URL·원문 예외 메시지를 진단 로그에 그대로 출력하지 않아요.

[[현재_구현_흐름|사용 흐름]] · [[최종_검토_2026_11_15|검토 범위]]
