"""Non-address identity evidence. Provider content stays in process memory."""
import copy
import argparse
import hashlib
import json
import math
import re
import urllib.parse
from collections import Counter
from datetime import datetime, timezone

import matching
import pilot
import refine

RULE = 'contact_or_scoped_website_identity_v4'
RESULT = pilot.OUTPUT / 'google-contact-evaluation-v4.json'
CATALOG = pilot.OUTPUT / 'google-bindings-v4.json'


def phone_key(value):
    # Strip extension before digits, rather than appending it to the main number.
    value = re.split(r'내선|ext\.?|[()（）#]', value or '', maxsplit=1, flags=re.I)[0]
    digits = re.sub(r'\D', '', value)
    if digits.startswith('0082'):
        digits = '0' + digits[4:]
    elif digits.startswith('82'):
        digits = '0' + digits[2:]
    return digits if digits.startswith('0') and 9 <= len(digits) <= 11 else None


def website_key(value):
    try:
        url = urllib.parse.urlsplit(value or '')
        if url.scheme not in {'http', 'https'} or not url.hostname or url.username or url.password:
            return None
        host = url.hostname.lower().removeprefix('www.')
        path = urllib.parse.unquote(url.path).rstrip('/') or '/'
        query = sorted((k, v) for k, v in urllib.parse.parse_qsl(url.query)
                       if not k.lower().startswith('utm_') and k.lower() not in {'gclid', 'fbclid'})
        # Query IDs can identify a branch. Never discard all query parameters.
        return (host, url.port if url.port not in {80, 443} else None, path, tuple(query))
    except ValueError:
        return None


def facility_type_matches(local, types):
    kinds = set(types)
    if '미술관' in local['name']:
        return 'art_gallery' in kinds
    if '박물관' in local['name']:
        return 'museum' in kinds
    return matching.activity_matches(local['category'], types)


def assess(local, remote, phone_counts):
    previous = matching.assess(local, remote)
    phone = phone_key(local.get('phone'))
    remote_phones = {phone_key(remote.get(k)) for k in ['nationalPhoneNumber', 'internationalPhoneNumber']} - {None}
    unique = phone is not None and phone_counts[phone] == 1
    phone_match = unique and phone in remote_phones
    phone_conflict = bool(unique and remote_phones and phone not in remote_phones)
    remote_site = website_key(remote.get('websiteUri'))
    # Root/brand homepages are not branch or facility identifiers.
    sites = {website_key(value) for value in [local.get('website'), *local.get('website_aliases', [])]} - {None}
    site_match = bool(remote_site in sites and remote_site and (remote_site[2] != '/' or remote_site[3]))
    verified_site_match = site_match and local.get('website_identity_verified', False)
    name = matching.name_method(local, remote.get('displayName', {}).get('text', ''))
    nearby = False
    try:
        location = remote['location']
        point = (float(location['latitude']), float(location['longitude']))
        nearby = (all(map(math.isfinite, point)) and -90 <= point[0] <= 90 and -180 <= point[1] <= 180
                  and pilot.distance((float(local['lat']), float(local['lon'])), point) <= 500)
    except (KeyError, TypeError, ValueError):
        pass
    typed = facility_type_matches(local, remote.get('types', []))
    # A scoped page alone is not a facility identity proof. Need a compatible name,
    # or both unique phone and scoped page. Different named branches never bypass name checks.
    contact_identity = (nearby and typed and not phone_conflict and
        (bool(name) and (phone_match or verified_site_match)
         or not local.get('branch') and phone_match and verified_site_match))
    reasons = []
    if phone_conflict:
        reasons.append('DIRECT_PHONE_CONFLICT')
    identity = (previous['identity_match'] or contact_identity) and not phone_conflict
    if not identity:
        reasons.extend(previous['reasons'])
        reasons.append('CONTACT_OR_SCOPED_WEBSITE_NOT_CONFIRMED')
    available = remote.get('businessStatus') == 'OPERATIONAL' and local.get('visit_available') is not False
    if identity and not available:
        reasons.append('VISIT_NOT_CONFIRMED')
    return {'google_place_id': remote.get('id'), 'identity_match': identity,
            'visit_eligible': identity and available,
            'name_method': name, 'address_method': previous['address_method'],
            'reasons': sorted(set(reasons)), 'contact_identity_match': bool(contact_identity),
            'unique_phone_match': phone_match, 'shared_source_phone': bool(phone and not unique),
            'scoped_website_match': site_match, 'verified_scoped_website_match': bool(verified_site_match)}


