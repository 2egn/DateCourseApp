# 부산 16개 구·군 분산 확장 · 2026-11-25

상태: 후보 생성·기존 객체/속성 보존·오프라인 추천 검증 완료. 운영 반영 여부는 `import-verification.json`과 `../current.json`을 확인한다.

직전 2,138곳을 보존하며 3,490곳을 추가한 전체 5,628곳이다. 기존 미수록 11개 구·군에 각각 식사 150·카페 120·문화 30곳을 추가하고 중구는 식사 110·카페 80곳을 보완했다. 기존에 목표를 넘은 남구·부산진구·수영구·해운대구는 추가하지 않았다. 중구 문화는 품질 검사를 통과한 추가 후보가 없어 18곳을 유지했다.

이는 구·군별 미수록을 줄이는 선택이며 부산 전체 업소의 통계적 대표 표본은 아니다. 구·군 내부는 기존 500m 격자·업종별 순환을 사용한다. 이미 많은 지역의 장소를 삭제해 개수를 맞추지 않는다.

| 파일 | 역할 |
|---|---|
| `policies/` | 구·군 이름 순서의 12단계 누적 상한·격자 선택 정책 |
| `stage-reports.json` | 단계별 원본/정책/결과 지문·보류 ID·품질 근거 |
| `baseline.json` | 직전 운영 파일 쌍·포인터와 최종 파일 지문 |
| `coverage-report.json` | 16개 구·군의 전후 업종 수·보존·가격 상태 |
| `places.json`, `appeal/` | 전체 장소·속성·공공 출처와 지문 |
| `price-review.json` | 신규 가격 보완 1,460건의 전후 값과 통계 근거 |
| `rehearsal-scenarios.json`, `rehearsal-report.json` | 이전 출발지와 16개 구·군 공공좌표 중앙값의 모의 추천 |
| `preflight-report.json`, `import-verification.json` | 운영 대조와 적재·보호 테이블·반복 적재 결과 |

식사 2,800·카페 2,260·문화 568곳이다. 가격은 참고 23·추정 3,092·미확인 2,513곳이다. 신규 가격 보완은 기존 지역 외식비 60건·커피 조사 1,400건이며 실제 해당 매장 가격이 아니다. 기존 2,138곳의 가격·기본정보·속성은 그대로 유지했다.

기존 체크섬이 검증된 공공 원본을 오프라인으로 재사용했다. 새 영업·메뉴 가격·지도 공급사 동일 장소 조회는 수행하지 않았다. 공원·도서관의 가격 미확인을 무료로 추론하지 않고 동일 이름·주소만으로 기존 ID를 합치지 않는다. 교통 특징은 기존 직선거리 기준이며 실제 출입구·도보 접근성은 별도다.

## 재현

프로젝트 루트에서 실행한다. 아래 생성 명령은 DB에 연결하지 않는다. `build`의 지정 경로만 사용하고 기존 파일을 일괄 삭제하지 않는다.

```powershell
$catalogPack = 'data/places/expansion/busan-balanced-2026-11-25'
$catalogBaseline = 'data/places/expansion/jung-gu-2026-11-25/places.json'
foreach ($catalogPolicy in Get-ChildItem "$catalogPack/policies/*.json" | Sort-Object Name) {
  $catalogStage = "build/balanced-rebuild/$($catalogPolicy.BaseName)"
  python tools/build_district_expansion.py --policy $catalogPolicy.FullName --output $catalogStage --baseline $catalogBaseline
  if ($LASTEXITCODE -ne 0) { throw '장소 생성 실패' }
  $catalogBaseline = "$catalogStage/places.json"
}
python tools/collect_busan_appeal_data.py --offline --dataset $catalogBaseline --output build/balanced-rebuild/appeal --baseline-dataset data/places/expansion/jung-gu-2026-11-25/places.json --baseline-facts data/places/expansion/jung-gu-2026-11-25/appeal/demo_appeal_facts.json
python tools/supplement_place_prices.py --dataset $catalogBaseline --facts build/balanced-rebuild/appeal/demo_appeal_facts.json --sources data/places/pricing/2026-11-24/sources.json --output build/balanced-final
```

새 속성의 생성 시각은 실행 시각이다. 저장된 가격 보완 보고서의 기준 지문은 가격 보완 전 단계이며, 최종 파일은 `baseline.json`·`coverage-report.json`의 지문으로 확인한다. 기존 장소와 속성의 계산일은 보존한다.

운영 적용에는 `tools/catalog-ops/verify.init.gradle`의 `verifyCatalogExpansion`을 사용하고 기준본·기준 속성은 위 중구 전체 파일 쌍으로 지정한다. 먼저 기본 읽기 전용 대조를 수행하며 명시적 `-PplaceExpansionMode=apply`에서만 쓴다. 운영 적용 후에는 2,138곳 기준본으로 다시 실행하지 말고 현재 포인터와 결과부터 확인한다. 실제 적재·반복 검증 성공 전에는 운영 완료로 표시하지 않는다.

Java 모의 코스 696개에서 세 활동 선택은 373→535, 기존 코스 조건 탈락·단계 수 감소는 0이었다. 고정 최근접 선택의 오프라인 비교이며 실제 이동 경로·사용자 완료율·추천 품질 점수는 아니다. Python 도구 32개도 통과했다. 앱은 DB를 읽으며 기존 장소 캐시는 다시 불러오거나 재시작해야 갱신된다.
