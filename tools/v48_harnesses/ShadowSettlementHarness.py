#!/usr/bin/env python3
from pathlib import Path
a=Path('app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text();s=Path('app/src/main/java/com/ethk3/novaonepro/ShadowObservationSettlementEngine.java').read_text();assert 'ShadowObservationSettlementEngine.settleByDraw' in a;assert 'TABLE_V48_DIAGNOSTIC_OBSERVATION' in s
print("PASS ShadowSettlementHarness")
