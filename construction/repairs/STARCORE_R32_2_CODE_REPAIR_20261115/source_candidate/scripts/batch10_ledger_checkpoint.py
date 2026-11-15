#!/usr/bin/env python3
"""Restore original receipts and apply keyed, non-destructive construction deltas.

The repository retains the complete original checkpoint. Deltas retain only
current execution fields; immutable contract columns and original denominators
are verified and never regenerated from blank templates.
"""
import argparse
import csv
import hashlib
import io
import json
from pathlib import Path
import tarfile

ROOT = Path(__file__).resolve().parents[1]
COUNTS = {'CARD_EXECUTION_LEDGER.csv': ('card_id', 299),
          'REQUIREMENT_EXECUTION_LEDGER.csv': ('requirement_id', 1832),
          'TEST_EXECUTION_LEDGER.csv': ('test_id', 328),
          'DETECTION_EXECUTION_LEDGER.csv': ('issue_id', None),
          'SUBCONDITION_EXECUTION_LEDGER.csv': ('subcondition_id', None)}
MUTABLE = {'current_status', 'code_status', 'current_contract_review',
           'current_before_sha256', 'current_after_sha256', 'diff_commit',
           'current_test_id', 'command', 'exit_code', 'log', 'current_evidence_scope',
           'blocker', 'acceptance_evidence', 'final_closure_status'}


def main():
    csv.field_size_limit(100000000)
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--root', type=Path, default=ROOT)
    p.add_argument('--delta', type=Path, default=ROOT / 'construction/batch01/ledger_delta.json')
    a = p.parse_args()
    root = a.root.resolve()
    delta = json.loads(a.delta.read_text())
    checkpoint = root / delta['prior_checkpoint']['path']
    digest = hashlib.sha256(checkpoint.read_bytes()).hexdigest()
    if digest != delta['prior_checkpoint']['sha256']:
        raise ValueError('PRIOR_CHECKPOINT_IDENTITY_DRIFT')
    ledger = root / 'construction/numbered'
    if not all((ledger / name).is_file() for name in COUNTS):
        with tarfile.open(checkpoint, 'r:gz') as archive:
            for member in archive:
                if not member.isfile() or member.issym() or member.islnk():
                    continue
                destination = (root / member.name).resolve()
                if not destination.is_relative_to(root) or not member.name.startswith('construction/numbered/'):
                    raise ValueError('CHECKPOINT_PATH_ESCAPE')
                if destination.exists():
                    continue  # Existing current source state always wins.
                destination.parent.mkdir(parents=True, exist_ok=True)
                destination.write_bytes(archive.extractfile(member).read())
    results = {}
    for name, (key, count) in COUNTS.items():
        path = ledger / name
        with path.open(encoding='utf-8-sig', newline='') as stream:
            reader = csv.DictReader(stream)
            fields, rows = reader.fieldnames, list(reader)
        keyed = {r[key]: r for r in rows}
        if len(keyed) != len(rows) or count is not None and count != len(rows):
            raise ValueError('ORIGINAL_LEDGER_DENOMINATOR_OR_DUPLICATE:' + name)
        if name.startswith('DETECTION') and sum(not r['issue_id'].startswith('NEW-DET-') for r in rows) != 166:
            raise ValueError('ORIGINAL_DETECTION_DENOMINATOR')
        for identity, changes in delta.get('updates', {}).get(name, {}).items():
            if identity not in keyed or set(changes) - MUTABLE or set(changes) - set(fields):
                raise ValueError('UNKNOWN_ID_OR_IMMUTABLE_FIELD:' + identity)
            keyed[identity].update(changes)
        buffer = io.StringIO(newline='')
        writer = csv.DictWriter(buffer, fieldnames=fields)
        writer.writeheader()
        writer.writerows(rows)
        data = buffer.getvalue().encode('utf-8-sig')
        path.write_bytes(data)
        results[name] = {'rows': len(rows), 'sha256': hashlib.sha256(data).hexdigest()}
    receipt = {'prior_checkpoint_sha256': digest, 'delta_sha256': hashlib.sha256(a.delta.read_bytes()).hexdigest(),
               'ledgers': results, 'scope': 'LOSSLESS_EXISTING_RECEIPTS_PLUS_KEYED_CURRENT_FIELDS_NO_CLOSURE_INFERENCE'}
    (a.delta.parent / 'LEDGER_MATERIALIZATION_RECEIPT.json').write_text(json.dumps(receipt, indent=2) + '\n')
    print(json.dumps(receipt))


if __name__ == '__main__':
    main()
