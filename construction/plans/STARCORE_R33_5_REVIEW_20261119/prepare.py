"""Restore the audited R33.5 plan from the published phone ZIP into a new directory."""
import argparse
import hashlib
import json
import os
from pathlib import Path, PurePosixPath
import shutil
import subprocess
import sys
import tempfile
from zipfile import ZipFile

REVIEW = Path(__file__).resolve().parent
PLAN = 'STARCORE_R33_5_20261118'
HOST = '2c1493b00d50b952bf3f43318962d2a2062b1cbb3bc40b8ab88fd38e788d03ed'
SEAL = 'eefe7128ed2a13065c95c31c2f424a42d3c5c3bc780fbc18ab0db1fcd6e0a43c'


def require(condition, message):
    if not condition:
        raise ValueError(message)


def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()


def safe_name(name):
    parts = name.split('/')
    require(all(p and p not in ('.', '..') and '\\' not in p and ':' not in p
                for p in parts), 'Invalid path: ' + name)
    return PurePosixPath(name)


def extract(archive, info, target):
    target.parent.mkdir(parents=True, exist_ok=True)
    with archive.open(info) as source, target.open('wb') as dest:
        shutil.copyfileobj(source, dest, 1024 * 1024)
    return digest(target)


def check_tree(files, spec):
    h = hashlib.sha256()
    for name, sha in sorted(files.items()):
        h.update((sha + '  ' + name + '\n').encode())
    require(len(files) == spec['fileCount'] and h.hexdigest() == spec['treeSha256'],
            'Member count or content digest mismatch')


def prepare(asset, dest):
    from verify_review import verify
    verify(REVIEW)
    meta = json.loads((REVIEW / 'packaging/release-comparison.json').read_text())
    require(asset.stat().st_size == meta['source']['bytes'] and
            digest(asset) == meta['source']['sha256'], 'Unexpected release asset')
    require(not dest.resolve().is_relative_to(REVIEW), 'Choose an output directory outside the review tree')
    require(not os.path.lexists(dest), 'Output directory already exists')
    dest.parent.mkdir(parents=True, exist_ok=True)
    files = {}
    with tempfile.TemporaryDirectory(dir=dest.parent, prefix='.r335-') as tmp:
        root = Path(tmp) / 'prepared'
        root.mkdir()
        with ZipFile(asset) as outer:
            for info in outer.infolist():
                if info.is_dir():
                    continue
                volume, name = info.filename.split('/', 1)
                require(volume + '/' in meta['source']['rootPrefixes'] and name not in files,
                        'Unexpected or duplicate release path: ' + name)
                files[name] = extract(outer, info, root / safe_name(name))
        check_tree(files, meta['source'])
        payload = REVIEW / 'packaging' / meta['delta']['name']
        require(digest(payload) == meta['delta']['sha256'], 'Repair archive digest')
        with ZipFile(payload) as fixes:
            require(sorted(fixes.namelist()) == sorted(x['path'] for x in meta['changes']),
                    'Repair archive member set')
            for row in meta['changes']:
                name = row['path']
                require(files.get(name) == row['beforeSha256'], 'Unexpected preimage: ' + name)
                files[name] = extract(fixes, name, root / safe_name(name))
                require(files[name] == row['afterSha256'], 'Unexpected replacement: ' + name)
        check_tree(files, meta['target'])
        receipt = REVIEW / 'verification/host-receipts.zip'
        require(digest(receipt) == HOST, 'Host receipt archive digest')
        # The identical release ZIP and delta remain the original source for the earlier receipts.
        with ZipFile(receipt) as logs:
            for name in logs.namelist():
                require(name in ('verification/HOST_RUNS.json', 'verification/SHA256.json') or
                        name.startswith('verification/host_logs_R335/'), 'Unexpected receipt path')
                extract(logs, name, root / PLAN / safe_name(name))
        plan = root / PLAN
        require(digest(plan / 'verification/SHA256.json') == SEAL, 'Unexpected sealed plan')
        subprocess.run([sys.executable, '-B', str(plan / 'verification/verify_package.py')], check=True)
        shutil.copytree(REVIEW, root / 'current-review', ignore=shutil.ignore_patterns('__pycache__', '*.pyc'))
        (root / 'START_HERE.md').write_text(
            '# R33.5 当前施工入口\n\n[复核方案](current-review/README.md) · '
            '[Codex 施工任务](current-review/CODEX_EXECUTE.md)\n', encoding='utf-8')
        # Never publish over an existing user directory. Claim the target exclusively first.
        if sys.platform.startswith('linux'):
            import ctypes
            libc = ctypes.CDLL(None, use_errno=True)
            rename = getattr(libc, 'renameat2', None)
            if rename is not None:
                if rename(-100, os.fsencode(root), -100, os.fsencode(dest), 1) != 0:
                    error = ctypes.get_errno()
                    raise OSError(error, os.strerror(error), str(dest))
            else:
                raise OSError('Atomic no-replace rename unavailable')
        else:
            # Publish into an exclusively reserved parent; concurrent runs cannot overwrite it.
            dest.mkdir()
            try:
                for child in root.iterdir():
                    child.rename(dest / child.name)
            except BaseException:
                # Preserve partial output for inspection rather than removing third-party files.
                raise
    print('Ready:', dest / 'START_HERE.md')


if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('release_zip', type=Path)
    p.add_argument('new_output_directory', type=Path)
    a = p.parse_args()
    prepare(a.release_zip.resolve(), a.new_output_directory.absolute())
