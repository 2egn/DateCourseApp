---
readability_updated: 2026-11-13
project: DateCourseApp
class: PlaceCalculationServiceTest
package: com.datecourse.service
status: 자동 테스트
reviewed: 2026-11-11
source: src/test/java/com/datecourse/service/PlaceCalculationServiceTest.java
source_sha256: 7062ef4f20c09c2fb0415b41c947f4a6ef254cca8abe072a72ada335cf76e890
tags:
  - Java
  - DateCourseApp
  - 개발노트
title: PlaceCalculationServiceTest
---

> [!info] 개발 중인 코드
> 2026-11-11 작업본 기준이다. 확정 설계가 아니며, 코드가 바뀌면 이 노트도 새 내용으로 바꿔요.

## 한눈에 보기

거리의 단위와 허용 범위, 인원별 금액 계산을 확인하는 검사예요.

- **현재 위치:** 자동 테스트
- **원본:** `src/test/java/com/datecourse/service/PlaceCalculationServiceTest.java`
- **전체 안내:** [[전체_동작_흐름|전체_동작_흐름]] · [[목차|목차]]

미터 단위, 경계, 잘못된 좌표·반경·인원, 금액 범위를 확인해요.

## 사용 장면과 읽는 순서

거리 300이 미터인지 다른 단위인지, 허용 거리와 정확히 같은 위치를 포함하는지, 잘못된 인원·좌표를 거부하는지 살펴봐요.

1. 테스트가 필요한 입력과 시험용 환경을 먼저 준비해요. 실제 서비스 대신 예상한 응답을 주는 대체 코드를 쓰는 경우도 있어요.
2. 확인하려는 클래스의 함수를 호출하고 결과를 받아요.
3. assertEquals·assertTrue 같은 확인 문장으로 예상 결과와 실제 결과를 비교해요. 각각의 테스트 함수는 독립적인 검사예요.
4. 직선 거리 계산을 검사해요. 실제 길을 따라 걷는 거리를 요청하는 TMAP 시험과는 달라요.

모르는 용어는 [[문서_읽는법|문서 읽는 법과 용어 설명]]에서 찾아볼 수 있어요.

## 동작 순서

```text
JUnit이 @Test 메서드 실행 → 테스트용 입력/대체 객체 준비 → 대상 코드 호출 → assert로 기대 결과 확인
```

## 주요 메서드

| 메서드 | 하는 일 |
|---|---|
| `distanceUsesMeters()` | 거리 결과의 단위가 미터인지 확인해요. |
| `filtersRepositoryPlacesUsingScreenRadiusInMeters()` | 화면과 같은 미터 반경으로 장소를 고르는지 확인해요. |
| `zeroRadiusDoesNotDisableFiltering()` | 반경 0이 무제한 검색으로 바뀌지 않는지 확인해요. |
| `rejectsInvalidRadiusAndCoordinates()` | 잘못된 반경과 좌표를 거부하는지 확인해요. |

## 사용한 Java 라이브러리·API

아래 표는 가져다 쓴 도구, 이 클래스에서 만나는 함수, 사용하는 이유를 연결해 보여줘요. JDK 기본은 Java에 포함된 기능이고, 별도 라이브러리는 프로젝트가 추가로 준비하는 도구예요. 처음에는 도구 이름을 외우기보다 마지막 열의 역할부터 읽어 보세요.

| 라이브러리·API | 사용 메서드/위치 | 여기서 하는 일 |
|---|---|---|
| 목록·대응표<br/>JDK 기본: java.util<br/>List | filtersRepositoryPlacesUsingScreenRadiusInMeters(), zeroRadiusDoesNotDisableFiltering(), rejectsInvalidRadiusAndCoordinates() | 장소 목록·ID별 반경·정렬·중복 제거·문자열 설정을 관리한다. stream/filter/map은 목록을 고르고 변환하는 데 사용해요. |
| JUnit 자동 검사<br/>외부: JUnit Jupiter 5.10.0<br/>Test, Assertions, assertEquals/assertTrue 등 | distanceUsesMeters(), filtersRepositoryPlacesUsingScreenRadiusInMeters(), zeroRadiusDoesNotDisableFiltering(), rejectsInvalidRadiusAndCoordinates(), calculatesTotalForAllPeople(), rejectsNonPositivePeople() 외 1개 | @Test로 검사 메서드를 지정하고 assertEquals/assertTrue 등으로 기대값을 확인해요. |

외부 라이브러리 버전과 전체 사용 위치: [[라이브러리_사용_안내|라이브러리 사용 안내]]

## 클래스 내부 함수 흐름도

```mermaid
flowchart TD
 N0["JUnit<br/>@Test 메서드 실행"]
 N1["준비<br/>입력·대체 객체 생성"]
 N0 --> N1
 N2["대상 코드 호출<br/>검사할 클래스의 메서드 실행"]
 N1 --> N2
 N3["assert<br/>기대값과 실제 결과 비교"]
 N2 --> N3
 N4["결과<br/>다르면 실패·같으면 통과"]
 N3 --> N4
```

개별 테스트는 각각 독립적으로 이 순서를 따른다. @Test 메서드끼리 순서대로 호출하는 구조는 아니다.

## 핵심 코드

원본 12행 부근의 실제 코드다.

```java
void distanceUsesMeters() {
    assertEquals(0, service.calculateDistance(35.1, 129.1, 35.1, 129.1), 0.001);
    assertEquals(1111.95, service.calculateDistance(0, 0, 0.01, 0), 0.1);
}
```

## 변경할 때 주의할 점

직선 거리 계산의 테스트이지 길찾기 API 테스트가 아니다.

## 함께 읽기

[[04_데이터/Place|Place]] · [[03_처리/PlaceCalculationService|PlaceCalculationService]]

## 나머지 검사 항목

메서드 이름은 원본 그대로 적었다. 세부 입력과 기대값은 해당 메서드의 `assert`를 확인해요.

- `calculatesTotalForAllPeople()`
- `rejectsNonPositivePeople()`
- `excessiveCostDoesNotWrapIntoANegativeBudget()`
