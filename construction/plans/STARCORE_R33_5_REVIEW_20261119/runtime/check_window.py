"""Replay a preregistered 100-period window against the enabled journal and SQLite originals."""
import argparse
import importlib.util
import json
import math
import re
import sqlite3
from pathlib import Path

from prepare_tools import verified_window_path

def summarize(rows, limit):
    values = sorted(row['ms'] for row in rows)
    return {'observed':len(values), 'limitMs':limit,
            'p95Ms':values[math.ceil(len(values)*0.95)-1] if values else None,
            'maxMs':max(values, default=None),
            'violations':[row for row in rows if row['ms'] < 0 or row['ms'] > limit]}

HEALTH = 'v57_runtime_health_r35'
FREEZE_RETURN = 'R335_CANONICAL_RETURN_BOUND'
SETTLE_RETURN = 'SETTLEMENT_TERMINAL'


def observed_return(db, trace, issue, event, attempt, expected_status, detail, earliest):
    """Read only this owner's exact identity. Async insert time is evidence availability."""
    rows = db.execute(
        'SELECT * FROM ' + HEALTH + ' WHERE target_issue=? AND event_type=? AND attempt_id=?',
        (issue, event, attempt))
    times = []
    for row in rows:
        # The original application also emits older settlement records of other types/meanings.
        if event == SETTLE_RETURN and not str(row['detail']).startswith('R334_DURABLE_RETURN_OBSERVATION|'):
            continue
        at = row['commit_at']
        if (row['stage'] != event or row['status'] != expected_status or row['terminal'] != 1
                or not all(type(row[k]) is int for k in ('request_at', 'commit_at', 'event_at', 'elapsed_ms'))
                or not (0 < row['request_at'] <= at <= row['event_at']) or row['elapsed_ms'] < 0
                or at < earliest
                or row['detail'] != detail + str(at)
                or row['record_hash'] != trace.digest('R36_HEALTH', event, attempt, issue, expected_status,
                                                       row['event_at'], row['elapsed_ms'], row['detail'])):
            raise ValueError('DURABLE_RETURN_IDENTITY_OR_TIME:' + event)
        times.append((at, row['request_at'], row['elapsed_ms']))
    if not times:
        raise ValueError('MISSING_DURABLE_RETURN:' + event)
    return times


def durable_freeze_time(db, trace, issue, row, drawn):
    request = trace.single(db, trace.REQUEST, 'target_issue', issue)
    if (not re.fullmatch('[0-9a-f]{64}', request['request_hash'] or '')
            or type(request['generation']) is not int or request['generation'] <= 0):
        raise ValueError('DURABLE_REQUEST_IDENTITY')
    attempt = request['request_hash'][:24] + '-' + str(request['generation'])
    freeze = observed_return(db, trace, issue, FREEZE_RETURN, attempt, 'OBSERVED',
                             'R335_CANONICAL_OBSERVED|canonicalHash=' + request['canonical_hash'] + '|observedAt=',
                             row['frozenAt'])
    if any(request_at != request['request_at'] for _, request_at, _ in freeze):
        raise ValueError('DURABLE_REQUEST_TIME')
    # Multiple exact re-reads can follow an earlier commit. Only the first observation
    # is an upper bound on availability; a later re-read does not erase that observation.
    first = min(at for at, _, _ in freeze)
    if first >= min(row['scheduledDeadline'], drawn['open_time'], drawn['first_seen_at']):
        raise ValueError('DURABLE_FREEZE_ONLY_OBSERVED_AFTER_CUTOFF')
    return first


def durable_settlement_time(db, trace, issue, task):
    detail = ('R334_DURABLE_RETURN_OBSERVATION|NOT_SQL_COMMIT_INSTANT|MODEL_CALLS=0|settlementHash='
              + task['settlement_hash'] + '|returnedAt=')
    settlement = observed_return(db, trace, issue, SETTLE_RETURN, 'SET-' + task['draw_hash'][:16],
                                 'COMPLETED', detail, task['completed_at'])
    if len(settlement) != 1 or settlement[0][1] > task['completed_at']:
        raise ValueError('AMBIGUOUS_SETTLEMENT_RETURN')
    returned, started, elapsed = settlement[0]
    # SettlementEngine samples SystemClock.elapsedRealtime() before and after the
    # coordinator call. Keep this independent duration when the wall clock steps.
    return returned, started + elapsed


