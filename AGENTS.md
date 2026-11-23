# 에이전트 작업 지침

이 저장소는 Java Swing·JavaFX 기반 팀용 데스크톱 앱이다. 실행·환경 설정은 [README](README.md), 현재 책임 분담은 [architecture](docs/architecture.md)를 먼저 확인한다.

## 변경 기준

- 통합 앱 진입점은 `com.datecourse.app.AppLauncher`다. 데모 화면을 실제 앱 흐름으로 혼동하지 않는다.
- 컨트롤러 안에서 기능별 `private` 메서드로 정리하는 방식을 우선한다. 필요 없이 베이스 클래스·인터페이스·프레임워크를 늘리지 않는다.
- 기존 공개 메서드, FXML의 컨트롤러·필드·이벤트 이름, Java–JavaScript 브리지 이름을 보존한다. 변경이 필요하면 호출부와 검증도 함께 갱신한다.
- 초기화와 이벤트 등록 순서를 유지한다. 이미지·경로·후보의 요청 번호와 세션 검사, 취소·로그아웃 처리를 분리 과정에서 누락하지 않는다.
- `RoutePlan`은 사용자 계획, `TravelRoute`는 조회 결과다. 실제 경로를 저장 코스의 직선 연결이나 가격 근거와 혼합하지 않는다.
- 반경 정책은 `TravelMode`, 후보 수는 `NextPlaceCondition`을 따른다. 가격 미확인을 무료로 바꾸지 않는다.
- 주요 메서드에는 역할이나 제약을 설명하는 짧은 한국어 주석을 둔다. 코드를 그대로 번역하는 장황한 주석은 피한다.
- 사용자의 기존 수정·공백 변경과 다른 worktree를 보존한다. master 반영은 일반 병합 또는 fast-forward로 처리하고 강제 푸시하지 않는다.

## 검증

프로젝트 루트에서 실행한다.

```powershell
.\gradlew.bat test
node --test docs/site/test/*.test.mjs
```

Java 변경은 관련 회귀 검사와 전체 `test`, 사이트 로직 변경은 Node 검사를 수행한다. 문서만 바뀌면 링크·함수명·게시 원본을 점검한다. 테스트 통과와 실제 공급사·운영 DB 검증을 구분해서 보고한다.

실제 API 호출, 운영 DB 적재·삭제, 문서 Worker 배포는 해당 작업으로 요청받은 경우에 수행한다. 키·DB 접속 정보·인증 토큰과 개인 `.local`·IDE·빌드 파일은 커밋하지 않는다.

## 문서 유지

- 현재 안내: `README.md`, `src/README.md`, `docs/architecture.md`, `docs/README.md`.
- 팀 사이트 원본: `docs/site/content`. 관련 기능·클래스 노트와 `업데이트_내역.md`를 같이 확인한다.
- 날짜가 붙은 검토·체크포인트는 당시 기록이다. 현재 상태인 것처럼 덮어쓰지 말고 최신 안내를 연결한다.
- 요약 → 주요 클래스·함수 → 필요한 상세 순서로 간결하게 쓴다. 긴 설명은 `<details>`로 접는다.
- `source_commit`·`source_sha256`은 발췌 원본 기준이다. 과거 발췌를 유지한다면 최신 확인은 `reviewed_commit`·`reviewed_at`로 구분한다.
- 저장소 수정만으로 운영 사이트가 배포됐다고 보고하지 않는다. 사이트 빌드는 Quartz와 노트 원본이 필요하며 준비법은 `docs/site/README.md`를 따른다.
