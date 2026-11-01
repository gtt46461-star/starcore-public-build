"""Actual telemetry sources with JDBC SQLite; a bounded HOST coordinator adapter is never packaged."""
import pathlib,os,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[2]
plan=root/'construction/plans/STARCORE_R30_FINAL_REPAIR_20261101'
src=root/'app/src/main/java/com/ethk3/novaonepro/strategyv2'
jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
def run(args):subprocess.run(list(map(str,args)),cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='repair-writer-') as directory:
 tmp=pathlib.Path(directory);out=tmp/'classes';out.mkdir()
 run(['javac','-encoding','UTF-8','-d',out,src/'WriterHoldLedgerR30.java',plan/'verification/WriterRotateTest.java'])
 run(['java','-cp',out,'com.ethk3.novaonepro.strategyv2.WriterRotateTest'])
 adapters={
 'com/ethk3/novaonepro/DbStore.java':'''package com.ethk3.novaonepro;
 import java.sql.*;import android.database.sqlite.SQLiteDatabase;
 public final class DbStore {
 public final Coordinator coordinator;public DbStore(Connection connection){coordinator=new Coordinator(connection);}
 public Coordinator coordinatorForResearchR952(){return coordinator;}
 public interface Work<T>{T call(SQLiteDatabase db)throws Exception;}
 public static final class Coordinator {
 final Connection connection;final SQLiteDatabase db;public boolean rejectBefore=true,rejectAfter;
 Coordinator(Connection c){connection=c;db=new SQLiteDatabase(c);}
 public <T>T runAtomicWriteOrThrowF01(String operation,Work<T> work){
 try{if(rejectBefore){rejectBefore=false;throw new IllegalStateException("SYNTHETIC_BUSY_BEFORE");}
 connection.setAutoCommit(false);T value=work.call(db);if(rejectAfter){rejectAfter=false;throw new IllegalStateException("SYNTHETIC_DISK_FAILURE_AFTER_SQL");}connection.commit();return value;
 }catch(Exception failure){try{connection.rollback();}catch(Exception ignored){}throw new IllegalStateException(failure);}
 finally{try{connection.setAutoCommit(true);}catch(Exception failure){throw new IllegalStateException(failure);}}}
 }}''',
 'android/util/Log.java':'''package android.util;public final class Log{public static int w(String tag,String message,Throwable error){return 0;}}''',
 'com/ethk3/novaonepro/strategyv2/NativeWriterRetryHost.java':'''package com.ethk3.novaonepro.strategyv2;
 import java.sql.*;import com.ethk3.novaonepro.DbStore;
 public final class NativeWriterRetryHost {
 static void need(boolean b,String message){if(!b)throw new AssertionError(message);}
 static void due()throws Exception{java.lang.reflect.Field f=HoldLedgerFlushR30.class.getDeclaredField("lastFlush");f.setAccessible(true);f.setLong(null,0);}
 static long total(Connection db)throws Exception{try(Statement s=db.createStatement();ResultSet r=s.executeQuery("SELECT coalesce(sum(count),0) FROM r30_writer_hold")){return r.getLong(1);}}
 public static void main(String[] args)throws Exception{Class.forName("org.sqlite.JDBC");try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
 try(Statement s=c.createStatement()){s.execute(HoldLedgerFlushR30.DDL);}WriterHoldLedgerR30.rotate();DbStore store=new DbStore(c);
 for(int i=0;i<12;i++)WriterHoldLedgerR30.record("old",true,40000,41000,false);HoldLedgerFlushR30.maybeFlush(store);need(total(c)==0,"rejected first write");
 for(int i=0;i<3;i++)WriterHoldLedgerR30.record("new",false,1,1,false);store.coordinator.rejectAfter=true;due();HoldLedgerFlushR30.maybeFlush(store);need(total(c)==0,"all actual SQL rolled back");
 due();HoldLedgerFlushR30.maybeFlush(store);need(total(c)==12,"original pending12 retained");
 try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT wait_p95,histogram_json,process_session FROM r30_writer_hold WHERE operation='old'")){need(r.next(),"actual original window");need(r.getObject(1)==null,"open upper not Long.MAX ms");need(r.getString(2).contains(WriterHoldLedgerR30.HISTOGRAM_VERSION),"persisted histogram contract");need(r.getString(3).equals(WriterHoldLedgerR30.PROCESS_SESSION),"process scope exact");}
 due();HoldLedgerFlushR30.maybeFlush(store);need(total(c)==15,"new3 preserved without duplicate old12");due();HoldLedgerFlushR30.maybeFlush(store);need(total(c)==15,"empty flush no duplicate");
 System.out.println("WRITER_RETRY_NATIVE_SQLITE_HOST_PASS old=12 new=3 before_busy_and_after_sql_rollback_preserved=1 duplicate=0 open_upper_histogram=1 scope=production_sources_with_HOST_coordinator_NOT_Android_NOT_process_death");}}
 }'''}
 files=[]
 for name,text in adapters.items():
  p=tmp/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(text);files.append(p)
 run(['javac','-encoding','UTF-8','-cp',out,'-d',out,*sorted((root/'tools/r29/qual_host/shim').rglob('*.java')),*files,src/'JsonR27.java',src/'HoldLedgerFlushR30.java'])
 run(['java','-cp',os.pathsep.join(map(str,[out,jdbc])),'com.ethk3.novaonepro.strategyv2.NativeWriterRetryHost'])
print('FR_B02_WRITER_SOURCE_AND_NATIVE_SQLITE_HOST_SCOPE_PASS_NOT_ANDROID_OR_PROCESS_DEATH')
