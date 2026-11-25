# 모델: 값과 책임을 왜 분리했는가

[흐름도로 보기](00-flow-map.md)

[전체 길잡이](README.md) · [모든 모델의 역할·선언](08-class-index.md)

| 모델 | 무엇을 담는가 | 분리한 구현상 효과 | 핵심 |
|---|---|---|---|
| Place | ID·분류·좌표·가격·상세 | 추천·지도·저장 사이에 같은 장소 단위 전달 | stableKey, sameIdentityAs |
| PlaceDetails | 주소·업종·전화·홈페이지·출처·속성·외부 연결 | 부가 정보와 근거를 함께 유지 | hasTag, featureState, withPublicFacts |
| PriceEstimate | 최소/최대·단위·근거·출처·자료일 | 미확인·무료·범위·추정을 구분 | finite, withinCap, estimated, exact |
| CostEstimate | 인원을 적용한 범위·미확인/추정 개수 | 아는 합과 모르는 부분을 함께 표시 | of, display, exactTotal |
| NextPlaceCondition | 다음 활동·상한·반경·유형·특징·개수 | 화면 조건을 한 객체로 전달 | 생성자 검증·5/10/20/30 표시 수 |
| RoutePlan | 출발지·인원·전체 범위·장소 순서·도착 반경·수단 | 편집 초안과 확정 코스 사이의 상태 전달 | List.copyOf, Map.copyOf |
| TravelRoute | 공급사 구간·좌표·거리·시간·단계 | 사용자 계획과 실제 조회 결과 구분 | Leg, TransitStage, movementSummary |
| SavedCourse | 코스 ID·소유 회원·계획·수정 시각 | 새 계획과 저장된 대상을 구분 | 회원 소유권 조건 |
| UserSession | 로그인 회원 식별 | 회원 데이터 요청의 소유 기준 | Repository의 member_id 조건 |
| SourceEvidence·PlaceAttributeFact | 출처·상태·근거·기준일·확인일 | 자료 기준일과 계산/확인일 구분 | origin·날짜 형식 확인 |
| FactState | AVAILABLE/UNAVAILABLE/UNKNOWN | 정보 없음과 불가능 확인을 구분 | 누락 시 UNKNOWN |
| ExternalPlaceLink | 외부 ID·대조 상태·실제 장소 여부 | ID가 있다는 것과 검토 승인을 구분 | usable, identityChanged |
| ExternalPlaceSnapshot | 외부 조회의 임시 상세 결과 | 외부 응답과 영구 공공 자료 구분 | 영구 JSON 저장 차단, openAt |

## record를 사용하는 이유와 정확한 범위

레코드는 관련 값을 한 단위로 전달하고 필드 재할당을 막는다. 내부 객체까지 자동으로 깊은 불변이 되는 것은 아니다. RoutePlan은 리스트·맵을 복사하고 PlaceDetails도 컬렉션을 복사해, 화면의 원본 리스트 변경이 이전 계획에 번지는 일을 줄인다. 유효성 검사는 생성자·서비스·저장소에 나누어 있으므로 record 문법이 모든 검증을 대신하지 않는다.

## 가격 논리

- `minimum=null`: 금액 미확인. 무료가 아니다.
- `minimum=maximum=0`: 숫자는 0원. 무료 필터는 유한 범위·추정 제외·UNKNOWN 근거 제외도 확인한다.
- `unit`: 1인·1잔·성인 관람권·단위 미확인. 합계는 현재 자료의 1인 1잔/성인 1매 같은 이용 가정에 따른 예상이다.
- `evidence`: 참고·추정·미확인 등을 구분한다. 한 숫자여도 추정일 수 있다.

**PriceEstimate.exact()는 ‘유한 범위의 양 끝이 같은 숫자’라는 뜻이며 실제 메뉴를 검증했다는 뜻이 아니다.** estimated()와 근거를 함께 확인한다. CostEstimate.exactTotal()은 추정·미확인·열린 범위가 있으면 단일 확정 합계를 거부한다.

| 예시 | 실제 해석 |
|---|---|
| 미확인 가격 | 기본 상한 판정에서는 제외. 사용자가 미확인 포함을 선택하면 별도 허용 |
| 8,000~12,000원, 상한 10,000원 | 최대 금액이 높으므로 기본 상한 검사에서 제외 |
| 4,118원/잔 통계 추정 | 한 숫자여도 추정이며 확정 매장 메뉴가 아님 |
| 근거 있는 0원 | 미확인과 구분하여 무료 표시 |
| 8,000원/인 + 미확인 1곳, 2명 | 계산 가능한 16,000원과 미확인 1곳을 함께 표시. 전체를 16,000원으로 확정하지 않음 |

인원은 연령 구분 없이 같은 이용 단위를 곱한다. 체류·주차·실제 주문량·교통비를 모두 포함한 총지출이라고 설명하지 않는다. 참고 가격의 자료일은 현재 영업 검증일과 다르다.

## 동일 장소 판정

stableKey는 공공 ID → placeId → recordId 순이다. 서로 다른 공공 ID는 이름·좌표가 같아도 자동 병합하지 않는다. 공공 ID가 한쪽에 없는 이전 자료에는 이름·좌표 일치의 호환 판정이 있다. 미정리 별칭이 남을 수 있지만 이름이 비슷한 다른 지점을 합치지 않는 경계를 둔 것이다.

소스: [Place](../../src/main/java/com/datecourse/model/Place.java), [상세](../../src/main/java/com/datecourse/model/PlaceDetails.java), [가격](../../src/main/java/com/datecourse/model/PriceEstimate.java), [비용](../../src/main/java/com/datecourse/model/CostEstimate.java), [계획](../../src/main/java/com/datecourse/model/RoutePlan.java), [경로](../../src/main/java/com/datecourse/model/TravelRoute.java), [임시 정보](../../src/main/java/com/datecourse/model/ExternalPlaceSnapshot.java).

MemberRepository 내부의 Profile/Login/Member 레코드는 model.Member·UserSession과 이름/역할이 다르다. 모든 회원 값을 하나의 모델로 사용한다고 설명하지 않는다.

<details><summary>값 해석에서 혼동하기 쉬운 부분</summary>

| 논점 | 현재 모델의 의미 |
|---|---|
| UNKNOWN인데 특징 필터에서 제외되는 이유 | 없는 것으로 확정한 것이 아니라, 사용자가 필수로 고른 특징은 AVAILABLE로 확인된 장소만 통과시키기 때문 |
| 1잔 가격과 인원 곱 | 현재 자료의 1인 1잔 가정. 모든 사람이 여러 잔·다른 메뉴를 주문하는 실제 비용을 보장하지 않음 |
| 무료와 공원 | 분류만으로 무료를 만들지 않음. UI의 무료 조건은 문화에 한정하며 가격 근거도 확인 |
| 수정된 장소와 저장 코스 | 현재 조회 정보와 저장 당시 스냅샷은 다름. 원본 변경이 과거 코스를 자동 수정하지 않음 |
</details>
