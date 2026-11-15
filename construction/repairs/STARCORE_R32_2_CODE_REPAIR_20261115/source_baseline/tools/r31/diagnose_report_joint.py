#!/usr/bin/env python3
"""Observe the existing signed test transport after a real restore; no DB reset.

Public output contains opaque request/job IDs, byte counts and source frames.
Original instrumentation, database contents and ANR files stay private.
"""
import argparse
import hashlib
import json
import re
import subprocess
import tempfile
import time
from pathlib import Path

PKG = 'com.starcore.research.r13'
ROOT = '/data/user/0/' + PKG + '/files/r981_reports'
RUNNER = PKG + '.test/com.ethk3.novaonepro.R30FaultAndroidRunner'


def run(*args, check=True, timeout=60):
    return subprocess.run(args, capture_output=True, check=check, timeout=timeout)


def adb(*args, check=True, timeout=60):
    return run('adb', *args, check=check, timeout=timeout).stdout.decode(errors='replace')


def one(root, pattern):
    rows = list(root.rglob(pattern))
    assert len(rows) == 1, 'EXACT_BUILD_FILE_REQUIRED:' + pattern
    return rows[0]


def read_json(path):
    return json.loads(adb('shell', 'cat', path))


def inventory():
    rows = []
    for subdir, pattern in [('requests', '*.json'), ('jobs', 'job.json')]:
        paths = adb('shell', 'find', ROOT + '/' + subdir, '-name', pattern, '-type', 'f', check=False).splitlines()
        for path in paths[:1024]:
            if not re.fullmatch(re.escape(ROOT) + r'/(?:requests/[a-f0-9]{64}(?:\.ack)?\.json|jobs/[a-f0-9]{64}/job\.json)', path):
                continue
            try:
                value = read_json(path)
                row = {key: value[key] for key in ['requestId', 'jobId', 'stage', 'mode', 'createdWallMs', 'updatedWallMs', 'submissionState', 'observationState'] if key in value}
                row['record'] = path.removeprefix(ROOT + '/')
                row['reasonSymbols'] = sorted(set(re.findall(r'\bREPORT_[A-Z0-9_]+\b', str(value.get('lastReason', '')) + str(value.get('reason', '')))))
                if '/jobs/' in path:
                    directory = path.rsplit('/', 1)[0]
                    row['fileBytes'] = {}
                    for name in ['R30_REPORT.json', 'R30_REPORT.md', 'delivery/STARCORE_FULL_REPORT.zip']:
                        size = adb('shell', 'stat', '-c', '%s', directory + '/' + name, check=False).strip()
                        row['fileBytes'][name] = int(size) if size.isdigit() else None
                rows.append(row)
            except Exception as failure:
                rows.append({'record': path.removeprefix(ROOT + '/'), 'readFailureClass': type(failure).__name__})
    return rows


