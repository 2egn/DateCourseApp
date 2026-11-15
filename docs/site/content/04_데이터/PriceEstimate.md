---
title: PriceEstimate
source: src/main/java/com/datecourse/model/PriceEstimate.java
source_commit: ea2242b
source_sha256: 140908f2fba91d979df75d903c6aa221ca241f558c9fc38872d597ffa1519e42
readability_updated: 2026-11-14
---

# PriceEstimate

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 한눈에 보기

가격의 최소·최대·단위·근거를 묶어요. null은 모름이며 무료 0원과 달라요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/com/datecourse/model/PriceEstimate.java)에서 확인해요.

```java
public record PriceEstimate(Integer minimum, Integer maximum, Unit unit, String basis) {
```

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `unknown()` | 가격 미확인 상태를 만들어요. |
| `withinCap()` | 확인된 최대 가격이 조건 안인지 확인해요. |
| `display()` | 1인·음료 한 잔 등 단위를 붙여 표시해요. |

## 동작 흐름

사용자 요청 또는 다른 클래스의 호출로 시작해요. 자세한 분기는 위 함수표와 원본 코드를 함께 봐요.

```mermaid
flowchart LR
 A[요청 또는 입력] --> B["unknown()"]
 B --> C[처리 결과 또는 화면 반영]
```

## 사용하는 라이브러리

| 도구 | 이 코드에서 필요한 이유 |
|---|---|
| Java 컬렉션·함수 | 목록 복사·조건 처리·전달할 동작 |

라이브러리가 쓰이는 구체적인 위치는 위 함수표와 원본의 해당 함수 본문에서 확인해요. 화면 작업과 DB 작업을 같은 스레드에서 섞지 않는 것이 중요해요.

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]
