from pathlib import Path
import hashlib,json,sys
p=Path(__file__).resolve().parent
m=json.loads((p/'MANIFEST_SHA256.json').read_text());bad=[]
for r in m['files']:
 f=p/r['path']
 if not f.is_file() or hashlib.sha256(f.read_bytes()).hexdigest()!=r['sha256']:bad.append(r['path'])
print(json.dumps({'status':'PASS' if not bad else 'FAIL','verified_files':len(m['files']),'mismatches':bad},ensure_ascii=False,indent=2))
sys.exit(bool(bad))
