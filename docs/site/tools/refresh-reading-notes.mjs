// Editorial helper, deliberately NOT run by the normal build. Read the reviewed
// baseline, apply authored explanations, and keep executable snippets unchanged.
import fs from 'node:fs';import path from 'node:path';
const baseline=path.resolve('build/docs-before-readable'),content=path.resolve('docs/site/content');
const notes=JSON.parse(fs.readFileSync(new URL('./reading-notes.json',import.meta.url),'utf8'));
const fences=text=>[...text.matchAll(/^```[^\n]*\n[\s\S]*?^```/gm)].map(m=>m[0]);
const polite=text=>text.replaceAll('반환한다.','돌려줘요.').replaceAll('갱신한다.','새 내용으로 바꿔요.').replaceAll('초기화한다.','처음 사용할 상태로 준비해요.').replaceAll('생성한다.','만들어요.').replaceAll('전달한다.','넘겨줘요.').replaceAll('검사한다.','확인해요.').replaceAll('확인한다.','확인해요.').replaceAll('보관한다.','보관해요.').replaceAll('저장한다.','저장해요.').replaceAll('처리한다.','처리해요.').replaceAll('구성한다.','구성해요.').replaceAll('준비한다.','준비해요.').replaceAll('연결한다.','연결해요.').replaceAll('수행한다.','실행해요.').replaceAll('로드한다.','읽어 와요.').replaceAll('사용한다.','사용해요.').replaceAll('계산한다.','계산해요.').replaceAll('구분한다.','구분해요.').replaceAll('표시한다.','보여줘요.').replaceAll('묶는다.','한데 묶어요.').replaceAll('읽는다.','읽어요.').replaceAll('받는다.','받아요.').replaceAll('찾는다.','찾아요.').replaceAll('만든다.','만들어요.').replaceAll('지운다.','지워요.').replaceAll('바꾼다.','바꿔요.').replaceAll('더한다.','더해요.');
const descriptions={
 'ConditionController.loadPlaces':'화면이 멈추지 않도록 별도 작업에서 DB 장소를 읽어요. 읽기가 끝나면 화면에 성공·실패 상태를 보여줘요.',
 'ConditionController.onRecommendClick':'원래 확정 코스를 남겨 두고, 공통 조건과 현재 장소를 담은 편집용 복사본을 만들어요.',
 'ConditionController.openEditor':'편집 화면을 열면서 완료·취소할 때 나중에 실행할 동작도 함께 넘겨줘요.',
 'ConditionController.applyEdits':'사용자가 편집을 완료했을 때 새 확정 코스·예상 금액·메인 지도를 함께 바꿔요.',
 'ConditionController.cancelEdits':'편집한 내용을 적용하지 않고 원래 확정 코스가 있는 메인 화면으로 돌아가요.',
 'RouteEditorController.begin':'기존 코스의 편집을 시작해요. 완료할 때와 취소할 때 메인 화면이 할 일도 함께 받아요.',
 'RouteEditorController.draft':'지금 입력된 값과 선택한 장소들로 새 코스 정보인 RoutePlan을 만들어요.',
 'RouteEditorController.onRecommendNext':'활동 종류·1인 가격·전체 범위·이전 장소와의 거리에 맞는 다음 후보를 요청해요.',
 'RouteEditorController.onAddCandidate':'고른 장소를 넣으면서 그 장소까지 이동할 수 있는 거리도 함께 기억해요.',
 'RouteEditorController.updateButtons':'전체 범위와 장소 사이 거리 제한을 다시 확인해, 완료 버튼을 눌러도 되는지 정해요.',
 'RouteEditorController.syncSelectedRadius':'이미 추가한 장소의 거리 조건을 입력칸에 보여줘요. 보여주기만 한 동작이 값 수정으로 처리되지 않게 해요.',
 'RouteEditorController.onUpdateSelectedRadius':'현재 선택한 장소의 허용 거리만 바꾼 뒤 코스가 여전히 조건에 맞는지 확인해요.',
 'RouteEditorController.onFinish':'완료 직전에 다시 검사하고 문제가 없을 때 편집한 코스를 메인 화면에 넘겨요.',
 'RouteEditorController.onComplete.accept':'완성한 코스를 미리 등록된 완료 동작에 넘겨 메인 화면이 반영하도록 해요.',
 'RouteEditorController.onCancel':'편집본을 메인에 반영하지 않고 미리 정한 취소 동작을 실행해요.',
 'PlannerMap.render':'Java 코스를 JSON 글자로 바꿔 JavaScript 지도 함수가 읽을 수 있게 보내요.',
 'PlannerMap.ensureRangesVisible':'거리 원이 화면 밖으로 나가는지 확인하고 필요한 경우에만 확대·축소를 조정해요.',
 'NextPlaceService.candidates':'전체 허용 범위 안이면서 이전 장소와의 거리 제한도 만족하는 다음 후보를 찾아요.',
 'NextPlaceService.identity':'장소 ID가 없는 자료는 DB 행 번호인 recordId를 사용해 구분해요.',
 'NextPlaceService.samePlace':'장소 번호가 같거나 이름과 위치가 같은 경우 이미 고른 같은 장소로 판단해요.',
 'RouteRangeValidator.legs':'현재 순서에서 실제로 이어질 이동 구간을 만들어요. 순서를 바꿨다면 새 이전 장소로 계산해요.',
 'RouteRangeValidator.exceeded':'해당 구간의 직선 거리가 도착 장소에 기록된 허용 거리를 넘었는지 확인해요.',
 'RouteRangeValidator.Leg.exceeded':'해당 구간의 직선 거리가 도착 장소에 기록된 허용 거리를 넘었는지 확인해요.',
 'WalkingRouteService.leg':'두 지점 사이 걷는 길을 구해요. 같은 키·옵션·두 지점의 이전 조회 결과가 있으면 재사용해요.',
 'WalkingRouteService.parse':'TMAP에서 받은 JSON 글자를 지도 좌표·거리·시간·길 안내로 나누어 읽어요.',
 'WalkingRouteService.nonnegative':'거리나 시간이 0 이상인 정상 숫자인지 확인해요. 음수인 값은 정상 거리·시간으로 쓰면 안 돼요.',
 'WalkingRouteService.coordinate':'JSON에서 읽은 위치를 정상 범위의 위도·경도로 바꿔요.',
 'RoutePlan.RoutePlan':'장소 목록과 장소별 허용 거리 표를 복사해서 새 코스를 만들어요. 원래 코스와 편집본의 변경을 분리하려는 목적이에요.',
 'RoutePlan.totalCost':'장소마다 1인 가격에 인원을 곱한 뒤 모두 더해 예상 총금액을 구해요.',
 'MemberRepository.login':'실제 DB에서 일반 회원의 로그인 입력이 맞는지 확인하고 회원 정보를 돌려줘요.',
 'PasswordHash.create':'비밀번호마다 무작위 salt를 준비한 뒤, 원문 대신 저장할 해시 문자열을 만들어요.',
 'PasswordHash.verify':'입력한 비밀번호로 비교값을 계산해 저장된 해시와 맞는지 확인해요. 비밀번호 원문을 복원하지 않아요.',
 'DatabasePool.getConnection':'다시 쓸 수 있는 DB 연결을 빌려줘요. 작업이 끝나면 close()로 연결을 풀에 돌려줘야 해요.',
 'DbSettings.value':'환경변수와 설정 파일 중 적용할 값을 골라요. 환경변수는 프로그램을 켤 때 밖에서 지정한 설정이에요.',
 'MemberRepository.close':'다른 조회에서도 빌려 쓰던 DB 연결 풀 전체를 종료해요. 연결 하나를 반납하는 것과 달라요.',
 'PlaceRepository.findAll':'지정한 출처의 장소 행을 DB에서 읽고, JSON 값을 화면과 추천에 쓸 Place 목록으로 바꿔요.',
 'ReplaceIntegratedData.commit':'이번 교체 작업의 DB 변경을 최종 확정해요.',
 'ReplaceIntegratedData.rollback':'이번 교체 작업에서 진행한 DB 변경을 취소해요.',
 'WalkingRoute.Coordinate':'위도·경도가 정상 숫자이고 실제 좌표의 허용 범위 안인지 확인해요.',
 'Course.Course':'예전 화면용 코스 번호·이름·방문 순서·총액을 하나의 값 묶음으로 만들어요.',
 'Course.title':'Java가 record에 자동으로 제공하는 읽기 함수예요. 묶어 둔 코스 이름을 꺼내요.',
 'CoursePlace.name':'이 장소 묶음에 들어 있는 이름을 읽어요. record가 자동으로 만들어 주는 함수예요.',
 'Place.name':'이 장소의 이름을 읽어요. record가 자동으로 만들어 주는 함수예요.',
 'Member.name':'예제 회원의 이름을 읽어요. 실제 로그인 회원을 찾는 DB 조회와는 달라요.',
 'Review.content':'이미 묶어 둔 후기의 글 내용을 읽어요. 새로운 후기를 DB에서 가져오는 함수는 아니에요.',
 'UserSession.name':'로그인 후 전달받은 회원 이름을 읽어요. 비밀번호를 읽는 함수는 없어요.'
};
let count=0;
function visit(dir){for(const item of fs.readdirSync(dir,{withFileTypes:true})){
 const file=path.join(dir,item.name);if(item.isDirectory()){visit(file);continue;}if(!item.name.endsWith('.md'))continue;
 const stem=item.name.slice(0,-3),note=notes[stem];if(!note)continue;
 const original=fs.readFileSync(file,'utf8');let text=original;
 text=text.replace(/^(---\n)/,'$1readability_updated: 2026-11-13\n');
 // Preserve warnings and provenance, but remove a duplicate manual page title.
 text=text.replace(new RegExp('^# '+stem.replace(/[.*+?^${}()|[\]\\]/g,'\\$&')+'\\n','m'),'');
 if(text.includes('## 한눈에 보기'))text=text.replace(/(## 한눈에 보기\n\n)[^\n]+/,'$1'+note.overview);
 else text=text.replace(/(^> [^\n]+\n\n)[^\n]+/m,'$1## 한눈에 보기\n\n'+note.overview);
 const explanation='## 사용 장면과 읽는 순서\n\n'+note.example+'\n\n'+note.steps.map((s,i)=>(i+1)+'. '+s).join('\n')+'\n\n모르는 용어는 [[문서_읽는법|문서 읽는 법과 용어 설명]]에서 찾아볼 수 있어요.\n\n';
 text=text.replace(/(?=## (?:동작 순서|주요 메서드|흐름)\n)/,explanation);
 // Edit prose only. Java, JavaScript, commands and Mermaid remain byte-for-byte.
 text=text.split(/(^```[^\n]*\n[\s\S]*?^```)/gm).map((part,i)=>{
  if(i%2)return part;
  let prose=polite(part);
  prose=prose.replace('JDK 기본 API와 별도로 추가한 라이브러리를 구분했다. 아래 위치는 이 파일의 실제 선언·호출을 기준으로 찾았다. 생성자/필드에서 만드는 객체는 이후 이벤트 콜백에서도 사용된다.','아래 표는 가져다 쓴 도구, 이 클래스에서 만나는 함수, 사용하는 이유를 연결해 보여줘요. JDK 기본은 Java에 포함된 기능이고, 별도 라이브러리는 프로젝트가 추가로 준비하는 도구예요. 처음에는 도구 이름을 외우기보다 마지막 열의 역할부터 읽어 보세요.');
  prose=prose.replace('이 record에는 직접 작성한 처리 메서드가 없다. 생성과 자동 접근 메서드를 통한 데이터 전달 흐름을 그렸다.', ['PlaceSearchService','RouteRangeValidator'].includes(stem)?'그림에는 처리 함수와 내부 데이터 묶음(record)의 관계가 함께 나와요. 화살표가 있다고 모든 함수가 항상 한 번에 실행되는 것은 아니에요.':'이 값 묶음(record)은 생성할 때 정보를 받고, Java가 제공하는 읽기 함수로 값을 꺼내요. 그림은 이 정보 전달을 보여줘요.');
  prose=prose.replace('서로 다른 요청의 경로를 한 그림에 모았다. 위에서 아래 순서가 모든 메서드의 자동 연속 호출을 뜻하지는 않는다.','이 그림에는 서로 다른 버튼·요청에서 시작하는 흐름도 함께 있어요. 모든 함수가 위에서 아래로 무조건 한 번씩 실행된다는 뜻은 아니에요.');
  prose=prose.replace('호출·데이터 전달의 주요 경로다. 화면 이벤트와 종료 처리는 별도 진입점이며 모든 분기가 자동으로 연속 실행되는 것은 아니다.','화살표는 함수가 다른 함수에 일이나 값을 넘기는 주요 흐름이에요. 창을 열 때, 버튼을 누를 때, 종료할 때의 시작 지점이 다르므로 모든 가지가 한꺼번에 실행되지는 않아요.');
  return prose;
 }).join('');
 let inMethods=false;
 text=text.split('\n').map(line=>{
  if(line.startsWith('## '))inMethods=line.startsWith('## 주요 메서드');
  if(inMethods&&line.startsWith('|')){
   const cells=line.split('|'),name=cells[1]?.match(/`([\w.$]+)\(\)`/)?.[1],description=descriptions[stem+'.'+name];
   if(description){cells[2]=' '+description+' ';line=cells.join('|');}
  }
  if(line.startsWith('|')&&/<br\/>ResourceBundle\s*\|/.test(line)){
   const cells=line.split('|');cells[1]=' 글자·번역 자료<br/>JDK 기본: java.util<br/>ResourceBundle ';cells[cells.length-2]=' 화면 초기화에 전달되는 글자·번역 자료의 타입이에요. 이 이름만 보고 장소 목록을 검색하거나 정렬한다고 읽으면 안 돼요. ';line=cells.join('|');
  }
  if(line.startsWith('|')&&/<br\/>URL\s*\|/.test(line)){
   const cells=line.split('|');cells[1]=' 화면 파일 주소<br/>JDK 기본: java.net<br/>URL ';cells[cells.length-2]=' 화면 초기화에 전달되는 파일 위치 같은 주소를 표현하는 Java 타입이에요. URL 타입을 받는다는 것만으로 외부 브라우저를 실행하는 것은 아니에요. ';line=cells.join('|');
  }
  return line;
 }).join('\n');
 if(note.focus){const guide='## 핵심 코드를 풀어서 읽기\n\n'+note.focus+'\n\n';text=text.replace(/(?=## (?:변경할 때 주의할 점|현재 주의할 점)\n)/,guide);}
 if(JSON.stringify(fences(text))!==JSON.stringify(fences(original)))throw Error('Code fence changed: '+file);
 if(!text.includes('## 사용 장면과 읽는 순서'))throw Error('Missing reading guide: '+file);
 const dest=path.join(content,path.relative(baseline,file));fs.mkdirSync(path.dirname(dest),{recursive:true});fs.writeFileSync(dest,text);count++;
}}
visit(baseline);
if(count!==Object.keys(notes).length)throw Error('Reading note coverage mismatch: '+count);
console.log('Rewrote '+count+' class/test/demo/map notes; executable excerpts and diagrams unchanged.');
