#!/usr/bin/env python3
"""Read-only cumulative candidate patch check in a temporary copy; never edits the app."""
import argparse
import hashlib
import json
import shutil
import subprocess
import tempfile
from pathlib import Path, PurePosixPath


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None


def safe(root, rel):
    q = PurePosixPath(rel)
    if q.is_absolute() or '..' in q.parts:
        raise ValueError('Unsafe relative path: ' + rel)
    return root / rel


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan-root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--source-root', type=Path, required=True)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    plan = args.plan_root.resolve()
    source = args.source_root.resolve()
    spec = json.loads((plan / 'PATCHSET.json').read_text())
    checked = []
    with tempfile.TemporaryDirectory(prefix='starcore-candidate-check-') as directory:
        temp = Path(directory)
        for patch in spec['patches']:
            patch_file = safe(plan, patch['path'])
            assert sha(patch_file) == patch['sha256'], patch['id'] + ' patch hash'
            for entry in patch['files']:
                original = safe(source, entry['path'])
                assert sha(original) == entry['before_sha256'], entry['path'] + ' source preimage'
                target = safe(temp, entry['path'])
                target.parent.mkdir(parents=True, exist_ok=True)
                if original.exists() and not target.exists():
                    shutil.copy2(original, target)
            for mode in (['--check'], []):
                subprocess.run(['git', 'apply', *mode, str(patch_file)], cwd=temp, check=True, capture_output=True, text=True)
            for entry in patch['files']:
                assert sha(safe(temp, entry['path'])) == entry['after_sha256'], entry['path'] + ' candidate postimage'
            checked.append({'id': patch['id'], 'files': len(patch['files']), 'cumulative_apply': 'PASS', 'postimage_hash': 'PASS'})
    result = {'scope': 'CUMULATIVE_PATCH_APPLICATION_IN_TEMPORARY_COPY_ONLY', 'base_commit': spec['base_source_commit'],
              'patches': checked, 'passed': True, 'application_source_changed': False,
              'android_build': 'NOT_RUN', 'device': 'NOT_RUN', 'natural_quality': 'NOT_PROVEN'}
    output = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
    if args.output:
        args.output.write_text(output, encoding='utf-8')
    else:
        print(output, end='')


if __name__ == '__main__':
    main()
