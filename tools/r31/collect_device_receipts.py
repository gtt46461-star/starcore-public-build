#!/usr/bin/env python3
"""Collect R31 device evidence without inferring the opportunity denominator.

R32.5: a pre-registered calendar is independent of successful frozen rows; full
window queries use fetchmany, never a newest-N limit. DB/WAL copies must have
matching before/copy/after SHA256, otherwise the snapshot is unusable. The
calendar is a device-measurement input, never a source-batch acceptance gate.
"""
import argparse, datetime as dt, hashlib, json, re, shlex, sqlite3, subprocess, tempfile, time
from pathlib import Path

PKG = 'com.starcore.research.r13'
DB = 'binance_k3_nova_one_pro_v57.db'
CONDITIONS = ('FOREGROUND', 'SCREEN_OFF', 'BACKGROUND', 'BATTERY_RESTRICTED', 'NETWORK_LOSS_RECOVERY', 'ONE_KEY_CLEANUP', 'USER_FORCE_STOP')
REQUIRED = ('deviceWallMs', 'bootId', 'sourceCommit', 'apkSha256', 'condition', 'pssKb', 'dirBytes')
DIRS = ('databases', 'files', 'cache', 'no_backup', 'shared_prefs')
CALENDAR_FORMAT = 'R325_PREREGISTERED_DEVICE_CALENDAR_V1'
SNAPSHOT_FORMAT = 'R325_STABLE_DB_WINDOW_V1'
MAX_SNAPSHOT_ROWS = 1_000_000  # Per-operation memory guard; exhaustion is explicit BLOCKED, never complete.


def sha_bytes(data): return hashlib.sha256(data).hexdigest()
def sha_file(path):
    h = hashlib.sha256()
    with Path(path).open('rb') as f:
        for block in iter(lambda: f.read(1 << 20), b''): h.update(block)
    return h.hexdigest()
def canonical(value): return json.dumps(value, sort_keys=True, separators=(',', ':'), ensure_ascii=False).encode()
def positive_int(value): return isinstance(value, int) and not isinstance(value, bool) and value > 0


def issue_offset(issue, offset):
    if not isinstance(issue, str) or not re.fullmatch(r'\d{12}', issue): raise ValueError('BAD_ANCHOR_ISSUE')
    day = dt.datetime.strptime(issue[:8], '%Y%m%d').date()
    sequence = int(issue[8:])
    if not 1 <= sequence <= 1440: raise ValueError('BAD_ANCHOR_SEQUENCE')
    days, seq = divmod(sequence - 1 + offset, 1440)
    return (day + dt.timedelta(days=days)).strftime('%Y%m%d') + f'{seq + 1:04d}'


