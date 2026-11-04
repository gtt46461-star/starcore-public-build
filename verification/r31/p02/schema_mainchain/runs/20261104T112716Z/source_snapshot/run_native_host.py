#!/usr/bin/env python3
"""Source-bound DbStore/Legacy schema execution on isolated native SQLite, no Gradle/device."""
from pathlib import Path
import datetime,hashlib,json,os,re,shutil,subprocess,tempfile

ROOT=Path(__file__).resolve().parents[4]
MAIN=ROOT/'app/src/main/java/com/ethk3/novaonepro'
CLASSES=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
SDK=Path('/workspace/starcore-environment/android-sdk/platforms/android-36/android.jar')
JDBC=Path('/tmp/r31-baseline-deps/sqlite-jdbc.jar')
OUT=Path(__file__).resolve().parent/'runs'/datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
OUT.mkdir(parents=True)
TMP=Path(tempfile.mkdtemp(prefix='r31-mainchain-native-'))
PLATFORM,SOURCES,PRODUCTION,TEST=[TMP/name for name in ['platform','sources','production','test']]
for directory in [PLATFORM,SOURCES,PRODUCTION,TEST]:directory.mkdir()
sha=lambda data:hashlib.sha256(data).hexdigest()
names=['DbStore','LegacySchemaCoordinatorR926','DbHelper','DatabaseCoordinator','SchemaDomainCoordinatorR926',
       'ReleaseManifestR926','V51Repository','SchemaRevisionR925','RuntimeEventLedgerR926','SettlementTaskStoreR36',
       'DrawDependencyInputCP12','TimeSourceR811','strategyv2/FailureDispositionR31','strategyv2/RuntimeSchemaR26']
snapshots={};production=[]
for name in names:
    relative=Path('app/src/main/java/com/ethk3/novaonepro')/(name+'.java')
    data=(ROOT/relative).read_bytes();destination=SOURCES/relative;destination.parent.mkdir(parents=True,exist_ok=True);destination.write_bytes(data)
    snapshots[str(relative)]=sha(data);production.append(destination)
shutil.copytree(SOURCES,OUT/'source_snapshot')
harness=Path(__file__).resolve().parent/'MainChainSchemaR31Host.java'
shutil.copyfile(harness,OUT/'source_snapshot'/harness.name)
shutil.copyfile(Path(__file__),OUT/'source_snapshot'/Path(__file__).name)
adapters={
 'android/content/Context.java':'package android.content;public class Context{public java.io.File getDatabasePath(String name){return new java.io.File(name);}public java.io.File getFilesDir(){return new java.io.File("files");}public Context getApplicationContext(){return this;}}',
 'android/app/Application.java':'package android.app;public class Application extends android.content.Context{private final java.io.File database;public Application(java.io.File db){database=db;}public java.io.File getDatabasePath(String name){return database;}public java.io.File getFilesDir(){java.io.File files=new java.io.File(database.getParentFile(),"files");files.mkdirs();return files;}}',
 'android/os/SystemClock.java':'package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000L;}}',
 'android/os/Looper.java':'package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}',
 'android/util/Log.java':'package android.util;public final class Log{public static int w(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int e(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}public static int d(String t,String m){return 0;}public static int i(String t,String m){return 0;}}'
}
shims=list((ROOT/'tools/r31/ddl_host').rglob('*.java'))
for name in ['SQLiteException','SQLiteConstraintException']:
    path=ROOT/'tools/r29/qual_host/shim/android/database/sqlite'/(name+'.java')
    if path.exists():shims.append(path)
for name,source in adapters.items():
    path=SOURCES/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(source);shims.append(path)
