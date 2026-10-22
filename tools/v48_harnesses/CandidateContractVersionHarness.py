#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/V48BuildManifest.java').read_text();assert all(x in s for x in ['MODEL_VERSION','CALIBRATION_VERSION','RANKING_VERSION','OOS_VERSION'])
print("PASS CandidateContractVersionHarness")
