#!/usr/bin/env python3
"""S00 LOCAL evidence checks. These never fabricate a build/CI result or edit the gate."""
from __future__ import annotations
import argparse,csv,hashlib,importlib.util,io,json,os,re,subprocess,sys,xml.etree.ElementTree as ET
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
PLAN=ROOT/'construction/STARCORE_R31_FINAL_20261104'
CONTROL=ROOT/'construction/r31/r312'
EVIDENCE=ROOT/'verification/r31/r312/S00'
class Unavailable(ValueError):pass
def sha(data):return hashlib.sha256(data).hexdigest()
def read(path):return json.loads(Path(path).read_text())
def git(*args):
 z=subprocess.run(['git',*args],cwd=ROOT,text=True,capture_output=True)
 if z.returncode:raise ValueError('GIT_FAILED:'+args[0])
 return z.stdout.strip()
def require(ok,reason):
 if not ok:raise ValueError(reason)
def load(name,path):
 spec=importlib.util.spec_from_file_location(name,path);module=importlib.util.module_from_spec(spec);spec.loader.exec_module(module);return module
def gate():return load('s00_original_gate',PLAN/'tools/r312_gate.py')
def head():return git('rev-parse','HEAD')
def source_ownership():
 g=gate();b=g.batch('S00');bindings=read(CONTROL/'S00/BINDINGS.json');scope=g.scope_report(b,bindings);require(not scope['undeclared'],'UNDECLARED_SOURCE_SCOPE:'+','.join(scope['undeclared']))
 identity_path=ROOT/'app/build/generated/assets/r31/diagnostics/build-identity-r31.json'
 manifest_path=ROOT/'app/build/generated/r815/source-manifest.json'
 if not identity_path.is_file() or not manifest_path.is_file():raise Unavailable('ACTUAL_BUILD_SOURCE_MANIFEST_MISSING')
 identity=read(identity_path);manifest=read(manifest_path);require(identity.get('source_commit')==head(),'SOURCE_COMMIT_DOES_NOT_MATCH_CURRENT_HEAD')
 require(identity.get('plan_manifest_sha256')==sha((PLAN/'MANIFEST_SHA256.json').read_bytes()),'BUILD_PLAN_MANIFEST_MISMATCH')
 actual_rows=manifest.get('inputs',{}).get('java',[]);require(actual_rows,'ACTUAL_JAVA_MANIFEST_EMPTY')
 seen=set();changed=[]
 for group in ['java','resources','native','assets','generatedJava','buildContracts']:
  rows=manifest.get('inputs',{}).get(group)
  require(isinstance(rows,list),'MANIFEST_GROUP_MISSING:'+group)
  for row in rows:
   relative=row['path'];path=ROOT/relative
   require(not Path(relative).is_absolute() and '..' not in Path(relative).parts,'UNSAFE_SOURCE_MANIFEST_PATH')
   require(path.is_file() and not path.is_symlink(),'MANIFEST_INPUT_MISSING:'+relative)
   require(path.stat().st_size==row['bytes'] and sha(path.read_bytes())==row['sha256'],'MANIFEST_INPUT_CHANGED:'+relative)
   if group=='java':require(relative not in seen,'DUPLICATE_JAVA_MANIFEST_PATH');seen.add(relative)
 tracked={p for p in git('ls-tree','-r','--name-only','HEAD','--','app/src/main/java').splitlines() if p.endswith('.java') and not p.endswith('/SourceManifestR815.java')}
 require(seen==tracked,'MANIFEST_JAVA_SET_MISMATCH')
 record_hash=sha(''.join('PATH='+x['path']+'\nSHA256='+x['sha256']+'\n' for x in actual_rows).encode());require(record_hash==manifest.get('sourceTreeSha256'),'SOURCE_MANIFEST_DIGEST_MISMATCH')
 extensions={x['path']:x for x in bindings.get('scope_extensions',[])}
 for relative in g.changed_files(b):
  owner='S00_SCOPE' if relative in scope['in_scope'] else 'S00_PRESERVED_EXTENSION' if relative in scope['declared_extensions'] else 'S00_TEST_TOOL_EVIDENCE_OR_AUTHORIZED_PLAN'
  if relative in extensions:
   extension=extensions[relative];require(len(extension.get('reason','').strip())>=10,'SCOPE_EXTENSION_REASON_MISSING:'+relative)
   require(extension.get('kind')=='PRESERVED_PRE_R312_WIP_NOT_NEW_CARD_COMPLETION','PRESERVED_SCOPE_NOT_IMPLEMENTATION:'+relative)
   committed=git('rev-parse','HEAD:'+relative);require(bool(committed),'HEAD_BLOB_MISSING:'+relative)
   original=subprocess.check_output(['git','show','HEAD:'+relative],cwd=ROOT)
   require(sha(original)==extension.get('source_sha256'),'PRESERVED_WIP_CHANGED_REQUIRES_EXPLICIT_SCOPE_REVIEW:'+relative)
  changed.append({'path':relative,'responsible_batch':'S00','scope':owner})
 return {'source_head':head(),'source_manifest_sha256':sha(manifest_path.read_bytes()),'java_sources':len(seen),'changed_files':changed,'original_report_baseline':'5237b265a27fe5ad27fd842901c445d5f8c27749','scope_is_not_implementation_acceptance':True}
