// Serve Quartz files only after GitHub authentication and explicit repository membership.
// No public-repository visibility check may substitute for collaboration permission.
const REPOSITORY_ID = 1390711615;
const SESSION = '__Host-dca_session';
const STATE = '__Host-dca_state';
const encoder = new TextEncoder();
export {TaskBoard} from './board-store.mjs';

function base64(bytes) {
  return btoa(String.fromCharCode(...bytes)).replaceAll('+', '-').replaceAll('/', '_').replace(/=+$/, '');
}
function unbase64(value) {
  return Uint8Array.from(atob(value.replaceAll('-', '+').replaceAll('_', '/')), c => c.charCodeAt(0));
}
function cookie(request, name) {
  return request.headers.get('Cookie')?.split(';').map(c => c.trim()).find(c => c.startsWith(name + '='))?.slice(name.length + 1);
}
function setCookie(name, value, age) {
  return `${name}=${value}; Path=/; Secure; HttpOnly; SameSite=Lax; Max-Age=${age}`;
}
function stateCookieName(nonce) {
  return /^[A-Za-z0-9_-]{43}$/.test(nonce || '') ? STATE + '_' + nonce : null;
}
function documentNavigation(request, url) {
  const destination = request.headers.get('Sec-Fetch-Dest');
  if (destination && destination !== 'document') return false;
  return !url.pathname.startsWith('/static/') && !/\.(?:json|js|mjs|css|png|jpg|jpeg|gif|svg|ico|woff2?|map|txt|xml|pdf)$/i.test(url.pathname);
}
function loginFailure(reason) {
  const messages = {
    AUTH_CANCELLED: 'GitHub 로그인이 취소되었거나 승인되지 않았습니다.',
    AUTH_CALLBACK_INCOMPLETE: '로그인 응답에 필요한 정보가 없습니다.',
    AUTH_COOKIE_MISSING: '로그인을 시작한 브라우저의 확인 정보를 찾지 못했습니다. 같은 브라우저에서 다시 시작해 주세요.',
    AUTH_STATE_EXPIRED: '로그인 확인 정보가 만료되었거나 갱신되었습니다. 아래 버튼으로 다시 시작해 주세요.',
    AUTH_STATE_MISMATCH: '로그인 요청과 응답이 일치하지 않습니다. 아래 버튼으로 다시 시작해 주세요.'
  };
  return protectedResponse(`<!doctype html><html lang="ko"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>로그인 다시 시작</title><body><h1>로그인을 다시 시작해 주세요</h1><p>${messages[reason]}</p><p><a href="/auth/login">GitHub 로그인 다시 시작</a></p><small>확인 코드: ${reason}</small></body></html>`,403,{
    'Content-Type':'text/html; charset=utf-8',
    'Content-Security-Policy':"default-src 'none'; base-uri 'none'; frame-ancestors 'none'"
  });
}
function protectedResponse(body, status = 200, headers = {}) {
  const result = new Response(body, {status, headers});
  result.headers.set('Cache-Control', 'private, no-store');
  result.headers.set('X-Robots-Tag', 'noindex, nofollow, noarchive');
  result.headers.set('Referrer-Policy', 'same-origin');
  result.headers.set('X-Content-Type-Options', 'nosniff');
  return result;
}
function redirect(location, cookies = []) {
  const result = protectedResponse(null, 302, {Location: location});
  for (const item of cookies) result.headers.append('Set-Cookie', item);
  return result;
}
function returnPath(value, origin) {
  const parsed = new URL(value || '/', origin);
  return parsed.origin === origin ? parsed.pathname + parsed.search : '/';
}
async function key(env) {
  if (!/^[a-f\d]{64}$/i.test(env.SESSION_KEY || '')) throw new Error('Invalid session key');
  const bytes = Uint8Array.from(env.SESSION_KEY.match(/../g), part => parseInt(part, 16));
  return crypto.subtle.importKey('raw', bytes, 'AES-GCM', false, ['encrypt', 'decrypt']);
}
export async function seal(payload, env, purpose) {
  const iv = crypto.getRandomValues(new Uint8Array(12));
  const encrypted = await crypto.subtle.encrypt({name: 'AES-GCM', iv, additionalData: encoder.encode(purpose)},
    await key(env), encoder.encode(JSON.stringify(payload)));
  return base64(iv) + '.' + base64(new Uint8Array(encrypted));
}
export async function unseal(value, env, purpose) {
  try {
    if (!value || value.length > 3500) return null;
    const pieces = value.split('.');
    if (pieces.length !== 2) return null;
    const plain = await crypto.subtle.decrypt({name: 'AES-GCM', iv: unbase64(pieces[0]), additionalData: encoder.encode(purpose)},
      await key(env), unbase64(pieces[1]));
    const payload = JSON.parse(new TextDecoder().decode(plain));
    return Number.isFinite(payload.expires) && payload.expires > Date.now() ? payload : null;
  } catch { return null; }
}
async function github(path, token, request = fetch) {
  const response = await request('https://api.github.com' + path, {
    headers: {Accept: 'application/vnd.github+json', Authorization: 'Bearer ' + token, 'User-Agent': 'DateCourseApp-Team-Docs'},
    signal: AbortSignal.timeout(8000)
  });
  if (!response.ok) throw new Error('GitHub access check failed');
  return response.json();
}

