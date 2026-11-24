# 프로그램 구조·방향 재검토 · 2026-11-16

> 이 문서는 2026-11-16 시점의 검토 기록이다. 현재 구현과 정리 범위는 [최신 아키텍처](architecture.md)를 참고한다. 아래 행 수·반경 제한·분리 제안은 당시 기준이다.

**현재 모델과 Java 중심 구조는 유지할 만하다. 다음 단계는 기능 확대보다 편집 상태·이동 정책·릴리스 기준을 정리하는 것이다.**
공개 배포를 목표로 할 경우에는 DB/공급사 자격증명을 데스크톱에 전달하는 경계부터 바꿔야 한다.

2026-11-17 후속: 변경 커밋 분리와 독립 소스 검증을 완료했다. 현재 버전 기준은 [체크포인트](releases/checkpoint-2026-11-17.md)를 따른다. 아래는 검토 당시 기록이다.

검토 기준은 `79b2134` 위의 현재 작업 트리다. 866곳 확대·카카오 경로·교통수단 저장을 포함하며 해당 변경 중 상당수는 아직 커밋되지 않았다.
이번에는 실행 코드와 운영 DB를 변경하지 않았다. 최근 Java 336개·Worker 55개 테스트 및 실제 카카오 호출·지도 검증 기록을 확인했고, 동일 테스트를 다시 실행하지 않았다.

| 영역 | 판단 |
|---|---|
| 도메인 모델 | 근거·미확인·영구 자료·임시 경로 분리가 잘 잡혀 있다. 유지 |
| 사용자 상태 | UserContext가 사용자별 캐시와 수명을 소유하는 방향 적절 |
| 화면/서비스 | 분리는 진행됐지만 큰 컨트롤러와 구체 DB 의존이 남아 있음 |
| 교통수단 | 실제 경로는 확장됐으나 후보 정책은 여전히 도보 중심 |
| 데이터 확장 | 866곳의 단계적 확대는 가능. 부산 전체는 품질/조회/배치 보완 필요 |
| 공개 배포 | 데스크톱 직접 DB 접근과 공통 자격증명 공급은 서버 경계로 재설계 필요 |
| 재현·문서 | 코드·DB·Worker·문서의 현재 기준을 한 릴리스로 묶는 작업 필요 |

<details>
<summary>유지할 구조와 객체지향 활용</summary>

- [Place](../src/main/java/com/datecourse/model/Place.java), [PriceEstimate](../src/main/java/com/datecourse/model/PriceEstimate.java), [SourceEvidence](../src/main/java/com/datecourse/model/SourceEvidence.java)는 장소 자체와 가격/근거를 나눈다. 미확인·추정·참고값 구분은 추천 신뢰도의 토대다.
- [RoutePlan](../src/main/java/com/datecourse/model/RoutePlan.java)은 저장 가능한 사용자 계획, [TravelRoute](../src/main/java/com/datecourse/model/TravelRoute.java)는 현재 조회한 공급사 경로를 맡는다. 경로 API 응답을 코스 DB에 넣지 않는 경계는 유지한다.
- [InformationReportRepository](../src/main/java/com/datecourse/db/InformationReportRepository.java)는 정정을 원본 위의 별도 적용값으로 관리한다. 원본·관리자 정정·저장 코스의 과거 스냅샷을 혼합하지 않는 방향이 좋다.
- [UserContext](../src/main/java/com/datecourse/service/UserContext.java)는 사용자 정보와 저장 코스의 캐시/무효화를 맡는다. [BasePageController](../src/main/java/com/datecourse/controller/BasePageController.java)는 공통 UI 피드백에 한정한다. 사용자와 DB를 모든 폼의 베이스 클래스에 밀어 넣지 않은 현재 구성이 적절하다.
- [PlacePresenter](../src/main/java/com/datecourse/service/PlacePresenter.java), [SavedCoursePresenter](../src/main/java/com/datecourse/service/SavedCoursePresenter.java)가 표시 문구를 Java 쪽에서 생성한다. JavaScript는 지도·DOM 표시와 이벤트를 맡는 방향을 유지한다.
- 불변 record와 조합, 작은 서비스는 이 프로젝트에 잘 맞는다. 장소 종류마다 상속 클래스를 만들거나 모든 클래스 앞에 인터페이스를 추가할 필요는 없다. 실제 교체 지점인 저장소·외부 API·실행 작업에 경계를 두면 된다.

