from pathlib import Path
import os,subprocess,json,time,hashlib
r=Path(__file__).resolve().parents[2];o=r.parent/'evidence/r992-verification';o.mkdir(exist_ok=True);cl=o/'drivers';cl.mkdir(exist_ok=True)
jdk=Path(os.environ['JAVA_HOME'])/'bin';jar=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';product=r.parent/'evidence/r992-schema-fixed/classes';cp=str(cl)+':'+str(product)+':'+str(jar)
files=sorted((r/'verification/r992').glob('*.java'))+sorted((r/'verification/canonical/tests/audit_cp12/report/tests').glob('*.java'))+[r/'verification/canonical/adapters/android/util/AtomicFile.java',r/'verification/r986/ReportSafFinalizationR986.java',r/'verification/r986/ReportSafResumeIdentityR986.java']
cmd=[str(jdk/'javac'),'-cp',str(jar)+':'+str(product),'-d',str(cl),*map(str,files)];p=subprocess.run(cmd,capture_output=True,text=True);(o/'compile.log').write_text(p.stdout+p.stderr);assert p.returncode==0,p.stderr
results=[]
for f in files:
 if f.name=='AtomicFile.java':continue
 name=f.stem;d=o/name;d.mkdir(exist_ok=True)
 if name in ['ProductionSchemaBackupR992','SingleZipExportR992']:args=[str(r.parent/'evidence/r992-schema-fixed/schema.db'),str(d/'files')]
 elif name in ['ParserMissingTimeR992','ReportSemanticContractCP12']:args=[]
 else:args=[str(d/'files')]
 cmd=[str(jdk/'java'),'-Xmx128m','-Djava.library.path='+str(r.parent/'evidence/r992-native'),'-cp',cp,'com.ethk3.novaonepro.'+name,*args];start=time.monotonic()
 with(d/'execution.log').open('w')as log:p=subprocess.run(cmd,stdout=log,stderr=subprocess.STDOUT,timeout=180)
 result={'test':name,'exitCode':p.returncode,'seconds':round(time.monotonic()-start,3),'command':cmd,'testSha256':hashlib.sha256(f.read_bytes()).hexdigest(),'log':str(d/'execution.log'),'scope':'Current complete product classes; actual JNI and filesystem; synthetic fixture, not Android device/user database'};results.append(result);(o/'receipt.json').write_text(json.dumps(results,indent=2));print(name,p.returncode,flush=True)
print('COUNTS',len(results),sum(x['exitCode']==0 for x in results),flush=True)
