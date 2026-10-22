#!/usr/bin/env python3
"""Replay existing R76 native archive/CAS tests using current transport and R77 governor.

No production implementations are replaced in --final-bytecode mode. The R64
SQLite adapter preserves typed cells and opens snapshot readers with mode=ro.
"""
import argparse,hashlib,json,pathlib,shutil,subprocess
p=argparse.ArgumentParser()
for k in ('jdk','classes','schema','android-runtime','native','out'):p.add_argument('--'+k,type=pathlib.Path,required=True)
p.add_argument('--final-bytecode',action='store_true');a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(exist_ok=False);product=out/'product';host=out/'schema'
shutil.copytree(a.classes,product);shutil.copytree(a.schema/'classes',host/'classes');shutil.copy2(a.schema/'schema-complete.db',host/'schema-complete.db');(host/'host-classpath').write_text(str(a.android_runtime.resolve()))
H=lambda f:hashlib.sha256(f.read_bytes()).hexdigest()
adapter=root/'verification/r64/adapters/android/database/sqlite/SQLiteDatabase.java';bridge=root/'verification/r64/sqlite_bridge.py';governor=root/'app/src/main/java/com/ethk3/novaonepro/DataRetentionGovernorR900.java'
def javac(src,dst):
 cp=':'.join(map(str,[host/'classes',product,a.android_runtime]));subprocess.run([str(a.jdk/'bin/javac'),'-encoding','UTF-8','-sourcepath',str(out/'empty'),'-cp',cp,'-d',str(dst),str(src)],check=True)
javac(adapter,host/'classes')
atomic=root/'verification/canonical/adapters/android/util/AtomicFile.java';javac(atomic,host/'classes')
constants=root/'verification/r72/adapters/android/system/OsConstants.java';javac(constants,host/'classes')
clock=root/'verification/canonical/adapters/android/os/SystemClock.java';javac(clock,host/'classes')
clock_bytecode=subprocess.check_output([str(a.jdk/'bin/javap'),'-c','-p','-classpath',str(host/'classes'),'android.os.SystemClock'],text=True)
assert clock_bytecode.count('java/lang/System.nanoTime')==2 and 'setForTest' not in clock_bytecode,'REAL_MONOTONIC_CLOCK_REQUIRED'
(out/'clock-bytecode.txt').write_text(clock_bytecode)
if not a.final_bytecode:javac(governor,product)
original=root/'verification/r76/run_retention_page.py';runner=out/'run_retention_page.py';source=original.read_text()
source=source.replace("r=Path(__file__).resolve().parents[2]","r=Path("+repr(str(root))+")")
source=source.replace("r/'verification/canonical/schema/sqlite_bridge.py'","r/'verification/r64/sqlite_bridge.py'");runner.write_text(source)
command=['python','-B',str(runner),'--schema',str(host),'--classes',str(product),'--jdk',str(a.jdk),'--native',str(a.native),'--out',str(out/'replay'),'--filler-mib','128']
with (out/'run.log').open('w') as log:r=subprocess.run(command,stdout=log,stderr=subprocess.STDOUT)
if r.returncode:print((out/'run.log').read_text()[-6000:]);raise SystemExit(r.returncode)
result=json.loads((out/'replay/run.json').read_text());result['r77_mode']='FINAL_PRODUCT_BYTECODE' if a.final_bytecode else 'CHANGED_GOVERNOR_COMPILED_IN_PRIVATE_PRODUCT_COPY';result['transport']={str(x.relative_to(root)):H(x) for x in (adapter,bridge,atomic,constants,clock,original)};result['governor_source_hash']=H(governor);result['clock']={'class_sha256':H(host/'classes/android/os/SystemClock.class'),'javap_sha256':H(out/'clock-bytecode.txt'),'explicit_real_nanoTime_adapter':True}
(out/'result.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n');print(json.dumps({k:v for k,v in result.items() if k not in ('product_bytecode','checks_detail')},ensure_ascii=False,indent=2))
