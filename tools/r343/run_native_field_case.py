#!/usr/bin/env python3
"""Execute a FIELD173 case through the Android instrumentation test APK."""
import base64
import json
import os
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(1, str(ROOT / 'tools/r33'))
import field_delivery_contract as contract

PACKAGE = 'com.starcore.research.r13'
RUNNER = PACKAGE + '.test/com.ethk3.novaonepro.R30FaultAndroidRunner'
IDENTITY_KEYS = ('caseId', 'sourceCommit', 'sourceTree', 'planCommit', 'planSha256',
                 'batch', 'apkSha256', 'buildRun', 'executionId')


def write_original(path, data):
    with path.open('xb') as out:
        out.write(data)
    return {'file': path.name, 'sha256': contract.digest(data), 'kind': 'READBACK'}


def read_status(stdout, name):
    prefix = f'INSTRUMENTATION_STATUS: {name}='
    return [json.loads(line.removeprefix(prefix)) for line in stdout.splitlines()
            if line.startswith(prefix)]


def adb(serial, *args):
    return ['adb', '-s', serial, *map(str, args)]


def installed_apk_sha(serial):
    path = subprocess.check_output(adb(serial, 'shell', 'pm', 'path', PACKAGE),
                                   text=True, timeout=20).strip()
    contract.require(re.fullmatch(r'package:/[^\n]+', path), 'FIELD173_NATIVE_PACKAGE_PATH_REQUIRED')
    return subprocess.check_output(adb(serial, 'shell', 'sha256sum', path.removeprefix('package:')),
                                   text=True, timeout=30).split()[0]


def ensure_candidate_installed(serial, candidate, expected_sha):
    contract.require(contract.binding.sha(candidate) == expected_sha, 'FIELD173_CANDIDATE_APK_MISMATCH')
    if installed_apk_sha(serial) != expected_sha:
        result = subprocess.check_output(adb(serial, 'install', '-r', candidate), text=True, timeout=180)
        contract.require('Success' in result, 'FIELD173_NATIVE_CANDIDATE_INSTALL_FAILED')
    contract.require(installed_apk_sha(serial) == expected_sha, 'FIELD173_NATIVE_INSTALLED_APK_MISMATCH')


def run(selector, host_verify=None):
    context = json.loads(Path(os.environ['STARCORE_FIELD173_CASE_CONTEXT']).read_bytes())
    target = Path(os.environ['STARCORE_FIELD173_CANDIDATE_APK'])
    output = Path(os.environ['STARCORE_FIELD173_CASE_OUTPUT'])
    serial = subprocess.check_output(['adb', 'get-serialno'], text=True, timeout=20).strip()
    contract.require(re.fullmatch(r'emulator-[0-9]+', serial), 'FIELD173_NATIVE_DISPOSABLE_ANDROID_REQUIRED')
    expected = {key: context[key] for key in IDENTITY_KEYS}
    ensure_candidate_installed(serial, target, expected['apkSha256'])
    host_original = None
    if host_verify:
        host_original = host_verify(expected, target, serial, output.parent)
    token = base64.urlsafe_b64encode(contract.canonical(expected)).decode('ascii')
    sandbox = 'R343_' + expected['executionId']
    argv = adb(serial, 'shell', 'am', 'instrument', '-w', '-r',
               '-e', 'class', selector, '-e', 'r30Sandbox', sandbox, '-e', 'field173Context', token, RUNNER)
    stem = 'field173-' + expected['caseId']
    native_argv = write_original(output.parent / (stem + '-instrumentation-argv.json'), contract.canonical(argv))
    result = subprocess.run(argv, capture_output=True, text=True, timeout=560, check=False)
    sys.stderr.write(result.stderr)
    print(result.stdout, end='')
    native = write_original(output.parent / (stem + '-android-native.log'), result.stdout.encode())
    contract.require(result.returncode == 0 and re.search(r'OK \(1 test\)', result.stdout)
                     and not re.search(r'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed', result.stdout),
                     'FIELD173_NATIVE_RUNNER_FAILED:' + expected['caseId'])
    events = read_status(result.stdout, 'field173Event')
    records = read_status(result.stdout, 'field173Readback')
    contract.require(len(records) == 1 and all(records[0].get(k) == v for k, v in expected.items()),
                     'FIELD173_NATIVE_READBACK_MISSING_OR_WRONG_IDENTITY')
    record = records[0]
    contract.require(isinstance(record.get('readback'), dict) and record['readback'],
                     'FIELD173_NATIVE_READBACK_EMPTY')
    readback = write_original(output.parent / (stem + '-android-readback.json'), contract.canonical(record))
    report = {'schema': 'STARCORE_FIELD173_CASE_OUTPUT_1', **expected,
              'dataScope': record['dataScope'], 'artifacts': [native_argv, native, readback]}
    if host_original:
        report['artifacts'].extend(host_original)
    write_original(output, contract.canonical(report))
    for event in events:
        print(contract.MARKER + json.dumps(event, ensure_ascii=False, separators=(',', ':')))


if __name__ == '__main__':
    if len(sys.argv) != 2 or not re.fullmatch(r'[A-Za-z0-9_.]+#[A-Za-z0-9_]+', sys.argv[1]):
        raise SystemExit('Supply one exact instrumentation class#method')
    run(sys.argv[1])
