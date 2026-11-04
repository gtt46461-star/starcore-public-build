"""Native SQLite API-call tracing of actual business methods; no Android/device DDL-zero claim."""
from datetime import datetime,timezone
import hashlib,json,os
from pathlib import Path
import re,subprocess,tempfile

ROOT=Path(__file__).resolve().parents[2]
MAIN=ROOT/'app/src/main/java/com/ethk3/novaonepro'
BASE_CLASSES=ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
CLASSES=Path(os.environ.get('R31_HOST_SUPPORT_CLASSES',str(BASE_CLASSES)))
SDK=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar'
JDBC=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'))
OUT=ROOT/'verification/r31/p02/runtime-ddl-hotspots'
JAVA=os.environ.get('R31_HOST_JAVA','java')
PRODUCTION=['DatabaseCoordinator','V51Repository','DeferredEvidenceWriterR35','RuntimeHealthLedgerR35','DbHelper','SchemaDomainCoordinatorR926','FetchLedgerR925','ApiClient','strategyv2/CertificateRevocationR30','strategyv2/FailureDispositionR31','strategyv2/SchemaR27','strategyv2/RuntimeSchemaR26']
OWNED=['DatabaseCoordinator','V51Repository','DeferredEvidenceWriterR35','RuntimeHealthLedgerR35','strategyv2/CertificateRevocationR30']
hashes={str((MAIN/(name+'.java')).relative_to(ROOT)):hashlib.sha256((MAIN/(name+'.java')).read_bytes()).hexdigest() for name in PRODUCTION}
OUT.mkdir(parents=True,exist_ok=True)
commands=[]
def run(args,log):
    if args[0]=='javac':args=[JAVA,'-m','jdk.compiler/com.sun.tools.javac.Main',*args[1:]]
    elif args[0]=='java':args=[JAVA,*args[1:]]
    result=subprocess.run(list(map(str,args)),cwd=ROOT,text=True,capture_output=True)
    (OUT/log).write_text(result.stdout+result.stderr)
    commands.append(dict(command=list(map(str,args)),log=log,exitCode=result.returncode))
    if result.returncode:
        print(result.stdout+result.stderr);raise RuntimeError('failed '+log)
    print(result.stdout[-3500:],end='')
    return result.stdout

