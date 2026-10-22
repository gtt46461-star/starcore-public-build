#!/usr/bin/env python3
"""Finite reviewed R65/R66/R67/R68/R69 numerical continuation; no certificate migration."""
import importlib.util, json
from pathlib import Path

spec=importlib.util.spec_from_file_location('semantic','scripts/r967_semantic_identity.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
previous=json.loads(Path('release/r69_training_continuation.json').read_text())
prior_notes={x['path']:x['rationale'] for b in previous['predecessors'] for x in b['reviewed_numeric_changes']}
review={'AutoFetchService.java': 'Move Android memory-callback persistence onto the existing guard handler; bounded retention retries, independent quiet reclaim and one coalesced research continuation on the existing handler. No score, loss, label, model, gate or certificate change.', 'BusinessReconcilerR36.java': 'Retry only documented request-write deadline/lock contention through the existing watchdog; keep immutable target deadline, settlement queue and fatal integrity errors.', 'FrozenCandidateAdapterR958.java': 'Raise capture execution slice from an admission-cost estimate to 400ms inside the existing 1200ms/preemption bound. Unchanged PIT, model scoring, transaction atomicity and future-only capture.', 'ReportDiagnosticEvidenceR64.java': 'Include calibration model and failure metadata in a read-only report; no training or qualification side effects.', 'ReportExportServiceCP4.java': 'Timestamp the immutable snapshot immediately after backup before hash I/O. Diagnostic freshness only.', 'ResearchCaptureQueueR946.java': 'Recover due calibration reservations and reviewed predecessor learning runs; do not close a target with an uncommitted reservation. No evidence migration or postdraw fill.', 'ResearchPredrawCaptureR923.java': 'Serve already-admitted consecutive prospective capture before atom discovery on the same bounded owner. All model and data-role rules retained.', 'StorageReclaimR943.java': 'Allow quiet checkpoint/reclaim when critical owners are idle; retry contended attempts without a false completed timestamp. No logical deletion changes.', 'r967_semantic_identity.py': 'Finite exact reviewed R65-R69 development continuation only. Same numerical functions and policy; no certificate or future-evidence inheritance.'}
review.update({
 'LanePolicyCompiler.java':'Retire prior-target process cache entries when the sole DecisionGate starts a new issue; same-target reset behavior and all numerical functions unchanged. Future in-flight target entries preserved.',
 'CompiledActionContract.java':'Retire prior-target cached contracts and counters with the existing new-target reset. No durable authorization, immutable record or settlement changed.'
})


records=json.loads(Path(m.MANIFEST).read_text())['files'];deps=[];provenance=[]
for record in sorted(records,key=lambda x:x['path']):
 path=record['path'];sha=m.digest(Path(path).read_bytes())
 if record['role'] in ('NUMERIC','BUILD_CONTRACT'):deps.append([path,record['role'],sha])
 if record['role']!='PROVENANCE':provenance.append([path,sha])
current={x[0]:x[2] for x in deps};predecessors=[]
r67=json.loads(Path('release/r69_semantic_baseline.json').read_text());r67hash={x[0]:x[2] for x in r67['dependencies']}
for version in ('65','66','67','68','69'):
 path=Path('release/r'+version+'_semantic_baseline.json');base=json.loads(path.read_text());old={x[0]:x[2] for x in base['dependencies']};changes=[]
 for name in sorted(set(old)|set(current)):
  if old.get(name)==current.get(name):continue
  rationale=prior_notes.get(name,'') if version!='69' else ''
  if r67hash.get(name)!=current.get(name):
   note=review.get(Path(name).name)
   if not note:raise ValueError('UNREVIEWED_R70_CHANGE:'+name)
   rationale=(rationale+' R70: '+note).strip()
  if not rationale:raise ValueError('UNREVIEWED_BASELINE_DIFF:'+name)
  changes.append(dict(path=name,before_sha256=old.get(name,''),after_sha256=current.get(name,''),rationale=rationale))
 predecessors.append(dict(baseline_file=str(path),baseline_manifest_sha256=m.digest(path.read_bytes()),predecessor_training_rule_sha256=base['training_rule_sha256'],reviewed_numeric_changes=changes))
release=json.loads(Path('release/release_manifest.json').read_text())
runtime={k:release[k] for k in ('applicationId','gameCode','databaseName','minSdk','runtimeContract')}|{'databaseUserVersion':release['schema']['userVersion']}
proof=dict(scope='EXACT_SOURCE_NUMERIC_CONTINUATION_ONLY_NO_CERTIFICATE_MIGRATION',reviewed_input_provenance_sha256=m.digest(m.canonical(provenance).encode()),runtime_contract_sha256=m.digest(m.canonical(runtime).encode()),predecessors=predecessors)
Path('release/r70_training_continuation.json').write_text(json.dumps(proof,indent=2)+'\n')
print('R70_REVIEWED_EXACT_BASELINES',len(predecessors))
