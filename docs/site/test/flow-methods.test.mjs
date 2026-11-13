import test from 'node:test';import assert from 'node:assert/strict';
import {enrichMethods,extractMethods} from '../tools/flow-methods.mjs';
test('existing record explanations survive repeat enrichment without duplicated method tables or qualified names',()=>{
 const source='## 주요 메서드와 흐름도 호출\n\n| 메서드 | 하는 일 |\n|---|---|\n| `SearchCondition()` | 조건을 한데 묶어요. |\n| `PlaceSearchService.recommend()` | 예전 조건으로 추천해요. |\n\n## 사용한 Java 라이브러리\n\n기본 도구\n\n```mermaid\nflowchart TD\n A["PlaceSearchService.recommend()에서 조건 읽기"]\n```\n';
 const once=enrichMethods(source,'SearchCondition',{}),twice=enrichMethods(once,'SearchCondition',{});assert.equal(once,source);assert.equal(twice,once);assert.equal((twice.match(/## 주요 메서드/g)||[]).length,1);assert.equal(extractMethods(twice)['PlaceSearchService.recommend'],'예전 조건으로 추천해요.');
});
