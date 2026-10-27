"""Verify the files actually shipped; archived inputs retain their own old manifests."""
from pathlib import Path
import hashlib,json
ROOT=Path(__file__).resolve().parents[1]
manifest=json.loads((ROOT/'MANIFEST_SHA256.json').read_text())
missing=[];bad=[];checked=0
for row in manifest['files']:
    p=ROOT/row['path']
    if not p.exists():missing.append(row['path']);continue
    data=p.read_bytes()
    if len(data)!=row['bytes'] or hashlib.sha256(data).hexdigest()!=row['sha256']:bad.append(row['path'])
    checked+=1
scope=ROOT/'DOWNLOAD_SCOPE.json'
allowed=set(json.loads(scope.read_text())['omitted_paths']) if scope.exists() else set()
unexpected_missing=[x for x in missing if x not in allowed]
if bad or unexpected_missing:raise SystemExit(json.dumps({'bad':bad,'unexpected_missing':unexpected_missing},ensure_ascii=False))
print(json.dumps({'verified':checked,'declared_omitted':len(missing),'scope':'DIRECT_CONSTRUCTION_DOWNLOAD' if missing else 'FULL_REPOSITORY_PLAN','all_shipped_hashes_match':True}))
