package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

/** Scheduling-index tests, explicitly NOT assertions that all lane owners trained a model. */
public final class MaintenanceContinuityHarnessR967 {
 static int checks;interface Op{void run();}
 static void check(boolean yes,String name){if(!yes)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
 static void tx(SQLiteDatabase d,Op op){d.beginTransaction();try{op.run();d.setTransactionSuccessful();}finally{d.endTransaction();}}
 static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 public static void main(String[] args)throws Exception {
  Path path=Files.createTempFile("r967-queue-restart-",".db");
  final SQLiteDatabase d=new SQLiteDatabase(args[0],path.toString());
  final ResearchTaskQueueR967.Lease[] current={null};
  try {
   d.execSQL("CREATE TABLE "+DbHelper.TABLE_APP_STATE+"(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
   tx(d,()->ResearchLearningContinuityR912.ensureDesired(d,"202609200001",false,false));
   check(n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE)==22,"ONE_ACTIVE_REVIEW_PER_LANE");
   tx(d,()->ResearchLearningContinuityR912.ensureDesired(d,"202609200002",false,false));
   check(n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE)==22,"NEW_DRAW_COALESCES_WITHOUT_DISCARDING_OLD_TASK");
   check(n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE target_issue='202609200001'")==22,"OLD_WATERMARK_AND_TASK_IDENTITY_RETAINED");
   check(n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.LANE+" WHERE desired_issue='202609200002'")==22,"DESIRED_WATERMARK_ADVANCES_DURABLY");
   // Under a real training backlog, every second ready turn belongs to lane 13, while normal
   // round-robin state advances only on its own turns. This is scheduling evidence only.
   tx(d,()->current[0]=ResearchLearningContinuityR912.claimNext(d,System.currentTimeMillis(),true));
   check(current[0]!=null&&current[0].candidate.equals("lane:0"),"TRAINING_PRIORITY_FIRST_TURN_PRESERVES_ROUND_ROBIN");
   tx(d,()->ResearchTaskQueueR967.yield(d,current[0],current[0].cursor,"TEST",0));
   tx(d,()->current[0]=ResearchLearningContinuityR912.claimNext(d,System.currentTimeMillis(),true));
   check(current[0]!=null&&current[0].candidate.equals("lane:13"),"TRAINING_PRIORITY_SECOND_TURN_RESERVES_R952");
   tx(d,()->ResearchTaskQueueR967.yield(d,current[0],current[0].cursor,"TEST",0));
   tx(d,()->current[0]=ResearchLearningContinuityR912.claimNext(d,System.currentTimeMillis(),true));
   check(current[0]!=null&&current[0].candidate.equals("lane:1"),"TRAINING_PRIORITY_DOES_NOT_ADVANCE_NORMAL_CURSOR");
   tx(d,()->ResearchTaskQueueR967.yield(d,current[0],current[0].cursor,"TEST",0));
   tx(d,()->{d.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES('r962_research_next_lane','0',0)");d.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES('r971_maintenance_turn','0',0)");});
   HashSet<String> served=new HashSet<String>();
   for(int i=0;i<22;i++) {
    tx(d,()->current[0]=ResearchLearningContinuityR912.claimNext(d,System.currentTimeMillis()));
    check(current[0]!=null&&served.add(current[0].candidate),"FAIR_FIRST_TURN_"+i);
    tx(d,()->ResearchTaskQueueR967.yield(d,current[0],current[0].cursor,"RESOURCE_YIELD",0));
   }
   check(served.size()==22,"REPEATED_YIELDS_CANNOT_STARVE_OTHER_LANES");
   check(!ResearchLearningContinuityR912.terminalReviewOutcome("R952_PENDING|advanced=7"),"R952_BACKLOG_RECEIPT_CANNOT_MARK_LANE_SERVED");
   check(ResearchLearningContinuityR912.terminalReviewOutcome("R952_DRAINED|advanced=7"),"R952_DRAINED_RECEIPT_MAY_COMPLETE_LANE");
   d.execSQL("CREATE TABLE r952_research_outbox(state TEXT NOT NULL)");
   d.execSQL("INSERT INTO r952_research_outbox VALUES('RETRYABLE_FAILED')");
   check(ResearchLearningContinuityR912.hasUnfinishedR952(d),"COOLDOWN_TRAINING_BACKLOG_REMAINS_PENDING");
   d.execSQL("UPDATE r952_research_outbox SET state='COMPLETED'");
   tx(d,()->ResearchTaskQueueR967.enqueue(d,ResearchTaskQueueR967.POST_TRAIN,"fixture","202609200001","1","input","authority"));
   check(ResearchLearningContinuityR912.hasUnfinishedR952(d),"POST_TRAIN_BACKLOG_REMAINS_PENDING");
   d.execSQL("UPDATE "+ResearchTaskQueueR967.TABLE+" SET status='FAILED_TERMINAL' WHERE kind='POST_TRAIN'");
   check(!ResearchLearningContinuityR912.hasUnfinishedR952(d),"TERMINAL_R952_WORK_DOES_NOT_BUSY_LOOP");
   tx(d,()->current[0]=ResearchLearningContinuityR912.claimNext(d,System.currentTimeMillis()));
   check(current[0]!=null&&current[0].candidate.equals("lane:0"),"ROUND_ROBIN_CURSOR_SURVIVES_FULL_TURN");
   // Simulate process termination while a persisted lease is RUNNING.
  }finally{d.shutdown();}
  Thread.sleep(30);
  final SQLiteDatabase reopened=new SQLiteDatabase(args[0],path.toString());
  try {
   final ResearchTaskQueueR967.Lease[] recovered={null};
   tx(reopened,()->recovered[0]=ResearchTaskQueueR967.claimFor(reopened,ResearchTaskQueueR967.MAINTENANCE,"lane:0",current[0].leaseUntil+1));
   check(recovered[0]!=null&&recovered[0].id.equals(current[0].id),"PROCESS_RESTART_RECOVERS_SAME_DURABLE_TASK");
   tx(reopened,()->check(!ResearchTaskQueueR967.owns(reopened,current[0]),"PRE_RESTART_LEASE_IS_FENCED"));
   tx(reopened,()->ResearchLearningContinuityR912.reviewCommitted(reopened,recovered[0],"REVIEWED_NOT_NUMERIC_EVIDENCE"));
   check(n(reopened,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.LANE+" WHERE lane=0 AND served_issue='202609200001'")==1,"REVIEW_ACK_ADVANCES_ONLY_ITS_ORIGINAL_WATERMARK");
   tx(reopened,()->ResearchLearningContinuityR912.ensureDesired(reopened,"202609200002",false,false));
   check(n(reopened,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE kind='MAINTENANCE' AND candidate_id='lane:0' AND target_issue='202609200002' AND status='PENDING'")==1,"OLDER_REVIEW_COMPLETION_SCHEDULES_NEW_WATERMARK");
   check(n(reopened,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL')")==22,"ACTIVE_MAINTENANCE_QUEUE_STAYS_BOUNDED_AT_22");
   check(n(reopened,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.LANE+" WHERE served_issue='202609200002'")==0,"NO_FALSE_CURRENT_PERIOD_COMPLETION");
   System.out.println("MAINTENANCE_CONTINUITY_PASS checks="+checks+"; restart=persistent_host_SQLite; Android_lifecycle=NOT_RUN; lane_numeric_execution=NOT_TESTED_HERE");
  }finally{reopened.shutdown();Thread.sleep(30);Files.deleteIfExists(path);}
 }
}
