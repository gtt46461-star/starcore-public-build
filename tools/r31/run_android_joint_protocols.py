#!/usr/bin/env python3
"""Run every named R31 protocol group on an already-installed disposable Android emulator."""
import argparse
import datetime as dt
import hashlib
import json
from pathlib import Path
import re
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[2]
SUITES = [
    ('original-runtime-storage-owner', 10, [
        'com.ethk3.novaonepro.R31RuntimeProtocolInstrumentedTest',
        'com.ethk3.novaonepro.R31StorageProtocolInstrumentedTest',
        'com.ethk3.novaonepro.R31OwnerFenceProtocolInstrumentedTest']),
    ('family-continuation', 2, ['com.ethk3.novaonepro.strategyv2.R31FamilyContinuationInstrumentedTest']),
    ('history-publication', 2, ['com.ethk3.novaonepro.R31HistoryImportInstrumentedTest']),
    ('platform-startup-readiness', 3, ['com.ethk3.novaonepro.R31PlatformReadinessInstrumentedTest']),
    ('storage-diagnostic-process', 2, ['com.ethk3.novaonepro.R31StorageCheckpointAndroidTest']),
    ('restore-proof-process', 1, ['com.ethk3.novaonepro.R31RestoreProofInstrumentedTest']),
    ('delivery-and-live-service', 6, ['com.ethk3.novaonepro.R31QDeliveryInstrumentedTest']),
]
# Only these tests may use the disposable VM's real private directory: the
# diagnostic service runs in another Android process and cannot share a test
# ContextWrapper. All original protocol groups keep their existing sandboxes.
REAL_PROCESS_ARGS = {
    'storage-diagnostic-process': ['-e', 'r31qRealService', 'true', '-e', 'r31WalDisposable', 'true'],
    'restore-proof-process': ['-e', 'r31qRealService', 'true', '-e', 'r31RestoreDisposable', 'true'],
    'delivery-and-live-service': ['-e', 'r31qRealService', 'true'],
}
# A new explicit offline restore contains two independently authenticated clone
# probes, an expected refusal, and a separate cleanup. Its own existing waits
# total at most 2160 seconds; the original seventeen protocols keep 240 seconds.
GROUP_TIMEOUT_SECONDS = {'restore-proof-process': 2400, 'delivery-and-live-service': 2400}


