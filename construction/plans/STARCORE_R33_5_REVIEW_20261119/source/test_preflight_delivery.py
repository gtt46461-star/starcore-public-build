import subprocess
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
import preflight_delivery as gate


class DeliveryInputs(unittest.TestCase):
    def test_full_and_shallow_history_are_distinct(self):
        with tempfile.TemporaryDirectory() as folder:
            root = Path(folder)
            full, shallow = root/'full', root/'shallow'
            subprocess.run(['git', 'init', '-q', str(full)], check=True)
            subprocess.run(['git', '-C', str(full), '-c', 'user.name=Fixture',
                            '-c', 'user.email=fixture@example.invalid', 'commit', '--allow-empty',
                            '-qm', 'fixture'], check=True)
            subprocess.run(['git', 'clone', '-q', '--depth=1', full.as_uri(), str(shallow)], check=True)
            self.assertEqual(gate.history(full), 'FULL_HISTORY_REPORTED')
            self.assertEqual(gate.history(shallow), 'SHALLOW_CHECKOUT')

    def test_baseline_must_match_bytes(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder)/'original.apk'
            self.assertEqual(gate.baseline(path), 'ORIGINAL_APK_MISSING')
            path.write_bytes(b'fixture')
            self.assertEqual(gate.baseline(path), 'ORIGINAL_APK_DIGEST_MISMATCH')
            with patch.object(gate, 'APK_033', gate.digest(path)):
                self.assertEqual(gate.baseline(path), 'MATCH')


if __name__ == '__main__':
    unittest.main(verbosity=2)
