"""Conservative identity matching, independently of suitability for a visit."""
import math
import re
import unicodedata

import pilot

RULE = 'name_address_identity_with_separate_visit_eligibility_v2'
REFINED_RULE = 'name_address_identity_with_typed_retrieval_and_safe_suffixes_v3'


def normalized(value):
    return pilot.normalized_name(unicodedata.normalize('NFKC', value))


def name_method(local, remote):
    remote = unicodedata.normalize('NFKC', remote)
    expected = normalized(local['name'] + local.get('branch', ''))
    if not expected:
        return None
    if expected == normalized(remote):
        return 'EXACT_NAME'
    # Ignore only a clearly delimited description, not a similar brand or branch.
    # A named branch must still appear before the delimiter.
    base = re.split(r'\s*[|｜]\s*', remote, maxsplit=1)[0]
    if base != remote and normalized(base) == expected:
        return 'DELIMITED_DESCRIPTION'
    bilingual = re.fullmatch(r'(.+?)\s*\(([A-Za-z][A-Za-z0-9 &._-]*)\)\s*', remote)
    if bilingual and normalized(bilingual[1]) == expected:
        return 'LATIN_NAME_SUFFIX'
    return None


def lot_building(address):
    match = re.search(r'([가-힣0-9]+(?:동|읍|면|리))\s*(산\s*)?(\d+(?:-\d+)?)', address)
    if not match:
        return None
    # Explicit local administrative/legal-dong equivalence, not arbitrary number removal.
    dong = re.sub(r'^(부전|전포|양정)제?[12]동$', r'\1동', match[1])
    return (dong, bool(match[2]), match[3])


def address_method(local, remote_address):
    # Require district and city as well as a building/lot number.
    if not re.search(r'(?:^|\s)부산(?:광역시)?(?:\s|$)', remote_address) or '부산진구' not in remote_address:
        return None
    road = pilot.road_building(local.get('road_address', ''))
    if road and road == pilot.road_building(remote_address):
        return 'ROAD_BUILDING'
    lot = lot_building(local.get('lot_address', ''))
    if lot and lot == lot_building(remote_address):
        return 'LOT_BUILDING'
    return None


def activity_matches(category, types):
    kinds = set(types)
    if category == 'MEAL':
        return bool(kinds & {'restaurant', 'meal_takeaway', 'meal_delivery', 'snack_bar', 'bakery'}
                    or any(t.endswith('_restaurant') for t in kinds))
    if category == 'CAFE':
        return bool(kinds & {'cafe', 'coffee_shop', 'tea_house', 'dessert_shop'})
    if category != 'CULTURE':
        return False
    return bool(kinds & {'museum', 'art_gallery', 'library', 'park', 'cultural_center',
                         'performing_arts_theater', 'botanical_garden', 'city_park'})


def assess(local, remote):
    name = name_method(local, remote.get('displayName', {}).get('text', ''))
    address = address_method(local, remote.get('formattedAddress', ''))
    reasons = []
    if not name:
        reasons.append('NAME_OR_BRANCH_NOT_CONFIRMED')
    if not address:
        reasons.append('ADDRESS_NOT_CONFIRMED')
    try:
        loc = remote['location']
        point = (float(loc['latitude']), float(loc['longitude']))
        if not all(map(math.isfinite, point)) or not (-90 <= point[0] <= 90 and -180 <= point[1] <= 180):
            raise ValueError()
        distance = pilot.distance((float(local['lat']), float(local['lon'])), point)
        limit = 200 if name == 'EXACT_NAME' else 50
        if distance > limit:
            reasons.append('LOCATION_NOT_CONFIRMED')
    except (KeyError, TypeError, ValueError):
        reasons.append('LOCATION_MISSING_OR_INVALID')
    if not activity_matches(local['category'], remote.get('types', [])):
        reasons.append('ACTIVITY_NOT_CONFIRMED')
    identity = not reasons
    eligible = identity and remote.get('businessStatus') == 'OPERATIONAL'
    return {'google_place_id': remote.get('id'), 'identity_match': identity,
            'visit_eligible': eligible, 'name_method': name, 'address_method': address,
            'reasons': reasons + (['VISIT_NOT_CONFIRMED'] if identity and not eligible else [])}


def decide(local_id, candidates, baseline=False):
    candidates = list({c['google_place_id']: c for c in candidates if c.get('google_place_id')}.values())
    matches = [c for c in candidates if c['identity_match']]
    if len(matches) == 1:
        choice = matches[0]
        decision = 'AUTO_CONFIRMED' if choice['visit_eligible'] else 'EXCLUDED'
        selected = choice['google_place_id']
        reasons = choice['reasons']
    elif len(matches) > 1:
        decision, selected, reasons = 'REVIEW', None, ['MULTIPLE_IDENTITIES_MATCH']
    else:
        decision, selected = ('REVIEW' if candidates else 'NOT_FOUND'), None
        reasons = sorted({reason for c in candidates for reason in c['reasons']}) or ['NO_CANDIDATES']
    return {'local_place_id': local_id, 'decision': decision, 'selected_google_place_id': selected,
            'baseline_top1_confirmed': baseline, 'reasons': reasons, 'candidates': candidates}


def block_shared_ids(outcomes):
    # Across the complete sample, never silently merge two source IDs.
    groups = {}
    for row in outcomes:
        if row['selected_google_place_id']:
            groups.setdefault(row['selected_google_place_id'], []).append(row)
    for group in groups.values():
        if len(group) > 1:
            for row in group:
                row['decision'] = 'REVIEW'
                row['selected_google_place_id'] = None
                row['reasons'] = ['SHARED_PROVIDER_ID_REQUIRES_REVIEW']
    return outcomes


def catalog(outcomes, dataset_hash, checked_at):
    bindings = [{'local_place_id': row['local_place_id'],
                 'google_place_id': row['selected_google_place_id'], 'eligibility': 'ELIGIBLE_AT_CHECK'}
                for row in outcomes if row['decision'] == 'AUTO_CONFIRMED']
    if len({r['local_place_id'] for r in outcomes}) != len(outcomes):
        raise ValueError('DUPLICATE_LOCAL_ID')
    if len({r['google_place_id'] for r in bindings}) != len(bindings):
        raise ValueError('SHARED_PROVIDER_ID')
    return {'schema_version': 2, 'match_rule': RULE, 'dataset_sha256': dataset_hash,
            'checked_at': checked_at, 'bindings': bindings}
