---
title: LoginUI
source: src/main/java/kr/ac/university/localdb/ui/LoginUI.java
source_commit: 9e59289
source_sha256: a7947498cb78daf10f4eea17c90aeeee94d60c1b2a57afda9c5b910d569cb5b7
reviewed_commit: 9e59289
reviewed_at: 2026-11-23
---

# LoginUI

Swing 로그인·회원가입 화면의 글꼴·색·입력칸과 비밀번호 표시를 통일해요. 현재는 제거된 `UiTheme` 대신 이 클래스를 사용해요. JavaFX 화면의 `style.css`와는 별도예요.

| 구성 | 역할 |
|---|---|
| `install()` | Swing 기본 글꼴과 배경을 설정해요. |
| `field()` | 입력칸의 색·테두리·포커스 표시를 맞춰요. |
| `TextField` | 공통 스타일을 적용한 일반 입력칸이에요. |
| `PasswordField` | 비밀번호 입력과 표시 전환을 제공해요. |

실제 인증·비밀번호 검증은 회원 처리 코드의 책임이에요. 이 클래스는 인증 권한을 판단하지 않아요.
