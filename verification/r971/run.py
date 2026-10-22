#!/usr/bin/env python3
"""R971 targeted regression runner. Requires a full scoped host build and report fixture.
Does not claim real Android UI, battery, device latency or future predictive performance.
"""
from pathlib import Path
import argparse,subprocess,json
p=argparse.ArgumentParser()
for key in ('android-jar','host-build','report-subset','full-report','out'):
    p.add_argument('--'+key,type=Path,required=True)
a=p.parse_args();root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=True)
host=a.host_build.resolve();cp=str(host/'classes')+':'+str(a.android_jar.resolve())
rows=[]
def run(name,cmd):
    with (out/(name+'.log')).open('w') as log:
        q=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,cwd=root)
    rows.append({'stage':name,'exit_code':q.returncode})
    (out/'stages.json').write_text(json.dumps(rows,indent=2))
    print(name,q.returncode,flush=True)
    if q.returncode:raise SystemExit(q.returncode)
run('compile',['javac','-cp',cp,'-d',str(out),*map(str,(root/'verification/r971').glob('*.java'))])
java=['java','-XX:ActiveProcessorCount=2','-Xmx512m','-cp',str(out)+':'+str(host/'classes')+':'+str(host/'android-runtime')]
bridge=str(root/'verification/r967/host/sqlite_bridge.py')
run('RegressionHarnessR971',java+['com.ethk3.novaonepro.RegressionHarnessR971',bridge,str(a.report_subset.resolve()),str(a.full_report.resolve()),str(out)])
run('CaptureFenceHarnessR971',java+['com.ethk3.novaonepro.CaptureFenceHarnessR971',bridge,str(a.report_subset.resolve()),str(out/'capture_fence.db')])
print('R971_TARGETED_TESTS=PASS; PHYSICAL_DEVICE=NOT_RUN; FULL_91_PLAN=NOT_COMPLETE')
