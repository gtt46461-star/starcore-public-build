#!/usr/bin/env python3
"""Verify report-to-owner mappings without treating them as product acceptance.
Usage: python3 verify_responsibility.py --plan PLAN --report-dir EXTRACTED --source-repo SOURCE
If --report-dir is omitted, reads PLAN/report/ORIGINAL_DIAGNOSTIC_173.zip (outer or inner ZIP).
"""
import argparse,collections,hashlib,io,json,re,sys,zipfile,subprocess
from pathlib import Path

BASE='4d1de9d83692635b5c5ce39144fece1c8a31c8ac'

def report_members(plan,report_dir):
 if report_dir:
  r=Path(report_dir);return {n:(r/n).read_bytes()for n in ['R30_REPORT.json','REPORT_DETAILS.txt']}
 raw=(plan/'report/ORIGINAL_DIAGNOSTIC_173.zip').read_bytes()
 with zipfile.ZipFile(io.BytesIO(raw)) as outer:
  if 'R30_REPORT.json'in outer.namelist():return {n:outer.read(n)for n in ['R30_REPORT.json','REPORT_DETAILS.txt']}
  nested=[n for n in outer.namelist()if n.endswith('/STARCORE_FULL_REPORT.zip')or n=='STARCORE_FULL_REPORT.zip']
  if len(nested)!=1:raise ValueError('REPORT_NESTED_ZIP_IDENTITY')
  inner=outer.read(nested[0])
 with zipfile.ZipFile(io.BytesIO(inner)) as z:return {n:z.read(n)for n in ['R30_REPORT.json','REPORT_DETAILS.txt']}

