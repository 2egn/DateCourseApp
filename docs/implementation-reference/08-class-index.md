# 활성 Java 파일 전체 색인

[전체 길잡이](README.md). 모든 main Java 파일을 포함한다. 기능별 핵심 설명은 앞 문서에 있고, 아래는 각 파일의 무엇·사용 요소·필요 기능·대표 처리 선언·소스 확인용이다.

대표 선언은 접근 제한자가 있는 메서드를 자동 추출한 것이며 전체 Java AST·호출 관계 분석은 아니다. 생성자·중첩 타입·접근 제한자 없는 메서드는 직접 소스에서 확인한다. main 파일이 모두 일반 화면에서 호출된다는 뜻은 아니며 demo·maintenance는 별도 역할이다.

## com.datecourse.app


<details>
<summary>AppLauncher — 통합 실행을 회원·코스 화면으로 연결</summary>

- **무엇:** 통합 실행을 회원·코스 화면으로 연결.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `AppLauncher()` (줄 8), `main()` (줄 10)
- **코드:** [AppLauncher.java](../../src/main/java/com/datecourse/app/AppLauncher.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.auth


<details>
<summary>PasswordHash — 개별 salt·반복 해시 생성과 비밀번호 비교</summary>

- **무엇:** 개별 salt·반복 해시 생성과 비밀번호 비교.
- **무엇을 사용:** JDK 암호화.
- **필요 기능·사용 효과:** salt 생성·반복 키 유도·해시 비교를 제공한다.
- **어떻게 확인:** `create()` (줄 15), `verify()` (줄 22), `derive()` (줄 30), `PasswordHash()` (줄 13)
- **코드:** [PasswordHash.java](../../src/main/java/com/datecourse/auth/PasswordHash.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.db


<details>
<summary>AdminPasswordReset — 명시적인 관리자 비밀번호 재설정 도구</summary>

- **무엇:** 명시적인 관리자 비밀번호 재설정 도구.
- **무엇을 사용:** Swing.
- **필요 기능·사용 효과:** 창·양식·이벤트 부품을 사용한다.
- **어떻게 확인:** `AdminPasswordReset()` (줄 17), `main()` (줄 51), `askPassword()` (줄 79)
- **코드:** [AdminPasswordReset.java](../../src/main/java/com/datecourse/db/AdminPasswordReset.java)
- **검증 참조:** [AdminPasswordResetTest.java](../../src/test/java/com/datecourse/db/AdminPasswordResetTest.java)

</details>

<details>
<summary>DatabasePool — DB 연결 재사용을 위한 풀 생성</summary>

- **무엇:** DB 연결 재사용을 위한 풀 생성.
- **무엇을 사용:** HikariCP.
- **필요 기능·사용 효과:** DB 연결 생성·반환·풀 한도를 관리한다.
- **어떻게 확인:** `DatabasePool()` (줄 8), `create()` (줄 10)
- **코드:** [DatabasePool.java](../../src/main/java/com/datecourse/db/DatabasePool.java)
- **검증 참조:** [DatabasePoolTest.java](../../src/test/java/com/datecourse/db/DatabasePoolTest.java)

</details>

<details>
<summary>DatabaseSchema — 필요한 DB 구조 존재 확인</summary>

- **무엇:** 필요한 DB 구조 존재 확인.
- **무엇을 사용:** JDBC.
- **필요 기능·사용 효과:** SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다.
- **어떻게 확인:** `DatabaseSchema()` (줄 10), `SetupRequiredException()` (줄 12), `requireTables()` (줄 17), `requireMembership()` (줄 30)
- **중첩 타입:** `SetupRequiredException`
- **코드:** [DatabaseSchema.java](../../src/main/java/com/datecourse/db/DatabaseSchema.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>DbSettings — DB 설정의 로딩·형식 확인</summary>

- **무엇:** DB 설정의 로딩·형식 확인.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `load()` (줄 16), `toString()` (줄 60), `value()` (줄 62)
- **주요 값:** `url`, `user`, `password`
- **코드:** [DbSettings.java](../../src/main/java/com/datecourse/db/DbSettings.java)
- **검증 참조:** [DbSettingsTest.java](../../src/test/java/com/datecourse/db/DbSettingsTest.java)

</details>

<details>
<summary>InformationReportRepository — 정정 제보·검토·승인값 적용</summary>

- **무엇:** 정정 제보·검토·승인값 적용.
- **무엇을 사용:** JDBC.
- **필요 기능·사용 효과:** SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다.
- **어떻게 확인:** `InformationReportRepository()` (줄 16), `initialize()` (줄 19), `submit()` (줄 25), `toString()` (줄 55)
- **중첩 타입:** `ReportFilter`, `ReportPage`, `Correction`, `ReviewContext`, `Override`
- **코드:** [InformationReportRepository.java](../../src/main/java/com/datecourse/db/InformationReportRepository.java)
- **검증 참조:** [InformationReportRepositoryTest.java](../../src/test/java/com/datecourse/db/InformationReportRepositoryTest.java)

</details>

<details>
<summary>MemberRepository — 가입·로그인·프로필·관리자 DB 작업</summary>

- **무엇:** 가입·로그인·프로필·관리자 DB 작업.
- **무엇을 사용:** JDBC · HikariCP.
- **필요 기능·사용 효과:** SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다. DB 연결 생성·반환·풀 한도를 관리한다.
- **어떻게 확인:** `register()` (줄 66), `login()` (줄 87), `profile()` (줄 18), `initialize()` (줄 62)
- **중첩 타입:** `Login`, `Member`, `Profile`, `AdminSession`
- **코드:** [MemberRepository.java](../../src/main/java/com/datecourse/db/MemberRepository.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PlaceCatalogRepository — 출처별 장소·버전·이력·보존 적재</summary>

- **무엇:** 출처별 장소·버전·이력·보존 적재.
- **무엇을 사용:** JDBC · Jackson.
- **필요 기능·사용 효과:** SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다. JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `PlaceCatalogRepository()` (줄 43), `prepare()` (줄 49), `initialize()` (줄 75), `synchronize()` (줄 80)
- **중첩 타입:** `Batch`, `ImportMode`, `ImportResult`, `PreparedBatch`, `Existing`
- **코드:** [PlaceCatalogRepository.java](../../src/main/java/com/datecourse/db/PlaceCatalogRepository.java)
- **검증 참조:** [PlaceCatalogRepositoryTest.java](../../src/test/java/com/datecourse/db/PlaceCatalogRepositoryTest.java)

</details>

<details>
<summary>PlaceRepository — 운영 카탈로그 우선 장소 조회</summary>

- **무엇:** 운영 카탈로그 우선 장소 조회.
- **무엇을 사용:** JDBC · Jackson.
- **필요 기능·사용 효과:** SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다. JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `PlaceRepository()` (줄 28), `findAll()` (줄 33)
- **코드:** [PlaceRepository.java](../../src/main/java/com/datecourse/db/PlaceRepository.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>SavedCourseRepository — 회원별 코스·장소 스냅샷·미리보기 저장</summary>

- **무엇:** 회원별 코스·장소 스냅샷·미리보기 저장.
- **무엇을 사용:** JDBC · Jackson.
- **필요 기능·사용 효과:** SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다. JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `persist()` (줄 148), `findAll()` (줄 51), `saveWithoutPreview()` (줄 143), `refreshPreview()` (줄 290)
- **중첩 타입:** `SaveResult`
- **코드:** [SavedCourseRepository.java](../../src/main/java/com/datecourse/db/SavedCourseRepository.java)
- **검증 참조:** [SavedCourseRepositoryTest.java](../../src/test/java/com/datecourse/db/SavedCourseRepositoryTest.java)

</details>

## com.datecourse.demo


<details>
<summary>CameraMotionDemoApp — 카메라 정책·지도 전환 시연</summary>

- **무엇:** 카메라 정책·지도 전환 시연.
- **무엇을 사용:** JavaFX · Jackson.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다. JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `start()` (줄 41), `radius()` (줄 73), `ready()` (줄 74), `select()` (줄 75)
- **중첩 타입:** `Command`, `Panel`, `Bridge`
- **코드:** [CameraMotionDemoApp.java](../../src/main/java/com/datecourse/demo/CameraMotionDemoApp.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>CameraMotionDemoLauncher — 카메라 시연 진입점</summary>

- **무엇:** 카메라 시연 진입점.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `main()` (줄 4)
- **코드:** [CameraMotionDemoLauncher.java](../../src/main/java/com/datecourse/demo/CameraMotionDemoLauncher.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>CoursePreviewSetup — 이전 명령에서 이미지 구조 준비로 위임</summary>

- **무엇:** 이전 명령에서 이미지 구조 준비로 위임.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `main()` (줄 6)
- **코드:** [CoursePreviewSetup.java](../../src/main/java/com/datecourse/demo/CoursePreviewSetup.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>InformationReportSetup — 이전 명령에서 DB 준비 도구로 위임</summary>

- **무엇:** 이전 명령에서 DB 준비 도구로 위임.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `main()` (줄 6)
- **코드:** [InformationReportSetup.java](../../src/main/java/com/datecourse/demo/InformationReportSetup.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>KakaoRouteProbe — 명시적 실제 카카오 경로 점검</summary>

- **무엇:** 명시적 실제 카카오 경로 점검.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `main()` (줄 10)
- **코드:** [KakaoRouteProbe.java](../../src/main/java/com/datecourse/demo/KakaoRouteProbe.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PublicPlaceImport — 공공 파일 쌍 검증·명시적 적재</summary>

- **무엇:** 공공 파일 쌍 검증·명시적 적재.
- **무엇을 사용:** Jackson.
- **필요 기능·사용 효과:** JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `batch()` (줄 14), `main()` (줄 29)
- **코드:** [PublicPlaceImport.java](../../src/main/java/com/datecourse/demo/PublicPlaceImport.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.maintenance


<details>
<summary>DatabaseSetup — 요청된 DB 구조 준비만 명시적으로 실행</summary>

- **무엇:** 요청된 DB 구조 준비만 명시적으로 실행.
- **무엇을 사용:** JDBC.
- **필요 기능·사용 효과:** SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다.
- **어떻게 확인:** `DatabaseSetup()` (줄 16), `main()` (줄 19), `prepare()` (줄 42), `prepareMembership()` (줄 58)
- **코드:** [DatabaseSetup.java](../../src/main/java/com/datecourse/maintenance/DatabaseSetup.java)
- **검증 참조:** [DatabaseSetupTest.java](../../src/test/java/com/datecourse/db/DatabaseSetupTest.java)

</details>

<details>
<summary>ReplaceIntegratedData — 이전 일반 데이터의 명시적 교체</summary>

- **무엇:** 이전 일반 데이터의 명시적 교체.
- **무엇을 사용:** JDBC.
- **필요 기능·사용 효과:** SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다.
- **어떻게 확인:** `main()` (줄 17)
- **코드:** [ReplaceIntegratedData.java](../../src/main/java/com/datecourse/maintenance/ReplaceIntegratedData.java)
- **검증 참조:** [ReplaceIntegratedDataTest.java](../../src/test/java/com/datecourse/maintenance/ReplaceIntegratedDataTest.java)

</details>

## com.datecourse.model


<details>
<summary>CostEstimate — 인원별 비용 범위와 미확인·추정 개수 집계</summary>

- **무엇:** 인원별 비용 범위와 미확인·추정 개수 집계.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `of()` (줄 15), `display()` (줄 39), `exactTotal()` (줄 33), `CostEstimate()` (줄 11)
- **주요 값:** `minimum`, `maximum`, `unknownPlaces`, `placeCount`, `estimatedPlaces`
- **코드:** [CostEstimate.java](../../src/main/java/com/datecourse/model/CostEstimate.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>ExternalPlaceLink — 외부 ID의 승인·일치도·실제 장소 여부 구분</summary>

- **무엇:** 외부 ID의 승인·일치도·실제 장소 여부 구분.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `usable()` (줄 26), `identityChanged()` (줄 27), `unreviewedGoogle()` (줄 31), `clean()` (줄 35)
- **주요 값:** `provider`, `externalId`, `confidence`, `reviewStatus`, `entityKind`, `matchMethod`, `checkedAt`, `reviewNote`
- **중첩 타입:** `Provider`, `Confidence`, `ReviewStatus`, `EntityKind`
- **코드:** [ExternalPlaceLink.java](../../src/main/java/com/datecourse/model/ExternalPlaceLink.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>ExternalPlaceSnapshot — 외부 상세 조회의 임시 결과·영업 구간</summary>

- **무엇:** 외부 상세 조회의 임시 결과·영업 구간.
- **무엇을 사용:** Java record · Jackson.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다. JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `contains()` (줄 25), `featureState()` (줄 38), `openAt()` (줄 40), `toString()` (줄 43)
- **주요 값:** `link`, `fetchedAt`, `priceRange`, `rating`, `ratingCount`, `features`, `openingIntervals`
- **중첩 타입:** `OpeningInterval`, `NoPersistence`
- **코드:** [ExternalPlaceSnapshot.java](../../src/main/java/com/datecourse/model/ExternalPlaceSnapshot.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>FactState — 가능·불가능·미확인의 세 상태</summary>

- **무엇:** 가능·불가능·미확인의 세 상태.
- **무엇을 사용:** Java enum.
- **필요 기능·사용 효과:** 상태·정책의 경우를 명시해 분기와 표시를 일관되게 사용한다.
- **어떻게 확인:** 값·상태·생성자·인터페이스 선언을 확인한다.
- **코드:** [FactState.java](../../src/main/java/com/datecourse/model/FactState.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>InformationReport — 정정 대상·이유·검토 상태 전달</summary>

- **무엇:** 정정 대상·이유·검토 상태 전달.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `label()` (줄 11), `toString()` (줄 12)
- **주요 값:** `id`, `placeId`, `placeName`, `field`, `originalValue`, `proposedValue`, `reason`, `sourceUrl`, `status`, `reviewNote`, `createdAt`, `reviewedValue`
- **중첩 타입:** `Field`
- **코드:** [InformationReport.java](../../src/main/java/com/datecourse/model/InformationReport.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>Member — 역할·가입일·정지 상태의 별도 회원 기본 모델</summary>

- **무엇:** 역할·가입일·정지 상태의 별도 회원 기본 모델.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** 값·상태·생성자·인터페이스 선언을 확인한다.
- **주요 값:** `memberId`, `name`, `role`, `joinedAt`, `suspended`
- **코드:** [Member.java](../../src/main/java/com/datecourse/model/Member.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>NextPlaceCondition — 다음 장소 조건·필터·표시 개수 전달</summary>

- **무엇:** 다음 장소 조건·필터·표시 개수 전달.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `NextPlaceCondition()` (줄 15)
- **주요 값:** `activity`, `maxPrice`, `radius`, `includeUnknown`, `subtypes`, `tags`, `displayLimit`, `detailTypes`, `priceFilter`
- **코드:** [NextPlaceCondition.java](../../src/main/java/com/datecourse/model/NextPlaceCondition.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>Place — 장소 ID·좌표·분류·가격·상세 전달</summary>

- **무엇:** 장소 ID·좌표·분류·가격·상세 전달.
- **무엇을 사용:** Java record · Jackson.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다. JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `stableKey()` (줄 30), `sameIdentityAs()` (줄 36), `Place()` (줄 19), `hasPublicIdentity()` (줄 44)
- **주요 값:** `recordId`, `placeId`, `name`, `category`, `lat`, `lng`, `priceEstimate`, `rating`, `details`
- **코드:** [Place.java](../../src/main/java/com/datecourse/model/Place.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PlaceAttributeFact — 속성의 상태·근거 전달</summary>

- **무엇:** 속성의 상태·근거 전달.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** 값·상태·생성자·인터페이스 선언을 확인한다.
- **주요 값:** `feature`, `state`, `evidence`
- **코드:** [PlaceAttributeFact.java](../../src/main/java/com/datecourse/model/PlaceAttributeFact.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PlaceDetails — 주소·업종·연락처·출처·속성·외부 연결 보관</summary>

- **무엇:** 주소·업종·연락처·출처·속성·외부 연결 보관.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `if()` (줄 16), `PlaceDetails()` (줄 18), `evidenceFor()` (줄 68), `featureState()` (줄 69)
- **주요 값:** `address`, `subcategory`, `phone`, `website`, `sourceUrl`, `sourceDate`, `identityCheckedAt`, `verificationUrl`, `externalIds`, `tags`, `attributeFacts`, `externalLinks`, `fieldEvidence`
- **중첩 타입:** `TagFact`
- **코드:** [PlaceDetails.java](../../src/main/java/com/datecourse/model/PlaceDetails.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PlaceFeature — 장소 속성의 종류 정의</summary>

- **무엇:** 장소 속성의 종류 정의.
- **무엇을 사용:** Java enum.
- **필요 기능·사용 효과:** 상태·정책의 경우를 명시해 분기와 표시를 일관되게 사용한다.
- **어떻게 확인:** `from()` (줄 12)
- **코드:** [PlaceFeature.java](../../src/main/java/com/datecourse/model/PlaceFeature.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PlaceField — 항목별 출처를 구분하는 필드 정의</summary>

- **무엇:** 항목별 출처를 구분하는 필드 정의.
- **무엇을 사용:** Java enum.
- **필요 기능·사용 효과:** 상태·정책의 경우를 명시해 분기와 표시를 일관되게 사용한다.
- **어떻게 확인:** 값·상태·생성자·인터페이스 선언을 확인한다.
- **코드:** [PlaceField.java](../../src/main/java/com/datecourse/model/PlaceField.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PlacePriceFilter — 참고·추정·무료 가격 조건 판정</summary>

- **무엇:** 참고·추정·무료 가격 조건 판정.
- **무엇을 사용:** Java enum.
- **필요 기능·사용 효과:** 상태·정책의 경우를 명시해 분기와 표시를 일관되게 사용한다.
- **어떻게 확인:** `accepts()` (줄 11), `toString()` (줄 17)
- **코드:** [PlacePriceFilter.java](../../src/main/java/com/datecourse/model/PlacePriceFilter.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PlaceTag — 확인된 특징의 표시 이름 정의</summary>

- **무엇:** 확인된 특징의 표시 이름 정의.
- **무엇을 사용:** Java enum.
- **필요 기능·사용 효과:** 상태·정책의 경우를 명시해 분기와 표시를 일관되게 사용한다.
- **어떻게 확인:** `label()` (줄 12), `toString()` (줄 13)
- **코드:** [PlaceTag.java](../../src/main/java/com/datecourse/model/PlaceTag.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PriceEstimate — 가격 범위·단위·근거·미확인 표현</summary>

- **무엇:** 가격 범위·단위·근거·미확인 표현.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `withinCap()` (줄 61), `exact()` (줄 36), `estimated()` (줄 42), `display()` (줄 70)
- **주요 값:** `minimum`, `maximum`, `unit`, `basis`, `evidence`, `sourceUrl`, `checkedAt`
- **중첩 타입:** `Unit`, `Evidence`
- **코드:** [PriceEstimate.java](../../src/main/java/com/datecourse/model/PriceEstimate.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>ProviderPriceRange — 외부 가격 범위·통화 전달</summary>

- **무엇:** 외부 가격 범위·통화 전달.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** 값·상태·생성자·인터페이스 선언을 확인한다.
- **주요 값:** `minimum`, `maximum`, `currencyCode`
- **코드:** [ProviderPriceRange.java](../../src/main/java/com/datecourse/model/ProviderPriceRange.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>RoutePlan — 사용자 계획·방문 순서·반경·수단 전달</summary>

- **무엇:** 사용자 계획·방문 순서·반경·수단 전달.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `RoutePlan()` (줄 9), `withTravelMode()` (줄 12), `costEstimate()` (줄 27), `totalCost()` (줄 29)
- **주요 값:** `startLat`, `startLng`, `people`, `radius`, `title`, `places`, `arrivalRadii`, `travelMode`
- **코드:** [RoutePlan.java](../../src/main/java/com/datecourse/model/RoutePlan.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>SavedCourse — 저장 코스 ID·소유자·계획·수정 시각 전달</summary>

- **무엇:** 저장 코스 ID·소유자·계획·수정 시각 전달.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `SavedCourse()` (줄 8)
- **주요 값:** `courseId`, `memberId`, `plan`, `updatedAt`
- **코드:** [SavedCourse.java](../../src/main/java/com/datecourse/model/SavedCourse.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>SearchCondition — 이전 일괄 추천의 예산·분류·방문 수 조건</summary>

- **무엇:** 이전 일괄 추천의 예산·분류·방문 수 조건.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `SearchCondition()` (줄 20)
- **주요 값:** `budget`, `people`, `categories`, `lat`, `lng`, `radius`, `minPlaces`, `maxPlaces`
- **코드:** [SearchCondition.java](../../src/main/java/com/datecourse/model/SearchCondition.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>SourceEvidence — 출처·기준일·확인일 구분</summary>

- **무엇:** 출처·기준일·확인일 구분.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `clean()` (줄 31)
- **주요 값:** `origin`, `basis`, `sourceUrl`, `referenceDate`, `checkedAt`
- **중첩 타입:** `Origin`
- **코드:** [SourceEvidence.java](../../src/main/java/com/datecourse/model/SourceEvidence.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>TravelMode — 이동수단별 반경 한도·안내 정책</summary>

- **무엇:** 이동수단별 반경 한도·안내 정책.
- **무엇을 사용:** Java enum.
- **필요 기능·사용 효과:** 상태·정책의 경우를 명시해 분기와 표시를 일관되게 사용한다.
- **어떻게 확인:** `maximumRadius()` (줄 12), `maximumLegRadius()` (줄 13), `label()` (줄 14), `timeNote()` (줄 15)
- **코드:** [TravelMode.java](../../src/main/java/com/datecourse/model/TravelMode.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>TravelRoute — 공급사가 조회한 실제 경로·구간·거리·시간 전달</summary>

- **무엇:** 공급사가 조회한 실제 경로·구간·거리·시간 전달.
- **무엇을 사용:** Java record · Jackson.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다. JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `TravelRoute()` (줄 10), `durationText()` (줄 16), `Leg()` (줄 44), `movementSummary()` (줄 48)
- **주요 값:** `legs`, `totalDistance`, `totalTime`, `mode`, `provider`
- **중첩 타입:** `Coordinate`, `Stop`, `Step`, `TransitStage`, `Leg`
- **코드:** [TravelRoute.java](../../src/main/java/com/datecourse/model/TravelRoute.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>UserSession — 로그인 회원 식별 정보 전달</summary>

- **무엇:** 로그인 회원 식별 정보 전달.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** 값·상태·생성자·인터페이스 선언을 확인한다.
- **주요 값:** `memberId`, `name`
- **코드:** [UserSession.java](../../src/main/java/com/datecourse/model/UserSession.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>WalkingSearchOption — 도보 탐색 옵션 정의</summary>

- **무엇:** 도보 탐색 옵션 정의.
- **무엇을 사용:** Java enum.
- **필요 기능·사용 효과:** 상태·정책의 경우를 명시해 분기와 표시를 일관되게 사용한다.
- **어떻게 확인:** `code()` (줄 15), `title()` (줄 16), `description()` (줄 17)
- **코드:** [WalkingSearchOption.java](../../src/main/java/com/datecourse/model/WalkingSearchOption.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.page._01step_planner


<details>
<summary>CoursePlannerController — 공통 조건·확정 코스·저장·페이지 전환 조율</summary>

- **무엇:** 공통 조건·확정 코스·저장·페이지 전환 조율.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `onRecommendClick()` (줄 717), `openEditor()` (줄 736), `onSaveCourse()` (줄 486), `restoreSavedCourse()` (줄 357)
- **코드:** [CoursePlannerController.java](../../src/main/java/com/datecourse/page/_01step_planner/CoursePlannerController.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>InformationReportDialog — 장소 정정 제보 입력</summary>

- **무엇:** 장소 정정 제보 입력.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `confirmSend()` (줄 124)
- **중첩 타입:** `Submission`
- **코드:** [InformationReportDialog.java](../../src/main/java/com/datecourse/page/_01step_planner/InformationReportDialog.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.page._02step_editor


<details>
<summary>CourseEditorController — 초안·후보·순서·반경·필터 조율</summary>

- **무엇:** 초안·후보·순서·반경·필터 조율.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `onRecommendNext()` (줄 434), `onAddCandidate()` (줄 550), `begin()` (줄 400), `resume()` (줄 366)
- **중첩 타입:** `FacetSelection`, `CandidateSearch`, `EditorState`
- **코드:** [CourseEditorController.java](../../src/main/java/com/datecourse/page/_02step_editor/CourseEditorController.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.page._03step_saved


<details>
<summary>CoursePreviewDialog — 미리보기 별도 창</summary>

- **무엇:** 미리보기 별도 창.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** 값·상태·생성자·인터페이스 선언을 확인한다.
- **코드:** [CoursePreviewDialog.java](../../src/main/java/com/datecourse/page/_03step_saved/CoursePreviewDialog.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>SavedCourseController — 저장 목록·이미지/지도·명시적 경로 조회 조율</summary>

- **무엇:** 저장 목록·이미지/지도·명시적 경로 조회 조율.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `configure()` (줄 301), `loadCourses()` (줄 414), `onRequestRoute()` (줄 617), `onOpenClick()` (줄 358)
- **중첩 타입:** `RouteSummary`, `PreviewImage`
- **코드:** [SavedCourseController.java](../../src/main/java/com/datecourse/page/_03step_saved/SavedCourseController.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.page.account


<details>
<summary>InputHistory — 입력 이력·기억 설정 관리</summary>

- **무엇:** 입력 이력·기억 설정 관리.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `InputHistory()` (줄 12), `savedIdentity()` (줄 28), `saveIdentity()` (줄 33), `save()` (줄 39)
- **코드:** [InputHistory.java](../../src/main/java/com/datecourse/page/account/InputHistory.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>MembershipApp — Swing 회원 화면과 JavaFX 코스 화면 연결</summary>

- **무엇:** Swing 회원 화면과 JavaFX 코스 화면 연결.
- **무엇을 사용:** JavaFX · Swing · JDBC.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다. 창·양식·이벤트 부품을 사용한다. SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다.
- **어떻게 확인:** `main()` (줄 82), `show()` (줄 96), `windowClosing()` (줄 161), `requestWindowClose()` (줄 166)
- **코드:** [MembershipApp.java](../../src/main/java/com/datecourse/page/account/MembershipApp.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>MyPageController — 프로필·내 코스 화면 연결</summary>

- **무엇:** 프로필·내 코스 화면 연결.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `configure()` (줄 21), `onRefresh()` (줄 28), `clear()` (줄 34), `loadProfile()` (줄 42)
- **코드:** [MyPageController.java](../../src/main/java/com/datecourse/page/account/MyPageController.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PlannerLoadingDialog — 코스 화면 준비 표시</summary>

- **무엇:** 코스 화면 준비 표시.
- **무엇을 사용:** Swing.
- **필요 기능·사용 효과:** 창·양식·이벤트 부품을 사용한다.
- **어떻게 확인:** 값·상태·생성자·인터페이스 선언을 확인한다.
- **코드:** [PlannerLoadingDialog.java](../../src/main/java/com/datecourse/page/account/PlannerLoadingDialog.java)
- **검증 참조:** [PlannerLoadingDialogTest.java](../../src/test/java/com/datecourse/page/account/PlannerLoadingDialogTest.java)

</details>

<details>
<summary>SignupForm — 회원가입 입력 양식</summary>

- **무엇:** 회원가입 입력 양식.
- **무엇을 사용:** Swing.
- **필요 기능·사용 효과:** 창·양식·이벤트 부품을 사용한다.
- **어떻게 확인:** `getPreferredScrollableViewportSize()` (줄 16), `getScrollableUnitIncrement()` (줄 17), `getScrollableBlockIncrement()` (줄 18), `getScrollableTracksViewportWidth()` (줄 21)
- **코드:** [SignupForm.java](../../src/main/java/com/datecourse/page/account/SignupForm.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>StartupPanel — 시작 화면 표시</summary>

- **무엇:** 시작 화면 표시.
- **무엇을 사용:** Swing.
- **필요 기능·사용 효과:** 창·양식·이벤트 부품을 사용한다.
- **어떻게 확인:** `doInBackground()` (줄 75), `done()` (줄 79), `showFailure()` (줄 98)
- **코드:** [StartupPanel.java](../../src/main/java/com/datecourse/page/account/StartupPanel.java)
- **검증 참조:** [StartupPanelTest.java](../../src/test/java/com/datecourse/page/account/StartupPanelTest.java)

</details>

## com.datecourse.page.admin


<details>
<summary>AdminStyle — 관리자 Swing 스타일</summary>

- **무엇:** 관리자 Swing 스타일.
- **무엇을 사용:** Swing.
- **필요 기능·사용 효과:** 창·양식·이벤트 부품을 사용한다.
- **어떻게 확인:** `AdminStyle()` (줄 25), `tabs()` (줄 34), `installDefaults()` (줄 42), `paintTabBackground()` (줄 52)
- **중첩 타입:** `ThinScrollBarUI`
- **코드:** [AdminStyle.java](../../src/main/java/com/datecourse/page/admin/AdminStyle.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>MemberAdminPanel — 회원 검색·수정·상태·삭제 관리 화면</summary>

- **무엇:** 회원 검색·수정·상태·삭제 관리 화면.
- **무엇을 사용:** Swing · JDBC.
- **필요 기능·사용 효과:** 창·양식·이벤트 부품을 사용한다. SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다.
- **어떻게 확인:** `toString()` (줄 34), `확인창()` (줄 43), `중()` (줄 54), `MemberAdminPanel()` (줄 59)
- **중첩 타입:** `Status`, `MemberTableModel`
- **코드:** [MemberAdminPanel.java](../../src/main/java/com/datecourse/page/admin/MemberAdminPanel.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.page.common


<details>
<summary>BasePageController — 공통 비동기·로딩·실패 표시</summary>

- **무엇:** 공통 비동기·로딩·실패 표시.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `showFuture()` (줄 17), `loadingProperty()` (줄 37), `isLoading()` (줄 38), `useFeedback()` (줄 39)
- **코드:** [BasePageController.java](../../src/main/java/com/datecourse/page/common/BasePageController.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.page.common.controls


<details>
<summary>DialogTheme — 대화상자 테마 적용</summary>

- **무엇:** 대화상자 테마 적용.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `DialogTheme()` (줄 7), `apply()` (줄 8)
- **코드:** [DialogTheme.java](../../src/main/java/com/datecourse/page/common/controls/DialogTheme.java)
- **검증 참조:** [DialogThemeTest.java](../../src/test/java/com/datecourse/page/common/controls/DialogThemeTest.java)

</details>

<details>
<summary>ExpandableFilterPane — 필터 묶음의 펼침·요약</summary>

- **무엇:** 필터 묶음의 펼침·요약.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `Section()` (줄 21), `requireNonNull()` (줄 22), `Selection()` (줄 25), `ExpandableFilterPane()` (줄 42)
- **중첩 타입:** `Tone`, `Section`, `Selection`
- **코드:** [ExpandableFilterPane.java](../../src/main/java/com/datecourse/page/common/controls/ExpandableFilterPane.java)
- **검증 참조:** [ExpandableFilterPaneTest.java](../../src/test/java/com/datecourse/page/common/controls/ExpandableFilterPaneTest.java)

</details>

<details>
<summary>FormScrollPane — 입력 양식 스크롤</summary>

- **무엇:** 입력 양식 스크롤.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `FormScrollPane()` (줄 5)
- **코드:** [FormScrollPane.java](../../src/main/java/com/datecourse/page/common/controls/FormScrollPane.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>ModernScrollPane — 공통 스크롤 외관·동작</summary>

- **무엇:** 공통 스크롤 외관·동작.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `ModernScrollPane()` (줄 7)
- **코드:** [ModernScrollPane.java](../../src/main/java/com/datecourse/page/common/controls/ModernScrollPane.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PlaceFeatureSelector — 활동별 특징 필터 구성</summary>

- **무엇:** 활동별 특징 필터 구성.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `PlaceFeatureSelector()` (줄 17), `selectedTags()` (줄 21), `availableTags()` (줄 22), `configure()` (줄 23)
- **코드:** [PlaceFeatureSelector.java](../../src/main/java/com/datecourse/page/common/controls/PlaceFeatureSelector.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.page.common.map


<details>
<summary>CourseMapView — WebView 지도·경로·캡처·비동기 수명 관리</summary>

- **무엇:** WebView 지도·경로·캡처·비동기 수명 관리.
- **무엇을 사용:** JavaFX · Jackson.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다. JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `requestWalkingRoute()` (줄 577), `isCurrentRouteRequest()` (줄 631), `showDetail()` (줄 495), `restoreDetailFocus()` (줄 505)
- **중첩 타입:** `WalkingRouter`, `PageSource`, `CaptureStage`
- **코드:** [CourseMapView.java](../../src/main/java/com/datecourse/page/common/map/CourseMapView.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>MapEventBridge — 지도 JavaScript에서 Java로 오는 이벤트 경계</summary>

- **무엇:** 지도 JavaScript에서 Java로 오는 이벤트 경계.
- **무엇을 사용:** Jackson.
- **필요 기능·사용 효과:** JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `setOnDetailClosed()` (줄 23), `onDetailClosed()` (줄 24), `setOnRouteImageRequested()` (줄 26), `requestRouteImage()` (줄 27)
- **코드:** [MapEventBridge.java](../../src/main/java/com/datecourse/page/common/map/MapEventBridge.java)
- **검증 참조:** [MapEventBridgeTest.java](../../src/test/java/com/datecourse/page/common/map/MapEventBridgeTest.java)

</details>

<details>
<summary>MapViewportLifecycle — 숨김·크기 변경 후 실제 표시 시점 감지</summary>

- **무엇:** 숨김·크기 변경 후 실제 표시 시점 감지.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `observe()` (줄 40), `close()` (줄 41)
- **코드:** [MapViewportLifecycle.java](../../src/main/java/com/datecourse/page/common/map/MapViewportLifecycle.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.page.common


<details>
<summary>NumberStepper — 인원·가격 입력과 증감 처리</summary>

- **무엇:** 인원·가격 입력과 증감 처리.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `NumberStepper()` (줄 13), `install()` (줄 15), `installPrice()` (줄 20), `readWon()` (줄 29)
- **코드:** [NumberStepper.java](../../src/main/java/com/datecourse/page/common/NumberStepper.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PageFeedback — 로딩·성공·오류 메시지 표시</summary>

- **무엇:** 로딩·성공·오류 메시지 표시.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `PageFeedback()` (줄 33), `show()` (줄 76), `hide()` (줄 92), `beginLoading()` (줄 98)
- **중첩 타입:** `Kind`
- **코드:** [PageFeedback.java](../../src/main/java/com/datecourse/page/common/PageFeedback.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.page.common.swing


<details>
<summary>AppDialogs — 공통 Swing 대화상자</summary>

- **무엇:** 공통 Swing 대화상자.
- **무엇을 사용:** Swing.
- **필요 기능·사용 효과:** 창·양식·이벤트 부품을 사용한다.
- **어떻게 확인:** `AppDialogs()` (줄 9), `showMessageDialog()` (줄 11), `showConfirmDialog()` (줄 17), `showOptionDialog()` (줄 23)
- **코드:** [AppDialogs.java](../../src/main/java/com/datecourse/page/common/swing/AppDialogs.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>CapsLockHint — 비밀번호 입력 Caps Lock 안내</summary>

- **무엇:** 비밀번호 입력 Caps Lock 안내.
- **무엇을 사용:** Swing.
- **필요 기능·사용 효과:** 창·양식·이벤트 부품을 사용한다.
- **어떻게 확인:** `focusGained()` (줄 24), `focusLost()` (줄 25), `refresh()` (줄 29)
- **코드:** [CapsLockHint.java](../../src/main/java/com/datecourse/page/common/swing/CapsLockHint.java)
- **검증 참조:** [CapsLockHintTest.java](../../src/test/java/com/datecourse/page/common/swing/CapsLockHintTest.java)

</details>

<details>
<summary>LoginUI — Swing 로그인 부품·외관</summary>

- **무엇:** Swing 로그인 부품·외관.
- **무엇을 사용:** Swing.
- **필요 기능·사용 효과:** 창·양식·이벤트 부품을 사용한다.
- **어떻게 확인:** `LoginUI()` (줄 20), `install()` (줄 22), `field()` (줄 33), `focusGained()` (줄 42)
- **중첩 타입:** `TextField`, `PasswordField`, `EyeIcon`, `FieldBorder`, `RememberIdIcon`
- **코드:** [LoginUI.java](../../src/main/java/com/datecourse/page/common/swing/LoginUI.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>RevealPanel — Swing 패널 펼침 전환</summary>

- **무엇:** Swing 패널 펼침 전환.
- **무엇을 사용:** Swing.
- **필요 기능·사용 효과:** 창·양식·이벤트 부품을 사용한다.
- **어떻게 확인:** `revealedHeight()` (줄 11), `RevealPanel()` (줄 15), `reveal()` (줄 23), `reset()` (줄 35)
- **코드:** [RevealPanel.java](../../src/main/java/com/datecourse/page/common/swing/RevealPanel.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.page.common


<details>
<summary>TmapSettingsDialog — 도보 탐색 옵션 입력</summary>

- **무엇:** 도보 탐색 옵션 입력.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `show()` (줄 14)
- **코드:** [TmapSettingsDialog.java](../../src/main/java/com/datecourse/page/common/TmapSettingsDialog.java)
- **검증 참조:** [TmapSettingsDialogTest.java](../../src/test/java/com/datecourse/page/common/TmapSettingsDialogTest.java)

</details>

## com.datecourse.page.review


<details>
<summary>InformationReviewController — 제보 목록·상세·승인·반려 화면</summary>

- **무엇:** 제보 목록·상세·승인·반려 화면.
- **무엇을 사용:** JavaFX.
- **필요 기능·사용 효과:** 화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.
- **어떻게 확인:** `번호()` (줄 37), `커서()` (줄 38), `initialize()` (줄 47), `updateItem()` (줄 53)
- **코드:** [InformationReviewController.java](../../src/main/java/com/datecourse/page/review/InformationReviewController.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

## com.datecourse.service


<details>
<summary>AppDiagnostics — 실패 단계·취소·결과 진단 정보</summary>

- **무엇:** 실패 단계·취소·결과 진단 정보.
- **무엇을 사용:** JDBC.
- **필요 기능·사용 효과:** SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다.
- **어떻게 확인:** `AppDiagnostics()` (줄 16), `begin()` (줄 17), `HttpFailure()` (줄 21), `status()` (줄 22)
- **중첩 타입:** `Operation`, `Stage`, `Outcome`, `HttpFailure`, `Trace`
- **코드:** [AppDiagnostics.java](../../src/main/java/com/datecourse/service/AppDiagnostics.java)
- **검증 참조:** [AppDiagnosticsTest.java](../../src/test/java/com/datecourse/service/AppDiagnosticsTest.java)

</details>

<details>
<summary>CameraMotionPolicy — 조작 의도·화면 거리·넘침에 따른 지도 이동 정책</summary>

- **무엇:** 조작 의도·화면 거리·넘침에 따른 지도 이동 정책.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `decide()` (줄 29), `defaults()` (줄 23), `CameraMotionPolicy()` (줄 27)
- **중첩 타입:** `Intent`, `Movement`, `Frame`, `Options`, `Transition`
- **코드:** [CameraMotionPolicy.java](../../src/main/java/com/datecourse/service/CameraMotionPolicy.java)
- **검증 참조:** [CameraMotionPolicyTest.java](../../src/test/java/com/datecourse/service/CameraMotionPolicyTest.java)

</details>

<details>
<summary>CandidateRecoveryService — 후보를 실제로 늘리는 조건 완화 안내</summary>

- **무엇:** 후보를 실제로 늘리는 조건 완화 안내.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `analyze()` (줄 17), `offer()` (줄 41), `matches()` (줄 44), `copy()` (줄 47)
- **중첩 타입:** `Change`, `Suggestion`, `Advice`
- **코드:** [CandidateRecoveryService.java](../../src/main/java/com/datecourse/service/CandidateRecoveryService.java)
- **검증 참조:** [CandidateRecoveryServiceTest.java](../../src/test/java/com/datecourse/service/CandidateRecoveryServiceTest.java)

</details>

<details>
<summary>CoursePreviewImage — PNG 생성·이미지 내용 지문</summary>

- **무엇:** PNG 생성·이미지 내용 지문.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `fingerprint()` (줄 25), `renderWithMap()` (줄 37), `renderMapOnly()` (줄 55), `render()` (줄 63)
- **코드:** [CoursePreviewImage.java](../../src/main/java/com/datecourse/service/CoursePreviewImage.java)
- **검증 참조:** [CoursePreviewImageTest.java](../../src/test/java/com/datecourse/service/CoursePreviewImageTest.java)

</details>

<details>
<summary>CoursePreviewRenderer — 미리보기 렌더러 함수형 경계</summary>

- **무엇:** 미리보기 렌더러 함수형 경계.
- **무엇을 사용:** Java interface.
- **필요 기능·사용 효과:** 호출부와 구현의 입력·출력 계약을 분리한다.
- **어떻게 확인:** 값·상태·생성자·인터페이스 선언을 확인한다.
- **코드:** [CoursePreviewRenderer.java](../../src/main/java/com/datecourse/service/CoursePreviewRenderer.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>ExternalBrowser — 외부 링크를 시스템 브라우저에 연결</summary>

- **무엇:** 외부 링크를 시스템 브라우저에 연결.
- **무엇을 사용:** Jackson.
- **필요 기능·사용 효과:** JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `ExternalBrowser()` (줄 28), `open()` (줄 30), `validate()` (줄 107)
- **중첩 타입:** `BrowserTarget`
- **코드:** [ExternalBrowser.java](../../src/main/java/com/datecourse/service/ExternalBrowser.java)
- **검증 참조:** [ExternalBrowserTest.java](../../src/test/java/com/datecourse/service/ExternalBrowserTest.java)

</details>

<details>
<summary>KakaoRouteService — 카카오 자동차·대중교통 응답 파싱</summary>

- **무엇:** 카카오 자동차·대중교통 응답 파싱.
- **무엇을 사용:** Jackson · HttpClient.
- **필요 기능·사용 효과:** JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다. 외부 HTTP 요청과 응답 처리를 제공한다.
- **어떻게 확인:** `KakaoRouteService()` (줄 23), `route()` (줄 26), `request()` (줄 49), `array()` (줄 137)
- **중첩 타입:** `NoTransitRouteException`
- **코드:** [KakaoRouteService.java](../../src/main/java/com/datecourse/service/KakaoRouteService.java)
- **검증 참조:** [KakaoRouteServiceTest.java](../../src/test/java/com/datecourse/service/KakaoRouteServiceTest.java)

</details>

<details>
<summary>KakaoRouteSettings — 카카오 REST 경로 키 설정 확인</summary>

- **무엇:** 카카오 REST 경로 키 설정 확인.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `toString()` (줄 18), `load()` (줄 19)
- **주요 값:** `restApiKey`
- **코드:** [KakaoRouteSettings.java](../../src/main/java/com/datecourse/service/KakaoRouteSettings.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>MapPageServer — 지도 자원을 로컬 HTTP로 제공</summary>

- **무엇:** 지도 자원을 로컬 HTTP로 제공.
- **무엇을 사용:** Jackson · HttpServer.
- **필요 기능·사용 효과:** JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다. 프로세스 안에서 로컬 지도 자원을 제공한다.
- **어떻게 확인:** `sharedPage()` (줄 48), `configuration()` (줄 112), `pageUri()` (줄 130), `close()` (줄 134)
- **코드:** [MapPageServer.java](../../src/main/java/com/datecourse/service/MapPageServer.java)
- **검증 참조:** [MapPageServerTest.java](../../src/test/java/com/datecourse/service/MapPageServerTest.java)

</details>

<details>
<summary>NextPlaceRadius — 전체 범위·직전 장소에 맞는 구간 반경 상한</summary>

- **무엇:** 전체 범위·직전 장소에 맞는 구간 반경 상한.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `NextPlaceRadius()` (줄 10), `maximum()` (줄 12)
- **코드:** [NextPlaceRadius.java](../../src/main/java/com/datecourse/service/NextPlaceRadius.java)
- **검증 참조:** [NextPlaceRadiusTest.java](../../src/test/java/com/datecourse/service/NextPlaceRadiusTest.java)

</details>

<details>
<summary>NextPlaceService — 조건에 맞는 다음 후보를 거리순으로 선택</summary>

- **무엇:** 조건에 맞는 다음 후보를 거리순으로 선택.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `matching()` (줄 30), `candidates()` (줄 23)
- **코드:** [NextPlaceService.java](../../src/main/java/com/datecourse/service/NextPlaceService.java)
- **검증 참조:** [NextPlaceServiceTest.java](../../src/test/java/com/datecourse/service/NextPlaceServiceTest.java)

</details>

<details>
<summary>PlaceCalculationService — 좌표 사이 구면 거리·이전 정액 계산</summary>

- **무엇:** 좌표 사이 구면 거리·이전 정액 계산.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `calculateDistance()` (줄 14), `filterPlacesByRadius()` (줄 25), `calculateTotalCost()` (줄 37), `validateCoordinates()` (줄 45)
- **코드:** [PlaceCalculationService.java](../../src/main/java/com/datecourse/service/PlaceCalculationService.java)
- **검증 참조:** [PlaceCalculationServiceTest.java](../../src/test/java/com/datecourse/service/PlaceCalculationServiceTest.java)

</details>

<details>
<summary>PlaceFacets — 공공 업종을 화면 유형·상세 분류에 연결</summary>

- **무엇:** 공공 업종을 화면 유형·상세 분류에 연결.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `PlaceFacets()` (줄 7), `detailType()` (줄 9), `subtype()` (줄 19)
- **코드:** [PlaceFacets.java](../../src/main/java/com/datecourse/service/PlaceFacets.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PlaceMapPayload — 모델에 지도 표시용 presentation을 덧붙임</summary>

- **무엇:** 모델에 지도 표시용 presentation을 덧붙임.
- **무엇을 사용:** Jackson.
- **필요 기능·사용 효과:** JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `PlaceMapPayload()` (줄 18), `place()` (줄 21), `places()` (줄 28), `plan()` (줄 31)
- **코드:** [PlaceMapPayload.java](../../src/main/java/com/datecourse/service/PlaceMapPayload.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PlacePresenter — 가격·출처·특징을 표시 문구로 변환</summary>

- **무엇:** 가격·출처·특징을 표시 문구로 변환.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `present()` (줄 24), `candidateType()` (줄 45), `badge()` (줄 51), `line()` (줄 60)
- **중첩 타입:** `Badge`, `DetailLine`, `Presentation`
- **코드:** [PlacePresenter.java](../../src/main/java/com/datecourse/service/PlacePresenter.java)
- **검증 참조:** [PlacePresenterTest.java](../../src/test/java/com/datecourse/service/PlacePresenterTest.java)

</details>

<details>
<summary>PlaceSearchService — 전체 범위·중복 대표 검색과 이전 일괄 추천</summary>

- **무엇:** 전체 범위·중복 대표 검색과 이전 일괄 추천.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `nearby()` (줄 19), `recommend()` (줄 52), `identity()` (줄 99), `categoryName()` (줄 103)
- **중첩 타입:** `NearbyPlace`, `Result`, `PlaceLocation`
- **코드:** [PlaceSearchService.java](../../src/main/java/com/datecourse/service/PlaceSearchService.java)
- **검증 참조:** [PlaceSearchServiceTest.java](../../src/test/java/com/datecourse/service/PlaceSearchServiceTest.java)

</details>

<details>
<summary>PlaceSource — 장소 조회의 공통 계약</summary>

- **무엇:** 장소 조회의 공통 계약.
- **무엇을 사용:** Java interface.
- **필요 기능·사용 효과:** 호출부와 구현의 입력·출력 계약을 분리한다.
- **어떻게 확인:** 값·상태·생성자·인터페이스 선언을 확인한다.
- **코드:** [PlaceSource.java](../../src/main/java/com/datecourse/service/PlaceSource.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>PublicPlaceFacts — 장소 파일 지문·ID 검사와 공공 속성 연결</summary>

- **무엇:** 장소 파일 지문·ID 검사와 공공 속성 연결.
- **무엇을 사용:** Jackson.
- **필요 기능·사용 효과:** JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `PublicPlaceFacts()` (줄 18), `read()` (줄 20), `readPlaces()` (줄 75), `required()` (줄 113)
- **코드:** [PublicPlaceFacts.java](../../src/main/java/com/datecourse/service/PublicPlaceFacts.java)
- **검증 참조:** [PublicPlaceFactsTest.java](../../src/test/java/com/datecourse/service/PublicPlaceFactsTest.java)

</details>

<details>
<summary>RouteRangeValidator — 완성 코스의 전체·구간 직선 반경 검사</summary>

- **무엇:** 완성 코스의 전체·구간 직선 반경 검사.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `exceeded()` (줄 15), `legs()` (줄 18), `outsideRange()` (줄 31)
- **중첩 타입:** `Leg`
- **코드:** [RouteRangeValidator.java](../../src/main/java/com/datecourse/service/RouteRangeValidator.java)
- **검증 참조:** [RouteRangeValidatorTest.java](../../src/test/java/com/datecourse/service/RouteRangeValidatorTest.java)

</details>

<details>
<summary>SavedCoursePresenter — 저장 코스 합계·이동 요약 문구</summary>

- **무엇:** 저장 코스 합계·이동 요약 문구.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `costSummary()` (줄 24), `present()` (줄 36)
- **중첩 타입:** `CourseDetailRow`, `PlaceInfoRow`, `Summary`
- **코드:** [SavedCoursePresenter.java](../../src/main/java/com/datecourse/service/SavedCoursePresenter.java)
- **검증 참조:** [SavedCoursePresenterTest.java](../../src/test/java/com/datecourse/service/SavedCoursePresenterTest.java)

</details>

<details>
<summary>TeamApiKeys — 공통 인증으로 실행 중 키·DB 설정 조회</summary>

- **무엇:** 공통 인증으로 실행 중 키·DB 설정 조회.
- **무엇을 사용:** Jackson.
- **필요 기능·사용 효과:** JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.
- **어떻게 확인:** `TeamApiKeys()` (줄 27), `get()` (줄 30), `database()` (줄 35), `fetch()` (줄 78)
- **코드:** [TeamApiKeys.java](../../src/main/java/com/datecourse/service/TeamApiKeys.java)
- **검증 참조:** [TeamApiKeysTest.java](../../src/test/java/com/datecourse/service/TeamApiKeysTest.java)

</details>

<details>
<summary>TmapSettings — 도보 키·옵션·설정 변경 버전</summary>

- **무엇:** 도보 키·옵션·설정 변경 버전.
- **무엇을 사용:** Java record.
- **필요 기능·사용 효과:** 관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.
- **어떻게 확인:** `preferencesVersion()` (줄 21), `load()` (줄 34), `saveLocal()` (줄 67), `saveSearchOption()` (줄 69)
- **주요 값:** `appKey`, `searchOption`
- **코드:** [TmapSettings.java](../../src/main/java/com/datecourse/service/TmapSettings.java)
- **검증 참조:** [TmapSettingsTest.java](../../src/test/java/com/datecourse/service/TmapSettingsTest.java)

</details>

<details>
<summary>TransitWalkingRouteService — 구간별 빠른 도보·대중교통 결과 선택</summary>

- **무엇:** 구간별 빠른 도보·대중교통 결과 선택.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** 값·상태·생성자·인터페이스 선언을 확인한다.
- **코드:** [TransitWalkingRouteService.java](../../src/main/java/com/datecourse/service/TransitWalkingRouteService.java)
- **검증 참조:** [TransitWalkingRouteServiceTest.java](../../src/test/java/com/datecourse/service/TransitWalkingRouteServiceTest.java)

</details>

<details>
<summary>TravelRouteService — 이동수단에 맞는 경로 어댑터 호출</summary>

- **무엇:** 이동수단에 맞는 경로 어댑터 호출.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `route()` (줄 15)
- **코드:** [TravelRouteService.java](../../src/main/java/com/datecourse/service/TravelRouteService.java)
- **검증 참조:** [기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.

</details>

<details>
<summary>UserContext — 한 로그인의 프로필·목록·이미지·지도 준비 캐시</summary>

- **무엇:** 한 로그인의 프로필·목록·이미지·지도 준비 캐시.
- **무엇을 사용:** Java 표준 클래스.
- **필요 기능·사용 효과:** 메서드·컬렉션·문자열·수학·상태 처리를 조합한다.
- **어떻게 확인:** `courses()` (줄 41), `preview()` (줄 47), `invalidateCourses()` (줄 44), `close()` (줄 73)
- **중첩 타입:** `PreviewKey`, `CachedValue`
- **코드:** [UserContext.java](../../src/main/java/com/datecourse/service/UserContext.java)
- **검증 참조:** [UserContextTest.java](../../src/test/java/com/datecourse/service/UserContextTest.java)

</details>

<details>
<summary>WalkingRouteService — TMAP 보행 구간 조회·파싱·짧은 캐시</summary>

- **무엇:** TMAP 보행 구간 조회·파싱·짧은 캐시.
- **무엇을 사용:** Jackson · HttpClient.
- **필요 기능·사용 효과:** JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다. 외부 HTTP 요청과 응답 처리를 제공한다.
- **어떻게 확인:** `WalkingRouteService()` (줄 35), `route()` (줄 41), `leg()` (줄 59), `nonnegative()` (줄 128)
- **중첩 타입:** `CacheKey`, `Cached`
- **코드:** [WalkingRouteService.java](../../src/main/java/com/datecourse/service/WalkingRouteService.java)
- **검증 참조:** [WalkingRouteServiceTest.java](../../src/test/java/com/datecourse/service/WalkingRouteServiceTest.java)

</details>
