# 현재 아키텍처

현재 앱과 검사는 `com.datecourse` 아래에 있다. DB는 `db`, 회원 화면은 `page.account`, 관리 화면은 `page.admin`, 공용 부품은 `page.common.controls`(JavaFX)·`page.common.swing`에 둔다. `UserSession`은 `model`, `PasswordHash`는 `auth`다. [패키지 통합과 남은 구조 점검](site/content/패키지_통합과_구조_점검_2026_11_24.md)을 참고한다.

운영 DB는 빈 백업 8개 정리 후 **15개 테이블**이다. [역할별 DB 구조·장소 저장 위치·보존 검증](site/content/DB_구조와_빈_백업_정리_2026_11_24.md)을 참고한다.

구조 정리 기준 **9e59289 · 2026-11-23**, 가격 보완 반영 **2026-11-24**. Java 모델·서비스를 중심으로 Swing 인증 화면과 JavaFX 코스 화면을 연결한다. 지도 DOM과 이벤트는 WebView 안의 JavaScript가 처리한다.

일반 실행의 `initialize()`는 이제 구조 조회만 수행한다. 테이블 생성·회원 해시 열 확장·최초 관리자 등록은 `maintenance.DatabaseSetup`의 명시적 명령으로 분리했다. 기본 DB 생성과 일회성 데이터·범위 마이그레이션은 별도 SQL로 유지한다. [준비 범위·기존 명령·검증](site/content/DB_준비와_일반_실행_분리_2026_11_24.md)을 참고한다.

## 주요 책임

| 영역 | 클래스·파일 | 역할 |
|---|---|---|
| 앱 시작·인증 | `AppLauncher`, `MembershipApp`, `LoginUI`, `MemberRepository` | 시작 로딩, 인증, 사용자 화면 연결 |
| 공통 조건 | `CoursePlannerController` | 출발지·인원·확정 코스, 화면 전환·저장 조율 |
| 코스 편집 | `CourseEditorController` | 편집 초안, 조건·후보·방문 순서·구간 반경 |
| 저장 목록 | `SavedCourseController` | 사용자별 목록, 이미지·지도 전환, 명시적 경로 조회 |
| 지도 연결 | `CourseMapView`, `MapEventBridge` | WebView 수명주기, 지도 명령, 캡처·경로 요청과 최신 응답 검사 |
| 지도 표시 | `map.js`, `itinerary-rail.js`, `camera-motion.js` | 범위·핀·경로·상세 카드·카메라 상호작용 |
| 추천·정책 | `NextPlaceService`, `TravelMode`, `NextPlaceCondition` | 직선 범위·가격·필터와 이동수단별 반경 제한 |
| 모델·표현 | `Place`, `RoutePlan`, `TravelRoute`, `PlacePresenter`, `SavedCoursePresenter` | 장소 근거·계획·조회 경로와 표시 문구 분리 |
| 사용자·저장소 | `UserContext`, `SavedCourseRepository`, `PlaceCatalogRepository` | 사용자 캐시·무효화, 코스와 장소 영속화 |

<details>
<summary>클래스 내부에서 기능을 나눈 부분</summary>

- `CourseEditorController.initialize()`: 표시·필터 → 지도·이동수단 → 입력 → 후보 목록 → 조건 이벤트 → 코스 목록 → 패널 전환 순서로 설정한다.
- `SavedCourseController.initialize()`: 코스 목록·지도·정보 표·메시지를 각각 설정한다.
- `refreshMapNotice()`: `updatePreviewControls()`, `buildPreviewCaption()`, `buildMapNotice()`로 버튼과 문구의 책임을 구분한다.
- `CourseMapView.requestWalkingRoute()`: 요청 흐름을 유지하면서 `applyRouteSuccess()`·`applyRouteFailure()`로 결과 처리를 구분한다. 기존 `walking*` 이름은 다중 이동수단과의 호환 때문에 유지한다.

현재는 기존 필드·메서드 서명·FXML 연결을 유지하는 작은 정리가 기준이다. 별도 편집 세션이나 요청 조정 클래스 도입은 완료된 구현이 아니라 향후 선택지다.