def preservation():
 v=load('s00_verifier',ROOT/'tools/r31/verify_baseline.py');rows=[]
 require(v.PLAN_REVISION=='R31.2-SEQUENTIAL-20261105' and v.PLAN_AUTHORIZATION_COMMIT=='fec7b4e3f20f8375befe8af75d95f08266235f50','R312_EXPLICIT_AUTHORIZATION_CHANGED')
 require(v.PLAN_MANIFEST_SHA256=='571fc11519680d0fee86cdfe9211ab7169358ce0a197da537ece8d33bed764c7' and v.PLAN_MEMBER_COUNT==452,'R312_FIXED_ACTIVE_PIN_CHANGED')
 require(v.ORIGINAL_PLAN_MANIFEST_SHA256=='13f34908b624c74e2852ce8f944b99f8978348820aab23f83043fab1d72753ce' and v.ORIGINAL_PLAN_MEMBER_COUNT==405,'ORIGINAL_ARCHIVE_FIXED_PIN_CHANGED')
 require(sha(subprocess.check_output(['git','show',v.PLAN_AUTHORIZATION_COMMIT+':'+v.PLAN_REL+'/MANIFEST_SHA256.json'],cwd=ROOT))==v.PLAN_MANIFEST_SHA256,'R312_AUTHORIZED_COMMIT_MANIFEST_MISMATCH')
 require(sha((PLAN/'MANIFEST_SHA256.json').read_bytes())==v.PLAN_MANIFEST_SHA256,'R312_ACTIVE_MANIFEST_MISMATCH')
 for role,base in [('active',PLAN),('original_archive',ROOT/v.ORIGINAL_PLAN_REL)]:
  for relative,(expected_count,expected_sha) in v.PRESERVED.items():
   data=(base/relative).read_bytes();count=sum(1 for _ in csv.DictReader(io.StringIO(data.decode('utf-8-sig'))));require(sha(data)==expected_sha and count==expected_count,'PRESERVED_ORIGINAL_MISMATCH:'+role+':'+relative);rows.append({'role':role,'path':relative,'rows':count,'sha256':sha(data)})
 snapshot=read(PLAN/'ledgers/r312_wip_snapshot.json')['cards'];legacy=read(ROOT/'construction/r31/EXECUTION_LEDGER.json')['cards'];states={c.get('id',c.get('card_id')):c.get('state',c.get('status')) for c in legacy};unstarted=[cid for cid,state in snapshot.items() if state=='NOT_STARTED']
 for cid in unstarted:require(states.get(cid)=='NOT_STARTED','UNIMPLEMENTED_CARD_STATUS_CHANGED:'+cid)
 return {'preservation':rows,'not_started_cards_unchanged':len(unstarted),'semantic_implementation_acceptance':'NOT_PERFORMED_BY_PRESERVATION_CHECK'}