def evaluate(tools, bundle, evidence, start, end, commit, apk, scope):
    path = verified_window_path(tools)
    spec = importlib.util.spec_from_file_location('r335_window', path)
    window = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(window)
    inputs = {'traceBundle': bundle, 'startCapture': start, 'endCapture': end}
    hashes = {role: window.trace.sha_file(Path(path)) for role, path in inputs.items()}
    manifest = window.trace.get_json(bundle)
    coverage = window.reconcile(bundle, evidence, start, end, scope, commit, apk)
    closed = window.trace.verify(bundle, evidence, scope, commit, apk)
    traces = {row['target']:row for row in closed['outcomes']}
    dbpath = window.trace.member(evidence.resolve(), manifest['database'])
    research_path = Path(__file__).resolve().parents[1] / 'research/check_research.py'
    research_spec = importlib.util.spec_from_file_location('r335_window_research', research_path)
    research = importlib.util.module_from_spec(research_spec)
    research_spec.loader.exec_module(research)
    validated = research.settlement_times(window.trace, dbpath, closed['outcomes'],
                                         manifest['database']['sha256'])
    disabled_targets = {row['target'] for row in coverage['entries']
                        if row['coverage'] == 'EXPLICITLY_DISABLED'}
    disabled_failures = [row for row in closed['missingOrInvalid'] if row['target'] in disabled_targets]
    enabled_failures = [row for row in closed['missingOrInvalid'] if row['target'] not in disabled_targets]
    freeze, terminal = [], []
    durable_freeze, durable_settlement, monotonic_settlement, unmeasured = [], [], [], []
    missing = []
    with sqlite3.connect(dbpath.as_uri()+'?mode=ro&immutable=1', uri=True) as db:
        db.row_factory = sqlite3.Row
        for entry in coverage['entries']:
            issue = entry['target']
            if entry['coverage'] == 'EXPLICITLY_DISABLED':
                continue
            if entry['coverage'] != 'FROZEN_ON_TIME' or issue not in traces:
                missing.append({'target':issue,'coverage':entry['coverage'],'closed':issue in traces})
                continue
            row = traces[issue]
            base, _ = window.trace.draw(db, row['base'])
            drawn, _ = window.trace.draw(db, issue)
            task = window.trace.single(db, window.trace.TASK, 'target_issue', issue)
            freeze.append({'target':issue,'ms':row['frozenAt']-base['open_time']})
            terminal.append({'target':issue,'ms':task['completed_at']-drawn['open_time']})
            try:
                durable_at = durable_freeze_time(db, window.trace, issue, row, drawn)
                durable_freeze.append({'target': issue, 'ms': durable_at - base['open_time']})
            except (ValueError, KeyError, IndexError, sqlite3.Error) as exc:
                unmeasured.append({'target': issue, 'boundary': 'freeze', 'reason': str(exc)})
            try:
                returned_at, elapsed_return = durable_settlement_time(db, window.trace, issue, task)
                durable_settlement.append({'target': issue, 'ms': returned_at - drawn['open_time']})
                monotonic_settlement.append({'target': issue, 'ms': elapsed_return - drawn['open_time']})
            except (ValueError, KeyError, IndexError, sqlite3.Error) as exc:
                unmeasured.append({'target': issue, 'boundary': 'settlement', 'reason': str(exc)})
    research.require(window.trace.sha_file(dbpath) == manifest['database']['sha256'], 'SNAPSHOT_CHANGED')
    for role, path in inputs.items():
        research.require(window.trace.sha_file(Path(path)) == hashes[role], 'WINDOW_INPUT_CHANGED:' + role)
    timing = {'previousOpenToFreeze':summarize(freeze, 5000), 'targetOpenToSettlement':summarize(terminal, 1000)}
    timing['previousOpenToDurableFreezeObservation'] = summarize(durable_freeze, 5000)
    timing['targetOpenToDurableSettlementReturn'] = summarize(durable_settlement, 1000)
    timing['targetOpenToSettlementReturnViaElapsedRealtime'] = summarize(monotonic_settlement, 1000)
    counts = coverage['counts']
    ok = (counts['calendarPeriods'] >= 100 and counts['expectedEnabledDue'] >= 100 and
          coverage['outcome'] == 'PASS' and not enabled_failures and not missing and not unmeasured and
          all(not metric['violations'] and metric['observed'] == counts['expectedEnabledDue'] for metric in timing.values()))
    return {'scope':scope,'outcome':'PASS' if ok else 'INCOMPLETE','counts':counts,
            'timing':timing, 'unmeasuredDurableReturns':unmeasured, 'missingOrInvalid':missing,'originalTraceFailures':closed['missingOrInvalid'],
            'enabledTraceFailures':enabled_failures,'explicitlyDisabledTraceFailures':disabled_failures,
            'settlementTimesValidated':validated,
            'settlementTimeRule':'settled_at >= max(open_time, first_seen_at), on this same snapshot',
            'calendarSha256':coverage['calendarSha256'],'snapshotSha256':coverage['databaseSha256'],
            'sourceCommit':commit,'apkSha256':apk,'captureHashes':coverage['inputSha256'],
            'captureHashesByRole':hashes}

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    for name in ('tools','bundle','evidence','start','end'):
        parser.add_argument('--'+name, type=Path, required=True)
    for name in ('commit','apk'):
        parser.add_argument('--'+name, required=True)
    args = parser.parse_args()
    result = evaluate(**vars(args), scope='INTEGRATED')
    print(json.dumps(result, ensure_ascii=False, indent=2))
    raise SystemExit(0 if result['outcome']=='PASS' else 1)
