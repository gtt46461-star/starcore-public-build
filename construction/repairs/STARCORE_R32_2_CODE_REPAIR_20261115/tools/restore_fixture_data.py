#!/usr/bin/env python3
"""Restore unchanged historical numerical fixtures from their exact Git objects."""
import argparse,json,pathlib,subprocess,hashlib,os,tempfile
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--package',type=pathlib.Path,default=pathlib.Path(__file__).resolve().parents[1]);p.add_argument('--repo',type=pathlib.Path,required=True);p.add_argument('--write',action='store_true');a=p.parse_args();repo=a.repo.resolve();rows=json.loads((a.package/'contracts/HISTORICAL_FIXTURES_IN_REPOSITORY.json').read_text());ready=[]
for r in rows:
 rel=pathlib.PurePosixPath(r['path'])
 if rel.is_absolute() or '..' in rel.parts:raise ValueError('unsafe fixture path')
 target=repo.joinpath(*rel.parts)
 if not target.resolve().is_relative_to(repo):raise ValueError('fixture symlink escape')
 if target.exists():
  b=target.read_bytes()
  if len(b)!=r['bytes'] or hashlib.sha256(b).hexdigest()!=r['sha256']:raise ValueError('existing fixture drift '+r['path'])
  continue
 b=subprocess.check_output(['git','-C',str(repo),'show',r['source_head']+':'+r['path']])
 if len(b)!=r['bytes'] or hashlib.sha256(b).hexdigest()!=r['sha256']:raise ValueError('Git fixture identity mismatch '+r['path'])
 ready.append((target,b))
if a.write:
 for target,b in ready:
  target.parent.mkdir(parents=True,exist_ok=True);fd,tmp=tempfile.mkstemp(dir=target.parent)
  try:
   with os.fdopen(fd,'wb') as f:f.write(b);f.flush();os.fsync(f.fileno())
   os.replace(tmp,target)
  finally:
   if os.path.exists(tmp):os.unlink(tmp)
print(json.dumps({'checked':len(rows),'available_from_exact_git':len(ready),'written':len(ready) if a.write else 0}))
