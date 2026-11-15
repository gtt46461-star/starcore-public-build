#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/AppConfig.java').read_text();assert 'V48_FULL_SCAN_CANDIDATE_COUNT = 1200' in s;assert 'V48_PROBABILITY_CANDIDATE_COUNT = 60' in s;assert 'V48_FORMAL_CANDIDATE_COUNT = 40' in s;assert 'V48_UI_DISPLAY_CANDIDATE_COUNT = 12' in s
print("PASS Pipeline1200To60To40Harness")
