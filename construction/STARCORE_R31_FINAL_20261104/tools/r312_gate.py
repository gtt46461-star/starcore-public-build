#!/usr/bin/env python3
"""R31.2 sequential construction gate.

One command family decides which batch is open and whether it may close:

  status                      current batch, its cards and LOCAL items
  check-plan                  plan ledgers are complete and consistent (works on main or the construction branch)
  scope   --batch Sxx         main-source files changed since the previous batch closed
  build   --batch Sxx         run pre_build, gradle gate tasks and HOST_SUITE; write BUILD_SUMMARY.json
  build   --batch Sxx --import FILE   accept a BUILD_SUMMARY.json produced by the r312-gate CI job
  close   --batch Sxx         verify everything and write RECEIPT.json

The gate never edits source, never marks EMU/DEVICE/NATURAL items PASS, and never weakens a check.
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
RELEASE_ONLY_TASKS = [':app:verifyFinalR31BuildIdentity', ':app:lintRelease']
EVIDENCE_MAX_BYTES = 20 * 1024 * 1024
FREE_PATHS = ['app/src/test/*', 'app/src/androidTest/*', 'tools/r31/*', 'tools/r312/*',
              'construction/r31/r312/*', 'verification/r31/r312/*', 'release/*', 'app/build/generated/*',
              PLAN_REL + '/*']


def die(msg, code=2):
    print('R312_GATE_FAIL: ' + msg, file=sys.stderr)
    sys.exit(code)


def load(rel):
    return json.loads((PLAN / rel).read_text())


def ledger():
    return load('ledgers/r312_batches.json')


def items():
    return load('ledgers/r312_acceptance_items.json')


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


def receipt_path(sid):
    return ROOT / 'construction/r31/r312' / sid / 'RECEIPT.json'


def bindings_path(sid):
    return ROOT / 'construction/r31/r312' / sid / 'BINDINGS.json'


def summary_path(sid):
    return ROOT / 'verification/r31/r312' / sid / 'BUILD_SUMMARY.json'


def batch(sid):
    for b in ledger()['batches']:
        if b['batch'] == sid:
            return b
    die('unknown batch ' + sid)


def current_batch():
    for b in ledger()['batches']:
        if not receipt_path(b['batch']).is_file():
            return b
    return None


def start_commit(b):
    if not b['requires_closed']:
        # R312_TEST_RESUME_COMMIT exists only for tools/test_r312_gate.py running in a throwaway repository.
        return os.environ.get('R312_TEST_RESUME_COMMIT') or ledger()['resume_commit']
    rec = json.loads(receipt_path(b['requires_closed'][0]).read_text())
    return rec['closed_commit']


def is_evidence(path, sid=None):
    return path.startswith('construction/r31/r312/') or path.startswith('verification/r31/r312/')


# ------------------------------------------------------------------ check-plan
def check_plan():
    errs = []
    L = ledger()
    its = items()
    cards = load('ledgers/cards.json')
    eng = load('ledgers/r312_engineering_cards.json')
    ids = [c['id'] for c in cards]
    placed = [cid for b in L['batches'] for cid in b['cards']]
    if sorted(placed) != sorted(ids + [e['id'] for e in eng]):
        errs.append('every original and engineering card must be placed exactly once')
    if [b['order'] for b in L['batches']] != list(range(len(L['batches']))):
        errs.append('batch order must be 0..n-1')
    for i, b in enumerate(L['batches']):
        if b['batch'] != f'S{i:02d}':
            errs.append('batch id must equal its sequence number: ' + b['batch'])
        if b['requires_closed'] != ([] if i == 0 else [L['batches'][i - 1]['batch']]):
            errs.append(b['batch'] + ' must require exactly the previous batch')
        if not b['local_items']:
            errs.append(b['batch'] + ' has no blocking LOCAL item')
        if not (PLAN / b['doc']).is_file():
            errs.append(b['batch'] + ' document missing')
        if b['emu_items_executed_here'] != (b['batch'] == 'S20'):
            errs.append('EMU items run only in S20')
        if b['release_identity_freeze'] != (b['batch'] == 'S21'):
            errs.append('release identity freeze only in S21')
        if b['full_suite_must_pass'] != (i > 0):
            errs.append('full suite must pass from S01 on, and only S00 may record failures')
    # original phase DAG preserved: original phase index never decreases along the sequence
    order = {'P0%d' % k: k for k in range(9)}
    seen_max = -1
    for b in L['batches']:
        ph = [order[p] for p in b['original_phases']]
        if ph:
            if min(ph) < seen_max:
                errs.append(b['batch'] + ' places a card before a later original phase was finished')
            seen_max = max(seen_max, max(ph))
    by_card = {c['id']: c for c in cards}
    for c in cards:
        if c.get('r312_batch') not in {b['batch'] for b in L['batches']}:
            errs.append(c['id'] + ' lacks r312_batch')
        if c['status'] != 'DESIGNED_NOT_IMPLEMENTED':
            errs.append(c['id'] + ' plan status changed')
    # every original acceptance item covered, text hash preserved
    covered = {}
    for x in its:
        covered.setdefault(x['original_item'], []).append(x)
        if x['level'] not in L['levels']:
            errs.append(x['item'] + ' unknown level')
        if x['blocking'] != (x['level'] in L['blocking_levels']):
            errs.append(x['item'] + ' blocking flag inconsistent')
        b = batch(x['batch'])
        if (x['item'] in b['local_items']) != (x['level'] == 'LOCAL'):
            errs.append(x['item'] + ' batch list inconsistent')
    for c in cards + [{'id': e['id'], 'acceptance': e['acceptance']} for e in eng]:
        for n, text in enumerate(c['acceptance'], 1):
            key = f"{c['id']}#A{n}"
            parts = covered.get(key)
            if not parts:
                errs.append(key + ' not classified')
                continue
            h = hashlib.sha256(text.encode()).hexdigest()
            if any(p['original_text_sha256'] != h for p in parts):
                errs.append(key + ' original acceptance text changed')
    for x in its:
        if x['level'] == 'EMU' and not any(b['emu_items_executed_here'] for b in L['batches']):
            errs.append('EMU item without an executing batch: ' + x['item'])
    deferred = load('ledgers/r312_deferred_items.json')
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
def changed_files(b):
    base = start_commit(b)
    out = git('diff', '--name-only', base, 'HEAD')
    return [p for p in out.splitlines() if p]


def scope_report(b, bindings=None):
    allowed = set(b['scope_files'])
    dirs = [f for f in allowed if f.endswith('/')]
    ext = {e['path']: e.get('reason', '') for e in (bindings or {}).get('scope_extensions', [])}
    inside, free, declared, undeclared = [], [], [], []
    for p in changed_files(b):
        if p in allowed or any(p.startswith(d) for d in dirs):
            inside.append(p)
        elif any(fnmatch.fnmatch(p, pat) for pat in FREE_PATHS):
            free.append(p)
        elif p in ext and len(ext[p].strip()) >= 10:
            declared.append(p)
        else:
            undeclared.append(p)
    return {'base': start_commit(b), 'in_scope': inside, 'tests_tools_evidence': free,
            'declared_extensions': declared, 'undeclared': undeclared}


# ------------------------------------------------------------------ build
def tree_identity():
    head = git('rev-parse', 'HEAD')
    status = [l for l in git('status', '--porcelain').splitlines() if l and not is_evidence(l[3:])]
    return head, status


def parse_junit(dirpath):
    tests, counts = {}, {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}
    for x in sorted(Path(dirpath).glob('TEST-*.xml')):
        root = ET.parse(x).getroot()
        for case in root.iter('testcase'):
            name = case.get('classname', '') + '#' + case.get('name', '')
            if case.find('failure') is not None:
                st = 'FAIL'
                counts['failures'] += 1
            elif case.find('error') is not None:
                st = 'ERROR'
                counts['errors'] += 1
            elif case.find('skipped') is not None:
                st = 'SKIP'
                counts['skipped'] += 1
            else:
                st = 'PASS'
            counts['tests'] += 1
            node = case.find('failure') if st == 'FAIL' else case.find('error')
            tests[name] = {'status': st, 'message': (node.get('message', '')[:300] if node is not None else '')}
    return counts, tests


def run_logged(cmd, log, shell=False):
    with open(log, 'wb') as fh:
        r = subprocess.run(cmd, cwd=ROOT, stdout=fh, stderr=subprocess.STDOUT, shell=shell)
    return r.returncode


def post_path(sid):
    return summary_path(sid).with_name('POST_BUILD_CHECKS.json')


def post_entries(suite):
    normal = suite.get('host', [])
    post = suite.get('post_build', [])
    ids = [h.get('id') for h in normal + post]
    if any(not isinstance(i, str) or not re.fullmatch(r'[A-Za-z0-9_-]+', i) for i in ids) or len(set(ids)) != len(ids):
        die('HOST and post_build ids must be valid, unique and mutually distinct')
    if any(h.get('retired') or not isinstance(h.get('command'), str) or not h['command'].strip() for h in post):
        die('post_build checks require actual nonempty commands; they cannot be retired')
    return post


def execute_post_build(b, suite, out, logs):
    post = post_entries(suite)
    if not post:
        return
    raw = out.read_bytes()
    main = json.loads(raw)
    suite_file = ROOT / 'construction/r31/r312/HOST_SUITE.json'
    suite_sha = hashlib.sha256(suite_file.read_bytes()).hexdigest()
    head = git('rev-parse', 'HEAD')
    started = dt.datetime.now(dt.timezone.utc).isoformat()
    checks = []
    for h in post:
        start = dt.datetime.now(dt.timezone.utc).isoformat()
        rc = run_logged(h['command'], logs / f"post_{h['id']}.log", shell=True)
        checks.append({'id': h['id'], 'command': h['command'], 'exit': rc,
                       'status': 'PASS' if rc == 0 else 'FAIL', 'started_at': start,
                       'finished_at': dt.datetime.now(dt.timezone.utc).isoformat()})
    stable = (out.read_bytes() == raw and git('rev-parse', 'HEAD') == head and
              hashlib.sha256(suite_file.read_bytes()).hexdigest() == suite_sha)
    data = {'format': 'R312_POST_BUILD_CHECKS_V1', 'batch': b['batch'],
            'source_head': head, 'build_source_head': main['source_head'],
            'main_summary_sha256': hashlib.sha256(raw).hexdigest(), 'host_suite_sha256': suite_sha,
            'started_at': started, 'finished_at': dt.datetime.now(dt.timezone.utc).isoformat(),
            'inputs_unchanged': stable, 'artifact_checks': checks}
    post_path(b['batch']).write_text(json.dumps(data, ensure_ascii=False, indent=1) + '\n')
    # Main summary is immutable here: post failures never replace its real UNIT/HOST inputs.


def validate_post_build(b, main_path, main, problems):
    suite_file = ROOT / 'construction/r31/r312/HOST_SUITE.json'
    suite = json.loads(suite_file.read_text())
    expected = post_entries(suite)
    if not expected:
        return set(), None
    path = post_path(b['batch'])
    if not path.is_file():
        problems.append('post_build checks not executed/imported: ' + str(path))
        return set(), None
    try:
        data = json.loads(path.read_text())
        if data.get('format') != 'R312_POST_BUILD_CHECKS_V1' or data.get('batch') != b['batch']:
            raise ValueError('wrong post_build format/batch')
        if data.get('source_head') != git('rev-parse', 'HEAD') or data.get('build_source_head') != main['source_head']:
            raise ValueError('post_build source HEAD mismatch')
        if data['source_head'] != main['source_head']:
            raise ValueError('post_build must use the same source as the main build')
        if data.get('main_summary_sha256') != hashlib.sha256(main_path.read_bytes()).hexdigest():
            raise ValueError('post_build main summary SHA256 mismatch')
        if data.get('host_suite_sha256') != hashlib.sha256(suite_file.read_bytes()).hexdigest():
            raise ValueError('post_build HOST_SUITE SHA256 mismatch')
        if data.get('inputs_unchanged') is not True:
            raise ValueError('post_build changed its source/summary/suite inputs')
        checks = data.get('artifact_checks', [])
        if [c.get('id') for c in checks] != [h['id'] for h in expected]:
            raise ValueError('post_build check set/order mismatch')
        for actual, h in zip(checks, expected):
            rc = actual.get('exit')
            if actual.get('command') != h['command'] or type(rc) is not int:
                raise ValueError('post_build command/actual exit missing: ' + h['id'])
            if actual.get('status') != ('PASS' if rc == 0 else 'FAIL'):
                raise ValueError('post_build exit/status mismatch: ' + h['id'])
            if rc != 0:
                problems.append('post_build check failed: ' + h['id'])
        return {c['id'] for c in checks if c['exit'] == 0}, data
    except (ValueError, KeyError, TypeError) as error:
        problems.append('invalid post_build evidence: ' + str(error))
        return set(), None


def build(b, import_file=None, skip_gradle=False, import_post=None):
    out = summary_path(b['batch'])
    out.parent.mkdir(parents=True, exist_ok=True)
    if import_file:
        s = json.loads(Path(import_file).read_text())
        if s.get('format') != 'R312_BUILD_SUMMARY_V1' or s.get('batch') != b['batch']:
            die('imported summary has wrong format or batch')
        out.write_bytes(Path(import_file).read_bytes())
        pp = post_path(b['batch'])
        if import_post:
            pp.write_bytes(Path(import_post).read_bytes())
        elif pp.is_file():
            pp.unlink()
        print(json.dumps({k: s[k] for k in ('source_head', 'gradle', 'unit', 'host_failed')}, ensure_ascii=False))
        return
    if import_post:
        die('--import-post requires --import')
    head, dirty = tree_identity()
    if dirty:
        die('commit source changes before build; dirty: ' + ', '.join(dirty[:10]))
    suite_file = ROOT / 'construction/r31/r312/HOST_SUITE.json'
    if not suite_file.is_file():
        die('construction/r31/r312/HOST_SUITE.json missing (install it in S00)')
    suite = json.loads(suite_file.read_text())
    post_entries(suite)
    pp = post_path(b['batch'])
    if pp.is_file():
        pp.unlink()
    empty = [h['id'] for h in suite.get('host', []) if not h.get('retired') and not h.get('command')]
    if empty and b['batch'] != 'S00':
        die('HOST_SUITE entries without command: ' + ', '.join(empty))
    logs = out.parent / 'logs'
    logs.mkdir(exist_ok=True)
    started = dt.datetime.now(dt.timezone.utc).isoformat()
    pre = []
    for i, cmd in enumerate(suite.get('pre_build', [])):
        rc = run_logged(cmd, logs / f'pre_{i}.log', shell=True)
        pre.append({'command': cmd, 'exit': rc})
        if rc:
            die(f'pre_build failed: {cmd} (see {logs / f"pre_{i}.log"})')
    if git('status', '--porcelain'):
        changed = [l[3:] for l in git('status', '--porcelain').splitlines() if not is_evidence(l[3:])]
        if changed:
            die('pre_build changed tracked/untracked source files; commit them and rerun: ' + ', '.join(changed[:10]))
    gradle = {'state': 'NOT_RUN', 'tasks': GRADLE_TASKS, 'exit': None}
    unit, tests = {'tests': 0, 'failures': 0, 'errors': 0, 'skipped': 0}, {}
    if not skip_gradle:
        res = ROOT / 'app/build/test-results/testDebugUnitTest'
        if res.exists():
            for f in res.glob('TEST-*.xml'):
                f.unlink()
        cmd = ['bash', './gradlew', '--no-daemon', '--continue', '-Pr30Instrumentation=true'] + GRADLE_TASKS
        rc = run_logged(cmd, logs / 'gradle.log')
        text = (logs / 'gradle.log').read_text(errors='replace')
        gradle = {'state': 'PASS' if rc == 0 else 'FAIL', 'tasks': GRADLE_TASKS, 'exit': rc,
                  'unit_task_executed': '> Task :app:testDebugUnitTest' in text and 'testDebugUnitTest UP-TO-DATE' not in text,
                  'failed_tasks': sorted(set(re.findall(r"Execution failed for task '([^']+)'", text)))}
        if res.exists():
            unit, tests = parse_junit(res)
    host = []
    for h in suite.get('host', []):
        if h.get('retired'):
            host.append({'id': h['id'], 'status': 'RETIRED', 'replacement': h.get('replacement')})
            continue
        if not h.get('command'):
            host.append({'id': h['id'], 'status': 'NOT_CONFIGURED'})
            continue
        rc = run_logged(h['command'], logs / f"host_{h['id']}.log", shell=True)
        host.append({'id': h['id'], 'status': 'PASS' if rc == 0 else 'FAIL', 'exit': rc})
    s = {'format': 'R312_BUILD_SUMMARY_V1', 'batch': b['batch'], 'source_head': head, 'started_at': started,
         'finished_at': dt.datetime.now(dt.timezone.utc).isoformat(), 'environment': 'LOCAL',
         'pre_build': pre, 'gradle': gradle, 'unit': unit, 'tests': tests, 'host': host,
         'host_failed': [h['id'] for h in host if h['status'] in ('FAIL', 'NOT_CONFIGURED')],
         'release_only_tasks_not_run': RELEASE_ONLY_TASKS,
         'not_run_layers': {'EMU': 'NOT_RUN' if b['batch'] != 'S20' else 'SEE_S20_EMU', 'DEVICE': 'NOT_RUN', 'NATURAL': 'NOT_PROVEN'}}
    if os.environ.get('GITHUB_RUN_ID'):
        s['environment'] = 'CI'
        s['ci_run'] = os.environ['GITHUB_RUN_ID']
    out.write_text(json.dumps(s, ensure_ascii=False, indent=1) + '\n')
    execute_post_build(b, suite, out, logs)
    for f in logs.glob('*.log'):
        name = f.name
        failed = (name == 'gradle.log' and gradle['state'] == 'FAIL') or any(
            name == f"host_{h['id']}.log" and h['status'] == 'FAIL' for h in host)
        if not failed and f.stat().st_size > 256 * 1024:
            f.write_bytes(f.read_bytes()[-256 * 1024:])
    print(json.dumps({'source_head': head, 'gradle': gradle['state'], 'unit': unit, 'host_failed': s['host_failed']}, ensure_ascii=False))


# ------------------------------------------------------------------ close
def close(b):
    problems = []
    cur = current_batch()
    if cur is None or cur['batch'] != b['batch']:
        die('batch %s is not the open batch (open: %s)' % (b['batch'], cur['batch'] if cur else 'none'))
    for req in b['requires_closed']:
        if not receipt_path(req).is_file():
            problems.append('previous batch not closed: ' + req)
    head, dirty = tree_identity()
    if dirty:
        problems.append('working tree has non-evidence changes')
    sp = summary_path(b['batch'])
    if not sp.is_file():
        die('run build first: ' + str(sp))
    s = json.loads(sp.read_text())
    after = [p for p in git('diff', '--name-only', s['source_head'], 'HEAD').splitlines() if p and not is_evidence(p)]
    if after:
        problems.append('BUILD_SUMMARY is for %s; source changed since: %s' % (s['source_head'][:12], ', '.join(after[:10])))
    bp = bindings_path(b['batch'])
    if not bp.is_file():
        die('missing ' + str(bp))
    bindings = json.loads(bp.read_text())
    sc = scope_report(b, bindings)
    if sc['undeclared']:
        problems.append('main-source changes outside batch scope without reason: ' + ', '.join(sc['undeclared'][:20]))
    if s['gradle']['state'] == 'NOT_RUN':
        problems.append('gradle gate not executed')
    if not s['gradle'].get('unit_task_executed', True):
        problems.append('unit test task did not execute (UP-TO-DATE or missing)')
    full = b['full_suite_must_pass']
    if full:
        if s['gradle']['state'] != 'PASS':
            problems.append('gradle gate failed: ' + ', '.join(s['gradle'].get('failed_tasks', [])))
        if s['unit']['failures'] or s['unit']['errors']:
            problems.append('unit failures=%d errors=%d' % (s['unit']['failures'], s['unit']['errors']))
        if s['unit']['tests'] == 0:
            problems.append('zero unit tests executed')
        if s['host_failed']:
            problems.append('HOST failures: ' + ', '.join(s['host_failed']))
        prev = None
        if b['requires_closed']:
            prev = json.loads(receipt_path(b['requires_closed'][0]).read_text()).get('unit')
        if prev:
            if s['unit']['tests'] < prev['tests']:
                problems.append('unit test count dropped from %d to %d' % (prev['tests'], s['unit']['tests']))
            if s['unit']['skipped'] > prev['skipped']:
                problems.append('skipped tests increased from %d to %d' % (prev['skipped'], s['unit']['skipped']))
    post_pass, post_checks = validate_post_build(b, sp, s, problems)
    host_pass = {h['id'] for h in s['host'] if h['status'] == 'PASS'} | post_pass
    its = {x['item']: x for x in items() if x['batch'] == b['batch']}
    bound = bindings.get('items', {})
    results = {}
    for iid in b['local_items']:
        refs = bound.get(iid) or []
        if not refs:
            problems.append(iid + ' has no bound test')
            results[iid] = 'UNBOUND'
            continue
        bad = []
        for r in refs:
            if r.startswith('HOST:'):
                if r[5:] not in host_pass:
                    bad.append(r)
            else:
                t = s['tests'].get(r)
                if not t or t['status'] != 'PASS':
                    bad.append(r + ('' if t else ' (not executed)'))
        if bad:
            problems.append(iid + ' bound tests not passing: ' + ', '.join(bad))
            results[iid] = 'FAIL'
        else:
            results[iid] = 'PASS'
    for iid in bound:
        if iid not in its:
            problems.append('binding for item not in this batch: ' + iid)
        elif its[iid]['level'] != 'LOCAL':
            problems.append('non-LOCAL item cannot be bound/PASSed during construction: ' + iid)
    ev = 0
    for base in (ROOT / 'construction/r31/r312' / b['batch'], ROOT / 'verification/r31/r312' / b['batch']):
        if base.exists():
            for f in base.rglob('*'):
                if f.is_file():
                    ev += f.stat().st_size
                    if f.suffix in ('.java', '.kt'):
                        problems.append('source snapshot copied into evidence: ' + str(f.relative_to(ROOT)))
    if ev > EVIDENCE_MAX_BYTES:
        problems.append('evidence for batch exceeds %d bytes' % EVIDENCE_MAX_BYTES)
    if problems:
        print(json.dumps({'batch': b['batch'], 'state': 'NOT_CLOSED', 'problems': problems}, ensure_ascii=False, indent=1))
        sys.exit(1)
    deferred = {x['item']: ('NOT_RUN' if x['level'] != 'NATURAL' else 'NOT_PROVEN') for x in its.values() if x['level'] != 'LOCAL'}
    rec = {'format': 'R312_BATCH_RECEIPT_V1', 'batch': b['batch'], 'name': b['name'], 'cards': b['cards'],
           'closed_commit': head, 'build_source_head': s['source_head'], 'build_environment': s['environment'],
           'closed_at': dt.datetime.now(dt.timezone.utc).isoformat(), 'full_suite_must_pass': full,
           'unit': s['unit'], 'host': {h['id']: h['status'] for h in s['host']},
           'local_items': results, 'deferred_items': deferred, 'scope': sc,
           'post_build': ({'main_summary_sha256': post_checks['main_summary_sha256'],
                           'checks': {c['id']: c['status'] for c in post_checks['artifact_checks']}} if post_checks else None),
           'unit_failures_recorded_for_next_batch': [k for k, v in s['tests'].items() if v['status'] in ('FAIL', 'ERROR')] if not full else [],
           'claims_not_made': ['APK released', 'Android emulator PASS', 'device 24h/72h PASS', 'natural quality proven']}
    receipt_path(b['batch']).parent.mkdir(parents=True, exist_ok=True)
    receipt_path(b['batch']).write_text(json.dumps(rec, ensure_ascii=False, indent=1) + '\n')
    print(json.dumps({'batch': b['batch'], 'state': 'CLOSED', 'next': nxt(b)}, ensure_ascii=False))


def nxt(b):
    bs = ledger()['batches']
    return bs[b['order'] + 1]['batch'] if b['order'] + 1 < len(bs) else None


def status():
    b = current_batch()
    if not b:
        print(json.dumps({'state': 'ALL_BATCHES_CLOSED'}))
        return
    its = {x['item']: x for x in items()}
    bp = bindings_path(b['batch'])
    bound = json.loads(bp.read_text()).get('items', {}) if bp.is_file() else {}
    print(json.dumps({'open_batch': b['batch'], 'name': b['name'], 'doc': PLAN_REL + '/' + b['doc'], 'cards': b['cards'],
                      'start_commit': start_commit(b), 'local_items': len(b['local_items']),
                      'unbound_local_items': [i for i in b['local_items'] if not bound.get(i)],
                      'deferred_items': len(b['deferred_items'])}, ensure_ascii=False, indent=1))


def main():
    ap = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    ap.add_argument('command', choices=['status', 'check-plan', 'scope', 'build', 'close'])
    ap.add_argument('--batch')
    ap.add_argument('--import', dest='import_file')
    ap.add_argument('--import-post', help='POST_BUILD_CHECKS.json accompanying an imported main summary')
    ap.add_argument('--skip-gradle', action='store_true', help='HOST-only dry run; a summary without gradle cannot close a batch')
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
        bp = bindings_path(b['batch'])
        print(json.dumps(scope_report(b, json.loads(bp.read_text()) if bp.is_file() else None), ensure_ascii=False, indent=1))
    elif a.command == 'build':
        build(b, a.import_file, a.skip_gradle, a.import_post)
    elif a.command == 'close':
        close(b)


if __name__ == '__main__':
    main()
