# 부산 공공 장소 데이터

수집일: 2026-11-10 (한국 시간). DB 적재용 파일이며, Google Places 데이터는 포함하지 않습니다.

## 포함된 데이터

| 구분 | 기록 수 | 기준일 |
|---|---:|---|
| 소상공인시장진흥공단 상가·업소 | 157,296 | 2026-09-30 |
| 부산문화포털 공공도서관 | 194 | 2026-09-11 |
| 박물관·미술관 표준데이터의 부산 기록 | 39 | 행별 데이터기준일자 |
| 도시공원 표준데이터의 부산 기록 | 731 | 행별 데이터기준일자 |
| 관광지 표준데이터의 부산 기록 | 87 | 행별 데이터기준일자 |
| 장소 기록 합계 | 158,347 | 위 기준일 혼합 |
| 부산 착한가격업소 (별도 자료) | 1,292 | 2026-09-30 |
| 착한가격업소 메뉴·가격 기록 | 2,660 | 2026-09-30 |

공공데이터에 등록된 범위입니다. 부산의 모든 실제 장소 또는 실시간 영업 현황을 보장하지 않습니다. 장소 합계는 출처별 기록 수로, 서로 다른 자료에 같은 시설이 포함될 수 있습니다. 동일 이름·도로명 건물주소가 겹치는 1,545개 그룹은 `possible_duplicate_places.json`에 보관했습니다. 같은 건물의 다른 점포일 수도 있어 자동으로 합치지 않았습니다.

## 파일과 필드

- `busan_places.csv`: 장소 기본정보. UTF-8 BOM, 쉼표 구분. `place_id`는 출처를 포함한 식별자입니다.
- `busan_good_price_shops.csv`: 착한가격업소 이름·주소·분류·출처. `price_shop_id`가 기본키입니다.
- `busan_menu_prices.csv`: 메뉴별 원문과 금액. `price_shop_id`로 착한가격업소와 연결합니다.
- `import_busan_public.sql`: 위 세 CSV와 같은 데이터, 테이블 생성 및 INSERT 문을 포함한 MySQL 8용 적재 파일.
- `manifest.json`: 출처 URL, 기준일, 수집 시각, 검증 통계 및 파일 SHA-256.
- `busan_data_overview.xlsx`: 수량과 메뉴 가격 표본만 담은 엑셀 요약본. 전체 데이터는 CSV와 SQL에 있습니다.
- `validation_report.json`: 파일 행 수, 식별자, 참조 관계, 원문 메뉴값, SQL 직렬화 행 수 및 체크섬 검증 결과. 실제 MySQL 실행 결과는 아닙니다.
- `raw/`: 부산 상가·도서관 원본 CSV와 부산에 해당하는 착한가격업소·표준데이터 원문 기록.

`busan_places.csv` 주요 열:

| 필드 | 의미 |
|---|---|
| place_id, source_record_id | 결합 식별자, 원본 식별자 |
| name, branch | 원본 상호·시설명, 지점명 |
| category | MEAL, CAFE, BAR, LEISURE, CULTURE, OTHER |
| industry_code, industry_name | 원본 업종 코드 및 업종명, 또는 시설자료명 |
| province, district, neighborhood | 시도, 구·군, 행정동 |
| road_address, lot_address | 도로명주소, 지번주소 |
| lat, lon | 위도, 경도. 누락 6건은 빈 값 유지 |
| phone, website | 원본에 있는 경우만 제공 |
| price_krw | 박물관·미술관의 성인 관람료 39건. 다른 장소는 미상 |
| price_basis | adult_admission: 성인 관람료, unknown: 미상 |
| rating, review_count | 원본에 없어 전부 미상 |
| source_dataset, source_reference_date, source_url | 출처 데이터셋 ID, 해당 행 기준일, 원문 링크 |

카테고리는 추천 점수가 아닙니다. 카페 업종은 CAFE, 음식 업종 중 주점은 BAR, 그 밖의 음식 업종은 MEAL, 예술·스포츠 업종은 LEISURE, 공공도서관·공원·박물관·관광지 자료는 CULTURE, 나머지는 OTHER로 분류했습니다. 업종명과 원문은 보존했습니다.

## 가격과 매칭의 의미

