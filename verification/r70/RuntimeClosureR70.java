package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.lang.reflect.Method;

/** Actual compiled owners and real SQLite. Synthetic fixtures never create live authority. */
public final class RuntimeClosureR70 {
 static int checks;
 static void need(boolean b,String name){if(!b)throw new AssertionError(name);checks++;System.out.println("PASS|"+name);}
 static boolean query(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){return c.moveToFirst();}}
 public static void main(String[] args)throws Exception{
  Path root=Path.of(args[1]);Files.createDirectories(root);
  try(SQLiteDatabase db=new SQLiteDatabase(args[0],root.resolve("queue.db").toString())){
   DbStore store=HostProductR974.store(db);
   db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
   db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue TEXT,snapshot_blob BLOB,snapshot_hash TEXT)");
   db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT,dirty INTEGER)");
   db.execSQL("CREATE TABLE "+GlobalResearchScheduleR932.PLAN+"(target_issue TEXT)");
   db.execSQL("CREATE TABLE "+GlobalResearchScheduleR932.TASK+"(target_issue TEXT,candidate_key TEXT,selected INTEGER,execution_mode TEXT,tier TEXT)");
   db.execSQL("CREATE TABLE "+GlobalResearchScheduleR932.PROXY+"(target_issue TEXT,candidate_key TEXT,action_count INTEGER,record_hash TEXT)");
   db.execSQL("CREATE TABLE "+GlobalResearchScheduleR932.RESERVATION_CP11+"(state TEXT,next_issue TEXT)");
   db.execSQL("CREATE TABLE r952_candidate(candidate_id TEXT,state TEXT)");
   db.execSQL("CREATE TABLE "+ResearchEvaluationRunR967.RUN+"(learner_id TEXT,first_issue TEXT,rule_hash TEXT)");
   db.execSQL("CREATE TABLE r952_candidate_predraw(candidate_id TEXT,target_issue TEXT)");
   ResearchCaptureLedgerR932.schema(db);
   ResearchCaptureQueueR946.verifySqlContract(db);
   String target="202610120661",next="202610120662",hash=V51ChecksumEngine.sha256("R70_SYNTHETIC");
   db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" VALUES(?,?,?)",new Object[]{target,new byte[]{1},hash});
   db.execSQL("INSERT INTO "+GlobalResearchScheduleR932.PLAN+" VALUES(?)",new Object[]{target});
   Method complete=ResearchCaptureQueueR946.class.getDeclaredMethod("completeSql");complete.setAccessible(true);String sql=(String)complete.invoke(null);
   need(query(db,sql,target),"EMPTY_ROSTER_COMPLETE");
   db.execSQL("INSERT INTO "+GlobalResearchScheduleR932.RESERVATION_CP11+" VALUES('RESERVED',?)",new Object[]{target});
   need(!query(db,sql,target),"RESERVED_CALIBRATION_PREVENTS_FALSE_COMPLETION");
   need(target.equals(ResearchCaptureQueueR946.recover(store)),"RESTART_RECOVERS_RESERVED_CAPTURE_WITH_COMPLETED_ROSTER");
   db.execSQL("UPDATE "+GlobalResearchScheduleR932.RESERVATION_CP11+" SET state='ACTIVE'");
   need(!query(db,sql,target),"ACTIVE_CALIBRATION_PREVENTS_FALSE_COMPLETION");
   db.execSQL("UPDATE "+GlobalResearchScheduleR932.RESERVATION_CP11+" SET next_issue=?",new Object[]{next});
   need(query(db,sql,target)&&ResearchCaptureQueueR946.recover(store).isEmpty(),"ADVANCED_RESERVATION_DOES_NOT_DUPLICATE_OLD_CAPTURE");
   String prior="f760443c676bc3a6cbae7a7388d6622a217a6b8af1da80ae5792403df49379fe";
   need(TrainingContinuationR973.compatible(prior),"EXACT_R69_TRAINING_STATE_CONTINUES");
   db.execSQL("INSERT INTO r952_candidate VALUES('old-model','OBSERVING')");
   db.execSQL("INSERT INTO "+ResearchEvaluationRunR967.RUN+" VALUES('old-model','202610120660',?)",new Object[]{prior});
   need(target.equals(ResearchCaptureQueueR946.recover(store)),"UPGRADE_RESTART_RECOVERS_REVIEWED_PREDECESSOR_RUN");
   db.execSQL("UPDATE "+ResearchEvaluationRunR967.RUN+" SET rule_hash=?",new Object[]{hash});
   need(ResearchCaptureQueueR946.recover(store).isEmpty(),"UNKNOWN_RULE_NEVER_MIGRATED");
   db.execSQL("UPDATE "+GlobalResearchScheduleR932.RESERVATION_CP11+" SET next_issue=?",new Object[]{target});
   db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,0)",new Object[]{target});
   need(ResearchCaptureQueueR946.recover(store).isEmpty(),"DRAWN_RESERVATION_NEVER_BACKFILLED_AS_PREDRAW");
   db.execSQL("CREATE TABLE fixture(value INTEGER)");
   try(DatabaseCoordinator.WriterDeadlineScopeCP3 scope=DatabaseCoordinator.withWriterDeadlineCP3(TimeSourceR811.monoNowMs()-1)){
    boolean failed=false;try{store.coordinatorR36().runAtomicWriteOrThrowF01("R36_ANALYSIS_REQUEST",d->{d.execSQL("INSERT INTO fixture VALUES(1)");return true;});}catch(IllegalStateException expected){failed=true;}
    need(failed&&!query(db,"SELECT 1 FROM fixture"),"EXPIRED_WRITE_NEVER_MUTATES_DATABASE");
   }
   need(Boolean.TRUE.equals(store.coordinatorR36().runAtomicWriteOrThrowF01("R36_ANALYSIS_REQUEST",d->{d.execSQL("INSERT INTO fixture VALUES(2)");return true;})),"NEXT_BOUNDED_ATTEMPT_RECOVERS_AFTER_CONTENTION");
   for(String code:new String[]{"CP3_DB_BUSY_DEADLINE","CP3_DB_BUSY_DEADLINE_AFTER_ACQUIRE","CP3_DB_BUSY_LOCK_TIMEOUT"})need(BusinessReconcilerR36.retryableRequestContention(new IllegalStateException("R36_REQUEST_PERSIST_FAILED:WRITE_COORDINATION_FAILED:"+code)),"REQUEST_RETRIES_"+code);
   for(String code:new String[]{"F01_TARGET_DEADLINE_EXPIRED","R36_REQUEST_PERSIST_FAILED:SCHEMA_MISSING","R36_REQUEST_PERSIST_FAILED:HASH_MISMATCH","CP3_DB_BUSY_DEADLINE"})need(!BusinessReconcilerR36.retryableRequestContention(new IllegalStateException(code)),"INTEGRITY_OR_UNSCOPED_ERROR_NOT_HIDDEN_"+code);
   RealtimePressureGateR811.resetForTests();RealtimePressureGateR811.pulseLiveRequest();
   need(RealtimePressureGateR811.isUnderPressure(),"FIXTURE_HAS_POLL_QUIET_PRESSURE");
   StorageReclaimR943.quiet(store);
   need(store.getState("r943_storage_reclaim","").startsWith("RECLAIM_COMPLETE"),"QUIET_POLL_PULSES_NO_LONGER_STARVE_RECLAIM");
   store.setState("r943_storage_reclaim_at","0");RealtimePressureGateR811.beginAnalysis();
   try{StorageReclaimR943.quiet(store);need(store.getState("r943_storage_reclaim_at","").equals("0"),"ACTIVE_ANALYSIS_PREEMPTS_STORAGE_MAINTENANCE");}finally{RealtimePressureGateR811.endAnalysis();}
  }
  java.lang.reflect.Field cacheField=LanePolicyCompiler.class.getDeclaredField("RESULT_CACHE");cacheField.setAccessible(true);
  @SuppressWarnings("unchecked") java.util.Map<String,Object> cache=(java.util.Map<String,Object>)cacheField.get(null);
  cache.clear();
  cache.put("202610120001|fixture",new LanePolicyCompiler.Result());
  cache.put("202610120002|fixture",new LanePolicyCompiler.Result());
  cache.put("202610120003|fixture",new LanePolicyCompiler.Result());
  LanePolicyCompiler.clearIssue("202610120002");
  need(cache.size()==1&&cache.containsKey("202610120003|fixture"),"NEW_TARGET_CLEARS_PAST_AND_CURRENT_CACHE_PRESERVES_FUTURE");
  cache.clear();String issue="202610010001";int maximum=0;
  for(int period=0;period<10000;period++){
   LanePolicyCompiler.clearIssue(issue);
   for(int candidate=0;candidate<40;candidate++)cache.put(issue+"|fixture"+candidate,new LanePolicyCompiler.Result());
   maximum=Math.max(maximum,cache.size());issue=TargetIssueResolver.next(issue).targetIssue;
  }
  need(maximum==40&&cache.size()==40,"TEN_THOUSAND_PERIODS_COMPILED_RESULT_CACHE_PLATEAUS_AT_ONE_40_CANDIDATE_BATCH");
  cache.clear();
  System.out.println("R70_RUNTIME_CLOSURE_PASS|checks="+checks+"|scope=HOST_REAL_SQLITE_SYNTHETIC_NOT_PHONE");
 }
}
