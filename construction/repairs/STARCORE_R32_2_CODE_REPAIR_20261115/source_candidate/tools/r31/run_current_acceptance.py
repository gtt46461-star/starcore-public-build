#!/usr/bin/env python3
"""A fresh R31 engineering invocation; historical receipts are never rewritten.

The original gate executes the full suite. This wrapper refuses stale summaries,
nonzero commands, missing suites, lower test counts, skips, source drift and every
unpassed LOCAL binding, including post-K11 repairs on the same Git SHA.
"""
import argparse,datetime as dt,hashlib,importlib.util,json,os,subprocess,sys,uuid
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
PLAN=ROOT/'construction/STARCORE_R31_FINAL_20261104'
# The sealed plan is an input. Imports must never create an unsealed pyc in it,
# including imports performed by child verifiers started without Python's -B.
sys.dont_write_bytecode=True
os.environ['PYTHONDONTWRITEBYTECODE']='1'
def write(path,value):
 temp=path.with_suffix('.partial');temp.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n');temp.replace(path)
def fingerprint():
 names=[]
 for top in ['app/src','scripts','tools','schema','.github/workflows','construction/r31']:
  names.extend(p for p in (ROOT/top).rglob('*') if p.is_file() and '__pycache__' not in p.parts and p.suffix not in ['.pyc'])
 names += [ROOT/p for p in ['app/build.gradle','build.gradle','settings.gradle','gradle.properties','release/release_manifest.json','release/release_identity.properties','release/research_dependencies_r967.json','release/r82_training_invalidation.json','release/strategy_su1_reviewed_inputs.json']]
 rows=[(str(p.relative_to(ROOT)),hashlib.sha256(p.read_bytes()).hexdigest()) for p in sorted(set(names))]
 return hashlib.sha256(json.dumps(rows,separators=(',',':')).encode()).hexdigest()
def validate(g,summary,invocation,expected_run,expected_inputs):
 problems=[]
 if summary.get('invocationId')!=invocation:problems.append('INVOCATION_MISMATCH')
 if summary.get('runId')!=expected_run:problems.append('RUN_ID_MISMATCH')
 if summary.get('inputsSha256')!=expected_inputs or summary.get('inputsAfterSha256')!=expected_inputs:problems.append('SOURCE_OR_CONTROL_INPUTS_CHANGED')
 gradle=summary.get('gradle',{});unit=summary.get('unit',{})
 if gradle.get('state')!='PASS' or gradle.get('exit')!=0 or summary.get('exitCode')!=0:problems.append('COMMAND_FAILED')
 if gradle.get('compile_failed') or not gradle.get('unit_task_executed'):problems.append('COMPILE_OR_UNIT_NOT_EXECUTED')
 historic=[]
 for receipt in (ROOT/'construction/r31/batches').glob('K*/RECEIPT.json'):
  row=json.loads(receipt.read_text());historic.append(row.get('unit',{}).get('tests',0))
 if unit.get('tests',0)<max([622,*historic]):problems.append('UNIT_COUNT_DROPPED_OR_EMPTY')
 if any(unit.get(key,1) for key in ['failures','errors','skipped']):problems.append('UNIT_FAILURE_ERROR_OR_SKIP')
 required={x['id'] for x in g.load_suite().get('host',[]) if not x.get('retired')}
 actual={x['id'] for x in summary.get('host',[]) if x.get('status')=='PASS'}
 if required-actual:problems.append('MISSING_OR_FAILED_HOST:'+','.join(sorted(required-actual)))
 if summary.get('host_failed'):problems.append('HOST_FAILURES')
 bound={}
 for path in (ROOT/'construction/r31/batches').glob('K*/BINDINGS.json'):
  for item,refs in json.loads(path.read_text()).get('items',{}).items():bound[item]=refs
 for item in g.items():
  if item['level']!='LOCAL':continue
  refs=bound.get(item['item'],[])
  if not refs or any(g.ref_status(summary,ref)!='PASS' for ref in refs):problems.append('LOCAL_BINDING_NOT_PASS:'+item['item'])
 return problems

def main():
 parser=argparse.ArgumentParser();parser.add_argument('--batch',default='K11');parser.add_argument('--out',type=Path);args=parser.parse_args()
 invocation=os.environ.get('R31_INVOCATION_ID') or str(uuid.uuid4());run=os.environ.get('GITHUB_RUN_ID','LOCAL-'+invocation)
 out=(args.out or ROOT/'verification/r31/batches'/args.batch).resolve();out.mkdir(parents=True,exist_ok=True)
 path=out/'BUILD_SUMMARY.json'
 # Remove an old PASS before even importing/configuring the gate.
 if path.exists():path.replace(out/('PREVIOUS-'+invocation+'.json'))
 started=dt.datetime.now(dt.timezone.utc).isoformat();before=None
 base={'format':'R31_CURRENT_INVOCATION_V1','batch':args.batch,'invocationId':invocation,'runId':run,'startedAt':started,'source_head':None,'source_tree':None,'inputsSha256':None,'exitCode':1,'state':'STARTED_NOT_PASS','unit':{'tests':0,'failures':0,'errors':0,'skipped':0},'gradle':{'state':'NOT_RUN','exit':None,'unit_task_executed':False},'host':[],'host_failed':[],'tests':{}}
 write(path,base);rc=1;failure=None;result={};gate=None
 try:
  base['source_head']=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
  base['source_tree']=subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=ROOT,text=True).strip()
  before=fingerprint();base['inputsSha256']=before;write(path,base)
  spec=importlib.util.spec_from_file_location('r31_original_gate',PLAN/'tools/r31_gate.py');gate=importlib.util.module_from_spec(spec);spec.loader.exec_module(gate)
  gate.summary_path=lambda bid:path
  batch=next(x for x in gate.ledger()['batches'] if x['batch']==args.batch)
  result=gate.build(batch);rc=0 if result.get('gradle',{}).get('exit')==0 and not result.get('host_failed') else 1
 except BaseException as error:
  failure=type(error).__name__+':'+str(error)
 finally:
  try:after=fingerprint()
  except BaseException as error:after=None;failure=(failure or '')+';INPUT_FINGERPRINT:'+str(error);rc=1
  summary={**base,**result,'format':'R31_CURRENT_INVOCATION_V1','legacyGateSummaryFormat':result.get('format'),'invocationId':invocation,'runId':run,'inputsSha256':before,'inputsAfterSha256':after,'exitCode':rc,'wrapperFailure':failure,'finishedAt':dt.datetime.now(dt.timezone.utc).isoformat()}
  try:problems=validate(gate,summary,invocation,run,before) if rc==0 else [failure or 'EARLY_FAILURE']
  except BaseException as error:problems=['VALIDATION_FAILED:'+type(error).__name__+':'+str(error)]
  summary['problems']=problems;summary['state']='PASS_ENGINEERING_ONLY' if not problems else 'FAILED_OR_INCOMPLETE';summary['device']='NOT_RUN';summary['naturalQuality']='NOT_PROVEN';write(path,summary)
 print(json.dumps({k:summary[k] for k in ['state','invocationId','source_head','exitCode','problems']},ensure_ascii=False));return 0 if not problems else 1
if __name__=='__main__':sys.exit(main())
