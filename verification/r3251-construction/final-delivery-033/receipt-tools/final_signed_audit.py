#!/usr/bin/env python3
"""Current per-ID delivery audit. No grouped result signs an original clause."""
import collections, datetime, hashlib, json, shutil, stat, subprocess
from pathlib import Path

REPO=Path('/workspace/starcore-r3251-attachment-final')
PACKAGE=Path('/workspace/starcore-r3251-owner5-revision')
ORIGINAL=Path('/workspace/starcore-plan-r3251/construction/repairs/STARCORE_R32_5_FINAL_REPAIR_20261115/R32_5_1_restored/STARCORE_R32_5_FINAL_REPAIR_20261115')
ROOT=Path('/workspace/starcore-r3251-evidence/complete-chain-final')
OUT=REPO/'verification/r3251-construction/final-delivery-033'
BUILD='bb7b49bbcc9266bad548ec37e71403256fc06a4d'
APK_SHA='8f0cb6e6eadbce5787cffde310a6cbf204d43962e636fd9ebedf6e66556fb763'
STAGES={'P2':'P2-attachment-owner','P3':'P3-complete-chain','P4':'P4-complete-chain','P5':'attachment-final'}

def sha(p):return hashlib.sha256(Path(p).read_bytes()).hexdigest()
def write(name,d):
 p=OUT/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n')
def cp(p,q):q.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,q)
def git(*args,repo=REPO):return subprocess.check_output(['git',*args],cwd=repo,text=True).strip()

