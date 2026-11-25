# 프로젝트 요소 전체 색인

[전체 길잡이](README.md). 공유 추적 파일 기준. 개인 값·생성물·캐시와 이 참조 묶음 자체를 제외한다. 오래된 기록·보관 파일을 현행 기능으로 읽지 않는다.

| 범위 | 파일 수 |
|---|---:|
| 프로젝트 기본 설정·탐색 안내 | 12 |
| 보관 원본·별도 검사 | 35 |
| 공공 원본·기준본·검증 이력 | 177 |
| SQL 구조·마이그레이션 | 11 |
| 설계·검토·변경 문서 | 57 |
| 팀 사이트 원본·자원·설정 | 179 |
| 팀 사이트 서버·공유 상태 | 15 |
| 사이트 로직 검사 | 13 |
| 사이트 빌드·표시 검사 | 27 |
| 앱 Java | 107 |
| 화면·지도·스타일·공개 설정 예제 | 21 |
| 앱 검사 | 57 |
| 자료·운영 도구·검사 | 32 |

<details><summary>프로젝트 기본 설정·탐색 안내 · 12개</summary>

무엇: 실행·빌드·공유 기준. 사용: Gradle·설정·길잡이. 왜: 환경과 진입점을 일관되게 사용. 어떻게: Wrapper → 설정/의존성 → 실행·검사.

| 파일 | 핵심 역할 |
|---|---|
| [.gitignore](../../.gitignore) | 개인 설정·키·빌드/캐시의 Git 포함 제외 |
| [AGENTS.md](../../AGENTS.md) | 변경·검증·커밋·운영 대상 작업 규칙 |
| [README.md](../../README.md) | 앱 기능·실행·현재 검증 안내 |
| [build.gradle](../../build.gradle) | 의존성·toolchain·자원·실행·검사 작업 |
| [gradle.properties](../../gradle.properties) | JDK 설치 탐색·환경 변수·자동 설치 정책 |
| [gradle/wrapper/gradle-wrapper.jar](../../gradle/wrapper/gradle-wrapper.jar) | 선언된 Gradle 배포를 실행하는 Wrapper |
| [gradle/wrapper/gradle-wrapper.properties](../../gradle/wrapper/gradle-wrapper.properties) | Wrapper 배포 버전·다운로드/저장 위치 |
| [gradlew](../../gradlew) | Unix 계열 Gradle Wrapper 진입점 |
| [gradlew.bat](../../gradlew.bat) | Windows Gradle Wrapper 진입점 |
| [settings.gradle](../../settings.gradle) | 프로젝트 이름·JDK 다운로드 resolver |
| [src/navigate.md](../../src/navigate.md) | 소스 폴더·핵심 책임의 길잡이 |
| [폴더_안내.txt](../../폴더_안내.txt) | 프로젝트 폴더 안내 |

</details>

<details><summary>보관 원본·별도 검사 · 35개</summary>

무엇: 사용하지 않는 이전 코드·구조 이력. 사용: 원본 보관·선택 검사. 왜: 현재 앱과 혼동 없이 이력 유지. 어떻게: 기본 sourceSets 제외·별도 init script.

| 파일 | 핵심 역할 |
|---|---|
| [archive/.gitattributes](../../archive/.gitattributes) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/README.md](../../archive/README.md) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/db-schema/2026-11-24-empty-backups/README.md](../../archive/db-schema/2026-11-24-empty-backups/README.md) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/db-schema/2026-11-24-empty-backups/cleanup-result.json](../../archive/db-schema/2026-11-24-empty-backups/cleanup-result.json) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/db-schema/2026-11-24-empty-backups/plan.json](../../archive/db-schema/2026-11-24-empty-backups/plan.json) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/db-schema/2026-11-24-empty-backups/postflight.json](../../archive/db-schema/2026-11-24-empty-backups/postflight.json) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/db-schema/2026-11-24-empty-backups/preflight.json](../../archive/db-schema/2026-11-24-empty-backups/preflight.json) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/db-schema/2026-11-24-empty-backups/restore.sql](../../archive/db-schema/2026-11-24-empty-backups/restore.sql) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/README.md](../../archive/legacy-ui/README.md) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/inventory.json](../../archive/legacy-ui/inventory.json) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/AdminLauncher.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/AdminLauncher.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/AdminTestApp.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/AdminTestApp.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/CommunityLauncher.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/CommunityLauncher.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/CommunityTestApp.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/CommunityTestApp.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/CourseEditLauncher.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/CourseEditLauncher.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/CourseEditTestApp.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/CourseEditTestApp.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/FakeCourseData.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/FakeCourseData.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/FakeMemberData.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/FakeMemberData.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/FakeReviewData.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/FakeReviewData.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/SavedCourseLauncher.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/SavedCourseLauncher.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/SavedCourseTestApp.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/SavedCourseTestApp.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/demo/courseeditor/RecommendedCourseEditorController.java](../../archive/legacy-ui/src/main/java/com/datecourse/demo/courseeditor/RecommendedCourseEditorController.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/model/Course.java](../../archive/legacy-ui/src/main/java/com/datecourse/model/Course.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/model/CoursePlace.java](../../archive/legacy-ui/src/main/java/com/datecourse/model/CoursePlace.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/model/Review.java](../../archive/legacy-ui/src/main/java/com/datecourse/model/Review.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/page/admin/AdminController.java](../../archive/legacy-ui/src/main/java/com/datecourse/page/admin/AdminController.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/page/community/CommunityController.java](../../archive/legacy-ui/src/main/java/com/datecourse/page/community/CommunityController.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/com/datecourse/service/RecommendedCourseMapper.java](../../archive/legacy-ui/src/main/java/com/datecourse/service/RecommendedCourseMapper.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/java/kr/ac/university/localdb/db/PlaceQueryDemo.java](../../archive/legacy-ui/src/main/java/kr/ac/university/localdb/db/PlaceQueryDemo.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/resources/com/datecourse/demo/courseeditor/recommended-course-editor-view.fxml](../../archive/legacy-ui/src/main/resources/com/datecourse/demo/courseeditor/recommended-course-editor-view.fxml) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/resources/com/datecourse/page/admin/admin-view.fxml](../../archive/legacy-ui/src/main/resources/com/datecourse/page/admin/admin-view.fxml) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/main/resources/com/datecourse/page/community/community-view.fxml](../../archive/legacy-ui/src/main/resources/com/datecourse/page/community/community-view.fxml) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/test/java/com/datecourse/demo/courseeditor/RecommendedCourseEditorUiTest.java](../../archive/legacy-ui/src/test/java/com/datecourse/demo/courseeditor/RecommendedCourseEditorUiTest.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/src/test/java/com/datecourse/service/RecommendedCourseMapperTest.java](../../archive/legacy-ui/src/test/java/com/datecourse/service/RecommendedCourseMapperTest.java) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |
| [archive/legacy-ui/verify.init.gradle](../../archive/legacy-ui/verify.init.gradle) | 보관 원본·과거 검증: 기본 앱 기능과 구분 |

</details>

<details><summary>공공 원본·기준본·검증 이력 · 177개</summary>

무엇: 장소·가격·속성·정책·결과. 사용: CSV/JSON/SQL/명세. 왜: 출처·기존 보존·재현. 어떻게: current 포인터 → 전체 파일 쌍 → 준비 도구 → 검증 기록.

