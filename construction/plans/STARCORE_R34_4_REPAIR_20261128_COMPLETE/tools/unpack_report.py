#!/usr/bin/env python3
"""Verify and unpack the immutable diagnostic attachment to a NEW directory."""
import argparse
import hashlib
import io
import json
import zipfile
from pathlib import Path, PurePosixPath


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan-root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    plan = args.plan_root.resolve()
    destination = args.output.resolve()
    if destination.exists():
        raise SystemExit('Output directory already exists; use a new directory to preserve prior evidence.')
    integrity = json.loads((plan / 'evidence/REPORT_INTEGRITY.json').read_text())
    raw = (plan / 'evidence/ORIGINAL_DIAGNOSTIC_211.zip').read_bytes()
    assert hashlib.sha256(raw).hexdigest() == integrity['outer_sha256']
    with zipfile.ZipFile(io.BytesIO(raw)) as outer:
        assert outer.testzip() is None
        members = [n for n in outer.namelist() if PurePosixPath(n).name == 'STARCORE_FULL_REPORT.zip']
        assert len(members) == 1
        inner_bytes = outer.read(members[0])
    assert hashlib.sha256(inner_bytes).hexdigest() == integrity['inner_sha256']
    with zipfile.ZipFile(io.BytesIO(inner_bytes)) as inner:
        assert inner.testzip() is None
        for member in inner.infolist():
            path = PurePosixPath(member.filename)
            assert not path.is_absolute() and '..' not in path.parts
            assert (member.external_attr >> 16) & 0o170000 != 0o120000, 'symlink rejected'
        for entry in integrity['member_checks']:
            content = inner.read(entry['name'])
            assert len(content) == entry['bytes']
            assert hashlib.sha256(content).hexdigest() == entry['sha256']
        destination.mkdir(parents=True)
        inner.extractall(destination)
    print(json.dumps({'verified': True, 'payloads_verified': len(integrity['member_checks']),
                      'output': str(destination), 'raw_database_included': False,
                      'installed_apk_included': False, 'model_training_originals_included': False}, ensure_ascii=False))


if __name__ == '__main__':
    main()
