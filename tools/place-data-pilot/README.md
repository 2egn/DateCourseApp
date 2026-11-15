# 장소 데이터 개선: 별도 검증 도구

현재 Java 데모는 `google-bindings-v5.json`의 9곳을 읽는다. 최신 전화·홈페이지 검증 결과는 `docs/research/place-binding-contact-results-2026-11-13.md`를 참고한다. 아래 5곳·24곳 설명과 v1~v3 결과는 이전 단계의 기록이다.

앱에 연결하기 전에 보유 공공자료와 Google Places API (New)의 가능성을 확인하는 실험이다. Python 기본 라이브러리만 사용하며 Java 앱·MySQL은 실행하지 않는다.

## 읽는 순서

1. `pilot.py`: `in_region` → `subcategory` / `public_price` → `prepare`.
2. `SAMPLE_IDS` → `GoogleProbe.request` → `match_status` → `live`.
3. `total_cost` / `within_budget` / `review_sections`: 후속 Java 구현에서 필요한 규칙의 작은 예제.
4. `test_pilot.py`: 가격 미확인·상한 초과·다른 지점·코스 후기의 처리 예시.

`total_cost`, `within_budget`, `review_sections`는 **테스트용 모델**이다. 현재 Java 서비스·실제 후기 DB와 연결되어 있지 않다. 시연용 가짜 후기를 실제 리뷰처럼 앱에 넣지 않는다.

## 실행

저장소 최상위에서 실행한다. Python 3.9 이상이 필요하다.

```powershell
# 외부 요청 없이 11개 규칙 검사
python -m unittest discover -s tools/place-data-pilot -p test_pilot.py -v

# 공공 CSV만 읽어서 별도 테스트 자료 생성
python tools/place-data-pilot/pilot.py prepare

# 명시적으로 실행해야만 Google 호출
python tools/place-data-pilot/pilot.py live
```

`prepare`는 이미 만들어진 테스트 파일을 덮어쓰지 않고 중단한다. 실제 결과는 `data/places/pilot/seomyeon-jeonpo-2026-11-13/`에 있다.

`live`는 [공통 접속 키](../../docs/공통_접속_키.md)가 있으면 서버에서 Google 키를 받아 메모리에 보관한다. 공통 설정이 없을 때만 `.local/google-places.properties`의 `google.places.api.key`를 사용한다. 검색 5회 + 상세 최대 5회, 재시도 없음. 검색은 ID 필드만 요청하며 상세는 리뷰까지 요청하므로 Place Details Enterprise + Atmosphere 등급에 해당한다. 응답 원문·가게명·좌표·가격·리뷰 본문을 저장하지 않고, 집계한 제공 여부만 `build/place-data-pilot/live-summary.json`에 남긴다.

중복 실행을 막기 위해 `.local/place-data-pilot-live.lock`을 생성한다. 이미 한 번 실행했으므로 다시 `live`를 실행하면 중단되는 것이 정상이다. 이 잠금은 API 전체 사용량이나 다른 프로그램의 요청을 제한하는 장치는 아니다. 이번 검증은 5곳의 표본이며 앱 추천 순위의 상위 5곳을 재현한 것이 아니다.

결과와 한계는 `docs/research/place-data-pilot-results-2026-11-13.md`를 읽는다. 키·빌드 결과는 Git에 넣지 않는다.

## 확인된 장소 ID 연결

`bind.py`는 별도의 5곳 대조를 실행해 확인된 연결만 `google-bindings.json`, 미확정 후보와 사유는 `google-binding-review.json`에 기록한다. 두 파일은 테스트 데이터 폴더에 있다. 가격·리뷰는 요청하지 않고, Google Place ID와 자체 검증 정보만 보관한다.

```powershell
# 매칭 규칙 + 기존 가격 규칙 전체: 16개. 외부 호출 없음
python -m unittest discover -s tools/place-data-pilot -p "test_*.py" -v

# 자바에서 연결표 조회. 외부 호출·DB 접속 없음
.\gradlew.bat runPlaceBindingDemo
```

실제 대조 명령은 `python tools/place-data-pilot/bind.py`다. 이미 수행했으며 `.local/place-data-binding-live.lock`과 결과 파일이 있어 다시 실행하면 중단한다. 앞선 `pilot.py live`의 잠금을 지우거나 재사용하지 않는다.

첫 연결표에는 대모골·파노라믹커피부산 2곳이 있다. 이후 24곳을 검증한 최신 `google-bindings-v3.json`에는 자동 조건 통과 8곳이 있다. `runPlaceBindingDemo`는 최신 표를 읽는다. 실제 앱의 `MEAL_`·`CAFE_` ID와 이 공공자료의 ID는 다르므로, 이 결과만으로 앱 화면에 외부 정보가 표시되지는 않는다.

`bind.py`의 `match_evidence` → `create_catalog` → Java `PlaceBindingCatalog.load` → `googlePlaceId` 순서로 읽는다. 자세한 결과와 Java 설명은 `docs/research/place-binding-2026-11-13.md`에 있다.

## 24곳 확대 검증과 최신 결과

- `matching.py`: 부가 설명·영문 병기·도로명/지번을 대조하고, 동일성과 방문 가능 여부를 구분한다.
- `evaluate.py`: 조회 전에 식사·카페·문화 각 8곳을 고정하고 후보 최대 2곳을 검사한다.
- `refine.py`: 근거가 있는 4곳만 추가 확인하고 한국어 검토 CSV를 만든다.
- `test_matching.py`: 다른 지점·애매한 후보·휴업·행정동·영문 병기·중복·검토 목록 검사를 수행한다.

전체 오프라인 Python 검사는 현재 33개다. `python -m unittest discover -s tools/place-data-pilot -p "test_*.py" -v`로 실행한다.

실제 조회 명령은 `python tools/place-data-pilot/evaluate.py live`, `python tools/place-data-pilot/refine.py`이며 둘 다 이미 실행했다. 잠금·출력 파일이 있어 재실행하면 중단하는 것이 정상이다. `plan`은 조회 전 표본을 고정하는 단계이며 이미 존재하는 표본을 덮어쓰지 않는다. 이전 파일을 지우고 연결률만 높이기 위해 다시 실행하지 않는다.

최종 판정은 자동 연결 8·검토 15·방문 제외 1이다. 자동 연결률 약 33%는 전면 도입에 부족하다. 독립 정답 검증을 한 정확도 수치가 아니다. 자세한 기준·한계·실행 방법은 `docs/research/place-binding-evaluation-2026-11-13.md`에 있다.

## 전화·홈페이지 검증

`contacts.py`는 문화시설 8곳에 주소 외 근거를 적용했다. `test_contacts.py`는 공유 번호·검증하지 않은 사이트·다른 지점·휴관·원본 보정 등을 검사한다. 전체 Python 검사는 현재 46개다.

공식 출처 보정은 `contact-source-overrides.json`으로 분리했다. 최신 판정은 연결 9·검토 13·방문 제외 2다. `google-contact-evaluation-v4.json`은 보정 전, `google-contact-reconciled-v5.json`은 보정 후이며, 검토 목록은 `binding-review-v5.csv`다. Google 응답이 아니라 공개 공식 페이지에서 확인한 세 곳의 보정만 저장했다.

`python tools/place-data-pilot/contacts.py live`와 `python tools/place-data-pilot/contacts.py reconcile`은 실제 API 호출 명령이며 이미 실행했다. 다시 호출하지 않고 `runPlaceBindingDemo`나 오프라인 테스트로 확인한다.
