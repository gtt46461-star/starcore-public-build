#!/usr/bin/env python3
"""Read-only cumulative candidate patch check in a temporary copy; never edits the app."""
import argparse
import hashlib
import json
import os
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

sys.dont_write_bytecode = True
from plan_tool_safety import fresh_output, require, safe_relative, write_fresh_text


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None


def safe(root, rel):
    return safe_relative(root, rel)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--plan-root', type=Path, default=Path(__file__).resolve().parents[1])
    parser.add_argument('--source-root', type=Path, required=True)
    parser.add_argument('--output', type=Path)
    args = parser.parse_args()
    plan = args.plan_root.resolve()
    source = args.source_root.resolve()
    require(source.is_dir(), 'source-root is not a directory')
    output_path = fresh_output(args.output, [plan, source]) if args.output else None
    spec = json.loads((plan / 'PATCHSET.json').read_text())
    require(spec.get('schema') == 'STARCORE_R344_CANDIDATES_1', 'wrong candidate schema')
    require(spec.get('base_source_commit') == '5c7f1b36eaee6036df280e85835bb0fb4ce0b40d',
            'wrong candidate source baseline')
    require(len(spec['patches']) == 5 and {p['id'] for p in spec['patches']} ==
            {'C01', 'C02', 'C03', 'C04', 'C05'}, 'all five unique candidates are required')
    checked = []
    source_before = {}
    git_environment = {key: value for key, value in os.environ.items() if not key.startswith('GIT_')}
    git_environment['GIT_CONFIG_NOSYSTEM'] = '1'
    git_environment['GIT_CONFIG_GLOBAL'] = os.devnull
    with tempfile.TemporaryDirectory(prefix='starcore-candidate-check-') as directory:
        temp = Path(directory)
        for patch in spec['patches']:
            patch_file = safe(plan, patch['path'])
            require(sha(patch_file) == patch['sha256'], patch['id'] + ' patch hash mismatch')
            for entry in patch['files']:
                original = safe(source, entry['path'])
                require(sha(original) == entry['before_sha256'], entry['path'] + ' source preimage mismatch')
                source_before[entry['path']] = sha(original)
                target = safe(temp, entry['path'])
                target.parent.mkdir(parents=True, exist_ok=True)
                if original.exists() and not target.exists():
                    shutil.copy2(original, target)
            for mode in (['--check'], []):
                subprocess.run(['git', 'apply', *mode, str(patch_file)], cwd=temp,
                               env=git_environment, check=True, capture_output=True, text=True)
            for entry in patch['files']:
                require(sha(safe(temp, entry['path'])) == entry['after_sha256'],
                        entry['path'] + ' candidate postimage mismatch')
            checked.append({'id': patch['id'], 'files': len(patch['files']), 'cumulative_apply': 'PASS', 'postimage_hash': 'PASS'})
        expected_files = {entry['path'] for patch in spec['patches'] for entry in patch['files']
                          if entry['after_sha256'] is not None}
        require({p.relative_to(temp).as_posix() for p in temp.rglob('*') if p.is_file()} == expected_files,
                'patches changed files outside their declared cumulative scope')
    require(all(sha(safe(source, path)) == value for path, value in source_before.items()),
            'source preimages changed while checking candidates')
    result = {'scope': 'CUMULATIVE_PATCH_APPLICATION_IN_TEMPORARY_COPY_ONLY', 'base_commit': spec['base_source_commit'],
              'patches': checked, 'passed': True, 'application_source_changed': False,
              'android_build': 'NOT_RUN', 'device': 'NOT_RUN', 'natural_quality': 'NOT_PROVEN'}
    output = json.dumps(result, ensure_ascii=False, indent=2) + '\n'
    if output_path:
        write_fresh_text(output_path, output, [plan, source])
    else:
        print(output, end='')


if __name__ == '__main__':
    try:
        main()
    except (OSError, ValueError, KeyError, TypeError, subprocess.CalledProcessError) as error:
        print('check_candidates: ' + str(error), file=sys.stderr)
        sys.exit(1)
