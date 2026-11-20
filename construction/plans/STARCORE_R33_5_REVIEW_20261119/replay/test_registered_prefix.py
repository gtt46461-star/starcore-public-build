"""Reject omitted/reverted real stage payloads even when all version labels are valid."""
import argparse
import sys
import unittest
from pathlib import Path
import stage_checked as checked


def configure(plan):
    sys.path.insert(0, str(plan / 'verification/contractor'))
    import stage
    import test_stage

    class RegisteredPrefix(unittest.TestCase):
        @classmethod
        def setUpClass(cls):
            test_stage.Handoff.setUpClass()
            cls.fixture = test_stage.Handoff
            cls.gen = checked.load_module(plan / 'P00/reference/scripts/r926_release_manifest.py', 'prefix_generator')
            cls.pair = checked.load_module(Path(__file__).resolve().parents[1] / 'research/b05_pair_source.py', 'prefix_pair')
            cls.advance = checked.load_module(plan / 'P00/payload/tools/r33/advance_release_version.py', 'prefix_advance')
            cls.path = 'app/src/main/java/com/ethk3/novaonepro/LegacyBackupRecoveryR335.java'
            current = cls.fixture.seed
            for batch in ('P00', 'B01'):
                out = cls.fixture.home / ('prefix-' + batch)
                checked.run_stage(stage, cls.gen, cls.pair, plan, current, batch, cls.fixture.base, '0' * 64, dest=out, review_root=cls.fixture.home / 'legacy-no-overlay')
                tx = cls.fixture.home / ('prefix-tx-' + batch)
                cls.advance.prepare(out, tx, 577311033, batch, '2026-11-20')
                cls.advance.apply(out, tx)
                stage.git(out, 'add', '--', *checked.VERSION_FILES)
                cls.fixture.commit(out)
                current = out
            cls.complete = current

        @classmethod
        def tearDownClass(cls):
            cls.fixture.tearDownClass()

        def copy(self, label):
            target = self.fixture.home / label
            stage.git(self.fixture.home, 'clone', '--quiet', '--local', str(self.complete), str(target))
            return target

        def run_next(self, source, target='B02'):
            return checked.run_stage(stage, self.gen, self.pair, plan, source, target, self.fixture.base, '0' * 64,
                                     review_root=self.fixture.home / 'legacy-no-overlay')

        def test_positive_complete_prefix(self):
            result = self.run_next(self.complete)
            self.assertEqual([r['stage'] for r in result['registeredPredecessorChecks']], ['P00', 'B01'])
            self.assertGreater(result['registeredPredecessorChecks'][-1]['registeredProductionFiles'], 30)

        def test_deleted_completed_stage_file_rejected(self):
            source = self.copy('missing-completed-source')
            stage.git(source, 'rm', '--', self.path)
            self.fixture.commit(source)
            with self.assertRaisesRegex(ValueError, 'REGISTERED_PREDECESSOR_BYTES_MISSING:B01:'):
                self.run_next(source)

        def test_changed_completed_stage_file_rejected(self):
            source = self.copy('reverted-completed-source')
            (source / self.path).write_text('// stale file from another candidate\n')
            stage.git(source, 'add', '--', self.path)
            self.fixture.commit(source)
            with self.assertRaisesRegex(ValueError, 'REGISTERED_PREDECESSOR_BYTES_MISSING:B01:'):
                self.run_next(source)

        def test_uncommitted_inherited_file_preserved_and_rejected(self):
            source = self.copy('uncommitted-inherited-source')
            original = (source / self.path).read_bytes()
            stage.git(source, 'update-index', '--assume-unchanged', '--', self.path)
            (source / self.path).write_bytes(original + b'\n// preserve local edit\n')
            with self.assertRaisesRegex(ValueError, 'LOCAL_PREDECESSOR_BYTES_DIFFER:'):
                self.run_next(source)
            self.assertEqual((source / self.path).read_bytes(), original + b'\n// preserve local edit\n')

        def test_staged_inherited_file_preserved_and_rejected(self):
            source = self.copy('staged-inherited-source')
            original = (source / self.path).read_bytes()
            (source / self.path).write_bytes(original + b'\n// staged edit\n')
            stage.git(source, 'add', '--', self.path)
            (source / self.path).write_bytes(original)
            with self.assertRaisesRegex(ValueError, 'INDEX_PREDECESSOR_BYTES_DIFFER:'):
                self.run_next(source)
            self.assertEqual(stage.git(source, 'show', ':' + self.path), original + b'\n// staged edit\n')

        def test_partial_next_stage_cannot_hide_incomplete_versioned_stage(self):
            source = self.copy('incomplete-versioned-stage')
            name = 'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java'
            row = next(r for r in stage.complete_records('B01') if r['path'] == name)
            (source / name).write_bytes((plan / row['beforeFile']).read_bytes())
            tx = self.fixture.home / 'incomplete-version-tx'
            self.advance.prepare(source, tx, 577311033, 'B01', '2026-11-20')
            self.advance.apply(source, tx)
            stage.git(source, 'add', '--', name, *checked.VERSION_FILES)
            self.fixture.commit(source)
            active = stage.materialize('B02', stage.records('B02'))
            (source / name).write_bytes(active[name])
            stage.git(source, 'add', '--', name)
            self.fixture.commit(source)
            with self.assertRaisesRegex(ValueError, 'VERSIONED_PREDECESSOR_BYTES_MISSING:B01:'):
                self.run_next(source)

        def test_committed_partial_active_stage_allowed(self):
            source = self.copy('partial-active-source')
            active = stage.materialize('B02', stage.records('B02'))
            name = 'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java'
            (source / name).write_bytes(active[name])
            stage.git(source, 'add', '--', name)
            self.fixture.commit(source)
            result = self.run_next(source)
            self.assertEqual(result['states'][name], 'ALREADY_PRESENT')

    return unittest.defaultTestLoader.loadTestsFromTestCase(RegisteredPrefix)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan', required=True, type=Path)
    suite = configure(parser.parse_args().plan.resolve())
    sys.exit(not unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful())
