#!/usr/bin/env python3
"""Execute all compiled retention predicates; preserve runtime schema gaps."""
import argparse
import csv
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import time

p = argparse.ArgumentParser()
for flag in ['out', 'schema', 'host-classpath', 'numbered-plan']:
    p.add_argument('--' + flag, type=Path, required=True)
p.add_argument('--javac', default='javac')
a = p.parse_args()
root = Path(__file__).resolve().parents[2]
out = a.out.resolve()
out.mkdir(parents=True, exist_ok=False)
cp = a.host_classpath.read_text().strip()
classes = out / 'classes'
classes.mkdir()
files = [root / path for path in [
    'verification/canonical/schema/SQLiteDatabase.java',
    'app/src/main/java/com/ethk3/novaonepro/RetentionTableRegistryCP12.java',
    'verification/numbered/RegistryCoverageCurrent.java',
]]
commands = [('compile', [a.javac, '-encoding', 'UTF-8', '-cp', cp,
                          '-d', str(classes)] + [str(f) for f in files])]
for mode in ['canonical-schema', 'runtime-diagnosis']:
    fixture = out / (mode + '.db')
    shutil.copyfile(a.schema, fixture)
    commands.append((mode, ['java', '-Xmx256m', '-cp', str(classes) + ':' + cp,
        'com.ethk3.novaonepro.RegistryCoverageCurrent',
        str(root / 'verification/canonical/schema/sqlite_bridge.py'),
        str(fixture), str(out / (mode + '_receipt.json')), mode]))
records = []
for name, command in commands:
    start = time.monotonic()
    result = subprocess.run(command, capture_output=True, text=True, timeout=180)
    log = out / (name + '.log')
    log.write_text(result.stdout + result.stderr)
    records.append({'name': name, 'command': command, 'exit_code': result.returncode,
                    'seconds': time.monotonic() - start,
                    'log_sha256': hashlib.sha256(log.read_bytes()).hexdigest()})
    print(name, result.returncode, result.stdout.strip(), result.stderr[-1500:], flush=True)
    if name == 'compile' and result.returncode:
        break
plan_csv = a.numbered_plan.resolve() / 'ORIGINAL_PLAN/35_REPLAY/workspace/audit_cp12/retention/CP12_EXACT_RETENTION_REGISTRY.csv'
csv.field_size_limit(256 * 1024 * 1024)
rows = list(csv.DictReader(plan_csv.open(encoding='utf-8-sig')))
comparison = {'original_rows_fully_read': len(rows), 'all_compiled_fields_exact_match': False}
canonical = out / 'canonical-schema_receipt.json'
if canonical.exists():
    compiled = json.loads(canonical.read_text())['all_actual_compiled_rules']
    assert len(rows) == 405
    assert len(compiled) == 411
    assert {r['table'] for r in compiled[405:]} == {
        'adaptive_judge_log', 'diagnosis_case_log', 'diagnosis_fix_plan',
        'dynamic_freeze_matrix', 'module_effect_cache', 'shadow_promotion_cache'}
    for original, actual in zip(rows, compiled[:405]):
        for key in ['table', 'policy', 'issue_column', 'time_column', 'eligible_sql']:
            assert original[key] == actual[key], (original['table'], key)
    comparison['all_compiled_fields_exact_match'] = True
(out / 'CURRENT_EXECUTION.json').write_text(json.dumps({
    'executions': records,
    'source_binding': [{'path': str(f.relative_to(root)),
                       'sha256': hashlib.sha256(f.read_bytes()).hexdigest()} for f in files],
    'original_csv': {'path': str(plan_csv), 'bytes': plan_csv.stat().st_size,
                     'sha256': hashlib.sha256(plan_csv.read_bytes()).hexdigest(), **comparison},
    'scope': 'Each actual compiled production Rule/args executes its exact predicate in real host SQLite. All original 405 rows and fields retained. Runtime mode invokes current DbStore initializer; extra tables remain a failed coverage gate, never excused by the canonical fixture passing. No customer row data changed or exported.',
}, indent=2) + '\n')
raise SystemExit(0 if records and all(r['exit_code'] == 0 for r in records) else 1)
