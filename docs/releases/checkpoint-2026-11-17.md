# 버전 기준 · 2026-11-17

**`checkpoint-2026-11-17`은 장소 866곳과 카카오 자동차·대중교통 연결을 묶은 로컬 체크포인트다.**
앱 버전은 `1.0-SNAPSHOT`을 유지한다. 파일 해시와 외부 상태는 [버전 명세](checkpoint-2026-11-17.json)에 기록했다. 이번 정리에서는 원격 push·DB 쓰기·Worker 재배포를 하지 않았다.

<details>
<summary>주요 변경 · 파일과 역할</summary>

| 커밋 | 파일·클래스 | 설명 |
|---|---|---|
| `9fe645b` | `PublicPlaceImport`, `PlaceCatalogRepository`, `tools/build_district_expansion.py` | 기존 장소를 보존하며 480곳 추가, 별도 데이터·근거 파일 입력과 축소 방지 |
| `10ae152` | `PlaceFacets`, `PlacePresenter` | 후보와 지도 상세의 시설 종류 표시 통일 |
| `dbca5f2` | `TravelMode`, `TravelRoute`, `KakaoRouteService`, `TravelRouteService` | 도보·자동차·대중교통 선택과 경로 조회 |
| `dbca5f2` | `12_saved_course_transport.sql`, `docs/site/src/` | 사용자 선택 교통수단 저장, 공유 키 응답에 카카오 REST 키 선택 제공 |
| `2d9a9ce` | 공공 입력 `.gitattributes`, `manifest.json`, `checksums.json` | Windows 체크아웃·아카이브에서 LF 기준 해시 재현 |

<details>
<summary>설계와 범위</summary>

장소 자체·가격 근거·태그·일시적인 경로 응답을 분리한다. 저장 코스에는 사용자 선택 교통수단을 기록하며 공급사 경로 응답이나 인증값은 넣지 않는다. 데이터 확장과 실제 지도 검증의 상세는 [수영구 확대](../district-expansion-2026-11-16.md), [카카오 경로](../kakao-routing-2026-11-16.md)를 참고한다.

</details>
</details>

<details>
<summary>코드·데이터·DB·Worker 기준</summary>

- 코드: `2d9a9ce98401326fb7e4e3ed280780c3606b0d45`. 최종 문서 커밋에 체크포인트 태그를 붙인다.
- 내장 시연 자료: 386곳. 운영 카탈로그: 866곳(식사 540·카페 270·문화 56).
- 운영 자료: `data/places/expansion/suyeong-2026-11-16/places.json`과 같은 폴더의 `appeal/demo_appeal_facts.json`.
- DB 읽기 전용 확인: 활성 866곳, 내용 불일치 0, 카탈로그 지문 일치. 지문은 버전 명세에 기록했다.
- 스키마 10/11은 카탈로그·미리보기 기준이다. **스키마 12의 교통수단 테이블은 확인 시점에 없었다.** 현재 코드가 최초 저장 때 생성하며, 이번 작업에서는 생성하지 않았다. 실제 DB 저장·다시 열기 검증은 남아 있다.
- Worker: `7266c63e-75db-4b39-9653-dae443a00352`, 2026-11-16 10:53:24 UTC 배포, 트래픽 100%. 비밀값은 기록하지 않는다.

</details>

<details>
<summary>검증과 재현 방법</summary>

독립 Git 아카이브에서 Java 336개·Python 69개·Worker 55개 통과, JAR 생성 확인. 표시 변경 커밋의 관련 Java 11개도 통과했다. 최초 아카이브 검증에서 발견한 해시 오류는 `2d9a9ce`로 수정한 뒤 Python 18개를 재검증했다. Java·Worker 소스는 해당 수정에서 변경하지 않았다.

사용자 변경이 있는 체크아웃은 그대로 두고, 새 폴더에서 확인한다.

```powershell
git worktree add --detach ../DateCourseApp-checkpoint checkpoint-2026-11-17
cd ../DateCourseApp-checkpoint
./gradlew test jar --console=plain
python -m unittest discover -s tools -p 'test_*.py'
python -m unittest discover -s tools/place-data-pilot -p 'test_*.py'
node --test docs/site/test/*.test.mjs
```

866곳 자료의 오프라인 입력 검증(실행 확인 완료):

```powershell
./gradlew previewPlaceImport '-PplaceDataset=data/places/expansion/suyeong-2026-11-16/places.json' '-PplaceFacts=data/places/expansion/suyeong-2026-11-16/appeal/demo_appeal_facts.json'
```

`importPublicPlaces`는 실제 DB 변경 작업이다. 기본 내장 386곳을 운영 866곳에 덮어쓰지 않는다. 축소 방지 검사가 이를 차단한다. `verifyKakaoRoutes`는 인증 설정과 실제 API 호출이 필요한 별도 검증이며 일반 테스트에는 포함하지 않는다.

</details>

<details>
<summary>남은 범위와 복구 기준</summary>

- 후보 반경·직선거리 정책은 교통수단별로 다시 검토해야 한다. 부산 전체 확대와 중복 의심 자료 검토도 후속 작업이다.
- 데스크톱 직접 DB 접근과 공유 인증 전달은 현재 팀 사용 범위의 구조다. 공개 배포 전 서버 경계를 정리한다.
- 태그는 소스 복구 기준이다. DB·Worker·비밀 설정은 Git 버전을 바꿔도 되돌아가지 않는다. 운영 자료를 과거 표본으로 축소하거나 사용자 코스·정정 정보를 제거하는 방식으로 복구하지 않는다.
- 기존 사용자 체크아웃과 공백 변경은 보존했다. 원격 저장소 반영은 별도 단계다.

</details>
