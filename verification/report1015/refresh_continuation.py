"""Refresh finite DEV compatibility only when every changed source has exact review.
Does not grant calibration, Future or certificate compatibility.
"""
from pathlib import Path
import hashlib, importlib.util, json
ROOT=Path(__file__).resolve().parents[2]
spec=importlib.util.spec_from_file_location('semantics',ROOT/'scripts/r967_semantic_identity.py')
m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m)
sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
review=json.loads((ROOT/'verification/report1015/source_review.json').read_text())
manifest=json.loads((ROOT/m.MANIFEST).read_text())
classified={x['path']:x for x in manifest['files']}
assert set(classified)==m.controlled_paths(ROOT),'UNCLASSIFIED_SOURCE'
numeric=[[p,x['role'],sha(ROOT/p)] for p,x in sorted(classified.items()) if x['role'] in ('NUMERIC','BUILD_CONTRACT')]
new={x[0]:x[2] for x in numeric}
baseline=json.loads((ROOT/'release/r77_semantic_baseline.json').read_text())
old={x[0]:x[2] for x in baseline['dependencies']}
changed={p for p in set(old)|set(new) if old.get(p)!=new.get(p)}
assert changed==set(review),'REVIEW_SOURCE_SET_MISMATCH'
for p,x in review.items():assert x['sha256']==new.get(p) and x['rationale'].strip(),'UNREVIEWED_BYTES:'+p
previous=json.loads((ROOT/'release/r77_training_continuation.json').read_text())
names=[x['baseline_file'] for x in previous['predecessors']]+['release/r77_semantic_baseline.json']
assert len(names)==len(set(names))==13
records=[]
for filename in names:
    b=json.loads((ROOT/filename).read_text());before={x[0]:x[2] for x in b['dependencies']}
    prior=next((x for x in previous['predecessors'] if x['baseline_file']==filename),None)
    reasons={x['path']:x['rationale'] for x in prior['reviewed_numeric_changes']} if prior else {}
    changes=[]
    for p in sorted(set(before)|set(new)):
        if before.get(p)==new.get(p):continue
        reason=reasons.get(p,'')
        if p in review:reason=(reason+' R78: '+review[p]['rationale']).strip()
        assert reason,'NO_REVIEW:'+p
        changes.append({'path':p,'before_sha256':before.get(p,''),'after_sha256':new.get(p,''),'rationale':reason})
    records.append({'baseline_file':filename,'baseline_manifest_sha256':sha(ROOT/filename),'predecessor_training_rule_sha256':b['training_rule_sha256'],'reviewed_numeric_changes':changes})
release=json.loads((ROOT/'release/release_manifest.json').read_text())
runtime={'applicationId':release['applicationId'],'gameCode':release['gameCode'],'databaseName':release['databaseName'],'databaseUserVersion':release['schema']['userVersion'],'minSdk':release['minSdk'],'runtimeContract':release['runtimeContract']}
provenance=[[p,sha(ROOT/p)] for p,x in sorted(classified.items()) if x['role']!='PROVENANCE']
proof={'scope':previous['scope'],'reviewed_input_provenance_sha256':m.digest(m.canonical(provenance).encode()),'runtime_contract_sha256':m.digest(m.canonical(runtime).encode()),'predecessors':records}
(ROOT/'release/r78_training_continuation.json').write_text(json.dumps(proof,ensure_ascii=False,indent=2)+'\n')
assert len(m.build(ROOT)['compatible_predecessor_rules_sha256'])==13
print('R78_EXACT_DEV_CONTINUATION_VERIFIED|predecessors=13|NO_CERTIFICATE_MIGRATION')
