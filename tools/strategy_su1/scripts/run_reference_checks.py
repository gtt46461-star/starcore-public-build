#!/usr/bin/env python3
"""Run all supplied reference checks. No network or production database access."""
from pathlib import Path
import datetime
import json
import os
import platform
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
records = []

def run(name, args, cwd=ROOT):
    started = time.monotonic()
    result = subprocess.run(args, cwd=cwd, text=True, capture_output=True)
    entry = dict(name=name, command=args, returncode=result.returncode,
                 seconds=round(time.monotonic()-started, 3),
                 stdout=result.stdout, stderr=result.stderr)
    records.append(entry)
    print(f"{name}: {'PASS' if result.returncode == 0 else 'FAIL'}", flush=True)
    if result.returncode:
        print(result.stdout + result.stderr)
    return result.returncode == 0

def main():
    checks = ROOT / 'verification'
    checks.mkdir(exist_ok=True)
    classes = checks / 'all_java_classes'
    classes.mkdir(exist_ok=True)
    okay = run('python_reference_suites', [sys.executable, '-m', 'unittest',
        'discover', '-s', 'reference/python', '-p', 'test_*.py', '-v'])
    replay = run('replay_engine_suite', [sys.executable, '-m', 'unittest',
        'discover', '-s', 'reference/replay', '-p', 'test_*.py', '-v'])
    okay = okay and replay
    sources = sorted(str(p) for p in (ROOT/'reference/java').rglob('*.java'))
    compiled = run('java_release8_compile', ['java','-m',
        'jdk.compiler/com.sun.tools.javac.Main','--release','8','-Xlint:all',
        '-d',str(classes)] + sources)
    okay = okay and compiled
    if compiled:
        for name in ('TypedProgramTest', 'PairedEvidenceTest', 'NullBaselineTest', 'ProgramTreeSU1Test'):
            passed = run(name, ['java','-cp',str(classes),
                'com.ethk3.novaonepro.strategyv2.'+name])
            okay = okay and passed
    report = dict(status='PASS' if okay else 'FAIL',
        generated_at=datetime.datetime.now(datetime.timezone.utc).isoformat(),
        scope='Host reference code and SQLite only; no Android integration or live predictive claim',
        python=platform.python_version(), platform=platform.platform(),
        baseline_sha='9ec590cafbd7cbcd1265a11b63a59df53ba78fcd', checks=records)
    (checks/'reference_run.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    return 0 if okay else 1

if __name__ == '__main__':
    sys.exit(main())
