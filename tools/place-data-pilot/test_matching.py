import unittest
from pathlib import Path
from tempfile import TemporaryDirectory
from unittest.mock import patch

import matching
import pilot
import refine


class MatchingTest(unittest.TestCase):
    def setUp(self):
        self.local = {'name': '예시밀면', 'branch': '', 'lat': '35.1578', 'lon': '129.0600',
                      'category': 'MEAL', 'road_address': '부산 부산진구 새싹로8번길 32',
                      'lot_address': '부산 부산진구 부전동 123-4'}
        self.remote = {'id': 'google_one', 'displayName': {'text': '예시밀면'},
                       'location': {'latitude': 35.1578, 'longitude': 129.06},
                       'formattedAddress': '부산광역시 부산진구 새싹로8번길 32',
                       'types': ['restaurant'], 'businessStatus': 'OPERATIONAL'}

    def test_delimited_description_accepted_only_with_stronger_location_evidence(self):
        remote = dict(self.remote, displayName={'text': '예시밀면 |서면 부전시장 본점'})
        result = matching.assess(self.local, remote)
        self.assertTrue(result['identity_match'])
        self.assertEqual('DELIMITED_DESCRIPTION', result['name_method'])
        with patch.object(pilot, 'distance', return_value=51):
            self.assertFalse(matching.assess(self.local, remote)['identity_match'])

    def test_similar_name_and_another_branch_never_pass(self):
        for name in ['예시밀면분점', '다른예시밀면', '예시밀면 전포점']:
            self.assertFalse(matching.assess(self.local,
                dict(self.remote, displayName={'text': name}))['identity_match'])
        local = dict(self.local, name='예시카페', branch='서면점', category='CAFE')
        self.assertIsNone(matching.name_method(local, '예시카페 |전포점'))
        self.assertIsNone(matching.name_method(local, '예시카페 전포점'))

    def test_lot_address_can_verify_identity_but_does_not_override_wrong_district(self):
        remote = dict(self.remote, formattedAddress='부산광역시 부산진구 부전동 123-4')
        result = matching.assess(self.local, remote)
        self.assertTrue(result['identity_match'])
        self.assertEqual('LOT_BUILDING', result['address_method'])
        for address in ['부산광역시 동래구 부전동 123-4', '서울 부산진구 부전동 123-4',
                        '부산광역시 부산진구 부전동 123-5']:
            self.assertFalse(matching.assess(self.local,
                dict(remote, formattedAddress=address))['identity_match'])

    def test_mountain_lot_differs_from_ordinary_lot(self):
        self.assertNotEqual(matching.lot_building('전포동 산65-3'), matching.lot_building('전포동 65-3'))

    def test_explicit_administrative_dong_equivalence_keeps_lot_number(self):
        self.assertEqual(matching.lot_building('부전제1동 123-4'), matching.lot_building('부전동 123-4'))
        self.assertNotEqual(matching.lot_building('부전제1동 123-4'), matching.lot_building('부전동 123-5'))
        self.assertNotEqual(matching.lot_building('중앙1동 123-4'), matching.lot_building('중앙동 123-4'))

    def test_latin_bilingual_suffix_is_not_permission_to_drop_a_korean_branch(self):
        remote = dict(self.remote, displayName={'text': '예시밀면 (Example Noodle)'})
        self.assertTrue(matching.assess(self.local, remote)['identity_match'])
        self.assertFalse(matching.assess(self.local,
            dict(self.remote, displayName={'text': '예시밀면 (다른지점)'}))['identity_match'])
        with patch.object(pilot, 'distance', return_value=51):
            self.assertFalse(matching.assess(self.local, remote)['identity_match'])

    def test_identity_and_operating_status_are_separate(self):
        for state in ['CLOSED_TEMPORARILY', 'CLOSED_PERMANENTLY', None]:
            assessed = matching.assess(self.local, dict(self.remote, businessStatus=state))
            self.assertTrue(assessed['identity_match'])
            self.assertFalse(assessed['visit_eligible'])
            outcome = matching.decide('A', [assessed])
            self.assertEqual('EXCLUDED', outcome['decision'])
            self.assertEqual([], matching.catalog([outcome], 'a'*64, 'now')['bindings'])

    def test_second_result_can_match_without_accepting_first_result(self):
        first = matching.assess(self.local, dict(self.remote, id='wrong',
            displayName={'text': '다른 가게'}))
        second = matching.assess(self.local, self.remote)
        result = matching.decide('A', [first, second])
        self.assertEqual('AUTO_CONFIRMED', result['decision'])
        self.assertEqual('google_one', result['selected_google_place_id'])

    def test_two_plausible_place_ids_remain_ambiguous(self):
        candidates = [matching.assess(self.local, dict(self.remote, id=key)) for key in ['one', 'two']]
        self.assertEqual('REVIEW', matching.decide('A', candidates)['decision'])

    def test_duplicate_search_results_are_not_false_ambiguity(self):
        candidate = matching.assess(self.local, self.remote)
        self.assertEqual('AUTO_CONFIRMED', matching.decide('A', [candidate, candidate])['decision'])

    def test_shared_provider_id_blocks_both_places(self):
        candidate = matching.assess(self.local, self.remote)
        outcomes = [matching.decide(key, [candidate]) for key in ['A', 'B']]
        matching.block_shared_ids(outcomes)
        self.assertTrue(all(r['decision'] == 'REVIEW' for r in outcomes))
        self.assertEqual([], matching.catalog(outcomes, 'a'*64, 'now')['bindings'])

    def test_generic_food_tag_is_insufficient(self):
        self.assertFalse(matching.assess(self.local, dict(self.remote, types=['food', 'store']))['identity_match'])

    def test_missing_nonfinite_location_and_missing_address_are_not_confirmed(self):
        for change in [{'location': {}}, {'location': {'latitude': float('nan'), 'longitude': 129.06}},
                       {'formattedAddress': ''}]:
            self.assertFalse(matching.assess(self.local, dict(self.remote, **change))['identity_match'])

    def test_confirmed_catalog_has_no_provider_content(self):
        outcome = matching.decide('A', [matching.assess(self.local, self.remote)])
        catalog = matching.catalog([outcome], 'a'*64, 'now')
        self.assertEqual([{'local_place_id': 'A', 'google_place_id': 'google_one',
                           'eligibility': 'ELIGIBLE_AT_CHECK'}], catalog['bindings'])
        with self.assertRaises(ValueError):
            matching.catalog([outcome, outcome], 'a'*64, 'now')

    def test_no_results_and_results_that_do_not_match_are_distinct(self):
        self.assertEqual('NOT_FOUND', matching.decide('A', [])['decision'])
        wrong = matching.assess(self.local, dict(self.remote, displayName={'text': '다른 곳'}))
        self.assertEqual('REVIEW', matching.decide('A', [wrong])['decision'])

    def test_configurable_request_cap_still_stops_network(self):
        probe = pilot.GoogleProbe('test', max_requests=70)
        probe.calls = 70
        with patch.object(probe.opener, 'open') as request:
            with self.assertRaises(pilot.ProbeError):
                probe.request('GET', 'places/test', 'id')
            request.assert_not_called()

    def test_review_queue_contains_only_unconfirmed_and_excluded_local_source_rows(self):
        confirmed = matching.decide('A', [matching.assess(self.local, self.remote)])
        excluded = matching.decide('B', [matching.assess(self.local,
            dict(self.remote, id='closed', businessStatus='CLOSED_TEMPORARILY'))])
        with TemporaryDirectory() as directory:
            output = Path(directory) / 'review.csv'
            source = {key: dict(self.local, place_id=key) for key in ['A', 'B']}
            refine.write_review_queue([confirmed, excluded], source, output)
            rows = pilot.rows(output)
            self.assertEqual(['B'], [r['장소ID'] for r in rows])
            self.assertEqual('방문 제외', rows[0]['판정'])
            with self.assertRaises(FileExistsError):
                refine.write_review_queue([excluded], source, output)


if __name__ == '__main__':
    unittest.main()
