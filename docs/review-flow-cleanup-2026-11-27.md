# 추천·저장 흐름 정리와 운영 장소 목록 통합 · 2026-11-27

추천 조건 모델 통일, 실제 경로 조회 상태 사용, 저장 표 행 직접 전달. 일반 앱은 현재 운영 장소 목록 하나로 통합.

| 변경 | 현재 흐름 |
|---|---|
| 추천 | `candidates(..., NextPlaceCondition)`과 전체 결과용 `matching()` 유지. 이전 인수 나열 오버로드·별칭 상수 제거, 호출부·검사 동시 전환 |
| 경로 상태 | `CourseEditorController`가 `CourseMapView.isRoutePending()` 조회. 안내 문구의 `중`·`준비` 포함 여부로 로딩을 추측하지 않음 |
| 저장 표 | `SavedCoursePresenter` 내부 행 record → `SavedCourseController`의 TableView. 설명 문자열 생성 후 다시 파싱하는 함수 3개 제거 |
| 장소 자료 | 앱은 운영 DB 목록만 사용. 자료 검증·적재 도구의 기본값은 `current.json`에 지정한 5,628곳 쌍 |
| 보관 | 시연 전용 코드·9곳/386곳 목록·파일럿 및 이전 속성 자료 54개를 `archive/legacy-place-demo`로 이동, SHA 보존 |

새 서비스·설정 클래스·상태 관리 계층은 추가하지 않음. 기존 Presenter·불변 코스 모델·요청 번호·세션 검사·저장 스냅샷은 유지.

Java 391개와 보관 UI 2개 통과, JAR 생성·시연 항목 제외 확인. 대중교통 실패 안내의 로딩 오인, 문장부호·줄바꿈이 있는 장소의 표 열 보존, 늦은 장소 조회 응답 무효화 검사 포함. 운영 API·DB 쓰기는 수행하지 않음.

Python 도구 검사 32개 중 31개, 별도 자료 도구 51개 통과. 남은 1개는 이번 변경과 무관한 기존 이력 지문 불일치: `jung-gu-2026-11-25/baseline.json`은 시각 정정 facts의 SHA를 `4e33ef…`로 기록했지만, 파일과 기존 HEAD 원본은 모두 `2c4df4…`. 원본·이력을 임의 수정하거나 검사를 완화하지 않음. 보관 파일 지문과 현재 운영 자료 쌍의 지문은 모두 일치.

## 컨트롤러 읽기 순서 추가 정리

클래스·파일·외부 API를 늘리지 않고 같은 클래스 안에서 입력·작업·결과 반영을 인접 배치. 기존 이벤트·처리 순서·문구·세션·요청 번호 검사는 유지.

| 진입점 | 같은 클래스에서 이어 읽을 처리 |
|---|---|
| `CourseEditorController.onRecommendNext()` | `readNextPlaceCondition()` → 기존 검색 실행 → `applyCandidateSearch()` |
| `CoursePlannerController.onSaveCourse()` | `readSavePlan()` → `setSaving()`·기존 저장 실행 → `applyCourseSave()` |
| `SavedCourseController.showPreview()` | `resetPreview()` → 기존 이미지 조회·`decodePreview()` → `applyPreview()` |

저장 입력 잠금·복원은 `setSaving()`에서 공통 처리. 미리보기는 화면 전용 변환 취소와 UserContext 공유 조회를 구분하며, FX 스레드 반영 전 요청 번호·세션 확인 유지.

추가 정리 검증: Java 391개·보관 UI 2개·JAR·문서 로컬 빌드 통과. 코드 기준 `55f1258`. 기존 자료 도구의 과거 지문 불일치 기록은 위와 같음.

## 주석 보완과 기준본 지문 정정

코드 기준 `c65c157`. 추천·대안 검증·가격 모델·비용 합산·직선 범위·카카오 경로·TMAP 혼합 선택의 판단 근거와 연결 클래스를 주석에 보완했다. 주석 제외 실행 코드는 동일하며 Java 전체 391개·보관 UI 2개 검사를 통과했다.

위에서 보류했던 과거 속성 지문은 최초 Git 원본 `933fd85`와 현재 파일의 바이트가 같고 기존 2,040개 속성 행이 중구 확장본에 그대로 보존됨을 확인했다. 기준본·입력 명세를 실제 지문 `2c4df4…`로 정정하고, 이전 `4e33ef…`와 확인 한계는 `data/places/expansion/jung-gu-2026-11-25/baseline-hash-correction-2026-11-27.json`에 보존했다. 해당 이전 값을 생성한 바이트는 확보하지 못했다. 당시 운영 보고서·장소·속성 본문·현재 운영 파일 쌍·DB는 변경하지 않았다. 기존 검사를 완화하지 않았으며 Python 32개가 모두 통과한다.

별도 자료 도구 51개·사이트 Node 64개 통과. 개인 vault 문서·IDE 설정·기존 사용자 공백 및 탐색 안내 수정은 이번 커밋에 포함하지 않는다. 원격 푸시와 운영 사이트 배포는 별도 결과로 구분한다.
