# 과거 카페 전용 프로젝트 안내 (현재 사용하지 않음)

이 프로젝트는 IntelliJ IDEA, Eclipse, VS Code 등 Maven을 지원하는 모든 Java IDE에서 동일하게 열고 실행할 수 있습니다. Java 17 이상과 MySQL Server 8.4 이상을 기준으로 합니다.

## 구성

- `members`: 회원 정보. 비밀번호 원문 대신 60자 bcrypt 해시만 저장하며, 이메일은 `UNIQUE` 제약으로 중복 저장할 수 없습니다.
- `categories`: Excel 데이터의 분류 기준입니다. `category_code`는 중복되지 않습니다.
- `data_records`: Excel의 각 행을 분류와 함께 저장합니다. 열 구조가 확정되기 전에는 원본 값을 `JSON`으로 보관하므로 카테고리별 열 차이를 안전하게 수용할 수 있습니다.
- `DatabaseSmokeTest`: MySQL 연결 후 데모 회원·카테고리·데이터를 삽입 또는 갱신하고, 각 테이블의 행 수를 출력합니다.

## 최초 실행

1. MySQL Server를 실행합니다.
2. MySQL Workbench 또는 명령행에서 `database/01_create_database.sql`, `database/02_create_tables.sql` 순서로 실행합니다.
3. `src/main/resources/db.properties.example`을 같은 폴더에 `db.properties`라는 이름으로 복사합니다.
4. `db.properties`에 실제 로컬 MySQL 계정(현재는 `root`), 비밀번호를 입력합니다. 이 파일은 Git에 포함되지 않습니다.
5. IDE에서 이 폴더를 Maven 프로젝트로 열고 Maven 동기화를 실행합니다.
6. `DatabaseSmokeTest`를 실행하거나, 터미널에서 `mvn compile exec:java`를 실행합니다.

정상 실행 결과에는 `members`, `categories`, `data_records`의 행 수와 `SUCCESS` 메시지가 표시됩니다. Workbench에서 `database/03_sample_queries.sql`을 실행하면 삽입된 데이터를 직접 확인할 수 있습니다.

## 부산 카페 데이터 적재

1. `database/04_import_busan_cafes.sql`의 CSV 절대 경로가 현재 프로젝트 위치와 일치하는지 확인합니다.
2. MySQL Workbench 연결 설정에서 `LOCAL INFILE`을 사용 가능하게 하고 `SHOW VARIABLES LIKE 'local_infile';` 결과가 `ON`인지 확인합니다.
3. `04_import_busan_cafes.sql`을 실행합니다. 성공한 2,447행이 `BUSAN_CAFE` 카테고리의 `data_records`에 저장됩니다.
4. `data/busan_cafes_rejected.csv`의 2행은 주소가 없어 지오코딩에 실패한 원본 행입니다. 이 파일은 자동 적재하지 않습니다.

## 필요한 설치와 권한

- JDK 17 이상
- Maven 3.9 이상 또는 Maven을 내장한 IDE
- MySQL Server 8.4 이상과 실행 권한
- 데이터베이스 생성 및 테이블 생성 권한을 가진 로컬 MySQL 계정
- Maven이 MySQL JDBC 드라이버를 내려받을 수 있는 인터넷 연결(최초 1회)

## Excel 파일을 받으면 할 작업

`data/busan_cafes_cleaned.csv`는 부산 카페 원본에서 빈 보조 열을 제거하고 공백·전화번호를 정리한 DB 적재 대상입니다. 지오코딩 실패 행은 `data/busan_cafes_rejected.csv`로 분리해 원본 손실 없이 검토할 수 있습니다. 파일의 열 구조와 카테고리 규칙이 추가로 정해지면 `data_records`에 대량으로 넣는 가져오기 코드를 추가합니다.
