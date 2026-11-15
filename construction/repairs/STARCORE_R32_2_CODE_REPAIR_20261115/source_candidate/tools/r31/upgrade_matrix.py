#!/usr/bin/env python3
"""R31-B12-C01 upgrade matrix: overlay-install upgrade with a process kill at every durable cut point.

HOST layer on native SQLite (sqlite-jdbc), production classes from the gradle build. Not ART, not a device,
not an old APK. Every scenario label states what it is:

  EMPTY      HOST       first overlay onto a fresh base install (no runtime/research tables yet)
  CURRENT    SIMULATED  constructed predecessor install with protected roots (the real ~600MB DB is NOT_RUN)
  STRESS     SIMULATED  constructed larger predecessor (20k draws, 4MB model artifacts); not the 30GB original
  PARTIAL    HOST       every CURRENT cut point killed (JVM halt, no finally/commit), then reopened and resumed

The receipt is machine-readable; anything this host cannot execute is NOT_RUN, never PASS.
"""
import argparse, concurrent.futures, json, os, re, shutil, subprocess, sys, tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
FIXTURE = ROOT / 'verification/r31/p02/schema_mainchain/runs/20261104T112314Z/source_snapshot/MainChainSchemaR31Host.java'
HOST = ROOT / 'tools/r31/upgrade_host/UpgradeMatrixR31Host.java'
PHASES = ['PREPARE', 'VERIFY_SOURCE', 'TRANSFORM', 'VERIFY_TARGET', 'PUBLISH', 'CLEANUP']
SCENARIOS = {'EMPTY': 'HOST', 'CURRENT': 'SIMULATED', 'STRESS': 'SIMULATED'}
NOT_RUN = {
    'ORIGINAL_30GB_DB': 'NOT_RUN: no 30GB original database exists in this environment; STRESS is SIMULATED',
    'REAL_CURRENT_600MB_DB': 'NOT_RUN: the user device database is not available on the host; CURRENT is SIMULATED',
    'OLD_APK_READS_NEW_DB': 'NOT_RUN: needs an Android install of the previous APK (EMU/DEVICE); the journal is additive and never changes user_version',
    'ANDROID_OVERLAY_INSTALL_KILL': 'NOT_RUN: R31-B12-C01#A1b is an EMU item',
}


class Fail(Exception):
    pass


def need(ok, why):
    if not ok:
        raise Fail(why)


class Host:
    def __init__(self, work):
        sdk = Path(os.environ['ANDROID_HOME']) / 'platforms/android-36/android.jar'
        jdbc = Path(os.environ['SQLITE_JDBC'])
        classes = Path(os.environ.get('R31_CLASSES', ROOT / 'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'))
        plat, host = work / 'platform', work / 'host'
        plat.mkdir(); host.mkdir()
        shim = [str(p) for p in (ROOT / 'tools/r31/ddl_host').rglob('*.java')]
        shim += [str(ROOT / 'tools/r29/qual_host/shim/android/database/sqlite/SQLiteException.java')]
        shim += [str(p) for p in (ROOT / 'verification/r932/android-json').rglob('*.java')]
        text = (ROOT / 'tools/r31/verify_platform_readiness.py').read_text()
        adapters = eval(re.search(r"adapters=(\{.*?\})\n", text, re.S).group(1))
        for name, body in adapters.items():
            q = work / 'adapters' / name
            q.parent.mkdir(parents=True, exist_ok=True); q.write_text(body); shim.append(str(q))
        subprocess.run(['javac', '-nowarn', '-encoding', 'UTF-8', '-cp', str(sdk), '-d', str(plat), *shim], check=True)
        cp = os.pathsep.join(map(str, [plat, classes, sdk, jdbc]))
        subprocess.run(['javac', '-nowarn', '-encoding', 'UTF-8', '-cp', cp, '-d', str(host), str(FIXTURE), str(HOST)], check=True)
        self.cp = os.pathsep.join(map(str, [plat, host, classes, sdk, jdbc]))

    def run(self, *args, expect_kill=False):
        r = subprocess.run(['java', '-Xmx768m', '-cp', self.cp, 'com.ethk3.novaonepro.UpgradeMatrixR31Host', *map(str, args)],
                           capture_output=True, text=True, timeout=900)
        out = r.stdout + r.stderr
        if expect_kill:
            need(r.returncode == 137, 'expected a halt (137), got %d: %s' % (r.returncode, out[-3000:]))
        else:
            need(r.returncode == 0, 'host exit %d: %s' % (r.returncode, out[-6000:]))
        return out

    @staticmethod
    def line(out, tag):
        for row in out.splitlines():
            if row.startswith(tag + '|'):
                return row[len(tag) + 1:]
        return None

    def verify(self, db, columns_file=None):
        out = self.run('verify', db, *( [columns_file] if columns_file else []))
        return json.loads(self.line(out, 'VERIFY'))

    def upgrade(self, db, kill=0, free=-1, tamper=False):
        return self.run('upgrade', db, kill, free, *(['tamper'] if tamper else []), expect_kill=kill > 0)


