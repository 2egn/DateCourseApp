// Reconcile only machine-maintained cards. Human edits and archived cards remain authoritative.
export const NOV16_VERSION='implementation-master-334705d-2026-11-16-v1';
export const IMPLEMENTATION_REFERENCE={commit:'334705d',reviewedAt:'2026-11-16'};
export const NOV16_UPDATES=[
 ['TIME-01','done','편집 중 장소 추가·삭제·순서 변경 후 도보 구간과 누적 시간을 갱신해요.'],
 ['T-D50D2A41','done','실제 관리자 인증과 AdminSession으로 회원·정정 검토를 연결했어요.'],
 ['T-BB974328','progress','실제 회원 관리·장소 정정 검토는 완료했어요. 커뮤니티 후기 관리는 후속 기능이에요.'],
 ['T-69444E49','done','두 원을 구분하고 조절 중인 원·새 구간 중심으로 맞춰요. 실제 SDK 검사 통과.'],
 ['T-6FC630A9','done','공통 Java 카메라 정책과 부드러운 이동을 적용하고 실제 SDK에서 확인했어요.'],
 ['T-CB2D61E4','done','후보와 코스 핀을 구분하고 핀은 장소 상세, 선·구간 번호는 경로 안내로 연결해요.'],
 ['T-2099B4C2','done','후보·코스 핀의 상세 열기와 hover·클릭 역할을 분리하고 회귀 검사했어요.'],
 ['T-2A0B9CF8','review','지도와 패널 크기·스크롤 배치 자동 검사를 통과했어요. 최종 사용자 PC 시연은 남겨요.'],
 ['T-18071D4A','review','추가·삭제·순서·반경 변경 후 전체/구간 제한 검사를 자동 검증했어요.'],
 ['T-5A59BC03','review','조건·중복 제외·가격 해석 자동 검사를 통과했어요. 최종 시연은 CHECK-01이에요.'],
 ['T-6E4E5509','review','업종·상세 종류·태그·가격 필터와 후보 5~30곳 선택을 구현·검사했어요.'],
 ['T-A8CE9C3C','done','다른 회원의 코스 조회·갱신·삭제를 격리 DB에서 거부하는지 확인했어요.'],
 ['T-67B4D1EA','done','개인 DB·지도 키를 패키징에서 제외하고 공통 키로 런타임 조회해요.'],
 ['T-B5F06FB3','done','공공자료 수집·적재·지문 검증 도구가 있고 LF 새 사본에서도 검사를 통과했어요.'],
 ['T-DD5B5813','progress','공공 시연 127곳·태그·카탈로그 적재를 연결했어요. 전체 원본 품질 검토는 계속해요.'],
 ['T-0E784F66','done','master 병합과 최신 사용 흐름·핵심 클래스 설명을 반영했어요.'],
 ['DATA-01','progress','가격 근거·확인 상태·공공 사실을 분리했어요. 실시간 평점 제공자 연결은 후속이에요.']
];
export const NOV16_ADDITIONS=[
 ['PREVIEW-01','저장 코스 지도 미리보기와 이미지 복구','frontend','done','실제 지도 바탕에 직선 방문 순서를 저장해요. 이미지 실패와 코스 저장을 구분하고 재생성해요.','이미지 없이 저장된 코스도 열 수 있고 이미지 조회·생성을 다시 시도할 수 있음','DB-02','/저장_미리보기와_복구.html'],
 ['ADMIN-01','회원 변경 확인과 장소 정정 검토','backend','done','정상/탈퇴 표기, 수정·삭제 확인, 오래된 회원·정정 덮어쓰기 차단을 구현했어요.','확인 취소 시 변경 없음, 저장 코스가 있는 회원 삭제 제한','T-D50D2A41','/관리자_이용과_검토.html'],
 ['DATA-LF-01','새 checkout에서도 공공자료 검증 통과','data','done','LF 원본 기준으로 지문을 갱신하고 CRLF 재생성 방지 검사를 추가했어요.','커밋만 추출한 새 사본에서 Java·자료 검사 통과','T-B5F06FB3','/현재_구현_흐름.html'],
 ['MERGE-01','검증된 기능을 master에 병합','infra','done','334705d가 origin/master에 반영됐어요. Java 324개·Python 59개·사이트 53개와 JAR를 새 사본에서 확인했어요.','원격 master 커밋 일치, 기존 사용자 변경 보존','','/현재_구현_흐름.html']
];
const machineAuthors=new Set(['초기 작업 목록','브랜치 대조 반영 (2egn 요청)','과제 시연 기준 재검토 (2egn 요청)','구현 동기화 (2egn 요청)']);
export function applyNov16(sql){
 if(sql.exec('SELECT value FROM board_meta WHERE key=?',NOV16_VERSION).toArray().length)return;
 const report={changed:[],added:[],skipped:[]},now=new Date().toISOString();
 for(const [id,status,evidence]of NOV16_UPDATES){
  const row=sql.exec('SELECT * FROM tasks WHERE id=?',id).toArray()[0];
  if(!row||row.archived||!machineAuthors.has(row.updatedBy)){report.skipped.push(id);continue;}
  const note=evidence+'\n\n[이전 기록 · 2026-11-16 점검 전]\n';
  if(row.description.length+note.length>2000){report.skipped.push(id);continue;}
  const description=note+row.description;
  sql.exec('UPDATE tasks SET status=?,description=?,version=version+1,updatedAt=?,updatedBy=? WHERE id=? AND version=?',status,description,now,'구현 동기화 (2026-11-16)',id,row.version);
  report.changed.push(id);
 }
 for(const [id,title,area,status,description,acceptance,dependencies,relatedNote]of NOV16_ADDITIONS){
  if(sql.exec('SELECT id FROM tasks WHERE id=?',id).toArray().length)continue;
  sql.exec('INSERT INTO tasks(id,title,area,status,priority,description,acceptance,dependencies,relatedNote,createdAt,updatedAt,updatedBy) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',id,title,area,status,'normal',description,acceptance,dependencies,relatedNote,now,now,'구현 동기화 (2026-11-16)');report.added.push(id);
 }
 sql.exec('INSERT INTO board_meta(key,value) VALUES(?,?)',NOV16_VERSION,JSON.stringify(report));
}
