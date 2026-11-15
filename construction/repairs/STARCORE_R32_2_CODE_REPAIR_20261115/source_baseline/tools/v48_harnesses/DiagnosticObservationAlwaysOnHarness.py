#!/usr/bin/env python3
from pathlib import Path
a=Path('app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text();d=Path('app/src/main/java/com/ethk3/novaonepro/DiagnosticObservationEngine.java').read_text();assert a.index('DiagnosticObservationEngine.persist') < a.index('before_decision_gate');assert 'Never creates a Trade' in d
print("PASS DiagnosticObservationAlwaysOnHarness")
