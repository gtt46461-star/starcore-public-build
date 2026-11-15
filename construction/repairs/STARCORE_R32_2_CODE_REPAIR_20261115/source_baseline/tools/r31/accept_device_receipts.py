#!/usr/bin/env python3
"""R31-B12-C03 device acceptance over merged evidence (tools/r31/collect_device_receipts.py merge output).

evaluate_gate  every gate is computed with a fixed denominator from the evidence and the unchanged thresholds of
               construction/STARCORE_R31_FINAL_20261104/ledgers/acceptance_config.json:
                 DURATION       observed hours >= initial_device_hours (24h) or final_device_hours (72h)
                 CONDITIONS     each operator condition recorded separately (never merged)
                 SCOPE          one source commit and one APK; no sampling gap (a gap blocks, it is never interpolated)
                 FREEZE         eligible targets frozen >= minimum_predraw_margin_ms before the deadline / all eligible
                                >= eligible_realtime_freeze_coverage_min
                 SETTLEMENT     settled / valid frozen with a draw >= settlement_coverage_of_valid_frozen_records_min
                 MEMORY         after memory_warmup_hours: last-window p95 PSS / first-window p95 <= the configured ratio
                 STORAGE        normal peak <= disk_normal_hard_admission_mib; the last two days' growth per day <=
                                post_warmup_daily_growth_target_mib; protected bytes and UNCLASSIFIED bytes reported on
                                their own lines and never subtracted from the total
                 TASKS          no task stays without progress and without a stated reason
               Missing sources or too little time make the gate BLOCKED, not PASS.
write_verdict  overall PASS only when every gate is PASS; otherwise the first non-PASS state. All anomalous samples kept.
"""
import argparse, json, math, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
CONFIG = ROOT / 'construction/STARCORE_R31_FINAL_20261104/ledgers/acceptance_config.json'
CONDITIONS = ('FOREGROUND', 'SCREEN_OFF', 'BACKGROUND', 'BATTERY_RESTRICTED', 'NETWORK_LOSS_RECOVERY', 'ONE_KEY_CLEANUP', 'USER_FORCE_STOP')


def p95(xs):
    xs = sorted(x for x in xs if x is not None)
    return xs[min(len(xs) - 1, int(math.ceil(.95 * len(xs))) - 1)] if xs else None


def gate(status, **facts):
    return dict(status=status, **facts)


