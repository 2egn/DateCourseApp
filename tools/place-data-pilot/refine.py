"""Four targeted follow-ups, maximum nine requests; preserve the 24-place baseline."""
import copy
import csv
import hashlib
import json
import urllib.parse
from datetime import datetime, timezone

import evaluate
import matching
import pilot

TARGET_NAMES = ['통영아구찜', '바램', '쥬바']
RESULT = pilot.OUTPUT / 'google-binding-refinement-v3.json'
CATALOG = pilot.OUTPUT / 'google-bindings-v3.json'
REVIEW_QUEUE = pilot.OUTPUT / 'binding-review-v3.csv'


def write_review_queue(outcomes, source, destination=REVIEW_QUEUE):
    labels = {'NAME_OR_BRANCH_NOT_CONFIRMED': '이름·지점 미확인',
              'ADDRESS_NOT_CONFIRMED': '주소 미확인', 'LOCATION_NOT_CONFIRMED': '좌표 거리 미확인',
              'LOCATION_MISSING_OR_INVALID': '좌표 누락·오류', 'ACTIVITY_NOT_CONFIRMED': '활동 유형 미확인',
              'VISIT_NOT_CONFIRMED': '방문 가능 여부 미확인', 'MULTIPLE_IDENTITIES_MATCH': '같은 장소 후보 여러 개',
              'DIRECT_PHONE_CONFLICT': '시설 연락처 불일치',
              'CONTACT_OR_SCOPED_WEBSITE_NOT_CONFIRMED': '연락처·개별 시설 페이지 근거 부족',
              'SHARED_PROVIDER_ID_REQUIRES_REVIEW': '여러 원본이 외부 ID를 공유', 'NO_CANDIDATES': '검색 후보 없음'}
    fields = ['장소ID', '상호', '지점', '활동', '판정', '검토사유', '우리자료주소', '후보확인링크', '확인후조치']
    with destination.open('x', encoding='utf-8-sig', newline='') as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        for row in outcomes:
            if row['decision'] == 'AUTO_CONFIRMED':
                continue
            local = source[row['local_place_id']]
            ids = list(dict.fromkeys(c['google_place_id'] for c in row['candidates']))
            writer.writerow({'장소ID': local['place_id'], '상호': local['name'], '지점': local['branch'],
                '활동': {'MEAL': '식사', 'CAFE': '카페', 'CULTURE': '문화'}[local['category']],
                '판정': '방문 제외' if row['decision'] == 'EXCLUDED' else '검토 필요',
                '검토사유': ' / '.join(labels.get(code, code) for code in row['reasons']),
                '우리자료주소': local['road_address'] or local['lot_address'],
                '후보확인링크': '\n'.join('https://www.google.com/maps/search/?api=1&query=Google&query_place_id='
                    + urllib.parse.quote(key, safe='') for key in ids),
                '확인후조치': '시연 후보에서 제외, 운영 재확인' if row['decision'] == 'EXCLUDED'
                    else '공식 홈페이지·원 출처로 같은 지점인지 확인; 미확인 상태로 강제 연결하지 않기'})


