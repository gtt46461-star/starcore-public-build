#!/usr/bin/env python3
"""R31-B12-C02#A1 self-test: CI failures and skips are reported exactly; empty or class-name-only tests never PASS;
the real fault manifest and the real unit-test tree are checked (not only fixtures)."""
import json, sys, tempfile, unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import verification_receipt as vr

ROOT = Path(__file__).resolve().parents[2]


def junit(directory, cases):
    rows = []
    for name, kind in cases:
        inner = {'fail': '<failure message="boom">trace</failure>', 'error': '<error message="npe"/>', 'skip': '<skipped/>', 'ok': ''}[kind]
        rows.append('<testcase classname="pkg.T" name="%s" time="0.1">%s</testcase>' % (name, inner))
    Path(directory).mkdir(parents=True, exist_ok=True)
    (Path(directory) / 'TEST-pkg.T.xml').write_text('<?xml version="1.0"?><testsuite name="pkg.T" tests="%d">%s</testsuite>' % (len(cases), ''.join(rows)))


class StageStatus(unittest.TestCase):
    def setUp(self):
        self.tmp = Path(tempfile.mkdtemp())

    def test_failure_and_error_are_fail_even_if_the_job_claims_success(self):
        junit(self.tmp / 'a', [('x', 'ok'), ('y', 'fail')]); j = vr.parse_junit(self.tmp / 'a')
        self.assertEqual(j['failures'], ['pkg.T#y']); self.assertEqual(vr.stage_status('success', j), 'FAIL')
        junit(self.tmp / 'b', [('x', 'error')]); self.assertEqual(vr.stage_status('success', vr.parse_junit(self.tmp / 'b')), 'FAIL')

    def test_skips_are_named_and_never_a_plain_pass(self):
        junit(self.tmp / 'c', [('x', 'ok'), ('y', 'skip'), ('z', 'skip')]); j = vr.parse_junit(self.tmp / 'c')
        self.assertEqual(j['skipped'], ['pkg.T#y', 'pkg.T#z']); self.assertEqual(vr.stage_status('success', j), 'PASS_WITH_SKIPPED:2')

    def test_no_executed_test_is_empty_and_a_skipped_or_cancelled_job_is_not_run(self):
        (self.tmp / 'd').mkdir(); self.assertEqual(vr.stage_status('success', vr.parse_junit(self.tmp / 'd')), 'EMPTY')
        self.assertEqual(vr.stage_status('success', vr.parse_junit(self.tmp / 'missing')), 'EMPTY')
        for r in ('skipped', 'cancelled', '', None): self.assertEqual(vr.stage_status(r, None), 'NOT_RUN')
        self.assertEqual(vr.stage_status('failure', None, expect_tests=False), 'FAIL')
        junit(self.tmp / 'e', [('x', 'ok')]); self.assertEqual(vr.stage_status('success', vr.parse_junit(self.tmp / 'e')), 'PASS')

    def test_aggregate_is_not_pass_when_any_stage_is_not_run(self):
        for name, status in (('baseline', 'PASS'), ('instrumentation', 'NOT_RUN')):
            (self.tmp / ('stage-%s.json' % name)).write_text(json.dumps({'stage': name, 'status': status}))
        r = vr.aggregate(self.tmp); self.assertEqual(r['overall'], 'NOT_PASS'); self.assertEqual(r['device'], 'NOT_RUN')


class TestScan(unittest.TestCase):
    def test_empty_and_class_name_only_tests_are_refused(self):
        d = Path(tempfile.mkdtemp())
        (d / 'T.java').write_text('''package p; import org.junit.Test; public class T {
          @Test public void empty() { }
          @Test public void onlyComment() { /* later */ }
          @Test public void exists() throws Exception { Class.forName("p.Prod"); }
          @Test public void named() { assertNotNull(Prod.class); assertEquals("Prod", Prod.class.getSimpleName()); }
          @Test public void real() { assertEquals(4, 2 + 2); }
        }''')
        r = vr.scan_tests(d)
        self.assertEqual(r['methods'], 5)
        self.assertEqual(sorted(r['refused']), ['T#empty:EMPTY', 'T#exists:CLASS_NAME_ONLY', 'T#named:CLASS_NAME_ONLY', 'T#onlyComment:EMPTY'])

    def test_the_real_unit_tests_have_no_empty_or_class_name_only_method(self):
        r = vr.scan_tests(ROOT / 'app/src/test/java')
        self.assertGreater(r['methods'], 500); self.assertEqual(r['refused'], [])


