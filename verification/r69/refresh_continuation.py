#!/usr/bin/env python3
"""Finite reviewed R65/R66/R67/R68 numerical continuation; no certificate migration."""
import importlib.util, json
from pathlib import Path

spec=importlib.util.spec_from_file_location('semantic','scripts/r967_semantic_identity.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
previous=json.loads(Path('release/r68_training_continuation.json').read_text())
prior_notes={x['path']:x['rationale'] for b in previous['predecessors'] for x in b['reviewed_numeric_changes']}
review={
 'ReportTaskControlR69.java':'Durable report-only cancellation and explicit deletion under the existing sole report owner. Cancel queued exports and SAF; preserve database, model and retention state and external saved files.',
 'ReportExportServiceCP4.java':'User-request generation fence rejects pre-reset in-memory requests; handle durable stop/clear controls before new work and defer retention for interactive reports. No numerical or qualification changes.',
 'ReportRequestQueueCP12.java':'Exact-ID removal and interactive selection retain all deferred retention requests and FIFO metadata; no learning owner changes.',
 'ReportJobJournalCP12.java':'Expose owner-lock check and explicit current-report pointer detach for deletion; no business state mutation.',
 'ReportPublicationCP12.java':'Explicit user deletion bypasses soft share grants but respects live reader locks; detached private report deletion can resume after a crash. No model/database/external-file deletion.',
 'r967_semantic_identity.py':'Finite exact reviewed R65/R66/R67/R68 continuation only; unchanged policy/runtime; no certificate or future-evidence migration.'
}

records=json.loads(Path(m.MANIFEST).read_text())['files'];deps=[];provenance=[]
for record in sorted(records,key=lambda x:x['path']):
 path=record['path'];sha=m.digest(Path(path).read_bytes())
 if record['role'] in ('NUMERIC','BUILD_CONTRACT'):deps.append([path,record['role'],sha])
 if record['role']!='PROVENANCE':provenance.append([path,sha])
current={x[0]:x[2] for x in deps};predecessors=[]
r67=json.loads(Path('release/r68_semantic_baseline.json').read_text());r67hash={x[0]:x[2] for x in r67['dependencies']}
for version in ('65','66','67','68'):
 path=Path('release/r'+version+'_semantic_baseline.json');base=json.loads(path.read_text());old={x[0]:x[2] for x in base['dependencies']};changes=[]
 for name in sorted(set(old)|set(current)):
  if old.get(name)==current.get(name):continue
  rationale=prior_notes.get(name,'') if version!='68' else ''
  if r67hash.get(name)!=current.get(name):
   note=review.get(Path(name).name)
   if not note:raise ValueError('UNREVIEWED_R69_CHANGE:'+name)
   rationale=(rationale+' R69: '+note).strip()
  if not rationale:raise ValueError('UNREVIEWED_BASELINE_DIFF:'+name)
  changes.append(dict(path=name,before_sha256=old.get(name,''),after_sha256=current.get(name,''),rationale=rationale))
 predecessors.append(dict(baseline_file=str(path),baseline_manifest_sha256=m.digest(path.read_bytes()),predecessor_training_rule_sha256=base['training_rule_sha256'],reviewed_numeric_changes=changes))
release=json.loads(Path('release/release_manifest.json').read_text())
runtime={k:release[k] for k in ('applicationId','gameCode','databaseName','minSdk','runtimeContract')}|{'databaseUserVersion':release['schema']['userVersion']}
proof=dict(scope='EXACT_SOURCE_NUMERIC_CONTINUATION_ONLY_NO_CERTIFICATE_MIGRATION',reviewed_input_provenance_sha256=m.digest(m.canonical(provenance).encode()),runtime_contract_sha256=m.digest(m.canonical(runtime).encode()),predecessors=predecessors)
Path('release/r69_training_continuation.json').write_text(json.dumps(proof,indent=2)+'\n')
print('R69_REVIEWED_EXACT_BASELINES',len(predecessors))
