#!/usr/bin/env python3
"""Audit actual final bytes and preserve per-ID evidence boundaries without importing PASS."""
import collections,hashlib,json,os,shutil,stat,subprocess
from pathlib import Path
from run_step import REPO,PACKAGE,ROOT,sha,now
ORIGINAL=Path('/workspace/starcore-r3251-evidence');OUT=ROOT/'delivery-audit';OUT.mkdir(exist_ok=True)
def write(name,value):
 p=OUT/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n')
manifest=json.loads((PACKAGE/'code_manifest.json').read_text());controlled=[]
for batch,value in manifest['batches'].items():
 for row in value['files']:
  path=REPO/row['path'];actual=sha(path) if path.is_file() else None;mode=stat.S_IMODE(path.stat().st_mode) if path.exists() else None
  controlled.append(dict(batch=batch,path=row['path'],owner=row['owner'],action=row['action'],sha256=actual,expectedSha256=row['after_sha256'],mode=oct(mode) if mode is not None else None,status='PASS_EXACT_FINAL_BYTES_AND_MODE' if actual==row['after_sha256'] and mode==int(row['mode'],8) else 'MISMATCH',cards=row['cards']))
assert len(controlled)==312 and all(x['status']=='PASS_EXACT_FINAL_BYTES_AND_MODE' for x in controlled)
write('CONTROLLED_FILES_312.json',controlled)
# Every current acceptance retains original text, hashes, oldRun and an explicit remaining condition.
acceptance=[];stages=[]
for b in ['P1','P2','P3','P4','P5']:
 base=ORIGINAL if b=='P1' else ROOT
 current=json.loads((base/(b+'-acceptance.json')).read_text());summary=json.loads((base/(b+'-junit-summary.json')).read_text())
 acceptance.extend(current);stages.append(dict(batch=b,junit=summary['counts'],gradleCommands=summary['gradleCommands'],wholeBusinessClauseProved=False))
original=json.loads((PACKAGE/'contracts/CURRENT_ACCEPTANCE_222.json').read_text());by_id={x['id']:x for x in original};assert len(acceptance)==len(by_id)==222
for row in acceptance:
 old=by_id[row['id']]
 for field in ['original_text','original_text_sha256','original_card_text','original_card_text_sha256','oldRun']:
  assert row.get(field)==old.get(field),(row['id'],field)
 assert row['historicalPassImported'] is False and row['wholeClauseProved'] is False
write('CURRENT_ACCEPTANCE_222_EXECUTION.json',acceptance);write('FIVE_STAGE_RESULTS.json',stages)
# The 165 action records are tied to exact actual source bytes; static existence is a separate axis.
objects=json.loads((PACKAGE/'audit/report/OBJECT_ACTIONS_165.json').read_text())['objects'];results=[]
for original in objects:
 checks=[]
 for binding in original['sourceBindings']:
  name=binding['path'];p=REPO/name;actual=sha(p) if p.is_file() else None
  checks.append(dict(path=name,expectedSha256=binding['sha256'],actualSha256=actual,methods=binding.get('methods',[]),status='MATCH_CURRENT_SOURCE_DECLARATIONS' if actual==binding['sha256'] else 'MISSING_OR_CHANGED',runtimeConsumptionProvenByThisCheck=False))
 row=dict(original);row.update(currentSourceChecks=checks,currentSourceBindingStatus='ALL_DECLARED_BYTES_MATCH' if checks and all(x['status']=='MATCH_CURRENT_SOURCE_DECLARATIONS' for x in checks) else 'REVIEW_REQUIRED',currentProductionSnapshot='NOT_CAPTURED_NO_ORIGINAL_032_DATABASE_OR_DEVICE',currentNumericalEffect='NOT_PROVEN_BY_STATIC_BINDING',sameOpportunityPairedOrAblation='NOT_PROVEN_FOR_THIS_OBJECT_BY_STATIC_BINDING',independentNaturalEffect='NOT_PROVEN',providedProductionRuntimeIsHistorical=True,wholeObjectAccepted=False)
 results.append(row)
assert len(results)==165 and all(x['currentSourceBindingStatus']=='ALL_DECLARED_BYTES_MATCH' for x in results)
write('OBJECTS_165_CURRENT_EVIDENCE.json',results)
# Requirements stay individually addressable. Resolve source/plan references but never turn location into execution.
for source_name,output_name,expected in [('contracts/CURRENT_REQUIREMENT_TRACE_1832.jsonl','REQUIREMENTS_1832_CURRENT_EVIDENCE.json',1832),('contracts/RECLASSIFIED_REQUIREMENTS_511.json','RECLASSIFIED_511_CURRENT_EVIDENCE.json',511)]:
 source=PACKAGE/source_name
 rows=[json.loads(line) for line in source.read_text().splitlines() if line.strip()] if source.suffix=='.jsonl' else json.loads(source.read_text())['items']
 assert len(rows)==expected;current=[]
 for original in rows:
  checks=[]
  for binding in original.get('bindings',[]):
   if not isinstance(binding,dict):continue
   name=binding.get('path');name=name.split('#',1)[0] if isinstance(name,str) else None
   if not name:continue
   resolved=REPO/name if (REPO/name).is_file() else PACKAGE/name
   exists=resolved.is_file();checks.append(dict(path=name,present=exists,sha256=sha(resolved) if exists else None,scope='ACTUAL_REPOSITORY' if exists and resolved.is_relative_to(REPO) else 'PLAN_REFERENCE' if exists else 'MISSING_REFERENCE',doesNotProveRuntime=True))
  row=dict(original);row.update(currentReferenceChecks=checks,semanticAcceptedByThisAudit=False,historicalPassImported=False,currentRemaining=original.get('specific_remaining_gap') or 'Requires assertion-level review of the full original requirement; named engineering tests, Android/device receipts and independent quality remain separate.')
  current.append(row)
 write(output_name,current);shutil.copy2(source,OUT/source.name)
summary=dict(at=now(),sourceCommit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=REPO,text=True).strip(),sourceTree=subprocess.check_output(['git','rev-parse','HEAD^{tree}'],cwd=REPO,text=True).strip(),parentPlanCommit='0e932cc14fa0d72dd67546488915fa038f201501',parentPackageSha256='ceb5e3516452524de2519781bff04ba9f844bb95f95052bd4c2778928b856966',reviewedManifestSha256=sha(PACKAGE/'code_manifest.json'),originalPayloadPaths=312,originalActualChangedPayloads=sum(x['action']!='PRESERVE_REFERENCE' for x in controlled),preservedPayloadsAreNotNewImplementation=sum(x['action']=='PRESERVE_REFERENCE' for x in controlled),fileCards=410,acceptanceIds=222,engineeringEntryStatus=dict(collections.Counter(x['engineeringStatus'] for x in acceptance)),objects=165,originalRequirements=1832,reclassifiedRequirements=511,physicalDevice='NOT_CONNECTED',original032Database='NOT_UPLOADED_WITH_ORIGINAL_REPORTS',missing556Checkpoints='NO_ORIGINAL_BYTES_AVAILABLE',physical24h='NOT_RUN',physical72h='NOT_RUN',naturalQuality='NOT_PROVEN',wholeBusinessAcceptance=False)
write('SUMMARY.json',summary)
write('ARTIFACT_SHA256.json',[dict(path=p.relative_to(OUT).as_posix(),bytes=p.stat().st_size,sha256=sha(p)) for p in sorted(OUT.rglob('*')) if p.is_file() and p.name!='ARTIFACT_SHA256.json'])
print(json.dumps(summary,ensure_ascii=False))
