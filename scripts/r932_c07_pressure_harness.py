#!/usr/bin/env python3
import os
import subprocess
from pathlib import Path

R = Path(__file__).resolve().parents[1]
O = R / 'build/r932-c07-pressure'
O.mkdir(parents=True, exist_ok=True)
H = R / 'build/r925-host/compiled-classes'
platform = Path(os.environ['ANDROID_PLATFORM_JAR'])

files = [
    R/'verification/r925/stubs/android/content/ContentValues.java',
    R/'verification/r925/stubs/android/database/sqlite/SQLiteDatabase.java',
    R/'verification/r925/stubs/com/ethk3/novaonepro/DatabaseCoordinator.java',
    R/'verification/r925/stubs/com/ethk3/novaonepro/DbStore.java',
    R/'verification/r932/GlobalSchedulePressureHarnessR932.java',
]
cp = os.pathsep.join([str(H), str(platform)])
subprocess.run([
    'java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8',
    '-cp',cp,'-d',str(O),*map(str,files)
], check=True)

cp = os.pathsep.join([str(O), str(H), str(platform)])
p = subprocess.run([
    'java','-cp',cp,'com.ethk3.novaonepro.GlobalSchedulePressureHarnessR932',
    str(R/'verification/r925/sqlite_bridge.py')
], capture_output=True, text=True)
report = R/'reports/r932/C07_PRESSURE.txt'
report.parent.mkdir(parents=True, exist_ok=True)
report.write_text(p.stdout+p.stderr, encoding='utf-8')
print(p.stdout+p.stderr, end='')
if p.returncode or 'R932_C07_PRESSURE_PASS' not in p.stdout:
    raise SystemExit(1)
