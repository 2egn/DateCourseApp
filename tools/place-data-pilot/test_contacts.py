import unittest
from collections import Counter

import contacts


class ContactTest(unittest.TestCase):
    def setUp(self):
        self.local = {'name': '예시도서관', 'branch': '', 'category': 'CULTURE',
            'lat': '35.1578', 'lon': '129.0600', 'road_address': '부산 부산진구 잘못된로 1',
            'lot_address': '', 'phone': '051-123-4567', 'website': 'https://library.test/branches/one',
            'website_identity_verified': True}
        self.remote = {'id': 'example', 'displayName': {'text': '예시도서관'},
            'formattedAddress': '부산 부산진구 다른로 2', 'location': {'latitude': 35.1578, 'longitude': 129.06},
            'types': ['library'], 'businessStatus': 'OPERATIONAL', 'nationalPhoneNumber': '051-123-4567',
            'websiteUri': 'http://www.library.test/branches/one/'}
        self.counts = Counter({'0511234567': 1})

    def test_phone_formats_and_extensions(self):
        for value in ['051-123-4567', '+82 51 123 4567', '0082 51 123 4567', '051-123-4567(내선1)']:
            self.assertEqual('0511234567', contacts.phone_key(value))
        self.assertIsNone(contacts.phone_key(''))
        self.assertIsNone(contacts.phone_key('1234'))

    def test_unique_phone_and_name_can_match_without_equal_address(self):
        result = contacts.assess(dict(self.local, website=''), self.remote, self.counts)
        self.assertTrue(result['identity_match'])
        self.assertTrue(result['contact_identity_match'])
        self.assertIsNone(result['address_method'])

    def test_shared_number_and_shared_brand_domain_are_insufficient(self):
        local = dict(self.local, website='https://library.test/')
        remote = dict(self.remote, websiteUri='http://library.test')
        result = contacts.assess(local, remote, Counter({'0511234567': 2}))
        self.assertTrue(result['shared_source_phone'])
        self.assertFalse(result['identity_match'])

    def test_scoped_page_and_name_can_match_when_phone_missing(self):
        result = contacts.assess(self.local, dict(self.remote, nationalPhoneNumber=''), self.counts)
        self.assertTrue(result['identity_match'])
        self.assertTrue(result['scoped_website_match'])

    def test_different_direct_phone_is_a_conflict(self):
        result = contacts.assess(self.local, dict(self.remote, nationalPhoneNumber='051-999-9999'), self.counts)
        self.assertFalse(result['identity_match'])
        self.assertIn('DIRECT_PHONE_CONFLICT', result['reasons'])

    def test_query_parameters_identify_pages_and_are_not_discarded(self):
        self.assertEqual(contacts.website_key('https://www.a.test/page?id=1&utm_source=x'),
                         contacts.website_key('http://a.test/page?id=1'))
        self.assertNotEqual(contacts.website_key('https://a.test/page?id=1'),
                            contacts.website_key('https://a.test/page?id=2'))
        self.assertIsNone(contacts.website_key('javascript:alert(1)'))

    def test_closed_same_identity_does_not_become_eligible(self):
        result = contacts.assess(self.local, dict(self.remote, businessStatus='CLOSED_TEMPORARILY'), self.counts)
        self.assertTrue(result['identity_match'])
        self.assertFalse(result['visit_eligible'])

    def test_phone_alone_does_not_override_another_named_branch(self):
        local = dict(self.local, name='예시카페', branch='서면점', category='CAFE')
        remote = dict(self.remote, displayName={'text': '예시카페 전포점'}, types=['cafe'])
        self.assertFalse(contacts.assess(local, remote, self.counts)['identity_match'])

    def test_matching_complex_contact_does_not_merge_its_gallery(self):
        local = dict(self.local, name='예시미술관')
        remote = dict(self.remote, displayName={'text': '예시복합문화시설'}, types=['cultural_center'])
        self.assertFalse(contacts.assess(local, remote, self.counts)['identity_match'])

    def test_contact_match_is_still_bound_to_a_region(self):
        remote = dict(self.remote, location={'latitude': 37.5, 'longitude': 127})
        self.assertFalse(contacts.assess(self.local, remote, self.counts)['identity_match'])

    def test_unverified_listing_url_is_not_an_identity_proof(self):
        local = dict(self.local, website_identity_verified=False, phone='')
        result = contacts.assess(local, dict(self.remote, nationalPhoneNumber=''), self.counts)
        self.assertTrue(result['scoped_website_match'])
        self.assertFalse(result['identity_match'])

    def test_official_closure_overrides_provider_operational_status(self):
        result = contacts.assess(dict(self.local, visit_available=False), self.remote, self.counts)
        self.assertTrue(result['identity_match'])
        self.assertFalse(result['visit_eligible'])

    def test_source_corrections_preserve_original_and_reject_stale_inputs(self):
        source = {'A': dict(self.local, phone='051-999-9999')}
        correction = {'updates': [{'local_place_id': 'A', 'expected': {'phone': '051-999-9999'},
            'values': {'phone': '051-123-4567'}, 'source_url': 'https://library.test/branches/one'}]}
        updated = contacts.apply_overrides(source, correction)
        self.assertEqual('051-999-9999', source['A']['phone'])
        self.assertTrue(contacts.assess(updated['A'], self.remote, self.counts)['identity_match'])
        with self.assertRaises(ValueError):
            contacts.apply_overrides(updated, correction)


if __name__ == '__main__':
    unittest.main()