메뉴 금액은 해당 메뉴의 공시 가격입니다. 세트·여러 명 분량·숙박·미용 등의 가격도 포함될 수 있어 **1인 예상 지출로 환산하지 않았습니다**. `price_raw`와 `menu_name`을 함께 확인해야 합니다. `price_basis=as_listed_portion_unknown`은 원문 가격이며 분량 기준을 확정하지 않았다는 의미입니다.

착한가격업소 1,292건 중 900건만 이름과 도로명 건물주소가 모두 일치하고 후보가 하나인 경우 장소에 연결했습니다. 나머지 392건은 `linked_place_id`가 빈 값이며 별도 자료로 남겼습니다. 이름이 비슷하다는 이유로 가격을 옮기지 않았습니다.

빈 값은 미상입니다. CSV에서는 빈 필드, SQL에서는 `NULL`입니다. 실제 무료 관람료는 숫자 `0`으로 보존했습니다. 리뷰 평점은 공공 원문에 없으므로 임의 생성하지 않았습니다.

## MySQL 적재

1. MySQL Workbench 또는 DBeaver에서 대상 DB를 선택합니다. 예: `USE university_project;`
2. `import_busan_public.sql`을 SQL 스크립트로 엽니다. UTF-8 인코딩으로 실행합니다.
3. 아래 표를 조회해 결과를 확인합니다.

```sql
SELECT COUNT(*) FROM public_busan_places;       -- 158347
SELECT COUNT(*) FROM public_busan_price_shops;  -- 1292
SELECT COUNT(*) FROM public_busan_menu_prices;  -- 2660

SELECT p.name, m.menu_name, m.price_krw, m.price_basis
FROM public_busan_price_shops p
JOIN public_busan_menu_prices m USING (price_shop_id)
WHERE p.industry_name IN ('한식','중식','일식','양식');
```

생성하는 테이블은 `public_busan_places`, `public_busan_price_shops`, `public_busan_menu_prices`입니다. 회원 테이블 및 기존 `data_records`에는 적재하지 않습니다. 같은 파일을 다시 실행하면 기본키 기준으로 해당 공공데이터 테이블의 값을 갱신합니다. 다른 기준일 자료를 나중에 적재하면 과거 자료에만 있던 행은 자동 삭제하지 않습니다.

파일 생성과 검증을 완료했으며 실제 사용 중인 DB에는 아직 실행하지 않았습니다. 현재 앱의 `PlaceRepository`는 기존 `integrated_places.csv` 출처만 읽고 미상 가격·평점을 0으로 읽을 수 있습니다. 새 테이블을 앱 추천에 쓰려면 조회 대상과 미상 값 처리를 함께 연결해야 합니다.

## 출처

- [소상공인시장진흥공단 상가(상권)정보](https://www.data.go.kr/data/15083033/fileData.do)
- [행정안전부 착한가격업소 현황](https://www.data.go.kr/data/3045247/fileData.do)
- [부산광역시 부산문화포털 생활 문화 공공도서관](https://www.data.go.kr/data/15167428/fileData.do)
- [전국박물관미술관정보표준데이터](https://www.data.go.kr/data/15017323/standard.do)
- [전국도시공원정보표준데이터](https://www.data.go.kr/data/15012890/standard.do)
- [전국관광지정보표준데이터](https://www.data.go.kr/data/15021141/standard.do)

표준데이터의 부산 필터는 도로명 또는 지번주소의 부산 시도 표기에 적용했습니다. 지역별 누락, 원문 오기, 행별 기준일 차이가 있을 수 있습니다. 원문의 문화시설·관광지 구분을 유지하되 기존 앱에 맞춰 CULTURE로 묶었습니다.

## 다시 생성

프로젝트 루트에서 Python 3.10 이상으로 `python tools/prepare_busan_public_data.py`를 실행합니다. 표준 라이브러리만 사용하며 API 키가 필요하지 않습니다. 상가 ZIP은 전국 자료라 최초 실행 시 약 349 MB를 다운로드할 수 있습니다. 원본 캐시는 `build/public-data-fetch`에 두고, 공식 다운로드 URL이 바뀌면 상가·가격 CSV를 새로 받습니다. 표준데이터를 새로 받으려면 캐시의 `standard_15017323.json`, `standard_15012890.json`, `standard_15021141.json`을 지운 뒤 다시 실행합니다. 원문 갱신 후에는 행 수와 문서의 기준일도 함께 확인해야 합니다.
