# 구현 흐름 한눈에 보기

[전체 참조 자료](README.md) · 소스 기준 `a0d9436` · 2026-11-25

**사용자가 하는 일 → 처리하는 클래스 → 사용하는 기술 → 결과** 순서로 읽는다. 네모는 처리, 마름모는 판단, 원통은 영구 저장이다. 화살표는 실행·데이터 전달 순서이며 모든 단계가 같은 스레드에서 실행된다는 뜻은 아니다.

## 1. 전체 사용자 흐름

```mermaid
%%{init: {"flowchart": {"curve": "linear", "rankSpacing": 28, "nodeSpacing": 20}, "themeVariables": {"fontFamily": "system-ui, sans-serif", "fontSize": "16px"}}}%%
flowchart TD
    A["① 로그인<br/>Swing · MemberRepository"] --> B["② 기본 조건 설정<br/>출발지 · 인원 · 전체 반경 · 이동수단"]
    B --> C["③ 다음 장소 검색<br/>활동 · 가격 · 유형 · 특징"]
    C --> D["④ 후보 비교 · 장소 선택<br/>목록 ↔ 지도 · 상세 카드"]
    D --> E{"코스를 더 편집?"}
    E -->|예| C
    E -->|완료| F["⑤ 계획 확정 · 저장<br/>RoutePlan → SavedCourseRepository"]
    F --> G["⑥ 저장한 코스 다시 열기<br/>이미지 · 지도 · 장소 목록"]
```

**왜 이렇게 나누는가:** 조건 입력·후보 탐색·확정·영구 저장을 구분해 편집 중인 초안과 저장된 코스를 분리한다. [화면별 코드](04-screens-and-map.md)

## 2. 기본 설정에서 앱 실행까지

```mermaid
%%{init: {"flowchart": {"curve": "linear", "rankSpacing": 28, "nodeSpacing": 20}, "themeVariables": {"fontFamily": "system-ui, sans-serif", "fontSize": "16px"}}}%%
flowchart TD
    A["① Gradle Wrapper<br/>동일한 빌드 도구 사용"] --> B{"JDK 21 검색"}
    B -->|설치됨| C["로컬 JDK 21 선택"]
    B -->|없음| D["Foojay resolver<br/>개인 Gradle 캐시에 다운로드"]
    C --> E["② toolchain 21 · release 21<br/>컴파일 · 검사 · 앱 실행 기준"]
    D --> E
    E --> F["③ AppLauncher → MembershipApp<br/>Swing 회원 화면"]
    F --> G["④ JFXPanel → JavaFX<br/>FXML 코스 화면 · WebView 지도"]
```

**왜 사용하는가:** Wrapper는 빌드 도구를 맞추고, toolchain은 요구 JDK를 선택하며, release는 대상 API·바이트코드를 제한한다. Gradle 자체 시작 JVM은 별도다. [버전·설정 파일](01-project-and-stack.md)

## 3. 모델이 화면·계산·저장을 연결하는 구조

```mermaid
%%{init: {"flowchart": {"curve": "linear", "rankSpacing": 28, "nodeSpacing": 20}, "themeVariables": {"fontFamily": "system-ui, sans-serif", "fontSize": "16px"}}}%%
flowchart TD
    A["장소 원본 · DB<br/>출처와 가격 근거"] --> P["Place + PlaceDetails<br/>한 장소의 식별 · 위치 · 상세"]
    P --> Q["PriceEstimate<br/>가격 범위 · 단위 · 근거"]
    Q --> C["CostEstimate<br/>인원을 적용한 예상 금액"]
    P --> R["RoutePlan<br/>사용자가 고른 순서 · 반경 · 수단"]
    R --> S[("SavedCourse<br/>저장 당시 계획 · 장소 스냅샷")]
    R --> T["실제 길찾기 요청<br/>공급사 API 조회"]
    T --> V["TravelRoute<br/>실제 구간 · 좌표 · 거리 · 시간"]
```

**왜 분리하는가:** 계획은 사용자의 선택이고 경로는 외부 조회 결과다. 가격은 미확인·추정을 보존해 알 수 없는 값을 확정 금액에 섞지 않는다. [모델 책임·불변성·동일성](02-models.md)

## 4. 다음 장소 추천 — 조건을 먼저, 표시 개수는 마지막에

```mermaid
%%{init: {"flowchart": {"curve": "linear", "rankSpacing": 28, "nodeSpacing": 20}, "themeVariables": {"fontFamily": "system-ui, sans-serif", "fontSize": "16px"}}}%%
flowchart TD
    A["① 화면 입력<br/>onRecommendNext"] --> B["② 조건 객체 · 검증<br/>NextPlaceCondition"]
    B --> C["③ 전체 범위 안의 장소<br/>nearby: 활동 · 좌표 · 동일성"]
    C --> D["④ 직전 장소에서 갈 수 있는 범위<br/>다음 반경 검사"]
    D --> E["⑤ 가격 · 선택 중복 · 유형 · 특징<br/>모든 필수 조건 함께 검사"]
    E --> F["⑥ 전체 일치 결과<br/>직선 거리순 · 동률 recordId"]
    F --> G["⑦ 표시 개수 제한<br/>5 · 10 · 20 · 30곳"]
    G --> H["⑧ 사용자가 선택<br/>onAddCandidate → 편집 초안"]
    F -. 일치 결과 없음 .-> I["조건 변경 대안 안내<br/>조건을 자동으로 바꾸지는 않음"]
```

