# 검증·팀 사이트·협업 지원

[흐름도로 보기](00-flow-map.md)

[전체 길잡이](README.md). 앱 기능과 개발/운영 지원 기능을 구분한다.

| 영역 | 사용 요소·필요 기능 | 실제 처리와 범위 |
|---|---|---|
| Java 검사 | JUnit 5·H2·WebView fixture | 모델·필터·세션·저장·지도 연결. 모의 공급사/검사 DB와 운영 성공은 다름 |
| 선택 실제 검사 | coursePreviewLiveTest·tmapLiveTest | SDK/공급사와 별도 worker에서 확인. 모든 PC·입력 환경의 증명은 아님 |
| 보관 UI | archive init script | 기본 앱에서 제외한 예제의 별도 호환 검사 |
| Python 자료 검사 | unittest·고정 입력 | ID·지문·보존·가격·매칭. 실제 영업 확인과 구분 |
| 사이트 로직 | Node test·SQLite fixture | 인증·보드·이력·사람의 수정 보존. 운영 배포와 구분 |
| 표시 점검 | Playwright 스크립트 | 데스크톱/모바일 DOM·오류·넘침·카드 상태. 실제 손 입력과 다름 |
| 문서 빌드 | Node·Quartz | Markdown → 링크·표·함수 안내 보정 → 정적 사이트 |
| 팀 사이트 | Cloudflare Worker·GitHub OAuth·쿠키 | 협업 권한 확인 후 문서·보드 제공. 앱 회원 로그인과 별개 |
| 공유 보드 | Durable Object·SQLite | 할 일·버전·담당자·보관·once-only 이력. 기존 팀원 수정 보존 |
| 앱 설정 전달 | 인증된 Worker endpoint | 공급사 키와 별도 DB 설정 전달. 전체 DB 프록시가 아님 |
| 에이전트/문서 연결 | agent store·읽기 인터페이스 | 사용자 동의·권한·토큰·읽기 범위. 코스 기능과 별개 |

근거: [worker](../site/src/worker.mjs), [board](../site/src/board-store.mjs), [agent](../site/src/agent-store.mjs), [설정 전달](../site/src/app-keys.mjs), [빌드](../site/tools/build.mjs), [사이트 안내](../site/README.md).

최근 별도 작업의 통과 기준은 Java 398개(앱 395+선택 유지보수 3)·보관 2개, 기본 Python 도구 32개, 사이트 Node 58개다. 이 문서 작성에서 전부 새로 실행했다는 뜻은 아니다. 문서만 바뀌면 링크·메서드·빌드·색인 누락을 확인한다. Java/사이트 로직이 바뀌면 해당 전체 검사를 수행한다. 검사 개수만으로 기능 완성을 주장하지 않는다.

```powershell
.\gradlew.bat test
node --test docs/site/test/*.test.mjs
```

카탈로그·보관 검사는 init script를 명시한다. 실제 API·운영 DB·비밀번호 재설정·SQL은 설명을 읽는 과정에서 자동 실행하지 않는다.

남은 실제 확인에는 물리 입력·모니터 이동·빠른 드래그·느린 네트워크가 있다. 여러 앱의 같은 코스 편집 충돌, 긴 코스 요청량, 설정 캐시 갱신, 대량 적재 추적/성능도 개선 후보다. 해결됐다고 설명하지 않는다.

운영 장소 반영·로컬 커밋·원격 푸시·master 통합·Worker 배포는 서로 다른 사건이다. 원격에 반영하지 않은 소스를 운영 배포 완료로 설명하지 않는다. 원격 운영은 저장소의 사용자별 지침을 따른다.
