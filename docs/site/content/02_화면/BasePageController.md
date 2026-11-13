---
title: BasePageController
source: src/main/java/com/datecourse/controller/BasePageController.java
source_commit: ea2242b
source_sha256: 06295b66d2c88d7227105cf60877784949d5bbbd0207c6937054531fda706c27
readability_updated: 2026-11-14
---

# BasePageController

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 한눈에 보기

화면들이 상속받는 공통 부모예요. 로딩과 백그라운드 작업, 알림 코드를 재사용해요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/com/datecourse/controller/BasePageController.java)에서 확인해요.

```java
public abstract class BasePageController {
```

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `runWork()` | DB 같은 작업을 별도 스레드에서 실행하고 결과만 화면에 반영해요. |
| `beginLoading()` | 진행 중 작업 수를 세고 마지막 작업이 끝나야 로딩을 닫아요. |
| `notifyUser()` | 공통 팝업에 결과를 전달해요. |

## 동작 흐름

사용자 요청 또는 다른 클래스의 호출로 시작해요. 자세한 분기는 위 함수표와 원본 코드를 함께 봐요.

```mermaid
flowchart LR
 A[요청 또는 입력] --> B["runWork()"]
 B --> C[처리 결과 또는 화면 반영]
```

## 사용하는 라이브러리

| 도구 | 이 코드에서 필요한 이유 |
|---|---|
| JavaFX | 이벤트·화면·백그라운드 작업 |
| Java 컬렉션·함수 | 목록 복사·조건 처리·전달할 동작 |

라이브러리가 쓰이는 구체적인 위치는 위 함수표와 원본의 해당 함수 본문에서 확인해요. 화면 작업과 DB 작업을 같은 스레드에서 섞지 않는 것이 중요해요.

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]
