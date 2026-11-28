#!/usr/bin/env python3
"""Verify and unpack the immutable diagnostic attachment to a NEW directory."""
import argparse
import hashlib
import io
import json
import sys
import zipfile
from pathlib import Path, PurePosixPath

sys.dont_write_bytecode = True
from plan_tool_safety import fresh_output, require, safe_relative
from verify_plan import VerificationError, Verifier


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan-root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    plan = args.plan_root.resolve()
    destination = fresh_output(args.output, [plan])
    verifier = Verifier(plan, None)
    verifier.check_identity()
    verifier.check_original_zip()
    integrity = json.loads((plan / 'evidence/REPORT_INTEGRITY.json').read_text())
    raw = (plan / 'evidence/ORIGINAL_DIAGNOSTIC_211.zip').read_bytes()
    require(hashlib.sha256(raw).hexdigest() == integrity['outer_sha256'], 'outer ZIP SHA mismatch')
    with zipfile.ZipFile(io.BytesIO(raw)) as outer:
        require(outer.testzip() is None, 'outer ZIP CRC mismatch')
        members = [n for n in outer.namelist() if PurePosixPath(n).name == 'STARCORE_FULL_REPORT.zip']
        require(len(members) == 1, 'missing or ambiguous inner ZIP')
        inner_bytes = outer.read(members[0])
    require(hashlib.sha256(inner_bytes).hexdigest() == integrity['inner_sha256'], 'inner ZIP SHA mismatch')
    with zipfile.ZipFile(io.BytesIO(inner_bytes)) as inner:
        require(inner.testzip() is None, 'inner ZIP CRC mismatch')
        for member in inner.infolist():
            safe_relative(destination, member.filename.rstrip('/'))
            require((member.external_attr >> 16) & 0o170000 != 0o120000, 'symlink rejected')
        for entry in integrity['member_checks']:
            content = inner.read(entry['name'])
            require(len(content) == entry['bytes'], 'payload length mismatch: ' + entry['name'])
            require(hashlib.sha256(content).hexdigest() == entry['sha256'], 'payload SHA mismatch: ' + entry['name'])
        destination.mkdir(parents=True)
        for member in inner.infolist():
            target = safe_relative(destination, member.filename.rstrip('/'))
            if member.is_dir():
                target.mkdir(parents=True, exist_ok=True)
            else:
                target.parent.mkdir(parents=True, exist_ok=True)
                with target.open('xb') as output, inner.open(member) as source:
                    while data := source.read(1024 * 1024):
                        output.write(data)
    print(json.dumps({'verified': True, 'payloads_verified': len(integrity['member_checks']),
                      'output': str(destination), 'raw_database_included': False,
                      'installed_apk_included': False, 'model_training_originals_included': False}, ensure_ascii=False))


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile, VerificationError) as error:
        print('unpack_report: ' + str(error), file=sys.stderr)
        sys.exit(1)
