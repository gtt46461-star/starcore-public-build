#!/usr/bin/env python3
"""Exercise the actual shell claim gate on real source and injected violations."""
import subprocess
import tempfile
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2]
POLICY = ROOT / 'app/src/main/java/com/ethk3/novaonepro/R36AnalysisLeasePolicyR365.java'
SCRIPT = ROOT / 'scripts/r36_behavior_gate.sh'


class ClaimDeadlineGateTest(unittest.TestCase):
    def gate(self, source):
        # Execute the functions and extraction/check block from the actual gate.
        # Only the real source path and temporary evidence directory are redirected.
        shell = SCRIPT.read_text()
        functions = shell[shell.index('fail() {'):shell.index('# R926+')]
        start = shell.index("sed -n '/public static Decision decide(/")
        end = shell.index("require_text 'LEASE_FENCE_LOST'", start)
        block = shell[start:end]
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / 'policy.java'
            path.write_text(source)
            block = block.replace('app/src/main/java/com/ethk3/novaonepro/R36AnalysisLeasePolicyR365.java', '"$POLICY"')
            return subprocess.run(['sh', '-c', 'set -eu\nTMP=$1\nPOLICY=$2\n' + functions + block,
                                   'claim-gate', directory, str(path)], capture_output=True, text=True)

    def test_actual_claim_and_preserved_predraw_refresh(self):
        source = POLICY.read_text()
        self.assertIn('shouldRefreshPredrawDeadline', source)
        self.assertIn('String status, long deadlineAt', source)
        self.assertEqual(0, self.gate(source).returncode)

    def test_deadline_in_claim_body_is_rejected(self):
        source = POLICY.read_text().replace('if (canonicalExists ||', 'if (deadlineAt > 0) return Decision.TERMINAL;\n        if (canonicalExists ||', 1)
        result = self.gate(source)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('R366_DEADLINE_MUST_NOT_BE_CLAIM_CORRECTNESS_GATE', result.stderr)

    def test_deadline_in_claim_signature_is_rejected(self):
        source = POLICY.read_text().replace('long nextAttemptAt,', 'long nextAttemptAt, long deadline_at,', 1)
        result = self.gate(source)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('R366_DEADLINE_MUST_NOT_BE_CLAIM_CORRECTNESS_GATE', result.stderr)

    def test_missing_claim_method_is_rejected(self):
        source = POLICY.read_text().replace('public static Decision decide(', 'public static Decision renamed(', 1)
        result = self.gate(source)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('R366_CLAIM_DECISION_MISSING', result.stderr)

    def test_missing_correctness_boundary_is_rejected(self):
        source = POLICY.read_text().replace('if (targetDrawn) return Decision.TARGET_ALREADY_DRAWN;', '', 1)
        result = self.gate(source)
        self.assertNotEqual(0, result.returncode)
        self.assertIn('R366_CLAIM_DRAW_BOUNDARY_MISSING', result.stderr)


if __name__ == '__main__':
    unittest.main(verbosity=2)
