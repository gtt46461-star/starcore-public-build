#!/usr/bin/env python3
"""Verify B01 sealed identities and execute their Android production owners."""
import copy
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import zipfile

import run_native_field_case as native

ROOT = native.ROOT
sys.path.insert(1, str(ROOT / 'scripts'))
import package_ci_candidate as package
import r967_semantic_identity as r967

SCOPE = native.contract
BINDING = SCOPE.binding
PLAN = ROOT / SCOPE.PLAN_PATH
BATCH_CLASS = 'com.ethk3.novaonepro.Field173B01ContractTest'
ORIGINAL_CERTIFICATE = 'd8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7'
RULE_173 = 'f4ee2a3513d7e10608c99465d909ba7538aebc9d2a762685b2c490e36a110a55'


def observed(argv, **kwargs):
    result = subprocess.run(list(map(str, argv)), capture_output=True, text=True, timeout=180, **kwargs)
    SCOPE.require(result.returncode == 0, 'FIELD173_B01_COMMAND_FAILED:' + str(argv[0]))
    return result.stdout


def expect_rejection(call, code):
    try:
        call()
    except ValueError as error:
        SCOPE.require(str(error).split(':')[0] == code,
                      'FIELD173_B01_WRONG_REJECTION:' + str(error))
        return str(error)
    raise AssertionError('Expected B01 rejection ' + code)


def prior_files_preserved():
    paths = ['release/' + name + '_plan_binding.json' for name in ('r33', 'r34', 'r341', 'r342')]
    paths.extend(['construction/plans/STARCORE_R34_1_REPAIR_20261124',
                  'construction/plans/STARCORE_R34_2_REPAIR_20261125',
                  'release/strategy_su1_reviewed_inputs.json', 'release/r81_controlled_source_baseline.json',
                  'release/r81_semantic_baseline.json', r967.POLICY])
    delta = BINDING.git(ROOT, 'diff', '--name-only', SCOPE.BASELINE, 'HEAD', '--', *paths)
    SCOPE.require(not delta, 'FIELD173_B01_PRESERVATION_BYTES_CHANGED')
    return paths


def identity_review(expected, apk):
    field = SCOPE.bound_field(ROOT)
    SCOPE.require(all(expected[field_name] == field[field_name] for field_name in
                      ('sourceCommit', 'sourceTree', 'planCommit', 'planSha256', 'batch')),
                  'FIELD173_B01_SOURCE_SCOPE_MISMATCH')
    patches = BINDING.read(PLAN / 'PATCHSET.json')
    reviews = []
    for group in patches['groups']:
        if group['batch'] != 'B01':
            continue
        for item in group['files']:
            path = item['path']
            if item['before_sha256'] is not None:
                before = BINDING.git(ROOT, 'show', SCOPE.BASELINE + ':' + path)
                SCOPE.require(SCOPE.digest(before) == item['before_sha256'], 'FIELD173_B01_UNEXPECTED_BEFORE:' + path)
            after = BINDING.git(ROOT, 'show', 'HEAD:' + path)
            SCOPE.require(SCOPE.digest(after) == item['after_sha256'], 'FIELD173_B01_UNEXPECTED_AFTER:' + path)
            reviews.append({'file': path, 'before': item['before_sha256'], 'after': item['after_sha256']})
    SCOPE.require(len(reviews) == 15, 'FIELD173_B01_MISSING_REVIEWED_FILES')
    inherited = BINDING.read(PLAN / 'inheritance/R342_ISSUES_57_CURRENT_STATUS.json')['rows']
    current = {row['id'] for row in BINDING.read(PLAN / 'ISSUES.json')['issues']}
    SCOPE.require(len(inherited) == len({row['old_issue_id'] for row in inherited}) == 57 and all(
        row['current_173_assessment'] and row['related_current_issues']
        and set(row['related_current_issues']) <= current for row in inherited
    ), 'FIELD173_B01_HISTORICAL_ISSUE_DISPOSITION_INCOMPLETE')
    entry = SCOPE.committed_bytes(ROOT, field['sourceCommit'], 'CURRENT_FINAL_PLAN.md')
    external = Path(os.environ['STARCORE_FIELD173_PLAN_ROOT']).resolve()
    external_head = BINDING.git(external, 'rev-parse', 'HEAD').decode().strip()
    SCOPE.require(external_head == field['planCommit'], 'FIELD173_B01_EXTERNAL_PLAN_COMMIT_MISMATCH')
    official = BINDING.git(external, 'show', field['planCommit'] + ':CURRENT_FINAL_PLAN.md')
    SCOPE.require(entry == official and SCOPE.PLAN_PATH.encode() in entry, 'FIELD173_B01_CURRENT_PLAN_LINK')
    return {'sourceReview': reviews, 'preservedPaths': prior_files_preserved(), 'inheritedIssues': inherited,
            'fieldIdentity': field, 'apkSha256': BINDING.sha(apk)}


