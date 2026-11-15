"""Offline regression tests; never call a provider or read credentials."""
import unittest
from check_rehearsal_places import address_matches

class SourceDistrictTest(unittest.TestCase):
    def test_accepts_same_road_building_in_each_source_district(self):
        self.assertTrue(address_matches('부산광역시 부산진구 서전로 5','대한민국 부산광역시 부산진구 서전로 5'))
        self.assertTrue(address_matches('부산광역시 남구 전포대로 133','대한민국 부산광역시 남구 전포대로 133'))
    def test_rejects_same_road_in_different_district_or_different_building(self):
        self.assertFalse(address_matches('부산광역시 남구 전포대로 133','부산광역시 부산진구 전포대로 133'))
        self.assertFalse(address_matches('부산광역시 남구 전포대로 133','부산광역시 남구 전포대로 132'))
    def test_missing_road_or_other_city_does_not_match(self):
        self.assertFalse(address_matches('부산광역시 남구','부산광역시 남구'))
        self.assertFalse(address_matches('부산광역시 남구 전포대로 133','울산광역시 남구 전포대로 133'))

if __name__=='__main__':unittest.main()
