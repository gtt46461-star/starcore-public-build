#!/usr/bin/env python3
"""Foreground reproducible R975 tests; Android API adapters + real product Java and SQLite.
No device, UI, network, live accuracy or all-software coverage claim.
"""
from pathlib import Path
import argparse,subprocess,os,json,hashlib,sys
p=argparse.ArgumentParser();p.add_argument('--step',choices=['compile','base','exact','numeric','clock','identity','blockers','report','legacy-cal','legacy-window1','legacy-real','v3-train-cal','v3-window1','v3-real'],required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--android-jar',type=Path,required=True);a=p.parse_args()
r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True,exist_ok=True);env=dict(os.environ,JAVA_TOOL_OPTIONS='-Xmx768m -XX:ActiveProcessorCount=2')
def run(label,cmd):
 log=o/(label+'.log')
 with log.open('w') as f:
  f.write('COMMAND='+json.dumps(list(map(str,cmd)))+'\n');f.flush();code=subprocess.run(list(map(str,cmd)),cwd=r,env=env,stdout=f,stderr=subprocess.STDOUT).returncode
 print(label,'exit='+str(code),'log='+str(log),flush=True)
 if code:raise SystemExit(code)
if a.step in ('compile','base','exact','numeric','clock','identity'):
 run(a.step+'_runner',[sys.executable,r/'verification/r974/run.py','--step',a.step,'--out',o,'--android-jar',a.android_jar])
 if a.step=='compile':run('source_index',[sys.executable,r/'scripts/r975_source_call_index.py','--classes',o/'classes','--check'])
else:
 h=hashlib.sha256()
 for folder in ['app/src','app/build/generated/source/r967','release']:
  for f in sorted((r/folder).rglob('*')):
   if f.is_file():h.update(str(f.relative_to(r)).encode()+b'\0'+f.read_bytes())
 if (o/'compiled.sha256').read_text().strip()!=h.hexdigest():raise SystemExit('SOURCE_CHANGED_SINCE_COMPILE')
 cp=os.pathsep.join([str(o/'classes'),str(o/'android-runtime')]);bridge=r/'verification/r967/host/sqlite_bridge.py'
 run('r975_harness_compile',['javac','-encoding','UTF-8','-cp',cp,'-d',o/'classes',*sorted((r/'verification/r975').glob('*.java'))])
 prefix=['java','-cp',cp]
 if a.step=='blockers':cmd=prefix+['com.ethk3.novaonepro.RootBlockerHarnessR975',bridge,'fixed']
 elif a.step=='report':cmd=prefix+['com.ethk3.novaonepro.ReportReplayHarnessR975',bridge,r/'verification/r975/fixtures',o]
 else:
  versioned=a.step.startswith('v3-');parts={'legacy-cal':(201,441),'legacy-window1':(442,561),'legacy-real':(562,801),'v3-train-cal':(321,561),'v3-window1':(562,681),'v3-real':(682,921)};first,last=parts[a.step]
  cmd=prefix+['com.ethk3.novaonepro.'+('VersionedEndToEndHarnessR975' if versioned else 'EndToEndHarnessR975'),bridge,o/('v3_e2e.sqlite' if versioned else 'legacy_e2e.sqlite'),str(first),str(last)]
 run(a.step,cmd)
