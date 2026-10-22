"""Exercise real semantic inputs in an isolated, byte-identical source fixture.

Controlled files are hard-linked read-only by these tests. JSON inputs are copied
before mutation, so tests never change the repository or fake identity results.
"""
import importlib.util
import json
import os
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    'semantic_input_contract', ROOT / 'scripts/r967_semantic_identity.py')
SEMANTICS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SEMANTICS)


class SemanticInputContractTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.root = Path(self.temp.name)
        copied = {SEMANTICS.MANIFEST, 'release/release_manifest.json'}
        for name in SEMANTICS.controlled_paths(ROOT) | copied:
            dest = self.root / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            if name in copied:
                dest.write_bytes((ROOT / name).read_bytes())
            else:
                os.link(ROOT / name, dest)

    def tearDown(self):
        self.temp.cleanup()

    def duplicate(self, name, key, value):
        path = self.root / name
        raw = path.read_text()
        marker = json.dumps(key) + ':'
        self.assertIn(marker, raw)
        path.write_text(raw.replace(marker, json.dumps(key) + ':' + json.dumps(value) + ',' + marker, 1))

    def test_actual_byte_identical_inputs_produce_same_identity(self):
        self.assertEqual(SEMANTICS.build(), SEMANTICS.build(self.root))

    def test_manifest_duplicate_revision_is_rejected(self):
        self.duplicate(SEMANTICS.MANIFEST, 'revision', 'UNREVIEWED')
        with self.assertRaisesRegex(ValueError, 'R967_DUPLICATE_IDENTITY_KEY:revision'):
            SEMANTICS.build(self.root)

    def test_manifest_duplicate_role_is_rejected(self):
        self.duplicate(SEMANTICS.MANIFEST, 'role', 'PRESENTATION')
        with self.assertRaisesRegex(ValueError, 'R967_DUPLICATE_IDENTITY_KEY:role'):
            SEMANTICS.build(self.root)

    def test_release_duplicate_application_id_is_rejected(self):
        self.duplicate('release/release_manifest.json', 'applicationId', 'unreviewed.application')
        with self.assertRaisesRegex(ValueError, 'R967_DUPLICATE_IDENTITY_KEY:applicationId'):
            SEMANTICS.build(self.root)

    def test_release_duplicate_schema_version_is_rejected(self):
        self.duplicate('release/release_manifest.json', 'userVersion', 0)
        with self.assertRaisesRegex(ValueError, 'R967_DUPLICATE_IDENTITY_KEY:userVersion'):
            SEMANTICS.build(self.root)

    def test_duplicate_classified_path_is_rejected(self):
        path = self.root / SEMANTICS.MANIFEST
        value = json.loads(path.read_text())
        value['files'].append(value['files'][0])
        path.write_text(json.dumps(value))
        with self.assertRaisesRegex(ValueError, 'R967_INVALID_CLASSIFICATION'):
            SEMANTICS.build(self.root)

    def test_missing_classified_source_is_rejected(self):
        (self.root / 'app/src/main/java/com/ethk3/novaonepro/AlgorithmGenomeVmR908.java').unlink()
        with self.assertRaisesRegex(ValueError, 'R967_UNCLASSIFIED_OR_MISSING_PATHS'):
            SEMANTICS.build(self.root)

    def test_new_unclassified_source_is_rejected(self):
        (self.root / 'app/src/main/java/Unreviewed.java').write_text('class Unreviewed {}\n')
        with self.assertRaisesRegex(ValueError, 'R967_UNCLASSIFIED_OR_MISSING_PATHS'):
            SEMANTICS.build(self.root)

    def test_dynamic_review_byte_drift_is_rejected(self):
        manifest = json.loads((ROOT / SEMANTICS.MANIFEST).read_text())
        record = next(x for x in manifest['files'] if x.get('dynamic_review_sha256'))
        path = self.root / record['path']
        raw = path.read_bytes()
        path.unlink()
        path.write_bytes(raw + b'\n')
        with self.assertRaisesRegex(ValueError, 'R967_UNKNOWN_DYNAMIC_DEPENDENCY'):
            SEMANTICS.build(self.root)

    def test_required_generator_symlink_is_rejected(self):
        path = self.root / 'scripts/cp4_compile_catalog.py'
        path.unlink()
        path.symlink_to(ROOT / 'scripts/cp4_compile_catalog.py')
        with self.assertRaisesRegex(ValueError, 'R967_UNREVIEWED_SYMLINK'):
            SEMANTICS.build(self.root)

    def test_required_input_parent_symlink_is_rejected(self):
        scripts = self.root / 'scripts'
        for path in scripts.iterdir():
            path.unlink()
        scripts.rmdir()
        scripts.symlink_to(ROOT / 'scripts', target_is_directory=True)
        with self.assertRaisesRegex(ValueError, 'R967_UNREVIEWED_SYMLINK'):
            SEMANTICS.build(self.root)

    def test_missing_required_generator_has_explicit_failure(self):
        (self.root / 'scripts/cp4_compile_catalog.py').unlink()
        with self.assertRaisesRegex(ValueError, 'R967_REQUIRED_INPUT_MISSING'):
            SEMANTICS.build(self.root)


if __name__ == '__main__':
    unittest.main(verbosity=2)
