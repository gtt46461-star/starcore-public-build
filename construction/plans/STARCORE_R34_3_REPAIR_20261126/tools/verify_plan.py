#!/usr/bin/env python3
"""Verify the sealed plan, exact source anchors, cumulative patches and coverage.

This program never declares Android, device or natural-quality acceptance.
"""
import argparse,ast,hashlib,json,re,subprocess,sys,tempfile
from pathlib import Path

BASE='4d1de9d83692635b5c5ce39144fece1c8a31c8ac'
REPORT='3fb241b3181dccd4d149cf91471d86532919f77d943847ff8ce348cce5ec4d86'
def require(ok,msg):
 if not ok:raise ValueError(msg)
def sha(b):return hashlib.sha256(b).hexdigest()
def read(p):return json.loads(p.read_text())
def member(root,name):
 require(isinstance(name,str) and name and not Path(name).is_absolute() and '..' not in Path(name).parts,'UNSAFE_MEMBER:'+str(name))
 p=root/name;require(p.resolve().is_relative_to(root.resolve()) and not p.is_symlink(),'UNSAFE_MEMBER:'+name)
 return p
def git(repo,*args,check=True):return subprocess.run(['git','-C',str(repo),*args],capture_output=True,check=check).stdout
def verify(plan,repo,unsealed=False):
 plan=plan.resolve();checks={}
 if not unsealed:
  manifest=read(plan/'MANIFEST_SHA256.json')
  require(manifest['schema']=='STARCORE_R343_PLAN_MANIFEST_1' and manifest['source_commit']==BASE,'MANIFEST_IDENTITY')
  expected={'MANIFEST_SHA256.json'}
  for row in manifest['files']:
   path=row['path'];p=member(plan,path);require(path not in expected,'DUPLICATE_MEMBER:'+path);expected.add(path)
   require(p.is_file() and p.stat().st_size==row['bytes'] and sha(p.read_bytes())==row['sha256'],'MEMBER_HASH:'+path)
  actual={p.relative_to(plan).as_posix() for p in plan.rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix not in ('.pyc','.pyo')}
  require(expected==actual and len(expected)==manifest['file_count']+1,'MEMBER_SET')
  checks['sealed_members']=manifest['file_count']
 require(sha((plan/'report/ORIGINAL_DIAGNOSTIC_173.zip').read_bytes())==REPORT,'ORIGINAL_REPORT')
 integrity=read(plan/'evidence/input_integrity.json');require(integrity['members_verified']==67 and len(integrity['members'])==67,'REPORT_MEMBER_COUNT')
 checks['report_payload_hashes']=67
 issues=read(plan/'ISSUES.json')['issues']; require(len(issues)==44 and len({q['id'] for q in issues})==44,'ISSUES_44')
 expected_ids={f'R343-{i:03d}' for i in range(1,45)};require({q['id'] for q in issues}==expected_ids,'ISSUE_ID_SET')
 require({q['batch'] for q in issues}=={f'B{i:02d}' for i in range(1,13)},'BATCH_SET')
 for q in issues:
  require(q['cause'] and q['treatment'] and q['acceptance'] and q['source'],'ISSUE_CONTENT:'+q['id'])
  require((plan/'issues'/f"{q['id']}.md").is_file(),'ISSUE_CARD:'+q['id'])
  require(all(s.get('source_sha256') for s in q['source']),'SOURCE_UNBOUND:'+q['id'])
 checks['issues']=44
 metrics=read(plan/'coverage/METRIC_ACTIONS_44.json')['metrics'];require(len(metrics)==44 and len({x['id'] for x in metrics})==44,'METRIC_SET')
 coverage=read(plan/'coverage/CHECK_ACTIONS_96.json')['checks'];require(len(coverage)==96 and len({x['id'] for x in coverage})==96,'CHECK_SET')
 for c in coverage:require(c['related_issues'] and set(c['related_issues'])<=expected_ids,'CHECK_CROSSWALK:'+c['id'])
 require(read(plan/'coverage/object_coverage_165.json')['count']==165,'OBJECT_SET')
 require(read(plan/'coverage/chart_source_mapping_63.json')['count']==63,'CHART_TABLE_SET')
 require(len(read(plan/'coverage/table_counts_801.json')['tables'])==801,'DATABASE_TABLE_SET')
 math=read(plan/'specs/MATHEMATICAL_MECHANISMS.json')
 require(len(math['r26'])==24 and len(math['modern'])==15 and math['sourceCommit']==BASE,'MATHEMATICAL_FAMILY_SET')
 for count in (1832,299):require(len(read(plan/f'inheritance/CURRENT_REPORT_CROSSWALK_{count}.json')['rows'])==count,'REPORT_LEDGER_COUNT')
 require(len(read(plan/'inheritance/R342_ISSUES_57_CURRENT_STATUS.json')['rows'])==57,'OLD_ISSUE_SET')
 require(read(plan/'inheritance/R341_FILE_CARDS_295_AT_173.json')['count']==295,'FILE_INHERITANCE')
 require(len(read(plan/'inheritance/R341_WORK_ORDERS_62_AT_173.json')['mapping'])==62,'OLD_WORK_ORDER_SET')
 require(len(read(plan/'inheritance/R341_ACCEPTANCE_152_AT_173.json')['cases'])==152,'OLD_ACCEPTANCE_SET')
 cases=read(plan/'ACCEPTANCE_MATRIX.json')['cases'];engineering=[c for c in cases if c['level']=='ENGINEERING']
 require(len(cases)==138 and len(engineering)==132,'ACCEPTANCE_COUNT')
 require({c['id'] for c in engineering}=={x+'-'+suffix for x in expected_ids for suffix in ('POS','NEG','KEEP')},'ENGINEERING_ACCEPTANCE_SET')
 require(all(c['assertions'] for c in cases),'EMPTY_ASSERTIONS')
 checks['coverage']={'checks':96,'metrics':44,'objects':165,'charts_and_tables':63,'old_issues':57,'report_requirements':1832,'report_cards':299,'engineering_scenarios':132,'device_natural_contracts':6}
 patchset=read(plan/'PATCHSET.json');require(not patchset['warnings'],'PATCH_WARNINGS:'+repr(patchset['warnings']))
 require(patchset['order']==[g['id'] for g in patchset['groups']] and len(set(patchset['order']))==len(patchset['order']),'PATCH_ORDER')
 require(set(patchset['order'])=={'field_binding','runtime','prepared_storage','coverage','research_core','rolling','creation','charts','delivery'},'PATCH_GROUP_SET')
 states={};first={};steps=0
 for group in patchset['groups']:
  for row in group['files']:
   name=row['path'];p=member(plan,group['full_source_directory']+'/'+name)
   require(p.is_file() and sha(p.read_bytes())==row['after_sha256'],'AFTER_HASH:'+name)
   if name in states:require(states[name]==row['before_sha256'],'CUMULATIVE_BEFORE:'+name)
   else:first[name]=row['before_sha256']
   if row['before_sha256'] is not None:
    before=member(plan,f"patches/{group['id']}/before/{name}")
    require(before.is_file() and sha(before.read_bytes())==row['before_sha256'],'BEFORE_HASH:'+name)
   states[name]=row['after_sha256'];steps+=1
   if name.endswith('.py'):ast.parse(p.read_text(),filename=name)
 cards=read(plan/'FILE_CARDS.json');require(cards['modification_steps']==steps and cards['unique_files']==len(states),'FILE_CARD_COUNTS')
 checks['patches']={'groups':len(patchset['groups']),'file_steps':steps,'unique_files':len(states)}
 links=[]
 for md in plan.rglob('*.md'):
  if 'before' in md.parts or 'after' in md.parts:continue
  prose=re.sub(r'```.*?```','',md.read_text(),flags=re.S)
  prose=re.sub(r'`[^`]*`','',prose)
  for destination in re.findall(r'\]\(([^)]+)\)',prose):
   if re.match(r'^(https?://|mailto:|#)',destination):continue
   target=destination.split('#',1)[0]
   if target and not (md.parent/target).exists():links.append([str(md.relative_to(plan)),target])
 require(not links,'BROKEN_LOCAL_LINKS:'+repr(links[:20]));checks['markdown_links']='PASS'
 if repo:
  require(git(repo,'rev-parse',BASE).decode().strip()==BASE,'SOURCE_BASELINE_OBJECT')
  cache={}
  def baseline(path):
   if path not in cache:
    r=subprocess.run(['git','-C',str(repo),'show',BASE+':'+path],capture_output=True)
    cache[path]=r.stdout if r.returncode==0 else None
   return cache[path]
  for path,h in first.items():
   b=baseline(path);require((sha(b) if b is not None else None)==h,'BASELINE_BEFORE:'+path)
  anchors=0
  for q in issues:
   for s in q['source']:
    b=baseline(s['path']);require(b is not None and sha(b)==s['source_sha256'],'ISSUE_SOURCE_HASH:'+s['path'])
    if s['anchor']:require(s['anchor'] in b.decode(errors='replace'),'ISSUE_SOURCE_ANCHOR:'+s['path']+':'+s['anchor'])
    anchors+=1
  with tempfile.TemporaryDirectory(prefix='field173-patch-check-') as tmp:
   tmp=Path(tmp)
   subprocess.run(['git','init','-q',str(tmp)],check=True)
   for path in first:
    b=baseline(path)
    if b is not None:
     p=tmp/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(b)
   for group in patchset['groups']:
    patch=plan/group['patch']
    subprocess.run(['git','-C',str(tmp),'apply','--check',str(patch)],check=True,capture_output=True)
    subprocess.run(['git','-C',str(tmp),'apply',str(patch)],check=True,capture_output=True)
    for row in group['files']:require(sha((tmp/row['path']).read_bytes())==row['after_sha256'],'PATCH_AFTER_DRIFT:'+row['path'])
  checks['baseline_source_anchors']=anchors;checks['cumulative_git_apply']='PASS'
 checks['status']='PLAN_INTEGRITY_AND_SCOPED_HOST_CHECKS_PASSED'
 checks['android']='NOT_RUN';checks['physical_device']='NOT_RUN';checks['natural_quality']='NOT_PROVEN'
 return checks

def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--plan',type=Path,required=True);p.add_argument('--source-repo',type=Path);p.add_argument('--out',type=Path);p.add_argument('--allow-unsealed',action='store_true',help='authoring only; never release acceptance')
 a=p.parse_args()
 if a.out:require(not a.out.resolve().is_relative_to(a.plan.resolve()),'OUTPUT_MUST_BE_OUTSIDE_SEALED_PLAN')
 try:
  result=verify(a.plan,a.source_repo,a.allow_unsealed);status=0
 except Exception as exc:result={'status':'REJECTED','error':str(exc),'type':type(exc).__name__};status=1
 body=json.dumps(result,ensure_ascii=False,indent=2)+'\n'
 if a.out:a.out.parent.mkdir(parents=True,exist_ok=True);a.out.write_text(body)
 print(body,end='');sys.exit(status)

if __name__=='__main__':main()
