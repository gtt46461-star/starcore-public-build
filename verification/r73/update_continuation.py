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
 'ReportExportServiceCP4.java':'Reuse an exact generation-checked, hash-verified private retention snapshot; recover a fully reverified immutable reply after crash. FULL_REPORT still takes its own fresh complete snapshot. No statistical or mainline decision changes.',
 'RetentionArchiveIoCP12.java':'Bound native SQLite at 32 MiB because full installed schema exceeds 4 MiB; buffer output without changing SCROW bytes. Reuse verified snapshot and independently reverify existing immutable receipts. Deletion/generation/predicate guards unchanged.',
 'RetentionSnapshotRowsCP7.java':'Permit re-opening an already hash-verified private immutable copy only with an in-process unforgeable proof and unchanged file stamp; process restart hashes again. Same selected row reader, typed values and SCROW proof.',
 'RetentionSnapshotCacheR73.java':'Sole diagnostics journal owner caches exactly one immutable native backup for at most 256 uses/10 minutes. Revalidate table schema and every requested generation; stale copy refreshed; source remains read-only. Writer independently checks live generation and eligibility. No score, label, policy or certificate changes.',
 'r967_semantic_identity.py':'Finite reviewed R65-R72 DEV-only numeric continuation. Exact byte and policy equality review required. No certificate or Future evidence migration.'}
previous=json.loads((r/'release/r72_training_continuation.json').read_text())
old_records={v['baseline_file']:v for v in previous['predecessors']}
records=[]
for version in range(65,73):
 name=f'release/r{version}_semantic_baseline.json';raw=(r/name).read_bytes();before=json.loads(raw)
 old={v[0]:v[2] for v in before['dependencies']};new={v[0]:v[2] for v in now['dependencies']}
 prior={v['path']:v['rationale'] for v in old_records.get(name,{}).get('reviewed_numeric_changes',[])}
 reviews=[]
 for path in sorted(set(old)|set(new)):
  if old.get(path)==new.get(path):continue
  rationale=prior.get(path,'');delta=review.get(Path(path).name,'')
  if not (rationale or delta):raise RuntimeError('UNREVIEWED_CHANGE:'+path)
  reviews.append(dict(path=path,before_sha256=old.get(path,''),after_sha256=new.get(path,''),rationale=(rationale+' R73: '+delta).strip() if delta else rationale))
 records.append(dict(baseline_file=name,baseline_manifest_sha256=hashlib.sha256(raw).hexdigest(),predecessor_training_rule_sha256=before['training_rule_sha256'],reviewed_numeric_changes=reviews))
release=json.loads((r/'release/release_manifest.json').read_text())
runtime={'applicationId':release['applicationId'],'gameCode':release['gameCode'],'databaseName':release['databaseName'],'databaseUserVersion':release['schema']['userVersion'],'minSdk':release['minSdk'],'runtimeContract':release['runtimeContract']}
proof=dict(scope='EXACT_SOURCE_NUMERIC_CONTINUATION_ONLY_NO_CERTIFICATE_MIGRATION',reviewed_input_provenance_sha256=now['source_provenance_sha256'],runtime_contract_sha256=m.digest(m.canonical(runtime).encode()),predecessors=records)
(r/'release/r73_training_continuation.json').write_text(json.dumps(proof,indent=2)+'\n')
print('R73_REVIEWED_CONTINUATION_WRITTEN',len(records))
