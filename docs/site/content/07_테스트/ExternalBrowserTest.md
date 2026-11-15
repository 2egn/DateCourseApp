---
readability_updated: 2026-11-13
project: DateCourseApp
class: ExternalBrowserTest
package: com.datecourse.service
status: 자동 테스트
reviewed: 2026-11-11
source: src/test/java/com/datecourse/service/ExternalBrowserTest.java
source_sha256: f4016437fba42b7fb533a759ff2d908eaa0bd838baa235b7daae56ae03d4e906
tags:
  - Java
  - DateCourseApp
  - 개발노트
title: ExternalBrowserTest
---

> [!info] 개발 중인 코드
> 2026-11-11 작업본 기준이다. 확정 설계가 아니며, 코드가 바뀌면 이 노트도 새 내용으로 바꿔요.

## 한눈에 보기

브라우저를 열기 위한 실행 항목이 올바르게 만들어지는지 확인하는 검사예요.

- **현재 위치:** 자동 테스트
- **원본:** `src/test/java/com/datecourse/service/ExternalBrowserTest.java`
- **전체 안내:** [[전체_동작_흐름|전체_동작_흐름]] · [[목차|목차]]

프로필 인자, 브라우저 종류, 비공개 옵션과 주소 검사를 확인해요.

## 사용 장면과 읽는 순서

상세 링크의 글자가 운영체제 명령처럼 실행되지 않고 주소라는 한 항목으로 전달되는지, 비공개 창 옵션이 맞는지 확인해요.

1. 테스트가 필요한 입력과 시험용 환경을 먼저 준비해요. 실제 서비스 대신 예상한 응답을 주는 대체 코드를 쓰는 경우도 있어요.
2. 확인하려는 클래스의 함수를 호출하고 결과를 받아요.
3. assertEquals·assertTrue 같은 확인 문장으로 예상 결과와 실제 결과를 비교해요. 각각의 테스트 함수는 독립적인 검사예요.
4. 실제 사용자 브라우저를 매번 여는 시험은 아니에요. 명령 구성과 주소 검사 결과를 살펴봐요.

모르는 용어는 [[문서_읽는법|문서 읽는 법과 용어 설명]]에서 찾아볼 수 있어요.

## 동작 순서

```text
JUnit이 @Test 메서드 실행 → 테스트용 입력/대체 객체 준비 → 대상 코드 호출 → assert로 기대 결과 확인
```

## 주요 메서드

| 메서드 | 하는 일 |
|---|---|
| `privateWindowsUseTheBrowsersPrivateMode()` | 비공개 창에 맞는 브라우저 옵션을 만드는지 확인해요. |
| `detectsPrivateTitlesAndLaunchFlagsWithoutCopyingOtherArguments()` | 창 제목·인자로 비공개 여부를 판별하고 불필요 인자는 복사하지 않는지 확인해요. |
| `chromiumReusesExistingProfileAndKeepsUrlOutOfShellCode()` | Chromium 프로필을 재사용하고 URL을 셸 코드로 합치지 않는지 확인해요. |
| `firefoxExplicitlyRequestsNewTab()` | Firefox에 새 탭 옵션을 전달하는지 확인해요. |

## 사용한 Java 라이브러리·API

아래 표는 가져다 쓴 도구, 이 클래스에서 만나는 함수, 사용하는 이유를 연결해 보여줘요. JDK 기본은 Java에 포함된 기능이고, 별도 라이브러리는 프로젝트가 추가로 준비하는 도구예요. 처음에는 도구 이름을 외우기보다 마지막 열의 역할부터 읽어 보세요.

| 라이브러리·API | 사용 메서드/위치 | 여기서 하는 일 |
|---|---|---|
| 목록·대응표<br/>JDK 기본: java.util<br/>List, Map | privateWindowsUseTheBrowsersPrivateMode(), chromiumReusesExistingProfileAndKeepsUrlOutOfShellCode(), firefoxExplicitlyRequestsNewTab(), onlyBrowserProfileSwitchesAreCarriedOver(), rejectsNonWebAddressesBeforeStartingAProcess() | 장소 목록·ID별 반경·정렬·중복 제거·문자열 설정을 관리한다. stream/filter/map은 목록을 고르고 변환하는 데 사용해요. |
| 파일·문자 입출력<br/>JDK 기본: java.io / java.nio<br/>Path | privateWindowsUseTheBrowsersPrivateMode(), chromiumReusesExistingProfileAndKeepsUrlOutOfShellCode(), firefoxExplicitlyRequestsNewTab(), rejectsNonWebAddressesBeforeStartingAProcess() | 설정·아이디 기록·SQL 파일을 읽거나 저장해요. 테스트에서는 화면 이미지를 파일로 남긴다. |
| 주소·외부 프로세스<br/>JDK 기본: java.net / java.lang<br/>URI | privateWindowsUseTheBrowsersPrivateMode(), chromiumReusesExistingProfileAndKeepsUrlOutOfShellCode(), firefoxExplicitlyRequestsNewTab(), rejectsNonWebAddressesBeforeStartingAProcess() | 웹 주소를 검사하거나 브라우저 실행 프로세스를 시작·조회한다. |
| JUnit 자동 검사<br/>외부: JUnit Jupiter 5.10.0<br/>Test, Assertions, assertEquals/assertTrue 등 | privateWindowsUseTheBrowsersPrivateMode(), detectsPrivateTitlesAndLaunchFlagsWithoutCopyingOtherArguments(), chromiumReusesExistingProfileAndKeepsUrlOutOfShellCode(), firefoxExplicitlyRequestsNewTab(), onlyBrowserProfileSwitchesAreCarriedOver(), rejectsNonWebAddressesBeforeStartingAProcess() | @Test로 검사 메서드를 지정하고 assertEquals/assertTrue 등으로 기대값을 확인해요. |

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

원본 10행 부근의 실제 코드다.

```java
@Test void privateWindowsUseTheBrowsersPrivateMode() {
    for (var entry : java.util.Map.of("chrome.exe", "--incognito", "brave.exe", "--incognito",
            "msedge.exe", "--inprivate", "firefox.exe", "--private-window", "opera.exe", "--private").entrySet()) {
        var target = new ExternalBrowser.BrowserTarget(Path.of(entry.getKey()), List.of("--profile-directory=Default"), true);
        assertEquals(List.of(entry.getKey(), "--profile-directory=Default", entry.getValue(), "https://map.kakao.com/"),
                ExternalBrowser.command(target, URI.create("https://map.kakao.com/")));
    }
}
```

## 변경할 때 주의할 점

실제 사용자의 브라우저 창을 자동으로 여는 시험과 구분해요.

## 함께 읽기

[[03_처리/ExternalBrowser|ExternalBrowser]]

## 나머지 검사 항목

메서드 이름은 원본 그대로 적었다. 세부 입력과 기대값은 해당 메서드의 `assert`를 확인해요.

- `onlyBrowserProfileSwitchesAreCarriedOver()`
- `rejectsNonWebAddressesBeforeStartingAProcess()`
