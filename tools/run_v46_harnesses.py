#!/usr/bin/env python3
from pathlib import Path
import runpy
base=Path(__file__).resolve().parent/'v46_harnesses'
files=sorted(base.glob('*Harness.py'))
for f in files: runpy.run_path(str(f),run_name='__main__')
print('PASS ALL_V46_HARNESSES count=%d'%len(files))
