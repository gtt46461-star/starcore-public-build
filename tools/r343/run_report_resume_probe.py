#!/usr/bin/env python3
"""Interrupt and resume one actual raw export on a disposable signed Android installation."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import zipfile

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT/'tools/r342'))
from ci_candidate_tests import PACKAGE, adb, installed, read, require, sha, write_json


def device_json(serial, file):
    result = subprocess.run(['adb', '-s', serial, 'shell', 'cat', file],
                            capture_output=True, text=True, timeout=15)
    if result.returncode and 'No such file or directory' in result.stderr:
        return None
    result.check_returncode()
    return json.loads(result.stdout)


def interrupt_after_freeze(serial, root, previous_job):
    deadline = time.monotonic() + 240
    active = {'SNAPSHOT_DURABLE', 'INDEXING', 'INDEX_DURABLE', 'COMPRESSING'}
    while time.monotonic() < deadline:
        pointer = device_json(serial, root+'/current-job.json')
        if pointer is not None:
            job_id = pointer.get('jobId')
            require(re.fullmatch('[0-9a-f]{64}', job_id or ''), 'REPORT_RESUME_JOB_ID')
            if job_id != previous_job:
                folder = f'{root}/jobs/{job_id}'
                state = device_json(serial, folder+'/job.json')
                closure = device_json(serial, folder+'/inputs-complete.json')
                if state is not None and closure is not None:
                    require(state['jobId'] == job_id == pointer['requestId'] == state['requestId']
                            and state['mode'] == 'FULL_REPORT', 'REPORT_RESUME_JOB_IDENTITY')
                    require(state['stage'] in active, 'REPORT_RESUME_INTERRUPT_MISSED')
                    require(state['snapshotSha'] == closure['snapshotSha256']
                            and state['snapshotBytes'] > 0, 'REPORT_RESUME_FIXED_SOURCE')
                    adb(serial, 'shell', 'am', 'force-stop', PACKAGE)
                    persisted = device_json(serial, folder+'/job.json')
                    require(persisted['stage'] in active
                            and persisted['snapshotSha'] == state['snapshotSha'],
                            'REPORT_RESUME_SNAPSHOT_NOT_RETAINED')
                    observed = adb(serial, 'shell', 'sha256sum', folder+'/snapshot.db').split()[0]
                    require(observed == state['snapshotSha'], 'REPORT_RESUME_STOPPED_SNAPSHOT_DIGEST')
                    return {'jobId': job_id, 'requestId': state['requestId'],
                            'stageAtStop': persisted['stage'], 'attemptAtStop': persisted['attempt'],
                            'snapshotSha256': observed, 'snapshotBytes': state['snapshotBytes'],
                            'closure': closure}
        time.sleep(0.2)
    raise ValueError('REPORT_RESUME_NO_DURABLE_SNAPSHOT_BEFORE_INTERRUPT')


def run(destination, build):
    import report_ui_driver as ui

    serial = ui.DEVICE
    require(re.fullmatch(r'emulator-[0-9]+', serial)
            and adb(serial, 'shell', 'getprop', 'ro.kernel.qemu') == '1',
            'REPORT_RESUME_REQUIRES_DISPOSABLE_ANDROID')
    require(build['gradleRelease'] == 'PASS' and build['sourceCommit'] == os.environ['SOURCE'],
            'REPORT_RESUME_REQUIRES_EXACT_BUILD')
    adb(serial, 'root')
    adb(serial, 'wait-for-device')
    adb(serial, 'shell', 'cmd', 'connectivity', 'airplane-mode', 'enable')
    adb(serial, 'shell', 'svc', 'wifi', 'disable')
    deadline = time.monotonic() + 30
    while adb(serial, 'shell', 'ip', 'route', 'show'):
        require(time.monotonic() < deadline, 'REPORT_RESUME_REQUIRES_OFFLINE')
        time.sleep(1)
    require(adb(serial, 'shell', 'settings', 'get', 'global', 'airplane_mode_on') == '1'
            and not adb(serial, 'shell', 'ip', 'route', 'show'), 'REPORT_RESUME_REQUIRES_OFFLINE')
    seen = installed(serial, destination/'report-resume-installed.txt')
    require(seen['apkSha256'] == build['apkSha256'], 'REPORT_RESUME_REQUIRES_EXACT_INSTALLED_APK')
    adb(serial, 'shell', 'pm', 'grant', PACKAGE, 'android.permission.POST_NOTIFICATIONS')
    source_root = f'/data/user/0/{PACKAGE}/files/r981_reports'
    prior = device_json(serial, source_root+'/current-job.json')

    ui.commit = build['sourceCommit']
    ui.out = {'reportReadbacks': [], 'activeProfile': 'FULL_RAW'}
    ui.report_entry('另存完整原始备份（体积大）')
    interrupted = interrupt_after_freeze(serial, source_root, prior.get('jobId') if prior else None)
    write_json(destination/'signed-report-resume-before.json', interrupted)
    ui.report_entry('取回上次报告或继续生成')
    reopened = ui.save_report(ui.wait_for_report('candidate-resume'), 'candidate-resume')
    final = device_json(serial, f"{source_root}/jobs/{interrupted['jobId']}/job.json")
    require(reopened['rawEvidenceIncluded'] and reopened['snapshotQuickCheck'] == 'ok'
            and reopened['jobId'] == interrupted['jobId']
            and reopened['snapshotSha256'] == interrupted['snapshotSha256']
            and reopened['snapshotBytes'] == interrupted['snapshotBytes']
            and final['requestId'] == interrupted['requestId']
            and final['attempt'] > interrupted['attemptAtStop']
            and final['stage'] == 'READY_TO_SHARE', 'REPORT_RESUME_SAME_JOB_SAME_SNAPSHOT_REQUIRED')
    original = ui.OUT/reopened['file']
    with zipfile.ZipFile(original) as report:
        with report.open('installed-app.apk') as apk:
            require(hashlib.file_digest(apk,'sha256').hexdigest() == build['apkSha256'],
                    'REPORT_RESUME_ARCHIVED_APK_CHANGED')
    encrypted = destination/'signed-report-resume-raw.zip.cms'
    certificate = ROOT/'tools/r342/report-originals-recipient.crt.txt'
    subprocess.run(['openssl','cms','-encrypt','-binary','-aes-256-gcm','-outform','DER',
                    '-in',str(original),'-out',str(encrypted),str(certificate)],check=True)
    write_json(destination/'signed-report-resume-readback.json',{
        'scope':'DISPOSABLE_ANDROID_API34_FORCE_STOP_UI_RESUME_AND_SAF_READBACK',
        'status':'PASS','sourceCommit':build['sourceCommit'],'apkSha256':build['apkSha256'],
        'run':build['ciRun'],'before':interrupted,'attemptAfter':final['attempt'],'reopened':reopened,
        'encryptedOriginal':encrypted.name,'encryptedSha256':sha(encrypted),
    })
    print('SIGNED_REPORT_RESUME_PASS same task, source snapshot and verified ZIP', flush=True)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--originals', type=Path, required=True)
    parser.add_argument('--build-receipt', type=Path, required=True)
    arguments = parser.parse_args()
    run(arguments.originals, read(arguments.build_receipt))
