---
title: PlaceBindingCatalog
source: src/main/java/com/datecourse/service/PlaceBindingCatalog.java
source_commit: ea2242b
source_sha256: 1b037e171df94a374055d327332347063f834941c61d5bbe374d6eedfc347b18
readability_updated: 2026-11-14
---

# PlaceBindingCatalog

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 한눈에 보기

검증한 공개 장소 ID와 외부 Google 장소 ID의 연결표를 읽어요. Google 리뷰·가격을 복사하는 저장소는 아니에요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/com/datecourse/service/PlaceBindingCatalog.java)에서 확인해요.

```java
public final class PlaceBindingCatalog {
```

## 동작 흐름

```mermaid
flowchart LR
 A[다른 클래스에서 값 전달] --> B[현재 타입으로 묶거나 변환]
 B --> C[화면 또는 처리 코드에서 사용]
```

## 사용하는 라이브러리

| 도구 | 이 코드에서 필요한 이유 |
|---|---|
| Jackson | 가격·출처 JSON 변환 |
| Java 컬렉션·함수 | 목록 복사·조건 처리·전달할 동작 |
| Java 시간 | 수정 시각 보관·표시 |

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]
