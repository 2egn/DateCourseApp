// 후속 구현의 새 기록만 추가하며 기존 팀 카드와 수동 확인 상태는 보존한다.
export const NOV25_FOLLOWUP_VERSION='implementation-followup-balanced-5ecb907-2026-11-25-v2';
export const IMPLEMENTATION_REFERENCE={commit:'d5659e3',reviewedAt:'2026-11-26',scope:'master',branch:'master'};
export const NOV25_FOLLOWUP_ADDITIONS=[
 ['KEYBOARD-POLISH-25','키보드·포커스 후속 구현의 실제 PC 확인','frontend','review','[기능 브랜치 4309741 · master 미반영] 저장 장소 방향키·Enter, 내 코스 Enter·실제 행 더블클릭, DOM·JavaFX 포커스 복귀, 확대 Tab, 지연 이미지 선택·이전 변환 취소를 구현했어요. 원래 카드의 수동 확인 상태는 유지해요.','Java 398개·보관 2개 통과. 실제 Windows 연속 조작·배율 확인은 후속','KEYBOARD-UX-25','/키보드_조작과_포커스_복귀_2026_11_25.html'],
 ['ROUTE-PREF-SYNC-25','다른 지도에 도보 설정 변경 반영','backend','done','[기능 브랜치 4309741 · master 미반영] 같은 코스 재표시·늦은 응답에 도보 설정 버전을 적용하고 저장 목록의 옛 시간도 제외해요. 실제 작업 상태로 재조회 버튼을 제어하고 빈 실패 메시지를 보완했어요.','다른 지도·지연 응답·자동/명시 조회·자동차 제외·재시도 회귀 검사 통과','T-AC26320C','/도보_설정과_지도_경로_동기화_2026_11_25.html'],
 ['RECOMMEND-REUSE-25','추천의 중복 거리·장소 검사 재사용','backend','done','[기능 브랜치 4309741 · master 미반영] 전체 범위의 유효성·공공 ID 우선 중복 대표와 첫 출발지 거리를 재사용해요. 장소·조건·가격·추천 순서 규칙을 바꾸지 않았어요.','기존 2040곳의 36개 조건에서 후보·거리·순서 일치, 전체 회귀 검사 통과','T-5A59BC03','/업데이트_내역.html'],
 ['JUNGGU-98-25','중구 98곳 운영 추가·가격 보완 검증','data','done','[운영 DB 반영 완료 · 원본 4309741] 기존 2040곳을 보존해 운영 2138곳으로 늘렸어요. 신규 43곳에 기존 가격 보완 규칙을 적용했어요. 보호 테이블·기존 번호·일반 데이터·반복 적재 보존을 확인했어요. 실제 업체 영업·메뉴 검증은 별도예요.','신규 식사40·카페40·문화18, Python32·모의 코스312, 두 적재의 반복 변경 없음','CATALOG-24','/중구_98곳_추가와_가격_보완_2026_11_25.html'],
 ['BUSAN-BALANCED-25','부산 16개 구·군 분산 확장·운영 보존 검증','data','done','[운영 DB 반영 완료 · 후보 5ecb907] 미수록 11개 구·군에 각300곳, 중구190곳을 추가해 총5628곳이에요. 기존2138곳·번호·회원·저장 코스·반복 적재를 보존했어요. 기존 공공 원본 재사용이며 현장 가격·영업 검증은 별도예요.','신규3490·갱신/재활성/비활성0, Python32·모의코스696, 보호테이블9·반복5628변경없음','JUNGGU-98-25','/부산_16개_구군_분산_확장_2026_11_25.html']
];
/** 기존 ID는 담당자·의존 관계·보관·내용을 포함해 그대로 둔다. */
export function applyNov25Followup(sql){
 if(sql.exec('SELECT value FROM board_meta WHERE key=?',NOV25_FOLLOWUP_VERSION).toArray().length)return;
 const report={added:[],skipped:[]},now=new Date().toISOString();
 for(const [id,title,area,status,description,acceptance,dependencies,relatedNote]of NOV25_FOLLOWUP_ADDITIONS){
  if(sql.exec('SELECT id FROM tasks WHERE id=?',id).toArray().length){report.skipped.push(id);continue;}
  sql.exec('INSERT INTO tasks(id,title,area,status,priority,description,acceptance,dependencies,relatedNote,createdAt,updatedAt,updatedBy) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',
   id,title,area,status,'normal',description,acceptance,dependencies,relatedNote,now,now,'후속 개선 기록 (2026-11-25)');
  report.added.push(id);
 }
 sql.exec('INSERT INTO board_meta(key,value) VALUES(?,?)',NOV25_FOLLOWUP_VERSION,JSON.stringify(report));
}
