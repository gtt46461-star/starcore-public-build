#!/usr/bin/env python3
"""Self-test of tools/r312_gate.py in a throwaway git repository.

Proves the gate refuses every shortcut the R31.2 contract forbids and closes a batch only when all
conditions hold. Uses synthetic BUILD_SUMMARY imports; it does not build Android code.
"""
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

PLAN = Path(__file__).resolve().parents[1]
REL = 'construction/STARCORE_R31_FINAL_20261104'
results = []


def run(cmd, cwd, env=None, ok=None):
    r = subprocess.run(cmd, cwd=cwd, env=env, text=True, capture_output=True)
    return r.returncode, r.stdout + r.stderr


def git(repo, *a):
    subprocess.run(['git', '-c', 'user.email=t@t', '-c', 'user.name=t', *a], cwd=repo, check=True, capture_output=True)


def expect(name, cond, detail=''):
    results.append({'case': name, 'status': 'PASS' if cond else 'FAIL', 'detail': '' if cond else detail[-600:]})


def main():
    tmp = Path(tempfile.mkdtemp(prefix='r312gate-'))
    try:
        repo = tmp / 'repo'
        (repo / 'app/src/main/java/com/ethk3/novaonepro').mkdir(parents=True)
        shutil.copytree(PLAN, repo / REL, ignore=shutil.ignore_patterns('__pycache__', 'evidence', 'baseline_source', 'reference', 'START_HERE.html'))
        src = repo / 'app/src/main/java/com/ethk3/novaonepro/DbStore.java'
        src.write_text('class DbStore {}\n')
        git(repo, 'init', '-q')
        git(repo, 'add', '-A')
        git(repo, 'commit', '-qm', 'base')
        base = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=repo, text=True).strip()
        env = dict(os.environ, R312_TEST_RESUME_COMMIT=base, PYTHONDONTWRITEBYTECODE='1')
        G = [sys.executable, str(repo / REL / 'tools/r312_gate.py')]
        L = json.loads((repo / REL / 'ledgers/r312_batches.json').read_text())
        s00, s01 = L['batches'][0], L['batches'][1]

        rc, out = run(G + ['check-plan'], repo, env)
        expect('check-plan passes on the shipped plan', rc == 0, out)
        rc, out = run(G + ['status'], repo, env)
        expect('first open batch is S00', rc == 0 and json.loads(out)['open_batch'] == 'S00', out)

        rc, out = run(G + ['close', '--batch', 'S01'], repo, env)
        expect('cannot close a batch out of order', rc != 0 and 'not the open batch' in out, out)

        d = repo / 'construction/r31/r312'
        (d / 'S00').mkdir(parents=True)
        (d / 'HOST_SUITE.json').write_text(json.dumps({'format': 'R312_HOST_SUITE_V1', 'pre_build': [], 'host': [{'id': 'ok', 'command': 'true'}]}))
        (d / 'S00/BINDINGS.json').write_text(json.dumps({'items': {i: ['x.T#t'] for i in s00['local_items']}}))
        git(repo, 'add', '-A')
        git(repo, 'commit', '-qm', 's00')

        rc, out = run(G + ['close', '--batch', 'S00'], repo, env)
        expect('cannot close without a build summary', rc != 0 and 'run build first' in out, out)

        def summary(batch, **kw):
            head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=repo, text=True).strip()
            s = {'format': 'R312_BUILD_SUMMARY_V1', 'batch': batch, 'source_head': head, 'environment': 'CI',
                 'gradle': {'state': 'PASS', 'exit': 0, 'unit_task_executed': True},
                 'unit': {'tests': 485, 'failures': 0, 'errors': 0, 'skipped': 0},
                 'tests': {'x.T#t': {'status': 'PASS'}}, 'host': [{'id': 'ok', 'status': 'PASS'}], 'host_failed': []}
            for k, v in kw.items():
                s[k] = v
            f = tmp / f'{batch}.json'
            f.write_text(json.dumps(s))
            rc, out = run(G + ['build', '--batch', batch, '--import', str(f)], repo, env)
            assert rc == 0, out
            git(repo, 'add', '-A')
            git(repo, 'commit', '-qm', 'summary ' + batch)

        summary('S00', gradle={'state': 'NOT_RUN'})
        rc, out = run(G + ['close', '--batch', 'S00'], repo, env)
        expect('gradle must actually run', rc != 0 and 'gradle gate not executed' in out, out)

        summary('S00', gradle={'state': 'FAIL', 'exit': 1, 'unit_task_executed': True, 'failed_tasks': [':app:testDebugUnitTest']},
                unit={'tests': 485, 'failures': 13, 'errors': 0, 'skipped': 0},
                tests={'x.T#t': {'status': 'PASS'}, 'y.U#u': {'status': 'FAIL', 'message': 'CapacityWait'}})
        rc, out = run(G + ['close', '--batch', 'S00'], repo, env)
        rec = d / 'S00/RECEIPT.json'
        expect('S00 closes with recorded failures for S01', rc == 0 and rec.is_file() and
               json.loads(rec.read_text())['unit_failures_recorded_for_next_batch'] == ['y.U#u'], out)
        git(repo, 'add', '-A')
        git(repo, 'commit', '-qm', 'receipt S00')
        rc, out = run(G + ['status'], repo, env)
        expect('next open batch is S01', json.loads(out)['open_batch'] == 'S01', out)

        (d / 'S01').mkdir()
        src.write_text('class DbStore { int x; }\n')
        b = {'items': {i: ['x.T#t'] for i in s01['local_items']}}
        b['items'][s01['local_items'][0]] = ['missing.T#gone']
        b['items']['R31-B12-C03#A1'] = ['x.T#t']
        (d / 'S01/BINDINGS.json').write_text(json.dumps(b))
        git(repo, 'add', '-A')
        git(repo, 'commit', '-qm', 's01 work')
        summary('S01', unit={'tests': 480, 'failures': 0, 'errors': 0, 'skipped': 2})
        rc, out = run(G + ['close', '--batch', 'S01'], repo, env)
        expect('rejects undeclared out-of-scope main source', 'outside batch scope' in out, out)
        expect('rejects dropped unit test count', 'unit test count dropped' in out, out)
        expect('rejects increased skipped count', 'skipped tests increased' in out, out)
        expect('rejects bound test that did not run', 'missing.T#gone (not executed)' in out, out)
        expect('rejects binding a DEVICE item / other batch item', 'binding for item not in this batch: R31-B12-C03#A1' in out, out)

        summary('S01', unit={'tests': 490, 'failures': 1, 'errors': 0, 'skipped': 0})
        rc, out = run(G + ['close', '--batch', 'S01'], repo, env)
        expect('from S01 a single unit failure blocks closure', rc != 0 and 'unit failures=1' in out, out)

        b = {'items': {i: ['x.T#t'] for i in s01['local_items']},
             'scope_extensions': [{'path': 'app/src/main/java/com/ethk3/novaonepro/DbStore.java', 'reason': 'caller updated for fingerprint source'}]}
        (d / 'S01/BINDINGS.json').write_text(json.dumps(b))
        git(repo, 'add', '-A')
        git(repo, 'commit', '-qm', 'declare')
        summary('S01', unit={'tests': 490, 'failures': 0, 'errors': 0, 'skipped': 0})
        src.write_text('class DbStore { int y; }\n')
        git(repo, 'add', '-A')
        git(repo, 'commit', '-qm', 'late edit')
        rc, out = run(G + ['close', '--batch', 'S01'], repo, env)
        expect('rejects stale build summary', rc != 0 and 'source changed since' in out, out)

        summary('S01', unit={'tests': 490, 'failures': 0, 'errors': 0, 'skipped': 0})
        (d / 'S01/Leaked.java').write_text('class Leaked {}')
        git(repo, 'add', '-A')
        git(repo, 'commit', '-qm', 'snapshot')
        rc, out = run(G + ['close', '--batch', 'S01'], repo, env)
        expect('rejects source snapshots in evidence', rc != 0 and 'source snapshot copied' in out, out)
        (d / 'S01/Leaked.java').unlink()
        git(repo, 'add', '-A')
        git(repo, 'commit', '-qm', 'rm snapshot')
        rc, out = run(G + ['close', '--batch', 'S01'], repo, env)
        rec = json.loads((d / 'S01/RECEIPT.json').read_text()) if (d / 'S01/RECEIPT.json').is_file() else {}
        expect('closes when every condition holds', rc == 0 and rec.get('local_items') and all(v == 'PASS' for v in rec['local_items'].values()), out)
        expect('receipt never marks deferred items PASS', all(v in ('NOT_RUN', 'NOT_PROVEN') for v in rec.get('deferred_items', {}).values()), out)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    fails = [r for r in results if r['status'] != 'PASS']
    out = {'scope': 'R312_GATE_SELF_TEST_SYNTHETIC_NOT_ANDROID', 'status': 'PASS' if not fails else 'FAIL',
           'cases': len(results), 'failed': len(fails), 'results': results}
    print(json.dumps(out, ensure_ascii=False, indent=1))
    sys.exit(1 if fails else 0)


if __name__ == '__main__':
    main()
