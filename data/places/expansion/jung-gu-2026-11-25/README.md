# 중구 98곳 추가 · 2026-11-25

상태: 운영 적재·기존 데이터 보존·반복 적재 검증 완료. 현재 운영 카탈로그는 2,138곳이다.

기존 2,040곳을 그대로 보존하고 식사 40·카페 40·문화 18곳을 더한 전체 2,138곳이다. 새 가격은 통계 추정 56·미확인 42곳이며, 문화 시설·관광지의 가격 미확인을 무료로 추론하지 않는다. 영업·현장 메뉴 가격·Google 동일 장소를 새로 확인한 자료가 아니다.

| 파일 | 역할 |
|---|---|
| `baseline.json` | 직전 운영 파일 쌍·지문과 새 파일 쌍 지문 |
| `policy.json` | 중구·500m 격자·업종별 순환 선택·누적 분류 상한 |
| `places.json` | 이전 장소 객체를 보존한 전체 장소 스냅샷 |
| `quality-report.json` | 신규 보류 51개 공공 ID와 검토 근거, 이전 자료 보존 |
| `appeal/` | 2,138곳 속성·원본 출처·입출력 지문, 기존 2,040곳 근거·계산일 유지 |
| `candidate-summary.json` | 추가 장소·가격·태그 요약 |
| `rehearsal-scenarios.json` | 기존 10개 지역과 중구 3개 모의 출발지 |
| `rehearsal-report.json` | Java 추천·가격·특징·반경 검사 312개 조합 |
| `preflight-report.json` | 가격 보완 전 최초 확장 입력의 읽기 전용 대조 |
| `import-verification.json` | 신규 98·기존 유지 2,040·보호 테이블·반복 적재 검증 |
| `price-review.json` | 신규 43곳의 지역 외식비·커피 조사 보완과 근거 |
| `price-import-verification.json` | 가격 43·나머지 유지 2,095·번호와 가격 외 데이터·반복 적재 검증 |

Python 도구 검사 32개 통과. Java 모의 코스 312개에서 3개 활동을 선택한 조합은 191→217, 단계 수 감소 0·기존 코스 조건 탈락 0이었다. 고정된 최근접 선택의 오프라인 비교이며 사용자 완료율·실제 이동 경로·추천 품질 점수가 아니다.

신규 특징은 공공 지정 연결 10곳·지하철 직선 거리 기준 71곳·버스 정류장 직선 거리 기준 83곳이다. 출입구·도보 동선은 확인하지 않았다. 중구 모범음식점 지정 자료가 없다는 이유로 미지정으로 판정하지 않는다.

기존 중복 후보나 부산박물관 명칭 별칭은 합치지 않는다. 새 공공 ID만 추가하며 이전 장소·속성·가격을 변경하지 않는다. 운영 카탈로그 적용 후 `../current.json`도 최종 파일 쌍으로 전환했다. 회원·관리자·제보·정정·저장 코스·이미지 등 카탈로그 외 보호 테이블 9개와 기존 일반 데이터 행을 보존했다.

## 재현

프로젝트 루트에서 실행한다. 두 생성 명령은 운영 DB를 사용하지 않는다.

```powershell
python tools/build_district_expansion.py --policy data/places/expansion/jung-gu-2026-11-25/policy.json --output build/jung-gu-rebuild --baseline data/places/pricing/2026-11-24/staged/places.json
python tools/collect_busan_appeal_data.py --offline --dataset build/jung-gu-rebuild/places.json --output build/jung-gu-rebuild/appeal --baseline-dataset data/places/pricing/2026-11-24/staged/places.json --baseline-facts data/places/pricing/clock-correction-2026-11-24/facts.json
```

새 속성 파일의 생성 시각은 실행 시각이며, 이전 장소의 속성·계산일은 유지한다. 실제 적재에는 기존 `ExpansionAudit`의 전체 기준본·운영 payload·보호 테이블 검사를 사용한다. 적재 결과와 반복 적재 검증이 끝나기 전에는 적용 완료로 표시하지 않는다.

## 최종 가격과 검증 기준

최초 준비본 `dbb2507`의 새 카페 추정은 이전 착한가격 표본이었다. 기존 카탈로그와 같은 보완 규칙을 적용해 신규 43곳만 갱신했다(지역 외식비 3·커피 조사 40). 기존 2,040곳의 가격·장소·속성은 그대로다. 새 카페 40곳은 기존과 같은 4,118원/잔 조사 추정이며 실제 매장 가격이 아니다. 최초 확장 자료·속성과 preflight 보고서의 지문은 `dbb2507`에 대응한다. 최종 파일 지문은 `baseline.json`과 `../current.json`, 최종 운영 payload 지문은 `price-import-verification.json`에 있다.

최종 가격까지 재현하려면 위 두 생성 명령 뒤 아래를 실행한다.

```powershell
python tools/supplement_place_prices.py --dataset build/jung-gu-rebuild/places.json --facts build/jung-gu-rebuild/appeal/demo_appeal_facts.json --sources data/places/pricing/2026-11-24/sources.json --output build/jung-gu-final
```

운영 적재·보존·반복 검증은 확장 약 110초, 후속 가격 보완 약 29초였다. 순수 삽입·갱신 시간이나 개선율로 해석하지 않는다. 최종 312개 모의 코스 결과는 `rehearsal-report.json`을 따른다. 실행 중 앱의 장소 캐시는 장소를 다시 읽거나 앱을 재시작해야 갱신된다.
