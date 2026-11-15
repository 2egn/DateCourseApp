---
title: RecommendedCourseMapper
source: src/main/java/com/datecourse/service/RecommendedCourseMapper.java
source_commit: ea2242b
source_sha256: ed57289784d3346d7af0faebaafd130e0d048617674f803920f3d51c570b7891
readability_updated: 2026-11-14
---

# RecommendedCourseMapper

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 한눈에 보기

추천 결과를 예전 Course 모델로 바꾸는 호환 코드예요. 현재 편집은 RoutePlan을 사용해요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/com/datecourse/service/RecommendedCourseMapper.java)에서 확인해요.

```java
public final class RecommendedCourseMapper {
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
| Java 컬렉션·함수 | 목록 복사·조건 처리·전달할 동작 |

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]
