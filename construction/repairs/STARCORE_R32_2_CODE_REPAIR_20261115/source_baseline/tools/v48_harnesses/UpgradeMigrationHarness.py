#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/BuildCohortMigrationEngine.java').read_text();assert 'READ_ONLY_EXCLUDED_FROM_V48_MODEL' in s;assert 'DELETE FROM' not in s
print("PASS UpgradeMigrationHarness")
