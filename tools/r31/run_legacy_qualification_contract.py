#!/usr/bin/env python3
"""Run the preserved destructive-reset expectation and the R31 exposure-preserving contract."""
import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile
import sys

ROOT = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(ROOT / 'tools/r31'))
from host_runtime_fixture import compile_owned_platform


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--classes', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=False)
    work = Path(tempfile.mkdtemp(prefix='r31-qual-contract-', dir='/workspace'))
    sdk = Path(os.environ['ANDROID_HOME']) / 'platforms/android-36/android.jar'
    jdbc = Path(os.environ['SQLITE_JDBC'])
    receipt = {'scope': 'NATIVE_SQLITE_EXPOSURE_PRESERVATION_NOT_WHOLE_ANDROID_OR_DEVICE',
               'sourceHead': subprocess.check_output(['git', 'rev-parse', 'HEAD'], cwd=ROOT, text=True).strip(),
               'startedAt': dt.datetime.now(dt.timezone.utc).isoformat(),
               'support': {'path': str(args.classes), 'sha256': sha(args.classes)},
               'sources': [], 'commands': [], 'state': 'IMPLEMENTING',
               'physicalDevice': 'NOT_RUN', '24h': 'NOT_RUN', '72h': 'NOT_RUN', 'naturalQuality': 'NOT_EVALUATED'}

    def save():
        (out / 'RECEIPT.json').write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + '\n')

    def run(name, argv):
        command = list(map(str, argv))
        result = subprocess.run(command, cwd=ROOT, capture_output=True, text=True, timeout=240)
        path = out / (name + '.log')
        path.write_text(result.stdout + result.stderr)
        receipt['commands'].append({'name': name, 'argv': command, 'exitCode': result.returncode, 'log': str(path), 'sha256': sha(path)})
        save()
        print(name, result.returncode, flush=True)
        return result

    def snapshot(origin, relative):
        target = work / relative
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_bytes(origin.read_bytes())
        receipt['sources'].append({'origin': str(origin), 'sha256': sha(origin), 'snapshot': str(target)})
        return target

    try:
        platform = compile_owned_platform(ROOT, work / 'platform-inputs', args.classes, sdk)
        production = work / 'production'
        production.mkdir()
        source = snapshot(ROOT / 'app/src/main/java/com/ethk3/novaonepro/strategyv2/QualificationInboxR29.java', 'source/QualificationInboxR29.java')
        cp = os.pathsep.join(map(str, (platform, args.classes, sdk)))
        if run('compile-production', ['javac', '-encoding', 'UTF-8', '-sourcepath', '', '-implicit:none', '-cp', cp, '-d', production, source]).returncode:
            raise RuntimeError('Actual production qualification source did not compile')
        cp = os.pathsep.join(map(str, (platform, production, args.classes, jdbc, sdk)))
        tests = [('original', ROOT / 'verification/r31/p01/qualification-host-contract/original-QualInboxR29HostTest.java.txt'),
                 ('r31', ROOT / 'tools/r29/qual_host/QualInboxR29HostTest.java')]
        for name, origin in tests:
            test = snapshot(origin, name + '/source/QualInboxR29HostTest.java')
            classes = work / name / 'classes'
            classes.mkdir()
            if run(name + '-compile', ['javac', '-encoding', 'UTF-8', '-sourcepath', '', '-implicit:none', '-cp', cp, '-d', classes, test]).returncode:
                raise RuntimeError(name + ' test source did not compile')
            result = run(name + '-native', ['java', '-Xmx768m', '-cp', str(classes) + os.pathsep + cp, 'com.ethk3.novaonepro.strategyv2.QualInboxR29HostTest'])
            failed = re.findall(r'R29_QUAL\|([^|\n]+)\|FAILED\|', result.stdout)
            if name == 'original':
                expected = {'Q3_LATE_HIT_MINUS2_RECOVERED_AND_REBUILT', 'Q11_REBUILD_GAPPED_CHECKPOINT'}
                if result.returncode != 1 or set(failed) != expected:
                    raise RuntimeError('Original counterexample did not fail in exactly the two preserved destructive-reset expectations: ' + repr(failed))
                receipt['preservedOriginal'] = {'state': 'EXPECTED_CONTRACT_FAILURE', 'failedCases': failed}
            elif result.returncode or failed or 'R29_QUAL|SUMMARY|OK' not in result.stdout:
                raise RuntimeError('R31 exposure-preservation contract failed: ' + repr(failed))
            else:
                receipt['r31Contract'] = {'state': 'HOST_PASS', 'checks': len(re.findall(r'R29_QUAL\|[^|\n]+\|OK\|', result.stdout))}
        changed = [row['origin'] for row in receipt['sources'] if sha(Path(row['origin'])) != row['sha256']]
        receipt['sourceChangesDuringRun'] = changed
        if changed:
            raise RuntimeError('Source changed while running; evidence applies to snapshot only')
        receipt['state'] = 'HOST_PASS_ORIGINAL_FAILURES_PRESERVED'
    except Exception as failure:
        receipt['state'] = 'FAILED'
        receipt['failure'] = repr(failure)
        raise
    finally:
        receipt['finishedAt'] = dt.datetime.now(dt.timezone.utc).isoformat()
        save()


if __name__ == '__main__':
    main()
