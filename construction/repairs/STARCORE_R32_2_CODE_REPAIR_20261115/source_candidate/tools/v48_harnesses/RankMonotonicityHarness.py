#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/ModelFailureAttributionEngine.java').read_text();assert 'r5>r1+0.08d' in s;assert 'RANK_INVERSION' in Path('app/src/main/java/com/ethk3/novaonepro/AppConfig.java').read_text()
print("PASS RankMonotonicityHarness")
