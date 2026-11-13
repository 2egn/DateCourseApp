// This capability is intentionally separate from document sessions and MCP tokens.
export async function appKeys(request, env, respond) {
  const json = (value, status) => respond(JSON.stringify(value), status, {'Content-Type':'application/json; charset=utf-8'});
  if (request.method !== 'POST') return json({error:'method_not_allowed'},405);
  if (new URL(request.url).search) return json({error:'invalid_request'},400);
  const expected = env.APP_ACCESS_KEY;
  if (!/^[a-f0-9]{64}$/.test(expected || '')) return json({error:'service_unavailable'},503);
  const provided = request.headers.get('Authorization') || '';
  // Hash fixed-size credentials before comparing; never return submitted tokens or provider keys on failure.
  if (!/^Bearer [a-f0-9]{64}$/.test(provided)) return json({error:'unauthorized'},401);
  const encoder = new TextEncoder();
  const digest = async value => new Uint8Array(await crypto.subtle.digest('SHA-256',encoder.encode(value)));
  const [a,b] = await Promise.all([digest(provided.slice(7)),digest(expected)]);
  let difference=0;
  for(let i=0;i<a.length;i++) difference |= a[i]^b[i];
  if(difference) return json({error:'unauthorized'},401);
  if(new URL(request.url).pathname === '/api/app/database') {
    const database={url:env.APP_DB_URL,user:env.APP_DB_USER,password:env.APP_DB_PASSWORD};
    if(typeof database.url !== 'string' || !/^jdbc:mysql:\/\/[^\r\n]{1,4096}$/.test(database.url)
       || typeof database.user !== 'string' || !database.user.trim() || database.user.length>256
       || typeof database.password !== 'string' || !database.password.trim() || database.password.length>2048)
      return json({error:'service_unavailable'},503);
    return json({version:1,database},200);
  }
  const keys={tmap:env.TMAP_APP_KEY,kakaoJavascript:env.KAKAO_MAP_JAVASCRIPT_KEY,googlePlaces:env.GOOGLE_PLACES_API_KEY};
  if(Object.values(keys).some(value => typeof value !== 'string' || !/^[A-Za-z0-9_-]{1,256}$/.test(value)))
    return json({error:'service_unavailable'},503);
  return json({version:1,keys},200);
}