| 파일 | 핵심 역할 |
|---|---|
| [data/README.md](../../data/README.md) | 앱 기능·실행·현재 검증 안내 |
| [data/busan_placedata/outputs/20261110_shop_average_01a1c4b1/부산_가게별_평균가격.csv](../../data/busan_placedata/outputs/20261110_shop_average_01a1c4b1/부산_가게별_평균가격.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/busan_placedata/outputs/20261110_shop_average_01a1c4b1/부산_가게별_평균가격.xlsx](../../data/busan_placedata/outputs/20261110_shop_average_01a1c4b1/부산_가게별_평균가격.xlsx) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/appeal/.gitattributes](../../data/places/appeal/.gitattributes) | 텍스트 줄바꿈 고정·자료 지문 재현 보조 |
| [data/places/appeal/busan-2026-11-14/.gitattributes](../../data/places/appeal/busan-2026-11-14/.gitattributes) | 텍스트 줄바꿈 고정·자료 지문 재현 보조 |
| [data/places/appeal/busan-2026-11-14/README.md](../../data/places/appeal/busan-2026-11-14/README.md) | 앱 기능·실행·현재 검증 안내 |
| [data/places/appeal/busan-2026-11-14/additional_facility_facts.json](../../data/places/appeal/busan-2026-11-14/additional_facility_facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/appeal/busan-2026-11-14/busanjin_model_restaurants.csv](../../data/places/appeal/busan-2026-11-14/busanjin_model_restaurants.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/appeal/busan-2026-11-14/checksums.json](../../data/places/appeal/busan-2026-11-14/checksums.json) | 원본/출력 지문·출처·수집 명세 |
| [data/places/appeal/busan-2026-11-14/demo_appeal_facts.json](../../data/places/appeal/busan-2026-11-14/demo_appeal_facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/appeal/busan-2026-11-14/demo_nearest_transit.csv](../../data/places/appeal/busan-2026-11-14/demo_nearest_transit.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/appeal/busan-2026-11-14/designation_links.csv](../../data/places/appeal/busan-2026-11-14/designation_links.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/appeal/busan-2026-11-14/designation_review_needed.json](../../data/places/appeal/busan-2026-11-14/designation_review_needed.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/appeal/busan-2026-11-14/raw/busan_bus_stops.csv](../../data/places/appeal/busan-2026-11-14/raw/busan_bus_stops.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/appeal/busan-2026-11-14/raw/busan_railway_stations.csv](../../data/places/appeal/busan-2026-11-14/raw/busan_railway_stations.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/appeal/busan-2026-11-14/raw/busanjin_model_restaurants_cp949.csv](../../data/places/appeal/busan-2026-11-14/raw/busanjin_model_restaurants_cp949.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/appeal/busan-2026-11-14/raw/national_railway_original.xlsx](../../data/places/appeal/busan-2026-11-14/raw/national_railway_original.xlsx) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/appeal/busan-2026-11-14/sources.json](../../data/places/appeal/busan-2026-11-14/sources.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/appeal/busan-2026-11-14/summary.json](../../data/places/appeal/busan-2026-11-14/summary.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/appeal/busan-2026-11-14/threshold_comparison.json](../../data/places/appeal/busan-2026-11-14/threshold_comparison.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/appeal/designation-sources.json](../../data/places/appeal/designation-sources.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/appeal/manual-facility-facts.json](../../data/places/appeal/manual-facility-facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/current/integrated_places.csv](../../data/places/current/integrated_places.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/demo/.gitattributes](../../data/places/demo/.gitattributes) | 텍스트 줄바꿈 고정·자료 지문 재현 보조 |
| [data/places/demo/README.md](../../data/places/demo/README.md) | 앱 기능·실행·현재 검증 안내 |
| [data/places/demo/expansion-baseline-2026-11-16.json](../../data/places/demo/expansion-baseline-2026-11-16.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/demo/expansion-report-2026-11-16.json](../../data/places/demo/expansion-report-2026-11-16.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/demo/rehearsal-place-checks-2026-11-14.json](../../data/places/demo/rehearsal-place-checks-2026-11-14.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/demo/rehearsal-place-checks-culture-alternatives-2026-11-14.json](../../data/places/demo/rehearsal-place-checks-culture-alternatives-2026-11-14.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/demo/rehearsal-place-checks-district-recheck-2026-11-14.json](../../data/places/demo/rehearsal-place-checks-district-recheck-2026-11-14.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/demo/selection-policy.json](../../data/places/demo/selection-policy.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/demo/seomyeon-jeonpo-demo.json](../../data/places/demo/seomyeon-jeonpo-demo.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/demo/seomyeon-public-estimates.json](../../data/places/demo/seomyeon-public-estimates.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/.gitattributes](../../data/places/expansion/.gitattributes) | 텍스트 줄바꿈 고정·자료 지문 재현 보조 |
| [data/places/expansion/busan-balanced-2026-11-25/README.md](../../data/places/expansion/busan-balanced-2026-11-25/README.md) | 앱 기능·실행·현재 검증 안내 |
| [data/places/expansion/busan-balanced-2026-11-25/appeal/additional_facility_facts.json](../../data/places/expansion/busan-balanced-2026-11-25/appeal/additional_facility_facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/expansion/busan-balanced-2026-11-25/appeal/busanjin_model_restaurants.csv](../../data/places/expansion/busan-balanced-2026-11-25/appeal/busanjin_model_restaurants.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/busan-balanced-2026-11-25/appeal/checksums.json](../../data/places/expansion/busan-balanced-2026-11-25/appeal/checksums.json) | 원본/출력 지문·출처·수집 명세 |
| [data/places/expansion/busan-balanced-2026-11-25/appeal/demo_appeal_facts.json](../../data/places/expansion/busan-balanced-2026-11-25/appeal/demo_appeal_facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/expansion/busan-balanced-2026-11-25/appeal/demo_nearest_transit.csv](../../data/places/expansion/busan-balanced-2026-11-25/appeal/demo_nearest_transit.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/busan-balanced-2026-11-25/appeal/designation_links.csv](../../data/places/expansion/busan-balanced-2026-11-25/appeal/designation_links.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/busan-balanced-2026-11-25/appeal/designation_review_needed.json](../../data/places/expansion/busan-balanced-2026-11-25/appeal/designation_review_needed.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/busan-balanced-2026-11-25/appeal/sources.json](../../data/places/expansion/busan-balanced-2026-11-25/appeal/sources.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-balanced-2026-11-25/appeal/summary.json](../../data/places/expansion/busan-balanced-2026-11-25/appeal/summary.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-balanced-2026-11-25/appeal/threshold_comparison.json](../../data/places/expansion/busan-balanced-2026-11-25/appeal/threshold_comparison.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-balanced-2026-11-25/baseline.json](../../data/places/expansion/busan-balanced-2026-11-25/baseline.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-balanced-2026-11-25/coverage-report.json](../../data/places/expansion/busan-balanced-2026-11-25/coverage-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/busan-balanced-2026-11-25/import-verification.json](../../data/places/expansion/busan-balanced-2026-11-25/import-verification.json) | 적재·보존·반복 또는 형식 검증 기록 |
| [data/places/expansion/busan-balanced-2026-11-25/places.json](../../data/places/expansion/busan-balanced-2026-11-25/places.json) | 해당 단계의 전체 장소 스냅샷 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/01.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/01.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/02.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/02.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/03.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/03.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/04.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/04.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/05.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/05.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/06.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/06.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/07.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/07.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/08.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/08.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/09.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/09.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/10.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/10.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/11.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/11.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/policies/12.json](../../data/places/expansion/busan-balanced-2026-11-25/policies/12.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-balanced-2026-11-25/preflight-report.json](../../data/places/expansion/busan-balanced-2026-11-25/preflight-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/busan-balanced-2026-11-25/price-review.json](../../data/places/expansion/busan-balanced-2026-11-25/price-review.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/busan-balanced-2026-11-25/rehearsal-report.json](../../data/places/expansion/busan-balanced-2026-11-25/rehearsal-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/busan-balanced-2026-11-25/rehearsal-scenarios.json](../../data/places/expansion/busan-balanced-2026-11-25/rehearsal-scenarios.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-balanced-2026-11-25/spatial-summary.json](../../data/places/expansion/busan-balanced-2026-11-25/spatial-summary.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-balanced-2026-11-25/stage-reports.json](../../data/places/expansion/busan-balanced-2026-11-25/stage-reports.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/busan-core-2026-11-23/01-busanjin-quality.json](../../data/places/expansion/busan-core-2026-11-23/01-busanjin-quality.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-core-2026-11-23/02-nam-quality.json](../../data/places/expansion/busan-core-2026-11-23/02-nam-quality.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-core-2026-11-23/03-haeundae-quality.json](../../data/places/expansion/busan-core-2026-11-23/03-haeundae-quality.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-core-2026-11-23/README.md](../../data/places/expansion/busan-core-2026-11-23/README.md) | 앱 기능·실행·현재 검증 안내 |
| [data/places/expansion/busan-core-2026-11-23/appeal/additional_facility_facts.json](../../data/places/expansion/busan-core-2026-11-23/appeal/additional_facility_facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/expansion/busan-core-2026-11-23/appeal/busanjin_model_restaurants.csv](../../data/places/expansion/busan-core-2026-11-23/appeal/busanjin_model_restaurants.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/busan-core-2026-11-23/appeal/checksums.json](../../data/places/expansion/busan-core-2026-11-23/appeal/checksums.json) | 원본/출력 지문·출처·수집 명세 |
| [data/places/expansion/busan-core-2026-11-23/appeal/demo_appeal_facts.json](../../data/places/expansion/busan-core-2026-11-23/appeal/demo_appeal_facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/expansion/busan-core-2026-11-23/appeal/demo_nearest_transit.csv](../../data/places/expansion/busan-core-2026-11-23/appeal/demo_nearest_transit.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/busan-core-2026-11-23/appeal/designation_links.csv](../../data/places/expansion/busan-core-2026-11-23/appeal/designation_links.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/busan-core-2026-11-23/appeal/designation_review_needed.json](../../data/places/expansion/busan-core-2026-11-23/appeal/designation_review_needed.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/busan-core-2026-11-23/appeal/sources.json](../../data/places/expansion/busan-core-2026-11-23/appeal/sources.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-core-2026-11-23/appeal/summary.json](../../data/places/expansion/busan-core-2026-11-23/appeal/summary.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-core-2026-11-23/appeal/threshold_comparison.json](../../data/places/expansion/busan-core-2026-11-23/appeal/threshold_comparison.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-core-2026-11-23/coverage-report.json](../../data/places/expansion/busan-core-2026-11-23/coverage-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/busan-core-2026-11-23/import-verification.json](../../data/places/expansion/busan-core-2026-11-23/import-verification.json) | 적재·보존·반복 또는 형식 검증 기록 |
| [data/places/expansion/busan-core-2026-11-23/places.json](../../data/places/expansion/busan-core-2026-11-23/places.json) | 해당 단계의 전체 장소 스냅샷 |
| [data/places/expansion/busan-core-2026-11-23/policies/01-busanjin.json](../../data/places/expansion/busan-core-2026-11-23/policies/01-busanjin.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-core-2026-11-23/policies/02-nam.json](../../data/places/expansion/busan-core-2026-11-23/policies/02-nam.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-core-2026-11-23/policies/03-haeundae.json](../../data/places/expansion/busan-core-2026-11-23/policies/03-haeundae.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/busan-core-2026-11-23/quality-report.json](../../data/places/expansion/busan-core-2026-11-23/quality-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/busan-core-2026-11-23/rehearsal-report.json](../../data/places/expansion/busan-core-2026-11-23/rehearsal-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/busan-core-2026-11-23/rehearsal-scenarios.json](../../data/places/expansion/busan-core-2026-11-23/rehearsal-scenarios.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/busan-core-2026-11-23/stage-reports.json](../../data/places/expansion/busan-core-2026-11-23/stage-reports.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/current.json](../../data/places/expansion/current.json) | 현재 운영 장소·속성 파일 쌍과 지문 포인터 |
| [data/places/expansion/jung-gu-2026-11-25/README.md](../../data/places/expansion/jung-gu-2026-11-25/README.md) | 앱 기능·실행·현재 검증 안내 |
| [data/places/expansion/jung-gu-2026-11-25/appeal/additional_facility_facts.json](../../data/places/expansion/jung-gu-2026-11-25/appeal/additional_facility_facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/expansion/jung-gu-2026-11-25/appeal/busanjin_model_restaurants.csv](../../data/places/expansion/jung-gu-2026-11-25/appeal/busanjin_model_restaurants.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/jung-gu-2026-11-25/appeal/checksums.json](../../data/places/expansion/jung-gu-2026-11-25/appeal/checksums.json) | 원본/출력 지문·출처·수집 명세 |
| [data/places/expansion/jung-gu-2026-11-25/appeal/demo_appeal_facts.json](../../data/places/expansion/jung-gu-2026-11-25/appeal/demo_appeal_facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/expansion/jung-gu-2026-11-25/appeal/demo_nearest_transit.csv](../../data/places/expansion/jung-gu-2026-11-25/appeal/demo_nearest_transit.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/jung-gu-2026-11-25/appeal/designation_links.csv](../../data/places/expansion/jung-gu-2026-11-25/appeal/designation_links.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/jung-gu-2026-11-25/appeal/designation_review_needed.json](../../data/places/expansion/jung-gu-2026-11-25/appeal/designation_review_needed.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/jung-gu-2026-11-25/appeal/sources.json](../../data/places/expansion/jung-gu-2026-11-25/appeal/sources.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/jung-gu-2026-11-25/appeal/summary.json](../../data/places/expansion/jung-gu-2026-11-25/appeal/summary.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/jung-gu-2026-11-25/appeal/threshold_comparison.json](../../data/places/expansion/jung-gu-2026-11-25/appeal/threshold_comparison.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/jung-gu-2026-11-25/baseline.json](../../data/places/expansion/jung-gu-2026-11-25/baseline.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/jung-gu-2026-11-25/candidate-summary.json](../../data/places/expansion/jung-gu-2026-11-25/candidate-summary.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/jung-gu-2026-11-25/import-verification.json](../../data/places/expansion/jung-gu-2026-11-25/import-verification.json) | 적재·보존·반복 또는 형식 검증 기록 |
| [data/places/expansion/jung-gu-2026-11-25/places.json](../../data/places/expansion/jung-gu-2026-11-25/places.json) | 해당 단계의 전체 장소 스냅샷 |
| [data/places/expansion/jung-gu-2026-11-25/policy.json](../../data/places/expansion/jung-gu-2026-11-25/policy.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/expansion/jung-gu-2026-11-25/preflight-report.json](../../data/places/expansion/jung-gu-2026-11-25/preflight-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/jung-gu-2026-11-25/price-import-verification.json](../../data/places/expansion/jung-gu-2026-11-25/price-import-verification.json) | 적재·보존·반복 또는 형식 검증 기록 |
| [data/places/expansion/jung-gu-2026-11-25/price-review.json](../../data/places/expansion/jung-gu-2026-11-25/price-review.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/jung-gu-2026-11-25/quality-report.json](../../data/places/expansion/jung-gu-2026-11-25/quality-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/jung-gu-2026-11-25/rehearsal-report.json](../../data/places/expansion/jung-gu-2026-11-25/rehearsal-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/jung-gu-2026-11-25/rehearsal-scenarios.json](../../data/places/expansion/jung-gu-2026-11-25/rehearsal-scenarios.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/suyeong-2026-11-16/appeal/additional_facility_facts.json](../../data/places/expansion/suyeong-2026-11-16/appeal/additional_facility_facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/expansion/suyeong-2026-11-16/appeal/busanjin_model_restaurants.csv](../../data/places/expansion/suyeong-2026-11-16/appeal/busanjin_model_restaurants.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/suyeong-2026-11-16/appeal/checksums.json](../../data/places/expansion/suyeong-2026-11-16/appeal/checksums.json) | 원본/출력 지문·출처·수집 명세 |
| [data/places/expansion/suyeong-2026-11-16/appeal/demo_appeal_facts.json](../../data/places/expansion/suyeong-2026-11-16/appeal/demo_appeal_facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/expansion/suyeong-2026-11-16/appeal/demo_nearest_transit.csv](../../data/places/expansion/suyeong-2026-11-16/appeal/demo_nearest_transit.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/suyeong-2026-11-16/appeal/designation_links.csv](../../data/places/expansion/suyeong-2026-11-16/appeal/designation_links.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/expansion/suyeong-2026-11-16/appeal/designation_review_needed.json](../../data/places/expansion/suyeong-2026-11-16/appeal/designation_review_needed.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/suyeong-2026-11-16/appeal/sources.json](../../data/places/expansion/suyeong-2026-11-16/appeal/sources.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/suyeong-2026-11-16/appeal/summary.json](../../data/places/expansion/suyeong-2026-11-16/appeal/summary.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/suyeong-2026-11-16/appeal/threshold_comparison.json](../../data/places/expansion/suyeong-2026-11-16/appeal/threshold_comparison.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/expansion/suyeong-2026-11-16/import-verification.json](../../data/places/expansion/suyeong-2026-11-16/import-verification.json) | 적재·보존·반복 또는 형식 검증 기록 |
| [data/places/expansion/suyeong-2026-11-16/places.json](../../data/places/expansion/suyeong-2026-11-16/places.json) | 해당 단계의 전체 장소 스냅샷 |
| [data/places/expansion/suyeong-2026-11-16/quality-report.json](../../data/places/expansion/suyeong-2026-11-16/quality-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/suyeong-2026-11-16/rehearsal-report.json](../../data/places/expansion/suyeong-2026-11-16/rehearsal-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/expansion/suyeong-policy.json](../../data/places/expansion/suyeong-policy.json) | 자료 선택 범위·개수·분류 정책 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/README.md](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/README.md) | 앱 기능·실행·현재 검증 안내 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/binding-review-v3.csv](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/binding-review-v3.csv) | 품질·가격·모의 결과·검토 기록 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/binding-review-v5.csv](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/binding-review-v5.csv) | 품질·가격·모의 결과·검토 기록 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/binding-sample-v2.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/binding-sample-v2.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/contact-source-overrides.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/contact-source-overrides.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/google-binding-evaluation-v2.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/google-binding-evaluation-v2.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/google-binding-refinement-v3.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/google-binding-refinement-v3.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/google-binding-review.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/google-binding-review.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/google-bindings-v2.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/google-bindings-v2.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/google-bindings-v3.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/google-bindings-v3.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/google-bindings-v4.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/google-bindings-v4.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/google-bindings-v5.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/google-bindings-v5.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/google-bindings.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/google-bindings.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/google-contact-evaluation-v4.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/google-contact-evaluation-v4.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/google-contact-reconciled-v5.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/google-contact-reconciled-v5.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/manifest.json](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/manifest.json) | 원본/출력 지문·출처·수집 명세 |
| [data/places/pilot/seomyeon-jeonpo-2026-11-13/places.csv](../../data/places/pilot/seomyeon-jeonpo-2026-11-13/places.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/pricing/.gitattributes](../../data/places/pricing/.gitattributes) | 텍스트 줄바꿈 고정·자료 지문 재현 보조 |
| [data/places/pricing/2026-11-24/README.md](../../data/places/pricing/2026-11-24/README.md) | 앱 기능·실행·현재 검증 안내 |
| [data/places/pricing/2026-11-24/import-verification.json](../../data/places/pricing/2026-11-24/import-verification.json) | 적재·보존·반복 또는 형식 검증 기록 |
| [data/places/pricing/2026-11-24/postflight.json](../../data/places/pricing/2026-11-24/postflight.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pricing/2026-11-24/preflight.json](../../data/places/pricing/2026-11-24/preflight.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pricing/2026-11-24/regional-raw.json](../../data/places/pricing/2026-11-24/regional-raw.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pricing/2026-11-24/rehearsal-report.json](../../data/places/pricing/2026-11-24/rehearsal-report.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/pricing/2026-11-24/sources.json](../../data/places/pricing/2026-11-24/sources.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pricing/2026-11-24/staged/appeal/demo_appeal_facts.json](../../data/places/pricing/2026-11-24/staged/appeal/demo_appeal_facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/pricing/2026-11-24/staged/places.json](../../data/places/pricing/2026-11-24/staged/places.json) | 해당 단계의 전체 장소 스냅샷 |
| [data/places/pricing/2026-11-24/staged/price-review.json](../../data/places/pricing/2026-11-24/staged/price-review.json) | 품질·가격·모의 결과·검토 기록 |
| [data/places/pricing/clock-correction-2026-11-24/README.md](../../data/places/pricing/clock-correction-2026-11-24/README.md) | 앱 기능·실행·현재 검증 안내 |
| [data/places/pricing/clock-correction-2026-11-24/correction-result.json](../../data/places/pricing/clock-correction-2026-11-24/correction-result.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pricing/clock-correction-2026-11-24/facts.json](../../data/places/pricing/clock-correction-2026-11-24/facts.json) | 장소 속성·시설·근거 자료 |
| [data/places/pricing/clock-correction-2026-11-24/postflight.json](../../data/places/pricing/clock-correction-2026-11-24/postflight.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pricing/clock-correction-2026-11-24/preflight.json](../../data/places/pricing/clock-correction-2026-11-24/preflight.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/pricing/clock-correction-2026-11-24/previous-current.json](../../data/places/pricing/clock-correction-2026-11-24/previous-current.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/public/busan/.gitattributes](../../data/places/public/busan/.gitattributes) | 텍스트 줄바꿈 고정·자료 지문 재현 보조 |
| [data/places/public/busan/README.md](../../data/places/public/busan/README.md) | 앱 기능·실행·현재 검증 안내 |
| [data/places/public/busan/busan_data_overview.xlsx](../../data/places/public/busan/busan_data_overview.xlsx) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/public/busan/busan_good_price_shops.csv](../../data/places/public/busan/busan_good_price_shops.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/public/busan/busan_menu_prices.csv](../../data/places/public/busan/busan_menu_prices.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/public/busan/busan_places.csv](../../data/places/public/busan/busan_places.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/public/busan/import_busan_public.sql](../../data/places/public/busan/import_busan_public.sql) | 자료 적재용 SQL 원본; 운영에 자동 재실행하지 않음 |
| [data/places/public/busan/manifest.json](../../data/places/public/busan/manifest.json) | 원본/출력 지문·출처·수집 명세 |
| [data/places/public/busan/possible_duplicate_places.json](../../data/places/public/busan/possible_duplicate_places.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/public/busan/raw/busan_good_price_source.json](../../data/places/public/busan/raw/busan_good_price_source.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/public/busan/raw/busan_libraries_source.csv](../../data/places/public/busan/raw/busan_libraries_source.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/public/busan/raw/busan_standard_sources.json](../../data/places/public/busan/raw/busan_standard_sources.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [data/places/public/busan/raw/busan_stores_source.csv](../../data/places/public/busan/raw/busan_stores_source.csv) | 공공 원본·정규화·표본·연결표; 폴더 명세로 구분 |
| [data/places/public/busan/validation_report.json](../../data/places/public/busan/validation_report.json) | 품질·가격·모의 결과·검토 기록 |

</details>

<details><summary>SQL 구조·마이그레이션 · 11개</summary>

무엇: 영속 구조 정의/변경. 사용: MySQL SQL. 왜: 저장 관계·제약 보장. 어떻게: 관리자 준비와 단계별 마이그레이션, 일반 실행과 분리.

| 파일 | 핵심 역할 |
|---|---|
| [data/sql/01_create_database.sql](../../data/sql/01_create_database.sql) | 기본 DB 생성 |
| [data/sql/02_create_tables.sql](../../data/sql/02_create_tables.sql) | 이전 일반 데이터·분류 구조 |
| [data/sql/05_replace_integrated_data.sql](../../data/sql/05_replace_integrated_data.sql) | 이전 일반 데이터 교체 |
| [data/sql/06_membership.sql](../../data/sql/06_membership.sql) | 회원·관리자 구조 |
| [data/sql/07_saved_courses.sql](../../data/sql/07_saved_courses.sql) | 코스·방문 장소 구조 |
| [data/sql/08_saved_course_price_estimates.sql](../../data/sql/08_saved_course_price_estimates.sql) | 가격 범위·근거 스냅샷 확장 |
| [data/sql/09_place_information_reports.sql](../../data/sql/09_place_information_reports.sql) | 제보·승인 정정 구조 |
| [data/sql/10_place_catalog.sql](../../data/sql/10_place_catalog.sql) | 카탈로그·출처·적재·이력·잠금 구조 |
| [data/sql/11_saved_course_previews.sql](../../data/sql/11_saved_course_previews.sql) | 내용 지문·PNG 미리보기 구조 |
| [data/sql/12_saved_course_transport.sql](../../data/sql/12_saved_course_transport.sql) | 저장 코스 이동수단 구조 |
| [data/sql/13_expand_course_ranges.sql](../../data/sql/13_expand_course_ranges.sql) | 기존 반경 제약 확장 |

</details>

<details><summary>설계·검토·변경 문서 · 57개</summary>

무엇: 구현 근거·설계·당시 검토. 사용: 문서·지문·검증 기록. 왜: 현재 구현과 제안을 구분. 어떻게: 기준 버전·날짜·연결 소스로 확인.

| 파일 | 핵심 역할 |
|---|---|
| [docs/README.md](../../docs/README.md) | 앱 기능·실행·현재 검증 안내 |
| [docs/admin-management.md](../../docs/admin-management.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/architecture-review-2026-11-16.md](../../docs/architecture-review-2026-11-16.md) | 품질·가격·모의 결과·검토 기록 |
| [docs/architecture.md](../../docs/architecture.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/archive/README.md](../../docs/archive/README.md) | 앱 기능·실행·현재 검증 안내 |
| [docs/archive/README_cafe_only.md](../../docs/archive/README_cafe_only.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/archive/pom.xml](../../docs/archive/pom.xml) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/archive/수정사항_요약.txt](../../docs/archive/수정사항_요약.txt) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/busan-core-expansion-2026-11-23.md](../../docs/busan-core-expansion-2026-11-23.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/camera-motion-demo-2026-11-15.md](../../docs/camera-motion-demo-2026-11-15.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/dataset-expansion-2026-11-16.md](../../docs/dataset-expansion-2026-11-16.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/district-expansion-2026-11-16.md](../../docs/district-expansion-2026-11-16.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/expanded-place-filters-2026-11-15.md](../../docs/expanded-place-filters-2026-11-15.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/filter-ui-2026-11-15.md](../../docs/filter-ui-2026-11-15.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/kakao-routing-2026-11-16.md](../../docs/kakao-routing-2026-11-16.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/map-camera-motion.md](../../docs/map-camera-motion.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/master-merge-2026-11-15.md](../../docs/master-merge-2026-11-15.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/pin-input-state-2026-11-15.md](../../docs/pin-input-state-2026-11-15.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/place-catalog-import-2026-11-15.md](../../docs/place-catalog-import-2026-11-15.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/place-data-model-2026-11-15.md](../../docs/place-data-model-2026-11-15.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/place-presentation-2026-11-15.md](../../docs/place-presentation-2026-11-15.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/release-readiness-2026-11-25.md](../../docs/release-readiness-2026-11-25.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/releases/checkpoint-2026-11-17.json](../../docs/releases/checkpoint-2026-11-17.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/releases/checkpoint-2026-11-17.md](../../docs/releases/checkpoint-2026-11-17.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/dataset-self-audit-2026-11-16.md](../../docs/research/dataset-self-audit-2026-11-16.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/demo-price-verification-2026-11-13.md](../../docs/research/demo-price-verification-2026-11-13.md) | 적재·보존·반복 또는 형식 검증 기록 |
| [docs/research/editor-travel-time-plan-2026-11-14.md](../../docs/research/editor-travel-time-plan-2026-11-14.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/hardcoding-review-2026-11-15.md](../../docs/research/hardcoding-review-2026-11-15.md) | 품질·가격·모의 결과·검토 기록 |
| [docs/research/map-camera-motion-review-2026-11-15.md](../../docs/research/map-camera-motion-review-2026-11-15.md) | 품질·가격·모의 결과·검토 기록 |
| [docs/research/place-binding-2026-11-13.md](../../docs/research/place-binding-2026-11-13.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/place-binding-contact-results-2026-11-13.md](../../docs/research/place-binding-contact-results-2026-11-13.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/place-binding-evaluation-2026-11-13.md](../../docs/research/place-binding-evaluation-2026-11-13.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/place-binding-signals-2026-11-13.md](../../docs/research/place-binding-signals-2026-11-13.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/place-classification-unused-fields-2026-11-25.json](../../docs/research/place-classification-unused-fields-2026-11-25.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/research/place-classification-unused-fields-2026-11-25.md](../../docs/research/place-classification-unused-fields-2026-11-25.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/place-data-free-options-2026-11-13.md](../../docs/research/place-data-free-options-2026-11-13.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/place-data-pilot-results-2026-11-13.md](../../docs/research/place-data-pilot-results-2026-11-13.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/place-filter-report-implementation-2026-11-14.md](../../docs/research/place-filter-report-implementation-2026-11-14.md) | 품질·가격·모의 결과·검토 기록 |
| [docs/research/program-review-2026-11-15.md](../../docs/research/program-review-2026-11-15.md) | 품질·가격·모의 결과·검토 기록 |
| [docs/research/route-modes-2026-11-14.md](../../docs/research/route-modes-2026-11-14.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/route-resource-exploration-2026-11-25.md](../../docs/research/route-resource-exploration-2026-11-25.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/research/route-resource-inventory-2026-11-25.json](../../docs/research/route-resource-inventory-2026-11-25.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/research/route-resource-lookahead-2026-11-25.json](../../docs/research/route-resource-lookahead-2026-11-25.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/research/ux-review-2026-11-15.md](../../docs/research/ux-review-2026-11-15.md) | 품질·가격·모의 결과·검토 기록 |
| [docs/review-2026-11-24.md](../../docs/review-2026-11-24.md) | 품질·가격·모의 결과·검토 기록 |
| [docs/review-2026-11-25.md](../../docs/review-2026-11-25.md) | 품질·가격·모의 결과·검토 기록 |
| [docs/saved-course-previews.md](../../docs/saved-course-previews.md) | 품질·가격·모의 결과·검토 기록 |
| [docs/suyeong-rehearsal-2026-11-16.md](../../docs/suyeong-rehearsal-2026-11-16.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/transport-options-2026-11-16.md](../../docs/transport-options-2026-11-16.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/transport-walking-and-ranges-2026-11-17.md](../../docs/transport-walking-and-ranges-2026-11-17.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/ui-consistency.md](../../docs/ui-consistency.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/user-feedback-and-diagnostics.md](../../docs/user-feedback-and-diagnostics.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/ux-improvements-2026-11-15.md](../../docs/ux-improvements-2026-11-15.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/공통_UI와_홈_메뉴.md](../../docs/공통_UI와_홈_메뉴.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/공통_접속_키.md](../../docs/공통_접속_키.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/시연_장소와_가격_흐름.md](../../docs/시연_장소와_가격_흐름.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/코스_저장과_다시열기.md](../../docs/코스_저장과_다시열기.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |

</details>

<details><summary>팀 사이트 원본·자원·설정 · 179개</summary>

무엇: 팀 문서·프런트·배포 설정. 사용: Markdown·HTML/CSS/JS·설정. 왜: 구현을 함께 읽고 공유. 어떻게: 원본 → 로컬 빌드, 운영 배포는 별도.

| 파일 | 핵심 역할 |
|---|---|
| [docs/site/.gitignore](../../docs/site/.gitignore) | 개인 설정·키·빌드/캐시의 Git 포함 제외 |
| [docs/site/README.md](../../docs/site/README.md) | 앱 기능·실행·현재 검증 안내 |
| [docs/site/content/01_시작/AppLauncher.md](../../docs/site/content/01_시작/AppLauncher.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/01_시작/MainApp.md](../../docs/site/content/01_시작/MainApp.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/AdminController.md](../../docs/site/content/02_화면/AdminController.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/BasePageController.md](../../docs/site/content/02_화면/BasePageController.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/CommunityController.md](../../docs/site/content/02_화면/CommunityController.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/ConditionController.md](../../docs/site/content/02_화면/ConditionController.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/CourseEditController.md](../../docs/site/content/02_화면/CourseEditController.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/InformationReviewController.md](../../docs/site/content/02_화면/InformationReviewController.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/MapBridge.md](../../docs/site/content/02_화면/MapBridge.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/MyPageController.md](../../docs/site/content/02_화면/MyPageController.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/NumberStepper.md](../../docs/site/content/02_화면/NumberStepper.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/PageFeedback.md](../../docs/site/content/02_화면/PageFeedback.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/PlannerMap.md](../../docs/site/content/02_화면/PlannerMap.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/RouteEditorController.md](../../docs/site/content/02_화면/RouteEditorController.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/SavedCourseController.md](../../docs/site/content/02_화면/SavedCourseController.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/TmapSettingsDialog.md](../../docs/site/content/02_화면/TmapSettingsDialog.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/02_화면/login.md](../../docs/site/content/02_화면/login.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/AppDiagnostics.md](../../docs/site/content/03_처리/AppDiagnostics.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/CameraMotionPolicy.md](../../docs/site/content/03_처리/CameraMotionPolicy.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/CoursePreviewImage.md](../../docs/site/content/03_처리/CoursePreviewImage.md) | 품질·가격·모의 결과·검토 기록 |
| [docs/site/content/03_처리/ExternalBrowser.md](../../docs/site/content/03_처리/ExternalBrowser.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/MapPageServer.md](../../docs/site/content/03_처리/MapPageServer.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/NextPlaceService.md](../../docs/site/content/03_처리/NextPlaceService.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/PilotPlaceRepository.md](../../docs/site/content/03_처리/PilotPlaceRepository.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/PlaceBindingCatalog.md](../../docs/site/content/03_처리/PlaceBindingCatalog.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/PlaceCalculationService.md](../../docs/site/content/03_처리/PlaceCalculationService.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/PlaceCountService.md](../../docs/site/content/03_처리/PlaceCountService.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/PlacePresenter.md](../../docs/site/content/03_처리/PlacePresenter.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/PlaceSearchService.md](../../docs/site/content/03_처리/PlaceSearchService.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/PlaceSource.md](../../docs/site/content/03_처리/PlaceSource.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/RecommendedCourseMapper.md](../../docs/site/content/03_처리/RecommendedCourseMapper.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/RouteRangeValidator.md](../../docs/site/content/03_처리/RouteRangeValidator.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/SavedCoursePresenter.md](../../docs/site/content/03_처리/SavedCoursePresenter.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/TeamApiKeys.md](../../docs/site/content/03_처리/TeamApiKeys.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/TmapSettings.md](../../docs/site/content/03_처리/TmapSettings.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/UserContext.md](../../docs/site/content/03_처리/UserContext.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/03_처리/WalkingRouteService.md](../../docs/site/content/03_처리/WalkingRouteService.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/CostEstimate.md](../../docs/site/content/04_데이터/CostEstimate.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/Course.md](../../docs/site/content/04_데이터/Course.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/CoursePlace.md](../../docs/site/content/04_데이터/CoursePlace.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/ExternalPlaceSnapshot.md](../../docs/site/content/04_데이터/ExternalPlaceSnapshot.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/Member.md](../../docs/site/content/04_데이터/Member.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/Place.md](../../docs/site/content/04_데이터/Place.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/PlaceDetails.md](../../docs/site/content/04_데이터/PlaceDetails.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/PriceEstimate.md](../../docs/site/content/04_데이터/PriceEstimate.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/Review.md](../../docs/site/content/04_데이터/Review.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/RoutePlan.md](../../docs/site/content/04_데이터/RoutePlan.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/SavedCourse.md](../../docs/site/content/04_데이터/SavedCourse.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/SearchCondition.md](../../docs/site/content/04_데이터/SearchCondition.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/WalkingRoute.md](../../docs/site/content/04_데이터/WalkingRoute.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/04_데이터/WalkingSearchOption.md](../../docs/site/content/04_데이터/WalkingSearchOption.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/AppDialogs.md](../../docs/site/content/05_회원_DB/AppDialogs.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/CapsLockHint.md](../../docs/site/content/05_회원_DB/CapsLockHint.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/DatabasePool.md](../../docs/site/content/05_회원_DB/DatabasePool.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/DbSettings.md](../../docs/site/content/05_회원_DB/DbSettings.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/InformationReportRepository.md](../../docs/site/content/05_회원_DB/InformationReportRepository.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/InputHistory.md](../../docs/site/content/05_회원_DB/InputHistory.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/LoginUI.md](../../docs/site/content/05_회원_DB/LoginUI.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/MemberAdminPanel.md](../../docs/site/content/05_회원_DB/MemberAdminPanel.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/MemberRepository.md](../../docs/site/content/05_회원_DB/MemberRepository.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/MembershipApp.md](../../docs/site/content/05_회원_DB/MembershipApp.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/PasswordHash.md](../../docs/site/content/05_회원_DB/PasswordHash.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/PlaceCatalogRepository.md](../../docs/site/content/05_회원_DB/PlaceCatalogRepository.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/PlaceQueryDemo.md](../../docs/site/content/05_회원_DB/PlaceQueryDemo.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/PlaceRepository.md](../../docs/site/content/05_회원_DB/PlaceRepository.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/PlannerLoadingDialog.md](../../docs/site/content/05_회원_DB/PlannerLoadingDialog.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/ReplaceIntegratedData.md](../../docs/site/content/05_회원_DB/ReplaceIntegratedData.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/RevealPanel.md](../../docs/site/content/05_회원_DB/RevealPanel.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/SavedCourseRepository.md](../../docs/site/content/05_회원_DB/SavedCourseRepository.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/SignupForm.md](../../docs/site/content/05_회원_DB/SignupForm.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/StartupPanel.md](../../docs/site/content/05_회원_DB/StartupPanel.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/UiTheme.md](../../docs/site/content/05_회원_DB/UiTheme.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/05_회원_DB/UserSession.md](../../docs/site/content/05_회원_DB/UserSession.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/AdminLauncher.md](../../docs/site/content/06_데모/AdminLauncher.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/AdminTestApp.md](../../docs/site/content/06_데모/AdminTestApp.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/CommunityLauncher.md](../../docs/site/content/06_데모/CommunityLauncher.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/CommunityTestApp.md](../../docs/site/content/06_데모/CommunityTestApp.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/ConditionLauncher.md](../../docs/site/content/06_데모/ConditionLauncher.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/ConditionTestApp.md](../../docs/site/content/06_데모/ConditionTestApp.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/CourseEditLauncher.md](../../docs/site/content/06_데모/CourseEditLauncher.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/CourseEditTestApp.md](../../docs/site/content/06_데모/CourseEditTestApp.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/FakeCourseData.md](../../docs/site/content/06_데모/FakeCourseData.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/FakeMapData.md](../../docs/site/content/06_데모/FakeMapData.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/FakeMemberData.md](../../docs/site/content/06_데모/FakeMemberData.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/FakeReviewData.md](../../docs/site/content/06_데모/FakeReviewData.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/PilotPlannerApp.md](../../docs/site/content/06_데모/PilotPlannerApp.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/PilotPlannerLauncher.md](../../docs/site/content/06_데모/PilotPlannerLauncher.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/PlaceBindingDemo.md](../../docs/site/content/06_데모/PlaceBindingDemo.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/SavedCourseLauncher.md](../../docs/site/content/06_데모/SavedCourseLauncher.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/06_데모/SavedCourseTestApp.md](../../docs/site/content/06_데모/SavedCourseTestApp.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/CapsLockHintTest.md](../../docs/site/content/07_테스트/CapsLockHintTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/DatabasePoolTest.md](../../docs/site/content/07_테스트/DatabasePoolTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/DbSettingsTest.md](../../docs/site/content/07_테스트/DbSettingsTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/ExternalBrowserTest.md](../../docs/site/content/07_테스트/ExternalBrowserTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/MapBridgeTest.md](../../docs/site/content/07_테스트/MapBridgeTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/MapSearchTest.md](../../docs/site/content/07_테스트/MapSearchTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/MemberProfileTest.md](../../docs/site/content/07_테스트/MemberProfileTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/NextPlaceServiceTest.md](../../docs/site/content/07_테스트/NextPlaceServiceTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/PasswordFieldTest.md](../../docs/site/content/07_테스트/PasswordFieldTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/PilotPriceFlowTest.md](../../docs/site/content/07_테스트/PilotPriceFlowTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/PlaceBindingCatalogTest.md](../../docs/site/content/07_테스트/PlaceBindingCatalogTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/PlaceCalculationServiceTest.md](../../docs/site/content/07_테스트/PlaceCalculationServiceTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/PlaceRepositoryPriceTest.md](../../docs/site/content/07_테스트/PlaceRepositoryPriceTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/PlaceSearchServiceTest.md](../../docs/site/content/07_테스트/PlaceSearchServiceTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/PlannerLoadingDialogTest.md](../../docs/site/content/07_테스트/PlannerLoadingDialogTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/RecommendedCourseMapperTest.md](../../docs/site/content/07_테스트/RecommendedCourseMapperTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/ReplaceIntegratedDataTest.md](../../docs/site/content/07_테스트/ReplaceIntegratedDataTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/RouteRangeValidatorTest.md](../../docs/site/content/07_테스트/RouteRangeValidatorTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/SavedCourseRepositoryTest.md](../../docs/site/content/07_테스트/SavedCourseRepositoryTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/SavedCourseSchemaTest.md](../../docs/site/content/07_테스트/SavedCourseSchemaTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/StartupPanelTest.md](../../docs/site/content/07_테스트/StartupPanelTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/07_테스트/WalkingRouteUiTest.md](../../docs/site/content/07_테스트/WalkingRouteUiTest.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/08_지도_보충/instant-zoom.js.md](../../docs/site/content/08_지도_보충/instant-zoom.js.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/08_지도_보충/itinerary-rail.js.md](../../docs/site/content/08_지도_보충/itinerary-rail.js.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/08_지도_보충/map.js.md](../../docs/site/content/08_지도_보충/map.js.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/DB_구조와_빈_백업_정리_2026_11_24.md](../../docs/site/content/DB_구조와_빈_백업_정리_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/DB_준비와_일반_실행_분리_2026_11_24.md](../../docs/site/content/DB_준비와_일반_실행_분리_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/JDK21_자동_검색과_선택_2026_11_25.md](../../docs/site/content/JDK21_자동_검색과_선택_2026_11_25.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/JavaFX21_유지보수_패치_2026_11_25.md](../../docs/site/content/JavaFX21_유지보수_패치_2026_11_25.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/index.md](../../docs/site/content/index.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/가격_보완_2026_11_24.md](../../docs/site/content/가격_보완_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/공통_UI와_홈_메뉴.md](../../docs/site/content/공통_UI와_홈_메뉴.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/공통_접속_키.md](../../docs/site/content/공통_접속_키.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/관리자_이용과_검토.md](../../docs/site/content/관리자_이용과_검토.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/데이터_확장_2026_11_23.md](../../docs/site/content/데이터_확장_2026_11_23.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/도보_설정과_지도_경로_동기화_2026_11_25.md](../../docs/site/content/도보_설정과_지도_경로_동기화_2026_11_25.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/라이브러리_사용_안내.md](../../docs/site/content/라이브러리_사용_안내.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/리뷰_및_구현_방향.md](../../docs/site/content/리뷰_및_구현_방향.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/마우스_키보드_입력_점검_2026_11_24.md](../../docs/site/content/마우스_키보드_입력_점검_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/목차.md](../../docs/site/content/목차.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/문서_읽는법.md](../../docs/site/content/문서_읽는법.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/배치_적재_개선_2026_11_24.md](../../docs/site/content/배치_적재_개선_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/변경_요약.md](../../docs/site/content/변경_요약.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/보유_자원의_경로_추천_활용_탐색_2026_11_25.md](../../docs/site/content/보유_자원의_경로_추천_활용_탐색_2026_11_25.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/부산_16개_구군_분산_확장_2026_11_25.md](../../docs/site/content/부산_16개_구군_분산_확장_2026_11_25.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/분류_세분화와_미사용_원본_필드_2026_11_25.md](../../docs/site/content/분류_세분화와_미사용_원본_필드_2026_11_25.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/사용자_안내와_진단.md](../../docs/site/content/사용자_안내와_진단.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/순서_넘버링과_미사용_정리_2026_11_24.md](../../docs/site/content/순서_넘버링과_미사용_정리_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/시연_장소와_가격_흐름.md](../../docs/site/content/시연_장소와_가격_흐름.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/실행_및_테스트.md](../../docs/site/content/실행_및_테스트.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/업데이트_내역.md](../../docs/site/content/업데이트_내역.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/장소_데이터와_표시.md](../../docs/site/content/장소_데이터와_표시.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/저장_미리보기와_복구.md](../../docs/site/content/저장_미리보기와_복구.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/전체_동작_흐름.md](../../docs/site/content/전체_동작_흐름.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/전체_점검_2026_11_24.md](../../docs/site/content/전체_점검_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/중구_98곳_추가와_가격_보완_2026_11_25.md](../../docs/site/content/중구_98곳_추가와_가격_보완_2026_11_25.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/지도_렌더링_여유_영역_2026_11_24.md](../../docs/site/content/지도_렌더링_여유_영역_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/지도_범위와_포커스.md](../../docs/site/content/지도_범위와_포커스.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/지도_선택과_저장_지도_준비_2026_11_24.md](../../docs/site/content/지도_선택과_저장_지도_준비_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/최종_검토_2026_11_15.md](../../docs/site/content/최종_검토_2026_11_15.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/코스_저장_설계.md](../../docs/site/content/코스_저장_설계.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/코스_저장과_다시열기.md](../../docs/site/content/코스_저장과_다시열기.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/키보드_조작과_포커스_복귀_2026_11_25.md](../../docs/site/content/키보드_조작과_포커스_복귀_2026_11_25.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/패키지_통합과_구조_점검_2026_11_24.md](../../docs/site/content/패키지_통합과_구조_점검_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/페이지_구조와_이름_2026_11_24.md](../../docs/site/content/페이지_구조와_이름_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/현재_구현_흐름.md](../../docs/site/content/현재_구현_흐름.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/content/화면_가독성과_작은_창_배치_2026_11_24.md](../../docs/site/content/화면_가독성과_작은_창_배치_2026_11_24.md) | 설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준 |
| [docs/site/document-revisions.json](../../docs/site/document-revisions.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/site/package.json](../../docs/site/package.json) | 사이트 배포·실행·의존성 설정 |
| [docs/site/web/compact-docs.css](../../docs/site/web/compact-docs.css) | 화면·문서의 구조·스타일·시각 자원 |
| [docs/site/web/compact-docs.js](../../docs/site/web/compact-docs.js) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/site/web/connect/connect.css](../../docs/site/web/connect/connect.css) | 화면·문서의 구조·스타일·시각 자원 |
| [docs/site/web/connect/connect.js](../../docs/site/web/connect/connect.js) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/site/web/connect/index.html](../../docs/site/web/connect/index.html) | 화면·문서의 구조·스타일·시각 자원 |
| [docs/site/web/dashboard/board.css](../../docs/site/web/dashboard/board.css) | 화면·문서의 구조·스타일·시각 자원 |
| [docs/site/web/dashboard/board.js](../../docs/site/web/dashboard/board.js) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/site/web/dashboard/index.html](../../docs/site/web/dashboard/index.html) | 화면·문서의 구조·스타일·시각 자원 |
| [docs/site/web/dashboard/task-tree.js](../../docs/site/web/dashboard/task-tree.js) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/site/web/document-updates.css](../../docs/site/web/document-updates.css) | 화면·문서의 구조·스타일·시각 자원 |
| [docs/site/web/document-updates.js](../../docs/site/web/document-updates.js) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/site/web/flow-tooltips.css](../../docs/site/web/flow-tooltips.css) | 화면·문서의 구조·스타일·시각 자원 |
| [docs/site/web/flow-tooltips.js](../../docs/site/web/flow-tooltips.js) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/site/web/learning-demos.css](../../docs/site/web/learning-demos.css) | 화면·문서의 구조·스타일·시각 자원 |
| [docs/site/web/learning-demos.js](../../docs/site/web/learning-demos.js) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/site/web/navigation.css](../../docs/site/web/navigation.css) | 화면·문서의 구조·스타일·시각 자원 |
| [docs/site/web/responsive-navigation.js](../../docs/site/web/responsive-navigation.js) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/site/wrangler.jsonc](../../docs/site/wrangler.jsonc) | 사이트 배포·실행·의존성 설정 |

