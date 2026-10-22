#!/usr/bin/env python3
"""Replay unchanged retention Owner assertions on the complete current schema.

The original test creates synthetic archive receipts. This proves production
publication/CAS/rollback behavior, not Service verification of archive bytes.
"""
import argparse
import hashlib
import json
from pathlib import Path
import shutil
import sqlite3
import subprocess
import time

p = argparse.ArgumentParser()
for flag in ['out', 'schema', 'host-classpath']:
    p.add_argument('--' + flag, type=Path, required=True)
p.add_argument('--javac', default='javac')
a = p.parse_args()
root = Path(__file__).resolve().parents[2]
out = a.out.resolve()
out.mkdir(parents=True, exist_ok=False)
fixture = out / 'current-schema-fixture.db'
shutil.copyfile(a.schema, fixture)
with sqlite3.connect(fixture) as db:
    for table in ['draws', 'diagnostic_log', 'research_evidence_archive_v2',
                  'research_retention_permit_v2', 'causal_research_feedback_r947']:
        assert db.execute('SELECT count(*) FROM "' + table + '"').fetchone()[0] == 0, table
    assert db.execute("SELECT count(*) FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'").fetchone()[0] == 405
    assert not db.execute('PRAGMA foreign_key_check').fetchall()

adapter = root / 'verification/canonical/schema/SQLiteDatabase.java'
production = root / 'app/src/main/java/com/ethk3/novaonepro/RetentionRevisionSchemaCP12.java'
test = root / 'verification/canonical/tests/audit_cp12/retention/tests/RetentionOwnerContractCP12.java'
classes = out / 'classes'
classes.mkdir()
cp = a.host_classpath.read_text().strip()
commands = [
    ('compile', [a.javac, '-encoding', 'UTF-8', '-cp', cp, '-d', str(classes),
                 str(adapter), str(production), str(test)]),
    ('original_owner', ['java', '-Xmx256m', '-cp', str(classes) + ':' + cp,
                        'com.ethk3.novaonepro.RetentionOwnerContractCP12',
                        str(root / 'verification/canonical/schema/sqlite_bridge.py'),
                        str(fixture), str(out / 'original_assertions_receipt.json')]),
]
records = []
for name, command in commands:
    start = time.monotonic()
    result = subprocess.run(command, capture_output=True, text=True, timeout=180)
    log = out / (name + '.log')
    log.write_text(result.stdout + result.stderr)
    records.append({'name': name, 'command': command, 'exit_code': result.returncode,
                    'seconds': time.monotonic() - start,
                    'log_sha256': hashlib.sha256(log.read_bytes()).hexdigest()})
    print(name, result.returncode, result.stdout.strip(), result.stderr[-1200:], flush=True)
    if result.returncode:
        break
inputs = [adapter, production, test, a.schema.resolve()]
inputs += [root / ('app/src/main/java/com/ethk3/novaonepro/' + name + '.java')
           for name in ['DataRetentionGovernorR900', 'RetentionArchiveProtocolCP12',
                        'RetentionTableRegistryCP12']]
(out / 'CURRENT_EXECUTION.json').write_text(json.dumps({
    'executions': records,
    'inputs': [{'path': str(f.relative_to(root)) if f.is_relative_to(root) else str(f),
                'bytes': f.stat().st_size, 'sha256': hashlib.sha256(f.read_bytes()).hexdigest()}
               for f in inputs],
    'scope': 'Unchanged original Owner test on real host SQLite and current Android product classes; full 405-table schema. Synthetic receipt fixtures prove generation CAS, rollback, idempotence, tail/pin/expiry/retry behavior. Archive Service/native IO and device acceptance are outside this replay.',
}, indent=2) + '\n')
raise SystemExit(records[-1]['exit_code'])
