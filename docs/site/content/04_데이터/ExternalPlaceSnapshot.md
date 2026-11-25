---
title: ExternalPlaceSnapshot
source: src/main/java/com/datecourse/model/ExternalPlaceSnapshot.java
source_commit: b69d00e
source_sha256: 4a58c8aa0d3354cdb71f206afc7e07002c1834d0402d1a8dd3d469848471d96b
readability_updated: 2026-11-15
---

## 하는 일

외부 제공자에서 읽은 평점·평가 수·가격·영업 구간을 일시적으로 다루는 객체예요.

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `ExternalPlaceSnapshot` | 검토된 외부 장소 연결에 대해 제공자 상세를 구성해요. |

## 함께 확인할 점

영구 Place 및 저장 코스에 넣지 않아요. JSON 저장 방지와 가려진 문자열 표현을 제공하지만 다른 객체로 값을 복사하는 모든 행위를 통제하는 보안 경계는 아니에요. 미확인을 0점·폐점으로 단정하지 않아요.

[[현재_구현_흐름|사용 흐름]] · [[99_이전_문서/이전_안내/최종_검토_2026_11_15|검토 범위]]