def main():
    if RESULT.exists() or CATALOG.exists():
        raise pilot.ProbeError('REFINEMENT_OUTPUT_EXISTS')
    prior = json.loads(evaluate.RESULT.read_text(encoding='utf-8'))
    if prior['error'] or prior['completed_count'] != 24:
        raise pilot.ProbeError('COMPLETE_BASELINE_REQUIRED')
    source = {r['place_id']: r for r in pilot.rows(pilot.OUTPUT / 'places.csv')}
    if hashlib.sha256((pilot.OUTPUT / 'places.csv').read_bytes()).hexdigest() != prior['dataset_sha256']:
        raise pilot.ProbeError('SOURCE_DATASET_CHANGED')
    outcomes = copy.deepcopy(prior['outcomes'])
    targets = [r for r in outcomes if source[r['local_place_id']]['name'] in TARGET_NAMES
               or r['local_place_id'] == 'SBIZ:MA0106202201A2364418']
    if len(targets) != 4:
        raise pilot.ProbeError('EXPECTED_FOUR_TARGETS')
    probe = pilot.GoogleProbe(pilot.load_google_key(), max_requests=9)
    with (pilot.ROOT / '.local/place-binding-v3-refinement.lock').open('x') as f:
        f.write(datetime.now(timezone.utc).isoformat())
    before = pilot.fingerprints()
    updates, error = [], None

    def details(place_id):
        remote = probe.request('GET', 'places/' + urllib.parse.quote(place_id, safe='')
            + '?languageCode=ko&regionCode=KR',
            'id,displayName,formattedAddress,location,businessStatus,types')
        if remote.get('id') != place_id:
            raise pilot.ProbeError('RETURNED_PLACE_ID_DIFFERS')
        return remote

    for row in targets:
        local = source[row['local_place_id']]
        try:
            if local['name'] in {'통영아구찜', '바램'}:
                candidates = [matching.assess(local, details(row['candidates'][0]['google_place_id']))]
            else:
                name = local['name'] + ' ' + local['branch']
                result = probe.request('POST', 'places:searchText', 'places.id', {
                    'textQuery': name.strip() + ' 부산 부산진구', 'languageCode': 'ko', 'regionCode': 'KR',
                    'includedType': 'cafe' if local['category'] == 'CAFE' else 'restaurant',
                    'strictTypeFiltering': True, 'pageSize': 2,
                    'locationBias': {'circle': {'center': {'latitude': float(local['lat']),
                        'longitude': float(local['lon'])}, 'radius': 500.0}}})
                ids = list(dict.fromkeys(c['id'] for c in result.get('places', [])[:2]))
                candidates = [matching.assess(local, details(key)) for key in ids]
            replacement = matching.decide(local['place_id'], candidates, row['baseline_top1_confirmed'])
            updates.append({'local_place_id': local['place_id'], 'previous_decision': row['decision'],
                            'new_decision': replacement['decision']})
            row.update(replacement)
            print(json.dumps(dict(updates[-1], local_name=local['name']), ensure_ascii=False), flush=True)
        except pilot.ProbeError as failure:
            error = str(failure)
            break
    matching.block_shared_ids(outcomes)
    if before != pilot.fingerprints():
        raise pilot.ProbeError('SOURCE_CHANGED_DURING_REFINEMENT')
    checked_at = datetime.now(timezone.utc).isoformat()
    report = {'schema_version': 3, 'dataset_sha256': prior['dataset_sha256'], 'checked_at': checked_at,
              'baseline_evaluation_sha256': hashlib.sha256(evaluate.RESULT.read_bytes()).hexdigest(),
              'request_attempts': dict(probe.counts), 'error': error, 'updates': updates, 'outcomes': outcomes,
              'source_files_unchanged': True, 'provider_payload_stored': False,
              'note': 'Targeted refinement of four cases; remaining cases retain baseline assessment; not a new independent validation'}
    with RESULT.open('x', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    if not error and len(updates) == 4:
        catalog = matching.catalog(outcomes, prior['dataset_sha256'], checked_at)
        catalog.update(schema_version=3, match_rule=matching.REFINED_RULE)
        with CATALOG.open('x', encoding='utf-8') as f:
            json.dump(catalog, f, ensure_ascii=False, indent=2)
        write_review_queue(outcomes, source)
    print(json.dumps({k: v for k, v in report.items() if k != 'outcomes'}, ensure_ascii=False, indent=2))
    if error:
        raise SystemExit(1)


if __name__ == '__main__':
    try:
        main()
    except (pilot.ProbeError, FileExistsError) as failure:
        print(str(failure) if isinstance(failure, pilot.ProbeError) else 'REFINEMENT_LOCK_OR_OUTPUT_EXISTS')
        raise SystemExit(1)
