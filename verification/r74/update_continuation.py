#!/usr/bin/env python3
"""Emit exact reviewed DEV-only continuation; normal semantic build revalidates every byte.
Does not change statistical policy, migrate certificates, or grant Future compatibility.
"""
from pathlib import Path
import hashlib, importlib.util, json
r=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('semantic',r/'scripts/r967_semantic_identity.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
# Compute the proposed source dependency set before the proof exists. This is a
# review producer, not the build verifier; normal Gradle invokes unmodified build().
m.continuation=lambda *args,**kwargs:([], '')
now=m.build(r)
review={
 'GlobalResearchScheduleR932.java':'Persistent bounded circular admission scan reaches candidates after terminal studies. Active reservations, calibration data and thresholds unchanged; no relabeling, synthetic evidence, certificate or REAL authority.',
 'TrainingContinuationR973.java':'Finite exact reviewed hash capacity becomes nine solely to preserve R65-R73 development state; unknown hashes still rejected, no transitive trust or certificate/Future migration.',
 'r967_semantic_identity.py':'Explicit R65-R73 baseline allowlist and at most nine exact source-reviewed records for modern DEV continuation; unchanged policy equality, per-file delta verification and legacy bound of eight.'}
previous=json.loads((r/'release/r73_training_continuation.json').read_text())
old_records={v['baseline_file']:v for v in previous['predecessors']}
records=[]
for version in range(65,74):
 name=f'release/r{version}_semantic_baseline.json';raw=(r/name).read_bytes();before=json.loads(raw)
 old={v[0]:v[2] for v in before['dependencies']};new={v[0]:v[2] for v in now['dependencies']}
 prior={v['path']:v['rationale'] for v in old_records.get(name,{}).get('reviewed_numeric_changes',[])}
 reviews=[]
 for path in sorted(set(old)|set(new)):
  if old.get(path)==new.get(path):continue
  rationale=prior.get(path,'');delta=review.get(Path(path).name,'')
  if not (rationale or delta):raise RuntimeError('UNREVIEWED_CHANGE:'+path)
  reviews.append(dict(path=path,before_sha256=old.get(path,''),after_sha256=new.get(path,''),rationale=(rationale+' R74: '+delta).strip() if delta else rationale))
 records.append(dict(baseline_file=name,baseline_manifest_sha256=hashlib.sha256(raw).hexdigest(),predecessor_training_rule_sha256=before['training_rule_sha256'],reviewed_numeric_changes=reviews))
release=json.loads((r/'release/release_manifest.json').read_text())
runtime={'applicationId':release['applicationId'],'gameCode':release['gameCode'],'databaseName':release['databaseName'],'databaseUserVersion':release['schema']['userVersion'],'minSdk':release['minSdk'],'runtimeContract':release['runtimeContract']}
proof=dict(scope='EXACT_SOURCE_NUMERIC_CONTINUATION_ONLY_NO_CERTIFICATE_MIGRATION',reviewed_input_provenance_sha256=now['source_provenance_sha256'],runtime_contract_sha256=m.digest(m.canonical(runtime).encode()),predecessors=records)
(r/'release/r74_training_continuation.json').write_text(json.dumps(proof,indent=2)+'\n')
print('R74_REVIEWED_CONTINUATION_WRITTEN',len(records))