receipt={'schema':'R31_MAIN_CHAIN_NATIVE_HOST_1','started_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
 'source_head':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
 'production_source_sha256':snapshots,'harness_sha256':sha(harness.read_bytes()),'runner_sha256':sha(Path(__file__).read_bytes()),
 'temporary_workspace':str(TMP),'commands':[],'scope':'FRESH_FULL_PRODUCTION_DBSTORE_LEGACY_HELPER_COORDINATOR_NATIVE_SQLITE_HOST',
 'gradle':'NOT_RUN','android':'NOT_RUN','device':'NOT_RUN','natural_quality':'NOT_PROVEN',
 'limitations':['Unsafe bypasses application/SQLiteOpenHelper construction only; actual production business/readiness/migration methods execute on JDBC SQLite.',
 'Host Critical predecessor schema methods are exercised on an isolated fresh database; this is not full Android startup/backup migration validation.',
 'Only named mainchain/SHADOW business paths are traced; no claim all application DDL or 64-op writer batch acceptance is complete.']}
def run(name,args):
    result=subprocess.run([str(arg) for arg in args],cwd=TMP,text=True,stdout=subprocess.PIPE,stderr=subprocess.STDOUT)
    path=OUT/(name+'.log');path.write_text(result.stdout)
    receipt['commands'].append({'name':name,'argv':[str(arg) for arg in args],'exit_code':result.returncode,'log':path.name,'log_sha256':sha(path.read_bytes())})
    (OUT/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'name':name,'exit_code':result.returncode,'log':str(path)}),flush=True)
    if result.returncode:raise RuntimeError('Command failed; original output retained: '+str(path))
    return result.stdout
try:
    compiler=['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8']
    run('platform-javac',[*compiler,'-cp',os.pathsep.join(map(str,[SDK,CLASSES])),'-d',PLATFORM,*shims])
    support=[SDK,Path('/tmp/r31-p02-compile'),CLASSES]
    run('production-javac',[*compiler,'-cp',os.pathsep.join(map(str,support)),'-sourcepath',SOURCES/'EMPTY_SOURCEPATH','-d',PRODUCTION,*production])
    runtime=os.pathsep.join(map(str,[PLATFORM,PRODUCTION,TEST,CLASSES,JDBC,SDK]))
    run('harness-javac',[*compiler,'-cp',runtime,'-d',TEST,harness])
    stdout=run('mainchain-native',['java','-Xmx768m','-cp',runtime,'com.ethk3.novaonepro.MainChainSchemaR31Host',TMP/'mainchain.db',OUT/'SQL_API_CALLS.tsv'])
    grouped={}
    for line in (OUT/'SQL_API_CALLS.tsv').read_text().splitlines():
        stage,method,sql=line.split('\t',2)
        counts=grouped.setdefault(stage,{'calls':0,'ddl':0})
        counts['calls']+=1;counts['ddl']+=int(bool(re.match(r'\s*(CREATE|ALTER|DROP|REINDEX|VACUUM|ATTACH|DETACH)\b',sql,re.I)))
    for stage in ['BUSINESS_MISSING','BUSINESS_READY','MIGRATION_REPEAT']:
        assert grouped[stage]['calls']>0 and grouped[stage]['ddl']==0,(stage,grouped[stage])
    changed=[relative for relative,digest in snapshots.items() if sha((ROOT/relative).read_bytes())!=digest]
    if changed:raise RuntimeError('Source changed during run; original sampled evidence retained: '+str(changed))
    receipt.update(status='HOST_MAINCHAIN_NAMED_PATHS_PASS_NOT_ANDROID_NOT_FULL_B02',source_changed_during_run=changed,
      sql_trace={'path':'SQL_API_CALLS.tsv','sha256':sha((OUT/'SQL_API_CALLS.tsv').read_bytes()),'scope':'JAVA_SQLITE_API_CALLS_ON_ACTUAL_NATIVE_SQLITE_JDBC','stages':grouped},
      checks=[line.removeprefix('CHECK ') for line in stdout.splitlines() if line.startswith('CHECK ')])
except Exception as error:
    receipt.update(status='EXECUTION_OR_HARNESS_FAILURE_NOT_PASS',failure=str(error));raise
finally:
    receipt['finished_utc']=datetime.datetime.now(datetime.timezone.utc).isoformat()
    (OUT/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    print('RECEIPT '+str(OUT/'receipt.json'),flush=True)
