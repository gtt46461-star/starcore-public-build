#!/usr/bin/env python3
"""Exercise the R34.2 baseline, history, and recoverable release identity."""
import argparse
import json
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
from unittest.mock import patch
import zipfile

sys.path.insert(0, str(Path(__file__).resolve().parents[1] / 'r33'))
import advance_release_version as versions
import package_ci_candidate as package
import verify_plan_binding as binding
from build_identity import BASELINE, BASELINE_APK, BASELINE_CODE, MANIFEST, PLAN_COMMIT, PLAN_PATH, PRESERVED, SEAL


def expect_failure(code, call):
    try:
        call()
    except ValueError as error:
        assert str(error).split(':', 1)[0] == code, str(error)
    else:
        raise AssertionError('Expected rejection: ' + code)


def test_r342_001(repo, plan_root, sdk_path, baseline_apk, identity):
    owner = binding.release_owner(repo, 'r342')
    inherited = plan_root / 'inheritance'
    previous = repo / 'construction/plans/STARCORE_R34_1_REPAIR_20261124'
    assert not binding.git(repo, 'diff', BASELINE, 'HEAD', '--', str(previous.relative_to(repo)),
                           '.github/workflows/r31-publish-release.yml')
    cards = binding.read(inherited / 'R341_FILE_DELTA_295.json')
    tree = package.source_tree_entries(repo, BASELINE)
    assert len(cards) == 295 and len({row['path'] for row in cards}) == 294
    for card in cards:
        assert tree[card['path']][1] == card['now'], card['file_card_id']
    orders = binding.read(inherited / 'R341_WORK_ORDER_CROSSWALK_62.json')['mapping']
    cases = binding.read(inherited / 'R341_ACCEPTANCE_CROSSWALK_152.json')['cases']
    case_ids = {row['old_case_id'] for row in cases}
    assert len({row['old_issue_id'] for row in orders}) == 62
    assert len(case_ids) == 152
    assert all(row['inherited_acceptance_ids'] and set(row['inherited_acceptance_ids']) <= case_ids for row in orders)
    for preserved in PRESERVED:
        assert (repo / preserved).read_bytes() == binding.git(repo, 'show', f'{BASELINE}:{preserved}')

    metadata, _, _ = package.inspect(baseline_apk, sdk_path / 'aapt', sdk_path / 'apksigner')
    assert metadata['signatureVerified'] and metadata['certificateSha256'] == package.CERT
    assert metadata['package'] == package.PACKAGE and metadata['versionCode'] == BASELINE_CODE
    assert metadata['sha256'] == BASELINE_APK
    with zipfile.ZipFile(baseline_apk) as apk:
        embedded = json.loads(apk.read('assets/diagnostics/build-identity-r341.json'))
        assert embedded['sourceCommit'] == BASELINE
        assert embedded['sourceTree'] == binding.git(repo, 'rev-parse', BASELINE + '^{tree}').decode().strip()
        assert embedded['planSha256'] == identity['r342']['legacyPlanSeals']['r341']['planSha256']

    assert identity['r342']['planCommit'] == PLAN_COMMIT and identity['r342']['planSha256'] == SEAL
    assert identity['r342']['sourceBaseline'] == BASELINE and identity['checkoutBytes']['headBytesVerified']
    expect_failure('R342_BASELINE_NOT_ANCESTOR', lambda: owner.verify_seal(repo, binding.BASE, plan_root))
    expect_failure('R342_PLAN_SEAL_MISMATCH', lambda: owner.verify_seal(repo, identity['source_commit'], previous))
    with tempfile.TemporaryDirectory(prefix='r342-seal-test-') as scratch:
        copy = Path(scratch) / 'plan'
        shutil.copytree(plan_root, copy)
        leaf = copy / 'FILE_CARDS.json'
        leaf.write_bytes(leaf.read_bytes() + b'\n')
        expect_failure('R342_PLAN_MEMBER_MISMATCH', lambda: owner.verify_seal(repo, identity['source_commit'], copy))
        leaf.write_bytes((plan_root / 'FILE_CARDS.json').read_bytes())
        with (copy / MANIFEST).open('ab') as seal:
            seal.write(b'\n')
        expect_failure('R342_PLAN_SEAL_MISMATCH', lambda: owner.verify_seal(repo, identity['source_commit'], copy))
    return {'baseline': metadata, 'inheritedFileCards': len(cards), 'workOrders': len(orders),
            'inheritedCases': len(cases), 'verifiedCheckout': identity['checkoutBytes']}


