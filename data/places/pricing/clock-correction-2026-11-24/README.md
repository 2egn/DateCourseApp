# 카탈로그 버전 시각 정정 · 2026-11-24

가격 보완 자료의 버전이 실제보다 미래인 `2026-11-24T01:00:00Z`로 고정돼 있었다. 실제 UTC로 생성 시각을 정정하고 운영 `place_catalog_sources.generated_at` 한 행과 현재 파일 기준본을 맞췄다. **2,040곳의 장소·가격 payload와 태그·과거 적재 이력은 변경하지 않았다.**

- `previous-current.json`: 정정 전 기준본 위치·지문.
- `facts.json`: 기존 특징 자료에서 전체 생성 시각만 정정한 파일. `places.json`은 이전 가격 보완 자료를 그대로 참조한다.
- `preflight.json`: 동일한 payload·fingerprint·DB 버전 확인.
- `correction-result.json`: 한 행의 시각 정정과 나머지 14개 테이블의 스키마·행 보존 결과. `phase=VERIFIED`가 완료 상태다.
- `postflight.json`: 새 연결에서 확인한 활성 버전·건수·무결성 결과.

<details>
<summary>정정 이유와 재발 방지</summary>

자료 버전의 순서를 검사하므로 미래 버전이 기록되면 그보다 이전인 실제 현재 시각으로 생성한 다음 자료가 거부될 수 있다. 일반 적재의 이전 버전 거부 규칙을 약화하지 않고, 확정된 source key·이전 시각·같은 fingerprint를 조건으로 버전 한 행만 정정했다. 공개 카탈로그 잠금 안에서 검증·수정했고 원본·사용자·코스·이미지·제보·정정·적재 이력을 보존했다.

`PlaceCatalogRepository.synchronize()`는 현재 UTC보다 5분 넘게 미래인 입력을 스키마 생성 전에 거부한다. 테스트에서 Clock을 고정해 미래 버전의 쓰기 차단과 작은 시계 오차 허용을 확인한다. 가격 보완 도구는 `--generated-at`을 생략하면 실제 현재 UTC를 사용한다. 명시적 시각 지정은 과거 자료 재현용이다.

이전 가격 적재·검증 기록과 당시 자료는 원래 위치에 보존했다. 현재 기준은 `data/places/expansion/current.json`의 `facts`·`generatedAt`·`metadataCorrection`이다. 이 정정은 참고 가격·추정값·미확인 구분이나 통계 근거를 바꾸지 않는다.

</details>
