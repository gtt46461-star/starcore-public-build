"""Verify the committed R34.1 plan and the separate external build input."""
from datetime import date
import hashlib
from importlib.util import module_from_spec, spec_from_file_location
import json
import os
from pathlib import Path
import re
import subprocess

BASELINE = '5562f9b19aa09f9f7d9e69ca085db56c537967fc'
PLAN_PATH = Path('construction/plans/STARCORE_R34_1_REPAIR_20261124')
PLAN_COMMIT = '2b0177b77fa5f6079da27c915201b95b8bdcef20'
SEAL = '30facecd7919242b976606ce74fd4fb961bc6969e4b5e7a7b4d2d9415cf64540'
BASELINE_APK = '5b887677a727e3b8d07362c02bc3cd497ae26de052944e8b399b5cefc9d96cc6'
BASELINE_CODE = 577311122
TRUSTED_DESCRIPTOR = {
    'schema': 'STARCORE_R341_PLAN_DESCRIPTOR_1',
    'planRevision': 'R34.1',
    'document_revision': 6,
    'application_baseline_commit': BASELINE,
    'application_baseline_version_code': BASELINE_CODE,
    'application_baseline_branch': 'construction/r34-20261123',
    'repository': 'gtt46461-star/starcore-public-build',
    'prior_plan_revision': 'R34',
    'prior_plan_commit': '4e1606e9c20c5f01a82cf495b8d39b2630c766f0',
}


def require(condition, code):
    if not condition:
        raise ValueError(code)


def verify_seal(repo, head, plan_root=None):
    binding = json.loads((repo/'release/r341_plan_binding.json').read_bytes())
    require(binding.get('schema') == 'STARCORE_R341_SOURCE_PLAN_BINDING_1'
            and binding.get('planCommit') == PLAN_COMMIT and binding.get('planSha256') == SEAL
            and binding.get('sourceBaseline') == BASELINE
            and binding.get('sourceApkEvidenceRef', {}).get('sha256') == BASELINE_APK
            and binding['sourceApkEvidenceRef'].get('versionCode') == BASELINE_CODE,
            'R341_SOURCE_PLAN_BINDING')

    external = plan_root or os.environ.get('STARCORE_R341_PLAN_ROOT')
    require(external, 'R341_SEALED_PLAN_REQUIRED')
    worktree_plan = repo/PLAN_PATH
    root = Path(external).resolve()
    verifier_path = worktree_plan/'tools/verify_plan.py'
    for plan in dict.fromkeys((worktree_plan.resolve(), root)):
        manifest_path = plan/'MANIFEST_SHA256.json'
        require(manifest_path.is_file() and not manifest_path.is_symlink(), 'R341_PLAN_MANIFEST_REQUIRED')
        require(hashlib.sha256(manifest_path.read_bytes()).hexdigest() == SEAL, 'R341_PLAN_SEAL_MISMATCH')
        manifest = json.loads(manifest_path.read_bytes())
        require(manifest.get('schema') == 'STARCORE_R341_PLAN_MANIFEST_1', 'R341_PLAN_MANIFEST_SCHEMA')
        leaf = next(row for row in manifest['files'] if row['path'] == 'tools/verify_plan.py')
        require(verifier_path.is_file() and not verifier_path.is_symlink()
                and hashlib.sha256(verifier_path.read_bytes()).hexdigest() == leaf['sha256'],
                'R341_PLAN_VERIFIER_MISMATCH')
    spec = spec_from_file_location('sealed_r341_plan_verifier', verifier_path)
    verifier = module_from_spec(spec)
    spec.loader.exec_module(verifier)
    try:
        for plan in dict.fromkeys((worktree_plan.resolve(), root)):
            sealed = verifier.verify_seal(plan)
            require(sealed.get('schema') == 'STARCORE_R341_PLAN_SEAL_1'
                    and sealed.get('manifest_sha256') == SEAL, 'R341_PLAN_SEAL_INVALID')
            descriptor = json.loads((plan/'control/PLAN_IDENTITY.json').read_bytes())
            require(all(descriptor.get(key) == value for key, value in TRUSTED_DESCRIPTOR.items()),
                    'R341_PLAN_DESCRIPTOR_INVALID')
    except (OSError, ValueError, KeyError) as error:
        raise ValueError('R341_PLAN_MEMBER_MISMATCH') from error
    ancestry = subprocess.run(
        ['git', '--no-replace-objects', '-C', str(repo), 'merge-base', '--is-ancestor', BASELINE, head],
        stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=25)
    require(ancestry.returncode == 0, 'R341_BASELINE_NOT_ANCESTOR')
    return {'sourceBaseline': BASELINE, 'planCommit': PLAN_COMMIT, 'planSha256': SEAL,
            'planRevision': 'R34.1', 'planDocumentRevision': TRUSTED_DESCRIPTOR['document_revision']}


def identity(repo, head, tree, version, plan_root=None, *, legacy_plan_seals):
    seal = verify_seal(repo, head, plan_root)
    order = re.fullmatch(r'R34\.1-(B(?:0[1-9]|1[0-2]))-(\d{4}-\d\d-\d\d)-(\d+)', version['versionName'])
    require(order and type(version['versionCode']) is int and BASELINE_CODE < version['versionCode']
            and int(order[3]) == version['versionCode'], 'R341_VERSION_IDENTITY')
    try:
        date.fromisoformat(order[2])
    except ValueError as error:
        raise ValueError('R341_VERSION_IDENTITY') from error
    return {'schema': 'STARCORE_R341_BUILD_IDENTITY_1', 'sourceCommit': head, 'sourceTree': tree,
            **seal, 'batch': order[1], 'versionCode': version['versionCode'], 'versionName': version['versionName'],
            'schemaRevision': version['schema']['userVersion'], 'legacyPlanSeals': legacy_plan_seals}
