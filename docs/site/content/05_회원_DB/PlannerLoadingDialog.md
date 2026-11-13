---
title: PlannerLoadingDialog
source: src/main/java/kr/ac/university/localdb/ui/PlannerLoadingDialog.java
source_commit: ea2242b
source_sha256: 6d886f43206e9fd6d4b9ecbce410fc74a509618eb3b3a79c3ce5fc8de92b82c1
readability_updated: 2026-11-14
---

# PlannerLoadingDialog

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 한눈에 보기

로그인 후 첫 화면과 장소 목록을 준비하는 동안 보여주는 Swing 로딩창이에요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/kr/ac/university/localdb/ui/PlannerLoadingDialog.java)에서 확인해요.

```java
final class PlannerLoadingDialog extends JDialog implements AutoCloseable {
```

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `open()` | 소유 창을 잠그고 로딩을 보여줘요. |
| `close()` | 로딩을 닫고 소유 창을 다시 켜요. |

## 동작 흐름

사용자 요청 또는 다른 클래스의 호출로 시작해요. 자세한 분기는 위 함수표와 원본 코드를 함께 봐요.

```mermaid
flowchart LR
 A[요청 또는 입력] --> B["open()"]
 B --> C[처리 결과 또는 화면 반영]
```

## 사용하는 라이브러리

| 도구 | 이 코드에서 필요한 이유 |
|---|---|
| Swing | 로그인 UI·화면 이벤트 |

라이브러리가 쓰이는 구체적인 위치는 위 함수표와 원본의 해당 함수 본문에서 확인해요. 화면 작업과 DB 작업을 같은 스레드에서 섞지 않는 것이 중요해요.

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]
