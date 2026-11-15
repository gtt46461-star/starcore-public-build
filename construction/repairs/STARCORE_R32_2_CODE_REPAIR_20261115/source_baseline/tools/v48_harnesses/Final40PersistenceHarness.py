#!/usr/bin/env python3
from pathlib import Path
s=Path('app/src/main/java/com/ethk3/novaonepro/OosWriteAuditEngine.java').read_text();o=Path('app/src/main/java/com/ethk3/novaonepro/OosActionLogEngine.java').read_text();assert 'inserted + duplicateExisting == requested' in o;assert 'inserted==40||inserted==0' not in s.replace(' ','')
print("PASS Final40PersistenceHarness")
