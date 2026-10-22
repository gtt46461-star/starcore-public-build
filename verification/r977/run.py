#!/usr/bin/env python3
"""R977 actual-product regression runner; synthetic input, not an Android device test.

Run each step in order or use --step all. Uses real product Java and a real SQLite
bridge, plus explicit Android API adapters. No mock certificate in the V3 path.
Never runs legacy 'base' fixtures that might shadow product classes.
"""
from pathlib import Path
import argparse
import hashlib
import json
import os
import subprocess
import sys

STEPS = ('compile', 'exact', 'numeric', 'clock', 'identity', 'blockers', 'report',
         'chain-blockers', 'post-retry', 'continuation', 'maintenance', 'evolution',
         'v3-train-cal', 'v3-window1', 'v3-real')
ROOT = Path(__file__).resolve().parents[2]


def source_digest() -> str:
    h = hashlib.sha256()
    for folder in ('app/src', 'app/build/generated/source/r967', 'release'):
        for f in sorted((ROOT / folder).rglob('*')):
            if f.is_file():
                h.update(str(f.relative_to(ROOT)).encode() + b'\0' + f.read_bytes())
    return h.hexdigest()


def main() -> None:
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('--step', choices=STEPS + ('all',), required=True)
    p.add_argument('--out', type=Path, required=True)
    p.add_argument('--android-jar', type=Path, required=True)
    a = p.parse_args()
    out = a.out.resolve()
    android = a.android_jar.resolve()
    if not android.is_file():
        p.error('--android-jar must point to the supplied Android SDK jar')
    if out == ROOT or ROOT.is_relative_to(out):
        p.error('test output must not contain the source repository')
    out.mkdir(parents=True, exist_ok=True)
    env = dict(os.environ, JAVA_TOOL_OPTIONS='-Xmx768m -XX:ActiveProcessorCount=2')
    meta = out / 'r977_run_manifest.json'
    records = json.loads(meta.read_text())['executions'] if meta.exists() else []

    def run(label: str, command: list) -> None:
        command = list(map(str, command))
        log = out / (label + '.log')
        before = source_digest()
        with log.open('w') as f:
            f.write('COMMAND=' + json.dumps(command) + '\n')
            f.flush()
            ret = subprocess.run(command, cwd=ROOT, env=env, stdout=f,
                                 stderr=subprocess.STDOUT, check=False).returncode
        records.append({'name': label, 'exit_code': ret, 'log': log.name,
                        'source_before_sha256': before,
                        'source_after_sha256': source_digest()})
        meta.write_text(json.dumps({'scope': 'SYNTHETIC_INPUT_ACTUAL_PRODUCT_JAVA_REAL_SQLITE_ANDROID_ADAPTERS',
                                    'device': 'NOT_RUN', 'live_accuracy': 'NOT_RUN',
                                    'report_step_fixture': 'BUNDLED_R975_NOT_USER_CURRENT_PHONE_DATABASE',
                                    'executions': records}, indent=2) + '\n')
        print(label, 'exit=' + str(ret), 'log=' + str(log), flush=True)
        if ret:
            raise SystemExit(ret)

    for step in (STEPS if a.step == 'all' else (a.step,)):
        if step not in ('chain-blockers', 'post-retry', 'continuation', 'maintenance', 'evolution'):
            run('r977_' + step + '_runner', [sys.executable, ROOT / 'verification/r975/run.py',
                '--step', step, '--out', out, '--android-jar', android])
            continue
        marker = out / 'compiled.sha256'
        if not marker.is_file() or marker.read_text().strip() != source_digest():
            raise SystemExit('SOURCE_CHANGED_OR_NOT_COMPILED: run --step compile first')
        cp = os.pathsep.join(map(str, (out / 'classes', out / 'android-runtime')))
        adapters = sorted((ROOT / 'verification/r974/android').rglob('*.java'))
        adapters += sorted((ROOT / 'verification/r932/android-json').rglob('*.java'))
        adapters += [ROOT / ('verification/r967/host/android/' + s) for s in
                     ('content/ContentValues.java', 'os/SystemClock.java', 'os/CancellationSignal.java')]
        adapters += [ROOT / 'verification/r974/HostProductR974.java']
        adapters += sorted((ROOT / 'verification/r976').glob('*.java'))
        adapters += sorted((ROOT / 'verification/r977').glob('*.java'))
        run('r977_' + step + '_compile', ['javac', '-encoding', 'UTF-8', '-cp', cp,
                                        '-d', out / 'classes', *adapters])
        main_class = {'chain-blockers': 'com.ethk3.novaonepro.ChainBlockerHarnessR976',
                      'post-retry': 'com.ethk3.novaonepro.PostRetryHarnessR976',
                      'continuation': 'com.ethk3.novaonepro.ContinuationHarnessR977',
                      'maintenance': 'com.ethk3.novaonepro.MaintenanceHarnessR977',
                      'evolution': 'com.ethk3.novaonepro.EvolutionIntegrationHarnessR977'}[step]
        run('r977_' + step, ['java', '-cp', cp, main_class,
                            ROOT / 'verification/r967/host/sqlite_bridge.py']
            + ([out / 'evolution_actual.sqlite'] if step == 'evolution' else []))


if __name__ == '__main__':
    main()
