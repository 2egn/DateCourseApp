# 문서와 팀 사이트

[홈 메뉴와 공통 UI](공통_UI와_홈_메뉴.md)에서 고정 헤더, 저장 시 이름 입력, 컨트롤러 상속과 공통 로딩·알림 사용법을 확인한다.

알림 자동 닫기·편집 초안 유지·도보 구간 표시도 같은 문서에 설명했다. 다른 교통수단의 무료 조건과 확장 범위는 [자동차·대중교통 경로 검토](research/route-modes-2026-11-14.md)를 참고한다.

[편집 중 이동 시간 누적 검토](research/editor-travel-time-plan-2026-11-14.md)는 장소 추가 후 한 구간 계산과 후보 5곳 비교를 구분한 제안이다. 자동 누적은 아직 구현하지 않았다.

회원별 저장·조회 화면의 최신 연결과 확인 절차는 [코스 저장과 다시 열기](코스_저장과_다시열기.md)를 읽는다. 실제 MySQL 수동 검증은 아직 남아 있다.

앱의 최신 연결 흐름과 실행 방법은 [시연 장소와 가격 흐름](시연_장소와_가격_흐름.md)을 먼저 읽는다. 시연 목록 선택·가격 미확인·범위 합계·저장 스냅샷을 설명한다.

| 위치 | 읽거나 수정할 내용 |
|---|---|
| `site/content/` | 팀 사이트에서 관리하는 실행 안내·클래스 보충 노트 |
| `site/web/dashboard/` | 작업 보드 화면 |
| `site/web/connect/` | Codex 연결 안내 화면 |
| `site/src/` | 로그인·열람 권한·보드 저장·읽기 전용 MCP 처리 |
| `site/tools/` | 문서 빌드·브라우저 검사 도구 |
| `site/test/` | 사이트 자동 테스트 |
| `research/` | 장소 데이터·가격·리뷰 조사와 실험 결과 |
| `archive/` | 이전 설명과 Maven 설정. 현재 앱 실행에는 사용하지 않음 |

장소 데이터 작업의 현재 결과는 [장소 연결 24곳 검증](research/place-binding-evaluation-2026-11-13.md)을 참고한다. 자동 연결 조건 통과 8곳과 검토 목록의 위치·실행 방법을 정리했다.

다음 개선 수단은 [주소 외 장소 연결 근거](research/place-binding-signals-2026-11-13.md)에 정리했다. 전화·공식 지점 페이지·지도 링크·다른 검색 서비스의 활용 가능성을 비교한다.

실제 후속 결과는 [문화 8곳 전화·홈페이지 검증](research/place-binding-contact-results-2026-11-13.md)에서 확인한다. 현재 Java 데모는 공식 출처 보정을 거친 v5 연결표 9곳을 읽는다.

시연 목록의 가격 근거는 [시연 장소 9곳 가격 점검](research/demo-price-verification-2026-11-13.md)을 참고한다. 출처 간 금액 차이와 메뉴 분량 미확인을 구분하고, 과제에서 먼저 확인할 소규모 표본을 정리했다.

## 자주 쓰는 명령

프로젝트 최상단에서 실행한다.

```powershell
node --test docs/site/test/*.test.mjs
node docs/site/tools/build.mjs
```

문서 빌드는 기존 로컬 Quartz(`build/docs-tools/quartz`)와 OneDrive 학습 노트를 사용한다. 경로가 다르면 `DOCS_NOTES_DIR`를 지정한다. 이 폴더를 정리하는 작업은 OneDrive 원본을 이동하지 않는다.

사이트 배포 설정은 `site/wrangler.jsonc`, 빌드 결과는 `site/public/`에 있다. 폴더 이동만으로 재배포되지는 않는다. 인증·배포의 상세 절차는 `site/README.md`를 참고한다.
