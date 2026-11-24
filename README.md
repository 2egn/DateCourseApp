# DateCourseApp

출발지·인원·검색 범위를 정하고 장소를 하나씩 골라 코스를 만드는 Java 데스크톱 앱입니다. 도보·자동차·대중교통과 도보를 지원하며, 장소 가격의 참고값·추정값·미확인을 구분합니다.

최근 검증: **2026-11-25**. 앱 Java 테스트 **383개**, 선택 유지보수 검사 **3개 통과**. [전체 점검 결과](docs/review-2026-11-24.md)에서 수정·검증 범위와 남은 확인을 볼 수 있습니다. 실제 API·운영 DB 검증은 자동 테스트와 별도로 기록합니다.

## 빠른 시작

1. Gradle을 시작할 JDK 17 이상과 저장소의 Gradle Wrapper를 사용합니다. 설치된 JDK 21을 우선 사용하며, 없으면 운영체제·CPU에 맞는 21을 Gradle 개인 캐시에 자동 다운로드합니다. 최초 다운로드에는 인터넷 연결이 필요하고 JDK 파일은 저장소에 포함하지 않습니다. Gradle toolchain으로 컴파일·검사·앱 실행 JDK를 21로 고정하고 `--release 21`도 유지합니다. Gradle 자체 실행 JDK는 IDE·`JAVA_HOME` 설정을 따릅니다. JavaFX 21과 나머지 라이브러리는 명시한 버전을 사용합니다. IDE에서 직접 실행할 때도 Project SDK를 21로 지정합니다. 여러 JDK가 있어도 자동 검색·`JDK21_HOME`·`JAVA_HOME_21_X64`·`JAVA_HOME`에서 찾은 후보 중 21을 선택합니다. 일반 위치 밖에 설치한 21은 `JDK21_HOME`에 설치 경로를 지정합니다. [선택 규칙과 검색 실패 해결](docs/site/content/JDK21_자동_검색과_선택_2026_11_25.md)을 참고합니다.
2. [공통 접속 키 안내](docs/공통_접속_키.md)에 따라 `.local/team-access.properties`를 준비합니다. 카카오 JavaScript·REST, TMAP, Google Places 키와 DB 접속 정보를 실행 시 받아옵니다.
3. 카카오 JavaScript 키의 허용 도메인에 `http://localhost:8080`을 등록합니다. 로컬 지도 서버는 앱이 시작합니다.
4. 프로젝트 루트에서 실행합니다.

```powershell
.\gradlew.bat run
```

IntelliJ 실행 진입점은 `com.datecourse.app.AppLauncher`이고 Working directory는 프로젝트 루트입니다. 공통 설정을 사용하지 않는 개별 키·DB 설정은 [공통 접속 키 안내](docs/공통_접속_키.md)와 [실행 및 테스트](docs/site/content/실행_및_테스트.md)를 참고하세요. 실제 키·접속 정보는 커밋하지 않습니다.

현재 변경의 상세와 세션 처리: [지도 선택·저장 지도 준비](docs/site/content/지도_선택과_저장_지도_준비_2026_11_24.md).

화면 가독성 개선: [작은 창 배치·정보 역할·대비](docs/site/content/화면_가독성과_작은_창_배치_2026_11_24.md).

지도 이동 개선: [렌더링 여유 영역과 좌표·캡처 보정](docs/site/content/지도_렌더링_여유_영역_2026_11_24.md).

입력 오류 수정과 추가 UX 검토: [마우스·키보드 입력 점검](docs/site/content/마우스_키보드_입력_점검_2026_11_24.md).

키보드 조작 개선: [저장 장소·편집 목록·상세 포커스 복귀](docs/site/content/키보드_조작과_포커스_복귀_2026_11_25.md).

## 현재 사용 흐름

로그인 → 출발지·인원·전체 반경 → 장소별 조건·후보 검색 → 상세 카드에서 추가 → 편집 완료 → 코스 저장·다시 열기 순서입니다.

