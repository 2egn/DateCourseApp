# 로컬 MySQL + Java 통합 데이터 프로젝트

## 회원가입 및 관리자 화면

입력 추천 기록은 항목별 최근 3개로 제한됩니다. 로그인/회원가입 성공 후 '아이디를 저장하시겠습니까?' 창에서 '동의'를 선택해야 저장합니다. 회원가입 시 이름·전화번호도 저장됨을 함께 안내합니다. '거부' 또는 창 닫기는 새로운 추천 기록을 저장하지 않으며 DB 회원가입 자체에는 영향을 주지 않습니다. 비밀번호는 기록하지 않습니다. 기존 8개 기록은 첫 실행 시 최근 3개로 정리됩니다.

관리자 목록의 INACTIVE 회원에만 작은 ×가 나타납니다. 클릭 후 영구 삭제를 확인하면 회원 행이 삭제됩니다. ACTIVE 회원은 DB 처리 단계에서도 삭제를 거부합니다.

화면 스타일은 큰 제목, 넉넉한 여백, 둥근 회색 입력칸과 버튼으로 구성했습니다. 사용자 지정 메인 색은 #FF6B81이며 로그인·회원가입·관리자 화면에 공통 적용됩니다. 버튼 글자는 가독성을 위해 진한 색을 사용합니다.

현재 첫 화면은 아이디·비밀번호 입력 및 로그인입니다. '아직 계정이 없으신가요?'를 클릭하면 회원가입으로 이동합니다. '회원가입 시작'을 누르고 이메일 → 비밀번호 → 확인 → 이름 → 전화번호 순으로 다음/Enter를 누르면 각 칸이 약 0.28초 동안 펼쳐집니다. 전화번호는 비워 두어도 됩니다. 마지막 '가입 완료'에서 DB에 저장합니다.

아이디/이메일·이름·전화번호의 이전 입력값은 해당 칸 클릭 시 선택할 수 있습니다. 이번 변경 이후 입력한 값부터 기록하며 최대 8개를 프로젝트 .local/input-history.properties에 보관합니다. 비밀번호와 확인은 기록·추천하지 않습니다. .local은 Git 제외 대상입니다.

IDE에서 com.datecourse.app 패키지의 [AppLauncher.java main](src/main/java/com/datecourse/app/AppLauncher.java)을 실행하거나 프로젝트 폴더에서 아래 명령을 실행합니다.

```powershell
.\gradlew.bat run
```

회원가입 창에서 이메일, 이름, 전화번호(선택), 비밀번호 및 확인을 입력합니다. 이메일 중복은 거부하며 비밀번호는 8~128자입니다. PBKDF2 SHA-256(600,000회, 개별 salt) 해시만 저장합니다.

로그인 탭에서 지정된 관리자 아이디 a1234로 로그인하면 관리자 창이 열립니다. 관리자 비밀번호는 프로젝트 소유자가 지정한 값이며 DB에는 해시만 저장합니다. 기존 관리자 창 열기 버튼과 최초 비밀번호 설정 창은 제거했습니다. 일반 회원은 가입 이메일로 로그인하며 비활성 계정은 거부됩니다. 로그인 후 관리자는 이메일/이름 검색, 목록 조회, 이름·전화번호·상태 수정이 가능합니다. 탈퇴 처리는 INACTIVE로 변경하며 ACTIVE로 복구할 수 있습니다. 비밀번호와 해시는 목록에 표시하지 않습니다.

시작할 때 기존 members.password_hash를 VARCHAR(255)로 확대하고 app_admins 테이블을 생성합니다. 기존 회원·장소 데이터는 유지됩니다. 초기화에는 로컬 DB의 ALTER/CREATE 권한이 필요합니다. 수동 실행용 SQL은 database/06_membership.sql입니다.

관리자 a1234 계정은 앱 시작 시 없으면 생성하며 기존 비밀번호는 재실행 때 덮어쓰지 않습니다. 이전 admin 계정은 보존하지만 앱 로그인에서는 사용하지 않습니다. 로컬 발표용이며 DB root 비밀번호와 관리자 로그인 비밀번호는 별개입니다.

JDK 17 이상, MySQL 8.4 이상과 Maven을 사용합니다. IDE에서 pom.xml을 Maven 프로젝트로 엽니다.

현재 파일은 data/integrated_places.csv이며 첨부 원본의 모든 값을 보존했습니다.

| category | 분류 | 행 수 |
|---|---|---:|
| MEAL | 식당 | 3,374 |
| CAFE | 카페 | 2,447 |
| CULTURE | 문화 | 318 |

총 6,139행이며 열은 id, name, category, lat, lon, price, rating입니다. 좌표는 위도·경도이며 새 원본에는 도로명 주소가 없습니다. 가격·평점은 원본 그대로 저장합니다. JSON에는 예제 호환용 businessName도 저장합니다.

## 실행

처음에는 database/01_create_database.sql과 02_create_tables.sql을 실행합니다. db.properties.example을 db.properties로 복사하고 계정을 설정합니다. 실제 비밀번호가 있는 db.properties는 공유하지 마세요.

프로젝트 루트에서 다음 명령으로 데이터를 교체합니다.

```powershell
mvn compile exec:java "-Dexec.mainClass=kr.ac.university.localdb.ReplaceIntegratedData"
```

기존 data_records와 categories는 타임스탬프 이름의 백업 테이블로 보관합니다. 이전 카페 파일, 예제 데이터, 이전 통합 파일 적재분만 교체합니다. 회원과 다른 출처의 행은 유지하며 실패 시 교체를 롤백합니다. 백업은 데이터 복사본이며 원래 제약조건까지 복제하지는 않습니다.

05_replace_integrated_data.sql을 DBeaver에서 Alt+X로 실행할 수도 있지만 자동 백업은 Java 프로그램에서만 수행합니다. 기존 카페 CSV, archive 폴더, java ex.zip은 과거 자료이며 현재 적재에 사용하지 않습니다.

## 간단한 Java 테스트

IDE에서 SimpleDatabaseTest.main을 실행하거나 다음을 사용합니다.

```powershell
mvn compile exec:java "-Dexec.mainClass=kr.ac.university.localdb.SimpleDatabaseTest"
```

카페 3개의 이름·위도·경도를 조회하고 가상 카페 1행을 저장 후 조회합니다. 반복 실행하면 같은 테스트 행을 갱신합니다. source_file='java-simple-test'로 실제 6,139행과 구별합니다.

## DB 확인

```sql
SELECT c.category_code, c.category_name, COUNT(*) AS imported_rows
FROM categories c JOIN data_records d USING(category_id)
WHERE d.source_file = 'integrated_places.csv'
GROUP BY c.category_id;

SELECT data_payload->>'$.id' AS id, data_payload->>'$.name' AS name,
       data_payload->>'$.lat' AS lat, data_payload->>'$.lon' AS lon,
       data_payload->>'$.price' AS price, data_payload->>'$.rating' AS rating
FROM data_records WHERE source_file='integrated_places.csv' LIMIT 10;
```