def cut_points(out):
    return [row.split('|', 4) for row in out.splitlines() if row.startswith('CUT|')]


def verify_recovery(seed, reference, resumed, label):
    """A resumed upgrade equals the uninterrupted one: same roots as the seed, same schema, no step re-applied."""
    need(resumed['integrity'] == 'ok', label + ': integrity ' + str(resumed['integrity']))
    need(resumed['roots'] == seed['roots'], label + ': protected roots changed: ' + json.dumps(
        {k: [seed['roots'].get(k), resumed['roots'].get(k)] for k in seed['roots'] if seed['roots'].get(k) != resumed['roots'].get(k)}))
    need(resumed['schemaHash'] == reference['schemaHash'], label + ': schema differs from the uninterrupted upgrade')
    need(resumed['critical'] == reference['critical'] and resumed['critical'].startswith('MATCH'), label + ': critical ' + resumed['critical'])
    need(resumed['userVersion'] == seed['userVersion'], label + ': user_version changed')
    need(sorted(resumed['steps']) == sorted(reference['steps']), label + ': journaled steps differ')
    for key, row in resumed['steps'].items():
        need(row['status'] == 'COMPLETED', label + ': step not completed ' + key)
        need(row['attempt'] == reference['steps'][key]['attempt'], label + ': step re-applied (duplicate migration) ' + key + ' attempt=%s' % row['attempt'])
    for phase in PHASES:
        row = resumed['upgradeJournal'].get(phase)
        need(row and row['status'] == 'COMPLETED', label + ': phase not completed ' + phase)
        need(row['attempt'] <= 2, label + ': phase attempted more than kill+resume ' + phase)
    stamps = {k: v for k, v in resumed['stamps'].items() if k != 'UPGRADE'}
    need(stamps == {k: v for k, v in reference['stamps'].items() if k != 'UPGRADE'}, label + ': domain stamps differ')
    need(resumed['stamps'].get('UPGRADE', '').startswith('READY|'), label + ': upgrade not READY')
    need(resumed['r26Sessions'] == 1, label + ': one running session expected')
    return True


