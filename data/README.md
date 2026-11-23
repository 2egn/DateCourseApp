# 데이터 준비

운영 공공 카탈로그는 **2,040곳 · 2026-11-23 검증**이다. 현재 전체 장소·속성 쌍과 지문은 [expansion/current.json](places/expansion/current.json), 상세 결과는 [확장 기록](../docs/busan-core-expansion-2026-11-23.md)을 확인한다. 앱은 CSV가 아니라 DB를 읽는다.

| 위치 | 용도 | 앱 연결 |
|---|---|---|
| `sql/` | 기본 스키마와 단계별 마이그레이션·이전 적재 SQL | DB 준비·유지보수 |
| `places/expansion/` | 기존 장소를 보존한 전체 확장 자료·속성·검증 기록 | 운영 공공 카탈로그 |
| `places/demo/` | 내장 공공 시연 386곳과 별도 검증용 9곳 | 시연 목록 선택 |
| `places/public/busan/` | 장소·메뉴·출처·지문을 가진 공공 원본 | 확장 자료 생성에 사용 |
| `places/current/` | 이전 `integrated_places.csv`와 적재 원본 | 공공 카탈로그가 없을 때의 이전 DB 경로 |
| `places/pilot/` | Google 연결·연락처 등 소규모 검증 기록 | 전체 장소의 최신 검증으로 해석하지 않음 |

`PlaceRepository`는 공공 카탈로그가 있으면 우선 사용하고, 없을 때 이전 일반 데이터 테이블을 조회한다. 파일 수정만으로 운영 DB가 바뀌지 않는다. 앱이 캐시 중인 장소 목록은 적재 후 다시 불러와야 한다.

<details>
<summary>스키마와 적재 순서</summary>

기본 DB는 `01_create_database.sql`, `02_create_tables.sql`, 회원·코스는 `06_membership.sql`, `07_saved_courses.sql`, `08_saved_course_price_estimates.sql`을 따른다. 카탈로그·PNG·교통수단·범위 확장은 `10_place_catalog.sql`부터 `13_expand_course_ranges.sql`까지 기능 문서와 함께 확인한다.

`05_replace_integrated_data.sql`은 이전 자료 교체 작업이며 현재 공공 카탈로그 확장 명령과 다르다. 운영 SQL은 단순 문서 열람이나 테스트 중 자동 실행하지 않는다.

확장 자료는 `PublicPlaceImport`와 `tools/catalog-ops`로 준비·검증한다. `EXPANSION`은 현재 전체 장소가 누락된 입력을 거부한다. 다음 확대의 기준본은 `places/expansion/current.json`이 가리키는 전체 자료를 사용한다.

</details>

공공 원본은 체크섬과 LF 형식을 보존한다. 가격 미확인·추정·참고를 구분하며, 자료 생성일을 실제 영업·지도·가격 검증일로 바꾸지 않는다.
