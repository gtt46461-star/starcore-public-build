package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Actual production owners; input payload shapes are copied from the R979 report.
 * Draw labels below are deliberately synthetic. They cannot prove market hit rate. */
public final class BacklogHarnessR980 {
 static int checks,failures;
 static void check(boolean b,String name){checks++;if(!b)failures++;System.out.println((b?"PASS ":"FAIL ")+name);}
 static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
 static void fairness(String bridge)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(bridge);try {
   DbStore store=HostProductR974.store(db);HostProductR974.schema(db,store);
   db.beginTransaction();try {
    for(int i=0;i<22;i++)ResearchTaskQueueR967.enqueue(db,ResearchTaskQueueR967.MAINTENANCE,"lane:"+i,"202609220800","1","synthetic-input","synthetic-authority");
    int[] served=new int[22];
    for(int i=0;i<176;i++) {
     ResearchTaskQueueR967.Lease task=ResearchLearningContinuityR912.claimNext(db,System.currentTimeMillis(),true);
     if(task==null)throw new AssertionError("NO_READY_TASK");served[Integer.parseInt(task.candidate.substring(5))]++;
     ResearchTaskQueueR967.yield(db,task,"","SYNTHETIC_PERSISTENT_BACKLOG",0);
    }
    int min=176;for(int n:served)min=Math.min(min,n);
    check(min>=4,"ALL_22_LANES_RETAIN_ROUND_ROBIN_SERVICE");
    check(served[6]>=22&&served[1]>=22,"CALIBRATION_AND_PREDRAW_LABEL_OWNERS_HAVE_RESERVED_SERVICE");
    check(served[13]>=22&&served[18]>=22,"BOTH_NUMERIC_TRAINING_OWNERS_HAVE_RESERVED_SERVICE");
    System.out.println("MEASURE scheduled_176_turns="+Arrays.toString(served));
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
  }finally{db.shutdown();}
 }
 static void calibration(String bridge,Path fixtures)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(bridge);try {
   DbStore store=HostProductR974.store(db);HostProductR974.schema(db,store);
   db.beginTransaction();try {FixedResearchWindowR932.schema(db);
    ResourceClosureHarnessR980.importTable(db,fixtures.resolve("research_candidate_cal_study_r932.json"));
    ResourceClosureHarnessR980.importTable(db,fixtures.resolve("research_candidate_cal_observation_r932.json"));
    ResourceClosureHarnessR980.importTable(db,fixtures.resolve("research_candidate_cal_model_r932.json"));
    List<String> issues=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT DISTINCT target_issue FROM "+CandidateCalibrationR932.OBS+" ORDER BY target_issue",null)){while(c.moveToNext())issues.add(c.getString(0));}
    // Do not reconstruct real draws from predictions. Explicit fixture labels, every
    // position a legal unique number, only in this disposable host database.
    for(String issue:issues)db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,open_time,fetch_time) VALUES(?,1,2,3,4,5,6,7,8,9,10,0,1,1)",new Object[]{issue});
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   long before=count(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS+" WHERE hit>=0");
   String preserved=scalar(db,"SELECT group_concat(study_hash||':'||target_issue||':'||hit,'|') FROM (SELECT * FROM "+CandidateCalibrationR932.OBS+" WHERE hit>=0 ORDER BY study_hash,target_issue)");
   store.setState("r979_window_phase","0");
   FixedResearchWindowR932.maintainReviewed(store);
   long added=count(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS+" WHERE hit>=0")-before;
   check(added>32,"AVAILABLE_BUDGET_DRAINS_MORE_THAN_OLD_TWO_ISSUE_CEILING");
   check(count(db,"SELECT SUM(settled_n) FROM "+CandidateCalibrationR932.STUDY)==count(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS+" WHERE hit>=0"),"STUDY_COUNTS_EQUAL_ACTUAL_SETTLED_ROWS");
   String after=scalar(db,"SELECT group_concat(study_hash||':'||target_issue||':'||hit,'|') FROM (SELECT * FROM "+CandidateCalibrationR932.OBS+" WHERE hit>=0 ORDER BY study_hash,target_issue)");
   Set<String> labels=new HashSet<>(Arrays.asList(after.split("\\|")));
   check(labels.containsAll(Arrays.asList(preserved.split("\\|"))),"ALL_ORIGINAL_REPORT_LABELS_UNCHANGED");
   check(count(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"LABEL_DRAIN_DOES_NOT_MINT_CERTIFICATE");
   check(count(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==10834,"NO_CAPTURE_ROWS_DELETED_OR_INVENTED");
   System.out.println("MEASURE report_calibration_rows=10834 added_synthetic_settlements="+added+" historical_labels="+before+" scope=SYNTHETIC_DRAWS_REPORT_QUEUE_SHAPE");
  }finally{db.shutdown();}
 }
 static void fullPopulationTraining(String bridge,Path fixtures)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(bridge);try {
   DbStore store=DevelopmentHarnessR978.setup(db);String parent="";
   db.beginTransaction();try {
    ResourceClosureHarnessR980.importTable(db,fixtures.resolve("v57_algorithm_genome_r908.json"));
    ResourceClosureHarnessR980.importTable(db,fixtures.resolve("research_lifecycle_r932.json"));
    for(int i=1;i<=320;i++)DevelopmentHarnessR978.draw(db,i);
    try(Cursor c=db.rawQuery("SELECT genome_id FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE future_n=0 AND state_updates=0 ORDER BY genome_id LIMIT 1",null)){if(c.moveToFirst())parent=c.getString(0);}
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   store.setState("r979_development_focus",parent);store.setState("r979_development_focus_turns","0");
   String key="r959_development_job_"+parent, raw="",status="";int turns=0;boolean rollbackTested=false;
   for(;turns<64;turns++) {
    String before=store.getState(key,"");
    if(!rollbackTested&&!before.isEmpty()&&new JSONObject(before).optInt("n")==224) {
     db.execSQL("CREATE TRIGGER r980_abort_handoff BEFORE UPDATE ON "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHEN OLD.genome_id='"+parent+"' AND NEW.status='PRUNED_RESOURCE_R966' BEGIN SELECT RAISE(ABORT,'R980_INJECTED_RETIRE_FAILURE'); END");
     String failed=ResearchDynamicBootstrapR922.pulse(store,1000).status;
     check(failed.contains("COMMIT_FAILED"),"ATOMIC_HANDOFF_FAILURE_IS_VISIBLE:"+failed);
     check(before.equals(store.getState(key,""))&&ResearchPopulationR966.active(db)==64&&count(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==64,"FAILED_HANDOFF_ROLLS_BACK_CHILD_PARENT_TRIAL_AND_CURSOR");
     db.execSQL("DROP TRIGGER r980_abort_handoff");store.setState("r978_development_retry_at_"+parent,"0");rollbackTested=true;
    }
    status=ResearchDynamicBootstrapR922.pulse(store,1000).status;raw=store.getState(key,"");
    if(!raw.isEmpty()&&new JSONObject(raw).has("output_child"))break;
    if(status.startsWith("DEV_WAIT_POPULATION_CAPACITY"))break;
    if(status.startsWith("ERROR"))throw new AssertionError(status);
   }
   JSONObject job=raw.isEmpty()?new JSONObject():new JSONObject(raw);
   String child=job.optString("output_child","");
   check(!child.isEmpty(),"REAL_VM_TRAINING_PUBLISHES_CHILD_WHEN_64_SLOTS_FULL:"+status);
   check(job.optInt("n")==240,"EXACT_240_HISTORICAL_VM_UPDATES");check(rollbackTested,"LATE_PUBLICATION_ROLLBACK_INJECTION_ACTUALLY_EXECUTED");
   if(!child.isEmpty()) {
    check(ResearchPopulationR966.active(db)==64,"POPULATION_REMAINS_BOUNDED_AFTER_ATOMIC_HANDOFF");
    check(scalar(db,"SELECT status FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+parent+"'").equals("PRUNED_RESOURCE_R966"),"PARENT_FORMULA_ARCHIVED_NOT_DELETED");
    check(count(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+child+"' AND dev_n=240 AND future_n=0 AND state_updates=0")==1,"CHILD_DEV_AND_FUTURE_DENOMINATORS_SEPARATE");
    check(count(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.CHILD+" WHERE child_key='algorithm:"+child+"' AND parent_key='algorithm:"+parent+"'")==1,"REAL_PARENT_CHILD_LINEAGE_PERSISTED");
   }
   check(count(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"HISTORICAL_TRAINING_HAS_NO_PRODUCTION_AUTHORITY");
   System.out.println("MEASURE full_pool_parent="+parent+" child="+child+" pulses="+(turns+1)+" actual_historical_updates="+job.optInt("n"));
  }finally{db.shutdown();}
 }
 public static void main(String[] args)throws Exception {
  android.os.SystemClock.setForTest(1000000,0);Path fixture=Path.of(args[1]);
  if(args[2].equals("backlog")){fairness(args[0]);calibration(args[0],fixture);}
  if(args[2].equals("population"))fullPopulationTraining(args[0],fixture);
  System.out.println("R980_BACKLOG checks="+checks+" failures="+failures+" device=NOT_RUN");
  if(failures>0)throw new AssertionError("failures="+failures);
 }
}
