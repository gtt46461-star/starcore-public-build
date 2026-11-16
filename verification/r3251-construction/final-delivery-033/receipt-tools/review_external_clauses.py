#!/usr/bin/env python3
"""Review original external IDs against concrete evidence; never sign a group."""
import hashlib,json
from pathlib import Path
ROOT=Path('/workspace/starcore-r3251-attachment-final/verification/r3251-construction/final-delivery-033')

def main():
 rows=json.loads((ROOT/'EXTERNAL_33_CURRENT_EVIDENCE.json').read_text())
 scopes={
 'R31-B01-C04#A1b':('android/android35-first/joint/JOINT-ANDROID-RECEIPT.json','Actual stop/lifecycle mechanisms ran. Full original UI stop-unfinished visible-state assertion is not inferred from the protocol group.'),
 'R31-B02-C02#A3a':('android/android35-first/original-regression/schema-stamp-acceptance.json','Actual Android SQLite restamp/reject controls and fault cells exist. Full protected trigger, WAL and checkpoint concurrency clause requires its own assertion review.'),
 'R31-B09-C02#A1b':('android/android35-first/original-regression/repair-chart-acceptance.json','All 47 chart rendering objects captured; other MainActivity panels retain their own visibility obligation.'),
 'R31-B09-C02#A2b':('android/android35-first/joint/ui-lifecycle-100.txt','100 actual tabs, backgrounds and rotations; workers terminate and windows detach. Original test explicitly does not prove VM heap retention, so whole no-leak clause is not signed.'),
 'R31-B09-C05#A1b':('android/android35-first/original-regression/repair-chart-acceptance.json','47 objects x four actual display scenarios; 72 research and 116 original frames individually verified.'),
 'R31-B12-C01#A1b':('android/android35-first/upgrade/EMU-UPGRADE-RECEIPT.json','Actual Debug same-build overlay and five confirmed process-death offsets passed. Actual original 032 Release overlay is separate; no original user data claimed.'),
 'R31-B12-C01#A2':('android/overlay/RELEASE_OVERLAY.json','Release 032 to033 preserves UID, actual old draws and integrity; old user training assets remain unavailable, so the full clause is incomplete.'),
 'R31-B12-C02#A3b':('android/android35-first/research-fault-matrix/research-fault-matrix.json','Original fourteen and research twelve actual fault cells pass on API29 and35. Original named delivery failure and exact seven-test retry remain separate; full API35 retry still tracked.')}
 proof=[]
 # This one exact original EMU clause only asks for 47-chart rendering evidence.
 # Match every original and research frame to the individual recorded hash.
 for api,folder in [(29,'android29-pass'),(34,'android34-pass'),(35,'android35-first')]:
  base=ROOT/'android'/folder/'original-regression'
  repair=json.loads((base/'repair-chart-acceptance.json').read_text())
  original=json.loads((base/'original/regression.json').read_text())
  assert repair['registeredObjects']==47 and repair['displayScenarios']==4 and repair['screenshots']==188
  assert len(repair['artifacts'])==72 and len(original['screenshots'])==116
  actual=[]
  for anchor,artifacts in [(base,repair['artifacts']),(base/'original',original['screenshots'])]:
   for a in artifacts:
    f=anchor/a['file'];assert f.is_file() and f.stat().st_size==a['bytes']
    h=hashlib.sha256(f.read_bytes()).hexdigest();assert h==a['sha256'],f
    actual.append({'path':f.relative_to(ROOT).as_posix(),'bytes':f.stat().st_size,'sha256':h})
  assert len(actual)==188
  proof.append({'api':api,'actualFrames':188,'sourceCommit':repair['sourceCommit'],'frames':actual})
 for d in rows:
  d['requiredOriginalObservation']=d['original_text'];d['currentSpecificRemaining']=d['currentBlockers'][:]
  if d['id'] in scopes:
   evidence,review=scopes[d['id']];d.update(supportingEvidence=evidence,assertionScopeReview=review)
  if d['id']=='R31-B09-C05#A1b':
   d.update(currentStatus='PASS_EXACT_ORIGINAL_EMULATOR_CLAUSE',wholeClauseProved=True,currentBlockers=[],currentSpecificRemaining=[],individualReview='47 charts x4 display scenarios on API29,34,35; all 564 screenshot hashes and sizes individually match actual frame files. No scientific quality or physical screenshot claim.',individualProof='INDIVIDUAL_47_CHART_FRAME_PROOF.json')
 (ROOT/'INDIVIDUAL_47_CHART_FRAME_PROOF.json').write_text(json.dumps(proof,indent=2)+'\n')
 (ROOT/'EXTERNAL_33_CURRENT_EVIDENCE.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2)+'\n')
 items=json.loads((ROOT/'CURRENT_ACCEPTANCE_222_EXECUTION.json').read_text());by={x['id']:x for x in rows}
 for x in items:
  if x['id'] in by:
   x['externalCurrentEvidence']=by[x['id']]
   if by[x['id']]['wholeClauseProved']:
    x.update(wholeClauseProved=True,remaining='None for this exact original EMU 47-chart rendering evidence clause. Physical and natural clauses remain separate.',individualClauseProof=by[x['id']]['individualProof'])
 (ROOT/'CURRENT_ACCEPTANCE_222_EXECUTION.json').write_text(json.dumps(items,ensure_ascii=False,indent=2)+'\n')
 status=json.loads((ROOT/'DELIVERY_STATUS.json').read_text());status.update(wholeOriginalClausesIndividuallySigned=1,wholeOriginalClausesNotYetSigned=221)
 (ROOT/'DELIVERY_STATUS.json').write_text(json.dumps(status,ensure_ascii=False,indent=2)+'\n')
 print('PASS_EXACT_47_CHART_CLAUSE_564_ACTUAL_FRAME_HASHES_REMAINING_ORIGINAL_CLAUSES_SEPARATE')

if __name__=='__main__':main()
