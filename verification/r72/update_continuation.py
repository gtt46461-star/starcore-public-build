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
 'ResearchPlatformStoreR929.java':'Cache only committed idempotent DDL by SQLite schema_version using the existing audited cache. Active bundle, certificate checks, revocation, promotion and numeric decisions unchanged; rollback/schema-change tests protect readiness.',
 'AlgorithmInventionLabR908.java':'Recover authentic frozen late outcomes exactly once without changing/replaying the newer VM head. Chronological update formula and DEV training implementation unchanged; verified late/duplicate/next-label regression. No inherited certificate authority.',
 'AssetGenesisEngineR915.java':'Settle valid late frozen labels without rewinding state; chronological VM update unchanged. Verified both ordering directions and repeated calls; no certificate created.',
 'AutoFetchService.java':'New target broadcasts bypass same-target tick limiter. Sole clock, DecisionGate and settlement owners retained; no reflected target or numerical change.',
 'CountdownClock.java':'Read-only same-process current anchor for resume; monotonic fetch interval. Target resolver, durable draw and model semantics unchanged.',
 'DataRetentionGovernorR900.java':'Optional draw archives use existing fair table cursor; same exact generation, archive proof, references and deletion predicates. No label or numeric change.',
 'FrozenCandidateAdapterR958.java':'Reserved capture uses remaining enclosing deadline minus commit margin, retaining preemption/PIT/model and transaction checks. No postdraw capture or retrospective probability.',
 'ReportExportServiceCP4.java':'Preserve STALE_PAGE and ARCHIVE error codes in diagnostic ACK; no data or numeric change.',
 'ResearchPopulationR966.java':'Completed DEV child may replace only its own zero-update/zero-Future parent with proxy records. Programs, pending labels and lineage retained; formal/calibration/elite/certificate protection remains. Resource retirement is not quality promotion.',
 'ResearchPredrawSettlementPumpR908.java':'Durable circular issue scan cannot stall behind an invalid first page. Existing exact settlement owners retained; no outcome acknowledged by scan cursor.',
 'DevelopmentInputCP9.java':'Buffer authenticated SCDEV input/output below digest. R71/current bytes and SHA256 identical on 5000 rows, corruption/trailing/truncation rejection and lease release checked.',
 'DevelopmentProductionCP12.java':'Read only history suffix selected by unchanged protocol; exact window equivalence tested. Stop feature computation before writer deadline, retaining immutable committed pages and same numerical inputs.',
 'ModernFamilyFactoryCP12.java':'Remove repeated hot-path schema calls after migration; checkpoint proposal before insufficient remaining preparation time. Same AST, pinned source, registration and training protocol.',
 'ResearchDirectorR952.java':'Use available bounded 850ms lane quantum for TRAIN/EVOLVE/IMPORT with 75ms commit margin. Same durable six-slot wheel and owner implementations.',
 'r967_semantic_identity.py':'Finite exact-source reviewed R65-R71 DEV continuation. Policy hash must match. No certificate or prospective-evidence migration.'}
previous=json.loads((r/'release/r71_training_continuation.json').read_text())
old_records={v['baseline_file']:v for v in previous['predecessors']}
records=[]
for version in range(65,72):
 name=f'release/r{version}_semantic_baseline.json';raw=(r/name).read_bytes();before=json.loads(raw)
 old={v[0]:v[2] for v in before['dependencies']};new={v[0]:v[2] for v in now['dependencies']}
 prior={v['path']:v['rationale'] for v in old_records.get(name,{}).get('reviewed_numeric_changes',[])}
 reviews=[]
 for path in sorted(set(old)|set(new)):
  if old.get(path)==new.get(path):continue
  rationale=prior.get(path,'');delta=review.get(Path(path).name,'')
  if not (rationale or delta):raise RuntimeError('UNREVIEWED_CHANGE:'+path)
  reviews.append(dict(path=path,before_sha256=old.get(path,''),after_sha256=new.get(path,''),rationale=(rationale+' R72: '+delta).strip() if delta else rationale))
 records.append(dict(baseline_file=name,baseline_manifest_sha256=hashlib.sha256(raw).hexdigest(),predecessor_training_rule_sha256=before['training_rule_sha256'],reviewed_numeric_changes=reviews))
release=json.loads((r/'release/release_manifest.json').read_text())
runtime={'applicationId':release['applicationId'],'gameCode':release['gameCode'],'databaseName':release['databaseName'],'databaseUserVersion':release['schema']['userVersion'],'minSdk':release['minSdk'],'runtimeContract':release['runtimeContract']}
proof=dict(scope='EXACT_SOURCE_NUMERIC_CONTINUATION_ONLY_NO_CERTIFICATE_MIGRATION',reviewed_input_provenance_sha256=now['source_provenance_sha256'],runtime_contract_sha256=m.digest(m.canonical(runtime).encode()),predecessors=records)
(r/'release/r72_training_continuation.json').write_text(json.dumps(proof,indent=2)+'\n')
print('R72_REVIEWED_CONTINUATION_WRITTEN',len(records))
