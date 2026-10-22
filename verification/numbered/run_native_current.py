#!/usr/bin/env python3
"""Compile current production JNI and execute unchanged canonical host contracts."""
import argparse, hashlib, json, pathlib, sqlite3, subprocess, time

root = pathlib.Path(__file__).resolve().parents[2]
p = argparse.ArgumentParser()
p.add_argument('--out', type=pathlib.Path, required=True)
p.add_argument('--jni-headers', type=pathlib.Path, required=True)
p.add_argument('--host-classpath', type=pathlib.Path, required=True)
a = p.parse_args()
out = a.out.resolve()
if out.exists() and any(out.iterdir()):
    raise SystemExit('FRESH_OUTPUT_REQUIRED_PRESERVE_PRIOR_FAILURES')
out.mkdir(parents=True, exist_ok=True)
native = out/'native'; native.mkdir()
classes = out/'classes'; classes.mkdir()
cpp = root/'app/src/main/cpp'
sources = [cpp/'report_native_cp4.c', cpp/'vendor/sqlite3.c']
for group in ['common', 'compress', 'decompress']:
    sources += sorted((cpp/'vendor/zstd/lib'/group).glob('*.c'))
receipt = {'scope':'Current production C/JNI and current Android product bytecode; host x86_64, synthetic filesystem/SQLite fixtures, not Android ABI/device/SAF or natural Future',
           'sources':{str(f.relative_to(root)):hashlib.sha256(f.read_bytes()).hexdigest() for f in sources},
           'wrapper_sha256':hashlib.sha256((root/'app/src/main/java/com/ethk3/novaonepro/ReportNativeCP4.java').read_bytes()).hexdigest(),
           'commands':[]}
def run(name, command, timeout=240):
    begin=time.monotonic()
    with (out/(name+'.log')).open('w') as log:
        r=subprocess.run(list(map(str,command)),stdout=log,stderr=subprocess.STDOUT,timeout=timeout,cwd=root)
    receipt['commands'].append({'name':name,'command':list(map(str,command)), 'exit_code':r.returncode,'seconds':round(time.monotonic()-begin,3)})
    (out/'CURRENT_EXECUTION.json').write_text(json.dumps(receipt,indent=2)+'\n')
    print(name, r.returncode,flush=True)
    return r.returncode

cmd=['gcc','-std=c11','-O2','-fPIC','-shared','-fvisibility=hidden',
     '-DSQLITE_THREADSAFE=1','-DSQLITE_TEMP_STORE=1','-DSQLITE_DEFAULT_MEMSTATUS=1',
     '-DSQLITE_OMIT_LOAD_EXTENSION=1','-DZSTD_DISABLE_ASM=1',
     '-I'+str(a.jni_headers.resolve()),'-I'+str(cpp/'vendor'),'-I'+str(cpp/'vendor/zstd/lib'),
     *sources,'-ldl','-lm','-o',native/'libstarcore_report_cp4.so']
if run('native_compile',cmd):raise SystemExit(1)
receipt['native_library_sha256']=hashlib.sha256((native/'libstarcore_report_cp4.so').read_bytes()).hexdigest()
tests=[root/'verification/canonical/tests/audit_cp12/native/java/com/ethk3/novaonepro/NativeReportContractCP12.java',
       root/'verification/canonical/tests/audit_cp12/native/java/com/ethk3/novaonepro/ArchivePipelineActualCP12.java',
       root/'verification/canonical/tests/audit_cp12/native/cas_java/com/ethk3/novaonepro/CasDirectoryActualCP12.java']
cp=str(classes)+':'+a.host_classpath.read_text().strip()
receipt['tests']={str(t.relative_to(root)):hashlib.sha256(t.read_bytes()).hexdigest() for t in tests}
if run('contracts_compile',[root.parent/'toolchain/bin/javac','-encoding','UTF-8','-cp',cp,'-d',classes,
                           root/'app/src/main/java/com/ethk3/novaonepro/ReportNativeCP4.java',*tests]):raise SystemExit(1)
fixture=out/'native-fixture';fixture.mkdir()
c=sqlite3.connect(fixture/'live.db')
c.execute('CREATE TABLE cp12_jni_fixture(text_payload TEXT,blob_payload BLOB)')
c.execute('INSERT INTO cp12_jni_fixture(rowid,text_payload,blob_payload) VALUES(?,?,?)',[-(1<<63),'A'*(2<<20),bytes(2<<20)])
c.commit();c.close()
for name, directory in [('NativeReportContractCP12',fixture),('ArchivePipelineActualCP12',out/'archive-fixture'),('CasDirectoryActualCP12',out/'cas-fixture')]:
    run(name,['java','-Xmx256m','-Djava.library.path='+str(native),'-cp',cp,'com.ethk3.novaonepro.'+name,directory])
raise SystemExit(0 if all(c['exit_code']==0 for c in receipt['commands']) else 1)
