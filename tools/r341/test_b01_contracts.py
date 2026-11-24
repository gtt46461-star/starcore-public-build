#!/usr/bin/env python3
"""Execute the B01 host contracts against the actual version, packaging and build owners."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
from unittest.mock import patch


def expect_error(reason, callback):
    try:
        callback()
    except ValueError as error:
        assert str(error) == reason, (reason, str(error))
    else:
        raise AssertionError(reason)


def test_r341_b01_01_t01(repo):
    import verify_plan_binding as binding
    from build_identity import BASELINE, PLAN_COMMIT, SEAL, identity, verify_seal
    root = Path(os.environ['STARCORE_R341_PLAN_ROOT'])
    old_root = Path(os.environ['STARCORE_R33_PLAN_ROOT'])
    old = binding.release_owner(repo, 'r34')
    legacy_seal = old.verify_seal(repo, BASELINE)
    legacy_release = json.loads(binding.git(repo, 'show', BASELINE + ':release/release_manifest.json'))
    legacy = old.identity(repo, BASELINE, binding.git(repo, 'rev-parse', BASELINE + '^{tree}').decode().strip(), legacy_release)
    assert legacy['versionCode'] == 577311122 and legacy['planSha256'] == legacy_seal['planSha256']
    result = binding.identity(repo, old_root, repo/'release/r33_plan_binding.json', True)
    assert result['r341']['planCommit'] == PLAN_COMMIT and result['r341']['planSha256'] == SEAL
    assert result['r341']['legacyPlanSeals']['r34'] == legacy_seal
    assert result['checkoutBytes']['headBytesVerified'] is True
    head, tree = result['source_commit'], result['source_tree']
    release = binding.read(repo/'release/release_manifest.json')
    values = []
    args = dict(plan_root=root, legacy_plan_seals=result['r341']['legacyPlanSeals'])
    for batch in range(1,13):
        number = f'B{batch:02}'
        version = dict(release, versionName=f'R34.1-{number}-2026-11-24-577311130', versionCode=577311130)
        value = identity(repo, head, tree, version, **args)
        assert value['batch'] == number and value == identity(repo, head, tree, version, **args)
        values.append(value['batch'])
    with tempfile.TemporaryDirectory(prefix='r341-b01-plan-fixtures-') as scratch:
        copied = Path(scratch)/'field6'
        shutil.copytree(root, copied)
        member = copied/'FIELD6.md'
        member.write_bytes(member.read_bytes()+b'\nmodified\n')
        expect_error('R341_PLAN_MEMBER_MISMATCH', lambda: verify_seal(repo, head, copied))
        expect_error('R341_PLAN_MANIFEST_REQUIRED', lambda: verify_seal(repo, head, old_root))
    expect_error('R341_BASELINE_NOT_ANCESTOR', lambda: verify_seal(repo, old.BASE, root))
    expect_error('R341_VERSION_IDENTITY', lambda: identity(repo, head, tree,
                 dict(release, versionName='R34.1-B00-2026-11-24-577311123'), **args))
    expect_error('R334_DIRTY_SOURCE_NOT_PUBLISHABLE', lambda: from_dirty(repo, binding, old_root))
    return {'scope':'BUILD_ROOTS_AND_BYTE_BOUND_IDENTITY', 'r341':result['r341'],
            'deterministic_batches':values, 'legacy_r34':legacy,
            'negative_cases':['SEALED_MEMBER_CHANGED','WRONG_ROOT','INVALID_ANCESTRY','BATCH_B00','DIRTY_RELEASE']}


def from_dirty(repo, binding, old_root):
    with tempfile.TemporaryDirectory(prefix='r341-dirty-branch-fixture-') as scratch:
        isolated = Path(scratch)
        subprocess.run(['git','-C',isolated,'init','-q'],check=True)
        objects = binding.git(repo, 'rev-parse', '--path-format=absolute', '--git-common-dir').decode().strip()
        (isolated/'.git/objects/info/alternates').write_text(str(Path(objects)/'objects')+'\n')
        subprocess.run(['git','-C',isolated,'checkout','-q','--detach',
                        binding.git(repo,'rev-parse','HEAD').decode().strip()],check=True)
        (isolated/'preserved-uncommitted-file').write_text('intentional dirty worktree fixture\n')
        binding.identity(isolated, old_root, isolated/'release/r33_plan_binding.json', True)


def test_r341_b01_01_t02(repo):
    import advance_release_version as version
    import package_ci_candidate as package
    with tempfile.TemporaryDirectory(prefix='r341-b01-version-journal-') as scratch:
        state = Path(scratch)
        copy = state/'source'
        for member in (*version.FILES, 'scripts/r926_release_manifest.py'):
            target = copy/member
            target.parent.mkdir(parents=True,exist_ok=True)
            shutil.copyfile(repo/member,target)
        subprocess.run(['git','init','-q',copy],check=True)
        journal = state/'journal'
        result = version.prepare(copy,journal,577311151,'B01','2026-11-24','R34.1',577311153)
        assert result['versionCode'] == 577311154
        original_write = version.atomic
        count = 0
        def interrupted(path,data):
            nonlocal count
            if count == 2:
                raise OSError('FIXTURE_INTERRUPTED_VERSION_GROUP_AFTER_TWO_WRITES')
            count += 1
            original_write(path,data)
        with patch.object(version,'atomic',interrupted):
            try:version.apply(copy,journal)
            except OSError as error:assert str(error) == 'FIXTURE_INTERRUPTED_VERSION_GROUP_AFTER_TWO_WRITES'
            else:raise AssertionError('Journal was not interrupted')
        assert [(copy/f).read_bytes()==(journal/'after'/f).read_bytes() for f in version.FILES] == [True,True,False,False]
        recovered = version.apply(copy,journal)
        assert all((copy/f).read_bytes()==(journal/'after'/f).read_bytes() for f in version.FILES)
    current = {'package':package.PACKAGE,'certificateSha256':package.CERT,
               'versionCode':577311123,'versionName':'R34.1-B01-2026-11-24-577311123'}
    ancestor = dict(current,sha256=package.BASE_APK,versionCode=577311033,versionName='R33')
    pinned = package.binding.read(repo/'release/r341_plan_binding.json')['sourceApkEvidenceRef']
    installed = dict(current,versionCode=577311122,sha256=pinned['sha256'],signatureVerified=True)
    manifest = dict(current,applicationId=package.PACKAGE)
    assert package.validate_pair(manifest,ancestor,current,expected_installed=pinned) == 'COVER_INSTALL_UNVERIFIED'
    assert package.validate_pair(manifest,ancestor,current,current_installed=installed,expected_installed=pinned) == 'APKS_COMPATIBLE_COVER_INSTALL_NOT_MEASURED'
    expect_error('EXACT_122_BASELINE_REQUIRED',lambda: package.validate_pair(manifest,ancestor,current,
                 current_installed=dict(ancestor,signatureVerified=True),expected_installed=pinned))
    expect_error('CURRENT_BASELINE_SIGNER_CHANGED',lambda: package.validate_pair(manifest,ancestor,current,
                 current_installed=dict(installed,certificateSha256='0'*64),expected_installed=pinned))
    return {'scope':'REAL_JOURNAL_WRITE_INTERRUPTION_AND_PACKAGER_RECORD_FIXTURES',
            'applied':recovered, 'packaging_cases':['033_ONLY','122_MATCHING','033_AS_CURRENT_REFUSED','122_SIGNER_CHANGED']}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--case',choices=('R341-B01-01-T01','R341-B01-01-T02'),required=True)
    parser.add_argument('--repo',type=Path,required=True)
    parser.add_argument('--out',type=Path,required=True)
    args=parser.parse_args()
    repo=args.repo.resolve()
    sys.path[:0]=[str(repo/'tools/r33'),str(repo/'tools/r341')]
    tests={'R341-B01-01-T01':test_r341_b01_01_t01,'R341-B01-01-T02':test_r341_b01_01_t02}
    evidence = tests[args.case](repo)
    evidence['case_id'] = args.case
    evidence['assertions_exit_code'] = 0
    if args.out.is_relative_to(repo):
        raise SystemExit('ENGINEERING_EVIDENCE_MUST_BE_OUTSIDE_SOURCE')
    args.out.parent.mkdir(parents=True,exist_ok=True)
    with args.out.open('x',encoding='utf-8') as original:
        json.dump(evidence,original,ensure_ascii=False,indent=2)
        original.write('\n')
    print(args.case,args.out)

if __name__=='__main__':
    main()
