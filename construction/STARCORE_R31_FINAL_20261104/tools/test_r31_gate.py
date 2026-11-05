#!/usr/bin/env python3
"""Self-test of tools/r31_gate.py in a throwaway git repository (synthetic summaries; no Android build).

Shows the gate lets construction proceed (known failures recorded once, owned and burned down; up to two
items carried with a red test) while still refusing every forbidden shortcut.
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


def expect(name, cond, detail=''):
    results.append({'case': name, 'status': 'PASS' if cond else 'FAIL', 'detail': '' if cond else str(detail)[-700:]})


def main():
    tmp = Path(tempfile.mkdtemp(prefix='r31gate-'))
    try:
        repo = tmp / 'repo'
        src_dir = repo / 'app/src/main/java/com/ethk3/novaonepro'
        src_dir.mkdir(parents=True)
        shutil.copytree(PLAN, repo / REL, ignore=shutil.ignore_patterns('__pycache__', 'evidence', 'baseline_source', 'reference', 'START_HERE.html'))
        out_scope = src_dir / 'ChartsActivityR83.java'
        out_scope.write_text('class ChartsActivityR83 {}\n')
        (repo / 'construction/r31').mkdir(parents=True)
        (repo / 'construction/r31/HOST_SUITE.json').write_text(json.dumps({'format': 'R312_HOST_SUITE_V1', 'pre_build': [], 'host': [{'id': 'ok', 'command': 'true'}]}))

        def g(*a):
            subprocess.run(['git', '-c', 'user.email=t@t', '-c', 'user.name=t', *a], cwd=repo, check=True, capture_output=True)

        def commit(msg):
            g('add', '-A')
            g('commit', '-qm', msg, '--allow-empty')

        g('init', '-q')
        commit('base')
        base = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=repo, text=True).strip()
        env = dict(os.environ, R31_TEST_RESUME_COMMIT=base, PYTHONDONTWRITEBYTECODE='1')
        G = [sys.executable, str(repo / REL / 'tools/r31_gate.py')]
        L = json.loads((repo / REL / 'ledgers/batches.json').read_text())
        B = {b['batch']: b for b in L['batches']}

        def run(*a):
            r = subprocess.run(G + list(a), cwd=repo, env=env, text=True, capture_output=True)
            return r.returncode, r.stdout + r.stderr

        def summary(bid, tests, unit=None, host=None, gradle=None):
            head = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=repo, text=True).strip()
            t = {k: {'status': v} for k, v in tests.items()}
            n = sum(1 for v in tests.values())
            s = {'format': 'R31_BUILD_SUMMARY_V2', 'batch': bid, 'source_head': head, 'environment': 'CI',
                 'gradle': gradle or {'state': 'PASS', 'exit': 0, 'unit_task_executed': True, 'failed_tasks': [], 'compile_failed': []},
                 'unit': unit or {'tests': 480 + n, 'failures': sum(v == 'FAIL' for v in tests.values()), 'errors': 0, 'skipped': 0},
                 'tests': t, 'host': [{'id': 'ok', 'status': 'PASS'}] + (host or []),
                 'host_failed': [h['id'] for h in (host or []) if h['status'] == 'FAIL']}
            f = tmp / f'{bid}.json'
            f.write_text(json.dumps(s))
            return str(f)

        def bind(bid, data):
            d = repo / 'construction/r31/batches' / bid
            d.mkdir(parents=True, exist_ok=True)
            (d / 'BINDINGS.json').write_text(json.dumps(data))

        rc, out = run('check-plan')
        expect('check-plan passes on the shipped plan', rc == 0, out)
        rc, out = run('status')
        expect('first open batch is K01', rc == 0 and json.loads(out)['open_batch'] == 'K01', out)
        rc, out = run('close', '--batch', 'K02')
        expect('cannot close a batch out of order', rc != 0 and 'not the open batch' in out, out)
        rc, out = run('close', '--batch', 'K01')
        expect('cannot close without bindings', rc != 0 and 'BINDINGS' in out, out)

        k01 = {'items': {i: ['t.Pass#ok'] for i in B['K01']['local_items']}}
        bind('K01', k01)
        commit('k01')
        base_tests = {'t.Pass#ok': 'PASS', 'mem.Cap#a': 'FAIL', 'mem.Cap#b': 'FAIL', 'cert.Start#c': 'FAIL', 'k05.Owned#d': 'FAIL'}
        rc, out = run('close', '--batch', 'K01', '--import', summary('K01', base_tests))
        expect('K01 requires an owner batch for every current failure', rc != 0 and 'known_failure_owners' in out, out)
        k01['known_failure_owners'] = {'mem.Cap#a': 'K02', 'mem.Cap#b': 'K02', 'cert.Start#c': 'K04', 'k05.Owned#d': 'K05'}
        bind('K01', k01)
        commit('owners')
        rc, out = run('close', '--batch', 'K01', '--import', summary('K01', base_tests))
        rec = json.loads((repo / 'construction/r31/batches/K01/RECEIPT.json').read_text()) if rc == 0 else {}
        expect('K01 closes and records 4 known failures (construction starts without global green)',
               rc == 0 and len(rec.get('known_failures', {})) == 4, out)
        commit('receipt k01')

        # K02 shortcuts refused
        out_scope.write_text('class ChartsActivityR83 { int x; }\n')
        k02_items = B['K02']['local_items']
        bad = {'items': {i: ['t.Pass#ok'] for i in k02_items}}
        bad['items']['R31-B12-C03#A1'] = ['t.Pass#ok']
        bad['carry'] = {k02_items[0]: {'to': 'K03', 'reason': 'needs fence api from next batch', 'red_tests': ['t.Red#1']},
                        k02_items[1]: {'to': 'K21', 'reason': 'too late target for carry', 'red_tests': ['t.Red#2']},
                        k02_items[2]: {'to': 'K03', 'reason': 'test never written yet', 'red_tests': ['t.Missing#x']}}
        bind('K02', bad)
        commit('k02 bad')
        tests = dict(base_tests, **{'t.Red#1': 'FAIL', 'new.Break#z': 'FAIL'})
        rc, out = run('close', '--batch', 'K02', '--import', summary('K02', tests, unit={'tests': 470, 'failures': 6, 'errors': 0, 'skipped': 3}))
        expect('rejects undeclared out-of-scope main source', 'outside batch scope' in out, out)
        expect('rejects a new failure', 'new failures (not allowed): new.Break#z' in out, out)
        expect('rejects owned known failures still failing', 'owned by K02 still failing' in out, out)
        expect('rejects more than two carries', 'at most 2 carried' in out, out)
        expect('rejects carry beyond K20', 'carry target for %s' % k02_items[1] in out, out)
        expect('rejects carry without an executed test', 'needs committed tests that actually executed' in out, out)
        expect('rejects binding a DEVICE item', 'binding for item not in this batch: R31-B12-C03#A1' in out, out)
        expect('rejects dropped test count and more skips', 'unit test count dropped' in out and 'skipped tests increased' in out, out)
        rc, out = run('close', '--batch', 'K02', '--import', summary('K02', tests, gradle={'state': 'FAIL', 'exit': 1, 'unit_task_executed': True,
                                                                                         'failed_tasks': [':app:compileDebugJavaWithJavac'], 'compile_failed': [':app:compileDebugJavaWithJavac']}))
        expect('compilation failure always blocks', rc != 0 and 'compilation failed' in out, out)

        # K02 valid: owned failures fixed, one item carried with a red test, scope extension declared
        good = {'items': {i: ['t.Pass#ok'] for i in k02_items[1:]},
                'carry': {k02_items[0]: {'to': 'K03', 'reason': 'needs fence api from next batch', 'red_tests': ['t.Red#1']}},
                'scope_extensions': [{'path': 'app/src/main/java/com/ethk3/novaonepro/ChartsActivityR83.java', 'reason': 'release lease on activity destroy'}]}
        bind('K02', good)
        commit('k02 good')
        tests = {'t.Pass#ok': 'PASS', 'mem.Cap#a': 'PASS', 'mem.Cap#b': 'PASS', 'cert.Start#c': 'FAIL', 'k05.Owned#d': 'FAIL', 't.Red#1': 'FAIL'}
        p = summary('K02', tests)
        commit('late change')
        out_scope.write_text('class ChartsActivityR83 { int y; }\n')
        commit('late source edit')
        rc, out = run('close', '--batch', 'K02', '--import', p)
        expect('rejects a stale imported summary', rc != 0 and 'source changed since' in out, out)
        rc, out = run('close', '--batch', 'K02', '--import', summary('K02', tests))
        rec = json.loads((repo / 'construction/r31/batches/K02/RECEIPT.json').read_text()) if rc == 0 else {}
        expect('K02 closes: owned failures fixed, red test of carried item exempt, carried is not PASS',
               rc == 0 and rec['known_failures_fixed_here'] == ['mem.Cap#a', 'mem.Cap#b'] and
               rec['local_items'][k02_items[0]] == 'CARRIED_TO_K03' and rec['carried'][k02_items[0]]['to'] == 'K03', out)
        expect('receipt never marks deferred items PASS', all(v in ('NOT_RUN', 'NOT_PROVEN') for v in rec.get('deferred_items', {}).values()), out)
        commit('receipt k02')

        rc, out = run('status')
        st = json.loads(out)
        expect('status shows carried item due in K03', st['open_batch'] == 'K03' and st['carried_items_due_now'] == [k02_items[0]], out)
        k03 = {'items': {i: ['t.Pass#ok'] for i in B['K03']['local_items']}}
        k03['items'][k02_items[0]] = ['t.Red#1']
        bind('K03', k03)
        commit('k03')
        rc, out = run('close', '--batch', 'K03', '--import', summary('K03', tests))
        expect('carried item must pass when due', rc != 0 and k02_items[0] + ' bound tests not passing' in out, out)
        tests['t.Red#1'] = 'PASS'
        rc, out = run('close', '--batch', 'K03', '--import', summary('K03', tests))
        expect('K03 closes once the carried item passes', rc == 0, out)
    finally:
        shutil.rmtree(tmp, ignore_errors=True)
    fails = [r for r in results if r['status'] != 'PASS']
    print(json.dumps({'scope': 'R31_GATE_SELF_TEST_SYNTHETIC_NOT_ANDROID', 'status': 'PASS' if not fails else 'FAIL',
                      'cases': len(results), 'failed': len(fails), 'results': results}, ensure_ascii=False, indent=1))
    sys.exit(1 if fails else 0)


if __name__ == '__main__':
    main()