실행 소스의 단순 행 수(공백·주석 포함)는 Java 10,546 / JavaScript 2,060 / FXML 1,089 / CSS 537이다. 이 네 종류 합계에서 Java는 약 74%다. 품질 점수로 해석하지 않는다. Python은 수집/검증 도구이며 Java 앱이 Python을 실행해 핵심 기능을 처리하는 연결은 발견하지 못했다.

</details>

<details>
<summary>우선 정리할 책임과 정책</summary>

**1. 편집 상태를 화면에서 분리**

[ConditionController](../src/main/java/com/datecourse/controller/ConditionController.java)는 767행으로 사용자/페이지 전환·코스 저장·미리보기·지도·공통 조건을 함께 조율한다.
[RouteEditorController](../src/main/java/com/datecourse/controller/RouteEditorController.java)는 728행으로 초안·방문 순서·도착 반경·후보·필터·화면 상태·이동시간을 함께 관리한다.
행 수 자체가 결함은 아니지만 두 클래스의 변경 사유가 많고, 테스트도 내부 필드/메서드에 접근하는 경우가 많다.

일반적인 `CourseDraft` 또는 `CourseEditingSession` 한 객체에 장소 추가/삭제/순서·도착 반경·이동 방법·스냅샷 생성을 맡기는 것이 첫 분리 단위다. 컨트롤러는 입력값을 읽고 결과를 표시한다. 지도 캡처→이미지 생성→저장은 작은 `CourseSaveService`로 묶을 수 있다. 명령 버스나 이벤트 소싱은 필요하지 않다.

**2. 교통수단과 후보 정책 연결**

[NextPlaceService.matching](../src/main/java/com/datecourse/service/NextPlaceService.java)은 교통수단을 받지 않는다. 후보는 직선 범위와 가격/필터로 좁히고 직선거리 순서로 표시한다. 현재 자동차·대중교통 선택은 경로 조회에 반영되며 후보의 검색 반경·순위 자체를 바꾸지는 않는다.
200~2,000m 규칙이 `NextPlaceCondition`, `RoutePlan`, `NextPlaceService`, `SavedCourseRepository`, FXML과 복구 안내 등에 반복된다. 자동차 범위를 늘릴 때 한 화면의 Slider만 고치면 저장/검증과 어긋날 수 있다.

수단별 범위와 검증을 작은 `TravelPolicy`/`SearchPolicy`로 모으고 동일 규칙을 폼·검색·저장에서 사용한다. 직선 범위와 실제 이동시간은 계속 구분한다. 후보 전부에 경로 API를 호출하지 않고, 공간 검색 후 사용자가 확정한 구간을 조회하는 흐름을 우선 유지한다.

**3. 지도 비동기 조회와 지도 그리기 분리**

[PlannerMap](../src/main/java/com/datecourse/controller/PlannerMap.java)은 448행이며 WebView 생명주기·캡처·브리지·경로 요청·취소·응답 버전 검사를 소유한다.
[map.js](../src/main/resources/map/map.js)는 1,315행이며 출발점·범위·핀·경로·팝업을 함께 관리한다. camera/rail 분리는 이미 시작됐다.

Java의 경로 요청 상태를 `RouteRequestCoordinator` 같은 작은 객체로 옮기고, JS는 경로 렌더러를 별도 파일로 분리하는 정도가 적절하다. 기존 늦은 응답 차단 검사는 반드시 보존한다.
`walking*` 내부 이름을 바로 전부 바꾸기보다는 새 요청 경계부터 `route*`로 정리한다. 기존 지도 브리지 이름은 호환 용도로 명시한다.

