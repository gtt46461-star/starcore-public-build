package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.*;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.nio.file.*;
/** Synthetic labels isolate scheduler/recovery correctness. No observations or predictions
 * from this test are applied to the report, phone, qualification, or REAL. */
public final class training_OnlineContinuityR77 {
 static DbStore owner(SQLiteDatabase d,String[] a)throws Exception{DbStore s=HostProductR974.store(d);java.io.File root=new java.io.File(a[2]+"-files");root.mkdirs();android.content.Context context=new android.app.Application(root,new java.io.File(a[3]));HostProductR974.set(s,"researchAppCP12",context);HostProductR974.set(s.coordinatorForResearchR952(),"app",context);HostProductR974.set(s,"researchInputRootCP11",new java.io.File(root,"research-input-v2"));return s;}
 static int checks;static void ok(boolean b,String m){if(!b)throw new AssertionError(m);checks++;System.out.println("PASS "+m);}
 static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static String h(String s){return V51ChecksumEngine.sha256(s);}
 static String label(SQLiteDatabase d,String candidate,String issue,boolean broken){
  String snapshot=h("R77_SYNTHETIC_SNAPSHOT|"+issue),features=CandidateCompilerR952.encode(new double[]{.055,.065,.075,.085,.095,.105,.115,.125,.135,.145});
  String input=h("R958_INPUT|"+features+'|'+snapshot),settlement=h("R77_SYNTHETIC_SETTLEMENT|"+issue),label=h("R958_LABEL|"+issue+'|'+candidate+"|7|1|3|"+settlement+'|'+snapshot);
  if(!broken)d.execSQL("INSERT INTO r952_candidate_predraw(target_issue,candidate_id,prediction_mask,position,snapshot_hash,structure_hash,model_hash,code_hash,data_hash,captured_at,settled,actual_number,hit,settlement_hash,label_hash,frozen_features,input_hash,evidence_role)VALUES(?,?,7,1,?,?,?,?,?,1,1,3,1,?,?,?,?,'ONLINE_TRAINING')",new Object[]{issue,candidate,snapshot,h(candidate),CandidateCompilerR952.Model.initial().hash(),h("code"),h("data"),settlement,label,features,input});
  return ResearchOutboxR952.enqueue(d,issue,candidate,settlement,label,input);
 }
 static final class PreemptDb extends SQLiteDatabase {
  boolean armed;int injections;
  PreemptDb(String bridge,String path)throws Exception{super(bridge,path);}
  @Override public synchronized Cursor rawQuery(String q,String[]args){
   if(armed&&q.startsWith("SELECT settled,label_hash")){armed=false;injections++;RealtimePressureGateR811.beginAnalysis();}
   return super.rawQuery(q,args);
  }
 }
 public static void main(String[]a)throws Exception{
  try(PreemptDb d=new PreemptDb(a[0],a[1])){
   RealtimePressureGateR811.resetForTests();DbStore store=owner(d,a);String orphan;
   d.beginTransaction();try{
    d.execSQL("INSERT INTO draws(issue,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES('202610130014',1,1,2,3,4,5,6,7,8,9,10,0)");
    for(int i=1;i<=12;i++)label(d,"R77-ONLINE",String.format("20261013%04d",i),false);
    orphan=h("none");try(Cursor c=d.rawQuery("SELECT delivery_hash FROM r952_research_outbox ORDER BY target_issue LIMIT 1",null)){c.moveToFirst();orphan=c.getString(0);}
    d.execSQL("UPDATE r952_research_outbox SET state='RUNNING',attempt_count=1,lease_owner='DEAD_R74_PROCESS',lease_until=1,updated_at=1 WHERE delivery_hash=?",new Object[]{orphan});
    label(d,"R77-BROKEN","202610130013",true);label(d,"R77-HEALTHY","202610130014",false);d.setTransactionSuccessful();
   }finally{d.endTransaction();}
   d.armed=true;
   ResearchWorkBudgetR927.Scope injected=ResearchWorkBudgetR927.enter(1000);
   try{ResearchOutboxR952.drainOne(store);}catch(ResearchWorkBudgetR927.Yield expected){}finally{RealtimePressureGateR811.endAnalysis();ResearchWorkBudgetR927.leave(injected);}
   ok(d.injections==1&&n(d,"SELECT COUNT(*) FROM r952_artifact")==0,"MID_PREPARE_P0_NO_MODEL_PUBLICATION");
   ok(n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='RETRYABLE_FAILED' AND last_error LIKE '%REALTIME_PREEMPTED%'")==1,"HEAD_RELEASED_FOR_P0_RETRY");
   int pulses=0;long start=System.nanoTime();
   for(;pulses<154&&(n(d,"SELECT COUNT(*) FROM r952_artifact")<13||n(d,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='POST_TRAIN' AND status='COMPLETED'")<13);pulses++){
    // Recreate each caller store; all progress must come from persisted owner state.
    store=owner(d,a);ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(850);
    try{ResearchLearningContinuityR912.pulse(store,850);}catch(ResearchWorkBudgetR927.Yield yielded){}finally{ResearchWorkBudgetR927.leave(scope);}
    Thread.sleep(100);
   }
   ok(n(d,"SELECT COUNT(*) FROM r952_artifact")==13,"THIRTEEN_REAL_NUMERIC_ARTIFACTS_FROM_VALID_LABELS");
   ok(n(d,"SELECT numeric_updates FROM r952_artifact WHERE candidate_id='R77-ONLINE' ORDER BY target_issue DESC LIMIT 1")==12,"CHRONOLOGICAL_MODEL_UPDATES_REACH_TWELVE");
   ok(n(d,"SELECT COUNT(*) FROM r952_research_outbox WHERE state='COMPLETED'")==13,"OUTBOX_ACK_MATCHES_ARTIFACTS");
   ok(n(d,"SELECT attempt_count FROM r952_research_outbox WHERE target_issue='202610130001'")>=2,"EXPIRED_CRASHED_LEASE_RECLAIMED");
   ok(n(d,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='POST_TRAIN' AND status='COMPLETED'")==13,"POST_TRAIN_FEEDBACK_COMPLETED");
   ok(n(d,"SELECT COUNT(*) FROM r952_knowledge WHERE kind='STRICT_FUTURE'")==13,"KNOWLEDGE_RECEIPTS_COMMITTED");
   ok(n(d,"SELECT COUNT(*) FROM r952_artifact WHERE candidate_id='R77-BROKEN'")==0,"INVALID_INPUT_NEVER_TRAINED");
   ok(n(d,"SELECT COUNT(*) FROM r952_artifact WHERE candidate_id='R77-HEALTHY'")==1,"FAILING_CANDIDATE_DOES_NOT_BLOCK_HEALTHY_CANDIDATE");
   for(int i=0;i<6;i++){ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(850);try{ResearchLearningContinuityR912.pulse(owner(d,a),850);}catch(ResearchWorkBudgetR927.Yield yielded){}finally{ResearchWorkBudgetR927.leave(scope);}}
   ok(n(d,"SELECT COUNT(*) FROM r952_artifact")==13,"REPEATED_PULSES_DO_NOT_DOUBLE_TRAIN");
   ok(n(d,"SELECT COUNT(*) FROM research_certificate_r929")==0&&n(d,"SELECT COUNT(*) FROM trades")==0,"NO_CERTIFICATE_OR_REAL_FROM_RECOVERY_TEST");
   Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"director_pulses\":"+pulses+",\"elapsed_ms\":"+(System.nanoTime()-start)/1_000_000+",\"artifact_count\":13,\"feedback_count\":13,\"scope\":\"ACTUAL_CONTINUITY_SYNTHETIC_VALID_LEGACY_LABELS_P0_CRASH_LEASE_NO_PHONE_SIDECAR_OR_FUTURE_QUALITY\"}\n");
  }
 }
}