def registry():
 suite=read(CONTROL/'HOST_SUITE.json');entries=suite.get('host',[]);require(entries,'HOST_SUITE_EMPTY');ids=set()
 for row in entries:
  require(row.get('id') and row['id'] not in ids,'DUPLICATE_OR_EMPTY_HOST_ID');ids.add(row['id'])
  if row.get('retired'):require(bool(row.get('replacement')),'RETIRED_WITHOUT_REPLACEMENT:'+row['id'])
  else:require(isinstance(row.get('command'),str) and row['command'].strip(),'EMPTY_HOST_COMMAND:'+row['id'])
 bindings=read(CONTROL/'S00/BINDINGS.json');g=gate();expected=g.batch('S00')['local_items'];require(set(bindings.get('items',{}))==set(expected),'LOCAL_BINDING_SET_MISMATCH')
 for item,refs in bindings['items'].items():
  require(bool(refs),'EMPTY_LOCAL_BINDING:'+item)
  for ref in refs:
   if ref.startswith('HOST:'):require(ref[5:] in ids,'BOUND_HOST_NOT_REGISTERED:'+ref)
   else:require('#' in ref,'INVALID_JUNIT_METHOD_BINDING:'+ref)
 for command,want in [('check-plan','PASS'),('status','S00')]:
  z=subprocess.run([sys.executable,str(PLAN/'tools/r312_gate.py'),command],cwd=ROOT,text=True,capture_output=True);require(z.returncode==0,'GATE_'+command+'_FAILED');result=json.loads(z.stdout);require(result.get('state')==want if command=='check-plan' else result.get('open_batch')==want,'GATE_'+command+'_WRONG_STATE')
 return {'registered_entries':len(entries),'bound_local_items':len(expected),'gate_check_plan':'PASS','open_batch':'S00'}
def workflow():
 try:import yaml
 except ImportError:raise Unavailable('YAML_PARSER_NOT_INSTALLED; static regex is not YAML syntax validation')
 path=ROOT/'.github/workflows/r312-gate.yml';document=yaml.load(path.read_text(),Loader=yaml.BaseLoader);require(isinstance(document,dict),'WORKFLOW_NOT_A_MAPPING');events=document.get('on',{});require(isinstance(events,dict),'WORKFLOW_ON_MAPPING_MISSING');require('construction/r31-final-20261104' in events.get('push',{}).get('branches',[]),'WORKFLOW_PUSH_BRANCH_MISMATCH');require('workflow_dispatch' in events,'WORKFLOW_DISPATCH_MISSING');job=document.get('jobs',{}).get('gate',{});steps=job.get('steps',[]);require(steps,'WORKFLOW_GATE_STEPS_MISSING');scripts='\n'.join(str(step.get('run','')) for step in steps)
 require('sdkmanager' in scripts and 'platforms;android-36' in scripts,'CI_SDK_INSTALL_MISSING');require('r312_gate.py build --batch' in scripts,'CI_REAL_GATE_BUILD_MISSING');require('--skip-gradle' not in scripts,'CI_GRADLE_BYPASS');require(any(s.get('uses','').startswith('actions/checkout@') and s.get('with',{}).get('fetch-depth')=='0' for s in steps),'CI_FULL_GIT_HISTORY_REQUIRED');uploads=[s for s in steps if s.get('uses','').startswith('actions/upload-artifact@')];require(any(s.get('if')=='always()' and 'verification/r31/r312/' in s.get('with',{}).get('path','') for s in uploads),'CI_SUMMARY_ARTIFACT_NOT_ALWAYS_UPLOADED')
 scripts_lower=scripts.lower();require('apksigner' not in scripts_lower and 'r312_identity_freeze' not in scripts_lower,'S00_RELEASE_SIGNING_OR_FREEZE_FORBIDDEN')
 sdk=Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','')));sdk_available=(sdk/'platforms/android-36/android.jar').is_file()
 if not sdk_available:raise Unavailable('LOCAL_SDK_ABSENT_ACTUAL_SAME_SOURCE_CI_FALLBACK_SUMMARY_REQUIRED; workflow syntax alone is not runtime proof')
 return {'workflow_sha256':sha(path.read_bytes()),'yaml_syntax':'PARSED','push_branch':'construction/r31-final-20261104','current_local_sdk_available':True,'ci_fallback_execution':'NOT_RUN','boundary':'LOCAL_WORKFLOW_AND_PLATFORM_REGISTRATION_ONLY;NO_ACTUAL_PUSH_ARTIFACT_CLAIM'}
