#!/usr/bin/env python3
"""Hash guarded, idempotent P1 runtime contribution; --apply writes full files."""
import argparse,hashlib,json,os,tempfile
from pathlib import Path
cli=argparse.ArgumentParser();cli.add_argument('--repo',required=True,type=Path);cli.add_argument('--apply',action='store_true');args=cli.parse_args()
here=Path(__file__).resolve().parent;repo=args.repo.resolve();records=json.loads((here/'FILES.json').read_text())['files'];pending=[]
for row in records:
    dest=repo/row['path'];source=here/'full_source'/row['path'];body=source.read_bytes()
    if hashlib.sha256(body).hexdigest()!=row['afterSha256']:raise SystemExit('PAYLOAD_HASH_MISMATCH '+row['path'])
    before=hashlib.sha256(dest.read_bytes()).hexdigest() if dest.exists() else None
    if before==row['afterSha256']:continue
    if before!=row['beforeSha256']:raise SystemExit('SOURCE_DIVERGED '+row['path'])
    pending.append((dest,body))
if args.apply:
    for dest,body in pending:
        dest.parent.mkdir(parents=True,exist_ok=True)
        fd,tmp=tempfile.mkstemp(prefix='.r32-runtime-',dir=dest.parent)
        try:
            with os.fdopen(fd,'wb') as f:f.write(body);f.flush();os.fsync(f.fileno())
            os.chmod(tmp,dest.stat().st_mode&0o777 if dest.exists() else 0o644)
            os.replace(tmp,dest)
        finally:
            if os.path.exists(tmp):os.unlink(tmp)
print(json.dumps({'status':'APPLIED' if args.apply else 'CHECK_PASS','filesToWrite':len(pending),'scope':'source overlay; does not claim Android runtime success'}))
