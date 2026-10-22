#!/usr/bin/env python3
from pathlib import Path
import argparse, json, os, subprocess
p=argparse.ArgumentParser();p.add_argument('--android-jar',type=Path,required=True);p.add_argument('--host',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
R=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=True);cl=out/'classes';cl.mkdir(exist_ok=True);results=[]
def run(name,cmd):
    with (out/(name+'.log')).open('w') as log:r=subprocess.run(list(map(str,cmd)),cwd=R,stdout=log,stderr=subprocess.STDOUT)
    results.append({'test':name,'exit_code':r.returncode});(out/'summary.json').write_text(json.dumps(results,indent=2));print(name,r.returncode,flush=True)
    if r.returncode:raise SystemExit(r.returncode)
run('identity_mutations',['python3',R/'verification/r968/test_semantic_identity.py'])
files=list((R/'verification/r968/host').glob('*.java'))+list((R/'verification/r932/android-json').rglob('*.java'))
run('contracts_compile',['javac','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[a.host/'classes',a.android_jar])),'-d',cl,*files])
cp=os.pathsep.join(map(str,[cl,a.host/'classes',a.host/'android-runtime']))
for main in ['IdentityContractHarnessR967','PlanContractHarnessR968']:
    run(main,['java','-cp',cp,'com.ethk3.novaonepro.'+main,R/'verification/r967/host/sqlite_bridge.py'])
print('CONTRACTS_FINISHED; HOST_ONLY_NOT_PHONE')
