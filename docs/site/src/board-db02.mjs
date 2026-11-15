// DB-02 implementation, version guarded to preserve concurrent teammate edits.
export function applyDb02Implementation(sql){
 const key='db02-implementation-2026-11-13-v1';if(sql.exec('SELECT value FROM board_meta WHERE key=?',key).toArray().length)return;
 const task=sql.exec('SELECT * FROM tasks WHERE id=?','DB-02').toArray()[0];
 const expected={"version":7,"status":"progress","description":"코스 기본 정보와 방문 장소 목록을 전부 저장하거나, 실패하면 모두 취소되도록 만들어요.\n예를 들어 코스 이름만 저장되고 장소 목록은 빠지는 일이 없어야 해요. 여러 저장을 한 묶음으로 처리하는 방식을 트랜잭션이라고 해요.\n\n[d32bc76 대조]\n편집 완료는 메모리의 appliedPlan 갱신. 코스 저장 Repository·트랜잭션은 없음. 미푸시 작업 여부는 별도 확인 필요.\n원격 브랜치에서 저장 구현이 확인되지 않아 ‘할 일’로 정리했어요. 별도 로컬 작업이 있다면 담당자와 진행 근거를 추가해 주세요.\n\n[과제 우선순위 · 필수]\n코스와 장소 목록을 실제 DB에 저장해야 핵심 흐름이 완성돼요. 여러 항목을 한 묶음으로 저장하는 기본 처리는 유지해요."};
 const after={
  "status": "done",
  "description": "SavedCourseRepository와 저장 결과 SavedCourse를 구현했어요.\n신규 저장은 코스 ID를 발급하고, 기존 저장은 로그인 회원 소유를 확인한 뒤 같은 ID로 기본 정보와 장소 목록을 갱신해요. 장소 순서·가격·NULL 반경을 보관하고, 전체·구간 거리 규칙을 저장 직전에 재검사해요.\n코스와 장소 목록은 연결 하나의 트랜잭션으로 처리하며 실패 시 전부 롤백해요. 회원 DB의 기존 연결 풀을 공유해요.\nH2 저장 검사 7개·스키마 검사 4개가 통과했고, 최신 d32bc76 별도 사본에 변경을 합친 전체 검사 125개가 통과했어요.\n실제 MySQL 접속·저장 버튼(UI-01)·앱 조회(DB-03)는 아직 연결·확인하지 않았어요. 현재 편집 완료만으로 DB가 저장되지는 않아요.",
  "relatedNote": "/05_회원_db/savedcourserepository.html"
};
 let changed=false;
 if(task&&!task.archived&&Object.entries(expected).every(([name,value])=>task[name]===value)){
  const fields=Object.keys(after);sql.exec('UPDATE tasks SET '+fields.map(f=>f+'=?').join(',')+',version=version+1,updatedAt=?,updatedBy=? WHERE id=? AND version=?',...fields.map(f=>after[f]),new Date().toISOString(),'DB-02 저장 구현 (2egn 요청)','DB-02',expected.version);changed=true;
 }
 sql.exec('INSERT INTO board_meta(key,value) VALUES(?,?)',key,JSON.stringify({changed,skipped:!changed}));
}