def rejected_plan(expected):
    owner = BINDING.release_owner(ROOT, 'r343')
    with tempfile.TemporaryDirectory(prefix='field173-invalid-plan-') as directory:
        altered = Path(directory) / 'plan'
        shutil.copytree(PLAN, altered)
        name = 'ACCEPTANCE_MATRIX.json'
        with (altered / name).open('ab') as stream:
            stream.write(b' ')
        return expect_rejection(lambda: owner.verify_plan(altered, expected['planSha256']),
                                'FIELD173_PLAN_MEMBER_MISMATCH')


def inspect_apks(expected, apk, baseline, tests):
    sdk = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
    candidate, cand_cert, cand_package = package.inspect(apk, sdk / 'aapt', sdk / 'apksigner')
    previous, old_cert, old_package = package.inspect(baseline, sdk / 'aapt', sdk / 'apksigner')
    SCOPE.require(candidate['package'] == previous['package'] == native.PACKAGE
                  and candidate['sha256'] == expected['apkSha256']
                  and previous['sha256'] == SCOPE.BASELINE_APK
                  and previous['versionCode'] == 577311173 < candidate['versionCode']
                  and candidate['certificateSha256'] == previous['certificateSha256'] == ORIGINAL_CERTIFICATE,
                  'FIELD173_B01_SIGNED_APK_PAIR_MISMATCH')
    tests_signed = observed([sdk / 'apksigner', 'verify', '--verbose', '--print-certs', tests])
    SCOPE.require(ORIGINAL_CERTIFICATE in tests_signed, 'FIELD173_B01_WRONG_INSTRUMENTATION_SIGNER')
    return {'baseline': previous, 'baselineSignerNative': old_cert, 'baselinePackageNative': old_package,
            'candidate': candidate, 'candidateSignerNative': cand_cert, 'candidatePackageNative': cand_package,
            'testApkSignerNative': tests_signed}


def signed_cover(serial, apk, baseline, tests, nonce):
    commands = []

    def command(*args):
        argv = native.adb(serial, *args)
        stdout = observed(argv)
        commands.append({'argv': argv, 'stdout': stdout})
        return stdout

    def status(phase):
        selector = '#recordBeforeSignedCover' if phase == 'before' else '#readAfterSignedCover'
        original = command('shell', 'am', 'instrument', '-w', '-r', '-e', 'class', BATCH_CLASS + selector,
                           '-e', 'r30Sandbox', 'R343_COVER_' + nonce, '-e', 'r31qRealService', 'true',
                           '-e', 'field173CoverProbe', nonce, native.RUNNER)
        records = native.read_status(original, 'field173CoverProbe')
        SCOPE.require(re.search(r'OK \(1 test\)', original)
                      and not re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed', original)
                      and len(records) == 1 and records[0]['phase'] == phase,
                      'FIELD173_B01_COVER_PROBE_FAILED:' + phase)
        return records[0]

    SCOPE.require('Success' in command('uninstall', native.PACKAGE), 'FIELD173_COVER_REMOVE_FIXTURE_FAILED')
    SCOPE.require('Success' in command('install', baseline), 'FIELD173_COVER_BASELINE_FAILED')
    SCOPE.require(native.installed_apk_sha(serial) == SCOPE.BASELINE_APK, 'FIELD173_COVER_BASELINE_PATH_SHA')
    SCOPE.require('Success' in command('install', '-r', '-t', tests), 'FIELD173_COVER_TEST_APK_INSTALL_FAILED')
    before = status('before')
    command('shell', 'am', 'force-stop', native.PACKAGE)
    SCOPE.require('Success' in command('install', '-r', apk), 'FIELD173_COVER_CANDIDATE_FAILED')
    SCOPE.require(native.installed_apk_sha(serial) == BINDING.sha(apk), 'FIELD173_COVER_CANDIDATE_PATH_SHA')
    after = status('after')
    keys = ('executionId', 'uid', 'packageName', 'dbUserVersion', 'databaseIdentity', 'original', 'userEnabled')
    SCOPE.require(all(before[key] == after[key] for key in keys) and before['executionId'] == nonce
                  and before['versionCode'] == 577311173 < after['versionCode']
                  and before['packageName'] == native.PACKAGE and before['original'] == nonce
                  and before['userEnabled'] is False,
                  'FIELD173_B01_COVER_PROTECTED_READBACK_MISMATCH')
    return {'before': before, 'after': after, 'commands': commands}


