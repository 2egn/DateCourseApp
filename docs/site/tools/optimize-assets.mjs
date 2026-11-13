import fs from 'node:fs';
import path from 'node:path';
import {createHash} from 'node:crypto';
import {createRequire} from 'node:module';

// Fewer protected requests, with no change to the site's authentication/cache policy.
export function optimizeAssets(publicDir,quartz){
 const {buildSync}=createRequire(path.join(quartz,'package.json'))('esbuild');
 const pages=[];
 function visit(dir){for(const entry of fs.readdirSync(dir,{withFileTypes:true})){const f=path.join(dir,entry.name);if(entry.isDirectory())visit(f);else if(entry.name.endsWith('.html'))pages.push({file:f,html:fs.readFileSync(f,'utf8')});}}
 visit(publicDir);
 const local=(file,href)=>{
  if(/^(?:https?:)?\/\//.test(href))return null;
  const result=path.resolve(href.startsWith('/')?publicDir:path.dirname(file),'.'+(href.startsWith('/')?href:'/'+href));
  if(!result.startsWith(publicDir+path.sep)||!fs.existsSync(result))throw Error('Missing local asset: '+href+' in '+file);
  return result;
 };
 const stylePattern=/<link\b(?=[^>]*\brel="stylesheet")(?=[^>]*\bhref="([^"]+)")[^>]*>/g;
 const scriptPattern=/<script\b[^>]*src="([^"]+)"[^>]*>\s*<\/script>/g;
 const hash=bytes=>createHash('sha256').update(bytes).digest('hex').slice(0,12);
 const url=file=>'/'+path.relative(publicDir,file).replaceAll(path.sep,'/');
 for(const group of ['docs','board','connect']){
  const selected=pages.filter(p=>{const section=path.relative(publicDir,p.file).split(path.sep)[0];return (section==='dashboard'?'board':section==='connect'?'connect':'docs')===group;});
  const styles=new Set(),scripts=new Set();
  for(const p of selected){for(const m of p.html.matchAll(stylePattern)){const f=local(p.file,m[1]);if(f)styles.add(f);}
   for(const m of p.html.matchAll(scriptPattern)){const f=local(p.file,m[1]);if(f&&!path.basename(f).startsWith('prescript-'))scripts.add(f);}}
  const css=buildSync({stdin:{contents:[...styles].map(f=>'@import '+JSON.stringify(f.replaceAll('\\','/'))+';').join('\n'),resolveDir:publicDir,loader:'css'},bundle:true,minify:true,write:false,logLevel:'silent'}).outputFiles[0].contents;
  const cssFile=path.join(publicDir,`${group}-ui-${hash(css)}.css`);fs.writeFileSync(cssFile,css);
  const js=buildSync({stdin:{contents:[...scripts].map(f=>'await import('+JSON.stringify(f.replaceAll('\\','/'))+');').join('\n'),resolveDir:publicDir,loader:'js'},bundle:true,format:'esm',target:'es2022',minify:true,write:false,logLevel:'silent',external:['https://*','http://*']}).outputFiles[0].contents;
  const jsFile=path.join(publicDir,`${group}-runtime-${hash(js)}.js`);fs.writeFileSync(jsFile,js);
  for(const p of selected){
   let html=p.html.replace(stylePattern,(tag,href)=>local(p.file,href)?'':tag).replace(/<link\b(?=[^>]*rel="preload")(?=[^>]*href="[^"]+\.(?:css|js)")[^>]*>/g,'');
   html=html.replace(scriptPattern,(tag,src)=>{
    const file=local(p.file,src);if(!file)return tag;
    return path.basename(file).startsWith('prescript-')?`<script src="${url(file)}" data-persist></script>`:'';
   });
   html=html.replace('</head>',`<link rel="stylesheet" href="${url(cssFile)}" data-persist><script type="module" src="${url(jsFile)}" data-persist></script></head>`);
   fs.writeFileSync(p.file,html);
  }
  console.log(`${group}: ${styles.size} stylesheets → 1, ${scripts.size} script entries → 1 runtime.`);
 }
}