</details>

<details><summary>팀 사이트 서버·공유 상태 · 15개</summary>

무엇: 인증·보드·문서/키 응답. 사용: Worker·OAuth·Durable Object SQLite. 왜: 협업 접근과 공유 상태. 어떻게: 요청 인증/권한 → 허용 경로 → 저장/응답.

| 파일 | 핵심 역할 |
|---|---|
| [docs/site/src/agent-store.mjs](../../docs/site/src/agent-store.mjs) | AgentAccess의 동의·토큰·읽기 권한 |
| [docs/site/src/agent.mjs](../../docs/site/src/agent.mjs) | 문서/에이전트 접근 연결 보조 |
| [docs/site/src/app-keys.mjs](../../docs/site/src/app-keys.mjs) | 인증된 앱에 허용 키·별도 DB 설정 전달 |
| [docs/site/src/assignment-priorities.mjs](../../docs/site/src/assignment-priorities.mjs) | 시연 기준 우선순위의 보존형 초기 반영 |
| [docs/site/src/board-audit.mjs](../../docs/site/src/board-audit.mjs) | 보드의 특정 시점 반영 이력·기존 사람 수정 보존 |
| [docs/site/src/board-copy.mjs](../../docs/site/src/board-copy.mjs) | 보드의 특정 시점 반영 이력·기존 사람 수정 보존 |
| [docs/site/src/board-db01.mjs](../../docs/site/src/board-db01.mjs) | 보드의 특정 시점 반영 이력·기존 사람 수정 보존 |
| [docs/site/src/board-db02.mjs](../../docs/site/src/board-db02.mjs) | 보드의 특정 시점 반영 이력·기존 사람 수정 보존 |
| [docs/site/src/board-nov14.mjs](../../docs/site/src/board-nov14.mjs) | 보드의 특정 시점 반영 이력·기존 사람 수정 보존 |
| [docs/site/src/board-nov16.mjs](../../docs/site/src/board-nov16.mjs) | 보드의 특정 시점 반영 이력·기존 사람 수정 보존 |
| [docs/site/src/board-nov24.mjs](../../docs/site/src/board-nov24.mjs) | 보드의 특정 시점 반영 이력·기존 사람 수정 보존 |
| [docs/site/src/board-nov25-followup.mjs](../../docs/site/src/board-nov25-followup.mjs) | 보드의 특정 시점 반영 이력·기존 사람 수정 보존 |
| [docs/site/src/board-nov25.mjs](../../docs/site/src/board-nov25.mjs) | 보드의 특정 시점 반영 이력·기존 사람 수정 보존 |
| [docs/site/src/board-store.mjs](../../docs/site/src/board-store.mjs) | TaskBoard의 공유 작업·버전·SQL 저장 |
| [docs/site/src/worker.mjs](../../docs/site/src/worker.mjs) | 문서/보드 라우팅·GitHub 인증·권한·보호 응답 |