</details>

<details>
<summary>상태와 비동기 처리에서 지킬 경계</summary>

- 편집 초안과 확정한 `RoutePlan`을 구분한다. 편집 완료 후 별도 저장한다.
- `TravelRoute`는 공급사 조회 결과다. 저장 미리보기의 직선 연결과 실제 도로 경로를 구분한다.
- 후보·목록·미리보기·경로 응답은 요청 번호와 현재 사용자·코스를 확인한 뒤 반영한다. 화면 전환·로그아웃 시 이전 응답을 무효화한다.
- 저장 PNG를 먼저 표시하고, 없으면 지도와 직선 연결, 지도 실패 시 로컬 연결도로 대체한다. 저장한 코스의 실제 경로는 사용자 요청 시에만 조회한다.
- `UserContext`가 사용자 캐시를 맡고 `BasePageController`는 공통 알림·로딩에 집중한다. DB·사용자 상태를 모든 화면의 공통 부모에 넣지 않는다.

</details>

<details>
<summary>지도·데이터·배포 정책</summary>

장소 핀은 상세 카드, 경로 호버는 요약, 클릭은 상세 이동 안내를 연다. 원 테두리에서만 전체·다음 장소 또는 편집 구간의 범위와 반경을 안내한다. 핀·경로 팝업이 우선하며 상시 범례는 사용하지 않는다.

가격의 참고·추정·미확인을 보존한다. 추천은 직선 거리 기준이며, 이동시간에는 체류·주차 등 제외 조건이 있으므로 수단별 안내를 따른다. 반경 제한은 `TravelMode`를 단일 기준으로 사용한다.

가격 보완 도구는 매장 메뉴와 지역·조사 통계를 분리한다. 통계는 `SIMILAR_ESTIMATE`로 유지하고 `PRICE_UPDATE` 적재는 가격 외 필드·장소 ID 변경을 차단한다. [선택 규칙과 자료](../data/places/pricing/2026-11-24/README.md)를 참고한다.

`PlaceCatalogRepository.synchronize()`는 갱신·비활성·이력 쓰기를 최대 200건씩 묶는다. 실행한 묶음도 전체 적재 트랜잭션의 일부이며 후속 오류 시 함께 롤백한다. 신규 장소·일반 데이터의 생성 키 연결은 개별 처리한다. `DatabasePool`은 MySQL 드라이버의 배치 전송을 활성화한다.

팀용 앱은 실행 중 DB 자격증명과 공급사 키를 받아 직접 사용한다. Git·JAR에 비밀값을 포함하지 않는 조치와 공개 배포용 서버 경계는 별개의 문제다.

</details>

검증 기준은 앱 Java 테스트 **372개와 선택 유지보수 검사 3개 통과**다. [11월 24일 전체 점검](review-2026-11-24.md)에서 Java 21 호환·미래 버전 차단·실제 DB 검증을 확인한다. 과거 분석·제안은 [11월 16일 구조 검토](architecture-review-2026-11-16.md), 실행·변경 지침은 [README](../README.md)와 [AGENTS](../AGENTS.md)를 참고한다.

화면 Java·FXML은 `com/datecourse/page/<페이지>` 경로를 공유한다. 공통 지도는 `page/common/map`, 공통 로딩·알림은 `page/common`에 둔다. [이전 이름·경로 대응](site/content/페이지_구조와_이름_2026_11_24.md)을 참고한다. `RoutePlan` 등 모델 이름과 JavaScript 콜백은 유지했다.

가짜 관리자·커뮤니티·추천 편집 화면과 전용 모델은 [archive/legacy-ui](../archive/legacy-ui/README.md)에 보관했다. 실제 회원 관리·정정 검토와 운영 데이터·SQL·유지보수 도구는 유지한다. 앱 Java 372개와 유지보수 검사 3개를 확인하며, 보관한 예제 전용 검사 2개는 이 수에 포함하지 않는다.
