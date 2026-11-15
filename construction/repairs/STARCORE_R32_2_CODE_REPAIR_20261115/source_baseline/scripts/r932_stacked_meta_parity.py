#!/usr/bin/env python3
import os,sys,struct,subprocess
from pathlib import Path
import numpy as np
R=Path(__file__).resolve().parents[1];sys.path.insert(0,str(R))
from training.r929 import pipeline as p,deep_candidates as d
from training.r932.stacking import rebuild
D=R/'build/r932-stacked-meta';D.mkdir(parents=True,exist_ok=True)
x=np.load(R/'build/r932-training-roundtrip/models/features.npy',mmap_mode='r');rng=np.random.default_rng(93212);count=100
q=d.sinkhorn(rng.normal(size=(count,100)),60)[0];g=d.softmax(rng.normal(size=(count,9)),1)
with (D/'cases.bin').open('wb') as stream:
    stream.write(struct.pack('>i',count))
    for t in range(count):
        expected=rebuild(x[t:t+1],q[t:t+1],g[t:t+1],p.ACTIONS)[0]
        stream.write(np.asarray(q[t],dtype='>f8').tobytes());stream.write(np.asarray(g[t],dtype='>f8').tobytes());stream.write(np.asarray(expected,dtype='>f8').tobytes())
O=R/'build/r932-model-bundle';H=R/'build/r925-host/compiled-classes';A=R/'build/r930-batch34-sql';platform=R.parent/'08_TOOLCHAIN/android-platform-36/android.jar'
cp=os.pathsep.join(map(str,[O,A,H,platform]))
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',cp,'-d',str(D),str(R/'verification/r932/StackedMetaHarnessR932.java')],check=True)
run=subprocess.run(['java','-Xmx768m','-cp',os.pathsep.join(map(str,[D,O,A,H,A/'platform-classes'])),'com.ethk3.novaonepro.StackedMetaHarnessR932',str(D/'cases.bin')],capture_output=True,text=True,check=True)
p.write_json(R/'reports/r932/STACKED_META_PARITY.json',{'status':'PASS','scope':'PRODUCTION_BUILDER_SYNTHETIC_HOST_NOT_DEVICE','issues':count,'meta_values':count*1200*96,'result':run.stdout.strip()})
print(run.stdout,end='')
