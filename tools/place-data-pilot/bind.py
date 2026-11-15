"""Bind only well-supported public-source IDs. Keep provider content in memory."""
import hashlib
import json
import math
import re
import urllib.parse
from collections import Counter
from datetime import datetime, timezone

import pilot

CATALOG = pilot.OUTPUT / 'google-bindings.json'
REVIEW = pilot.OUTPUT / 'google-binding-review.json'


def match_evidence(local, remote):
    reasons = []
    if pilot.normalized_name(local['name'] + local['branch']) != pilot.normalized_name(
            remote.get('displayName', {}).get('text', '')):
        reasons.append('NAME_OR_BRANCH_DIFFERS')
    location = remote.get('location', {})
    try:
        point = (float(location['latitude']), float(location['longitude']))
        if not all(map(math.isfinite, point)) or not (-90 <= point[0] <= 90 and -180 <= point[1] <= 180):
            raise ValueError()
        if pilot.distance((float(local['lat']), float(local['lon'])), point) > 200:
            reasons.append('LOCATION_OVER_200M')
    except (KeyError, TypeError, ValueError):
        reasons.append('LOCATION_MISSING_OR_INVALID')
    address = pilot.road_building(local['road_address'])
    if not address or address != pilot.road_building(remote.get('formattedAddress', '')):
        reasons.append('ROAD_OR_BUILDING_DIFFERS')
    # District is essential: the same road/building can occur in another city.
    if '부산' not in remote.get('formattedAddress', '') or '부산진구' not in remote.get('formattedAddress', ''):
        reasons.append('CITY_OR_DISTRICT_DIFFERS')
    if remote.get('businessStatus') != 'OPERATIONAL':
        reasons.append('OPERATION_NOT_CONFIRMED')
    # This is identity validation, not assigning Google's category to our dataset.
    kinds = set(remote.get('types', []))
    expected = {'MEAL': {'restaurant', 'meal_takeaway', 'meal_delivery', 'bakery', 'food'},
                'CAFE': {'cafe', 'coffee_shop'}, 'CULTURE': {'museum', 'art_gallery',
                    'library', 'park', 'cultural_center', 'performing_arts_theater'}}[local['category']]
    if not kinds.intersection(expected):
        reasons.append('ACTIVITY_TYPE_NOT_CONFIRMED')
    return reasons


def create_catalog(observations, dataset_hash, checked_at):
    local_ids = [r['local_place_id'] for r in observations]
    if len(local_ids) != len(set(local_ids)):
        raise ValueError('DUPLICATE_LOCAL_ID')
    provider_counts = Counter(r['google_place_id'] for r in observations if r.get('google_place_id'))
    bindings, pending = [], []
    for row in observations:
        reasons = list(row['reasons'])
        if row.get('google_place_id') and provider_counts[row['google_place_id']] > 1:
            reasons.append('SHARED_PROVIDER_ID_REQUIRES_REVIEW')
        if not row.get('google_place_id') and not reasons:
            reasons.append('NOT_FOUND')
        if reasons:
            pending.append({'local_place_id': row['local_place_id'],
                            'candidate_google_place_id': row.get('google_place_id'), 'reasons': reasons})
        else:
            bindings.append({'local_place_id': row['local_place_id'],
                             'google_place_id': row['google_place_id']})
    return ({'schema_version': 1, 'dataset_sha256': dataset_hash, 'checked_at': checked_at,
             'match_rule': 'exact_normalized_name_road_building_district_within_200m_operational_activity',
             'bindings': bindings},
            {'schema_version': 1, 'dataset_sha256': dataset_hash,
             'checked_at': checked_at, 'pending': pending})


def main():
    # Do every local preflight before taking the one-time request lock.
    if CATALOG.exists() or REVIEW.exists():
        raise pilot.ProbeError('BINDING_OUTPUT_EXISTS')
    dataset = pilot.OUTPUT / 'places.csv'
    source = {r['place_id']: r for r in pilot.rows(dataset)}
    sample = [source[key] for key in pilot.SAMPLE_IDS]
    key = pilot.load_google_key()
    before = pilot.fingerprints()
    checked_at = datetime.now(timezone.utc).isoformat()
    with (pilot.ROOT / '.local/place-data-binding-live.lock').open('x', encoding='utf-8') as handle:
        handle.write(checked_at)
    probe = pilot.GoogleProbe(key)
    observations = []
    for local in sample:
        result = probe.request('POST', 'places:searchText', 'places.id', {
            'textQuery': local['name'] + ' ' + local['branch'] + ' ' + local['road_address'],
            'languageCode': 'ko', 'regionCode': 'KR', 'pageSize': 1})
        if not result.get('places'):
            observations.append({'local_place_id': local['place_id'], 'reasons': ['NOT_FOUND']})
            continue
        place_id = result['places'][0]['id']
        if not re.fullmatch(r'[A-Za-z0-9_-]+', place_id):
            raise pilot.ProbeError('INVALID_PROVIDER_ID')
        remote = probe.request('GET', 'places/' + urllib.parse.quote(place_id, safe='')
            + '?languageCode=ko&regionCode=KR',
            'id,displayName,formattedAddress,location,businessStatus,types')
        reasons = match_evidence(local, remote)
        if remote.get('id') != place_id:
            reasons.append('PROVIDER_ID_DIFFERS')
        observations.append({'local_place_id': local['place_id'],
                             'google_place_id': place_id, 'reasons': reasons})
    if before != pilot.fingerprints():
        raise pilot.ProbeError('SOURCE_CHANGED_DURING_BINDING')
    catalog, review = create_catalog(observations, hashlib.sha256(dataset.read_bytes()).hexdigest(), checked_at)
    for path, content in [(CATALOG, catalog), (REVIEW, review)]:
        with path.open('x', encoding='utf-8') as handle:
            json.dump(content, handle, ensure_ascii=False, indent=2)
    # Show local-source names + decisions only. No Google names, address or coords.
    print(json.dumps({'request_attempts': dict(probe.counts), 'matched': len(catalog['bindings']),
        'pending': len(review['pending']), 'pending_details': [dict(r, local_name=source[r['local_place_id']]['name'])
            for r in review['pending']], 'matched_local_names': [source[r['local_place_id']]['name']
            for r in catalog['bindings']], 'source_files_unchanged': True}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    try:
        main()
    except (pilot.ProbeError, FileExistsError) as failure:
        print(str(failure) if isinstance(failure, pilot.ProbeError) else 'BINDING_LOCK_OR_OUTPUT_EXISTS')
        raise SystemExit(1)
