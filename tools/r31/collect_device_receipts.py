#!/usr/bin/env python3
"""R31-B12-C03 device evidence collector (DEVICE layer; the target phone is V2353A).

collect        samples the installed app over adb every --interval seconds and appends JSON lines: device wall time,
               boot id, pid, PSS/Java heap, bytes of every app directory (databases, files, cache, no_backup, and the
               rest as UNCLASSIFIED), exit reasons, and - from a pulled database copy - every target's frozen/deadline/
               settled times and task commit progress. The operator labels the test condition (FOREGROUND, SCREEN_OFF,
               BACKGROUND, BATTERY_RESTRICTED, NETWORK_LOSS_RECOVERY, ONE_KEY_CLEANUP, USER_FORCE_STOP); conditions are
               never merged.
merge_windows  merges collection files by device time, splits them into boot windows and lists sampling gaps.
verify_scope   every sample must name the same source commit and APK digest and carry every required field; a mixed
               or unknown source is reported, never averaged.
Nothing here is a PASS by itself; tools/r31/accept_device_receipts.py evaluates the merged evidence.
"""
import argparse, hashlib, json, re, sqlite3, subprocess, sys, tempfile, time
from pathlib import Path

PKG = 'com.starcore.research.r13'
DB = 'binance_k3_nova_one_pro_v57.db'
CONDITIONS = ('FOREGROUND', 'SCREEN_OFF', 'BACKGROUND', 'BATTERY_RESTRICTED', 'NETWORK_LOSS_RECOVERY', 'ONE_KEY_CLEANUP', 'USER_FORCE_STOP')
REQUIRED = ('deviceWallMs', 'bootId', 'sourceCommit', 'apkSha256', 'condition', 'pssKb', 'dirBytes')
DIRS = ('databases', 'files', 'cache', 'no_backup', 'shared_prefs')


def adb(*args, timeout=60):
    r = subprocess.run(['adb', *args], capture_output=True, timeout=timeout)
    return r.stdout.decode(errors='replace').strip()


