---
aliases:
  - "04_데이터/PlaceDetails"
document_status: archived
archived_at: 2026-11-26
title: "보관 · PlaceDetails"
source: src/main/java/com/datecourse/model/PlaceDetails.java
source_commit: ea2242b
source_sha256: ebd34a4e2a87bed374df6cc3b3270f2cc783a8ce54665d93d55f0179dbe99896
readability_updated: 2026-11-14
reviewed_commit: b69d00e
reviewed_at: 2026-11-15
---

> **보관 문서 · 현재 구현 안내가 아닙니다.** 현재 소스와 다른 버전의 코드 발췌입니다. 원래 source_commit·source_sha256과 발췌는 당시 근거로 보존합니다. 현재 설명은 [[기능별_구현/목차|기능별 구현 문서]], 보관 사유는 [[99_이전_문서/목차|이전 문서 목록]]에서 확인하세요.


# PlaceDetails

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 최신 변경 · 2026-11-15

공공 태그와 필드 근거, 외부 장소 연결을 구분해요. UNKNOWN은 불가능이 아니라 미확인이에요. 이름·주소 정정으로 동일성 근거가 달라지면 외부 연결을 재검토 상태로 바꿔요.

[[현재_구현_흐름|최신 사용 흐름]] · [[99_이전_문서/이전_안내/최종_검토_2026_11_15|검증 범위]]

## 이전 버전의 상세 설명

아래 함수표·발췌·원본 링크는 기존 source_commit 기준 기록이에요. 위 최신 변경과 함께 읽어 주세요.

## 한눈에 보기

주소·세부 분류·연락처·출처·확인일·외부 장소 ID를 묶는 record예요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/com/datecourse/model/PlaceDetails.java)에서 확인해요.

```java
public record PlaceDetails(String address, String subcategory, String phone, String website,
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
