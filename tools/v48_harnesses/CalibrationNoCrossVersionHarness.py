#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/RankConditionalCalibrationEngine.java').read_text();assert all(x in s for x in ['build_id=?','cohort_id=?','schema_version=?','pipeline_version=?','model_version=?'])
print("PASS CalibrationNoCrossVersionHarness")
