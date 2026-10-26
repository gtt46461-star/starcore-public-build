"""Package content verifier; does not certify Android or strategy performance."""
import hashlib,json,sys,zipfile
from pathlib import Path
root=Path(sys.argv[1])
m=json.loads((root/'MANIFEST_SHA256_R25.json').read_text())
bad=[]
for r in m['files']:
    p=root/r['path']
    if not p.is_file() or p.stat().st_size!=r['bytes'] or hashlib.sha256(p.read_bytes()).hexdigest()!=r['sha256']:bad.append(r['path'])
if bad:raise SystemExit('HASH_MISMATCH '+str(bad))
u=root/'upgrade_r25'
assert len(json.loads((u/'registries/cards_R25_001_032.json').read_text()))==32
assert len(json.loads((u/'registries/unified_80_cards.json').read_text()))==80
assert len(json.loads((u/'registries/report_contract_r25.json').read_text())['sections'])==36
print(json.dumps({'verified_files':len(m['files']),'scope':'PACKAGE_INTEGRITY_ONLY','status':'PASS'}))
