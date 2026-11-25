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


def cover_install(serial, baseline, apk, release, originals, signed_reports=None):
    require(sha(baseline) == BASELINE_APK, 'COVER_EXACT_BASELINE_MISSING')
    require('Success' in adb(serial, 'install', baseline), 'COVER_BASELINE_INSTALL_FAILED')
    before = installed(serial, originals / 'baseline-installed.txt')
    require(before['versionCode'] == BASELINE_CODE and before['apkSha256'] == BASELINE_APK,
            'COVER_BASELINE_IDENTITY_MISMATCH')
    if signed_reports:
        signed_reports('baseline', 'COMPACT_DIAGNOSTIC', 'eb8f82a8bda8db22f1f122095217132f07053bcb')
        adb(serial, 'shell', 'am', 'force-stop', PACKAGE)
    require('Success' in adb(serial, 'install', '-r', apk), 'COVER_UPGRADE_INSTALL_FAILED')
    after = installed(serial, originals / 'candidate-installed.txt')
    require(after == {'uid': before['uid'], 'versionCode': release['versionCode'], 'apkSha256': sha(apk)},
            'COVER_UPGRADE_IDENTITY_MISMATCH')
    write_json(originals / 'cover-install.json', {'serial': serial, 'before': before, 'after': after,
               'scope': 'DISPOSABLE_ANDROID_API34_ORIGINAL_SIGNATURE', 'status': 'PASS'})
    if signed_reports:
        for name, profile in [('candidate-1','COMPACT_DIAGNOSTIC'),('candidate-2','FULL_RAW'),
                              ('candidate-3','COMPACT_DIAGNOSTIC')]:
            signed_reports(name, profile, os.environ['SOURCE'])
    require('Success' in adb(serial, 'uninstall', PACKAGE), 'DISPOSABLE_BASELINE_UNINSTALL_FAILED')


def signed_report_campaign(serial, originals, apk_sha):
    import time
    import report_ui_driver as driver
    require(driver.DEVICE == serial and adb(serial, 'shell', 'getprop', 'ro.kernel.qemu') == '1',
            'SIGNED_REPORT_TEST_REQUIRES_DISPOSABLE_EMULATOR')
    adb(serial, 'root')
    adb(serial, 'wait-for-device')
    adb(serial, 'shell', 'cmd', 'connectivity', 'airplane-mode', 'enable')
    adb(serial, 'shell', 'svc', 'wifi', 'disable')
    deadline = time.monotonic()+30
    while adb(serial, 'shell', 'ip', 'route', 'show'):
        require(time.monotonic() < deadline, 'SIGNED_REPORT_TEST_NETWORK_MUST_BE_DISABLED')
        time.sleep(1)
    require(adb(serial, 'shell', 'settings', 'get', 'global', 'airplane_mode_on') == '1',
            'SIGNED_REPORT_TEST_AIRPLANE_MODE_MISSING')
    adb(serial, 'shell', 'wm', 'size', '480x850')
    adb(serial, 'shell', 'wm', 'density', '195')
    adb(serial, 'shell', 'settings', 'put', 'system', 'screen_off_timeout', '7200000')
    adb(serial, 'shell', 'svc', 'power', 'stayon', 'true')
    records = []
    certificate = Path(__file__).with_name('report-originals-recipient.crt.txt')

    def capture(name, profile, source):
        print('Signed report campaign',name,profile,flush=True)
        adb(serial, 'shell', 'pm', 'grant', PACKAGE, 'android.permission.POST_NOTIFICATIONS')
        driver.commit = source
        driver.out = {'reportReadbacks': records, 'activeProfile': profile}
        try:
            driver.report_entry('另存完整原始备份（体积大）' if profile == 'FULL_RAW' else driver.CREATE)
            record = driver.save_report(driver.wait_for_report(name),name)
            require(record['rawEvidenceIncluded'] == (profile == 'FULL_RAW'), 'UNEXPECTED_REPORT_EVIDENCE_PROFILE')
            zip_file = driver.OUT / record['file']
            encrypted = originals / f'signed-report-{name}.zip.cms'
            subprocess.run(['openssl','cms','-encrypt','-binary','-aes-256-gcm','-outform','DER',
                            '-in',str(zip_file),'-out',str(encrypted),str(certificate)],check=True)
            records.append(dict(record,sourceCommit=source,profile=profile,
                                encryptedOriginal=encrypted.name,encryptedSha256=sha(encrypted)))
            write_json(originals/'signed-report-readbacks.json',{
                'sourceCommit':os.environ['SOURCE'],'apkSha256':apk_sha,
                'scope':'SIGNED_RELEASE_API34_OFFLINE_UI_AND_SAF','reports':records,
                'natural':False,'physicalDevice':False,
            })
        except Exception:
            progress = driver.OUT / (name+'-progress.json')
            if progress.is_file():
                # Original UI details remain encrypted with the existing report recipient.
                subprocess.run(['openssl','cms','-encrypt','-binary','-aes-256-gcm','-outform','DER',
                                '-in',str(progress),'-out',str(originals/(name+'-ui-failure.cms')),
                                str(certificate)],check=True)
            raise
    return capture


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
            require(case.get('classname') == selector and not any(child.tag in ('failure', 'skipped', 'error') for child in case),
                    'ANDROID_CASE_FAILED_OR_SKIPPED')
            checked.append(case.get('name'))
    methods = {f"test_r341_b01_{case.split('-')[-2]}_t{case[-2:]}" for case in cases}
    require(returncode == 0 and len(checked) == 8 and set(checked) == methods and set(recorded) == cases
            and all(events == ['CASE_ENTER', 'CASE_ASSERTIONS_COMPLETE'] for events in recorded.values()),
            'ANDROID_INHERITED_B01_CONTRACT_FAILED')
    return sorted(cases)


