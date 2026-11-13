# DateCourseApp 팀원 전용 문서 사이트

문서 변환은 Quartz 5.1.0, 호스팅·인증 관문은 Cloudflare Workers를 사용합니다. GitHub 로그인 연결과 소유자 로그인을 확인했습니다.

주소: https://datecourseapp-team-docs.ate-ourse-pp.workers.dev

## 접근 기준

- 대상: `2egn/DateCourseApp`의 소유자와 **초대 수락한 Collaborators**.
- 커밋 기여자(Contributors), 단순 방문자·Star·Fork 사용자는 허용하지 않습니다.
- GitHub OAuth 로그인 후 `/user/repos?affiliation=owner,collaborator`에서 대상 저장소의 고정 ID `1390711615`를 확인합니다.
- 현재 대상 저장소가 공개이므로 별도의 `repo` 쓰기 scope를 요청하지 않습니다. 저장소를 비공개로 바꾸거나 조직으로 이전하면 인증 설정을 재검토합니다.
- 각 문서·첨부·검색 JSON·JS 요청에서 권한을 다시 확인합니다. API 오류 시 문서를 제공하지 않습니다.
- `assets.run_worker_first=true`를 유지해야 합니다. 공개 Pages·별도 정적 호스팅에 같은 파일을 배포하면 안 됩니다.

## 로컬 검증

```powershell
node --test docs/site/test/*.test.mjs
```

이 테스트는 GitHub를 모의 응답으로 대체합니다. 실제 OAuth 앱·팀원 계정·비팀원 계정 검증은 배포 전후 별도로 해야 합니다.

로그인 상태는 시도별 쿠키로 분리해 여러 탭의 요청이 서로 덮어쓰지 않게 합니다. 비로그인 문서 탐색만 로그인으로 이동하며, favicon·검색 JSON·이미지 같은 부가 요청은 401로 차단합니다. 로그인 실패 화면의 `AUTH_COOKIE_MISSING`, `AUTH_STATE_EXPIRED`, `AUTH_STATE_MISMATCH`는 비밀값 없이 실패 단계를 구분하는 코드입니다. 상태 검증을 생략해 통과시키지 않습니다.

## 문서 준비

현재 코드 기준은 `ea2242b`입니다. 변경 클래스 32개·검사 노트 12개와 현재 전체 흐름을 갱신했습니다. `board-nov14.mjs`는 첫 보드 접근 시 구현 상태를 한 번 동기화합니다. 보관 카드와 담당자·우선순위·선행 관계를 유지하며 최신 근거를 설명 끝에 추가합니다. 새 카드에는 최종 시연과 미구현 시간·교통수단 검토를 구분합니다.

리뷰를 반영할 때 `content/업데이트_내역.md`에 이전/이후와 기준 커밋을 먼저 기록합니다. `content/리뷰_및_구현_방향.md`는 `d32bc76` 리뷰 요약이며 앱 수정 완료 목록과 구분합니다. 새 클래스 노트의 `source_commit`을 유지하고, 초기 작성본과 최신 소스가 다른 기존 페이지에는 버전 안내를 붙입니다. 상단 업데이트 메뉴와 첫 화면에서 변경 내역을 찾을 수 있습니다.

원본은 OneDrive의 `SchoolStudies/Java/프로젝트/DateCourseApp` 폴더입니다. 이 폴더만 사이트로 복사하고 Obsidian 보관함 기준 링크 접두어만 웹용으로 바꿉니다. 다른 공부 노트는 가져오지 않습니다.

이번 로컬 검증은 `build/docs-tools/quartz`에 공식 Quartz 5.1.0을 설치하고 `build/prepare-quartz.mjs`로 구성했습니다. 생성 결과는 이 폴더의 `public/`입니다. 이 내용은 `.gitignore`로 제외하며 공개 앱 저장소에 커밋하지 않습니다.

정식 운영용 빌드 환경은 인증·주소가 확정된 뒤 따로 고정합니다. `build/`는 Gradle clean 시 삭제될 수 있는 임시 검증 공간입니다.

## 계정 생성 다음 순서

