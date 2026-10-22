"""Verify the real R967 generator against corrupted output artifacts.

All identity inputs are read from the actual repository. Only output paths are
redirected to temporary files; no identity calculation or policy is mocked.
"""
import importlib.util
from pathlib import Path
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[2]
SPEC = importlib.util.spec_from_file_location(
    "r967_semantic_identity", ROOT / "scripts/r967_semantic_identity.py")
SEMANTICS = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(SEMANTICS)


class SemanticOutputIdentityTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.original_paths = SEMANTICS.GENERATED, SEMANTICS.REPORT
        SEMANTICS.GENERATED = str(Path(self.temp.name) / "ResearchSemanticsGeneratedR967.java")
        SEMANTICS.REPORT = str(Path(self.temp.name) / "semantic_manifest.json")
        self.java = Path(SEMANTICS.GENERATED)
        self.report = Path(SEMANTICS.REPORT)
        self.value = SEMANTICS.write_outputs()

    def tearDown(self):
        SEMANTICS.GENERATED, SEMANTICS.REPORT = self.original_paths
        self.temp.cleanup()

    def test_unchanged_outputs_and_repeated_generation_are_identical(self):
        before = self.java.read_bytes(), self.report.read_bytes()
        self.assertEqual(self.value, SEMANTICS.verify_outputs())
        SEMANTICS.write_outputs()
        self.assertEqual(before, (self.java.read_bytes(), self.report.read_bytes()))

    def test_java_line_ending_drift_is_rejected(self):
        self.java.write_bytes(self.java.read_bytes().replace(b"\n", b"\r\n"))
        with self.assertRaisesRegex(ValueError, "R967_GENERATED_IDENTITY_STALE"):
            SEMANTICS.verify_outputs()

    def test_manifest_duplicate_key_is_rejected(self):
        raw = self.report.read_bytes()
        duplicate = b'  "revision": "R967_SEMANTIC_BUILD_V1",\n'
        self.report.write_bytes(b"{\n" + duplicate + raw[2:])
        with self.assertRaisesRegex(ValueError, "R967_GENERATED_MANIFEST_STALE"):
            SEMANTICS.verify_outputs()

    def test_manifest_integer_to_float_drift_is_rejected(self):
        raw = self.report.read_bytes()
        original = str(self.value["controlled_file_count"]).encode("ascii")
        field = b'"controlled_file_count": ' + original
        self.assertIn(field, raw)
        self.report.write_bytes(raw.replace(field, field + b".0", 1))
        with self.assertRaisesRegex(ValueError, "R967_GENERATED_MANIFEST_STALE"):
            SEMANTICS.verify_outputs()

    def test_missing_output_is_rejected_and_regeneration_recovers(self):
        self.report.unlink()
        with self.assertRaisesRegex(ValueError, "R967_GENERATED_MANIFEST_STALE"):
            SEMANTICS.verify_outputs()
        SEMANTICS.write_outputs()
        self.assertEqual(self.value, SEMANTICS.verify_outputs())

    def test_truncated_output_is_rejected_and_regeneration_recovers(self):
        self.java.write_bytes(self.java.read_bytes()[:32])
        with self.assertRaisesRegex(ValueError, "R967_GENERATED_IDENTITY_STALE"):
            SEMANTICS.verify_outputs()
        SEMANTICS.write_outputs()
        self.assertEqual(self.value, SEMANTICS.verify_outputs())


if __name__ == "__main__":
    unittest.main(verbosity=2)
