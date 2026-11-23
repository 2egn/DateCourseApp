---
title: SavedCourseController
source: src/main/java/com/datecourse/controller/SavedCourseController.java
source_commit: ea2242b
source_sha256: bb25f532492d2e91244aa3b5389ed547ff641ea08967090fed710ee6ae47098e
readability_updated: 2026-11-14
reviewed_commit: 9e59289
reviewed_at: 2026-11-23
---

## 현재 구현 · 2026-11-23

저장 PNG를 먼저 보여주고 없으면 지도·직선 연결, 지도 실패 시 로컬 연결도로 대체해요. 코스 선택만으로 경로 API를 호출하지 않아요.

| 주요 함수·구성 | 역할 |
|---|---|
| `initialize()` | 목록·지도·정보 표·메시지를 기능별로 준비해요. |
| `refreshMapNotice()` | 버튼·이미지·안내 상태를 조율해요. |
| `updatePreviewControls()` | 현재 화면에 맞는 지도·조회 버튼을 갱신해요. |
| `buildPreviewCaption() / buildMapNotice()` | 중첩 조건 대신 현재 필요한 문구를 골라요. |
| `onRequestRoute()` | 사용자가 누를 때만 실제 경로를 조회해요. |

앱 코드 기준 `9e59289`. [[현재_구현_흐름|현재 흐름]] · [[변경_요약|변경 요약]]

<details>
<summary>이전 기준의 발췌와 학습 설명</summary>

# SavedCourseController

> 개발 중인 코드예요. `ea2242b` 기준이며, 후속 커밋에서 동작이 바뀔 수 있어요.

## 최신 변경 · 2026-11-15

로그인별 저장 목록 캐시를 재사용하고 변경 후 갱신해요. 요약·확대된 이미지·방문 순서를 보여주며 이미지 조회 실패와 이미지 없음의 재시도를 구분해요. 미리보기만 재생성할 때 코스는 새로 저장하지 않아요.

[[현재_구현_흐름|최신 사용 흐름]] · [[최종_검토_2026_11_15|검증 범위]]

## 이전 버전의 상세 설명

아래 함수표·발췌·원본 링크는 기존 source_commit 기준 기록이에요. 위 최신 변경과 함께 읽어 주세요.

## 한눈에 보기

예제 데이터 대신 로그인 회원이 DB에 저장한 코스를 보여줘요. 최근 수정 시각은 한국 시간이에요.

## 코드 읽기

아래 선언부터 읽고 함수표의 순서로 찾아보세요. 전체 코드는 [기준 커밋의 원본](https://github.com/2egn/DateCourseApp/blob/ea2242b/src/main/java/com/datecourse/controller/SavedCourseController.java)에서 확인해요.

```java
public class SavedCourseController extends BasePageController {
```

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `configure()` | 저장소와 현재 회원, 불러오기 동작을 연결해요. |
| `onRefreshClick()` | 실제 DB 목록을 다시 읽어요. |
| `showDetail()` | 장소 순서·인원·가격 상태·수정 시각을 보여줘요. |
| `onOpenClick()` | 선택한 저장 코스를 홈에 전달해요. |
| `onRenameClick()` | 이름을 바꾸고 목록을 갱신해요. |
| `onDeleteClick()` | 확인 후 DB 코스를 삭제해요. |

## 동작 흐름

사용자 요청 또는 다른 클래스의 호출로 시작해요. 자세한 분기는 위 함수표와 원본 코드를 함께 봐요.

```mermaid
flowchart LR
 A[요청 또는 입력] --> B["configure()"]
 B --> C[처리 결과 또는 화면 반영]
```

## 사용하는 라이브러리

| 도구 | 이 코드에서 필요한 이유 |
|---|---|
| JavaFX | 이벤트·화면·백그라운드 작업 |
| Java 컬렉션·함수 | 목록 복사·조건 처리·전달할 동작 |
| Java 시간 | 수정 시각 보관·표시 |

라이브러리가 쓰이는 구체적인 위치는 위 함수표와 원본의 해당 함수 본문에서 확인해요. 화면 작업과 DB 작업을 같은 스레드에서 섞지 않는 것이 중요해요.

관련 안내: [[현재_구현_흐름|현재 전체 흐름]] · [[업데이트_내역|이번 변경]]


</details>