</details>

<details><summary>사이트 로직 검사 · 13개</summary>

무엇: 인증·보드·이력의 회귀. 사용: Node test·SQLite fixture. 왜: 권한과 사람의 수정 보존. 어떻게: 요청/상태 준비 → handler → 응답·DB 상태 확인.

| 파일 | 핵심 역할 |
|---|---|
| [docs/site/test/agent.test.mjs](../../docs/site/test/agent.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/app-keys.test.mjs](../../docs/site/test/app-keys.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/assignment-priorities.test.mjs](../../docs/site/test/assignment-priorities.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/board-nov14.test.mjs](../../docs/site/test/board-nov14.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/board-nov16.test.mjs](../../docs/site/test/board-nov16.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/board-nov24.test.mjs](../../docs/site/test/board-nov24.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/board-nov25-followup.test.mjs](../../docs/site/test/board-nov25-followup.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/board-nov25.test.mjs](../../docs/site/test/board-nov25.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/board-store.test.mjs](../../docs/site/test/board-store.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/document-revisions.test.mjs](../../docs/site/test/document-revisions.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/flow-methods.test.mjs](../../docs/site/test/flow-methods.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/task-tree.test.mjs](../../docs/site/test/task-tree.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [docs/site/test/worker.test.mjs](../../docs/site/test/worker.test.mjs) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |

</details>

<details><summary>사이트 빌드·표시 검사 · 27개</summary>

무엇: 문서 생성·표시 점검. 사용: Node·Quartz·Playwright. 왜: 참조성·화면 크기·링크 확인. 어떻게: 원본 가공 → 정적 빌드 → DOM/화면 검사.

| 파일 | 핵심 역할 |
|---|---|
| [docs/site/tools/add-learning-demos.mjs](../../docs/site/tools/add-learning-demos.mjs) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/site/tools/build.mjs](../../docs/site/tools/build.mjs) | Quartz 설정·원본 처리·정적 빌드·자원 최적화 |
| [docs/site/tools/check-agent-client.mjs](../../docs/site/tools/check-agent-client.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-agent-consent.mjs](../../docs/site/tools/check-agent-consent.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-beginner-board.mjs](../../docs/site/tools/check-beginner-board.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-board-hierarchy.mjs](../../docs/site/tools/check-board-hierarchy.mjs) | 보드의 특정 시점 반영 이력·기존 사람 수정 보존 |
| [docs/site/tools/check-compact-docs.mjs](../../docs/site/tools/check-compact-docs.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-connection.mjs](../../docs/site/tools/check-connection.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-db01-design.mjs](../../docs/site/tools/check-db01-design.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-document-updates.mjs](../../docs/site/tools/check-document-updates.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-flow-tooltips.mjs](../../docs/site/tools/check-flow-tooltips.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-initial-board.mjs](../../docs/site/tools/check-initial-board.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-latest-board.mjs](../../docs/site/tools/check-latest-board.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-learning-demos.mjs](../../docs/site/tools/check-learning-demos.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-navigation-performance.mjs](../../docs/site/tools/check-navigation-performance.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-readable-docs.mjs](../../docs/site/tools/check-readable-docs.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-responsive-navigation.mjs](../../docs/site/tools/check-responsive-navigation.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-tables.mjs](../../docs/site/tools/check-tables.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/check-ui.mjs](../../docs/site/tools/check-ui.mjs) | 사이트의 해당 화면/동작 브라우저 점검 |
| [docs/site/tools/compact-docs.mjs](../../docs/site/tools/compact-docs.mjs) | 문서 구조·표시 보정 |
| [docs/site/tools/document-revisions.mjs](../../docs/site/tools/document-revisions.mjs) | 본문 변경과 문서 읽음 버전 관리 |
| [docs/site/tools/flow-methods.mjs](../../docs/site/tools/flow-methods.mjs) | 문서의 함수 설명·호출 단서 보강 |
| [docs/site/tools/optimize-assets.mjs](../../docs/site/tools/optimize-assets.mjs) | 정적 CSS·JS 자원 최적화 |
| [docs/site/tools/reading-notes.json](../../docs/site/tools/reading-notes.json) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [docs/site/tools/refresh-reading-guides.mjs](../../docs/site/tools/refresh-reading-guides.mjs) | 문서 원본의 설명·읽기 안내 갱신 도구 |
| [docs/site/tools/refresh-reading-notes.mjs](../../docs/site/tools/refresh-reading-notes.mjs) | 문서 원본의 설명·읽기 안내 갱신 도구 |
| [docs/site/tools/table-links.mjs](../../docs/site/tools/table-links.mjs) | 문서 표의 링크 정리 |

