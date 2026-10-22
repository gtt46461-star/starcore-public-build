#!/usr/bin/env python3
"""Refresh only exact manually reviewed R79 DEV compatibility. Never migrate Future."""
from pathlib import Path
import hashlib,importlib.util,json
R=Path(__file__).resolve().parents[2]
s=importlib.util.spec_from_file_location('semantic',R/'scripts/r967_semantic_identity.py');m=importlib.util.module_from_spec(s);s.loader.exec_module(m)
H=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
review=json.loads((R/'verification/report1015_r79/source_review.json').read_text())
classified={x['path']:x['role']for x in json.loads((R/m.MANIFEST).read_text())['files']}
assert set(classified)==m.controlled_paths(R),'UNCLASSIFIED_SOURCE'
new={p:H(R/p)for p,role in classified.items()if role in('NUMERIC','BUILD_CONTRACT')}
baseline=json.loads((R/'release/r78_semantic_baseline.json').read_text());before={x[0]:x[2]for x in baseline['dependencies']}
assert {p for p in set(before)|set(new)if before.get(p)!=new.get(p)}==set(review),'R79_REVIEW_SET_DRIFT'
for p,x in review.items():assert x['sha256']==new[p]and x['rationale'].strip(),'R79_REVIEW_BYTES:'+p
previous=json.loads((R/'release/r78_training_continuation.json').read_text());records=[]
names=[x['baseline_file']for x in previous['predecessors']]+['release/r78_semantic_baseline.json'];assert len(names)==len(set(names))==14
for name in names:
    b=json.loads((R/name).read_text());old={x[0]:x[2]for x in b['dependencies']}
    prior=next((x for x in previous['predecessors']if x['baseline_file']==name),None)
    reasons={x['path']:x['rationale']for x in prior['reviewed_numeric_changes']}if prior else {}
    changes=[]
    for p in sorted(set(old)|set(new)):
        if old.get(p)==new.get(p):continue
        reason=reasons.get(p,'')
        if p in review:reason=(reason+' R79: '+review[p]['rationale']).strip()
        assert reason,'MISSING_EXACT_REVIEW:'+p
        changes.append({'path':p,'before_sha256':old.get(p,''),'after_sha256':new.get(p,''),'rationale':reason})
    records.append({'baseline_file':name,'baseline_manifest_sha256':H(R/name),'predecessor_training_rule_sha256':b['training_rule_sha256'],'reviewed_numeric_changes':changes})
release=json.loads((R/'release/release_manifest.json').read_text())
runtime={k:release[k]for k in ['applicationId','gameCode','databaseName','minSdk','runtimeContract']};runtime['databaseUserVersion']=release['schema']['userVersion']
provenance=[[p,H(R/p)]for p,role in sorted(classified.items())if role!='PROVENANCE']
proof={'scope':previous['scope'],'reviewed_input_provenance_sha256':m.digest(m.canonical(provenance).encode()),'runtime_contract_sha256':m.digest(m.canonical(runtime).encode()),'predecessors':records}
(R/'release/r79_training_continuation.json').write_text(json.dumps(proof,ensure_ascii=False,indent=2)+'\n')
assert len(m.build(R)['compatible_predecessor_rules_sha256'])==14
print('R79_EXACT_14_DEV_PREDECESSORS|CERTIFICATE_CALIBRATION_FUTURE_MIGRATION=0')
