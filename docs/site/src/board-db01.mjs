// DB-01 design deliverable, agreed in this chat. Does not mark persistence implemented.
export function applyDb01Design(sql){
 const key='db01-design-2026-11-13-v1';if(sql.exec('SELECT value FROM board_meta WHERE key=?',key).toArray().length)return;
 const task=sql.exec('SELECT * FROM tasks WHERE id=?','DB-01').toArray()[0];
 const expected={"version":9,"status":"progress","description":"회원별로 어떤 코스 정보를 저장할지 정해, 다시 열었을 때 그대로 복원할 수 있게 설계해요.\n장소 순서·인원·전체 허용 거리·장소별 이동 거리·코스 이름을 포함해요. 개발 참고: 현재 코스 정보 객체(RoutePlan)를 기준으로 하고, 이전 Course의 가격 기준과 혼동하지 않아요.\n\n[d32bc76 대조]\n배포 SQL에 코스·방문 순서·구간별 거리 저장 스키마가 없음.\n\n[과제 우선순위 · 필수]\n저장에 필요한 회원·장소 순서·인원·거리 항목을 먼저 정해야 저장과 재조회 구현을 진행할 수 있어요. 다음 구현의 시작점이에요."};
 const values={
  "status": "done",
  "description": "저장 항목표와 saved_courses·saved_course_places 두 테이블의 생성 SQL을 작성했어요.\n2026-11-13 채팅에서 현재 가격 보관, 장소 이름·좌표 스냅샷, 같은 코스 ID로 갱신하는 기준을 정했어요. RoutePlan의 장소 순서·인원·반경·분류·평점도 복원하도록 설계했어요.\nH2 메모리 DB의 스키마 검사 4개가 통과했어요. 실제 MySQL에는 실행하지 않았고, 앱의 저장·조회 기능은 아직 구현하지 않았어요.\n결과물: data/sql/07_saved_courses.sql, 코스 저장 설계 문서, SavedCourseSchemaTest.\n다음 작업 DB-02에서 저장 트랜잭션과 코스 ID 유지 처리를 구현해요.",
  "acceptance": "RoutePlan·Place·회원 ID의 저장 항목표와 생성 SQL이 있고, 장소 3개의 순서·가격·NULL 반경을 복원하는 예시 검사가 통과해요. 실제 앱 저장은 DB-02/UI-01 범위예요.",
  "dependencies": "",
  "relatedNote": "/코스_저장_설계.html"
};
 let changed=false;
 if(task&&!task.archived&&Object.entries(expected).every(([field,value])=>task[field]===value)){
  const columns=Object.keys(values);sql.exec('UPDATE tasks SET '+columns.map(c=>c+'=?').join(',')+',version=version+1,updatedAt=?,updatedBy=? WHERE id=? AND version=?',...columns.map(c=>values[c]),new Date().toISOString(),'DB-01 설계 정리 (2egn 요청)','DB-01',expected.version);changed=true;
 }
 sql.exec('INSERT INTO board_meta(key,value) VALUES(?,?)',key,JSON.stringify({changed,skipped:!changed}));
}
