# 앱 코드 탐색

구현 동작과 기술의 연결은 [구현 참조 길잡이](../docs/implementation-reference/README.md), 클래스별 역할·대표 선언은 [전체 클래스 색인](../docs/implementation-reference/08-class-index.md), 설정·자원·도구·검사·자료까지는 [프로젝트 요소 색인](../docs/implementation-reference/09-project-index.md)에서 확인한다.

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
│  ├─ <a href="main/java/com/datecourse/maintenance/">maintenance/</a>                   DB 추가 구조 준비·자료 교체 명령
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
| DB 준비·일반 실행의 구조 확인 | [DatabaseSetup](main/java/com/datecourse/maintenance/DatabaseSetup.java) · [DatabaseSchema](main/java/com/datecourse/db/DatabaseSchema.java) |
| 로그인과 화면 연결 | [MembershipApp](main/java/com/datecourse/page/account/MembershipApp.java) |
| 출발 조건·확정 코스 | [CoursePlannerController](main/java/com/datecourse/page/_01step_planner/CoursePlannerController.java) |
| 조건·후보·편집 초안 | [CourseEditorController](main/java/com/datecourse/page/_02step_editor/CourseEditorController.java) |
| 저장 목록·이미지/지도 전환·준비 상태 | [SavedCourseController](main/java/com/datecourse/page/_03step_saved/SavedCourseController.java) |
| 장소 검색·추천 | [NextPlaceService](main/java/com/datecourse/service/NextPlaceService.java) · [PlaceSearchService](main/java/com/datecourse/service/PlaceSearchService.java) |
| 지도·이동 경로 | [CourseMapView](main/java/com/datecourse/page/common/map/CourseMapView.java) · [TravelRouteService](main/java/com/datecourse/service/TravelRouteService.java) |
| 장소·저장 코스 DB | [PlaceCatalogRepository](main/java/com/datecourse/db/PlaceCatalogRepository.java) · [SavedCourseRepository](main/java/com/datecourse/db/SavedCourseRepository.java) |
| 사용자 정보·목록 캐시 | [UserContext](main/java/com/datecourse/service/UserContext.java) |

</details>

대표 화면은 `_01step_planner` → `_02step_editor` → `_03step_saved` 순서로 읽습니다. JavaFX 컨트롤러와 FXML은 같은 페이지 경로를 사용하며, 공유 모델·서비스·DB는 페이지 밖에 둡니다.

실행은 프로젝트 루트에서 `.\gradlew.bat run`, 검사는 `.\gradlew.bat test`를 사용합니다. 단독 시연은 통합 앱 흐름과 구분합니다. 미연결 예제는 [archive/legacy-ui](../archive/legacy-ui/README.md)에 보관하며 기본 빌드에서 제외합니다.

[아키텍처](../docs/architecture.md) · [변경 지침](../AGENTS.md) · [페이지 이름 대응](../docs/site/content/페이지_구조와_이름_2026_11_24.md) · [패키지 구조 점검](../docs/site/content/패키지_통합과_구조_점검_2026_11_24.md)

지도 호버·포커스는 `map/map.js`, 왼쪽 상세 카드는 `map/itinerary-rail.js`에서 관리한다. 저장 지도 준비 상태는 `UserContext.isMapPrepared/markMapPrepared`에서 로그인 세션 안에만 기억한다. [상세 흐름](../docs/site/content/지도_선택과_저장_지도_준비_2026_11_24.md).

작은 창의 편집 합계는 `CourseEditorController.configureResponsiveSummary`, 저장 표는 `SavedCourseController.configureResponsiveDetails/layoutDetailTables`에서 배치한다. 알림 위치는 `PageFeedback`, 글자·선택·스크롤 표시는 공통 CSS와 지도 테마를 확인한다. [화면 가독성 안내](../docs/site/content/화면_가독성과_작은_창_배치_2026_11_24.md).

