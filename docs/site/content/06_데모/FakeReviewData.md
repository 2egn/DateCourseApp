---
readability_updated: 2026-11-13
project: DateCourseApp
class: FakeReviewData
package: com.datecourse.demo
status: 예제 데이터
reviewed: 2026-11-11
source: src/main/java/com/datecourse/demo/FakeReviewData.java
source_sha256: 3be8275f474a29a278183c08613e412f0db97907c298e579aea3f33972c9435b
tags:
  - Java
  - DateCourseApp
  - 개발노트
title: FakeReviewData
---

> [!info] 개발 중인 코드
> 2026-11-11 작업본 기준이다. 확정 설계가 아니며, 코드가 바뀌면 이 노트도 새 내용으로 바꿔요.

## 한눈에 보기

화면을 먼저 만들어 볼 수 있도록 후기 예시 자료를 준비하는 도구예요. 아직 실제 저장소를 연결하지 않은 화면에서도 목록과 버튼을 확인할 수 있게 해줘요.

- **현재 위치:** 예제 데이터
- **원본:** `src/main/java/com/datecourse/demo/FakeReviewData.java`
- **전체 안내:** [[전체_동작_흐름|전체_동작_흐름]] · [[목차|목차]]

## 사용 장면과 읽는 순서

목록이 비어 있으면 선택·정렬·상세 화면을 확인하기 어려우므로, 개발 중에는 예시 후기 자료를 넣어 화면 동작을 살펴봐요.

1. `getReviews()`가 실행 중인 프로그램 안에 예시 목록을 만들어요.
2. 호출한 예제 화면이 그 목록을 받아 보여줘요.
3. 실제 후기를 읽지 않아요. EMPTY_TEST로 목록이 비었을 때의 화면도 확인할 수 있어요.

모르는 용어는 [[문서_읽는법|문서 읽는 법과 용어 설명]]에서 찾아볼 수 있어요.

## 동작 순서

```text
getReviews() → EMPTY_TEST 확인 → FakeCourseData 코스 → Review 목록
```

## 주요 메서드

| 메서드 | 하는 일 |
|---|---|
| `getReviews()` | 메모리에 예제 목록을 만들어 돌려줘요. |

## 사용한 Java 라이브러리·API

아래 표는 가져다 쓴 도구, 이 클래스에서 만나는 함수, 사용하는 이유를 연결해 보여줘요. JDK 기본은 Java에 포함된 기능이고, 별도 라이브러리는 프로젝트가 추가로 준비하는 도구예요. 처음에는 도구 이름을 외우기보다 마지막 열의 역할부터 읽어 보세요.

| 라이브러리·API | 사용 메서드/위치 | 여기서 하는 일 |
|---|---|---|
| 목록·대응표<br/>JDK 기본: java.util<br/>List, ArrayList | getReviews() | 장소 목록·ID별 반경·정렬·중복 제거·문자열 설정을 관리한다. stream/filter/map은 목록을 고르고 변환하는 데 사용해요. |
| 날짜<br/>JDK 기본: java.time<br/>LocalDate | getReviews() | 예제 코스·리뷰 날짜와 기본 코스 이름의 날짜를 만들어요. |

외부 라이브러리 버전과 전체 사용 위치: [[라이브러리_사용_안내|라이브러리 사용 안내]]

## 클래스 내부 함수 흐름도

```mermaid
flowchart TD
 N0["getReviews()<br/>예제 데이터 생성"]
 N1["목록 반환<br/>화면 테스트에서 사용·DB 조회 아님"]
 N0 --> N1
```

## 핵심 코드

원본 16행 부근의 일부 발췌다. 아래 코드만 따로 복사해 실행하는 예제는 아니다.

```java
public static List<Review> getReviews() {
    List<Review> list = new ArrayList<>();
    if (EMPTY_TEST) return list;

    List<Course> courses = FakeCourseData.getSavedCourses();
    Course c1 = courses.get(0);
    Course c2 = courses.get(1);
    Course c3 = courses.get(2);

    list.add(new Review("r1", "민지", c1, 5,
            "산책하고 카페까지 동선이 정말 좋았어요. 예산도 딱 맞았어요!", LocalDate.of(2026, 10, 8)));
    list.add(new Review("r2", "준호", c2, 4,
            "파스타집이 맛있었는데 주말엔 웨이팅이 좀 길어요.", LocalDate.of(2026, 10, 6)));
    list.add(new Review("r3", "서연", c3, 3,
            "영화관에서 카페까지 걸어서 이동이 조금 멀었어요. 그래도 무난했어요.", LocalDate.of(2026, 10, 3)));
    list.add(new Review("r4", "하늘", c1, 5,
            "전시관이 생각보다 알차서 만족했어요. 부모님과 가도 좋을 듯해요.", LocalDate.of(2026, 9, 29)));
    list.add(new Review("r5", "도윤", c2, 2,
// ... 이후 코드는 원본 참고
```

## 변경할 때 주의할 점

EMPTY_TEST는 빈 목록 상태를 확인하는 스위치다. 실제 리뷰 조회가 아니다.

## 함께 읽기

[[04_데이터/Course|Course]] · [[06_데모/FakeCourseData|FakeCourseData]] · [[04_데이터/Review|Review]]