def validate(plan,report_dir=None,source=None,metrics_override=None,checks_override=None):
 plan=Path(plan);checks=[]
 def require(ok,code):
  checks.append({'id':code,'passed':bool(ok)})
  if not ok:raise AssertionError(code)
 def j(p):return json.loads(Path(p).read_text())
 def exactids(rows,key,expected,code):
  ids=[r[key]for r in rows];require(len(ids)==len(set(ids)) and set(ids)==set(expected),code)
 metric_path=plan/'coverage/METRIC_ACTIONS_44.json';check_path=plan/'coverage/CHECK_ACTIONS_96.json'
 ms=metrics_override if metrics_override is not None else j(metric_path)['metrics']
 cs=checks_override if checks_override is not None else j(check_path)['checks']
 orig=report_members(plan,report_dir);r30=json.loads(orig['R30_REPORT.json'])
 original_metrics={r['metricId']:r for r in r30['metricResults']}
 original_checks={}
 for line in orig['REPORT_DETAILS.txt'].decode().splitlines():
  if line.startswith('{'):
   x=json.loads(line)
   if x.get('kind')=='check_result':original_checks[x['id']]=x
 ids=j(plan/'ISSUES.json')['issues'];issues={i['id']:i for i in ids}
 exactids(ms,'id',original_metrics,'EXACT_METRIC_IDS_44');exactids(cs,'id',original_checks,'EXACT_CHECK_IDS_96')
 require(len(original_metrics)==44 and len(original_checks)==96,'EXPECTED_REPORT_COUNTS')
 require(len(set(m['treatment']for m in ms))==44,'NO_UNIFORM_METRIC_TEMPLATE')
 for m in ms:
  id=m['id'];base=original_metrics[id]
  require(m['original_metric']==base,'ORIGINAL_METRIC_ALL_FIELDS_UNCHANGED:'+id)
  require(m['target_preserved']==base['target'] and m['definition_version_preserved']==base['definitionVersion'],'TARGET_AND_DEFINITION_UNCHANGED:'+id)
  require(bool(m['related_issues']) and all(i in issues for i in m['related_issues']),'METRIC_ISSUE_LINKS:'+id)
  owner=m['implementation_owner_batch'];direct=m.get('supplemental_measurement_work_item')
  owned=any(issues[i]['batch']==owner for i in m['related_issues'])
  if not owned:
   require(id=='M16' and direct is not None and direct.get('id')=='R343-B11-M16' and direct.get('owner_batch')==owner=='B11','EXPLICIT_MEASUREMENT_OWNER:'+id)
   require(direct.get('method')=='finishCancellationR30' and direct.get('required_receipt'),'CANCELLATION_RECEIPT_CONTRACT')
  require(m['verification_owner_batch']=='B11' and m['device_signoff_batch']=='B12','DISTINCT_VERIFICATION_OWNER:'+id)
  require(bool(m.get('acceptance_positive')) and bool(m.get('acceptance_negative')),'POSITIVE_NEGATIVE_ACCEPTANCE:'+id)
  require(m['new_status']!='PASS','PLAN_CANNOT_GRANT_METRIC_PASS:'+id)
  require(bool(m.get('source_bindings')),'METRIC_SOURCE_BINDINGS:'+id)
 byid={m['id']:m for m in ms}
 for id,needed in {'M03':['R343-011'],'M04':['R343-013','R343-014'],'M05':['R343-010'],'M10':['R343-026'],'M11':['R343-026'],'M20':['R343-017','R343-026'],'M22':['R343-027'],'M25':['R343-031'],'M28':['R343-034'],'M33':['R343-040'],'M34':['R343-002'],'Q08':['R343-034','R343-044']}.items():
  require(set(needed)<=set(byid[id]['related_issues']),'SEMANTIC_RESPONSIBILITY:'+id)
 require('5000ms' in ' '.join(byid['M03']['acceptance_positive']),'M03_REQUEST_DURABLE_5000MS')
 require('1000ms' in ' '.join(byid['M04']['acceptance_positive']),'M04_OBSERVE_SETTLE_1000MS')
 require('NEXT_DEADLINE' in byid['M05']['treatment'] and '5000ms' in byid['M05']['treatment'] and '8/30' in ' '.join(byid['M05']['acceptance_negative']),'M05_FIVE_SECOND_MARGIN_NOT_GENERIC_PREDRAW')
 require('SYNTHETIC' in str(byid['Q08']['original_metric']['r31Definition']['role']),'Q08_SYNTHETIC_NOT_NATURAL')
 for c in cs:
  id=c['id'];require(c['report_check']==original_checks[id],'ORIGINAL_CHECK_RESULT_UNCHANGED:'+id)
  require(bool(c['related_issues']) and all(i in issues for i in c['related_issues']),'CHECK_ISSUE_LINKS:'+id)
  require(not c['automatically_satisfies_metrics'],'CHECK_NOT_AUTOMATIC_METRIC_PASS:'+id)
  require(bool(c['acceptance_specific']) and bool(c['issue_contracts']),'CHECK_SPECIFIC_ACCEPTANCE:'+id)
  for child in c['composite_child_checks']:require(child in original_checks,'COMPOSITE_CHILD_RESOLVES:'+id+':'+child)
  if c['composite_child_checks']:require(c['verification_kind']=='COMPOSITION_ONLY','COMPOSITION_NOT_RUNTIME:'+id)
 if source:
  source=Path(source);seen={};source_cache={}
  require(subprocess.check_output(['git','-C',str(source),'rev-parse',BASE],text=True).strip()==BASE,'EXACT_BASELINE_OBJECT')
  def baseline(path):
   if path not in source_cache:
    result=subprocess.run(['git','-C',str(source),'show',BASE+':'+path],capture_output=True)
    require(result.returncode==0,'SOURCE_EXISTS:'+path)
    source_cache[path]=result.stdout
   return source_cache[path]
  for row in ms+cs:
   for b in row['source_bindings']:
    path=b.get('path',b.get('file'));expected=b.get('source_sha256')
    data=baseline(path)
    if(path,expected)not in seen:
     if expected:require(hashlib.sha256(data).hexdigest()==expected,'SOURCE_HASH:'+path)
     seen[path,expected]=True
    anchor=b.get('anchor') or b.get('method_or_token')
    if anchor:require(anchor in data.decode(),'SOURCE_ANCHOR:'+path+':'+anchor)
  for m in ms:
   b=m.get('supplemental_measurement_work_item')
   if b:
    data=baseline(b['production_path']);require(hashlib.sha256(data).hexdigest()==b['source_sha256'],'SUPPLEMENTAL_SOURCE:'+m['id'])
    require(b['method'] in data.decode().splitlines()[b['line']-1],'SUPPLEMENTAL_METHOD:'+m['id'])
 return {'status':'COVERAGE_VALIDATED_NOT_PRODUCT_ACCEPTANCE','checks':len(checks),'metrics':len(ms),'report_checks':len(cs),'passed':checks,'scope':'PLAN_TRACEABILITY_ONLY; ANDROID_DEVICE_NATURAL_QUALITY_NOT_RUN'}

def main():
 p=argparse.ArgumentParser();p.add_argument('--plan',required=True);p.add_argument('--report-dir');p.add_argument('--source-repo');p.add_argument('--out');a=p.parse_args()
 try:r=validate(a.plan,a.report_dir,a.source_repo)
 except Exception as e:
  print(json.dumps({'status':'FAIL','error':str(e)},ensure_ascii=False));return 1
 if a.out:Path(a.out).write_text(json.dumps(r,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({k:v for k,v in r.items()if k!='passed'},ensure_ascii=False));return 0
if __name__=='__main__':sys.exit(main())
