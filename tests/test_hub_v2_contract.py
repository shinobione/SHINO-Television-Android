import json
import pathlib
import sys
import unittest

ROOT = pathlib.Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / '.local-build/python-deps'))
import jsonschema


class HubV2ContractTest(unittest.TestCase):
    def test_shared_lab_transcript_matches_relay_contract(self):
        schema = json.loads((ROOT / 'contracts/snapshot-v2.schema.json').read_text('utf-8'))
        jsonschema.Draft202012Validator.check_schema(schema)
        validator = jsonschema.Draft202012Validator(schema, format_checker=jsonschema.FormatChecker())
        transcript = json.loads((ROOT / 'app/src/test/resources/hub-scenario-test.json').read_text('utf-8'))
        count = 0
        for step in transcript['steps']:
            body = step.get('body', {})
            if body.get('schemaVersion') == 2:
                validator.validate(body)
                self.assertEqual('TEST', body['sampleKind'])
                count += 1
        self.assertGreaterEqual(count, 2)


if __name__ == '__main__':
    unittest.main()
