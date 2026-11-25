# 카탈로그 확장 검증 도구

기존 앱의 Java 모델·저장소를 그대로 사용하는 유지보수 도구다. 일반 앱 빌드에는 포함하지 않고 Gradle init script로 테스트 소스에만 추가한다. 실제 DB 주소나 키를 인수·문서에 넣지 않는다.

- `ExpansionAudit`: 기존 전체 기준본과 운영 활성 payload가 일치하는지 확인한다. 기본 `preflight`는 읽기 전용이다. `-PplaceExpansionMode=apply`일 때만 EXPANSION 모드 적재·반복 적재를 수행한다.
- 가격 보완은 `price-preflight`/`price-apply`를 사용한다. `PRICE_UPDATE` 모드는 전체 ID·가격 외 필드를 고정하며 장소 추가·삭제를 거부한다. [가격 자료 안내](../../data/places/pricing/2026-11-24/README.md)를 참고한다.
- 적재 전후 기존 DB 번호·payload·일반 데이터 행과 카탈로그 외 테이블을 비교한다. 보호 테이블은 개인정보·PNG 원문 대신 메모리 내 행 지문으로 비교하며 지문도 결과에 내보내지 않는다.
- 기준본이 바뀌거나 누락됐으면 쓰기 전에 중단한다. SQL 오류 원문이나 연결 정보를 출력하지 않는다.
- `ExpansionRehearsal`: 실제 추천·가격·특징·반경 검사를 사용한다. 실제 경로 API·DB 연결 없이, 고정 최근접 선택과 기존 코스의 유지 여부를 비교한다.
- `ExpansionAuditTest`: H2에서 전체 확장·기존 번호 보존·보호 테이블 보존·변형/부분 입력 거부를 검증한다.

## 실행

프로젝트 루트에서 `-I tools/catalog-ops/verify.init.gradle`을 붙인다. 데이터 경로와 모드를 명시하며 `verifyCatalogExpansion`과 `rehearseCatalogExpansion` 작업을 사용한다. 구체적인 재현 명령과 불변 기준 자료는 해당 `data/places/expansion/*/README.md`를 따른다.

```powershell
.\gradlew.bat -I tools/catalog-ops/verify.init.gradle test
```

DB 적재 결과가 성공했더라도 후속 검증이 실패할 수 있으므로, 재실행 전에 출력 보고서와 적재 이력을 먼저 확인한다. 이 도구는 다른 사용자의 변경을 자동으로 되돌리지 않는다.

<details>
<summary>배치 쓰기 · 2026-11-24</summary>

- `PlaceCatalogRepository.synchronize()`: 갱신·비활성·변경 이력을 최대 200건씩 모아 실행한다. 원본 버전·가격 전용 보존 검사·잠금·전체 롤백을 유지한다.
- `addBatch()` / `flushBatches()`: 매개변수 적재와 실행을 분리하며 묶음 실행 실패는 전체 적재를 취소한다. 마지막 나머지 행도 처리한다.
- `DatabasePool.create()`: MySQL에서 `rewriteBatchedStatements`를 활성화한다. 새 장소와 일반 데이터의 생성 ID는 기존 개별 연결을 유지한다.
- 405곳의 갱신·비활성·재활성·반복 적재와, 앞의 묶음 실행 후 마지막 이력 쓰기 실패 시 전체 롤백을 검사했다. Java 370개(유지보수 3개 포함)가 통과했다.

운영 데이터 변경이나 실제 MySQL 속도 비교는 이 수정의 자동 검사에 포함하지 않았다. 앞선 가격 보완의 8분 29초는 배치 적용 전의 적재·보존 검증 전체 시간이다. 새 구현의 속도 개선율로 사용하지 않는다.

</details>

## 현재 자료의 추천 활용 탐색

`RouteResourceProbe`와 `exploreCatalogRoutes`는 이미 통과한 모의 코스 결과 중 끊긴 조건에서 단계별 가까운 후보 최대 5곳만 비교한다. 장소·속성·모의 결과·출발지·보고서 경로를 명시한다. DB·경로 공급사 호출 없이 기존 추천 조건을 그대로 쓰며 앱의 추천 순서를 바꾸지 않는다. [현재 보유 자원·실험·한계·재현](../../docs/research/route-resource-exploration-2026-11-25.md)을 참고한다.
