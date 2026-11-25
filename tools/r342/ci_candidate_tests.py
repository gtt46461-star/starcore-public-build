#!/usr/bin/env python3
"""Capture candidate-bound evidence from a disposable Android CI device."""
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/r33'))
from verify_plan_binding import read, require, sha, write_json
from build_identity import BASELINE_CODE, BASELINE_APK, PLAN_COMMIT, SEAL

PACKAGE = 'com.starcore.research.r13'


def adb(serial, *argv):
    return subprocess.run(['adb', '-s', serial, *map(str, argv)], check=True,
                          capture_output=True, text=True, timeout=240).stdout.strip()


def installed(serial, destination):
    out = adb(serial, 'shell', 'dumpsys', 'package', PACKAGE)
    destination.write_text(out + '\n')
    package = re.search(r'\bversionCode=(\d+)\b', out)
    uid = re.search(r'\b(?:userId|appId)=(\d+)\b', out)
    require(package and uid, 'CANDIDATE_INSTALLED_IDENTITY_MISSING')
    path = adb(serial, 'shell', 'pm', 'path', PACKAGE)
    require(re.fullmatch(r'package:/[^\n]+', path), 'EXACT_APK_PATH_REQUIRED')
    digest = adb(serial, 'shell', 'sha256sum', path.removeprefix('package:')).split()[0]
    return {'versionCode': int(package[1]), 'uid': int(uid[1]), 'apkSha256': digest}


def cover_install(serial, baseline, apk, release, originals):
    require(sha(baseline) == BASELINE_APK, 'COVER_EXACT_BASELINE_MISSING')
    require('Success' in adb(serial, 'install', baseline), 'COVER_BASELINE_INSTALL_FAILED')
    before = installed(serial, originals / 'baseline-installed.txt')
    require(before['versionCode'] == BASELINE_CODE and before['apkSha256'] == BASELINE_APK,
            'COVER_BASELINE_IDENTITY_MISMATCH')
    require('Success' in adb(serial, 'install', '-r', apk), 'COVER_UPGRADE_INSTALL_FAILED')
    after = installed(serial, originals / 'candidate-installed.txt')
    require(after == {'uid': before['uid'], 'versionCode': release['versionCode'], 'apkSha256': sha(apk)},
            'COVER_UPGRADE_IDENTITY_MISMATCH')
    write_json(originals / 'cover-install.json', {'serial': serial, 'before': before, 'after': after,
               'scope': 'DISPOSABLE_ANDROID_API34_ORIGINAL_SIGNATURE', 'status': 'PASS'})
    require('Success' in adb(serial, 'uninstall', PACKAGE), 'DISPOSABLE_BASELINE_UNINSTALL_FAILED')


