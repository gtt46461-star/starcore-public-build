"""Additional read-only R33.5 checks. The released plan is an input, not an output."""
import argparse
import importlib.util
import json
from pathlib import Path
import sqlite3
import sys


def require(ok, reason):
    if not ok:
        raise ValueError(reason)


def load_module(name, path):
    spec = importlib.util.spec_from_file_location(name, path)
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def open_frozen(path):
    path = Path(path).resolve(strict=True)
    for suffix in ('-wal', '-shm', '-journal'):
        require(not Path(str(path) + suffix).exists(), 'SNAPSHOT_HAS_SIDECAR')
    return sqlite3.connect(path.as_uri() + '?mode=ro&immutable=1', uri=True)


def paired(db, child, parent, through):
    """Audit the exact published comparison window and count unmatched arms."""
    require(child and parent and child != parent, 'DISTINCT_LINEAGE_MEMBERS_REQUIRED')
    born = db.execute(
        'SELECT g.born_sequence FROM su2_lineage l JOIN su2_model_genome g '
        'ON g.genome_key=l.child_key WHERE l.child_key=? AND l.parent_key=?',
        (child, parent)).fetchall()
    require(len(born) == 1 and type(born[0][0]) is int and 0 <= born[0][0] < through,
            'LINEAGE_BIRTH_OR_END')
    start = max(born[0][0], through - 5000)
    arms = {child: {}, parent: {}}
    sql = ('SELECT genome_key,sequence_ordinal,act,hit,frozen_at,target_open_time,settled_at '
           'FROM su2_live_decision WHERE genome_key IN (?,?) AND sequence_ordinal>=? '
           'AND sequence_ordinal<? ORDER BY sequence_ordinal,genome_key LIMIT 10001')
    for genome, seq, act, hit, frozen, opening, settled in db.execute(sql, (child, parent, start, through)):
        require(type(seq) is int and seq not in arms[genome], 'DUPLICATE_OR_BAD_SEQUENCE')
        require(act in (0, 1) and all(type(v) is int for v in (frozen, opening))
                and 0 < frozen < opening, 'INVALID_PREDRAW')
        if hit is None:
            require(settled in (None, 0), 'SETTLEMENT_MISSING_HIT')
        else:
            require(hit in (0, 1) and type(settled) is int and settled >= opening, 'INVALID_SETTLEMENT')
        arms[genome][seq] = (opening, hit, act)
    joined = child_wins = parent_wins = child_acts = parent_acts = 0
    missing_freeze = pending = 0
    for seq in range(start, through):
        a, b = arms[child].get(seq), arms[parent].get(seq)
        if a is None or b is None:
            missing_freeze += 1
        elif a[0] != b[0]:
            raise ValueError('PAIRED_OPEN_TIME_MISMATCH')
        elif a[1] is None or b[1] is None:
            pending += 1
        else:
            joined += 1
            child_acts += a[2]
            parent_acts += b[2]
            child_wins += a[1] * a[2]
            parent_wins += b[1] * b[2]
    return dict(scope='DESCRIPTIVE_LINEAGE_WINDOW', start=start, throughExclusive=through,
                ordinals=through-start, missingOneOrBothFreezes=missing_freeze, pending=pending,
                joined=joined, childActs=child_acts, parentActs=parent_acts,
                childActHits=child_wins, parentActHits=parent_wins,
                meanOpportunityHitDifference=(child_wins-parent_wins)/joined if joined else None,
                naturalQualification='NOT_EVALUATED')


def followup(plan, chain_path):
    oracle = load_module('r335_paired_oracle', plan/'B07/verify/check_improvement.py')
    root = chain_path.parent
    chain = json.loads(chain_path.read_bytes())
    result = oracle.compare_followup(root, chain)
    work = oracle.read_ref(root, chain['work'])
    next_case = oracle.read_ref(root, chain['nextCase'])
    fresh = oracle.read_ref(root, next_case['assessment'])
    if 'experiment' in fresh:
        raw = oracle.read_ref(root, fresh['experiment'])
        contract = oracle.read_ref(root, fresh['contract'])
        registered, applied, observed = contract['registeredAt'], work['appliedAt'], next_case['observedAt']
        require(all(type(v) is int and v > 0 for v in (registered, applied, observed)), 'INVALID_CLOCK')
        for row in raw['rows']:
            base, challenger, draw = (row[k] for k in ('baseAt', 'challengerAt', 'drawAt'))
            require(all(type(v) is int for v in (base, challenger, draw)), 'INVALID_CLOCK')
            require(max(registered, applied) < min(base, challenger)
                    and max(base, challenger) < draw <= observed, 'FOLLOWUP_WINDOW')
    return result


def trace(plan, bundle, root, commit, apk):
    checker = load_module('r335_main_trace', plan/'verification/mainline/check_trace.py')
    result = checker.verify(bundle, root, 'INTEGRATED', commit, apk)
    require(result['outcome'] == 'PASS', 'BASE_TRACE_INCOMPLETE')
    root = root.resolve()
    manifest = checker.get_json(bundle)
    db_path = checker.member(root, manifest['database'])
    with open_frozen(db_path) as db:
        for row in result['outcomes']:
            target = row['target']
            times = db.execute(
                'SELECT l.settled_at,d.open_time,d.first_seen_at FROM v57_oos_label_r35 l '
                'JOIN draw_dependency_source_cp12 d ON d.issue=l.target_issue WHERE d.issue=?',
                (target,)).fetchall()
            require(len(times) == 1, 'TARGET_OR_DRAW_JOIN')
            settled, opened, first_seen = times[0]
            require(all(type(v) is int and v > 0 for v in times[0])
                    and settled >= max(opened, first_seen), 'LABEL_BEFORE_OPEN_OR_OBSERVATION')
    require(checker.sha_file(db_path) == manifest['database']['sha256'], 'SNAPSHOT_CHANGED')
    return dict(scope='INTEGRATED_OOS_SETTLEMENT_TIME', checked=result['joined'],
                naturalQualification='NOT_EVALUATED')


def main():
    p = argparse.ArgumentParser(description=__doc__)
    sub = p.add_subparsers(dest='command', required=True)
    a = sub.add_parser('paired')
    a.add_argument('snapshot', type=Path)
    a.add_argument('child')
    a.add_argument('parent')
    a.add_argument('through', type=int)
    a = sub.add_parser('followup')
    a.add_argument('plan', type=Path)
    a.add_argument('chain', type=Path)
    a = sub.add_parser('trace')
    a.add_argument('plan', type=Path)
    a.add_argument('bundle', type=Path)
    a.add_argument('evidence_root', type=Path)
    a.add_argument('commit')
    a.add_argument('apk')
    args = p.parse_args()
    if args.command == 'paired':
        with open_frozen(args.snapshot) as db:
            result = paired(db, args.child, args.parent, args.through)
    elif args.command == 'followup':
        result = followup(args.plan, args.chain)
    else:
        result = trace(args.plan, args.bundle, args.evidence_root, args.commit, args.apk)
    print(json.dumps(result, ensure_ascii=False))


if __name__ == '__main__':
    try:
        main()
    except (KeyError, OSError, TypeError, ValueError, sqlite3.Error) as exc:
        sys.exit(f'R335_RESEARCH_CHECK_FAILED: {exc}')
