---
title: PlaceBindingCatalogTest
source: src/test/java/com/datecourse/service/PlaceBindingCatalogTest.java
source_commit: ea2242b
source_sha256: 73cd05b79a8c5705dcbef80a08dba2d1e33cc8ff1f0ffe955b46368d3b38b1b7
readability_updated: 2026-11-14
---

# PlaceBindingCatalogTest

`ea2242b`의 자동 검사예요. 실제 MySQL·외부 지도 API·Windows 포커스의 수동 확인과 구분해요. 테스트 DB와 모의 지도를 사용해 실패·취소·오래된 응답·상태 보존을 재현해요.

## 검사 이름으로 읽기

- `resolvesExactSourceIdWithoutNetworkOrNameFallback()`
- `changedDatasetCannotReusePreviouslyVerifiedBindings()`
- `duplicateLocalOrGoogleIdsRejectEntireCatalog()`
- `missingOrMalformedFieldsRejectEntireCatalog()`
- `unknownSchemaOrMissingCatalogCannotBecomeAnEmptySuccess()`
- `realPilotCatalogMatchesDatasetAndExcludesReviewCandidates()`
- `expandedAndRefinedCatalogsPreserveClosedAndUncertainExclusions()`
- `newCatalogRequiresExplicitEligibilityAndCorrectRuleForItsVersion()`
- `officialContactCatalogDependsOnVerifiedCorrectionsAndExcludesClosedLibrary()`

[검사 코드](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/test/java/com/datecourse/service/PlaceBindingCatalogTest.java) · [[현재_구현_흐름|현재 전체 흐름]]
