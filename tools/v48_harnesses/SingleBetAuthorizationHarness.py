#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/V48PipelineLedger.java').read_text();d=Path('app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java').read_text();assert 'authorized <= 1' in s;assert d.count('tryV28HonestUtilityAuthorizedDecision')>=1
print("PASS SingleBetAuthorizationHarness")
