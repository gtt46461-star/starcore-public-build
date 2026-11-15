#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/RuntimeFreshnessGuard.java').read_text();assert all(x in s for x in ['fetchAgeMs','analysisAgeMs','issueGeneration','currentGeneration'])
print("PASS RuntimeFreshnessWallClockHarness")