def evaluate_gate(merged, phase='24h', config=None):
    cfg = config or json.loads(CONFIG.read_text())
    rows = merged.get('samples', []); g = {}
    need_h = cfg['initial_device_hours'] if phase == '24h' else cfg['final_device_hours']
    if not rows:
        return {k: gate('BLOCKED', reason='NO_SAMPLES') for k in ('DURATION', 'CONDITIONS', 'SCOPE', 'FREEZE', 'SETTLEMENT', 'MEMORY', 'STORAGE', 'TASKS')}
    t0, t1 = rows[0]['deviceWallMs'], rows[-1]['deviceWallMs']; hours = (t1 - t0) / 3.6e6
    g['DURATION'] = gate('PASS' if hours >= need_h else 'BLOCKED', observedHours=round(hours, 3), requiredHours=need_h)
    seen = {c: sum(1 for r in rows if r.get('condition') == c) for c in CONDITIONS}
    g['CONDITIONS'] = gate('PASS' if all(seen.values()) else 'BLOCKED', samplesPerCondition=seen, missing=[c for c, n in seen.items() if not n])
    problems = merged.get('scopeProblems', [])
    g['SCOPE'] = gate('PASS' if not problems else 'BLOCKED', problems=problems, gaps=merged.get('gaps', []))
    # latest database copy is the denominator source for freeze/settlement
    dbs = [r['db'] for r in rows if r.get('db', {}).get('pulled')]
    if not dbs:
        g['FREEZE'] = gate('BLOCKED', reason='NO_DATABASE_SAMPLE'); g['SETTLEMENT'] = gate('BLOCKED', reason='NO_DATABASE_SAMPLE')
    else:
        db = dbs[-1]; draws = {issue: open_ms for issue, open_ms in db.get('draws', [])}; frozen = db.get('frozen', [])
        window = [f for f in frozen if f[2] and t0 <= f[2] <= t1]
        eligible = len(window); ok = [f for f in window if f[1] and f[2] - f[1] >= cfg['minimum_predraw_margin_ms']]
        cov = len(ok) / eligible if eligible else None
        g['FREEZE'] = gate('BLOCKED' if not eligible else ('PASS' if cov >= cfg['eligible_realtime_freeze_coverage_min'] else 'FAIL'),
                           eligible=eligible, frozenWithMargin=len(ok), coverage=cov, late=[f[0] for f in window if f not in ok][:200])
        valid = [f for f in window if f[0] in draws]; settled = [f for f in valid if f[3] and f[4] in (0, 1)]
        sc = len(settled) / len(valid) if valid else None
        g['SETTLEMENT'] = gate('BLOCKED' if not valid else ('PASS' if sc >= cfg['settlement_coverage_of_valid_frozen_records_min'] else 'FAIL'),
                               validFrozenWithDraw=len(valid), settled=len(settled), coverage=sc, unsettled=[f[0] for f in valid if f not in settled][:200])
    warm = t0 + cfg['memory_warmup_hours'] * 3.6e6; span = cfg['memory_comparison_window_hours'] * 3.6e6
    first = [r.get('pssKb') for r in rows if warm <= r['deviceWallMs'] < warm + span]; last = [r.get('pssKb') for r in rows if r['deviceWallMs'] >= t1 - span]
    if not first or not last or t1 - warm < 2 * span:
        g['MEMORY'] = gate('BLOCKED', reason='COMPARISON_WINDOWS_NOT_COVERED', firstSamples=len(first), lastSamples=len(last))
    else:
        ratio = p95(last) / p95(first)
        g['MEMORY'] = gate('PASS' if ratio <= cfg['memory_constant_workload_last_vs_first_p95_pss_ratio_max'] else 'FAIL', firstP95Kb=p95(first), lastP95Kb=p95(last), ratio=ratio)
    mib = 1024 * 1024
    totals = [(r['deviceWallMs'], sum(v for v in r['dirBytes'].values() if v)) for r in rows if r.get('dirBytes')]
    unclassified = max((r['dirBytes'].get('UNCLASSIFIED') or 0 for r in rows if r.get('dirBytes')), default=0)
    if not totals:
        g['STORAGE'] = gate('BLOCKED', reason='NO_STORAGE_SAMPLES')
    else:
        peak = max(v for _, v in totals) / mib
        two_days = [v for t, v in totals if t >= t1 - 2 * 86400000]
        growth = (two_days[-1] - two_days[0]) / mib / 2 if len(two_days) > 1 and t1 - t0 >= 2 * 86400000 else None
        status = 'FAIL' if peak > cfg['disk_normal_hard_admission_mib'] else ('BLOCKED' if growth is None else ('PASS' if growth <= cfg['post_warmup_daily_growth_target_mib'] else 'FAIL'))
        g['STORAGE'] = gate(status, peakMib=round(peak, 2), dailyGrowthLastTwoDaysMib=growth, unclassifiedPeakMib=round(unclassified / mib, 2),
                            protected='reported by the app report; never subtracted from the total', note='a total is never reduced by excluding protected or unclassified bytes')
    tasks = dbs[-1].get('tasks', []) if dbs else []
    silent = [t[0] for t in tasks if t[1] in ('BLOCKED', 'YIELDED', 'READY') and not t[4] and (not t[3] or t[3] < t1 - 6 * 3.6e6)]
    g['TASKS'] = gate('BLOCKED' if not tasks else ('PASS' if not silent else 'FAIL'), tasks=len(tasks), silentWithoutReason=silent[:200])
    return g


def write_verdict(path, gates, phase):
    order = ('FAIL', 'BLOCKED')
    states = [v['status'] for v in gates.values()]
    overall = 'PASS' if states and all(s == 'PASS' for s in states) else next(s for s in order if s in states) if any(s in order for s in states) else 'BLOCKED'
    verdict = {'format': 'R31_DEVICE_VERDICT_V1', 'phase': phase, 'overall': overall, 'gates': gates, 'naturalQuality': 'NOT_PROVEN',
               'rule': 'failed windows are kept; a rerun after a fix is a new verdict, never an average with the failure removed'}
    Path(path).write_text(json.dumps(verdict, indent=1, ensure_ascii=False) + '\n')
    return verdict


def main():
    ap = argparse.ArgumentParser(); ap.add_argument('merged'); ap.add_argument('--phase', choices=['24h', '72h'], default='24h'); ap.add_argument('--out', required=True)
    a = ap.parse_args()
    v = write_verdict(a.out, evaluate_gate(json.loads(Path(a.merged).read_text()), a.phase), a.phase)
    print(json.dumps({'overall': v['overall'], 'gates': {k: g['status'] for k, g in v['gates'].items()}}))
    sys.exit(0 if v['overall'] == 'PASS' else 1)


if __name__ == '__main__':
    main()
