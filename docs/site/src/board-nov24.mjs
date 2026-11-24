// 기능 브랜치 검증 결과만 반영하며 팀원 수정·보관·담당자·선행 관계는 유지한다.
export const NOV24_VERSION='implementation-feature-933fd85-2026-11-24-v1';
export const IMPLEMENTATION_REFERENCE={commit:'933fd85',reviewedAt:'2026-11-24',scope:'feature',branch:'data/busan-core-expansion-20261123'};
export const NOV24_UPDATES=[
 ['T-DD5B5813','progress','운영 카탈로그 2,040곳과 내장 시연 386곳을 구분해요. 장소별 영업·메뉴의 전체 수동 확인은 남아 있어요.'],
 ['DATA-01','progress','가격은 참고·추정·미확인을 구분해요. 지역 통계는 해당 매장의 실제 메뉴 확인값으로 표시하지 않아요.'],
 ['T-6E4E5509','review','무료 가격 조건과 무료입장 특징은 문화 활동에만 제공해요. 활동 전환 시 선택 복원을 자동 검사했으며 최종 수동 시연은 별도예요.'],
 ['T-B5F06FB3','done','기준본 지문·전체 ID 보존과 배치 오류 롤백을 검사했어요. 5분 넘는 미래 자료 버전은 쓰기 전에 차단해요.']
];
export const NOV24_ADDITIONS=[
 ['STRUCTURE-24','앱 패키지·대표 페이지와 보관 코드 정리','infra','done','앱을 com.datecourse로 통일하고 만들기·편집·저장 화면에 순서를 붙였어요. 미사용 예제 24개를 원본 지문과 함께 보관하고 기본 빌드에서 제외했어요.','Java 21 앱·유지보수 370개 및 보관 예제 2개 통과, FXML 5개와 JAR 확인','','/패키지_통합과_구조_점검_2026_11_24.html'],
 ['CATALOG-24','장소 2,040곳 확장과 가격 근거 보완','data','review','기존 장소·사용자 코스를 보존하며 운영 장소를 확장했어요. 참고 가격 17곳·통계 추정 1,176곳·미확인 847곳이며 전체 매장의 실제 가격 검증은 남겨요.','출처·가격 상태를 구분하고 기준본·기존 ID를 보존, 전체 영업·가격 확인은 후속','','/가격_보완_2026_11_24.html'],
 ['MAINTENANCE-24','빈 백업 정리·카탈로그 버전 복원 기록','backend','done','승인된 빈 백업 8개 정리 후 15개 테이블을 유지해요. 카탈로그의 잘못된 미래 시각만 정정하고 원본·사용자·코스·가격 payload 보존 기록을 남겼어요.','복원 SQL·정정 기록 보존, 해당 운영 작업은 재실행하지 않음','','/DB_구조와_빈_백업_정리_2026_11_24.html']
];
const machineAuthors=new Set(['초기 작업 목록','브랜치 대조 반영 (2egn 요청)','과제 시연 기준 재검토 (2egn 요청)','구현 동기화 (2egn 요청)','구현 동기화 (2026-11-14)','구현 동기화 (2026-11-16)']);
/** 한 번만 자동 관리 카드를 갱신하고 사용자 수정과 긴 설명은 건너뛴다. */
export function applyNov24(sql){
 if(sql.exec('SELECT value FROM board_meta WHERE key=?',NOV24_VERSION).toArray().length)return;
 const report={changed:[],added:[],skipped:[]},now=new Date().toISOString();
 for(const [id,status,evidence]of NOV24_UPDATES){
  const row=sql.exec('SELECT * FROM tasks WHERE id=?',id).toArray()[0];
  if(!row||row.archived||!machineAuthors.has(row.updatedBy)){report.skipped.push(id);continue;}
  const note='[기능 브랜치 · master 미반영]\n'+evidence+'\n\n[이전 기록 · 2026-11-24 점검 전]\n';
  if(row.description.length+note.length>2000){report.skipped.push(id);continue;}
  sql.exec('UPDATE tasks SET status=?,description=?,version=version+1,updatedAt=?,updatedBy=? WHERE id=? AND version=?',status,note+row.description,now,'구현 동기화 (2026-11-24)',id,row.version);
  report.changed.push(id);
 }
 for(const [id,title,area,status,evidence,acceptance,dependencies,relatedNote]of NOV24_ADDITIONS){
  if(sql.exec('SELECT id FROM tasks WHERE id=?',id).toArray().length)continue;
  const description='[기능 브랜치 · master 미반영]\n'+evidence;
  sql.exec('INSERT INTO tasks(id,title,area,status,priority,description,acceptance,dependencies,relatedNote,createdAt,updatedAt,updatedBy) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',id,title,area,status,'normal',description,acceptance,dependencies,relatedNote,now,now,'구현 동기화 (2026-11-24)');report.added.push(id);
 }
 sql.exec('INSERT INTO board_meta(key,value) VALUES(?,?)',NOV24_VERSION,JSON.stringify(report));
}
