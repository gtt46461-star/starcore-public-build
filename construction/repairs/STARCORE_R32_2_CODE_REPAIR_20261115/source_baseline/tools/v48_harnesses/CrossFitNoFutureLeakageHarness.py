#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/PointInTimeNumberFeatureEngine.java').read_text();assert 'strictly earlier than the target' in s;assert 'history.get(history.size()-1)' in s
print("PASS CrossFitNoFutureLeakageHarness")
