import copy
import json
import pathlib
import sys
import unittest
from datetime import datetime, timezone

ROOT=pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'.local-build/python-deps'))
try:
    import jsonschema
except ImportError:
    jsonschema=None
from test_gateway import gateway

@unittest.skipIf(jsonschema is None,'Install project-local jsonschema==4.23.0 for schema validation')
class ContractTests(unittest.TestCase):
    def setUp(self):
        self.schema=json.loads((ROOT/'companion/windows/snapshot.schema.json').read_text())
        self.validator=jsonschema.Draft202012Validator(self.schema,format_checker=jsonschema.FormatChecker())
        self.payload=dict(schemaVersion=1,generatedAt='2026-10-26T12:00:00Z',codex=gateway.unavailable(),media=dict(status='UNAVAILABLE',source='PC'),telemetry=dict(status='UNAVAILABLE'))

    def test_schema_and_empty_snapshot_valid(self):
        jsonschema.Draft202012Validator.check_schema(self.schema)
        self.validator.validate(self.payload)

    def test_realistic_projected_sample_valid(self):
        now=datetime(2026,10,26,12,tzinfo=timezone.utc)
        self.payload['codex']=gateway.project_history(dict(utc=now.isoformat(),five=83,week=62,source='Codex CLI',fiveResetUtc=None,weekResetUtc=None),now)
        self.payload['telemetry']=gateway.project_telemetry(dict(ok=True,cpu_usage=20,memory_used_gb=8.2,gpu_available=True,gpu_usage=10,gpu_temp_c=45))
        self.validator.validate(self.payload)

    def test_private_fields_not_permitted(self):
        for key in ['token','prompt','auth','sessionId']:
            value=copy.deepcopy(self.payload);value['codex'][key]='forbidden'
            with self.assertRaises(jsonschema.ValidationError):self.validator.validate(value)

    def test_wrong_version_and_invalid_date_rejected(self):
        value=copy.deepcopy(self.payload);value['schemaVersion']=2
        with self.assertRaises(jsonschema.ValidationError):self.validator.validate(value)
        value=copy.deepcopy(self.payload);value['generatedAt']='yesterday'
        with self.assertRaises(jsonschema.ValidationError):self.validator.validate(value)