def main():
 OUT.mkdir(exist_ok=True)
 assert not git('diff',BUILD,'--','app','tools','scripts','gradle','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat'), 'PRODUCT_INPUT_DRIFT'
 old=REPO/'verification/r3251-construction/delivery'
 tests={};hosts={};stages=[]
 p1=json.loads(Path('/workspace/starcore-r3251-evidence/P1-junit-summary.json').read_text())
 tests['P1']=p1['tests'];stages.append(dict(batch='P1',sourceCommit='8a8ee55cf63c9e7faf6383a2b477b00a9dadbf9a',junit=p1['counts'],boundHostCount=2,evidenceCommit='8a8ee55cf63c9e7faf6383a2b477b00a9dadbf9a',wholeBusinessAcceptance=False))
 hosts['P1']={}
 for f in Path('/workspace/starcore-r3251-evidence').glob('P1-bound-*.command.json'):
  d=json.loads(f.read_text())
  if d.get('exitCode')==0:
   assert sha(f.with_name(d['log']))==d['logSha256']
   entry=d['id'][len('P1-bound-'):]
   if entry.endswith('-approved'):entry=entry[:-len('-approved')]
   hosts['P1'][entry]={'status':'PASS','selected':f.name,'attempts':[d]}
 for f in Path('/workspace/starcore-r3251-evidence').glob('P1-*'):
  if f.is_file() and f.suffix in ('.json','.log','.patch','.txt'):cp(f,OUT/'stages/P1'/f.name)
 for folder in ['P1-gradle-results','P1-host']:
  for f in (Path('/workspace/starcore-r3251-evidence')/folder).rglob('*'):
   if f.is_file() and f.suffix in ('.json','.log','.xml','.txt','.md'):cp(f,OUT/'stages/P1'/folder/f.relative_to(Path('/workspace/starcore-r3251-evidence')/folder))
 for b,dirname in STAGES.items():
  repo=Path('/workspace/starcore-r3251-'+dirname);source=repo/'verification/r3251-construction/complete-research-chain'/b
  status=json.loads((source/'STATUS.json').read_text());assert status['state']=='PASS_CURRENT_OWN_STAGE_DECLARED_ENGINEERING_GATES'
  tests[b]=json.loads((source/'current-validation/JUNIT_METHOD_RESULTS.json').read_text());hosts[b]=status['boundHostCommands']
  stages.append(dict(batch=b,sourceCommit=status['sourceCommit'],sourceTree=status['sourceTree'],evidenceCommit=git('rev-parse','HEAD',repo=repo),junit=status['junit'],boundHostCount=status['boundHostCount'],wholeBusinessAcceptance=False))
  for f in source.rglob('*'):
   if f.is_file():cp(f,OUT/'stages'/b/f.relative_to(source))
 # The original sealed contracts remain byte-identical references.
 for name in ['CURRENT_ACCEPTANCE_222.json','CURRENT_REQUIREMENT_TRACE_1832.jsonl','ACCEPTANCE_EXECUTION_MAP_222.json','EXTERNAL_ACCEPTANCE_PROTOCOLS_33.md']:
  cp(ORIGINAL/'contracts'/name,OUT/'original-contracts'/name)
 original=json.loads((ORIGINAL/'contracts/CURRENT_ACCEPTANCE_222.json').read_text());by_id={d['id']:d for d in original}
 mapping=json.loads((ORIGINAL/'contracts/ACCEPTANCE_EXECUTION_MAP_222.json').read_text())['items']
 rows=[]
 for x in mapping:
  d=dict(by_id[x['id']]);b=x['runAt'];refs=[]
  for entry in x.get('entries',[]):
   if entry.startswith('HOST:'):
    h=hosts[b].get(entry[5:]);assert h,(b,entry)
    refs.append(dict(entry=entry,status=h['status'],selectedCommand=h['selected'],attempts=h['attempts'],ownStageSourceCommit=next(s['sourceCommit'] for s in stages if s['batch']==b)))
   else:
    t=tests[b].get(entry);assert t and t['status']=='PASS',(b,entry)
    refs.append(dict(entry=entry,evidenceRoot='stages/P1' if b=='P1' else 'stages/'+b+'/current-validation',**t))
  d.update(currentBatch=b,currentBuildSource=BUILD,apkSha256=APK_SHA,engineeringEntryResults=refs,
   engineeringStatus='PASS_ALL_INDIVIDUALLY_NAMED_ENTRIES' if refs else 'EXTERNAL_PROTOCOL_SEPARATELY_EVALUATED',
   wholeClauseProved=False,historicalPassImported=False,
   remaining='Named assertions passed on the declared owner-stage code; original full-clause semantic acceptance, device and independent quality claims remain separately reviewable.' if refs else 'See this original ID in EXTERNAL_33_CURRENT_EVIDENCE.json; Android emulators do not replace physical or natural observation.')
  for field in ['original_text','original_text_sha256','original_card_text','original_card_text_sha256','oldRun']:
   assert d.get(field)==by_id[x['id']].get(field),(x['id'],field)
  rows.append(d)
 assert len(rows)==222 and sum(bool(x['engineeringEntryResults']) for x in rows)==189
 write('CURRENT_ACCEPTANCE_222_EXECUTION.json',rows);write('FIVE_STAGE_RESULTS.json',stages)
 # Explicitly audit the parent payload plus reviewed owner changes. No expected
 # parent SHA, old transaction journal or sealed package manifest is rewritten.
 declared=json.loads((PACKAGE/'code_manifest.json').read_text());audit=json.loads((REPO/'verification/r3251-construction/complete-research-chain/P5/ORIGINAL_PAYLOAD_DIFFERENCE_AUDIT.json').read_text())
 changed={x['path']:x for x in audit['reviewedDifferences']};controlled=[]
 for b,value in declared['batches'].items():
  for x in value['files']:
   p=REPO/x['path'];h=sha(p);mode=stat.S_IMODE(p.stat().st_mode)
   assert mode==int(x['mode'],8),x['path']
   if x['path'] in changed:assert h==changed[x['path']]['reviewedCurrentSha256']
   else:assert h==x['after_sha256'],x['path']
   controlled.append(dict(path=x['path'],batch=b,owner=x['owner'],action=x['action'],originalParentAfterSha256=x['after_sha256'],actualSha256=h,mode=oct(mode),cards=x['cards'],reviewedOwnerCorrection=changed.get(x['path']),status='PASS_REVIEWED_OWNER_CORRECTION' if x['path'] in changed else 'PASS_PARENT_EXACT_BYTES_AND_MODE'))
 assert len(controlled)==312 and len(changed)==4
 write('CONTROLLED_FILES_312_WITH_REVIEWED_OWNER_DELTA.json',controlled)
 write('ORIGINAL_PAYLOAD_DIFFERENCE_AUDIT.json',audit)
 # Keep every object individually addressable; a source checksum cannot prove
 # runtime numerical consumption, paired ablation or independent natural effect.
 objects=json.loads((ORIGINAL/'audit/report/OBJECT_ACTIONS_165.json').read_text())['objects'];objects_out=[]
 for source in objects:
  d=dict(source);checks=[]
  for binding in source['sourceBindings']:
   p=REPO/binding['path'];h=sha(p) if p.is_file() else None
   checks.append(dict(path=binding['path'],planSha256=binding['sha256'],actualSha256=h,status='MATCH_ORIGINAL_SOURCE_BINDING' if h==binding['sha256'] else 'REVIEWED_CORRECTION_REQUIRES_BINDING_REVIEW',runtimeNumericalEffectProvenByHash=False))
  d.update(currentSourceChecks=checks,currentNumericalEffect='NOT_PROVEN_BY_SOURCE_HASH',sameOpportunityPairedOrAblation='NOT_PROVEN_FOR_THIS_OBJECT_BY_SOURCE_HASH',independentNaturalEffect='NOT_PROVEN',wholeObjectAccepted=False)
  objects_out.append(d)
 assert len(objects_out)==165;write('OBJECTS_165_CURRENT_EVIDENCE.json',objects_out)
 # Do not carry forward obsolete signer/cache blockers after actual delivery.
 blockers=json.loads((old/'EXTERNAL_33_CURRENT_BLOCKERS.json').read_text());external=[]
 for source in blockers:
  d=dict(source);level=d['level']
  d['protocolTextIsOriginalUnmodifiedNotCurrentStatus']=True
  d['currentStatus']='NOT_PROVEN' if level=='NATURAL' else 'BLOCKED_PHYSICAL_DEVICE_OBSERVATION' if level=='DEVICE' else 'CURRENT_ANDROID_EVIDENCE_ATTACHED_SCOPE_REVIEW_REQUIRED'
  d['currentBlockers']=['Independent natural future observations and original quality/frequency contract are not proven.'] if level=='NATURAL' else ['No connected original physical device or its registered full observation window.','Original user 032 DB/WAL/SHM and 556 checkpoint originals are unavailable.'] if level=='DEVICE' else ['Review the complete original clause against assertion-level Android receipts; a group PASS does not sign this ID.']
  d['signedApkBlockerResolved']=True;d['formalApiBindingBlockerResolved']=True
  d['currentEvidenceRoot']='android';d['currentBuildSource']=BUILD;d['apkSha256']=APK_SHA;d['wholeClauseProved']=False
  external.append(d)
 write('EXTERNAL_33_CURRENT_EVIDENCE.json',external)
 # Copy approved CI artifact outputs, including actual frames, failed runs and
 # same-byte selected retry. Never publish the raw application databases.
 for f in (ROOT/'signed-build').rglob('*'):
  if f.is_file() and f.suffix.lower() in ('.json','.jsonl','.txt','.log','.xml','.png','.md','.csv','.tsv','.py'):
   assert not any(x in f.parts for x in ('db','private','private-inputs'))
   cp(f,OUT/'android'/f.relative_to(ROOT/'signed-build'))
 for name in ['REQUIREMENTS_1832_CURRENT_EVIDENCE.json','RECLASSIFIED_511_CURRENT_EVIDENCE.json']:
  items=json.loads((old/name).read_text())
  for d in items:
   for binding in d.get('currentReferenceChecks',[]):
    p=REPO/binding['path']
    if p.is_file():binding.update(present=True,sha256=sha(p),scope='ACTUAL_REPOSITORY')
   d.update(semanticAcceptedByThisAudit=False,historicalPassImported=False,currentBuildSource=BUILD)
  write(name,items)
 status=dict(at=datetime.datetime.now(datetime.timezone.utc).isoformat(),buildSource=BUILD,buildSourceTree=git('rev-parse',BUILD+'^{tree}'),sourceBaseline='7424eeae6354eb6599dd8498e123c059bd410c90',planCommit='0e932cc14fa0d72dd67546488915fa038f201501',restoredZipSha256='ceb5e3516452524de2519781bff04ba9f844bb95f95052bd4c2778928b856966',formalSignedApk='PRODUCED_AND_VERIFIED',apkSha256=APK_SHA,apkBytes=28909143,sameSignerOriginal032Overlay='PASS_ACTUAL_API34',sameReleaseReinstall='PASS',versionCode=577311033,package='com.starcore.research.r13',certificateSha256='d8d8cee7f406267b2bf18fc9aef0c6ab760423cf205ccd6613dd44085bcc74e7',originalSignerReused=True,replacementKeyGenerated=False,ownStageDevelopmentGradle='PASS_ALL_FIVE',ownStageUnitTests=3291,boundHostExecutions=55,engineeringEntryIdsPassed=189,originalAcceptanceIds=222,originalExternalProtocolIds=33,originalRequirementIds=1832,objects=165,allFiveEngineeringGatesPassed=True,whole222BusinessAcceptance=False,physical24h='NOT_RUN',physical72h='NOT_RUN',naturalQuality='NOT_PROVEN',realSwitchEnabledByThisTask=False,api29='PASS_FULL_MATRIX',api34='PASS_FULL_MATRIX',api35='SELECTED_ORIGINAL_SEVEN_PASSED_FULL_MATRIX_RERUN_PENDING',originalApi35FailureRetained=True,ciRun=43796780479,api35SelectedRetestRun=43801480348,originalPhysicalDatabase='NOT_AVAILABLE',missing556OriginalCheckpoints='NOT_AVAILABLE',historicalCP4CoverageOriginal='NOT_AVAILABLE',historicalCatalogOriginals='NOT_AVAILABLE')
 write('DELIVERY_STATUS.json',status)
 write('CURRENT_SOURCE_RELATION.json',{'buildCommit':BUILD,'appTree':git('rev-parse',BUILD+':app'),'currentAppTree':git('rev-parse','HEAD:app'),'productPathsDiffFromBuild':git('diff','--name-only',BUILD,'--','app','tools','scripts','gradle','build.gradle','settings.gradle','gradle.properties','gradlew','gradlew.bat'),'evidenceOnlyArchive':True,'sourceInspectionHeadBeforeFinalEvidence':git('rev-parse','HEAD')})
 write('ARTIFACT_SHA256.json',[dict(path=f.relative_to(OUT).as_posix(),bytes=f.stat().st_size,sha256=sha(f)) for f in sorted(OUT.rglob('*')) if f.is_file() and f.name!='ARTIFACT_SHA256.json'])
 print(json.dumps(status,ensure_ascii=False))

if __name__=='__main__':main()