def apply_overrides(source, document):
    updated = copy.deepcopy(source)
    seen = set()
    for change in document['updates']:
        key = change['local_place_id']
        if key in seen or key not in updated or not change.get('source_url'):
            raise ValueError('INVALID_SOURCE_CORRECTION')
        seen.add(key)
        if any(updated[key].get(k) != value for k, value in change['expected'].items()):
            raise ValueError('CORRECTION_SOURCE_CHANGED')
        allowed = {'phone', 'website', 'website_identity_verified', 'website_aliases', 'visit_available'}
        if set(change['values']) - allowed:
            raise ValueError('CORRECTION_FIELD_NOT_ALLOWED')
        updated[key].update(change['values'])
    return updated


def reconcile():
    result_path = pilot.OUTPUT / 'google-contact-reconciled-v5.json'
    catalog_path = pilot.OUTPUT / 'google-bindings-v5.json'
    override_path = pilot.OUTPUT / 'contact-source-overrides.json'
    if result_path.exists() or catalog_path.exists():
        raise pilot.ProbeError('RECONCILIATION_OUTPUT_EXISTS')
    prior = json.loads(RESULT.read_text(encoding='utf-8'))
    overrides = json.loads(override_path.read_text(encoding='utf-8'))
    digest = hashlib.sha256((pilot.OUTPUT / 'places.csv').read_bytes()).hexdigest()
    if prior['error'] or prior['dataset_sha256'] != digest or overrides['dataset_sha256'] != digest:
        raise pilot.ProbeError('RECONCILIATION_SOURCE_CHANGED')
    override_hash = hashlib.sha256(override_path.read_bytes()).hexdigest()
    source = apply_overrides({r['place_id']: r for r in pilot.rows(pilot.OUTPUT / 'places.csv')}, overrides)
    counts = Counter(phone_key(r['phone']) for r in source.values() if phone_key(r['phone']))
    outcomes = copy.deepcopy(prior['outcomes'])
    targets = {r['local_place_id'] for r in overrides['updates']}
    probe = pilot.GoogleProbe(pilot.load_google_key(), max_requests=3)
    with (pilot.ROOT / '.local/place-binding-v5-reconciliation.lock').open('x') as handle:
        handle.write(datetime.now(timezone.utc).isoformat())
    before = pilot.fingerprints()
    updates, error = [], None
    for row in outcomes:
        if row['local_place_id'] not in targets:
            continue
        try:
            key = row['candidates'][0]['google_place_id']
            remote = probe.request('GET', 'places/' + urllib.parse.quote(key, safe='')
                + '?languageCode=ko&regionCode=KR',
                'id,displayName,formattedAddress,location,businessStatus,types,'
                'nationalPhoneNumber,internationalPhoneNumber,websiteUri')
            if remote.get('id') != key:
                raise pilot.ProbeError('RETURNED_PLACE_ID_DIFFERS')
            candidate = assess(source[row['local_place_id']], remote, counts)
            replacement = matching.decide(row['local_place_id'], [candidate], row['baseline_top1_confirmed'])
            updates.append({'local_place_id': row['local_place_id'], 'previous_decision': row['decision'],
                            'new_decision': replacement['decision']})
            row.update(replacement)
        except pilot.ProbeError as failure:
            error = str(failure)
            break
    matching.block_shared_ids(outcomes)
    if before != pilot.fingerprints() or hashlib.sha256(override_path.read_bytes()).hexdigest() != override_hash:
        raise pilot.ProbeError('SOURCE_CHANGED_DURING_RECONCILIATION')
    checked_at = datetime.now(timezone.utc).isoformat()
    report = {'schema_version': 5, 'dataset_sha256': digest, 'checked_at': checked_at,
        'verification_overrides_sha256': override_hash, 'request_attempts': dict(probe.counts),
        'updates': updates, 'outcomes': outcomes, 'error': error,
        'source_files_unchanged': True, 'provider_payload_stored': False}
    with result_path.open('x', encoding='utf-8') as handle:
        json.dump(report, handle, ensure_ascii=False, indent=2)
    if not error and len(updates) == 3:
        catalog = matching.catalog(outcomes, digest, checked_at)
        catalog.update(schema_version=5, match_rule='official_source_corrected_contact_identity_v5',
                       verification_overrides_sha256=override_hash)
        with catalog_path.open('x', encoding='utf-8') as handle:
            json.dump(catalog, handle, ensure_ascii=False, indent=2)
        refine.write_review_queue(outcomes, source, pilot.OUTPUT / 'binding-review-v5.csv')
    print(json.dumps({k: v for k, v in report.items() if k != 'outcomes'}, ensure_ascii=False, indent=2))
    print(json.dumps({'decisions': Counter(r['decision'] for r in outcomes)}, ensure_ascii=False))
    if error:
        raise SystemExit(1)