1. 이 폴더에서 `npx wrangler login`으로 Cloudflare에 로그인합니다.
2. `npx wrangler deploy`로 인증 설정 전에는 **503만 반환하는** Worker를 먼저 등록하고 `https://datecourseapp-team-docs.<계정이름>.workers.dev` 주소를 확인합니다. `public/`가 없으면 먼저 문서를 빌드해야 합니다.
3. GitHub → Settings → Developer settings → OAuth Apps → New OAuth App에서 다음을 입력합니다.

| 항목 | 값 |
|---|---|
| Application name | DateCourseApp Team Docs |
| Homepage URL | 위 Worker의 HTTPS 주소 |
| Authorization callback URL | 위 주소 + `/auth/callback` |

4. `wrangler.jsonc`의 `SITE_ORIGIN`을 실제 HTTPS 주소, `GITHUB_CLIENT_ID`를 발급받은 Client ID로 바꿉니다. 주소에는 마지막 `/`를 넣지 않습니다.
5. 다음 비밀값은 Cloudflare에만 입력합니다. 채팅·Git·문서에는 적지 않습니다.

```powershell
npx wrangler secret put GITHUB_CLIENT_SECRET
npx wrangler secret put SESSION_KEY
```

`SESSION_KEY`는 암호학적으로 무작위인 32바이트를 64자리 hex로 표현한 값입니다. 비밀값 입력란을 열어 둔 상태에서 별도의 로컬 생성 도구로 만들고 붙여 넣습니다. 테스트의 고정 키는 사용하지 않습니다.

6. `npx wrangler deploy`로 인증 설정을 반영합니다. 문서 사이트 전용 유료 플랜·도메인을 구매할 필요는 없습니다. Workers Free의 사용량 한도 안에서 운영합니다.
7. 소유자·팀원은 열림, 비팀원·비로그인은 내용이 안 보임을 각각 확인합니다. `/static/contentIndex.json` 직접 접근도 확인합니다.

## 실제 사용 전 확인할 것

- OAuth 앱과 Client ID는 등록했습니다. Client Secret은 Cloudflare의 `GITHUB_CLIENT_SECRET` 비밀값으로 직접 설정해야 합니다.
- 실제 GitHub 로그인에서 public repo의 명시적 협업 목록이 기대대로 반환되는지 검증해야 합니다. 결과가 다르면 인증 기준을 느슨하게 바꾸지 않고 GitHub App 등 읽기 전용 권한 방식으로 조정합니다.
- 기본 `SITE_ORIGIN`과 Client ID가 비어 있으면 문서를 제공하지 않습니다.
- HTML·검색·원본 문서를 공개 앱 저장소나 공개 Actions 산출물에 올리지 않습니다. 문서용 별도 저장소를 만들면 비공개로 둡니다.
- 삭제된 팀원의 접근 제한은 GitHub API가 현재 반환하는 권한 결과를 따릅니다.

## 공식 자료

## 공동 작업 보드

문서와 보드는 상단 메뉴로 분리합니다. 게시 준비 계획과 대시보드 안내 노트는 웹 문서 목록·검색에서 제외하고 기존 URL은 해당 영역으로 안내합니다. Obsidian 원본은 보존합니다. 기본 Quartz 홍보 링크와 공통 태그·속성 노출도 줄였습니다.

`content/`에는 사이트에서 직접 관리하는 시작 화면과 clone 직후의 Windows 실행·테스트 안내를 보관합니다. OneDrive에서 가져오는 학습 노트 위에 적용하므로 재빌드해도 유지됩니다. `web/navigation.css`와 빌드 단계가 두 영역의 공통 메뉴를 생성합니다.

클래스 흐름도의 함수 노드에 마우스를 올리거나 Tab으로 포커스하면 함수표의 간략 설명을 표시합니다. 여러 함수는 각각 구분하며 Esc로 닫습니다. `tools/flow-methods.mjs`가 기존 함수표를 읽고 부족한 흐름도 호출 설명을 웹 복사본의 표에 보충합니다. 표로부터 페이지별 설명 데이터를 생성하고 `web/flow-tooltips.js`가 Mermaid SVG에 연결합니다. 설명이 누락된 함수는 빌드를 중단하므로 새 흐름도 호출을 추가할 때 함께 갱신합니다. `node docs/site/tools/check-flow-tooltips.mjs`로 전체 도표 문법, 호버·키보드·문서 간 이동·모바일 배치를 확인합니다.

