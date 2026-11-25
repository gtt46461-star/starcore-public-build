#!/usr/bin/env python3
"""Verify this construction plan and exact source linkage. Never certifies APK/device/quality."""
import argparse,collections,csv,hashlib,json,re,subprocess,sys
from pathlib import Path

COMMIT='eb8f82a8bda8db22f1f122095217132f07053bcb'
SCHEMA='R342_PLAN_VERIFICATION_1'
def read(p):return json.loads(p.read_text(encoding='utf-8'))
def rows(x,keys=('rows','items','checks','metrics','directions','layers')):
 if isinstance(x,list):return x
 for key in keys:
  if isinstance(x.get(key),list):return x[key]
 raise ValueError('Unrecognized row collection: '+str(list(x)))
def sha(p):return hashlib.sha256(p.read_bytes()).hexdigest()

def verify(plan,source=None,skip_manifest=False):
 errors=[];checks=[]
 def check(name,condition,detail=None):
  checks.append({'check':name,'passed':bool(condition),'detail':detail})
  if not condition:errors.append(name)
 identity=read(plan/'control/PLAN_IDENTITY.json')
 check('identity_source_baseline',identity['source_baseline']==COMMIT)
 check('no_app_or_natural_claim',identity['apk_delivered_by_this_task'] is False and identity['natural_quality_proven'] is False)
 issues=read(plan/'ISSUES.json')['issues'];ids={x['id'] for x in issues};origin={x['origin_id']:x['id'] for x in issues}
 check('57_unique_issues',len(issues)==len(ids)==len(origin)==57)
 check('stable_sequential_numbers',[x['number'] for x in issues]==list(range(1,58)))
 batch=read(plan/'WORK_ORDERS.json')['batches'];bids={x['id'] for x in batch}
 check('12_unique_batches',len(batch)==len(bids)==12)
 check('every_issue_one_batch',collections.Counter(i for b in batch for i in b['issue_ids'])==collections.Counter(ids))
 finished=set()
 for x in issues:
  deps=x['depends_on']
  check('dependency_before_'+x['id'],set(deps)<=finished,{'dependencies':deps})
  finished.add(x['id'])
  for field in ['confirmed_causes','implementation_steps','files','preserve','acceptance','mainline_integration','evidence']:
   check('complete_'+x['id']+'_'+field,bool(x.get(field)))
  check('unimplemented_status_'+x['id'],x['app_implementation_status']=='NOT_IMPLEMENTED_BY_THIS_PLANNING_TASK' and x['device_acceptance_status']=='NOT_RUN_IN_THIS_TASK')
  check('individual_card_'+x['id'],(plan/'issues'/f"{x['id']}.md").is_file())
  check('batch_matches_'+x['id'],x['batch'] in bids and any(b['id']==x['batch'] and x['id'] in b['issue_ids'] for b in batch))
 check('topological_batch_order',[x['batch'] for x in issues]==sorted(x['batch'] for x in issues))
 for no,b in enumerate(batch):
  check('batch_dependency_'+b['id'],b['depends_on']==([] if no==0 else [batch[no-1]['id']]))
  check('batch_count_'+b['id'],b['issue_count']==len(b['issue_ids']))

 files=read(plan/'FILE_CARDS.json')['files']
 check('file_paths_unique',len(files)==len({x['path'] for x in files}))
 allpaths={f['path'] for x in issues for f in x['files']}
 check('all_issue_files_in_matrix',allpaths=={x['path'] for x in files})
 planned={x['path'] for x in files if not x['exists_at_baseline']}
 known_new={'app/src/main/java/com/ethk3/novaonepro/strategyv2/TypedGenerationRuleR342.java','release/r342_plan_binding.json','tools/r342/build_identity.py',
 'construction/plans/STARCORE_R34_2_REPAIR_20261125/FILE_CARDS.json','construction/plans/STARCORE_R34_2_REPAIR_20261125/control/PLAN_IDENTITY.json'}
 known_new.add('tools/r342/test_b01_contracts.py')
 known_new.update(f'app/src/androidTest/java/com/ethk3/novaonepro/r342/R342B{i:02d}ContractTest.java' for i in range(2,13))
 check('explicit_planned_new_paths',planned<=known_new,sorted(planned))
 forbidden=['ACTUAL_RELEASE_WORKFLOW','NEW_TYPED_RULE_IMPLEMENTATION_UNDER_STRATEGYV2']
 for f in files:
  check('no_placeholder_'+f['path'],not any(v in f['path'] for v in forbidden))
  check('file_batch_order_'+f['path'],f['batch_order']==sorted(set(f['batch_order'])))
  check('file_links_issues_'+f['path'],set(f['issue_ids'])<=ids)
 if source:
  tree={}
  data=subprocess.check_output(['git','-C',str(source),'ls-tree','-r',COMMIT],text=True)
  for line in data.splitlines():
   meta,path=line.split('\t',1);tree[path]=meta.split()[2]
  for f in files:
   if f['exists_at_baseline']:
    check('baseline_blob_'+f['path'],tree.get(f['path'])==f['git_blob_before'])
   else:check('new_absent_'+f['path'],f['path'] not in tree)
 else:
  checks.append({'check':'exact_source_git_tree','passed':None,'detail':'NOT_RUN: pass --source-repo with the exact commit available; no application test implied.'})

 old=read(plan/'inheritance/R341_WORK_ORDER_CROSSWALK_62.json')['mapping']
 oldcases=read(plan/'inheritance/R341_ACCEPTANCE_CROSSWALK_152.json')['cases']
 delta=read(plan/'inheritance/R341_FILE_DELTA_295.json')
 check('62_inherited_work_orders',len(old)==len({x['old_issue_id'] for x in old})==62)
 check('152_inherited_acceptance',len(oldcases)==len({x['old_case_id'] for x in oldcases})==152)
 check('old_cases_not_run',all(x['current_execution']=='NOT_RUN_IN_THIS_AUDIT' for x in oldcases))
 caseids={x['old_case_id'] for x in oldcases}
 check('all_old_issue_cases_preserved',all(x['inherited_acceptance_ids'] and set(x['inherited_acceptance_ids'])<=caseids for x in old))
 check('295_inherited_file_cards',len(delta)==295 and len({x['path'] for x in delta})==294)
 check('inherited_alias_is_explicit',[(k,v) for k,v in collections.Counter(x['path'] for x in delta).items() if v>1]==[('tools/r341/verify_integrated_chain.py',2)])
 check('inherited_file_delta_counts',collections.Counter(x['state'] for x in delta)=={'EXISTING_IDENTICAL':127,'MODIFIED':122,'ADDED':46},dict(collections.Counter(x['state'] for x in delta)))
 for name,count,key in [('REQUIREMENTS_1832.csv',1832,'requirement_id'),('FILE_CARDS_299.csv',299,'card_id')]:
  path=plan/'inheritance/report_originals'/name
  with path.open(encoding='utf-8-sig',newline='') as f: original=list(csv.DictReader(f))
  mapped=read(plan/'inheritance'/name.replace('.csv','_CROSSWALK.json'))
  check('original_rows_'+name,len(original)==len({x[key] for x in original})==count)
  check('original_hash_'+name,sha(path)==mapped['source_sha256'])
  check('all_rows_inherited_'+name,[x[key] for x in original]==[x['id'] for x in mapped['rows']])
  check('row_states_preserved_'+name,all(a.get('current_status','')==b['original_current_status'] for a,b in zip(original,mapped['rows'])))

 newcases=read(plan/'ACCEPTANCE_MATRIX.json')['cases']
 check('57_new_acceptance_contracts',len(newcases)==57 and {x['issue_id'] for x in newcases}==ids)
 check('new_contracts_positive_negative',all(x['positive_scenario'] and x['negative_scenario'] and x['assertions'] for x in newcases))
 check('new_contracts_not_executed',all(x['execution_status']=='NOT_RUN_IN_THIS_TASK' for x in newcases))

 originals={'checks':rows(read(plan/'evidence/REPORT_CHECKS_96.json')),'metrics':rows(read(plan/'evidence/METRICS_44_PROJECTED.json'))}
 covdefs=[('REPORT_CHECKS_96_CROSSWALK.json',96,'checks'),('METRICS_44_CROSSWALK.json',44,'metrics'),('DIRECTIONS_165_CROSSWALK.json',165,None),('NINE_FAILURE_LAYERS.json',9,None)]
 for name,count,kind in covdefs:
  path=plan/'coverage'/name
  if not path.exists():check('coverage_exists_'+name,False);continue
  data=rows(read(path));check('coverage_count_'+name,len(data)==count)
  keys=[x.get('id',x.get('original_id',x.get('layer',x.get('code')))) for x in data]
  unique_keys=[(x.get('group'),k) for x,k in zip(data,keys)] if name.startswith('DIRECTIONS') else keys
  check('coverage_ids_unique_'+name,all(k is not None for k in keys) and len(set(unique_keys))==count)
  if kind:check('coverage_exact_ids_'+name,set(keys)=={x['id'] for x in originals[kind]})
  check('coverage_current_issue_ids_'+name,all(x.get('current_issue_ids') and set(x['current_issue_ids'])<=ids for x in data))
  if name.startswith('REPORT_CHECKS'):
   raw={x['id']:x for x in originals['checks']}
   check('check_original_domain_status_preserved',all(x.get('original_domain_outcome',x.get('original_status',x.get('domain_outcome')))==raw[k]['domain_outcome'] for x,k in zip(data,keys)))
   check('check_original_health_preserved',all(x.get('original_health',x.get('health'))==raw[k]['health'] for x,k in zip(data,keys)))
  if name.startswith('METRICS'):
   raw={x['id']:x for x in originals['metrics']}
   check('metric_original_status_preserved',all(x.get('original_status',x.get('status'))==raw[k]['status'] for x,k in zip(data,keys)))
 check('96_health_distribution',collections.Counter(x['health'] for x in originals['checks'])=={'PASS':4,'FAIL':4,'WARN':7,'NOT_PROVEN':21,'OBSERVED':44,'NOT_APPLICABLE':16})
 check('44_status_distribution',collections.Counter(x['status'] for x in originals['metrics'])=={'INSUFFICIENT':35,'RECORDED_UNVALIDATED':5,'BLOCKED':1,'UNKNOWN_SOURCE_MISSING':1,'NOT_DEPLOYED':1,'UNKNOWN_STALE_SYNTHETIC_SOURCE':1})

 required=['README.md','CONSTRUCTION_PLAN.md','ISSUES.md','CODEX_EXECUTE.md','BATCH_INDEX.md','EVIDENCE_SCOPE.md','specs/MAINLINE.md','specs/PRESERVATION.md','specs/CERTIFICATE_DOMAINS.md','specs/TYPED_GENERATION_RULE.md','specs/ACCEPTANCE_AND_STATUS.md','coverage/README.md']
 for name in required:check('required_file_'+name,(plan/name).is_file())
 # Historic originals retain obsolete paths as historical evidence, not executable current links.
 active_md=[p for p in plan.rglob('*.md') if not ('inheritance' in p.parts or 'evidence' in p.parts or p.name=='SELF_CHECK.md')]
 missing=[]
 for path in active_md:
  body=path.read_text()
  for dest in re.findall(r'(?<!!)\[[^\]]+\]\(([^\s)]+)\)',body):
   if re.match(r'^(?:https?:|mailto:|#)',dest):continue
   target=(path.parent/dest.split('#')[0])
   if skip_manifest and dest=='MANIFEST_SHA256.json':continue
   if not target.exists():missing.append({'file':str(path.relative_to(plan)),'target':dest})
 check('active_markdown_links_resolve',not missing,missing)
 check('no_workspace_absolute_in_active_docs',all('/workspace/scratch/' not in p.read_text() for p in active_md))

 if not skip_manifest:
  manifest=read(plan/'MANIFEST_SHA256.json')
  entries=manifest['files'];actual={str(p.relative_to(plan)) for p in plan.rglob('*') if p.is_file() and p.name!='MANIFEST_SHA256.json'}
  check('manifest_exact_file_set',{x['path'] for x in entries}==actual)
  bad=[x['path'] for x in entries if not (plan/x['path']).is_file() or (plan/x['path']).stat().st_size!=x['bytes'] or sha(plan/x['path'])!=x['sha256']]
  check('manifest_all_bytes_hashes',not bad,bad)
 else:checks.append({'check':'manifest','passed':None,'detail':'SKIPPED_PRESEAL_CHECK'})
 return {'schema':SCHEMA,'plan_version':'R34.2','verification_scope':'PLAN_CONTENT_SOURCE_LINKAGE_ONLY','application_test_executed':False,'device_test_executed':False,'natural_quality_proven':False,'status':'PASS' if not errors else 'FAIL','check_count':len(checks),'passed_count':sum(x['passed'] is True for x in checks),'skipped_count':sum(x['passed'] is None for x in checks),'errors':errors,'checks':checks}

def main():
 ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--plan',type=Path,default=Path(__file__).resolve().parents[1]);ap.add_argument('--source-repo',type=Path);ap.add_argument('--skip-manifest',action='store_true');ap.add_argument('--out',type=Path)
 args=ap.parse_args()
 try:result=verify(args.plan.resolve(),args.source_repo,args.skip_manifest)
 except Exception as e:result={'schema':SCHEMA,'status':'ERROR','error':type(e).__name__+': '+str(e),'application_test_executed':False}
 if args.out:args.out.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({k:v for k,v in result.items() if k!='checks'},ensure_ascii=False))
 return 0 if result['status']=='PASS' else 1

if __name__=='__main__':sys.exit(main())
