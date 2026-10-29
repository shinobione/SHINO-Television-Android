"""Exercise the Windows USB exporter with synthetic public trust/token only."""
import json
import os
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]


class UsbPairingExporterTest(unittest.TestCase):
    def export(self, host, expected):
        with tempfile.TemporaryDirectory() as folder:
            folder = Path(folder)
            config = folder / 'public.json'
            destination = folder / 'pairing.json'
            config.write_text(json.dumps(dict(host=host, port=8767,
                certificateSha256='a' * 64,
                caFile=str(ROOT / 'app/src/test/resources/tvhome-test-ca.pem'))), encoding='utf-8')
            env = dict(os.environ, USB_TEST_SCRIPT=str(ROOT / 'tools/New-TvHomeUsbPairing.ps1'),
                       USB_TEST_CONFIG=str(config), USB_TEST_OUTPUT=str(destination))
            command = """
function Read-Host {
 param([string]$Prompt,[switch]$AsSecureString)
 ConvertTo-SecureString 'test_reader_tvhome_123456789' -AsPlainText -Force
}
try {
 & $env:USB_TEST_SCRIPT -PublicConfig $env:USB_TEST_CONFIG -OutputPath $env:USB_TEST_OUTPUT
 exit 0
} catch { exit 2 }
"""
            result = subprocess.run([shutil.which('pwsh') or 'powershell', '-NoProfile', '-NonInteractive',
                                     '-Command', command], env=env, capture_output=True, timeout=20)
            self.assertEqual(result.returncode, 0 if expected else 2, host)
            self.assertEqual(destination.exists(), expected)
            if expected:
                raw = destination.read_bytes()
                self.assertFalse(raw.startswith(b'\xef\xbb\xbf'))
                document = json.loads(raw)
                self.assertEqual(document['host'], host)
                self.assertEqual(document['readerToken'], 'test_reader_tvhome_123456789')
                self.assertEqual(document['format'], 'SHINO_TV_HOME_HUB_PAIRING')

    def test_rejects_public_and_malformed_ipv4(self):
        for host in ['192.0.2.1', '172.32.0.1', '10.999.1.2', '8.8.8.8']:
            with self.subTest(host=host):
                self.export(host, False)

    def test_accepts_all_private_ipv4_ranges(self):
        for host in ['192.168.1.195', '10.12.0.1', '172.16.0.1', '172.31.255.254']:
            with self.subTest(host=host):
                self.export(host, True)
