import hashlib
import json
import subprocess
import tempfile
import unittest
from pathlib import Path

from verify_delivery import PACKAGE, inputs, installed_code


class DeliveryInputs(unittest.TestCase):
    def test_package_and_version(self):
        self.assertEqual(installed_code('Packages:\n  Package [other] (a):\n    versionCode=1\n  Package [' + PACKAGE + '] (b):\n    versionCode=577311033 minSdk=21\n'), 577311033)
        with self.assertRaises(ValueError):
            installed_code('Packages:\n  Package [other] (a):\n    versionCode=577311033\n')
        with self.assertRaises(ValueError):
            installed_code('  Package [' + PACKAGE + '] (b):\n    versionCode=1\n  Package [' + PACKAGE + '] (c):\n    versionCode=2\n')

    def test_later_stage_and_changed_candidate(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            repo = root / 'repo'
            candidate = root / 'candidate'
            (repo / 'release').mkdir(parents=True)
            candidate.mkdir()
            def git(*args):
                return subprocess.check_output(['git', '-C', str(repo), *args], stderr=subprocess.DEVNULL, text=True).strip()
            git('init', '-q')
            manifest = {'applicationId': PACKAGE, 'versionCode': 577311044, 'versionName': 'R33.5-B08-577311044'}
            (repo / 'release/release_manifest.json').write_text(json.dumps(manifest))
            git('add', '.')
            git('-c', 'user.name=Fixture', '-c', 'user.email=fixture@example.invalid', 'commit', '-qm', 'fixture')
            apk = candidate / 'candidate.apk'
            apk.write_bytes(b'fixture for input resolution')
            record = dict(manifest, file=apk.name, sha256=hashlib.sha256(apk.read_bytes()).hexdigest())
            delivery = {'apk': record, 'sourceCommit':git('rev-parse', 'HEAD')}
            (candidate / 'DELIVERY.json').write_text(json.dumps(delivery))
            dump = root / 'installed.txt'
            dump.write_text('  Package [' + PACKAGE + '] (b):\n    versionCode=577311033 minSdk=21\n')
            args = inputs(candidate, repo, dump)
            self.assertEqual(args[args.index('--version-code')+1], '577311044')
            self.assertEqual(args[args.index('--installed-version')+1], '577311033')
            apk.write_bytes(b'changed')
            with self.assertRaises(ValueError):
                inputs(candidate, repo, dump)


if __name__ == '__main__':
    unittest.main()
