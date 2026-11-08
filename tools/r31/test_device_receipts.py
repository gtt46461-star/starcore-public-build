#!/usr/bin/env python3
"""Self-test of the R31-B12-C03 device tools on synthetic samples (no device; proves only the evaluation rules)."""
import json, sys, tempfile, unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import collect_device_receipts as col
import accept_device_receipts as acc

H = 3600000


def samples(hours, gap_at=None, conditions=col.CONDITIONS, growth_mib_per_day=10, late=0, unsettled=0, pss_drift=1.0):
    rows, t0 = [], 1_800_000_000_000
    targets = []
    for k in range(int(hours * 60)):
        t = t0 + k * 60000
        targets.append(['2026%08d' % k, t - 30000, t, t + 60000, 1])
    for i, f in enumerate(targets[:late]): f[1] = f[2] - 1000
    for f in targets[late:late + unsettled]: f[3] = 0; f[4] = -1
    n = int(hours * 60)
    for k in range(n):
        if gap_at is not None and gap_at <= k < gap_at + 10:
            continue
        t = t0 + k * 60000
        base = 400 * 1024 * 1024 + int(growth_mib_per_day * 1024 * 1024 * k / 1440)
        r = {'deviceWallMs': t, 'bootId': 'boot-1', 'sourceCommit': 'c' * 40, 'apkSha256': 'a' * 64, 'condition': conditions[k % len(conditions)],
             'pssKb': int(200000 * (pss_drift if k > n / 2 else 1.0)), 'dirBytes': {'databases': base, 'files': 1024, 'UNCLASSIFIED': 5 * 1024 * 1024}}
        if k == n - 1:
            r['db'] = {'pulled': True, 'frozen': targets[:-2], 'draws': [[f[0], f[2]] for f in targets[:-2]], 'tasks': [['r27-live', 'RUNNING', 5, t, '']]}
        rows.append(r)
    f = Path(tempfile.mkdtemp()) / 's.jsonl'
    f.write_text(json.dumps({'header': True, 'model': 'V2353A', 'intervalSeconds': 60}) + '\n' + ''.join(json.dumps(r) + '\n' for r in rows))
    m = col.merge_windows([f]); m['scopeProblems'] = col.verify_scope(m)
    return m


class Device(unittest.TestCase):
    def test_short_run_gap_or_missing_condition_blocks_and_nothing_is_interpolated(self):
        g = acc.evaluate_gate(samples(3), '24h'); self.assertEqual(g['DURATION']['status'], 'BLOCKED')
        g = acc.evaluate_gate(samples(25, gap_at=300), '24h'); self.assertEqual(g['SCOPE']['status'], 'BLOCKED'); self.assertEqual(len(g['SCOPE']['gaps']), 1)
        g = acc.evaluate_gate(samples(25, conditions=col.CONDITIONS[:3]), '24h'); self.assertEqual(g['CONDITIONS']['status'], 'BLOCKED')
        self.assertIn('USER_FORCE_STOP', g['CONDITIONS']['missing'])
        v = acc.write_verdict(Path(tempfile.mkdtemp()) / 'v.json', acc.evaluate_gate(samples(25), '24h'), '24h')
        self.assertNotEqual(v['overall'], 'PASS', 'memory windows of a 24h run cannot be compared, so the verdict is not PASS')
        self.assertEqual(v['gates']['MEMORY']['status'], 'BLOCKED')

    def test_fixed_denominators_for_freeze_settlement_and_storage(self):
        g = acc.evaluate_gate(samples(52, late=40, unsettled=3, growth_mib_per_day=80), '24h')
        self.assertEqual(g['FREEZE']['status'], 'FAIL'); self.assertEqual(g['FREEZE']['eligible'] - g['FREEZE']['frozenWithMargin'], 40)
        self.assertEqual(g['SETTLEMENT']['status'], 'FAIL'); self.assertEqual(g['SETTLEMENT']['validFrozenWithDraw'] - g['SETTLEMENT']['settled'], 3)
        self.assertEqual(g['STORAGE']['status'], 'FAIL'); self.assertGreater(g['STORAGE']['dailyGrowthLastTwoDaysMib'], 50)
        self.assertEqual(g['STORAGE']['unclassifiedPeakMib'], 5.0)
        ok = acc.evaluate_gate(samples(52), '24h')
        for k in ('DURATION', 'CONDITIONS', 'SCOPE', 'FREEZE', 'SETTLEMENT', 'MEMORY', 'STORAGE', 'TASKS'): self.assertEqual(ok[k]['status'], 'PASS', k)
        self.assertEqual(acc.write_verdict(Path(tempfile.mkdtemp()) / 'v.json', ok, '24h')['overall'], 'PASS')
        self.assertEqual(acc.evaluate_gate(samples(52), '72h')['DURATION']['status'], 'BLOCKED')
        self.assertEqual(acc.evaluate_gate(samples(52, pss_drift=1.3), '24h')['MEMORY']['status'], 'FAIL')

    def test_mixed_source_is_reported_not_averaged(self):
        m = samples(2); m['samples'][5]['sourceCommit'] = 'd' * 40
        self.assertIn('MIXED_SOURCE:' + ','.join(sorted(['c' * 40, 'd' * 40])), col.verify_scope(m))


if __name__ == '__main__':
    unittest.main(verbosity=2)
