# 통합 전 UI 예제 보관

통합 앱 `AppLauncher → MembershipApp`에서 호출하지 않는 예제 19개 Java 파일·FXML 3개·예제 전용 검사 2개를 보관했다. 기존 디렉터리 경로는 `src/` 아래에 그대로 남겼으며 [목록과 지문](inventory.json)으로 확인할 수 있다.

| 묶음 | 보관 이유 |
|---|---|
| 가짜 관리자·커뮤니티 화면 | `FakeMemberData`·`FakeReviewData`를 사용하며 실제 관리자와 연결되지 않음 |
| 추천 코스 편집 예제 | 현재 `CourseEditorController`와 별개의 `Course`·`CoursePlace` 모델 사용 |
| 예제 자료·모델·변환기 | 위 예제에서만 사용. 복원 시 함께 필요 |
| 저장 화면 단독 실행·장소 조회 예제 | 등록된 실행 작업·호출처가 없어 보관. 현재 화면과 유지보수 도구는 유지 |

<details>
<summary>삭제·유지 판단과 복원</summary>

삭제한 파일은 없는 `/fxml/Login.fxml`을 여는 `MainApp`, 참조 없는 픽셀 좌표 `FakeMapData`, 반경으로 가짜 수를 만드는 `PlaceCountService`다. 삭제 전 코드는 `879679e`에 있으며 삭제 목록도 inventory에 있다.

실제 회원 관리 `MemberAdminPanel`·정보 정정 검토, `runMapDemo`·`runPilotDemo`·카메라 시안과 공급사/DB 유지보수 도구는 사용처가 있어 유지했다. `ExternalPlaceSnapshot`은 외부 정보의 저장 금지 계약을 검사하는 모델이므로 유지했다. 운영 자료·SQL·과거 검증 기록도 정리 대상에서 제외했다.

복원은 필요한 묶음의 `src/main/java`·`src/main/resources`·`src/test/java` 파일을 프로젝트의 같은 경로에 복사하고 참조를 확인하는 방식이다. 관리자·커뮤니티·추천 편집 예제는 관련 가짜 자료·`Course`·`CoursePlace`·`Review`·변환기를 함께 검토해야 한다. 현재 실제 `Member` 모델·공통 스타일 등은 원래 프로젝트를 사용한다. 별도 실행 프로젝트는 아니다.

`RecommendedCourseMapperTest`와 `RecommendedCourseEditorUiTest`는 예제 전용 검사다. 후자는 기존 `CoursePlannerUiTest`에서 해당 검사 한 개를 추출했다. 기본 검사 수가 370개에서 368개로 줄어든 이유이며, 실제 코스 구성·편집·저장 검사는 유지한다.

이후 전체 점검에서 미래 버전 차단 회귀 검사 1개를 추가해 369개가 됐고, 문화 활동 전용 무료 필터 검사까지 추가한 현재 검사는 370개(유지보수 3개 포함)다. 보관 예제 검사는 별도 2개다.

프로젝트 루트에서 아래 명령을 명시적으로 실행하면 기본 앱·JAR에는 넣지 않고 별도 소스 집합에서 보관 코드를 컴파일하고 검사 2개를 실행한다. 이번 정리에서 두 검사 모두 통과했다.

현재 앱의 DB 패키지는 `com.datecourse.db`다. 보관 원본과 SHA는 유지하며, `stageArchivedUi`가 빌드 폴더의 검사 복사본에서 옛 DB 패키지 선언만 바꾼다. `PlaceQueryDemo`의 기존 package-private DB 접근을 검사하기 위한 처리이며 원본 복원 시에도 패키지를 맞춰야 한다.

```powershell
.\gradlew.bat -I archive/legacy-ui/verify.init.gradle verifyArchivedUi
```

</details>
