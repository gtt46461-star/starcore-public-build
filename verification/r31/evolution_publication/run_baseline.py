#!/usr/bin/env python3
"""Compile the assigned publication sources against a copied, fixed Android app jar.
Runs exact native SQLite actor paths and retains old-budget failures verbatim.
No Gradle, mutable main-worktree classes, device, or user database.
"""
from pathlib import Path
import datetime,gzip,hashlib,json,os,re,shutil,subprocess,tempfile

ROOT=Path(__file__).resolve().parents[3]
BASE='961a6b8a74b9624a2b195e9562fbdfc9283b29fc'
OUT=Path(__file__).resolve().parent/'runs'/datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
OUT.mkdir(parents=True,exist_ok=False)
TMP=Path(tempfile.mkdtemp(prefix='.r31-publication-native-',dir='/workspace'))
SUPPORT=TMP/'fixed-support.jar';SDK=TMP/'android.jar';JDBC=TMP/'sqlite-jdbc.jar'
shutil.copy2('/tmp/r31-evolution-publication-frozen/classes.jar',SUPPORT)
shutil.copy2('/workspace/starcore-environment/android-sdk/platforms/android-36/android.jar',SDK)
shutil.copy2('/tmp/r31-baseline-deps/sqlite-jdbc.jar',JDBC)
sha=lambda data:hashlib.sha256(data).hexdigest()
sources=[Path('app/src/main/java/com/ethk3/novaonepro/strategyv2')/(name+'.java') for name in ['ModelStoreSU2','LegacyGenerationR26','FoundryCampaignR26','StageStoreSU2','MatrixStoreSU2','PolicyIdentityLedgerR26']]
hashes={str(p):sha((ROOT/p).read_bytes()) for p in sources}
PLATFORM=TMP/'platform';PLATFORM.mkdir();platform_sources=TMP/'platform-source';platform_sources.mkdir()
shims=[]
for name in ['android/database/Cursor.java','android/content/ContentValues.java','android/database/sqlite/SQLiteOpenHelper.java']:
    original=ROOT/'tools/r31/ddl_host'/name;target=platform_sources/name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(original,target);shims.append(target)
target=platform_sources/'android/database/sqlite/SQLiteDatabase.java';shutil.copy2(Path(__file__).resolve().parent/'adapters/android/database/sqlite/SQLiteDatabase.java',target);shims.append(target)
for name in ['SQLiteException','SQLiteConstraintException','SQLiteDatabaseLockedException']:
    original=ROOT/'tools/r29/qual_host/shim/android/database/sqlite'/(name+'.java')
    if original.exists():
        target=platform_sources/'android/database/sqlite'/(name+'.java');shutil.copy2(original,target);shims.append(target)
adapters={
 'android/content/Context.java':'package android.content;public class Context{public java.io.File getDatabasePath(String name){return new java.io.File(name);}public java.io.File getFilesDir(){return new java.io.File("files");}public Context getApplicationContext(){return this;}}',
 'android/app/Application.java':'package android.app;public class Application extends android.content.Context{private final java.io.File database;public Application(java.io.File file){database=file;}public java.io.File getDatabasePath(String name){return database;}public java.io.File getFilesDir(){java.io.File files=new java.io.File(database.getParentFile(),"files");files.mkdirs();return files;}}',
 'android/os/SystemClock.java':'package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000L;}}',
 'android/os/Looper.java':'package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}',
 'android/os/Process.java':'package android.os;public final class Process{public static final int THREAD_PRIORITY_BACKGROUND=10;public static void setThreadPriority(int value){}public static int myPid(){return(int)java.lang.ProcessHandle.current().pid();}}',
 'android/util/Log.java':'package android.util;public final class Log{public static int w(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int e(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}public static int d(String t,String m){return 0;}public static int i(String t,String m){return 0;}}'
}
for name,text in adapters.items():
    target=platform_sources/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(text);shims.append(target)

shims.extend(sorted((ROOT/'verification/r932/android-json/org/json').glob('*.java')))
receipt={'schema':'R31_EVOLUTION_PUBLICATION_NATIVE_1','base':BASE,'supportSha256':sha(SUPPORT.read_bytes()),'sourceSha256':hashes,'android':'NOT_RUN','trainingQuality':'NOT_RUN','commands':[]}
assert receipt['supportSha256']=='15a524242feefe47f158c3cb4513b0ac4a5c7dc9038b4bf924525d11ec710b96'
def run(name,args,expected=0,marker=None):
    result=subprocess.run(list(map(str,args)),cwd=TMP,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=240)
    (OUT/(name+'.log')).write_text(result.stdout)
    frames=[json.loads(s[6:]) for s in result.stdout.splitlines() if s.startswith('FRAME ')]
    (OUT/(name+'.frames.json')).write_text(json.dumps(frames,indent=2)+'\n')
    receipt['commands'].append({'name':name,'argv':list(map(str,args)),'exit':result.returncode,'expected':expected,'logSha256':sha(result.stdout.encode()),'frames':len(frames)})
    (OUT/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
    print(name,result.returncode,result.stdout[-1800:],flush=True)
    if result.returncode!=expected or marker and marker not in result.stdout:raise RuntimeError(name)
compiler=['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-g']
run('platform-compile',[*compiler,'-cp',os.pathsep.join(map(str,[SDK,SUPPORT,JDBC])),'-d',PLATFORM,*shims])
classes=TMP/'baseline-classes';classes.mkdir();original=[]
for path in sources:
    data=subprocess.check_output(['git','show',BASE+':'+str(path)],cwd=ROOT)
    target=TMP/'baseline-source'/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data);original.append(target)
run('baseline-production-compile',[*compiler,'-cp',str(SDK)+os.pathsep+str(SUPPORT),'-sourcepath',TMP/'empty','-d',classes,*original])
cp=os.pathsep.join(map(str,[PLATFORM,classes,SUPPORT,JDBC,SDK]));test=TMP/'test';test.mkdir()
run('baseline-harness-compile',[*compiler,'-cp',cp,'-d',test,Path(__file__).resolve().parent/'PublicationBaselineHostR31.java'])
run('baseline-pop24-hof12',['java','-Xmx512m','-cp',str(test)+os.pathsep+cp,'com.ethk3.novaonepro.strategyv2.PublicationBaselineHostR31',TMP/'baseline.db'],1,'BASELINE_FAIL_REPRODUCED_R31_PUBLICATION_BUDGET')
receipt['status']='BASELINE_COUNTEREXAMPLE_REPRODUCED_NOT_REPAIR_PASS'
(OUT/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n')
print('EVIDENCE',OUT,flush=True)
