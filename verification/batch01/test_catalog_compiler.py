"""Current production compiler: frozen full input, failure safety and -O guards."""
import csv
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('compiler', ROOT / 'scripts/cp4_compile_catalog.py')
compiler = importlib.util.module_from_spec(spec)
spec.loader.exec_module(compiler)


class CompilerContract(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.work = Path(self.temp.name)
        self.catalog = Path(os.environ['STARCORE_PLAN_ROOT']) / '09_ATOM_MASTER_CATALOG/ATOM_MASTER_CATALOG.csv'
        with self.catalog.open(encoding='utf-8-sig', newline='') as stream:
            reader = csv.DictReader(stream)
            self.fields = reader.fieldnames
            self.rows = [next(reader), next(reader)]
        self.output = self.work / 'output.jsonl'
        self.output.write_bytes(b'previous accepted output\n')

    def mutate(self, rows):
        p = self.work / 'bad.csv'
        with p.open('w', newline='') as stream:
            w = csv.DictWriter(stream, fieldnames=self.fields)
            w.writeheader()
            w.writerows(rows)
        return p

    def rejected(self, p, reason):
        with self.assertRaisesRegex(ValueError, reason):
            compiler.compile_catalog(p, self.output)
        self.assertEqual(self.output.read_bytes(), b'previous accepted output\n')
        self.assertEqual(list(self.work.glob('*.partial')), [])
        self.assertFalse(self.output.with_suffix('.manifest.json').exists())

    def test_full_frozen_regeneration_matches_runtime_bytes(self):
        manifest = compiler.compile_catalog(self.catalog, self.output)
        self.assertEqual(self.output.read_bytes(), (ROOT / 'app/src/main/assets/cp4_atom_registry.jsonl').read_bytes())
        self.assertEqual((manifest['atoms'], manifest['families'], manifest['requiresBinding']), (25350, 28, 130))

    def test_duplicate_fails_without_destroying_previous_output(self):
        self.rejected(self.mutate([self.rows[0], self.rows[0]]), 'CATALOG_DUPLICATE')

    def test_truncated_catalog_fails_without_publishing(self):
        self.rejected(self.mutate(self.rows), 'CATALOG_CARDINALITY')

    def test_bad_hash_fails(self):
        self.rows[0]['spec_hash'] = '0' * 64
        self.rejected(self.mutate(self.rows), 'CATALOG_SPEC_IDENTITY')

    def test_invalid_history_fails(self):
        self.rows[0]['minimum_history'] = '5001'
        self.rejected(self.mutate(self.rows), 'CATALOG_INVALID_TYPE_HISTORY')

    def test_invalid_output_type_fails(self):
        self.rows[0]['output'] = 'REAL_BET'
        self.rejected(self.mutate(self.rows), 'CATALOG_INVALID_TYPE_HISTORY')

    def test_input_output_collision_preserves_source(self):
        p = self.mutate(self.rows)
        before = p.read_bytes()
        with self.assertRaisesRegex(ValueError, 'COLLISION'):
            compiler.compile_catalog(p, p)
        self.assertEqual(p.read_bytes(), before)

    def test_optimized_python_cannot_remove_guards(self):
        p = self.mutate([self.rows[0], self.rows[0]])
        result = subprocess.run([sys.executable, '-O', str(ROOT / 'scripts/cp4_compile_catalog.py'),
                                 '--catalog', str(p), '--output', str(self.output)], capture_output=True, text=True)
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('CATALOG_DUPLICATE', result.stderr)
        self.assertEqual(self.output.read_bytes(), b'previous accepted output\n')


if __name__ == '__main__':
    unittest.main(verbosity=2)
