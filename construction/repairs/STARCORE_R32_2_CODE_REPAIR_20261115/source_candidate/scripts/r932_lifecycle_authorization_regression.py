#!/usr/bin/env python3
"""Run the real Java lifecycle against the retained real-SQLite bridge.

Prerequisites: r925_compile_host.sh, r930_batch34_sql_harness.py and
r932_model_bundle_harness.py. This is engineering evidence, not device/Future proof.
"""
import os
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/r932-lifecycle-authorization'
OUT.mkdir(parents=True, exist_ok=True)
platform = Path(os.environ.get('ANDROID_PLATFORM_JAR', ROOT.parent / '08_TOOLCHAIN/android-platform-36/android.jar'))
parts = [ROOT / 'build/r932-model-bundle', ROOT / 'build/r930-batch34-sql', ROOT / 'build/r925-host/compiled-classes', platform]
for path in parts:
    if not path.exists():
        raise SystemExit('Missing required compiled production dependency: ' + str(path))
classpath = os.pathsep.join(map(str, parts))
source = ROOT / 'verification/r932/LifecycleAuthorizationHarnessR932.java'
subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main', '-encoding', 'UTF-8', '-cp', classpath, '-d', str(OUT), str(source)], check=True)
command = ['java', '-cp', str(OUT) + os.pathsep + classpath, 'com.ethk3.novaonepro.LifecycleAuthorizationHarnessR932', str(ROOT / 'verification/r925/sqlite_bridge.py')]
result = subprocess.run(command, capture_output=True, text=True)
text = result.stdout + result.stderr
report = ROOT / 'reports/r932/LIFECYCLE_AUTHORIZATION.txt'
report.parent.mkdir(parents=True, exist_ok=True)
report.write_text(text, encoding='utf-8')
print(text, end='')
if result.returncode != 0 or 'R932_LIFECYCLE_AUTHORIZATION_PASS|' not in result.stdout:
    raise SystemExit(result.returncode or 1)