def run_inherited_b01(serial, plan, source, originals):
    import xml.etree.ElementTree as ET
    selector = 'com.ethk3.novaonepro.r341.R341B01ContractTest'
    scope = 'R342_B01_INHERITED_' + source[:12]
    cases = {x['id'] for x in read(plan / 'ACCEPTANCE_MATRIX.json')['cases']
             if x['id'].startswith('R341-B01-') and x['id'] not in ('R341-B01-01-T01', 'R341-B01-01-T02')}
    require(len(cases) == 8, 'B01_INHERITED_CASE_SELECTION')
    argv = ['./gradlew', '--no-daemon', '--max-workers=4', ':app:connectedDebugAndroidTest',
            '-Pr30Instrumentation=true', '-Pandroid.testInstrumentationRunnerArguments.class=' + selector,
            '-Pandroid.testInstrumentationRunnerArguments.r30Sandbox=' + scope]
    write_json(originals / 'android-argv.json', argv)
    results = ROOT / 'app/build/outputs/androidTest-results/connected'
    if results.is_dir():
        shutil.rmtree(results)
    with (originals / 'android-gradle.log').open('x') as log:
        returncode = subprocess.run(argv, cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, timeout=1200).returncode
    recorded = {}
    for line in adb(serial, 'logcat', '-d', '-v', 'brief', 'STARCORE_R341:I', '*:S').splitlines():
        _, marker, raw = line.partition('R341_INSTRUMENTATION ')
        if not marker:
            continue
        row = json.loads(raw)
        if row.get('r30Sandbox') == scope:
            require(row['source_commit'] == source, 'ANDROID_RESULT_HAS_WRONG_SOURCE')
            recorded.setdefault(row['case_id'], []).append(row['event'])
    write_json(originals / 'android-markers.json', recorded)
    checked = []
    for index, xml in enumerate(sorted(results.rglob('*.xml'))):
        dest = originals / f'android-native-{index}.xml'
        shutil.copyfile(xml, dest)
        for case in ET.parse(dest).iter('testcase'):
            require(case.get('classname') == selector and len(case) == 0, 'ANDROID_CASE_FAILED_OR_SKIPPED')
            checked.append(case.get('name'))
    methods = {f"test_r341_b01_{case.split('-')[-2]}_t{case[-2:]}" for case in cases}
    require(returncode == 0 and len(checked) == 8 and set(checked) == methods and set(recorded) == cases
            and all(events == ['CASE_ENTER', 'CASE_ASSERTIONS_COMPLETE'] for events in recorded.values()),
            'ANDROID_INHERITED_B01_CONTRACT_FAILED')
    return sorted(cases)


def main():
    stage = os.environ['STAGE']
    source = os.environ['SOURCE']
    temp = Path(os.environ['RUNNER_TEMP'])
    originals = temp / 'engineering'
    release = read(ROOT / 'release/release_manifest.json')
    apk, = (ROOT / 'app/build/outputs/apk/release').glob('*.apk')
    build = read(temp / 'build/CI_BUILD_RECEIPT.json')
    require(build['sourceCommit'] == source and build['r342PlanCommit'] == PLAN_COMMIT
            and build['r342PlanManifestSha256'] == SEAL and build['r342Batch'] == stage
            and build['apkSha256'] == sha(apk), 'ANDROID_CANDIDATE_BUILD_IDENTITY_MISMATCH')
    serial = subprocess.run(['adb', 'get-serialno'], capture_output=True, text=True, check=True).stdout.strip()
    require(re.fullmatch(r'emulator-[0-9]+', serial), 'DISPOSABLE_ANDROID_CI_REQUIRED')
    require(adb(serial, 'shell', 'getprop', 'ro.build.version.sdk') == '34', 'ANDROID_API_LEVEL_MISMATCH')
    cover_install(serial, temp / 'baseline152.apk', apk, release, originals)

    receipts = {'B01': lambda: run_inherited_b01(serial, Path(os.environ['STARCORE_R341_PLAN_ROOT']), source, originals)}
    require(stage in receipts, 'R342_STAGE_ANDROID_TESTS_NOT_IMPLEMENTED')
    ran = receipts[stage]()
    host = read(originals / 'b01-host-contracts.json')
    require(host['status'] == 'PASS' and host['sourceCommit'] == source
            and set(host['cases']) == {'R342-001', 'R342-002'}, 'ANDROID_BATCH_HOST_RECEIPT_MISMATCH')
    write_json(originals / 'batch-receipt.json', {
        'schema': 'STARCORE_R342_BATCH_ENGINEERING_1', 'status': 'ENGINEERING_CHECKS_PASSED',
        'buildRun': os.environ['GITHUB_RUN_ID'], 'sourceCommit': source, 'batch': stage,
        'versionCode': release['versionCode'], 'apkSha256': sha(apk), 'planSha256': SEAL,
        'androidInheritedCases': ran, 'hostIdentityCases': sorted(host['cases']),
        'vivoV2353A': 'NOT_RUN_DISPOSABLE_EMULATOR', 'physical24h': 'NOT_RUN',
        'physical72h': 'NOT_RUN', 'natural': 'NOT_RUN',
        'originals': {leaf.name: sha(leaf) for leaf in sorted(originals.iterdir()) if leaf.is_file()},
    })


if __name__ == '__main__':
    main()