class Manifest(unittest.TestCase):
    def test_the_real_fault_manifest_names_only_existing_executors(self):
        self.assertEqual(vr.check_manifest(ROOT / 'tools/r31/fault_injection_manifest.json', ROOT / 'construction/r31/HOST_SUITE.json', ROOT / 'app/src/test/java'), [])

    def test_an_unknown_executor_or_an_android_pass_claim_is_refused(self):
        d = Path(tempfile.mkdtemp()); m = json.loads((ROOT / 'tools/r31/fault_injection_manifest.json').read_text())
        m['scenarios'][0]['executed_by'] = ['HOST:not_a_host', 'pkg.Nope#nothing']; m['scenarios'][1]['android'] = 'PASS'
        (d / 'm.json').write_text(json.dumps(m))
        p = vr.check_manifest(d / 'm.json', ROOT / 'construction/r31/HOST_SUITE.json', ROOT / 'app/src/test/java')
        self.assertIn('F01_LOCK_CONTENTION:UNKNOWN_HOST:HOST:not_a_host', p); self.assertIn('F01_LOCK_CONTENTION:UNKNOWN_TEST:pkg.Nope#nothing', p)
        self.assertIn('F02_PROCESS_DEATH:ANDROID_CLAIM_WITHOUT_RECEIPT', p)

    def test_the_workflow_marks_every_unexecuted_stage_not_run(self):
        wf = (ROOT / '.github/workflows/r31-verification.yml').read_text()
        for stage in ('baseline', 'contracts', 'android_build', 'instrumentation', 'artifact_receipt'):
            self.assertIn('\n  %s:' % stage, wf)
        self.assertIn('if: always()', wf); self.assertIn('needs.instrumentation.result', wf)
        self.assertNotIn('continue-on-error: true', wf)


class AndroidOutputTransport(unittest.TestCase):
    def setUp(self):
        import android_delivery_protocol as protocol
        self.verify = protocol.verify
        fixture = ROOT / 'verification/r31/android-delivery-protocol'
        self.instrumentation = (fixture / '027-instrumentation.txt').read_text()
        self.logcat = (fixture / '027-logcat-test-events.txt').read_text()

    def test_actual_027_markers_are_bound_to_their_test_in_logcat(self):
        self.assertNotIn('FULL_REPORT_PUBLISHED|PROVIDER_READBACK_HASH_PASS', self.instrumentation)
        result = self.verify(self.instrumentation, self.logcat)
        self.assertTrue(result['passed'], result['problems'])
        self.assertEqual(7, len(result['markers']))

    def test_no_marker_skip_foreign_process_or_old_run_can_substitute(self):
        variants = [
            (self.instrumentation, self.logcat.replace('R31Q_ANDROID|REAL_API_FETCH', 'MISSING|REAL_API_FETCH')),
            (self.instrumentation.replace('INSTRUMENTATION_STATUS_CODE: 0', 'INSTRUMENTATION_STATUS_CODE: -3', 1), self.logcat),
            (self.instrumentation, self.logcat.replace('  2468  2503 I System.out:', '  9876  2503 I System.out:')),
            (self.instrumentation, self.logcat + self.logcat),
            (self.instrumentation, ''),
            (self.instrumentation + self.instrumentation, self.logcat),
            (self.instrumentation.replace('OK (7 tests)', 'FAILURES!!!'), self.logcat),
        ]
        for protocol, logcat in variants:
            self.assertFalse(self.verify(protocol, logcat)['passed'])


if __name__ == '__main__':
    unittest.main(verbosity=2)
