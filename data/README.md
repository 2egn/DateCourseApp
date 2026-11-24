# 데이터 준비

운영 공공 카탈로그는 **2,138곳**이며 2026-11-25에 [중구 98곳 추가와 신규 가격 보완](places/expansion/jung-gu-2026-11-25/README.md)을 검증했다. 기존 2,040곳은 그대로 보존했다. 현재 전체 장소·속성 쌍과 지문은 [expansion/current.json](places/expansion/current.json), 가격 선택 규칙과 운영 검증은 [가격 보완 기록](places/pricing/2026-11-24/README.md)을 확인한다. [확장 기록](../docs/busan-core-expansion-2026-11-23.md)은 그 이전 단계다. 앱은 CSV가 아니라 DB를 읽는다.

고정된 미래 버전 시각은 [메타데이터 정정](places/pricing/clock-correction-2026-11-24/README.md)으로 바로잡았다. 장소·가격 payload와 태그는 동일하며, 현재 속성 파일과 버전은 current.json을 따른다.

| 위치 | 용도 | 앱 연결 |
|---|---|---|
| `sql/` | 기본 스키마와 단계별 마이그레이션·이전 적재 SQL | DB 준비·유지보수 |
| `places/expansion/` | 기존 장소를 보존한 전체 확장 자료·속성·검증 기록 | 운영 공공 카탈로그 |
| `places/pricing/` | 통계·매장 가격 선택 근거와 가격 보완 스냅샷·검증 | 현재 기준본의 가격 보완 |
| `places/demo/` | 내장 공공 시연 386곳과 별도 검증용 9곳 | 시연 목록 선택 |
| `places/public/busan/` | 장소·메뉴·출처·지문을 가진 공공 원본 | 확장 자료 생성에 사용 |
| `places/current/` | 이전 `integrated_places.csv`와 적재 원본 | 공공 카탈로그가 없을 때의 이전 DB 경로 |
| `places/pilot/` | Google 연결·연락처 등 소규모 검증 기록 | 전체 장소의 최신 검증으로 해석하지 않음 |

`PlaceRepository`는 공공 카탈로그가 있으면 우선 사용하고, 없을 때 이전 일반 데이터 테이블을 조회한다. 파일 수정만으로 운영 DB가 바뀌지 않는다. 앱이 캐시 중인 장소 목록은 적재 후 다시 불러와야 한다.

<details>
<summary>스키마와 적재 순서</summary>

기본 DB는 `01_create_database.sql`, `02_create_tables.sql`, 회원·코스는 `06_membership.sql`, `07_saved_courses.sql`, `08_saved_course_price_estimates.sql`을 따른다. 정보 정정은 `09_place_information_reports.sql`, 카탈로그·PNG·교통수단·범위 확장은 `10_place_catalog.sql`부터 `13_expand_course_ranges.sql`까지 기능 문서와 함께 확인한다.

`05_replace_integrated_data.sql`은 이전 자료 교체 작업이며 현재 공공 카탈로그 확장 명령과 다르다. 운영 SQL은 단순 문서 열람이나 테스트 중 자동 실행하지 않는다.

확장 자료는 `PublicPlaceImport`와 `tools/catalog-ops`로 준비·검증한다. `EXPANSION`은 현재 전체 장소가 누락된 입력을 거부한다. 다음 확대의 기준본은 `places/expansion/current.json`이 가리키는 전체 자료를 사용한다.

</details>

공공 원본은 체크섬과 LF 형식을 보존한다. 가격 미확인·추정·참고를 구분하며, 자료 생성일을 실제 영업·지도·가격 검증일로 바꾸지 않는다.

## 명시적 DB 준비

일반 앱 시작·제보·코스 저장·카탈로그 적재에서는 테이블을 생성하지 않는다. `initialize()`라는 기존 메서드 이름은 유지하지만 구조 확인만 한다. 관리자는 `maintenance.DatabaseSetup`을 명시적으로 실행한다.

1. 대상 DB를 선택하고 `01`·`02`·`07` 기본 구조와 `08` 가격 스냅샷 확장을 먼저 준비한다. 기존 DB라면 이미 적용된 SQL을 반복 실행하지 말고 필요한 마이그레이션만 확인한다.
2. 이전 반경 제한이 남아 있다면 `13_expand_course_ranges.sql`을 별도로 적용한다. `setupDatabase`가 `08`·`13`의 일회성 ALTER를 대신하거나 반복 실행하지 않는다.
3. 추가 구조가 필요한 DB에서만 프로젝트 루트의 아래 명령을 실행한다. 현재 운영 DB에 자동 실행한 것은 아니다.

```powershell
.\gradlew.bat setupDatabase
```

| 범위 | 작업 |
|---|---|
| 회원·관리자 | 해시 열이 짧을 때만 255자로 확장, 관리자 테이블과 없는 초기 계정만 준비. 기존 비밀번호는 유지 |
| 제보·카탈로그 | `09`·`10` 구조와 카탈로그 잠금 행만 준비. 장소 자료 적재·정정은 수행하지 않음 |
| 이미지·이동수단 | `11`·`12` 구조만 준비. 기존 코스·이미지·이동수단은 유지 |

부분 준비는 `setupDatabase --args="--membership"`, `--reports`, `--catalog`, `--previews`, `--transport`로 선택한다. 이전 `setupInformationReports`·`setupCoursePreviews` 명령은 새 유지보수 진입점으로 연결한다. 옛 `demo` 클래스는 직접 실행 경로를 위한 호환 위임만 남긴다.

DB 생성, 원본 자료 교체(`05`), 장소 적재, 관리자 비밀번호 재설정은 별도 작업이다. MySQL DDL은 전체 롤백을 보장하지 않으므로 실패 시 일부 구조가 준비됐을 수 있다. 상태 확인 후 같은 준비 명령을 다시 실행할 수 있도록 기존 계정·코스를 보존한다.
