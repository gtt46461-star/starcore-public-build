#!/usr/bin/env python3
"""Exact independent learner and Android math parity, including absent/changed evidence."""
import os,sys,json,subprocess
from pathlib import Path
import numpy as np
R=Path(__file__).resolve().parents[1];sys.path.insert(0,str(R))
from training.r929 import pipeline as p
from training.r932 import predictability as trust
D=R/'build/r932-predictability';D.mkdir(parents=True,exist_ok=True)
source=R/'build/r932-training-roundtrip/models'
for name in ('features.npy','labels.npy','issues.json'):
    target=D/name
    if not target.exists():target.symlink_to(source/name)
manifest=p.read_json(source/'dataset_manifest.json');rng=np.random.default_rng(93209)
labels=np.load(D/'labels.npy',mmap_mode='r');n=len(labels);x=rng.uniform(0,1,(n,12));selector=p.digest(b'SYNTHETIC_FROZEN_SELECTOR');schema=p.digest(b'SYNTHETIC_PREDICTABILITY_SCHEMA')
rows=[{'inputs':x[i].tolist(),'hit':int(labels[i,i%1200]),'selected_ordinal':i%1200,
       'selection_policy_sha256':selector,'schema_sha256':schema,'prediction_sha256':p.digest(str(i).encode())} for i in range(n)]
def install(captures):
    p.write_json(D/'predictability_captures.json',captures)
    manifest['predictability_captures_sha256']=p.file_digest(D/'predictability_captures.json')
    manifest.pop('dataset_contract_sha256',None);manifest['dataset_contract_sha256']=p.digest(p.canonical(manifest));p.write_json(D/'dataset_manifest.json',manifest)
os.environ['STARCORE_EXPERIMENT_REGISTRY']=str(D/'synthetic-registry')
install(rows);artifact=trust.stage(D)
assert artifact['state']=='FROZEN_RESEARCH_MODEL' and not artifact['sealed_used'] and len(artifact['folds'])==3
assert all(len(row['brier_folds'])==5 for row in artifact['folds'])
test=rng.uniform(0,1,(1000,12));fixture={'model':artifact['model'],'inputs':test.tolist(),'expected':trust.predict(artifact['model'],test).tolist()};p.write_json(D/'parity.json',fixture)
O=R/'build/r932-model-bundle';A=R/'build/r930-batch34-sql';H=R/'build/r925-host/compiled-classes';platform=R.parent/'08_TOOLCHAIN/android-platform-36/android.jar'
cp=os.pathsep.join(map(str,[O,A,H,platform]))
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',cp,'-d',str(D),str(R/'verification/r932/PredictabilityParityHarnessR932.java'),str(R/'app/src/main/java/com/ethk3/novaonepro/TrainedModelMathR932.java')],check=True)
result=subprocess.run(['java','-cp',os.pathsep.join(map(str,[D,O,A,H,A/'platform-classes'])),'com.ethk3.novaonepro.PredictabilityParityHarnessR932',str(D/'parity.json')],capture_output=True,text=True,check=True)
missing=list(rows);missing[3]=None;install(missing);assert trust.stage(D)['state']=='BLOCKED_INPUT_EVIDENCE'
changed=list(rows);changed[3]=dict(rows[3],selection_policy_sha256=p.digest(b'CHANGED'));install(changed);assert trust.stage(D)['state']=='BLOCKED_INPUT_EVIDENCE'
install(rows)
# Changing future labels cannot alter training/selection: exact same artifact is reproduced.
sealed_a,sealed_b=manifest['splits']['sealed'];future=list(rows)
for i in range(sealed_a,sealed_b):future[i]=dict(rows[i],hit=1-rows[i]['hit'])
install(future);after=trust.stage(D);assert after['model']==artifact['model'] and after['metrics']==artifact['metrics']
report={'status':'PASS','scope':'SYNTHETIC_INDEPENDENT_MODEL_HOST_NOT_EFFECT_NOT_DEVICE','five_expanding_folds_with_embargo':True,'missing_capture_blocked':True,'changed_selector_blocked':True,'future_label_changes_cannot_change_model':True,'parity':result.stdout.strip()}
p.write_json(R/'reports/r932/PREDICTABILITY_REGRESSION.json',report);print(json.dumps(report))
