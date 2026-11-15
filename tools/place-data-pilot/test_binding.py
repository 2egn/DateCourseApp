import unittest
import bind


class BindingTest(unittest.TestCase):
    def setUp(self):
        self.local = {'name': '예시카페', 'branch': '서면점', 'lat': '35.1578', 'lon': '129.0600',
                      'road_address': '부산광역시 부산진구 서전로 1', 'category': 'CAFE'}
        self.remote = {'displayName': {'text': '예시카페 서면점'},
                       'formattedAddress': '대한민국 부산광역시 부산진구 서전로 1',
                       'location': {'latitude': 35.1578, 'longitude': 129.06},
                       'businessStatus': 'OPERATIONAL', 'types': ['cafe', 'food']}

    def test_all_evidence_required(self):
        self.assertEqual([], bind.match_evidence(self.local, self.remote))
        for change, reason in [({'displayName': {'text': '예시카페 전포점'}}, 'NAME_OR_BRANCH_DIFFERS'),
                ({'formattedAddress': '부산 부산진구 서전로 3'}, 'ROAD_OR_BUILDING_DIFFERS'),
                ({'formattedAddress': '서울 종로구 서전로 1'}, 'CITY_OR_DISTRICT_DIFFERS'),
                ({'businessStatus': 'CLOSED_PERMANENTLY'}, 'OPERATION_NOT_CONFIRMED'),
                ({'types': ['restaurant']}, 'ACTIVITY_TYPE_NOT_CONFIRMED'),
                ({'location': {}}, 'LOCATION_MISSING_OR_INVALID'),
                ({'location': {'latitude': 35.17, 'longitude': 129.06}}, 'LOCATION_OVER_200M')]:
            with self.subTest(reason=reason):
                self.assertIn(reason, bind.match_evidence(self.local, dict(self.remote, **change)))

    def test_diagnostics_keep_simultaneous_mismatches(self):
        reasons = bind.match_evidence(self.local, dict(self.remote,
            displayName={'text': '다른 가게'}, formattedAddress='서울 서전로 3'))
        self.assertIn('NAME_OR_BRANCH_DIFFERS', reasons)
        self.assertIn('ROAD_OR_BUILDING_DIFFERS', reasons)
        self.assertIn('CITY_OR_DISTRICT_DIFFERS', reasons)

    def test_shared_remote_id_never_silently_merges_local_places(self):
        catalog, review = bind.create_catalog([
            {'local_place_id': 'A', 'google_place_id': 'same', 'reasons': []},
            {'local_place_id': 'B', 'google_place_id': 'same', 'reasons': []}], 'a'*64, '2026-11-13')
        self.assertEqual([], catalog['bindings'])
        self.assertEqual(2, len(review['pending']))

    def test_only_confirmed_ids_enter_catalog_and_no_provider_payload_is_stored(self):
        catalog, review = bind.create_catalog([
            {'local_place_id': 'A', 'google_place_id': 'one', 'reasons': []},
            {'local_place_id': 'B', 'google_place_id': 'two', 'reasons': ['NAME_OR_BRANCH_DIFFERS']}],
            'a'*64, '2026-11-13')
        self.assertEqual([{'local_place_id': 'A', 'google_place_id': 'one'}], catalog['bindings'])
        self.assertEqual(['B'], [r['local_place_id'] for r in review['pending']])

    def test_duplicate_local_id_rejected(self):
        with self.assertRaises(ValueError):
            bind.create_catalog([{'local_place_id': 'A', 'google_place_id': 'one', 'reasons': []}]*2,
                'a'*64, '2026-11-13')


if __name__ == '__main__':
    unittest.main()
