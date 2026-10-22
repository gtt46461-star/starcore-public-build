#!/usr/bin/env python3
"""Run actual compiled reservation owner with full installed schema, no device claim."""
from pathlib import Path
import argparse, hashlib, json, shutil, subprocess

p=argparse.ArgumentParser()
for name in ('workspace','schema','classes','jdk','out'):
    p.add_argument('--'+name,type=Path,required=True)
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir()
tests=o/'classes';tests.mkdir();j=a.jdk.resolve()/'bin'
original=(a.schema/'host-classpath').read_text().strip().split(':')
cp=':'.join([str(tests),str(a.schema.resolve()/'classes'),str(a.classes.resolve()),original[-1]])
source=r/'verification/r74/CaptureAdmissionR74.java'
with (o/'compile.log').open('w') as f:
    subprocess.run([str(j/'javac'),'-cp',cp,'-d',str(tests),str(source)],stdout=f,stderr=subprocess.STDOUT,check=True)
db=o/'capture.db';shutil.copy2(a.schema/'schema-complete.db',db)
command=[str(j/'java'),'-Xmx128m','-cp',cp,'com.ethk3.novaonepro.CaptureAdmissionR74',
         str(r/'verification/canonical/schema/sqlite_bridge.py'),str(db),str(o/'result.json')]
with (o/'test.log').open('w') as f:
    result=subprocess.run(command,stdout=f,stderr=subprocess.STDOUT)
(o/'run.json').write_text(json.dumps({'exit_code':result.returncode,'command':command,
    'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest(),
    'log_sha256':hashlib.sha256((o/'test.log').read_bytes()).hexdigest()},indent=2)+'\n')
print((o/'test.log').read_text()[-5000:]);raise SystemExit(result.returncode)
