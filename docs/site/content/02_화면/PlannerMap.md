---
current_source: src/main/java/com/datecourse/page/common/map/CourseMapView.java
layout_updated: 2026-11-24
title: CourseMapView (이전 PlannerMap)
source: src/main/java/com/datecourse/controller/PlannerMap.java
source_commit: ea2242b
source_sha256: b42018476b4b1d279ad83320174d3bfb24f5a7fa804396fc9a13834e80d65f06
readability_updated: 2026-11-14
reviewed_commit: 9e59289
reviewed_at: 2026-11-23
---

## 현재 이름·경로 · 2026-11-24

`CourseMapView` → `src/main/java/com/datecourse/page/common/map/CourseMapView.java`. [[페이지_구조와_이름_2026_11_24|페이지별 배치]]를 확인하세요. 아래 원본 링크·코드 발췌는 기존 `source_commit`의 기록이며 이름 변경 전 코드예요.


## 현재 구현 · 2026-11-23

지도 수명주기와 코스 표시·캡처·경로 조회를 연결해요. 도보·자동차·대중교통과 도보를 지원하고, 저장 코스의 실제 경로는 명시적으로 요청할 때만 조회해요.

| 주요 함수·구성 | 역할 |
|---|---|
| `requestWalkingRoute()` | 요청 준비와 작업 시작. 기존 walking 이름은 호환을 위해 유지해요. |
| `applyRouteSuccess() / applyRouteFailure()` | 현재 요청만 성공·실패 처리해요. |
| `captureRouteImage()` | 다른 코스·이전 페이지의 이미지 응답을 차단해요. |
| `dispose()` | 요청과 지도 수명주기를 정리해요. |

앱 코드 기준 `9e59289`. [[현재_구현_흐름|현재 흐름]] · [[변경_요약|변경 요약]]

<details>
<summary>이전 기준의 발췌와 학습 설명</summary>

# PlannerMap

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 최신 변경 · 2026-11-15

비표시 지도의 로드를 미루고, Java CameraMotionPolicy를 공통 지도 어댑터로 연결해요. 반경 편집은 현재 원, 장소 추가는 새 구간을 맞춰요. 저장용 지도 캡처는 실제 지도 바탕에 직선 경로를 표시하고 완료 후 편집 화면을 복원해요.

[[현재_구현_흐름|최신 사용 흐름]] · [[최종_검토_2026_11_15|검증 범위]]

## 이전 버전의 상세 설명

아래 함수표·발췌·원본 링크는 기존 source_commit 기준 기록이에요. 위 최신 변경과 함께 읽어 주세요.

## 한눈에 보기

Java 코스와 WebView 지도를 연결해요. 두 지도는 별도 상태를 갖고 실제 TMAP 도보 선만 표시해요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/com/datecourse/controller/PlannerMap.java)에서 확인해요.

```java
final class PlannerMap {
```

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `presentCandidates()` | 코스와 후보를 지도에 전달해요. |
| `setOriginLocked()` | 장소가 있는 코스의 출발지 변경을 막아요. |
| `requestWalkingRouteWhenReady()` | 편집 완료·불러오기 후 지도 준비에 맞춰 경로를 계산해요. |
| `toggleWalkingExplanation()` | 요청할 때만 경로 설명을 보여줘요. |
| `dispose()` | 로그아웃 시 요청과 WebView를 정리해요. |

## 동작 흐름

사용자 요청 또는 다른 클래스의 호출로 시작해요. 자세한 분기는 위 함수표와 원본 코드를 함께 봐요.

```mermaid
flowchart LR
 A[요청 또는 입력] --> B["presentCandidates()"]
 B --> C[처리 결과 또는 화면 반영]
```

## 사용하는 라이브러리

| 도구 | 이 코드에서 필요한 이유 |
|---|---|
| JavaFX | 이벤트·화면·백그라운드 작업 |
| Jackson | 가격·출처 JSON 변환 |
| Java 컬렉션·함수 | 목록 복사·조건 처리·전달할 동작 |

라이브러리가 쓰이는 구체적인 위치는 위 함수표와 원본의 해당 함수 본문에서 확인해요. 화면 작업과 DB 작업을 같은 스레드에서 섞지 않는 것이 중요해요.

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]


</details>
