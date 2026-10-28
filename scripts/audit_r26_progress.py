#!/usr/bin/env python3
"""Evidence-scoped progress: full card closure, kernel diagnostics and test execution are different denominators."""
from pathlib import Path
import collections,csv,gzip,hashlib,json,subprocess,tempfile,argparse
ROOT=Path(__file__).resolve().parents[1]
PLAN=ROOT/'construction/plans/STARCORE_R26_MERGED_20261026'
def sha(data):return hashlib.sha256(data).hexdigest()
def run(argv):return subprocess.check_output(list(map(str,argv)),cwd=ROOT,text=True)
def main():
 p=argparse.ArgumentParser();p.add_argument('--write',action='store_true');args=p.parse_args()
 bundle=json.loads(run(['python3',PLAN/'code/verify_bundle.py']));assert bundle['all_shipped_hashes_match'] and bundle['declared_omitted']==0
 cards=json.loads((PLAN/'registries/unified_128_cards.json').read_text());coverage=json.loads((PLAN/'registries/coverage.json').read_text());trace=json.loads((PLAN/'registries/requirements_trace.json').read_text())
 origins=dict(collections.Counter(c['origin'] for c in cards));assert len(cards)==len({c['id'] for c in cards})==128 and origins=={'R24':48,'A':32,'B':32,'R26':16}
 assert len(trace['P001_P124_original'])==124 and len(trace['P125_P156'])==32
 legacy={}
 for name,row in trace['legacy_sources'].items():
  path=PLAN/row['path'];assert sha(path.read_bytes())==row['sha256']
  with path.open(encoding='utf-8-sig',newline='') as f:n=sum(1 for _ in csv.DictReader(f))
  assert n==row['rows'];legacy[name]=n
 assert sorted(legacy.values())==[299,1832]
 assert all(coverage[k]==v for k,v in {'unified_card_records':128,'legacy_requirements':1832,'legacy_cards':299,'P001_P124':124,'A_P125_P156':32,'families':24,'experts':16,'report_sections':40,'original_checks':96}.items())
 merge={'status':'PASS','cardId':'R26-001','scope':'Entire stated card acceptance: A32/B32/R24-48/R26-16, all preserved denominators, zero duplicate unified IDs; no algorithm or quality acceptance inferred','bundle':bundle,'origins':origins,'uniqueUnifiedIds':128,'legacyRows':legacy,'priorP001P124':124,'priorP125P156':32}
 verified=json.loads((ROOT/'build/r26/verification.json').read_text());assert verified['status']=='PASS'
 for name,digest in verified['source_sha256'].items():assert sha((ROOT/name).read_bytes())==digest,name
 java='''package com.ethk3.novaonepro.strategyv2; public final class ProgressInventory {public static void main(String[] a){StringBuilder b=new StringBuilder("{\\"families\\":[");for(int i=1;i<=24;i++){if(i>1)b.append(',');String id=String.format("F%02d",i);boolean measured=false,binding=false;try{Object m=FamilyRegistryR26.build(id);measured=m instanceof TrainingDiagnosticsR26||m instanceof StatisticalTrainingDiagnosticsR26;}catch(IllegalArgumentException missing){if(i!=23)throw missing;binding=true;}b.append("{\\"id\\":\\"").append(id).append("\\",\\"diagnostics\\":").append(measured).append(",\\"requiresAndroidTeacherBinding\\":").append(binding).append('}');}System.out.println(b.append("],\\"reportEnvelopeSections\\":").append(ReportMetricContractR26.sections()).append('}'));}}'''
 with tempfile.TemporaryDirectory(prefix='r26-progress-') as tmp:
  d=Path(tmp);(d/'ProgressInventory.java').write_text(java);classes=ROOT/'build/r26/classes';run(['javac','-cp',classes,'-d',d,d/'ProgressInventory.java']);inventory=json.loads(run(['java','-Xmx512m','-cp',str(d)+':'+str(classes),'com.ethk3.novaonepro.strategyv2.ProgressInventory']))
 measured=[x['id'] for x in inventory['families'] if x['diagnostics']];pending=[x['id'] for x in inventory['families'] if not x['diagnostics']]
 ledger=ROOT/'construction/r26/acceptance.json';acceptance=json.loads(ledger.read_text());assert {c['id'] for c in acceptance['cards']}=={c['id'] for c in cards}
 proofs=[(p,json.loads(p.read_text())) for p in (ROOT/'construction/r26/continuation').glob('runtime-*.json')];proofs=[(p,r) for p,r in proofs if all(k in r for k in ['native','raw','runId','compiledSource','source'])];assert proofs;proofPath,proof=max(proofs,key=lambda pair:pair[1]['runId']);raw={}
 for item in proof['raw']:
  b=gzip.decompress((proofPath.parent/item['path']).read_bytes());assert sha(b)==item['rawSha256'];raw[item['path']]=b
 instrument=json.loads(next(v for k,v in raw.items() if 'instrumentation-results.json' in k));runtime=json.loads(next(v for k,v in raw.items() if 'runtime-synthetic.json' in k))
 assert instrument['declaredTests']==instrument['executedTests']+instrument['notExecutedTests'] and instrument['declaredTests']>0 and instrument['failedSelectors']>=0
 assert all(x['sourceCommit']==proof['compiledSource'] for x in runtime['committedResearchService']['sessions'])
 currentSuite={k:instrument[k] for k in ['declaredTests','executedTests','failedSelectors','notExecutedTests','status']};currentSuite.update(source=proof['source'],compiledSource=proof['compiledSource'],api=29,evidence=str(proofPath.relative_to(ROOT)),scope='MATCHING_SOURCE_EXECUTION_RESULT_INCLUDING_FAILURES; NOT_CURRENT_UNEXECUTED_TESTS_OR_NATURAL_QUALITY',runtimeEngineeringResult=runtime['result'],durableCommittedCapacityFailures=runtime['committedResearchService']['committedCapacityFailuresSinceInstallation'])
 passedProofs=[(p,r) for p,r in proofs if r['native'].get('failedSelectors')==0 and r['native'].get('notExecutedTests')==0 and r['native'].get('executedTests',0)>0];passedPath,passed=max(passedProofs,key=lambda pair:pair[1]['runId']);passedSuite={'source':passed['source'],'compiledSource':passed['compiledSource'],'runId':passed['runId'],'evidence':str(passedPath.relative_to(ROOT)),**passed['native']}
 if args.write:
  mergePath=ROOT/'construction/r26/continuation/merged-requirements-acceptance.json';mergePath.write_text(json.dumps(merge,ensure_ascii=False,indent=2)+'\n')
  for c in acceptance['cards']:
   if c['id']=='R26-001':c.update(status='PASS',reason='Full stated denominator and namespace-merge acceptance independently verified; algorithm/quality statuses remain separate',evidence='continuation/merged-requirements-acceptance.json')
  if 'historicalDeliveryRecords' not in acceptance:
   keys=['compiled_source','apk_sha256','android_ci','qualification_protocol','fixed_window_statistical_power','legacy_predraw_bootstrap','legacy_committee_stage_generation'];acceptance['historicalDeliveryRecords']=[{k:acceptance[k] for k in keys}]
  packages=[(p,json.loads(p.read_text())) for p in (ROOT/'construction/r26/continuation').glob('package-*-independent.json')];packagePath,package=max(packages,key=lambda pair:pair[1].get('runId',0));matching=[r for p,r in proofs if r['source']==package.get('source')];assert matching;packageSource=matching[-1]['compiledSource'];acceptance['compiled_source']=packageSource;acceptance['apk_sha256']=package['apkSha256'];acceptance['latestVerifiedPackage']={'evidence':str(packagePath.relative_to(ROOT)),'source':package['source'],'compiledSource':packageSource,'apkSha256':package['apkSha256'],'scope':'INDEPENDENTLY_VERIFIED_HISTORICAL_ENGINEERING_APK_NOT_CURRENT_OR_COMPLETE_DELIVERY'};acceptance['android_ci']={'run_id':proof['runId'],'build':proof.get('build','RECORDED_SEPARATELY'),'api_29_native':currentSuite,'api_35_scope':'STARTUP_RESTART_RELEASE_SURVIVAL_ONLY','api_36_scope':'STARTUP_RESTART_RELEASE_SURVIVAL_ONLY','physical_device':'USER_WAIVED_NOT_PERFORMED'}
  acceptance['qualification_protocol']='R26_PREREGISTERED_AGGREGATE_LCB_V3;69120_PERIODS_STRIDE720;NATURAL_EVIDENCE_INSUFFICIENT';acceptance['fixed_window_statistical_power']='Historical4000-period failure retained in historicalDeliveryRecords; current69120 calendar does not establish actual natural strategy quality';acceptance['user_physical_device']='USER_WAIVED_NOT_PERFORMED'
  acceptance['legacy_predraw_bootstrap']='RESUMABLE_REPLAY_AND_BOUNDED_OWNER_PARTS_IMPLEMENTED;FULL_LEGACY_ACCEPTANCE_PENDING';acceptance['legacy_committee_stage_generation']='PARTIAL_RESUMABLE_GENERATION_AND_CONTINUATION_VERIFIED;PRIVATE_TRAINING_ROSTER_PERSISTENCE_ACCEPTANCE_PENDING'
  ledger.write_text(json.dumps(acceptance,ensure_ascii=False,indent=2)+'\n')
 counts=dict(collections.Counter(c['status'] for c in acceptance['cards']));result={'schema':'R26_PROGRESS_SCOPED_V1','overallImplementationPercent':None,'overallPercentReason':'No reviewed partial-item weighting; do not substitute diagnostics, test counts or complete-card closure for whole implementation completion','overallAcceptance':acceptance['overall'],'wholeCardAcceptance':{'total':128,'statusCounts':counts,'fullPassCount':counts.get('PASS',0),'scope':'WHOLE_CARD_ACCEPTANCE_ONLY_NOT_A_SOFTWARE_IMPLEMENTATION_PERCENT'},'trainingDiagnostics':{'implemented':len(measured),'total':24,'percent':len(measured)*100/24,'families':measured,'pending':pending,'scope':'DEFAULT_KERNEL_DIAGNOSTIC_INTERFACE_ONLY_NOT_FULL_FAMILY_CONTRACT_OR_QUALITY','inventory':inventory['families']},'reportEnvelopes':{'implemented':inventory['reportEnvelopeSections'],'total':40,'scope':'ENVELOPES_ONLY_NOT_ALL_REQUIRED_METRICS_ACCEPTED'},'protocolGroups':{'passed':len(verified['checks']),'evidence':'build/r26/verification.json','scope':verified['scope']},'latestVerifiedNativeSuite':currentSuite,'latestPassedNativeSuite':passedSuite,'naturalQuality':'INSUFFICIENT','mergedRequirementAudit':merge}
 enduranceProofs=[]
 for candidate in (ROOT/'construction/r26/continuation').glob('endurance-*-six*.json'):
  record=json.loads(candidate.read_text())
  if record.get('status','').startswith('PASS_') and len(record.get('scenarios',[]))==6 and all('raw' in x for x in record['scenarios']):enduranceProofs.append((candidate,record))
 if enduranceProofs:
  ep,record=max(enduranceProofs,key=lambda pair:pair[1]['runId']);scenarios=[]
  for scenario in record['scenarios']:
   evidence=next(x for x in scenario['raw'] if 'runtime-synthetic.json' in x['path']);data=gzip.decompress((ep.parent/evidence['path']).read_bytes());assert sha(data)==evidence['rawSha256'];receipt=json.loads(data);e=receipt['endurance'];assert receipt['result']=='PASS_WITH_STATED_ENGINEERING_SCOPE' and e['elapsedMs']>=3600000 and len(e['samples'])>=59 and receipt['committedResearchService']['committedCapacityFailuresSinceInstallation']==0
   assert all(x['progressDelta']>0 and x['pluggedTelemetry']==0 for x in e['samples']);scenarios.append({'scenario':scenario['scenario'],'elapsedMs':e['elapsedMs'],'minuteSamples':len(e['samples']),'durableFailures':0})
  assert len({x['scenario'] for x in scenarios})==6
  result['latestVerifiedEnduranceMatrix']={'source':record['source'],'runId':record['runId'],'evidence':str(ep.relative_to(ROOT)),'scenarios':scenarios,'scope':record['scope']}
 if args.write:(ROOT/'construction/r26/progress.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps(result,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
