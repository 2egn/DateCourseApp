---
readability_updated: 2026-11-13
project: DateCourseApp
reviewed: 2026-11-11
source: src/main/resources/map/instant-zoom.js
source_sha256: 076146dcec366c684a5786465c2c669983cebf7714f094ca19d06403a1710afc
status: 지도 보충 설명
title: instant-zoom.js
---

> 개발 중인 2026-11-11 작업본 기준. Java 클래스는 아니지만 실제 지도 흐름을 이해하는 데 필요하다.

## 한눈에 보기

지도 위의 마우스 휠·더블클릭 입력을 받아 확대·축소 요청으로 바꾸는 JavaScript 파일이에요. 현재 방식은 웹 화면 전체를 늘리는 대신 카카오 지도 자체의 확대 단계를 바꿔요.

## 사용 장면과 읽는 순서

작은 터치패드 입력은 조금 모았다가 처리하고, 실제로 단계를 바꿀 때는 지도 애니메이션을 사용해요. 모든 휠 입력마다 화면 전체가 커지는 방식은 아니에요.

1. `installInstantZoom()`이 두 지도에 같은 입력 처리 방식을 연결해요.
2. `wheel()`이 방향과 누적 입력을 확인해 불필요한 연속 요청을 조절해요.
3. `apply()`가 허용된 확대 단계 안에서 setLevel과 애니메이션을 적용해요. installFractionalZoom()은 보류된 별도 실험 경로예요.

모르는 용어는 [[문서_읽는법|문서 읽는 법과 용어 설명]]에서 찾아볼 수 있어요.

## 흐름

```text
installInstantZoom() → wheel/dblclick 이벤트 연결
wheel() → 작은 입력 누적 → 대기 요청 하나 → apply()
apply() → 현재 레벨 ±1 → 마우스 아래 좌표 anchor → setLevel(animate)
실험 installFractionalZoom() → 소수 배율 계산 (현재 기본 경로에서 사용하지 않음)
```

## 주요 함수

| 함수 | 역할 |
|---|---|
| `installInstantZoom()` | 공통 이벤트 처리를 설치하고 해제 함수를 돌려줘요. |
| `wheel()` | 휠 방향·누적량과 호출 간격을 처리해요. |
| `apply()` | 레벨 1~14 안에서 변경하고 SDK 애니메이션을 준다. |
| `installFractionalZoom()` | 보류한 지도 화면 확대 실험 코드다. |

## 코드 발췌

```javascript
        const motion = options && options.motion || 'auto';
        const interval = 320; // Let the SDK's 300ms animation finish before the next level.
        const threshold = 40; // One normal wheel notch reacts immediately; tiny trackpad noise accumulates.
        const duration = 300;
        let pending = null, timer, resetTimer, delta = 0, lastApplied = -Infinity;
        const reducedMotion = window.matchMedia('(prefers-reduced-motion: reduce)');
```

## 현재 주의할 점

현재 duration=300ms, interval=320ms, 작은 입력 임계값=40px이다. 첫 유효 입력은 바로 처리해요. 시스템 애니메이션 감소 설정이면 animate=false다. 화면을 늘려 중간 배율에 멈추는 방식과 구분해요.

[[02_화면/PlannerMap|PlannerMap]] · [[02_화면/MapBridge|MapBridge]] · [[전체_동작_흐름|전체_동작_흐름]]