def validate_summary(summary,expected_head,suite):
 require(summary.get('format')=='R312_BUILD_SUMMARY_V1' and summary.get('batch')=='S00','WRONG_BUILD_SUMMARY_FORMAT_OR_BATCH')
 require(summary.get('source_head')==expected_head,'BUILD_SUMMARY_STALE_SOURCE_HEAD')
 gradle=summary.get('gradle',{});require(gradle.get('state') in ('PASS','FAIL') and isinstance(gradle.get('exit'),int),'ACTUAL_GRADLE_NOT_EXECUTED')
 require(gradle.get('unit_task_executed') is True,'ACTUAL_UNIT_TASK_NOT_EXECUTED')
 require(gradle.get('tasks')==gate().GRADLE_TASKS,'REQUIRED_GRADLE_TASK_SET_MISMATCH')
 require(isinstance(summary.get('unit',{}).get('tests'),int) and summary['unit']['tests']>0,'ZERO_ACTUAL_UNIT_TESTS')
 declared={x['id']:x for x in suite.get('host',[])};observed={x['id']:x for x in summary.get('host',[])}
 require(len(observed)==len(summary.get('host',[])) and set(observed)==set(declared),'ACTUAL_HOST_EXECUTION_SET_MISMATCH')
 for key,entry in declared.items():
  result=observed[key]
  if entry.get('retired'):require(result.get('status')=='RETIRED' and result.get('replacement')==entry.get('replacement'),'RETIRED_HOST_RESULT_MISMATCH:'+key)
  else:require(isinstance(result.get('exit'),int) and result.get('status')==('PASS' if result['exit']==0 else 'FAIL'),'HOST_NOT_ACTUALLY_EXECUTED:'+key)
 failed=sorted(x['id'] for x in observed.values() if x.get('status')=='FAIL');require(sorted(summary.get('host_failed',[]))==failed,'HOST_FAILURE_SUMMARY_MISMATCH')
 return {'source_head':expected_head,'gradle':gradle['state'],'unit':summary['unit'],'host_executed':len(observed),'host_failures':failed}
def gate_summary():
 path=EVIDENCE/'BUILD_SUMMARY.json'
 if not path.is_file():raise Unavailable('CURRENT_ACTUAL_BUILD_SUMMARY_MISSING; gate writes it only after all ordinary HOST commands return')
 summary=read(path);result=validate_summary(summary,head(),read(CONTROL/'HOST_SUITE.json'))
 log=EVIDENCE/'logs/gradle.log'
 if not log.is_file():raise Unavailable('ACTUAL_GRADLE_LOG_MISSING')
 text=log.read_text(errors='replace')
 for task in gate().GRADLE_TASKS:require('> Task '+task in text,'GRADLE_TASK_NOT_RECORDED:'+task)
 require('testDebugUnitTest UP-TO-DATE' not in text,'UNIT_TASK_DID_NOT_EXECUTE')
 counts,tests=gate().parse_junit(ROOT/'app/build/test-results/testDebugUnitTest');require(counts==summary['unit'] and tests==summary.get('tests',{}),'ACTUAL_JUNIT_RESULTS_DIFFER_FROM_SUMMARY')
 result['summary_sha256']=sha(path.read_bytes());result['boundary']='Actual completed summary only; never synthesized from a running HOST, previous-source summary or gate self-test.'
 return result

CASES={'source-ownership':source_ownership,'preservation':preservation,'gate-registry':registry,'workflow':workflow,'gate-summary':gate_summary}
def main():
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('--case',choices=list(CASES),required=True);args=p.parse_args()
 try:result=CASES[args.case]();print(json.dumps({'case':args.case,'status':'PASS','source_head':head(),'result':result},ensure_ascii=False));return 0
 except (ValueError,OSError,KeyError,subprocess.SubprocessError) as error:print(json.dumps({'case':args.case,'status':'NOT_RUN' if isinstance(error,Unavailable) else 'FAIL','source_head':head(),'error':str(error)},ensure_ascii=False));return 2 if isinstance(error,Unavailable) else 1
if __name__=='__main__':sys.exit(main())
