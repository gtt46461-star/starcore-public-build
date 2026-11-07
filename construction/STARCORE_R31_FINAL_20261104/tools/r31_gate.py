#!/usr/bin/env python3
"""R31 construction gate (R31.3 construction-first rules; R31.4 layout: 11 merged batches).

  status                    open batch, its cards, unbound LOCAL items, known failures it must fix
  check-plan                plan ledgers complete and consistent
  scope  --batch Kxx        main-source files changed since the previous batch closed
  quick  --batch Kxx        fast loop while coding: compile + bound JUnit classes + bound HOST entries
  build  --batch Kxx        full run: gradle debug compile/androidTest compile/unit/lint + HOST_SUITE
  build  --batch Kxx --import FILE   accept a BUILD_SUMMARY.json produced by the r31-gate CI job
  close  --batch Kxx        full run if needed, judge with the ratchet rules, write RECEIPT.json

Ratchet rules (no global-green prerequisite before construction):
  * K01 records every failure of the first full run as a known failure with an owner batch (K02..K11).
  * Every later batch: no new failure; known failures owned by this batch must pass; known failures may only shrink.
  * Every LOCAL item of the batch is bound to tests that pass, or is carried (max 2, red test committed, target <= K11).
  * K11: zero known failures, zero carried items.
  * Receipts written under an earlier plan version name batches in its numbering; they are translated
    through ledgers/batches.json legacy_batch_map (R31.3 K04 -> R31.4 K03, ...).
  * Compilation failure always blocks. EMU/DEVICE/NATURAL items are never PASS.
Python 3 standard library only.
"""
import argparse
import datetime as dt
import fnmatch
import hashlib
import json
import os
import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

PLAN = Path(__file__).resolve().parents[1]
PLAN_REL = 'construction/STARCORE_R31_FINAL_20261104'
GRADLE_TASKS = [':app:compileDebugJavaWithJavac', ':app:compileDebugAndroidTestJavaWithJavac',
                ':app:testDebugUnitTest', ':app:lintDebug']
COMPILE_TASKS = {':app:compileDebugJavaWithJavac', ':app:compileDebugAndroidTestJavaWithJavac', ':app:compileDebugUnitTestJavaWithJavac'}
RELEASE_ONLY_TASKS = [':app:verifyFinalR31BuildIdentity', ':app:lintRelease']
EVIDENCE_MAX_BYTES = 20 * 1024 * 1024
MAX_REASSIGN_PER_BATCH = 3
FREE_PATHS = ['app/src/test/*', 'app/src/androidTest/*', 'tools/r31/*', 'tools/r312/*', 'tools/r30-repair/*',
              'construction/r31/*', 'verification/r31/*', 'release/*', 'app/build/generated/*', PLAN_REL + '/*',
              '.github/workflows/r31-gate.yml']


def die(msg, code=2):
    print('R31_GATE_FAIL: ' + msg, file=sys.stderr)
    sys.exit(code)


def load(rel):
    return json.loads((PLAN / rel).read_text())


def ledger():
    return load('ledgers/batches.json')


def items():
    return load('ledgers/acceptance_items.json')


def repo_root():
    try:
        return Path(subprocess.check_output(['git', 'rev-parse', '--show-toplevel'], cwd=PLAN, text=True).strip())
    except Exception:
        return PLAN.parents[1]


ROOT = repo_root()


def git(*args, check=True):
    r = subprocess.run(['git', *args], cwd=ROOT, text=True, capture_output=True)
    if check and r.returncode:
        die('git ' + ' '.join(args) + ': ' + r.stderr.strip())
    return r.stdout.strip()


def bdir(bid):
    return ROOT / 'construction/r31/batches' / bid


def receipt_path(bid):
    return bdir(bid) / 'RECEIPT.json'


def bindings_path(bid):
    return bdir(bid) / 'BINDINGS.json'


def summary_path(bid):
    return ROOT / 'verification/r31/batches' / bid / 'BUILD_SUMMARY.json'


def suite_path():
    return ROOT / 'construction/r31/HOST_SUITE.json'


def batch(bid):
    for b in ledger()['batches']:
        if b['batch'] == bid:
            return b
    die('unknown batch ' + str(bid))


