package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.util.*;import org.json.*;
/** Actual product DAO/schema/VM and SQLite; only Android startup and clock are adapted. */
public final class DevelopmentHarnessR978 {
 static int n,fail;static void check(boolean b,String s){n++;if(!b)fail++;System.out.println((b?"PASS ":"FAIL ")+s);}
 static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static DbStore setup(SQLiteDatabase db)throws Exception{
  DbStore s=HostProductR974.store(db);
  db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
  DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);RuntimeEventLedgerR926.ensureSchema(db);
  db.execSQL("CREATE TABLE "+DbHelper.TABLE_OOS_ACTION_LOG+"(id INTEGER PRIMARY KEY)");DbHelper.ensureR811CriticalSchema(db);DbHelper.ensureR906CriticalSchema(db);V51Repository.ensure(db);
  db.execSQL("CREATE TABLE draws(_id INTEGER PRIMARY KEY,issue TEXT UNIQUE,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER,fetch_time INTEGER,source_line TEXT,raw_json TEXT,dirty_reason TEXT)");
  db.execSQL("CREATE TABLE miss_reason_sample(target_issue TEXT,created_at INTEGER,position INTEGER,actual_number INTEGER,window_wrong_score REAL,position_wrong_score REAL,main_number_wrong_score REAL,third_drag_score REAL,counter_ignored_score REAL)");
  RealtimePressureGateR811.resetForTests();
  check(ResearchSchemaCoordinatorR926.ensureToReady(s.coordinatorR36(),8),"REAL_SCHEMA_READY");return s;
 }
 static String draw(SQLiteDatabase db,int k){String issue=String.format(java.util.Locale.ROOT,"202609210%03d",k);List<Integer>a=new ArrayList<>();for(int i=1;i<=10;i++)a.add(i);Collections.shuffle(a,new Random(978+k));Object[] x=new Object[14];x[0]=issue;for(int i=0;i<10;i++)x[i+1]=a.get(i);x[11]=0;x[12]=1;x[13]=1;db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,open_time,fetch_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",x);return issue;}
 static String seed(SQLiteDatabase db){AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(978L,1,"");AlgorithmInventionLabR908.registerTrained(db,g,"202609210020","202609210021",0,0,0,0,"SYNTHETIC_UNTRAINED_FIXTURE");return g.genomeId;}
 static void eligibilityAndPriority(String bridge)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(bridge);try{
   DbStore s=setup(db);draw(db,120);db.beginTransaction();String id;try{id=seed(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
   check(count(db,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT+" WHERE child_key='algorithm:"+id+"'")==0,"LEGACY_FIXTURE_HAS_NO_CREATED_RECEIPT");
   check(ResearchDynamicBootstrapR922.hasUnservedTraining(s),"IMPORTED_ALGORITHM_ENTER_HISTORICAL_TRAINING");
   s.setState("r960_development_advanced_"+id,"202609210120");check(ResearchDynamicBootstrapR922.hasUnservedTraining(s),"PARTIAL_SAME_ISSUE_IS_STILL_RUNNABLE");
   s.setState("r959_development_done_"+id,"DONE");check(!ResearchDynamicBootstrapR922.hasUnservedTraining(s),"DONE_NOT_REPEATED");s.setState("r959_development_done_"+id,"");
   s.setState("r978_development_retry_at_"+id,Long.toString(System.currentTimeMillis()+60000));check(!ResearchDynamicBootstrapR922.hasUnservedTraining(s),"INPUT_BACKOFF_DOES_NOT_SPIN");s.setState("r978_development_retry_at_"+id,"0");
   db.beginTransaction();try{
    ResearchTaskQueueR967.schema(db);for(int lane:new int[]{0,13,18})ResearchTaskQueueR967.enqueue(db,ResearchTaskQueueR967.MAINTENANCE,"lane:"+lane,"202609210120","1","fixture-input","fixture-owner");
    db.execSQL("INSERT OR REPLACE INTO app_state VALUES('r971_maintenance_turn','1',1)");
    ResearchTaskQueueR967.Lease first=ResearchLearningContinuityR912.claimNext(db,System.currentTimeMillis(),true);check(first!=null&&first.candidate.equals("lane:13"),"PRIORITY_ONLINE_FIRST");ResearchTaskQueueR967.yield(db,first,"","fixture",0);
    db.execSQL("INSERT OR REPLACE INTO app_state VALUES('r971_maintenance_turn','1',1)");
    ResearchTaskQueueR967.Lease second=ResearchLearningContinuityR912.claimNext(db,System.currentTimeMillis(),true);check(second!=null&&second.candidate.equals("lane:18"),"PRIORITY_HISTORICAL_NEXT");
    ResearchTaskQueueR967.Lease ordinary=ResearchLearningContinuityR912.claimNext(db,System.currentTimeMillis(),true);check(ordinary!=null&&ordinary.candidate.equals("lane:0"),"WORLD_ROUND_ROBIN_NOT_STARVED");db.setTransactionSuccessful();
   }finally{db.endTransaction();}
  }finally{db.shutdown();}
 }
 static void train(String bridge)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(bridge);try{
   DbStore s=setup(db);db.beginTransaction();String id;try{for(int i=1;i<=320;i++)draw(db,i);id=seed(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
   String key="r959_development_job_"+id,first=ResearchDynamicBootstrapR922.pulse(s,1000).status;String raw=s.getState(key,"");
   check(!raw.isEmpty(),"IMPORT_ACTUALLY_REGISTERS_AND_TRAINS:"+first);if(raw.isEmpty())return;
   JSONObject job=new JSONObject(raw);check(job.getString("birth").equals("202609210320"),"NEW_TRIAL_USES_CURRENT_HISTORY_NOT_CREATION_DATE");
   check(job.getInt("n")>0&&job.getInt("n")<240,"BOUNDED_PARTIAL_NUMERIC_COMMIT");
   int initial=job.getInt("n");String originalDataset=job.getString("dataset");
   // Checkpoint storage error must preserve ALL prior model/cursor/trial rows.
   db.execSQL("CREATE TRIGGER fail_dev BEFORE INSERT ON app_state WHEN NEW.state_key='"+key+"' BEGIN SELECT RAISE(ABORT,'R978_INJECTED_CHECKPOINT_FAILURE'); END");
   String failure=ResearchDynamicBootstrapR922.pulse(s,1000).status;check(failure.contains("COMMIT_FAILED"),"CHECKPOINT_FAILURE_VISIBLE:"+failure);check(raw.equals(s.getState(key,"")),"CHECKPOINT_FAILURE_ROLLS_BACK_MODEL_AND_CURSOR");db.execSQL("DROP TRIGGER fail_dev");s.setState("r978_development_retry_at_"+id,"0");
   draw(db,321);String end="";int calls=0;
   for(;calls<100;calls++){s=HostProductR974.store(db);end=ResearchDynamicBootstrapR922.pulse(s,1000).status;if(end.startsWith("DEV_COMPLETED"))break;if(end.startsWith("ERROR"))throw new AssertionError(end);}
   check(calls<100,"TRAINING_COMPLETES_WITH_OWNER_RECREATION:"+end);job=new JSONObject(s.getState(key,""));
   check(job.getInt("n")==240&&job.getInt("cursor")==320,"240_REAL_VM_LABEL_UPDATES");check(job.getString("dataset").equals(originalDataset)&&job.getString("birth").equals("202609210320"),"LATER_DRAW_NOT_INSERTED_IN_FROZEN_TRAINING_SET");
   String child=job.optString("output_child","");try(Cursor c=db.rawQuery("SELECT dev_n,future_n,state_updates,earliest_use_issue,genome_spec FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{child})){
    check(c.moveToFirst(),"TRAINED_CHILD_PUBLISHED");if(!child.isEmpty()){check(c.getInt(0)==240&&c.getInt(1)==0&&c.getInt(2)==0,"DEVELOPMENT_NOT_FUTURE_EVIDENCE");check(c.getString(3).equals("202609210322"),"PROSPECTIVE_CHILD_USE_BOUNDARY");AlgorithmGenomeR908 g=AlgorithmGenomeR908.parse(c.getString(4));check(!g.initialParameters.isEmpty()&&g.generation==2,"PARAMETERS_AND_GENERATION_PERSISTED");}
   }
   check(count(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW)==0&&count(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"NO_FAKE_PREDRAW_OR_CERTIFICATE");
   check(!ResearchDynamicBootstrapR922.hasUnservedTraining(s),"TRAINED_PARENT_AND_CHILD_NOT_TRAINED_TWICE");System.out.println("MEASURE devUpdates=240 pulsesAfterFirst="+calls+" sourceOwnerRecreated=true");
  }finally{db.shutdown();}
 }

 static void scheduler(String bridge)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(bridge);try{
   DbStore s=setup(db);db.beginTransaction();String id;try{for(int i=1;i<=320;i++)draw(db,i);id=seed(db);ResearchTaskQueueR967.schema(db);
    // All other maintenance was reviewed earlier this SAME issue. No direct lane call.
    for(int lane=0;lane<22;lane++)db.execSQL("INSERT INTO "+ResearchTaskQueueR967.LANE+"(lane,desired_issue,served_issue,updated_at) VALUES(?,'202609210320','202609210320',1)",new Object[]{lane});db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   String key="r959_development_job_"+id;int pulses=0;boolean pendingSeen=false;long start=System.nanoTime();
   for(;pulses<35;pulses++){
    ResearchLearningContinuityR912.Result result=ResearchLearningContinuityR912.pulse(s,1000);
    String text=result.status;if(text.contains("DEV_ROUND_PENDING")) {pendingSeen=true;check(text.startsWith("PERIOD_PENDING"),"ACTIVE_REOPENED_LANE_IS_NOT_PERIOD_COMPLETE");}
    String job=s.getState(key,"");if(!job.isEmpty()&&new JSONObject(job).optString("output_child","").length()>0)break;
    Thread.sleep(260L); // Real retry expiration; do not edit queue status or call the lane directly.
   }
   check(pendingSeen,"SCHEDULER_DISCOVERS_UNFINISHED_SAME_DRAW_JOB");
   check(pulses<35,"SCHEDULER_FINISHES_DEVELOPMENT_WITHOUT_NEW_DRAW_OR_DIRECT_WORKER_CALL");
   String raw=s.getState(key,"");check(!raw.isEmpty()&&new JSONObject(raw).getInt("n")==240,"AUTONOMOUS_OWNER_240_NUMERIC_UPDATES");
   check(!ResearchDynamicBootstrapR922.hasUnservedTraining(s),"AUTONOMOUS_OWNER_DRAINS_JOB");
   check(count(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"AUTONOMOUS_TRAINING_DOES_NOT_ISSUE_CERTIFICATE");
   System.out.println("MEASURE scheduler_pulses="+(pulses+1)+" real_elapsed_ms="+(System.nanoTime()-start)/1000000+" new_draws=0 manual_lane_invocations=0");
  }finally{db.shutdown();}
 }
 public static void main(String[] args)throws Exception{android.os.SystemClock.setForTest(1000000L,0L);eligibilityAndPriority(args[0]);if(args.length>1&&args[1].equals("train"))train(args[0]);if(args.length>1&&args[1].equals("scheduler"))scheduler(args[0]);System.out.println("R978_DEVELOPMENT checks="+n+" failures="+fail+" REAL_PRODUCT_VM_SQLITE_SYNTHETIC_INPUT_NOT_DEVICE");if(fail>0)throw new AssertionError("failures="+fail);}
}
