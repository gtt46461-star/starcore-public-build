package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.*;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.nio.file.*;import java.io.*;
/** Observed R74 NEURAL AST and report history through actual prepare/register/train owners.
 * Registration boundary metadata is deterministically reconstructed by the fixture builder;
 * no original Future/OOS, certificates, or REAL are copied or fabricated. */
public final class training_ReportModernR75 {
 static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static DbStore owner(SQLiteDatabase d,String[] a)throws Exception{DbStore s=HostProductR974.store(d);File root=new File(a[2]);root.mkdirs();HostProductR974.set(s,"researchAppCP12",new android.content.Context(root,new File(a[3])));HostProductR974.set(s,"researchInputRootCP11",new File(root,"research-input-v2"));return s;}
 public static void main(String[]a)throws Exception{
  try(SQLiteDatabase d=new SQLiteDatabase(a[0],a[1])){
   RealtimePressureGateR811.resetForTests();DbStore store=owner(d,a);String ast,id,action,evidence,source;
   try(Cursor c=d.rawQuery("SELECT f.ast,f.candidate_id,f.action_id,a.evidence_hash,f.source_issue FROM research_family_creation_cp12 f JOIN research_action_v2 a ON a.action_id=f.action_id WHERE f.family='NEURAL'",null)){if(!c.moveToFirst())throw new AssertionError("REPORT_NEURAL_PROPOSAL_MISSING");ast=c.getString(0);id=c.getString(1);action=c.getString(2);evidence=c.getString(3);source=c.getString(4);}
   final String fid=id,fa=action,fe=evidence,fs=source,fast=ast;
   String intent=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R75_TEST_REPORT_INTENT",db->ResearchIntentLedgerR952.put(db,"R75_REPORT_AST_REPLAY",fast,"UNTRAINED","DEVELOPMENT_ONLY","NEURAL",1,fs));
   try(DevelopmentProductionCP12.PreparedProtocol protocol=DevelopmentProductionCP12.prepare(store,CandidateAstR952.parse(ast),id,action,"202610130324")){
    ResearchDirectorR952.PreparedEvolution child=new ResearchDirectorR952.PreparedEvolution(id,action,"",intent,"",evidence);
    String task=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R75_TEST_REPORT_REGISTER",db->ResearchDirectorR952.commitPreparedCandidateCP5(db,protocol,child));System.out.println("REGISTERED "+task+" trainN="+protocol.trainN);
   }
   int turns=0;long prior=0,start=System.nanoTime();
   for(;turns<600&&n(d,"SELECT COUNT(*) FROM research_development_receipt_v2")==0;turns++){
    store=owner(d,a);ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(850);
    try{ResearchTrainingHookRegistryR952.drainTrainingSlotCP11(store,System.nanoTime()+775_000_000L);}catch(ResearchWorkBudgetR927.Yield expected){}finally{ResearchWorkBudgetR927.leave(scope);}
    long rows=n(d,"SELECT COUNT(*) FROM research_training_feature_row_cp11");if(turns%20==0||rows>prior){System.out.println("PULSE "+turns+" featureRows="+rows+" receipts="+n(d,"SELECT COUNT(*) FROM research_development_receipt_v2"));prior=rows;}
    if(n(d,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND status='FAILED_TERMINAL'")>0)throw new AssertionError("TRAIN_TERMINAL_FAILURE");Thread.sleep(100);
   }
   if(n(d,"SELECT COUNT(*) FROM research_development_receipt_v2")!=1)throw new AssertionError("MODERN_NEURAL_TRAINING_STALLED");
   if(n(d,"SELECT SUM(future_n+may_create_real) FROM r952_candidate")!=0||n(d,"SELECT COUNT(*) FROM research_certificate_r929")!=0||n(d,"SELECT COUNT(*) FROM trades")!=0)throw new AssertionError("DEVELOPMENT_CONTAMINATED_FUTURE_OR_REAL");
   try(Cursor c=d.rawQuery("SELECT model_payload,numeric_updates,development_sample_n,validation_sample_n FROM r952_artifact WHERE candidate_id=?",new String[]{id})){if(!c.moveToFirst())throw new AssertionError("MODEL_ARTIFACT_MISSING");CandidateCompilerR952.Model model=CandidateCompilerR952.Model.parse(c.getString(0));if(model.modern()==null||!model.modern().kind.name().equals("MLP_D32_16_10")||c.getLong(2)!=1200||c.getLong(3)!=120)throw new AssertionError("MLP_TRAINING_OR_VALIDATION_NOT_REAL");System.out.println("MODEL kind="+model.modern().kind+" train="+c.getLong(2)+" validation="+c.getLong(3)+" hash="+model.hash());}
   Files.writeString(Path.of(a[4]),"{\"status\":\"PASS\",\"training_turns\":"+turns+",\"elapsed_ms\":"+(System.nanoTime()-start)/1_000_000+",\"feature_rows\":"+prior+",\"train_n\":1200,\"validation_n\":120,\"receipt_count\":1,\"model_kind\":\"MLP_D32_16_10\",\"scope\":\"REPORT_AST_HISTORY_ACTUAL_OWNER_HOST_RESTARTED_NOT_PHONE_FUTURE_OR_REAL\"}\n");
  }
 }
}
