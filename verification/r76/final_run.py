"""Locked R76 release-bytecode host regressions, not device acceptance."""
from pathlib import Path
from concurrent.futures import ThreadPoolExecutor, as_completed
import argparse, hashlib, json, subprocess, sys, time
p=argparse.ArgumentParser()
for key in ('jdk','schema','classes','android-runtime','native','report','prior-report','repair','queue-database','build-receipt','out'):
    p.add_argument('--'+key,type=Path,required=True)
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir()
receipt=json.loads(a.build_receipt.read_text())
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def hashes():return {str(x.relative_to(a.classes)):digest(x) for x in sorted(a.classes.rglob('*.class'))}
before=hashes();assert before
host_collisions=[str(x.relative_to(a.schema/'classes')) for x in (a.schema/'classes').rglob('*.class') if (a.classes/x.relative_to(a.schema/'classes')).exists()]
assert not host_collisions,host_collisions
common=['--jdk',a.jdk,'--classes',a.classes]
android=[*common,'--android-runtime',a.android_runtime,'--final-bytecode']
schema=[*common,'--schema',a.schema]
product=['--jdk',a.jdk,'--schema',a.schema,'--product-classes',a.classes]
jobs=[
 ('ui','r76/ui_run.py',android),
 ('observation','r76/runtime_observation_run.py',android),
 ('diagnosis','r76/runtime_diagnosis_run.py',android),
 ('pipeline','r76/pipeline_status_run.py',[*schema,'--report',a.report,'--repair',a.repair,'--final-classes']),
 ('continuation','r76/training_continuation_run.py',common),
 ('queue','r76/training_run_ready.py',[*schema,'--database',a.queue_database]),
 ('cal_head','r76/run_fixed_window_head.py',product),
 ('cal_maintenance','r76/run_mainline_maintenance.py',product),
 ('retention','r76/run_retention_page.py',[*schema,'--native',a.native]),
 ('fetch_callback','r75/fetch_callback_actual_run.py',schema),
 ('training_report','r76/training_run_report.py',[*schema,'--report',a.report]),
 ('training_shared','r75/training_run_all.py',[*schema,'--report',a.prior_report,'--native',a.native]),
]
results=[]
def run(job):
    name,script,options=job;command=list(map(str,[sys.executable,r/'verification'/script,*options,'--out',o/name]))
    start=time.monotonic()
    with (o/(name+'.log')).open('w') as f:
        result=subprocess.run(command,cwd=r,stdout=f,stderr=subprocess.STDOUT)
    return {'name':name,'exit_code':result.returncode,'seconds':round(time.monotonic()-start,3),'command':command,'log_sha256':digest(o/(name+'.log'))}
with ThreadPoolExecutor(max_workers=3) as pool:
    for future in as_completed([pool.submit(run,job) for job in jobs]):
        result=future.result();results.append(result)
        (o/'progress.json').write_text(json.dumps(results,indent=2)+'\n')
        print(json.dumps({k:v for k,v in result.items() if k!='command'}),flush=True)
after=hashes();assert before==after,'RELEASE_CLASSES_CHANGED_DURING_TESTS'
collisions={}
for name,_,_ in jobs:
    for folder in (o/name/'classes',o/name/'tests'):
        found=[str(x.relative_to(folder)) for x in folder.rglob('*.class') if (a.classes/x.relative_to(folder)).exists()]
        # This suite deliberately compiles invalid generated identities in isolated
        # mutant subdirectories; its normal release path still has no overlay.
        if found:collisions[str(folder.relative_to(o))]=found
assert not collisions,collisions
summary={'status':'PASS' if all(x['exit_code']==0 for x in results) else 'FAIL','scope':'FINAL_RELEASE_JAVA_BYTECODE_HOST_ADAPTERS_NOT_ANDROID_DEVICE_OR_NATURAL_QUALITY','source_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=r,text=True).strip(),'build_receipt_sha256':digest(a.build_receipt),'build_receipt':receipt,'product_classes':str(a.classes),'class_count':len(before),'bytecode_unchanged':True,'before':before,'after':after,'host_collisions':host_collisions,'results':sorted(results,key=lambda x:x['name']),'native_host_sha256':digest(a.native/'libstarcore_report_cp4.so')}
(o/'summary.json').write_text(json.dumps(summary,indent=2)+'\n')
print(summary['status'],flush=True)
sys.exit(0 if summary['status']=='PASS' else 1)