**왜 이 순서인가:** 가까운 일부를 먼저 자르면 그 뒤에 있는 조건 일치 장소를 놓칠 수 있다. 전체 후보에 조건을 적용한 뒤 화면 표시 수를 제한한다. **정렬은 직선 거리 기준**이며 실제 시간 최적화·개인화 학습은 아니다. [메서드·근거·회귀 검사](03-recommendation.md)

### 두 반경은 서로 다른 질문에 답한다

```mermaid
%%{init: {"flowchart": {"curve": "linear", "rankSpacing": 28, "nodeSpacing": 20}, "themeVariables": {"fontFamily": "system-ui, sans-serif", "fontSize": "16px"}}}%%
flowchart TD
    P["후보 장소 P"] --> A{"출발지 S 기준<br/>전체 반경 R 이내?"}
    A -->|아니오| X["후보에서 제외"]
    A -->|예| B{"직전 장소 L 기준<br/>다음 반경 r 이내?"}
    B -->|아니오| X
    B -->|예| C["범위 조건 통과<br/>가격 · 유형 · 특징도 검사"]
```

| 검사 | 막으려는 상황 | 예시 |
|---|---|---|
| 전체 반경 | 장소를 따라가며 출발지에서 계속 멀어짐 | 전체 2km인데 출발지에서 2.1km → 제외 |
| 다음 반경 | 한 구간을 지나치게 멀리 이동함 | 다음 500m인데 직전 장소에서 700m → 제외 |

두 조건의 **교집합**이다. 첫 장소는 직전 장소 대신 출발지를 기준으로 한다. 반경 안이라는 사실은 실제 도로 접근 가능성이나 짧은 이동시간을 보장하지 않는다.

## 5. 지도 표시와 실제 길찾기는 별도 흐름

```mermaid
%%{init: {"flowchart": {"curve": "linear", "rankSpacing": 28, "nodeSpacing": 20}, "themeVariables": {"fontFamily": "system-ui, sans-serif", "fontSize": "16px"}}}%%
sequenceDiagram
    actor U as 사용자
    participant C as 화면 Controller
    participant M as CourseMapView
    participant S as TravelRouteService
    participant J as WebView 지도
    U->>C: 장소 선택 · 코스 편집
    C->>M: RoutePlan · 후보 · 선택 상태
    M->>J: JSON으로 마커 · 계획 표시
    J-->>M: 마커/경로 선택 이벤트
    M-->>C: MapEventBridge 콜백
    U->>M: 실제 경로 조회 요청
    M->>S: 장소 순서 · 수단 · 설정
    Note over S: 배경 작업에서 공급사 API 조회
    S-->>M: TravelRoute
    Note over M: 요청 번호 · 세션 · 설정 버전 확인
    M->>J: 현재 요청이면 실제 경로 표시
```

**왜 사용하는가:** WebView는 웹 지도 SDK를 JavaFX 화면에 넣고, 브리지는 Java와 JavaScript의 선택·표시를 연결한다. 요청 검사는 늦은 응답이 새 코스에 섞이는 것을 막는다. [지도 모듈·비동기·포커스](04-screens-and-map.md)

## 6. 로그인과 코스 저장

```mermaid
%%{init: {"flowchart": {"curve": "linear", "rankSpacing": 28, "nodeSpacing": 20}, "themeVariables": {"fontFamily": "system-ui, sans-serif", "fontSize": "16px"}}}%%
flowchart TD
    A["가입 시 비밀번호"] --> B["PasswordHash.create<br/>개별 salt + PBKDF2 반복 계산"]
    B --> C[("회원 DB<br/>형식 · 반복 수 · salt · 해시")]
    D["로그인 입력"] --> E["MemberRepository.login<br/>회원 조회 → PasswordHash.verify"]
    C --> E
    E --> F{"인증 성공?"}
    F -->|예| G["UserSession · UserContext<br/>회원별 조회 · 캐시"]
    F -->|아니오| H["실패 안내"]
```

```mermaid
%%{init: {"flowchart": {"curve": "linear", "rankSpacing": 28, "nodeSpacing": 20}, "themeVariables": {"fontFamily": "system-ui, sans-serif", "fontSize": "16px"}}}%%
flowchart TD
    A["확정한 RoutePlan"] --> B["SavedCourseRepository.persist<br/>회원 소유 조건 · 트랜잭션 시작"]
    B --> C["코스 본문 · 방문 순서 · 반경 · 수단<br/>장소 스냅샷 저장"]
    C --> D{"정상 처리?"}
    D -->|예| E["commit → 목록 캐시 무효화"]
    D -->|실패| F["rollback → 오류 안내"]
    E --> G["findAll → readPlaceSnapshot<br/>저장 당시 코스 복원"]
```

