import io
import json
import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import team_keys

class Response(io.BytesIO):
    status = 200

class TeamKeysTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        self.addCleanup(self.directory.cleanup)
        self.root = Path(self.directory.name)
        team_keys._CACHE = None
        team_keys._ACCESS = None
        self.environment = patch.dict(os.environ, {}, clear=True)
        self.environment.start()
        self.addCleanup(self.environment.stop)

    def configure(self, access='ab'*32):
        (self.root / '.local').mkdir(exist_ok=True)
        (self.root / '.local/team-access.properties').write_text('team.access.key='+access)

    def payload(self):
        return json.dumps({'version':1,'keys':{'tmap':'tmap-test','kakaoJavascript':'kakao-test','googlePlaces':'google-test'}}).encode()

    def test_absent_configuration_keeps_legacy_loading_available(self):
        with patch('urllib.request.build_opener') as opener:
            self.assertEqual('',team_keys.load(self.root,'googlePlaces'))
            opener.assert_not_called()

    def test_authenticated_fetch_is_cached_and_not_written_to_disk(self):
        self.configure()
        with patch('urllib.request.build_opener') as opener:
            opener.return_value.open.return_value=Response(self.payload())
            self.assertEqual('google-test',team_keys.load(self.root,'googlePlaces'))
            self.assertEqual('tmap-test',team_keys.load(self.root,'tmap'))
            self.assertEqual(1,opener.return_value.open.call_count)
            request=opener.return_value.open.call_args.args[0]
            self.assertEqual('POST',request.method)
            self.assertEqual('Bearer '+'ab'*32,request.get_header('Authorization'))
        files=list(self.root.rglob('*'))
        self.assertEqual(1,len([f for f in files if f.is_file()]))
        self.assertNotIn('google-test',(self.root/'.local/team-access.properties').read_text())

    def test_failed_fetch_can_retry_without_leaking_server_body(self):
        self.configure()
        with patch('urllib.request.build_opener') as opener:
            opener.return_value.open.side_effect=RuntimeError('private-secret')
            with self.assertRaisesRegex(RuntimeError,'TEAM_KEYS_UNAVAILABLE') as caught:
                team_keys.load(self.root,'googlePlaces')
            self.assertNotIn('private-secret',str(caught.exception))
            self.assertIsNone(team_keys._CACHE)
            opener.return_value.open.side_effect=None
            opener.return_value.open.return_value=Response(self.payload())
            self.assertEqual('google-test',team_keys.load(self.root,'googlePlaces'))

    def test_malformed_and_partial_responses_are_rejected(self):
        self.configure()
        for raw in [b'private-secret',b'{}',b'x'*4097]:
            with patch('urllib.request.build_opener') as opener:
                opener.return_value.open.return_value=Response(raw)
                with self.assertRaisesRegex(RuntimeError,'TEAM_KEYS_UNAVAILABLE'):
                    team_keys.load(self.root,'googlePlaces')
                self.assertIsNone(team_keys._CACHE)

    def test_invalid_access_key_fails_before_network(self):
        self.configure('bad-key')
        with patch('urllib.request.build_opener') as opener:
            with self.assertRaisesRegex(RuntimeError,'TEAM_ACCESS_KEY_INVALID'):
                team_keys.load(self.root,'googlePlaces')
            opener.assert_not_called()
