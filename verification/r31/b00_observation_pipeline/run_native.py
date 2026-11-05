#!/usr/bin/env python3
"""Complete production observation entry and report reader; isolated native HOST, no Android claim."""
from pathlib import Path
import argparse,datetime,hashlib,json,os,shutil,subprocess,tempfile
p=argparse.ArgumentParser();p.add_argument('--repo',type=Path,default=Path('/workspace/r31-integration-20261105'));p.add_argument('--support',type=Path,default=Path('/workspace/r31-f607-frozen/classes.jar'));p.add_argument('--only',choices=['all','claim-wait','pipeline-fault','pipeline-observation','capacity-rows','capacity-bytes'],default='all');args=p.parse_args()
ROOT=args.repo.resolve();HERE=Path(__file__).resolve().parent
OUT=HERE/'runs'/datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ');OUT.mkdir(parents=True)
TMP=Path(tempfile.mkdtemp(prefix='.r31-b00-observation-',dir='/workspace'));(TMP/'java-tmp').mkdir()
SUPPORT=TMP/'fixed-support.jar';SDK=TMP/'android.jar';JDBC=TMP/'sqlite-jdbc.jar'
shutil.copy2(args.support,SUPPORT);shutil.copy2('/workspace/starcore-environment/android-sdk/platforms/android-36/android.jar',SDK);shutil.copy2('/tmp/r31-baseline-deps/sqlite-jdbc.jar',JDBC)
sha=lambda b:hashlib.sha256(b).hexdigest()
assert sha(SUPPORT.read_bytes())=='111dfd9f2591906036d031a32adfcc999ab58e644ebda7e24548fceb1d37e30f'
sources=[Path('app/src/main/java/com/ethk3/novaonepro/strategyv2')/(n+'.java') for n in ['ResearchPoolR27','RuntimeFaultEvidenceR31','ReportSnapshotR30','ReportR27','RuntimeDiagnosticLinksR31']]
hashes={str(p):sha((ROOT/p).read_bytes()) for p in sources}
PLATFORM=TMP/'platform';PLATFORM.mkdir();platform_sources=TMP/'platform-source';platform_sources.mkdir()
shims=[]
for name in ['android/database/Cursor.java','android/content/ContentValues.java','android/database/sqlite/SQLiteOpenHelper.java']:
    original=ROOT/'tools/r31/ddl_host'/name;target=platform_sources/name;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(original,target);shims.append(target)
target=platform_sources/'android/database/sqlite/SQLiteDatabase.java';shutil.copy2(HERE/'adapters/android/database/sqlite/SQLiteDatabase.java',target);shims.append(target)
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
adapters['android/os/Build.java']='package android.os;public final class Build{public static final String MODEL="HOST_JVM",BRAND="HOST",DEVICE="HOST",FINGERPRINT="HOST_NATIVE_SQLITE_ONLY";public static final String[] SUPPORTED_ABIS={System.getProperty("os.arch")};public static final class VERSION{public static final int SDK_INT=34;public static final String RELEASE="HOST";}}'
adapters['android/os/CancellationSignal.java']='package android.os;public final class CancellationSignal{private volatile boolean cancelled;public void cancel(){cancelled=true;}public boolean isCanceled(){return cancelled;}public void throwIfCanceled(){if(cancelled)throw new OperationCanceledException("HOST_CANCELLED");}}'
for name,text in adapters.items():
    target=platform_sources/name;target.parent.mkdir(parents=True,exist_ok=True);target.write_text(text);shims.append(target)

shims.extend(sorted((ROOT/'verification/r932/android-json/org/json').glob('*.java')))

shims.append(ROOT/'verification/r73/adapters/android/system/Os.java')

