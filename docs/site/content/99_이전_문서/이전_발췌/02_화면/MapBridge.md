---
aliases:
  - "02_화면/MapBridge"
document_status: archived
archived_at: 2026-11-26
current_source: src/main/java/com/datecourse/page/common/map/MapEventBridge.java
layout_updated: 2026-11-24
title: "보관 · MapEventBridge (이전 MapBridge)"
source: src/main/java/com/datecourse/controller/MapBridge.java
source_commit: ea2242b
source_sha256: 1a86040173a31e904f80856371408dbbd496481b1d71ea4f896c2b19281ebc59
readability_updated: 2026-11-14
---

> **보관 문서 · 현재 구현 안내가 아닙니다.** 현재 소스와 다른 버전의 코드 발췌입니다. 원래 source_commit·source_sha256과 발췌는 당시 근거로 보존합니다. 현재 설명은 [[기능별_구현/목차|기능별 구현 문서]], 보관 사유는 [[99_이전_문서/목차|이전 문서 목록]]에서 확인하세요.


## 현재 이름·경로 · 2026-11-24

`MapEventBridge` → `src/main/java/com/datecourse/page/common/map/MapEventBridge.java`. [[09_구조와_검토/페이지_구조와_이름_2026_11_24|페이지별 배치]]를 확인하세요. 아래 원본 링크·코드 발췌는 기존 `source_commit`의 기록이며 이름 변경 전 코드예요.


# MapBridge

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 한눈에 보기

지도 JavaScript가 Java에 보낼 수 있는 요청만 모은 연결 통로예요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/com/datecourse/controller/MapBridge.java)에서 확인해요.

```java
public final class MapBridge {
```

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `onLocationSelected()` | 유효한 위도·경도만 전달해요. |
| `onOriginChangeBlocked()` | 출발지 변경이 잠겼다는 안내를 요청해요. |
| `onPlaceSelected()` | 선택한 후보 번호를 전달해요. |
| `openExternalLink()` | 허용된 카카오 주소만 외부 브라우저로 열어요. |

## 동작 흐름

사용자 요청 또는 다른 클래스의 호출로 시작해요. 자세한 분기는 위 함수표와 원본 코드를 함께 봐요.

```mermaid
flowchart LR
 A[요청 또는 입력] --> B["onLocationSelected()"]
 B --> C[처리 결과 또는 화면 반영]
```

## 사용하는 라이브러리

| 도구 | 이 코드에서 필요한 이유 |
|---|---|
| Java 컬렉션·함수 | 목록 복사·조건 처리·전달할 동작 |

라이브러리가 쓰이는 구체적인 위치는 위 함수표와 원본의 해당 함수 본문에서 확인해요. 화면 작업과 DB 작업을 같은 스레드에서 섞지 않는 것이 중요해요.

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]
