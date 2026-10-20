# DateCourseApp

위치·반경·예산·인원·분류를 기준으로 데이트 코스를 추천하는 Java 데스크톱 앱입니다. 현재는 프로토타입을 개발 중이며, 회원가입·로그인에서 조건 입력 화면까지 연결되어 있습니다.

## 현재 구현 상태

| 기능 | 상태 |
|---|---|
| 시작 로딩 | 백그라운드 DB 초기화, 성공 시 로그인 전환, 실패 시 재시도·종료 |
| 회원가입 | 이메일·비밀번호·확인·이름·전화번호 순서로 입력 및 DB 저장 |
| 로그인 | 실제 DB 인증, 비활성 회원 차단, 조건 입력 화면으로 전환 |
| 아이디 저장 | 체크 후 로그인 성공 시 한 개 저장, 다음 실행 시 자동 입력 |
| 사용자 세션 | 회원 ID·이름을 보관하고 조건 입력 컨트롤러에 전달 |
| 장소 적재 | 6,139건을 최대 250건씩 묶어 INSERT, 진행 상황 표시 |
| 장소 조회 | PlaceRepository.findAll()에서 실제 데이터를 Place 목록으로 반환 |
| 거리·비용 계산 | 미터 단위 거리·반경 필터, 1인당 가격 합계 × 인원 계산 |
| 지도 | 임시 화면. 클릭값은 위도·경도가 아닌 픽셀 좌표 |
| 주변 장소 수 | 임시 계산. 실제 장소 조회·거리 필터와 연결 필요 |
| 추천·코스 저장 | 화면은 있으나 실제 추천 생성 및 회원별 DB 저장·조회는 미연결 |
| 커뮤니티·관리자 | 기존 화면·처리 코드가 남아 있으나 통합 흐름은 미완성 |

현재 MemberRepository.login()은 일반 회원 인증만 처리합니다. app_admins 초기화와 관리자 화면 코드는 남아 있지만 관리자 인증 분기가 없어, 관리자 계정으로 화면에 진입하는 기능은 동작하지 않습니다.

## 개발 환경과 실행

- 빌드·실행 기준: Gradle Wrapper
- 현재 빌드 검증 환경: Temurin JDK 25, Gradle 9.6.0
- 화면: Swing 및 JavaFX 21 (controls, fxml, web, swing)
- DB: MySQL 호환 서버. 로컬 MySQL 또는 Aiven for MySQL 접속 정보 사용
- 주요 라이브러리: MySQL Connector/J, HikariCP, Jackson
- 테스트: JUnit 5, H2

다른 JDK를 사용할 때는 JavaFX 21과 Gradle Wrapper의 호환성을 확인해야 합니다. 기존 pom.xml은 남아 있지만 현재 통합 앱의 실행 안내는 Gradle을 기준으로 합니다.

프로젝트 루트에서 실행합니다.

~~~powershell
.\gradlew.bat run
~~~

IntelliJ에서는 com.datecourse.app.AppLauncher.main()을 실행하거나 Gradle의 run 작업을 사용합니다. 실행 설정의 Working directory는 프로젝트 루트로 지정합니다.

~~~text
AppLauncher
  → MembershipApp
  → 시작 로딩 / DB 초기화
      ├─ 실패 → 다시 시도 또는 종료
      └─ 성공 → 로그인
                  ├─ 회원가입 → 가입 완료 → 로그인
                  └─ 로그인 성공 → 조건 입력 화면
~~~

com.datecourse.app.MainApp은 이전 실행 코드이며 현재 저장소에 없는 /fxml/Login.fxml을 참조합니다. 통합 앱은 AppLauncher로 실행합니다.

## DB 준비

### 1. 사용할 DB 선택

- Aiven 등에서 이미 생성된 DB가 있으면 해당 DB를 사용합니다. 예: defaultdb.
- 로컬에서 새 DB가 필요하면 생성 권한이 있는 계정으로 [01_create_database.sql](database/01_create_database.sql)을 실행할 수 있습니다. 이 스크립트는 university_project라는 DB를 만드는 선택적 초기화 도구입니다.
- DBeaver 등의 SQL 편집기에서 **앱과 같은 서버 및 DB를 선택한 뒤** [02_create_tables.sql](database/02_create_tables.sql)을 실행합니다.

기본 테이블은 members, categories, data_records입니다. 테이블 생성 스크립트는 DB를 임의로 전환하지 않습니다.

### 2. 접속 설정

[db.properties.example](src/main/resources/db.properties.example)을 같은 폴더의 db.properties로 복사하고 실제 접속 정보를 입력합니다.

