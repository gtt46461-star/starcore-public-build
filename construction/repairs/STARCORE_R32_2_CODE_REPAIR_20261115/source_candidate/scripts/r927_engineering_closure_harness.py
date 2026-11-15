#!/usr/bin/env python3
"""Host-only production resource/SQL/cache checks. Android services are not claimed executed."""
import hashlib,json,os,subprocess,zipfile
from pathlib import Path
R=Path(__file__).resolve().parents[1];O=R/'build/r927-engineering';O.mkdir(parents=True,exist_ok=True)
REPORT_DIR=R/'reports/r927';REPORT_DIR.mkdir(parents=True,exist_ok=True)
tc=Path(os.environ.get('STARCORE_TOOLCHAIN_ROOT',R.parent/'08_TOOLCHAIN'))
a=Path(os.environ.get('ANDROID_PLATFORM_JAR',tc/'android-platform-36/android.jar'))
h=R/'build/r925-host/compiled-classes';ar=O/'android-runtime'
with zipfile.ZipFile(a) as z:
 for n in z.namelist():
  if n.endswith('.class'):z.extract(n,ar)
stubs={
'android/os/SystemClock.java':'package android.os; public final class SystemClock { public static long elapsedRealtime(){return System.nanoTime()/1000000L;} public static long uptimeMillis(){return elapsedRealtime();} }',
'android/os/Debug.java':'package android.os; public final class Debug { public static long count,time; public static String getRuntimeStat(String key){return Long.toString(key.endsWith("gc-count")?count:time);} }'
}
# Android adapters only. All business/resource/ledger classes come from production compilation.
cv='package android.content; public class ContentValues { private final java.util.LinkedHashMap<String,Object> m=new java.util.LinkedHashMap<String,Object>(); public java.util.Set<java.util.Map.Entry<String,Object>> valueSet(){return m.entrySet();} public void putNull(String k){m.put(k,null);} public Object get(String k){return m.get(k);} '
for t in ['String','Integer','Long','Boolean','Double','Float','Short','Byte','byte[]']:
 cv+='public void put(String k,'+t+' v){m.put(k,v);} '
stubs['android/content/ContentValues.java']=cv+'}'
stubs['android/os/CancellationSignal.java']='package android.os; public final class CancellationSignal { private volatile boolean cancelled; public boolean isCanceled(){return cancelled;} public void cancel(){cancelled=true;} }'
# Generate a transparent CursorWrapper host adapter from the installed SDK interface.
api=subprocess.check_output(['java','-m','jdk.jdeps/com.sun.tools.javap.Main','-classpath',str(a),'android.database.Cursor'],text=True)
import re
wrapper='package android.database; public class CursorWrapper implements Cursor { protected final Cursor delegate; public CursorWrapper(Cursor c){delegate=c;} '
for ret,name,params in re.findall(r'public (?:abstract|default) (\S+) (\w+)\(([^)]*)\)',api):
 types=[] if not params else [x.strip() for x in params.split(',')]
 decl=','.join(t+' a'+str(i) for i,t in enumerate(types));names=','.join('a'+str(i) for i in range(len(types)))
 wrapper+='public '+ret+' '+name+'('+decl+'){'+('' if ret=='void' else 'return ')+'delegate.'+name+'('+names+');}'
stubs['android/database/CursorWrapper.java']=wrapper+'}'
files=[]
for n,s in stubs.items():
 p=O/n;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s);files.append(p)
files += [R/'verification/r925/stubs/android/database/sqlite/SQLiteDatabase.java',R/'verification/r925/stubs/com/ethk3/novaonepro/DbStore.java',R/'verification/r925/stubs/com/ethk3/novaonepro/DatabaseCoordinator.java',R/'verification/r925/R927EngineeringClosureHarness.java',R/'verification/r925/R927BackupReplayFixture.java',R/'verification/r925/R927BackupReplay.java']
subprocess.run(['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[h,a])),'-d',str(O),*map(str,files)],check=True)
p=subprocess.run(['java','-cp',os.pathsep.join(map(str,[O,h,ar])),'com.ethk3.novaonepro.R927EngineeringClosureHarness',str(R/'verification/r925/sqlite_bridge.py')],capture_output=True,text=True)
(O/'RESULT.txt').write_text(p.stdout+p.stderr);print(p.stdout+p.stderr)
if p.returncode or 'R927_ENGINEERING_CLOSURE_PASS' not in p.stdout:raise SystemExit(1)
e={'passed':True,'scope':'PRODUCTION_JAVA_WITH_REAL_SQLITE_AND_INJECTED_ANDROID_COUNTERS','assertions':p.stdout.splitlines(),'device_execution':False,'input_sha256':hashlib.sha256((R/'verification/r925/R927EngineeringClosureHarness.java').read_bytes()).hexdigest(),'production_sources':{str(p.relative_to(R)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted((R/'app/src/main/java').rglob('*.java'))}}
(REPORT_DIR/'ENGINEERING_CLOSURE_RESULT.json').write_text(json.dumps(e,ensure_ascii=False,indent=2)+'\n')
