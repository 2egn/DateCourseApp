"""Load shared provider keys into memory; never persist or print credentials."""
import json
import os
import re
import urllib.request
from pathlib import Path

ENDPOINT = 'https://datecourseapp-team-docs.ate-ourse-pp.workers.dev/api/app/keys'
_CACHE = None
_ACCESS = None

class NoRedirect(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None

def load(root, provider):
    global _CACHE, _ACCESS
    access = os.environ.get('DATECOURSE_TEAM_ACCESS_KEY', '').strip()
    path = Path(root) / '.local/team-access.properties'
    if not access and path.exists():
        for line in path.read_text(encoding='utf-8-sig').splitlines():
            if not line.lstrip().startswith(('#', '!')) and '=' in line:
                key, value = line.split('=', 1)
                if key.strip() == 'team.access.key':
                    access = value.strip()
    if not access:
        return ''
    if not re.fullmatch('[a-f0-9]{64}', access):
        raise RuntimeError('TEAM_ACCESS_KEY_INVALID')
    if _ACCESS != access or _CACHE is None:
        request = urllib.request.Request(ENDPOINT, method='POST',
            headers={'Authorization': 'Bearer ' + access, 'Accept': 'application/json',
                     'User-Agent': 'DateCourseApp/1.0'})
        try:
            with urllib.request.build_opener(NoRedirect()).open(request, timeout=8) as response:
                raw = response.read(4097)
                if len(raw) > 4096 or response.status != 200:
                    raise ValueError()
                data = json.loads(raw)
                keys = data['keys']
                if data.get('version') != 1 or any(not isinstance(keys.get(k), str)
                        or not re.fullmatch('[A-Za-z0-9_-]{1,256}', keys[k])
                        for k in ('tmap', 'kakaoJavascript', 'googlePlaces')):
                    raise ValueError()
        except Exception:
            raise RuntimeError('TEAM_KEYS_UNAVAILABLE_CHECK_ACCESS_AND_NETWORK') from None
        _CACHE = {k: keys[k] for k in ('tmap', 'kakaoJavascript', 'googlePlaces')}
        _ACCESS = access
    return _CACHE[provider]
