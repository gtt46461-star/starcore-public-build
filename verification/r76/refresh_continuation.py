#!/usr/bin/env python3
"""Refresh only an explicitly reviewed, finite DEV continuation proof.

The review pins every changed controlled source to exact bytes. This tool cannot
accept an unreviewed source or invent a reason for a numerical dependency change.
It preserves the original ten predecessor reviews and adds the final R75 build
manifest as the eleventh exact predecessor; no certificate compatibility exists.
"""
from pathlib import Path
import hashlib, json, subprocess, importlib.util

ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('semantics',ROOT/'scripts/r967_semantic_identity.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()
review=json.loads((ROOT/'verification/r76/source_review.json').read_text())
old=json.loads(subprocess.check_output(['git','show','22e9a2ee20f10a7373a4f7eac1b6a57d6cffbad3:release/research_dependencies_r967.json'],cwd=ROOT))
manifest=json.loads((ROOT/m.MANIFEST).read_text())
classified={x['path']:x for x in manifest['files']}
assert m.controlled_paths(ROOT)==set(classified),'UNCLASSIFIED_SOURCE'
prior_sources={}
for row in old['files']:
    name=row['path']
    if row['role']=='PROVENANCE':continue
    # The exact R75 predecessor source was already independently frozen in git.
    prior_sources[name]=hashlib.sha256(subprocess.check_output(['git','show','22e9a2ee20f10a7373a4f7eac1b6a57d6cffbad3:'+name],cwd=ROOT)).hexdigest()
current={p:sha(ROOT/p) for p,x in classified.items() if x['role']!='PROVENANCE'}
changed={p for p in set(prior_sources)|set(current) if prior_sources.get(p)!=current.get(p)}
assert changed==set(review),'REVIEW_SOURCE_SET_MISMATCH:'+str(sorted(changed^set(review)))
for p,x in review.items():
    assert x['sha256']==current.get(p) and x['rationale'].strip(),'SOURCE_NOT_EXACTLY_REVIEWED:'+p
numeric=[[p,x['role'],sha(ROOT/p)] for p,x in sorted(classified.items()) if x['role'] in ('NUMERIC','BUILD_CONTRACT')]
provenance=[[p,h] for p,h in sorted(current.items())]
release=json.loads((ROOT/'release/release_manifest.json').read_text())
runtime={'applicationId':release['applicationId'],'gameCode':release['gameCode'],'databaseName':release['databaseName'],
         'databaseUserVersion':release['schema']['userVersion'],'minSdk':release['minSdk'],'runtimeContract':release['runtimeContract']}
previous=json.loads((ROOT/'release/r75_training_continuation.json').read_text())
records=[]
names=[x['baseline_file'] for x in previous['predecessors']]+['release/r75_semantic_baseline.json']
assert len(names)==len(set(names))==11
for filename in names:
    baseline=json.loads((ROOT/filename).read_text())
    old_numeric={x[0]:x[2] for x in baseline['dependencies']};new_numeric={x[0]:x[2] for x in numeric}
    existing=next((r for r in previous['predecessors'] if r['baseline_file']==filename),None)
    rationales={x['path']:x['rationale'] for x in existing['reviewed_numeric_changes']} if existing else {}
    changes=[]
    for p in sorted(set(old_numeric)|set(new_numeric)):
        if old_numeric.get(p)==new_numeric.get(p):continue
        reason=rationales.get(p,'')
        if p in review:reason=(reason+' '+review[p]['rationale']).strip()
        assert reason,'NO_REVIEWED_NUMERIC_REASON:'+p
        changes.append({'path':p,'before_sha256':old_numeric.get(p,''),'after_sha256':new_numeric.get(p,''),'rationale':reason})
    records.append({'baseline_file':filename,'baseline_manifest_sha256':sha(ROOT/filename),
                    'predecessor_training_rule_sha256':baseline['training_rule_sha256'],'reviewed_numeric_changes':changes})
proof={'scope':previous['scope'],'reviewed_input_provenance_sha256':m.digest(m.canonical(provenance).encode()),
       'runtime_contract_sha256':m.digest(m.canonical(runtime).encode()),'predecessors':records}
(ROOT/'release/r76_training_continuation.json').write_text(json.dumps(proof,ensure_ascii=False,indent=2)+'\n')
print('R76_FINITE_REVIEW_REFRESHED|predecessors=11|DEV_ONLY|NO_CERTIFICATE_MIGRATION')
