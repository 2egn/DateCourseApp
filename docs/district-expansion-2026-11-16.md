# 수영구 표본 확대 · 2026-11-16

후속 품질 점검: [대표 지역 코스 모의 테스트](suyeong-rehearsal-2026-11-16.md). 지역 편중과 다른 이름의 동일 건물 기록을 추가 확인했다.

기존 서면 386곳을 그대로 유지하고 수영구 480곳을 추가한 **866곳의 확대본**을 만들었다.
운영 DB 적재 결과는 신규 480·갱신 0·비활성 0·기존 유지 386이며 활성/정정 적용 조회는 모두 866곳이다.
현재 보유한 공공 원본을 재사용했으며 전체 장소의 현재 영업·외부 지도 동일성을 새로 검증한 것은 아니다.

| 구분 | 기존 | 추가 | 합계 |
|---|---:|---:|---:|
| 식사 | 240 | 300 | 540 |
| 카페 | 120 | 150 | 270 |
| 문화 | 26 | 30 | 56 |

<details>
<summary>수집·가격·검토 범위</summary>

- 수영구의 대상 업종 원본 3,089행을 검사해 검토 대상 27건을 신규 선택에서 제외했다. 근접 동명은 중복 확정이나 자동 병합을 의미하지 않는다. 업종별 상한을 둔 표본이며 수영구 전수 적재가 아니다.
- 기존 동명 의심 3쌍은 보존하고 검토 목록에 남겼다. 기존의 넓은 유형 가격 추정 51곳도 별도 검토 대상으로 표시했다. 기존 장소의 이름·좌표·ID·가격 등을 바꾸지 않았다.
- 새 장소 중 추정 가격 248·미확인 232곳이다. 일식의 작은 표본과 넓은 유형 적용을 신규 장소에 사용하지 않았다. 표본 20곳 이상 및 허용 업종 조건을 함께 검사한다.
- 가격 표본은 착한가격업소여서 일반 가게보다 낮을 수 있다. 표본 수·단위·집계 방식·편향을 quality report에 구조화해 기록한다. 개별 매장 메뉴 확인값이나 통계적 신뢰구간을 확보한 것은 아니다.
- 수영구 모범음식점 지정 자료는 아직 없으므로 태그 부재를 미지정으로 단정하지 않는다. 확인 자료의 지역 범위를 출력한다.
- 최종 태그: 착한가격 76·역 근접 575·버스 근접 800·모범음식점 3·무료입장 1. 교통 접근성은 직선거리이며 오래된 원본 기준일도 그대로 유지한다.

</details>

<details>
<summary>주요 파일·클래스·함수</summary>

| 파일 / 함수 | 간단 설명 |
|---|---|
| [build_district_expansion / stage](../tools/build_district_expansion.py) | 선언된 기존 스냅샷을 보존하고 특정 구의 신규 후보만 합친다. 품질 보고서와 입력 지문을 생성한다. |
| [place_data_quality / screen](../tools/place_data_quality.py) | 누락·좌표·기준일·동명 근접/동일 주소를 검사하고 신규 검토 대상을 분리한다. 기존 장소는 삭제하지 않는다. |
| [build_public_demo / build](../tools/build_public_demo.py) | 공공 입력 지문 검증, 기존 장소 보존, 구별 선택 및 신규 가격 표본 정책을 적용한다. 기존 데모 재생성 결과는 유지한다. |
| [collect_busan_appeal_data / collect](../tools/collect_busan_appeal_data.py) | 지정 출처·수동 확인을 파일로 분리하고 확대본의 속성을 별도 출력한다. 수동 확인일은 생성일로 바꾸지 않는다. |
| 같은 파일 / NearestTransitIndex | 격자로 근접 후보를 좁히고 정확한 거리로 비교한다. 근처에 후보가 없으면 전체 비교하며 원본 순서로 동률을 처리한다. |
| [PlaceCatalogRepository / synchronize](../src/main/java/com/datecourse/db/PlaceCatalogRepository.java) | EXPANSION 모드는 기존 활성 장소가 빠진 입력을 트랜잭션 안에서 거부한다. 의도적인 전체 스냅샷 비활성화 동작은 별도로 유지한다. |
| [PublicPlaceImport / batch](../src/main/java/com/datecourse/demo/PublicPlaceImport.java) | 외부 장소/속성 파일 쌍을 실제 Java 모델로 검증한다. 운영 CLI는 확장 모드를 사용한다. |
| [suyeong-policy.json](../data/places/expansion/suyeong-policy.json) | 구·상한·가격 표본 정책. 상한은 기존 장소를 포함한 전체 수다. district 선택에서는 반경 대신 행정구 필드를 사용한다. |

