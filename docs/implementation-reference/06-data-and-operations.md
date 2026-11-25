# 원본·카탈로그·가격·운영 도구

[짧은 전체 흐름](00-flow-map.md) · [이 기능의 코드 흐름](10-flow-details.md#data)

[전체 길잡이](README.md) · [현재 파일 쌍](../../data/places/expansion/current.json) · [자료 안내](../../data/README.md)

```mermaid
flowchart LR
 A[공공 원본·출처·SHA] --> B[정규화 CSV]
 B --> C[구군 정책·품질 보류·기존 보존]
 C --> D[전체 장소]
 D --> E[공공 속성·교통 근거]
 E --> F[단위·근거별 가격 보완]
 F --> G[Java 모델·DB 기준본 대조]
 G --> H[명시적 적재·보호·반복 검증]
 H --> I[PlaceRepository → 추천·지도]
```

| 요소 | 무엇을 구현했는가 | 필요한 기능·사용 이유와 처리 |
|---|---|---|
| build_public_demo.py | 공공 원본을 장소 모델로 구성 | SHA·ID·업종·좌표·가격 근거를 관리하고 이전 객체 보존 |
| place_data_quality.py | 보류 ID·근거 생성 | 이름/주소·좌표·기준일·중복 후보 검사. 비슷한 이름을 자동 병합하지 않음 |
| build_district_expansion.py | 구군 단계 확장 | 직전 전체 기준본·500m 격자·업종 순환·누적 상한 |
| collect_busan_appeal_data.py | 지정·교통 근거 연결 | 원본 기준일과 계산일 구분. 직선거리는 실제 출입구 접근성과 다름 |
| supplement_place_prices.py | 매장·지역·조사 가격 보완 | ID·주소·이름·메뉴·단위 조건, 추정은 추정으로 보존 |
| PublicPlaceFacts.read | 장소·속성 파일 쌍 검사 | 지문·ID·태그·기준일 확인 후 타입 있는 속성 연결 |
| PlaceCatalogRepository.synchronize | 출처·버전·payload·이력 적재 | 확장/가격 전용 보존 정책·트랜잭션·반복 처리 |
| ExpansionAudit | 기준본 대조·운영 검증 | 기본 preflight 읽기 전용, 명시적 apply에서 번호·보호·반복 확인 |
| ExpansionRehearsal | Java 추천 모의 검사 | 반경·가격·후보를 오프라인 확인. 실제 경로/API/DB와 구분 |
| RouteResourceProbe | 후속 후보 탐색 실험 | 현재 앱과 별도. 실험을 기능 구현 완료로 세지 않음 |
| place-data-pilot | Google 장소 ID·연락처 대조 | 동일 장소·방문 가능성·가격 확인을 구분. 파일럿 연동은 main 실시간 기능이 아님 |

운영은 **16개 구군 5,628곳**: 식사 2,800·카페 2,260·문화 568. 직전 2,138곳에 3,490곳을 더하고 기존 장소·번호·회원·저장 코스 등을 보존했다. 내장 기본 공공 시연 386곳·별도 파일럿 9곳과 구분한다.

미수록 11개 구군에 각 300곳, 중구에 190곳을 보완했다. 지역별 같은 개수·인구 비례·전체 업소 대표 표본을 뜻하지 않으며 동/읍 안의 편중도 남는다. 기존 공공 스냅샷 재사용으로 현재 영업·실제 메뉴·공급사 동일 장소를 모두 새로 검증한 자료는 아니다.

가격은 참고 23·추정 3,092·미확인 2,513곳이다. 공원·도서관을 무료로 추론하지 않는다. 착한가격은 공공 지정 연결이지 현재 모든 메뉴가 저렴하다는 점수나 일반 시장 평균이 아니다.

## 실제 사용과 지원 도구 구분

- PlaceRepository는 운영 공공 카탈로그 우선이며 없을 때 이전 integrated_places.csv 출처의 DB 행을 읽는다.
- 파일 수정만으로 운영 DB가 바뀌지 않는다. 준비·적재는 별도 작업이다.
- DB 준비·원본 교체·적재·비밀번호 재설정은 명시적 도구이며 일반 앱 버튼 흐름이 아니다.
- archive/legacy-ui는 기본 빌드에서 제외한 보관 원본이다.
- 신규 생성 ID 연결은 개별 DB 처리가 남아 있다. 최근 적재·검증 전체 약 49분을 순수 삽입 시간으로 해석하지 않는다. 사후 검증 실패와 이미 반영됐는지도 구분해야 한다.

근거: [자료 도구](../../tools/README.md), [카탈로그 도구](../../tools/catalog-ops/README.md), [전역 확장](../../data/places/expansion/busan-balanced-2026-11-25/README.md), [가격 규칙](../../tools/supplement_place_prices.py), [분류·미사용 필드](../research/place-classification-unused-fields-2026-11-25.md).