**왜 사용하는가:** JDBC 트랜잭션은 본문과 방문 목록이 일부만 저장되는 일을 막고, 스냅샷은 원본 변경과 별개로 저장 당시 정보를 복원한다. 현재 Planner는 `saveWithoutPreview`를 사용한다. [DB·해시·동시 편집 한계](05-storage-and-account.md)

### 저장 이미지와 지도 준비 상태

```mermaid
%%{init: {"flowchart": {"curve": "linear", "rankSpacing": 28, "nodeSpacing": 20}, "themeVariables": {"fontFamily": "system-ui, sans-serif", "fontSize": "16px"}}}%%
flowchart TD
    A["저장 코스 선택"] --> B{"이미지 확인 상태"}
    B -->|확인 중| C["로딩 유지<br/>이미지 없음으로 간주하지 않음"]
    B -->|이미지 있음| D["저장 PNG 표시"]
    D --> E{"지도 탭 선택"}
    E -->|이번 세션에서 준비됨| H["지도 표시"]
    E -->|아직 준비 안 됨| F["지도 불러오기 버튼"]
    F -->|사용자가 누름| H
    B -->|이미지 없음 확인| H
    H --> I["실제 경로는 별도 요청<br/>기본 연결과 구분"]
    H -. 지도 실패 .-> J["로컬 연결도"]
```

**DB에 저장한 PNG**, **로그인 세션의 지도 준비 여부**, **현재 요청의 실제 길찾기 결과**는 보관 위치와 수명이 다르다.

## 7. 공공 원본에서 추천에 쓰이는 자료까지

```mermaid
%%{init: {"flowchart": {"curve": "linear", "rankSpacing": 28, "nodeSpacing": 20}, "themeVariables": {"fontFamily": "system-ui, sans-serif", "fontSize": "16px"}}}%%
flowchart TD
    A["① 공공 원본<br/>출처 · 기준일 · 지문"] --> B["② 정규화 · 품질 검사<br/>ID · 좌표 · 분류 · 보류"]
    B --> C["③ 지역별 확장<br/>기존 자료 보존 · 구군 분산"]
    C --> D["④ 속성 · 가격 근거 연결<br/>참고 / 추정 / 미확인 구분"]
    D --> E["⑤ 전체 파일 쌍 · current.json<br/>Java 모델 · 지문 · 보존 검증"]
    E --> F["⑥ 명시적 DB 적재<br/>트랜잭션 · 보호 · 반복 검증"]
    F --> G[("운영 카탈로그")]
    G --> H["PlaceRepository<br/>추천 · 지도 · 상세"]
```

**왜 사용하는가:** 정규화는 서로 다른 원본을 같은 모델에 연결하고, 지문·보존 검사는 기존 자료와 이용자 데이터를 지키면서 확장한 근거를 남긴다. 파일 수정과 운영 DB 반영은 별도 작업이다. [데이터 도구·출처·표본 한계](06-data-and-operations.md)

## 8. 검증과 문서 공유

```mermaid
%%{init: {"flowchart": {"curve": "linear", "rankSpacing": 28, "nodeSpacing": 20}, "themeVariables": {"fontFamily": "system-ui, sans-serif", "fontSize": "16px"}}}%%
flowchart TD
    A["코드 · 자료 · 문서 변경"] --> B["변경 범위에 맞는 검사<br/>JUnit/H2 · Python · Node"]
    B --> C["문서 대조<br/>링크 · 메서드 · 소스 기준"]
    C --> D["Quartz 로컬 빌드<br/>Markdown → 사이트"]
    D --> E["공유 가능한 원본 · 검증 기록"]
    E -. 별도 승인된 운영 절차 .-> F["원격 반영 · 사이트 배포"]
    G["팀원 사이트 접속"] --> H["Worker · GitHub OAuth<br/>접근 권한 확인"]
    H --> I["문서 · 공유 보드<br/>Durable Object / SQLite"]
```

**왜 사용하는가:** 자동 검사는 규칙의 회귀를 확인하고, 문서는 코드와 기술 선택의 근거를 공유한다. 로컬 검사 통과·푸시·운영 배포는 각각 다른 사건이다. 팀 사이트 인증은 앱 회원 인증과 별개다. [검사 범위·사이트 역할](07-verification-and-team-site.md)

## 코드 설명으로 이어가기

| 시각 자료에서 확인한 내용 | 근거를 더 볼 곳 |
|---|---|
| 사용 기술 · 버전 · 설정 | [기본 설정·스택](01-project-and-stack.md) |
| 모델 책임 · 값의 의미 | [모델 논리](02-models.md) |
| 후보를 남기고 제외하는 이유 | [추천 논리](03-recommendation.md) |
| 특정 클래스의 대표 메서드 | [Java 전체 색인](08-class-index.md) |
| SQL · 도구 · 자료 · 자원 위치 | [프로젝트 전체 색인](09-project-index.md) |
