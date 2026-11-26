#!/usr/bin/env python3
"""Apply exact field173 patch after checking every before hash. Default is check only."""
import argparse,hashlib,json,subprocess,sys
from pathlib import Path
from verify_plan import BASE,require,read,member,verify

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest() if path.is_file() else None
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--plan',type=Path,required=True);p.add_argument('--repo',type=Path,required=True);p.add_argument('--group',required=True);p.add_argument('--apply',action='store_true')
 a=p.parse_args();plan=a.plan.resolve();repo=a.repo.resolve()
 # Full seal and source checks precede every mutation. Does not require a clean
 # entire tree: unrelated user edits are preserved; exact touched files must match.
 verify(plan,repo)
 subprocess.run(['git','-C',str(repo),'merge-base','--is-ancestor',BASE,'HEAD'],check=True,capture_output=True)
 ps=read(plan/'PATCHSET.json');selected=[g for g in ps['groups'] if g['id']==a.group];require(len(selected)==1,'UNKNOWN_GROUP')
 group=selected[0];states=[]
 for row in group['files']:
  path=member(repo,row['path']);value=digest(path)
  require(value in (row['before_sha256'],row['after_sha256']),'LOCAL_BEFORE_MISMATCH:'+row['path'])
  states.append('AFTER' if value==row['after_sha256'] else 'BEFORE')
 require(len(set(states))==1,'PARTIAL_GROUP_REQUIRES_REVIEW_DO_NOT_OVERWRITE')
 if states[0]=='AFTER':print('ALREADY_APPLIED_EXACT:'+a.group);return
 # All earlier groups must be installed or superseded by a later exact state.
 prior={}
 for g in ps['groups']:
  if g['id']==a.group:break
  for row in g['files']:prior[row['path']]=row['after_sha256']
 current_paths={r['path']:r for r in group['files']}
 for name,h in prior.items():
  expected=current_paths[name]['before_sha256'] if name in current_paths else h
  require(digest(member(repo,name))==expected,'PRIOR_GROUP_NOT_APPLIED:'+name)
 patch=member(plan,group['patch'])
 subprocess.run(['git','-C',str(repo),'apply','--check',str(patch)],check=True)
 if a.apply:
  subprocess.run(['git','-C',str(repo),'apply',str(patch)],check=True)
  for row in group['files']:require(digest(member(repo,row['path']))==row['after_sha256'],'APPLY_AFTER_HASH:'+row['path'])
 print(('APPLIED_EXACT:' if a.apply else 'CHECK_ONLY_READY:')+a.group)

if __name__=='__main__':
 try:main()
 except Exception as exc:print('REJECTED:'+str(exc),file=sys.stderr);sys.exit(1)
