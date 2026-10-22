"""Execute current seed compiler/type checks against full canonical graph input."""
import copy
import importlib.util
import json
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
spec = importlib.util.spec_from_file_location('seed', ROOT / 'scripts/cp4_compile_molecule_seeds.py')
seed = importlib.util.module_from_spec(spec)
spec.loader.exec_module(seed)


class SeedContract(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.source = ROOT / 'app/src/main/assets/research/cp4_molecule_seeds.jsonl'
        with cls.source.open() as stream:
            cls.first = seed.parse(stream.readline())
        with (ROOT / 'app/src/main/assets/cp4_atom_registry.jsonl').open() as stream:
            cls.atoms = {r['atomId']: r for r in map(seed.parse, stream)}

    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.work = Path(self.temp.name)
        self.output = self.work / 'accepted.jsonl'
        self.output.write_bytes(b'previous accepted seed asset\n')

    def fail_row(self, row, reason):
        row['graphHash'] = seed.digest(seed.canonical(row['graph']))
        source = self.work / 'invalid.jsonl'
        source.write_text(seed.canonical(row) + '\n')
        with self.assertRaisesRegex(ValueError, reason):
            seed.compile_seeds(source, self.atoms, self.output)
        self.assertEqual(self.output.read_bytes(), b'previous accepted seed asset\n')
        self.assertFalse(list(self.work.glob('*.partial')))

    def test_all4200_regenerate_exact_runtime_asset(self):
        receipt = seed.compile_seeds(self.source, self.atoms, self.output)
        self.assertEqual(self.source.read_bytes(), self.output.read_bytes())
        self.assertEqual((receipt['rows'], receipt['sourceSeeds']), (4200, 420))
        self.assertIn('LEGACY420_SOURCE_AST_NOT_REVERIFIED', receipt['provenanceBasis'])

    def test_missing_atom_fails_even_with_valid_graph_hash(self):
        row = copy.deepcopy(self.first)
        row['graph']['nodes'][0]['atomId'] = 'ATOM2-' + '0' * 24
        self.fail_row(row, 'ARITY_TYPE_OR_ATOM')

    def test_future_parent_fails(self):
        row = copy.deepcopy(self.first)
        row['graph']['nodes'][0]['parents'] = ['n2']
        self.fail_row(row, 'PARENT_ORDER')

    def test_wrong_node_type_fails(self):
        row = copy.deepcopy(self.first)
        row['graph']['nodes'][-1]['type'] = 'SCALAR'
        self.fail_row(row, 'ARITY_TYPE_OR_ATOM')

    def test_noncanonical_numeric_parameter_fails(self):
        row = copy.deepcopy(self.first)
        row['graph']['nodes'][1]['parameters'] = ['0.3']
        self.fail_row(row, 'PARAMETER_CANONICAL_OR_FINITE')

    def test_cross_position_atom_fails(self):
        row = copy.deepcopy(self.first)
        row['graph']['position'] = 2
        self.fail_row(row, 'ARITY_TYPE_OR_ATOM')

    def test_real_authority_cannot_be_added(self):
        row = copy.deepcopy(self.first)
        row['graph']['role'] = 'REAL'
        self.fail_row(row, 'FEATURE_CONTRACT')

    def test_duplicate_source_position_fails(self):
        source = self.work / 'duplicate.jsonl'
        source.write_text((seed.canonical(self.first) + '\n') * 2)
        with self.assertRaisesRegex(ValueError, 'DUPLICATE_POSITION_ID'):
            seed.compile_seeds(source, self.atoms, self.output)
        self.assertEqual(self.output.read_bytes(), b'previous accepted seed asset\n')

    def test_duplicate_json_key_fails(self):
        with self.assertRaisesRegex(ValueError, 'DUPLICATE_JSON_KEY'):
            seed.parse('{"graph":{},"graph":{}}')

    def test_truncated_catalog_does_not_publish(self):
        source = self.work / 'truncated.jsonl'
        source.write_text(seed.canonical(self.first) + '\n')
        with self.assertRaisesRegex(ValueError, 'FROZEN_CARDINALITY'):
            seed.compile_seeds(source, self.atoms, self.output)
        self.assertEqual(self.output.read_bytes(), b'previous accepted seed asset\n')


if __name__ == '__main__':
    unittest.main(verbosity=2)
