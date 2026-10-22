package com.ethk3.novaonepro.r952;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

public final class OutboxTransactionHarnessR967 {
 static int checks;interface Op{void run();}
 static void check(boolean v,String s){if(!v)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
 static void tx(SQLiteDatabase d,Op op){d.beginTransaction();try{op.run();d.setTransactionSuccessful();}finally{d.endTransaction();}}
 static void reject(String token,Op op){try{op.run();throw new AssertionError("expected "+token);}catch(RuntimeException e){check(e.getMessage()!=null&&e.getMessage().contains(token),"REJECT_"+token);}}
 static long n(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static String text(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
 static String h(String s){return V51ChecksumEngine.sha256(s);}
 static void label(SQLiteDatabase d,String candidate,String target,String role) {
  String snapshot=h("snapshot:"+target),frozen=CandidateCompilerR952.encode(new double[]{.055,.065,.075,.085,.095,.105,.115,.125,.135,.145}),input=h("R958_INPUT|"+frozen+'|'+snapshot),settled=h("settled:"+target);int mask=7,pos=1,actual=3;
  String hash=h("R958_LABEL|"+target+'|'+candidate+'|'+mask+'|'+pos+'|'+actual+'|'+settled+'|'+snapshot);
  d.execSQL("INSERT INTO r952_candidate_predraw(target_issue,candidate_id,settled,label_hash,settlement_hash,actual_number,frozen_features,input_hash,evidence_role,snapshot_hash,prediction_mask,position,hit) VALUES(?,?,1,?,?,?,?,?,?,?,?,?,1)",new Object[]{target,candidate,hash,settled,actual,frozen,input,role,snapshot,mask,pos});
  ResearchOutboxR952.enqueue(d,target,candidate,settled,hash,input);
 }
 static void schema(SQLiteDatabase d) {
  d.execSQL("CREATE TABLE r958_frozen_candidate(learner_id TEXT,created_issue TEXT)");
  ResearchEvaluationRunR967.schema(d);
  d.execSQL("CREATE TABLE r952_candidate_predraw(target_issue TEXT,candidate_id TEXT,settled INTEGER,label_hash TEXT,settlement_hash TEXT,actual_number INTEGER,frozen_features TEXT,input_hash TEXT,evidence_role TEXT,snapshot_hash TEXT,prediction_mask INTEGER,position INTEGER,hit INTEGER,model_hash TEXT DEFAULT '',PRIMARY KEY(target_issue,candidate_id))");
  d.execSQL("CREATE TABLE r952_artifact(artifact_hash TEXT PRIMARY KEY,candidate_id TEXT,parent_hash TEXT,target_issue TEXT,delivery_hash TEXT UNIQUE,payload_hash TEXT,created_at INTEGER,model_payload TEXT,numeric_updates INTEGER)");
  d.execSQL("CREATE TABLE r952_learning_cursor(consumer_id TEXT PRIMARY KEY,trained_until_issue TEXT,artifact_hash TEXT,last_delivery_hash TEXT,updated_at INTEGER)");
  d.execSQL("CREATE TABLE r952_candidate_funnel(target_issue TEXT,candidate_id TEXT,stage TEXT,stage_hash TEXT,reason TEXT,created_at INTEGER)");
  d.execSQL("CREATE TABLE r952_runtime_event(event_hash TEXT PRIMARY KEY,target_issue TEXT,event_type TEXT,entity_id TEXT,payload_hash TEXT,source_commit TEXT,created_at INTEGER)");
  d.execSQL("CREATE TABLE r952_research_outbox(delivery_hash TEXT PRIMARY KEY,target_issue TEXT,candidate_id TEXT,settlement_hash TEXT,label_hash TEXT,model_input_hash TEXT,event_type TEXT,state TEXT,attempt_count INTEGER,lease_until INTEGER,last_error TEXT,created_at INTEGER,updated_at INTEGER)");
  d.execSQL("CREATE TABLE r952_recovery_journal(delivery_hash TEXT,attempt INTEGER,state TEXT,error_code TEXT,event_hash TEXT PRIMARY KEY,created_at INTEGER)");
  d.execSQL("CREATE TABLE r952_knowledge(knowledge_hash TEXT PRIMARY KEY,target_issue TEXT,candidate_id TEXT,kind TEXT,payload_hash TEXT,created_at INTEGER,payload TEXT,payload_format TEXT)");
  ResearchTaskQueueR967.schema(d);
 }
 public static void main(String[] args)throws Exception {
  final SQLiteDatabase d=new SQLiteDatabase(args[0]);try {
   tx(d,()->schema(d));tx(d,()->{label(d,"C1","202609200001","ONLINE_TRAINING");label(d,"C1","202609200002","ONLINE_TRAINING");});
   final ResearchOutboxR952.Event[] e=new ResearchOutboxR952.Event[4];long now=System.currentTimeMillis();
   tx(d,()->e[0]=ResearchOutboxR952.claim(d,now));
   check("202609200001".equals(e[0].targetIssue),"EARLIEST_LABEL_CLAIMED");
   tx(d,()->check(ResearchOutboxR952.claim(d,now)==null,"LATER_LABEL_CANNOT_OVERTAKE_EARLIER_LEASE"));
   final ResearchTrainingHookRegistryR952.PreparedBatch p=ResearchTrainingHookRegistryR952.prepare(d,e[0]);
   d.execSQL("CREATE TRIGGER inject_post BEFORE INSERT ON "+ResearchTaskQueueR967.TABLE+" BEGIN SELECT RAISE(ABORT,'INJECT_POST_INTENT'); END");
   reject("INJECT_POST_INTENT",()->tx(d,()->ResearchOutboxR952.consume(d,e[0],p)));
   check(n(d,"SELECT COUNT(*) FROM r952_artifact")==0&&n(d,"SELECT COUNT(*) FROM r952_learning_cursor")==0,"POST_INTENT_FAILURE_ROLLS_BACK_MODEL_AND_CURSOR");
   check(n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='COMPLETED'")==0,"POST_INTENT_FAILURE_ROLLS_BACK_TRAINING_ACK");
   d.execSQL("DROP TRIGGER inject_post");tx(d,()->check(ResearchOutboxR952.consume(d,e[0],p),"RETRY_COMMITS_TRAIN_MODEL_CURSOR_ACK_AND_POST"));
   check(n(d,"SELECT COUNT(*) FROM r952_artifact")==1&&n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE)==1,"ONE_NUMERIC_UPDATE_ONE_DURABLE_POST_INTENT");
   tx(d,()->check(!ResearchOutboxR952.consume(d,e[0],p),"ACK_REPLAY_NEVER_TRAINS_TWICE"));
   final ResearchTaskQueueR967.Lease[] post={null};tx(d,()->post[0]=ResearchTaskQueueR967.claim(d,ResearchTaskQueueR967.POST_TRAIN,System.currentTimeMillis()));
   check(post[0]!=null,"POST_ACK_CRASH_RECOVERED_FROM_PERSISTENT_INTENT");
   d.execSQL("CREATE TRIGGER inject_post_ack BEFORE UPDATE ON "+ResearchTaskQueueR967.TABLE+" WHEN NEW.status='COMPLETED' BEGIN SELECT RAISE(ABORT,'INJECT_POST_ACK'); END");
   reject("INJECT_POST_ACK",()->tx(d,()->{String result=PostSettlementResearchLoopR952.projectOnline(d,post[0]);ResearchTaskQueueR967.complete(d,post[0],result);}));
   check(n(d,"SELECT COUNT(*) FROM r952_knowledge")==0,"POST_ACK_FAILURE_ROLLS_BACK_KNOWLEDGE");
   d.execSQL("DROP TRIGGER inject_post_ack");tx(d,()->{String result=PostSettlementResearchLoopR952.projectOnline(d,post[0]);ResearchTaskQueueR967.complete(d,post[0],result);});
   check(n(d,"SELECT COUNT(*) FROM r952_knowledge")==1,"POST_RETRY_COMMITS_KNOWLEDGE_ONCE");
   tx(d,()->check(PostSettlementResearchLoopR952.rehydrate(d,8)==0,"INDEX_REPAIR_DOES_NOT_DUPLICATE_EXISTING_RECEIPT"));
   tx(d,()->e[1]=ResearchOutboxR952.claim(d,System.currentTimeMillis()));
   tx(d,()->e[2]=ResearchOutboxR952.claim(d,e[1].lease+1));
   final ResearchTrainingHookRegistryR952.PreparedBatch p2=ResearchTrainingHookRegistryR952.prepare(d,e[1]);
   tx(d,()->check(!ResearchOutboxR952.consume(d,e[1],p2),"OLD_TRAINING_LEASE_CANNOT_COMMIT_AFTER_RECOVERY"));
   tx(d,()->check(!ResearchOutboxR952.release(d,e[1],"STALE",0),"OLD_TRAINING_RELEASE_CANNOT_OVERWRITE_SUCCESSOR"));
   tx(d,()->check(ResearchOutboxR952.release(d,e[2],"RESOURCE_YIELD",0),"COOPERATIVE_YIELD_RELEASES_OWN_LEASE"));
   for(int i=0;i<12;i++) {tx(d,()->e[3]=ResearchOutboxR952.claim(d,System.currentTimeMillis()));tx(d,()->ResearchOutboxR952.release(d,e[3],"RESOURCE_YIELD",0));}
   check(n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='TERMINAL_FAILED'")==0,"MORE_THAN_EIGHT_RESOURCE_YIELDS_ARE_NOT_QUALITY_FAILURE");
   check(n(d,"SELECT MAX(attempt_count) FROM r952_research_outbox")>8,"FENCING_GENERATION_REMAINS_MONOTONIC");
   tx(d,()->e[3]=ResearchOutboxR952.claim(d,System.currentTimeMillis()));
   final ResearchTrainingHookRegistryR952.PreparedBatch p3=ResearchTrainingHookRegistryR952.prepare(d,e[3]);tx(d,()->ResearchOutboxR952.consume(d,e[3],p3));
   check(n(d,"SELECT MAX(numeric_updates) FROM r952_artifact")==2,"EVENTUALLY_RESUMED_LABEL_UPDATES_EXACTLY_ONCE");
   reject("NATURAL_KEY_DRIFT",()->tx(d,()->ResearchOutboxR952.enqueue(d,"202609200002","C1",h("changed"),h("different-label"),h("input"))));
   tx(d,()->label(d,"SEALED","202609200003","EVAL_SEALED"));tx(d,()->e[3]=ResearchOutboxR952.claim(d,System.currentTimeMillis()));
   reject("NOT_ONLINE",()->ResearchTrainingHookRegistryR952.prepare(d,e[3]));
   check(n(d,"SELECT COUNT(*) FROM r952_artifact")==2,"SEALED_EVALUATION_NEVER_UPDATES_ONLINE_MODEL");
   System.out.println("OUTBOX_TRANSACTION_PASS checks="+checks+"; actual_claim_consume_release_SQL=YES; device=NOT_RUN");
  }finally{d.shutdown();}
 }
}
