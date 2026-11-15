"""Frozen balanced sample, bounded requests; never save provider response content."""
import argparse
import hashlib
import json
import time
import urllib.parse
from collections import Counter
from datetime import datetime, timezone

import bind
import matching
import pilot

PLAN = pilot.OUTPUT / 'binding-sample-v2.json'
CATALOG = pilot.OUTPUT / 'google-bindings-v2.json'
RESULT = pilot.OUTPUT / 'google-binding-evaluation-v2.json'


def ranked(rows):
    return sorted(rows, key=lambda r: hashlib.sha256(('balanced-v2|' + r['place_id']).encode()).hexdigest())


def plan():
    rows = pilot.rows(pilot.OUTPUT / 'places.csv')
    indexed = {r['place_id']: r for r in rows}
    chosen = [indexed[key] for key in pilot.SAMPLE_IDS]
    seen = {r['place_id'] for r in chosen}
    # Diverse meal industries; do not select known cafes/dessert-only/cafeteria labels as a full meal.
    excluded = {'빵/도넛', '떡/한과', '아이스크림/빙수', '구내식당'}
    meal_industries = {r['industry_name'] for r in chosen if r['category'] == 'MEAL'}
    for row in ranked([r for r in rows if r['category'] == 'MEAL' and r['industry_name'] not in excluded]):
        if row['place_id'] not in seen and row['industry_name'] not in meal_industries:
            chosen.append(row); seen.add(row['place_id']); meal_industries.add(row['industry_name'])
            if sum(r['category'] == 'MEAL' for r in chosen) == 8:
                break
    cafes = ranked([r for r in rows if r['category'] == 'CAFE' and r['place_id'] not in seen])
    chosen.extend(cafes[:6]); seen.update(r['place_id'] for r in cafes[:6])
    # Three museums, three libraries, two parks. The original museum stays in the sample.
    for subcategory, count in [('박물관·미술관', 3), ('도서관', 3), ('공원', 2)]:
        needed = count - sum(r['category'] == 'CULTURE' and r['subcategory'] == subcategory for r in chosen)
        candidates = ranked([r for r in rows if r['category'] == 'CULTURE'
                             and r['subcategory'] == subcategory and r['place_id'] not in seen])
        # Duplicate source names are sampled once, not merged in the dataset.
        names = set()
        for row in candidates:
            if row['name'] in names:
                continue
            chosen.append(row); seen.add(row['place_id']); names.add(row['name']); needed -= 1
            if not needed:
                break
    counts = Counter(r['category'] for r in chosen)
    if counts != {'MEAL': 8, 'CAFE': 8, 'CULTURE': 8} or len(seen) != 24:
        raise ValueError('BALANCED_SAMPLE_NOT_AVAILABLE')
    result = {'seed': 'balanced-v2', 'selection_note': 'Original five plus hash-ranked diversity sample; frozen before API calls; not population-random',
              'dataset_sha256': hashlib.sha256((pilot.OUTPUT / 'places.csv').read_bytes()).hexdigest(),
              'rows': [{k: r[k] for k in ['place_id', 'name', 'branch', 'category', 'subcategory']} for r in chosen]}
    with PLAN.open('x', encoding='utf-8') as f:
        json.dump(result, f, ensure_ascii=False, indent=2)
    print(json.dumps({'sample_counts': counts, 'local_names': [r['name'] for r in chosen]}, ensure_ascii=False))


