#!/usr/bin/env python3
"""Final release owners only: compile Android adapters and the observational fixture."""
import argparse,hashlib,json,shutil,subprocess
from pathlib import Path
p=argparse.ArgumentParser()
for key in ('jdk','classes','schema','out'):
    p.add_argument('--'+key,type=Path,required=True)
p.add_argument('--preflight',action='store_true',help='Label an isolated product copy as preflight, not final release validation')
a=p.parse_args();root=Path(__file__).resolve().parents[2];out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
tests=out/'tests';tests.mkdir();classes=a.classes.resolve()
android=Path((a.schema/'host-classpath').read_text().strip().split(':')[-1])
resources=root/'app/build/intermediates/compile_and_runtime_not_namespaced_r_class_jar/release/processReleaseResources/R.jar'
cp=':'.join(map(str,[tests,classes,resources,android]))
owners=('AutoFetchService','DatabaseCoordinator','DbStore','ResearchWorkBudgetR927','ResearchRuntimeSqlGateR911',
        'ResearchCaptureQueueR946','ResearchLearningContinuityR912','DataRetentionGovernorR900','StorageReclaimR943','ServiceHeartbeatStore')
def hashes():
    return {str(v.relative_to(classes)):hashlib.sha256(v.read_bytes()).hexdigest()
            for name in owners for v in sorted((classes/'com/ethk3/novaonepro').glob(name+'*.class'))}
before=hashes();records=[]
def run(name,command,required=True):
    log=out/(name+'.log')
    with log.open('w') as f:result=subprocess.run(list(map(str,command)),cwd=root,stdout=f,stderr=subprocess.STDOUT)
    records.append({'name':name,'exit_code':result.returncode,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest()})
    (out/'steps.json').write_text(json.dumps(records,indent=2)+'\n')
    if required and result.returncode:print(log.read_text()[-8000:]);raise SystemExit(result.returncode)
    return result.returncode
sources=[root/v for v in (
    'verification/r75/fetch_callback_actual.java','verification/r974/HostProductR974.java',
    'verification/r64/adapters/android/database/sqlite/SQLiteDatabase.java',
    'verification/r974/android/database/sqlite/SQLiteOpenHelper.java',
    'verification/r974/android/database/DatabaseUtils.java','verification/r967/host/android/content/ContentValues.java',
    'verification/r967/host/android/os/CancellationSignal.java','verification/canonical/schema/Looper.java',
    'verification/r36-stubs/android/util/Log.java','verification/r979/android/database/Cursor.java',
    'verification/r991/android/os/Build.java','verification/canonical/adapters/android/os/Process.java',
    'verification/r973/clock-stubs/android/os/Handler.java','verification/r72/realtime/android/os/SystemClock.java')]
run('compile',[a.jdk/'bin/javac','-encoding','UTF-8','-sourcepath',out/'empty-sourcepath','-cp',cp,'-d',tests,*sources])
collisions=[str(v.relative_to(tests)) for v in tests.rglob('*.class') if (classes/v.relative_to(tests)).is_file()]
assert not collisions,'PRODUCT_CLASS_OVERLAY_FORBIDDEN:'+str(collisions)
database=out/'callback.db';shutil.copy2(a.schema/'schema-complete.db',database)
status=run('test',[a.jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.fetch_callback_actual',
                  root/'verification/canonical/schema/sqlite_bridge.py',database,out/'result.json',out/'context'],False)
after=hashes();assert before and before==after,'FINAL_PRODUCT_BYTECODE_CHANGED_DURING_TEST'
(out/'product-bytecode.json').write_text(json.dumps({'mode':'PREFLIGHT_ISOLATED_PRODUCT_COPY' if a.preflight else 'FINAL_BYTECODE_NO_PRODUCT_OVERLAY',
    'product_classes':str(classes),
    'unchanged':before==after,'product_class_collisions':collisions,'before':before,'after':after},indent=2)+'\n')
(out/'source-hashes.json').write_text(json.dumps({str(v.relative_to(root)):hashlib.sha256(v.read_bytes()).hexdigest() for v in sources},indent=2)+'\n')
print((out/'test.log').read_text());raise SystemExit(status)