대량 처리용 새 프레임워크는 도입하지 않았다. 기존 Python 수집 도구와 Java/JDBC 모델을 사용한다.

</details>

<details>
<summary>재생성·적재 명령과 산출물</summary>

```powershell
python tools/build_district_expansion.py --output data/places/expansion/suyeong-2026-11-16
python tools/collect_busan_appeal_data.py --offline --dataset data/places/expansion/suyeong-2026-11-16/places.json --output data/places/expansion/suyeong-2026-11-16/appeal
./gradlew previewPlaceImport '-PplaceDataset=data/places/expansion/suyeong-2026-11-16/places.json' '-PplaceFacts=data/places/expansion/suyeong-2026-11-16/appeal/demo_appeal_facts.json'
```

DB 적용은 같은 두 경로를 지정해 `importPublicPlaces`를 실행한다. 기본 `previewPlaceImport`와 비로그인 공공 시연은 기존 386곳이다. 운영 DB를 읽는 로그인 목록에서 확대본을 사용한다. 실행 중인 앱은 장소 목록을 다시 불러와야 한다.

다음 구 확대 시에는 현재 최신 전체 `places.json`을 `--baseline`으로 지정하고 다음 구 정책을 사용한다. 속성 팩도 그 전체 목록으로 생성한다. 최초 386곳을 다시 baseline으로 사용해 이전 확대분이 빠지면 운영 확장 모드가 거부한다. 공공 원본 갱신·다른 구 지정 자료 확보는 별도 수집 작업이다.

- [장소 스냅샷](../data/places/expansion/suyeong-2026-11-16/places.json)
- [운영 적재 검증 기록](../data/places/expansion/suyeong-2026-11-16/import-verification.json)
- [품질·검토 보고서](../data/places/expansion/suyeong-2026-11-16/quality-report.json)
- [속성 팩·원본 지문](../data/places/expansion/suyeong-2026-11-16/appeal/checksums.json)

</details>

<details>
<summary>검증과 다음 단계</summary>

Java 327개·Python 69개, JAR 및 확대본 오프라인 적재 미리보기가 통과했다. H2에서 386→866 적재·기존 장소 내용/번호 유지·수영구 후보 검색·부분 입력 거부·반복 적재를 검증했다.
실제 DB에서도 기존 record_id·저장 코스·장소 스냅샷·이미지·제보·정정이 보존됐고, 저장 payload 불일치는 0건이었다. 같은 확대본 재적재는 변경 없이 866곳을 유지했다.
적재와 전후 비교·반복 검증을 포함한 Gradle 실행은 약 4분 57초였다. 순수 삽입 시간이나 부산 전체 예상 시간으로 해석하지 않는다.
866곳의 역/정류장 1,732건을 기존 전수 거리 계산과 대조해 불일치가 없었다.

이번 규모에서는 기존 메모리 검색을 유지한다. 부산 전체로 확대하기 전에는 DB 지역 조회용 정형 컬럼/인덱스와 적재 배치 성능을 별도로 보완하고 측정해야 한다. 격자 최적화는 교통 속성 수집에 적용한 것이며 DB 검색 최적화를 완료한 것은 아니다.

</details>
