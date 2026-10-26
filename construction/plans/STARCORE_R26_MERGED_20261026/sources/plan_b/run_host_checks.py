"""Run reproducible HOST checks. Never builds an APK, authorizes REAL or edits a repository."""
from pathlib import Path
import sys,subprocess,tempfile,shutil,json,re,time
ROOT=Path(__file__).resolve().parent
OUT=ROOT/'evidence/current_run';OUT.mkdir(parents=True,exist_ok=True)
results=[]
def run(label,args,cwd=None):
 start=time.monotonic();p=subprocess.run(args,cwd=cwd,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
 (OUT/(label+'.log')).write_text(p.stdout,encoding='utf-8')
 item=dict(name=label,status='PASS' if p.returncode==0 else 'FAIL',returncode=p.returncode,seconds=round(time.monotonic()-start,3));results.append(item)
 print(json.dumps(item));
 if p.returncode:print(p.stdout[-6000:])
 return p.returncode==0
run('r25_python',[sys.executable,'-m','unittest','discover','-s',str(ROOT/'tests'),'-p','test*.py','-v'])
run('r24_python',[sys.executable,'-m','unittest','discover','-s',str(ROOT/'baseline_R24/tests'),'-v'])
run('r23_python',[sys.executable,'-m','unittest','discover','-s',str(ROOT/'baseline_R23/tests'),'-v'])
if not shutil.which('javac') or not shutil.which('java'):
 results.append(dict(name='java_checks',status='NOT_RUN',reason='JDK_REQUIRED'))
else:
 run('r24_java',[sys.executable,str(ROOT/'baseline_R24/code/run_java_regression.py')])
 with tempfile.TemporaryDirectory(prefix='starcore_r25_') as t:
  tmp=Path(t);r23=tmp/'r23';r23.mkdir()
  for p in (ROOT/'baseline_R23/java').glob('*.java'):shutil.copy2(p,r23/p.name)
  if run('r23_javac',['javac','-encoding','UTF-8','-d',str(r23)]+[str(p) for p in r23.glob('*.java')]):
   run('r23_java',['java','-cp',str(r23),'com.ethk3.novaonepro.strategyv2.R23HostTest'])
  src=tmp/'original';src.mkdir();classes=tmp/'classes';classes.mkdir()
  origin=ROOT/'baseline_R24/evidence/source_verified/app/src/main/java/com/ethk3/novaonepro/strategyv2'
  for p in origin.glob('*.java'):
   text=p.read_text()
   if p.name=='ProgramModelSU2.java' or re.search(r'^import (android|com\.ethk3\.novaonepro\.[A-Z])',text,re.M):continue
   shutil.copy2(p,src/p.name)
  print('unchanged_production_java_files_selected=',len(list(src.glob('*.java'))))
  shutil.copy2(ROOT/'tests/R25LegacyDefectReproducer.java',src/'R25LegacyDefectReproducer.java')
  if run('original_java_compile',['javac','-encoding','UTF-8','-d',str(classes)]+[str(p) for p in src.glob('*.java')]):
   run('legacy_defect_reproduction',['java','-cp',str(classes),'com.ethk3.novaonepro.strategyv2.R25LegacyDefectReproducer'])
report=dict(scope='HOST_ONLY_NOT_ANDROID_NOT_PROSPECTIVE',runs=results,all_required_pass=all(r['status']=='PASS' for r in results))
(OUT/'summary.json').write_text(json.dumps(report,ensure_ascii=False,indent=2))
print(json.dumps(report,ensure_ascii=False,indent=2))
sys.exit(0 if report['all_required_pass'] else 1)
