package com.ethk3.novaonepro.r952;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import com.ethk3.novaonepro.V51ChecksumEngine;
/** Real candidate DAO SQL over real host SQLite. Android scheduling/driver are not emulated. */
public final class TrainingTwoPhaseHarnessR967 {
 static int checks;static void check(boolean v,String n){if(!v)throw new AssertionError(n);checks++;System.out.println("PASS "+n);}
 static String h(String s){return V51ChecksumEngine.sha256(s);}
 static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static String text(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getString(0):"";}}
 static ResearchOutboxR952.Event label(SQLiteDatabase db,String candidate,String target,String role){
  String snapshot=h("snapshot:"+target),frozen=CandidateCompilerR952.encode(new double[]{.055,.065,.075,.085,.095,.105,.115,.125,.135,.145}),input=h("R958_INPUT|"+frozen+'|'+snapshot),settled=h("settled:"+target);
  int mask=7,position=1,actual=3;String label=h("R958_LABEL|"+target+'|'+candidate+'|'+mask+'|'+position+'|'+actual+'|'+settled+'|'+snapshot);
  db.execSQL("INSERT INTO r952_candidate_predraw(target_issue,candidate_id,settled,label_hash,settlement_hash,actual_number,frozen_features,input_hash,evidence_role,snapshot_hash,prediction_mask,position) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{target,candidate,1,label,settled,actual,frozen,input,role,snapshot,mask,position});
  return new ResearchOutboxR952.Event(RuntimeEventContractR952.deliveryHash(target,candidate,settled,label),target,candidate,settled,label,input,1,System.currentTimeMillis()+45000);
 }
 static void commit(SQLiteDatabase db,ResearchOutboxR952.Event e,ResearchTrainingHookRegistryR952.PreparedBatch p){
  db.beginTransaction();try{ResearchTrainingHookRegistryR952.commit(db,e,p);db.setTransactionSuccessful();}finally{db.endTransaction();}
 }
 interface Op{void run();}
 static void reject(String token,Op op){try{op.run();throw new AssertionError("expected "+token);}catch(RuntimeException ex){check(ex.getMessage()!=null&&ex.getMessage().contains(token),"REJECT_"+token);}}
 public static void main(String[] args)throws Exception{
  final SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
   db.execSQL("CREATE TABLE r958_frozen_candidate(learner_id TEXT,created_issue TEXT)");
   db.execSQL("CREATE TABLE r952_candidate_predraw(target_issue TEXT,candidate_id TEXT,settled INTEGER,label_hash TEXT,settlement_hash TEXT,actual_number INTEGER,frozen_features TEXT,input_hash TEXT,evidence_role TEXT,snapshot_hash TEXT,prediction_mask INTEGER,position INTEGER,model_hash TEXT DEFAULT '',PRIMARY KEY(target_issue,candidate_id))");
   db.execSQL("CREATE TABLE r952_artifact(artifact_hash TEXT PRIMARY KEY,candidate_id TEXT,parent_hash TEXT,target_issue TEXT,delivery_hash TEXT UNIQUE,payload_hash TEXT,created_at INTEGER,model_payload TEXT,numeric_updates INTEGER)");
   db.execSQL("CREATE TABLE r952_learning_cursor(consumer_id TEXT PRIMARY KEY,trained_until_issue TEXT,artifact_hash TEXT,last_delivery_hash TEXT,updated_at INTEGER)");
   db.execSQL("CREATE TABLE r952_candidate_funnel(target_issue TEXT,candidate_id TEXT,stage TEXT,stage_hash TEXT,reason TEXT,created_at INTEGER)");
   db.execSQL("CREATE TABLE r952_runtime_event(event_hash TEXT PRIMARY KEY,target_issue TEXT,event_type TEXT,entity_id TEXT,payload_hash TEXT,source_commit TEXT,created_at INTEGER)");
   com.ethk3.novaonepro.ResearchEvaluationRunR967.schema(db);
   final ResearchOutboxR952.Event e=label(db,"C1","202609200001","ONLINE_TRAINING");
   long before=n(db,"SELECT total_changes()");
   final ResearchTrainingHookRegistryR952.PreparedBatch p=ResearchTrainingHookRegistryR952.prepare(db,e);
   check(n(db,"SELECT total_changes()")==before,"PREPARE_AND_NUMERIC_UPDATE_NO_DB_WRITES");
   db.beginTransaction();try{reject("PREPARE",new Op(){public void run(){ResearchTrainingHookRegistryR952.prepare(db,e);}});}finally{db.endTransaction();}
   reject("COMMIT_CONTRACT",new Op(){public void run(){ResearchTrainingHookRegistryR952.commit(db,e,p);}});
   commit(db,e,p);
   check(n(db,"SELECT COUNT(*) FROM r952_artifact")==1&&n(db,"SELECT COUNT(*) FROM r952_learning_cursor")==1,"STATE_CURSOR_COMMITTED_TOGETHER");
   String expected=CandidateCompilerR952.Model.initial().update(new double[]{.055,.065,.075,.085,.095,.105,.115,.125,.135,.145},3).hash();
   check(expected.equals(text(db,"SELECT payload_hash FROM r952_artifact")),"EXACT_NUMERIC_ORACLE_PARITY");
   before=n(db,"SELECT total_changes()");commit(db,e,p);check(n(db,"SELECT total_changes()")==before,"COMMIT_REPLAY_NO_SECOND_UPDATE");
   ResearchTrainingHookRegistryR952.PreparedBatch replay=ResearchTrainingHookRegistryR952.prepare(db,e);commit(db,e,replay);
   check(n(db,"SELECT COUNT(*) FROM r952_artifact")==1,"PREPARE_REPLAY_IDEMPOTENT");
   // A failure AFTER artifact INSERT must roll back model, cursor, funnel and event.
   final ResearchOutboxR952.Event second=label(db,"C1","202609200002","ONLINE_TRAINING");
   final ResearchTrainingHookRegistryR952.PreparedBatch secondP=ResearchTrainingHookRegistryR952.prepare(db,second);
   db.execSQL("CREATE TRIGGER fail_cursor BEFORE INSERT ON r952_learning_cursor BEGIN SELECT RAISE(ABORT,'INJECT_CURSOR_FAIL'); END");
   reject("INJECT_CURSOR_FAIL",new Op(){public void run(){commit(db,second,secondP);}});
   check(n(db,"SELECT COUNT(*) FROM r952_artifact")==1&&"202609200001".equals(text(db,"SELECT trained_until_issue FROM r952_learning_cursor")),"ROLLBACK_AFTER_ARTIFACT_PRESERVES_OLD_STATE");
   db.execSQL("DROP TRIGGER fail_cursor");commit(db,second,secondP);
   check(n(db,"SELECT MAX(numeric_updates) FROM r952_artifact")==2,"RETRY_AFTER_ROLLBACK_UPDATES_ONCE");
   // A changed cursor token cannot be overwritten by an already computed update.
   final ResearchOutboxR952.Event third=label(db,"C1","202609200003","ONLINE_TRAINING");
   final ResearchTrainingHookRegistryR952.PreparedBatch thirdP=ResearchTrainingHookRegistryR952.prepare(db,third);
   db.execSQL("UPDATE r952_learning_cursor SET last_delivery_hash='OTHER_WRITER_TOKEN'");
   reject("SNAPSHOT_CHANGED",new Op(){public void run(){commit(db,third,thirdP);}});
   check(n(db,"SELECT COUNT(*) FROM r952_artifact")==2,"STALE_PREPARED_UPDATE_WRITES_NOTHING");
   db.execSQL("UPDATE r952_learning_cursor SET last_delivery_hash=?",new Object[]{second.deliveryHash});
   commit(db,third,ResearchTrainingHookRegistryR952.prepare(db,third));check(n(db,"SELECT MAX(numeric_updates) FROM r952_artifact")==3,"REFRESHED_SNAPSHOT_CAN_COMMIT");
   final ResearchOutboxR952.Event sealed=label(db,"C2","202609200004","EVAL_SEALED");
   reject("NOT_ONLINE",new Op(){public void run(){ResearchTrainingHookRegistryR952.prepare(db,sealed);}});
   final ResearchOutboxR952.Event frozen=label(db,"C3","202609200005","ONLINE_TRAINING");
   db.execSQL("INSERT INTO r958_frozen_candidate VALUES('C3','202609200005')");
   reject("EVALUATION_LABEL_CANNOT_TRAIN",new Op(){public void run(){ResearchTrainingHookRegistryR952.prepare(db,frozen);}});
   final ResearchOutboxR952.Event bad=label(db,"C4","202609200006","ONLINE_TRAINING");
   db.execSQL("UPDATE r952_candidate_predraw SET actual_number=4 WHERE candidate_id='C4'");
   reject("LABEL_CONTENT_DRIFT",new Op(){public void run(){ResearchTrainingHookRegistryR952.prepare(db,bad);}});
   db.execSQL("UPDATE r952_candidate_predraw SET actual_number=3,frozen_features='tampered' WHERE candidate_id='C4'");
   reject("INPUT_HASH_DRIFT",new Op(){public void run(){ResearchTrainingHookRegistryR952.prepare(db,bad);}});
   check(n(db,"SELECT COUNT(*) FROM r952_artifact")==3,"INVALID_LABELS_NEVER_TRAINED");
   System.out.println("TWO_PHASE_SQLITE_PASS checks="+checks+"; real_production_prepare_commit=YES; host_sqlite=YES; full_outbox_scheduler=NOT_RUN; android_device=NOT_RUN");
  }finally{db.shutdown();}
 }
}