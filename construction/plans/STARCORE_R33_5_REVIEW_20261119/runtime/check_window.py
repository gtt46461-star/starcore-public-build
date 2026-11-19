"""Replay a preregistered 100-period window against the enabled journal and SQLite originals."""
import argparse
import importlib.util
import json
import math
import sqlite3
from pathlib import Path

def summarize(rows, limit):
    values = sorted(row['ms'] for row in rows)
    return {'observed':len(values), 'limitMs':limit,
            'p95Ms':values[math.ceil(len(values)*0.95)-1] if values else None,
            'maxMs':max(values, default=None),
            'violations':[row for row in rows if row['ms'] < 0 or row['ms'] > limit]}

def evaluate(tools, bundle, evidence, start, end, commit, apk, scope):
    path = tools / 'B02/enabled_window_r335/collect_enabled_window.py'
    spec = importlib.util.spec_from_file_location('r335_window', path)
    window = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(window)
    manifest = window.trace.get_json(bundle)
    coverage = window.reconcile(bundle, evidence, start, end, scope, commit, apk)
    closed = window.trace.verify(bundle, evidence, scope, commit, apk)
    traces = {row['target']:row for row in closed['outcomes']}
    dbpath = window.trace.member(evidence.resolve(), manifest['database'])
    freeze, terminal = [], []
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
    timing = {'previousOpenToFreeze':summarize(freeze, 5000), 'targetOpenToSettlement':summarize(terminal, 1000)}
    counts = coverage['counts']
    ok = (counts['calendarPeriods'] >= 100 and counts['expectedEnabledDue'] >= 100 and
          coverage['outcome'] == 'PASS' and not missing and
          all(not metric['violations'] and metric['observed'] == counts['expectedEnabledDue'] for metric in timing.values()))
    return {'scope':scope,'outcome':'PASS' if ok else 'INCOMPLETE','counts':counts,
            'timing':timing, 'missingOrInvalid':missing,'originalTraceFailures':closed['missingOrInvalid'],
            'calendarSha256':coverage['calendarSha256'],'snapshotSha256':coverage['databaseSha256'],
            'sourceCommit':commit,'apkSha256':apk,'captureHashes':coverage['inputSha256']}

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
