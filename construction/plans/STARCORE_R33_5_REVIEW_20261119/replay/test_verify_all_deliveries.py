"""Cover order, ancestry, same-version follow-up commits and retained bytes with Git."""
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from verify_all_deliveries import STAGES, VERSION_FILES, check_stage_source, git, validate_rows

class StageDeliveryTest(unittest.TestCase):
    def test_index(self):
        rows = [{'stage': s} for s in STAGES]
        self.assertEqual(validate_rows({'stages':rows}), rows)
        for bad in (rows[1:], rows[::-1], rows[:1] + rows[:-1]):
            with self.assertRaises(ValueError):
                validate_rows({'stages':bad})

    def test_real_version_commits(self):
        with tempfile.TemporaryDirectory() as tmp:
            repo = Path(tmp)
            git(repo,'init','-q')
            def commit(name):
                git(repo,'add','-A')
                git(repo,'-c','user.name=Test','-c','user.email=test@example.invalid','commit','-qm',name)
                return git(repo,'rev-parse','HEAD').decode().strip()
            version = {'versionCode':40,'versionName':'R33.5-P00-2026-11-20-40'}
            manifest = repo / VERSION_FILES[0]
            manifest.parent.mkdir()
            manifest.write_text(json.dumps(version))
            (repo/'owner').write_text('registered')
            start = commit('P00')
            (repo/'evidence').write_text('later valid documentation')
            valid = commit('P00 build')
            (repo/'owner').write_text('unreviewed')
            bad = commit('wrong bytes')
            manifest.write_text('{}')
            end = commit('next stage')
            def validate(head, stage='P00', following=end):
                return check_stage_source(repo,stage,{'sourceCommit':head,'apk':version},
                    {'versionCommit':start}, {'versionCommit':following} if following else None,
                    end, {VERSION_FILES[0],'owner'})
            validate(start)
            validate(valid)
            with self.assertRaises(subprocess.CalledProcessError):
                validate(bad)
            with self.assertRaisesRegex(ValueError,'NEXT_STAGE'):
                validate(end)
            with self.assertRaisesRegex(ValueError,'VERSION'):
                validate(start,'B04')
            with self.assertRaisesRegex(ValueError,'FINAL_HEAD'):
                validate(valid,following=None)

if __name__ == '__main__':
    unittest.main(verbosity=2)
