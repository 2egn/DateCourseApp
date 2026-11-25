from pathlib import Path
import re,json,subprocess,hashlib,collections
R=Path.cwd();O=R/'docs/implementation-reference'
SOURCE_REF=re.search(r'소스 기준 `([^`]+)`',(O/'README.md').read_text(encoding='utf-8-sig')).group(1)
if subprocess.run(['git','diff','--quiet',SOURCE_REF,'--','src/main','build.gradle','settings.gradle','gradle.properties','tools','data/sql','data/places/expansion/current.json']).returncode!=0:raise SystemExit('기준 커밋과 구현/현재 자료가 다릅니다. 핵심 문서를 검토하고 README의 소스 기준을 갱신하세요.')
roletext='''AppLauncher|통합 실행을 회원·코스 화면으로 연결
PasswordHash|개별 salt·반복 해시 생성과 비밀번호 비교
DatabasePool|DB 연결 재사용을 위한 풀 생성
DbSettings|DB 설정의 로딩·형식 확인
DatabaseSchema|필요한 DB 구조 존재 확인
MemberRepository|가입·로그인·프로필·관리자 DB 작업
PlaceRepository|운영 카탈로그 우선 장소 조회
PlaceCatalogRepository|출처별 장소·버전·이력·보존 적재
SavedCourseRepository|회원별 코스·장소 스냅샷·미리보기 저장
InformationReportRepository|정정 제보·검토·승인값 적용
AdminPasswordReset|명시적인 관리자 비밀번호 재설정 도구
Place|장소 ID·좌표·분류·가격·상세 전달
PlaceDetails|주소·업종·연락처·출처·속성·외부 연결 보관
PriceEstimate|가격 범위·단위·근거·미확인 표현
CostEstimate|인원별 비용 범위와 미확인·추정 개수 집계
RoutePlan|사용자 계획·방문 순서·반경·수단 전달
SavedCourse|저장 코스 ID·소유자·계획·수정 시각 전달
TravelRoute|공급사가 조회한 실제 경로·구간·거리·시간 전달
TravelMode|이동수단별 반경 한도·안내 정책
NextPlaceCondition|다음 장소 조건·필터·표시 개수 전달
SearchCondition|이전 일괄 추천의 예산·분류·방문 수 조건
UserSession|로그인 회원 식별 정보 전달
Member|역할·가입일·정지 상태의 별도 회원 기본 모델
InformationReport|정정 대상·이유·검토 상태 전달
PlaceTag|확인된 특징의 표시 이름 정의
PlaceFeature|장소 속성의 종류 정의
PlaceField|항목별 출처를 구분하는 필드 정의
PlacePriceFilter|참고·추정·무료 가격 조건 판정
PlaceAttributeFact|속성의 상태·근거 전달
FactState|가능·불가능·미확인의 세 상태
SourceEvidence|출처·기준일·확인일 구분
ExternalPlaceLink|외부 ID의 승인·일치도·실제 장소 여부 구분
ExternalPlaceSnapshot|외부 상세 조회의 임시 결과·영업 구간
ProviderPriceRange|외부 가격 범위·통화 전달
WalkingSearchOption|도보 탐색 옵션 정의
NextPlaceService|조건에 맞는 다음 후보를 거리순으로 선택
PlaceSearchService|전체 범위·중복 대표 검색과 이전 일괄 추천
PlaceCalculationService|좌표 사이 구면 거리·이전 정액 계산
PlaceFacets|공공 업종을 화면 유형·상세 분류에 연결
NextPlaceRadius|전체 범위·직전 장소에 맞는 구간 반경 상한
RouteRangeValidator|완성 코스의 전체·구간 직선 반경 검사
CandidateRecoveryService|후보를 실제로 늘리는 조건 완화 안내
PlacePresenter|가격·출처·특징을 표시 문구로 변환
SavedCoursePresenter|저장 코스 합계·이동 요약 문구
PlaceMapPayload|모델에 지도 표시용 presentation을 덧붙임
TravelRouteService|이동수단에 맞는 경로 어댑터 호출
WalkingRouteService|TMAP 보행 구간 조회·파싱·짧은 캐시
KakaoRouteService|카카오 자동차·대중교통 응답 파싱
TransitWalkingRouteService|구간별 빠른 도보·대중교통 결과 선택
TmapSettings|도보 키·옵션·설정 변경 버전
KakaoRouteSettings|카카오 REST 경로 키 설정 확인
TeamApiKeys|공통 인증으로 실행 중 키·DB 설정 조회
UserContext|한 로그인의 프로필·목록·이미지·지도 준비 캐시
MapPageServer|지도 자원을 로컬 HTTP로 제공
ExternalBrowser|외부 링크를 시스템 브라우저에 연결
CameraMotionPolicy|조작 의도·화면 거리·넘침에 따른 지도 이동 정책
CoursePreviewRenderer|미리보기 렌더러 함수형 경계
CoursePreviewImage|PNG 생성·이미지 내용 지문
AppDiagnostics|실패 단계·취소·결과 진단 정보
PublicPlaceFacts|장소 파일 지문·ID 검사와 공공 속성 연결
PublicDemoPlaceRepository|내장 공공 시연 자료 읽기
PilotPlaceRepository|소규모 대조 완료 시연 자료 읽기
PlaceBindingCatalog|자료 버전과 일치하는 외부 ID 연결표 읽기
PlaceSource|장소 조회의 공통 계약
CoursePlannerController|공통 조건·확정 코스·저장·페이지 전환 조율
CourseEditorController|초안·후보·순서·반경·필터 조율
SavedCourseController|저장 목록·이미지/지도·명시적 경로 조회 조율
CoursePreviewDialog|미리보기 별도 창
InformationReportDialog|장소 정정 제보 입력
InformationReviewController|제보 목록·상세·승인·반려 화면
CourseMapView|WebView 지도·경로·캡처·비동기 수명 관리
MapEventBridge|지도 JavaScript에서 Java로 오는 이벤트 경계
MapViewportLifecycle|숨김·크기 변경 후 실제 표시 시점 감지
BasePageController|공통 비동기·로딩·실패 표시
PageFeedback|로딩·성공·오류 메시지 표시
NumberStepper|인원·가격 입력과 증감 처리
TmapSettingsDialog|도보 탐색 옵션 입력
DialogTheme|대화상자 테마 적용
ExpandableFilterPane|필터 묶음의 펼침·요약
FormScrollPane|입력 양식 스크롤
ModernScrollPane|공통 스크롤 외관·동작
PlaceFeatureSelector|활동별 특징 필터 구성
MembershipApp|Swing 회원 화면과 JavaFX 코스 화면 연결
SignupForm|회원가입 입력 양식
StartupPanel|시작 화면 표시
PlannerLoadingDialog|코스 화면 준비 표시
MyPageController|프로필·내 코스 화면 연결
InputHistory|입력 이력·기억 설정 관리
LoginUI|Swing 로그인 부품·외관
CapsLockHint|비밀번호 입력 Caps Lock 안내
RevealPanel|Swing 패널 펼침 전환
AppDialogs|공통 Swing 대화상자
MemberAdminPanel|회원 검색·수정·상태·삭제 관리 화면
AdminStyle|관리자 Swing 스타일
DatabaseSetup|요청된 DB 구조 준비만 명시적으로 실행
ReplaceIntegratedData|이전 일반 데이터의 명시적 교체
PublicPlaceImport|공공 파일 쌍 검증·명시적 적재
InformationReportSetup|이전 명령에서 DB 준비 도구로 위임
CoursePreviewSetup|이전 명령에서 이미지 구조 준비로 위임
ConditionLauncher|회원 DB 없는 조건 화면 시연 실행
ConditionTestApp|조건 화면의 별도 JavaFX 시연 호스트
PilotPlannerLauncher|소규모 장소 시연 진입점
PilotPlannerApp|소규모 자료로 코스 화면 구성
PlaceBindingDemo|확정 외부 ID 연결표 오프라인 확인
KakaoRouteProbe|명시적 실제 카카오 경로 점검
CameraMotionDemoLauncher|카메라 시연 진입점
CameraMotionDemoApp|카메라 정책·지도 전환 시연'''
roles=dict(line.split('|',1) for line in roletext.splitlines())
reference_paths={x for x in subprocess.check_output(['git','ls-tree','-r','--name-only',SOURCE_REF],text=True).splitlines() if x.startswith('src/main/java/com/datecourse/') and x.endswith('.java')}
actual_paths={x.relative_to(R).as_posix() for x in (R/'src/main/java/com/datecourse').rglob('*.java')}
assert actual_paths==reference_paths,'main Java 파일 집합이 기준 커밋과 다릅니다.'
files=sorted((R/'src/main/java/com/datecourse').rglob('*.java'));assert all(p.stem in roles for p in files),[p.stem for p in files if p.stem not in roles]
capabilities={'Java record':'관련 값을 한 단위로 전달하고 필드 재할당을 제한한다. 컬렉션 복사·검증은 별도 코드 확인이 필요하다.','Java enum':'상태·정책의 경우를 명시해 분기와 표시를 일관되게 사용한다.','Java interface':'호출부와 구현의 입력·출력 계약을 분리한다.','JavaFX':'화면 노드·속성·이벤트·UI 스레드 연결 기능을 사용한다.','Swing':'창·양식·이벤트 부품을 사용한다.','JDBC':'SQL 실행·값 바인딩·연결/트랜잭션 기능을 사용한다.','HikariCP':'DB 연결 생성·반환·풀 한도를 관리한다.','Jackson':'JSON과 Java 값의 읽기·쓰기·직렬화 제어를 제공한다.','HttpClient':'외부 HTTP 요청과 응답 처리를 제공한다.','HttpServer':'프로세스 안에서 로컬 지도 자원을 제공한다.','JDK 암호화':'salt 생성·반복 키 유도·해시 비교를 제공한다.','Java 표준 클래스':'메서드·컬렉션·문자열·수학·상태 처리를 조합한다.'}
def methods(text):
 result=[]
 for i,line in enumerate(text.splitlines(),1):
  clean=re.sub(r'@\w+(?:\([^)]*\))?\s*','',line).strip()
  if not re.match(r'(public|private|protected)\s',clean) or '(' not in clean:continue
  before=clean.split('(',1)[0]
  if '=' in before or re.search(r'\b(class|enum|record|interface)\b',before):continue
  name=re.search(r'(\w+)\s*$',before)
  if name and name.group(1) not in [x['name'] for x in result]:result.append({'name':name.group(1),'line':i})
 return result
