"""Compares Python results with the exact Java implementation compiled into the APK."""
import hashlib,json,os,subprocess,sys
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[2];e=ROOT/'build/r925-host';bundle=Path((e/'latest_training_bundle.txt').read_text())
android=Path(sys.argv[1]);cp=str(e/'compiled-classes')+os.pathsep+str(android)
for name in ['RankerParityHarness','DeepParityHarness']:
 subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',cp,'-d',str(e/'compiled-classes'),str(ROOT/'verification/r925'/f'{name}.java')],check=True)
results={}
for name,inputfile,expectedfile in [('RankerParityHarness','parity_input.tsv','parity_expected.npy'),('DeepParityHarness','deep_parity.tsv','deep_parity_expected.npy')]:
 manifest=json.loads((bundle/'bundle_manifest.json').read_text())
 for filename in (inputfile,expectedfile):
  expected_hash=manifest['files'].get(filename)
  actual_hash=hashlib.sha256((bundle/filename).read_bytes()).hexdigest()
  if not expected_hash or actual_hash!=expected_hash:
   raise ValueError('PARITY_INPUT_INTEGRITY_FAILED:'+filename)
 out=subprocess.check_output(['java','-cp',cp,'com.ethk3.novaonepro.'+name,str(bundle/inputfile)],text=True)
 (e/(name+'.tsv')).write_text(out)
 expected=np.load(bundle/expectedfile);actual=np.fromstring(out,sep=' ')
 if actual.size!=expected.size:raise ValueError('PARITY_SCORE_COUNT_MISMATCH:'+name+':'+str(actual.size)+'/'+str(expected.size))
 if not np.isfinite(actual).all():raise ValueError('PARITY_NONFINITE_OUTPUT:'+name)
 actual=actual.reshape(expected.shape)
 error=float(abs(actual-expected).max());assert error<1e-9,(name,error)
 results[name]={'scores':int(expected.size),'max_abs_error':error,'rank1_match':float(np.mean(np.argmax(actual,axis=-1)==np.argmax(expected,axis=-1))),'status':'PASS','android_device':'NOT_RUN'}
(e/'R925_NUMERIC_PARITY.json').write_text(json.dumps(results,indent=2));print(json.dumps(results))
# Expert surface inversion is checked on random logits; corrupted nonlinear means are rejected.
sys.path.insert(0,str(ROOT/'training/r925'));from deep_candidates import surfaces
from pipeline import ACTIONS
rng=np.random.default_rng(5925);raw=rng.normal(size=(2,9,10,10));x=np.zeros((2,1200,96))
for i,a in enumerate(ACTIONS):
 for expert in range(9):x[:,i,25+5*expert]=raw[:,expert,a[0]-1][:,a[1:]-1].mean(1)
f,slow,reconstructed=surfaces(x);assert np.max(abs(raw-reconstructed))<1e-10
x[0,0,25]+=.1
try:surfaces(x);raise AssertionError('corrupt expert schema accepted')
except ValueError:pass
print('NINE_EXPERT_FROZEN_SURFACE_INVERSION=PASS|SCHEMA_CORRUPTION_REJECTED=PASS')
# Nonzero gate weights test the actual routing arithmetic, independent of whether fixture training learned signal.
from deep_candidates import gate_predict
from r811_autonomous_research import encode_artifact
m={'mean':rng.normal(size=96),'scale':rng.uniform(.5,2,size=96),'weight':rng.normal(0,.2,size=(9,96)),'bias':rng.normal(size=9)}
gate_x=rng.normal(size=(1,96));experts=rng.normal(size=(1,9,10,10));expected=gate_predict(m,gate_x,experts).reshape(-1)
(e/'gate_test.bin').write_bytes(encode_artifact('R925_GATE9','202609060001',{'feature_mean':m['mean'],'feature_scale':m['scale'],'gate_W':m['weight'],'gate_b':m['bias']}))
(e/'gate_test.tsv').write_text('\t'.join(map(str,gate_x.flatten()))+'\n'+'\t'.join(map(str,experts.flatten()))+'\n')
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',cp,'-d',str(e/'compiled-classes'),str(ROOT/'verification/r925/GateParityHarness.java')],check=True)
actual=np.fromstring(subprocess.check_output(['java','-cp',cp,'com.ethk3.novaonepro.GateParityHarness',str(e/'gate_test.bin'),str(e/'gate_test.tsv')],text=True),sep=' ')
error=float(abs(expected-actual).max());assert error<1e-9,error
results['GateParityHarness']={'scores':100,'max_abs_error':error,'status':'PASS','pit_and_nonfinite_rejection':'PASS','android_device':'NOT_RUN'}
(e/'R925_NUMERIC_PARITY.json').write_text(json.dumps(results,indent=2));print('GATE9_JAVA_NUMPY_PARITY=PASS|MAX_ERROR='+str(error))