| 기능 | 현재 동작 |
|---|---|
| 조건·후보 | 활동·유형·특징·가격·반경으로 거리순 후보를 찾고 5·10·20·30곳 중 표시 개수를 선택합니다. |
| 편집 | 내 코스·다음 장소 조건·추천 후보 중 하나를 펼칩니다. 추가·삭제·순서·구간 반경 변경은 편집 초안에 적용합니다. |
| 이동 방법 | 도보는 TMAP, 자동차와 대중교통은 카카오 기반입니다. 대중교통은 도보 구간을 포함하며 상세 정책은 [혼합 이동과 범위](docs/transport-walking-and-ranges-2026-11-17.md)를 참고합니다. |
| 지도 안내 | 장소·경로 호버는 한 개의 짧은 안내, 클릭은 왼쪽 상세 카드와 오른쪽 구간 안내로 연결됩니다. 기본 반투명에서 호버·선택 시 강조하고 마커 입력을 경로보다 우선합니다. 상시 범례 대신 원 테두리 호버로 범위·반경을 안내합니다. |
| 저장한 코스 | 상단 이미지·지도 전환을 항상 제공합니다. 저장 PNG를 우선 표시하고 이미지 전용 코스는 지도 탭의 `지도 불러오기`로 준비합니다. 불러온 여부는 로그인 세션에서만 기억하고 코스 목록 갱신·로그아웃 시 비웁니다. 이미지가 없으면 지도와 직선 연결로 보여줍니다. 지도 실패 시 로컬 연결도를 사용합니다. 실제 이동 경로는 버튼을 눌렀을 때만 조회합니다. |
| 사용자 상태 | `UserContext`에서 사용자와 저장 목록·미리보기 캐시를 관리하고 변경·로그아웃 시 무효화합니다. |
| 관리자 | 관리자 인증 후 회원 관리와 장소 정정 제보 승인·반려를 처리합니다. 일반 사용자에게 관리 기능을 제공하지 않습니다. |

반경 최솟값은 200m이며, 최댓값은 `TravelMode`가 정의합니다. 추천 거리는 직선 거리이고 실제 이동시간·체류시간과 구분합니다.

| 이동 방법 | 전체 반경 최대 | 구간 반경 최대 |
|---|---:|---:|
| 도보 | 2km | 2km |
| 자동차 | 30km | 60km |
| 대중교통 + 도보 | 20km | 40km |

운영 공공 카탈로그는 **2,040곳**으로 확장했다. 내장 시연 386곳은 유지한다. [확장·보존 검증 결과](docs/busan-core-expansion-2026-11-23.md)와 [현재 기준본](data/places/expansion/current.json)을 참고한다.

운영 DB는 비어 있는 이전 백업 8개 정리 후 15개 테이블이다. 원본·사용자·코스·변경 이력은 보존했다. [현재 DB 구조](docs/site/content/DB_구조와_빈_백업_정리_2026_11_24.md)와 [복원 SQL·검증 기록](archive/db-schema/2026-11-24-empty-backups/README.md)을 참고한다.

가격 보완은 매장 참고값과 통계 추정을 구분한다. [지역 외식비·커피 조사 적용](data/places/pricing/2026-11-24/README.md)에서 근거·제약·적재 검증을 확인한다.

카탈로그 갱신·비활성·변경 이력은 JDBC 배치로 처리한다. 신규 ID 생성은 기존 절차를 유지하며 운영 성능 수치는 아직 측정하지 않았다. [적재 도구 안내](tools/catalog-ops/README.md)를 참고한다.

## 코드와 문서 찾기

주요 폴더·파일 이름을 누르면 해당 경로로 이동합니다. 코드 내부 구조는 [코드 탐색 안내](src/navigate.md)에서 확인합니다.

<pre>
DateCourseApp/
├─ <a href="src/">src/</a>                 앱 코드·화면·지도·자동 테스트
│  └─ <a href="src/navigate.md">navigate.md</a>        기능별 코드 위치와 읽는 순서
├─ <a href="data/">data/</a>                장소 원본·가격 근거·DB 스키마
│  └─ <a href="data/README.md">README.md</a>          자료 기준본과 DB 준비·적재 안내
├─ <a href="docs/">docs/</a>                기능 설명·검토 기록·팀 문서 사이트
│  ├─ <a href="docs/README.md">README.md</a>          문서 탐색과 팀 사이트 안내
│  ├─ <a href="docs/architecture.md">architecture.md</a>    현재 책임 분담과 상태·비동기 처리 원칙
│  └─ <a href="docs/site/content/">site/content/</a>      팀 사이트에 게시하는 문서 원본
├─ <a href="tools/">tools/</a>               데이터 수집·검증·적재 보조 도구
│  └─ <a href="tools/README.md">README.md</a>          도구별 용도와 실행 방법
├─ <a href="archive/">archive/</a>             기본 빌드에서 제외한 예제·복원 자료
├─ <a href="gradle/">gradle/</a>              Gradle Wrapper 실행 구성
├─ <a href="build.gradle">build.gradle</a>         의존성·앱 실행·검사·관리 작업 정의
└─ <a href="AGENTS.md">AGENTS.md</a>            변경·호환성·검증·문서 갱신 지침
</pre>

