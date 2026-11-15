// Internal-only Durable Object. No HTTP route exposes its storage operations.
// Codes are consumed transactionally; only SHA-256 digests of bearer tokens are keys.
export class AgentAccess {
 constructor(ctx){
  this.ctx=ctx;this.sql=ctx.storage.sql;
  this.sql.exec('CREATE TABLE IF NOT EXISTS agent_records (key TEXT PRIMARY KEY,value TEXT NOT NULL,expires INTEGER NOT NULL)');
 }
 async fetch(request){
  const input=await request.json(),now=Date.now();
  this.sql.exec('DELETE FROM agent_records WHERE expires < ?',now);
  const read=key=>{const row=this.sql.exec('SELECT value FROM agent_records WHERE key=?',key).toArray()[0];return row?JSON.parse(row.value):null;};
  const put=(key,value,expires)=>this.sql.exec('INSERT OR REPLACE INTO agent_records VALUES(?,?,?)',key,JSON.stringify(value),expires);
  let result;
  this.ctx.storage.transactionSync(()=>{
   switch(input.op){
    case 'get':result=read(input.key);break;
    case 'put':put(input.key,input.value,input.expires);result=true;break;
    case 'take':result=read(input.key);this.sql.exec('DELETE FROM agent_records WHERE key=?',input.key);break;
    case 'epoch':result=read('user:'+input.userId)||{epoch:'initial'};break;
    case 'revoke':result={epoch:crypto.randomUUID(),revokedAt:now};put('user:'+input.userId,result,now+366*86400000);break;
    case 'limit':{
     const old=read(input.key)||{count:0};old.count++;put(input.key,old,input.expires);result=old.count<=input.limit;break;
    }
    case 'status':{
     const epoch=(read('user:'+input.userId)||{epoch:'initial'}).epoch;
     const rows=this.sql.exec("SELECT value FROM agent_records WHERE key LIKE 'access:%'").toArray();
     result={connections:rows.map(row=>JSON.parse(row.value)).filter(v=>v.userId===input.userId&&v.epoch===epoch)
      .map(v=>({name:v.clientName,createdAt:v.createdAt,expires:v.expires}))};break;
    }
    default:result=null;
   }
  });
  if(input.op==='put'||input.op==='limit'||input.op==='revoke'){
   if(!await this.ctx.storage.getAlarm())await this.ctx.storage.setAlarm(now+86400000);
  }
  return Response.json(result);
 }
 async alarm(){
  this.sql.exec('DELETE FROM agent_records WHERE expires < ?',Date.now());
  if(this.sql.exec('SELECT key FROM agent_records LIMIT 1').toArray().length)await this.ctx.storage.setAlarm(Date.now()+86400000);
 }
}
