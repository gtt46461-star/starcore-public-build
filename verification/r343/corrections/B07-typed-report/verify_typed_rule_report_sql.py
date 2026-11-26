#!/usr/bin/env python3
"""Host regression for exact R343-028 Java report queries on the production SQLite schema."""
import argparse
import hashlib
import json
import pathlib
import re
import sqlite3


def digest(s: str) -> str:
    return hashlib.sha256(s.encode()).hexdigest()


def java_sql(owner: str) -> dict[str, str]:
    constants = {
        'AtomPublicationStoreR31.RULES_R342': 'r342_typed_generation_rule',
        'AtomPublicationStoreR31.RULE_USES_R342': 'r342_typed_generation_rule_use',
    }
    names = list(re.finditer(r'private static final String (TYPED_RULE_[A-Z]+_R343) = (.*?);', owner, re.S))
    assert len(names) == 5, 'R343 projections are absent from this source'
    for name in names:
        tokens = re.findall(r'"([^"\n]*)"\s*(?:\+|$)|([\w.]+)\s*(?:\+|$)', name[2].strip())
        constants[name[1]] = ''.join(constants[reference] if reference else literal for literal, reference in tokens)
    return constants


def schema(db: sqlite3.Connection, baseline: pathlib.Path) -> None:
    for name, wanted in {
        'ResearchSchemaR30R1': {'r30r1_analysis_run', 'r30r1_research_measure', 'r30r1_proposal', 'r30r1_consume'},
        'AtomPublicationStoreR31': {'r342_typed_generation_rule', 'r342_typed_generation_rule_use'},
        'SchemaR27': {'r27_replay_score'},
    }.items():
        content = (baseline / (name + '.java')).read_text()
        tables = re.findall(r'"(CREATE TABLE IF NOT EXISTS (\w+)\([^"\n]*)"', content)
        assert {table for ddl, table in tables if table in wanted} == wanted
        for ddl, table in tables:
            if table in wanted:
                db.execute(ddl)


def insert_synthetic_originals(db: sqlite3.Connection) -> None:
    for rule, abi in (('current', 'current-abi'), ('prior', 'prior-abi')):
        db.execute('INSERT INTO r342_typed_generation_rule VALUES(?,?,?,?,?,?,?,?,?)',
                   (digest(rule), digest('shape'), digest('discovery'), 2, abi, 1000, 1999, '{}', 10))
    for name, source, dataset, child, from_ordinal, state, original in (
        ('exact', 'current', 'target', 'child-a', 2000, 'CONSUMED', 'MATCH'),
        ('ready', 'current', 'target', 'child-b', 2000, 'READY', 'ABSENT'),
        ('different-receipt', 'current', 'other', 'child-b', 2000, 'CONSUMED', 'MISMATCH'),
        ('prior', 'prior', 'other', 'child-a', 2000, 'CONSUMED', 'MATCH'),
        ('discovery-itself', 'current', 'discovery', 'child-a', 2000, 'READY', 'ABSENT'),
        ('overlap', 'current', 'other', 'child-a', 1999, 'READY', 'ABSENT'),
        ('malformed-json', 'current', 'other', 'child-a', 2000, 'READY', 'ABSENT'),
    ):
        run, proposal, dataset, child = map(digest, ('run:' + name, 'proposal:' + name, dataset, child))
        parent, rule_id = digest('parent'), digest('current')
        db.execute('INSERT INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',
                   (run, dataset, parent, 'SELECT', source, 4, from_ordinal, from_ordinal + 300,
                    digest('{}'), 'COMMITTED', '{}', 20))
        payload = '{' if name == 'malformed-json' else json.dumps({
            'typedRule': rule_id, 'ruleTransferDataset': dataset, 'candidateHash': child,
        })
        db.execute('INSERT INTO r30r1_proposal VALUES(?,?,?,?,?,?,?,?,?)',
                   (proposal, run, parent, 'TYPED_GENERATION_RULE', payload, digest(payload), state, 'fixture', 30))
        if original != 'ABSENT':
            use, gate = digest('use:' + name), digest('gate:' + name)
            db.execute('INSERT INTO r342_typed_generation_rule_use VALUES(?,?,?,?,?,?,?,?)',
                       (proposal, rule_id, child, dataset, 2, run, use, 100))
            db.execute('INSERT INTO r30r1_consume VALUES(?,?,?,?,?,?)',
                       (proposal, child, 2, 'ORIGINAL_BIRTH_GATE_R30_REPAIR1', gate, 100))
            db.execute('INSERT INTO r30r1_research_measure VALUES(?,?,?,?,?,?,?,?,?,?,?,?)',
                       (run, 'CONSUMED_BY:' + proposal, 'RESEARCH_SOURCE_CONSUMPTION', child,
                        'ORIGINAL_BIRTH_GATE_RECEIPT', 2000, 2300, 0, None, None, None,
                        json.dumps({'typedRuleUseReceipt': use if original == 'MATCH' else digest('bad'), 'receiptHash': gate})))


def add_score(db: sqlite3.Connection, child: str, dataset: str, split: str, at: int) -> None:
    db.execute('INSERT OR REPLACE INTO r27_replay_score(policy_hash,dataset_hash,split,n_sched,n_act,hits,'
               'max_losing_streak,paired_u_mean,paired_u_lcb,computed_at) VALUES(?,?,?,40,20,12,1,0.2,0.1,?)',
               (digest(child), digest(dataset), split, at))


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('baseline', type=pathlib.Path)
    parser.add_argument('owner', type=pathlib.Path)
    args = parser.parse_args()
    source_dir = args.baseline / 'app/src/main/java/com/ethk3/novaonepro/strategyv2'
    owner = args.owner.read_text()
    sql = java_sql(owner)
    db = sqlite3.connect(':memory:')
    schema(db, source_dir)

    def query(key: str, *args: str) -> tuple:
        return db.execute(sql['TYPED_RULE_' + key + '_R343'], args).fetchone()

    assert query('CATALOG', 'current-abi') == (0, 0)
    assert query('PROPOSALS', 'current') == (0, 0)
    assert query('RESULTS', 'current', 'H_SELECT') == (0, 0)
    insert_synthetic_originals(db)
    add_score(db, 'child-a', 'target', 'H_TRAIN', 101)
    add_score(db, 'child-a', 'target', 'H_HOLDOUT', 101)
    add_score(db, 'child-a', 'other', 'H_SELECT', 101)
    add_score(db, 'child-b', 'other', 'H_SELECT', 101)
    add_score(db, 'child-a', 'target', 'H_SELECT', 99)
    assert query('CATALOG', 'current-abi') == (2, 1)
    assert query('PROPOSALS', 'current') == (3, 2)
    assert query('RESULTS', 'current', 'H_SELECT') == (1, 0)
    add_score(db, 'child-a', 'target', 'H_SELECT', 101)
    assert query('RESULTS', 'current', 'H_SELECT') == (1, 1)
    print('exact production SQL host readback: seven assertions passed, catalog=(2,1) transfers=(3,2) born=(1,1)')


if __name__ == '__main__':
    main()