</details>

<details><summary>앱 Java · 107개</summary>

무엇: 실제 앱의 타입·정책·화면·DB. 사용: Java와 클래스별 기술. 왜·어떻게: 08 클래스 색인과 기능 문서의 카드에 연결.

| 파일 | 핵심 역할 |
|---|---|
| [src/main/java/com/datecourse/app/AppLauncher.java](../../src/main/java/com/datecourse/app/AppLauncher.java) | 통합 실행을 회원·코스 화면으로 연결 |
| [src/main/java/com/datecourse/auth/PasswordHash.java](../../src/main/java/com/datecourse/auth/PasswordHash.java) | 개별 salt·반복 해시 생성과 비밀번호 비교 |
| [src/main/java/com/datecourse/db/AdminPasswordReset.java](../../src/main/java/com/datecourse/db/AdminPasswordReset.java) | 명시적인 관리자 비밀번호 재설정 도구 |
| [src/main/java/com/datecourse/db/DatabasePool.java](../../src/main/java/com/datecourse/db/DatabasePool.java) | DB 연결 재사용을 위한 풀 생성 |
| [src/main/java/com/datecourse/db/DatabaseSchema.java](../../src/main/java/com/datecourse/db/DatabaseSchema.java) | 필요한 DB 구조 존재 확인 |
| [src/main/java/com/datecourse/db/DbSettings.java](../../src/main/java/com/datecourse/db/DbSettings.java) | DB 설정의 로딩·형식 확인 |
| [src/main/java/com/datecourse/db/InformationReportRepository.java](../../src/main/java/com/datecourse/db/InformationReportRepository.java) | 정정 제보·검토·승인값 적용 |
| [src/main/java/com/datecourse/db/MemberRepository.java](../../src/main/java/com/datecourse/db/MemberRepository.java) | 가입·로그인·프로필·관리자 DB 작업 |
| [src/main/java/com/datecourse/db/PlaceCatalogRepository.java](../../src/main/java/com/datecourse/db/PlaceCatalogRepository.java) | 출처별 장소·버전·이력·보존 적재 |
| [src/main/java/com/datecourse/db/PlaceRepository.java](../../src/main/java/com/datecourse/db/PlaceRepository.java) | 운영 카탈로그 우선 장소 조회 |
| [src/main/java/com/datecourse/db/SavedCourseRepository.java](../../src/main/java/com/datecourse/db/SavedCourseRepository.java) | 회원별 코스·장소 스냅샷·미리보기 저장 |
| [src/main/java/com/datecourse/demo/CameraMotionDemoApp.java](../../src/main/java/com/datecourse/demo/CameraMotionDemoApp.java) | 카메라 정책·지도 전환 시연 |
| [src/main/java/com/datecourse/demo/CameraMotionDemoLauncher.java](../../src/main/java/com/datecourse/demo/CameraMotionDemoLauncher.java) | 카메라 시연 진입점 |
| [src/main/java/com/datecourse/demo/ConditionLauncher.java](../../src/main/java/com/datecourse/demo/ConditionLauncher.java) | 회원 DB 없는 조건 화면 시연 실행 |
| [src/main/java/com/datecourse/demo/ConditionTestApp.java](../../src/main/java/com/datecourse/demo/ConditionTestApp.java) | 조건 화면의 별도 JavaFX 시연 호스트 |
| [src/main/java/com/datecourse/demo/CoursePreviewSetup.java](../../src/main/java/com/datecourse/demo/CoursePreviewSetup.java) | 이전 명령에서 이미지 구조 준비로 위임 |
| [src/main/java/com/datecourse/demo/InformationReportSetup.java](../../src/main/java/com/datecourse/demo/InformationReportSetup.java) | 이전 명령에서 DB 준비 도구로 위임 |
| [src/main/java/com/datecourse/demo/KakaoRouteProbe.java](../../src/main/java/com/datecourse/demo/KakaoRouteProbe.java) | 명시적 실제 카카오 경로 점검 |
| [src/main/java/com/datecourse/demo/PilotPlannerApp.java](../../src/main/java/com/datecourse/demo/PilotPlannerApp.java) | 소규모 자료로 코스 화면 구성 |
| [src/main/java/com/datecourse/demo/PilotPlannerLauncher.java](../../src/main/java/com/datecourse/demo/PilotPlannerLauncher.java) | 소규모 장소 시연 진입점 |
| [src/main/java/com/datecourse/demo/PlaceBindingDemo.java](../../src/main/java/com/datecourse/demo/PlaceBindingDemo.java) | 확정 외부 ID 연결표 오프라인 확인 |
| [src/main/java/com/datecourse/demo/PublicPlaceImport.java](../../src/main/java/com/datecourse/demo/PublicPlaceImport.java) | 공공 파일 쌍 검증·명시적 적재 |
| [src/main/java/com/datecourse/maintenance/DatabaseSetup.java](../../src/main/java/com/datecourse/maintenance/DatabaseSetup.java) | 요청된 DB 구조 준비만 명시적으로 실행 |
| [src/main/java/com/datecourse/maintenance/ReplaceIntegratedData.java](../../src/main/java/com/datecourse/maintenance/ReplaceIntegratedData.java) | 이전 일반 데이터의 명시적 교체 |
| [src/main/java/com/datecourse/model/CostEstimate.java](../../src/main/java/com/datecourse/model/CostEstimate.java) | 인원별 비용 범위와 미확인·추정 개수 집계 |
| [src/main/java/com/datecourse/model/ExternalPlaceLink.java](../../src/main/java/com/datecourse/model/ExternalPlaceLink.java) | 외부 ID의 승인·일치도·실제 장소 여부 구분 |
| [src/main/java/com/datecourse/model/ExternalPlaceSnapshot.java](../../src/main/java/com/datecourse/model/ExternalPlaceSnapshot.java) | 외부 상세 조회의 임시 결과·영업 구간 |
| [src/main/java/com/datecourse/model/FactState.java](../../src/main/java/com/datecourse/model/FactState.java) | 가능·불가능·미확인의 세 상태 |
| [src/main/java/com/datecourse/model/InformationReport.java](../../src/main/java/com/datecourse/model/InformationReport.java) | 정정 대상·이유·검토 상태 전달 |
| [src/main/java/com/datecourse/model/Member.java](../../src/main/java/com/datecourse/model/Member.java) | 역할·가입일·정지 상태의 별도 회원 기본 모델 |
| [src/main/java/com/datecourse/model/NextPlaceCondition.java](../../src/main/java/com/datecourse/model/NextPlaceCondition.java) | 다음 장소 조건·필터·표시 개수 전달 |
| [src/main/java/com/datecourse/model/Place.java](../../src/main/java/com/datecourse/model/Place.java) | 장소 ID·좌표·분류·가격·상세 전달 |
| [src/main/java/com/datecourse/model/PlaceAttributeFact.java](../../src/main/java/com/datecourse/model/PlaceAttributeFact.java) | 속성의 상태·근거 전달 |
| [src/main/java/com/datecourse/model/PlaceDetails.java](../../src/main/java/com/datecourse/model/PlaceDetails.java) | 주소·업종·연락처·출처·속성·외부 연결 보관 |
| [src/main/java/com/datecourse/model/PlaceFeature.java](../../src/main/java/com/datecourse/model/PlaceFeature.java) | 장소 속성의 종류 정의 |
| [src/main/java/com/datecourse/model/PlaceField.java](../../src/main/java/com/datecourse/model/PlaceField.java) | 항목별 출처를 구분하는 필드 정의 |
| [src/main/java/com/datecourse/model/PlacePriceFilter.java](../../src/main/java/com/datecourse/model/PlacePriceFilter.java) | 참고·추정·무료 가격 조건 판정 |
| [src/main/java/com/datecourse/model/PlaceTag.java](../../src/main/java/com/datecourse/model/PlaceTag.java) | 확인된 특징의 표시 이름 정의 |
| [src/main/java/com/datecourse/model/PriceEstimate.java](../../src/main/java/com/datecourse/model/PriceEstimate.java) | 가격 범위·단위·근거·미확인 표현 |
| [src/main/java/com/datecourse/model/ProviderPriceRange.java](../../src/main/java/com/datecourse/model/ProviderPriceRange.java) | 외부 가격 범위·통화 전달 |
| [src/main/java/com/datecourse/model/RoutePlan.java](../../src/main/java/com/datecourse/model/RoutePlan.java) | 사용자 계획·방문 순서·반경·수단 전달 |
| [src/main/java/com/datecourse/model/SavedCourse.java](../../src/main/java/com/datecourse/model/SavedCourse.java) | 저장 코스 ID·소유자·계획·수정 시각 전달 |
| [src/main/java/com/datecourse/model/SearchCondition.java](../../src/main/java/com/datecourse/model/SearchCondition.java) | 이전 일괄 추천의 예산·분류·방문 수 조건 |
| [src/main/java/com/datecourse/model/SourceEvidence.java](../../src/main/java/com/datecourse/model/SourceEvidence.java) | 출처·기준일·확인일 구분 |
| [src/main/java/com/datecourse/model/TravelMode.java](../../src/main/java/com/datecourse/model/TravelMode.java) | 이동수단별 반경 한도·안내 정책 |
| [src/main/java/com/datecourse/model/TravelRoute.java](../../src/main/java/com/datecourse/model/TravelRoute.java) | 공급사가 조회한 실제 경로·구간·거리·시간 전달 |
| [src/main/java/com/datecourse/model/UserSession.java](../../src/main/java/com/datecourse/model/UserSession.java) | 로그인 회원 식별 정보 전달 |
| [src/main/java/com/datecourse/model/WalkingSearchOption.java](../../src/main/java/com/datecourse/model/WalkingSearchOption.java) | 도보 탐색 옵션 정의 |
| [src/main/java/com/datecourse/page/_01step_planner/CoursePlannerController.java](../../src/main/java/com/datecourse/page/_01step_planner/CoursePlannerController.java) | 공통 조건·확정 코스·저장·페이지 전환 조율 |
| [src/main/java/com/datecourse/page/_01step_planner/InformationReportDialog.java](../../src/main/java/com/datecourse/page/_01step_planner/InformationReportDialog.java) | 장소 정정 제보 입력 |
| [src/main/java/com/datecourse/page/_02step_editor/CourseEditorController.java](../../src/main/java/com/datecourse/page/_02step_editor/CourseEditorController.java) | 초안·후보·순서·반경·필터 조율 |
| [src/main/java/com/datecourse/page/_03step_saved/CoursePreviewDialog.java](../../src/main/java/com/datecourse/page/_03step_saved/CoursePreviewDialog.java) | 미리보기 별도 창 |
| [src/main/java/com/datecourse/page/_03step_saved/SavedCourseController.java](../../src/main/java/com/datecourse/page/_03step_saved/SavedCourseController.java) | 저장 목록·이미지/지도·명시적 경로 조회 조율 |
| [src/main/java/com/datecourse/page/account/InputHistory.java](../../src/main/java/com/datecourse/page/account/InputHistory.java) | 입력 이력·기억 설정 관리 |
| [src/main/java/com/datecourse/page/account/MembershipApp.java](../../src/main/java/com/datecourse/page/account/MembershipApp.java) | Swing 회원 화면과 JavaFX 코스 화면 연결 |
| [src/main/java/com/datecourse/page/account/MyPageController.java](../../src/main/java/com/datecourse/page/account/MyPageController.java) | 프로필·내 코스 화면 연결 |
| [src/main/java/com/datecourse/page/account/PlannerLoadingDialog.java](../../src/main/java/com/datecourse/page/account/PlannerLoadingDialog.java) | 코스 화면 준비 표시 |
| [src/main/java/com/datecourse/page/account/SignupForm.java](../../src/main/java/com/datecourse/page/account/SignupForm.java) | 회원가입 입력 양식 |
| [src/main/java/com/datecourse/page/account/StartupPanel.java](../../src/main/java/com/datecourse/page/account/StartupPanel.java) | 시작 화면 표시 |
| [src/main/java/com/datecourse/page/admin/AdminStyle.java](../../src/main/java/com/datecourse/page/admin/AdminStyle.java) | 관리자 Swing 스타일 |
| [src/main/java/com/datecourse/page/admin/MemberAdminPanel.java](../../src/main/java/com/datecourse/page/admin/MemberAdminPanel.java) | 회원 검색·수정·상태·삭제 관리 화면 |
| [src/main/java/com/datecourse/page/common/BasePageController.java](../../src/main/java/com/datecourse/page/common/BasePageController.java) | 공통 비동기·로딩·실패 표시 |
| [src/main/java/com/datecourse/page/common/NumberStepper.java](../../src/main/java/com/datecourse/page/common/NumberStepper.java) | 인원·가격 입력과 증감 처리 |
| [src/main/java/com/datecourse/page/common/PageFeedback.java](../../src/main/java/com/datecourse/page/common/PageFeedback.java) | 로딩·성공·오류 메시지 표시 |
| [src/main/java/com/datecourse/page/common/TmapSettingsDialog.java](../../src/main/java/com/datecourse/page/common/TmapSettingsDialog.java) | 도보 탐색 옵션 입력 |
| [src/main/java/com/datecourse/page/common/controls/DialogTheme.java](../../src/main/java/com/datecourse/page/common/controls/DialogTheme.java) | 대화상자 테마 적용 |
| [src/main/java/com/datecourse/page/common/controls/ExpandableFilterPane.java](../../src/main/java/com/datecourse/page/common/controls/ExpandableFilterPane.java) | 필터 묶음의 펼침·요약 |
| [src/main/java/com/datecourse/page/common/controls/FormScrollPane.java](../../src/main/java/com/datecourse/page/common/controls/FormScrollPane.java) | 입력 양식 스크롤 |
| [src/main/java/com/datecourse/page/common/controls/ModernScrollPane.java](../../src/main/java/com/datecourse/page/common/controls/ModernScrollPane.java) | 공통 스크롤 외관·동작 |
| [src/main/java/com/datecourse/page/common/controls/PlaceFeatureSelector.java](../../src/main/java/com/datecourse/page/common/controls/PlaceFeatureSelector.java) | 활동별 특징 필터 구성 |
| [src/main/java/com/datecourse/page/common/map/CourseMapView.java](../../src/main/java/com/datecourse/page/common/map/CourseMapView.java) | WebView 지도·경로·캡처·비동기 수명 관리 |
| [src/main/java/com/datecourse/page/common/map/MapEventBridge.java](../../src/main/java/com/datecourse/page/common/map/MapEventBridge.java) | 지도 JavaScript에서 Java로 오는 이벤트 경계 |
| [src/main/java/com/datecourse/page/common/map/MapViewportLifecycle.java](../../src/main/java/com/datecourse/page/common/map/MapViewportLifecycle.java) | 숨김·크기 변경 후 실제 표시 시점 감지 |
| [src/main/java/com/datecourse/page/common/swing/AppDialogs.java](../../src/main/java/com/datecourse/page/common/swing/AppDialogs.java) | 공통 Swing 대화상자 |
| [src/main/java/com/datecourse/page/common/swing/CapsLockHint.java](../../src/main/java/com/datecourse/page/common/swing/CapsLockHint.java) | 비밀번호 입력 Caps Lock 안내 |
| [src/main/java/com/datecourse/page/common/swing/LoginUI.java](../../src/main/java/com/datecourse/page/common/swing/LoginUI.java) | Swing 로그인 부품·외관 |
| [src/main/java/com/datecourse/page/common/swing/RevealPanel.java](../../src/main/java/com/datecourse/page/common/swing/RevealPanel.java) | Swing 패널 펼침 전환 |
| [src/main/java/com/datecourse/page/review/InformationReviewController.java](../../src/main/java/com/datecourse/page/review/InformationReviewController.java) | 제보 목록·상세·승인·반려 화면 |
| [src/main/java/com/datecourse/service/AppDiagnostics.java](../../src/main/java/com/datecourse/service/AppDiagnostics.java) | 실패 단계·취소·결과 진단 정보 |
| [src/main/java/com/datecourse/service/CameraMotionPolicy.java](../../src/main/java/com/datecourse/service/CameraMotionPolicy.java) | 조작 의도·화면 거리·넘침에 따른 지도 이동 정책 |
| [src/main/java/com/datecourse/service/CandidateRecoveryService.java](../../src/main/java/com/datecourse/service/CandidateRecoveryService.java) | 후보를 실제로 늘리는 조건 완화 안내 |
| [src/main/java/com/datecourse/service/CoursePreviewImage.java](../../src/main/java/com/datecourse/service/CoursePreviewImage.java) | PNG 생성·이미지 내용 지문 |
| [src/main/java/com/datecourse/service/CoursePreviewRenderer.java](../../src/main/java/com/datecourse/service/CoursePreviewRenderer.java) | 미리보기 렌더러 함수형 경계 |
| [src/main/java/com/datecourse/service/ExternalBrowser.java](../../src/main/java/com/datecourse/service/ExternalBrowser.java) | 외부 링크를 시스템 브라우저에 연결 |
| [src/main/java/com/datecourse/service/KakaoRouteService.java](../../src/main/java/com/datecourse/service/KakaoRouteService.java) | 카카오 자동차·대중교통 응답 파싱 |
| [src/main/java/com/datecourse/service/KakaoRouteSettings.java](../../src/main/java/com/datecourse/service/KakaoRouteSettings.java) | 카카오 REST 경로 키 설정 확인 |
| [src/main/java/com/datecourse/service/MapPageServer.java](../../src/main/java/com/datecourse/service/MapPageServer.java) | 지도 자원을 로컬 HTTP로 제공 |
| [src/main/java/com/datecourse/service/NextPlaceRadius.java](../../src/main/java/com/datecourse/service/NextPlaceRadius.java) | 전체 범위·직전 장소에 맞는 구간 반경 상한 |
| [src/main/java/com/datecourse/service/NextPlaceService.java](../../src/main/java/com/datecourse/service/NextPlaceService.java) | 조건에 맞는 다음 후보를 거리순으로 선택 |
| [src/main/java/com/datecourse/service/PilotPlaceRepository.java](../../src/main/java/com/datecourse/service/PilotPlaceRepository.java) | 소규모 대조 완료 시연 자료 읽기 |
| [src/main/java/com/datecourse/service/PlaceBindingCatalog.java](../../src/main/java/com/datecourse/service/PlaceBindingCatalog.java) | 자료 버전과 일치하는 외부 ID 연결표 읽기 |
| [src/main/java/com/datecourse/service/PlaceCalculationService.java](../../src/main/java/com/datecourse/service/PlaceCalculationService.java) | 좌표 사이 구면 거리·이전 정액 계산 |
| [src/main/java/com/datecourse/service/PlaceFacets.java](../../src/main/java/com/datecourse/service/PlaceFacets.java) | 공공 업종을 화면 유형·상세 분류에 연결 |
| [src/main/java/com/datecourse/service/PlaceMapPayload.java](../../src/main/java/com/datecourse/service/PlaceMapPayload.java) | 모델에 지도 표시용 presentation을 덧붙임 |
| [src/main/java/com/datecourse/service/PlacePresenter.java](../../src/main/java/com/datecourse/service/PlacePresenter.java) | 가격·출처·특징을 표시 문구로 변환 |
| [src/main/java/com/datecourse/service/PlaceSearchService.java](../../src/main/java/com/datecourse/service/PlaceSearchService.java) | 전체 범위·중복 대표 검색과 이전 일괄 추천 |
| [src/main/java/com/datecourse/service/PlaceSource.java](../../src/main/java/com/datecourse/service/PlaceSource.java) | 장소 조회의 공통 계약 |
| [src/main/java/com/datecourse/service/PublicDemoPlaceRepository.java](../../src/main/java/com/datecourse/service/PublicDemoPlaceRepository.java) | 내장 공공 시연 자료 읽기 |
| [src/main/java/com/datecourse/service/PublicPlaceFacts.java](../../src/main/java/com/datecourse/service/PublicPlaceFacts.java) | 장소 파일 지문·ID 검사와 공공 속성 연결 |
| [src/main/java/com/datecourse/service/RouteRangeValidator.java](../../src/main/java/com/datecourse/service/RouteRangeValidator.java) | 완성 코스의 전체·구간 직선 반경 검사 |
| [src/main/java/com/datecourse/service/SavedCoursePresenter.java](../../src/main/java/com/datecourse/service/SavedCoursePresenter.java) | 저장 코스 합계·이동 요약 문구 |
| [src/main/java/com/datecourse/service/TeamApiKeys.java](../../src/main/java/com/datecourse/service/TeamApiKeys.java) | 공통 인증으로 실행 중 키·DB 설정 조회 |
| [src/main/java/com/datecourse/service/TmapSettings.java](../../src/main/java/com/datecourse/service/TmapSettings.java) | 도보 키·옵션·설정 변경 버전 |
| [src/main/java/com/datecourse/service/TransitWalkingRouteService.java](../../src/main/java/com/datecourse/service/TransitWalkingRouteService.java) | 구간별 빠른 도보·대중교통 결과 선택 |
| [src/main/java/com/datecourse/service/TravelRouteService.java](../../src/main/java/com/datecourse/service/TravelRouteService.java) | 이동수단에 맞는 경로 어댑터 호출 |
| [src/main/java/com/datecourse/service/UserContext.java](../../src/main/java/com/datecourse/service/UserContext.java) | 한 로그인의 프로필·목록·이미지·지도 준비 캐시 |
| [src/main/java/com/datecourse/service/WalkingRouteService.java](../../src/main/java/com/datecourse/service/WalkingRouteService.java) | TMAP 보행 구간 조회·파싱·짧은 캐시 |

