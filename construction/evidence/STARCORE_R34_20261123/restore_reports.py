#!/usr/bin/env python3
"""Verify or restore the three exact user uploads; never rewrite ZIP contents."""
import argparse
import hashlib
import json
import os
from pathlib import Path


def digest(data):
    return hashlib.sha256(data).hexdigest()


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--root', type=Path, default=Path(__file__).resolve().parent)
    args = parser.parse_args()
    root = args.root.resolve()
    manifest = json.loads((root / 'TRANSPORT_MANIFEST.json').read_text())
    verified = []
    for item in manifest['reports']:
        dest = (root / item['path']).resolve()
        if not dest.is_relative_to(root):
            raise ValueError('unsafe destination')
        if dest.exists():
            data = dest.read_bytes()
        else:
            chunks = []
            for part in item['parts']:
                source = (root / part['path']).resolve()
                if not source.is_relative_to(root):
                    raise ValueError('unsafe part path')
                content = source.read_bytes()
                if len(content) != part['bytes'] or digest(content) != part['sha256']:
                    raise ValueError('invalid part: ' + part['path'])
                chunks.append(content)
            data = b''.join(chunks)
        if len(data) != item['bytes'] or digest(data) != item['sha256']:
            raise ValueError('invalid original: ' + item['path'])
        if not dest.exists():
            dest.parent.mkdir(parents=True, exist_ok=True)
            temp = dest.with_suffix(dest.suffix + '.restoring')
            with temp.open('xb') as stream:
                stream.write(data)
                stream.flush()
                os.fsync(stream.fileno())
            temp.replace(dest)
        verified.append({'path': item['path'], 'bytes': len(data), 'sha256': digest(data)})
    result = {'schema': 'R34_ORIGINAL_READBACK_V1', 'status': 'VERIFIED', 'count': len(verified), 'reports': verified}
    (root / 'ORIGINALS_VERIFIED.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps(result, indent=2))


if __name__ == '__main__':
    main()