지도 이동의 렌더링 여유는 [map-viewport.js](main/resources/map/map-viewport.js)가 관리한다. 보이는 창과 내부 SDK 크기를 분리하고 `camera-motion.js`·`instant-zoom.js`·`map.js`에서 좌표를 보정한다. [설계와 검증 범위](../docs/site/content/지도_렌더링_여유_영역_2026_11_24.md).

마우스 입력 우선순위는 `map.js.pointerFrom`, 경로 상세의 키보드 접근은 `itinerary-rail.js.openRouteDetails`, 저장 이미지 확인 중 전환은 `SavedCourseController.previewLoading`에서 관리한다. [수정과 추가 UX 검토](../docs/site/content/마우스_키보드_입력_점검_2026_11_24.md).

컴파일·테스트·실행 JDK는 루트 [build.gradle](../build.gradle)의 Java 21 toolchain, 설치 검색은 [gradle.properties](../gradle.properties)에서 확인한다. 개인 JDK 경로는 공유 파일에 넣지 않는다.

로컬 JDK 21이 없을 때의 자동 다운로드 저장소는 루트 [settings.gradle](../settings.gradle)에 고정 버전으로 등록한다. 다운로드는 개인 Gradle 캐시에서 관리한다.

목록 키보드 조작은 `SavedCourseController.focusPlaceRow`·`CourseEditorController.configureEditorNavigation`, 상세 포커스 복귀는 `itinerary-rail.js.rememberDetailOpener`·`map.html.closeZoom`에서 관리한다. [현재 동작·검증 범위](../docs/site/content/키보드_조작과_포커스_복귀_2026_11_25.md).

도보 옵션 저장 이력은 `TmapSettings.preferencesVersion`, 지도 경로 유효성은 `CourseMapView.synchronizeWalkingRoute/isCurrentRouteRequest`에서 확인한다. [다른 지도와의 동기화](../docs/site/content/도보_설정과_지도_경로_동기화_2026_11_25.md).

지도 상세를 닫은 뒤 JavaFX 조작 위치로 돌아오는 연결은 `MapEventBridge.onDetailClosed` → `CourseMapView.restoreDetailFocus`다. 코스 초기화·폐기 때는 보관한 포커스와 이전 콜백을 무효화한다.

저장 경로 조회 버튼은 `CourseMapView.isRoutePending`의 실제 작업·대기 상태로 제어하고, 지연 이미지 확인 뒤 장소 이동은 `SavedCourseController.showMapPreview/focusPendingPlace`에서 연결한다. [후속 검증](../docs/review-2026-11-25.md).

추천의 전체 범위·중복 대표 선택은 `PlaceSearchService.nearby`, 직전 장소 기준 거리 재계산과 조건 필터는 `NextPlaceService.matching`이 맡는다. [결과 보존 대조·로컬 측정](../docs/review-2026-11-25.md#추천-후보의-중복-계산-정리).

JavaFX UI·WebView 유지보수 버전도 `build.gradle`의 `javafx.version`에서 고정한다. 현재 21.0.13.1이며 [같은 LTS 계열 비교 결과](../docs/site/content/JavaFX21_유지보수_패치_2026_11_25.md)를 참고한다.

실제 지도 선택 검사의 Windows 배율 재현은 `CourseMapCaptureLiveTest.verifyBufferedEdgeAndAttribution`과 `build.gradle`의 `coursePreviewLiveTest`에서 관리한다. [125%·150% 실행법과 확인 범위](../docs/site/content/지도_렌더링_여유_영역_2026_11_24.md).

추천 자원 탐색용 `RouteResourceProbe`는 `NextPlaceService`를 재사용하는 오프라인 유지보수 도구이며 기본 앱에는 포함하지 않는다. [활용 가능 요소·기존 연결 상태·탐색 실험](../docs/research/route-resource-exploration-2026-11-25.md)을 확인한다.

부산 전역 장소 선택은 기존 `tools/build_district_expansion.py`를 구·군별 정책 순서로 실행한다. [16개 구·군 정책·현재 자료·운영 검증](../data/places/expansion/busan-balanced-2026-11-25/README.md), 현재 파일 쌍은 [current.json](../data/places/expansion/current.json)을 확인한다.
