// GFM splits unescaped pipes before Obsidian aliases are converted to links.
// Normalize links only in table rows; keep code examples and source notes intact.
export function normalizeTableLinks(markdown){
 let fence=null;
 return markdown.split('\n').map(line=>{
  const marker=line.match(/^\s*(`{3,}|~{3,})/);
  if(marker){if(!fence)fence=marker[1][0];else if(marker[1][0]===fence)fence=null;return line;}
  if(fence||!/^\s*\|/.test(line))return line;
  return line.replace(/\[\[([^\]\n]+)\]\]/g,(_,body)=>{
   const [target,...alias]=body.replaceAll('\\|','|').split('|');
   const [note,...anchor]=target.trim().split('#');
   const label=(alias.join('|')||note.split('/').at(-1)).replace(/[\\[\]|]/g,c=>c==='|'?'&#124;':'\\'+c);
   const file=note.endsWith('.md')?note:note+'.md';
   return '['+label+'](</'+file.replace(/^\/+/, '')+(anchor.length?'#'+anchor.join('#'):'')+'>)';
  });
 }).join('\n');
}
