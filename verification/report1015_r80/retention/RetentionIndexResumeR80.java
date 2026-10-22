package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import org.json.JSONObject;
/** Actual Gradle product governor/coordinator bytecode on the full installed SQLite schema. */
public final class RetentionIndexResumeR80 {
 static int checks;
 static final String TABLE="diagnostic_log",KEY="retention_cp12_revision_after_"+TABLE,CURSOR="retention_cp12_revision_cursor";
 static void ok(boolean value,String message){if(!value)throw new AssertionError(message);checks++;System.out.println("PASS "+message);}
 static long num(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError(sql);return c.getLong(0);}}
 static String state(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",new String[]{key})){return c.moveToFirst()?c.getString(0):"";}}
 static void set(SQLiteDatabase db,String key,String value){db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES(?,?,1)",new Object[]{key,value});}
 static int ordinal(){for(int i=0;i<RetentionTableRegistryCP12.ALL_INSTALLED.size();i++)if(RetentionTableRegistryCP12.ALL_INSTALLED.get(i).table.equals(TABLE))return i;throw new AssertionError("registry");}
 static long metric(SQLiteDatabase db,String key){for(String part:state(db,"retention_cp12_revision_last").split("\\|"))if(part.startsWith(key+"="))return Long.parseLong(part.substring(key.length()+1));throw new AssertionError(key);}
 static int clean(SQLiteDatabase db,long deadline,boolean commit){db.beginTransaction();try{int n=DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(db,deadline);if(commit)db.setTransactionSuccessful();return n;}finally{db.endTransaction();}}
 static final class TraceDatabase extends SQLiteDatabase {
  int pages,maxPageRows,maxDeleteRows;boolean p0AfterPage,deadlineAfterPage,raised;
  TraceDatabase(String bridge,String path)throws Exception{super(bridge,path);}
  @Override public synchronized Cursor rawQuery(String sql,String[] args){
   Cursor c=super.rawQuery(sql,args);
   if(sql.startsWith("SELECT r.source_rowid,r.generation,EXISTS(")){
    pages++;int limit=Integer.parseInt(sql.substring(sql.lastIndexOf("LIMIT ")+6));maxPageRows=Math.max(maxPageRows,limit);
    if(p0AfterPage&&!raised){RealtimePressureGateR811.beginAnalysis();raised=true;}
    if(deadlineAfterPage){try{Thread.sleep(60);}catch(InterruptedException e){throw new AssertionError(e);}}
   }return c;
  }
  @Override public synchronized void execSQL(String sql,Object[] args){
   if(sql.startsWith("DELETE FROM research_row_revision_v2 WHERE generation IN("))maxDeleteRows=Math.max(maxDeleteRows,args.length);
   super.execSQL(sql,args);
  }
  void clearPressure(){if(raised){RealtimePressureGateR811.endAnalysis();raised=false;}}
 }
 public static void main(String[] args)throws Exception{
  String mode=args[0];JSONObject result=new JSONObject().put("mode",mode).put("scope","ACTUAL_GRADLE_PRODUCT_SQLITE_HOST_SYNTHETIC_NO_PHONE_TIMING");
  RealtimePressureGateR811.resetForTests();
  try(TraceDatabase db=new TraceDatabase(args[1],args[2])){
   db.setForeignKeyConstraintsEnabled(true);
   DbStore store=HostProductR974.store(db);File files=new File(args[3]);files.mkdirs();HostProductR974.set(store.coordinatorR36(),"app",new android.app.Application(files,files));
   ok(num(db,"SELECT COUNT(*) FROM sqlite_master WHERE type='trigger'")>=1300,"full installed trigger schema");
   String status="";int turns=0;
   if(mode.equals("baseline")||mode.equals("initial")){
    set(db,CURSOR,Integer.toString(ordinal()));
    long before=num(db,"SELECT COUNT(*) FROM research_row_revision_v2");status=DataRetentionGovernorR900.trim(store,150L);turns++;
    long key=Long.parseLong(state(db,KEY));
    ok(key>0&&key<=2048,"real 50 ms trim persists a bounded live-prefix keyset");
    ok(metric(db,"examined")>0&&metric(db,"examined")<=2048,"at most 2048 index rows examined per trim");
    ok(before==num(db,"SELECT COUNT(*) FROM research_row_revision_v2"),"zero-delete prefix preserves all live generations");
    ok(state(db,CURSOR).equals(Integer.toString((ordinal()+1)%RetentionTableRegistryCP12.ALL_INSTALLED.size())),"table fairness rotation preserved after incomplete large table");
    if(mode.equals("baseline"))ok(status.equals("RETENTION_NO_ELIGIBLE_ROWS"),"exact remote R79 reproduces false empty-backlog status with an unscanned suffix");
    else ok(status.equals("RETENTION_INDEX_SCAN_PROGRESS"),"zero-delete live-prefix scan requests existing short continuation");
    result.put("firstKey",key).put("revisionLast",state(db,"retention_cp12_revision_last")).put("deleted",0).put("statusFromTrim",status);
    String priorKey=state(db,KEY),priorCursor=state(db,CURSOR);RealtimePressureGateR811.beginAnalysis();
    try{ok(DataRetentionGovernorR900.trim(store,150L).equals("YIELD_ACTIVE_CRITICAL"),"P0 at entry preempts actual trim");}finally{RealtimePressureGateR811.endAnalysis();}
    ok(priorKey.equals(state(db,KEY))&&priorCursor.equals(state(db,CURSOR)),"P0 entry preserves persisted scan state");
   }else if(mode.equals("resume")){
    long prior=Long.parseLong(state(db,KEY));ok(prior>0&&prior<=2048,"new JVM reopens committed keyset from prior trim");
    while(num(db,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'")>8192){
     status=DataRetentionGovernorR900.trim(store,150L);turns++;
     ok(!status.equals("RETENTION_NO_ELIGIBLE_ROWS"),"unfinished resumed scan never claims empty backlog at turn "+turns);
     ok(metric(db,"examined")<=2048,"resumed trim retains examined-row cap at turn "+turns);
     if(turns>512)throw new AssertionError("bounded table rotation did not reach suffix");
    }
    ok(num(db,"SELECT COUNT(*) FROM diagnostic_log")==8192,"all 8192 live records survive continuation");
    ok(num(db,"SELECT COUNT(*) FROM diagnostic_log d JOIN r80_expected_generation e ON e.source_rowid=d.rowid JOIN research_row_revision_v2 r ON r.source_table='diagnostic_log' AND r.source_rowid=d.rowid AND r.generation=e.generation")==8192,"all original live generation identities survive continuation");
    ok(num(db,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid>8192")==0,"orphan suffix is eventually removed after restart");
   }else if(mode.equals("invariants")){
    set(db,CURSOR,Integer.toString(ordinal()));
    int removed=clean(db,TimeSourceR811.monoNowMs()+5000,true);
    ok(removed==2048&&metric(db,"examined")==2048,"public int API preserves exact 2048-row cap when deadline permits");
    ok(db.maxPageRows==64&&db.maxDeleteRows==64,"each indexed query and generation-CAS delete remains capped at 64");
    String priorKey=state(db,KEY),priorCursor=state(db,CURSOR);long before=num(db,"SELECT COUNT(*) FROM research_row_revision_v2");
    db.beginTransaction();int attempted;try{set(db,CURSOR,Integer.toString(ordinal()));attempted=DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(db,TimeSourceR811.monoNowMs()+5000);}finally{db.endTransaction();}
    ok(attempted>0,"rollback test actually attempts orphan deletion");
    ok(before==num(db,"SELECT COUNT(*) FROM research_row_revision_v2")&&priorKey.equals(state(db,KEY))&&priorCursor.equals(state(db,CURSOR)),"rollback atomically restores generation rows and both cursors");
    set(db,CURSOR,Integer.toString(ordinal()));db.pages=0;db.p0AfterPage=true;
    try{status=DataRetentionGovernorR900.trim(store,150L);}finally{db.clearPressure();db.p0AfterPage=false;}
    ok(db.pages==1&&metric(db,"examined")==64,"P0 raised after first real SQLite page stops subsequent pages");
    ok(status.equals("RETENTION_INDEX_SCAN_PROGRESS"),"mid-scan P0 cannot masquerade as an empty index");
    set(db,CURSOR,Integer.toString(ordinal()));db.pages=0;db.deadlineAfterPage=true;
    try{status=DataRetentionGovernorR900.trim(store,150L);}finally{db.deadlineAfterPage=false;}
    ok(db.pages==1&&metric(db,"examined")==64,"elapsed 50 ms deadline stops after the completed 64-row page");
    ok(status.equals("RETENTION_INDEX_SCAN_PROGRESS"),"deadline exhaustion keeps short continuation");
    priorKey=state(db,KEY);before=num(db,"SELECT COUNT(*) FROM research_row_revision_v2");db.pages=0;
    ok(clean(db,0,true)==0&&db.pages==0,"expired public deadline performs no candidate query");
    ok(priorKey.equals(state(db,KEY))&&before==num(db,"SELECT COUNT(*) FROM research_row_revision_v2"),"expired deadline preserves keyset and revisions");
    db.execSQL("INSERT INTO diagnostic_log(rowid,log_key,created_at,level,module,code,message,detail) VALUES(9000,'live-cas',1,'INFO','R80','HOST','live','')");
    long oldGeneration=num(db,"SELECT generation FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=9000");
    db.execSQL("UPDATE diagnostic_log SET detail='new generation' WHERE rowid=9000");
    long liveGeneration=num(db,"SELECT generation FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=9000");
    ok(liveGeneration>oldGeneration,"installed update trigger advances live generation identity");
    set(db,CURSOR,Integer.toString(ordinal()));set(db,KEY,"8999");
    ok(clean(db,TimeSourceR811.monoNowMs()+5000,true)==0,"resumed live-only suffix never deletes its generation");
    ok(num(db,"SELECT generation FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=9000")==liveGeneration,"current live generation survives exact-row probe");
    ok(state(db,KEY).equals(Long.toString(Long.MIN_VALUE)),"completed table wraps persisted keyset to catch low replacements");
    db.execSQL("INSERT INTO research_row_revision_v2(source_table,source_rowid) VALUES('diagnostic_log',-7)");
    set(db,CURSOR,Integer.toString(ordinal()));clean(db,TimeSourceR811.monoNowMs()+5000,true);
    ok(num(db,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=-7")==0,"wrapped keyset reaches newly inserted negative-rowid orphan");
    java.util.concurrent.CountDownLatch ready=new java.util.concurrent.CountDownLatch(1);
    java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
    java.util.concurrent.atomic.AtomicBoolean committed=new java.util.concurrent.atomic.AtomicBoolean();
    try(SQLiteDatabase other=new SQLiteDatabase(args[1],args[2])){
     db.beginTransaction();Thread writer=new Thread(()->{ready.countDown();try{
      other.execSQL("INSERT INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES('concurrent',1,'INFO','R80','HOST','new','')");
      other.execSQL("INSERT OR REPLACE INTO diagnostic_log(log_key,created_at,level,module,code,message,detail) VALUES('concurrent',1,'INFO','R80','HOST','replaced','')");committed.set(true);
     }catch(Throwable error){failure.set(error);}});
     try{writer.start();ready.await();Thread.sleep(10);ok(!committed.get(),"independent SQLite writer waits behind cleanup transaction");
      set(db,CURSOR,Integer.toString(ordinal()));DataRetentionGovernorR900.cleanOrphanRevisionIndexCP12(db,TimeSourceR811.monoNowMs()+5000);db.setTransactionSuccessful();
     }finally{db.endTransaction();}
     writer.join(10000);ok(!writer.isAlive()&&failure.get()==null&&committed.get(),"concurrent replacement commits after cleanup writer releases");
    }
    int drainTurns=0;
    while(num(db,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'")>2){
     set(db,CURSOR,Integer.toString(ordinal()));clean(db,TimeSourceR811.monoNowMs()+5000,true);if(++drainTurns>16)throw new AssertionError("orphan drain did not finish");
    }
    ok(num(db,"SELECT COUNT(*) FROM diagnostic_log")==2&&num(db,"SELECT COUNT(*) FROM research_row_revision_v2 r JOIN diagnostic_log d ON d.rowid=r.source_rowid WHERE r.source_table='diagnostic_log'")==2,"concurrent current rows and exactly their generations survive cleanup");
    ok(num(db,"SELECT generation FROM research_row_revision_v2 WHERE source_table='diagnostic_log' AND source_rowid=9000")==liveGeneration,"unrelated live generation remains unchanged through concurrent replacement");
    db.execSQL("DELETE FROM app_state WHERE state_key LIKE 'retention_cp12_revision_after_%'");set(db,CURSOR,"0");
    ok(clean(db,TimeSourceR811.monoNowMs()+5000,true)==0&&metric(db,"complete")==1,"full from-start live-only scan can prove completed revision pass");
   }else if(mode.equals("empty")){
    ok(num(db,"SELECT COUNT(*) FROM research_row_revision_v2")==0,"fixture has genuinely empty global revision index");
    status=DataRetentionGovernorR900.trim(store,150L);
    ok(status.equals("RETENTION_NO_ELIGIBLE_ROWS"),"complete empty proof retains the owner minute cooldown status");
    ok(metric(db,"examined")==0&&metric(db,"complete")==1,"empty global proof avoids table traversal and records completion");
   }else if(mode.equals("fairness")){
    set(db,CURSOR,Integer.toString(ordinal()));set(db,"retention_cp12_table_cursor",Integer.toString(ordinal()));
    long live=num(db,"SELECT COUNT(*) FROM diagnostic_log"),revisions=num(db,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'");
    status=DataRetentionGovernorR900.trim(store,150L);
    ok(status.startsWith("RETENTION_IO_QUEUED:"),"eligible raw archive page queues despite unfinished revision backlog");
    ok(metric(db,"complete")==0,"fairness case really leaves revision sweep unfinished");
    String id=store.getState("retention_cp12_inflight_request","");File request=new File(files,"retention-requests-cp12/"+id+".json");
    RetentionArchiveProtocolCP12.Page page=RetentionArchiveProtocolCP12.Page.parse(ReportJobJournalCP12.readAtomic(request,65536));
    ok(page.table.equals(TABLE)&&page.rows.size()==1,"actual durable request selects the eligible diagnostic row");
    ok(android.content.Context.serviceStarts==1,"actual report dispatcher requests one Android service start");
    ok(new File(files,"r981_reports/requests/"+id+".json").isFile(),"actual report journal request is durable");
    ok(num(db,"SELECT COUNT(*) FROM diagnostic_log")==live,"queueing does not bypass proof or delete live raw rows");
    ok(num(db,"SELECT COUNT(*) FROM research_row_revision_v2 WHERE source_table='diagnostic_log'")<revisions,"revision cleanup and raw-page selection both make progress in one turn");
   }else throw new IllegalArgumentException(mode);
   ok(db.maxPageRows<=64&&db.maxDeleteRows<=64,"all observed query/delete pages remain bounded by 64");
   result.put("status","PASS").put("checks",checks).put("turns",turns).put("lastResult",status).put("revisionLast",state(db,"retention_cp12_revision_last")).put("maxQueryRows",db.maxPageRows).put("maxDeleteRows",db.maxDeleteRows);
  }
  Files.writeString(Path.of(args[4]),result.toString()+"\n");System.out.println(result.toString());
 }
}