def run_android_case(serial, source, result_dir, *, case_id, selector, scope, phase=None):
    test_package = PACKAGE + '.test'
    runner = test_package + '/com.ethk3.novaonepro.R30FaultAndroidRunner'
    arguments = ['shell', 'am', 'instrument', '-w', '-r', '-e', 'class', selector, '-e', 'r30Sandbox', scope]
    if phase:
        arguments.extend(['-e', 'r342Phase', phase])
    arguments.append(runner)
    stem = case_id if phase is None else case_id + '-' + phase
    write_json(result_dir / (stem + '-argv.json'), ['adb', '-s', serial, *arguments])
    output = subprocess.run(['adb', '-s', serial, *arguments], capture_output=True, text=True, timeout=600)
    (result_dir / (stem + '-native.log')).write_text(output.stdout + '\n' + output.stderr)
    require(output.returncode == 0 and re.search(r'OK \(1 test\)', output.stdout),
            'ANDROID_CASE_FAILED:' + stem)
    require(not re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed', output.stdout),
            'ANDROID_CASE_RUNNER_FAILURE:' + stem)
    tag = 'STARCORE_R342' if phase else 'STARCORE_R341'
    prefix = 'R342_INSTRUMENTATION ' if phase else 'R341_INSTRUMENTATION '
    selected = []
    for line in adb(serial, 'logcat', '-d', '-v', 'brief', tag + ':I', '*:S').splitlines():
        _, marker, data = line.partition(prefix)
        if not marker:
            continue
        event = json.loads(data)
        if phase and (event.get('scope'), event.get('phase'), event.get('caseId')) == (scope, phase, case_id):
            require(event['sourceCommit'] == source, 'ANDROID_RESULT_HAS_WRONG_SOURCE')
            selected.append(event)
        elif not phase and (event.get('r30Sandbox'), event.get('case_id')) == (scope, case_id):
            require(event.get('source_commit', event.get('sourceCommit')) == source, 'ANDROID_RESULT_HAS_WRONG_SOURCE')
            selected.append(event)
    require([event['event'] for event in selected] == ['CASE_ENTER', 'CASE_ASSERTIONS_COMPLETE'],
            'ANDROID_CASE_ASSERTIONS_NOT_OBSERVED:' + stem)
    write_json(result_dir / (stem + '-events.json'), selected)
    return selected[-1]


