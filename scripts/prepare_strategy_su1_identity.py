#!/usr/bin/env python3
"""Rebind reviewed source changes using the existing closed R967/R82 identity contracts.

This does not grant predecessor qualification, change a statistical threshold, or
invent test evidence. Unknown source changes fail before the official generators run.
"""
import hashlib, importlib.util, json, re, subprocess, sys
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
BASE='f3e249ce3aef9a8975abe0be38cbe2d1183e4078'
REVIEW=ROOT/'release/strategy_su1_reviewed_inputs.json'

def sha(data):return hashlib.sha256(data).hexdigest()
def canonical(value):return json.dumps(value,ensure_ascii=False,sort_keys=True,separators=(',',':'),allow_nan=False)
def write(path,value):path.write_text(json.dumps(value,ensure_ascii=False,sort_keys=True,indent=2)+'\n')

def main():
    review=json.loads(REVIEW.read_text())
    if review['base_commit']!=BASE or review['qualification_inheritance'] is not False:raise SystemExit('SU1_SOURCE_REVIEW_SCOPE')
    pinned={x['path']:x for x in review['inputs']}
    if len(pinned)!=len(review['inputs']):raise SystemExit('SU1_DUPLICATE_REVIEW')
    for name,row in pinned.items():
        if not (ROOT/name).is_file() or sha((ROOT/name).read_bytes())!=row['sha256']:raise SystemExit('SU1_UNREVIEWED_SOURCE:'+name)
    subprocess.run([sys.executable,str(ROOT/'scripts/r926_release_manifest.py'),'--write'],check=True)
    subprocess.run([sys.executable,str(ROOT/'scripts/generate_strategy_su1_schema.py'),'--check'],check=True)
    module_spec=importlib.util.spec_from_file_location('su1_r967',ROOT/'scripts/r967_semantic_identity.py');module=importlib.util.module_from_spec(module_spec);module_spec.loader.exec_module(module)
    manifest=json.loads((ROOT/module.MANIFEST).read_text());classified={x['path']:x for x in manifest['files']};actual=module.controlled_paths(ROOT)
    for name in sorted(actual-set(classified)):
        if name=='release/strategy_su1_reviewed_inputs.json':role='PROVENANCE'
        elif name in pinned:role=pinned[name]['role']
        else:raise SystemExit('SU1_NEW_INPUT_NOT_REVIEWED:'+name)
        classified[name]={'path':name,'role':role,'rationale':'SU1 explicit source review; independent research identity, no inheritance of historical qualification.'}
    if set(classified)!=actual:raise SystemExit('SU1_MISSING_CONTROLLED_INPUT')
    tree=subprocess.check_output(['git','ls-tree','-r',BASE],cwd=ROOT,text=True);old_blobs={}
    for line in tree.splitlines():
        meta,name=line.split('\t',1);old_blobs[name]=meta.split()[2]
    provenance_exceptions={module.MANIFEST,'release/r82_training_invalidation.json','release/r982_source_calls.tsv.gz','release/r982_source_calls.manifest.json','release/strategy_su1_reviewed_inputs.json'}
    for name in sorted(actual):
        data=(ROOT/name).read_bytes();blob=hashlib.sha1(b'blob '+str(len(data)).encode()+b'\0'+data).hexdigest()
        if old_blobs.get(name)!=blob and name not in pinned and name not in provenance_exceptions:raise SystemExit('SU1_DIFF_NOT_REVIEWED:'+name)
        row=classified[name]
        if row['role'] in ('NUMERIC','BUILD_CONTRACT') and name.endswith('.java') and module.DYNAMIC.search(data.decode()):
            if name in pinned:row['dynamic_review_sha256']=sha(data)
    manifest['files']=[classified[p] for p in sorted(classified)];write(ROOT/module.MANIFEST,manifest)
    proof_path=ROOT/'release/r82_training_invalidation.json';proof=json.loads(proof_path.read_text());previous={r['path']:r for r in proof['controlled_changes']}
    old={r['path']:r for r in json.loads((ROOT/proof['controlled_baseline_file']).read_text())['files']}
    changes=[];new_roles=[]
    for name in sorted(set(old)|actual):
        before=old.get(name,{});after=classified.get(name,{});br=before.get('role','');ar=after.get('role','')
        if br=='PROVENANCE' and ar=='PROVENANCE' or not br and ar=='PROVENANCE':continue
        ah=sha((ROOT/name).read_bytes()) if after else '';bh=before.get('sha256','')
        if (bh,br)!=(ah,ar):
            prior=previous.get(name)
            if name not in pinned and not (prior and prior['after_sha256']==ah and prior['after_role']==ar):raise SystemExit('SU1_R81_DIFF_NOT_REVIEWED:'+name)
            changes.append({'path':name,'before_sha256':bh,'after_sha256':ah,'before_role':br,'after_role':ar,'reviewer':'Codex SU1 source integration','rationale':'User-authorized SU1 implementation and isolated install identity. Preserve old evidence bytes; invalidate all prior numerical qualification.'})
    for name in sorted(actual-set(old)):
        new_roles.append({'path':name,'role':classified[name]['role'],'reviewer':'Codex SU1 source integration','rationale':'Explicitly classified production source/build input for SU1. Presentation and provenance carry no prediction-quality authority.'})
    provenance=[[name,sha((ROOT/name).read_bytes())] for name in sorted(actual) if classified[name]['role']!='PROVENANCE']
    release=json.loads((ROOT/'release/release_manifest.json').read_text())
    runtime={'applicationId':release['applicationId'],'gameCode':release['gameCode'],'databaseName':release['databaseName'],'databaseUserVersion':release['schema']['userVersion'],'minSdk':release['minSdk'],'runtimeContract':release['runtimeContract']}
    proof['controlled_changes']=changes;proof['new_role_reviews']=new_roles;proof['predecessors']=[]
    proof['reviewed_input_provenance_sha256']=sha(canonical(provenance).encode());proof['runtime_contract_sha256']=sha(canonical(runtime).encode());proof['current_controlled_roles_sha256']=sha(canonical(sorted([p,r['role']] for p,r in classified.items())).encode());write(proof_path,proof)
    subprocess.run([sys.executable,str(ROOT/'scripts/r967_semantic_identity.py')],check=True)
    subprocess.run([sys.executable,str(ROOT/'scripts/r967_semantic_identity.py'),'--check'],check=True)
    print('SU1_REVIEWED_IDENTITIES_REBOUND_NO_QUALIFICATION_INHERITANCE')
if __name__=='__main__':main()
