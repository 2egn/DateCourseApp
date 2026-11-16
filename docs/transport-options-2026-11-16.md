# 교통수단 확장 검토 · 2026-11-16

후속 구현은 [카카오 경로 연결](kakao-routing-2026-11-16.md)에 정리했다. 아래는 구현 전 검토 기록이다.

**도보는 유지하고 자동차를 추가할 만하다. 대중교통도 무료 쿼터가 있는 후보가 있어, 제외하기보다 소량 실측 후 판단한다.**
이번 검토는 코드와 공개 공식 문서 확인이다. 계정 권한·잔여 쿼터·부산 실제 응답은 확인하지 않았고 API 키나 결제 설정을 변경하지 않았다.

| 선택지 | 공식 안내 기준 | 판단 |
|---|---|---|
| 카카오 자동차 길찾기 | 무료 일 10,000건. 초과 유료 사용은 별도 정책 적용 | 현재 카카오 지도와 함께 쓰는 자동차 1차 후보 |
| 카카오 대중교통 경로 | 무료 일 1,000건. 카카오맵 무료 쿼터 적용 앱 조건 확인 필요. 유료 단가 10원/건 | 버스·지하철·도보 단계/좌표·시간·환승·요금 응답이 있어 우선 검증할 후보 |
| TMAP 자동차 | 공식 안내에서 경로탐색 무료 일 1,000건 제시. 개별 상품/키 적용은 확인 필요 | 기존 도보 HTTP 연동 경험을 재사용할 수 있는 대안 |
| TMAP 대중교통 | 상세·요약 각 일 10건 무료체험. 저장 후 24시간 이상 사용 불가 | 무료 시연에서도 반복 편집 호출에 여유가 작음 |
| ODsay 대중교통 Basic | 개인·학생·5인 이하 스타트업 대상 일 30회, 기간 제한 없는 무료 | 작은 검증에는 가능하나 여러 코스의 반복 편집에는 작음 |

무제한 무료라는 의미는 아니다. 일간/월간 한도, 앱 활성화 순서와 상품 권한은 실제 계정에서 확인해야 한다.

<details>
<summary>확인한 공식 문서</summary>

- [카카오 쿼터](https://developers.kakao.com/docs/ko/getting-started/quota): 자동차 일 10,000·대중교통 일 1,000 및 유료 쿼터 단가.
- [카카오맵 사용 방법/무료 적용 앱](https://developers.kakao.com/docs/ko/kakaomap/common): 첫 활성화 앱의 무료 쿼터와 플랫폼별 키 구분.
- [2026-07-21 기능·정책 변경 공지](https://devtalk.kakao.com/t/api-notice-on-new-kakao-map-api-features-and-free-quota-policy/150222): 대중교통·도보·자전거·정적 지도 API 추가 안내. 과거의 ‘카카오는 대중교통 REST API가 없다’는 전제로 판단하면 안 된다.
- [카카오맵 REST API](https://developers.kakao.com/docs/ko/kakaomap/rest-api): `/v2/routing/publictraffic`, 버스/지하철/도보 단계, 구간 좌표·시간·환승·요금 정보.
- [카카오 자동차 API](https://developers.kakaomobility.com/guide/navi-api/directions.html), [가격](https://developers.kakaomobility.com/price/): REST 키 인증, 상세 도로/안내와 시간·거리 등.
- [TMAP 공식 안내](https://tmapapi.tmapmobility.com/), [대중교통 약관](https://transit.tmapmobility.com/terms).
- [ODsay 요금/이용 대상](https://lab.odsay.com/contact/contact).

현재 JavaScript 지도 키가 있다는 사실만으로 REST 키/경로 API 권한까지 확인된 것은 아니다. 타사 지도 표시·출처 표기·캐시·미리보기 저장에 대한 이용 조건은 선택한 상품의 최신 정책으로 확인한다. 임베딩이나 사이트 화면 수집을 자유로운 대체 API로 가정하지 않는다.

</details>

<details>
<summary>현재 코드에서 필요한 변화</summary>

- [WalkingRouteService](../src/main/java/com/datecourse/service/WalkingRouteService.java): 현재 TMAP 보행자 endpoint에 연결되어 있다. HTTP 오류 처리와 중복 구간 캐시 방식은 재사용할 수 있다.
- [WalkingRoute](../src/main/java/com/datecourse/model/TravelRoute.java): 구간·좌표·거리·시간·안내는 공통 결과로 활용한다. 교통수단을 가진 공통 경로 결과와 간단한 서비스 인터페이스로 정리하고 도보/자동차 구현을 분리하는 정도가 적절하다.
- [PlannerMap](../src/main/java/com/datecourse/controller/PlannerMap.java): 조회·취소·캐시·늦은 응답 차단에 교통수단을 포함해야 한다. 모드 전환 시 이전 도보 응답이 자동차 화면을 덮으면 안 된다.
- [RoutePlan](../src/main/java/com/datecourse/model/RoutePlan.java): 코스 교통수단을 저장하고 기존 저장 코스는 도보로 호환한다. 현재 장소별 반경의 최대 2km와 NextPlaceService의 범위 검증은 도보 중심이므로 모드별 범위 정책을 함께 검토한다.
- 대중교통은 한 이동 구간 안에 도보·버스·지하철 단계가 다시 들어간다. 자동차와 같은 단일 선으로 의미를 지우기보다 간단한 단계 목록을 보존한다.

화면은 공통 조건의 ‘이동 방법: 도보 / 자동차’ 선택 하나로 시작한다. 대중교통은 실제 API 검증 후 추가한다. 초기에는 코스 전체가 같은 수단을 사용하게 하며 구간별 수단 선택은 실제 필요가 확인되면 확장한다.
모드 전환 후 최근·누적 이동시간과 경로 설명을 일관되게 갱신한다. 자동차는 운전 예상 시간이며 주차 탐색·주차 후 도보를 포함하지 않는다는 범위를 간단히 표시한다.
장소 예산과 교통비는 분리한다. 대중교통 요금은 인원 단위, 자동차 주차/통행/유류비는 다른 계산 단위이므로 현재 장소 가격처럼 무조건 인원수를 곱하지 않는다. 확인하지 못한 비용을 0원으로 채우지 않는다.

</details>

<details>
<summary>호출량과 검증 순서</summary>

현재처럼 연속 장소 구간을 조회하면 출발지+장소 3곳은 구간 3개다. 후보 30곳을 매 검색마다 모두 조회하는 설계보다, 추가된 코스 구간만 조회하고 동일 요청을 재사용하는 방식이 시연 규모에 적합하다. 공급사별 캐시 허용 범위 안에서만 재사용한다.

1. 현재 앱의 카카오 REST 키 종류·무료 적용 앱·경로 API 권한을 확인한다.
2. 광안리↔수영역, 수영역↔민락동 등 부산 2~3개 구간에서 카카오 대중교통의 성공/결과 없음·좌표·시간·환승·요금을 소량 검증한다. 문서의 기능 존재가 부산 실응답 품질까지 보장하지는 않는다.
3. 도보/자동차 코스 단위 선택 시안을 만들고 실제 자동차 경로와 시간을 표시한다. 기존 도보는 유지한다.
4. 대중교통 응답이 충분하면 같은 선택기에 추가하고 단계별 표시를 붙인다. 필요하면 ODsay/TMAP을 비교한다.

현재 코드는 변경하지 않았다. 이 문서의 교통수단 구조와 UI는 검토 제안이다.

</details>
