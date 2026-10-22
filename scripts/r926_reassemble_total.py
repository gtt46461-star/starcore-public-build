#!/usr/bin/env python3
"""Reassemble the complete delivery only after checking every tracked binary part."""
import argparse
import hashlib
import json
import os
import tempfile
import zipfile
from pathlib import Path


def main():
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser()
    parser.add_argument('--manifest', type=Path, default=root / 'delivery/r926-batch1/full-total-parts/parts_manifest.json')
    parser.add_argument('--out-dir', type=Path, default=root / 'build/r926-delivery')
    args = parser.parse_args()
    manifest = json.loads(args.manifest.read_text(encoding='utf-8'))
    filename = manifest['filename']
    if Path(filename).name != filename or not filename.endswith('.zip'):
        raise ValueError('INVALID_TOTAL_FILENAME')
    parts = manifest['parts']
    if not parts or len({part['filename'] for part in parts}) != len(parts):
        raise ValueError('EMPTY_OR_DUPLICATE_PARTS')
    args.out_dir.mkdir(parents=True, exist_ok=True)
    output = args.out_dir / filename
    fd, temporary = tempfile.mkstemp(prefix='.r926-total-', dir=args.out_dir)
    whole_hash = hashlib.sha256()
    whole_size = 0
    try:
        with os.fdopen(fd, 'wb') as stream:
            for index, part in enumerate(parts):
                name = part['filename']
                if Path(name).name != name or part['index'] != index:
                    raise ValueError('INVALID_PART_ORDER_OR_PATH')
                part_hash = hashlib.sha256()
                part_size = 0
                with (args.manifest.parent / name).open('rb') as source:
                    for chunk in iter(lambda: source.read(1024 * 1024), b''):
                        part_hash.update(chunk)
                        whole_hash.update(chunk)
                        stream.write(chunk)
                        part_size += len(chunk)
                if part_size != part['bytes'] or part_hash.hexdigest() != part['sha256']:
                    raise ValueError('PART_INTEGRITY_FAILED:' + name)
                whole_size += part_size
            stream.flush()
            os.fsync(stream.fileno())
        if whole_size != manifest['bytes'] or whole_hash.hexdigest() != manifest['sha256']:
            raise ValueError('TOTAL_INTEGRITY_FAILED')
        with zipfile.ZipFile(temporary) as archive:
            damaged = archive.testzip()
            if damaged:
                raise ValueError('ZIP_CRC_FAILED:' + damaged)
        os.replace(temporary, output)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)
    print(json.dumps({'status': 'PASS', 'file': str(output), 'bytes': whole_size,
                      'sha256': whole_hash.hexdigest(), 'parts': len(parts)}, ensure_ascii=False))


if __name__ == '__main__':
    main()
