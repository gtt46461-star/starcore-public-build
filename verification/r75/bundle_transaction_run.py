#!/usr/bin/env python3
"""Compile the two real changed owners and exercise actual SQLite transaction waits."""
import argparse
from pathlib import Path
import shutil
import subprocess

p=argparse.ArgumentParser()
p.add_argument('--schema',type=Path,required=True)
p.add_argument('--classpath',type=Path,required=True,help='host adapters + release classes + Android jar')
p.add_argument('--product-classes',type=Path,required=True)
p.add_argument('--android-jar',type=Path,required=True)
p.add_argument('--jdk',type=Path,required=True)
p.add_argument('--out',type=Path,required=True)
p.add_argument('--no-product-overlay',action='store_true',help='verify already built actual product classes without recompiling owners')
a=p.parse_args()
root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=True)
db=out/'timing.db'
if db.exists():raise SystemExit('Use a new output directory to preserve previous evidence.')
classes=out/'classes';classes.mkdir(exist_ok=True)
cp=':'.join(str(a.product_classes) if 'compileReleaseJavaWithJavac/classes' in part else part for part in a.classpath.read_text().strip().split(':'))
sources=[root/'app/src/main/java/com/ethk3/novaonepro'/n for n in ['DatabaseCoordinator.java','RuntimeResourceAuditR927.java']]
# Compile product against Android APIs, never narrowed host adapters.
if not a.no_product_overlay:
 subprocess.run([str(a.jdk/'bin/javac'),'-cp',str(a.product_classes)+':'+str(a.android_jar),'-d',str(classes)]+list(map(str,sources)),check=True)
subprocess.run([str(a.jdk/'bin/javac'),'-cp',str(classes)+':'+cp,'-d',str(classes),str(root/'verification/r75/bundle_TransactionAuditR75.java')],check=True)
shutil.copyfile(a.schema,db)
command=[str(a.jdk/'bin/java'),'-cp',str(classes)+':'+cp,'com.ethk3.novaonepro.bundle_TransactionAuditR75',str(root/'verification/r967/host/sqlite_bridge.py'),str(db),str(out/'result.json')]
r=subprocess.run(command,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True)
(out/'run.log').write_text(r.stdout)
print(r.stdout,end='')
raise SystemExit(r.returncode)
