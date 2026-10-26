import importlib.util
import json
import pathlib
import tempfile
import unittest
import threading
import urllib.request
import urllib.error
import socket
import ssl
import time
from datetime import datetime, timezone, timedelta

ROOT = pathlib.Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location('gateway', ROOT / 'companion/windows/gateway.py')
gateway = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gateway)

class GatewayTests(unittest.TestCase):
    def setUp(self):
        self.now = datetime(2026, 10, 26, 12, tzinfo=timezone.utc)

    def sample(self, **extra):
        return dict(utc=self.now.isoformat(), five=76.5, week=10,
                    fiveResetUtc=(self.now + timedelta(hours=2)).isoformat(),
                    weekResetUtc=None, source='Codex CLI / account/rateLimits/read', **extra)

    def test_remaining_not_inverted_and_no_private_fields(self):
        value = gateway.project_history(self.sample(prompt='PRIVATE', token='SECRET'), self.now)
        self.assertEqual(value['fiveHour']['remainingPercent'], 76.5)
        self.assertNotIn('PRIVATE', json.dumps(value))
        self.assertNotIn('SECRET', json.dumps(value))
        self.assertEqual(value['activity'], 'UNVERIFIED')

    def test_invalid_values_rejected(self):
        for bad in [True, -1, 101, float('nan'), '50']:
            row = self.sample(); row['five'] = bad
            self.assertRaises(ValueError, gateway.project_history, row, self.now)

    def test_future_timestamp_rejected(self):
        row = self.sample(); row['utc'] = (self.now + timedelta(minutes=6)).isoformat()
        self.assertRaises(ValueError, gateway.project_history, row, self.now)

    def test_expired_quota_is_never_current(self):
        row = self.sample(); row['fiveResetUtc'] = (self.now - timedelta(seconds=1)).isoformat()
        result = gateway.project_history(row, self.now)
        self.assertEqual(result['status'], 'STALE')

    def test_old_sample_keeps_source_timestamp(self):
        row = self.sample(); row['utc'] = (self.now - timedelta(hours=1)).isoformat()
        result = gateway.project_history(row, self.now)
        self.assertEqual(result['status'], 'STALE')
        self.assertEqual(result['capturedAt'], row['utc'])

    def test_latest_valid_line_and_partial_write(self):
        with tempfile.TemporaryDirectory() as td:
            file = pathlib.Path(td) / 'history.jsonl'
            file.write_text(json.dumps(self.sample()) + '\n{"utc":', encoding='utf8')
            self.assertEqual(gateway.read_history(file, self.now)['fiveHour']['remainingPercent'], 76.5)

    def test_missing_history(self):
        self.assertEqual(gateway.read_history(pathlib.Path('absent-history'), self.now)['status'], 'UNAVAILABLE')

    def test_only_rfc1918_or_loopback(self):
        for ip in ['192.168.1.4','10.0.0.8','172.16.0.2','127.0.0.1']:
            self.assertTrue(gateway.allowed_ip(ip))
        for ip in ['0.0.0.0','8.8.8.8','169.254.1.1','100.64.0.1','224.0.0.1']:
            self.assertFalse(gateway.allowed_ip(ip))

    def test_telemetry_absent_gpu_stays_null(self):
        result = gateway.project_telemetry(dict(ok=True, cpu_usage=20, memory_used_gb=8.2, gpu_available=False, gpu_usage=0, gpu_temp_c=0))
        self.assertEqual(result['ramUsedGb'], 8.2)
        self.assertIsNone(result['gpuPercent'])

class HttpTests(unittest.TestCase):
    def setUp(self):
        self.token='syntheticTestTokenNeverForPairing0000'
        self.payload={'schemaVersion':1,'codex':{'activity':'UNVERIFIED'}}
        self.server=gateway.BridgeServer(('127.0.0.1',0),gateway.handler_for(self.token,lambda:self.payload))
        self.thread=threading.Thread(target=self.server.serve_forever,daemon=True);self.thread.start()
        self.url='http://127.0.0.1:%d/v1/snapshot'%self.server.server_port
        self.opener=urllib.request.build_opener(urllib.request.ProxyHandler({}))

    def tearDown(self):
        self.server.shutdown();self.server.server_close();self.thread.join(timeout=2)

    def request(self,method='GET',token=None,path=None):
        req=urllib.request.Request(path or self.url,method=method)
        if token:req.add_header('Authorization','Bearer '+token)
        return self.opener.open(req,timeout=3)

    def test_auth_required_and_invalid_token_denied(self):
        for token in [None,'wrong']:
            with self.assertRaises(urllib.error.HTTPError) as err:self.request(token=token)
            self.assertEqual(err.exception.code,401)

    def test_authorized_whitelist_and_no_token_response(self):
        with self.request(token=self.token) as response:
            self.assertEqual(response.headers['Cache-Control'],'no-store')
            raw=response.read().decode();self.assertNotIn(self.token,raw)
            self.assertEqual(json.loads(raw),self.payload)

    def test_write_methods_denied(self):
        for method in ['POST','PUT','DELETE','PATCH']:
            with self.assertRaises(urllib.error.HTTPError) as err:self.request(method,self.token)
            self.assertEqual(err.exception.code,405)

    def test_unknown_endpoint_denied(self):
        with self.assertRaises(urllib.error.HTTPError) as err:self.request(token=self.token,path=self.url+'/other')
        self.assertEqual(err.exception.code,404)

    def test_recovery_after_transient_unavailability(self):
        self.payload={'schemaVersion':1,'codex':{'status':'UNAVAILABLE'}}
        with self.request(token=self.token) as response:self.assertEqual(json.load(response)['codex']['status'],'UNAVAILABLE')
        self.payload={'schemaVersion':1,'codex':{'status':'AVAILABLE'}}
        with self.request(token=self.token) as response:self.assertEqual(json.load(response)['codex']['status'],'AVAILABLE')

    def test_failed_tls_handshake_does_not_block_accept_loop(self):
        entered=threading.Event()
        release=threading.Event()
        timeouts=[]
        class SlowContext:
            def wrap_socket(self, connection, **options):
                timeouts.append(connection.gettimeout())
                entered.set()
                release.wait(timeout=2)
                raise ssl.SSLError('synthetic stalled TLS peer')
        self.server.tls_context=SlowContext()
        peer=socket.create_connection(('127.0.0.1',self.server.server_port),timeout=2)
        try:
            self.assertTrue(entered.wait(timeout=1))
            self.server.tls_context=None
            # The first worker is still waiting; a second HTTP test client can be served.
            with self.request(token=self.token) as response:self.assertEqual(response.status,200)
            self.assertEqual(timeouts,[5.0])
        finally:
            release.set();peer.close()

if __name__ == '__main__':
    unittest.main()
