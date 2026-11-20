"""Verify current cross-batch handoffs and the independently stored work-order attachments."""
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest

import check_work_orders as orders

PLAN = Path(sys.argv.pop(1)).resolve(strict=True)


class MaterialTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.addClassCleanup(cls.temp.cleanup)
        cls.root = Path(cls.temp.name)
        # Link files read-only when possible. The damage test unlinks its private copy.
        def copy(src, dst):
            try:
                return os.link(src, dst)
            except OSError:
                return shutil.copyfile(src, dst)

        cls.plan = cls.root / 'plan'
        shutil.copytree(PLAN, cls.plan, copy_function=copy)
        cls.effective = cls.root / 'images'
        cls.effective.mkdir()
        cls.index = orders.build(cls.plan, cls.effective)
        cls.issues = {i['id']: i for i in cls.index['issues']}
        cls.manifest = json.loads((cls.effective / 'control/APPLY_MANIFEST_R335.json').read_bytes())
        cls.tools = cls.root / 'tools'
        orders.prepare_tools.prepare(cls.plan, cls.tools)

    def material(self, issue, basename):
        return next(m for m in self.issues[issue]['materials'] if Path(m['path']).name == basename)

    def test_complete_index(self):
        expected = json.loads((orders.REVIEW / 'WORK_ORDERS.json').read_bytes())
        self.assertEqual(expected, self.index)
        self.assertEqual(self.index['materialCounts'], {
            'PRODUCTION': 199, 'PLAN_TOOL': 81, 'CONTRACT_OR_EVIDENCE': 68,
            'PRESERVED_BASELINE': 12, 'CROSS_STAGE_REFERENCE': 5})
        self.assertEqual(len(self.issues), 66)
        self.assertTrue(all(batch['acceptanceEntries'] for batch in self.index['batches']))

    def test_refs_at_consumer_stage(self):
        for issue, name, stage in [('R335-RE19', 'SettlementEngine.java', 'B02'),
                                   ('R335-RT09', 'ReportSnapshotR30.java', 'B07')]:
            item = self.material(issue, name)
            current = item['current']
            self.assertEqual((current['root'], current['stage']), ('EFFECTIVE', stage))
            self.assertNotEqual(current['sha256'], item['original']['sha256'])
            self.assertEqual(current['sha256'], hashlib.sha256((self.effective / current['file']).read_bytes()).hexdigest())
            self.assertEqual(self.material(issue, name)['kind'], 'CROSS_STAGE_REFERENCE')

    def test_prepared_checkers(self):
        item = self.material('R335-D06', 'check_trace.py')
        self.assertEqual(item['current']['root'], 'TOOLS')
        self.assertNotEqual(item['current']['sha256'], item['original']['sha256'])
        for stage in self.index['batches']:
            for entry in stage['acceptanceEntries']:
                root = {'TOOLS': self.tools, 'PLAN': self.plan, 'REVIEW': orders.REVIEW}[entry['root']]
                self.assertTrue((root / entry['file']).is_file())
                if 'sha256' in entry:
                    self.assertEqual(hashlib.sha256((root / entry['file']).read_bytes()).hexdigest(), entry['sha256'])
        self.assertEqual(item['current']['sha256'], hashlib.sha256((self.tools / item['current']['file']).read_bytes()).hexdigest())

    def test_failed_export_does_not_publish(self):
        name = self.material('R335-D06', 'check_trace.py')['original']['issueCopy']
        path, output = self.plan / name, self.root / 'published'
        original = path.read_bytes()
        path.unlink()
        command = [sys.executable, '-B', orders.__file__, str(self.plan), '--emit-to', str(output)]
        try:
            attempt = subprocess.run(command, capture_output=True, text=True)
            self.assertNotEqual(attempt.returncode, 0)
            self.assertIn('OVERLAY_IMAGE_MISSING:', attempt.stderr)
            self.assertFalse(output.exists())
        finally:
            path.write_bytes(original)
        subprocess.run(command, stdout=subprocess.PIPE, check=True)
        self.assertTrue((output / 'control/APPLY_MANIFEST_R335.json').is_file())
        user_file = output / 'retained'
        user_file.write_text('keep')
        self.assertNotEqual(subprocess.run(command, capture_output=True).returncode, 0)
        self.assertEqual(user_file.read_text(), 'keep')

    def test_missing_or_changed_issue_copy_refused(self):
        name = self.material('R335-D06', 'check_trace.py')['original']['issueCopy']
        path = self.plan / name
        original = path.read_bytes()
        path.unlink()
        try:
            with self.assertRaisesRegex(ValueError, 'OVERLAY_IMAGE_MISSING:'):
                orders.material_index(self.plan, self.manifest)
            path.write_bytes(b'Truncated issue copy\n')
            with self.assertRaisesRegex(ValueError, 'OVERLAY_IMAGE_DIGEST:'):
                orders.material_index(self.plan, self.manifest)
        finally:
            path.write_bytes(original)


if __name__ == '__main__':
    unittest.main(verbosity=2)
