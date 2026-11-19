#!/usr/bin/env python3
"""Restore the two original R33.5 ZIPs from repository binary parts."""
from pathlib import Path, PurePosixPath
import argparse
import hashlib
import json
import os
import shutil
import stat
import tempfile
import zipfile

def digest(path):
    h = hashlib.sha256()
    with path.open('rb') as source:
        for block in iter(lambda: source.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()

def safe_path(root, relative):
    p = PurePosixPath(relative)
    if p.is_absolute() or not p.parts or any(x in ('..', '') for x in p.parts) or '\\' in relative:
        raise ValueError('Unsafe archive path: ' + relative)
    target = root.joinpath(*p.parts)
    if not target.resolve().is_relative_to(root.resolve()):
        raise ValueError('Path leaves destination: ' + relative)
    return target

def restore(root, archive, output):
    target = output / archive['archive']
    if target.exists():
        if target.stat().st_size == archive['bytes'] and digest(target) == archive['sha256']:
            print('Already restored:', target)
            return target
        raise FileExistsError('Existing output differs; choose a new output directory: ' + str(target))
    temporary = target.with_name(target.name + '.assembling')
    total = hashlib.sha256()
    count = 0
    try:
        with temporary.open('xb') as dest:
            for part in archive['parts']:
                source_path = safe_path(root, part['name'])
                if source_path.stat().st_size != part['bytes']:
                    raise ValueError('Part size mismatch: ' + part['name'])
                one = hashlib.sha256()
                with source_path.open('rb') as source:
                    for block in iter(lambda: source.read(1024 * 1024), b''):
                        one.update(block)
                        total.update(block)
                        count += len(block)
                        dest.write(block)
                if one.hexdigest() != part['sha256']:
                    raise ValueError('Part hash mismatch: ' + part['name'])
            dest.flush()
            os.fsync(dest.fileno())
        if count != archive['bytes'] or total.hexdigest() != archive['sha256']:
            raise ValueError('Original archive identity mismatch: ' + archive['archive'])
        os.replace(temporary, target)
    except BaseException:
        if temporary.exists():
            temporary.unlink()
        raise
    print('Restored:', target, count, 'bytes', archive['sha256'])
    return target

def extract(archives, destination):
    if destination.exists():
        raise FileExistsError('Extraction destination must be new: ' + str(destination))
    destination.parent.mkdir(parents=True, exist_ok=True)
    stage = Path(tempfile.mkdtemp(prefix=destination.name + '.extracting-', dir=destination.parent))
    try:
        for archive in archives:
            with zipfile.ZipFile(archive) as package:
                for member in package.infolist():
                    target = safe_path(stage, member.filename)
                    mode = member.external_attr >> 16
                    if stat.S_ISLNK(mode):
                        raise ValueError('Symlink member requires manual review: ' + member.filename)
                    if member.is_dir():
                        target.mkdir(parents=True, exist_ok=True)
                        continue
                    target.parent.mkdir(parents=True, exist_ok=True)
                    if target.exists():
                        raise FileExistsError('Duplicate extracted path: ' + member.filename)
                    with package.open(member) as source, target.open('xb') as dest:
                        shutil.copyfileobj(source, dest, 1024 * 1024)
                    if mode & 0o111:
                        target.chmod(0o755)
        os.replace(stage, destination)
    except BaseException:
        shutil.rmtree(stage)
        raise
    print('Extracted:', destination)

def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output-dir', type=Path, default=Path('restored_archives'))
    parser.add_argument('--extract', type=Path)
    args = parser.parse_args()
    root = Path(__file__).resolve().parent
    manifest = json.loads((root / 'PACKAGE_PARTS.json').read_text(encoding='utf-8'))
    args.output_dir.mkdir(parents=True, exist_ok=True)
    archives = [restore(root, entry, args.output_dir) for entry in manifest['archives']]
    if args.extract:
        extract(archives, args.extract)
    print('Transport restoration complete. This is not plan or Android acceptance.')

if __name__ == '__main__':
    main()
