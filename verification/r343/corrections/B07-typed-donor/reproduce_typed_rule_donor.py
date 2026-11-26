#!/usr/bin/env python3
"""Run the exact Java donor SQL against the production R27 SQLite definitions."""
import argparse
import pathlib
import re
import sqlite3


def java_literal(expression: str) -> str:
    expression = expression.replace('SplitR27.SELECT', '"H_SELECT"')
    return ''.join(re.findall(r'"([^"\n]*)"', expression))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('checkout', type=pathlib.Path)
    args = parser.parse_args()
    source = args.checkout / 'app/src/main/java/com/ethk3/novaonepro/strategyv2'
    schema = (source / 'SchemaR27.java').read_text()
    owners = (source / 'AtomPublicationStoreR31.java').read_text()
    query = java_literal(re.search(r'RULE_DONOR_SQL = (.*?);', owners, re.S)[1])
    guard_query = query + ' AND p.policy_hash=?'
    connection = sqlite3.connect(':memory:')
    for table in ('r27_policy', 'r27_replay_score'):
        connection.execute(re.search(r'"(CREATE TABLE IF NOT EXISTS ' + table + r'\([^"]+)"', schema)[1])
    donor_data = [
        ('a', 'dev', 'H_SELECT', 30, 12, 6, 0.19, 0.05),
        ('b', 'dev', 'H_SELECT', 31, 12, 7, 0.23, 0.08),
        ('train-only', 'dev', 'H_TRAIN', 90, 60, 59, 0.60, 0.50),
        ('holdout-only', 'dev', 'H_HOLDOUT', 90, 60, 59, 0.60, 0.50),
        ('other-dataset', 'other', 'H_SELECT', 90, 60, 59, 0.60, 0.50),
        ('short', 'dev', 'H_SELECT', 29, 12, 11, 0.50, 0.40),
        ('zero-action', 'dev', 'H_SELECT', 90, 0, 0, 0.0, 0.50),
        ('nonpositive', 'dev', 'H_SELECT', 90, 12, 7, 0.1, 0.0),
    ]
    for policy, dataset, split, scheduled, actions, hits, mean, lcb in donor_data:
        connection.execute('INSERT INTO r27_policy VALUES (?, ?, ?, 0, ?, ?, ?, ?, 0, 0)',
                           (policy, '{}', 'I09', 'fixture', dataset, 'ACTIVE', 'fixture only'))
        connection.execute('INSERT INTO r27_replay_score(policy_hash, dataset_hash, split, n_sched, n_act, hits, '
                           'max_losing_streak, paired_u_mean, paired_u_lcb, computed_at) '
                           'VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, 0)',
                           (policy, dataset, split, scheduled, actions, hits, mean, lcb))
    baseline_rows = {row[0] for row in connection.execute(query, ('dev',))}
    publication_guards = {policy for policy, *_ in donor_data
                          if connection.execute(guard_query, ('dev', policy)).fetchone() is not None}
    print('production_query=' + query.strip())
    print('prepare_rule_eligible=' + repr(sorted(baseline_rows)) + ' expected=[\'a\', \'b\']')
    print('publication_guard_eligible=' + repr(sorted(publication_guards)) + ' expected=[\'a\', \'b\']')
    assert baseline_rows == publication_guards == {'a', 'b'}, 'eligible H_SELECT donors were lost or wrong evidence crossed roles'


if __name__ == '__main__':
    main()
