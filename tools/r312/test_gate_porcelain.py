"""Regression for real Git porcelain whitespace; these are gate tests, not Android tests."""
import importlib.util
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest

sys.dont_write_bytecode = True
ROOT = Path(__file__).resolve().parents[2]


class GatePorcelainTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory(prefix="r312-porcelain-")
        self.repo = Path(self.directory.name)
        self.run_git("init", "-q")
        self.run_git("config", "user.name", "Gate fixture")
        self.run_git("config", "user.email", "gate-fixture@example.invalid")
        self.evidence = self.repo / "verification/r31/r312/S00/GATE_CONTEXT.json"
        self.source = self.repo / "app/src/main/Probe.java"
        for path in (self.evidence, self.source):
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text("initial\n")
        self.run_git("add", ".")
        self.run_git("commit", "-qm", "fixture")
        spec = importlib.util.spec_from_file_location("actual_r312_gate", ROOT / "construction/STARCORE_R31_FINAL_20261104/tools/r312_gate.py")
        self.gate = importlib.util.module_from_spec(spec)
        spec.loader.exec_module(self.gate)
        self.gate.ROOT = self.repo

    def tearDown(self):
        self.directory.cleanup()

    def run_git(self, *args):
        return subprocess.check_output(["git", *args], cwd=self.repo, text=True)

    def test_first_modified_evidence_keeps_its_status_column_and_path(self):
        self.evidence.write_text("changed\n")
        status = self.gate.git("status", "--porcelain")
        self.assertTrue(status.startswith(" M verification/"), status)
        _, dirty = self.gate.tree_identity()
        self.assertEqual([], dirty)

    def test_actual_source_modification_is_rejected_with_exact_path(self):
        self.source.write_text("changed\n")
        _, dirty = self.gate.tree_identity()
        self.assertEqual([" M app/src/main/Probe.java"], dirty)

    def test_source_and_evidence_do_not_hide_each_other(self):
        self.source.write_text("changed\n")
        self.evidence.write_text("changed\n")
        _, dirty = self.gate.tree_identity()
        self.assertEqual([" M app/src/main/Probe.java"], dirty)


if __name__ == "__main__":
    unittest.main()