**4. 저장소와 앱 조립의 경계**

`PlaceSource` 인터페이스가 있지만 `ConditionController.setPlaceRepository`는 구체 `PlaceRepository`를 받는다. 화면 테스트에서 DB 없는 저장소를 만들기 위해 구체 클래스를 대체하는 구조가 생긴다. 조회에는 `PlaceSource`를 받도록 좁히는 것이 단순하다.
`MemberRepository`는 회원 기능 외에 DB pool 소유와 장소·저장 코스·제보 저장소의 팩토리도 맡는다. 앱 시작부의 작은 `AppServices`가 pool/서비스를 조립하고 닫게 하면 각 저장소 이름과 책임이 명확해진다. DI 프레임워크 도입은 필요하지 않다.

</details>

<details>
<summary>데이터 모델과 부산 전체 확장</summary>

현재 866곳은 서면 386 + 수영구 480의 표본이다. [모의 테스트](suyeong-rehearsal-2026-11-16.md)에서 수영역/민락동의 좁은 범위 카페 표본 부족과 다른 이름의 동일 건물 기록을 확인했다. 이 문제는 컨트롤러 분리만으로 해결되지 않는다.

- 다음 데이터 확대는 지역별 분배와 검토된 동일 장소/별칭 연결을 우선한다. 주소만으로 공동 입점 업소를 자동 병합하지 않는다.
- 착한가격업소 표본의 편향과 문화 가격 미확인을 유지해서 설명한다. 저렴한 평균값으로 예산 충족을 보장하지 않는다.
- [PlaceDetails](../src/main/java/com/datecourse/model/PlaceDetails.java)는 legacy tags와 typed attributeFacts를 함께 갖는다. 현재는 typed 상태를 우선하는 읽기 규칙이 있어 곧바로 충돌한다고 볼 근거는 없다. 장기적으로는 typed facts를 기준으로 하고 legacy 변환을 입력 경계에 모으면 변경이 쉬워진다.
- `Place`의 안정적인 원본 ID와 검토된 동일 장소 관계는 서로 다른 책임이다. ID를 유지하되 별칭/이전/합병의 검토 이력을 별도로 연결한다.
- [PlaceCatalogRepository.findImported](../src/main/java/com/datecourse/db/PlaceCatalogRepository.java)는 전체 활성 payload를 읽고 해시/JSON을 복원한다. 도시 규모에서는 좌표·업종·지역 조회 컬럼과 인덱스로 먼저 후보를 줄인다.
- 적재는 기존 장소를 유지하는 전체 스냅샷 보호가 있다. 도시 전체 확대 전에는 JDBC 배치/임시 적재/최종 발행을 개선하고 실제 시간을 측정한다.
- 공개 시연 목록 386과 운영 866의 차이는 의도된 것이지만 기본 생성·적재 명령과 현재 운영본의 위치가 분리돼 있다. 현재 dataset/facts/policy/version을 가리키는 작은 활성 manifest가 있으면 운영 실수를 줄일 수 있다.

</details>

<details>
<summary>배포 경계와 운영 재현성</summary>

**공개 사용자에게 배포하기 전의 필수 변경**

[TeamApiKeys.database](../src/main/java/com/datecourse/service/TeamApiKeys.java)와 [DbSettings.load](../src/main/java/com/datecourse/db/DbSettings.java)는 공통 접속 키로 받은 DB 자격증명을 데스크톱에서 사용한다. Worker의 [app-keys](site/src/app-keys.mjs)는 같은 자격으로 공급사 키도 내려준다.
따라서 secret 등록은 Git/리소스에 키를 넣지 않게 하지만, 실행 중인 클라이언트가 자격증명을 받는다는 사실은 유지된다. 클라이언트 SQL의 소유자 조건만을 불특정 사용자에 대한 서버 권한 경계로 삼을 수 없다.

