#!/usr/bin/env python3
"""Compile the actual working product and run real-SQLite host regression tests."""
from pathlib import Path
import argparse,json,os,subprocess,zipfile,shutil
p=argparse.ArgumentParser();p.add_argument('--android-jar',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
v=Path(__file__).resolve().parent;root=v.parents[1];out=a.out.resolve();out.mkdir(parents=True,exist_ok=True);cl=out/'classes';shutil.rmtree(cl,ignore_errors=True);cl.mkdir(exist_ok=True);runtime=out/'android-runtime';runtime.mkdir(exist_ok=True);results=[]
def run(name,cmd):
 with (out/(name+'.log')).open('w') as log:
  log.write('COMMAND='+json.dumps(list(map(str,cmd)))+'\n');log.flush();r=subprocess.run(list(map(str,cmd)),stdout=log,stderr=subprocess.STDOUT)
 results.append({'name':name,'exit_code':r.returncode});(out/'summary.json').write_text(json.dumps({'tests':results,'android_device':'NOT_RUN','future_evidence':'NOT_RUN'},indent=2))
 print(name, r.returncode,flush=True)
 if r.returncode:raise SystemExit(r.returncode)
run('semantic_generation',['python3',root/'scripts/r967_semantic_identity.py'])
prod=sorted((root/'app/src/main/java').rglob('*.java'))+sorted((root/'app/build/generated/source/r967').rglob('*.java'))
fixtures=[root/'verification/r36-stubs/com/ethk3/novaonepro'/n for n in ['BuildConfig.java','R.java']]
lst=out/'sources.list';lst.write_text('\n'.join(map(str,prod+fixtures)))
run('product_compile',['javac','-encoding','UTF-8','-source','17','-target','17','-cp',a.android_jar,'-d',cl,'@'+str(lst)])
with zipfile.ZipFile(a.android_jar) as z:
 for name in z.namelist():
  if name.endswith('.class'):
   dest=runtime/name;dest.parent.mkdir(parents=True,exist_ok=True)
   if not dest.exists():dest.write_bytes(z.read(name))
host=sorted((v/'host').rglob('*.java'));hl=out/'host.list';hl.write_text('\n'.join(map(str,host)))
run('host_compile',['javac','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[cl,a.android_jar])),'-d',cl,'@'+str(hl)])
cp=os.pathsep.join(map(str,[cl,runtime]));bridge=v/'host/sqlite_bridge.py'
for name,main in [('genesis','com.ethk3.novaonepro.GenesisResumeHarnessR967'),('training','com.ethk3.novaonepro.r952.TrainingTwoPhaseHarnessR967'),('registry_queue','com.ethk3.novaonepro.RegistryQueueHarnessR967'),('outbox','com.ethk3.novaonepro.r952.OutboxTransactionHarnessR967'),('maintenance','com.ethk3.novaonepro.MaintenanceContinuityHarnessR967'),('funnel','com.ethk3.novaonepro.FunnelReadOnlyHarnessR967')]:
 run(name,['java','-cp',cp,main,bridge])
print('PRODUCT_JAVA_COUNT='+str(len(prod)))
