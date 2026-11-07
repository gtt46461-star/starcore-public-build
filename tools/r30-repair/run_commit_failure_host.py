"""Run original and repaired production coordinators against a real native SQLite COMMIT failure."""
import pathlib,os,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[2];main=root/'app/src/main/java/com/ethk3/novaonepro'
classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
def run(args):subprocess.run(list(map(str,args)),cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='repair-actual-commit-') as temporary:
 out=pathlib.Path(temporary);platform=out/'platform';platform.mkdir();sources=out/'sources';sources.mkdir();test=out/'test';test.mkdir()
 shim_sources=[]
 for source in sorted((root/'tools/r29/qual_host/shim').rglob('*.java')):
  target=sources/source.relative_to(root/'tools/r29/qual_host/shim');target.parent.mkdir(parents=True,exist_ok=True);text=source.read_text()
  if source.name=='SQLiteDatabase.java':
   at=text.index('    static final class Rows')
   text=text[:at]+'''    private boolean successful;
    public void beginTransaction(){try{jdbc.setAutoCommit(false);successful=false;}catch(java.sql.SQLException e){throw new SQLiteException(e.getMessage());}}
    public void setTransactionSuccessful(){successful=true;}
    public void endTransaction(){try{if(successful)jdbc.commit();else jdbc.rollback();}catch(java.sql.SQLException failure){try{jdbc.rollback();}catch(java.sql.SQLException ignored){}throw new SQLiteException(failure.getMessage());}finally{successful=false;try{jdbc.setAutoCommit(true);}catch(java.sql.SQLException failure){throw new SQLiteException(failure.getMessage());}}}
'''+text[at:]
  target.write_text(text);shim_sources.append(target)
 adapters={
 'android/os/SystemClock.java':'''package android.os;public final class SystemClock{public static long elapsedRealtime(){return System.nanoTime()/1000000;}public static long uptimeMillis(){return elapsedRealtime();}public static long currentThreadTimeMillis(){return elapsedRealtime();}public static void sleep(long time){try{Thread.sleep(time);}catch(InterruptedException e){Thread.currentThread().interrupt();}}}''',
 'android/os/Looper.java':'''package android.os;public final class Looper{public static Looper myLooper(){return null;}public static Looper getMainLooper(){return null;}}''',
 'android/util/Log.java':'''package android.util;public final class Log{public static int w(String t,String m){return 0;}public static int w(String t,String m,Throwable e){return 0;}public static int e(String t,String m){return 0;}public static int e(String t,String m,Throwable e){return 0;}public static int d(String t,String m){return 0;}public static int i(String t,String m){return 0;}}''',
 'com/ethk3/novaonepro/DbHelper.java':'''package com.ethk3.novaonepro;import android.database.sqlite.SQLiteDatabase;public final class DbHelper{private final SQLiteDatabase db;public DbHelper(SQLiteDatabase db){this.db=db;}public SQLiteDatabase getReadableDatabase(){return db;}public SQLiteDatabase getWritableDatabase(){return db;}}''',
 'com/ethk3/novaonepro/strategyv2/WriterHoldLedgerR30.java':'''package com.ethk3.novaonepro.strategyv2;import java.util.concurrent.locks.ReentrantLock;public final class WriterHoldLedgerR30{public static ReentrantLock observedLock;public static long observedWhileLocked,observedFailures;public static final String PROCESS_SESSION="SYNTHETIC_COORDINATOR_SCOPE";public static void record(String op,boolean critical,long wait,long hold,boolean failed){if(observedLock!=null&&observedLock.isHeldByCurrentThread())observedWhileLocked++;if(failed)observedFailures++;}public static void record(String op,boolean critical,long wait,long hold,boolean failed,String outcome){record(op,critical,wait,hold,failed);}}'''}
 for name,text in adapters.items():
  p=sources/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);shim_sources.append(p)
 run(['javac','-nowarn','-encoding','UTF-8','-cp',sdk,'-d',platform,*shim_sources])
 # Compile the full actual coordinator against the real Android SDK and current app contracts.
 production=out/'production';production.mkdir()
 run(['javac','-nowarn','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',production,main/'DatabaseCoordinator.java',main/'strategyv2/IssueTimelineR30.java',main/'strategyv2/IssueTimelineWriterR30.java'])
 driver=root/'tools/r30-repair/qual_host/RepairCommitFailureR30HostTest.java'
 run(['javac','-nowarn','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[platform,production,classes,sdk])),'-d',test,driver])
 baseline_source=out/'baseline-source/DatabaseCoordinator.java';baseline_source.parent.mkdir();baseline_source.write_bytes(subprocess.check_output(['git','show','b225ab204db043b347f9d76d6c5c1376fd9a2590:app/src/main/java/com/ethk3/novaonepro/DatabaseCoordinator.java'],cwd=root))
 baseline=out/'baseline';baseline.mkdir();run(['javac','-nowarn','-encoding','UTF-8','-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',baseline,baseline_source])
 for mode,directory in [('baseline',baseline),('fixed',production)]:
  run(['java','-Xmx512m','-cp',os.pathsep.join(map(str,[platform,directory,test,classes,jdbc,sdk])),'com.ethk3.novaonepro.RepairCommitFailureR30HostTest',mode])
print('REPAIR_COMMIT_FAILURE_BASELINE_AND_FIXED_NATIVE_SQLITE_HOST_PASS_NOT_ANDROID')
