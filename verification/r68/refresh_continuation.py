#!/usr/bin/env python3
"""Finite reviewed R65/R66/R67 numerical continuation; no certificate migration."""
import importlib.util, json
from pathlib import Path

spec=importlib.util.spec_from_file_location('semantic','scripts/r967_semantic_identity.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
previous=json.loads(Path('release/r67_training_continuation.json').read_text())
prior_notes={x['path']:x['rationale'] for b in previous['predecessors'] for x in b['reviewed_numeric_changes']}
review={
 'DatabaseHealthActivity.java':'Move existing database health read to a background worker; error rendering and lifecycle guard only. Shared DB ownership and numeric code unchanged.',
 'ModelLearningActivity.java':'Lazy-open DbStore on the existing serial background handler; encompass model status in error handling. Import and rollback retain their original owners and authorization.',
 'ReportArchiveVerifierCP12.java':'Use ZIP deflate level 6; existing lossless per-member SHA, byte count and CRC readback stay enforced.',
 'ReportAttachmentClosureCP12.java':'Reuse the owner-verified immutable snapshot hash instead of hashing the same private file twice. Resume still verifies SHA and size; no live database mutations.',
 'ReportCatalogCP4.java':'Clarify diagnostic versus raw backup coverage; same 96 evaluation handlers, no health or qualification thresholds changed.',
 'ReportDiagnosticEvidenceR64.java':'Read-only full-snapshot diagnostic metadata and file-state observations. Every selected metadata row and text column retained, raw binary size explicitly marked. No training/inference or evidence state changes.',
 'ReportExportServiceCP4.java':'Default full diagnostic request omits the raw backup; explicit raw option remains. Add file-state member and accurate stage progress; retain snapshot, archive, CRC and SAF integrity gates.',
 'ReportPublicationCP12.java':'After publication verify the sealed single ZIP and manifest before removing redundant private compressed parts; legacy multipart keeps all parts. No learning state or external user files touched.',
 'RuntimeEventLedgerR926.java':'Rebuild only absent or mismatched projection watermarks within the same bounded issue set. Replays original immutable events with unchanged reducer; no event fabrication, rank changes or numerical training changes.',
 'r967_semantic_identity.py':'Admit exactly the reviewed R65, R66 and R67 baselines under unchanged policy/runtime; reject unreviewed hashes. No evidence or certificate migration.'
}
records=json.loads(Path(m.MANIFEST).read_text())['files'];deps=[];provenance=[]
for record in sorted(records,key=lambda x:x['path']):
 path=record['path'];sha=m.digest(Path(path).read_bytes())
 if record['role'] in ('NUMERIC','BUILD_CONTRACT'):deps.append([path,record['role'],sha])
 if record['role']!='PROVENANCE':provenance.append([path,sha])
current={x[0]:x[2] for x in deps};predecessors=[]
r67=json.loads(Path('release/r67_semantic_baseline.json').read_text());r67hash={x[0]:x[2] for x in r67['dependencies']}
for version in ('65','66','67'):
 path=Path('release/r'+version+'_semantic_baseline.json');base=json.loads(path.read_text());old={x[0]:x[2] for x in base['dependencies']};changes=[]
 for name in sorted(set(old)|set(current)):
  if old.get(name)==current.get(name):continue
  rationale=prior_notes.get(name,'') if version!='67' else ''
  if r67hash.get(name)!=current.get(name):
   note=review.get(Path(name).name)
   if not note:raise ValueError('UNREVIEWED_R68_CHANGE:'+name)
   rationale=(rationale+' R68: '+note).strip()
  if not rationale:raise ValueError('UNREVIEWED_BASELINE_DIFF:'+name)
  changes.append(dict(path=name,before_sha256=old.get(name,''),after_sha256=current.get(name,''),rationale=rationale))
 predecessors.append(dict(baseline_file=str(path),baseline_manifest_sha256=m.digest(path.read_bytes()),predecessor_training_rule_sha256=base['training_rule_sha256'],reviewed_numeric_changes=changes))
release=json.loads(Path('release/release_manifest.json').read_text())
runtime={k:release[k] for k in ('applicationId','gameCode','databaseName','minSdk','runtimeContract')}|{'databaseUserVersion':release['schema']['userVersion']}
proof=dict(scope='EXACT_SOURCE_NUMERIC_CONTINUATION_ONLY_NO_CERTIFICATE_MIGRATION',reviewed_input_provenance_sha256=m.digest(m.canonical(provenance).encode()),runtime_contract_sha256=m.digest(m.canonical(runtime).encode()),predecessors=predecessors)
Path('release/r68_training_continuation.json').write_text(json.dumps(proof,indent=2)+'\n')
print('R68_REVIEWED_EXACT_BASELINES',len(predecessors))
