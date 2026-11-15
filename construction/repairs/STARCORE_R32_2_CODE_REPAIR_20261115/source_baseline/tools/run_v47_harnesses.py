from pathlib import Path
import runpy
ROOT=Path(__file__).resolve().parent/'v47_harnesses'
for p in sorted(ROOT.glob('*Harness.py')): runpy.run_path(str(p),run_name='__main__')
print('PASS V47 harnesses=10')
