#!/usr/bin/env python3
"""Reproducible actual-product host tests; no device/live evidence claim.
Steps compile, base, exact, numeric, clock, identity are separated for bounded executors.
"""
from pathlib import Path
import os,subprocess,argparse,json,hashlib,zipfile,shutil
p=argparse.ArgumentParser();p.add_argument('--step',choices=['compile','base','exact','numeric','clock','identity','all'],required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--android-jar',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[2];v=r/'verification/r974';o=a.out.resolve();o.mkdir(parents=True,exist_ok=True);cl=o/'classes';cl.mkdir(exist_ok=True);rt=o/'android-runtime';rt.mkdir(exist_ok=True)
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xmx768m -XX:ActiveProcessorCount=2');cp=os.pathsep.join(map(str,[cl,rt]));records=[]
meta=o/'summary.json'
if meta.exists():records=json.loads(meta.read_text())['steps']
def digest():
 h=hashlib.sha256()
 for folder in ['app/src','app/build/generated/source/r967','release']:
  for f in sorted((r/folder).rglob('*')):
   if f.is_file():h.update(str(f.relative_to(r)).encode()+b'\0'+f.read_bytes())
 return h.hexdigest()
def run(n,cmd):
 with (o/(n+'.log')).open('w') as log:
  log.write('COMMAND='+json.dumps(list(map(str,cmd)))+'\n');log.flush();ret=subprocess.run(list(map(str,cmd)),cwd=r,env=env,stdout=log,stderr=subprocess.STDOUT).returncode
 records.append(dict(name=n,exit_code=ret,source_sha256=digest(),log=n+'.log'))
 meta.write_text(json.dumps(dict(steps=records,device='NOT_RUN',live_future='NOT_RUN',scope='SYNTHETIC_INPUT_ACTUAL_PRODUCT_CODE_REAL_SQLITE_WITH_ANDROID_API_ADAPTERS'),indent=2)+'\n');print(n,ret,flush=True)
 if ret:raise SystemExit(ret)
def javac(n,sources,destination=cl):
 destination.mkdir(exist_ok=True,parents=True);lst=o/(n+'.list');lst.write_text('\n'.join(map(str,sources)))
 run(n,['javac','-encoding','UTF-8','-source','17','-target','17','-cp',cp+os.pathsep+str(a.android_jar),'-d',destination,'@'+str(lst)])
steps=['compile','base','exact','numeric','clock','identity'] if a.step=='all' else [a.step]
for step in steps:
 if step=='compile':
  shutil.rmtree(cl);cl.mkdir(parents=True)  # Never compile product against previous host adapters.
  run('semantic_generation',['python3',r/'scripts/r967_semantic_identity.py'])
  javac('product_compile',sorted((r/'app/src/main/java').rglob('*.java'))+sorted((r/'app/build/generated/source/r967').rglob('*.java'))+[r/'verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java',r/'verification/r36-stubs/com/ethk3/novaonepro/R.java'])
  with zipfile.ZipFile(a.android_jar) as z:
   for n in z.namelist():
    if n.endswith('.class'):
     f=rt/n;f.parent.mkdir(parents=True,exist_ok=True);f.write_bytes(z.read(n))
  (o/'compiled.sha256').write_text(digest()+'\n')
  print('PRODUCT_JAVA_FILES='+str(len(list((r/'app/src/main/java').rglob('*.java')))),flush=True)
  continue
 if not (o/'compiled.sha256').exists() or (o/'compiled.sha256').read_text().strip()!=digest():raise SystemExit('SOURCE_CHANGED_SINCE_COMPILE')
 bridge=r/'verification/r967/host/sqlite_bridge.py'
 if step=='base':
  javac('legacy_host_compile',sorted((r/'verification/r967/host').rglob('*.java')))
  for n,main in [('genesis','com.ethk3.novaonepro.GenesisResumeHarnessR967'),('training','com.ethk3.novaonepro.r952.TrainingTwoPhaseHarnessR967'),('registry_queue','com.ethk3.novaonepro.RegistryQueueHarnessR967'),('outbox','com.ethk3.novaonepro.r952.OutboxTransactionHarnessR967'),('maintenance','com.ethk3.novaonepro.MaintenanceContinuityHarnessR967'),('funnel','com.ethk3.novaonepro.FunnelReadOnlyHarnessR967')]:run(n,['java','-cp',cp,main,bridge])
 if step=='exact':
  # Restore actual product classes: existing fixtures must never shadow the tested gate or DAO.
  javac('exact_host_compile',sorted((v/'android').rglob('*.java'))+sorted((r/'verification/r932/android-json').rglob('*.java'))+[r/'verification/r967/host/android/content/ContentValues.java',r/'verification/r967/host/android/os/SystemClock.java',r/'verification/r967/host/android/os/CancellationSignal.java',v/'HostProductR974.java',v/'ExactRealHarnessR974.java',v/'MigrationCaptureHarnessR974.java',v/'WorldReceiptHarnessR974.java'])
  run('exact_real',['java','-cp',cp,'com.ethk3.novaonepro.ExactRealHarnessR974',bridge])
  run('migration_capture',['java','-cp',cp,'com.ethk3.novaonepro.MigrationCaptureHarnessR974',bridge])
  run('world_receipts',['java','-cp',cp,'com.ethk3.novaonepro.r952.WorldReceiptHarnessR974',bridge])
 if step=='numeric':
  javac('numeric_compile',[v/'NumericWorldHarnessR974.java']);run('numeric',['java','-cp',cp,'com.ethk3.novaonepro.r952.NumericWorldHarnessR974'])
 if step=='clock':
  d=o/'clock-adapter';javac('clock_compile',sorted((r/'verification/r973/clock-stubs').rglob('*.java'))+[r/'app/src/main/java/com/ethk3/novaonepro/CountdownClock.java',r/'verification/r973/CountdownLoopRegressionR973.java'],d)
  run('clock',['java','-cp',str(d)+os.pathsep+cp,'com.ethk3.novaonepro.CountdownLoopRegressionR973'])
 if step=='identity':
  for n,args in [('semantic_mutation',['scripts/r967_semantic_identity.py','--self-test']),('semantic_check',['scripts/r967_semantic_identity.py','--check']),('release_check',['scripts/r926_release_manifest.py','--check']),('release_drift',['scripts/r926_release_manifest.py','--drift-test'])]:run(n,['python3']+args)
