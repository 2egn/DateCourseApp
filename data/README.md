# 데이터 준비

| 위치 | 용도 | 현재 앱 연결 |
|---|---|---|
| `sql/` | DB·회원·장소·저장 코스 테이블 생성과 장소 적재 SQL | 사용 |
| `places/current/` | 현재 적재 대상인 `integrated_places.csv` 원본 | SQL을 통해 적재 |
| `places/public/busan/` | 부산 공공자료·메뉴·출처·원본 파일 | 미연결 |
| `places/pilot/` | 공공자료에서 따로 추출한 작은 검증용 범위 | 미연결 |
| `places/demo/` | 앱에 넣는 자체 ID 기반 9곳의 시연 목록 | 화면에서 선택 가능 |

현재 앱은 CSV가 아니라 DB를 조회한다. 장소 적재 프로그램은 프로젝트 최상단 기준 `data/sql/05_replace_integrated_data.sql`을 읽는다. CSV를 수정하는 것만으로 앱의 데이터가 바뀌지는 않는다.

## SQL 읽는 순서

1. `sql/01_create_database.sql`: 로컬 DB 생성이 필요한 경우에만 사용.
2. `sql/02_create_tables.sql`: 기본 테이블.
3. `sql/05_replace_integrated_data.sql`: 현재 장소 자료. 실행하면 데이터를 변경하므로 단순 열람과 구분.
4. `sql/06_membership.sql`: 회원 관련 수동 준비.
5. `sql/07_saved_courses.sql`: 저장 코스·방문 순서·장소 스냅샷 테이블.
6. `sql/08_saved_course_price_estimates.sql`: 07 적용 후 한 번 실행하는 가격 범위·미확인·출처 스냅샷 확장. 실제 DB에는 아직 실행하지 않음.

실제 실행 방법은 최상단 README의 DB 준비를 따른다. 폴더 정리 작업에서는 DB에 SQL을 실행하지 않았다.

공공·실험 자료는 기존 자료에 덮어쓰지 않는다. 각 자료의 README와 manifest에서 출처와 가격 미확인 여부를 확인한다.
