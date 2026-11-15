---
title: SavedCourseRepositoryTest
source: src/test/java/kr/ac/university/localdb/db/SavedCourseRepositoryTest.java
source_commit: ea2242b
source_sha256: 0a62987cc3bddb12a12eb13236eebce4d4ea68dfd53a71db898418094b6604d0
readability_updated: 2026-11-14
---

# SavedCourseRepositoryTest

`ea2242b`의 자동 검사예요. 실제 MySQL·외부 지도 API·Windows 포커스의 수동 확인과 구분해요. 테스트 DB와 모의 지도를 사용해 실패·취소·오래된 응답·상태 보존을 재현해요.

## 검사 이름으로 읽기

- `createsSnapshotWithGeneratedIdOrderedPlacesAndMissingRadius()`
- `updatesSameIdReplacesOrderAndKeepsOriginalCreationTime()`
- `savesAndResavesAnEmptyCourseWithoutInventingPlaces()`
- `rejectsForeignAndMissingCourseWithoutChangingOrCreatingAnything()`
- `failedPlaceBatchRollsBackNewCourseAndPoolCanBeUsedAgain()`
- `failedReplacementRestoresCourseMetadataAndOriginalPlaceRows()`
- `invalidPlansAndRangesAreRejectedBeforeAnyCourseIsStored()`
- `updatedTimestampIsReadFromDatabaseAndRefreshesAfterRename()`
- `readsOnlyOwnerCoursesIncludingEmptyAndKeepsSnapshotOrderOnUpdate()`
- `renameAndDeleteRequireOwnershipAndDeletionRemovesPlaceSnapshots()`
- `unknownRangeFreeAndPublicProvenanceRoundTripWithoutZeroDefaults()`
- `oldUnverifiedZeroSnapshotDoesNotRestoreAsConfirmedFree()`

[검사 코드](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/test/java/kr/ac/university/localdb/db/SavedCourseRepositoryTest.java) · [[현재_구현_흐름|현재 전체 흐름]]
