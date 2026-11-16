# 카카오 자동차·대중교통 경로 연결 · 2026-11-16

공통 조건과 코스 편집에서 **도보 / 자동차 / 대중교통**을 선택할 수 있다. 도보는 기존 TMAP을 유지하고 자동차·대중교통은 카카오 REST로 요청한다.
**구현·실제 부산 호출·지도 표시 검증 완료.** REST 키는 팀 Worker secret에 등록했고 공통 접속 키만으로 공급된다. 테스트용 로컬 REST 값은 비웠다.

<details>
<summary>사용과 키 설정</summary>

- 공통 조건의 ‘이동 방법’을 고르거나 편집 상단에서 바꾸면 현재 장소 순서로 경로와 최근/누적 이동 시간을 다시 조회한다.
- 자동차 시간은 주차·주차 후 도보·체류시간을 제외한다. 자동차/대중교통의 교통비는 장소 예상 비용과 별도다.
- 대중교통 상세 경로 팝업에는 도보·버스·지하철 단계와 응답에 포함된 환승·참고 요금을 보여준다. 미제공 요금을 0원으로 채우지 않는다.
- 경로 없음·권한 부족·호출 한도·불완전 응답은 오류로 안내한다. 다른 수단이나 직선 시간으로 조용히 대체하지 않는다.

실행 작업 폴더의 `.local/kakao-routing.properties`:

```properties
kakao.rest.api.key=카카오디벨로퍼스_REST_API_키
```

또는 환경변수 `KAKAO_REST_API_KEY`를 사용한다. 환경변수→로컬 설정→팀 키 서버의 선택 항목 `kakaoRest` 순이다. 기존 지도용 `kakaoJavascript`를 REST 키로 사용하지 않는다.
팀 키 서버는 기존 3개 항목에 선택적인 `kakaoRest`를 추가해 제공한다. Worker의 `KAKAO_REST_API_KEY` secret에 등록했고 클라이언트·서버 모두 이전 3개 항목 형식과 호환된다. 기존 실행 중인 앱은 공통 키 응답을 메모리에 유지하므로 새 빌드로 재실행한다. 키를 리소스나 JAR에 넣지 않는다.

실제 호출 검증:

```powershell
./gradlew verifyKakaoRoutes
```

광안공원→수영역→수변공원의 두 구간을 자동차/대중교통으로 조회한다. 정상 시 총 4회 호출하며 오류나 결과 없음이면 해당 수단의 검증은 실패로 종료한다. 출력은 성공 여부만 남기고 공급사 응답 원문을 파일로 보관하지 않는다.
2026-11-16에 로컬 키와 팀 secret 경로로 각각 위 검증이 통과했다. 다른 날짜·출발점·호출량에서의 응답까지 보장하는 것은 아니다.

</details>

<details>
<summary>주요 파일·클래스·함수</summary>

| 파일 / 함수 | 설명 |
|---|---|
| [TravelMode](../src/main/java/com/datecourse/model/TravelMode.java) | 교통수단과 사용자 안내 문구 |
| [TravelRoute](../src/main/java/com/datecourse/model/TravelRoute.java) | 기존 WalkingRoute를 공통 결과로 변경. 구간·경로·시간·안내·공급자·대중교통 단계를 임시 보유 |
| [TravelRouteService.route](../src/main/java/com/datecourse/service/TravelRouteService.java) | 도보는 기존 서비스, 자동차/대중교통은 카카오로 분기 |
| [KakaoRouteService](../src/main/java/com/datecourse/service/KakaoRouteService.java) | REST 요청, 자동차 도로 좌표/안내, 대중교통 최단 소요시간 경로와 단계 파싱. 별도 경로 캐시 없음 |
| [KakaoRouteSettings](../src/main/java/com/datecourse/service/KakaoRouteSettings.java) | Java 전용 REST 키 로드. 키의 문자열 표현은 가림 처리 |
| [PlannerMap](../src/main/java/com/datecourse/controller/PlannerMap.java) | 수단을 요청 식별에 포함. 전환 시 취소하고 이전 수단의 늦은 응답을 무시 |
| [ConditionController](../src/main/java/com/datecourse/controller/ConditionController.java), [RouteEditorController](../src/main/java/com/datecourse/controller/RouteEditorController.java) | 코스의 수단과 폼 선택을 동기화하고 수단별 시간/오류/설정 버튼 표시 |
| [RoutePlan](../src/main/java/com/datecourse/model/RoutePlan.java) | 사용자 선택 `travelMode` 추가. 과거 생성자/누락 JSON 필드는 WALKING으로 호환 |
| [SavedCourseRepository](../src/main/java/kr/ac/university/localdb/db/SavedCourseRepository.java) | 장소 스냅샷과 선택 수단을 같은 트랜잭션으로 저장. 기존 코스와 새 테이블이 없는 DB의 읽기는 도보로 처리 |
| [12_saved_course_transport.sql](../data/sql/12_saved_course_transport.sql) | 사용자 선택만 별도 테이블에 보관. 저장 시 최초 초기화하며 기존 코스 테이블을 변경하지 않음 |
| [KakaoRouteProbe](../src/main/java/com/datecourse/demo/KakaoRouteProbe.java) | 키 등록 후 소량 실제 호출 검증 |