</details>

<details><summary>화면·지도·스타일·공개 설정 예제 · 21개</summary>

무엇: 배치·웹 지도·외관·설정 형식. 사용: FXML/HTML/JS/CSS. 왜: 화면 구조·입력·표시를 연결. 어떻게: 컨트롤러·WebView·브리지에서 로딩/이벤트 처리.

| 파일 | 핵심 역할 |
|---|---|
| [src/main/resources/com/datecourse/page/_01step_planner/condition-view.fxml](../../src/main/resources/com/datecourse/page/_01step_planner/condition-view.fxml) | JavaFX 화면 배치·컨트롤러·이벤트 선언 |
| [src/main/resources/com/datecourse/page/_02step_editor/route-editor-view.fxml](../../src/main/resources/com/datecourse/page/_02step_editor/route-editor-view.fxml) | JavaFX 화면 배치·컨트롤러·이벤트 선언 |
| [src/main/resources/com/datecourse/page/_03step_saved/saved-course-view.fxml](../../src/main/resources/com/datecourse/page/_03step_saved/saved-course-view.fxml) | JavaFX 화면 배치·컨트롤러·이벤트 선언 |
| [src/main/resources/com/datecourse/page/account/my-page-view.fxml](../../src/main/resources/com/datecourse/page/account/my-page-view.fxml) | JavaFX 화면 배치·컨트롤러·이벤트 선언 |
| [src/main/resources/com/datecourse/page/review/information-review-view.fxml](../../src/main/resources/com/datecourse/page/review/information-review-view.fxml) | JavaFX 화면 배치·컨트롤러·이벤트 선언 |
| [src/main/resources/db.properties.example](../../src/main/resources/db.properties.example) | 공유 가능한 설정 형식 예제; 개인 실제 값과 구분 |
| [src/main/resources/map/camera-motion-demo.html](../../src/main/resources/map/camera-motion-demo.html) | 카메라 시연 화면 |
| [src/main/resources/map/camera-motion-demo.js](../../src/main/resources/map/camera-motion-demo.js) | 카메라 시연 JavaScript |
| [src/main/resources/map/camera-motion.js](../../src/main/resources/map/camera-motion.js) | 지도 카메라 이동 실행 |
| [src/main/resources/map/instant-zoom.js](../../src/main/resources/map/instant-zoom.js) | 화면 중심 기준 확대 연결 |
| [src/main/resources/map/itinerary-rail.css](../../src/main/resources/map/itinerary-rail.css) | 방문 목록·상세 카드의 스타일 |
| [src/main/resources/map/itinerary-rail.js](../../src/main/resources/map/itinerary-rail.js) | 방문 목록·상세·포커스·키보드 동작 |
| [src/main/resources/map/kakao-config.example.js](../../src/main/resources/map/kakao-config.example.js) | 공유 가능한 설정 형식 예제; 개인 실제 값과 구분 |
| [src/main/resources/map/map-viewport.js](../../src/main/resources/map/map-viewport.js) | 렌더링 여백·화면 좌표·출처 배치 |
| [src/main/resources/map/map.html](../../src/main/resources/map/map.html) | 지도·검색·상세·구간 안내 DOM |
| [src/main/resources/map/map.js](../../src/main/resources/map/map.js) | SDK·검색·핀·반경·경로·캡처의 JavaScript 연결 |
| [src/main/resources/map/pin-pointer-gesture.js](../../src/main/resources/map/pin-pointer-gesture.js) | 마커 포인터 조작·우선 처리 |
| [src/main/resources/modern-scroll.css](../../src/main/resources/modern-scroll.css) | JavaFX 스크롤 외관 |
| [src/main/resources/style.css](../../src/main/resources/style.css) | JavaFX 공통 화면 스타일 |
| [src/main/resources/team-access.properties.example](../../src/main/resources/team-access.properties.example) | 공유 가능한 설정 형식 예제; 개인 실제 값과 구분 |
| [src/main/resources/tmap.properties.example](../../src/main/resources/tmap.properties.example) | 공유 가능한 설정 형식 예제; 개인 실제 값과 구분 |

