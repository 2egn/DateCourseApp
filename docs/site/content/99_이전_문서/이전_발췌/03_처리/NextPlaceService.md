---
aliases:
  - "03_처리/NextPlaceService"
document_status: archived
archived_at: 2026-11-26
title: "보관 · NextPlaceService"
source: src/main/java/com/datecourse/service/NextPlaceService.java
source_commit: ea2242b
source_sha256: 1e013ddb325051be362ce7b3d5362afd65c004eab97f1e6361b93c0259a9b487
readability_updated: 2026-11-14
reviewed_commit: b69d00e
reviewed_at: 2026-11-15
---

> **보관 문서 · 현재 구현 안내가 아닙니다.** 현재 소스와 다른 버전의 코드 발췌입니다. 원래 source_commit·source_sha256과 발췌는 당시 근거로 보존합니다. 현재 설명은 [[기능별_구현/목차|기능별 구현 문서]], 보관 사유는 [[99_이전_문서/목차|이전 문서 목록]]에서 확인하세요.


# NextPlaceService

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 최신 변경 · 2026-11-15

후보 표시 수 5·10·15·20·30과 업종·상세 종류·태그·가격 필터를 지원해요. 전체 원과 다음 장소 원의 교집합, 중복 제외, 가격 조건을 함께 적용해요. 도보 경로로 후보 전체를 비교하는 기능은 별도예요.

[[현재_구현_흐름|최신 사용 흐름]] · [[99_이전_문서/이전_안내/최종_검토_2026_11_15|검증 범위]]

## 이전 버전의 상세 설명

아래 함수표·발췌·원본 링크는 기존 source_commit 기준 기록이에요. 위 최신 변경과 함께 읽어 주세요.

## 한눈에 보기

전체 원과 이전 장소 원의 교집합 안에서 중복 없는 후보를 최대 5곳 골라요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/com/datecourse/service/NextPlaceService.java)에서 확인해요.

```java
public final class NextPlaceService {
```

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `candidates()` | 거리·활동·가격 조건을 검사해요. 미확인 가격은 포함 옵션이 켜졌을 때만 허용해요. |

## 동작 흐름

사용자 요청 또는 다른 클래스의 호출로 시작해요. 자세한 분기는 위 함수표와 원본 코드를 함께 봐요.

```mermaid
flowchart LR
 A[요청 또는 입력] --> B["candidates()"]
 B --> C[처리 결과 또는 화면 반영]
```

## 사용하는 라이브러리

| 도구 | 이 코드에서 필요한 이유 |
|---|---|
| Java 컬렉션·함수 | 목록 복사·조건 처리·전달할 동작 |

라이브러리가 쓰이는 구체적인 위치는 위 함수표와 원본의 해당 함수 본문에서 확인해요. 화면 작업과 DB 작업을 같은 스레드에서 섞지 않는 것이 중요해요.

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]