기존 지도 페이지의 `showWalkingRoute` 브리지 이름은 호환을 위해 유지하되 전달 결과는 공통 모델이다. 긴급한 전면 명칭 변경이나 복잡한 서비스 계층은 추가하지 않았다.

</details>

<details>
<summary>보존·오류·호출 정책</summary>

저장 코스에는 공공/사용자 장소 스냅샷과 선택 교통수단만 저장한다. 카카오 응답의 선·시간·요금·단계와 REST 키는 DB에 넣지 않는다. 코스를 다시 열면 경로는 다시 조회한다.
기존 코스 미리보기는 장소 좌표를 직선으로 잇는 요약 방식이며 공급사 경로를 영구 이미지로 저장하는 용도로 바꾸지 않았다. 도보·자동차 선택만 바꿔도 같은 직선 요약 이미지 내용은 유지된다.

카카오 경로는 현재 안내 화면에서만 보유하며 다른 요청에 재사용하는 캐시를 추가하지 않았다. 기존 TMAP 도보 캐시는 별도 동작이다. 후보 검색마다 모든 가게의 경로를 요청하지 않고 확정된 연속 구간만 조회한다. 코스 추가/순서/수단 변경 시 조회하므로 장소가 늘수록 호출 수는 증가한다.

현재 장소 검색의 직선 반경 상한 2km는 유지한다. 자동차 경로의 실제 도로 길이를 2km로 자르는 것은 아니다. 도시 전체 운전 탐색을 위한 반경 정책 확대는 별도 작업이다.

공식 근거:

- [자동차 API](https://developers.kakaomobility.com/guide/navi-api/directions.html): `/v1/directions`, REST 키 인증.
- [대중교통 API](https://developers.kakao.com/docs/ko/kakaomap/rest-api): `/v2/routing/publictraffic`, WGS84 좌표, 이동 단계.
- [현재 안내 중 임시 보유에 대한 공식 답변](https://devtalk.kakao.com/t/api/151736): 현재 안내 동안 보유하고 종료 후 폐기하는 구조와 재사용 캐시 구분.
- [단기 재사용 캐시에 대한 공식 답변](https://devtalk.kakao.com/t/api-totaltime-caching/151653).

</details>

<details>
<summary>검증</summary>

Java 전체 336개·Worker 55개 테스트 및 JAR 빌드 통과.
추가 검증은 자동차 좌표/안내 파싱, 대중교통 단계/환승/요금·경로 공백 보존, 경로 없음·잘못된 수치/좌표 거부, 인증 헤더와 비밀값 보호, 로컬 키 우선순위, 수단 전환 시 오래된 응답 차단, 저장 수단의 수정/복원과 이전 코스 호환을 포함한다.
공급사 테스트 응답은 사양을 바탕으로 작성한 가상 fixture이며 실제 API 응답을 저장한 자료가 아니다.

실제 검증:

- 로컬 REST 키로 자동차/대중교통 각각 두 구간의 좌표·시간 응답 확인.
- Worker secret 등록 및 키 공급 코드 배포 후, REST 환경변수·로컬 REST 설정 없이 공통 접속 키만으로 같은 검증 재통과.
- 운영 FXML/controller의 읽기 전용 검증 창에서 광안공원 출발→나쓰커피→스타벅스 밀락더마켓을 표시. 자동차에서 대중교통으로 전환했을 때 실제 선과 누적 시간이 갱신됐다.
- 대중교통 경로 팝업에서 버스·도보 단계, 환승, 참고 요금 표시 확인. API 응답 원문·좌표·시간·요금은 검증 파일에 보관하지 않았다.
- 테스트용 로컬 REST 속성 값을 비웠다. 실제 키는 Cloudflare secret에 보관하며 Java 프로세스의 인증된 요청으로만 받아 쓴다.
- Worker 배포 버전: `7266c63e-75db-4b39-9653-dae443a00352`. 기존 문서 자산 변경은 없었다.

운영 DB에 대한 코스 변경·삭제나 새 저장은 수행하지 않았다. 새 저장 테이블은 운영에서 첫 저장 시 준비된다.

</details>