def run_native_batch(stage, serial, plan, source, originals):
    debug, = (ROOT / 'app/build/outputs/apk/debug').glob('*.apk')
    tests, = (ROOT / 'app/build/outputs/apk/androidTest/debug').glob('*.apk')
    require('Success' in adb(serial, 'install', debug), 'INSTRUMENTATION_APP_INSTALL_FAILED')
    require('Success' in adb(serial, 'install', '-t', tests), 'INSTRUMENTATION_RUNNER_INSTALL_FAILED')
    current = Path(os.environ['STARCORE_R342_PLAN_ROOT'])
    selected = [case for case in read(current / 'ACCEPTANCE_MATRIX.json')['cases'] if case['batch'] == stage]
    require(len(selected) == {'B02': 4, 'B03': 5, 'B04': 3, 'B05': 5, 'B06': 4, 'B07': 7,
                              'B08': 12, 'B09': 2, 'B10': 4, 'B11': 7, 'B12': 2}[stage], 'R342_STAGE_CASE_SELECTION')
    for case in selected:
        method = 'test_' + case['issue_id'].lower().replace('-', '_')
        selector = 'com.ethk3.novaonepro.r342.R342' + stage + 'ContractTest#' + method
        scope = 'R342_' + case['issue_id'].replace('-', '_') + '_' + source[:12]
        phase = 'normal' if case['level'] == 'ENGINEERING' else 'engineering-probe'
        run_android_case(serial, source, originals, case_id=case['id'], selector=selector, scope=scope, phase=phase)
        if case['issue_id'] == 'R342-004':
            restart_scope = scope + '_RESTART'
            first = run_android_case(serial, source, originals, case_id=case['id'], selector=selector,
                                     scope=restart_scope, phase='stage-restart')
            adb(serial, 'shell', 'am', 'force-stop', PACKAGE)
            second = run_android_case(serial, source, originals, case_id=case['id'], selector=selector,
                                      scope=restart_scope, phase='restart')
            require(first['pid'] != second['pid'], 'ANDROID_WRITER_RESTART_NOT_OBSERVED')
    inherited_cases = [case for case in read(plan / 'ACCEPTANCE_MATRIX.json')['cases']
                       if case['id'].startswith(('R341-' + stage + '-', 'A-' + stage + '-'))
                       or stage == 'B07' and case['id'] == 'R341-B08-04-A01']
    inherited = [case['id'] for case in inherited_cases if case['level'] == 'ENGINEERING'
                 and case['test_contract'].get('android_scope_contract')]
    require(len(inherited) == {'B02': 8, 'B03': 8, 'B04': 14, 'B05': 7, 'B06': 15, 'B07': 16,
                               'B08': 21, 'B09': 14, 'B10': 12, 'B11': 12, 'B12': 0}[stage], 'R342_STAGE_INHERITED_CASE_SELECTION')
    for case in inherited:
        argv = json.loads(subprocess.run([sys.executable, '-B', str(plan / 'tools/dispatch5.py'),
                          '--plan', str(plan), '--case', case], cwd=ROOT, capture_output=True, text=True,
                          check=True).stdout)
        selector = next(x.split('=', 1)[1] for x in argv if x.startswith('-Pandroid.testInstrumentationRunnerArguments.class='))
        scope = next(x.split('=', 1)[1] for x in argv if x.startswith('-Pandroid.testInstrumentationRunnerArguments.r30Sandbox='))
        run_android_case(serial, source, originals, case_id=case, selector=selector, scope=scope)
    current_engineering = [case['id'] for case in selected if case['level'] == 'ENGINEERING']
    engineering_probes = [case['id'] for case in selected if case['level'] != 'ENGINEERING']
    awaiting_field = {case['id']: case['level'] for case in selected + inherited_cases if case['level'] != 'ENGINEERING'}
    return inherited, current_engineering, engineering_probes, awaiting_field