def binding_review(expected, apk, serial, scenario, originals):
    manifest = BINDING.read(ROOT / 'release/release_manifest.json')
    scope = SCOPE.bound_field(ROOT)
    baseline = Path(os.environ['RUNNER_TEMP']) / 'baseline173.apk'
    test_apk, = (ROOT / 'app/build/outputs/apk/androidTest/debug').glob('*.apk')
    sdk = Path(os.environ['ANDROID_HOME']) / 'build-tools/36.0.0'
    evidence = inspect_apks(expected, apk, baseline, test_apk)
    identity = BINDING.identity(ROOT, Path(os.environ['STARCORE_R33_PLAN_ROOT']),
                                ROOT / 'release/r33_plan_binding.json', True)
    package.embedded(apk, identity)
    SCOPE.require(identity['fieldRepair173'] == scope and scope['versionCode'] == manifest['versionCode'],
                  'FIELD173_B01_EMBEDDED_IDENTITY_MISMATCH')
    evidence['identity'] = scope
    evidence['preservedPaths'] = prior_files_preserved()
    semantic = r967.build(ROOT)
    SCOPE.require(semantic['training_rule_sha256'] == RULE_173, 'FIELD173_B01_TRAINING_RULE_DRIFTED')
    evidence['originalSemanticIdentity'] = semantic
    evidence['originalSelfTest'] = observed([sys.executable, '-B', ROOT/'scripts/r967_semantic_identity.py', '--self-test'])
    evidence['priorAndroidOriginals'] = {
        name: BINDING.sha(originals / name)
        for name in ('android-argv.json', 'android-gradle.log', 'android-markers.json', 'b01-host-contracts.json')
    }
    if scenario == 'NEG':
        bad_identity = copy.deepcopy(identity)
        bad_identity['source_commit'] = SCOPE.BASELINE
        evidence['wrongApkSource'] = expect_rejection(lambda: package.embedded(apk, bad_identity),
                                                     'EMBEDDED_R31_SOURCE_IDENTITY_MISMATCH')
        with tempfile.TemporaryDirectory(prefix='field173-b01-unsigned-') as scratch:
            copy_path = Path(scratch) / 'untrusted.apk'
            with zipfile.ZipFile(apk) as original, zipfile.ZipFile(copy_path, 'w') as candidate:
                for entry in original.infolist():
                    if not entry.filename.startswith('META-INF/'):
                        candidate.writestr(entry, original.read(entry))
            evidence['unsignedRejected'] = expect_rejection(
                lambda: package.inspect(copy_path, sdk / 'aapt', sdk / 'apksigner'), 'PUBLIC_BUILD_TOOL_FAILED')
        evidence['tamperedPlan'] = rejected_plan(expected)
        older = BINDING.git(ROOT, 'rev-parse', SCOPE.BASELINE + '^').decode().strip()
        owner = BINDING.release_owner(ROOT, 'r343')
        evidence['unrelatedSourceRejected'] = {
            'sourceCommit': older,
            'error': expect_rejection(lambda: owner.identity(ROOT, older, expected['sourceTree'], manifest), 'GIT_FAILED')
        }
    evidence['coverInstallReadback'] = signed_cover(serial, apk, baseline, test_apk, expected['executionId'])
    return evidence


def observation_review():
    report = BINDING.read(PLAN / 'evidence/input_integrity.json')
    SCOPE.require(report['outer_sha256'] == BINDING.sha(PLAN / 'report/ORIGINAL_DIAGNOSTIC_173.zip')
                  and report['members_verified'] == 67
                  and report['source_identity']['sourceCommit'] == SCOPE.BASELINE,
                  'FIELD173_B01_REPORT_SOURCE_SCOPE')
    stdout = observed([sys.executable, '-B', PLAN / 'tools/verify_responsibility.py', '--plan', PLAN])
    responsibility = json.loads(stdout)
    SCOPE.require(responsibility['metrics'] == 44 and responsibility['report_checks'] == 96,
                  'FIELD173_B01_REPORT_RESPONSIBILITY_INCOMPLETE')
    checks = BINDING.read(PLAN / 'coverage/CHECK_ACTIONS_96.json')['checks']
    metrics = BINDING.read(PLAN / 'coverage/METRIC_ACTIONS_44.json')['metrics']
    return {'reportSource': report['source_identity']['verifiedInstallationR341'],
            'reportSha256': report['outer_sha256'], 'responsibility': responsibility,
            'checks': {row['id']: {'owners': row['implementation_owner_batches'],
                                  'currentStatus': row['new_software_acceptance']} for row in checks},
            'metrics': {row['id']: {'owner': row['implementation_owner_batch'],
                                   'currentStatus': row['new_status'],
                                   'originalReason': row['original_metric'].get('reason'),
                                   'originalScope': row['scope_evidence_preserved']} for row in metrics}}


def verify_host(expected, apk, serial, originals):
    case = expected['caseId']
    issue, scenario = case.rsplit('-', 1)
    if issue == 'R343-001':
        evidence = identity_review(expected, apk)
        if scenario == 'NEG':
            evidence['tamperedPlan'] = rejected_plan(expected)
    elif issue == 'R343-002':
        evidence = binding_review(expected, apk, serial, scenario, originals)
    elif issue == 'R343-003':
        evidence = observation_review()
    else:
        raise ValueError('Unsupported B01 case ' + case)
    stem = 'field173-' + case
    return [native.write_original(originals / (stem + '-host-readback.json'),
                                  SCOPE.canonical(evidence))]


if __name__ == '__main__':
    if len(sys.argv) != 2 or sys.argv[1] not in (
        'identityPositive', 'identityNegative', 'identityPreservation',
        'bindingPositive', 'bindingNegative', 'bindingPreservation',
        'observationsPositive', 'observationsNegative', 'observationsPreservation',
    ):
        raise SystemExit('Supply one exact B01 method')
    native.run(BATCH_CLASS + '#' + sys.argv[1], host_verify=verify_host)
