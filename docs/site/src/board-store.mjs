import {COPY_VERSION,TASK_COPY} from './board-copy.mjs';
import {AUDIT_VERSION,AUDIT_TASKS,AUDIT_ADDITIONS} from './board-audit.mjs';
import {taskTree} from '../web/dashboard/task-tree.js';
import {ASSIGNMENT_TASKS,applyAssignmentPriorities} from './assignment-priorities.mjs';
import {applyDb01Design} from './board-db01.mjs';
import {applyDb02Implementation} from './board-db02.mjs';
import {applyNov14} from './board-nov14.mjs';
export const STATUSES = {todo:'할 일',progress:'진행 중',review:'동작 확인 중',done:'완료'};
export const AREAS = {frontend:'화면·지도',backend:'저장·내부 처리',infra:'로그인·실행 환경',docs:'설명서·동작 확인',data:'장소 자료'};
export const PRIORITIES = {high:'높음',normal:'보통',low:'낮음'};

const seed = [
 ['PUB-01','팀원 전용 문서 사이트 게시','infra','done','GitHub 로그인과 Collaborators 열람 제한을 연결했습니다.','소유자 로그인과 비로그인 차단 확인','', ''],
 ['PUB-02','팀원·비팀원 계정으로 접근 검증','docs','review','다른 팀원 계정과 비팀원 계정으로 문서·검색·보드 API를 확인합니다.','팀원 허용, 비팀원 읽기·수정 거부','PUB-01',''],
 ['DASH-01','작업 보드·상태 이동·영역 필터','frontend','done','상태별 열, 기능 라벨, 카드 편집과 관련 노트 연결을 구현합니다.','데스크톱·모바일에서 작업을 찾고 이동할 수 있음','', ''],
 ['DASH-02','보드 공유 저장 확인','backend','review','다른 브라우저·팀원 화면에서 저장한 카드 상태가 유지되는지 확인합니다.','새로고침·다른 팀원 화면에 동일한 변경 표시','DASH-01',''],
 ['DB-01','회원별 코스 저장 구조 설계','backend','todo','코스·장소 순서·장소별 허용 반경·회원 관계를 설계합니다.','RoutePlan 정보를 손실 없이 저장할 수 있음','팀 합의','/04_데이터/routeplan.html'],
 ['DB-02','코스 저장 로직·트랜잭션','backend','todo','코스와 장소 목록을 하나의 저장 작업으로 처리합니다.','실패 시 부분 저장되지 않음','DB-01','/04_데이터/routeplan.html'],
 ['UI-01','저장 버튼과 결과 안내','frontend','todo','편집 완료와 DB 저장을 구분하는 버튼과 성공·실패 안내를 만듭니다.','저장 상태를 명확히 알 수 있음','DB-02','/02_화면/conditioncontroller.html'],
 ['DB-03','회원별 코스 목록·상세 조회','backend','todo','로그인 회원에 속한 코스만 조회하도록 연결합니다.','다른 회원 코스에 접근하지 않음','DB-02','/05_회원_db/memberrepository.html'],
 ['UI-02','저장 목록과 편집 복원 연결','frontend','todo','예제 저장 목록을 실제 조회 결과로 교체하고 선택 코스를 복원합니다.','순서·인원·반경이 유지됨','DB-03','/02_화면/savedcoursecontroller.html'],
 ['TEST-01','저장·종료·재조회 통합 검사','docs','todo','실제 DB를 사용해 앱을 재실행하고 저장 코스를 확인합니다.','순서·허용 반경·금액이 일치함','UI-01, UI-02',''],
 ['MAP-01','지도 로딩 실패 후 재시도','frontend','todo','서버를 나중에 켜도 지도를 다시 연결할 수 있게 합니다.','앱 재시작 없이 지도 복구','', '/02_화면/plannermap.html'],
 ['DATA-01','가격·평점 출처와 확인일 정리','data','todo','1인 가격 기준과 출처·확인일을 기록하고 무료와 가격 미확인을 구분합니다.','가격 해석 기준을 확인할 수 있음','자료 확인','/04_데이터/place.html'],
 ['DOC-01','클래스별 코드 학습 노트','docs','done','Java 역할·주요 메서드·호출 흐름을 정리했습니다.','현재 코드와 링크 일치','', '/목차.html'],
 ['DOC-02','라이브러리 사용 표·내부 흐름도','docs','done','Java API가 쓰인 메서드와 클래스 내부 흐름도를 추가했습니다.','라이브러리 설명과 Mermaid 렌더 확인','', '/라이브러리_사용_안내.html'],
 ['EDIT-01','전체·구간 반경 검사와 수정','backend','done','순서·삭제·기존 장소 반경 수정 후 조건을 재검사합니다.','위반 구간 완료 차단·수정 후 복구','', '/03_처리/routerangevalidator.html'],
 ['EDIT-02','빈 코스 확정·취소 처리','frontend','done','코스 전체 삭제 완료와 취소 시 원본 보존을 지원합니다.','메인 장소·경로·금액 초기화, 공통 조건 유지','', '/02_화면/routeeditorcontroller.html']
];

