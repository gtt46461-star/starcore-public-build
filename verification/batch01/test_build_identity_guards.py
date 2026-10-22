"""Current production closed classification and catalog validation negative cases."""
import copy
import importlib.util
import json
from pathlib import Path
import sys
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'scripts'))
import r967_semantic_identity as identity
import verify_catalog_assets_cp12 as catalog


class IdentityGuards(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.manifest = json.loads((ROOT / identity.MANIFEST).read_text())

    def test_illegal_role_is_rejected(self):
        value = copy.deepcopy(self.manifest)
        value['files'][0]['role'] = 'TRUSTED_UNCLASSIFIED'
        with self.assertRaisesRegex(ValueError, 'INVALID_CLASSIFICATION'):
            identity.build(ROOT, manifest_override=value)

    def test_duplicate_path_is_rejected(self):
        value = copy.deepcopy(self.manifest)
        value['files'].append(value['files'][0])
        with self.assertRaisesRegex(ValueError, 'INVALID_CLASSIFICATION'):
            identity.build(ROOT, manifest_override=value)

    def test_generator_cannot_be_omitted(self):
        value = copy.deepcopy(self.manifest)
        value['files'] = [row for row in value['files'] if row['path'] != 'scripts/cp4_compile_catalog.py']
        with self.assertRaisesRegex(ValueError, 'UNCLASSIFIED_OR_MISSING'):
            identity.build(ROOT, manifest_override=value)

    def test_missing_declared_input_is_rejected(self):
        value = copy.deepcopy(self.manifest)
        value['files'].append({'path': 'app/src/main/java/Missing.java', 'role': 'NUMERIC'})
        with self.assertRaisesRegex(ValueError, 'UNCLASSIFIED_OR_MISSING'):
            identity.build(ROOT, manifest_override=value)

    def test_changed_generator_changes_identity(self):
        value = identity.build(ROOT)
        script = 'scripts/cp4_compile_catalog.py'
        changed = identity.build(ROOT, overrides={script: (ROOT / script).read_bytes() + b'\n'})
        self.assertNotEqual(changed['training_rule_sha256'], value['training_rule_sha256'])

    def test_directory_symlink_is_rejected(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root / 'app/src/main').mkdir(parents=True)
            (root / 'app/src/main/java').symlink_to(ROOT / 'app/src/main/java', target_is_directory=True)
            with self.assertRaisesRegex(ValueError, 'MISSING_OR_SYMLINK_DIRECTORY'):
                identity.controlled_paths(root)

    def test_gate_policy_cannot_escalate_real_authority(self):
        policy = json.loads((ROOT / identity.POLICY).read_text())
        policy['real_authority'] = True
        with self.assertRaisesRegex(ValueError, 'POLICY_VALUE_OR_TYPE_UNSUPPORTED'):
            identity.read_policy(json.dumps(policy))

    def test_declared_pin_cannot_redefine_approved_count(self):
        source = 'app/src/main/java/com/ethk3/novaonepro/r952/AtomCatalogPinnedCP12.java'
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            target = root / source
            target.parent.mkdir(parents=True)
            target.write_text((ROOT / source).read_text().replace('ROWS=25350', 'ROWS=1'))
            original = catalog.ROOT
            try:
                catalog.ROOT = root
                with self.assertRaisesRegex(ValueError, 'APPROVED_PIN_DRIFT'):
                    catalog.verify(source, 'SHA256', 'ROWS', set(), 'AtomCatalogImportCP12', 'BYTES')
            finally:
                catalog.ROOT = original


if __name__ == '__main__':
    unittest.main(verbosity=2)
