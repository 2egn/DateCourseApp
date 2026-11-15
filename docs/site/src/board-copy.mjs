// One-time, reviewed wording update. Expected versions/text protect concurrent team edits.
export const COPY_VERSION='plain-language-2026-11-13-v1';
export const TASK_COPY=[
  {
    "id": "T-AAE5B56F",
    "expectedVersion": 1,
    "before": {
      "title": "미채택 줌 실험 코드 정리",
      "description": "[d32bc76 리뷰 / 신규] 채택하지 않은 WebView 전체 배율·줌 실험 경로를 현재 실행 코드에서 정리하고 Git 이력으로 남긴다.",
      "acceptance": "채택한 카카오 SDK 방식 유지, 두 지도·마커·상세창 회귀 확인",
      "dependencies": ""
    },
    "after": {
      "title": "사용하지 않는 지도 확대·축소 실험 코드 정리하기",
      "description": "현재 사용하지 않는 지도 확대·축소 코드를 정리해, 실제 실행되는 코드를 찾기 쉽게 만들어요.\n개발 참고: 앱 안의 웹 화면(WebView) 전체 크기를 바꾸던 실험은 Git의 변경 이력에 남기고, 현재 카카오 지도 방식은 유지해요.",
      "acceptance": "정리 후에도 메인·편집 지도의 확대·축소, 장소 표시, 상세정보가 이전처럼 작동해요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-75B6DB05",
    "expectedVersion": 1,
    "before": {
      "title": "대용량 원본·중복 산출물 관리 정리",
      "description": "[d32bc76 리뷰 / 신규] 약 206MB 데이터의 원본·CSV·SQL·엑셀 중복을 점검한다. 생성 코드·명세·작은 표본과 큰 원본·산출물을 구분해 관리한다.",
      "acceptance": "원본 출처·재생성 경로 유지, 불필요한 중복과 저장소 부담 감소",
      "dependencies": "공공데이터 생성 스크립트·검증 절차 복구"
    },
    "after": {
      "title": "크고 중복된 장소 자료 정리하기",
      "description": "같은 장소 자료가 여러 파일로 중복 저장되어 있는지 확인하고, 필요한 파일만 관리해요.\n개발 참고: 약 206MB의 원본·CSV·SQL·엑셀을 구분하고, 원본을 다시 가공하는 코드와 작은 예시 자료는 함께 남겨요.",
      "acceptance": "원본 출처와 자료를 다시 만드는 방법이 남아 있고, 불필요한 중복 파일이 줄어들어요.",
      "dependencies": "T-B5F06FB3"
    }
  },
  {
    "id": "T-DD5B5813",
    "expectedVersion": 1,
    "before": {
      "title": "새 공공데이터 조회·추천 연결",
      "description": "[d32bc76 리뷰 / 신규] 자료는 추가됐지만 PlaceRepository는 기존 integrated_places.csv만 조회한다. 가격 모델·생성 절차를 정리한 뒤 조회·추천에 연결한다.",
      "acceptance": "실제 조회 출처와 후보 데이터 일치, 가격 미상·중복·범위 규칙 유지",
      "dependencies": "무료·가격 미상·확인 가격 모델 분리, 공공데이터 생성 스크립트·검증 절차 복구"
    },
    "after": {
      "title": "새로 모은 장소 자료를 실제 추천에 사용하기",
      "description": "추가해 둔 공공데이터의 장소들이 실제 검색과 추천 결과에도 나오도록 연결해요.\n개발 참고: 장소를 읽는 PlaceRepository는 현재 integrated_places.csv만 사용해요. 가격 구분과 자료 생성 방법을 먼저 정리해요.",
      "acceptance": "추천 결과가 실제로 읽은 장소 자료와 일치하고, 중복·거리·가격 조건을 지켜요.",
      "dependencies": "T-31FAD2F2, T-B5F06FB3"
    }
  },
  {
    "id": "T-31FAD2F2",
    "expectedVersion": 1,
    "before": {
      "title": "무료·가격 미상·확인 가격 모델 분리",
      "description": "[d32bc76 리뷰 / 신규] 가격 미상이 0원으로 변환되면 조건·총액이 잘못된다. 저장 흐름 완성 후 새 데이터 연결 전에 무료·미상·확인 가격과 1인 가격 기준을 구분한다.",
      "acceptance": "미상을 무료로 표시·합산하지 않음, 메뉴 분량·가격 출처 기준 명확",
      "dependencies": "핵심 저장·재조회 완료"
    },
    "after": {
      "title": "‘무료’와 ‘가격을 모름’을 구분하기",
      "description": "가격 정보가 없는 장소를 0원짜리 장소로 잘못 보여주거나 합산하지 않게 해요.\n무료·가격 미확인·확인된 가격을 따로 구분하고, 1인 가격과 메뉴 가격의 기준도 정해요. 코스 저장·다시 불러오기를 먼저 완성한 뒤 진행해요.",
      "acceptance": "가격 미확인이 무료로 표시되지 않고, 총금액 계산에서도 확인된 가격과 구분돼요.",
      "dependencies": "TEST-01"
    }
  },
  {
    "id": "T-B5F06FB3",
    "expectedVersion": 1,
    "before": {
      "title": "공공데이터 생성 스크립트·검증 절차 복구",
      "description": "[d32bc76 리뷰 / 신규] 데이터 README가 안내하는 tools/prepare_busan_public_data.py가 커밋에 없다. 생성 코드·입력 명세·실행·결과 검증을 함께 관리한다.",
      "acceptance": "같은 입력으로 재생성 가능, 건수·필드·좌표·출처 검증 방법 제공",
      "dependencies": ""
    },
    "after": {
      "title": "장소 자료를 다시 만들 수 있도록 준비하기",
      "description": "원본 장소 자료를 앱에서 쓸 수 있는 형태로 바꾸는 코드와 사용 방법을 함께 남겨요.\n개발 참고: 자료 안내에 적힌 tools/prepare_busan_public_data.py 파일이 검토한 커밋에 없어요. 입력 파일 형식과 실행 방법도 복구해야 해요.",
      "acceptance": "같은 원본으로 자료를 다시 만들 수 있고, 장소 수·필수 항목·지도 위치·출처를 확인하는 방법이 있어요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-4320E4E0",
    "expectedVersion": 1,
    "before": {
      "title": "도보 안내 표시·펼침 시 지도 크기 유지",
      "description": "[d32bc76 리뷰 / 신규] 도보 안내 표시·펼침 시 지도 높이가 605→548→494px로 줄어든다. 고정 패널 또는 우측 메뉴에 안내를 배치하고 내용만 스크롤한다.",
      "acceptance": "안내 표시·접힘·펼침 전후 두 지도의 크기와 선택 시점 유지",
      "dependencies": ""
    },
    "after": {
      "title": "도보 안내를 펼쳐도 지도 크기 유지하기",
      "description": "걷는 길 안내를 펼치거나 접어도 지도가 작아지거나 흔들리지 않게 해요.\n검토 당시에는 안내를 펼칠수록 지도 높이가 605→548→494px로 줄었어요. 안내 영역의 크기를 정하고 내용만 스크롤하도록 개선해요.",
      "acceptance": "메인·편집 화면 모두 안내를 펼치고 접어도 지도 크기와 보고 있던 위치가 유지돼요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-AC26320C",
    "expectedVersion": 1,
    "before": {
      "title": "도보 탐색 옵션·두 지도 경로 동기화",
      "description": "[d32bc76 리뷰 / 신규] 편집 화면에서 계단 제외 등 옵션을 바꾸면 메인에 이전 경로가 남는다. 장소 순서와 탐색 옵션을 함께 검사해 두 지도 경로를 무효화·재조회한다.",
      "acceptance": "메인·편집 모두 저장된 옵션과 표시 경로 일치, 늦은 이전 응답 차단",
      "dependencies": ""
    },
    "after": {
      "title": "걷는 길 조건을 바꾸면 두 지도에 함께 반영하기",
      "description": "편집 화면에서 ‘계단 제외’ 같은 조건을 바꾸면 메인 지도도 새 조건에 맞는 길을 보여주게 해요.\n개발 참고: 장소 순서와 도보 탐색 옵션이 바뀌면 이전 경로를 버리고 다시 요청해요. 늦게 도착한 이전 결과로 새 경로가 바뀌면 안 돼요.",
      "acceptance": "메인·편집 지도의 경로가 선택한 조건과 일치하고, 이전 요청 결과가 새 경로를 덮어쓰지 않아요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-3CB9F373",
    "expectedVersion": 2,
    "before": {
      "title": "새 PC clone부터 첫 실행까지 검증",
      "description": "[d32bc76 리뷰 / 실행 안내 갱신] 새 PC에서 JDK·DB·카카오 키 준비 후 내장 지도 서버 자동 시작→가입·로그인→추천·편집을 확인한다. 별도 Python 서버는 사용하지 않는다.",
      "acceptance": "저장소와 최신 안내만으로 정상 진입, 도보 선택 기능과 필수 실행 조건 구분",
      "dependencies": ""
    },
    "after": {
      "title": "처음 쓰는 PC에서 앱 실행까지 따라 해보기",
      "description": "프로젝트를 처음 내려받은 PC에서 안내 문서만 보고 가입·로그인·추천·편집까지 실행해 봐요.\n개발 참고: Java 실행·개발 도구(JDK), 데이터 저장소(DB), 카카오 지도 키를 준비해요. 지도 서버는 앱이 자동으로 켜며 별도 Python 서버는 쓰지 않아요.",
      "acceptance": "추가 구두 설명 없이 첫 화면까지 들어갈 수 있고, 필수 설정과 선택 기능인 도보 길찾기 설정이 구분되어 있어요.",
      "dependencies": ""
    }
  },
  {
    "id": "DB-01",
    "expectedVersion": 5,
    "before": {
      "title": "회원별 코스 저장 구조 설계",
      "description": "[d32bc76 리뷰 / 핵심 구현] RoutePlan 기준으로 회원·장소 순서·인원·전체 반경·장소별 이동 반경·이름을 저장하도록 설계한다. 이전 Course 모델의 가격 기준과 섞지 않는다.",
      "acceptance": "RoutePlan을 손실 없이 저장·복원할 수 있고 1인 가격과 인원 기준 일치",
      "dependencies": "팀 합의"
    },
    "after": {
      "title": "코스를 저장할 때 필요한 항목 정하기",
      "description": "회원별로 어떤 코스 정보를 저장할지 정해, 다시 열었을 때 그대로 복원할 수 있게 설계해요.\n장소 순서·인원·전체 허용 거리·장소별 이동 거리·코스 이름을 포함해요. 개발 참고: 현재 코스 정보 객체(RoutePlan)를 기준으로 하고, 이전 Course의 가격 기준과 혼동하지 않아요.",
      "acceptance": "저장한 코스를 다시 열면 필요한 정보가 빠지지 않고, 1인 가격과 인원 계산 기준도 일치해요.",
      "dependencies": "저장할 항목과 가격 기준에 대한 팀 합의"
    }
  },
  {
    "id": "T-8837FCB6",
    "expectedVersion": 2,
    "before": {
      "title": "TMAP 도보 조회 실패·늦은 응답 검증",
      "description": "[d32bc76 리뷰 / 구현 상태 반영] 도보 연동은 구현됨. 실제 키·카카오 환경에서 실패 안내·이전 응답 차단·코스 변경 무효화와 직선 거리 구분을 확인한다.",
      "acceptance": "도보 실패가 추천·편집 완료를 막지 않으며 오래된 응답이 새 코스를 덮지 않음",
      "dependencies": ""
    },
    "after": {
      "title": "도보 길찾기가 실패하거나 늦어질 때 확인하기",
      "description": "TMAP 도보 길찾기가 실패하거나 응답이 늦어도 코스를 추천·편집할 수 있는지 확인해요.\n도보 연결은 구현되어 있어요. 실제 지도 키로 사용해 보고, 코스를 바꾼 뒤 도착한 이전 결과가 남지 않는지 확인해요.",
      "acceptance": "길찾기 실패 안내가 보이고 편집을 마칠 수 있어요. 도보 거리와 직선 거리가 구분되고, 이전 결과가 새 코스를 덮어쓰지 않아요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-D148D21F",
    "expectedVersion": 2,
    "before": {
      "title": "내장 지도 서버·재시도 실환경 검증",
      "description": "[d32bc76 리뷰 / 구현 상태 반영] 내장 서버·재시도는 구현됨. 새 PC에서 자동 시작·두 지도 공유·포트 충돌·키 수정 후 재시도·앱 종료 정리를 확인한다.",
      "acceptance": "별도 Python 없이 실행, 실패 안내·재시도 정상, 코스·선택 위치 유지",
      "dependencies": ""
    },
    "after": {
      "title": "지도 자동 실행과 다시 연결을 실제 PC에서 확인하기",
      "description": "앱이 지도에 필요한 작은 서버를 자동으로 켜고, 연결 실패 후 다시 시도할 수 있는지 확인해요.\n이 기능은 구현되어 있어요. 두 지도가 서버를 함께 쓰는지, 다른 프로그램이 같은 연결 번호(포트)를 쓰는 경우와 지도 키를 수정한 경우도 점검해요.",
      "acceptance": "별도 Python 서버 없이 지도가 열리고, 다시 연결해도 코스·선택 위치가 유지돼요. 앱을 끄면 지도 서버도 정리돼요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-6EA7B7D4",
    "expectedVersion": 2,
    "before": {
      "title": "커뮤니티 목록·작성·상세 폼 연결",
      "description": "[d32bc76 리뷰 / 우선순위 변경] 핵심 저장·재조회 완료 후 진행. 미완성 커뮤니티 화면을 실제 로그인 세션·코스·리뷰 데이터에 연결하고 정렬·평점·등록 결과를 표시한다.",
      "acceptance": "정상 등록 후 목록·상세 갱신, 미선택·오입력·실패 안내 정상",
      "dependencies": "커뮤니티 실제 데이터 연결, 핵심 저장·재조회 완료"
    },
    "after": {
      "title": "커뮤니티에서 글 목록·작성·상세 화면 연결하기",
      "description": "실제 회원의 코스와 후기를 커뮤니티 화면에서 보고 작성할 수 있게 해요.\n기본 기능인 코스 저장·다시 불러오기를 먼저 완성한 뒤, 목록 정렬·평점·등록 결과를 화면에 보여줘요.",
      "acceptance": "후기를 등록하면 목록과 상세 내용에 반영되고, 입력이 부족하거나 등록이 실패하면 이유가 보여요.",
      "dependencies": "T-98DE32B6, TEST-01"
    }
  },
  {
    "id": "T-98DE32B6",
    "expectedVersion": 2,
    "before": {
      "title": "커뮤니티 실제 데이터 연결",
      "description": "[d32bc76 리뷰 / 우선순위 변경] 핵심 저장·재조회 완료 후 진행. 예제 리뷰를 로그인 회원·실제 코스와 연결하고 등록·목록·상세의 저장 흐름을 완성한다.",
      "acceptance": "앱 재실행 후 리뷰 유지, 작성자·코스·평점 일치",
      "dependencies": "DB-03, 핵심 저장·재조회 완료"
    },
    "after": {
      "title": "회원이 쓴 코스 후기를 저장하고 불러오기",
      "description": "예시 후기 대신 회원이 실제로 작성한 코스 후기를 저장하고 다시 보여줘요.\n코스 저장·다시 불러오기를 먼저 완성한 뒤 진행해요. 후기 작성자와 어떤 코스의 후기인지도 함께 저장해요.",
      "acceptance": "앱을 껐다 켜도 후기가 남고, 작성자·코스·평점이 올바르게 표시돼요.",
      "dependencies": "DB-03, TEST-01"
    }
  },
  {
    "id": "T-BB974328",
    "expectedVersion": 2,
    "before": {
      "title": "관리자 회원·리뷰 관리 폼 연결",
      "description": "[d32bc76 리뷰 / 우선순위 변경] 핵심 저장·재조회 완료 후 진행. 미완성 관리자 화면의 예제 표·통계를 실제 관리 기능과 연결하고 권한 없는 진입을 막는다.",
      "acceptance": "관리자만 진입, 검색·상태 변경·처리 결과·오류 표시 정상",
      "dependencies": "관리자 인증·관리 화면 연결, 핵심 저장·재조회 완료"
    },
    "after": {
      "title": "관리자 화면에 실제 회원·후기 정보 표시하기",
      "description": "관리자 화면의 예시 표와 통계를 실제 회원·후기 정보로 바꾸고 관리 버튼을 연결해요.\n코스 저장·다시 불러오기를 먼저 완성한 뒤 진행해요.",
      "acceptance": "관리자만 들어갈 수 있고, 검색·상태 변경 결과와 실패 이유를 화면에서 확인할 수 있어요.",
      "dependencies": "T-D50D2A41, TEST-01"
    }
  },
  {
    "id": "T-D50D2A41",
    "expectedVersion": 2,
    "before": {
      "title": "관리자 인증·관리 화면 연결",
      "description": "[d32bc76 리뷰 / 우선순위 변경] 핵심 저장·재조회 완료 후 진행. 남아 있는 관리자 화면과 인증 분기를 연결하고 예제 관리 기능을 실제 회원 관리와 구분한다.",
      "acceptance": "관리자만 진입, 일반 회원 차단, 실제 처리 범위 명확",
      "dependencies": "핵심 저장·재조회 완료"
    },
    "after": {
      "title": "관리자만 관리 화면에 들어가게 만들기",
      "description": "관리자와 일반 회원을 구분해, 관리 화면은 관리자에게만 열어줘요.\n코스 저장·다시 불러오기를 먼저 완성한 뒤 진행해요. 예시로만 동작하는 관리 기능과 실제 처리되는 기능도 구분해요.",
      "acceptance": "일반 회원은 관리 화면에 들어갈 수 없고, 관리자가 실제로 할 수 있는 작업이 명확해요.",
      "dependencies": "TEST-01"
    }
  },
  {
    "id": "UI-02",
    "expectedVersion": 3,
    "before": {
      "title": "저장 코스 편집 복원 연결",
      "description": "저장 목록에서 고른 코스를 RoutePlan으로 복원해 현재 편집 화면을 연다. 목록·상세 폼 완성과는 별도 작업이다.",
      "acceptance": "장소 순서·인원·전체 반경·장소별 반경·이름 유지, 취소 시 저장 원본 보존",
      "dependencies": "DB-03, 저장 코스 목록·상세 폼 완성"
    },
    "after": {
      "title": "저장한 코스를 다시 편집할 수 있게 연결하기",
      "description": "저장 목록에서 고른 코스를 현재 코스 편집 화면에 그대로 열어줘요.\n개발 참고: 저장한 내용을 현재 편집용 코스 정보(RoutePlan)로 되돌려요. 목록·상세 화면을 만드는 작업과는 별도예요.",
      "acceptance": "장소 순서·인원·전체 및 장소별 허용 거리·이름이 유지되고, 편집을 취소하면 저장된 원본은 바뀌지 않아요.",
      "dependencies": "DB-03, T-72A24203"
    }
  },
  {
    "id": "T-717139F6",
    "expectedVersion": 1,
    "before": {
      "title": "메인·저장 코스 화면 이동 연결",
      "description": "저장한 코스를 실제로 찾아갈 수 있도록 메인에서 저장 목록 진입과 돌아가기를 연결한다.",
      "acceptance": "메인→저장 목록→메인 이동 가능, 확정 코스·공통 조건 유지",
      "dependencies": "저장 코스 목록·상세 폼 완성"
    },
    "after": {
      "title": "메인 화면에서 저장한 코스 목록으로 이동하기",
      "description": "메인 화면에서 저장한 코스 목록을 열고, 다시 메인 화면으로 돌아올 수 있게 해요.",
      "acceptance": "메인→저장 목록→메인으로 이동할 수 있고, 정한 코스와 공통 조건이 사라지지 않아요.",
      "dependencies": "T-72A24203"
    }
  },
  {
    "id": "T-72A24203",
    "expectedVersion": 1,
    "before": {
      "title": "저장 코스 목록·상세 폼 완성",
      "description": "저장·재조회 흐름의 핵심 화면이다. 예제 목록을 실제 회원 데이터로 바꾸고 장소 순서·금액·빈 목록·조회 실패 상태를 표시한다.",
      "acceptance": "본인 코스 목록·상세가 DB와 일치하며 빈 결과·실패 안내 정상",
      "dependencies": "DB-03"
    },
    "after": {
      "title": "내가 저장한 코스 목록과 상세 화면 만들기",
      "description": "예시 목록 대신 로그인한 회원이 실제로 저장한 코스들을 보여줘요.\n코스 하나를 열면 방문 순서와 예상 금액을 볼 수 있고, 저장한 코스가 없거나 불러오기에 실패했을 때도 안내해요.",
      "acceptance": "목록과 상세 내용이 저장소(DB)의 본인 코스와 일치하고, 빈 목록·조회 실패 안내가 보여요.",
      "dependencies": "DB-03"
    }
  },
  {
    "id": "T-8430AFC9",
    "expectedVersion": 1,
    "before": {
      "title": "모바일 문서·흐름도 가독성 팀 검토",
      "description": "배포된 문서 메뉴 분리, 실행 안내, 함수 호버·키보드 설명을 팀원 환경에서 확인한다.",
      "acceptance": "가로 넘침·설명 잘림 없음, 필요한 문서와 보드 접근 가능",
      "dependencies": ""
    },
    "after": {
      "title": "휴대폰에서도 문서와 보드가 읽기 쉬운지 확인하기",
      "description": "팀원의 휴대폰 환경에서 문서 메뉴·실행 안내·함수 설명·흐름도를 직접 확인해요.\n함수 설명은 마우스를 올리거나 키보드로 선택했을 때 나오는 짧은 안내를 말해요.",
      "acceptance": "화면이 옆으로 넘치거나 설명이 잘리지 않고, 필요한 문서와 작업 보드로 이동할 수 있어요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-0E784F66",
    "expectedVersion": 1,
    "before": {
      "title": "코드 변경과 클래스 문서 동기화",
      "description": "기능 변경 시 클래스 설명·함수표·흐름도·툴팁·전체 흐름을 함께 갱신하고 이전 코드 상태를 표시한다.",
      "acceptance": "현재 소스와 설명 일치, 문서 링크·함수 설명 누락 없음",
      "dependencies": ""
    },
    "after": {
      "title": "코드를 바꾸면 설명 문서도 함께 고치기",
      "description": "기능을 수정할 때 관련 클래스 설명·함수표·흐름도도 현재 코드에 맞게 고쳐요.\n아직 옛 코드를 설명하는 문서는 어느 버전 기준인지 표시해요.",
      "acceptance": "문서와 코드 설명이 일치하고, 링크나 함수 설명이 빠져 있지 않아요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-67B4D1EA",
    "expectedVersion": 1,
    "before": {
      "title": "배포 전 개인 설정 리소스 분리",
      "description": "지금 빌드 작업은 보류한다. 배포를 준비할 때 db.properties와 지도 키가 JAR에 포함되지 않도록 설정 주입 방식을 정리한다.",
      "acceptance": "배포 산출물에 개인 DB 설정 없음, 외부 설정으로 실행",
      "dependencies": "배포 준비 시"
    },
    "after": {
      "title": "앱을 배포하기 전에 개인 접속 설정 분리하기",
      "description": "앱을 다른 사람에게 전달할 때 개인 DB 접속 정보와 지도 키가 실행 파일에 섞이지 않도록 준비해요.\n지금 당장 앱을 빌드하는 작업은 아니에요. 개발 참고: db.properties를 Java 배포 파일(JAR) 밖에서 읽는 방식을 정리해요.",
      "acceptance": "배포 파일에 개인 접속 정보가 없고, 각 사용자의 외부 설정으로 앱을 실행할 수 있어요.",
      "dependencies": "앱 배포를 준비할 때 진행"
    }
  },
  {
    "id": "T-5EF350F3",
    "expectedVersion": 1,
    "before": {
      "title": "이전 편집·일괄 추천 경로 정리",
      "description": "현재 RouteEditorController·RoutePlan 경로와 이전 편집·일괄 추천·데모 코드를 구분하고 불필요한 참조를 정리한다.",
      "acceptance": "통합 앱 진입점 명확, 데모 정리 후 현재 흐름 정상",
      "dependencies": ""
    },
    "after": {
      "title": "현재 쓰는 추천·편집 코드와 옛 실험 코드 구분하기",
      "description": "실제 앱에서 쓰는 추천·편집 코드가 무엇인지 명확하게 하고, 사용하지 않는 옛 코드의 연결을 정리해요.\n개발 참고: 현재 RouteEditorController·RoutePlan과 예전 편집 화면·한꺼번에 추천하는 방식·데모 코드를 구분해요.",
      "acceptance": "실제 앱의 시작 지점과 실행 흐름을 찾기 쉽고, 옛 코드를 정리한 뒤에도 현재 기능이 정상 동작해요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-75B483C8",
    "expectedVersion": 1,
    "before": {
      "title": "추천 품질·방문 순서 개선",
      "description": "정상 동작 완성 후 거리순 상위 후보의 품질과 코스 동선 개선 기준을 검토한다.",
      "acceptance": "현재 조건·중복·범위 규칙 유지하며 개선 효과 비교",
      "dependencies": ""
    },
    "after": {
      "title": "더 나은 추천 후보와 방문 순서 찾아보기",
      "description": "기본 기능이 정상 동작한 뒤, 거리순으로 고른 후보와 방문 동선을 더 좋게 만들 방법을 비교해요.",
      "acceptance": "가격·활동·거리 조건과 중복 방지 규칙을 지키면서 기존 방식보다 나아진 점을 설명할 수 있어요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-CB2D61E4",
    "expectedVersion": 1,
    "before": {
      "title": "후보·확정 마커 가독성 점검",
      "description": "후보는 기본 마커와 색·투명도로 구분하고 이질적인 ?N 표기를 사용하지 않는다.",
      "acceptance": "후보와 코스 순서 식별 가능, 겹친 마커에서도 선택 가능",
      "dependencies": ""
    },
    "after": {
      "title": "지도에서 추천 후보와 선택한 장소를 쉽게 구분하기",
      "description": "아직 고르지 않은 후보와 코스에 넣은 장소를 지도 표시의 색·투명도로 구분해요.\n지도에서 장소를 가리키는 표시는 마커라고 해요. 후보에 어색한 ‘?N’ 번호는 쓰지 않아요.",
      "acceptance": "후보와 확정한 방문 순서를 알아볼 수 있고, 표시가 겹쳐도 장소를 선택할 수 있어요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-6FC630A9",
    "expectedVersion": 1,
    "before": {
      "title": "줌 감도·애니메이션 추가 조정",
      "description": "현재 JavaFX WebView·카카오 SDK 방식은 유지한다. 여유가 생기면 두 지도의 휠 응답과 단계 전환을 추가 비교한다.",
      "acceptance": "지도만 확대, 검색창·마커 왜곡 없음, 조작감 비교 기록",
      "dependencies": ""
    },
    "after": {
      "title": "지도 확대·축소가 더 부드럽게 느껴지는지 비교하기",
      "description": "마우스 휠로 두 지도를 확대·축소할 때 너무 뻑뻑하거나 갑자기 바뀌는지 비교해요.\n현재 JavaFX 안의 웹 지도(WebView)와 카카오 지도 도구(SDK)는 유지하고, 기본 기능을 완성한 뒤 추가 조정해요.",
      "acceptance": "지도만 확대·축소되고 검색창·장소 표시가 왜곡되지 않아요. 조작감의 차이를 기록해요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-8557BEF4",
    "expectedVersion": 1,
    "before": {
      "title": "부산 서비스 범위 일관성 점검",
      "description": "검색 결과·지도 직접 선택·DB 장소가 동일한 지역 기준을 따르는지 확인하고 지원 범위를 정한다.",
      "acceptance": "출발지 선택과 추천 대상의 지역 기준 일치",
      "dependencies": ""
    },
    "after": {
      "title": "출발지와 추천 장소의 서비스 지역 기준 맞추기",
      "description": "검색으로 고른 출발지, 지도에서 직접 고른 출발지, 저장소의 장소가 같은 지역 기준을 따르는지 확인해요.\n부산 중 어디까지 지원할지도 정해요.",
      "acceptance": "출발지는 고를 수 있는데 주변 장소는 추천할 수 없는 식의 지역 기준 차이가 없어요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-4A36C384",
    "expectedVersion": 1,
    "before": {
      "title": "장소 좌표·분류·가격 이상값 점검",
      "description": "잘못된 좌표·활동 분류·음수 또는 미확인 가격을 점검하고 제외·보완 기준을 정한다.",
      "acceptance": "유효 데이터만 추천에 사용, 미확인과 무료 구분",
      "dependencies": "DATA-01"
    },
    "after": {
      "title": "잘못된 장소 위치·분류·가격 찾아 고치기",
      "description": "장소가 엉뚱한 위치에 찍히거나 활동 종류·가격이 잘못 적힌 자료를 찾아 고치거나 제외해요.\n가격이 음수이거나 확인되지 않은 경우도 점검해요.",
      "acceptance": "사용 가능한 장소 자료만 추천에 쓰이고, 무료와 가격 미확인이 구분돼요.",
      "dependencies": "DATA-01"
    }
  },
  {
    "id": "T-4B15CE1A",
    "expectedVersion": 1,
    "before": {
      "title": "장소 원본 중복 데이터 정리",
      "description": "추천 단계 중복 제거와 별개로 이름·좌표가 같은 원본 묶음과 서로 다른 가격의 대표값 기준을 정한다.",
      "acceptance": "CSV·DB 중복과 대표값 기준 정리, 추천 결과 재검증",
      "dependencies": ""
    },
    "after": {
      "title": "같은 장소가 자료에 여러 번 들어 있는지 정리하기",
      "description": "이름과 지도 위치가 같은 장소가 원본 자료에 여러 번 있는지 확인해요.\n같은 장소에 가격이 여러 개 있으면 어떤 가격을 대표로 쓸지도 정해요. 추천 화면에서 중복을 막는 것과는 별도예요.",
      "acceptance": "파일(CSV)과 저장소(DB)의 중복 기준·대표 가격 기준이 정리되고, 추천 결과에도 문제가 없어요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-86806A84",
    "expectedVersion": 1,
    "before": {
      "title": "저장 코스 이름 변경·삭제 연결",
      "description": "저장·조회 연결 후 본인 코스의 이름 변경과 삭제를 실제 DB 및 화면에 반영한다.",
      "acceptance": "변경·삭제 후 재조회 결과 일치, 실패 안내 정상",
      "dependencies": "DB-03"
    },
    "after": {
      "title": "저장한 코스의 이름 바꾸기·삭제 연결하기",
      "description": "코스를 저장하고 불러오는 기능을 만든 뒤, 본인 코스의 이름 변경과 삭제도 실제 저장 내용에 반영해요.",
      "acceptance": "다시 불러와도 바뀐 이름이나 삭제 결과가 유지되고, 실패하면 안내가 보여요.",
      "dependencies": "DB-03"
    }
  },
  {
    "id": "T-2A0B9CF8",
    "expectedVersion": 1,
    "before": {
      "title": "편집 패널·footer 레이아웃 검증",
      "description": "우측 목록·메시지가 늘어도 지도 크기가 바뀌지 않는지 창 크기별로 확인한다.",
      "acceptance": "지도·footer 위치 유지, 목록과 버튼 잘림 없음",
      "dependencies": ""
    },
    "after": {
      "title": "옆 메뉴가 길어져도 지도와 아래 버튼 위치 유지하기",
      "description": "편집 화면에서 오른쪽 목록이나 안내가 길어져도 지도 크기와 아래쪽 영역(footer)이 흔들리지 않는지 확인해요.",
      "acceptance": "창 크기를 바꿔도 지도·아래 영역의 위치가 유지되고, 목록과 버튼이 잘리지 않아요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-6E4E5509",
    "expectedVersion": 1,
    "before": {
      "title": "조건·후보 전환과 가격 입력 검증",
      "description": "공통 조건과 장소별 조건을 분리하고 Condition·Choice 전환, 추천 전 패널 숨김, 만원 단위 화살표·휠 입력을 확인한다.",
      "acceptance": "조건값 유지, 한 패널만 펼침, 중복 진입 버튼 혼동 없음",
      "dependencies": ""
    },
    "after": {
      "title": "다음 장소의 조건 입력과 후보 선택 화면 확인하기",
      "description": "코스 전체에 공통인 조건과 다음 장소 한 곳에만 적용할 조건을 나눠 확인해요.\n조건 입력(Condition)과 후보 선택(Choice) 중 하나만 펼쳐지고, 추천 전에는 후보 영역이 숨겨지는지 봐요. 가격은 만원 단위 화살표·휠 입력도 확인해요.",
      "acceptance": "화면을 바꿔도 조건값이 유지되고, 두 영역이 동시에 펼쳐지거나 진입 버튼의 역할이 헷갈리지 않아요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-69444E49",
    "expectedVersion": 1,
    "before": {
      "title": "두 지도 범위 표시·시점 유지 검증",
      "description": "전체 범위와 이전 장소 반경을 구분한다. 반경 변경 때 필요한 경우만 화면을 맞추고 장소 추가 시 시점을 유지한다.",
      "acceptance": "두 원 식별 가능, 기본 범위 유지, 편집 완료 후 범위 전체 표시",
      "dependencies": ""
    },
    "after": {
      "title": "지도에 두 거리 범위를 보여주고 화면 위치 유지하기",
      "description": "출발지 중심의 전체 허용 범위와 이전 장소에서 이동할 수 있는 범위를 서로 구분해 보여줘요.\n범위를 바꿀 때 필요한 경우에만 확대·축소하고, 장소를 추가할 때 보고 있던 위치는 바꾸지 않는지 확인해요.",
      "acceptance": "두 원을 구분할 수 있고 기본 범위가 보이며, 편집 완료 후 선택한 범위 전체가 화면에 들어와요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-2099B4C2",
    "expectedVersion": 1,
    "before": {
      "title": "장소 상세정보·외부 지도 연결 검증",
      "description": "후보·내 코스 목록, 더블클릭, 두 지도의 마커에서 상세정보 접근과 기본 브라우저 연결을 확인한다.",
      "acceptance": "동일 장소 상세 표시, 없는 정보 구분, 링크 정상 열림",
      "dependencies": ""
    },
    "after": {
      "title": "여러 곳에서 장소 상세정보가 열리는지 확인하기",
      "description": "추천 후보·내 코스 목록·지도 표시에서 같은 장소의 상세정보를 열 수 있는지 확인해요.\n더블클릭과 외부 지도 링크가 기본 브라우저에서 열리는지도 봐요.",
      "acceptance": "어디서 열어도 같은 장소 정보가 나오고, 없는 정보는 없는 것으로 표시되며 링크가 정상으로 열려요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-A8CE9C3C",
    "expectedVersion": 1,
    "before": {
      "title": "다른 회원 코스 접근 차단 검증",
      "description": "회원별 저장 기능의 데이터 경계를 검증한다. 다른 회원 코스 ID로 조회·수정·삭제가 가능한지 확인한다.",
      "acceptance": "본인 코스만 조회·수정·삭제 가능",
      "dependencies": "DB-03"
    },
    "after": {
      "title": "다른 사람의 코스를 열거나 바꿀 수 없는지 확인하기",
      "description": "로그인한 회원이 다른 회원의 코스 번호(ID)를 알아도 그 코스를 보거나 수정·삭제할 수 없는지 확인해요.",
      "acceptance": "본인이 저장한 코스만 조회·수정·삭제할 수 있어요.",
      "dependencies": "DB-03"
    }
  },
  {
    "id": "T-D764A20A",
    "expectedVersion": 1,
    "before": {
      "title": "인원·1인 가격·누적 금액 검증",
      "description": "잘못된 예상 금액을 막는다. 장소 가격 합계×인원 계산과 장소 추가·삭제·인원 변경 후 표시를 확인한다.",
      "acceptance": "장소별 비용·합계 일치, 총예산 별도 입력 없이 확인 가능",
      "dependencies": ""
    },
    "after": {
      "title": "인원과 장소 가격에 맞게 총금액이 계산되는지 확인하기",
      "description": "장소들의 1인 가격을 더한 뒤 인원수를 곱해 예상 총금액을 올바르게 표시하는지 확인해요.\n장소 추가·삭제·인원 변경 뒤에도 다시 계산되는지 봐요. 가격 미확인 자료를 구분하는 작업은 별도 카드에서 진행해요.",
      "acceptance": "장소별 금액과 총합이 계산 기준에 맞고, 총예산을 따로 입력하지 않아도 현재 예상 금액을 볼 수 있어요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-18071D4A",
    "expectedVersion": 1,
    "before": {
      "title": "장소 변경 후 범위 위반 차단 검증",
      "description": "잘못된 코스 확정을 막는다. 추가·삭제·순서·개별 반경·공통 범위 변경 후 전체 및 구간 조건을 재검사한다.",
      "acceptance": "첫 장소는 출발지 기준, 위반 시 완료 차단·수정 후 복구",
      "dependencies": ""
    },
    "after": {
      "title": "코스를 고친 뒤에도 거리 제한을 지키는지 확인하기",
      "description": "장소 추가·삭제·순서 변경이나 허용 거리 수정 뒤에 코스 전체를 다시 검사해요.\n첫 장소는 출발지에서의 거리로 확인하고, 이후 장소는 이전 장소와의 거리도 확인해요.",
      "acceptance": "전체 범위 또는 장소 사이 거리 제한을 넘으면 편집 완료를 막고, 문제를 고치면 다시 완료할 수 있어요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-F3E0801C",
    "expectedVersion": 1,
    "before": {
      "title": "편집 완료·취소·빈 코스 회귀 검증",
      "description": "편집 중 메인 코스가 바뀌거나 취소한 내용이 남는 데이터 오류를 막는다. 완료·취소·재진입·전체 삭제를 실제 화면에서 확인한다.",
      "acceptance": "완료만 메인 반영, 취소 시 원본 보존, 빈 코스 완료 정상",
      "dependencies": ""
    },
    "after": {
      "title": "편집 완료·취소·전체 삭제가 제대로 되는지 확인하기",
      "description": "편집 중인 내용이 메인 화면에 미리 섞이지 않는지, 취소했을 때 원래 코스로 돌아오는지 확인해요.\n편집을 다시 열거나 장소를 모두 지운 경우도 점검해요. 이런 확인을 회귀 검사, 즉 기존 기능이 깨지지 않았는지 다시 확인하는 검사라고 해요.",
      "acceptance": "완료한 내용만 메인에 반영되고, 취소하면 원본이 유지되며, 장소가 없는 코스로도 완료할 수 있어요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-5A59BC03",
    "expectedVersion": 1,
    "before": {
      "title": "추천 후보 교집합·중복·순서 검증",
      "description": "추천 결과의 정확성을 확인한다. 전체 원형 범위와 이전 장소 반경의 교집합, 활동·가격 조건, 기존 장소 중복 제외와 상위 5곳 순서를 검사한다.",
      "acceptance": "모든 후보가 두 범위·조건을 만족하고 중복 없이 최대 5곳 표시",
      "dependencies": ""
    },
    "after": {
      "title": "추천 후보가 거리·가격 조건을 지키고 중복되지 않는지 확인하기",
      "description": "후보가 전체 허용 범위 안이면서 이전 장소와의 거리 제한도 만족하는지 확인해요.\n두 범위를 동시에 만족하는 영역을 ‘교집합’이라고 해요. 활동 종류·가격 조건, 이미 고른 장소 제외, 추천 순서도 함께 확인해요.",
      "acceptance": "모든 후보가 두 거리 범위와 활동·가격 조건을 만족하고, 중복 없이 추천 순서대로 최대 5곳이 보여요.",
      "dependencies": ""
    }
  },
  {
    "id": "T-6AE25265",
    "expectedVersion": 1,
    "before": {
      "title": "실제 DB 회원가입·로그인·실패 복구 검증",
      "description": "진입 흐름이 막히지 않도록 중복 이메일·오입력·비활성 계정·DB 연결 실패와 재시도를 확인한다.",
      "acceptance": "정상 계정 로그인 성공, 실패 안내·재시도 정상",
      "dependencies": ""
    },
    "after": {
      "title": "회원가입·로그인과 실패 후 재시도 확인하기",
      "description": "실제 회원 저장소(DB)로 회원가입과 로그인이 되는지 확인해요.\n중복 이메일·잘못된 입력·사용 중지 계정·저장소 연결 실패도 시험하고, 오류를 고친 뒤 다시 시도할 수 있는지 봐요.",
      "acceptance": "정상 계정은 로그인할 수 있고, 실패하면 이유와 재시도 방법이 안내돼요.",
      "dependencies": ""
    }
  },
  {
    "id": "DASH-02",
    "expectedVersion": 2,
    "before": {
      "title": "보드 공유 저장 확인",
      "description": "다른 브라우저·팀원 화면에서 저장한 카드 상태가 유지되는지 확인합니다.",
      "acceptance": "새로고침·다른 팀원 화면에 동일한 변경 표시",
      "dependencies": "DASH-01"
    },
    "after": {
      "title": "다른 팀원에게도 같은 작업 상태가 보이는지 확인하기",
      "description": "한 팀원이 바꾼 카드 내용과 진행 상태가 다른 브라우저·팀원에게도 보이는지 확인해요.",
      "acceptance": "새로고침하거나 다른 팀원 화면을 열어도 같은 변경 내용이 보여요.",
      "dependencies": "DASH-01"
    }
  },
  {
    "id": "TEST-01",
    "expectedVersion": 2,
    "before": {
      "title": "저장·종료·재조회 통합 검사",
      "description": "실제 DB를 사용해 앱을 재실행하고 저장 코스를 확인합니다.",
      "acceptance": "순서·허용 반경·금액이 일치함",
      "dependencies": "UI-01, UI-02"
    },
    "after": {
      "title": "코스 저장부터 앱 재실행·다시 불러오기까지 확인하기",
      "description": "실제 저장소(DB)에 코스를 저장하고 앱을 완전히 껐다 켠 뒤, 같은 코스를 다시 열어 봐요.\n여러 기능을 한 흐름으로 확인하는 통합 검사예요.",
      "acceptance": "장소 순서·전체 및 장소별 허용 거리·금액이 저장 전과 같아요.",
      "dependencies": "UI-01, UI-02"
    }
  },
  {
    "id": "UI-01",
    "expectedVersion": 2,
    "before": {
      "title": "저장 버튼과 결과 안내",
      "description": "편집 완료와 DB 저장을 구분하는 버튼과 성공·실패 안내를 만듭니다.",
      "acceptance": "저장 상태를 명확히 알 수 있음",
      "dependencies": "DB-02"
    },
    "after": {
      "title": "코스 저장 버튼과 성공·실패 안내 만들기",
      "description": "‘편집을 마쳐 지도에 반영하기’와 ‘나중에 다시 열도록 저장하기’를 사용자가 구분할 수 있게 해요.\n저장 버튼과 저장 성공·실패 안내를 연결해요.",
      "acceptance": "현재 코스가 실제 저장됐는지 알 수 있고, 저장 실패를 성공으로 오해하지 않아요.",
      "dependencies": "DB-02"
    }
  },
  {
    "id": "DB-03",
    "expectedVersion": 2,
    "before": {
      "title": "회원별 코스 목록·상세 조회",
      "description": "로그인 회원에 속한 코스만 조회하도록 연결합니다.",
      "acceptance": "다른 회원 코스에 접근하지 않음",
      "dependencies": "DB-02"
    },
    "after": {
      "title": "로그인한 회원의 코스만 불러오기",
      "description": "저장소(DB)에서 로그인한 회원의 코스 목록과 자세한 내용을 불러오는 기능을 만들어요.",
      "acceptance": "다른 회원의 코스는 조회 결과에 포함되지 않아요.",
      "dependencies": "DB-02"
    }
  },
  {
    "id": "DB-02",
    "expectedVersion": 3,
    "before": {
      "title": "코스 저장 로직·트랜잭션",
      "description": "코스와 장소 목록을 하나의 저장 작업으로 처리합니다.",
      "acceptance": "실패 시 부분 저장되지 않음",
      "dependencies": "DB-01"
    },
    "after": {
      "title": "코스가 빠짐없이 한 번에 저장되게 만들기",
      "description": "코스 기본 정보와 방문 장소 목록을 전부 저장하거나, 실패하면 모두 취소되도록 만들어요.\n예를 들어 코스 이름만 저장되고 장소 목록은 빠지는 일이 없어야 해요. 여러 저장을 한 묶음으로 처리하는 방식을 트랜잭션이라고 해요.",
      "acceptance": "중간에 오류가 나도 코스나 장소 목록 일부만 저장되지 않아요.",
      "dependencies": "DB-01"
    }
  },
  {
    "id": "DATA-01",
    "expectedVersion": 3,
    "before": {
      "title": "가격·평점 출처와 확인일 정리",
      "description": "1인 가격 기준과 출처·확인일을 기록하고 무료와 가격 미확인을 구분합니다.",
      "acceptance": "가격 해석 기준을 확인할 수 있음",
      "dependencies": "자료 확인"
    },
    "after": {
      "title": "가격과 평점을 어디서 언제 확인했는지 남기기",
      "description": "장소 가격과 평점의 출처·확인 날짜를 기록하고, 가격이 1인 기준인지 명확히 해요.\n무료인 장소와 가격 정보를 아직 모르는 장소도 구분해요.",
      "acceptance": "가격과 평점의 출처·확인 날짜·1인 가격 기준을 확인할 수 있어요.",
      "dependencies": "가격·평점 원본 자료 확인"
    }
  },
  {
    "id": "PUB-02",
    "expectedVersion": 2,
    "before": {
      "title": "팀원·비팀원 계정으로 접근 검증",
      "description": "다른 팀원 계정과 비팀원 계정으로 문서·검색·보드 API를 확인합니다.",
      "acceptance": "팀원 허용, 비팀원 읽기·수정 거부",
      "dependencies": "PUB-01"
    },
    "after": {
      "title": "초대된 팀원만 문서를 볼 수 있는지 확인하기",
      "description": "팀원 계정과 팀원이 아닌 계정으로 문서·검색·작업 보드에 접근해 봐요.",
      "acceptance": "팀원은 사용할 수 있고, 팀원이 아닌 사람은 내용을 읽거나 바꿀 수 없어요.",
      "dependencies": "PUB-01"
    }
  },
  {
    "id": "MAP-01",
    "expectedVersion": 2,
    "before": {
      "title": "지도 로딩 실패 후 재시도",
      "description": "서버를 나중에 켜도 지도를 다시 연결할 수 있게 합니다.",
      "acceptance": "앱 재시작 없이 지도 복구",
      "dependencies": ""
    },
    "after": {
      "title": "지도가 안 열렸을 때 다시 연결할 수 있게 만들기",
      "description": "지도 서버가 늦게 켜졌더라도 앱을 끄지 않고 다시 지도를 연결할 수 있게 해요.",
      "acceptance": "앱을 다시 실행하지 않아도 지도가 복구돼요.",
      "dependencies": ""
    }
  },
  {
    "id": "DASH-01",
    "expectedVersion": 1,
    "before": {
      "title": "작업 보드·상태 이동·영역 필터",
      "description": "상태별 열, 기능 라벨, 카드 편집과 관련 노트 연결을 구현합니다.",
      "acceptance": "데스크톱·모바일에서 작업을 찾고 이동할 수 있음",
      "dependencies": ""
    },
    "after": {
      "title": "할 일을 상태별로 나누고 찾아보는 작업 보드 만들기",
      "description": "할 일·진행 중·동작 확인 중·완료로 카드를 나누고, 분야별로 찾거나 내용을 고칠 수 있게 만들어요.",
      "acceptance": "PC와 휴대폰에서 작업을 찾고 상태를 바꾸며 관련 문서를 열 수 있어요.",
      "dependencies": ""
    }
  },
  {
    "id": "DOC-01",
    "expectedVersion": 1,
    "before": {
      "title": "클래스별 코드 학습 노트",
      "description": "Java 역할·주요 메서드·호출 흐름을 정리했습니다.",
      "acceptance": "현재 코드와 링크 일치",
      "dependencies": ""
    },
    "after": {
      "title": "클래스별 역할과 코드 읽는 순서 정리하기",
      "description": "Java 클래스마다 맡은 일과 주요 함수, 함수가 이어지는 순서를 학습 노트로 정리했어요.",
      "acceptance": "설명이 해당 코드와 맞고 관련 링크가 열려요.",
      "dependencies": ""
    }
  },
  {
    "id": "DOC-02",
    "expectedVersion": 1,
    "before": {
      "title": "라이브러리 사용 표·내부 흐름도",
      "description": "Java API가 쓰인 메서드와 클래스 내부 흐름도를 추가했습니다.",
      "acceptance": "라이브러리 설명과 Mermaid 렌더 확인",
      "dependencies": ""
    },
    "after": {
      "title": "사용한 Java 도구와 함수 흐름을 그림으로 설명하기",
      "description": "미리 제공되는 Java 기능(라이브러리)이 어디에 쓰이는지와 클래스 내부 함수 흐름을 정리했어요.",
      "acceptance": "사용한 기능의 설명을 읽고, 흐름도 그림을 정상적으로 볼 수 있어요.",
      "dependencies": ""
    }
  },
  {
    "id": "EDIT-01",
    "expectedVersion": 1,
    "before": {
      "title": "전체·구간 반경 검사와 수정",
      "description": "순서·삭제·기존 장소 반경 수정 후 조건을 재검사합니다.",
      "acceptance": "위반 구간 완료 차단·수정 후 복구",
      "dependencies": ""
    },
    "after": {
      "title": "거리 제한을 넘은 코스를 찾고 고칠 수 있게 만들기",
      "description": "장소를 지우거나 순서·허용 거리를 바꾼 뒤, 전체 범위와 장소 사이 거리 제한을 다시 검사해요.",
      "acceptance": "제한을 넘으면 완료를 막고, 사용자가 문제를 고치면 다시 완료할 수 있어요.",
      "dependencies": ""
    }
  },
  {
    "id": "EDIT-02",
    "expectedVersion": 1,
    "before": {
      "title": "빈 코스 확정·취소 처리",
      "description": "코스 전체 삭제 완료와 취소 시 원본 보존을 지원합니다.",
      "acceptance": "메인 장소·경로·금액 초기화, 공통 조건 유지",
      "dependencies": ""
    },
    "after": {
      "title": "장소를 모두 지우거나 편집을 취소하는 동작 만들기",
      "description": "장소를 전부 지운 코스로도 편집을 마칠 수 있고, 취소하면 편집 전 코스로 돌아가게 해요.",
      "acceptance": "전체 삭제를 완료하면 메인의 장소·경로·금액이 초기화되고 공통 조건은 유지돼요. 취소하면 원본이 보존돼요.",
      "dependencies": ""
    }
  },
  {
    "id": "PUB-01",
    "expectedVersion": 1,
    "before": {
      "title": "팀원 전용 문서 사이트 게시",
      "description": "GitHub 로그인과 Collaborators 열람 제한을 연결했습니다.",
      "acceptance": "소유자 로그인과 비로그인 차단 확인",
      "dependencies": ""
    },
    "after": {
      "title": "GitHub로 로그인하는 팀원 전용 문서 사이트 열기",
      "description": "저장소에 초대된 팀원(Collaborators)만 GitHub로 로그인해서 문서를 볼 수 있게 했어요.",
      "acceptance": "소유자는 로그인할 수 있고, 로그인하지 않은 사람에게는 문서가 공개되지 않아요.",
      "dependencies": ""
    }
  }
];
