"""Verify final immutable file hashes. Re-run before any intentional edits."""
from pathlib import Path
import hashlib,json,sys
p=Path(__file__).resolve().parents[1]
m=json.loads((p/'MANIFEST_SHA256.json').read_text(encoding='utf-8'))
bad=[]
for x in m['files']:
 f=p/x['path']
 if not f.is_file() or f.stat().st_size!=x['bytes'] or hashlib.sha256(f.read_bytes()).hexdigest()!=x['sha256']:bad.append(x['path'])
print(json.dumps({'checked':len(m['files']),'mismatches':bad},ensure_ascii=False,indent=2))
sys.exit(bool(bad))
