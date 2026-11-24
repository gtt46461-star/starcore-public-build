"""Reproduce the field comparison from the two original single-volume reports."""
import argparse
from collections import Counter
import hashlib
from io import BytesIO
import json
from pathlib import Path
from zipfile import ZipFile


def require(ok, message):
    if not ok:
        raise ValueError(message)


def members(path, names):
    with ZipFile(path) as wrapper:
        indices = [n for n in wrapper.namelist() if n.endswith('/index.json')]
        require(len(indices) == 1, 'Expected one outer index')
        index = json.loads(wrapper.read(indices[0]))
        part = index['files'][0]
        nested = wrapper.read(str(Path(indices[0]).parent / part['name']))
        require(len(index['files']) == 1 and len(nested) == part['bytes'] and
                hashlib.sha256(nested).hexdigest() == part['sha256'], 'Volume identity mismatch')
    with ZipFile(BytesIO(nested)) as archive:
        return index, {n: json.loads(archive.read(n)) for n in names}


def index_rows(rows, key):
    result = {}
    for row in rows:
        k = row[key]
        require(k not in result, 'Duplicate ' + key + ': ' + str(k))
        result[k] = row
    return result


def compare(previous, current):
    scope = 'DIAGNOSTIC_SCOPE.json'
    report = 'R30_REPORT.json'
    old_index, old = members(previous, [report])
    new_index, new = members(current, [report, scope])
    a, b = old[report]['snapshotWatermark'], new[report]['snapshotWatermark']
    link = new[report]['previousReportComparison']
    require(a['reportJobId'] == old_index['jobId'] == link['previousJobId'] and
            b['reportJobId'] == new_index['jobId'] == link['currentJobId'] and
            a['sourceCommit'] == b['sourceCommit'] and
            a['sourceManifest']['apkSha256'] == b['sourceManifest']['apkSha256'] and
            link['status'] == 'COMPARABLE_TWO_SUCCESSFUL_SNAPSHOTS', 'Report pair mismatch')
    lo, hi = a['latestIssue'], b['latestIssue']
    require(lo[:8] == hi[:8] and lo < hi and len(lo) == len(hi) == 12, 'Requires ordered same-day issue window')
    keys = {lo[:8] + f'{n:04d}' for n in range(1 + int(lo[8:]), 1 + int(hi[8:]))}
    tables = index_rows(new[scope]['recentEvidence'], 'table')
    def scoped(table, key):
        part = tables[table]
        require(part['complete'] is True and len(part['rows']) == part['exportedRows'] == part['totalRows'],
                'Incomplete table: ' + table)
        return index_rows([r for r in part['rows'] if r[key] in keys and
                           r.get('game_code', 'Binance1-PK10') == 'Binance1-PK10'], key)
    draws = scoped('draws', 'issue')
    tasks = scoped('v57_settlement_task_r36', 'target_issue')
    canonical = scoped('v57_shadow_observation_r35', 'target_issue')
    projections = scoped('v57_issue_projection_r926', 'target_issue')
    require(set(draws) == keys, 'Draw calendar is incomplete or keys conflict')
    recent = new[report]['chapters'][11]['fieldFollowupR34']['sources']
    s0 = next(x for x in recent if x['issueId'] == 'R34-B04-01')
    require(s0['state'] == 'READBACK', 'Missing S0 readback')
    s0 = index_rows(s0['rows'], 'target_issue')
    results = []
    for target in sorted(keys):
        draw, task = draws[target], tasks.get(target, {})
        row = canonical.get(target, {})
        projection = projections.get(target, {})
        status = 'MISSING_R35'
        hit = None
        if row:
            require(row['runtime_version'] == 'R34-B12-2026-11-24-577311122', 'Scope is for installed version 122')
            require(row['decision_action'] == row['lane'] == 'SHADOW', 'Observed a different role')
            if row['pre_draw_frozen'] == '1' and 0 < int(row['commit_at']) < int(draw['open_time']) and row['settled'] == '1':
                actual = int(draw['n' + row['position']])
                hit = int(actual in (int(row['n1']), int(row['n2']), int(row['n3'])))
                require(actual == int(row['actual_number']) and hit == int(row['hit_label']), 'Wrong settlement: '+target)
                status = 'R35_ROW_FROZEN_AND_SETTLED'
            else:
                status = 'R35_PRESENT_OTHER_STATE'
        results.append(dict(target=target, r35=status, task=task.get('status'), terminal=task.get('terminal_code'),
                            r35_hit=hit, r926_state=projection.get('current_state'),
                            r926_canonical_seen=projection.get('canonical_seen'),
                            s0_in_recent_readback=s0.get(target, {}).get('state')))
    return dict(previous_job=a['reportJobId'], current_job=b['reportJobId'], source_commit=b['sourceCommit'],
                after_exclusive=lo, through_inclusive=hi, draw_targets=len(keys),
                r35=dict(Counter(x['r35'] for x in results)), settlement_tasks=dict(Counter(x['task'] for x in results)),
                missing_r35_terminal=dict(Counter(x['terminal'] for x in results if x['r35']=='MISSING_R35')),
                r35_hits=sum(x['r35_hit'] or 0 for x in results), observations=results)


def main():
    cli = argparse.ArgumentParser(description=__doc__)
    cli.add_argument('--previous', type=Path, required=True)
    cli.add_argument('--current', type=Path, required=True)
    cli.add_argument('--rows', action='store_true', help='Include full target-key ledger')
    args = cli.parse_args()
    result = compare(args.previous, args.current)
    if not args.rows:
        result.pop('observations')
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__':
    main()
