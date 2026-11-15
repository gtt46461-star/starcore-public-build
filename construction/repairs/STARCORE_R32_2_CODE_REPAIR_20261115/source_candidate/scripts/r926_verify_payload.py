#!/usr/bin/env python3
"""Verify all files in an extracted delivery against one UTF-8 JSON manifest."""
import argparse
import hashlib
import json
import sys
from pathlib import Path


def verify(root):
    root = root.resolve()
    manifest = json.loads((root / 'PAYLOAD_MANIFEST.json').read_text(encoding='utf-8'))
    failures = []
    seen = set()
    total = 0
    for entry in manifest['files']:
        name = entry['path']
        path = (root / name).resolve()
        if name in seen or not path.is_relative_to(root):
            failures.append({'path': name, 'error': 'DUPLICATE_OR_UNSAFE_PATH'})
            continue
        seen.add(name)
        if not path.is_file():
            failures.append({'path': name, 'error': 'MISSING'})
            continue
        digest = hashlib.sha256()
        with path.open('rb') as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b''):
                digest.update(chunk)
        size = path.stat().st_size
        total += size
        if size != entry['bytes'] or digest.hexdigest() != entry['sha256']:
            failures.append({'path': name, 'error': 'SIZE_OR_SHA256_MISMATCH'})
    exclusions = {'PAYLOAD_MANIFEST.json', 'MANIFEST_SHA256.txt', 'MANIFEST_SHA256.sha256',
                  'PACKAGE_INTEGRITY.json'}
    actual = {str(path.relative_to(root)) for path in root.rglob('*') if path.is_file()}
    for name in sorted(actual - seen - exclusions):
        failures.append({'path': name, 'error': 'UNLISTED'})
    result = {'status': 'FAIL' if failures else 'PASS', 'checked_files': len(seen),
              'checked_bytes': total, 'errors': failures}
    print(json.dumps(result, ensure_ascii=True))
    return 1 if failures else 0


if __name__ == '__main__':
    parser = argparse.ArgumentParser()
    parser.add_argument('root', type=Path, nargs='?', default=Path(__file__).resolve().parent)
    sys.exit(verify(parser.parse_args().root))
