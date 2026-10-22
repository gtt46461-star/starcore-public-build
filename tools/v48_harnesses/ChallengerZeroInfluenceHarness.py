#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/CrossFittedNumberModelEngine.java').read_text();r=Path('app/src/main/java/com/ethk3/novaonepro/RealtimeCausalProbabilityEngine.java').read_text();assert 's.champion?AppConfig.V47_CHAMPION_BLEND:0.0d' in s;assert 'LiveWeight=0' in r
print("PASS ChallengerZeroInfluenceHarness")
