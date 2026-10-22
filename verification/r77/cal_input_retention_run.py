#!/usr/bin/env python3
"""Retain original draw clocks until actual CAL owner releases them."""
import argparse,json,subprocess,hashlib,shutil
from pathlib import Path
p=argparse.ArgumentParser()
for k in ['jdk','classes','adapters','android-runtime','schema','out']:p.add_argument('--'+k,type=Path,required=True)
p.add_argument('--overlay',action='store_true');a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True,exist_ok=False);c=o/'classes';c.mkdir()
cp=':'.join(map(str,[c,a.adapters,a.classes,a.android_runtime]));sources=[r/'verification/r77/CalInputRetentionR77.java']
sources.append(r/'verification/canonical/adapters/android/os/SystemClock.java')
if a.overlay:sources += [r/('app/src/main/java/com/ethk3/novaonepro/'+x+'.java') for x in ['CandidateCalibrationR932','DrawDependencyInputCP12']]
with (o/'compile.log').open('w') as f:subprocess.run([str(a.jdk/'bin/javac'),'-cp',cp,'-d',str(c),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
db=o/'retention.db';shutil.copy2(a.schema,db)
with (o/'run.log').open('w') as f:z=subprocess.run([str(a.jdk/'bin/java'),'-cp',cp,'com.ethk3.novaonepro.CalInputRetentionR77',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(o/'result.json')],stdout=f,stderr=subprocess.STDOUT,timeout=120)
assert z.returncode==0,(o/'run.log').read_text();result=json.loads((o/'result.json').read_text())
result.update({'implementation':'development-source-overlay' if a.overlay else 'supplied-product-bytecode','sources':{str(s.relative_to(r)):hashlib.sha256(s.read_bytes()).hexdigest() for s in sources},'product_classes':str(a.classes)})
(o/'summary.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