def make_calendar(anchor_issue, anchor_open_ms, count, registered_at_ms, source_commit, apk_sha, evidence_sha, exclusions=(), window_start_ms=None):
    if not positive_int(anchor_open_ms) or not positive_int(count) or not positive_int(registered_at_ms): raise ValueError('BAD_CALENDAR_INPUT')
    start = window_start_ms if window_start_ms is not None else registered_at_ms + 120000
    if not positive_int(start) or start < registered_at_ms or anchor_open_ms > registered_at_ms: raise ValueError('BAD_CALENDAR_START_OR_FUTURE_ANCHOR')
    # The exact existing TargetIssueResolver contract is one minute; no guessed game cadence.
    first_offset = max(1, (start - anchor_open_ms) // 60000 + 1)
    entries = []
    for offset in range(first_offset, first_offset + count):
        issue, deadline = issue_offset(anchor_issue, offset), anchor_open_ms + offset * 60000
        row = {'targetIssue': issue, 'deadlineAt': deadline, 'eligible': True}
        matches = [x for x in exclusions if x['fromMs'] <= deadline <= x['toMs']]
        if len(matches) > 1: raise ValueError('OVERLAPPING_EXCLUSIONS')
        if matches:
            x = matches[0]
            if not x.get('reason') or not re.fullmatch('[0-9a-f]{64}', x.get('evidenceSha256', '')): raise ValueError('EXCLUSION_EVIDENCE_REQUIRED')
            row.update(eligible=False, exclusionReason=x['reason'], exclusionEvidenceSha256=x['evidenceSha256'])
        entries.append(row)
    result = {'format': CALENDAR_FORMAT, 'role': 'DEVICE_ENGINEERING_ONLY_NOT_NATURAL_CERTIFICATE',
              'gameCode': 'Binance1-PK10', 'periodMs': 60000, 'registeredAtMs': registered_at_ms,
              'windowStartMs': start, 'windowEndMs': entries[-1]['deadlineAt'],
              'sourceCommit': source_commit, 'apkSha256': apk_sha, 'scheduleEvidenceSha256': evidence_sha,
              'anchorIssue': anchor_issue, 'anchorOpenMs': anchor_open_ms, 'entries': entries}
    problems = validate_calendar(result)
    if problems: raise ValueError(';'.join(problems))
    return result


def validate_calendar(cal):
    errors = []
    if not isinstance(cal, dict) or cal.get('format') != CALENDAR_FORMAT: return ['CALENDAR_MISSING_OR_OLD_FORMAT']
    if cal.get('gameCode') != 'Binance1-PK10' or cal.get('periodMs') != 60000: errors.append('UNVERIFIED_PERIOD_CONTRACT')
    for name, length in (('sourceCommit', 40), ('apkSha256', 64), ('scheduleEvidenceSha256', 64)):
        if not isinstance(cal.get(name), str) or not re.fullmatch('[0-9a-f]{%d}' % length, cal[name]): errors.append('CALENDAR_' + name + '_INVALID')
    start, end, registered = (cal.get(k) for k in ('windowStartMs', 'windowEndMs', 'registeredAtMs'))
    if not all(positive_int(x) for x in (start, end, registered)) or not registered <= start < end: return errors + ['CALENDAR_WINDOW_OR_REGISTRATION_INVALID']
    entries = cal.get('entries')
    if not isinstance(entries, list) or not entries: return errors + ['CALENDAR_ENTRIES_MISSING']
    seen, previous = set(), None
    try:
        anchor, opened = cal['anchorIssue'], cal['anchorOpenMs']
        if not positive_int(opened) or opened > registered: raise ValueError('bad/future anchor')
        issue_offset(anchor, 0)
        first_offset = max(1, (start - opened) // 60000 + 1)
        expected_count = (end - (opened + first_offset * 60000)) // 60000 + 1
        if expected_count != len(entries): errors.append('CALENDAR_COUNT_OR_GAP')
        for index, entry in enumerate(entries):
            issue, deadline = entry.get('targetIssue'), entry.get('deadlineAt')
            if issue in seen or issue != issue_offset(anchor, first_offset + index): errors.append('CALENDAR_ISSUE_DUPLICATE_OR_GAP')
            seen.add(issue)
            if not positive_int(deadline) or deadline != opened + (first_offset + index) * 60000 or not start < deadline <= end: errors.append('CALENDAR_DEADLINE_GAP')
            if type(entry.get('eligible')) is not bool: errors.append('CALENDAR_ELIGIBILITY_UNKNOWN')
            if entry.get('eligible') is False and (not entry.get('exclusionReason') or not re.fullmatch('[0-9a-f]{64}', str(entry.get('exclusionEvidenceSha256', '')))): errors.append('CALENDAR_EXCLUSION_UNPROVEN')
            previous = deadline
        if previous != end: errors.append('CALENDAR_END_NOT_COVERED')
    except (KeyError, TypeError, ValueError, OverflowError): errors.append('CALENDAR_ENTRY_INVALID')
    return sorted(set(errors))


def adb(*args, timeout=60):
    result = subprocess.run(['adb', *args], capture_output=True, timeout=timeout)
    if result.returncode: raise RuntimeError('ADB_FAILED:' + ' '.join(args[:4]))
    return result.stdout.decode(errors='replace').strip()


def remote_hash(name):
    # name comes only from the two fixed DB filenames below, never from user text.
    script = f'if [ -f databases/{name} ]; then sha256sum databases/{name}; else echo ABSENT; fi'
    text = adb('shell', 'run-as ' + PKG + ' sh -c ' + shlex.quote(script))
    if text == 'ABSENT': return None
    match = re.fullmatch(r'([0-9a-f]{64})\s+databases/' + re.escape(name), text)
    if not match: raise RuntimeError('DATABASE_FINGERPRINT_UNAVAILABLE')
    return match.group(1)


def _copy_stable_database(directory):
    names = (DB, DB + '-wal')
    before = {name: remote_hash(name) for name in names}
    if before[DB] is None: raise RuntimeError('DATABASE_ABSENT')
    for name in names:
        if before[name] is None: continue
        with (directory / name).open('wb') as dst:
            result = subprocess.run(['adb', 'exec-out', 'run-as', PKG, 'cat', 'databases/' + name], stdout=dst, stderr=subprocess.PIPE, timeout=600)
        if result.returncode: raise RuntimeError('DATABASE_COPY_FAILED')
    after = {name: remote_hash(name) for name in names}
    copied = {name: sha_file(directory / name) if (directory / name).exists() else None for name in names}
    if before != after or copied != before: raise RuntimeError('DATABASE_CHANGED_DURING_COPY')
    return {'before': before, 'copied': copied, 'after': after}


def _query_rows(c, sql, params=()):
    cursor = c.execute(sql, params); names = [x[0] for x in cursor.description]; count = 0
    while True:
        page = cursor.fetchmany(256)
        if not page: break
        for raw in page:
            count += 1
            if count > MAX_SNAPSHOT_ROWS: raise RuntimeError('SNAPSHOT_ROW_BUDGET_EXCEEDED_NOT_COMPLETE')
            yield dict(zip(names, raw))


def existing_causal_evidence(c, names, start_ms, end_ms):
    """Read existing R31 -> R974 -> canonical COMMIT-clock bindings, if present.

    This optional lane never manufactures a new receipt. Raw legacy shadows do
    not have this typed-mainline join and remain unknown unless observed early.
    """
    needed = {'r31_mainline_freeze', 'r974_exact_real_receipt', 'v57_shadow_observation_r35', 'r27_chart_cache'}
    if not needed.issubset(names): return {'available': False, 'reason': 'EXISTING_TYPED_MAINLINE_TABLES_MISSING'}
    where = 'EXISTS(SELECT 1 FROM r27_live_frozen f WHERE f.target_issue=x.target_issue AND f.deadline_at>? AND f.deadline_at<=?)'
    params = (start_ms, end_ms)
    try:
        guards = list(_query_rows(c, "SELECT name,sql FROM sqlite_master WHERE type='trigger' AND name IN ('r31_mainline_freeze_no_update','r31_mainline_freeze_no_delete','r974_real_no_update')"))
        typed = list(_query_rows(c, 'SELECT policy_hash,target_ordinal,target_issue,choice_id,certificate_digest,model_state_hash,source_revision,source_schema,selection_fence,CASE WHEN length(CAST(proof_json AS BLOB))<=16384 THEN proof_json ELSE NULL END AS proof_json,proof_hash,created_at FROM r31_mainline_freeze x WHERE '+where, params))
        receipts = list(_query_rows(c, 'SELECT target_issue,trade_id,candidate_id,model_hash,certificate_hash,proof_hash,recipe_hash,action_hash,authorization_hash,committed_at FROM r974_exact_real_receipt x WHERE '+where, params))
        originals = list(_query_rows(c, 'SELECT target_issue,game_code,base_issue,position,n1,n2,n3,authorization_hash,record_hash,pre_draw_frozen,analysis_attempt_id,deadline_at,commit_at FROM v57_shadow_observation_r35 x WHERE '+where, params))
        events = list(_query_rows(c, "SELECT key,CASE WHEN length(CAST(payload_json AS BLOB))<=65536 THEN payload_json ELSE NULL END AS payload_json FROM r27_chart_cache WHERE panel_id='r30-timeline-r1:PRED' AND updated_at>? AND updated_at<=? ORDER BY key", (start_ms-60000, end_ms)))
        return {'available': True, 'guards': guards, 'typedFreezes': typed, 'realReceipts': receipts, 'canonicalPredictions': originals, 'predictionEvents': events}
    except (RuntimeError, sqlite3.Error) as error:
        return {'available': False, 'reason': type(error).__name__+':'+str(error)}


def database_facts(c, start_ms, end_ms, observed_at_ms, copy_proof):
    """Read a coherent private copy; no live connection or newest-N query."""
    if not positive_int(start_ms) or not positive_int(end_ms) or start_ms >= end_ms: raise ValueError('EXPLICIT_TIME_WINDOW_REQUIRED')
    c.execute('PRAGMA query_only=ON')
    if c.execute('PRAGMA quick_check').fetchone()[0] != 'ok': raise RuntimeError('DATABASE_QUICK_CHECK_FAILED')
    names = {r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table'")}
    out = {'pulled': True, 'format': SNAPSHOT_FORMAT, 'complete': False, 'consistentCopy': copy_proof,
           'windowStartMs': start_ms, 'windowEndMs': end_ms, 'observedAtMs': observed_at_ms}
    needed = {'draws', 'r27_live_frozen', 'r26_task'}
    if not needed.issubset(names): raise RuntimeError('DATABASE_REQUIRED_TABLES_MISSING:' + ','.join(sorted(needed - names)))
    c.execute('BEGIN')
    try:
        out['drawRows'] = list(_query_rows(c, 'SELECT issue,open_time,first_seen_at,dirty FROM draws WHERE open_time>? AND open_time<=? ORDER BY open_time,issue', (start_ms, end_ms)))
        out['freezeRows'] = list(_query_rows(c, 'SELECT policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at,hit,settled_at FROM r27_live_frozen WHERE deadline_at>? AND deadline_at<=? ORDER BY target_ordinal,policy_hash', (start_ms, end_ms)))
        for row in out['freezeRows']:
            blob = row.pop('probabilities')
            row['probabilities_sha256'] = sha_bytes(blob) if isinstance(blob, bytes) else None
        out['tasks'] = [list(x.values()) for x in _query_rows(c, 'SELECT task_id,status,cursor,last_progress,coalesce(blocked_reason,"") AS blocked_reason FROM r26_task ORDER BY task_id')]
        out['causalEvidence'] = existing_causal_evidence(c, names, start_ms, end_ms)
        if 'r31_commit_receipt' in names: out['commits'] = list(c.execute('SELECT count(*),max(committed_at) FROM r31_commit_receipt').fetchone())
        out['complete'] = True
    finally: c.rollback()
    return out


def _db_facts(start_ms, end_ms):
    try:
        with tempfile.TemporaryDirectory(prefix='r325-device-copy-') as temporary:
            directory = Path(temporary); proof = _copy_stable_database(directory)
            observed = int(adb('shell', 'date', '+%s%3N'))
            with sqlite3.connect(directory / DB) as c: return database_facts(c, start_ms, end_ms, observed, proof)
    except (RuntimeError, ValueError, OSError, sqlite3.Error, subprocess.SubprocessError) as error:
        return {'pulled': False, 'format': SNAPSHOT_FORMAT, 'complete': False, 'reason': type(error).__name__ + ':' + str(error)}


def sample(condition, source_commit, apk_sha, with_db, start_ms=None, end_ms=None):
    meminfo = adb('shell', 'dumpsys', 'meminfo', PKG)
    pss = re.search(r'TOTAL(?: PSS)?:?\s+(\d+)', meminfo); heap = re.search(r'Java Heap:\s+(\d+)', meminfo)
    dirs = {}
    for directory in DIRS:
        try: found = re.match(r'(\d+)', adb('shell', 'run-as', PKG, 'du', '-sk', directory))
        except RuntimeError: found = None
        dirs[directory] = int(found.group(1)) * 1024 if found else None
    try: total = re.match(r'(\d+)', adb('shell', 'run-as', PKG, 'du', '-sk', '.'))
    except RuntimeError: total = None
    dirs['UNCLASSIFIED'] = max(0, int(total.group(1)) * 1024 - sum(dirs.values())) if total and all(v is not None for v in dirs.values()) else None
    try: pid = adb('shell', 'pidof', PKG)
    except RuntimeError: pid = ''
    out = {'deviceWallMs': int(adb('shell', 'date', '+%s%3N')), 'bootId': adb('shell', 'cat', '/proc/sys/kernel/random/boot_id'),
           'hostMonotonicNs': time.monotonic_ns(), 'sourceCommit': source_commit, 'apkSha256': apk_sha, 'condition': condition,
           'pid': pid,
           'pssKb': int(pss.group(1)) if pss else None, 'javaHeapKb': int(heap.group(1)) if heap else None, 'dirBytes': dirs,
           'exitInfo': adb('shell', 'dumpsys', 'activity', 'exit-info', PKG)[:4000]}
    if with_db:
        out['db'] = _db_facts(start_ms, end_ms)
    # Kernel elapsed time lets an existing post-COMMIT event retain its own
    # timestamp even if the database is copied many minutes later.
    out['deviceElapsedMs'] = int(float(adb('shell', 'cat', '/proc/uptime').split()[0])*1000)
    out['deviceWallMs'] = int(adb('shell', 'date', '+%s%3N'))
    return out


def collect(out, condition, interval, duration, source_commit, apk_sha, db_every, calendar=None):
    if condition not in CONDITIONS or interval <= 0 or duration <= 0 or db_every <= 0: raise ValueError('INVALID_COLLECTION_ARGUMENT')
    if calendar is not None:
        errors = validate_calendar(calendar)
        if errors: raise ValueError(';'.join(errors))
        if calendar['sourceCommit'] != source_commit or calendar['apkSha256'] != apk_sha: raise ValueError('CALENDAR_INSTALL_IDENTITY_MISMATCH')
    begin = int(adb('shell', 'date', '+%s%3N'))
    start, end = (calendar['windowStartMs'], calendar['windowEndMs']) if calendar else (begin, begin + duration * 1000)
    header = {'header': True, 'format': 'R325_DEVICE_COLLECTION_V1', 'model': adb('shell', 'getprop', 'ro.product.model'),
              'intervalSeconds': interval, 'condition': condition, 'calendar': calendar,
              'calendarSha256': sha_bytes(canonical(calendar)) if calendar else None,
              'sourceCommit': source_commit, 'apkSha256': apk_sha}
    until, count = time.monotonic() + duration, 0
    with open(out, 'a', encoding='utf-8') as f:
        f.write(json.dumps(header, ensure_ascii=False) + '\n'); f.flush()
        while True:
            final_sample = time.monotonic() >= until
            row = sample(condition, source_commit, apk_sha, final_sample or count % db_every == 0, start, end)
            f.write(json.dumps(row, ensure_ascii=False) + '\n'); f.flush(); count += 1
            if final_sample: break
            time.sleep(min(interval, max(0, until - time.monotonic())))
    return count


def load(paths):
    rows, headers, ordering = [], [], []
    for p in paths:
        previous = None
        with Path(p).open(encoding='utf-8') as f:
            for number, line in enumerate(f, 1):
                if not line.strip(): continue
                row = json.loads(line)
                if row.get('header'): headers.append(row); continue
                if previous is not None and row.get('deviceWallMs', 0) < previous: ordering.append('WALL_CLOCK_REVERSED:' + str(Path(p).name) + ':' + str(number))
                previous = row.get('deviceWallMs', 0); rows.append(row)
    return headers, rows, ordering


def merge_windows(paths):
    headers, incoming, problems = load(paths)
    unique = {}
    for row in incoming:
        key = (row.get('bootId'), row.get('deviceWallMs'))
        if key in unique and canonical(row) != canonical(unique[key]): problems.append('DUPLICATE_SAMPLE_CONFLICT:' + str(key))
        else: unique[key] = row
    rows = sorted(unique.values(), key=lambda row: row.get('deviceWallMs', 0))
    intervals = [h.get('intervalSeconds') for h in headers]
    if not intervals or any(not positive_int(i) for i in intervals): problems.append('INVALID_SAMPLING_INTERVAL'); interval = 60
    else: interval = min(intervals)
    windows, gaps = [], []
    for row in rows:
        if not windows or windows[-1]['bootId'] != row.get('bootId'):
            windows.append({'bootId': row.get('bootId'), 'fromMs': row['deviceWallMs'], 'toMs': row['deviceWallMs'], 'samples': 0, 'conditions': []})
        window = windows[-1]
        if window['samples'] and row['deviceWallMs'] - window['toMs'] > 3 * interval * 1000: gaps.append({'bootId': window['bootId'], 'fromMs': window['toMs'], 'toMs': row['deviceWallMs']})
        window['toMs'] = row['deviceWallMs']; window['samples'] += 1
        if row.get('condition') not in window['conditions']: window['conditions'].append(row.get('condition'))
    calendars = {sha_bytes(canonical(h['calendar'])): h['calendar'] for h in headers if h.get('calendar') is not None}
    for h in headers:
        if h.get('calendar') is not None and h.get('calendarSha256') != sha_bytes(canonical(h['calendar'])): problems.append('CALENDAR_HASH_MISMATCH')
    if len(calendars) != 1: problems.append('MISSING_OR_MIXED_CALENDAR')
    calendar = next(iter(calendars.values())) if len(calendars) == 1 else None
    return {'format': 'R325_MERGED_DEVICE_EVIDENCE_V1', 'intervalSeconds': interval, 'samples': rows, 'windows': windows,
            'gaps': gaps, 'calendar': calendar, 'collectionProblems': sorted(set(problems)),
            'models': sorted({h.get('model') for h in headers if h.get('model')}), 'deduplicatedSamples': len(incoming) - len(rows)}


def verify_scope(merged, source_commit=None, apk_sha=None):
    problems = list(merged.get('collectionProblems', [])); rows = merged.get('samples', [])
    if not rows: return problems + ['NO_SAMPLES']
    for key in REQUIRED:
        count = sum(row.get(key) in (None, '', {}) for row in rows if not (key == 'pssKb' and row.get('condition') == 'USER_FORCE_STOP'))
        if count: problems.append('MISSING_%s:%d' % (key, count))
    commits, apks = {r.get('sourceCommit') for r in rows}, {r.get('apkSha256') for r in rows}
    if len(commits) != 1: problems.append('MIXED_SOURCE:' + ','.join(sorted(map(str, commits))))
    if len(apks) != 1: problems.append('MIXED_APK:' + ','.join(sorted(map(str, apks))))
    if source_commit and commits != {source_commit}: problems.append('SOURCE_NOT_THE_DELIVERY')
    if apk_sha and apks != {apk_sha}: problems.append('APK_NOT_THE_DELIVERY')
    if merged.get('gaps'): problems.append('SAMPLING_GAPS:%d' % len(merged['gaps']))
    interval=merged.get('intervalSeconds',60)
    if not positive_int(interval): problems.append('INVALID_SAMPLING_INTERVAL')
    else:
        for earlier,later in zip(rows,rows[1:]):
            delta=later.get('deviceWallMs',0)-earlier.get('deviceWallMs',0)
            if delta<0: problems.append('WALL_CLOCK_REVERSED_IN_MERGED_ROWS')
            if delta>3*interval*1000: problems.append('SAMPLING_GAP_RECOMPUTED_FROM_ORIGINAL_ROWS')
    cal = merged.get('calendar'); calendar_errors=validate_calendar(cal)
    problems.extend(calendar_errors)
    if isinstance(cal,dict) and (commits != {cal.get('sourceCommit')} or apks != {cal.get('apkSha256')}): problems.append('CALENDAR_SOURCE_OR_APK_MISMATCH')
    if not calendar_errors and (rows[0]['deviceWallMs'] > cal['windowStartMs'] or rows[-1]['deviceWallMs'] < cal['windowEndMs']): problems.append('CALENDAR_OBSERVATION_WINDOW_INCOMPLETE')
    return sorted(set(problems))


def main():
    parser = argparse.ArgumentParser(description=__doc__); sub = parser.add_subparsers(dest='cmd', required=True)
    generate = sub.add_parser('calendar'); generate.add_argument('--anchor-issue', required=True); generate.add_argument('--anchor-open-ms', type=int, required=True)
    generate.add_argument('--count', type=int, required=True); generate.add_argument('--registered-at-ms', type=int, required=True)
    generate.add_argument('--window-start-ms', type=int, help='Default: registration +120s, allowing collection to start before the window')
    generate.add_argument('--source-commit', required=True); generate.add_argument('--apk', required=True); generate.add_argument('--schedule-evidence', required=True)
    generate.add_argument('--exclusions'); generate.add_argument('--out', required=True)
    c = sub.add_parser('collect'); c.add_argument('--out', required=True); c.add_argument('--condition', required=True, choices=CONDITIONS)
    c.add_argument('--interval', type=int, default=60); c.add_argument('--duration', type=int, required=True)
    c.add_argument('--source-commit', required=True); c.add_argument('--apk', required=True); c.add_argument('--db-every', type=int, default=10); c.add_argument('--opportunities')
    merge = sub.add_parser('merge'); merge.add_argument('files', nargs='+'); merge.add_argument('--out', required=True); merge.add_argument('--source-commit'); merge.add_argument('--apk-sha')
    args = parser.parse_args()
    if args.cmd == 'calendar':
        exclusions = json.loads(Path(args.exclusions).read_text()) if args.exclusions else []
        value = make_calendar(args.anchor_issue, args.anchor_open_ms, args.count, args.registered_at_ms, args.source_commit, sha_file(args.apk), sha_file(args.schedule_evidence), exclusions, args.window_start_ms)
        Path(args.out).write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n'); print(json.dumps({'status': 'CALENDAR_CREATED_NOT_DEVICE_PASS', 'entries': len(value['entries']), 'sha256': sha_bytes(canonical(value))}))
    elif args.cmd == 'collect':
        calendar = json.loads(Path(args.opportunities).read_text()) if args.opportunities else None
        print('R325_DEVICE_SAMPLES', collect(args.out, args.condition, args.interval, args.duration, args.source_commit, sha_file(args.apk), args.db_every, calendar))
    else:
        merged = merge_windows(args.files); merged['scopeProblems'] = verify_scope(merged, args.source_commit, args.apk_sha)
        Path(args.out).write_text(json.dumps(merged, ensure_ascii=False) + '\n'); print(json.dumps({'windows': len(merged['windows']), 'gaps': len(merged['gaps']), 'scopeProblems': merged['scopeProblems']}))


if __name__ == '__main__': main()
