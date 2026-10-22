package com.ethk3.novaonepro;
import android.database.*;import android.database.sqlite.*;import java.lang.reflect.*;import com.ethk3.novaonepro.r952.*;
/** Isolated migration/queue SQL contracts on actual SQLite. No Android startup claim. */
public final class MigrationCaptureHarnessR974 {
 static int checks;static String h=V51ChecksumEngine.sha256("SYNTHETIC_MIGRATION_ONLY");
 static void check(boolean v,String n){if(!v)throw new AssertionError(n);checks++;System.out.println("PASS "+n);}
 static ResearchEvaluationRunR967.Run run(SQLiteDatabase db,String learner,String first,String split,String rule)throws Exception{
  CandidateAstR952 ast=new CandidateAstR952(CandidateAstR952.Operator.POSITION_FREQUENCY,1,20);String initial=CandidateCompilerR952.Model.initial().payload();
  Method origin=ResearchEvaluationRunR967.class.getDeclaredMethod("originHash",String.class,String.class,String.class,String.class,String.class,String.class);origin.setAccessible(true);String epoch=(String)origin.invoke(null,learner,first,ast.structuralHash,rule,initial,"");
  Method hash=ResearchEvaluationRunR967.class.getDeclaredMethod("runHash",String.class,String.class,String.class,String.class,String.class,String.class,String.class,String.class);hash.setAccessible(true);String rh=(String)hash.invoke(null,learner,first,ast.structuralHash,rule,initial,"",split,epoch);
  db.execSQL("INSERT INTO "+ResearchEvaluationRunR967.RUN+" VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{rh,learner,first,ast.structuralHash,rule,initial,0,"",split,epoch,1});
  return ResearchEvaluationRunR967.current(db,learner,first);
 }
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try{DbStore store=HostProductR974.store(db);HostProductR974.schema(db,store);
   ResearchEvaluationRunR967.Run old=run(db,"OLD","202609200201",ResearchEvaluationRunR967.LEGACY_SPLIT,h),next=run(db,"NEW","202609200201",ResearchEvaluationRunR967.SPLIT,TrainingCodeIdentityR964.SHA256);
   check(ResearchEvaluationRunR967.trainable(old,"202609200201"),"OLD_ODD_TRAIN_PRESERVED");check(!ResearchEvaluationRunR967.trainable(old,"202609200202"),"OLD_EVEN_HELDOUT_PRESERVED");
   check(ResearchEvaluationRunR967.trainable(next,"202609200201")&&ResearchEvaluationRunR967.trainable(next,"202609200202"),"NEW_RUN_TRAINS_BOTH_PARITIES");
   check(!ResearchEvaluationRunR967.eligibleEvaluation("R958-F-V2-x","202609200201")&&ResearchEvaluationRunR967.eligibleEvaluation("R958-F-V2-x","202609200202"),"OLD_STATIC_EVAL_DOMAIN_UNCHANGED");
   check(ResearchEvaluationRunR967.eligibleEvaluation("R958-F-V3-x","202609200201")&&ResearchEvaluationRunR967.eligibleEvaluation("R958-F-V3-x","202609200202"),"NEW_FROZEN_EVAL_EVERY_ISSUE");
   check(ResearchEvaluationRunR967.endpoint("R958-F-V2-x","202609201440",2).equals("202609210002"),"OLD_DAY_BOUNDARY_DOMAIN");
   check(ResearchEvaluationRunR967.endpoint("R958-F-V3-x","202609201440",2).equals("202609210001"),"NEW_DAY_BOUNDARY_DOMAIN");
   check(!TrainingContinuationR973.compatible(h),"UNKNOWN_NUMERICAL_RULE_NO_SILENT_CONTINUATION");
   try{db.execSQL("UPDATE "+ResearchEvaluationRunR967.RUN+" SET split_rule=? WHERE learner_id='OLD'",new Object[]{ResearchEvaluationRunR967.SPLIT});throw new AssertionError("OLD_RELABELED");}catch(IllegalStateException expected){check(true,"OLD_SPLIT_IMMUTABLE");}
   db.beginTransaction();try{ResearchMigrationRegistryR952.ensure(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
   check(ResearchEvaluationRunR967.current(db,"OLD","202609200202").hash.equals(old.hash),"IDEMPOTENT_MIGRATION_PRESERVES_OLD_IDENTITY");
   check(DatabaseUtils.longForQuery(db,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.EVAL,null)==0,"NEW_RUN_INHERITS_NO_FROZEN_SAMPLES");
   db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue TEXT PRIMARY KEY,snapshot_blob BLOB,snapshot_hash TEXT)");
   db.execSQL("CREATE TABLE "+GlobalResearchScheduleR932.PLAN+"(target_issue TEXT PRIMARY KEY)");
   db.execSQL("CREATE TABLE "+GlobalResearchScheduleR932.TASK+"(target_issue TEXT,candidate_key TEXT,selected INTEGER,execution_mode TEXT,tier TEXT)");
   db.execSQL("CREATE TABLE IF NOT EXISTS "+GlobalResearchScheduleR932.PROXY+"(target_issue TEXT,candidate_key TEXT,action_count INTEGER,record_hash TEXT)");ResearchCaptureLedgerR932.schema(db);
   ResearchCaptureQueueR946.verifySqlContract(db);check(true,"ACTUAL_COMPLETE_RECOVER_SQL_PREPARES");
   db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" VALUES(?,?,?)",new Object[]{"202609200202",new byte[]{1},h});db.execSQL("INSERT INTO "+GlobalResearchScheduleR932.PLAN+" VALUES('202609200202')");
   check(ResearchCaptureQueueR946.complete(store,"202609200202"),"EMPTY_ELIGIBLE_ROSTER_COMPLETE");
   CandidateAstR952 ast=new CandidateAstR952(CandidateAstR952.Operator.POSITION_FREQUENCY,1,20);
   db.execSQL("INSERT INTO r952_candidate VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{"NEW",h,ast.canonical,ast.structuralHash,"","NURSERY",0,"202609200201",0,1});
   check(ResearchDirectorR952.hasPendingCapture(store,"202609200202"),"UNSEEN_NUMERIC_CANDIDATE_DETECTED");check(!ResearchCaptureQueueR946.complete(store,"202609200202"),"NUMERIC_CAPTURE_CANNOT_BE_PREMATURELY_COMPLETE");
   check(ResearchCaptureQueueR946.recover(store).equals("202609200202"),"RESTART_RECOVERS_NUMERIC_CAPTURE_WITH_COMPLETED_OLD_ROSTER");
   ExactRealHarnessR974.draw(db,202);check(ResearchCaptureQueueR946.recover(store).isEmpty(),"DRAWN_ISSUE_NO_CAPTURE_BACKFILL");
   check(DatabaseUtils.longForQuery(db,"SELECT COUNT(*) FROM r974_computation_receipt",null)==0,"NO_FAKE_COMPUTATION_ROWS_FROM_MIGRATION");
   System.out.println("R974_MIGRATION_CAPTURE_PASS|checks="+checks+"|actual_SQL=true|Android_startup=NOT_RUN");
  }finally{db.shutdown();}
 }
}