function json(value,status=200){return Response.json(value,{status});}
class InputError extends Error {constructor(message,status=400){super(message);this.status=status;}}
const fields={title:100,description:2000,acceptance:1000,dependencies:200,assignee:39,relatedNote:300};
function changes(value,partial){
 if(!value||typeof value!=='object'||Array.isArray(value))throw new InputError('작업 내용을 확인해 주세요.');
 const allowed=new Set([...Object.keys(fields),'status','area','priority','version','archived']);
 if(Object.keys(value).some(key=>!allowed.has(key)))throw new InputError('지원하지 않는 작업 항목입니다.');
 const result={};
 for(const [name,limit] of Object.entries(fields)){
  if(partial && !(name in value))continue;
  const input=value[name]??'';
  if(typeof input!=='string'||input.trim().length>limit)throw new InputError(`${name}: 최대 ${limit}자까지 입력해 주세요.`);
  result[name]=input.trim();
 }
 if('title' in result&&!result.title)throw new InputError('작업 제목을 입력해 주세요.');
 for(const [name,options,fallback] of [['status',STATUSES,'todo'],['area',AREAS,'frontend'],['priority',PRIORITIES,'normal']]){
  if(partial&&!(name in value))continue;
  const input=value[name]??fallback;
  if(typeof input!=='string'||!Object.hasOwn(options,input))throw new InputError('상태·영역·우선순위를 확인해 주세요.');
  result[name]=input;
 }
 if(result.assignee && !/^[A-Za-z0-9][A-Za-z0-9-]{0,38}$/.test(result.assignee))throw new InputError('담당자는 GitHub 아이디로 입력해 주세요.');
 if(result.relatedNote && (!/^\/(?!\/)[^?#:\\]+\.html$/.test(result.relatedNote)||result.relatedNote.split('/').includes('..')))
  throw new InputError('관련 노트는 이 사이트의 문서를 선택해 주세요.');
 if('archived' in value){if(typeof value.archived!=='boolean')throw new InputError('보관 상태가 올바르지 않습니다.');result.archived=Number(value.archived);}
 return result;
}
async function readJson(request){
 if(!request.headers.get('Content-Type')?.toLowerCase().startsWith('application/json'))throw new InputError('JSON 요청이 필요합니다.',415);
 if(Number(request.headers.get('Content-Length'))>8192)throw new InputError('요청 내용이 너무 큽니다.',413);
 const reader=request.body?.getReader();if(!reader)throw new InputError('작업 내용이 없습니다.');
 let length=0;const chunks=[];
 while(true){const {value,done}=await reader.read();if(done)break;length+=value.length;if(length>8192){await reader.cancel();throw new InputError('요청 내용이 너무 큽니다.',413);}chunks.push(value);}
 const bytes=new Uint8Array(length);let at=0;for(const chunk of chunks){bytes.set(chunk,at);at+=chunk.length;}
 try{return JSON.parse(new TextDecoder().decode(bytes));}catch{throw new InputError('작업 내용을 읽지 못했습니다.');}
}

export class TaskBoard {
 constructor(ctx){
  this.ctx=ctx;this.sql=ctx.storage.sql;
  ctx.storage.transactionSync(()=>{
   this.sql.exec(`CREATE TABLE IF NOT EXISTS tasks (
    id TEXT PRIMARY KEY,title TEXT NOT NULL,description TEXT NOT NULL DEFAULT '',
    acceptance TEXT NOT NULL DEFAULT '',dependencies TEXT NOT NULL DEFAULT '',
    status TEXT NOT NULL,area TEXT NOT NULL,priority TEXT NOT NULL DEFAULT 'normal',
    assignee TEXT NOT NULL DEFAULT '',relatedNote TEXT NOT NULL DEFAULT '',
    archived INTEGER NOT NULL DEFAULT 0,version INTEGER NOT NULL DEFAULT 1,
    createdAt TEXT NOT NULL,updatedAt TEXT NOT NULL,updatedBy TEXT NOT NULL)`);
   this.sql.exec('CREATE TABLE IF NOT EXISTS board_meta (key TEXT PRIMARY KEY,value TEXT NOT NULL)');
   if(!this.sql.exec("SELECT value FROM board_meta WHERE key='seeded'").toArray().length){
    const now=new Date().toISOString();
    for(const [id,title,area,status,description,acceptance,dependencies,relatedNote] of seed){
     const wording={...TASK_COPY.find(item=>item.id===id)?.after,...AUDIT_TASKS.find(item=>item.id===id)?.after},assignment=ASSIGNMENT_TASKS.find(item=>item.id===id)?.after;
     this.sql.exec('INSERT OR IGNORE INTO tasks(id,title,area,status,description,acceptance,dependencies,relatedNote,createdAt,updatedAt,updatedBy,priority) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',
      id,wording?.title??title,area,status,assignment?.description??wording?.description??description,wording?.acceptance??acceptance,wording?.dependencies??dependencies,relatedNote,now,now,'초기 작업 목록',assignment?.priority??'normal');
    }
    this.sql.exec("INSERT INTO board_meta(key,value) VALUES('seeded','1')");
   }
   if(!this.sql.exec('SELECT value FROM board_meta WHERE key=?',COPY_VERSION).toArray().length){
    const report={changed:[],skipped:[]},now=new Date().toISOString();
    for(const item of TASK_COPY){
     const task=this.sql.exec('SELECT * FROM tasks WHERE id=?',item.id).toArray()[0];if(!task)continue;
     if(Object.entries(item.after).every(([key,value])=>task[key]===value))continue;
     if(task.version!==item.expectedVersion||!Object.entries(item.before).every(([key,value])=>task[key]===value)){report.skipped.push(item.id);continue;}
     this.sql.exec('UPDATE tasks SET title=?,description=?,acceptance=?,dependencies=?,version=version+1,updatedAt=?,updatedBy=? WHERE id=? AND version=?',
      item.after.title,item.after.description,item.after.acceptance,item.after.dependencies,now,'용어 정리 (2egn 요청)',item.id,item.expectedVersion);
     report.changed.push(item.id);
    }
    this.sql.exec('INSERT INTO board_meta(key,value) VALUES(?,?)',COPY_VERSION,JSON.stringify(report));
   }
   if(!this.sql.exec('SELECT value FROM board_meta WHERE key=?',AUDIT_VERSION).toArray().length){
    const report={changed:[],skipped:[],added:[]},now=new Date().toISOString();
    for(const item of AUDIT_TASKS){
     const task=this.sql.exec('SELECT * FROM tasks WHERE id=?',item.id).toArray()[0];if(!task)continue;
     if(Object.entries(item.after).every(([key,value])=>task[key]===value))continue;
     if(task.version!==item.expectedVersion||!Object.entries(item.before).every(([key,value])=>task[key]===value)){report.skipped.push(item.id);continue;}
     const keys=Object.keys(item.after);
     this.sql.exec(`UPDATE tasks SET ${keys.map(key=>key+'=?').join(',')},version=version+1,updatedAt=?,updatedBy=? WHERE id=? AND version=?`,
      ...keys.map(key=>item.after[key]),now,'브랜치 대조 반영 (2egn 요청)',item.id,item.expectedVersion);report.changed.push(item.id);
    }
    for(const item of AUDIT_ADDITIONS){
     if(this.sql.exec('SELECT id FROM tasks WHERE id=?',item.id).toArray().length)continue;
     this.sql.exec('INSERT INTO tasks(id,title,description,acceptance,dependencies,status,area,priority,relatedNote,createdAt,updatedAt,updatedBy) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',
      item.id,item.title,item.description,item.acceptance,item.dependencies,item.status,item.area,item.priority,item.relatedNote,now,now,'브랜치 대조 반영 (2egn 요청)');report.added.push(item.id);
    }
    this.sql.exec('INSERT INTO board_meta(key,value) VALUES(?,?)',AUDIT_VERSION,JSON.stringify(report));
   }
   applyAssignmentPriorities(this.sql);
   applyDb01Design(this.sql);
   applyDb02Implementation(this.sql);
   applyNov14(this.sql);
  });
 }
 async fetch(request){
  const url=new URL(request.url);const user=request.headers.get('X-Board-User');
  if(!user)return json({error:'권한 확인이 필요합니다.'},403);
  try{
   if(request.method==='GET'&&url.pathname==='/api/board'){
    const tasks=this.sql.exec('SELECT * FROM tasks ORDER BY updatedAt DESC,id ASC').toArray().map(row=>({...row,archived:!!row.archived}));
    const tree=taskTree(tasks);
    return json({tasks:tasks.map(task=>({...task,parentId:tree.parents.get(task.id)||'',dependencyWarning:tree.warnings.has(task.id)})),statuses:STATUSES,areas:AREAS,priorities:PRIORITIES});
   }
   if(request.method==='POST'&&url.pathname==='/api/board/tasks'){
    const input=changes(await readJson(request),false);const id='T-'+crypto.randomUUID().slice(0,8).toUpperCase();const now=new Date().toISOString();
    this.sql.exec(`INSERT INTO tasks(id,title,description,acceptance,dependencies,status,area,priority,assignee,relatedNote,createdAt,updatedAt,updatedBy)
     VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)`,id,input.title,input.description,input.acceptance,input.dependencies,input.status,input.area,input.priority,input.assignee,input.relatedNote,now,now,user);
    return json({task:this.sql.exec('SELECT * FROM tasks WHERE id=?',id).toArray()[0]},201);
   }
   const match=url.pathname.match(/^\/api\/board\/tasks\/([A-Za-z0-9_-]{1,64})$/);
   if(request.method==='PATCH'&&match){
    const body=await readJson(request);
    if(!body||!Number.isSafeInteger(body.version)||body.version<1)throw new InputError('현재 작업 버전이 필요합니다.');
    const values=changes(body,true);if(!Object.keys(values).length)throw new InputError('변경할 항목이 없습니다.');
    return this.ctx.storage.transactionSync(()=>{
     const current=this.sql.exec('SELECT * FROM tasks WHERE id=?',match[1]).toArray()[0];
     if(!current)throw new InputError('작업을 찾지 못했습니다.',404);
     if(current.version!==body.version)return json({error:'다른 팀원이 먼저 수정했습니다. 최신 목록을 불러온 뒤 다시 저장해 주세요.',task:current},409);
     const columns=Object.keys(values);
     this.sql.exec(`UPDATE tasks SET ${columns.map(name=>name+'=?').join(',')},version=version+1,updatedAt=?,updatedBy=? WHERE id=?`,
      ...columns.map(name=>values[name]),new Date().toISOString(),user,match[1]);
     return json({task:this.sql.exec('SELECT * FROM tasks WHERE id=?',match[1]).toArray()[0]});
    });
   }
   return json({error:'지원하지 않는 요청입니다.'},405);
  }catch(error){if(error instanceof InputError)return json({error:error.message},error.status);return json({error:'작업 저장소에 연결하지 못했습니다. 잠시 후 다시 시도해 주세요.'},503);}
 }
}
