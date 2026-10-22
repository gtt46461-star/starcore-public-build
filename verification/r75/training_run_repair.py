"""Copy a product-registered CAL repair fixture and continue actual TRAIN/FREEZE owners."""
from pathlib import Path
import argparse, subprocess, shutil, json, sqlite3, hashlib
p=argparse.ArgumentParser()
for name in ('fixture','schema','classes','jdk','out'):p.add_argument('--'+name,type=Path,required=True)
p.add_argument('--overlay',type=Path,action='append',default=[])
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True,exist_ok=True);cl=o/'classes';cl.mkdir(exist_ok=True)
original=(a.schema/'host-classpath').read_text().strip().split(':')
cp=':'.join(map(str,[cl,a.schema.resolve()/'classes',*[x.resolve() for x in a.overlay],a.classes.resolve(),original[-1]]))
sources=[r/'verification/r75/training_RepairContinuationR75.java',r/'verification/r72/realtime/android/os/SystemClock.java']
with (o/'compile.log').open('w') as f:subprocess.run([str(a.jdk/'bin/javac'),'-cp',cp,'-d',str(cl),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
db=o/'repair.db'
with sqlite3.connect('file:'+str((a.fixture/'repair.db').resolve())+'?mode=ro',uri=True) as src,sqlite3.connect(db) as dst:src.backup(dst)
shutil.copytree(a.fixture/'app-repair',o/'app-repair',dirs_exist_ok=True)
cmd=[str(a.jdk/'bin/java'),'-Djava.library.path='+str(r.parent/'host-native'),'-cp',cp,'com.ethk3.novaonepro.training_RepairContinuationR75',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(o/'app-repair'),str(r/'app/src/main/assets'),str(o/'result.json')]
with (o/'test.log').open('w') as f:rc=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT).returncode
(o/'run.json').write_text(json.dumps({'exit_code':rc,'command':cmd,'fixture':str(a.fixture.resolve()),'sources':{str(x.relative_to(r)):hashlib.sha256(x.read_bytes()).hexdigest() for x in sources}},indent=2)+'\n')
print((o/'test.log').read_text()[-3000:]);raise SystemExit(rc)
