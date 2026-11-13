---
title: UiTheme
source: src/main/java/kr/ac/university/localdb/ui/UiTheme.java
source_commit: ea2242b
source_sha256: fe69c4e93f6167dcfb85578bcf4ac23592be89beb3d620951c7ee60f6f0ed2c3
readability_updated: 2026-11-14
---

# UiTheme

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 한눈에 보기

로그인·가입의 Swing 색상·입력 필드·버튼과 Caps Lock 안내 배치를 관리해요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/kr/ac/university/localdb/ui/UiTheme.java)에서 확인해요.

```java
public final class UiTheme {
```

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `loginPanel()` | 아이디·비밀번호·안내·로그인 버튼을 배치해요. |

## 동작 흐름

사용자 요청 또는 다른 클래스의 호출로 시작해요. 자세한 분기는 위 함수표와 원본 코드를 함께 봐요.

```mermaid
flowchart LR
 A[요청 또는 입력] --> B["loginPanel()"]
 B --> C[처리 결과 또는 화면 반영]
```

## 사용하는 라이브러리

| 도구 | 이 코드에서 필요한 이유 |
|---|---|
| Swing | 로그인 UI·화면 이벤트 |

라이브러리가 쓰이는 구체적인 위치는 위 함수표와 원본의 해당 함수 본문에서 확인해요. 화면 작업과 DB 작업을 같은 스레드에서 섞지 않는 것이 중요해요.

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]
