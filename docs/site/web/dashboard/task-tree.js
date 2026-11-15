// Dependencies remain the source of truth; every task keeps its own ID and state.
export function dependencyIds(task){return [...new Set((task.dependencies||'').match(/\b(?:T-[A-F0-9]{8}|[A-Z]+-\d+)\b/g)||[])];}
export function taskTree(tasks){
 const byId=new Map(tasks.map(t=>[t.id,t])),parents=new Map(),warnings=new Set(),depths=new Map();
 const depth=(id,path=new Set())=>{
  if(path.has(id)){for(const key of path)warnings.add(key);return 0;}
  if(depths.has(id))return depths.get(id);
  const next=new Set(path).add(id),deps=dependencyIds(byId.get(id)).filter(d=>byId.has(d));
  const value=deps.length?1+Math.max(...deps.map(d=>depth(d,next))):0;depths.set(id,value);return value;
 };
 for(const task of tasks)depth(task.id);
 for(const task of tasks){
  const deps=dependencyIds(task).filter(id=>byId.has(id)&&id!==task.id&&!warnings.has(id));
  // Cross-column or cross-area prerequisites stay as links, never hidden children.
  if(deps.some(id=>{const prerequisite=byId.get(id);return prerequisite.status!==task.status||prerequisite.area!==task.area;}))continue;
  // Prefer the latest prerequisite in a chain, retaining all others as links.
  deps.sort((a,b)=>depths.get(b)-depths.get(a));
  if(!warnings.has(task.id)&&deps.length)parents.set(task.id,deps[0]);
 }
 const children=new Map(tasks.map(t=>[t.id,[]]));
 for(const task of tasks)if(parents.has(task.id))children.get(parents.get(task.id)).push(task);
 return {parents,children,roots:tasks.filter(t=>!parents.has(t.id)),warnings};
}
export function visibleTree(tasks,matches){
 const tree=taskTree(tasks),ids=new Set(matches.map(t=>t.id));
 for(const task of matches){let id=tree.parents.get(task.id);while(id){ids.add(id);id=tree.parents.get(id);}}
 return {...tree,ids,roots:tree.roots.filter(t=>ids.has(t.id))};
}
