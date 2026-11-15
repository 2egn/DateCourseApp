---
readability_updated: 2026-11-13
project: DateCourseApp
reviewed: 2026-11-11
source: src/main/resources/map/itinerary-rail.js
source_sha256: 6ac64c13eb96e8da2cf8b424e2383e8447b18772c57aac4ae0e6ed0354a9a63b
status: 지도 보충 설명
title: itinerary-rail.js
---

> 개발 중인 2026-11-11 작업본 기준. Java 클래스는 아니지만 실제 지도 흐름을 이해하는 데 필요하다.

## 한눈에 보기

지도 안에서 선택한 장소의 상세정보를 보여주고 현재 선택 상태를 기억하는 JavaScript 파일이에요. 예전 지도 내부 목록 코드도 함께 남아 있어요.

## 사용 장면과 읽는 순서

식당 상세 링크를 찾는 동안 사용자가 카페로 선택을 바꿀 수 있어요. 늦게 도착한 식당 결과를 카페 상세정보에 붙이지 않도록 현재 선택을 다시 확인해야 해요.

1. `setStandaloneItems()` 등으로 상세정보에 사용할 장소 목록을 받아요.
2. `activatePlace()`가 선택 상태를 바꾸고 `openPlace()`가 해당 장소의 상세를 열어요.
3. 링크를 찾은 뒤에도 같은 장소가 선택돼 있는지 확인해 반영해요. 현재 편집 목록은 JavaFX 오른쪽에 있고 예전 지도 내부 목록은 숨겨져 있어요.

모르는 용어는 [[문서_읽는법|문서 읽는 법과 용어 설명]]에서 찾아볼 수 있어요.

## 그림으로 보는 늦게 도착한 이전 응답

식당을 요청한 뒤 카페로 선택을 바꿨다면, 늦게 온 식당 응답을 지금 카페 화면에 적용하면 안 돼요. 응답을 반영하기 전 현재 선택과 같은 요청인지 확인해요.

<div class="learning-demo" data-learning="async"></div>

## 흐름

```text
map.js가 모듈 생성 → 장소 목록 전달 → 활성 장소/선택 상태 관리
목록·마커·Java 상세 요청 → 해당 장소 상세 표시
상세 링크 비동기 조회 → 현재 선택과 일치하면 반영
코스 교체·상세 닫기 → 이전 선택/애니메이션 정리
```

## 주요 함수

| 함수 | 역할 |
|---|---|
| `setStandaloneItems()` | 현재 편집 화면처럼 별도 목록 패널 없이 상세용 장소를 받아요. |
| `activatePlace()` | 해당 장소를 선택 상태로 전환한다. |
| `openPlace()` | 장소의 상세 정보를 연다. |

## 코드 일부 발췌

```javascript
        function switchTab(tab, user) {
            activeTab = tab;
            courseTab.setAttribute('aria-selected', String(tab === 'course'));
            nearbyTab.setAttribute('aria-selected', String(tab === 'nearby'));
            courseTab.tabIndex = tab === 'course' ? 0 : -1;
            nearbyTab.tabIndex = tab === 'nearby' ? 0 : -1;
            document.getElementById('itinerary-title').textContent = tab === 'course' ? '추천 코스' : '주변 장소';
            document.getElementById('itinerary-hint').textContent = tab === 'course'
// ... 표현식과 함수의 나머지는 원본 참고
```

## 현재 주의할 점

예전 지도 안의 코스 rail 코드도 남아 있다. 현재 편집 흐름은 JavaFX 우측 목록을 사용하므로 rail을 숨기고 상세 패널은 재사용해요. 비동기 링크 조회 결과가 다른 장소에 붙지 않게 선택 상태를 확인해요.

[[02_화면/PlannerMap|PlannerMap]] · [[02_화면/MapBridge|MapBridge]] · [[전체_동작_흐름|전체_동작_흐름]]
