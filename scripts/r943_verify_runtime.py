#!/usr/bin/env python3
"""Compile current production, then execute targeted real-SQLite regression contracts."""
from pathlib import Path
import os,subprocess
R=Path(__file__).resolve().parents[1];O=R/'build/r943-tests';O.mkdir(parents=True,exist_ok=True)
H=R/'build/r925-host/compiled-classes';A=R/'build/r930-batch34-sql';P=Path(os.environ['ANDROID_PLATFORM_JAR'])
source=list((R/'app/src/main/java').rglob('*.java'))+[R/'verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java',R/'verification/r36-stubs/com/ethk3/novaonepro/R.java']
proc=subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-cp',str(P),'-d',str(H),*map(str,source)],capture_output=True,text=True)
(R/'reports/r943/FULL_COMPILE.txt').write_text(proc.stdout+proc.stderr+f'\nPRODUCTION_JAVA={len(source)-2}|exit={proc.returncode}\n')
if proc.returncode:print(proc.stderr);raise SystemExit(proc.returncode)
print('CURRENT_PRODUCTION_COMPILE_PASS',flush=True)
cp=os.pathsep.join(map(str,[O,A,H,P,A/'platform-classes']))
files=list((R/'verification/r932/android-json').rglob('*.java'))+[R/'verification/r943/AtomicTrainingStorageHarnessR943.java',R/'verification/r943/PrimaryNetworkTrainingHarnessR943.java',R/'verification/r943/StagedRosterGateHarnessR943.java',R/'verification/r943/AlgorithmSettlementHarnessR943.java',R/'verification/r942/FreshInstallSchemaHarnessR942.java',R/'verification/r925/stubs/com/ethk3/novaonepro/DatabaseCoordinator.java']
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-cp',cp,'-d',str(O),*map(str,files)],check=True)
for name in ['FreshInstallSchemaHarnessR942','PrimaryNetworkTrainingHarnessR943','AtomicTrainingStorageHarnessR943','StagedRosterGateHarnessR943','AlgorithmSettlementHarnessR943']:
 proc=subprocess.run(['java','-Xmx192m','-cp',cp,'com.ethk3.novaonepro.'+name,str(R/'verification/r925/sqlite_bridge.py'),str(O/'report-cache')],capture_output=True,text=True)
 (R/'reports/r943'/(name+'.txt')).write_text(proc.stdout+proc.stderr);print(proc.stdout+proc.stderr,end='',flush=True)
 if proc.returncode:print(name+" exit="+str(proc.returncode),flush=True);raise SystemExit(proc.returncode)
print('R943_TARGETED_RUNTIME_PASS',flush=True)