팀원만 사용하는 시연 범위를 명확히 유지한다면 당장 모든 UI를 갈아엎을 이유는 없다. 공개 서비스 단계에서는 Java 서버의 인증된 API가 회원별 소유권과 관리자 권한을 검증하고 DB/외부 키를 소유하도록 바꾸는 것이 적절하다. 데스크톱은 필요한 기능 API를 호출한다.

**운영 작업을 사용자 저장과 분리**

[SavedCourseRepository.persist](../src/main/java/com/datecourse/db/SavedCourseRepository.java)는 최초 저장에서 교통수단/미리보기 테이블을 준비한다. DDL을 트랜잭션 전에 실행한 것은 좋지만 실제 사용자 저장에 스키마 준비 책임이 들어 있다. 배포/초기화 명령으로 옮기면 앱 계정 권한과 실패 원인이 단순해진다.
현재 교통수단 저장은 H2 회귀 검증이 있으며 최근 실제 지도 검증은 운영 코스를 저장하지 않았다. 신규 수단의 실제 MySQL 저장→재실행→복원은 다음 통합 확인 항목이다.

**현재 릴리스 기준 확정**

로컬 HEAD는 `79b2134`이고 다수 최신 변경은 작업 트리에 있다. Worker의 REST 키 공급 코드는 이미 배포됐으며 DB 카탈로그는 866곳이다. 변경 묶음·스키마·데이터 manifest·Worker 버전·검증 결과를 한 체크포인트로 기록해야 재현과 복원이 쉬워진다. 원격 브랜치 최신 여부는 이번 검토에서 fetch해 확인하지 않았다.
문서의 일부 ‘최신’ 표시는 아직 2026-11-15 기준이다. 과거 구현 기록과 현재 릴리스 정보를 구분한다.

Swing 회원/관리 화면과 JavaFX 코스 화면은 EDT/FX 두 UI 스레드를 사용한다. 실제 동작을 유지하면서 공통 서비스와 로그인/종료 수명부터 정리하고, UI 통합은 팀 일정에 맞춰 진행한다. 과거 `controller.*` 예제와 현재 `com.datecourse.*`, `kr.ac.university.*` 경로도 사용 참조를 확인한 뒤 정돈한다.

</details>

<details>
<summary>권장 진행 순서와 완료 기준</summary>

1. **릴리스 체크포인트:** 현재 변경 범위와 데이터/스키마/Worker 기준을 정리하고 커밋 가능한 묶음으로 구분. 테스트 통과한 소스에서 같은 앱을 다시 실행할 수 있어야 한다.
2. **편집 상태 + 이동 정책:** CourseDraft·TravelPolicy를 먼저 분리하고 같은 사용자 흐름을 회귀 검증. 수단 전환·편집 재개·초기화·저장/복원이 정책을 잃지 않아야 한다.
3. **데이터 품질:** 지역 표본 균형과 동일 장소 검토를 보완하고 세 대표 지역에서 다시 코스 구성. 없는 정보는 계속 미확인으로 남긴다.
4. **도시 규모 처리:** 지역 조회·배치 발행과 실제 DB/UI 성능을 확인한 뒤 다음 구로 확대.
5. **공개 배포 단계:** 서버 인증·소유권·관리자 권한과 외부 API 프록시 경계를 마련. 이는 팀 시연 범위 밖으로 배포할 때의 선행 조건이다.

검증은 기존 336개 Java·55개 Worker 테스트를 바탕으로 편집 정책의 단위 테스트, 공급사 HTTP fixture, 저장 트랜잭션/복원, 핵심 지도 상호작용을 나눠 유지한다. 현재 `MapSearchTest`처럼 화면 전체에 집중된 테스트도 책임을 나눌 때 함께 분리하면 실패 원인을 파악하기 쉽다.

당분간 핵심 제품 흐름은 **조건 설정 → 장소 선택 → 실제 이동 확인 → 코스 저장·복원**으로 유지하는 것이 좋다. 지금은 새 태그/화면/교통수단의 추가보다 이 흐름의 데이터 신뢰도와 상태 일관성을 높일 때다.

</details>
