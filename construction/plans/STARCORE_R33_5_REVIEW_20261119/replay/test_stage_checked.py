"""Legacy package/pair regression; current production overlays are verified separately."""
import argparse
import sys
import unittest

from pathlib import Path

import stage_checked as checked


def configure(plan):
    sys.path.insert(0, str(plan / 'verification/contractor'))
    import stage
    import test_stage

    class VersionHistory(unittest.TestCase):
        def test_stage_versions_and_empty_deltas(self):
            # Build the original handoff suite's real-byte local source fixture.
            test_stage.Handoff.setUpClass()
            self.addCleanup(test_stage.Handoff.tearDownClass)
            fixture = test_stage.Handoff
            current = fixture.seed
            generator = checked.load_module(plan / 'P00/reference/scripts/r926_release_manifest.py', 'r926')
            pair = checked.load_module(Path(__file__).resolve().parents[1] / 'research/b05_pair_source.py', 'b05_pair_source')
            fixture_seal = '0' * 64  # Synthetic input identity for this isolated history test only.
            def run(source, target, **kwargs):
                return checked.run_stage(stage, generator, pair, plan, source, target, fixture.base,
                                         fixture_seal, review_root=fixture.home / "legacy-no-overlay", **kwargs)
            advance = checked.load_module(plan / 'P00/payload/tools/r33/advance_release_version.py', 'r335version')
            for empty in ('B06', 'B08'):
                self.assertEqual(stage.prepare(current, empty, baseline=fixture.base)['states'], {})
                with self.assertRaisesRegex(ValueError, 'EXPECTED_PREVIOUS_STAGE'):
                    checked.ancestry(stage, generator, current, empty, fixture.base)
            for target in stage.STAGES:
                head, prefix = checked.ancestry(stage, generator, current, target, fixture.base)
                self.assertEqual([item[0] for item in prefix], list(stage.STAGES[:stage.STAGES.index(target)]))
                self.assertEqual(head, stage.git(current, 'rev-parse', 'HEAD').decode().strip())
                out = fixture.home / ('real-version-' + target)
                receipt = run(current, target, dest=out)
                self.assertEqual(receipt['reviewManifestSha256'], fixture_seal)
                if target in ('B05', 'B06', 'B07', 'B08'):
                    self.assertEqual(stage.sha((out / pair.REL).read_bytes()), pair.AFTER)
                    repeat = run(current, target, dest=out)
                    self.assertEqual(repeat['outputStatus'], 'REUSED_VERIFIED_OUTPUT')
                    self.assertEqual(repeat['unifiedDiffSha256'], receipt['unifiedDiffSha256'])
                if target == 'B05':
                    # A partial port of the old B05 must be upgraded to the new full Java and R82 proof.
                    legacy = fixture.home / 'legacy-b05'
                    stage.prepare(current, target, legacy, baseline=fixture.base)
                    fixture.commit(legacy)
                    new = fixture.home / 'upgraded'
                    fix = run(legacy, target, dest=new)
                    self.assertEqual(stage.sha((new / pair.REL).read_bytes()), pair.AFTER)
                    self.assertEqual((new / checked.INVALIDATION).read_bytes(), (out / checked.INVALIDATION).read_bytes())
                    self.assertEqual(len([s for s in fix['states'].values() if s == 'TO_PORT']), 2)
                    with self.assertRaisesRegex(ValueError, 'EXPECTED_PREVIOUS_STAGE'):
                        run(legacy, 'B06')
                    patch = fixture.home / 'new-b05.diff'
                    run(current, target, emit=patch)
                    self.assertEqual(stage.sha(patch.read_bytes()), receipt['unifiedDiffSha256'])
                    stage.git(current, 'apply', '--check', '--index', str(patch))
                if target == 'B07':
                    applied = fixture.home / 'b07-bytes-before-version'
                    stage.git(fixture.home, 'clone', '--quiet', '--local', str(current), str(applied))
                    patch = fixture.home / 'new-b07.diff'
                    run(current, target, emit=patch)
                    stage.git(applied, 'apply', '--index', str(patch))
                    fixture.commit(applied)
                    already = run(applied, target)
                    self.assertEqual(set(already['states'].values()), {'ALREADY_PRESENT'})
                    self.assertEqual(already['unifiedDiffSha256'], stage.sha(b''))
                if target == 'B06':
                    # A verified-empty stage must also recheck its retained files on reuse.
                    unchanged = (out / pair.REL).read_bytes()
                    (out / pair.REL).write_bytes(b'local stale file')
                    with self.assertRaisesRegex(ValueError, 'worktree bytes differ'):
                        run(current, target, dest=out)
                    (out / pair.REL).write_bytes(unchanged)
                journal = fixture.home / ('tx-' + target)
                advance.prepare(out, journal, 577311033, target, '2026-11-19')
                advance.apply(out, journal)
                stage.git(out, 'add', '--', *checked.VERSION_FILES)
                fixture.commit(out)
                current = out
                if target in ('B05', 'B06', 'B07'):
                    next_stage = stage.STAGES[stage.STAGES.index(target) + 1]
                    for name in (pair.REL.as_posix(), checked.INVALIDATION):
                        damaged = fixture.home / ('stale-' + target + '-' + Path(name).stem)
                        stage.git(fixture.home, 'clone', '--quiet', '--local', str(out), str(damaged))
                        (damaged / name).write_bytes((plan / 'B05/after_full' / name).read_bytes())
                        stage.git(damaged, 'add', '--', name)
                        fixture.commit(damaged)
                        with self.assertRaisesRegex(ValueError, 'PREDECESSOR_REVIEW_BYTES_MISSING'):
                            run(damaged, next_stage)
                if target == 'B04':
                    # A version label with one stale generated identity must not authorize B05.
                    damaged = fixture.home / 'damaged'
                    stage.git(fixture.home, 'clone', '--quiet', '--local', str(out), str(damaged))
                    props = damaged / checked.VERSION_FILES[1]
                    props.write_bytes(props.read_bytes() + b'\n')
                    stage.git(damaged, 'add', '--', checked.VERSION_FILES[1])
                    fixture.commit(damaged)
                    with self.assertRaisesRegex(ValueError, 'FOUR_FILE_VERSION_DRIFT'):
                        checked.ancestry(stage, generator, damaged, 'B05', fixture.base)
                    # A consistent B07 identity on B04 head still has missing B05/B06 ancestors.
                    label_tx = fixture.home / 'wrong-label-tx'
                    advance.prepare(damaged, label_tx, 577311033, 'B07', '2026-11-19')
                    advance.apply(damaged, label_tx)
                    stage.git(damaged, 'add', '--', *checked.VERSION_FILES)
                    fixture.commit(damaged)
                    with self.assertRaisesRegex(ValueError, 'MISSING_COMMITTED_VERSION_ANCESTOR:B06'):
                        checked.ancestry(stage, generator, damaged, 'B08', fixture.base)

    return unittest.defaultTestLoader.loadTestsFromTestCase(VersionHistory)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan', type=Path, required=True)
    suite = configure(parser.parse_args().plan.resolve())
    sys.exit(not unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful())
