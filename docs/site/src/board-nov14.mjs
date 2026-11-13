// One-time publication sync. Keep assignment, priority, links and archive state owned by teammates.
export const NOV14_VERSION='implementation-ea2242b-2026-11-14-v1';
export const NOV14_UPDATES=[
 ['DB-01','done','07·08 SQL과 가격·출처 스냅샷 구조를 구현했어요.'],
 ['DB-02','done','신규 저장·같은 ID 갱신·롤백과 가격 범위 보관을 구현했어요.'],
 ['DB-03','done','본인 코스 목록·장소 스냅샷·최근 수정 시각을 실제 DB에서 읽어요.'],
 ['UI-01','done','하단 저장 버튼과 이름 입력·성공 실패 안내를 연결했어요.'],
 ['UI-02','done','불러오기·재편집·같은 ID 저장을 연결했어요.'],
 ['TEST-01','done','사용자가 실제 저장·수정·앱 재시작 복원을 정상 확인했어요. 이후 UI 변경의 최종 시연은 CHECK-01에서 확인해요.'],
 ['T-717139F6','done','헤더의 저장한 코스로 같은 창 안에서 이동해요.'],
 ['T-72A24203','done','예제 목록을 본인 DB 목록으로 교체했어요.'],
 ['T-86806A84','done','저장 코스 이름 변경·삭제를 DB에 연결했어요.'],
 ['T-31FAD2F2','done','미확인·범위·근거 있는 무료를 구분해 추천과 합계에 전달해요.'],
 ['T-D764A20A','review','범위·인원·미확인 합계 자동 검사를 통과했어요. 시연 목록 가격 9곳은 미확인이므로 실제 메뉴 가격 검증과 구분해요.'],
 ['T-DD5B5813','progress','기존 DB를 유지하고 별도 시연 JSON 9곳을 앱에 연결했어요. 전체 자료 개선은 아직 끝나지 않았어요.'],
 ['T-F3E0801C','review','돌아가기에서 초안을 유지하는 방식으로 변경했어요. 초기화·로그아웃·실제 지도 팝업은 최종 시연을 확인해요.'],
 ['T-4320E4E0','done','경로 설명을 지도 위에 겹쳐 표시해 지도 크기를 유지해요.'],
 ['T-0E784F66','done','ea2242b 기준 변경 클래스 32개·검사 노트 12개와 현재 전체 흐름을 갱신했어요.'],
 ['SITE-01','done','앱·폴더 재정리와 문서 사이트 소스를 같은 브랜치에 커밋·푸시했어요.']
];
export const NOV14_ADDITIONS=[
 ['CHECK-01','최종 시연: 로그인부터 로그아웃·재로그인까지','docs','todo','high','저장·재시작 복원은 사용자 확인 완료예요. 새 메뉴·초안 유지·팝업·실제 지도·Windows 포커스를 한 번에 시연해요.','로그인 → 코스 → 저장 → 재시작 복원 → 내 페이지 → 로그아웃 → 재로그인 확인','','/현재_구현_흐름.html'],
 ['TIME-01','장소를 추가할 때 누적 이동 시간 표시','frontend','todo','normal','검토만 완료했어요. 새로 생긴 한 구간을 계산해 누적하고 계산 중·미확인 상태를 구분해요.','장소 추가·삭제·순서 변경 후 올바른 이동 시간 합계 표시','','/현재_구현_흐름.html'],
 ['TIME-02','추천 후보 5곳의 이동 시간 비교','frontend','todo','low','선택 전에 후보까지의 시간을 비교하는 후속 제안이에요. 요청 수·취소·재사용부터 설계해요.','후보별 이전 장소에서의 시간과 미확인 표시','TIME-01','/현재_구현_흐름.html'],
 ['ROUTE-01','자동차·대중교통 경로 확장 검토','backend','todo','low','공식 API·무료 조건만 검토했어요. 현재 구현은 TMAP 도보예요. 과제 일정상 선택 기능이에요.','선택한 한 수단의 실제 계정 권한·무료 한도·표시 방식 확인','','/현재_구현_흐름.html'],
 ['UI-03','공통 폼·내 페이지·로그아웃 연결','frontend','done','normal','BasePageController 상속, 공통 로딩·자동 알림, 내 페이지 읽기·로그아웃을 구현했어요. 실제 재로그인 시연은 CHECK-01이에요.','본인 이름·이메일·전화번호 조회 및 세션 정리','','/02_화면/mypagecontroller.html']
];
export function applyNov14(sql){
 if(sql.exec('SELECT value FROM board_meta WHERE key=?',NOV14_VERSION).toArray().length)return;
 const report={changed:[],added:[],skipped:[]},now=new Date().toISOString();
 for(const [id,status,evidence] of NOV14_UPDATES){
  const task=sql.exec('SELECT * FROM tasks WHERE id=?',id).toArray()[0];
  if(!task||task.archived){report.skipped.push(id);continue;}
  const note='\n\n[2026-11-14 · ea2242b 확인]\n'+evidence;
  const description=task.description.slice(0,Math.max(0,2000-note.length))+note;
  sql.exec('UPDATE tasks SET status=?,description=?,version=version+1,updatedAt=?,updatedBy=? WHERE id=? AND version=?',status,description,now,'구현 동기화 (2egn 요청)',id,task.version);
  report.changed.push(id);
 }
 for(const [id,title,area,status,priority,description,acceptance,dependencies,relatedNote] of NOV14_ADDITIONS){
  if(sql.exec('SELECT id FROM tasks WHERE id=?',id).toArray().length)continue;
  sql.exec('INSERT INTO tasks(id,title,area,status,priority,description,acceptance,dependencies,relatedNote,createdAt,updatedAt,updatedBy) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',id,title,area,status,priority,description,acceptance,dependencies,relatedNote,now,now,'구현 동기화 (2egn 요청)');report.added.push(id);
 }
 sql.exec('INSERT INTO board_meta(key,value) VALUES(?,?)',NOV14_VERSION,JSON.stringify(report));
}
