# 회원·코스 저장·정정·DB

[짧은 전체 흐름](00-flow-map.md) · [이 기능의 코드 흐름](10-flow-details.md#storage)

[전체 길잡이](README.md). 일반 실행과 관리용 DB 준비를 구분한다.

| 기능 | 사용 요소 | 필요한 기능·구현상 효과 | 핵심 처리 |
|---|---|---|---|
| 가입·로그인 | Swing·JDBC·PasswordHash | 입력·회원 조회·원문 대신 해시 비교 | MembershipApp → MemberRepository.register/login → create/verify |
| 로그인별 데이터 | UserSession·UserContext | 회원 소유 기준·반복 조회 공유 | Future 재사용, 저장 후 invalidate, 로그아웃 close |
| DB 연결 | HikariCP·JDBC | 연결 재사용·대기 제한·SQL 바인딩 | DatabasePool → Repository 연결 사용·반환 |
| 코스 저장 | JDBC 트랜잭션·스냅샷 | 머리·방문 순서·반경·수단을 함께 보관 | persist → insert/update → 장소·수단 교체 → commit/rollback |
| 저장 목록 | 회원 조건 JOIN 조회 | 현재 원본과 별개로 저장 당시 코스 복원 | findAll → readPlaceSnapshot → SavedCourse |
| 미리보기 | PNG·내용 지문·savepoint | 본문 저장과 이미지 실패를 분리 | 현재 계획 지문 확인, 선택적 이미지 쓰기 |
| 정정 제보 | InformationReport·Repository | 요청과 승인 반영을 구분 | submit → 관리자 review → 조회 시 apply |
| 관리자 | AdminSession·관리 화면 | 일반 회원과 관리자 작업 구분 | 회원·제보 작업에 소유·상태·변경 충돌 검사 |
| 구조 준비 | DatabaseSetup·SQL | 앱 사용 중 임의 테이블 생성 방지 | 일반 initialize는 존재 확인, setupDatabase는 명시적 준비 |

근거: [회원](../../src/main/java/com/datecourse/db/MemberRepository.java), [해시](../../src/main/java/com/datecourse/auth/PasswordHash.java), [저장](../../src/main/java/com/datecourse/db/SavedCourseRepository.java), [정정](../../src/main/java/com/datecourse/db/InformationReportRepository.java), [준비](../../src/main/java/com/datecourse/maintenance/DatabaseSetup.java), [SQL](../../data/sql/).

## 스냅샷 저장의 논리

장소 ID만 저장하면 이후 원본 변경이 저장 코스의 내용에 영향을 준다. 현재는 장소·가격·상세·방문 순서를 함께 보존해 당시 화면의 내용을 복원한다. 최신 영업·주소·가격을 자동 갱신하는 기능은 아니다. 다른 앱의 오래된 편집본을 현재 본문과 비교하는 버전 조건은 없어 동시 편집 충돌은 남은 개선 후보다.

PNG 자체의 선택적 DB 저장, 로그인 세션의 지도 준비 상태, 현재 화면의 실제 공급사 경로는 서로 다르다. 실제 경로 응답을 영구 DB에 저장했다고 설명하지 않는다. 현재 Planner의 본문 저장은 saveWithoutPreview를 사용하며, 항상 SDK 캡처까지 함께 저장하는 것은 아니다. 이미지 생성·갱신 경로는 별도다.

## 비밀번호·설정의 실제 처리

PasswordHash는 SecureRandom의 16바이트 salt, PBKDF2WithHmacSHA256, 생성 시 600,000회 반복·256비트 결과를 사용한다. 형식·반복 수·salt·해시를 저장하고 로그인 때 다시 계산해 비교한다. 현재 코드 값이며 최적 보안 설정·모든 공격 방어를 증명하지 않는다. 단일 SHA-256 해싱이라고 설명하지 않는다.

TeamApiKeys는 실행 중 공통 인증으로 공급사 키와 별도 DB 설정을 받아 메모리에 둔다. 개인 값을 JAR에서 제외하는 조치와 클라이언트의 DB 자격증명 접근을 막는 서버 구조는 다른 문제다. 현재 앱은 직접 DB·공급사에 연결한다. 서버 키 교체가 실행 중 캐시에 즉시 반영되지 않을 수 있다.

## DB 구조를 보는 기준

운영 15개 테이블은 회원·관리자·코스·장소 스냅샷·이미지·수단·제보·정정·카탈로그/이력·이전 데이터로 나눈다. 원본의 모든 항목이 독립 DB 열인 것은 아니다. 카탈로그 상세는 JSON payload이고 버전·지문·활성 상태와 별도로 관리한다. [현재 구조·보존 기록](../site/content/05_회원_DB/DB_구조와_빈_백업_정리_2026_11_24.md)을 참조한다. 과거 빈 백업 정리는 완료된 이력이며 새 실행에서 반복하지 않는다.