def stack_sample(private, seen):
    for process in [PKG, PKG + ':diagnostics']:
        pid = adb('shell', 'pidof', process, check=False).strip()
        if re.fullmatch('[0-9]+', pid):
            adb('shell', 'kill', '-3', pid, check=False)
    time.sleep(.5)
    paths = adb('shell', 'find', '/data/anr', '-maxdepth', '1', '-type', 'f', check=False).splitlines()
    frames = []
    for path in paths[-24:]:
        if path in seen or not path.startswith('/data/anr/'):
            continue
        seen.add(path)
        local = private / ('anr-' + str(len(seen)))
        pull = run('adb', 'pull', path, str(local), check=False)
        if pull.returncode or not local.is_file():
            continue
        thread = None
        for line in local.read_text(errors='replace').splitlines():
            if line.startswith('"'):
                thread = line.split('"', 2)[1]
            if thread and any(name in thread for name in ['report-request', 'report-owner', 'report-control']):
                match = re.search(r'at (com\.ethk3\.novaonepro\.[A-Za-z0-9_.$]+\([A-Za-z0-9_.$: -]+\))', line)
                if match:
                    frames.append({'thread': thread, 'sourceFrame': match.group(1)})
    return frames[:120]


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--built', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    parser.add_argument('--source', required=True)
    args = parser.parse_args()
    assert adb('shell', 'getprop', 'ro.kernel.qemu').strip() == '1'
    assert adb('shell', 'id', '-u').strip() == '0'
    assert one(args.built, 'source-commit.txt').read_text().strip() == args.source
    apks = list((args.built / 'app/build/outputs/apk/debug').glob('*.apk'))
    assert len(apks) == 1
    apk, test = apks[0], one(args.built, '*-androidTest.apk')
    sums = dict((line.split()[1].rsplit('/', 1)[-1], line.split()[0]) for line in one(args.built, 'SHA256SUMS.txt').read_text().splitlines())
    for path in [apk, test]:
        assert hashlib.sha256(path.read_bytes()).hexdigest() == sums[path.name]
        adb('install', '-r', '-g', '-t', str(path), timeout=120)
    args.out.mkdir(parents=True, exist_ok=False)
    result = {'sourceCommit': args.source, 'debugApkSha256': sums[apk.name], 'testApkSha256': sums[test.name],
              'harnessSourceCommit': run('git', 'rev-parse', 'HEAD').stdout.decode().strip(),
              'scope': 'EXACT_EXISTING_DEBUG_APP_AND_SIGNED_TEST_TRANSPORT; RESTORE_THEN_ORIGINAL_SEVEN; DIAGNOSIS_NOT_ACCEPTANCE',
              'physicalDevice': 'NOT_RUN', 'naturalQuality': 'NOT_PROVEN', 'samples': []}
    base = ['adb', 'shell', 'am', 'instrument', '-w', '-r', '-e', 'r30Sandbox', 'r31-joint-report-diagnosis', '-e', 'r31qRealService', 'true']
    with tempfile.TemporaryDirectory(prefix='r31-joint-report-private-') as directory:
        private = Path(directory)
        prelude = run(*base, '-e', 'r31RestoreDisposable', 'true', '-e', 'class', 'com.ethk3.novaonepro.R31RestoreProofInstrumentedTest', RUNNER, check=False, timeout=2400)
        text = prelude.stdout.decode(errors='replace')
        result['restorePassed'] = bool(re.search(r'OK \(1 test\)', text)) and 'FAILURES!!!' not in text
        adb('logcat', '-c')
        log = private / 'joint.txt'
        with log.open('wb') as stream:
            process = subprocess.Popen([*base, '-e', 'class', 'com.ethk3.novaonepro.R31QDeliveryInstrumentedTest', RUNNER], stdout=stream, stderr=subprocess.STDOUT)
            started = time.monotonic()
            next_sample = 30
            seen = set()
            while process.poll() is None and time.monotonic() - started < 1200:
                elapsed = time.monotonic() - started
                if elapsed >= next_sample:
                    sample = {'elapsedSeconds': round(elapsed, 3), 'records': inventory(), 'sourceFrames': stack_sample(private, seen)}
                    result['samples'].append(sample)
                    (args.out / 'REPORT_JOINT_DIAGNOSIS.json').write_text(json.dumps(result, indent=2) + '\n')
                    print(json.dumps({'elapsedSeconds': sample['elapsedSeconds'], 'records': len(sample['records']), 'sourceFrames': len(sample['sourceFrames'])}), flush=True)
                    next_sample += 60
                time.sleep(1)
            if process.poll() is None:
                process.kill(); process.wait(); result['outerDiagnosticWindowClosed'] = True
        output = log.read_text(errors='replace')
        result['originalSevenPassed'] = bool(re.search(r'OK \(7 tests\)', output)) and 'FAILURES!!!' not in output
        result['failureSymbols'] = sorted(set(re.findall(r'AssertionError: ([A-Z][A-Z0-9_]{3,100})', output)))
        result['finalRecords'] = inventory()
        result['sqliteCliPresent'] = bool(adb('shell', 'command', '-v', 'sqlite3', check=False).strip())
        result['originalReportPollCeilingSeconds'] = 360
    (args.out / 'REPORT_JOINT_DIAGNOSIS.json').write_text(json.dumps(result, indent=2) + '\n')
    print(json.dumps({key: result[key] for key in ['sourceCommit', 'restorePassed', 'originalSevenPassed', 'failureSymbols', 'sqliteCliPresent']}))
    return 0 if result['restorePassed'] and result['originalSevenPassed'] else 1


if __name__ == '__main__':
    raise SystemExit(main())