def observe_inherited_b12(plan, originals):
    cases = [case for case in read(plan / 'ACCEPTANCE_MATRIX.json')['cases']
             if case['batch'] == 'B12' and case['level'] == 'ENGINEERING']
    require(len(cases) == 10, 'R342_STAGE_INHERITED_HOST_SELECTION')
    receipts = {}
    for case in cases:
        case_id = case['id']
        receipt = originals / (case_id+'-host.json')
        command = [sys.executable, '-B', 'tools/r341/test_b12_contracts.py',
                   '--plan', str(plan), '--repo', str(ROOT), '--out', str(receipt), '--case', case_id]
        result = subprocess.run(command,cwd=ROOT,capture_output=True,text=True,timeout=420)
        (originals/(case_id+'-host.log')).write_text(result.stdout+'\n'+result.stderr)
        receipts[case_id] = {'exitCode':result.returncode,
                             'outcome':read(receipt)['outcome'] if receipt.is_file() else None}
        # Some B12 cases consume independently produced delivery or field originals.
        # These exit codes describe the available evidence. The full ledger retains each one.
    return receipts


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
    campaign = signed_report_campaign(serial,originals,sha(apk)) if stage == 'B12' else None
    cover_install(serial, temp / 'baseline152.apk', apk, release, originals, campaign)

    old_plan = Path(os.environ['STARCORE_R341_PLAN_ROOT'])
    host_cases = []
    field_probes = []
    awaiting_field = {}
    inherited_b12 = {}
    if stage == 'B01':
        inherited = run_inherited_b01(serial, old_plan, source, originals)
        host = read(originals / 'b01-host-contracts.json')
        require(host['status'] == 'PASS' and host['sourceCommit'] == source
                and set(host['cases']) == {'R342-001', 'R342-002'}, 'ANDROID_BATCH_HOST_RECEIPT_MISMATCH')
        host_cases = sorted(host['cases'])
        current = []
    elif stage in ('B02', 'B03', 'B04', 'B05', 'B06', 'B07', 'B08', 'B09', 'B10', 'B11', 'B12'):
        inherited, current, field_probes, awaiting_field = run_native_batch(stage, serial, old_plan, source, originals)
        if stage == 'B12':
            inherited_b12 = observe_inherited_b12(old_plan,originals)
    else:
        raise ValueError('R342_STAGE_ANDROID_TESTS_NOT_IMPLEMENTED')
    write_json(originals / 'batch-receipt.json', {
        'schema': 'STARCORE_R342_BATCH_ENGINEERING_1', 'status': 'ENGINEERING_CHECKS_PASSED',
        'buildRun': os.environ['GITHUB_RUN_ID'], 'sourceCommit': source, 'batch': stage,
        'versionCode': release['versionCode'], 'apkSha256': sha(apk), 'planSha256': SEAL,
        'androidInheritedCases': inherited, 'hostIdentityCases': host_cases, 'androidCurrentCases': current,
        'androidProbesForFieldCases': field_probes, 'fieldCasesAwaitingOriginals': awaiting_field,
        'inheritedB12HostChecks': {case:row['exitCode'] for case,row in inherited_b12.items()},
        'vivoV2353A': 'NOT_RUN_DISPOSABLE_EMULATOR', 'physical24h': 'NOT_RUN',
        'physical72h': 'NOT_RUN', 'natural': 'NOT_RUN',
        'originals': {leaf.name: sha(leaf) for leaf in sorted(originals.iterdir()) if leaf.is_file()},
    })


if __name__ == '__main__':
    main()
