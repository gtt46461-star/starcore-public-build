#!/usr/bin/env python3
"""Replay current JNI archive/retention consumers with explicit host adapters and fresh data."""
import argparse,hashlib,json,pathlib,shutil,subprocess,time
p=argparse.ArgumentParser()
for name in ['out','schema','host-classpath','native-dir','private-work']:
    p.add_argument('--'+name,type=pathlib.Path,required=True)
p.add_argument('--javac',default='javac');a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(exist_ok=False)
work=a.private_work.resolve();work.mkdir(exist_ok=False,mode=0o700)
classes=out/'classes';classes.mkdir();cp=str(classes)+':'+a.host_classpath.read_text().strip()
library=a.native_dir.resolve()/'libstarcore_report_cp4.so'
assert library.is_file(),'FRESH_CURRENT_NATIVE_LIBRARY_REQUIRED'
fixture=work/'synthetic.db';shutil.copyfile(a.schema,fixture)
sources=[root/'verification/r983/android/database/sqlite/SQLiteDatabase.java',root/'verification/r996/android/system/Os.java',root/'verification/numbered/DiagnosisNativeArchiveCurrent.java']
records=[]
def run(name,command):
    start=time.monotonic();r=subprocess.run(command,cwd=root,capture_output=True,text=True,timeout=240)
    log=out/(name+'.log');log.write_text(r.stdout+r.stderr)
    records.append({'name':name,'command':command,'exit_code':r.returncode,'seconds':time.monotonic()-start,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest()})
    print(name,r.returncode,r.stdout.strip(),r.stderr[-1500:],flush=True);return r.returncode
status=run('compile',[a.javac,'-cp',cp,'-d',str(classes)]+[str(x) for x in sources])
if status==0:
    status=run('native_archive',['java','-Xmx256m','-Djava.library.path='+str(a.native_dir.resolve()),'-Dr982.readonly.bridge='+str(root/'verification/r996/read_only_bridge.py'),'-cp',cp,'com.ethk3.novaonepro.DiagnosisNativeArchiveCurrent',str(root/'verification/r983/typed_live_bridge.py'),str(fixture),str(work/'files'),str(out/'native_archive_receipt.json')])
receipt={'source_commit':subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip(),'executions':records,'production_class_binding':json.loads(a.host_classpath.with_name('CURRENT_BYTECODE_BINDING.json').read_text()),'sources':{str(x.relative_to(root)):hashlib.sha256(x.read_bytes()).hexdigest() for x in sources},'initial_schema_fixture_sha256':hashlib.sha256(a.schema.read_bytes()).hexdigest(),'native_library_sha256':hashlib.sha256(library.read_bytes()).hexdigest(),'scope':'Actual current production bytecode plus explicit host SQLite/Linux file adapters and fresh current production JNI. Independent synthetic fixture; no customer row deletion, Android Service lifecycle, SAF, device or natural future proof.'}
(out/'CURRENT_EXECUTION.json').write_text(json.dumps(receipt,indent=2)+'\n');raise SystemExit(status)
