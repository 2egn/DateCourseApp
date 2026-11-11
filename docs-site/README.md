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
node --test docs-site/test/*.test.mjs
```

이 테스트는 GitHub를 모의 응답으로 대체합니다. 실제 OAuth 앱·팀원 계정·비팀원 계정 검증은 배포 전후 별도로 해야 합니다.

로그인 상태는 시도별 쿠키로 분리해 여러 탭의 요청이 서로 덮어쓰지 않게 합니다. 비로그인 문서 탐색만 로그인으로 이동하며, favicon·검색 JSON·이미지 같은 부가 요청은 401로 차단합니다. 로그인 실패 화면의 `AUTH_COOKIE_MISSING`, `AUTH_STATE_EXPIRED`, `AUTH_STATE_MISMATCH`는 비밀값 없이 실패 단계를 구분하는 코드입니다. 상태 검증을 생략해 통과시키지 않습니다.

## 문서 준비

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

- `/dashboard/`: 할 일 / 진행 중 / 검토·테스트 / 완료. 기능 영역·우선순위 필터, 검색, 생성·수정·보관·복원, 담당자와 관련 노트 연결.
- `src/board-store.mjs`: SQLite Durable Object에 영구 저장. `wrangler.jsonc`의 `BOARD` 바인딩과 `task-board-v1` 마이그레이션이 필요합니다. Workers Free의 SQLite 지원 범위에서 사용합니다.
- `/api/board` 읽기와 `/api/board/tasks` 생성, `/api/board/tasks/:id` 수정은 같은 GitHub 팀원 인증을 거칩니다. 수정은 동일 출처 요청만 받고 사용자 ID와 변경 시각은 서버에서 기록합니다.
- 버전이 다른 수정 요청은 409로 거부합니다. 화면 입력을 유지하며 최신 카드를 다시 열어 비교하도록 안내합니다. 삭제 대신 보관합니다. 다른 화면의 변경은 새로고침 또는 30초 주기 갱신으로 확인합니다.
- 첫 저장소 생성 때 초기 16개 작업만 등록하며, 재배포·재시작으로 사용자의 변경을 덮어쓰지 않습니다.
- `node docs-site/tools/build.mjs`: `build/docs-tools/quartz`의 Quartz 5.1.0으로 문서와 대시보드를 함께 빌드합니다. `DOCS_NOTES_DIR`로 문서 원본 폴더를 지정할 수 있습니다. 원본을 보존하고 웹 복사본의 `00_`만 제거합니다. 그래프 뷰는 비활성화하며 Mermaid 흐름도는 유지합니다.
- `node docs-site/tools/check-ui.mjs`: Chrome/Playwright와 모의 인증 저장소로 생성·이동·두 화면 공유·충돌·보관·복원·모바일 레이아웃을 검사합니다. 실제 팀원 계정 검증과 구분합니다.

## 참고 링크

- [GitHub Pages 비공개 열람 제한](https://docs.github.com/en/enterprise-cloud@latest/pages/getting-started-with-github-pages/changing-the-visibility-of-your-github-pages-site)
- [명시적 권한이 있는 저장소 목록](https://docs.github.com/en/rest/repos/repos#list-repositories-for-the-authenticated-user)
- [GitHub OAuth 앱 인증](https://docs.github.com/en/apps/oauth-apps/building-oauth-apps/authorizing-oauth-apps)
- [Cloudflare 인증 후 정적 파일 제공](https://developers.cloudflare.com/workers/static-assets/routing/worker-script/)
- [Workers Free 한도](https://developers.cloudflare.com/workers/platform/pricing/)