def run_upgrade_matrix(work, out_dir, workers):
    host = Host(work)
    receipt = {'format': 'R31_UPGRADE_MATRIX_V1', 'layer': 'HOST_NATIVE_SQLITE_NOT_ART_NOT_DEVICE', 'scenarios': {}, 'not_run': NOT_RUN}
    for scenario, label in SCENARIOS.items():
        seed_db = work / (scenario + '-seed.db')
        host.run('seed', seed_db, scenario)
        seed = host.verify(seed_db)
        columns = work / (scenario + '-columns.txt'); columns.write_text(seed['rootColumns'])
        ref_db = work / (scenario + '-ref.db'); shutil.copy(seed_db, ref_db)
        ref_out = host.upgrade(ref_db)
        need(host.line(ref_out, 'UPGRADE') and host.line(ref_out, 'UPGRADE').startswith('COMPLETE|'), scenario + ': reference upgrade ' + ref_out[-4000:])
        points = cut_points(ref_out)
        reference = host.verify(ref_db, columns)
        need(reference['roots'] == seed['roots'], scenario + ': reference upgrade changed protected roots')
        for phase in PHASES:
            need(reference['upgradeJournal'][phase]['attempt'] == 1, scenario + ': reference phase attempt ' + phase)
        # a second start re-enters only the idempotent owner: journal, steps and roots unchanged
        again = host.upgrade(ref_db)
        need(host.line(again, 'UPGRADE').startswith('COMPLETE|'), scenario + ': steady start')
        need(not cut_points(again), scenario + ': steady start re-entered a phase or a completed step: ' + str(cut_points(again)[:4]))
        steady = host.verify(ref_db, columns)
        need(steady['upgradeJournal'] == reference['upgradeJournal'] and steady['steps'] == reference['steps'] and steady['roots'] == seed['roots'],
             scenario + ': steady start changed journal/steps/roots')
        # kill points: every cut point for CURRENT; the upgrade-phase boundaries for EMPTY and STRESS
        indices = [int(p[1]) for p in points if scenario == 'CURRENT' or p[2].startswith('R31_UPGRADE:')]

        def one(k):
            db = work / ('%s-kill-%04d.db' % (scenario, k)); shutil.copy(seed_db, db)
            killed = host.upgrade(db, kill=k)
            at = host.line(killed, 'KILLED_AT'); need(at is not None and at.startswith('%d|' % k), '%s: kill %d not reached' % (scenario, k))
            resumed_out = host.upgrade(db)
            need(host.line(resumed_out, 'UPGRADE').startswith('COMPLETE|'), '%s: resume after kill %d: %s' % (scenario, k, resumed_out[-3000:]))
            facts = host.verify(db, columns)
            verify_recovery(seed, reference, facts, '%s kill@%d(%s)' % (scenario, k, at))
            os.remove(db)
            return {'cut': k, 'at': at, 'phaseAttempts': {p: facts['upgradeJournal'][p]['attempt'] for p in PHASES}}

        kills = []
        with concurrent.futures.ThreadPoolExecutor(max_workers=workers) as pool:
            for r in pool.map(one, indices):
                kills.append(r)
        receipt['scenarios'][scenario] = {
            'label': label, 'cutPoints': len(points), 'killed': len(kills), 'resumedEqualUninterrupted': True,
            'roots': seed['roots'], 'rootColumns': seed['rootColumns'], 'kills': kills,
            'reference': {'critical': reference['critical'], 'schemaHash': reference['schemaHash'], 'phaseHashes': {p: reference['upgradeJournal'][p]['hash'] for p in PHASES}},
        }
        print('R31_UPGRADE_MATRIX|%s|%s|cuts=%d|killed=%d|PASS' % (scenario, label, len(points), len(kills)), flush=True)
    # low disk: no addition happens, nothing is deleted, the same upgrade completes once space exists
    seed_db = work / 'CURRENT-seed.db'; seed = host.verify(seed_db); columns = work / 'CURRENT-columns.txt'
    low = work / 'low-disk.db'; shutil.copy(seed_db, low)
    blocked = host.upgrade(low, free=1)
    need(host.line(blocked, 'UPGRADE').startswith('BLOCKED_LOW_DISK|'), 'low disk not blocked: ' + blocked[-3000:])
    facts = host.verify(low, columns)
    need(facts['roots'] == seed['roots'] and facts['schemaHash'] == seed['schemaHash'], 'low disk changed roots or schema')
    need('TRANSFORM' not in facts['upgradeJournal'], 'low disk reached TRANSFORM')
    done = host.upgrade(low); need(host.line(done, 'UPGRADE').startswith('COMPLETE|'), 'upgrade after low disk')
    after = host.verify(low, columns); need(after['roots'] == seed['roots'] and after['upgradeJournal']['PREPARE']['attempt'] == 2, 'low disk retry facts')
    receipt['lowDisk'] = {'blocked': True, 'schemaUnchanged': True, 'rootsUnchanged': True, 'resumedAfterSpace': True}
    # a transform that changes one original root value is rolled back before commit and nothing is published
    bad = work / 'tamper.db'; shutil.copy(seed_db, bad)
    refused = host.upgrade(bad, tamper=True)
    need((host.line(refused, 'UPGRADE_FAILED') or '').startswith('R31_UPGRADE_ROOT_CHANGED:r27_certificate'), 'tamper not refused: ' + refused[-3000:])
    facts = host.verify(bad, columns)
    need(facts['roots'] == seed['roots'], 'tampered root committed')
    need(facts['upgradeJournal']['TRANSFORM']['status'] == 'FAILED' and 'PUBLISH' not in facts['upgradeJournal'], 'tamper journal')
    need(facts['stamps'].get('UPGRADE', '').startswith('FAILED|'), 'tamper stamp')
    fixed = host.upgrade(bad); need(host.line(fixed, 'UPGRADE').startswith('COMPLETE|'), 'recovery after refused transform')
    receipt['rootTamper'] = {'refused': 'R31_UPGRADE_ROOT_CHANGED:r27_certificate', 'rolledBack': True, 'published': False, 'recoveredByRollForward': True}
    # the receipt never claims what the host did not run
    need(receipt['scenarios']['CURRENT']['label'] == 'SIMULATED' and receipt['scenarios']['STRESS']['label'] == 'SIMULATED', 'constructed DBs must be SIMULATED')
    need(all(v.startswith('NOT_RUN') for v in receipt['not_run'].values()), 'unexecuted items must be NOT_RUN')
    need('PASS' not in json.dumps(receipt['not_run']), 'NOT_RUN item reported as PASS')
    receipt['status'] = 'PASS'
    out_dir.mkdir(parents=True, exist_ok=True)
    (out_dir / 'upgrade_matrix_receipt.json').write_text(json.dumps(receipt, indent=1, sort_keys=True))
    return receipt


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--out', default=str(ROOT / 'app/build/r312-host/run_upgrade_matrix_host'))
    ap.add_argument('--workers', type=int, default=max(2, min(6, os.cpu_count() or 2)))
    a = ap.parse_args()
    work = Path(tempfile.mkdtemp(prefix='r31-upgrade-matrix-'))
    try:
        r = run_upgrade_matrix(work, Path(a.out), a.workers)
        print('R31_UPGRADE_MATRIX_PASS ' + json.dumps({k: {'cuts': v['cutPoints'], 'killed': v['killed'], 'label': v['label']} for k, v in r['scenarios'].items()}))
    except Fail as failure:
        print('R31_UPGRADE_MATRIX_FAIL ' + str(failure)); sys.exit(1)
    finally:
        shutil.rmtree(work, ignore_errors=True)


if __name__ == '__main__':
    main()