- `/dashboard/`: 할 일 / 진행 중 / 검토·테스트 / 완료. 기능 영역·우선순위 필터, 검색, 생성·수정·보관·복원, 담당자와 관련 노트 연결.
- `src/board-store.mjs`: SQLite Durable Object에 영구 저장. `wrangler.jsonc`의 `BOARD` 바인딩과 `task-board-v1` 마이그레이션이 필요합니다. Workers Free의 SQLite 지원 범위에서 사용합니다.
- `/api/board` 읽기와 `/api/board/tasks` 생성, `/api/board/tasks/:id` 수정은 같은 GitHub 팀원 인증을 거칩니다. 수정은 동일 출처 요청만 받고 사용자 ID와 변경 시각은 서버에서 기록합니다.
- 버전이 다른 수정 요청은 409로 거부합니다. 화면 입력을 유지하며 최신 카드를 다시 열어 비교하도록 안내합니다. 삭제 대신 보관합니다. 다른 화면의 변경은 새로고침 또는 30초 주기 갱신으로 확인합니다.
- 첫 저장소 생성 때 기본 16개와 사이트 개선 이력 4개를 등록합니다. 기존 보드의 나머지 작업을 재배포로 다시 만들거나 사용자의 변경을 덮어쓰지 않습니다.
- `node docs/site/tools/build.mjs`: `build/docs-tools/quartz`의 Quartz 5.1.0으로 문서와 대시보드를 함께 빌드합니다. `DOCS_NOTES_DIR`로 문서 원본 폴더를 지정할 수 있습니다. 원본을 보존하고 웹 복사본의 `00_`만 제거합니다. 그래프 뷰는 비활성화하며 Mermaid 흐름도는 유지합니다.
- `node docs/site/tools/check-ui.mjs`: Chrome/Playwright와 모의 인증 저장소로 생성·이동·두 화면 공유·충돌·보관·복원·모바일 레이아웃을 검사합니다. 실제 팀원 계정 검증과 구분합니다.

## 처음 보는 팀원을 위한 작업 보드

현재 우선순위는 출시 품질이 아닌 과제 시연 기준입니다. 높음은 핵심 흐름 필수, 보통은 시연 보완, 낮음은 선택·후속입니다. `src/assignment-priorities.mjs`가 45개 할 일의 중요도와 이유를 버전·원문 검사 후 한 번 반영합니다. 진행 중이거나 보관됐거나 다른 팀원이 수정한 카드는 건너뛰며, 기존 상태·선행 관계·담당자·완료 기준은 유지합니다. 모든 완료 기준을 구현해야 과제 제출이 가능하다는 뜻은 아니며, 선택 카드는 보류할 수 있습니다.

카드 제목은 사용자에게 보이는 행동으로, 설명은 첫 줄의 쉬운 요약과 필요한 개발 참고로 나눕니다. 완료 기준은 실제 화면에서 확인할 수 있는 결과로 씁니다. 기술 용어를 제목에서 줄여도 설명에 남겨 검색할 수 있습니다. 분야 이름·진행 상태 설명·용어 사전·중요도 기준은 보드 안에서 접어 볼 수 있습니다. 각 상태 열은 중요도가 높은 순서이며, 먼저 할 작업은 번호와 제목을 함께 보여 주고 해당 카드를 열어 줍니다.

`src/board-copy.mjs`는 2026-11-13 확인한 53개 작업의 문구 변경안입니다. `TaskBoard` 시작 시 기존 버전과 원문이 모두 일치하는 카드에만 한 번 적용합니다. 상태·중요도·분야·담당자·보관 여부·관련 문서는 유지하며, 문구 수정 버전을 올려 열려 있던 오래된 편집본의 저장을 막습니다. 그 사이 팀원이 바꾼 카드는 건너뛰고 결과를 `board_meta`에 기록합니다. 이후 재배포로 문구를 되돌리지 않습니다. 신규 저장소의 초기 카드에도 쉬운 문구를 사용합니다.