def current_batch():
    for b in ledger()['batches']:
        if not receipt_path(b['batch']).is_file():
            return b
    return None


def translate_receipt(rec):
    """Map owner/target batch ids of a receipt written under an earlier plan version to this layout."""
    m = ledger().get('legacy_batch_map', {}).get(rec.get('plan'))
    if not m:
        return rec
    rec = dict(rec)
    rec['known_failures'] = {f: m.get(o, o) for f, o in rec.get('known_failures', {}).items()}
    rec['carried'] = {i: dict(c, to=m.get(c.get('to'), c.get('to'))) for i, c in rec.get('carried', {}).items()}
    rec['translated_from_plan'] = rec.get('plan')
    return rec


def prev_receipt(b):
    if not b['requires_closed']:
        return None
    return translate_receipt(json.loads(receipt_path(b['requires_closed'][0]).read_text()))


def start_commit(b):
    if not b['requires_closed']:
        # R31_TEST_RESUME_COMMIT exists only for tools/test_r31_gate.py in a throwaway repository.
        return os.environ.get('R31_TEST_RESUME_COMMIT') or ledger()['resume_commit']
    return prev_receipt(b)['closed_commit']


def is_evidence(path):
    return path.startswith('construction/r31/batches/') or path.startswith('verification/r31/batches/')


def read_bindings(bid):
    p = bindings_path(bid)
    return json.loads(p.read_text()) if p.is_file() else {}


# ------------------------------------------------------------------ check-plan
def check_plan():
    errs = []
    L = ledger()
    its = items()
    cards = load('ledgers/cards.json')
    eng = load('ledgers/repair_cards.json')
    ids = [c['id'] for c in cards]
    placed = [cid for b in L['batches'] for cid in b['cards']]
    if sorted(placed) != sorted(ids + [e['id'] for e in eng]):
        errs.append('every original and repair card must be placed exactly once')
    n = len(L['batches'])
    last = L['last_batch_for_known_failures_and_carries']
    for i, b in enumerate(L['batches'], 1):
        if b['batch'] != f'K{i:02d}' or b['order'] != i:
            errs.append('batch id must equal its sequence number: ' + b['batch'])
        if b['requires_closed'] != ([] if i == 1 else [f'K{i - 1:02d}']):
            errs.append(b['batch'] + ' must require exactly the previous batch')
        if not b['local_items']:
            errs.append(b['batch'] + ' has no LOCAL item')
        if not (PLAN / b['doc']).is_file():
            errs.append(b['batch'] + ' document missing')
        if b['emu_items_executed_here'] != (b['batch'] == last):
            errs.append('EMU items run only in ' + last)
        if b['release_identity_freeze'] != (i == n):
            errs.append('release identity freeze only in the last batch')
        if b['requires_zero_known_failures'] != (b['batch'] >= last):
            errs.append('zero known failures required exactly from ' + last)
        if b['records_known_failure_baseline'] != (i == 1):
            errs.append('only K01 records the known-failure baseline')
        if (b['max_carried_items'] == 0) != (b['batch'] >= last):
            errs.append('carries allowed only before ' + last)
    order = {'P0%d' % k: k for k in range(9)}
    seen_max = -1
    for b in L['batches']:
        ph = [order[p] for p in b['original_phases']]
        if ph:
            if min(ph) < seen_max:
                errs.append(b['batch'] + ' places a card before an earlier original phase is finished')
            seen_max = max(seen_max, max(ph))
    for c in cards:
        if c.get('construction_batch') not in {b['batch'] for b in L['batches']}:
            errs.append(c['id'] + ' lacks construction_batch')
        if c['status'] != 'DESIGNED_NOT_IMPLEMENTED':
            errs.append(c['id'] + ' plan status changed')
    covered = {}
    for x in its:
        covered.setdefault(x['original_item'], []).append(x)
        if x['level'] not in L['levels']:
            errs.append(x['item'] + ' unknown level')
        if x['blocking'] != (x['level'] in L['blocking_levels']):
            errs.append(x['item'] + ' blocking flag inconsistent')
        if (x['item'] in batch(x['batch'])['local_items']) != (x['level'] == 'LOCAL'):
            errs.append(x['item'] + ' batch list inconsistent')
    for c in cards + [{'id': e['id'], 'acceptance': e['acceptance']} for e in eng]:
        for k, text in enumerate(c['acceptance'], 1):
            key = f"{c['id']}#A{k}"
            parts = covered.get(key)
            if not parts:
                errs.append(key + ' not classified')
            elif any(p['original_text_sha256'] != hashlib.sha256(text.encode()).hexdigest() for p in parts):
                errs.append(key + ' original acceptance text changed')
    deferred = load('ledgers/deferred_items.json')
    if sorted(x['item'] for x in deferred['items']) != sorted(x['item'] for x in its if x['level'] != 'LOCAL'):
        errs.append('deferred ledger out of sync')
    policy = load('ledgers/execution_policy.json')
    if policy.get('version') != L['version']:
        errs.append('execution_policy version differs from batch ledger')
    cfg = hashlib.sha256((PLAN / 'ledgers/acceptance_config.json').read_bytes()).hexdigest()
    if cfg != policy['preservation']['acceptance_config_sha256']:
        errs.append('acceptance_config.json thresholds changed')
    return errs


