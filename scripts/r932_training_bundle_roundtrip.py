#!/usr/bin/env python3
"""Full synthetic production capture -> staged trainers -> signed package -> phone Java loader."""
import json,os,subprocess,sys,time
from pathlib import Path
R=Path(__file__).resolve().parents[1];sys.path.insert(0,str(R))
from cryptography.hazmat.primitives import serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from training.r929 import pipeline as p
from training.r932.export_bundle import export
O=R/'build/r932-model-bundle';D=R/'build/r932-training-roundtrip';D.mkdir(parents=True,exist_ok=True)
A=R/'build/r930-batch34-sql';H=R/'build/r925-host/compiled-classes'
platform=Path(os.environ.get('ANDROID_PLATFORM_JAR',R.parent/'08_TOOLCHAIN/android-platform-36/android.jar'))
if not platform.is_file():raise SystemExit('R932_ROUNDTRIP_ANDROID_PLATFORM_REQUIRED:'+str(platform))
# Functional SQLite/ContentValues adapters in O/A shadow Android methods. The platform still
# supplies interfaces such as Cursor at runtime; compilation alone does not make them available.
cp=os.pathsep.join(map(str,[O,A,H,platform]))
production=['ModelBundleParametersR932','ModelBundleRuntimeR932','RuntimeModelContractR932','UpstreamModelMathR932','SlowStateR932','V51Models','RawTrainingInputR932','V51PredictabilityEngine','V51ActionRankerEngine','V51PermutationNumberModelEngine','V51ExpertGateEngine']
sources=[R/'app/src/main/java/com/ethk3/novaonepro'/f'{name}.java' for name in production]+[R/'verification/r932'/f'{name}.java' for name in ('TrainingBundleHarnessR932','RawUpstreamHarnessR932')]
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-cp',cp,'-d',str(O),*map(str,sources)],check=True)
cp=os.pathsep.join(map(str,[O,A,H,A/'platform-classes',platform]))
contract=D/'runtime_contract.json';source=D/'production_synthetic_capture.jsonl'
subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.TrainingBundleHarnessR932',str(contract)],check=True)
subprocess.run(['java','-Xmx1024m','-cp',cp,'com.ethk3.novaonepro.RawUpstreamHarnessR932',str(source),'full-export'],check=True)
os.environ['STARCORE_EXPERIMENT_REGISTRY']=str(D/'synthetic-registry-stacked-r2')
models=D/'models';stages={}
for name,run in [('dataset',lambda:p.stage_dataset(source,models)),('number',lambda:p.stage_number(models)),('gate',lambda:p.stage_gate(models)),('ranker',lambda:p.stage_ranker(models)),('calibration',lambda:p.stage_calibration(models)),('predictability',lambda:p.stage_predictability(models)),('policy',lambda:p.stage_policy(models)),('evaluate',lambda:p.stage_evaluate(models))]:
 start=time.monotonic();result=run();stages[name]={'completed':True,'seconds':time.monotonic()-start};print('STAGE_'+name.upper()+'=COMPLETE',flush=True)
key=rsa.generate_private_key(public_exponent=65537,key_size=2048);private=D/'synthetic-test-owner-key.pem';public=D/'synthetic-test-owner-key.der'
try:
 private.write_bytes(key.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption()));private.chmod(0o600)
 public.write_bytes(key.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo))
 archive=D/'synthetic-trained-bundle.zip';packaged=export(models,contract,private,archive,4102444800000)
 subprocess.run(['java','-cp',cp,'com.ethk3.novaonepro.TrainingBundleHarnessR932',str(archive),str(public),str(R/'verification/r925/sqlite_bridge.py')],check=True)
finally:
 if private.exists():private.unlink() # Test key is never a deliverable, including failed runs.
report={'status':'PASS','scope':'SYNTHETIC_END_TO_END_HOST_ENGINEERING_NOT_FUTURE_EFFECT_NOT_DEVICE','stages':stages,'package':packaged}
p.write_json(R/'reports/r932/TRAINING_BUNDLE_ROUNDTRIP.json',report);print('R932_TRAINING_BUNDLE_ROUNDTRIP_PASS')
