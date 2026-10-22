from pathlib import Path
import argparse,subprocess,shutil,json,hashlib
p=argparse.ArgumentParser()
for name in ('schema','classes','jdk','out'):p.add_argument('--'+name,type=Path,required=True)
p.add_argument('--compile-product',action='store_true');p.add_argument('--overlay',type=Path,action='append',default=[]);a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True,exist_ok=True);cl=o/'classes';cl.mkdir(exist_ok=True)
original=(a.schema/'host-classpath').read_text().strip().split(':');cp=':'.join([str(cl),str(a.schema.resolve()/'classes'),*[str(x.resolve()) for x in a.overlay],str(a.classes.resolve()),original[-1]])
# Real monotonic time for parent-scope exhaustion; Android startup alone is stubbed.
sources=[r/'verification/r75/training_DirectorBudgetR75.java',r/'verification/r72/realtime/android/os/SystemClock.java']
if a.compile_product:sources += [r/'app/src/main/java/com/ethk3/novaonepro/r952/ResearchDirectorR952.java',r/'app/src/main/java/com/ethk3/novaonepro/r952/ResearchTrainingHookRegistryR952.java']
with (o/'compile.log').open('w') as f:subprocess.run([str(a.jdk/'bin/javac'),'-cp',cp,'-d',str(cl),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
db=o/'director.db';shutil.copy2(a.schema/'schema-complete.db',db)
cmd=[str(a.jdk/'bin/java'),'-cp',cp,'com.ethk3.novaonepro.training_DirectorBudgetR75',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(o/'result.json')]
with (o/'test.log').open('w') as f:rc=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT).returncode
(o/'run.json').write_text(json.dumps({'exit_code':rc,'command':cmd,'sources':{str(x.relative_to(r)):hashlib.sha256(x.read_bytes()).hexdigest() for x in sources}},indent=2)+'\n');print((o/'test.log').read_text()[-3000:]);raise SystemExit(rc)
