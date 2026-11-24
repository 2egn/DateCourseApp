// 검증된 기능 브랜치 기록만 추가하며 기존 카드·담당자·선행 작업은 변경하지 않는다.
export const NOV25_VERSION='implementation-feature-64d93e3-2026-11-25-v1';
export const IMPLEMENTATION_REFERENCE={commit:'64d93e3',reviewedAt:'2026-11-25',scope:'feature',branch:'data/busan-core-expansion-20261123'};
export const NOV25_ADDITIONS=[
 ['UI-VISIBILITY-25','작은 창 배치와 글자 가독성 확인','frontend','review','좁은 편집 합계·저장 상세 표를 재배치하고 대비·선택·스크롤·알림 위치를 개선했어요. Windows 배율별 최종 시연은 남아 있어요.','자동 배치 검사 통과, 실제 배율·창 크기 수동 확인은 후속','/화면_가독성과_작은_창_배치_2026_11_24.html'],
 ['MAP-BUFFER-25','지도 여유 렌더링과 경계 표시 확인','frontend','review','SDK 지도를 화면보다 넓게 렌더링하고 입력·카메라·이미지 캡처를 보정했어요. 실제 타일 속도와 경계 소실 개선은 아직 확정하지 않아요.','모의 SDK 좌표·경계·출처 배치 검사, 실제 SDK 시연은 후속','/지도_렌더링_여유_영역_2026_11_24.html'],
 ['INPUT-FIXES-25','지도 버튼 우선 입력과 상세 닫기 오류 수정','frontend','done','버튼 아래 마커의 입력 가로채기, 지연 이미지의 명시적 지도 로딩 우회, 경로 닫기의 Tab 접근을 수정하고 재현 검사를 추가했어요.','세 재현 검사와 전체 Java 384개·보관 2개 통과','/마우스_키보드_입력_점검_2026_11_24.html'],
 ['JDK21-25','JDK 21 검색과 자동 다운로드 연결','infra','done','설치된 21을 검색하고 없으면 개인 Gradle 캐시에 받아요. JDK 파일·개인 경로는 공유하지 않아요.','설치된 JDK 사용·실제 다운로드·오프라인 재사용과 전체 검사 통과','/JDK21_자동_검색과_선택_2026_11_25.html'],
 ['DB-SETUP-25','일반 실행과 명시적 DB 준비 분리','backend','done','140267b에서 일반 앱의 자동 테이블 생성을 제거하고 명시적 유지보수 명령으로 분리했어요. 지도 불러온 여부는 세션에서만 기억해요.','추가 지도 테이블 없음, 격리 DB 검사 통과, 운영 DB 준비 재실행 없음','/DB_준비와_일반_실행_분리_2026_11_24.html'],
 ['KEYBOARD-UX-25','장소 목록 키보드 조작과 포커스 복귀 개선','frontend','todo','저장 장소 표의 방향키·Enter, 상세 종료 후 포커스 복귀, 내 코스 Enter 상세, 포커스 표시 강화를 제안했어요. 아직 구현하지 않았어요.','제안 항목의 키보드·마우스 동등 조작과 포커스 확인','/마우스_키보드_입력_점검_2026_11_24.html']
];
/** 같은 ID가 있으면 원문·상태·담당자·보관 여부를 그대로 유지한다. */
export function applyNov25(sql){
 if(sql.exec('SELECT value FROM board_meta WHERE key=?',NOV25_VERSION).toArray().length)return;
 const report={added:[],skipped:[]},now=new Date().toISOString();
 for(const [id,title,area,status,evidence,acceptance,relatedNote]of NOV25_ADDITIONS){
  if(sql.exec('SELECT id FROM tasks WHERE id=?',id).toArray().length){report.skipped.push(id);continue;}
  sql.exec('INSERT INTO tasks(id,title,area,status,priority,description,acceptance,dependencies,relatedNote,createdAt,updatedAt,updatedBy) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',
   id,title,area,status,'normal','[기능 브랜치 64d93e3 · master 미반영]\n'+evidence,acceptance,'',relatedNote,now,now,'구현 동기화 (2026-11-25)');
  report.added.push(id);
 }
 sql.exec('INSERT INTO board_meta(key,value) VALUES(?,?)',NOV25_VERSION,JSON.stringify(report));
}