receipt={'schema':'B00_FULL_OBSERVATION_PIPELINE_NATIVE_1','candidateHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'candidateRoot':str(ROOT),'supportSha256':sha(SUPPORT.read_bytes()),'sourceSha256':hashes,'sourceOverlayScope':'Explicit five current sources; all other production uses copied fixed actual4493-class f607 appjar. No DbStore facade.','android':'NOT_RUN','naturalQuality':'NOT_PROVEN','commands':[],'status':'RUNNING'}
def run(name,argv,expected=0,marker=None):
 environment=dict(os.environ);environment.pop('JAVA_TOOL_OPTIONS',None);environment['TMPDIR']=str(TMP/'java-tmp');environment['SQLITE_TMPDIR']=str(TMP/'java-tmp')
 result=subprocess.run(list(map(str,argv)),cwd=TMP,env=environment,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,timeout=180)
 (OUT/(name+'.log')).write_text(result.stdout);frames=[json.loads(s[6:]) for s in result.stdout.splitlines() if s.startswith('FRAME ')];(OUT/(name+'.frames.json')).write_text(json.dumps(frames,indent=2)+'\n');receipt['commands'].append({'name':name,'argv':list(map(str,argv)),'exit':result.returncode,'expected':expected,'logSha256':sha(result.stdout.encode()),'frames':len(frames)});(OUT/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n');print(name,result.returncode,result.stdout[-1800:],flush=True)
 if result.returncode!=expected or marker and marker not in result.stdout:receipt['status']='FAILED_RETAINED_ORIGINAL_LOG';(OUT/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n');raise RuntimeError(name)
compiler=['java','-Djava.io.tmpdir='+str(TMP/'java-tmp'),'-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-g']
run('platform-compile',[*compiler,'-cp',os.pathsep.join(map(str,[SDK,SUPPORT,JDBC])),'-d',PLATFORM,*shims])
classes=TMP/'classes';classes.mkdir();fixed=[]
for path in sources:
 target=OUT/'source_snapshot'/path;target.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(ROOT/path,target);fixed.append(target)
run('actual-five-overlay-compile',[*compiler,'-cp',os.pathsep.join(map(str,[SDK,SUPPORT])),'-sourcepath',TMP/'empty','-d',classes,*fixed])
cp=os.pathsep.join(map(str,[PLATFORM,classes,SUPPORT,JDBC,SDK]));test=TMP/'test';test.mkdir()
run('harness-compile',[*compiler,'-cp',cp,'-d',test,HERE/'ObservationPipelineHostR31.java'])
main='com.ethk3.novaonepro.strategyv2.ObservationPipelineHostR31';cp=str(test)+os.pathsep+cp
# Baseline uses actual frozen f607 ResearchPool class; the other four current overlays are binary compatible.
old=TMP/'baseline-pool';old.mkdir();import zipfile
receipt['baselinePoolClassesSha256']={}
receipt['baselinePoolSourceCommit']='f60757fd56c04da3900babababdb0e1721cadb71'
receipt['baselinePoolSourceSha256']=sha(subprocess.check_output(['git','show',receipt['baselinePoolSourceCommit']+':app/src/main/java/com/ethk3/novaonepro/strategyv2/ResearchPoolR27.java'],cwd=ROOT))
with zipfile.ZipFile(SUPPORT) as archive:
 for name in archive.namelist():
  if name.startswith('com/ethk3/novaonepro/strategyv2/ResearchPoolR27') and name.endswith('.class'):
   dest=old/name;dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(archive.read(name));receipt['baselinePoolClassesSha256'][name]=sha(dest.read_bytes())
oldcp=os.pathsep.join(map(str,[test,PLATFORM,old,classes,SUPPORT,JDBC,SDK]));java=['java','-Djava.io.tmpdir='+str(TMP/'java-tmp'),'-Xmx512m']
if args.only in ['all','claim-wait']:
 run('before-claim-wait',[*java,'-cp',oldcp,main,'claim-wait',TMP/'before-wait.db'],1,'NORMAL_UNCLAIMED_WAIT_MUST_NOT_COUNT_RECEIPT_FAILURE')
 run('after-claim-wait',[*java,'-cp',cp,main,'claim-wait',TMP/'after-wait.db'])
for mode in ['pipeline-fault','pipeline-observation','capacity-rows','capacity-bytes']:
 if args.only in ['all',mode]:run(mode,[*java,'-cp',cp,main,mode,TMP/(mode+'.db')])
receipt['sourceChangedAfterSnapshot']=[str(p) for p in sources if sha((ROOT/p).read_bytes())!=hashes[str(p)]]
receipt['status']='NAMED_COMPLETE_OBSERVATION_HOST_PASS_NOT_ANDROID' if not receipt['sourceChangedAfterSnapshot'] else 'FIXED_SNAPSHOTS_PASSED_CURRENT_SOURCE_DRIFT_REQUIRES_REBIND'
(OUT/'receipt.json').write_text(json.dumps(receipt,indent=2)+'\n');print('EVIDENCE',OUT,flush=True)