`test/board-store.test.mjs`에서 문구·선행 작업 연결·상태 보존·동시 수정 보호·일회성 적용을 검사합니다. 기존 `tools/check-beginner-board.mjs` 명령은 현재 하위 목록 검증 도구 `tools/check-board-hierarchy.mjs`로 연결됩니다. 검토 스냅샷은 게시 파일에 포함하지 않습니다.

### 선행 작업과 하위 목록

접힌 하위 목록에는 바로 아래 작업들의 제목을 간략하게 표시하며, 펼치면 설명과 상태 변경 메뉴를 보여줍니다.

`먼저 끝내야 할 작업`에 카드 번호를 입력하면 모든 선행 카드의 상태·분야가 현재 작업과 같을 때만 하위 목록에 표시합니다. 다른 상태 열이나 분야 탭의 선행 작업이 있으면 독립 카드로 남기고 연결은 유지합니다. 상태·분야를 바꾸면 표시 구조도 즉시 다시 계산합니다. 상위 작업이 끝나도 하위 작업 상태는 바뀌지 않습니다. 여러 선행 조건이 있으면 연결이 가장 깊은 작업 아래 한 번만 표시하고, 나머지 조건은 설명 창에서 모두 확인합니다. 검색·분야·중요도 필터는 상위 경로를 함께 보여줍니다. 상위 카드의 위치는 상위 상태를 따르고, 상태 열 숫자와 전체 통계는 하위 작업을 포함합니다. 중요한 미완료 하위 작업이 있으면 상위 카드를 먼저 보여 줍니다. 서로를 기다리는 잘못된 연결은 숨기지 않고 경고합니다.

`web/dashboard/task-tree.js`가 표시와 읽기 API의 공통 선행 관계를 계산합니다. `/api/board`와 MCP `get_tasks` 응답의 `parentId`로 에이전트도 같은 구조를 확인할 수 있습니다. 작업을 삭제하거나 별도 텍스트 체크리스트로 바꾸지 않으므로 담당자·상태·분야·중요도·기존 ID는 유지됩니다.

`src/board-audit.mjs`는 d32bc76 대조 결과를 한 번 적용합니다. DB-02는 원격 구현 근거가 없어 할 일로 정리하며, 문서 완료 범위를 초기 작성으로 명확히 합니다. 실제 확인 카드에는 구현·자동 확인 범위를 적고, 불필요한 선행 연결을 정리합니다. 현재 사이트 소스의 Git 반영 작업과 완료된 MCP·읽음 표시·학습 예시 카드도 추가합니다. 버전·원문이 달라진 카드는 건너뛰고 `board_meta`에 기록하므로 동시 수정은 보존됩니다. 재배포로 다시 적용하지 않습니다.

`node docs/site/tools/check-board-hierarchy.mjs`는 적용 전 로컬 스냅샷 `build/board-reorg-before.json`을 사용해 체인·복수 선행 조건·검색·개별 상태 저장·보관·320px 화면을 확인합니다. 스냅샷은 게시하지 않습니다.

### 이동 속도

문서 링크의 파란 점은 아직 읽지 않은 문서 업데이트를 뜻합니다. `document-revisions.json`은 문서별 내용 지문과 변경 차수를 보관하며, 단순 리빌드·공통 CSS 변경은 차수를 올리지 않습니다. 이 파일은 문서와 함께 버전 관리하고 삭제하지 않습니다. `tools/document-revisions.mjs`가 변경 이력을 갱신하며 실제 문서 내용이 바뀌면 다시 표시합니다.

읽음 기록은 같은 브라우저의 localStorage에 문서별로 저장합니다. 실제 문서를 열었을 때만 읽음으로 처리하고 호버 미리보기는 포함하지 않습니다. 다른 탭의 읽음 상태도 반영하며, 이전 버전의 탭이 최신 읽음 기록을 덮어쓰지 않습니다. 브라우저·기기 간 서버 동기화는 하지 않습니다. `tools/check-document-updates.mjs`에서 읽기·재접속·재업데이트·탭 동기화를 검증합니다.

