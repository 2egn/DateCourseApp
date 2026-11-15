# 앱 코드 찾기

`main`은 앱이 사용하는 코드, `test`는 동작을 검사하는 코드다. Gradle이 자동으로 찾는 표준 구조를 사용한다.

| 기능 | `main/java/` 아래 위치 | 먼저 읽을 클래스 |
|---|---|---|
| 앱 시작 | `com/datecourse/app/` | `AppLauncher` |
| 로그인·회원가입 | `kr/ac/university/localdb/ui/` | `MembershipApp`, `SignupForm` |
| 회원·장소·코스 DB | `kr/ac/university/localdb/db/` | `MemberRepository`, `PlaceRepository`, `SavedCourseRepository` |
| 출발 조건·코스 편집 화면 | `com/datecourse/controller/` | `ConditionController`, `RouteEditorController` |
| 장소 추천·거리 검사 | `com/datecourse/service/` | `NextPlaceService`, `PlaceSearchService`, `RouteRangeValidator` |
| 지도와 도보 경로 | `com/datecourse/controller/`, `com/datecourse/service/` | `PlannerMap`, `MapBridge`, `MapPageServer`, `WalkingRouteService` |
| 화면 사이에 전달하는 데이터 | `com/datecourse/model/` | `Place`, `RoutePlan`, `SavedCourse` |
| 시연 목록·가격 범위 | `com/datecourse/service/`, `com/datecourse/model/` | `PilotPlaceRepository`, `PriceEstimate`, `CostEstimate`, `PlaceDetails` |
| 개별 화면 시연·예제 자료 | `com/datecourse/demo/` | `ConditionLauncher`, `FakeCourseData` |

현재 소스는 화면·처리·데이터 역할로 나뉘고, 회원/DB 부분은 별도 패키지에 있다. 기능별로 찾을 때는 위 표를 사용한다. `demo`나 이전 `MainApp` 대신 `AppLauncher`로 통합 앱을 실행한다.

## 화면 파일

- `main/resources/*-view.fxml`: JavaFX 화면 배치.
- `main/resources/style.css`: 앱 화면 스타일.
- `main/resources/map/`: 웹 지도 HTML·JavaScript·CSS.
- `main/resources/*.example`: 팀원이 복사해서 사용하는 설정 양식.

`test/java/`는 검사 대상과 같은 자바 패키지를 따라간다. 실행은 프로젝트 최상단에서 `.\gradlew.bat test`를 사용한다.
