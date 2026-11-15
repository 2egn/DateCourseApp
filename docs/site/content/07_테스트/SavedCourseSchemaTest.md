---
title: SavedCourseSchemaTest
source: src/test/java/kr/ac/university/localdb/db/SavedCourseSchemaTest.java
source_commit: ea2242b
source_sha256: 8bebf79aba75558bdbc73abdcf7d1e4a32d5ca4fb5626a83f49573130a840d2e
readability_updated: 2026-11-14
---

# SavedCourseSchemaTest

`ea2242b`의 자동 검사예요. 실제 MySQL·외부 지도 API·Windows 포커스의 수동 확인과 구분해요. 테스트 DB와 모의 지도를 사용해 실패·취소·오래된 응답·상태 보존을 재현해요.

## 검사 이름으로 읽기

- `threePlaceSnapshotRestoresOrderMissingRadiusAndPerPersonPricesWithoutSourceData()`
- `schemaRejectsDuplicateOrderDuplicateRecordAndInvalidBasicValues()`
- `priceMigrationPreservesExistingCourseAndMakesUnknownSnapshotsPossible()`
- `emptyCourseAndSameIdUpdatesAreRepresentableAndSchemaCanBeAppliedAgain()`
- `deletingCourseRemovesItsPlacesButMemberCannotBeDeletedWhileCoursesRemain()`

[검사 코드](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/test/java/kr/ac/university/localdb/db/SavedCourseSchemaTest.java) · [[현재_구현_흐름|현재 전체 흐름]]