def test_r342_002(repo, identity):
    release = binding.read(repo / versions.FILES[0])
    assert identity['r342']['versionCode'] == release['versionCode']
    environment = __import__('run_release_build').build_environment(
        Path('/tmp/r33'), inherited={}, r342_plan=repo / PLAN_PATH)
    assert environment['STARCORE_R342_PLAN_ROOT'] == str(repo / PLAN_PATH)
    assert environment['R13_EXPECTED_CERT_SHA256'] == package.CERT
    settings = {'plan_root': repo / PLAN_PATH, 'legacy_plan_seals': identity['r342']['legacyPlanSeals']}
    owner = binding.release_owner(repo, 'r342')
    expect_failure('R342_VERSION_IDENTITY', lambda: owner.identity(
        repo, identity['source_commit'], identity['source_tree'],
        {**release, 'versionName': release['versionName'].replace('-B01-', '-B00-')}, **settings))
    with tempfile.TemporaryDirectory(prefix='r342-version-test-') as scratch:
        state = Path(scratch)
        fixture = state / 'source'
        for name in (*versions.FILES, 'scripts/r926_release_manifest.py'):
            leaf = fixture / name
            leaf.parent.mkdir(parents=True, exist_ok=True)
            leaf.write_bytes(binding.git(repo, 'show', f'{BASELINE}:{name}'))
        subprocess.run(['git', '-C', fixture, 'init', '-q'], check=True)
        journal = state / 'journal'
        expect_failure('R342_PUBLISHED_VERSION_LOWER_BOUND_REQUIRED', lambda: versions.prepare(
            fixture, journal, BASELINE_CODE, 'B01', '2026-11-25', 'R34.2', BASELINE_CODE - 1))
        receipt = versions.prepare(fixture, journal, BASELINE_CODE + 40, 'B01', '2026-11-25',
                                   'R34.2', BASELINE_CODE + 45)
        assert receipt['versionCode'] == BASELINE_CODE + 46
        atomic = versions.atomic
        writes = []

        def interrupt(path, data):
            if len(writes) == 2:
                raise OSError('INJECTED_PROCESS_INTERRUPTION')
            atomic(path, data)
            writes.append(path)

        with patch.object(versions, 'atomic', interrupt):
            try:
                versions.apply(fixture, journal)
            except OSError as error:
                assert str(error) == 'INJECTED_PROCESS_INTERRUPTION'
            else:
                raise AssertionError('Version group did not stop after two writes')
        assert [(fixture / name).read_bytes() == (journal / 'after' / name).read_bytes()
                for name in versions.FILES] == [True, True, False, False]
        edited = fixture / versions.FILES[2]
        original = edited.read_bytes()
        user_bytes = original + b'\n// fixture user edit\n'
        edited.write_bytes(user_bytes)
        expect_failure('IDENTITY_DIVERGED_RETAIN_USER_EDIT', lambda: versions.apply(fixture, journal))
        assert edited.read_bytes() == user_bytes
        edited.write_bytes(original)
        applied = versions.apply(fixture, journal)
        assert versions.apply(fixture, journal) == applied
        assert all((fixture / name).read_bytes() == (journal / 'after' / name).read_bytes() for name in versions.FILES)

    baseline = {'package': package.PACKAGE, 'certificateSha256': package.CERT, 'signatureVerified': True}
    formal = dict(baseline, versionCode=577311033, versionName='baseline-033', sha256=package.BASE_APK)
    candidate = dict(baseline, versionCode=release['versionCode'], versionName=release['versionName'])
    current = dict(baseline, versionCode=BASELINE_CODE, sha256=BASELINE_APK)
    pin = binding.read(repo / 'release/r342_plan_binding.json')['sourceApkEvidenceRef']
    assert package.validate_pair(release, formal, candidate, current_installed=current, expected_installed=pin) == (
        'APKS_COMPATIBLE_COVER_INSTALL_NOT_MEASURED')
    expect_failure('EXACT_152_BASELINE_REQUIRED', lambda: package.validate_pair(
        release, formal, candidate, current_installed=formal, expected_installed=pin))
    expect_failure('CURRENT_BASELINE_SIGNER_CHANGED', lambda: package.validate_pair(
        release, formal, candidate, current_installed=dict(current, certificateSha256='0' * 64), expected_installed=pin))
    return {'versionTransaction': applied, 'buildIdentity': identity['r342']}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--repo', type=Path, required=True)
    p.add_argument('--plan', type=Path, required=True)
    p.add_argument('--sdk-tools', type=Path, required=True)
    p.add_argument('--baseline-apk', type=Path, required=True)
    p.add_argument('--legacy-plan', type=Path, required=True)
    p.add_argument('--out', type=Path, required=True)
    args = p.parse_args()
    repo = args.repo.resolve()
    assert not args.out.resolve().is_relative_to(repo)
    result = binding.identity(repo, args.legacy_plan, repo / 'release/r33_plan_binding.json', True, r342_plan=args.plan)
    checks = {
        'R342-001': test_r342_001(repo, args.plan, args.sdk_tools, args.baseline_apk, result),
        'R342-002': test_r342_002(repo, result),
    }
    binding.write_json(args.out, {'status': 'PASS', 'sourceCommit': result['source_commit'], 'cases': checks})


if __name__ == '__main__':
    main()
