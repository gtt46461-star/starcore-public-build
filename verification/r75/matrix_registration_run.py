#!/usr/bin/env python3
"""Run R75 matrix registration with actual SQLite and product classes (no product overlay by default)."""
import argparse,json,shutil,subprocess,hashlib
from pathlib import Path
p=argparse.ArgumentParser()
p.add_argument('--schema',type=Path,required=True)
p.add_argument('--host-classpath',type=Path,required=True)
p.add_argument('--product-classes',type=Path,required=True)
p.add_argument('--android-jar',type=Path,required=True)
p.add_argument('--jdk',type=Path,required=True)
p.add_argument('--ast',type=Path,required=True)
p.add_argument('--out',type=Path,required=True)
p.add_argument('--compile-overlay',action='store_true',help='development only; final verification omits this option')
a=p.parse_args();root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=True)
classes=out/'test-classes';classes.mkdir(exist_ok=True)
source_names=['MatrixRegistrationR75.java','MatrixSnapshotBusR952.java','MatrixSourceRouterCP12.java']
product_sources=[root/'app/src/main/java/com/ethk3/novaonepro/r952'/n for n in source_names]
# Host adapters are first, actual requested product classes replace any stale product path in their catalog.
host_parts=a.host_classpath.read_text().strip().split(':')
host_parts=[s for s in host_parts if 'compileReleaseJavaWithJavac/classes' not in s and s!=str(a.android_jar)]
base=':'.join(host_parts+[str(a.product_classes),str(a.android_jar)])
if a.compile_overlay:
 subprocess.run([str(a.jdk/'bin/javac'),'-cp',str(a.product_classes)+':'+str(a.android_jar),'-d',str(classes)]+list(map(str,product_sources)),check=True)
cp=str(classes)+':'+base
subprocess.run([str(a.jdk/'bin/javac'),'-cp',cp,'-d',str(classes),str(root/'verification/canonical/extended/ModernModelContractTest.java'),str(root/'verification/r75/MatrixRegistrationSourcesR75.java'),str(root/'verification/r75/MatrixRegistrationContractR75.java')],check=True)
results=[]
for mode,main in [('fair','MatrixRegistrationContractR75'),('tail','MatrixRegistrationContractR75'),('source','MatrixRegistrationSourcesR75')]:
 db=out/(mode+'.db')
 if db.exists():raise SystemExit('Use a fresh --out directory.')
 shutil.copyfile(a.schema,db)
 command=[str(a.jdk/'bin/java'),'-Dstarcore.test.ast='+str(a.ast),'-cp',cp,'com.ethk3.novaonepro.r952.'+main,str(root/'verification/r967/host/sqlite_bridge.py'),str(db),str(root/'app/src/main/assets/cp4_atom_registry.jsonl') if mode=='source' else mode]
 r=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True);(out/(mode+'.log')).write_text(r.stdout);print(r.stdout,end='')
 results.append({'mode':mode,'exit_code':r.returncode,'log':mode+'.log'})
 if r.returncode:break
manifest={'product_overlay':a.compile_overlay,'product_classes':str(a.product_classes),'tests':results,'source_sha256':{str(f.relative_to(root)):hashlib.sha256(f.read_bytes()).hexdigest() for f in product_sources}}
(out/'result.json').write_text(json.dumps(manifest,indent=2)+'\n')
raise SystemExit(max(r['exit_code'] for r in results))
