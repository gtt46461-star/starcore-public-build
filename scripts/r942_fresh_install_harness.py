#!/usr/bin/env python3
"""Run the real production schema scheduler; adapt Android boundaries, never migration logic."""
import os
import subprocess
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'build/r942-schema'
OUT.mkdir(parents=True, exist_ok=True)
toolchain = Path(os.environ['STARCORE_TOOLCHAIN_ROOT'])
platform = toolchain / 'android-platform-36/android.jar'
host = ROOT / 'build/r925-host/compiled-classes'
adapters = ROOT / 'build/r930-batch34-sql'
cp = os.pathsep.join(map(str, [OUT, adapters, host, platform]))
files = list((ROOT / 'verification/r932/android-json').rglob('*.java'))
files += [ROOT / 'verification/r925/stubs/com/ethk3/novaonepro/DatabaseCoordinator.java',
          ROOT / 'verification/r942/FreshInstallSchemaHarnessR942.java']
subprocess.run(['java', '-m', 'jdk.compiler/com.sun.tools.javac.Main', '-encoding', 'UTF-8',
                '-cp', cp, '-d', str(OUT), *map(str, files)], check=True)
subprocess.run(['java', '-cp', cp, 'com.ethk3.novaonepro.FreshInstallSchemaHarnessR942',
                str(ROOT / 'verification/r925/sqlite_bridge.py')], check=True)
