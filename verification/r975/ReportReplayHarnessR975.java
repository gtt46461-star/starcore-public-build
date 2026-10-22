package com.ethk3.novaonepro;
import android.database.*;import android.database.sqlite.*;import com.ethk3.novaonepro.r952.*;
import java.nio.file.*;import java.io.*;import java.util.*;import org.json.*;
/** Replays captured user report rows through real product code. Does NOT reconstruct a
 * complete phone backup or claim real-world future outcomes for the new build. */
public final class ReportReplayHarnessR975 {
 static int checks;
 static void check(boolean b,String n){if(!b)throw new AssertionError(n);checks++;System.out.println("PASS "+n);}
 static long n(SQLiteDatabase d,String sql){return DatabaseUtils.longForQuery(d,sql,null);}
 static String text(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
 static JSONObject read(String p)throws Exception{return new JSONObject(Files.readString(Path.of(p)));}
 static void insert(SQLiteDatabase d,String table,JSONObject row)throws Exception{
  ArrayList<String> keys=new ArrayList<>();for(Iterator<String> it=row.keys();it.hasNext();)keys.add(it.next());Collections.sort(keys);
  Object[] values=new Object[keys.size()];StringBuilder cols=new StringBuilder(),qs=new StringBuilder();
  for(int i=0;i<keys.size();i++){String k=keys.get(i);if(!k.matches("[A-Za-z0-9_]+"))throw new IllegalArgumentException(k);if(i>0){cols.append(',');qs.append(',');}cols.append(k);qs.append('?');values[i]=row.isNull(k)?null:row.get(k);}
  d.execSQL("INSERT INTO "+table+"("+cols+") VALUES("+qs+")",values);
 }
 static void tx(SQLiteDatabase d,Runnable r){d.beginTransaction();try{r.run();d.setTransactionSuccessful();}finally{d.endTransaction();}}
 public static void main(String[] args)throws Exception{
  String bridge=args[0],fixtures=args[1];
  SQLiteDatabase db=new SQLiteDatabase(bridge);try{DbStore s=HostProductR974.store(db);HostProductR974.schema(db,s);tx(db,()->FixedResearchWindowR932.schema(SQLiteOpenHelper.host));
   JSONObject f=read(fixtures+"/report_ready_calibration.json");insert(db,CandidateCalibrationR932.STUDY,f.getJSONObject("study"));JSONArray obs=f.getJSONArray("observations");
   db.beginTransaction();try{for(int i=0;i<obs.length();i++)insert(db,CandidateCalibrationR932.OBS,obs.getJSONObject(i));db.setTransactionSuccessful();}finally{db.endTransaction();}
   check(obs.length()==241,"ACTUAL_REPORT_FIXED_241_OBSERVATIONS_IMPORTED");CandidateCalibrationR932.fitOne(s);
   check(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)==1,"ACTUAL_REPORT_READY_STUDY_FITS_AND_PUBLISHES");
   JSONObject model=new JSONObject(text(db,"SELECT model_json FROM "+CandidateCalibrationR932.MODEL));
   check(model.getString("study_hash").equals(f.getJSONObject("study").getString("study_hash")),"ACTUAL_STUDY_IDENTITY_RETAINED");
   System.out.println("REPORT_CALIBRATION_RESULT="+model.toString());
   check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"CAL_FIT_DOES_NOT_MANUFACTURE_CERTIFICATE");
   String h=V51ChecksumEngine.sha256("INVALID_READY_FIXED_SUPPORT");db.execSQL("INSERT INTO "+CandidateCalibrationR932.STUDY+" VALUES(?, 'algorithm:invalid',241,241,'READY','202609200001')",new Object[]{h});
   CandidateCalibrationR932.fitOne(s);check(text(db,"SELECT state FROM "+CandidateCalibrationR932.STUDY+" WHERE study_hash='"+h+"'").equals("BLOCKED_INVALID_INPUT"),"MALFORMED_READY_STUDY_ISOLATED_NOT_ENDLESS_HEAD_OF_LINE");
   check(n(db,"SELECT COUNT(*) FROM r975_calibration_failure")==1,"INVALID_STUDY_HAS_DURABLE_REASON");
   long before=n(db,"SELECT total_changes()");StringWriter w=new StringWriter();RuntimeChainDiagnosisR975.write(db,w);check(n(db,"SELECT total_changes()")==before,"DIAGNOSTIC_REPORT_STRICTLY_READ_ONLY");
   check(w.toString().contains("UNKNOWN")&&w.toString().contains("NOT_INSTRUMENTED")==false,"MISSING_RUNTIME_TABLE_IS_UNKNOWN_NOT_FAKE_PASS");Files.writeString(Path.of(args[2],"report_replay_diagnosis.txt"),w.toString());
  }finally{db.shutdown();}
  Path persistent=Path.of(args[2],"evolution_restart.sqlite");Files.deleteIfExists(persistent);db=new SQLiteDatabase(bridge,persistent.toString());
  try{DbStore s=HostProductR974.store(db);HostProductR974.schema(db,s);
   db.execSQL("DROP TABLE "+ResearchLifecycleR932.TABLE);tx(db,()->ResearchLifecycleR932.schema(SQLiteOpenHelper.host));
   JSONObject f=read(fixtures+"/report_evolution_inputs.json");JSONObject tables=f.getJSONObject("tables");
   db.beginTransaction();try{for(Iterator<String> it=tables.keys();it.hasNext();){String t=it.next();JSONArray rows=tables.getJSONArray(t);for(int i=0;i<rows.length();i++)insert(db,t,rows.getJSONObject(i));}db.setTransactionSuccessful();}finally{db.endTransaction();}
   String issue=f.getString("terminal_issue");db.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT PRIMARY KEY,status TEXT,settlement_hash TEXT,completed_at INTEGER)");db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED',?,1)",new Object[]{issue,V51ChecksumEngine.sha256("SIMULATED_AUTHORITY_FOR_REPORT_REPLAY")});
   System.out.println("AUTHORITY_SCOPE=SIMULATED_TERMINAL_FOR_REPORT_REPLAY_NOT_PHONE_BACKUP");
   String first=EvolutionControllerR952.evolveOne(s,issue);System.out.println(first);
   check(n(db,"SELECT MAX(verified_n) FROM r975_evolution_review")==8,"EVOLUTION_VERIFIES_EXACTLY_BOUNDED_FIRST_PAGE");
   db.execSQL("CREATE TRIGGER test_review_crash BEFORE UPDATE ON r975_evolution_review WHEN NEW.verified_n=16 BEGIN SELECT RAISE(ABORT,'SIMULATED_REVIEW_CRASH'); END");
   try{EvolutionControllerR952.evolveOne(s,issue);throw new AssertionError("REVIEW_CRASH_NOT_TRIGGERED");}catch(IllegalStateException expected){check(n(db,"SELECT MAX(verified_n) FROM r975_evolution_review")==8,"PAGE_CRASH_ROLLS_BACK_PROGRESS");}
   db.execSQL("DROP TRIGGER test_review_crash");db.shutdown();db=new SQLiteDatabase(bridge,persistent.toString());s=HostProductR974.store(db);
   long before=n(db,"SELECT COUNT(*) FROM r952_candidate");
   for(int i=0;i<95;i++){String status=EvolutionControllerR952.evolveOne(s,issue);System.out.println("pulse="+i+"|"+status);}
   check(n(db,"SELECT COUNT(*) FROM r952_candidate")>before,"REPORT_ERRORS_GENERATE_REAL_NEW_FORMULAS_AFTER_RESTART");
   check(n(db,"SELECT COUNT(*) FROM r975_evolution_review WHERE state='CONSUMED'")>0,"REVIEW_CONSUMED_ONLY_WITH_ACTUAL_CHILD");
   check(n(db,"SELECT COUNT(*) FROM r952_candidate WHERE created_issue='"+issue+"' AND (future_n<>0 OR state<>'NURSERY')")==0,"NEW_CHILDREN_DO_NOT_INHERIT_SAMPLES_OR_STATES");
   check(n(db,"SELECT COUNT(*) FROM r974_model_lineage WHERE parent_role='FUSION_SECONDARY'")>0,"REAL_TWO_PARENT_FUSION_LINEAGE_CREATED");
   check(n(db,"SELECT COUNT(*) FROM r952_artifact a JOIN r952_candidate c ON c.candidate_id=a.candidate_id WHERE c.created_issue='"+issue+"'")==0,"NO_FAKE_CHILD_TRAINING_ARTIFACT");
   check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0&&n(db,"SELECT COUNT(*) FROM trades")==0,"ERROR_RESEARCH_DOES_NOT_CREATE_CERTIFICATE_OR_REAL");
   // NEW synthetic next-issue history/authority only. Original report labels are never
   // reinterpreted as these new samples. Actual six children must enter capture and training.
   for(int i=1200;i<=1440;i++){String t=String.format(Locale.US,"20260921%04d",i);db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(?,1,2,3,4,5,6,7,8,9,10,0)",new Object[]{t});}
   for(int i=1;i<=7;i++)db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(?,1,2,3,4,5,6,7,8,9,10,0)",new Object[]{String.format(Locale.US,"20260922%04d",i)});
   tx(db,()->FixedResearchWindowR932.schema(SQLiteOpenHelper.host));
   String next="202609220008";long oldRuns=n(db,"SELECT COUNT(*) FROM r967_numeric_training_run WHERE rule_hash='02da605b2a25ddfd7dfcf30ddcd76c0bad8fdb0519d7eff55970ccf401a56a9f'");
   int captured=ResearchDirectorR952.capturePredraw(s,next,V51ChecksumEngine.sha256("NEW_SYNTHETIC_NEXT_ISSUE"),16);
   check(captured==12,"ALL_SIX_PARENTS_AND_SIX_NEW_CHILDREN_CAPTURE_NEXT_ISSUE");
   check(n(db,"SELECT COUNT(*) FROM r967_numeric_training_run WHERE learner_id IN (SELECT candidate_id FROM r952_candidate WHERE created_issue='"+issue+"') AND initial_updates=0")==6,"CHILD_TRAINING_RUNS_START_AT_ZERO");
   if(TrainingContinuationR973.compatible("02da605b2a25ddfd7dfcf30ddcd76c0bad8fdb0519d7eff55970ccf401a56a9f")){
    check(n(db,"SELECT COUNT(*) FROM r967_numeric_training_run WHERE rule_hash='02da605b2a25ddfd7dfcf30ddcd76c0bad8fdb0519d7eff55970ccf401a56a9f'")==oldRuns,"EXACT_REVIEWED_R974_RUNS_NOT_RESET");
    check(n(db,"SELECT COUNT(*) FROM r967_numeric_training_run WHERE first_issue='"+next+"' AND learner_id NOT IN (SELECT candidate_id FROM r952_candidate WHERE created_issue='"+issue+"')")==0,"NO_REPLACEMENT_RUN_FOR_COMPATIBLE_PARENTS");
   }
   Models.Draw draw=new Models.Draw();draw.issue=next;for(int j=0;j<10;j++)draw.numbers[j]=j+1;draw.openTime=System.currentTimeMillis()+1000;long complete=draw.openTime+1000;String sh=V51ChecksumEngine.sha256("NEW_SYNTHETIC_SETTLEMENT");
   db.execSQL("INSERT INTO draws(issue,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(?,?,1,2,3,4,5,6,7,8,9,10,0)",new Object[]{next,draw.openTime});db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED',?,?)",new Object[]{next,sh,complete});
   check(ResearchDirectorR952.onSettlementCompleted(s,draw,sh,complete)==12,"AUTHORITATIVE_SETTLEMENT_LABELS_ALL_CAPTURED_MODELS");
   int commits=0;for(int i=0;i<24;i++){if(ResearchOutboxR952.drainOne(s))commits++;}
   check(n(db,"SELECT COUNT(*) FROM r952_artifact a JOIN r952_candidate c ON c.candidate_id=a.candidate_id WHERE c.created_issue='"+issue+"' AND a.target_issue='"+next+"' AND a.numeric_updates=1")==6,"ALL_NEW_CHILDREN_ACTUALLY_TRAIN_PARAMETERS_ONCE");
   check(ResearchDirectorR952.onSettlementCompleted(s,draw,sh,complete)==0,"DUPLICATE_SETTLEMENT_DOES_NOT_RETRAIN_OR_INCREMENT");
   check(n(db,"SELECT COUNT(*) FROM r952_candidate WHERE created_issue='"+issue+"' AND future_n=1")==6,"CHILDREN_HAVE_ONE_NEW_SAMPLE_NOT_PARENT_COUNTS");
   System.out.println("NEW_SYNTHETIC_TRAINING_COMMITS="+commits+"|report_replay_is_not_live_performance");
   StringWriter w=new StringWriter();RuntimeChainDiagnosisR975.write(db,w);Files.writeString(Path.of(args[2],"evolution_replay_diagnosis.txt"),w.toString());
  }finally{db.shutdown();}
  System.out.println("REPORT_REPLAY_PASS|checks="+checks+"|live_future=NOT_TESTED|phone_database_migration=NOT_TESTED");
 }
}
