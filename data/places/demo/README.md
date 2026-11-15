# 앱에서 선택할 서면·전포 시연 목록

`seomyeon-jeonpo-demo.json`은 원본을 변경하지 않고 따로 만든 9곳의 앱용 자료다. 식사 5·카페 2·도서관 2곳이며, 다양한 문화 활동이 충분한 전체 서비스용 자료는 아니다.

- 앱 ID: `DCA-DEMO-001`~`DCA-DEMO-009`. 순서를 바꿔도 기존 ID를 다시 배정하지 않는다.
- `recordId`: 1,000,001~1,000,009. 지도 선택·방문 순서·저장 스냅샷용 숫자 ID다.
- 공개 원본 ID와 확인된 Google ID는 `details.externalIds`에 별도로 둔다.
- 기본 이름·주소·좌표·세부 업종은 보유 공공자료, 일부 연락처·시설 페이지는 공식 출처 보정 파일을 사용했다.
- Google의 가격·평점·리뷰·좌표 응답을 복사한 자료가 아니다. Google 값은 저장 허용 예외인 장소 ID만 포함한다.
- **9곳 모두 1인 가격은 미확인**이다. 메뉴 분량을 확인하지 않고 숫자를 채우지 않는다. 장소 대조일은 가격 확인일이나 현재 영업 보장이 아니다.

기준 자료는 `../pilot/seomyeon-jeonpo-2026-11-13/places.csv`, `google-bindings-v5.json`, `contact-source-overrides.json`이다. 각 행의 출처·기준일·대조일을 함께 보관했다.

Gradle `processResources`가 이 JSON 한 개만 `datasets/` 리소스로 복사한다. 다른 원본 CSV·실험 결과·개인 API 키를 포함시키는 설정은 추가하지 않았다. 앱의 `PilotPlaceRepository`는 이 리소스를 읽으며 DB에 적재하지 않는다.

## 실행

```powershell
.\gradlew.bat runPilotDemo
```

회원 DB 없이 시연 목록으로 시작한다. 실제 지도에는 기존 카카오 JavaScript 키 설정과 네트워크가 필요하다. Google API 키나 Google 호출은 이 화면 실행에 필요하지 않다.

편집 화면에서 **가격 미확인·상한 미확인 장소도 포함**을 선택해야 현재 시연 장소를 추천받을 수 있다. 체크하지 않으면 가격 상한을 증명할 수 없는 후보는 제외한다. 원본 0원도 근거 없이 무료로 표시하지 않는다.
