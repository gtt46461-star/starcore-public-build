"""Check the documented wrapper against both archived phone jobs."""
import argparse
import json
from pathlib import Path
import tempfile
import unittest
import zipfile

from prepare_export import unwrap
from stage_checked import load_module


def suite(plan):
    class OriginalExports(unittest.TestCase):
        def test_real_wrappers(self):
            reports = sorted((plan / 'evidence/original').glob('*.zip'))
            self.assertEqual(len(reports), 2)
            validator = load_module(plan / 'B01/scripts/verify_export_artifacts_r334.py', 'validator')
            for outer in reports:
                with self.subTest(outer=outer.name), tempfile.NamedTemporaryFile() as temp:
                    with zipfile.ZipFile(outer) as archive:
                        index = json.loads(archive.read(next(n for n in archive.namelist() if n.endswith('/index.json'))))
                    with self.assertRaises(ValueError):
                        unwrap(outer, '0'*64, temp)
                    sha = unwrap(outer, index['jobId'], temp)
                    self.assertEqual(sha, index['files'][0]['sha256'])
                    self.assertFalse(index['allChecksPassed'])
                    facts = validator.validate_export(Path(temp.name), {'jobId':index['jobId']}, allow_legacy_033=True)
                    self.assertEqual(facts['status'], 'LEGACY_ARTIFACT_VERIFIED_WITH_SCHEMA_DEFECT')
                    with self.assertRaises(ValueError):
                        validator.validate_export(Path(temp.name), {'jobId':index['jobId']})
    return unittest.defaultTestLoader.loadTestsFromTestCase(OriginalExports)


if __name__ == '__main__':
    p = argparse.ArgumentParser(); p.add_argument('--plan',type=Path,required=True)
    raise SystemExit(not unittest.TextTestRunner(verbosity=2).run(suite(p.parse_args().plan)).wasSuccessful())
