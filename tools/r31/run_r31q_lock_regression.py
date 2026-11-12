#!/usr/bin/env python3
"""Compile current product sources and reproduce real cross-process admission contention.
Compile-time Android signatures throw if used; the exercised filesystem census,
OS locks and scheduling use current product code. No Android runtime is claimed.
"""
import hashlib,json,os,subprocess,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
MAIN=ROOT/'app/src/main/java/com/ethk3/novaonepro'
sources=[MAIN/(p+'.java') for p in ['DirectoryCursorR31','StorageCensusSU2','StorageBudgetR31','ReportRequestStatusR31','strategyv2/FailureDispositionR31']]
drivers=[ROOT/'tools/r31/host/R31QLockRegressionHost.java']
out=Path(os.environ.get('R31Q_OUTPUT',os.environ.get('R312_HOST_OUTPUT',str(ROOT/'verification/r31q/021'))))
out.mkdir(parents=True,exist_ok=True)
logs=[]
def run(args,expect=0):
    p=subprocess.run(list(map(str,args)),cwd=ROOT,text=True,capture_output=True,timeout=60)
    logs.append(p.stdout+p.stderr)
    if p.returncode!=expect:raise RuntimeError('\n'.join(logs))
    return p
with tempfile.TemporaryDirectory(prefix='r31q-lock-') as temp:
    temp=Path(temp);classes=temp/'classes';classes.mkdir();annotations=temp/'annotations/androidx/annotation';annotations.mkdir(parents=True)
    for name,field in [('ChecksSdkIntAtLeast','int api();'),('RequiresApi','int value();')]:
        (annotations/(name+'.java')).write_text('package androidx.annotation;public @interface '+name+'{'+field+'}')
    signatures={
        'android/database/Cursor.java':'package android.database;public interface Cursor extends java.io.Closeable{boolean moveToFirst();boolean moveToNext();String getString(int i);long getLong(int i);void close();}',
        'android/os/CancellationSignal.java':'package android.os;public class CancellationSignal{}',
        'android/os/OperationCanceledException.java':'package android.os;public class OperationCanceledException extends RuntimeException{}',
        'android/database/sqlite/SQLiteDatabase.java':'package android.database.sqlite;public class SQLiteDatabase{public android.database.Cursor rawQuery(String s,String[] a,android.os.CancellationSignal c){throw new UnsupportedOperationException("SDK_SIGNATURE_ONLY_NOT_TESTED");}}',
    }
    for name,text in signatures.items():
        p=temp/'signatures'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text)
    args=['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-d',classes,*annotations.glob('*.java'),*(temp/'signatures').rglob('*.java')]
    run([*args,*sources,*drivers])
    p=run(['java','-Xmx128m','-cp',classes,'com.ethk3.novaonepro.R31QLockRegressionHost','run',temp/'app'])
    print(p.stdout,end='')
(out/'lock-regression.log').write_text('\n'.join(logs))
(out/'LOCK_REGRESSION.json').write_text(json.dumps({'status':'PASS_HOST_ONLY','scope':'real separate JVMs, OS file locks and files; current product sources; unused SDK signatures throw; no Android, device or natural quality claim','sources':{str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources+drivers},'checks':['transient contention resumes without retrying user request','permanent contention respects monotonic deadline and does not mutate ledger','cancellation preserves interrupt','capacity contract still rejects','process-local UI failure is request-scoped and bounded']},ensure_ascii=False,indent=2)+'\n')
