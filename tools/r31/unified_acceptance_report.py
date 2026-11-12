#!/usr/bin/env python3
"""All 58 cards / 222 current items, bound to one actual engineering invocation.
An executed unit/HOST binding is not silently promoted to whole-product closure.
Original plan files and historical gate receipts are read only.
"""
import argparse,collections,csv,hashlib,importlib.util,json,subprocess
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
REPAIR=ROOT/'construction/repairs/STARCORE_R31_UNIFIED_FINAL_20261112'
PLAN=ROOT/'construction/STARCORE_R31_FINAL_20261104'
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def main():
 p=argparse.ArgumentParser();p.add_argument('--summary',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
 s=json.loads(a.summary.read_text());head=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip()
 if s.get('source_head')!=head or not s.get('invocationId') or not s.get('runId'):raise SystemExit('CURRENT_INVOCATION_SOURCE_IDENTITY_REQUIRED')
 spec=importlib.util.spec_from_file_location('immutable_r31_gate',PLAN/'tools/r31_gate.py');gate=importlib.util.module_from_spec(spec);spec.loader.exec_module(gate)
 proof=json.loads((REPAIR/'PLAN_PRESERVATION_PROOF.json').read_text())
 config=PLAN/'ledgers/acceptance_config.json'
 if sha(config)!=proof['acceptance_config_sha256']:raise SystemExit('ORIGINAL_THRESHOLDS_CHANGED')
 original=list(csv.DictReader((REPAIR/'ORIGINAL_TO_CURRENT_ACCEPTANCE.csv').open(encoding='utf-8-sig')))
 if len(original)!=222 or len({r['current_item'] for r in original})!=222:raise SystemExit('CURRENT_ACCEPTANCE_SET_CHANGED')
 bindings={}
 for file in (ROOT/'construction/r31/batches').glob('K*/BINDINGS.json'):bindings.update(json.loads(file.read_text()).get('items',{}))
 rows=[]
 for item in original:
  refs=bindings.get(item['current_item'],[]);actual=[{'reference':ref,'currentInvocationResult':gate.ref_status(s,ref)} for ref in refs]
  status='NOT_RUN' if item['level']!='NATURAL' else 'NOT_PROVEN'
  if item['level']=='LOCAL':status='PASS' if actual and all(x['currentInvocationResult']=='PASS' for x in actual) else 'FAIL_OR_MISSING_CURRENT_BINDING'
  rows.append({**item,'textSha256':hashlib.sha256(item['text'].encode()).hexdigest(),'engineeringBindingResult':status,'boundChecks':actual,
   'productAcceptance':'NOT_CLOSED_BY_HOST_ONLY' if item['level']=='LOCAL' else status,'evidenceScope':'CURRENT_INVOCATION_ONLY; NO_HISTORICAL_GREEN_IMPORT; RUNTIME_AND_LONG_DURATION_EVIDENCE_SEPARATE'})
 source=json.loads((REPAIR/'CURRENT_SOURCE_STATUS_58.json').read_text());cards=[]
 for prior in source['rows']:
  files=[]
  for old in prior.get('source_files',[]):
   file=ROOT/old['path'];files.append({'path':old['path'],'currentSha256':sha(file) if file.is_file() else None,'checkpointSha256':old.get('sha256'),'changedFromCheckpoint':file.is_file() and sha(file)!=old.get('sha256')})
  cards.append({'card':prior['card_id'],'title':prior['title'],'K_batch':prior['batch'],'repairCards':prior.get('repair_cards',[]),'currentSources':files,
   'currentItems':[r for r in rows if r['card']==prior['card_id']],'priorSourceFindingsPreserved':prior.get('current_findings',[]),
   'wholeCardClosure':'REQUIRES_PRODUCTION_AND_EXECUTION_EVIDENCE; HOST_BINDINGS_LISTED_SEPARATELY'})
 if len(cards)!=58:raise SystemExit('ORIGINAL_56_PLUS_2_CARD_SET_CHANGED')
 a.out.mkdir(parents=True,exist_ok=True)
 identity={k:s.get(k) for k in ['source_head','source_tree','invocationId','runId','inputsSha256','inputsAfterSha256','state','exitCode']}
 document={'format':'R31_UNIFIED_CURRENT_ACCEPTANCE_1','identity':identity,'summarySha256':sha(a.summary),'preservation':proof,'levels':dict(collections.Counter(x['level'] for x in rows)),
  'itemCounts':dict(collections.Counter(x['engineeringBindingResult'] for x in rows)),'items':rows,'cards':cards,
  'limits':{'physicalDevice':'NOT_RUN','oldDeviceDatabase':'UNAVAILABLE','device24h':'NOT_RUN','device72h':'NOT_RUN','naturalQuality':'NOT_PROVEN','hostTestsAreWholeProductAcceptance':False}}
 (a.out/'ACCEPTANCE_RESULTS.json').write_text(json.dumps(document,ensure_ascii=False,indent=2)+'\n')
 with (a.out/'ACCEPTANCE_RESULTS.csv').open('w',encoding='utf-8-sig',newline='') as stream:
  writer=csv.DictWriter(stream,fieldnames=['original_item','current_item','card','K_batch','level','text','original_text_sha256','textSha256','engineeringBindingResult','productAcceptance','boundChecks'])
  writer.writeheader()
  for row in rows:writer.writerow({k:json.dumps(row[k],ensure_ascii=False) if k=='boundChecks' else row[k] for k in writer.fieldnames})
 print(json.dumps({'cards':len(cards),'items':len(rows),'invocation':s['invocationId'],'bindingResults':document['itemCounts'],'wholeProductClosure':'NOT_INFERRED'},ensure_ascii=False))
if __name__=='__main__':main()
