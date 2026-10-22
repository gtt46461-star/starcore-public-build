package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import java.nio.file.*;
public final class ContinuityWheelContractCP12{
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("continuity "+checks);}
 public static void main(String[] a)throws Exception{SQLiteDatabase db=new SQLiteDatabase(a[0],a[1]);try{
  ok(!ResearchLearningContinuityR912.directorTurnCompleteCP12(db));
  for(String slot:new String[]{"POST","MATRIX","RESEARCH","EVOLVE","IMPORT"}){db.execSQL("INSERT OR REPLACE INTO app_state VALUES('r975_director_priority',?,1)",new Object[]{slot});ok(!ResearchLearningContinuityR912.directorTurnCompleteCP12(db));}
  db.execSQL("INSERT OR REPLACE INTO app_state VALUES('r975_director_priority','TRAIN',1)");ok(ResearchLearningContinuityR912.directorTurnCompleteCP12(db));
  ok(ResearchLearningContinuityR912.terminalReviewOutcome("RETIRED_CP12_MODERN_TRAINING_OWNER"));ok(ResearchLearningContinuityR912.terminalReviewOutcome("RETIRED_CP12_DIRECTOR_IMPORT_OWNER"));ok(!ResearchLearningContinuityR912.terminalReviewOutcome("R952_PENDING|advanced=0"));
  ok(!ResearchLearningContinuityR912.hasUnfinishedR952(db));
  for(String kind:new String[]{"TRAIN_CANDIDATE","FREEZE_CANDIDATE","RESEARCH_ACTION","POST_TRAIN"}){
   db.execSQL("INSERT INTO v57_research_task_r967(task_id,kind,candidate_id,target_issue,stream_seq,input_hash,authority_ref,status,created_at,updated_at) VALUES(?,?,'fixture','202609230001','stream','input','authority','WAIT_INPUT',1,1)",new Object[]{kind,kind});ok(ResearchLearningContinuityR912.hasUnfinishedR952(db));db.execSQL("UPDATE v57_research_task_r967 SET status='COMPLETED' WHERE task_id=?",new Object[]{kind});ok(!ResearchLearningContinuityR912.hasUnfinishedR952(db));
  }
  Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"assertions\":"+checks+",\"scope\":\"Actual continuity queue/wheel SQL prevents serving modern queued work or skipping five Director slots; retired lane outcomes are terminal without ERROR spin. Host SQLite/JVM.\"}\n");
 }finally{db.shutdown();}}
}
