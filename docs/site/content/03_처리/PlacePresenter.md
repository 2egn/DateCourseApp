---
title: PlacePresenter
source: src/main/java/com/datecourse/service/PlacePresenter.java
source_commit: b69d00e
source_sha256: 70a40faa272883ccaa0e45ca036c7f9e406036ad9292f09b3e0bb57aa548557e
readability_updated: 2026-11-15
---

## 하는 일

장소 한 곳을 사용자에게 어떻게 표현할지 모아요. 지도와 후보 목록이 가격·업종·태그를 제각각 해석하지 않도록 해요.

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `present()` | 장소 이름·종류·가격 설명·근거·태그를 화면용 값으로 바꿔요. |

## 함께 확인할 점

표시 문자열을 추천 판정의 원본으로 사용하지 않아요. 조건 검사는 도메인 모델로 수행해요.

[[현재_구현_흐름|사용 흐름]] · [[최종_검토_2026_11_15|검토 범위]]
