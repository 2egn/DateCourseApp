# 서면·전포 테스트용 공공 장소 자료

**현재 최신 연결표는 `google-bindings-v5.json`(9곳)**이다. 공식 출처 보정은 `contact-source-overrides.json`, 검토 목록은 `binding-review-v5.csv`다. 아래 v1~v3 설명은 이전 단계이며 결과 파일은 보존한다.

기존 자료를 수정하지 않고 추출한 **2,669행**이다. 앱이나 DB에는 아직 연결하지 않았다. Google 가격·리뷰는 포함하지 않는다.

| 활동 | 범위 | 행 수 |
|---|---|---:|
| 식사 | 부산진구 부전1·2동, 전포1·2동 | 2,182 |
| 카페 | 같은 행정동 | 467 |
| 문화 | 서면역 임시 중심점 반경 2km 안, 주소상 부산진구 | 20 |

문화시설은 원본 행정동이 비어 있어 좌표와 주소를 사용했다. **식사·카페와 문화의 추출 범위는 같지 않다.** 원형 범위는 앱에서 사용자가 고르는 범위와 별개인 이번 자료 추출용 기준이다. 문화 20행은 도서관 8, 공원 9, 박물관·미술관 3행이다.

`places.csv`는 원본 열을 보존하고 다음 열만 덧붙였다.

| 추가 열 | 의미 |
|---|---|
| `subcategory` | 원본 업종을 활용한 세부 분류. 카페의 분위기·디저트 종류까지 구분하는 값은 아님 |
| `price_min_krw`, `price_max_krw` | 성인 관람료 근거가 있을 때만 채움. 빈칸은 미확인 |
| `price_unit` | `ADULT_TICKET` 또는 `UNKNOWN` |
| `price_state` | `EXACT`, `EXPLICIT_FREE`, `UNKNOWN` |
| `menu_rows_portion_unverified` | 연결된 공개 메뉴 행 수. 1인분 기준을 확인했다는 뜻이 아님 |

성인 관람료 근거가 있는 3행 중 원본상 무료 2행, 유료 1행이다. **현재 실제 가격을 새로 확인한 것은 아니다.** 나머지 2,666행은 가격 미확인이다. 공개 메뉴 65행이 장소 27곳에 연결되지만, 모두 분량 미확인이므로 인당 비용에 반영하지 않았다.

식사 분류 안에도 빵·떡·구내식당 등이 있다. 바로 데이트 식당 후보로 쓰지 말고 세부 업종 허용 목록을 검토해야 한다. 원본 ID 2,669개는 유일하지만, 실제 가게 중복이 없다는 보장은 아니다. 이름만으로 자동 병합하지 않았다.

출처는 각 행의 `source_url`, `source_dataset`, `source_reference_date`에서 확인한다. 원본 SHA-256과 생성 조건은 `manifest.json`에 있다. 재생성 코드는 `tools/place-data-pilot/pilot.py`이다.

## 외부 ID 연결표

자료 생성 후, 별도 대조를 통해 두 파일을 추가했다. `places.csv`와 기존 `manifest.json` 내용은 변경하지 않았다.

- `google-bindings.json`: 대모골·파노라믹커피부산의 공공자료 ID ↔ Google Place ID. 확인된 2곳만 사용 가능.
- `google-binding-review.json`: 미확정 3곳의 후보 ID와 검토 사유. 앱 조회에서 사용하지 않음.

Google 이름·좌표·주소·평점·가격·리뷰 본문을 옮겨 담은 자료가 아니다. 저장 예외인 Place ID와 자체 검증 상태만 담았다. 확인 시점 이후의 영업 상태를 보장하지 않는다.

Java `PlaceBindingCatalog`는 `places.csv`의 해시가 연결표와 다르면 읽기를 거절한다. 다른 파일을 복사해 같은 ID에 붙이지 말고, 자료가 변경되면 연결도 다시 검증한다.

### 최신 연결표와 검토 목록

후속 24곳 검사와 표적 보완을 거쳐 `google-bindings-v3.json`에 8곳을 등록했다. 이전 파일은 유지한다. Java 데모는 이 최신 표를 읽는다.

- `binding-sample-v2.json`: 식사·카페·문화 각 8곳의 고정 표본.
- `google-binding-evaluation-v2.json`: 첫 확대 검증의 결과와 자체 근거 코드.
- `google-bindings-v2.json`: 첫 확대 검증을 통과한 6곳.
- `google-binding-refinement-v3.json`: 4곳 추가 대조 이력과 전체 판정.
- `google-bindings-v3.json`: 자동 연결 조건 통과 8곳. 실제 앱은 아직 미연결.
- `binding-review-v3.csv`: 검토 15곳과 방문 제외 1곳을 한국어로 정리. 우리 자료 주소와 후보 확인 링크 포함.

원본 2,669행 중 24곳만 검사했다. 8곳의 규칙 통과가 현장 검증·독립적인 정확도 검증을 의미하지는 않는다. 가격과 영업을 보장하지 않으며, 기존 `places.csv`의 가격 값도 새로 확인한 것은 아니다. 상세 결과는 `docs/research/place-binding-evaluation-2026-11-13.md`를 참고한다.

전화·홈페이지 후속 검증에서는 문화 8곳을 대조하고 3곳의 공식 출처를 확인했다. 원본을 유지한 보정 파일에 연락처·올바른 시설 페이지·휴관 여부를 기록했고, 최종 판정은 전체 24곳에서 연결 9·검토 13·방문 제외 2다. Java 조회기는 보정 파일의 해시도 확인한다. 상세 결과는 `docs/research/place-binding-contact-results-2026-11-13.md`를 참고한다.
