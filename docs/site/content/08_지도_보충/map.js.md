---
readability_updated: 2026-11-13
project: DateCourseApp
reviewed: 2026-11-11
source: src/main/resources/map/map.js
source_sha256: 41e7c36f8cbcfb39e1169501cb3968f91feb3e37952d1089702a568b37991f38
status: 지도 보충 설명
title: map.js
reviewed_commit: 9e59289
reviewed_at: 2026-11-23
---

## 렌더링 여유 영역 · 2026-11-24

[[지도_렌더링_여유_영역_2026_11_24|SDK 렌더링과 보이는 창의 분리·좌표·확대·캡처 보정]]을 반영했어요. 이전 발췌는 당시 기준으로 유지해요.

## 현재 구현 · 2026-11-23

상시 범례 대신 원 테두리에서 범위·반경을 안내해요. 장소 핀은 상세 카드, 경로선·번호는 호버 요약과 클릭 상세 안내를 사용해요.

| 주요 함수·구성 | 역할 |
|---|---|
| `rangeTooltip` | 투영 좌표로 테두리만 판정하고 드래그·다른 팝업을 우선해요. |
| `showWalkingRoute()` | 선택한 이동수단의 경로·구간 안내를 표시해요. |
| `presentRoute()` | 새 코스를 반영하고 이전 코스 이미지를 무효화해요. |

앱 코드 기준 `9e59289`. [[현재_구현_흐름|현재 흐름]] · [[변경_요약|변경 요약]]

<details>
<summary>이전 기준의 발췌와 학습 설명</summary>

## 최신 변경 · 2026-11-15

코스 핀은 장소 상세, 선과 구간 번호는 경로 안내로 분리해요. 원 조절은 현재 원을 맞추고, 새 구간 맞춤은 직전 장소와 추가 장소 및 준비된 경로 좌표만 사용해요. 두 범위 보기와 저장용 직선 지도는 별도 목적이에요.

[[현재_구현_흐름|최신 사용 흐름]] · [[최종_검토_2026_11_15|검증 범위]]

## 이전 버전의 상세 설명

아래 함수표·발췌·원본 링크는 기존 source_commit 기준 기록이에요. 위 최신 변경과 함께 읽어 주세요.

## ea2242b 최신 변경

- `setOriginLocked()`·`selectMapLocation()`: 장소가 있는 코스의 출발지 변경을 막아요.
- `showWalkingRoute()`·`selectWalkingLeg()`: 실제 도보 구간과 고정 팝업을 표시하고 선·핀으로 선택해요.
- `toggleWalkingExplanation()`: 버튼을 눌렀을 때만 상단 설명을 열어요.
- `dismissWalkingPopup()`: 빈 지도 클릭은 팝업만 닫고 출발지 알림을 만들지 않아요.
- 고정 팝업 중에도 다른 선 hover는 강조하며, mouseout은 원래 구간으로 돌아와요.

[[현재_구현_흐름|현재 흐름]] · 아래는 이전 버전의 배경 설명이에요.

> 개발 중인 2026-11-11 작업본 기준. Java 클래스는 아니지만 실제 지도 흐름을 이해하는 데 필요하다.

## 한눈에 보기

Java가 전달한 코스를 받아 카카오 지도 위에 장소 표시·두 거리 원·경로·상세정보를 그리는 JavaScript 파일이에요. Java는 코스 정보를 계산하고, 이 파일은 그 정보를 지도에서 보이게 해요.

## 사용 장면과 읽는 순서

카페를 추가하면 Java가 새 코스를 보내고, 이 파일이 해당 위치에 장소 표시를 그려요. 반대로 지도에서 출발지를 클릭하면 선택 좌표를 MapBridge를 통해 Java에 알려줘요.

1. `presentRoute()`가 코스·후보·거리 조건을 받아 표시해요.
2. 검색이나 지도 클릭은 위치 선택 알림으로, 상세 보기 요청은 상세 패널로 이어져요.
3. `fitRouteView()`는 요청한 전체 화면 맞춤이고, `ensureRouteRangesVisible()`은 거리 범위를 바꿨을 때 필요한 경우에만 확대·축소를 조정해요.

모르는 용어는 [[문서_읽는법|문서 읽는 법과 용어 설명]]에서 찾아볼 수 있어요.

## 흐름

```text
PlannerMap.render() → window.presentRoute() → 원·장소 마커·경로 표시
검색 입력 → scheduleSearch()/searchPlaces() → 카카오 장소 검색 → 결과 선택
지도 선택 → selectMapLocation() → MapBridge.onLocationSelected()
상세 열기 → itinerary 모듈 → resolvePlaceUrl() → 카카오 장소 링크
반경 변경 → ensureRouteRangesVisible() → 두 원의 화면 위치 확인 → 필요한 줌만 변경
```

## 주요 함수

| 함수 | 역할 |
|---|---|
| `presentRoute()` | Java가 넘긴 코스·후보를 지도에 보여줘요. |
| `fitRouteView()` | 전체 코스 또는 두 범위를 화면에 맞춘다. |
| `ensureRouteRangesVisible()` | 두 원이 벗어나면 줌아웃, 다음 줌인에도 15% 여유가 있으면 다시 확대한다. |
| `resolvePlaceUrl()` | 이름·좌표 등을 사용해 상세 링크를 찾아요. |
| `connectJavaMapBridge()` | 페이지·SDK의 준비 순서와 관계없이 Java 연결을 마친다. |

## 코드 발췌

```javascript
    // Set false to restore the original SDK wheel/double-click zoom behavior.
    const USE_INSTANT_ZOOM = true;
    // Experimental map-only scaling inside WebView; false restores SDK-level wheel behavior.
    const USE_FRACTIONAL_ZOOM = false;
```

## 현재 주의할 점

전체 원은 파란색, 다음 장소 원은 초록 점선이다. fit은 사용자가 요청하는 화면 맞춤이고 ensure는 반경 변경에 따른 조건부 조정이다. 별도 화면 배율 실험은 현재 비활성이다.

[[02_화면/PlannerMap|PlannerMap]] · [[02_화면/MapBridge|MapBridge]] · [[전체_동작_흐름|전체_동작_흐름]]


</details>