def live():
    if RESULT.exists() or CATALOG.exists():
        raise pilot.ProbeError('EVALUATION_OUTPUT_EXISTS')
    frozen = json.loads(PLAN.read_text(encoding='utf-8'))
    dataset_hash = hashlib.sha256((pilot.OUTPUT / 'places.csv').read_bytes()).hexdigest()
    if frozen['dataset_sha256'] != dataset_hash:
        raise pilot.ProbeError('SAMPLE_DATASET_CHANGED')
    source = {r['place_id']: r for r in pilot.rows(pilot.OUTPUT / 'places.csv')}
    sample = [source[r['place_id']] for r in frozen['rows']]
    old = json.loads(bind.CATALOG.read_text(encoding='utf-8'))
    pending = json.loads(bind.REVIEW.read_text(encoding='utf-8'))
    known = {r['local_place_id']: r['google_place_id'] for r in old['bindings']}
    known.update({r['local_place_id']: r['candidate_google_place_id'] for r in pending['pending']})
    probe = pilot.GoogleProbe(pilot.load_google_key(), max_requests=70)
    with (pilot.ROOT / '.local/place-binding-v2-evaluation.lock').open('x', encoding='utf-8') as f:
        f.write(datetime.now(timezone.utc).isoformat())
    before = pilot.fingerprints()
    started = time.monotonic()
    outcomes, error = [], None
    # Only process-lifetime memory reuse; no payload is written to disk.
    detail_cache = {}

    def details(place_id):
        if place_id not in detail_cache:
            remote = probe.request('GET', 'places/' + urllib.parse.quote(place_id, safe='')
                + '?languageCode=ko&regionCode=KR',
                'id,displayName,formattedAddress,location,businessStatus,types')
            if remote.get('id') != place_id:
                raise pilot.ProbeError('RETURNED_PLACE_ID_DIFFERS')
            detail_cache[place_id] = remote
        return detail_cache[place_id]

    for local in sample:
        try:
            assessments, baseline = [], False
            if known.get(local['place_id']):
                remote = details(known[local['place_id']])
                baseline = not bind.match_evidence(local, remote)
                assessments.append(matching.assess(local, remote))
            # Reuse a known identity; mismatched IDs get a new address-led search.
            if not any(a['identity_match'] for a in assessments):
                name = local['name'] + (' ' + local['branch'] if local['branch'] else '')
                # For previously wrong branches, let the source building anchor the search.
                if known.get(local['place_id']):
                    name = local['name']
                address = local['road_address'] or local['lot_address']
                result = probe.request('POST', 'places:searchText', 'places.id', {
                    'textQuery': address + ' ' + name, 'languageCode': 'ko', 'regionCode': 'KR', 'pageSize': 2})
                for index, candidate in enumerate(result.get('places', [])[:2]):
                    remote = details(candidate['id'])
                    if not known.get(local['place_id']) and index == 0:
                        baseline = not bind.match_evidence(local, remote)
                    assessments.append(matching.assess(local, remote))
            outcome = matching.decide(local['place_id'], assessments, baseline)
            outcomes.append(outcome)
            print(json.dumps({'progress': len(outcomes), 'of': len(sample), 'category': local['category'],
                              'local_name': local['name'], 'decision': outcome['decision']}, ensure_ascii=False), flush=True)
        except pilot.ProbeError as failure:
            error = str(failure)
            break
    matching.block_shared_ids(outcomes)
    checked_at = datetime.now(timezone.utc).isoformat()
    if before != pilot.fingerprints() or hashlib.sha256((pilot.OUTPUT / 'places.csv').read_bytes()).hexdigest() != dataset_hash:
        raise pilot.ProbeError('SOURCE_CHANGED_DURING_EVALUATION')
    report = {'schema_version': 2, 'dataset_sha256': dataset_hash, 'checked_at': checked_at,
              'sample_count': len(sample), 'completed_count': len(outcomes), 'error': error,
              'request_attempts': dict(probe.counts), 'elapsed_seconds': round(time.monotonic()-started, 2),
              'baseline_top1_confirmed': sum(r['baseline_top1_confirmed'] for r in outcomes),
              'decisions': dict(Counter(r['decision'] for r in outcomes)),
              'by_category': {category: dict(Counter(r['decision'] for r in outcomes
                  if source[r['local_place_id']]['category'] == category)) for category in ['MEAL', 'CAFE', 'CULTURE']},
              'outcomes': outcomes, 'source_files_unchanged': True,
              'provider_payload_stored': False, 'human_ground_truth_verified': False}
    with RESULT.open('x', encoding='utf-8') as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    # An incomplete run never becomes an active catalog.
    if not error and len(outcomes) == len(sample):
        with CATALOG.open('x', encoding='utf-8') as f:
            json.dump(matching.catalog(outcomes, dataset_hash, checked_at), f, ensure_ascii=False, indent=2)
    print(json.dumps({k: v for k, v in report.items() if k != 'outcomes'}, ensure_ascii=False, indent=2))
    if error:
        raise SystemExit(1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['plan', 'live'])
    args = parser.parse_args()
    try:
        plan() if args.command == 'plan' else live()
    except (pilot.ProbeError, FileExistsError) as failure:
        print(str(failure) if isinstance(failure, pilot.ProbeError) else 'EVALUATION_LOCK_OR_OUTPUT_EXISTS')
        raise SystemExit(1)
