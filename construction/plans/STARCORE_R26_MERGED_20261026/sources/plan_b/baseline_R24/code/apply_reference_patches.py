"""Explicit, SHA-guarded two-file reference replacement; NOT a full R24 installer."""
from pathlib import Path
import argparse,hashlib,json,shutil,datetime
p=Path(__file__).resolve().parents[1]
a=argparse.ArgumentParser();a.add_argument('--repo',required=True,type=Path);a.add_argument('--apply',action='store_true');x=a.parse_args()
items=json.loads((p/'patches/preconditions.json').read_text());work=[]
for row in items:
 dst=x.repo/row['file'];src=p/'patches/after'/row['file']
 if not dst.is_file() or hashlib.sha256(dst.read_bytes()).hexdigest()!=row['before_sha256']:raise SystemExit('PRECONDITION_FAILED: '+row['file'])
 if hashlib.sha256(src.read_bytes()).hexdigest()!=row['after_sha256']:raise SystemExit('PATCH_PAYLOAD_CORRUPT: '+row['file'])
 work.append((dst,src,row))
print('All two-file SHA preconditions match. Android validation still required.')
if x.apply:
 backup=x.repo/('r24_patch_backup_'+datetime.datetime.now().strftime('%Y%m%d_%H%M%S_%f'));backup.mkdir()
 try:
  for dst,src,row in work:
   b=backup/row['file'];b.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(dst,b);tmp=dst.with_suffix(dst.suffix+'.r24tmp');shutil.copy2(src,tmp);tmp.replace(dst)
 except Exception:
  for dst,src,row in work:
   b=backup/row['file']
   if b.exists():shutil.copy2(b,dst)
  raise
 print('APPLIED reference fixes; backup='+str(backup))
else:print('CHECK_ONLY: pass --apply to modify the checkout')