보드 첫 진입은 GitHub 권한 확인이 끝난 HTML 응답에 현재 카드 JSON을 포함합니다. 첫 목록 API 재호출을 생략하며, 문서·API와 동일하게 캐시하지 않습니다. 저장소 오류로 초기 전달에 실패하면 기존 API 조회로 복구합니다. 관련 노트 목록은 편집 창을 열 때 불러옵니다. `tools/check-initial-board.mjs`가 실제 Worker 처리와 브라우저를 함께 사용해 정상·실패 복구 경로를 검사합니다.

`web/responsive-navigation.js`는 800px 경계를 지날 때 모바일 탐색기를 접고 스크롤 잠금을 해제합니다. 넓은 화면으로 돌아오면 이전 펼침 상태를 복원하며, 모바일 수동 열기·Esc 닫기·문서 전환도 지원합니다. `tools/check-responsive-navigation.mjs`에서 검색 버튼 접근과 화면 전환을 검증합니다.

`tools/optimize-assets.mjs`가 문서의 CSS 21개와 분리된 JavaScript 모듈을 각각 묶고 공통 파일은 문서 간 전환에서 유지합니다. 로그인·문서·검색 JSON·보드 API의 권한 검사 및 `private, no-store` 정책은 유지합니다. 동시에 들어온 같은 토큰의 권한 확인만 진행 중 Promise를 공유하며, 완료된 결과는 캐시하지 않습니다.

보드 저장은 서버 응답의 카드와 버전을 즉시 반영하며 저장 직후 전체 목록 재요청을 생략합니다. 자동 갱신 중에도 편집 창을 열 수 있고, 열려 있는 편집본은 백그라운드 결과로 덮어쓰지 않습니다. 다른 팀원의 변경은 기존 30초 갱신·수동 새로고침으로 확인합니다.

`tools/check-navigation-performance.mjs`는 `build/perf-before-site`와 새 빌드를 비교하는 로컬 네트워크 지연 모의 검사입니다. 실제 사이트의 응답 시간 보장 수치와 구분합니다.

## 쉬운 문서 설명과 학습용 그림

`content/`의 문서 86개가 게시용 설명의 관리 원본입니다. 78개 클래스·테스트·데모·지도 노트에 필요한 이유·사용 예·읽는 순서를 풀어썼고, 실행·읽는 법·전체 흐름·목차·라이브러리 안내도 함께 정리했습니다. 코드 발췌·명령·기존 Mermaid·함수명·source/source_commit/source_sha256은 유지하며 `readability_updated`만 별도로 표시합니다. 초기 ce1cd24 설명과 d32bc76 추가 코드의 기준은 합치거나 최신 구현으로 바꾸지 않습니다.

`tools/reading-notes.json`은 검토한 78개 설명입니다. `refresh-reading-notes.mjs`, `refresh-reading-guides.mjs`, `add-learning-demos.mjs`는 이번 편집을 위한 도구이며 일반 빌드에서 자동 실행하지 않습니다. 이전 게시본은 로컬 `build/docs-before-readable`에 보관합니다. 이후 수동으로 고친 설명을 지우지 않도록 이 도구들을 무조건 재실행하면 안 됩니다. OneDrive 노트나 Java 코드가 바뀌면 대응하는 content 문서도 확인·수정합니다.

`web/learning-demos.js`와 CSS가 13개 문서의 17곳에 로컬 학습 예시를 표시합니다. 편집 취소·완료, 두 반경의 교집합, 이전 응답 차단, 트랜잭션, 인원별 금액을 다룹니다. 실제 서비스 요청·DB 변경·브라우저 저장 없이 동작하며, MCP·JavaScript 비활성 환경을 위한 문장 설명도 유지합니다. Quartz가 페이지 바깥 틀을 재사용하면서 내부 내용을 교체하는 경우도 다시 연결합니다. 지도 예시는 가상 좌표의 직선 거리이며 실제 가게·도보 경로가 아닙니다.

`check-readable-docs.mjs`는 이전 게시본과 비교해 발췌·함수·기준 보존을 검사하고, `check-learning-demos.mjs`는 다섯 예시의 동작·문서 간 이동·키보드·모바일·다크 모드를 확인합니다. `check-flow-tooltips.mjs`의 76개 Mermaid와 `check-tables.mjs`의 표·링크 검사도 함께 사용합니다.

