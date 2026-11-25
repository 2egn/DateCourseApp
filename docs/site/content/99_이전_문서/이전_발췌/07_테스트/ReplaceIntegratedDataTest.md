---
aliases:
  - "07_테스트/ReplaceIntegratedDataTest"
document_status: archived
archived_at: 2026-11-26
title: "보관 · ReplaceIntegratedDataTest"
source: src/test/java/kr/ac/university/localdb/ReplaceIntegratedDataTest.java
current_source: src/test/java/com/datecourse/maintenance/ReplaceIntegratedDataTest.java
source_commit: ea2242b
source_sha256: bfb9083517dd43e25992e438b91db5048cb3a42290d02e456c4b4cd1af11d342
readability_updated: 2026-11-14
---

> **보관 문서 · 현재 구현 안내가 아닙니다.** 현재 소스와 다른 버전의 코드 발췌입니다. 원래 source_commit·source_sha256과 발췌는 당시 근거로 보존합니다. 현재 설명은 [[기능별_구현/목차|기능별 구현 문서]], 보관 사유는 [[99_이전_문서/목차|이전 문서 목록]]에서 확인하세요.


# ReplaceIntegratedDataTest

`ea2242b`의 자동 검사예요. 실제 MySQL·외부 지도 API·Windows 포커스의 수동 확인과 구분해요. 테스트 DB와 모의 지도를 사용해 실패·취소·오래된 응답·상태 보존을 재현해요.

## 검사 이름으로 읽기

- `ignoresSchemaSwitchesAndScriptTransactionControl()`
- `preservesDataModificationStatements()`
- `actualDatasetUses25InsertsInsteadOf6139()`
- `executesPartialBatchesBeforeOtherStatementsInOrder()`
- `failingBatchCanRollBackEarlierBatchesAndDeletion()`

[검사 코드](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/test/java/com/datecourse/maintenance/ReplaceIntegratedDataTest.java) · [[현재_구현_흐름|현재 전체 흐름]]
