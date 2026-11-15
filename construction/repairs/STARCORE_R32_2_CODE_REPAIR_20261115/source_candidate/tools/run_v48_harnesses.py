#!/usr/bin/env python3
from pathlib import Path
import runpy
files=sorted(Path('tools/v48_harnesses').glob('*Harness.py'))
assert len(files)==15, len(files)
for path in files: runpy.run_path(str(path),run_name='__main__')
print('V48_HARNESSES_PASS=15/15')
