"""Bind the sealed R34.2 contract and the signed 152 source baseline to a build."""
from datetime import date
import os
from pathlib import Path
import re

from verify_plan_binding import git, read, require, safe, sha

BASELINE = 'eb8f82a8bda8db22f1f122095217132f07053bcb'
BASELINE_CODE = 577311152
BASELINE_APK = '2afc220e8942f7eda678b7ddd09ca16c48225c2f4f99167a9233a5e9bf1a8c38'
PLAN_COMMIT = 'e95b72c6a6a963d7f1a1db3596bb2f05b501240f'
SEAL = '096df19913f10f84cf89b0bdca22e30337860efef31ff59662fb2a9b2c30eb45'
PLAN_PATH = Path('construction/plans/STARCORE_R34_2_REPAIR_20261125')
BINDING = Path('release/r342_plan_binding.json')
MANIFEST = 'MANIFEST_SHA256.json'
PRESERVED = (
    'release/r33_plan_binding.json', 'release/r34_plan_binding.json', 'release/r341_plan_binding.json',
    'construction/plans/STARCORE_R34_1_REPAIR_20261124/MANIFEST_SHA256.json',
)
BASELINE_EVIDENCE = {
    'releaseTag': 'r34.1-B12-eb8f82a8bda8',
    'member': 'STARCORE-R341-577311152-eb8f82a8bda8-release.apk',
    'sha256': BASELINE_APK,
    'versionCode': BASELINE_CODE,
}


def verify_plan_members(root):
    manifest_path = safe(root, MANIFEST)
    require(manifest_path.is_file(), 'R342_PLAN_MANIFEST_REQUIRED')
    require(sha(manifest_path) == SEAL, 'R342_PLAN_SEAL_MISMATCH')
    manifest = read(manifest_path)
    require(manifest['schema'] == 'R342_MANIFEST_1' and manifest['source_commit'] == BASELINE
            and manifest['plan_version'] == 'R34.2' and manifest['document_revision'] == 1,
            'R342_PLAN_DESCRIPTOR_INVALID')
    expected = {MANIFEST}
    for row in manifest['files']:
        name = row['path']
        leaf = safe(root, name)
        require(name not in expected and leaf.is_file() and leaf.stat().st_size == row['bytes']
                and sha(leaf) == row['sha256'], 'R342_PLAN_MEMBER_MISMATCH:' + name)
        expected.add(name)
    actual = {leaf.relative_to(root).as_posix() for leaf in root.rglob('*')
              if leaf.is_file() and '__pycache__' not in leaf.parts and leaf.suffix not in ('.pyc', '.pyo')}
    require(actual == expected and len(expected) == manifest['file_count'] + 1,
            'R342_PLAN_MEMBER_SET_MISMATCH')


def verify_seal(repo, head, plan_root=None):
    repo = Path(repo).resolve()
    binding_path = safe(repo, BINDING.as_posix())
    require(binding_path.is_file() and read(binding_path) == {
        'schema': 'STARCORE_R342_SOURCE_PLAN_BINDING_1',
        'series': 'R34.2', 'planCommit': PLAN_COMMIT, 'planSha256': SEAL,
        'sourceBaseline': BASELINE, 'sourceApkEvidenceRef': BASELINE_EVIDENCE,
    }, 'R342_SOURCE_PLAN_BINDING')
    try:
        git(repo, 'merge-base', '--is-ancestor', BASELINE, head)
    except ValueError as error:
        raise ValueError('R342_BASELINE_NOT_ANCESTOR') from error
    for name in PRESERVED:
        require(safe(repo, name).read_bytes() == git(repo, 'show', f'{BASELINE}:{name}'),
                'R342_INHERITED_BINDING_CHANGED:' + name)
    external = plan_root or os.environ.get('STARCORE_R342_PLAN_ROOT')
    require(external, 'R342_SEALED_PLAN_REQUIRED')
    in_tree = safe(repo, PLAN_PATH.as_posix())
    for root in dict.fromkeys((in_tree, Path(external).resolve())):
        verify_plan_members(root)
    return {'sourceBaseline': BASELINE, 'planCommit': PLAN_COMMIT, 'planSha256': SEAL,
            'planRevision': 'R34.2', 'planDocumentRevision': 1, 'baselineVersionCode': BASELINE_CODE}


def identity(repo, head, tree, version, plan_root=None, *, legacy_plan_seals):
    seal = verify_seal(repo, head, plan_root)
    order = re.fullmatch(r'R34\.2-(B(?:0[1-9]|1[0-2]))-(\d{4}-\d\d-\d\d)-(\d+)', version['versionName'])
    code = version['versionCode']
    require(order and type(code) is int and BASELINE_CODE < code <= 2100000000
            and int(order[3]) == code and version['applicationId'] == 'com.starcore.research.r13',
            'R342_VERSION_IDENTITY')
    try:
        date.fromisoformat(order[2])
    except ValueError as error:
        raise ValueError('R342_VERSION_IDENTITY') from error
    return {'schema': 'STARCORE_R342_BUILD_IDENTITY_1', 'sourceCommit': head, 'sourceTree': tree,
            **seal, 'batch': order[1], 'versionCode': code, 'versionName': version['versionName'],
            'schemaRevision': version['schema']['userVersion'], 'legacyPlanSeals': legacy_plan_seals}