def main():
    if RESULT.exists() or CATALOG.exists():
        raise pilot.ProbeError('CONTACT_OUTPUT_EXISTS')
    source = {r['place_id']: r for r in pilot.rows(pilot.OUTPUT / 'places.csv')}
    prior = json.loads(refine.RESULT.read_text(encoding='utf-8'))
    if prior['error']:
        raise pilot.ProbeError('COMPLETE_PREVIOUS_RESULT_REQUIRED')
    dataset_hash = hashlib.sha256((pilot.OUTPUT / 'places.csv').read_bytes()).hexdigest()
    if prior['dataset_sha256'] != dataset_hash:
        raise pilot.ProbeError('SOURCE_DATASET_CHANGED')
    outcomes = copy.deepcopy(prior['outcomes'])
    targets = [r for r in outcomes if source[r['local_place_id']]['category'] == 'CULTURE']
    if len(targets) != 8:
        raise pilot.ProbeError('EXPECTED_EIGHT_CULTURE_PLACES')
    counts = Counter(phone_key(r['phone']) for r in source.values() if phone_key(r['phone']))
    probe = pilot.GoogleProbe(pilot.load_google_key(), max_requests=28)
    with (pilot.ROOT / '.local/place-binding-v4-contacts.lock').open('x', encoding='utf-8') as handle:
        handle.write(datetime.now(timezone.utc).isoformat())
    before = pilot.fingerprints()
    cache, updates, error = {}, [], None

    def details(place_id):
        if place_id not in cache:
            remote = probe.request('GET', 'places/' + urllib.parse.quote(place_id, safe='')
                + '?languageCode=ko&regionCode=KR',
                'id,displayName,formattedAddress,location,businessStatus,types,'
                'nationalPhoneNumber,internationalPhoneNumber,websiteUri')
            if remote.get('id') != place_id:
                raise pilot.ProbeError('RETURNED_PLACE_ID_DIFFERS')
            cache[place_id] = remote
        return cache[place_id]

    for row in targets:
        local = source[row['local_place_id']]
        try:
            old_id = row['selected_google_place_id'] or row['candidates'][0]['google_place_id']
            candidates = [assess(local, details(old_id), counts)]
            phone = phone_key(local['phone'])
            # No search for a number shared by parks, institutions or branches.
            searched = False
            if not candidates[0]['identity_match'] and phone and counts[phone] == 1:
                searched = True
                result = probe.request('POST', 'places:searchText', 'places.id', {
                    'textQuery': '+82 ' + phone[1:], 'regionCode': 'KR', 'languageCode': 'ko', 'pageSize': 2})
                for key in dict.fromkeys(p['id'] for p in result.get('places', [])[:2]):
                    candidates.append(assess(local, details(key), counts))
            replacement = matching.decide(local['place_id'], candidates, row['baseline_top1_confirmed'])
            updates.append({'local_place_id': local['place_id'], 'previous_decision': row['decision'],
                'new_decision': replacement['decision'], 'phone_search_used': searched,
                'non_address_identity_found': any(c['contact_identity_match'] for c in candidates),
                'shared_source_phone': bool(phone and counts[phone] > 1)})
            row.update(replacement)
            print(json.dumps(dict(updates[-1], local_name=local['name']), ensure_ascii=False), flush=True)
        except pilot.ProbeError as failure:
            error = str(failure)
            break
    matching.block_shared_ids(outcomes)
    if before != pilot.fingerprints() or hashlib.sha256((pilot.OUTPUT / 'places.csv').read_bytes()).hexdigest() != dataset_hash:
        raise pilot.ProbeError('SOURCE_CHANGED_DURING_CONTACT_TEST')
    checked_at = datetime.now(timezone.utc).isoformat()
    report = {'schema_version': 4, 'checked_at': checked_at, 'dataset_sha256': dataset_hash,
        'prior_result_sha256': hashlib.sha256(refine.RESULT.read_bytes()).hexdigest(),
        'request_attempts': dict(probe.counts), 'error': error, 'updates': updates, 'outcomes': outcomes,
        'source_files_unchanged': True, 'provider_payload_stored': False,
        'note': 'Eight culture places checked with contacts; sixteen others retain v3 assessment; no independent accuracy ground truth'}
    with RESULT.open('x', encoding='utf-8') as handle:
        json.dump(report, handle, ensure_ascii=False, indent=2)
    if not error and len(updates) == 8:
        catalog = matching.catalog(outcomes, dataset_hash, checked_at)
        catalog.update(schema_version=4, match_rule=RULE)
        with CATALOG.open('x', encoding='utf-8') as handle:
            json.dump(catalog, handle, ensure_ascii=False, indent=2)
    print(json.dumps({k: v for k, v in report.items() if k != 'outcomes'}, ensure_ascii=False, indent=2))
    if error:
        raise SystemExit(1)


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('command', choices=['live', 'reconcile'], nargs='?', default='live')
    args = parser.parse_args()
    try:
        main() if args.command == 'live' else reconcile()
    except (pilot.ProbeError, FileExistsError) as failure:
        print(str(failure) if isinstance(failure, pilot.ProbeError) else 'CONTACT_LOCK_OR_OUTPUT_EXISTS')
        raise SystemExit(1)
