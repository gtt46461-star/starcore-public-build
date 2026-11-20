"""Exercise full stage composition with new and successively replaced production paths."""
import argparse
import json
import sys
import unittest
from pathlib import Path
import stage_checked as checked
import production_overlay as overlay


def configure(plan):
    sys.path.insert(0, str(plan / 'verification/contractor'))
    import stage
    import test_stage

    class ProductionOverlay(unittest.TestCase):
        def setUp(self):
            test_stage.Handoff.setUpClass()
            self.addCleanup(test_stage.Handoff.tearDownClass)
            self.fixture = test_stage.Handoff
            self.gen = checked.load_module(plan / 'P00/reference/scripts/r926_release_manifest.py', 'overlay_generator')
            self.pair = checked.load_module(Path(__file__).resolve().parents[1] / 'research/b05_pair_source.py', 'overlay_pair')
            self.advance = checked.load_module(plan / 'P00/payload/tools/r33/advance_release_version.py', 'overlay_advance')
            self.review = self.fixture.home / 'review'
            self.review.mkdir()
            self.existing = 'app/src/main/java/com/ethk3/novaonepro/LegacyBackupRecoveryR335.java'
            self.new = 'app/src/main/java/com/ethk3/novaonepro/ReviewFixture.java'
            original = stage.materialize('B01', stage.records('B01'))[self.existing]
            after_b01 = original + b'\n// review first stage\n'
            after_b02 = after_b01 + b'\n// review next stage\n'
            new_bytes = b'package com.ethk3.novaonepro; final class ReviewFixture {}\n'
            self.expected = after_b02
            def item(batch, name, original_after, before, after):
                payload = f'production/images/{batch}/{name}'
                p = self.review / payload
                p.parent.mkdir(parents=True, exist_ok=True)
                p.write_bytes(after)
                before_file = None
                if before is not None:
                    before_file = f'production/before/{batch}/{name}'
                    p = self.review / before_file
                    p.parent.mkdir(parents=True, exist_ok=True)
                    p.write_bytes(before)
                return {'path': name, 'originalAfterSha256': original_after,
                        'beforeSha256': 'NEW' if before is None else overlay.sha(before),
                        'afterSha256': overlay.sha(after), 'beforeFile': before_file,
                        'payload': payload, 'mode': '100644', 'action': 'ADD' if before is None else 'REPLACE'}
            self.spec = {'schema': overlay.SCHEMA, 'sourceBaseline': stage.BASE, 'stages': {
                'B01': {'files': [item('B01', self.existing, overlay.sha(original), None, after_b01)]},
                'B02': {'files': [item('B02', self.existing, None, after_b01, after_b02),
                                  item('B02', self.new, None, None, new_bytes)]}}}
            self.save()

        def save(self):
            (self.review / 'production/OVERLAY.json').write_text(json.dumps(self.spec))

        def test_sequential_overlay_and_retention(self):
            current = self.fixture.seed
            for batch in ('P00', 'B01', 'B02', 'B03'):
                out = self.fixture.home / ('effective-' + batch)
                receipt = checked.run_stage(stage, self.gen, self.pair, plan, current, batch, self.fixture.base,
                                            '0' * 64, dest=out, review_root=self.review)
                self.assertEqual(receipt['productionReview']['stageOperations'], 144)
                self.assertEqual(receipt['productionReview']['manifestValidation'],
                                 {'stages': 9, 'fileChanges': 144, 'distinctPaths': 130})
                self.assertEqual(receipt['productionReview']['distinctProductionPaths'], 130)
                if batch in ('B02', 'B03'):
                    self.assertEqual((out / self.existing).read_bytes(), self.expected)
                    self.assertTrue((out / self.new).is_file())
                tx = self.fixture.home / ('effective-tx-' + batch)
                self.advance.prepare(out, tx, 577311033, batch, '2026-11-20')
                self.advance.apply(out, tx)
                stage.git(out, 'add', '--', *checked.VERSION_FILES)
                self.fixture.commit(out)
                current = out
            (current / self.new).unlink()
            stage.git(current, 'add', '-u', '--', self.new)
            self.fixture.commit(current)
            with self.assertRaisesRegex(ValueError, 'REGISTERED_PREDECESSOR_BYTES_MISSING:B03:'):
                checked.run_stage(stage, self.gen, self.pair, plan, current, 'B04', self.fixture.base,
                                  '0' * 64, review_root=self.review)

        def test_mismatched_base_after_rejected(self):
            self.spec['stages']['B01']['files'][0]['originalAfterSha256'] = '0' * 64
            self.save()
            with self.assertRaisesRegex(ValueError, 'OVERLAY_ORIGINAL_AFTER_DRIFT:'):
                checked.run_stage(stage, self.gen, self.pair, plan, self.fixture.seed, 'P00', self.fixture.base,
                                  '0' * 64, review_root=self.review)

        def test_corrupt_image_rejected(self):
            patch = self.spec['stages']['B02']['files'][1]
            (self.review / patch['payload']).write_bytes(b'corrupt')
            with self.assertRaisesRegex(ValueError, 'OVERLAY_IMAGE_DIGEST:'):
                checked.run_stage(stage, self.gen, self.pair, plan, self.fixture.seed, 'P00', self.fixture.base,
                                  '0' * 64, review_root=self.review)

    return unittest.defaultTestLoader.loadTestsFromTestCase(ProductionOverlay)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan', required=True, type=Path)
    suite = configure(parser.parse_args().plan.resolve())
    sys.exit(not unittest.TextTestRunner(verbosity=2).run(suite).wasSuccessful())
