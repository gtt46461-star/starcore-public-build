#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/AppConfig.java').read_text();b=Path('app/src/main/java/com/ethk3/novaonepro/BuildIdentity.java').read_text();assert 'ACTIVE_QUALITY_BUILD_COHORT = V48BuildManifest.COHORT_ID' in s;assert 'V48BuildManifest.exact' in b;assert 'BUILD_IDENTITY_ALLOW_AIDE_VERSION_WARNING = false' in s
print("PASS BuildIdentityExactMatchHarness")
