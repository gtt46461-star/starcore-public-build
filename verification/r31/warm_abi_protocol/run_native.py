#!/usr/bin/env python3
"""Compile the assigned publication sources against a copied, fixed Android app jar.
Runs exact native SQLite actor paths and retains old-budget failures verbatim.
No Gradle, mutable main-worktree classes, device, or user database.
"""
from pathlib import Path
import datetime,gzip,hashlib,json,os,re,shutil,subprocess,tempfile

ROOT=Path(__file__).resolve().parents[3]
BASE='503efdc2c7abdb78b6e30fbd9d881d8dc772265e'
from generate_root_hunks import patched_sources
OVERLAYS=patched_sources()
ORIGINAL_TESTS=Path('/workspace/r31-evolution-publication-961a/verification/r31/evolution_publication')
OUT=Path(__file__).resolve().parent/'runs'/datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
OUT.mkdir(parents=True,exist_ok=False)
TMP=Path(tempfile.mkdtemp(prefix='.r31-warm-abi-native-',dir='/workspace'))
SUPPORT=TMP/'fixed-support.jar';SDK=TMP/'android.jar';JDBC=TMP/'sqlite-jdbc.jar'
shutil.copy2('/workspace/.r31-warm-abi-fixed/classes.jar',SUPPORT)
shutil.copy2('/workspace/starcore-environment/android-sdk/platforms/android-36/android.jar',SDK)
shutil.copy2('/tmp/r31-baseline-deps/sqlite-jdbc.jar',JDBC)
sha=lambda data:hashlib.sha256(data).hexdigest()
sources=[Path('app/src/main/java/com/ethk3/novaonepro/strategyv2')/(name+'.java') for name in ['ModelPublicationR31','ModelStoreSU2','LegacyGenerationR26','FoundryCampaignR26','StageStoreSU2','MatrixStoreSU2','PolicyIdentityLedgerR26','WarmCandidateR26','WarmModelSeedR26']]
sources.append(Path('app/src/main/java/com/ethk3/novaonepro/strategyv2/WarmPublicationR31.java'))
sources.append(Path('app/src/main/java/com/ethk3/novaonepro/strategyv2/RuntimeCoordinatorR26.java'))
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
 'android/system/OsConstants.java':'package android.system;public final class OsConstants{public static final int O_RDONLY=0;}',
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

shims.append(ROOT/'verification/r73/adapters/android/system/Os.java')
shims.append(ROOT/'verification/r31/warm_abi_protocol/adapters/android/os/StatFs.java')
receipt={'schema':'R31_WARM_ABI_NATIVE_1','base':BASE,'supportSha256':sha(SUPPORT.read_bytes()),'sourceSha256':hashes,'android':'NOT_RUN','trainingQuality':'NOT_RUN','commands':[]}
assert receipt['supportSha256']=='582f7475b43d218c50d34f8f24d4399ae4916b53579807c22c162bd25b7fea56'
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
classes=TMP/'classes';classes.mkdir();fixed=[]
for path in sources:
 target=OUT/'source_snapshot'/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(OVERLAYS[path][1]) if path in OVERLAYS else shutil.copy2(ROOT/path,target);fixed.append(target)
run('production-compile',[*compiler,'-cp',str(SDK)+os.pathsep+str(SUPPORT),'-sourcepath',TMP/'empty','-d',classes,*fixed])
cp=os.pathsep.join(map(str,[PLATFORM,classes,SUPPORT,JDBC,SDK]));test=TMP/'test';test.mkdir()
run('harness-compile',[*compiler,'-cp',cp,'-d',test,Path(__file__).resolve().parent/'WarmAbiHostR31.java',Path(__file__).resolve().parent/'WarmProtocolHostR31.java',Path(__file__).resolve().parent/'WarmIndexVmHostR31.java',ORIGINAL_TESTS/'PublicationBaselineHostR31.java',ORIGINAL_TESTS/'PublicationNativeHostR31.java',ROOT/'tools/strategy_su2/tests/R26WarmSeedTest.java'])
main='com.ethk3.novaonepro.strategyv2.WarmAbiHostR31';cp=str(test)+os.pathsep+cp
run('original-all-family-seed',['java','-Xmx512m','-cp',cp,'com.ethk3.novaonepro.strategyv2.R26WarmSeedTest'])
for heap in ['512m','64m']:
 run('new-warm-birth-'+heap,['java','-Xmx'+heap,'-cp',cp,main,'birth',TMP/('birth'+heap+'.db')])
protocol='com.ethk3.novaonepro.strategyv2.WarmProtocolHostR31'
for mode in ['drain','closed','rollback','source','late-owner','cancel','diagnostic']:
 run('warm-'+mode,['java','-Xmx512m','-cp',cp,protocol,mode,TMP/(mode+'.db')])
run('warm-real-process-halt',['java','-Xmx512m','-cp',cp,protocol,'halt',TMP/'restart.db'],74)
run('warm-new-jvm-resume',['java','-Xmx512m','-cp',cp,protocol,'resume',TMP/'restart.db'])
index='com.ethk3.novaonepro.strategyv2.WarmIndexVmHostR31'
run('new-initializer-100000-native-vm',['java','-Xmx512m','-cp',cp,index,'new',TMP/'vm-new.db'])
oldclasses=TMP/'baseline-classes';oldclasses.mkdir();old=[]
for path in sources:
 if path.stem not in ['WarmCandidateR26','WarmModelSeedR26']:continue
 target=OUT/'baseline_source_snapshot'/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(subprocess.check_output(['git','show',BASE+':'+str(path)],cwd=ROOT));old.append(target)
run('baseline-warm-compile',[*compiler,'-cp',os.pathsep.join(map(str,[SDK,SUPPORT])),'-sourcepath',TMP/'empty','-d',oldclasses,*old])
oldcp=os.pathsep.join(map(str,[test,PLATFORM,oldclasses,classes,SUPPORT,JDBC,SDK]))
run('baseline-initializer-100000-native-vm',['java','-Xmx512m','-cp',oldcp,index,'old',TMP/'vm-old.db'],1,'BASELINE_INITIALIZER_WRITER_SCAN_R31')
receipt['rootCallerOverlaySha256']={str(p):sha(v[1].encode()) for p,v in OVERLAYS.items()}
receipt['status']='NAMED_WARM_PROTOCOL_HOST_NOT_ANDROID_OR_FULL_B02'

receipt['changedDuringRun']=[str(p) for p in sources if sha((ROOT/p).read_bytes())!=hashes[str(p)]]
assert not receipt['changedDuringRun']
(OUT/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n');print('EVIDENCE',OUT,flush=True)
