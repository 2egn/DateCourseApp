# 부산 주요 지역 2,040곳 확장 · 2026-11-23

상태: 운영 DB 적재·전후 보존·반복 적재 검증 완료. 신규 1,174·갱신/재활성/비활성 0·기존 유지 866, 활성/정정 적용 조회 2,040곳이다.

기존 수영구 확대본 866곳을 보존하고 부산진구·남구·해운대구 1,174곳을 추가한다. 식사 1,000·카페 820·문화 220곳이다. 공공 원본 스냅샷을 재사용하며 현재 영업·Google 동일 장소·실제 메뉴 가격을 새로 검증한 자료는 아니다.

- 정책: `policies/`의 누적 상한과 500m 격자·업종별 순환 선택. 직전 전체 스냅샷을 단계별 기준본으로 사용한다.
- `places.json`: 최종 전체 장소. 기존 866곳의 객체·ID·가격·좌표를 그대로 유지한다.
- `appeal/demo_appeal_facts.json`: 최종 장소 지문과 연결된 속성. 기존 866곳은 근거와 계산일을 보존한다.
- `stage-reports.json`, `*-quality.json`: 단계별 추가·제외·검토 대상. 기존 검토 대상은 조용히 정정하거나 합치지 않는다.
- `coverage-report.json`: 지역·업종·500m 격자 커버리지와 가격 상태.
- `rehearsal-scenarios.json`, `rehearsal-report.json`: 10개 지역·3개 이동수단, 총 240개 최근접 모의 코스. 실제 이동 경로·사용자 완료율이 아니다.

기준본은 `../suyeong-2026-11-16/places.json`과 같은 폴더의 `appeal/demo_appeal_facts.json`이다. 속성 생성은 `--offline --baseline-dataset ... --baseline-facts ...`로 기존 근거를 보존한다. 가격 미확인을 무료로 바꾸지 않으며, 신규 추정에는 표본 20곳 이상과 허용 세부 업종 조건을 적용한다.

내장 기본 시연 386곳은 이번 확장으로 바꾸지 않는다. 운영 앱은 DB 공공 카탈로그를 우선 조회한다.

<details>
<summary>적재 완료 후 읽기 전용 재확인</summary>

현재 DB와 같은 전체 스냅샷을 기준본으로 지정한다. 아래 명령은 DB를 변경하지 않는다.

```powershell
$catalogRoot = 'data/places/expansion/busan-core-2026-11-23'
$catalogArgs = @(
  '-I', 'tools/catalog-ops/verify.init.gradle', 'verifyCatalogExpansion',
  "-PplaceBaseline=$catalogRoot/places.json",
  "-PplaceBaselineFacts=$catalogRoot/appeal/demo_appeal_facts.json",
  "-PplaceDataset=$catalogRoot/places.json",
  "-PplaceFacts=$catalogRoot/appeal/demo_appeal_facts.json",
  '-PplaceReport=build/catalog-readonly-verification.json'
)
.\gradlew.bat @catalogArgs
```

최초 확장에서는 기준본으로 `suyeong-2026-11-16` 파일 쌍을 사용하고 `-PplaceExpansionMode=apply`를 명시했다. DB가 이미 2,040곳이면 866곳 기준본으로 다시 실행하지 않는다. 부분·변형 기준본은 쓰기 전에 거부한다.

모의 추천 재현은 같은 init script의 `rehearseCatalogExpansion`을 사용한다. 866곳 기준본, 이번 전체 장소·속성 쌍, `-PplaceScenarios=.../rehearsal-scenarios.json`, `-PplaceReport=build/catalog-rehearsal.json`을 전달한다.

</details>

## 운영 결과

[검증 기록](import-verification.json)에 기존 번호·payload 불일치 0건과 카탈로그 외 17개 테이블 보존을 기록했다. 같은 자료를 반복 적재해 신규·갱신·비활성 0건, 유지 2,040곳을 확인했다. 적재와 전후·반복 검증 전체는 약 22분 26초이며 순수 삽입 시간은 아니다. 부산 전체 확대 전에는 JDBC 배치 적재 성능을 보완하고 별도 측정해야 한다.

현재 운영 기준 파일 쌍은 [current.json](../current.json)이 가리킨다. 다음 확장에서 866곳이나 내장 386곳으로 되돌아가 기준본을 만들지 않는다.
