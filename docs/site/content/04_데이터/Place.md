---
title: Place
source: src/main/java/com/datecourse/model/Place.java
source_commit: ea2242b
source_sha256: a44baeb21e7cf2580239a0a10bd80424de113d3a1b55de201ad43aa86049d4e5
readability_updated: 2026-11-14
---

# Place

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 한눈에 보기

장소 하나를 묶는 record예요. 가격 범위와 미확인 상태, 공개 출처도 함께 가져요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/com/datecourse/model/Place.java)에서 확인해요.

```java
public record Place (
```

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `priceText()` | 화면에 쓸 가격 범위 또는 미확인 문구를 만들어요. |
| `price()` | 옛 코드용 정확한 금액 접근이에요. 범위·미확인에서는 예외가 나요. |

## 동작 흐름

사용자 요청 또는 다른 클래스의 호출로 시작해요. 자세한 분기는 위 함수표와 원본 코드를 함께 봐요.

```mermaid
flowchart LR
 A[요청 또는 입력] --> B["priceText()"]
 B --> C[처리 결과 또는 화면 반영]
```

## 사용하는 라이브러리

| 도구 | 이 코드에서 필요한 이유 |
|---|---|
| Jackson | 가격·출처 JSON 변환 |

라이브러리가 쓰이는 구체적인 위치는 위 함수표와 원본의 해당 함수 본문에서 확인해요. 화면 작업과 DB 작업을 같은 스레드에서 섞지 않는 것이 중요해요.

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]