</details>

<details><summary>앱 검사 · 57개</summary>

무엇: 구현 규칙의 회귀 확인. 사용: JUnit/H2/JavaFX fixture. 왜: 조건·저장·늦은 응답 오류 방지. 어떻게: 준비 입력 → 동작 → 결과/불변 조건 확인.

| 파일 | 핵심 역할 |
|---|---|
| [src/test/java/com/datecourse/db/AdminMemberManagementTest.java](../../src/test/java/com/datecourse/db/AdminMemberManagementTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/db/AdminPasswordResetTest.java](../../src/test/java/com/datecourse/db/AdminPasswordResetTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/db/DatabasePoolTest.java](../../src/test/java/com/datecourse/db/DatabasePoolTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/db/DatabaseSetupTest.java](../../src/test/java/com/datecourse/db/DatabaseSetupTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/db/DbSettingsTest.java](../../src/test/java/com/datecourse/db/DbSettingsTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/db/InformationReportRepositoryTest.java](../../src/test/java/com/datecourse/db/InformationReportRepositoryTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/db/MemberProfileTest.java](../../src/test/java/com/datecourse/db/MemberProfileTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/db/PlaceCatalogRepositoryTest.java](../../src/test/java/com/datecourse/db/PlaceCatalogRepositoryTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/db/PlaceRepositoryPriceTest.java](../../src/test/java/com/datecourse/db/PlaceRepositoryPriceTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/db/SavedCourseRepositoryTest.java](../../src/test/java/com/datecourse/db/SavedCourseRepositoryTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/db/SavedCourseSchemaTest.java](../../src/test/java/com/datecourse/db/SavedCourseSchemaTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/demo/CameraMotionLiveTest.java](../../src/test/java/com/datecourse/demo/CameraMotionLiveTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/maintenance/ReplaceIntegratedDataTest.java](../../src/test/java/com/datecourse/maintenance/ReplaceIntegratedDataTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/model/PlaceDataModelTest.java](../../src/test/java/com/datecourse/model/PlaceDataModelTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/_01step_planner/CoursePlannerUiTest.java](../../src/test/java/com/datecourse/page/_01step_planner/CoursePlannerUiTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/_03step_saved/CourseMapCaptureLiveTest.java](../../src/test/java/com/datecourse/page/_03step_saved/CourseMapCaptureLiveTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/_03step_saved/SavedCourseMapTest.java](../../src/test/java/com/datecourse/page/_03step_saved/SavedCourseMapTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/account/MembershipCloseTest.java](../../src/test/java/com/datecourse/page/account/MembershipCloseTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/account/PlannerLoadingDialogTest.java](../../src/test/java/com/datecourse/page/account/PlannerLoadingDialogTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/account/StartupPanelTest.java](../../src/test/java/com/datecourse/page/account/StartupPanelTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/common/TmapSettingsDialogTest.java](../../src/test/java/com/datecourse/page/common/TmapSettingsDialogTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/common/controls/DialogThemeTest.java](../../src/test/java/com/datecourse/page/common/controls/DialogThemeTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/common/controls/ExpandableFilterPaneTest.java](../../src/test/java/com/datecourse/page/common/controls/ExpandableFilterPaneTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/common/map/CourseMapViewCaptureTest.java](../../src/test/java/com/datecourse/page/common/map/CourseMapViewCaptureTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/common/map/CourseMapViewLoadTest.java](../../src/test/java/com/datecourse/page/common/map/CourseMapViewLoadTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/common/map/MapEventBridgeTest.java](../../src/test/java/com/datecourse/page/common/map/MapEventBridgeTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/common/map/WalkingRouteLiveTest.java](../../src/test/java/com/datecourse/page/common/map/WalkingRouteLiveTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/common/map/WalkingRouteUiTest.java](../../src/test/java/com/datecourse/page/common/map/WalkingRouteUiTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/common/swing/CapsLockHintTest.java](../../src/test/java/com/datecourse/page/common/swing/CapsLockHintTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/page/common/swing/PasswordFieldTest.java](../../src/test/java/com/datecourse/page/common/swing/PasswordFieldTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/AppDiagnosticsTest.java](../../src/test/java/com/datecourse/service/AppDiagnosticsTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/CameraMotionPolicyTest.java](../../src/test/java/com/datecourse/service/CameraMotionPolicyTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/CandidateRecoveryServiceTest.java](../../src/test/java/com/datecourse/service/CandidateRecoveryServiceTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/CoursePreviewImageTest.java](../../src/test/java/com/datecourse/service/CoursePreviewImageTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/ExpandedPlaceFiltersTest.java](../../src/test/java/com/datecourse/service/ExpandedPlaceFiltersTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/ExternalBrowserTest.java](../../src/test/java/com/datecourse/service/ExternalBrowserTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/KakaoRouteServiceTest.java](../../src/test/java/com/datecourse/service/KakaoRouteServiceTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/LiveMapTestServer.java](../../src/test/java/com/datecourse/service/LiveMapTestServer.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/MapPageServerTest.java](../../src/test/java/com/datecourse/service/MapPageServerTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/NextPlaceConditionsTest.java](../../src/test/java/com/datecourse/service/NextPlaceConditionsTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/NextPlaceRadiusTest.java](../../src/test/java/com/datecourse/service/NextPlaceRadiusTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/NextPlaceServiceTest.java](../../src/test/java/com/datecourse/service/NextPlaceServiceTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/PilotPriceFlowTest.java](../../src/test/java/com/datecourse/service/PilotPriceFlowTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/PlaceBindingCatalogTest.java](../../src/test/java/com/datecourse/service/PlaceBindingCatalogTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/PlaceCalculationServiceTest.java](../../src/test/java/com/datecourse/service/PlaceCalculationServiceTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/PlacePresenterTest.java](../../src/test/java/com/datecourse/service/PlacePresenterTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/PlaceSearchServiceTest.java](../../src/test/java/com/datecourse/service/PlaceSearchServiceTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/PriceEvidenceTest.java](../../src/test/java/com/datecourse/service/PriceEvidenceTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/PublicPlaceFactsTest.java](../../src/test/java/com/datecourse/service/PublicPlaceFactsTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/RouteRangeValidatorTest.java](../../src/test/java/com/datecourse/service/RouteRangeValidatorTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/SavedCoursePresenterTest.java](../../src/test/java/com/datecourse/service/SavedCoursePresenterTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/TeamApiKeysTest.java](../../src/test/java/com/datecourse/service/TeamApiKeysTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/TmapSettingsTest.java](../../src/test/java/com/datecourse/service/TmapSettingsTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/TransitWalkingRouteServiceTest.java](../../src/test/java/com/datecourse/service/TransitWalkingRouteServiceTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/UserContextTest.java](../../src/test/java/com/datecourse/service/UserContextTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/WalkingPreferencesMapTest.java](../../src/test/java/com/datecourse/service/WalkingPreferencesMapTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [src/test/java/com/datecourse/service/WalkingRouteServiceTest.java](../../src/test/java/com/datecourse/service/WalkingRouteServiceTest.java) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |

</details>

<details><summary>자료·운영 도구·검사 · 32개</summary>

무엇: 자료 준비·대조·명시적 운영 작업. 사용: Python/Java/Gradle. 왜: 원본과 근거·기존 데이터 보존. 어떻게: 파일 입력 → 품질/근거/지문 → 모의·사전 확인 → 명시적 적용.

| 파일 | 핵심 역할 |
|---|---|
| [tools/README.md](../../tools/README.md) | 앱 기능·실행·현재 검증 안내 |
| [tools/build_district_expansion.py](../../tools/build_district_expansion.py) | 기존 전체 보존·구군 확장·품질 결과 생성 |
| [tools/build_public_demo.py](../../tools/build_public_demo.py) | 공공 장소·가격 근거·시연 스냅샷 생성 |
| [tools/catalog-ops/ExpansionAudit.java](../../tools/catalog-ops/ExpansionAudit.java) | 기준본·보호 데이터·명시적 적재·반복 확인 |
| [tools/catalog-ops/ExpansionAuditTest.java](../../tools/catalog-ops/ExpansionAuditTest.java) | 해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인 |
| [tools/catalog-ops/ExpansionRehearsal.java](../../tools/catalog-ops/ExpansionRehearsal.java) | Java 추천의 오프라인 모의 조건 검사 |
| [tools/catalog-ops/README.md](../../tools/catalog-ops/README.md) | 앱 기능·실행·현재 검증 안내 |
| [tools/catalog-ops/RouteResourceProbe.java](../../tools/catalog-ops/RouteResourceProbe.java) | 후속 후보 탐색의 별도 오프라인 실험 |
| [tools/catalog-ops/verify.init.gradle](../../tools/catalog-ops/verify.init.gradle) | 유지보수/보관 코드를 선택 검증에 연결 |
| [tools/check_rehearsal_places.py](../../tools/check_rehearsal_places.py) | 시연 장소의 공급사 대조 점검 |
| [tools/collect_busan_appeal_data.py](../../tools/collect_busan_appeal_data.py) | 공공 지정·교통·시설 근거 팩 생성 |
| [tools/place-data-pilot/.gitignore](../../tools/place-data-pilot/.gitignore) | 개인 설정·키·빌드/캐시의 Git 포함 제외 |
| [tools/place-data-pilot/README.md](../../tools/place-data-pilot/README.md) | 앱 기능·실행·현재 검증 안내 |
| [tools/place-data-pilot/bind.py](../../tools/place-data-pilot/bind.py) | 공공 장소와 외부 ID 대조·검토 표 생성 |
| [tools/place-data-pilot/contacts.py](../../tools/place-data-pilot/contacts.py) | 연락처·홈페이지의 동일 장소 대조 |
| [tools/place-data-pilot/evaluate.py](../../tools/place-data-pilot/evaluate.py) | 파일럿 연결 결과 평가 |
| [tools/place-data-pilot/matching.py](../../tools/place-data-pilot/matching.py) | 외부 동일 장소 대조 규칙 |
| [tools/place-data-pilot/pilot.py](../../tools/place-data-pilot/pilot.py) | Google 소규모 대조의 요청·원문 미보관 집계 |
| [tools/place-data-pilot/refine.py](../../tools/place-data-pilot/refine.py) | 보류 후보 검토 자료 구성 |
| [tools/place-data-pilot/team_keys.py](../../tools/place-data-pilot/team_keys.py) | 파일럿 도구의 팀 키 조회 |
| [tools/place-data-pilot/test_binding.py](../../tools/place-data-pilot/test_binding.py) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [tools/place-data-pilot/test_contacts.py](../../tools/place-data-pilot/test_contacts.py) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [tools/place-data-pilot/test_matching.py](../../tools/place-data-pilot/test_matching.py) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [tools/place-data-pilot/test_pilot.py](../../tools/place-data-pilot/test_pilot.py) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [tools/place-data-pilot/test_team_keys.py](../../tools/place-data-pilot/test_team_keys.py) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [tools/place_data_quality.py](../../tools/place_data_quality.py) | 이름·주소·좌표·기준일·중복 후보 보류 |
| [tools/supplement_place_prices.py](../../tools/supplement_place_prices.py) | 단위·메뉴·출처별 가격 보완 |
| [tools/test_build_public_demo.py](../../tools/test_build_public_demo.py) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [tools/test_check_rehearsal_places.py](../../tools/test_check_rehearsal_places.py) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [tools/test_collect_busan_appeal_data.py](../../tools/test_collect_busan_appeal_data.py) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [tools/test_district_expansion.py](../../tools/test_district_expansion.py) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |
| [tools/test_supplement_place_prices.py](../../tools/test_supplement_place_prices.py) | 해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인 |

</details>
