from pathlib import Path
import subprocess,shutil,json,hashlib,sqlite3
import argparse
p=argparse.ArgumentParser();p.add_argument('--workspace',type=Path,required=True);p.add_argument('--jdk',type=Path,required=True);p.add_argument('--android-runtime',type=Path,required=True);a=p.parse_args()
w=a.workspace.resolve();r=Path(__file__).resolve().parents[2];jdk=a.jdk.resolve();jar=a.android_runtime.resolve()
assert (w/'r72-build/build-receipt.json').is_file(),'BUILD_NOT_VERIFIED'
classes=r/'app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes';schema=w/'r72-final-schema';bridge=r/'verification/canonical/schema/sqlite_bridge.py';o=w/'r72-final-checks';o.mkdir(exist_ok=False)
cp=(schema/'host-classpath').read_text().strip();records=[]
def run(name,args):
 with (o/(name+'.log')).open('w') as f:res=subprocess.run(list(map(str,args)),cwd=r,stdout=f,stderr=subprocess.STDOUT)
 records.append({'name':name,'exit_code':res.returncode,'log_sha256':hashlib.sha256((o/(name+'.log')).read_bytes()).hexdigest()})
 (o/'results.json').write_text(json.dumps({'scope':'FINAL_RELEASE_PRODUCT_HOST_NO_DEVICE_NO_NATURAL_FUTURE','steps':records},indent=2)+'\n')
 print(name,res.returncode,flush=True)
 if res.returncode:print((o/(name+'.log')).read_text()[-4500:]);raise SystemExit(res.returncode)
run('runtime-regression',['python',r/'verification/r71/run.py','--classes',classes,'--android-runtime',jar,'--jdk',jdk,'--out',o/'runtime','--native',w/'host-native'])
run('domains',['python',r/'verification/r72/run_domains.py','--workspace',w,'--schema',schema,'--jdk',jdk,'--out',o/'domains'])
run('mainline',['python',r/'verification/r72/run_mainline.py','--workspace',w,'--schema',schema,'--jdk',jdk,'--fixture',o/'domains/CertificateScopeR70.db','--out',o/'mainline'])
run('compile-additional',[jdk/'bin/javac','-cp',cp,'-d',schema/'classes',*list((r/'verification/r72').glob('*.java')),r/'verification/r973/clock-stubs/android/os/Handler.java',r/'verification/r72/adapters/android/system/OsConstants.java'])
for name in ['SettlementProgressR72','CapacityClockR72']:
 db=o/(name+'.db');shutil.copy2(schema/'schema-complete.db',db)
 run(name,[jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.'+name,bridge,db,o/(name+'.json')])
run('DevelopmentIoR72',[jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.r952.DevelopmentIoR72',o/'training.scdev'])
assert (o/'training.scdev').read_bytes()==(w/'r72-iteration/input-before.scdev').read_bytes(),'BASELINE_INPUT_FORMAT_CHANGED'
real=o/'realtime';real.mkdir();run('compile-real-clock',[jdk/'bin/javac','-d',real,r/'verification/r72/realtime/android/os/SystemClock.java'])
db=o/'family.db';shutil.copy2(schema/'schema-complete.db',db)
c=sqlite3.connect(db)
x=json.load(open(w/'r72-input/DIAGNOSTIC_SCOPE.json'))
for t in x['recentEvidence']:
 if t['table'] not in ['draws','v57_draw_canonical_r811']:continue
 cols=t['columns'];marks=','.join('?' for _ in cols);keys=','.join('"'+v+'"' for v in cols)
 for row in t['rows']:c.execute('INSERT INTO "'+t['table']+'"('+keys+') VALUES('+marks+')',[row.get(k) for k in cols])
c.commit();c.close()
run('FamilyProgressR72',[jdk/'bin/java','-Xmx512m','-Djava.library.path='+str(w/'host-native'),'-cp',str(real)+':'+cp,'com.ethk3.novaonepro.FamilyProgressR72',bridge,db,o/'family-files',r/'app/src/main/assets',o/'family.json'])
h=hashlib.sha256()
for p in sorted(classes.rglob('*.class')):h.update(p.relative_to(classes).as_posix().encode()+b'\0'+hashlib.sha256(p.read_bytes()).digest())
(o/'product-classes.json').write_text(json.dumps({'class_tree_sha256':h.hexdigest(),'source_commit':'c636dd6afc9eac3ce6134e1eecce90ba835c8505','class_count':len(list(classes.rglob('*.class')))},indent=2)+'\n')
print('ALL_FINAL_HOST_CHECKS_PASS',flush=True)
