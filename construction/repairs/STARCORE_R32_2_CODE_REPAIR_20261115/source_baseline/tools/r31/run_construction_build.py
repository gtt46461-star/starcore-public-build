#!/usr/bin/env python3
"""Record unique real Android build attempts; bootstrap is never final acceptance."""
import argparse
import datetime as dt
import hashlib
import json
from pathlib import Path
import shutil
import subprocess
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[2]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def inputs():
    paths = []
    for directory in ['app/src/main', 'app/src/test', 'app/src/androidTest']:
        paths.extend(p for p in (ROOT / directory).rglob('*') if p.is_file())
    paths.extend(ROOT / path for path in ['app/build.gradle', 'release/release_manifest.json'])
    return {str(path.relative_to(ROOT)): digest(path) for path in sorted(paths)}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--mode', choices=['bootstrap', 'joint'], required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    out = args.out.resolve(); out.mkdir(parents=True, exist_ok=False)
    command = ['bash', './gradlew', '--no-daemon', '--max-workers=4', '-Pr30Instrumentation=true']
    excluded = []
    if args.mode == 'bootstrap':
        command += [':app:compileDebugJavaWithJavac']
        excluded = [':app:generateR982SourceCallAsset', ':app:generateR997ConstructionAssets']
        for task in excluded: command += ['-x', task]
    else:
        command += ['--continue', ':app:compileDebugJavaWithJavac', ':app:compileDebugAndroidTestJavaWithJavac',
                    ':app:testDebugUnitTest', ':app:lintDebug', ':app:lintRelease', ':app:verifyFinalR31BuildIdentity']
        old = ROOT / 'app/build/test-results/testDebugUnitTest'
        if old.exists(): shutil.rmtree(old)
    before = inputs()
    receipt = {'scope': 'ACTUAL_ANDROID_PROJECT_BUILD_NOT_DEVICE_OR_NATURAL_QUALITY', 'mode': args.mode,
               'sourceCommit': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
               'command': command, 'excludedTasks': excluded, 'startedAt': dt.datetime.now(dt.timezone.utc).isoformat(),
               'inputSha256Before': before, 'APK': None, 'physicalDevice': 'NOT_RUN', '24h': 'NOT_RUN', '72h': 'NOT_RUN',
               'naturalQuality': 'NOT_PROVEN', 'boundary': 'BOOTSTRAP_CLASSES_ONLY_FINAL_UNEXCLUDED_STILL_REQUIRED' if args.mode == 'bootstrap' else 'UNEXCLUDED_REQUESTED_CHECKS_FAILURES_RETAINED'}
    log = out / 'BUILD.log'
    with log.open('wb') as output:
        result = subprocess.run(command, cwd=ROOT, stdout=output, stderr=subprocess.STDOUT)
    after = inputs()
    changed = [name for name in sorted(before.keys() | after.keys()) if before.get(name) != after.get(name)]
    text = log.read_text(errors='replace')
    executed = args.mode == 'joint' and '> Task :app:testDebugUnitTest' in text and '> Task :app:testDebugUnitTest UP-TO-DATE' not in text
    counts = None; failures = []
    if executed:
        xmls = sorted((ROOT / 'app/build/test-results/testDebugUnitTest').glob('TEST-*.xml'))
        suite = [ET.parse(path).getroot() for path in xmls]
        counts = {field: sum(int(row.get(field, '0')) for row in suite) for field in ['tests', 'failures', 'errors', 'skipped']}
        target = out / 'unit-xml'; target.mkdir()
        for path, row in zip(xmls, suite):
            shutil.copy2(path, target / path.name)
            for case in row.findall('testcase'):
                for failure in case.findall('failure') + case.findall('error'):
                    failures.append({'suite': path.name, 'test': case.get('name'), 'message': failure.get('message')})
    receipt.update(finishedAt=dt.datetime.now(dt.timezone.utc).isoformat(), exitCode=result.returncode,
                   inputsChangedDuringRun=changed, unitTaskActuallyExecuted=executed, junit=counts, failures=failures,
                   logSha256=digest(log), state='COMMAND_PASS' if result.returncode == 0 and not changed else 'FAILED')
    if args.mode == 'joint' and (not executed or counts is None or counts['tests'] == 0 or counts['failures'] or counts['errors'] or counts['skipped']):
        receipt['state'] = 'FAILED_OR_INCOMPLETE_UNIT_EVIDENCE'
    (out / 'RECEIPT.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')
    print(json.dumps({key: value for key, value in receipt.items() if key != 'inputSha256Before'}, ensure_ascii=False), flush=True)
    sys.exit(0 if receipt['state'] == 'COMMAND_PASS' else result.returncode or 1)


if __name__ == '__main__':
    main()