앱 코드는 `com.datecourse`로 통일했습니다. JavaFX 컨트롤러와 FXML은 같은 페이지 경로를 사용합니다. [페이지 이름 대응](docs/site/content/페이지_구조와_이름_2026_11_24.md) · [패키지 통합·구조 점검](docs/site/content/패키지_통합과_구조_점검_2026_11_24.md) · [현재 구현 흐름](docs/site/content/현재_구현_흐름.md)

<details>
<summary>개발 검증과 시연 명령</summary>

```powershell
.\gradlew.bat test
.\gradlew.bat runPilotDemo
node --test docs/site/test/*.test.mjs
```

`test`는 모의 지도·공급사와 H2로 동작을 검사합니다. `runPilotDemo`는 회원 DB 없이 별도 시연 목록을 사용하며, 가격 미확인 장소는 해당 포함 옵션이 필요합니다. 선택 검사 `tmapLiveTest`는 실제 API 호출이 발생하므로 자동 테스트와 구분합니다.

지도·FXML 캡처는 `build/ui-preview`, 테스트 결과는 `build/reports/tests/test`에서 확인합니다. Node 검증에는 Node 22 이상을 사용합니다.

</details>

<details>
<summary>DB 준비·관리자 유지보수</summary>

스키마와 마이그레이션은 [data 안내](data/README.md), 저장 코스 연결은 [코스 저장과 다시 열기](docs/코스_저장과_다시열기.md)를 참고합니다. 실행 중 팀 DB를 이용하므로 일반 테스트에서 운영 데이터 적재·삭제 명령을 실행하지 않습니다.

일반 앱은 DB 구조를 확인만 하며 테이블·관리자 계정을 자동 생성하지 않습니다. 기본 스키마와 기존 DB의 필요한 마이그레이션을 준비한 관리자는 추가 구조를 아래 명령으로 준비합니다. 이미 준비된 운영 DB에 이번 코드 변경 때문에 재실행할 필요는 없습니다.

```powershell
.\gradlew.bat setupDatabase
```

준비 범위·선행 SQL·부분 실행은 [DB 준비 안내](data/README.md#명시적-db-준비)를 참고합니다. `setupInformationReports`·`setupCoursePreviews` 명령도 유지합니다.

관리자 비밀번호 재설정은 아래 명령의 마스킹 입력창을 사용합니다. 비밀번호를 명령 인수·문서·채팅에 넣지 않습니다.

```powershell
.\gradlew.bat resetAdminPassword
```

</details>

<details>
<summary>배포 경계와 남은 확인</summary>

- `db.properties`, 실제 지도 키, TMAP 키는 빌드 리소스·JAR에서 제외합니다. Git 제외와 배포 제외를 함께 유지합니다.
- 현재 앱은 팀용으로 DB와 공급사 키를 실행 중 전달받습니다. 공개 사용자 배포를 위해서는 서버 경계 재설계가 필요합니다.
- 실제 로그인·회원별 저장·관리자·공급사 경로의 최종 수동 시연과 부산 전체 데이터 품질 점검은 자동 테스트와 별도로 수행합니다.
- 과거 `controller.Main`, `MyPage`, `CourseRecommend`, `FeedBack`, `login`, `UiTheme` 예제는 현재 master 통합 과정에서 제거됐습니다. 미연결 UI 예제는 `archive/legacy-ui`로 보관했고, 없는 로그인 화면을 여는 이전 `MainApp`은 삭제했습니다.
- 팀 문서 게시 원본은 `docs/site/content`입니다. 저장소 문서 갱신과 운영 Worker 재배포는 별개입니다.

</details>

## 브랜치 운영

기능 브랜치에서 최신 master와의 병합 가능성·빌드·테스트를 확인한 뒤 master에 반영합니다. 팀원의 작업 폴더나 브랜치를 강제로 초기화하지 않습니다.

대표 페이지는 `_01step_planner` → `_02step_editor` → `_03step_saved` 순서다. [미사용 코드 보관·삭제 내역](archive/legacy-ui/README.md)을 참고한다. 예제 전용 검사 2개도 함께 보관했으며, 실제 앱 검사와 기본 빌드에서는 제외한다.
