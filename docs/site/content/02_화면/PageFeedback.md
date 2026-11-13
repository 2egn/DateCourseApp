---
title: PageFeedback
source: src/main/java/com/datecourse/controller/PageFeedback.java
source_commit: ea2242b
source_sha256: 251dbce5d314c3d61014acc0e6b3d194bdde96913ead7f0fc30869cce8d1ecfd
readability_updated: 2026-11-14
---

# PageFeedback

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 한눈에 보기

여러 화면이 같은 객체를 공유하는 로딩·알림 UI예요. 지도 위에 겹쳐 띄워 크기를 바꾸지 않아요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/com/datecourse/controller/PageFeedback.java)에서 확인해요.

```java
public final class PageFeedback {
```

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `show()` | 작은 알림과 5초 남은 시간 바를 표시한 뒤 흐려지며 닫아요. |
| `beginLoading()` | 공통 로딩 오버레이를 열고 종료 동작을 돌려줘요. |
| `dispose()` | 로그아웃 때 애니메이션을 정리해요. |

## 동작 흐름

사용자 요청 또는 다른 클래스의 호출로 시작해요. 자세한 분기는 위 함수표와 원본 코드를 함께 봐요.

```mermaid
flowchart LR
 A[요청 또는 입력] --> B["show()"]
 B --> C[처리 결과 또는 화면 반영]
```

## 사용하는 라이브러리

| 도구 | 이 코드에서 필요한 이유 |
|---|---|
| JavaFX | 이벤트·화면·백그라운드 작업 |
| Java 컬렉션·함수 | 목록 복사·조건 처리·전달할 동작 |

라이브러리가 쓰이는 구체적인 위치는 위 함수표와 원본의 해당 함수 본문에서 확인해요. 화면 작업과 DB 작업을 같은 스레드에서 섞지 않는 것이 중요해요.

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]
