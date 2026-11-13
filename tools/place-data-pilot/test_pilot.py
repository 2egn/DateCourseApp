"""Offline fixtures only: running these tests never calls Google."""
import unittest
from unittest.mock import patch
import pilot


class PilotTest(unittest.TestCase):
    def test_region_culture_uses_coordinates_and_address_not_missing_neighborhood(self):
        row = {'lat': '35.1578', 'lon': '129.0600', 'category': 'CULTURE',
               'road_address': '부산광역시 부산진구 서면로 1', 'lot_address': '', 'neighborhood': ''}
        self.assertTrue(pilot.in_region(row))
        self.assertFalse(pilot.in_region(dict(row, lat='nan')))
        self.assertFalse(pilot.in_region(dict(row, lat='36')))

    def test_unverified_menu_price_and_unknown_zero_are_not_per_person_cost(self):
        self.assertEqual('UNKNOWN', pilot.public_price({'price_krw': '9000',
            'price_basis': 'as_listed_portion_unknown'})['price_state'])
        self.assertEqual('UNKNOWN', pilot.public_price({'price_krw': '0',
            'price_basis': 'unknown'})['price_state'])

    def test_explicit_free_adult_admission(self):
        self.assertEqual('EXPLICIT_FREE', pilot.public_price({'price_krw': '0',
            'price_basis': 'adult_admission'})['price_state'])

    def test_total_for_two_people_and_missing_price(self):
        prices = [{'min': 10000, 'max': 15000, 'unit': 'PER_PERSON'},
                  {'min': 4000, 'max': 6000, 'unit': 'PER_DRINK'},
                  {'min': 0, 'max': 0, 'unit': 'ADULT_TICKET'}, {'unit': 'UNKNOWN'}]
        self.assertEqual({'known_min': 28000, 'known_max': 42000,
            'unknown_places': 1, 'open_ended_places': 0}, pilot.total_cost(prices, 2))

    def test_group_and_unknown_units_not_multiplied(self):
        self.assertEqual(2, pilot.total_cost([
            {'min': 30000, 'max': 40000, 'unit': 'PER_GROUP'},
            {'min': 8000, 'max': 10000, 'unit': 'UNKNOWN'}], 2)['unknown_places'])

    def test_open_ended_has_no_finite_total_upper_bound(self):
        self.assertIsNone(pilot.total_cost([{'min': 10000, 'max': None,
            'unit': 'PER_PERSON'}], 2)['known_max'])

    def test_budget_uses_upper_bound_and_known_unit(self):
        self.assertFalse(pilot.within_budget({'min': 8000, 'max': 15000, 'unit': 'PER_PERSON'}, 10000))
        self.assertTrue(pilot.within_budget({'min': 8000, 'max': 10000, 'unit': 'PER_PERSON'}, 10000))
        self.assertFalse(pilot.within_budget({'min': 0, 'max': 0, 'unit': 'UNKNOWN'}, 10000))

    def test_no_arbitrary_money_for_price_level_and_missing_end_price(self):
        self.assertEqual('LEVEL_ONLY', pilot.google_price_kind({'priceLevel': 'PRICE_LEVEL_MODERATE'}))
        self.assertEqual('OPEN_ENDED', pilot.google_price_kind({'priceRange': {
            'startPrice': {'units': '10000', 'currencyCode': 'KRW'}}}))
        self.assertEqual('RANGE', pilot.google_price_kind({'priceRange': {
            'startPrice': {'units': '0'}, 'endPrice': {'units': '10000'}}}))

    def test_same_name_in_another_branch_is_not_accepted(self):
        local = {'name': '예시카페', 'branch': '서면점', 'lat': '35.1578',
                 'lon': '129.0600', 'road_address': '부산광역시 부산진구 서전로 1'}
        remote = {'displayName': {'text': '예시카페 서면점'},
                  'location': {'latitude': 35.1578, 'longitude': 129.06},
                  'formattedAddress': '대한민국 부산 부산진구 서전로 1'}
        self.assertEqual('NAME_ADDRESS_COORDS_AGREE', pilot.match_status(local, remote))
        self.assertEqual('NEEDS_REVIEW', pilot.match_status(local,
            dict(remote, formattedAddress='대한민국 부산 부산진구 서전로 3')))

    def test_internal_course_reviews_and_external_reviews_stay_separate(self):
        result = pilot.review_sections('canonical:A', [
            {'place_ids': ['canonical:A'], 'content': '설명용 코스 후기'},
            {'place_ids': ['canonical:B'], 'content': '다른 코스'}],
            [{'text': '설명용 외부 후기', 'provider': 'Google'}])
        self.assertEqual(1, len(result['course_reviews_containing_place']))
        self.assertEqual('Google', result['external_google_reviews'][0]['provider'])

    def test_request_cap_blocks_network(self):
        probe = pilot.GoogleProbe('dummy-key')
        probe.calls = 10
        with patch.object(probe.opener, 'open') as request:
            with self.assertRaises(pilot.ProbeError):
                probe.request('GET', 'places/dummy', 'id')
            request.assert_not_called()


if __name__ == '__main__':
    unittest.main()
