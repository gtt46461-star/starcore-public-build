#!/usr/bin/env python3
"""Run the production observation bridge with the existing real SQLite host adapters."""
import os
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/r930-batch34-sql'
HOST = ROOT / 'build/r925-host/compiled-classes'
ANDROID = Path(os.environ.get('ANDROID_PLATFORM_JAR', ROOT.parent / '08_TOOLCHAIN/android-platform-36/android.jar'))
REPORT = ROOT / 'reports/r931/BRIDGE_REGRESSION.txt'

def main():
    if not (OUT / 'android/content/ContentValues.class').is_file():
        raise SystemExit('R931_BRIDGE_ADAPTERS_MISSING_RUN_R930_SQL_HARNESS_FIRST')
    cp = os.pathsep.join(map(str, [OUT, HOST, ANDROID]))
    subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8',
                    '-cp',cp,'-d',str(OUT),str(ROOT/'verification/r930/R931BridgeRegressionHarness.java')],check=True)
    # Extracted platform classes also support retained APK-shaped platform bundles.
    runtime = OUT / 'platform-classes'
    import zipfile
    with zipfile.ZipFile(ANDROID) as archive:
        for name in archive.namelist():
            if name.endswith('.class'): archive.extract(name, runtime)
    cp = os.pathsep.join(map(str, [OUT, HOST, runtime]))
    result = subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.R931BridgeRegressionHarness',
                             str(ROOT/'verification/r925/sqlite_bridge.py')],capture_output=True,text=True)
    REPORT.parent.mkdir(parents=True,exist_ok=True)
    REPORT.write_text(result.stdout+result.stderr)
    print(result.stdout+result.stderr,end='')
    if result.returncode or 'R931_BRIDGE_REGRESSION_PASS' not in result.stdout.splitlines():
        raise SystemExit(1)

if __name__=='__main__':main()
