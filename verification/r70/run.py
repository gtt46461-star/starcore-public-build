#!/usr/bin/env python3
"""Run actual compiled product against real SQLite. No device/live-quality claim."""
import argparse, hashlib, json, os, subprocess
from pathlib import Path
p=argparse.ArgumentParser()
p.add_argument('--classes',type=Path,required=True)
p.add_argument('--android-runtime',type=Path,required=True)
p.add_argument('--jdk',type=Path,required=True)
p.add_argument('--out',type=Path,required=True)
p.add_argument('--native',type=Path,required=True)
a=p.parse_args();r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True,exist_ok=True)
ad=o/'adapters';ad.mkdir(exist_ok=True)
sources=['r64/adapters/android/database/sqlite/SQLiteDatabase.java','r974/android/database/sqlite/SQLiteOpenHelper.java','r974/android/database/DatabaseUtils.java','r967/host/android/content/ContentValues.java','r967/host/android/os/SystemClock.java','r967/host/android/os/CancellationSignal.java','canonical/schema/Looper.java','r36-stubs/android/util/Log.java','r979/android/database/Cursor.java','r991/android/os/Build.java','canonical/adapters/android/os/Process.java','r974/HostProductR974.java','r974/NumericWorldHarnessR974.java','r66/RuntimeRegressionR66.java','r67/RuntimeRegressionR67.java','r68/ReportAndStartupR68.java','r69/ReportResetR69.java','r70/RuntimeClosureR70.java','canonical/adapters/android/util/AtomicFile.java','canonical/tests/audit_cp12/native/java/com/ethk3/novaonepro/ArchivePipelineActualCP12.java']
cp=os.pathsep.join(map(str,[ad,a.classes.resolve(),a.android_runtime.resolve()]))
records=[]
def run(name,args):
 log=o/(name+'.log')
 with log.open('w') as f:status=subprocess.run(list(map(str,args)),cwd=r,stdout=f,stderr=subprocess.STDOUT).returncode
 records.append({'name':name,'exit_code':status,'log_sha256':hashlib.sha256(log.read_bytes()).hexdigest()})
 (o/'result.json').write_text(json.dumps({'scope':'ACTUAL_PRODUCT_REAL_SQLITE_SYNTHETIC_INPUT','device':'NOT_RUN','future_performance':'NOT_MEASURED','steps':records},indent=2)+'\n')
 print(log.read_text()[-8000:],end='')
 if status:raise SystemExit(status)
run('compile-adapters',[a.jdk/'bin/javac','-encoding','UTF-8','-cp',cp,'-d',ad]+[r/'verification'/s for s in sources])
run('regression',[a.jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.RuntimeRegressionR66',r/'verification/r64/sqlite_bridge.py',o/'database'])
run('regression-r67',[a.jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.RuntimeRegressionR67',r/'verification/r64/sqlite_bridge.py',o/'database-r67',r])
run('numeric',[a.jdk/'bin/java','-cp',cp,'com.ethk3.novaonepro.r952.NumericWorldHarnessR974'])

run('report-startup-r68',[a.jdk/'bin/java','-Xmx512m','-Djava.library.path='+str(a.native.resolve()),'-cp',cp,'com.ethk3.novaonepro.ReportAndStartupR68',r/'verification/r64/sqlite_bridge.py',o/'database-r68'])
run('archive-streaming',[a.jdk/'bin/java','-Xmx64m','-Djava.library.path='+str(a.native.resolve()),'-cp',cp,'com.ethk3.novaonepro.ArchivePipelineActualCP12',o/'archive-large'])

run('report-control-r69',[a.jdk/'bin/java','-Xmx128m','-cp',cp,'com.ethk3.novaonepro.ReportResetR69',o/'report-reset-r69'])

run('runtime-r70',[a.jdk/'bin/java','-Xmx256m','-cp',cp,'com.ethk3.novaonepro.RuntimeClosureR70',r/'verification/r64/sqlite_bridge.py',o/'database-r70'])
