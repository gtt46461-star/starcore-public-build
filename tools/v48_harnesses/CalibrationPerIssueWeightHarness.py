#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/RankConditionalCalibrationEngine.java').read_text();assert 'TABLE_V48_DIAGNOSTIC_OBSERVATION' in s;assert 'TABLE_OOS_ACTION_LOG' not in s
print("PASS CalibrationPerIssueWeightHarness")
