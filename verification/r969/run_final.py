#!/usr/bin/env python3
"""Re-run this checkout's complete scoped host regressions, without an Android device."""
from pathlib import Path
import argparse,json,os,subprocess,sys
p=argparse.ArgumentParser();p.add_argument('--android-jar',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
R=Path(__file__).resolve().parents[2];O=a.out.resolve();O.mkdir(parents=True,exist_ok=True);env=os.environ.copy();env.setdefault('JAVA_TOOL_OPTIONS','-XX:ActiveProcessorCount=2');rows=[]
def run(name,cmd,extra=None):
    e=env.copy();e.update(extra or {})
    with (O/(name+'_console.log')).open('w') as f:
        f.write('COMMAND='+json.dumps(list(map(str,cmd)))+'\n');f.flush()
        q=subprocess.run(list(map(str,cmd)),cwd=R,env=e,stdout=f,stderr=subprocess.STDOUT)
    rows.append({'stage':name,'exit_code':q.returncode});(O/'stages.json').write_text(json.dumps(rows,indent=2));print(name,q.returncode,flush=True)
    if q.returncode:sys.exit(q.returncode)
run('host',['python3',R/'verification/r967/run.py','--android-jar',a.android_jar,'--out',O/'host'])
run('contracts',['python3',R/'verification/r968/run_contracts.py','--android-jar',a.android_jar,'--host',O/'host','--out',O/'contracts'])
run('integration',['python3',R/'verification/r969/run_evidence.py'],{'ANDROID_PLATFORM_JAR':str(a.android_jar.resolve()),'STARCORE_HOST_CLASSES':str(O/'host/classes'),'STARCORE_SQLITE_OUT':str(O/'integration')})
print('R969_FINAL_SCOPED_HOST_TESTS=PASS; DEVICE=NOT_RUN; FULL_91_PLAN=NOT_COMPLETE')