with tempfile.TemporaryDirectory(prefix='r31-runtime-ddl-') as temporary:
    tmp=Path(temporary);platform,sources,production,test=[tmp/name for name in ['platform','sources','production','test']]
    for folder in [platform,sources,production,test]:folder.mkdir()
    adapters={
      'android/content/Context.java':'package android.content;public class Context{public java.io.File getDatabasePath(String name){return new java.io.File(name);}public Context getApplicationContext(){return this;}}',
      'android/app/Application.java':'package android.app;public class Application extends android.content.Context{private final java.io.File path;public Application(java.io.File path){this.path=path;}public java.io.File getDatabasePath(String name){return path;}}',
      'android/os/SystemClock.java':'package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return java.lang.management.ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime()/1000000L;}}',
      'android/os/Looper.java':'package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}',
      'android/util/Log.java':'package android.util;public final class Log{public static int w(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int e(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}public static int d(String t,String m){return 0;}public static int i(String t,String m){return 0;}}',
      'com/ethk3/novaonepro/DbStore.java':'''package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
public final class DbStore{private final SQLiteDatabase db;private final DatabaseCoordinator coordinator;private final java.util.Map<String,String> state=new java.util.HashMap<>();
public DbStore(SQLiteDatabase db,DatabaseCoordinator coordinator){this.db=db;this.coordinator=coordinator;}public DbStore(SQLiteDatabase db){this(db,null);}public SQLiteDatabase readable(){return db;}public SQLiteDatabase writable(){return db;}public DatabaseCoordinator coordinatorR36(){return coordinator;}public DatabaseCoordinator coordinatorForResearchR952(){return coordinator;}public void setState(String key,String value){state.put(key,value);}public String getState(String key,String fallback){if(state.containsKey(key))return state.get(key);try(Cursor c=db.rawQuery("SELECT value FROM host_state WHERE key=?",new String[]{key})){return c.moveToFirst()?c.getString(0):fallback;}catch(RuntimeException missing){return fallback;}}public Models.Trade getTradeByTargetIssue(String target){try(Cursor c=db.rawQuery("SELECT 1 FROM trades WHERE target_issue=? AND action='BET'",new String[]{target})){return c.moveToFirst()?new Models.Trade():null;}}}
'''
    }
    shims=list((ROOT/'tools/r31/ddl_host').rglob('*.java'))
    for name in ['SQLiteException','SQLiteConstraintException']:
        path=ROOT/'tools/r29/qual_host/shim/android/database/sqlite'/(name+'.java')
        if path.exists():shims.append(path)
    for name,text in adapters.items():
        path=sources/name;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text);shims.append(path)
    run(['javac','-nowarn','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[SDK,CLASSES,BASE_CLASSES])),'-d',platform,*shims],'platform-javac.log')
    # Existing compiled numerical closure is explicit support. All edited owners and schema definitions below compile from actual current source.
    compile_cp=os.pathsep.join(map(str,[SDK,CLASSES,BASE_CLASSES]))
    run(['javac','-nowarn','-encoding','UTF-8','-cp',compile_cp,'-d',production,*[MAIN/(name+'.java') for name in PRODUCTION]],'production-javac.log')
    runtime_cp=os.pathsep.join(map(str,[platform,production,test,CLASSES,BASE_CLASSES,JDBC,SDK]))
    run(['javac','-nowarn','-encoding','UTF-8','-cp',runtime_cp,'-d',test,ROOT/'tools/r31/host/R31RuntimeDdlHost.java',ROOT/'tools/r30/qual_host/CertificateRevocationR30HostTest.java',ROOT/'tools/r30/qual_host/RealAuthorityR30HostTest.java'],'business-host-javac.log')
    actual=run(['java','-Xmx768m','-cp',runtime_cp,'com.ethk3.novaonepro.R31RuntimeDdlHost',tmp/'business.db',OUT/'SQL_API_CALLS.tsv'],'business-native.log')
    run(['java','-Xmx768m','-cp',runtime_cp,'com.ethk3.novaonepro.strategyv2.CertificateRevocationR30HostTest'],'original-revocation-native.log')
    run(['java','-Xmx768m','-cp',runtime_cp,'com.ethk3.novaonepro.strategyv2.RealAuthorityR30HostTest'],'original-authority-native.log')
    for path,digest in hashes.items():
        if hashlib.sha256((ROOT/path).read_bytes()).hexdigest()!=digest:raise RuntimeError('production source changed during compile/test: '+path)
    events=[]
    for line in (OUT/'SQL_API_CALLS.tsv').read_text().splitlines():
        stage,method,sql=line.split('\t',2)
        if stage.startswith('BUSINESS_'):events.append(dict(stage=stage,method=method,sql=sql,ddl=bool(re.match(r'\s*(CREATE|ALTER|DROP|VACUUM|REINDEX)\b',sql,re.I))))
    grouped={}
    for event in events:
        counts=grouped.setdefault(event['stage'],dict(calls=0,ddl=0));counts['calls']+=1;counts['ddl']+=int(event['ddl'])
    if not events or any(value['ddl'] for value in grouped.values()):raise RuntimeError('business SQL trace is empty or has DDL')
    result=dict(card='R31-B02-C02',phase='P02',status='HOST_PASS_ROOT_ANDROID_INTEGRATION_PENDING',capturedAt=datetime.now(timezone.utc).isoformat(),sourceHead=subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),sourceDirty=True,
       assertions=int(re.search(r'R31_RUNTIME_DDL_NATIVE_HOST_PASS assertions=(\d+)',actual).group(1)),productionSourceSHA256=hashes,
       supportClasses=str(CLASSES),baselineCompiledNumericalClosure=str(BASE_CLASSES),supportReceipt=os.environ.get('R31_HOST_SUPPORT_RECEIPT','ROOT_ANDROID_SUPPORT_CLASSES_NO_EXTRA_RECEIPT'),hostJavaLauncher=JAVA,
       ownedSourceSHA256={str((MAIN/(name+'.java')).relative_to(ROOT)):hashes[str((MAIN/(name+'.java')).relative_to(ROOT))] for name in OWNED},
       sqlTrace=dict(path='SQL_API_CALLS.tsv',scope='JAVA_SQLITE_API_CALLS_EXECUTED_ON_NATIVE_SQLITE_JDBC_WITH_HOST_PLATFORM_ADAPTERS_NOT_ANDROID_TRACE',businessStages=grouped,businessDDL=0),
       regressions=dict(originalCertificateRevocation='PASS_FOUR_SOURCE_MODES_LATE_SU2_AND_ACTUAL_NEXT_READERS',originalRealAuthority='PASS'),commands=commands,
       android='NOT_RUN',device='NOT_RUN',continuous24h='NOT_RUN',continuous72h='NOT_RUN',naturalQuality='NOT_PROVEN',allApplicationRuntimeDDL='NOT_ESTABLISHED',
       limitations=['Controlled SQL/frozen-bundle/authority fixtures are not natural prediction or Android timing evidence.','Only named traced business methods are covered; source audit identifies other remaining paths.','DbStore and its legacy mainchain runtime DDL are owned by another task.','The dormant R35 enqueue still cannot reconstruct a missing in-memory model result after restart; the actual R36 snapshot owner remains authoritative. These changes preserve its snapshot/lease columns and persistent retry markers.'])
    (OUT/'RECEIPT.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
print(json.dumps(dict(status=result['status'],assertions=result['assertions'],tracedBusinessStages=len(grouped),businessDDL=0,originalRevocation='PASS',originalAuthority='PASS'),ensure_ascii=False))
