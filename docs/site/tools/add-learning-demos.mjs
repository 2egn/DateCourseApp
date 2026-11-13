// Run after an intentional editorial refresh. These are teaching-only views;
// fallback Markdown remains useful to MCP clients and without JavaScript.
import fs from 'node:fs';import path from 'node:path';
const content=path.resolve('docs/site/content');
const types={
 draft:{title:'그림으로 보는 편집본과 확정본',fallback:'원래 코스를 복사해 편집하고, 취소하면 원래 코스를 유지해요. 완료했을 때만 편집 결과를 메인에 반영해요. DB 저장은 별도예요.'},
 ranges:{title:'그림으로 보는 두 거리 조건',fallback:'출발지 중심 전체 범위와 이전 장소 중심 범위에 모두 들어오는 장소만 거리 조건을 통과해요. 실제 추천은 활동·가격·중복 조건도 확인해요.'},
 async:{title:'그림으로 보는 늦게 도착한 이전 응답',fallback:'식당을 요청한 뒤 카페로 선택을 바꿨다면, 늦게 온 식당 응답을 지금 카페 화면에 적용하면 안 돼요. 응답을 반영하기 전 현재 선택과 같은 요청인지 확인해요.'},
 transaction:{title:'그림으로 보는 전체 확정과 전체 취소',fallback:'여러 DB 변경을 한 묶음으로 처리해요. 모두 성공하면 commit으로 확정하고 중간에 실패하면 rollback으로 그 묶음의 변경을 취소해요.'},
 cost:{title:'그림으로 보는 1인 가격과 총금액',fallback:'현재 RoutePlan은 장소별 1인 가격에 인원을 곱해 모두 더해요. 예전 CoursePlace에 전체 인원 금액이 이미 들어 있다면 인원을 다시 곱하면 안 돼요.'}
};
const placements=[
 ['전체_동작_흐름.md','ranges','### 장소 한 개 추천·선택'],
 ['전체_동작_흐름.md','draft','## 3. 편집본과 확정본은 어떻게 분리되는가'],
 ['전체_동작_흐름.md','async','## 5. 시간이 오래 걸리는 일과 화면 작업'],
 ['문서_읽는법.md','async','## 기다리는 작업과 화면을 바꾸는 작업'],
 ['문서_읽는법.md','transaction','## 저장·지도·테스트에서 만나는 말'],
 ['02_화면/ConditionController.md','draft','## 동작 순서'],
 ['02_화면/RouteEditorController.md','draft','## 동작 순서'],
 ['02_화면/PlannerMap.md','async','## 동작 순서'],
 ['03_처리/NextPlaceService.md','ranges','## 동작 순서'],
 ['03_처리/PlaceSearchService.md','ranges','## 동작 순서'],
 ['03_처리/RouteRangeValidator.md','ranges','## 동작 순서'],
 ['03_처리/WalkingRouteService.md','async','## 주요 메서드'],
 ['03_처리/PlaceCalculationService.md','cost','## 동작 순서'],
 ['04_데이터/RoutePlan.md','draft','## 동작 순서'],
 ['04_데이터/RoutePlan.md','cost','## 주요 메서드'],
 ['05_회원_DB/ReplaceIntegratedData.md','transaction','## 동작 순서','replace'],
 ['08_지도_보충/itinerary-rail.js.md','async','## 흐름']
];
for(const [file,type,anchor,context]of placements){
 const dest=path.join(content,file);let text=fs.readFileSync(dest,'utf8');if(text.includes('data-learning="'+type+'"'))continue;
 if(!text.includes(anchor+'\n'))throw Error('Missing demo anchor: '+file+' '+anchor);
 const d=types[type],block='## '+d.title+'\n\n'+d.fallback+'\n\n<div class="learning-demo" data-learning="'+type+'"'+(context?' data-context="'+context+'"':'')+'></div>\n\n';
 text=text.replace(anchor+'\n',block+anchor+'\n');fs.writeFileSync(dest,text);
}
console.log('Added '+placements.length+' teaching examples across '+new Set(placements.map(v=>v[0])).size+' documents.');