export async function isCollaborator(token, request = fetch) {
  // /user/repos is explicitly scoped to repositories the user owns or collaborates on.
  // /repos/{owner}/{repo} alone would also succeed for every visitor of this public repo.
  for (let page = 1; page <= 100; page++) {
    const repos = await github(`/user/repos?affiliation=owner,collaborator&per_page=100&page=${page}`, token, request);
    if (!Array.isArray(repos)) throw new Error('Invalid GitHub response');
    if (repos.some(repo => repo.id === REPOSITORY_ID)) return true;
    if (repos.length < 100) return false;
  }
  return false; // Never allow access just because pagination did not finish.
}

export async function handle(request, env, remote = fetch) {
  let origin;
  try {
    origin = new URL(env.SITE_ORIGIN).origin;
    if (!origin.startsWith('https://') || !env.GITHUB_CLIENT_ID || !env.GITHUB_CLIENT_SECRET) throw new Error();
    await key(env);
  } catch { return protectedResponse('사이트 인증 설정이 준비되지 않았습니다.', 503); }
  const url = new URL(request.url);
  if (url.origin !== origin) return protectedResponse('허용되지 않은 사이트 주소입니다.', 403);
  if (url.pathname === '/auth/logout') {
    if (request.method !== 'POST' || request.headers.get('Origin') !== origin) return protectedResponse('잘못된 요청입니다.', 403);
    return redirect('/', [setCookie(SESSION, '', 0)]);
  }
  const boardApi=url.pathname==='/api/board'||url.pathname.startsWith('/api/board/');
  if (!['GET', 'HEAD'].includes(request.method) && !(boardApi && ['POST','PATCH'].includes(request.method))) return protectedResponse('지원하지 않는 요청입니다.', 405);
  try {
    if (url.pathname === '/auth/login') {
      const nonce = base64(crypto.getRandomValues(new Uint8Array(32)));
      const verifier = base64(crypto.getRandomValues(new Uint8Array(32)));
      const challenge = base64(new Uint8Array(await crypto.subtle.digest('SHA-256', encoder.encode(verifier))));
      const state = await seal({nonce, verifier, next: returnPath(url.searchParams.get('next'), origin), expires: Date.now() + 600_000}, env, STATE);
      const authorize = new URL('https://github.com/login/oauth/authorize');
      authorize.search = new URLSearchParams({client_id: env.GITHUB_CLIENT_ID, redirect_uri: origin + '/auth/callback',
        scope: '', state: nonce, code_challenge: challenge, code_challenge_method: 'S256', allow_signup: 'false'}).toString();
      return redirect(authorize.href, [setCookie(stateCookieName(nonce), state, 600)]);
    }
    if (url.pathname === '/auth/callback') {
      if (url.searchParams.get('error')) return loginFailure('AUTH_CANCELLED');
      const nonce = url.searchParams.get('state');
      if (!nonce || !url.searchParams.get('code')) return loginFailure('AUTH_CALLBACK_INCOMPLETE');
      const attemptName = stateCookieName(nonce);
      // Read the legacy single cookie only to finish sign-ins started before this update.
      const attemptCookie = attemptName && cookie(request, attemptName);
      const raw = attemptCookie || cookie(request, STATE);
      if (!raw) return loginFailure('AUTH_COOKIE_MISSING');
      const state = await unseal(raw, env, STATE);
      if (!state) return loginFailure('AUTH_STATE_EXPIRED');
      if (state.nonce !== nonce) return loginFailure('AUTH_STATE_MISMATCH');
      const tokenResponse = await remote('https://github.com/login/oauth/access_token', {
        method: 'POST', headers: {Accept: 'application/json', 'Content-Type': 'application/x-www-form-urlencoded'},
        signal: AbortSignal.timeout(8000),
        body: new URLSearchParams({client_id: env.GITHUB_CLIENT_ID, client_secret: env.GITHUB_CLIENT_SECRET,
          code: url.searchParams.get('code'), redirect_uri: origin + '/auth/callback', code_verifier: state.verifier})
      });
      if (!tokenResponse.ok) throw new Error('Token exchange failed');
      const token = await tokenResponse.json();
      if (typeof token.access_token !== 'string' || !token.access_token) throw new Error('No token');
      const user = await github('/user', token.access_token, remote);
      if (!Number.isSafeInteger(user.id)) throw new Error('Invalid user');
      if (!await isCollaborator(token.access_token, remote)) return protectedResponse('초대된 저장소 팀원만 열람할 수 있습니다.', 403);
      const session = await seal({id: user.id, login: user.login, token: token.access_token, expires: Date.now() + 3_600_000}, env, SESSION);
      return redirect(returnPath(state.next, origin), [setCookie(SESSION, session, 3600),
        setCookie(attemptCookie ? attemptName : STATE, '', 0)]);
    }
    const session = await unseal(cookie(request, SESSION), env, SESSION);
    if (!session || typeof session.token !== 'string' || !Number.isSafeInteger(session.id)) {
      if(boardApi)return protectedResponse(JSON.stringify({error:'로그인이 필요합니다.'}),401,{'Content-Type':'application/json'});
      if (!documentNavigation(request, url)) return protectedResponse('로그인이 필요합니다.',401);
      return redirect('/auth/login?next=' + encodeURIComponent(url.pathname + url.search));
    }
    // Recheck membership for every request, including attachments, JS and search JSON.
    if (!await isCollaborator(session.token, remote)) return protectedResponse('저장소 협업 권한이 없습니다.', 403);
    if(boardApi){
      if(request.method!=='GET' && request.headers.get('Origin')!==origin)
        return protectedResponse(JSON.stringify({error:'같은 사이트에서 요청해 주세요.'}),403,{'Content-Type':'application/json'});
      if(!env.BOARD)return protectedResponse('작업 저장소를 준비 중입니다.',503);
      const headers=new Headers(request.headers);
      headers.set('X-Board-User',session.login||`GitHub #${session.id}`);
      const forwarded=new Request(request,{headers});
      const board=env.BOARD.get(env.BOARD.idFromName('datecourseapp-team-board-v1'));
      const result=await board.fetch(forwarded);
      return protectedResponse(result.body,result.status,result.headers);
    }
    if(url.pathname.startsWith('/00_'))return redirect(url.pathname.replace(/^\/00_/,'/')+url.search);
    const asset = await env.ASSETS.fetch(request);
    return protectedResponse(asset.body, asset.status, asset.headers);
  } catch {
    // API failure, missing binding or malformed response must never fall back to public assets.
    return protectedResponse('권한 확인을 완료하지 못했습니다. 잠시 후 다시 시도해 주세요.', 503);
  }
}

export default {fetch: (request, env) => handle(request, env)};
