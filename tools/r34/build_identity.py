"""Bind the R34 APK to its sealed external plan and the installed-report source baseline."""
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess

BASE = '273641e838e0b8dd842b42a67a674e184942a30a'
PLAN_COMMIT = '4e1606e9c20c5f01a82cf495b8d39b2630c766f0'
SEAL = '6b8156c404fdcf13f9699ca888b1c44626d91c7c439130ec8de6137718e9ff05'


def verify_seal(repo, head, plan_root=None):
    binding = json.loads((repo / 'release/r34_plan_binding.json').read_bytes())
    if (binding.get('schema') != 'STARCORE_R34_SOURCE_PLAN_BINDING_1' or binding.get('planSha256') != SEAL
            or binding.get('sourceBaseline') != BASE or binding.get('planCommit') != PLAN_COMMIT):
        raise ValueError('R34_SOURCE_PLAN_BINDING')
    root = plan_root or os.environ.get('STARCORE_R34_PLAN_ROOT')
    if not root:
        raise ValueError('R34_SEALED_PLAN_REQUIRED')
    plan = Path(root).resolve(strict=True)
    seal = (plan / 'MANIFEST_SHA256.json').read_bytes()
    if hashlib.sha256(seal).hexdigest() != SEAL:
        raise ValueError('R34_PLAN_SEAL_MISMATCH')
    manifest = json.loads(seal)
    expected = {row['path']: row for row in manifest['files']}
    actual = {p.relative_to(plan).as_posix() for p in plan.rglob('*') if p.is_file()
              and '__pycache__' not in p.parts and p.suffix != '.pyc'}
    if manifest['source_baseline'] != BASE or actual != set(expected) | {'MANIFEST_SHA256.json'}:
        raise ValueError('R34_PLAN_FILE_SET')
    for name, row in expected.items():
        file = plan / name
        if (file.is_symlink() or file.stat().st_size != row['bytes']
                or hashlib.sha256(file.read_bytes()).hexdigest() != row['sha256']):
            raise ValueError('R34_PLAN_MEMBER_CHANGED:' + name)
    subprocess.run(['git','--no-replace-objects','-C',str(repo),'merge-base','--is-ancestor',BASE,head],check=True)
    return {'sourceBaseline': BASE, 'planCommit': PLAN_COMMIT,
            'planRevision': 'R34-20261123', 'planSha256': SEAL}


def identity(repo, head, tree, version, plan_root=None):
    seal = verify_seal(repo, head, plan_root)
    order = re.fullmatch(r'R34-(B(?:0[0-9]|1[0-2]))-\d{4}-\d\d-\d\d-(\d+)', version['versionName'])
    if not order or int(order[2]) != version['versionCode']:
        raise ValueError('R34_VERSION_IDENTITY')
    return {'schema':'STARCORE_R34_BUILD_IDENTITY_1','sourceCommit':head,'sourceTree':tree,
            **seal,'batch':order[1],'versionCode':version['versionCode'],
            'versionName':version['versionName'],'schemaRevision':version['schema']['userVersion']}
