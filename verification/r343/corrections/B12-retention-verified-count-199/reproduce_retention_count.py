"""Use the production SQLite DDL and rawQuery's text bindings to replay the seal check."""
import json
import re
import sqlite3
import sys
from pathlib import Path


def read_sql(path):
    source = path.read_text()
    ddl = source.split('private static final String[] DDL={', 1)[1].split('    };', 1)[0]
    query = source.split('private static boolean verifiedRun(', 1)[1].split('static void sealRuntimeBase(', 1)[0]
    literals = r'"(?:[^"\\]|\\.)*"'
    return ([json.loads(piece) for piece in re.findall(literals, ddl)],
            ''.join(json.loads(piece) for piece in re.findall(literals, query)))


def run(ddl, query, receipt_count=58, round_number=4, receipt_phase='VERIFY',
        receipt_plan=None, phase='DONE', numeric_count=False, db_match=True):
    plan = 'a' * 64
    run_id = 'b' * 64
    root = 'c' * 64
    with sqlite3.connect(':memory:') as db:
        db.execute('PRAGMA foreign_keys=ON')
        for statement in ddl:
            db.execute(statement)
        db.execute('''INSERT INTO r34_schema_unit_run
                      VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)''',
                   (run_id, 'fixture-database', 'R34_RETENTION', plan, 1,
                    'R34_RETENTION_DEFINITIONS_1', 58, phase,
                    58 if phase == 'DONE' else 57, 116, 'fixture-owner', 1, 4, 22, '', 1))
        for index in range(receipt_count):
            db.execute('''INSERT INTO r34_schema_unit_receipt VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)''',
                       (f'{index + 1:064x}', run_id, plan if receipt_plan is None else receipt_plan,
                        receipt_phase, index, 'source_' + str(index), round_number, index, index + 1,
                        'fixture-owner', 1, root, root, root, 22, 22, 1))
        values = (plan, 'fixture-database' if db_match else 'other-database',
                  'R34_RETENTION_DEFINITIONS_1', '58', '4', '58', 58 if numeric_count else '58')
        return db.execute(query, values).fetchone()[0]


before, after = (Path(argument) for argument in sys.argv[1:3])
ddl_before, query_before = read_sql(before)
ddl_after, query_after = read_sql(after)
assert len(ddl_before) == 5 and ddl_before == ddl_after
assert query_before[:-1] + 'CAST(? AS INTEGER)' == query_after
checks = {
    'former_query_rejects_58_correct_receipts_with_text_bind': run(ddl_before, query_before) == 0,
    'former_query_accepts_integer_bind_for_the_exact_same_receipts': run(ddl_before, query_before, numeric_count=True) == 1,
    'new_query_accepts_exact_58_verified_receipts': run(ddl_after, query_after) == 1,
    'new_query_rejects_57_verified_receipts': run(ddl_after, query_after, receipt_count=57) == 0,
    'new_query_rejects_59_verified_receipts': run(ddl_after, query_after, receipt_count=59) == 0,
    'new_query_rejects_58_receipts_from_a_prior_audit': run(ddl_after, query_after, round_number=3) == 0,
    'new_query_rejects_58_install_receipts': run(ddl_after, query_after, receipt_phase='INSTALL') == 0,
    'new_query_rejects_58_receipts_from_a_different_plan': run(ddl_after, query_after, receipt_plan='d' * 64) == 0,
    'new_query_rejects_a_run_before_done': run(ddl_after, query_after, phase='VERIFY') == 0,
    'new_query_rejects_the_wrong_database': run(ddl_after, query_after, db_match=False) == 0,
}
assert all(checks.values()), checks
print(json.dumps({'scope': 'HOST_SQLITE_EXACT_PRODUCTION_QUERY_AND_SCHEMA',
                  'sqliteVersion': sqlite3.sqlite_version, 'checkCount': len(checks),
                  'checks': checks}, indent=2, sort_keys=True))