### Codex 읽기 전용 연결

`/connect/` 한 페이지에 주소·이름 복사, 3단계 설정, GitHub 인증 안내, 확인 질문, 현재 계정의 인증 개수와 전체 해제를 모았습니다. 추가 키나 GitHub 앱 등록은 팀원에게 요구하지 않습니다. Codex 로컬 앱·CLI·IDE의 Streamable HTTP + OAuth 연결용이며 웹 플러그인 등록과는 구분합니다.

- `/mcp`: stateless Streamable HTTP(JSON 응답). `connection_status`, `list_documents`, `search_documents`, `read_document`, `get_tasks`만 제공합니다. GitHub 자격 증명이나 브라우저 세션을 반환하지 않으며 에이전트 토큰은 다른 웹·보드 API에서 사용할 수 없습니다.
- `/.well-known/oauth-protected-resource/mcp`, `/.well-known/oauth-authorization-server`: 인증 탐색 메타데이터만 공개합니다. 문서 목록·본문·연결 관리·문서 JSON은 계속 팀원 인증을 요구합니다.
- `/oauth/register`: public client DCR, 로컬 loopback callback만 등록합니다. `/oauth/authorize`는 기존 웹 로그인과 팀원 확인 후 명시적 승인 화면을 표시합니다. PKCE S256·정확한 callback·resource·scope를 검사하고 승인과 인증 코드는 일회성입니다.
- `/oauth/token`: authorization code만 지원합니다. 팀 문서 전용 `team:read` 토큰은 최대 8시간이며 재인증으로 다시 연결합니다. refresh token은 발급하지 않습니다. GitHub 자격 증명이 먼저 만료되면 재인증이 필요합니다.
- `AGENT_ACCESS` SQLite Durable Object 및 `agent-access-v1` 마이그레이션은 보드 저장소와 별개입니다. 토큰은 SHA-256 해시로 조회하며 GitHub 자격 증명은 목적별 AES-GCM으로 암호화합니다. 만료 기록은 요청·알람에서 정리하고 공개 등록·토큰 교환 요청은 IP별로 제한합니다.
- 모든 MCP 조회 시 현재 Collaborator 권한과 연결 해제 세대를 재검사합니다. `/api/agent/revoke`는 같은 출처의 로그인된 사용자가 본인의 연결만 해제합니다. 에이전트의 POST는 읽기 프로토콜 전달용이며 문서·작업 생성·수정 도구는 없습니다.
- 빌드가 생성하는 `/agent/documents.json`에는 현재 게시 문서 Markdown과 내용 버전이 들어 있습니다. 인증 없이 공개하지 말아야 합니다. 에이전트 조회는 사람의 localStorage 읽음 기록을 변경하지 않습니다.
- 검증: `node --test docs/site/test/*.test.mjs`, `node docs/site/tools/check-connection.mjs`. `tools/check-agent-client.mjs`는 `build/docs-tools/mcp-check`에 설치한 `@modelcontextprotocol/sdk`로 실제 탐색·등록·PKCE·도구 호출을 검사합니다. Codex가 등록 시 요청하는 `refresh_token`은 지원하는 `authorization_code`로 협상하며, 반환값과 실제 발급 권한 모두 code-only입니다. 실제 GitHub 로그인 및 각 팀원 Codex 버전에서의 연결 확인은 별도입니다.

### 참고 링크

- [Codex MCP 연결](https://developers.openai.com/codex/mcp)
- [MCP OAuth 인증](https://developers.openai.com/plugins/build/auth)

- [GitHub Pages 비공개 열람 제한](https://docs.github.com/en/enterprise-cloud@latest/pages/getting-started-with-github-pages/changing-the-visibility-of-your-github-pages-site)
- [명시적 권한이 있는 저장소 목록](https://docs.github.com/en/rest/repos/repos#list-repositories-for-the-authenticated-user)
- [GitHub OAuth 앱 인증](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps)
- [Cloudflare 인증 후 정적 파일 제공](https://developers.cloudflare.com/workers/static-assets/routing/worker-script/)
- [Workers Free 한도](https://developers.cloudflare.com/workers/platform/pricing/)