def _db_facts():
    with tempfile.TemporaryDirectory() as t:
        target = Path(t) / DB
        for suffix in ('', '-wal', '-shm'):
            raw = subprocess.run(['adb', 'exec-out', 'run-as', PKG, 'cat', 'databases/' + DB + suffix], capture_output=True, timeout=600).stdout
            if raw and not raw.startswith(b'cat:'):
                (Path(t) / (DB + suffix)).write_bytes(raw)
        if not target.is_file():
            return {'pulled': False}
        with sqlite3.connect(target) as c:
            names = {r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table'")}
            out = {'pulled': True}
            if 'r27_live_frozen' in names:
                out['frozen'] = [list(r) for r in c.execute('SELECT target_issue,min(frozen_at),min(deadline_at),max(settled_at),max(hit) FROM r27_live_frozen GROUP BY target_issue ORDER BY target_issue DESC LIMIT 2000')]
            if 'draws' in names:
                out['draws'] = [list(r) for r in c.execute('SELECT issue,open_time FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 2000')]
            if 'r26_task' in names:
                out['tasks'] = [list(r) for r in c.execute('SELECT task_id,status,cursor,last_progress,coalesce(blocked_reason,"") FROM r26_task')]
            if 'r31_commit_receipt' in names:
                out['commits'] = c.execute('SELECT count(*),max(committed_at) FROM r31_commit_receipt').fetchone()
            return out


def sample(condition, source_commit, apk_sha, with_db):
    meminfo = adb('shell', 'dumpsys', 'meminfo', PKG)
    pss = re.search(r'TOTAL(?: PSS)?:?\s+(\d+)', meminfo); heap = re.search(r'Java Heap:\s+(\d+)', meminfo)
    dirs = {}
    for d in DIRS:
        m = re.match(r'(\d+)', adb('shell', 'run-as', PKG, 'du', '-sk', d) or '')
        dirs[d] = int(m.group(1)) * 1024 if m else None
    total = re.match(r'(\d+)', adb('shell', 'run-as', PKG, 'du', '-sk', '.') or '')
    if total and all(v is not None for v in dirs.values()):
        dirs['UNCLASSIFIED'] = max(0, int(total.group(1)) * 1024 - sum(dirs.values()))
    s = {'deviceWallMs': int(adb('shell', 'date', '+%s%3N') or 0), 'bootId': adb('shell', 'cat', '/proc/sys/kernel/random/boot_id'),
         'sourceCommit': source_commit, 'apkSha256': apk_sha, 'condition': condition, 'pid': adb('shell', 'pidof', PKG),
         'pssKb': int(pss.group(1)) if pss else None, 'javaHeapKb': int(heap.group(1)) if heap else None, 'dirBytes': dirs,
         'exitInfo': adb('shell', 'dumpsys', 'activity', 'exit-info', PKG)[:4000]}
    if with_db:
        s['db'] = _db_facts()
    return s


def collect(out, condition, interval, duration, source_commit, apk_sha, db_every):
    if condition not in CONDITIONS:
        raise SystemExit('UNKNOWN_CONDITION ' + condition)
    if adb('shell', 'getprop', 'ro.product.model') == '':
        raise SystemExit('NO_DEVICE')
    end = time.time() + duration; n = 0
    with open(out, 'a') as f:
        f.write(json.dumps({'header': True, 'model': adb('shell', 'getprop', 'ro.product.model'), 'intervalSeconds': interval, 'condition': condition}) + '\n')
        while time.time() < end:
            f.write(json.dumps(sample(condition, source_commit, apk_sha, n % db_every == 0)) + '\n'); f.flush(); n += 1
            time.sleep(interval)
    return n


def load(paths):
    rows, headers = [], []
    for p in paths:
        for line in Path(p).read_text().splitlines():
            if not line.strip():
                continue
            r = json.loads(line)
            (headers if r.get('header') else rows).append(r)
    return headers, rows


def merge_windows(paths):
    headers, rows = load(paths)
    rows.sort(key=lambda r: r.get('deviceWallMs', 0))
    interval = max([h.get('intervalSeconds', 60) for h in headers] or [60])
    windows, gaps = [], []
    for r in rows:
        if not windows or windows[-1]['bootId'] != r.get('bootId'):
            windows.append({'bootId': r.get('bootId'), 'fromMs': r['deviceWallMs'], 'toMs': r['deviceWallMs'], 'samples': 0, 'conditions': []})
        w = windows[-1]
        if w['samples'] and r['deviceWallMs'] - w['toMs'] > 3 * interval * 1000:
            gaps.append({'bootId': w['bootId'], 'fromMs': w['toMs'], 'toMs': r['deviceWallMs']})
        w['toMs'] = r['deviceWallMs']; w['samples'] += 1
        if r.get('condition') not in w['conditions']:
            w['conditions'].append(r.get('condition'))
    return {'intervalSeconds': interval, 'samples': rows, 'windows': windows, 'gaps': gaps,
            'models': sorted({h.get('model') for h in headers if h.get('model')})}


def verify_scope(merged, source_commit=None, apk_sha=None):
    problems = []
    rows = merged['samples']
    if not rows:
        return ['NO_SAMPLES']
    for k in REQUIRED:
        missing = sum(1 for r in rows if r.get(k) in (None, '', {}))
        if missing:
            problems.append('MISSING_%s:%d' % (k, missing))
    commits = {r.get('sourceCommit') for r in rows}; apks = {r.get('apkSha256') for r in rows}
    if len(commits) != 1: problems.append('MIXED_SOURCE:' + ','.join(sorted(map(str, commits))))
    if len(apks) != 1: problems.append('MIXED_APK:' + ','.join(sorted(map(str, apks))))
    if source_commit and commits != {source_commit}: problems.append('SOURCE_NOT_THE_DELIVERY')
    if apk_sha and apks != {apk_sha}: problems.append('APK_NOT_THE_DELIVERY')
    if merged['gaps']: problems.append('SAMPLING_GAPS:%d' % len(merged['gaps']))
    return problems


def main():
    ap = argparse.ArgumentParser(); sub = ap.add_subparsers(dest='cmd', required=True)
    c = sub.add_parser('collect'); c.add_argument('--out', required=True); c.add_argument('--condition', required=True, choices=CONDITIONS)
    c.add_argument('--interval', type=int, default=60); c.add_argument('--duration', type=int, required=True)
    c.add_argument('--source-commit', required=True); c.add_argument('--apk', required=True); c.add_argument('--db-every', type=int, default=10)
    m = sub.add_parser('merge'); m.add_argument('files', nargs='+'); m.add_argument('--out', required=True)
    m.add_argument('--source-commit'); m.add_argument('--apk-sha')
    a = ap.parse_args()
    if a.cmd == 'collect':
        n = collect(a.out, a.condition, a.interval, a.duration, a.source_commit, hashlib.sha256(Path(a.apk).read_bytes()).hexdigest(), a.db_every)
        print('R31_DEVICE_SAMPLES', n)
    else:
        merged = merge_windows(a.files); merged['scopeProblems'] = verify_scope(merged, a.source_commit, a.apk_sha)
        Path(a.out).write_text(json.dumps(merged) + '\n'); print(json.dumps({'windows': len(merged['windows']), 'gaps': len(merged['gaps']), 'scopeProblems': merged['scopeProblems']}))


if __name__ == '__main__':
    main()
