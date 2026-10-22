package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import java.lang.reflect.*;
import org.json.*;
/** Synthetic earlier-build provenance; real numeric state, trial/checkpoint owner and SQL. */
public final class TrainingUpgradeHarnessR964 {
 static int checks;static void check(boolean x,String n){if(!x)throw new AssertionError(n);checks++;System.out.println("PASS "+n);}
 static ExperimentProtocolR929.TrialIdentity identity(JSONObject job)throws Exception{
  Method m=ResearchDynamicBootstrapR922.class.getDeclaredMethod("identity",JSONObject.class);m.setAccessible(true);return (ExperimentProtocolR929.TrialIdentity)m.invoke(null,job);
 }
 static void provenance(SQLiteDatabase db,DbStore store,String key,JSONObject job,String source,String code)throws Exception{
  String old=identity(job).trialId;job.put("source",source);job.put("training_code",code);
  ExperimentProtocolR929.TrialRecord record=new ExperimentProtocolR929.TrialRecord(identity(job),ExperimentProtocolR929.TrialStatus.RUNNING,job.getLong("registered_at"),job.getLong("registered_at"),0,"","",false,"");
  db.beginTransaction();try{db.delete(ResearchPlatformStoreR929.TABLE_TRIAL,"trial_id=?",new String[]{old});db.insertWithOnConflict(ResearchPlatformStoreR929.TABLE_TRIAL,null,ResearchPlatformStoreR929.trialValues(record),SQLiteDatabase.CONFLICT_ABORT);store.setState(key,job.toString());db.setTransactionSuccessful();}finally{db.endTransaction();}
 }
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
   DbStore store=DevelopmentTrainingHarnessR959.init(db);String latest="202609190001";Random random=new Random(964);
   for(int i=0;i<121;i++){DevelopmentTrainingHarnessR959.draw(db,latest,random);if(i<120)latest=TargetIssueResolver.next(latest).targetIssue;}
   DirectedResearchCreationR932.maintain(store,"algorithm");ResearchDynamicBootstrapR922.pulse(store,1000);ResearchDynamicBootstrapR922.pulse(store,1000);
   String id="";try(Cursor c=db.rawQuery("SELECT genome_id FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME,null)){c.moveToFirst();id=c.getString(0);}
   String key="r959_development_job_"+id;JSONObject job=new JSONObject(store.getState(key,""));int before=job.getInt("n");
   check(before>0,"REAL_PARTIAL_NUMERIC_CHECKPOINT_BEFORE_UPGRADE");
   String compatible=V51ChecksumEngine.sha256("SYNTHETIC_EARLIER_UI_ONLY_BUILD");provenance(db,store,key,job,compatible,TrainingCodeIdentityR964.SHA256);
   String status=ResearchDynamicBootstrapR922.pulse(store,1000).status;JSONObject after=new JSONObject(store.getState(key,""));
   check(status.startsWith("DEV_RUNNING")&&after.getInt("n")>before,"SAME_TRAINING_CODE_DIFFERENT_RELEASE_CONTINUES_NUMERIC_STATE");
   check(after.getString("source").equals(compatible)&&after.getString("last_execution_source").equals(SourceManifestR815.SOURCE_TREE_SHA256),"ORIGINAL_TRIAL_SOURCE_AND_CURRENT_EXECUTOR_BOTH_RETAINED");
   check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_TRIAL+" WHERE status='FAILED'")==0,"COMPATIBLE_UI_BUILD_DOES_NOT_FAIL_TRIAL");
   provenance(db,store,key,after,V51ChecksumEngine.sha256("SYNTHETIC_DIFFERENT_TRAINER_BUILD"),V51ChecksumEngine.sha256("DIFFERENT_TRAINING_CODE"));
   status=ResearchDynamicBootstrapR922.pulse(store,1000).status;
   JSONObject restarted=new JSONObject(store.getState(key,""));
   check(restarted.getString("training_code").equals(TrainingCodeIdentityR964.SHA256)&&restarted.getInt("n")<after.getInt("n"),"INCOMPATIBLE_TRAINING_CODE_ARCHIVES_AND_STARTS_NEW_TRIAL_IN_SAME_SLICE");
   check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_TRIAL+" WHERE status='FAILED'")==1,"ABANDONED_TRIAL_REMAINS_AUDITABLE");
   check(restarted.getInt("n")<=4,"NEW_TRIAL_COUNT_ONLY_INCLUDES_NEW_NUMERIC_WORK");
   check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key LIKE 'r965_development_archive_input_%'")==1,"ARCHIVED_TRIAL_RETAINS_ORIGINAL_INPUT_DATA");
   System.out.println("R964_TRAINING_UPGRADE_PASS|checks="+checks+"|SYNTHETIC_PROVENANCE_REAL_TRAINING_NO_CERTIFICATE_COMPATIBILITY");
  }finally{db.shutdown();}
 }
}