~~~properties
db.url=jdbc:mysql://YOUR_HOST:YOUR_PORT/YOUR_DATABASE
db.user=YOUR_USER
db.password=YOUR_PASSWORD
~~~

호스트·포트·DB 이름과 SSL 관련 옵션은 서버의 연결 설정에 맞춥니다. Aiven을 사용하면 로컬 예제 주소를 그대로 사용하지 않습니다.

- 세 항목 모두 필요하며 db.url에는 대상 DB 이름까지 포함합니다.
- URL이 없으면 localhost로 대신 연결하지 않고 설정 오류를 표시합니다.
- 비어 있지 않은 환경변수 DB_URL, DB_USER, DB_PASSWORD는 각각 파일 설정보다 우선합니다.
- 실제 db.properties는 Git 제외 대상입니다. 접속 비밀번호를 커밋하지 않습니다.

앱 시작 시 회원 저장소는 필요에 따라 members.password_hash를 확장하고 app_admins 테이블을 준비합니다. 접속 계정에는 해당 작업에 필요한 ALTER·CREATE 및 기능에 필요한 조회·변경 권한이 있어야 합니다. [06_membership.sql](database/06_membership.sql)은 수동 준비용입니다.

## 회원가입·로그인 동작

- 회원가입 버튼을 누르면 첫 입력 단계로 바로 이동합니다.
- 이메일 → 비밀번호 → 확인 → 이름 → 전화번호 순서로 다음 버튼 또는 Enter를 사용합니다.
- 전화번호는 선택이며 이메일 중복 가입은 거부합니다.
- 비밀번호는 8~128자이며 PBKDF2 SHA-256, 600,000회, 개별 salt로 해시해 DB에 저장합니다.
- 일반 회원은 가입 이메일로 로그인합니다. 비활성 계정은 로그인할 수 없습니다.
- 로그인 성공 후 UserSession에 회원 ID·이름을 담아 JavaFX 조건 입력 화면에 전달합니다.
- DB 작업은 화면 스레드 밖에서 실행하며 회원 저장소의 연결은 HikariCP 풀에서 재사용합니다.

로그인 화면의 **아이디 저장**을 체크하고 로그인에 성공하면 아이디 하나를 .local/input-history.properties에 보관합니다. 다음 실행 시 자동 입력되며, 체크를 해제하면 저장 기록을 지웁니다.

비밀번호와 회원가입 이름·전화번호를 입력 추천용으로 저장하지 않습니다. 이전 버전의 이름·전화번호 추천 기록도 파일을 읽을 때 정리합니다.

## 장소 데이터 적재

[data/integrated_places.csv](data/integrated_places.csv)는 원본 데이터 파일입니다.

| category | 분류 | 건수 |
|---|---|---:|
| MEAL | 식사 | 3,374 |
| CAFE | 카페 | 2,447 |
| CULTURE | 문화 | 318 |
| 합계 | | 6,139 |

원본 필드는 id, name, category, lat, lon, price, rating입니다. 도로명 주소는 포함되어 있지 않습니다. DB의 data_payload에는 JSON으로 저장되며 호환용 businessName도 포함됩니다.

### Java 적재 프로그램

기본 테이블과 접속 설정을 준비한 다음, IntelliJ에서 **kr.ac.university.localdb.ReplaceIntegratedData.main()**을 실행합니다. Working directory는 프로젝트 루트로 지정합니다.

현재 프로그램은 CSV를 직접 해석하지 않고 [05_replace_integrated_data.sql](database/05_replace_integrated_data.sql)을 읽습니다. CSV만 변경해도 적재 내용이 자동으로 바뀌지는 않습니다.

처리 순서:

1. 접속 설정의 DB에 연결합니다. SQL에 예전 USE 구문이 있어도 Java 적재에서는 건너뜁니다.
2. 기존 data_records, categories를 타임스탬프가 붙은 백업 테이블에 복사합니다.
3. 이전 카페·예제·통합 데이터 등 스크립트가 지정한 출처의 행을 교체합니다. 회원과 다른 출처의 장소 행은 유지합니다.
4. 장소 INSERT를 최대 250건씩 묶어 전송하고 커밋 전 진행 상황을 출력합니다.
5. 통합 장소 6,139건과 회원 수 유지를 확인한 뒤 커밋합니다. 교체 도중 오류가 발생하면 해당 트랜잭션을 롤백합니다.

성공 시 **장소 적재 커밋 완료**와 **SUCCESS: integrated dataset replaced**가 출력됩니다. 진행 건수 출력만으로 최종 저장이 끝난 것은 아닙니다.

