#!/usr/bin/env python3
"""Compile only the two owned producers, against a copied, fixed Android app jar.
Runs exact native SQLite actor paths and retains old-budget failures verbatim.
No Gradle, mutable main-worktree classes, device, or user database.
"""
from pathlib import Path
import datetime,gzip,hashlib,json,os,re,shutil,subprocess,tempfile

ROOT=Path(__file__).resolve().parents[3]
BASE='3246ccf8eb6f7b2f7b9d0fe77fc18a70ab958056'
OUT=Path(__file__).resolve().parent/'runs'/datetime.datetime.now(datetime.timezone.utc).strftime('%Y%m%dT%H%M%SZ')
OUT.mkdir(parents=True,exist_ok=False)
TMP=Path(tempfile.mkdtemp(prefix='r31-live-physical-native-'))
SUPPORT=TMP/'fixed-support.jar';SDK=TMP/'android.jar';JDBC=TMP/'sqlite-jdbc.jar'
shutil.copy2('/tmp/r31-p02-012-actual-android-support/classes.jar',SUPPORT)
shutil.copy2('/workspace/starcore-environment/android-sdk/platforms/android-36/android.jar',SDK)
shutil.copy2('/tmp/r31-baseline-deps/sqlite-jdbc.jar',JDBC)
sha=lambda data:hashlib.sha256(data).hexdigest()
sources=[Path('app/src/main/java/com/ethk3/novaonepro/strategyv2')/(name+'.java') for name in ['LiveShadowsR27','QualificationInboxR29']]
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
receipt={'schema':'R31_LIVE_PRODUCER_PHYSICAL_NATIVE_1','started_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),
 'base_commit':BASE,'branch':subprocess.check_output(['git','branch','--show-current'],cwd=ROOT,text=True).strip(),
 'owned_source_sha256':hashes,'support_jar_sha256':sha(SUPPORT.read_bytes()),'sdk_sha256':sha(SDK.read_bytes()),'jdbc_sha256':sha(JDBC.read_bytes()),
 'support_jar_origin':'/tmp/r31-p02-012-actual-android-support/classes.jar','private_execution_workspace':str(TMP),
 'runner_sha256':sha(Path(__file__).read_bytes()),'harness_sha256':sha((Path(__file__).resolve().parent/'LiveBudgetHostR31.java').read_bytes()),
 'sqlite_adapter_sha256':sha((Path(__file__).resolve().parent/'adapters/android/database/sqlite/SQLiteDatabase.java').read_bytes()),
 'gradle':'NOT_RUN','mutable_main_worktree_classes_used':False,'android_device':'NOT_RUN','natural_quality':'NOT_PROVEN','commands':[],'results':[]}
def run(name,args,expected=0,failure_marker=None):
    result=subprocess.run([str(a) for a in args],cwd=TMP,stdout=subprocess.PIPE,stderr=subprocess.STDOUT,text=True,timeout=240)
    data=result.stdout.encode();log=OUT/(name+'.log.gz')
    with gzip.open(log,'wb') as f:f.write(data)
    record={'name':name,'argv':[str(a) for a in args],'exit_code':result.returncode,'expected_exit_code':expected,'log':log.name,'uncompressed_sha256':sha(data)}
    receipt['commands'].append(record)
    frames=[json.loads(line[6:]) for line in result.stdout.splitlines() if line.startswith('FRAME ')]
    observations=[json.loads(line[7:]) for line in result.stdout.splitlines() if line.startswith('RESULT ')]
    if observations:receipt['results'].append({'name':name,'observations':observations})
    framepath=OUT/(name+'.frames.json.gz')
    with gzip.open(framepath,'wt') as f:json.dump(frames,f,ensure_ascii=False)
    record['frames']=framepath.name
    (OUT/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'name':name,'exit':result.returncode,'expected':expected,'frames':len(frames),'output':result.stdout[-1500:]}),flush=True)
    if result.returncode!=expected:raise RuntimeError('Unexpected result, original log retained: '+name)
    if expected==1 and (failure_marker or 'BASELINE_FAIL_REPRODUCED_R31_WRITER_BUDGET') not in result.stdout:raise RuntimeError('Expected failure was not reproduced for the named reason: '+name)
    return result.stdout
