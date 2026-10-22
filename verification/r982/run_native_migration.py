"""Current native/report regression and original R980 incremental schema migration."""
from pathlib import Path
import subprocess,json,sqlite3,shutil,hashlib
src=Path(__file__).resolve().parents[2];root=src.parent;out=root/'evidence/certificate-native-migration-complete';out.mkdir(exist_ok=True)
tool=Path('/tmp/starcore-cdaf1f33/toolchain');java=tool/'jdk-17.0.16+8/bin/java';jar=tool/'android-sdk/platforms/android-36/android.jar';cl=root/'evidence/certificate-final-schema/classes'
files=list((src/'verification/canonical/tests/audit_cp12/native/java').rglob('*.java'))+list((src/'verification/canonical/tests/audit_cp12/native/cas_java').rglob('*.java'))+[src/'verification/canonical/schema/MigrationUpgradeActualCP12.java']
cmd=[str(java.with_name('javac')),'-cp',str(cl)+':'+str(jar),'-d',str(cl)]+list(map(str,files))
with(out/'compile.log').open('w') as f: p=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
if p.returncode:raise SystemExit(p.returncode)
receipts=[]
def run(name,args,d):
 cmd=[str(java),'-Xmx128m','-Djava.library.path='+str(root/'evidence/current-native'),'-cp',str(cl)+':'+str(jar),'com.ethk3.novaonepro.'+name]+list(map(str,args))
 with(d/'execution.log').open('w') as f:p=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT,timeout=180)
 receipts.append({'test':name,'exit_code':p.returncode,'status':'PASS' if p.returncode==0 else 'FAIL','command':cmd,'scope':'Current product JVM/JNI with host SQLite; not Android device/SAF provider'})
 (out/'execution_receipts.json').write_text(json.dumps(receipts,indent=2));print(name,receipts[-1]['status'],flush=True)
for name in ['NativeReportContractCP12','ArchivePipelineActualCP12','CasDirectoryActualCP12']:
 d=out/name;d.mkdir()
 if name=='NativeReportContractCP12':
  (d/'files').mkdir();c=sqlite3.connect(d/'files/live.db');c.execute('CREATE TABLE cp12_jni_fixture(text_payload TEXT,blob_payload BLOB)');c.execute('INSERT INTO cp12_jni_fixture(rowid,text_payload,blob_payload) VALUES(?,?,?)',(-9223372036854775808,'A'*2097152,bytes(2097152)));c.commit();c.close()
 run(name,[d/'files'],d)
d=out/'R980Upgrade';d.mkdir();db=d/'fixture.db';original=root/'evidence/r980-schema/schema.db'
shutil.copy2(original,db);c=sqlite3.connect(db);before=dict(c.execute("SELECT name,0 FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'"));c.close()
for n in range(2):
 step=d/str(n);step.mkdir();run('MigrationUpgradeActualCP12',[src/'verification/canonical/schema/sqlite_bridge.py',db],step)
c=sqlite3.connect(db);after={r[0] for r in c.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")};integrity=c.execute('PRAGMA integrity_check').fetchall();fk=c.execute('PRAGMA foreign_key_check').fetchall()
receipt={'input_sha256':hashlib.sha256(original.read_bytes()).hexdigest(),'before_tables':len(before),'after_tables':len(after),'lost_tables':sorted(set(before)-after),'integrity':integrity,'foreign_keys':fk,'scope':'Actual incremental schema owner on empty generated original R980 DB twice. Customer populated DB and CP11 original fixture not available.'}
(d/'schema_receipt.json').write_text(json.dumps(receipt,indent=2));assert not receipt['lost_tables'] and integrity==[('ok',)] and not fk
cmd=[str(tool/'android-sdk/platform-tools/adb'),'devices','-l'];p=subprocess.run(cmd,capture_output=True,text=True);(out/'device_inventory.log').write_text(p.stdout+p.stderr);(out/'DEVICE_STATUS.json').write_text(json.dumps({'command':cmd,'exit_code':p.returncode,'status':'DEVICE_NOT_VERIFIED','reason':'No device listed by adb devices -l'},indent=2))