백업은 CREATE TABLE ... LIKE로 기본 키·인덱스를 유지하고 데이터를 복사합니다. 외래 키는 복제하지 않습니다. 백업 생성은 교체 트랜잭션 밖에서 수행하므로 실패하거나 반복 실행한 경우에도 백업이 남을 수 있습니다.

SQL 파일을 DBeaver에서 직접 실행할 수도 있지만 Java 프로그램의 자동 백업·250건 묶음 처리·회원 수 검증은 적용되지 않습니다. 클라우드 적재에는 Java 프로그램을 기준으로 사용합니다.

### 적재 결과 확인

앱과 같은 DB에서 실행합니다.

~~~sql
SELECT DATABASE();

SELECT COUNT(*) AS place_count
FROM data_records
WHERE source_file = 'integrated_places.csv';

SELECT c.category_code, c.category_name, COUNT(*) AS imported_rows
FROM categories c
JOIN data_records d USING (category_id)
WHERE d.source_file = 'integrated_places.csv'
GROUP BY c.category_id, c.category_code, c.category_name;
~~~

통합 데이터의 예상 건수는 **6,139개**입니다. 다른 출처의 데이터가 있으면 data_records 전체 건수는 더 클 수 있습니다.

### Java 장소 조회 확인

IntelliJ에서 **kr.ac.university.localdb.db.PlaceQueryDemo.main()**을 실행합니다.

- PlaceRepository.findAll()로 통합 장소 목록을 읽습니다.
- 조회 건수와 앞의 장소 3개를 출력합니다.
- DB 데이터를 변경하지 않고 종료 시 데모의 연결 풀을 닫습니다.

Place의 recordId는 DB 기본 키, placeId는 MEAL_0001 같은 원본 ID입니다. JSON의 lon은 Java 모델의 lng에 연결합니다.

## 빌드와 테스트

~~~powershell
.\gradlew.bat build
.\gradlew.bat test
~~~

테스트는 H2 또는 모의 객체를 사용하며 실제 Aiven DB 적재를 실행하지 않습니다. 현재 검증 범위는 연결 재사용·교체·종료, 일반 회원 로그인 성공·실패, 설정 로딩, 배치 적재·롤백, 시작 로딩 재시도, 거리·반경·비용 계산입니다.

실제 접속 환경은 앱 로그인과 PlaceQueryDemo로 별도 확인합니다.

## 코드 구조

~~~text
src/main/java
├─ com/datecourse
│  ├─ app          AppLauncher 등 실행 시작점
│  ├─ controller   JavaFX 화면 컨트롤러
│  ├─ model        Place, Course, SearchCondition 등
│  ├─ service      장소 수·거리·비용 계산
│  └─ demo         개별 화면 실행 및 임시 데이터
└─ kr/ac/university/localdb
   ├─ db           DB 설정, 연결 풀, 회원·장소 저장소, 조회 데모
   ├─ ui           Swing 회원·로딩 화면과 공통 UI
   └─ 기타          사용자 세션, 비밀번호 해시, 아이디 저장, 데이터 적재

src/main/resources  FXML, 스타일, DB 설정 예제
src/test/java       단위·동작 테스트
database            DB 초기화·적재 SQL
data                원본 장소 CSV
archive             과거 자료
~~~

## 다음 구현 범위

1. 실제 지도 표시, 위치 선택, 마커·반경 표시 및 위도·경도 전달
2. 기존 연결 풀을 공유해 장소 데이터를 백그라운드로 읽고 실제 주변 장소 수 표시
3. 분류·반경·예산·인원에 맞는 코스 추천
4. 추천 결과와 편집 화면 연결
5. 로그인한 회원별 코스 저장·재조회·이름 변경·삭제
6. 커뮤니티·관리자 기능 통합

PlaceCalculationService의 거리와 반경 단위는 **미터**입니다. 현재 지도 영역의 픽셀 좌표를 그대로 거리 계산에 전달하면 안 됩니다. 계산 기능은 아직 조건 화면에 연결되지 않았습니다.

## 브랜치 운영과 로컬 파일

- master: 검증된 팀 공통 코드
- feature/*: 지도 연결·장소 수 조회·추천·저장 등 기능별 작업
- 기능 완료 후 빌드·테스트·수동 동작을 확인하고 PR로 master에 병합합니다.
- 새 기능은 최신 master에서 시작합니다. 기존 통합 브랜치는 이전 작업 이력으로 남아 있습니다.

개인 DB 설정 db.properties, 아이디 기록 .local/, 개인 IDE 설정 및 .codex/config.toml은 Git에서 제외합니다. 공유할 DB 설정 형식은 db.properties.example에서 관리합니다.
