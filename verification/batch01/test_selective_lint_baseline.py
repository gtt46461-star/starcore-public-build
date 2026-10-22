"""Execute the original exact-advisory generator against retained real inputs.

The 373-finding case reconstructs XML from the original finding ledger; it is
explicitly not a recovered raw lint report or a fresh lint execution.
"""
import copy
import hashlib
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]
PLAN = Path(os.environ.get('STARCORE_PLAN_ROOT', ROOT / 'verification/canonical/lint/inputs'))
AUDIT = PLAN / '35_REPLAY/workspace/audit_cp12/lint'


class SelectiveBaselineContract(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix='starcore-baseline-')
        self.addCleanup(self.temporary.cleanup)
        self.work = Path(self.temporary.name)
        self.project = self.work / 'project'
        self.audit = self.work / 'audit_cp12/lint'
        self.audit.mkdir(parents=True)
        for name in ['LINT_FINDING_MASTER_CP12.json', 'INCREMENTAL_LINT_DISPOSITIONS_CP12.json']:
            shutil.copyfile(AUDIT / name, self.audit / name)
        baseline = self.project / 'app/lint-baseline.xml'
        baseline.parent.mkdir(parents=True)
        self.original_baseline = (ROOT / 'app/lint-baseline.xml').read_bytes()
        baseline.write_bytes(self.original_baseline)
        self.report = ET.fromstring(self.original_baseline)
        for finding in self.report:
            finding.set('severity', 'Warning')
            for location in finding.findall('location'):
                location.set('file', str((self.project / 'app' / location.get('file')).resolve()))
        for card_id in ['FILE-CAN-265', 'FILE-CAN-267']:
            card = json.loads((PLAN / '31_FILE_CONSTRUCTION_CARDS/card_data' / (card_id + '.json')).read_text())
            name = card['path']
            current = ROOT / name
            self.assertEqual(hashlib.sha256(current.read_bytes()).hexdigest(), card['after_sha256'])
            destination = self.project / name
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(current, destination)
            oldroot = self.work / 'current_source/STARCORE_5.4.2.23_R980_SOURCE'
            old = oldroot / name
            old.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(PLAN / card['after_ref'], old)
            command = ['patch', '-R', '--batch', '-p1', '-i', str(PLAN / '31_FILE_CONSTRUCTION_CARDS' / card['patch'])]
            result = subprocess.run(command, cwd=oldroot, capture_output=True, text=True)
            self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
            self.assertEqual(hashlib.sha256(old.read_bytes()).hexdigest(), card['before_sha256'])

    def execute(self, report, expected_exit, name):
        path = self.work / 'input.xml'
        ET.ElementTree(report).write(path, encoding='utf-8', xml_declaration=True)
        baseline = self.project / 'app/lint-baseline.xml'
        before = baseline.read_bytes()
        command = [sys.executable, str(AUDIT / 'build_selective_baseline.py'), str(self.project), str(path)]
        result = subprocess.run(command, cwd=self.work, capture_output=True, text=True)
        receipt = json.loads((self.audit / 'SELECTIVE_BASELINE_RECEIPT_CP12.json').read_text())
        output = os.environ.get('STARCORE_TEST_EVIDENCE_DIR')
        if output:
            destination = Path(output)
            destination.mkdir(parents=True, exist_ok=True)
            (destination / (name + '.stdout.log')).write_text(result.stdout)
            (destination / (name + '.stderr.log')).write_text(result.stderr)
            (destination / (name + '.input.xml')).write_bytes(path.read_bytes())
            receipt.update(command=command, exit_code=result.returncode, test=self.id(),
                           generator_sha256=hashlib.sha256((AUDIT / 'build_selective_baseline.py').read_bytes()).hexdigest(),
                           input_kind='ORIGINAL_LEDGER_XML_RECONSTRUCTION' if name == 'original-373' else 'ORIGINAL_BASELINE_MUTATION',
                           no_fresh_lint_claim=True)
            (destination / (name + '.json')).write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')
        self.assertEqual(result.returncode, expected_exit, result.stdout + result.stderr)
        if expected_exit:
            self.assertEqual(baseline.read_bytes(), before, 'Rejected input must not overwrite baseline')
        return receipt, baseline.read_bytes()

    def test_exact_59_are_stable_and_idempotent(self):
        self.assertEqual(self.original_baseline, (PLAN / '32_METHOD_PATCH_BLUEPRINTS/FINAL_CANONICAL_AFTER/app/lint-baseline.xml').read_bytes())
        first, data = self.execute(self.report, 0, 'exact-59-first')
        self.assertEqual(first['approved_residual_count'], 59)
        self.assertEqual(first['rejected_count'], 0)
        self.assertEqual(data, self.original_baseline)
        second, repeated = self.execute(self.report, 0, 'exact-59-repeat')
        self.assertEqual(repeated, data)
        self.assertEqual(second['matches'], first['matches'])

    def test_original_373_refuse_all_314_unapproved(self):
        report = ET.Element('issues')
        rows = json.loads((AUDIT / 'LINT_FINDING_MASTER_CP12.json').read_text())['rows']
        for row in rows:
            issue = ET.SubElement(report, 'issue', id=row['detector'], severity=row['severity'], message=row['message'], errorLine1=row['error_line'])
            ET.SubElement(issue, 'location', file=str(self.project / row['file']), line=str(row['line']), column=str(row['column']))
        receipt, _ = self.execute(report, 2, 'original-373')
        self.assertEqual(len(rows), 373)
        self.assertEqual(receipt['approved_residual_count'], 59)
        self.assertEqual(receipt['rejected_count'], 314)

    def mutation(self, name, mutate):
        report = copy.deepcopy(self.report)
        mutate(report)
        receipt, _ = self.execute(report, 2, name)
        self.assertEqual(receipt['rejected_count'], 1)
        self.assertEqual(receipt['approved_residual_count'], 58)
        recovered, data = self.execute(self.report, 0, name + '-recovery')
        self.assertEqual(recovered['rejected_count'], 0)
        self.assertEqual(data, self.original_baseline)

    def test_error_severity_is_not_approved(self):
        self.mutation('error-severity', lambda r: r[0].set('severity', 'Error'))

    def test_changed_anchor_is_not_approved(self):
        self.mutation('changed-anchor', lambda r: r[0].set('errorLine1', 'ed.commit(); unapproved();'))

    def test_new_message_is_not_approved(self):
        self.mutation('new-message', lambda r: r[0].set('message', 'unapproved new warning'))

    def test_other_file_is_not_approved(self):
        self.mutation('other-file', lambda r: r[0].find('location').set('file', str(self.project / 'app/src/main/java/Other.java')))

    def test_outside_project_is_not_approved(self):
        self.mutation('outside-project', lambda r: r[0].find('location').set('file', str(self.work / 'outside.java')))

    def test_duplicate_does_not_reuse_one_approval(self):
        report = copy.deepcopy(self.report)
        report.append(copy.deepcopy(report[0]))
        receipt, _ = self.execute(report, 2, 'duplicate-approval')
        self.assertEqual(receipt['approved_residual_count'], 59)
        self.assertEqual(receipt['rejected_count'], 1)

    def test_layout_growth_is_not_approved(self):
        layout = self.project / 'app/src/main/res/layout/activity_main.xml'
        tree = ET.parse(layout)
        ET.SubElement(tree.getroot(), 'TextView')
        tree.write(layout, encoding='utf-8')
        receipt, _ = self.execute(self.report, 2, 'layout-growth')
        self.assertEqual(receipt['rejected_count'], 1)
        self.assertEqual(receipt['approved_residual_count'], 58)


if __name__ == '__main__':
    unittest.main(verbosity=2)
