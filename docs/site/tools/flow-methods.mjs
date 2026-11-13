// Diagram summaries come from each note's method table. These additions cover
// diagram calls missing from the original short table, including named callbacks.
const extra={
 AdminController:{initialize:'예제 관리자 화면의 리뷰·회원 표를 구성하고 초기 목록을 불러온다.',loadData:'예제 리뷰·회원 데이터를 목록에 넣고 통계를 갱신한다.'},
 CommunityController:{initialize:'정렬·평점 입력과 리뷰 목록의 표시·선택 이벤트를 준비한다.'},
 SavedCourseController:{initialize:'예제 저장 코스 목록과 정렬·선택·상세 표시를 준비한다.'},
 CourseEditController:{initialize:'이전 편집 화면의 목록·버튼·금액 표시를 연결한다.',refreshAll:'이전 편집본의 장소 목록·합계·선택 상태를 다시 표시한다.'},
 ConditionController:{initialize:'인원 입력과 지도·편집 화면 전환을 준비한다.',setPlaceRepository:'로그인 과정에서 만든 장소 저장소를 받아 추천 준비에 사용한다.','Task.call':'화면을 멈추지 않도록 백그라운드에서 DB 장소 목록을 조회한다.',begin:'RouteEditorController.begin()을 호출해 확정 코스의 편집본과 완료·취소 콜백을 전달한다.'},
 MapBridge:{'selected.accept':'선택한 위도·경도를 Java 화면의 위치 선택 콜백에 전달한다.'},
 NumberStepper:{'field.setText':'범위와 표시 형식을 적용한 숫자를 입력란에 다시 표시한다.'},
 PlannerMap:{connectJavaMapBridge:'지도가 준비되면 Java 객체를 JavaScript의 연결 함수에 전달한다.','window.presentRoute':'Java에서 전달한 장소·후보·반경 정보를 지도에 표시한다.'},
 RouteEditorController:{changed:'편집 내용이 바뀐 뒤 합계·버튼·지도 표시를 갱신한다.',refreshSummary:'편집본의 누적 금액과 장소 목록·유효성 안내를 갱신한다.',onCancel:'편집본을 메인에 적용하지 않고 취소 콜백을 실행한다.','onComplete.accept':'완성한 RoutePlan을 메인 화면의 완료 콜백에 전달한다.'},
 RoutePlan:{RoutePlan:'전달받은 장소 순서와 장소별 이동 반경을 복사해 코스 데이터로 묶는다.'},
 MembershipApp:{validateStep:'현재 회원가입 단계의 입력값을 검사하고 진행 가능 여부를 반환한다.',showAdmin:'기존 Swing 관리자 회원 관리 창을 구성한다. 현재 일반 회원 로그인 흐름과는 별도다.'},
 PlaceRepository:{Place:'DB에서 읽은 장소 식별자·분류·좌표·가격·평점을 Place 객체로 묶는다.'},
 RevealPanel:{'finished.run':'펼침 애니메이션이 끝난 뒤 전달받은 후속 동작을 실행한다.'},
 StartupPanel:{doInBackground:'Swing 화면 스레드 밖에서 초기화 작업을 실행한다.',done:'초기화 결과를 화면에 반영하고 성공 시 다음 화면으로 넘긴다.','prepare.call':'외부에서 전달받은 DB 초기화 작업을 실행한다.','onReady.run':'초기화에 성공한 뒤 다음 화면으로 이동하는 콜백을 실행한다.'}
};
const api={
 launch:'JavaFX 런타임을 시작하고 Application.start()를 호출하게 한다.',
 'stage.show':'준비한 JavaFX 창을 화면에 표시한다.',
 'Desktop.browse':'운영체제의 기본 브라우저로 검증된 주소를 연다.',
 'ProcessBuilder.start':'구성한 브라우저 실행 명령으로 새 프로세스를 시작한다.',
 'MessageDigest.isEqual':'저장된 비밀번호 해시와 새로 계산한 해시의 바이트를 비교한다.',
 getConnection:'DB 연결을 요청한다. 반환받은 연결은 사용 후 닫아 반환한다.',
 commit:'트랜잭션에서 수행한 DB 변경을 최종 반영한다.',rollback:'현재 트랜잭션의 DB 변경을 취소한다.',
 filter:'Stream의 항목 중 조건을 만족하는 값만 남긴다.',map:'Stream의 각 항목을 필요한 결과 값으로 바꾼다.',stream:'목록의 항목을 순서대로 처리할 Stream을 만든다.',limit:'Stream에서 앞쪽의 정해진 개수만 남긴다.',
 repaint:'Swing이 화면을 다시 그리도록 요청한다.',revalidate:'Swing이 변경된 컴포넌트의 배치를 다시 계산하도록 요청한다.'
};
const records={
 Course:{Course:'코스 ID·제목·방문 순서의 장소 목록·총액을 하나의 record로 묶는다.',title:'record에 저장된 코스 제목을 읽는 자동 접근 메서드다.'},
 CoursePlace:{CoursePlace:'장소 ID·이름·분류·주소·좌표·가격을 이전 코스 편집 모델로 묶는다.',name:'record에 저장된 장소 이름을 읽는 자동 접근 메서드다.'},
 Member:{Member:'예제 회원 화면에서 사용할 회원 정보와 정지 여부를 묶는다.',name:'record에 저장된 회원 이름을 읽는 자동 접근 메서드다.'},
 Place:{Place:'DB 장소의 ID·분류·좌표·1인 가격·평점을 record로 묶는다.',name:'record에 저장된 장소 이름을 읽는 자동 접근 메서드다.'},
 Review:{Review:'작성자·대상 코스·평점·내용·작성일을 리뷰 record로 묶는다.',content:'record에 저장된 리뷰 내용을 읽는 자동 접근 메서드다.'},
 UserSession:{UserSession:'로그인한 회원 ID와 이름을 화면 사이에 전달할 세션 데이터로 묶는다.',name:'세션에 저장된 로그인 회원의 이름을 읽는 자동 접근 메서드다.'},
 SearchCondition:{SearchCondition:'이전 일괄 추천에서 쓸 예산·인원·분류·좌표·반경·장소 수 조건을 묶는다.','PlaceSearchService.recommend':'기존 일괄 추천 서비스가 이 record의 조건을 읽어 코스를 만든다.'}
};
export const methodNames=text=>[...new Set([...text.matchAll(/([A-Za-z_$][\w.$]*)\s*\(/g)].map(m=>m[1]))];
export function extractMethods(text){const section=text.match(/## 주요 메서드[^\n]*\n([\s\S]*?)(?=\n## |$)/)?.[1]||'';const result={};for(const line of section.split('\n')){if(!line.startsWith('|'))continue;const cells=line.split('|');if(cells.length<4)continue;for(const name of methodNames(cells[1]))result[name]=cells.slice(2,-1).join('|').replace(/<br\s*\/?>/g,' ').replace(/`/g,'').trim();}return result;}
export function enrichMethods(text,stem,all){
 if(records[stem]){
  if(stem==='Course')text=text.replaceAll('name()','title()');
  if(stem==='Review')text=text.replaceAll('name()','content()');
  if(stem==='SearchCondition')text=text.replace(/(?<![\w.])recommend\(\)에서 조건 읽기/,'PlaceSearchService.recommend()에서 조건 읽기');
  const table='## 주요 메서드와 흐름도 호출\n\n| 메서드 | 하는 일 |\n|---|---|\n'+Object.entries(records[stem]).map(([n,d])=>'| `'+n+'()` | '+d+' |').join('\n')+'\n\n';
  if(!text.includes('## 주요 메서드'))text=text.replace(/(?=## (?:코드 읽는 법|사용한 Java 라이브러리))/,(match)=>table+match);
 }
 if(!text.includes('## 주요 메서드'))return text;
 if(stem==='AppDialogs')text=text.replace('showMessage/Confirm/OptionDialog()','showMessageDialog() / showConfirmDialog() / showOptionDialog()');
 const diagrams=[...text.matchAll(/```mermaid\n([\s\S]*?)```/g)].map(m=>m[1]).join('\n'),current=extractMethods(text),additions={};
 for(const name of methodNames(diagrams)){
  if(current[name])continue;
  const description=extra[stem]?.[name]||all[name]||current[name.split('.').at(-1)]||api[name];
  if(description)additions[name]=description;
 }
 if(Object.keys(additions).length)text=text.replace(/(## 주요 메서드[^\n]*\n)([\s\S]*?)(?=\n## |$)/,(_,heading,table)=>heading+table.trimEnd()+'\n'+Object.entries(additions).map(([n,d])=>'| `'+n+'()` | '+d+' |').join('\n')+'\n');
 return text;
}