def redact(text):
    text = re.sub(r'(?i)Bearer\s+[^\s,;\"\'}]+', 'Bearer [REDACTED]', text)
    return re.sub(r'(?i)((?:api[_-]?key|authorization|password|token|secret)[\"\']?\s*[=:]\s*[\"\']?)(?:Bearer\s+)?[^\s,;\"\'}]+', r'\1[REDACTED]', text)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--sandbox-prefix', required=True)
    args = parser.parse_args()
    if not re.fullmatch(r'[A-Za-z0-9_-]{1,45}', args.sandbox_prefix):
        raise SystemExit('INVALID_EXPLICIT_EMULATOR_SANDBOX')
    out = args.out.resolve(); out.mkdir(parents=True, exist_ok=False)
    source = subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip()
    qemu = subprocess.check_output(['adb', 'shell', 'getprop', 'ro.kernel.qemu'], text=True).strip()
    if qemu != '1': raise SystemExit('DISPOSABLE_ANDROID_EMULATOR_REQUIRED_NO_PHYSICAL_DEVICE_CLAIM')
    receipt = {'scope': 'ACTUAL_ANDROID_EMULATOR_NAMED_PROTOCOLS_NOT_NATURAL_OR_ENDURANCE',
               'sourceCommit': source, 'startedAt': dt.datetime.now(dt.timezone.utc).isoformat(),
               'api': subprocess.check_output(['adb', 'shell', 'getprop', 'ro.build.version.sdk'], text=True).strip(),
               'serial': subprocess.check_output(['adb', 'get-serialno'], text=True).strip(),
               'groups': [], 'physicalDevice': 'NOT_RUN', '24h': 'NOT_RUN', '72h': 'NOT_RUN', 'naturalQuality': 'NOT_PROVEN'}

    def save():
        (out / 'JOINT-ANDROID-RECEIPT.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')

    save()
    for name, expected, classes in SUITES:
        timeout_seconds = GROUP_TIMEOUT_SECONDS.get(name, 240)
        sandbox = args.sandbox_prefix + '-' + name[:30]
        command = ['adb', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'r30Sandbox', sandbox,
                   *REAL_PROCESS_ARGS.get(name, []),
                   '-e', 'class', ','.join(classes),
                   'com.starcore.research.r13.test/com.ethk3.novaonepro.R30FaultAndroidRunner']
        row = {'name': name, 'expectedTests': expected, 'classes': classes, 'command': command,
               'startedAt': dt.datetime.now(dt.timezone.utc).isoformat(), 'status': 'IN_PROGRESS',
               'executionTimeoutSeconds': timeout_seconds}
        receipt['groups'].append(row); save()
        try:
            result = subprocess.run(command, capture_output=True, text=True, timeout=timeout_seconds)
            raw = result.stdout + result.stderr
            found = re.search(r'OK \((\d+) tests?\)', raw)
            passed = result.returncode == 0 and found is not None and int(found.group(1)) == expected
            passed = passed and 'FAILURES!!!' not in raw and 'INSTRUMENTATION_FAILED' not in raw and 'Process crashed' not in raw
            row.update(exitCode=result.returncode, observedTests=int(found.group(1)) if found else None,
                       status='PASS_NAMED_ENGINEERING_PROTOCOLS' if passed else 'FAILED_OR_INCOMPLETE')
        except subprocess.TimeoutExpired as failure:
            def decoded(value): return value.decode(errors='replace') if isinstance(value, bytes) else value or ''
            raw = decoded(failure.stdout) + decoded(failure.stderr)
            row.update(status='FAILED_REAL_TIMEOUT', observedTests=None, timeoutSeconds=timeout_seconds)
        except Exception as failure:
            raw = str(failure)
            row.update(status='FAILED_EXECUTION', observedTests=None)
        row['originalOutputSha256BeforeRedaction'] = hashlib.sha256(raw.encode()).hexdigest()
        clean = redact(raw); log = out / (name + '.txt'); log.write_text(clean)
        row.update(log=log.name, logSha256=hashlib.sha256(log.read_bytes()).hexdigest(),
                   finishedAt=dt.datetime.now(dt.timezone.utc).isoformat())
        if row['status'] != 'PASS_NAMED_ENGINEERING_PROTOCOLS':
            cleanup = {}
            commands = [
                ('crashLog', ['adb', 'logcat', '-d', '-b', 'crash', '-t', '256', '-v', 'threadtime']),
                ('forceStop', ['adb', 'shell', 'am', 'force-stop', 'com.starcore.research.r13']),
                ('remainingPid', ['adb', 'shell', 'pidof', 'com.starcore.research.r13']),
            ]
            for action, argv in commands:
                try:
                    check = subprocess.run(argv, capture_output=True, text=True, timeout=20)
                    clean_output = redact(check.stdout + check.stderr)
                    cleanup[action] = {'exitCode': check.returncode, 'output': clean_output}
                    if action == 'crashLog':
                        (out / (name + '-crash-redacted.txt')).write_text(clean_output)
                    elif action == 'remainingPid':
                        cleanup['processExitConfirmed'] = check.returncode == 1 and not check.stdout.strip()
                except subprocess.TimeoutExpired:
                    cleanup[action] = {'status': 'TIMEOUT', 'timeoutSeconds': 20}
                except Exception as failure:
                    cleanup[action] = {'status': 'FAILED_EXECUTION', 'error': redact(str(failure))}
                row['failedGroupCleanup'] = cleanup
                save()
            cleanup.setdefault('processExitConfirmed', False)
        save()
        print('R31_ANDROID_JOINT_GROUP|' + json.dumps(row, ensure_ascii=False, sort_keys=True), flush=True)
    receipt['finishedAt'] = dt.datetime.now(dt.timezone.utc).isoformat()
    receipt['passedGroups'] = sum(row['status'] == 'PASS_NAMED_ENGINEERING_PROTOCOLS' for row in receipt['groups'])
    receipt['failedGroups'] = len(SUITES) - receipt['passedGroups']
    receipt['state'] = 'PASS_NAMED_GROUPS_ONLY' if receipt['failedGroups'] == 0 else 'FAILED_OR_INCOMPLETE'
    receipt['existingFullFaultMatrices'] = 'MUST_RUN_SEPARATELY_NOT_INFERRED_FROM_THE_ORIGINAL_17_OR_NEW_PROCESS_TESTS'
    save()
    print('R31_ANDROID_JOINT_DONE|' + json.dumps({k: v for k, v in receipt.items() if k != 'groups'}, ensure_ascii=False), flush=True)
    sys.exit(0 if receipt['failedGroups'] == 0 else 1)


if __name__ == '__main__':
    main()