def link(path,label=None):return f'[{label or Path(path).name}](../../{path})'
entries=[];index=['# 활성 Java 파일 전체 색인','','[전체 길잡이](README.md). 모든 main Java 파일을 포함한다. 기능별 핵심 설명은 앞 문서에 있고, 아래는 각 파일의 무엇·사용 요소·필요 기능·대표 처리 선언·소스 확인용이다.','','대표 선언은 접근 제한자가 있는 메서드를 자동 추출한 것이며 전체 Java AST·호출 관계 분석은 아니다. 생성자·중첩 타입·접근 제한자 없는 메서드는 직접 소스에서 확인한다. main 파일이 모두 일반 화면에서 호출된다는 뜻은 아니며 demo·maintenance는 별도 역할이다.'];last=None
for p in files:
 text=p.read_text(encoding='utf-8-sig');rel=p.relative_to(R).as_posix();name=p.stem;pkg=re.search(r'package\s+([^;]+)',text).group(1)
 if pkg!=last:index+=['',f'## {pkg}',''];last=pkg
 kind=next((k for k in ['record','enum','interface'] if re.search(r'\b'+k+r'\s+'+name+r'\b',text)),'class')
 tech=[]
 if kind!='class':tech.append('Java '+kind)
 for needle,label in [('javafx.','JavaFX'),('javax.swing.','Swing'),('java.sql.','JDBC'),('com.zaxxer.hikari','HikariCP'),('com.fasterxml.jackson','Jackson'),('java.net.http.','HttpClient'),('com.sun.net.httpserver','HttpServer'),('javax.crypto','JDK 암호화')]:
  if needle in text:tech.append(label)
 if not tech:tech=['Java 표준 클래스']
 declarations=methods(text);preferred={'CoursePlannerController':['onRecommendClick','openEditor','onSaveCourse','restoreSavedCourse'],'CourseEditorController':['onRecommendNext','onAddCandidate','begin','resume'],'SavedCourseController':['configure','loadCourses','onRequestRoute','onOpenClick'],'CourseMapView':['requestWalkingRoute','isCurrentRouteRequest','showDetail','restoreDetailFocus'],'NextPlaceService':['matching','candidates'],'PlaceSearchService':['nearby','recommend'],'PriceEstimate':['withinCap','exact','estimated','display'],'CostEstimate':['of','display','exactTotal'],'Place':['stableKey','sameIdentityAs'],'SavedCourseRepository':['persist','findAll','saveWithoutPreview','refreshPreview'],'MemberRepository':['register','login','profile','initialize'],'PasswordHash':['create','verify','derive'],'UserContext':['courses','preview','invalidateCourses','close'],'CameraMotionPolicy':['decide']}.get(name,[]);chosen=sorted(declarations,key=lambda d:preferred.index(d['name']) if d['name'] in preferred else len(preferred)+declarations.index(d))[:4];tests=[q for q in (R/'src/test/java').rglob('*.java') if q.stem==name+'Test']
 entries.append({'path':rel,'name':name,'package':pkg,'kind':kind,'role':roles[name],'technology':tech,'methods':declarations,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
 index+=['','<details>',f'<summary>{name} — {roles[name]}</summary>','',f'- **무엇:** {roles[name]}.',f'- **무엇을 사용:** {" · ".join(tech)}.','- **필요 기능·사용 효과:** '+' '.join(capabilities[t] for t in tech[:3]),'- **어떻게 확인:** '+(', '.join(f'`{m["name"]}()` (줄 {m["line"]})' for m in chosen) if chosen else '값·상태·생성자·인터페이스 선언을 확인한다.'),'- **코드:** '+link(rel),'- **검증 참조:** '+(' · '.join(link(q.relative_to(R).as_posix()) for q in tests) if tests else '[기능별 검사 안내](07-verification-and-team-site.md). 동명 테스트의 부재만으로 미검증이라고 단정하지 않는다.'),'','</details>']
(O/'08-class-index.md').write_text('\n'.join(index)+'\n',encoding='utf-8',newline='\n')
tracked=[p for p in subprocess.check_output(['git','ls-tree','-r','--name-only','-z',SOURCE_REF]).decode().split('\0') if p]
def excluded(p):
 return any(x in p.split('/') for x in ('.git','.local','.idea','.codex','.agents','.aws','build','node_modules')) or p.startswith(('docs/site/public/','docs/implementation-reference/')) or p.endswith('.iml') or Path(p).name in ('db.properties','tmap.properties','team-access.properties','kakao-config.js') or (Path(p).name.startswith('.env') and 'example' not in p)
tracked=sorted(p for p in tracked if not excluded(p))
def group(p):
 for prefix,label in [('src/main/java/','앱 Java'),('src/main/resources/','화면·지도·스타일·공개 설정 예제'),('src/test/','앱 검사'),('tools/','자료·운영 도구·검사'),('data/sql/','SQL 구조·마이그레이션'),('data/','공공 원본·기준본·검증 이력'),('archive/','보관 원본·별도 검사'),('docs/site/src/','팀 사이트 서버·공유 상태'),('docs/site/test/','사이트 로직 검사'),('docs/site/tools/','사이트 빌드·표시 검사'),('docs/site/','팀 사이트 원본·자원·설정'),('docs/','설계·검토·변경 문서')]:
  if p.startswith(prefix):return label
 return '프로젝트 기본 설정·탐색 안내'
groups=collections.defaultdict(list)
for p in tracked:groups[group(p)].append(p)
assert 'data/places/public/busan/busan_places.csv' in tracked
index=['# 프로젝트 요소 전체 색인','','[전체 길잡이](README.md). 공유 추적 파일 기준이다. 개인 값·캐시·생성물과 현재 참조 문서 묶음 자체는 제외했다. 파일 수는 구현 기능 수가 아니며 오래된 자료는 당시 기록이다.','','| 범위 | 파일 수 |','|---|---:|']
for key,ps in groups.items():index.append(f'| {key} | {len(ps)} |')
for key,ps in groups.items():
 index+=['',f'<details><summary>{key} · {len(ps)}개</summary>','']+[ '- '+link(p,p) for p in ps]+['','</details>']
(O/'09-project-index.md').write_text('\n'.join(index)+'\n',encoding='utf-8',newline='\n')
manifest={'reviewedCommit':SOURCE_REF,'date':'2026-11-25','scope':'Shared tracked project components, excluding personal settings/secrets, IDE state, Git internals, generated outputs/caches and this reference package.','mainJavaFiles':len(files),'mainJava':entries,'files':[{'path':p,'group':group(p)} for p in tracked],'counts':{k:len(ps) for k,ps in groups.items()},'reasonBasis':'Capabilities actually used and implementation effects; not undocumented historical author decisions.'}
(O/'coverage.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
print(json.dumps({'mainJava':len(files),'indexedFiles':len(tracked),'groups':{k:len(ps) for k,ps in groups.items()}},ensure_ascii=False))

# 파일별 역할을 붙여 코드 이외의 자원도 기능·기술 기준으로 찾도록 한다.
root_roles={'.gitignore':'개인 설정·키·빌드/캐시의 Git 포함 제외','AGENTS.md':'변경·검증·커밋·운영 대상 작업 규칙','README.md':'앱 기능·실행·현재 검증 안내','build.gradle':'의존성·toolchain·자원·실행·검사 작업','settings.gradle':'프로젝트 이름·JDK 다운로드 resolver','gradle.properties':'JDK 설치 탐색·환경 변수·자동 설치 정책','gradlew':'Unix 계열 Gradle Wrapper 진입점','gradlew.bat':'Windows Gradle Wrapper 진입점','gradle-wrapper.jar':'선언된 Gradle 배포를 실행하는 Wrapper','gradle-wrapper.properties':'Wrapper 배포 버전·다운로드/저장 위치','navigate.md':'소스 폴더·핵심 책임의 길잡이','폴더_안내.txt':'프로젝트 폴더 안내'}
asset_roles={'map.js':'SDK·검색·핀·반경·경로·캡처의 JavaScript 연결','map.html':'지도·검색·상세·구간 안내 DOM','map-viewport.js':'렌더링 여백·화면 좌표·출처 배치','itinerary-rail.js':'방문 목록·상세·포커스·키보드 동작','itinerary-rail.css':'방문 목록·상세 카드의 스타일','camera-motion.js':'지도 카메라 이동 실행','camera-motion-demo.js':'카메라 시연 JavaScript','camera-motion-demo.html':'카메라 시연 화면','instant-zoom.js':'화면 중심 기준 확대 연결','pin-pointer-gesture.js':'마커 포인터 조작·우선 처리','style.css':'JavaFX 공통 화면 스타일','modern-scroll.css':'JavaFX 스크롤 외관'}
other_roles={'worker.mjs':'문서/보드 라우팅·GitHub 인증·권한·보호 응답','board-store.mjs':'TaskBoard의 공유 작업·버전·SQL 저장','agent-store.mjs':'AgentAccess의 동의·토큰·읽기 권한','agent.mjs':'문서/에이전트 접근 연결 보조','app-keys.mjs':'인증된 앱에 허용 키·별도 DB 설정 전달','assignment-priorities.mjs':'시연 기준 우선순위의 보존형 초기 반영','build.mjs':'Quartz 설정·원본 처리·정적 빌드·자원 최적화','document-revisions.mjs':'본문 변경과 문서 읽음 버전 관리','flow-methods.mjs':'문서의 함수 설명·호출 단서 보강','table-links.mjs':'문서 표의 링크 정리','optimize-assets.mjs':'정적 CSS·JS 자원 최적화','compact-docs.mjs':'문서 구조·표시 보정','build_public_demo.py':'공공 장소·가격 근거·시연 스냅샷 생성','build_district_expansion.py':'기존 전체 보존·구군 확장·품질 결과 생성','place_data_quality.py':'이름·주소·좌표·기준일·중복 후보 보류','collect_busan_appeal_data.py':'공공 지정·교통·시설 근거 팩 생성','supplement_place_prices.py':'단위·메뉴·출처별 가격 보완','check_rehearsal_places.py':'시연 장소의 공급사 대조 점검','ExpansionAudit.java':'기준본·보호 데이터·명시적 적재·반복 확인','ExpansionRehearsal.java':'Java 추천의 오프라인 모의 조건 검사','RouteResourceProbe.java':'후속 후보 탐색의 별도 오프라인 실험','verify.init.gradle':'유지보수/보관 코드를 선택 검증에 연결','pilot.py':'Google 소규모 대조의 요청·원문 미보관 집계','bind.py':'공공 장소와 외부 ID 대조·검토 표 생성','matching.py':'외부 동일 장소 대조 규칙','contacts.py':'연락처·홈페이지의 동일 장소 대조','refine.py':'보류 후보 검토 자료 구성','evaluate.py':'파일럿 연결 결과 평가','team_keys.py':'파일럿 도구의 팀 키 조회'}
sql_roles={'01_create_database.sql':'기본 DB 생성','02_create_tables.sql':'이전 일반 데이터·분류 구조','05_replace_integrated_data.sql':'이전 일반 데이터 교체','06_membership.sql':'회원·관리자 구조','07_saved_courses.sql':'코스·방문 장소 구조','08_saved_course_price_estimates.sql':'가격 범위·근거 스냅샷 확장','09_place_information_reports.sql':'제보·승인 정정 구조','10_place_catalog.sql':'카탈로그·출처·적재·이력·잠금 구조','11_saved_course_previews.sql':'내용 지문·PNG 미리보기 구조','12_saved_course_transport.sql':'저장 코스 이동수단 구조','13_expand_course_ranges.sql':'기존 반경 제약 확장'}
def describe(p):
 name=Path(p).name;stem=Path(p).stem
 if p.startswith('src/main/java/'):return roles[stem]
 if p.startswith('archive/'):return '보관 원본·과거 검증: 기본 앱 기능과 구분'
 if p.startswith('src/test/') or '/test/' in p or name.startswith('test_'):return '해당 대상의 자동 회귀 검사·모의 입력; 실연동 조건은 본문 확인'
 if name in root_roles:return root_roles[name]
 if name in asset_roles:return asset_roles[name]
 if name in other_roles:return other_roles[name]
 if name in sql_roles:return sql_roles[name]
 if name.endswith('.fxml'):return 'JavaFX 화면 배치·컨트롤러·이벤트 선언'
 if 'board-' in name and name.endswith('.mjs'):return '보드의 특정 시점 반영 이력·기존 사람 수정 보존'
 if name.startswith('check-') and name.endswith('.mjs'):return '사이트의 해당 화면/동작 브라우저 점검'
 if name.startswith('refresh-') and name.endswith('.mjs'):return '문서 원본의 설명·읽기 안내 갱신 도구'
 if name.endswith('.example') or '.example.' in name:return '공유 가능한 설정 형식 예제; 개인 실제 값과 구분'
 if name in ('wrangler.jsonc','package.json','pnpm-lock.yaml'):return '사이트 배포·실행·의존성 설정'
 if name=='.gitattributes':return '텍스트 줄바꿈 고정·자료 지문 재현 보조'
 if name=='current.json':return '현재 운영 장소·속성 파일 쌍과 지문 포인터'
 if name=='places.json':return '해당 단계의 전체 장소 스냅샷'
 if 'facts' in name:return '장소 속성·시설·근거 자료'
 if 'checksum' in name or name=='manifest.json':return '원본/출력 지문·출처·수집 명세'
 if 'verification' in name:return '적재·보존·반복 또는 형식 검증 기록'
 if 'policy' in name or '/policies/' in p:return '자료 선택 범위·개수·분류 정책'
 if 'report' in name or 'review' in name:return '품질·가격·모의 결과·검토 기록'
 if p.startswith('data/') and name.endswith('.csv'):return '공공 원본·정규화·표본·연결표; 폴더 명세로 구분'
 if p.startswith('data/') and name.endswith('.sql'):return '자료 적재용 SQL 원본; 운영에 자동 재실행하지 않음'
 if name.endswith('.md'):return '설명·설계·당시 검토/변경 기록; 구현 상태는 본문 기준'
 if name.endswith(('.css','.html','.svg','.png','.ico','.woff2')):return '화면·문서의 구조·스타일·시각 자원'
 return '해당 폴더의 설정·자료·지원 자원; 원본/안내에서 범위 확인'
flow={
'프로젝트 기본 설정·탐색 안내':'무엇: 실행·빌드·공유 기준. 사용: Gradle·설정·길잡이. 왜: 환경과 진입점을 일관되게 사용. 어떻게: Wrapper → 설정/의존성 → 실행·검사.',
'앱 Java':'무엇: 실제 앱의 타입·정책·화면·DB. 사용: Java와 클래스별 기술. 왜·어떻게: 08 클래스 색인과 기능 문서의 카드에 연결.',
'화면·지도·스타일·공개 설정 예제':'무엇: 배치·웹 지도·외관·설정 형식. 사용: FXML/HTML/JS/CSS. 왜: 화면 구조·입력·표시를 연결. 어떻게: 컨트롤러·WebView·브리지에서 로딩/이벤트 처리.',
'앱 검사':'무엇: 구현 규칙의 회귀 확인. 사용: JUnit/H2/JavaFX fixture. 왜: 조건·저장·늦은 응답 오류 방지. 어떻게: 준비 입력 → 동작 → 결과/불변 조건 확인.',
'자료·운영 도구·검사':'무엇: 자료 준비·대조·명시적 운영 작업. 사용: Python/Java/Gradle. 왜: 원본과 근거·기존 데이터 보존. 어떻게: 파일 입력 → 품질/근거/지문 → 모의·사전 확인 → 명시적 적용.',
'SQL 구조·마이그레이션':'무엇: 영속 구조 정의/변경. 사용: MySQL SQL. 왜: 저장 관계·제약 보장. 어떻게: 관리자 준비와 단계별 마이그레이션, 일반 실행과 분리.',
'공공 원본·기준본·검증 이력':'무엇: 장소·가격·속성·정책·결과. 사용: CSV/JSON/SQL/명세. 왜: 출처·기존 보존·재현. 어떻게: current 포인터 → 전체 파일 쌍 → 준비 도구 → 검증 기록.',
'보관 원본·별도 검사':'무엇: 사용하지 않는 이전 코드·구조 이력. 사용: 원본 보관·선택 검사. 왜: 현재 앱과 혼동 없이 이력 유지. 어떻게: 기본 sourceSets 제외·별도 init script.',
'팀 사이트 서버·공유 상태':'무엇: 인증·보드·문서/키 응답. 사용: Worker·OAuth·Durable Object SQLite. 왜: 협업 접근과 공유 상태. 어떻게: 요청 인증/권한 → 허용 경로 → 저장/응답.',
'사이트 로직 검사':'무엇: 인증·보드·이력의 회귀. 사용: Node test·SQLite fixture. 왜: 권한과 사람의 수정 보존. 어떻게: 요청/상태 준비 → handler → 응답·DB 상태 확인.',
'사이트 빌드·표시 검사':'무엇: 문서 생성·표시 점검. 사용: Node·Quartz·Playwright. 왜: 참조성·화면 크기·링크 확인. 어떻게: 원본 가공 → 정적 빌드 → DOM/화면 검사.',
'팀 사이트 원본·자원·설정':'무엇: 팀 문서·프런트·배포 설정. 사용: Markdown·HTML/CSS/JS·설정. 왜: 구현을 함께 읽고 공유. 어떻게: 원본 → 로컬 빌드, 운영 배포는 별도.',
'설계·검토·변경 문서':'무엇: 구현 근거·설계·당시 검토. 사용: 문서·지문·검증 기록. 왜: 현재 구현과 제안을 구분. 어떻게: 기준 버전·날짜·연결 소스로 확인.'}
idx=['# 프로젝트 요소 전체 색인','','[전체 길잡이](README.md). 공유 추적 파일 기준. 개인 값·생성물·캐시와 이 참조 묶음 자체를 제외한다. 오래된 기록·보관 파일을 현행 기능으로 읽지 않는다.','','| 범위 | 파일 수 |','|---|---:|']
for key,ps in groups.items():idx.append(f'| {key} | {len(ps)} |')
for key,ps in groups.items():
 idx+=['',f'<details><summary>{key} · {len(ps)}개</summary>','',flow.get(key,''),'','| 파일 | 핵심 역할 |','|---|---|']
 for p in ps:idx.append(f'| {link(p,p)} | {describe(p)} |')
 idx+=['','</details>']
(O/'09-project-index.md').write_text('\n'.join(idx)+'\n',encoding='utf-8',newline='\n')
for item in manifest['files']:item['role']=describe(item['path'])
(O/'coverage.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')

# 값 모델의 필드와 중첩 타입도 소스 선언에서 확인한다.
index_path=O/'08-class-index.md';content=index_path.read_text(encoding='utf-8')
for entry in entries:
 text=(R/entry['path']).read_text(encoding='utf-8-sig')
 nested=[m.group(2) for m in re.finditer(r'^\s*(?:(?:public|private|protected|static|final|abstract)\s+)*(class|record|enum|interface)\s+(\w+)',text,re.M) if m.group(2)!=entry['name']]
 entry['nestedTypes']=list(dict.fromkeys(nested))
 fields=[]
 if entry['kind']=='record':
  match=re.search(r'\brecord\s+'+re.escape(entry['name'])+r'\s*\((.*?)\)\s*(?:implements[^\{]+)?\{',text,re.S)
  if match:
   signature=re.sub(r'//[^\n]*','',match.group(1));depth=0;start=0;parts=[]
   for i,c in enumerate(signature):
    if c=='<':depth+=1
    elif c=='>':depth-=1
    elif c==',' and depth==0:parts.append(signature[start:i]);start=i+1
   parts.append(signature[start:]);fields=[part.strip().split()[-1] for part in parts if part.strip()]
 entry['fields']=fields
 lines=[]
 if fields:lines.append('- **주요 값:** '+', '.join('`'+field+'`' for field in fields))
 if entry['nestedTypes']:lines.append('- **중첩 타입:** '+', '.join('`'+name+'`' for name in entry['nestedTypes']))
 if lines:content=content.replace('- **코드:** '+link(entry['path']),'\n'.join(lines)+'\n- **코드:** '+link(entry['path']))
index_path.write_text(content,encoding='utf-8',newline='\n')
(O/'coverage.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8',newline='\n')
