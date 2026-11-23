# 가격 보완 · 2026-11-24

매장 참고 가격을 보존하면서, 메뉴가 맞는 부산 외식비와 커피 전문점 조사값으로 추정을 보완한다. `staged/places.json`은 2,040곳 중 857곳의 가격만 변경한다. 운영 반영 여부는 `import-verification.json`의 `applied`와 후속 보존 검사 결과로 확인한다.

운영 적재는 **가격 갱신 857·유지 1,183·추가/삭제/재활성 0곳**으로 완료했다. 활성·정정 적용 조회는 모두 2,040곳이며 번호·가격 외 필드·일반 데이터 행과 카탈로그 외 17개 테이블을 보존했다. 반복 적재는 2,040곳 모두 변경 없음이다. 적재와 보존·반복 검증 전체는 약 8분 29초였다.

| 근거 | 이번 변경 | 처리 |
|---|---:|---|
| 부산 외식비 2026.10 | 37곳 | 확인된 메뉴 또는 매장명에 해당 메뉴가 있을 때 추정 |
| 2025 커피 전문점 조사 | 820곳 | 커피 530사례 평균 4,118원, 대표 메뉴 1잔 가정 |
| 매장별 참고 가격 | 변경 없음 | 기존 17곳 유지; 제공량 미확인은 실값으로 승격하지 않음 |

가격은 해당 매장의 현재 메뉴 전체나 실제 결제액을 보장하지 않는다. 미확인은 847곳이며 무료로 처리하지 않는다. 새 근거에 연결되지 않은 기존 추정은 유지하므로 착한가격 표본 편향이 모두 해소된 것은 아니다.

기존 착한가격업소 기반 추정은 319곳 남는다. 다음 확대는 현재 기준본의 가격을 보존하고 신규 장소에도 같은 보완 규칙을 적용한다. 가격을 되돌릴 때도 과거 스냅샷을 그대로 재적재하지 않고 이후 생성 시각을 가진 가격 전용 스냅샷으로 검증한다.

<details>
<summary>파일과 선택 규칙</summary>

- `sources.json`: 지역·메뉴·단위·통계 기간·표 위치·조회 인수와 원문 지문.
- `regional-raw.json`: 공개 조회 응답. [행정안전부 외식비](https://hamoni.mois.go.kr/portal/lcpr/indvSrcvEatoutCstList.do)의 부산·2026.10 자료. 김밥은 1줄, 삼겹살은 중량 기준이므로 제외한다.
- [농림축산식품부 통계보고서](https://www.mafra.go.kr/bbs/home/798/595247/download.do): 표 85, 인쇄 152쪽/PDF 162쪽, 커피 전문점의 커피 행을 사용한다. 전국 조사이며 부산 매장의 실값은 아니다.
- `staged/price-review.json`: 장소별 이전·새 가격·선택 근거. 별도 출처를 평균 내지 않고 같은 등급의 여러 메뉴는 범위로 표현한다.
- `supplement_place_prices.choose_price()`: 기존 참고값 보존 → 장소 ID·이름·주소·메뉴·제공 단위가 맞는 매장 자료 → 같은 메뉴의 부산 외식비 → 지원하는 조사 집단 → 기존값/미확인 순서다.
- 매장 메뉴의 `as_listed_portion_unknown`은 실제 1인 가격으로 변환하지 않는다. 냉면을 밀면에, 비빔밥을 일반 한식 전체에 적용하지 않는다.
- 저장 JSON의 enum·필드는 기존 앱과 동일하다. 통계 추정도 `SIMILAR_ESTIMATE`이며 화면에는 짧은 근거와 ‘추정’을 표시한다.

</details>

<details>
<summary>재현과 운영 검사</summary>

프로젝트 루트에서 실행한다. 생성 시각은 실제 운영 기준본보다 이후여야 한다.

```powershell
python tools/supplement_place_prices.py --dataset data/places/expansion/busan-core-2026-11-23/places.json --facts data/places/expansion/busan-core-2026-11-23/appeal/demo_appeal_facts.json --sources data/places/pricing/2026-11-24/sources.json --output build/price-reproduction --generated-at 2026-11-24T01:00:00Z
python -m unittest discover -s tools -p test_supplement_place_prices.py -v
.\gradlew.bat -I tools/catalog-ops/verify.init.gradle test
```

`verifyCatalogExpansion`의 `price-preflight`는 읽기 전용, `price-apply`는 `PRICE_UPDATE` 적재 모드다. 인수는 이 폴더의 자료와 바로 이전 전체 기준본으로 지정한다. 적용 후에는 이전 기준본으로 다시 적재하지 않는다. 저장소는 트랜잭션 잠금 안에서 ID 집합·가격 외 필드를 대조하고 추가·삭제·다른 필드 변경을 거부한다. 검증 도구는 사용자 테이블·관리자 정정·저장 코스·기존 DB 번호와 반복 적재 결과를 확인한다.

`rehearsal-report.json`은 새 가격을 사용한 240개 오프라인 추천 검사다. 최근접 후보를 순서대로 선택했을 때 3단계 완성은 전후 179개, 감소 사례와 기존 코스 후보 이탈은 0개다. 이는 실제 이동 경로나 사용자 성공률이 아니다. `rehearseCatalogExpansion`에 `-PplaceRehearsalMode=price-update`를 지정하면 가격 변경으로 인한 후보 이탈을 별도 기록한다.

</details>
