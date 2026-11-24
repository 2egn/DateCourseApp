# 앱 코드 탐색

앱 코드는 `com.datecourse` 아래에서 화면·모델·서비스·DB 역할로 나눕니다. 폴더·파일 이름을 누르면 해당 경로로 이동합니다.

<pre>
src/
├─ <a href="main/java/com/datecourse/">main/java/com/datecourse/</a>          실제 앱과 실행 도구
│  ├─ <a href="main/java/com/datecourse/app/">app/</a>                           통합 앱 시작
│  │  └─ <a href="main/java/com/datecourse/app/AppLauncher.java">AppLauncher.java</a>            실행 진입점
│  ├─ <a href="main/java/com/datecourse/page/">page/</a>                          페이지 구현과 공용 화면 동작
│  │  ├─ <a href="main/java/com/datecourse/page/_01step_planner/">_01step_planner/</a>             출발지·인원·전체 범위와 코스 저장
│  │  ├─ <a href="main/java/com/datecourse/page/_02step_editor/">_02step_editor/</a>              다음 장소 조건·후보 검색·코스 편집
│  │  ├─ <a href="main/java/com/datecourse/page/_03step_saved/">_03step_saved/</a>               저장한 코스·미리보기·다시 열기
│  │  ├─ <a href="main/java/com/datecourse/page/account/">account/</a>                     로그인·회원가입·내 정보
│  │  ├─ <a href="main/java/com/datecourse/page/admin/">admin/</a>                       관리자 회원 관리
│  │  ├─ <a href="main/java/com/datecourse/page/review/">review/</a>                      장소 정보 정정 제보 검토
│  │  └─ <a href="main/java/com/datecourse/page/common/">common/</a>                      공용 로딩·알림·입력 동작
│  │     ├─ <a href="main/java/com/datecourse/page/common/controls/">controls/</a>                 JavaFX 필터·스크롤·대화상자 부품
│  │     ├─ <a href="main/java/com/datecourse/page/common/map/">map/</a>                      WebView 지도·Java–JavaScript 연결
│  │     └─ <a href="main/java/com/datecourse/page/common/swing/">swing/</a>                    Swing 스타일·입력·대화상자 부품
│  ├─ <a href="main/java/com/datecourse/model/">model/</a>                         장소·가격·코스·경로·사용자 세션
│  ├─ <a href="main/java/com/datecourse/service/">service/</a>                       추천·경로 조회·가격 계산·표현·캐시
│  ├─ <a href="main/java/com/datecourse/db/">db/</a>                            회원·장소·저장 코스 저장소와 DB 설정
│  ├─ <a href="main/java/com/datecourse/auth/">auth/</a>                          비밀번호 해시 생성·검증
│  ├─ <a href="main/java/com/datecourse/maintenance/">maintenance/</a>                   명시적으로 실행하는 자료 교체 도구
│  └─ <a href="main/java/com/datecourse/demo/">demo/</a>                          단독 시연과 기존 검증·관리 명령
├─ <a href="main/resources/">main/resources/</a>                   화면 배치·스타일·지도 리소스
│  ├─ <a href="main/resources/com/datecourse/page/">com/datecourse/page/</a>           Java 컨트롤러와 같은 페이지 경로의 FXML
│  ├─ <a href="main/resources/map/">map/</a>                           지도 HTML·JavaScript·CSS
│  ├─ <a href="main/resources/style.css">style.css</a>                      앱 공용 화면 스타일
│  └─ <a href="main/resources/modern-scroll.css">modern-scroll.css</a>              공용 스크롤 스타일
└─ <a href="test/java/com/datecourse/">test/java/com/datecourse/</a>          검사 대상과 같은 패키지 구조의 자동 테스트
</pre>

설정 양식은 [팀 접속](main/resources/team-access.properties.example)·[DB](main/resources/db.properties.example)·[TMAP](main/resources/tmap.properties.example)을 참고합니다. 실제 설정값은 커밋하지 않습니다.

<details>
<summary>기능별로 먼저 읽을 클래스</summary>

| 기능 | 시작할 클래스 |
|---|---|
| 로그인과 화면 연결 | [MembershipApp](main/java/com/datecourse/page/account/MembershipApp.java) |
| 출발 조건·확정 코스 | [CoursePlannerController](main/java/com/datecourse/page/_01step_planner/CoursePlannerController.java) |
| 조건·후보·편집 초안 | [CourseEditorController](main/java/com/datecourse/page/_02step_editor/CourseEditorController.java) |
| 저장 목록·미리보기 | [SavedCourseController](main/java/com/datecourse/page/_03step_saved/SavedCourseController.java) |
| 장소 검색·추천 | [NextPlaceService](main/java/com/datecourse/service/NextPlaceService.java) · [PlaceSearchService](main/java/com/datecourse/service/PlaceSearchService.java) |
| 지도·이동 경로 | [CourseMapView](main/java/com/datecourse/page/common/map/CourseMapView.java) · [TravelRouteService](main/java/com/datecourse/service/TravelRouteService.java) |
| 장소·저장 코스 DB | [PlaceCatalogRepository](main/java/com/datecourse/db/PlaceCatalogRepository.java) · [SavedCourseRepository](main/java/com/datecourse/db/SavedCourseRepository.java) |
| 사용자 정보·목록 캐시 | [UserContext](main/java/com/datecourse/service/UserContext.java) |

</details>

대표 화면은 `_01step_planner` → `_02step_editor` → `_03step_saved` 순서로 읽습니다. JavaFX 컨트롤러와 FXML은 같은 페이지 경로를 사용하며, 공유 모델·서비스·DB는 페이지 밖에 둡니다.

실행은 프로젝트 루트에서 `.\gradlew.bat run`, 검사는 `.\gradlew.bat test`를 사용합니다. 단독 시연은 통합 앱 흐름과 구분합니다. 미연결 예제는 [archive/legacy-ui](../archive/legacy-ui/README.md)에 보관하며 기본 빌드에서 제외합니다.

[아키텍처](../docs/architecture.md) · [변경 지침](../AGENTS.md) · [페이지 이름 대응](../docs/site/content/페이지_구조와_이름_2026_11_24.md) · [패키지 구조 점검](../docs/site/content/패키지_통합과_구조_점검_2026_11_24.md)