def existing_contract_tests(compile_command,cp):
    """Retain the original old-contract failure, then adapt only chunk/prefix
    assumptions. Original random-arrival, conflict, replay and numeric checks stay.
    No checked-in tools source is edited and no historical tool pass is claimed.
    """
    import difflib
    names=['tools/r29/qual_host/QualInboxR29HostTest.java','tools/r30/qual_host/SettleChunkR30HostTest.java','tools/r30/qual_host/PendingDrawR30HostTest.java']
    originals={name:(ROOT/name).read_text() for name in names}
    adapted=dict(originals)
    q=names[0]
    for old,new in [
       ('plan(a, rows, 30, 256)','plan(a, rows, 30, 16)'),
       ('plan(b2, rows, 30, 256)','plan(b2, rows, 30, 16)'),
       ('            check("Q6_STALE_STEP_COMMITS_NOTHING"','            pass(db, s, 30); // Finish the remaining original opportunities through bounded production prefixes.\n            check("Q6_STALE_STEP_COMMITS_NOTHING"')]:
        if adapted[q].count(old)!=1:raise RuntimeError('Old qualification test contract drift: '+old)
        adapted[q]=adapted[q].replace(old,new)
    q=names[1]
    for old,new in [
       ('chunks.size() == 4','chunks.size() == (640 + LiveShadowsR27.SETTLE_CHUNK_R30 - 1) / LiveShadowsR27.SETTLE_CHUNK_R30'),
       ('sized &= n == 160','sized &= n > 0 && n <= LiveShadowsR27.SETTLE_CHUNK_R30'),
       ('S1_640_ROWS_FOUR_CHUNKS_seed','S1_640_ROWS_BOUNDED_PHYSICAL_CHUNKS_seed'),
       ('afterDeath == 160 && k2[0] == 480','afterDeath == LiveShadowsR27.SETTLE_CHUNK_R30 && k2[0] == 640 - LiveShadowsR27.SETTLE_CHUNK_R30')]:
        if adapted[q].count(old)!=1:raise RuntimeError('Old settlement test contract drift: '+old)
        adapted[q]=adapted[q].replace(old,new)
    evidence=[]
    for version,data in [('original',originals),('bounded-contract-adapted',adapted)]:
        dest=TMP/('existing-tests-'+version);dest.mkdir();files=[]
        for name,text in data.items():
            target=dest/Path(name).name;target.write_text(text);files.append(target)
            archive=OUT/'existing_test_sources'/version/Path(name).name;archive.parent.mkdir(parents=True,exist_ok=True);archive.write_text(text)
        classes=TMP/('existing-test-classes-'+version);classes.mkdir()
        testcp=str(classes)+os.pathsep+cp
        run('existing-'+version+'-compile',[*compile_command,'-cp',cp,'-d',classes,*files])
        if version=='original':
            run('existing-original-qualification-old-prefix-contract',['java','-Xmx512m','-cp',testcp,'com.ethk3.novaonepro.strategyv2.QualInboxR29HostTest'],1,'R31_QUALIFICATION_PHYSICAL_PREFIX_TOO_LARGE')
            run('existing-original-settlement-old-160-contract',['java','-Xmx512m','-cp',testcp,'com.ethk3.novaonepro.strategyv2.SettleChunkR30HostTest'],1,'R30_SETTLE|SUMMARY|FAILED')
        else:
            for cls in ['QualInboxR29HostTest','SettleChunkR30HostTest','PendingDrawR30HostTest']:
                run('existing-bounded-contract-'+cls,['java','-Xmx512m','-cp',testcp,'com.ethk3.novaonepro.strategyv2.'+cls])
    for name in names:
        diff=''.join(difflib.unified_diff(originals[name].splitlines(keepends=True),adapted[name].splitlines(keepends=True),fromfile=name,tofile='bounded-contract-adapted/'+Path(name).name))
        evidence.append({'source':name,'original_sha256':sha(originals[name].encode()),'adapted_sha256':sha(adapted[name].encode()),'diff':diff})
    (OUT/'existing_test_contract_changes.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2)+'\n')
compiler=['java','-m','jdk.compiler/com.sun.tools.javac.Main','-encoding','UTF-8','-g']
try:
    run('platform-compile',[*compiler,'-cp',os.pathsep.join(map(str,[SDK,SUPPORT,JDBC])),'-d',PLATFORM,*shims])
    builds={}
    for version in ['baseline','new']:
        src=TMP/(version+'-source');classes=TMP/(version+'-classes');test=TMP/(version+'-test');src.mkdir();classes.mkdir();test.mkdir()
        files=[]
        for relative in sources:
            data=subprocess.check_output(['git','show',BASE+':'+str(relative)],cwd=ROOT) if version=='baseline' else (ROOT/relative).read_bytes()
            target=src/relative;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(data);files.append(target)
            archived=OUT/'source_snapshot'/version/relative;archived.parent.mkdir(parents=True,exist_ok=True);archived.write_bytes(data)
        run(version+'-production-compile',[*compiler,'-cp',str(SDK)+os.pathsep+str(SUPPORT),'-sourcepath',TMP/'empty-sourcepath','-d',classes,*files])
        cp=os.pathsep.join(map(str,[PLATFORM,classes,test,SUPPORT,JDBC,SDK]));builds[version]=cp
        run(version+'-harness-compile',[*compiler,'-cp',cp,'-d',test,Path(__file__).resolve().parent/'LiveBudgetHostR31.java'])
    main='com.ethk3.novaonepro.strategyv2.LiveBudgetHostR31'
    for case in ['baseline-settle','baseline-pending','baseline-consume','baseline-resolve']:
        run(case,['java','-Xmx512m','-cp',builds['baseline'],main,case,TMP/'baseline-fixtures'],1)
    for case in ['settle','pending','consume','resolve','faults']:
        run('new-'+case,['java','-Xmx512m','-cp',builds['new'],main,case,TMP/'new-fixtures'])
    run('new-real-jvm-halt',['java','-Xmx512m','-cp',builds['new'],main,'halt',TMP/'halt-fixture'],73)
    run('new-fresh-jvm-resume',['java','-Xmx512m','-cp',builds['new'],main,'resume',TMP/'halt-fixture'])
    for case in ['settle','pending','resolve']:
        run('new-real-jvm-halt-'+case,['java','-Xmx512m','-cp',builds['new'],main,'halt-'+case,TMP/'halt-fixture'],73)
        run('new-fresh-jvm-resume-'+case,['java','-Xmx512m','-cp',builds['new'],main,'resume-'+case,TMP/'halt-fixture'])
    existing_contract_tests(compiler,builds['new'])
    changed=[str(p) for p in sources if sha((ROOT/p).read_bytes())!=hashes[str(p)]]
    if changed:raise RuntimeError('Owned production source changed during run: '+str(changed))
    receipt.update(status='NAMED_PRODUCER_NATIVE_HOST_PASS_NOT_ANDROID_NOT_FULL_B02',source_changed_during_run=changed,
      limitations=['Controlled bootstrap and synthetic predraw rows are mechanics fixtures, never natural prediction/evidence or a user DB.','Protocol/support executes fixed actual Android app classes on host platform adapters; no Android scheduling/WAL latency/24h/72h acceptance.','Each frame includes CAS/read/DML/fence/progress/receipt and binds, with transaction duration. These are native host transaction timings, not Android writer hold SLO.','Freeze/certify large payloads and legacy qualification rebuild remain explicit separate debts.'])
except Exception as error:
    receipt.update(status='EXECUTION_OR_COUNTEREXAMPLE_FAILURE_NOT_PASS',failure=str(error));raise
finally:
    receipt['finished_utc']=datetime.datetime.now(datetime.timezone.utc).isoformat()
    (OUT/'receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2)+'\n')
    print('RECEIPT '+str(OUT/'receipt.json'),flush=True)
