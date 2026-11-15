#!/usr/bin/env python3
"""Train D0-D3/Gate9 on synthetic production captures; compare actual Java adapters."""
import json, os, subprocess, sys
from pathlib import Path
import numpy as np
R=Path(__file__).resolve().parents[1];sys.path.insert(0,str(R))
from training.r929 import pipeline as p,deep_candidates as d
O=R/'build/r932-model-bundle';rows=[json.loads(line) for line in (O/'raw_upstream.jsonl').read_text().splitlines()][:100]
raw=[json.loads(r['proofs'][0].split('=',1)[1]) for r in rows];state=np.asarray([r['states'] for r in raw]);experts=np.asarray([r['expert_logits'] for r in raw])
features=d.feature_vectors(state,experts);slow=d.slow_memory(features);labels=(np.arange(100)[:,None]+np.arange(10)[None,:])%10;teacher=d.base_probability(experts)
models=[d.train(kind,features[:24],slow[:24],experts[:24],labels[:24],teacher[:24],epochs=2) for kind in ('D0','D1','D2','D3')]
gate=d.train_gate9(state[:24],experts[:24],labels[:24],epochs=5)
model_path=O/'upstream_models.json';p.write_json(model_path,{'numbers':[d.serial(m) for m in models],'gate':d.serial(gate),'scope':'SYNTHETIC_PARITY_ONLY'})
H=R/'build/r925-host/compiled-classes';A=R/'build/r930-batch34-sql';platform=R.parent/'08_TOOLCHAIN/android-platform-36/android.jar';cp=os.pathsep.join(map(str,[O,A,H,platform]))
sources=[R/'app/src/main/java/com/ethk3/novaonepro/UpstreamModelMathR932.java',R/'verification/r932/UpstreamParityHarnessR932.java']
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',cp,'-d',str(O),*map(str,sources)],check=True)
cp=os.pathsep.join(map(str,[O,A,H,A/'platform-classes']));output=O/'upstream_scores.bin'
subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.UpstreamParityHarnessR932',str(model_path),str(O/'raw_upstream.jsonl'),str(output)],check=True)
actual=np.fromfile(output,dtype='>f8').reshape(100,1049+400+9);reports=[]
feature_error=float(np.max(np.abs(actual[:,:1049]-features)))
if feature_error>1e-12:raise AssertionError(('RAW_UPSTREAM_FEATURES',feature_error))
for i,model in enumerate(models):
 java=actual[:,1049+100*i:1049+100*(i+1)].reshape(100,10,10);expected=d.predict(model,features,slow,experts);error=float(np.max(np.abs(java-expected)))
 if error>1e-8 or (java<0).any() or np.max(np.abs(java.sum(1)-1))>1e-6 or np.max(np.abs(java.sum(2)-1))>1e-6:raise AssertionError((model['kind'],error))
 reports.append({'family':model['kind'],'issue_count':100,'max_probability_error':error,'finite_nonnegative_marginals_pass':True})
expected=d.predict_gate9(gate,state,experts)[1];error=float(np.max(np.abs(actual[:,-9:]-expected)))
if error>1e-10:raise AssertionError(('GATE9',error))
reports.append({'family':'GATE9','issue_count':100,'max_weight_error':error})
p.write_json(R/'reports/r932/UPSTREAM_MODEL_PARITY.json',{'status':'PASS','scope':'SYNTHETIC_HOST_JAVA_PYTHON_NOT_DEVICE_NOT_EFFECT','feature_error':feature_error,'results':reports})
print('R932_UPSTREAM_MODEL_PARITY_PASS|issues=100|number_probabilities=40000|gate_weights=900')