# ------------------------------------------------------------------ scope
def scope_report(b, bindings=None):
    allowed = set(b['scope_files'])
    ext = {e['path']: e.get('reason', '') for e in (bindings or {}).get('scope_extensions', [])}
    res = {'base': start_commit(b), 'in_scope': [], 'tests_tools_evidence': [], 'declared_extensions': [], 'undeclared': []}
    for p in [x for x in git('diff', '--name-only', res['base'], 'HEAD').splitlines() if x]:
        if p in allowed:
            res['in_scope'].append(p)
        elif any(fnmatch.fnmatch(p, pat) for pat in FREE_PATHS):
            res['tests_tools_evidence'].append(p)
        elif p in ext and len(ext[p].strip()) >= 10:
            res['declared_extensions'].append(p)
        else:
            res['undeclared'].append(p)
    return res


# ------------------------------------------------------------------ build
def dirty_source():
    out = subprocess.run(['git', 'status', '--porcelain', '--untracked-files=all'], cwd=ROOT, text=True, capture_output=True).stdout
    paths = [l[3:].split(' -> ')[-1].strip('"') for l in out.splitlines() if len(l) > 3]
    return [p for p in paths if not is_evidence(p)]


def parse_junit(dirpath):
    tests, counts = {}, {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    for x in sorted(Path(dirpath).glob('TEST-*.xml')):
        for case in ET.parse(x).getroot().iter('testcase'):
            name = case.get('classname', '') + '#' + case.get('name', '')
            node = None
            if case.find('failure') is not None:
                st, node = 'FAIL', case.find('failure')
                counts['failures'] += 1
            elif case.find('error') is not None:
                st, node = 'ERROR', case.find('error')
                counts['errors'] += 1
            elif case.find('skipped') is not None:
                st = 'SKIP'
                counts['skipped'] += 1
            else:
                st = 'PASS'
            counts['tests'] += 1
            tests[name] = {'status': st, 'message': (node.get('message', '')[:300] if node is not None else '')}
    return counts, tests


def run_logged(cmd, log, shell=False):
    with open(log, 'wb') as fh:
        return subprocess.run(cmd, cwd=ROOT, stdout=fh, stderr=subprocess.STDOUT, shell=shell).returncode


def load_suite():
    if not suite_path().is_file():
        die('construction/r31/HOST_SUITE.json missing')
    return json.loads(suite_path().read_text())


def run_pre_build(suite, logs):
    pre = []
    for i, cmd in enumerate(suite.get('pre_build', [])):
        rc = run_logged(cmd, logs / f'pre_{i}.log', shell=True)
        pre.append({'command': cmd, 'exit': rc})
        if rc:
            die(f'pre_build failed: {cmd} (see {logs / f"pre_{i}.log"})')
    return pre


def run_host(entries, logs):
    out = []
    for h in entries:
        if h.get('retired'):
            out.append({'id': h['id'], 'status': 'RETIRED', 'replacement': h.get('replacement')})
        elif not h.get('command'):
            out.append({'id': h['id'], 'status': 'NOT_CONFIGURED'})
        else:
            rc = run_logged(h['command'], logs / f"host_{h['id']}.log", shell=True)
            out.append({'id': h['id'], 'status': 'PASS' if rc == 0 else 'FAIL', 'exit': rc})
    return out


def build(b, import_file=None):
    out = summary_path(b['batch'])
    out.parent.mkdir(parents=True, exist_ok=True)
    if import_file:
        s = json.loads(Path(import_file).read_text())
        if s.get('format') != 'R31_BUILD_SUMMARY_V2' or s.get('batch') != b['batch']:
            die('imported summary has wrong format or batch')
        out.write_text(json.dumps(s, ensure_ascii=False, indent=1) + '\n')
        print(json.dumps({k: s.get(k) for k in ('source_head', 'gradle', 'unit', 'host_failed')}, ensure_ascii=False))
        return s
    if dirty_source():
        die('commit source changes before a full build; dirty: ' + ', '.join(dirty_source()[:10]))
    head = git('rev-parse', 'HEAD')
    suite = load_suite()
    logs = out.parent / 'logs'
    logs.mkdir(exist_ok=True)
    for f in logs.glob('*.log'):
        f.unlink()
    started = dt.datetime.now(dt.timezone.utc).isoformat()
    pre = run_pre_build(suite, logs)
    if dirty_source():
        die('pre_build changed source files; commit them and rerun: ' + ', '.join(dirty_source()[:10]))
    res = ROOT / 'app/build/test-results/testDebugUnitTest'
    if res.exists():
        for f in res.glob('TEST-*.xml'):
            f.unlink()
    rc = run_logged(['bash', './gradlew', '--no-daemon', '--continue', '-Pr30Instrumentation=true'] + GRADLE_TASKS, logs / 'gradle.log')
    text = (logs / 'gradle.log').read_text(errors='replace')
    failed_tasks = sorted(set(re.findall(r"Execution failed for task '([^']+)'", text)))
    gradle = {'state': 'PASS' if rc == 0 else 'FAIL', 'tasks': GRADLE_TASKS, 'exit': rc, 'failed_tasks': failed_tasks,
              'compile_failed': sorted(set(failed_tasks) & COMPILE_TASKS),
              'unit_task_executed': '> Task :app:testDebugUnitTest' in text and 'testDebugUnitTest UP-TO-DATE' not in text}
    unit, tests = parse_junit(res) if res.exists() else ({'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}, {})
    host = run_host(suite.get('host', []), logs)
    s = {'format': 'R31_BUILD_SUMMARY_V2', 'batch': b['batch'], 'source_head': head, 'started_at': started,
         'finished_at': dt.datetime.now(dt.timezone.utc).isoformat(),
         'environment': 'CI' if os.environ.get('GITHUB_RUN_ID') else 'LOCAL', 'ci_run': os.environ.get('GITHUB_RUN_ID'),
         'pre_build': pre, 'gradle': gradle, 'unit': unit, 'tests': tests, 'host': host,
         'host_failed': [h['id'] for h in host if h['status'] in ('FAIL', 'NOT_CONFIGURED')],
         'release_only_tasks_not_run': RELEASE_ONLY_TASKS,
         'not_run_layers': {'EMU': 'NOT_RUN', 'DEVICE': 'NOT_RUN', 'NATURAL': 'NOT_PROVEN'}}
    out.write_text(json.dumps(s, ensure_ascii=False, indent=1) + '\n')
    keep = {'gradle.log'} if gradle['state'] == 'FAIL' else set()
    keep |= {f"host_{h['id']}.log" for h in host if h['status'] == 'FAIL'}
    for f in logs.glob('*.log'):
        if f.name not in keep and f.stat().st_size > 64 * 1024:
            f.write_bytes(f.read_bytes()[-64 * 1024:])
    print(json.dumps({'source_head': head[:12], 'gradle': gradle['state'], 'compile_failed': gradle['compile_failed'],
                      'unit': unit, 'host_failed': s['host_failed']}, ensure_ascii=False))
    return s


def quick(b):
    """Fast inner loop. Writes nothing tracked; never closes a batch."""
    bind = read_bindings(b['batch'])
    refs = sorted({r for v in bind.get('items', {}).values() for r in v} |
                  {r for c in bind.get('carry', {}).values() for r in c.get('red_tests', [])})
    classes = sorted({r.split('#')[0] for r in refs if not r.startswith('HOST:')})
    hosts = sorted({r[5:] for r in refs if r.startswith('HOST:')})
    logs = ROOT / 'app/build/r31-quick'
    logs.mkdir(parents=True, exist_ok=True)
    suite = load_suite()
    run_pre_build(suite, logs)
    cmd = ['bash', './gradlew', '--no-daemon', '--continue', '-Pr30Instrumentation=true', ':app:compileDebugJavaWithJavac']
    res = ROOT / 'app/build/test-results/testDebugUnitTest'
    if classes:
        if res.exists():
            for f in res.glob('TEST-*.xml'):
                f.unlink()
        cmd += [':app:testDebugUnitTest'] + [a for c in classes for a in ('--tests', c)]
    rc = run_logged(cmd, logs / 'gradle.log')
    unit, tests = parse_junit(res) if classes and res.exists() else ({}, {})
    host = run_host([h for h in suite.get('host', []) if h['id'] in hosts], logs)
    status = {}
    for r in refs:
        if r.startswith('HOST:'):
            status[r] = next((h['status'] for h in host if h['id'] == r[5:]), 'NOT_IN_SUITE')
        else:
            status[r] = tests.get(r, {}).get('status', 'NOT_EXECUTED')
    print(json.dumps({'batch': b['batch'], 'gradle_exit': rc, 'log': str((logs / 'gradle.log').relative_to(ROOT)),
                      'unit': unit, 'bound_tests': status,
                      'unbound_local_items': [i for i in b['local_items'] if not bind.get('items', {}).get(i) and i not in bind.get('carry', {})]},
                     ensure_ascii=False, indent=1))


# ------------------------------------------------------------------ close
def failure_set(s):
    f = {k for k, v in s['tests'].items() if v['status'] in ('FAIL', 'ERROR')}
    f |= {'HOST:' + h for h in s['host_failed']}
    f |= {'GRADLE:' + t for t in s['gradle'].get('failed_tasks', []) if t != ':app:testDebugUnitTest' and t not in COMPILE_TASKS}
    return f


def ref_status(s, r):
    if r.startswith('HOST:'):
        return next((h['status'] for h in s['host'] if h['id'] == r[5:]), 'NOT_EXECUTED')
    return s['tests'].get(r, {}).get('status', 'NOT_EXECUTED')


def close(b, import_file=None):
    L = ledger()
    bid = b['batch']
    last = L['last_batch_for_known_failures_and_carries']
    cur = current_batch()
    if cur is None or cur['batch'] != bid:
        die('batch %s is not the open batch (open: %s)' % (bid, cur['batch'] if cur else 'none'))
    if not bindings_path(bid).is_file():
        die('missing %s (bind each LOCAL item to tests first)' % bindings_path(bid).relative_to(ROOT))
    if dirty_source():
        die('commit source changes first; dirty: ' + ', '.join(dirty_source()[:10]))
    head = git('rev-parse', 'HEAD')
    sp = summary_path(bid)
    s = json.loads(sp.read_text()) if sp.is_file() else None
    stale = s is None or [p for p in git('diff', '--name-only', s['source_head'], 'HEAD').splitlines() if p and not is_evidence(p)]
    if import_file:
        s = build(b, import_file)
        stale = [p for p in git('diff', '--name-only', s['source_head'], 'HEAD').splitlines() if p and not is_evidence(p)]
        if stale:
            die('imported summary is for %s; source changed since: %s' % (s['source_head'][:12], ', '.join(stale[:10])))
    elif stale:
        s = build(b)
    bind = read_bindings(bid)
    prev = prev_receipt(b)
    problems = []
    sc = scope_report(b, bind)
    if sc['undeclared']:
        problems.append('main-source changes outside batch scope without reason: ' + ', '.join(sc['undeclared'][:20]))
    if s['gradle'].get('state') == 'NOT_RUN' or not s['gradle'].get('unit_task_executed', False):
        problems.append('unit test task did not execute')
    if s['gradle'].get('compile_failed'):
        problems.append('compilation failed: ' + ', '.join(s['gradle']['compile_failed']))
    if s['unit']['tests'] == 0:
        problems.append('zero unit tests executed')
    if prev:
        if s['unit']['tests'] < prev['unit']['tests']:
            problems.append('unit test count dropped from %d to %d' % (prev['unit']['tests'], s['unit']['tests']))
        if s['unit']['skipped'] > prev['unit']['skipped']:
            problems.append('skipped tests increased from %d to %d' % (prev['unit']['skipped'], s['unit']['skipped']))

    its = {x['item']: x for x in items()}
    batch_ids = [x['batch'] for x in L['batches']]

    def later(target, upto=last):
        return target in batch_ids and bid < target <= upto

    # carries: incoming (from earlier batches) + new ones declared now
    incoming = {k: v for k, v in (prev or {}).get('carried', {}).items()}
    due = {k for k, v in incoming.items() if v['to'] == bid}
    carry = bind.get('carry', {})
    if len(carry) > b['max_carried_items']:
        problems.append('at most %d carried items in %s' % (b['max_carried_items'], bid))
    for iid, c in carry.items():
        if iid not in b['local_items'] and iid not in due:
            problems.append('can only carry this batch\'s LOCAL items or items due now: ' + iid)
        if not later(c.get('to', '')):
            problems.append('carry target for %s must be a later batch no later than %s' % (iid, last))
        if len(c.get('reason', '').strip()) < 10:
            problems.append('carry reason too short: ' + iid)
        reds = c.get('red_tests', [])
        if not reds or any(ref_status(s, r) == 'NOT_EXECUTED' for r in reds):
            problems.append('carried item %s needs committed tests that actually executed' % iid)
    still_carried = {k: v for k, v in incoming.items() if v['to'] != bid}
    for iid, c in carry.items():
        still_carried[iid] = {'to': c.get('to'), 'reason': c.get('reason', ''), 'red_tests': c.get('red_tests', []), 'from': bid}
    exempt = {r for c in still_carried.values() for r in c.get('red_tests', [])}

    # LOCAL items (and items due from earlier carries) must have passing bound tests
    results = {}
    for iid in list(b['local_items']) + sorted(due):
        if iid in carry:
            results[iid] = 'CARRIED_TO_' + carry[iid]['to']
            continue
        refs = bind.get('items', {}).get(iid) or []
        if not refs:
            problems.append(iid + ' has no bound test')
            results[iid] = 'UNBOUND'
            continue
        bad = [r + ' (' + ref_status(s, r) + ')' for r in refs if ref_status(s, r) != 'PASS']
        results[iid] = 'FAIL' if bad else 'PASS'
        if bad:
            problems.append(iid + ' bound tests not passing: ' + ', '.join(bad))
    for iid in bind.get('items', {}):
        x = its.get(iid)
        if not x or (x['batch'] != bid and iid not in due):
            problems.append('binding for item not in this batch: ' + iid)
        elif x['level'] != 'LOCAL':
            problems.append('non-LOCAL item cannot be bound/PASSed during construction: ' + iid)

    # ratchet on failures
    fails = failure_set(s)
    if b['records_known_failure_baseline']:
        owners = bind.get('known_failure_owners', {})
        unowned = sorted(f for f in fails - exempt if not later(owners.get(f, '')))
        if unowned:
            problems.append('K01: every current failure needs an owner batch K02..%s in known_failure_owners: %s' % (last, ', '.join(unowned[:15])))
        known = {f: owners.get(f) for f in sorted(fails - exempt)}
    else:
        prev_known = dict((prev or {}).get('known_failures', {}))
        new = sorted(fails - set(prev_known) - exempt)
        if new:
            problems.append('new failures (not allowed): ' + ', '.join(new[:20]))
        reassign = bind.get('reassign', {})
        if len(reassign) > MAX_REASSIGN_PER_BATCH:
            problems.append('at most %d known failures may be reassigned per batch' % MAX_REASSIGN_PER_BATCH)
        for f, r in reassign.items():
            if prev_known.get(f) != bid or not later(r.get('to', '')) or len(r.get('reason', '').strip()) < 10:
                problems.append('invalid reassignment of ' + f)
            else:
                prev_known[f] = r['to']
        owed = sorted(f for f, owner in prev_known.items() if owner == bid and f in fails)
        if owed:
            problems.append('known failures owned by %s still failing: %s' % (bid, ', '.join(owed[:20])))
        known = {f: o for f, o in prev_known.items() if f in fails}
    if b['requires_zero_known_failures'] and (fails or still_carried):
        problems.append('%s requires zero failures and zero carried items; failures=%d carried=%d' % (bid, len(fails), len(still_carried)))

    ev = 0
    for base in (bdir(bid), summary_path(bid).parent):
        if base.exists():
            for f in base.rglob('*'):
                if f.is_file():
                    ev += f.stat().st_size
                    if f.suffix in ('.java', '.kt'):
                        problems.append('source snapshot copied into evidence: ' + str(f.relative_to(ROOT)))
    if ev > EVIDENCE_MAX_BYTES:
        problems.append('evidence for batch exceeds %d bytes' % EVIDENCE_MAX_BYTES)

    if problems:
        print(json.dumps({'batch': bid, 'state': 'NOT_CLOSED', 'problems': problems}, ensure_ascii=False, indent=1))
        sys.exit(1)
    rec = {'format': 'R31_BATCH_RECEIPT_V2', 'plan': L['version'], 'batch': bid, 'name': b['name'], 'cards': b['cards'],
           'closed_commit': head, 'build_source_head': s['source_head'], 'build_environment': s['environment'],
           'closed_at': dt.datetime.now(dt.timezone.utc).isoformat(),
           'unit': s['unit'], 'host': {h['id']: h['status'] for h in s['host']},
           'local_items': results, 'carried': still_carried, 'known_failures': known,
           'known_failures_fixed_here': sorted(set((prev or {}).get('known_failures', {})) - set(known)),
           'deferred_items': {x['item']: ('NOT_PROVEN' if x['level'] == 'NATURAL' else 'NOT_RUN') for x in its.values()
                              if x['batch'] == bid and x['level'] != 'LOCAL'},
           'scope': sc,
           'claims_not_made': ['APK released', 'Android emulator PASS', 'device 24h/72h PASS', 'natural quality proven']}
    receipt_path(bid).parent.mkdir(parents=True, exist_ok=True)
    receipt_path(bid).write_text(json.dumps(rec, ensure_ascii=False, indent=1) + '\n')
    nxt = L['batches'][b['order']]['batch'] if b['order'] < len(L['batches']) else None
    print(json.dumps({'batch': bid, 'state': 'CLOSED', 'known_failures_left': len(known), 'carried': len(still_carried), 'next': nxt}, ensure_ascii=False))


def status():
    b = current_batch()
    if not b:
        print(json.dumps({'state': 'ALL_BATCHES_CLOSED'}))
        return
    bind = read_bindings(b['batch'])
    prev = prev_receipt(b) or {}
    print(json.dumps({'open_batch': b['batch'], 'name': b['name'], 'doc': PLAN_REL + '/' + b['doc'], 'cards': b['cards'],
                      'start_commit': start_commit(b), 'local_items': len(b['local_items']),
                      'unbound_local_items': [i for i in b['local_items'] if not bind.get('items', {}).get(i) and i not in bind.get('carry', {})],
                      'carried_items_due_now': sorted(k for k, v in prev.get('carried', {}).items() if v['to'] == b['batch']),
                      'known_failures_owned_by_this_batch': sorted(f for f, o in prev.get('known_failures', {}).items() if o == b['batch']),
                      'known_failures_total': len(prev.get('known_failures', {})),
                      'deferred_items': len(b['deferred_items'])}, ensure_ascii=False, indent=1))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('command', choices=['status', 'check-plan', 'scope', 'quick', 'build', 'close'])
    ap.add_argument('--batch')
    ap.add_argument('--import', dest='import_file')
    a = ap.parse_args()
    if a.command == 'check-plan':
        errs = check_plan()
        print(json.dumps({'state': 'PASS' if not errs else 'FAIL', 'errors': errs}, ensure_ascii=False, indent=1))
        sys.exit(1 if errs else 0)
    if a.command == 'status':
        return status()
    if not a.batch:
        cur = current_batch()
        if not cur:
            die('all batches closed')
        a.batch = cur['batch']
    b = batch(a.batch)
    if a.command == 'scope':
        print(json.dumps(scope_report(b, read_bindings(b['batch'])), ensure_ascii=False, indent=1))
    elif a.command == 'quick':
        quick(b)
    elif a.command == 'build':
        build(b, a.import_file)
    elif a.command == 'close':
        close(b, a.import_file)


if __name__ == '__main__':
    main()
