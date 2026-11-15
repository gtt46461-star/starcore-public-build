#!/usr/bin/env python3
"""Actually train every exported ranker/calibrator family and compare its Java inference."""
import json, os, subprocess, sys
from pathlib import Path
import numpy as np
R=Path(__file__).resolve().parents[1];sys.path.insert(0,str(R))
from training.r929 import pipeline as p
O=R/'build/r932-model-bundle';O.mkdir(parents=True,exist_ok=True)
t=np.arange(100)[:,None,None];a=np.arange(1200)[None,:,None];f=np.arange(96)[None,None,:]
x=((t*37+a*19+f*13)%997)/996.0
# Labels are explicitly synthetic, for export/inference validation only.
y=np.zeros((100,1200))
for i in range(100):y[i,(np.arange(360)*3+i)%1200]=1
models=[{'family':'RAW_ACTION_P'}]+[p.linear_fit(x[:8],y[:8],mode=m,epochs=2) for m in ('pairwise','listwise','top_weighted')]+[p.mlp_fit(x[:8],y[:8],epochs=2)]
raw=np.linspace(-3,3,120);hit=(np.arange(120)%3==0).astype(float)
cals=[p.logistic_fit(raw,hit),p.beta_fit(raw,hit),p.isotonic_fit(raw,hit)]
path=O/'trained_parity_models.json';p.write_json(path,{'rankers':models,'calibrators':cals,'scope':'SYNTHETIC_ONLY_NO_EFFECT_EVIDENCE'})
H=R/'build/r925-host/compiled-classes';A=R/'build/r930-batch34-sql';platform=R.parent/'08_TOOLCHAIN/android-platform-36/android.jar'
cp=os.pathsep.join(map(str,[O,A,H,platform]))
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',cp,'-d',str(O),str(R/'app/src/main/java/com/ethk3/novaonepro/TrainedModelMathR932.java'),str(R/'verification/r932/TrainedMathParityHarnessR932.java')],check=True)
cp=os.pathsep.join(map(str,[O,A,H,A/'platform-classes']))
output=O/'trained_scores.bin';subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.TrainedMathParityHarnessR932',str(path),str(output)],check=True)
values=np.fromfile(output,dtype='>f8');offset=0;reports=[]
for model in models:
 java=values[offset:offset+120000].reshape(100,1200);offset+=120000;py=p.model_scores(model,x)
 error=float(np.max(np.abs(java-py)));same=np.array_equal(p.rank_order(java,x[:,:,0]),p.rank_order(py,x[:,:,0]))
 if error>1e-11 or not same:raise AssertionError((model['family'],error,same))
 reports.append({'family':model['family'],'issues':100,'action_scores':120000,'max_absolute_error':error,'all_1200_ranks_equal':same})
for model in cals:
 java=values[offset:offset+1000];offset+=1000;py=p.calibration_predict(model,(np.arange(1000)-500)/20.0)
 error=float(np.max(np.abs(java-py)))
 if error>1e-12:raise AssertionError((model['family'],error))
 reports.append({'family':model['family'],'probabilities':1000,'max_absolute_error':error})
report={'scope':'HOST_JAVA_PYTHON_SYNTHETIC_TRAINING_NOT_ANDROID_DEVICE_NOT_MODEL_EFFECT','status':'PASS','results':reports}
p.write_json(R/'reports/r932/TRAINED_MODEL_PARITY.json',report)
print('R932_TRAINED_MODEL_PARITY_PASS|ranker_scores=600000|calibrated_probabilities=3000')
