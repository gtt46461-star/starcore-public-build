"""Reopen registered opportunities with the original validated readers, including all unknowns."""
from collections import Counter
import hashlib
import importlib.util
import json
from pathlib import Path
import sqlite3
import sys
import os

ROOT = Path(os.environ['RUNNER_TEMP'])
RUNTIME = ROOT / 'material/current-review/runtime'
sys.path.insert(0, str(RUNTIME))
import check_window
from prepare_tools import verified_window_path


def load_collector():
    path = verified_window_path(ROOT / 'tools')
    spec = importlib.util.spec_from_file_location('final_reviewed_collector', path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def file_hash(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def period_observations(raw, source, apk, db=None, snapshot_sha=None):
    c = load_collector()
    trace = c.trace
    result = {'sourceCommit': source, 'apkSha256': apk, 'snapshotSha256': snapshot_sha,
              'scope': '原登记日历逐期只读。应用启用分母单独取自应用的赋值日志及 START/END。',
              'rows': [], 'thresholdsMs': {'canonicalFreeze': 5000, 'settlementReturn': 1000}}
    cal_path = raw / 'calendar.json'
    if not cal_path.exists():
        return dict(result, reason='NO_RECEIVED_PREREGISTERED_CALENDAR', calendarPeriods=None)
    cal = trace.get_json(cal_path)
    schedule = raw / 'schedule.bin'
    assert source == cal['sourceCommit'] and apk == cal['apkSha256']
    assert not trace.calendar.validate_calendar(cal)
    assert schedule.is_file() and file_hash(schedule) == cal['scheduleEvidenceSha256']
    result.update(calendarPeriods=len(cal['entries']), calendarSha256=file_hash(cal_path),
                  preregisteredAtMs=cal['registeredAtMs'], registeredWindowEndMs=cal['windowEndMs'])
    assignment = {}
    bundle = raw / 'trace.json'
    if bundle.is_file() and db is not None:
        try:
            cov = c.reconcile(bundle,raw,raw/'enable-start.json',raw/'enable-end.json','INTEGRATED',source,apk)
            result['officialEnabledCounts'] = cov['counts']
            assignment = {e['target']:e for e in cov['entries']}
        except (KeyError, IndexError, OSError, ValueError, TypeError, sqlite3.Error) as exc:
            result['assignmentEvidenceError'] = str(exc)
    table_names = {x[0] for x in db.execute("SELECT name FROM sqlite_master WHERE type='table'")} if db is not None else set()
    facts = [('targetOriginals',trace.DRAW,'issue'),('requests',trace.REQUEST,'target_issue'),
             ('canonicalRows',trace.FREEZE,'target_issue'),('labels',trace.LABEL,'target_issue'),
             ('settlementTasks',trace.TASK,'target_issue')]
    metric_rows = {name: [] for name in ['freezeRecord','settlementRecord','durableFreeze','durableSettlement','monotonicSettlement']}
    for entry in cal['entries']:
        target = entry['targetIssue']
        row = {'target':target,'deadlineAt':entry['deadlineAt'],'calendarEligible':entry['eligible'],
               'assignmentCoverage':assignment.get(target,{}).get('coverage','UNKNOWN_ASSIGNMENT_FROM_ORIGINALS')}
        result['rows'].append(row)
        if db is None:
            row.update(freeze='UNOBSERVED',reason='NO_SOURCE_BOUND_CONSISTENT_SNAPSHOT',canonicalClosure=None)
            continue
        row['tableCounts'] = {}
        for name,table,key in facts:
            row['tableCounts'][name] = db.execute(f'SELECT count(*) FROM {table} WHERE {key}=?',(target,)).fetchone()[0] if table in table_names else None
        try:
            row['freeze'],row['reason'] = c.freeze_fact(db, entry)
        except (KeyError, IndexError, ValueError, TypeError, sqlite3.Error) as exc:
            row['freeze'],row['reason'] = 'UNKNOWN_FREEZE_SOURCE',str(exc)
        if trace.REQUEST in table_names:
            row['requestState'] = [i[0] for i in db.execute('SELECT DISTINCT status FROM '+trace.REQUEST+' WHERE target_issue=?',(target,))]
        try:
            closed = trace.trace(db,entry)
        except (KeyError, IndexError, ValueError, TypeError, sqlite3.Error) as exc:
            row.update(canonicalClosure=False,traceFailure=str(exc))
            continue
        row.update(canonicalClosure=True,base=closed['base'],action=closed['action'],hit=closed['hit'],
                   labelHash=closed['labelHash'],settlementHash=closed['settlementHash'])
        prev,_ = trace.draw(db,closed['base'])
        draw,_ = trace.draw(db,target)
        task = trace.single(db,trace.TASK,'target_issue',target)
        row['wall'] = {'previousOpenMs':prev['open_time'],'previousFirstSeenMs':prev['first_seen_at'],
                       'targetOpenMs':draw['open_time'],'targetFirstSeenMs':draw['first_seen_at'],
                       'frozenAtMs':closed['frozenAt'],'taskCompletedAtMs':task['completed_at']}
        def record(metric,value):
            metric_rows[metric].append({'target':target,'ms':value})
            row.setdefault('elapsedFromOfficialOpenMs',{})[metric]=value
        record('freezeRecord',closed['frozenAt']-prev['open_time'])
        record('settlementRecord',task['completed_at']-draw['open_time'])
        try:
            t = check_window.durable_freeze_time(db,trace,target,closed,draw)
            record('durableFreeze',t-prev['open_time'])
        except (KeyError, IndexError, ValueError, TypeError, sqlite3.Error) as exc:
            row['freezeReturnUnavailable'] = str(exc)
        try:
            t,m = check_window.durable_settlement_time(db,trace,target,task)
            record('durableSettlement',t-draw['open_time'])
            record('monotonicSettlement',m-draw['open_time'])
        except (KeyError, IndexError, ValueError, TypeError, sqlite3.Error) as exc:
            row['settlementReturnUnavailable'] = str(exc)
    result['freezeStates']=dict(Counter(v['freeze'] for v in result['rows']))
    if db is not None:
        for key,rows in metric_rows.items():
            result.setdefault('timings',{})[key]=check_window.summarize(rows,5000 if key in ('freezeRecord','durableFreeze') else 1000)
        result['duplicateCanonicalOrSettlementRows']=[v['target'] for v in result['rows'] if any((v['tableCounts'][k] or 0)>1 for k in ('targetOriginals','canonicalRows','settlementTasks'))]
        closed=[v for v in result['rows'] if v['canonicalClosure']]
        bets=[v for v in closed if v['action']=='BET']
        result['strictJoined']=len(closed)
        result['joinedRealSettledActs']=len(bets)
        result['joinedRealHits']=sum(v['hit'] for v in bets)
        result['joinedRealHitRate']=result['joinedRealHits']/len(bets) if bets else None
    return result
