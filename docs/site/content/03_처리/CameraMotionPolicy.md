---
title: CameraMotionPolicy
source: src/main/java/com/datecourse/service/CameraMotionPolicy.java
source_commit: b69d00e
source_sha256: ba18e99ef79cd6924eb39f9ebcbd9e494078cacedc9b0719095d1799a69f7e2f
readability_updated: 2026-11-15
---

## 하는 일

Java에서 화면 기준 거리·배율·이동 의도로 지도 이동 방법을 결정해요.

## 주요 함수

| 함수 | 하는 일 |
|---|---|
| `decide()` | 가까운 이동·범위 드래그·반경 확정·전체 보기·초기화를 공통 규칙으로 판정해요. |

## 함께 확인할 점

SDK 좌표 측정과 실제 이동은 camera-motion.js가 맡아요. 특정 장소 좌표별 예외나 강제 지연 재중심을 추가하지 않아요.

[[현재_구현_흐름|사용 흐름]] · [[최종_검토_2026_11_15|검토 범위]]
