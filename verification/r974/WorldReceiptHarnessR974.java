package com.ethk3.novaonepro.r952;
import com.ethk3.novaonepro.*;import android.database.*;import android.database.sqlite.*;
/** Actual PIT capture -> matrix/fusion computation -> model inference -> consumed-input SQL receipt. */
public final class WorldReceiptHarnessR974 {
 static int checks;static void check(boolean v,String n){if(!v)throw new AssertionError(n);checks++;System.out.println("PASS "+n);}
 static long count(SQLiteDatabase db,String t){return DatabaseUtils.longForQuery(db,"SELECT COUNT(*) FROM "+t,null);}
 static void throwsState(Runnable r,String n){try{r.run();throw new AssertionError("ACCEPTED "+n);}catch(IllegalStateException expected){check(true,n);}}
 public static void main(String[] args)throws Exception{SQLiteDatabase db=new SQLiteDatabase(args[0]);try{DbStore store=HostProductR974.store(db);HostProductR974.schema(db,store);
  for(int issue=200;issue<250;issue++){Object[] vals=new Object[12];vals[0]=String.format(java.util.Locale.US,"20260920%04d",issue);vals[1]=1;for(int n=0;n<10;n++)vals[n+2]=(n+issue)%10+1;db.execSQL("INSERT INTO draws(issue,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",vals);}
  String target="202609200250",h=V51ChecksumEngine.sha256("SYNTHETIC_WORLD_RECEIPT");PITSnapshotContractR952 pit=PITSnapshotContractR952.capture(store,target,40);
  CandidateAstR952 left=new CandidateAstR952(CandidateAstR952.Operator.MARKOV_TRANSITION,1,40),right=new CandidateAstR952(CandidateAstR952.Operator.CROSS_POSITION_LAG,1,40),fusion=CandidateAstR952.fuse(left,right,50);
  CandidateCompilerR952.Model model=CandidateCompilerR952.Model.initial();
  // This harness tests consumed-input/transaction contracts, NOT cold-start latency.
  // Warm numerical class loading explicitly; preserve the separate cold-start failure log.
  WorldFeatureComputationR974.Result warmed=WorldFeatureComputationR974.compute(fusion,pit.history);model.probabilities(warmed.values());
  System.out.println("PERFORMANCE_SCOPE=PREWARMED_FUNCTIONAL_TEST_NOT_COLD_LATENCY");
  CandidateSandboxR952.Prediction prediction=CandidateSandboxR952.execute(CandidateCompilerR952.compile(fusion),pit,model);
  check(prediction.computation.trace.contains("LAGGED_ASSOCIATION_NOT_CAUSAL_PROOF"),"ASSOCIATION_NOT_FALSE_CAUSAL_PROOF");
  throwsState(()->ComputationReceiptR974.record(db,target,"CHILD",fusion.structuralHash,model.hash(),h,prediction),"COMPUTATION_RECEIPT_REQUIRES_TRANSACTION");
  db.beginTransaction();try{
   ComputationReceiptR974.record(db,target,"CHILD",fusion.structuralHash,model.hash(),h,prediction);
   ComputationReceiptR974.record(db,target,"CHILD",fusion.structuralHash,model.hash(),h,prediction);
   ComputationReceiptR974.lineage(db,"CHILD","PARENT_A","PRIMARY",left.canonical,h,target);
   ComputationReceiptR974.lineage(db,"CHILD","PARENT_B","FUSION_SECONDARY",right.canonical,h,target);
   db.setTransactionSuccessful();
  }finally{db.endTransaction();}
  check(count(db,ComputationReceiptR974.TABLE)==1,"CONSUMPTION_RECEIPT_REPLAY_NO_DUPLICATE");check(count(db,ComputationReceiptR974.LINEAGE)==2,"BOTH_PARENT_FORMULAS_RETAINED");
  try(Cursor c=db.rawQuery("SELECT features,world_input_hash,world_output_hash,prediction_mask,trace FROM "+ComputationReceiptR974.TABLE,null)){check(c.moveToFirst(),"ACTUAL_COMPUTATION_ROW_PRESENT");check(c.getString(0).equals(prediction.features),"RECORDED_FEATURES_EXACTLY_MODEL_INPUT");check(c.getString(1).equals(prediction.computation.inputHash)&&c.getString(2).equals(prediction.computation.outputHash),"INPUT_AND_OUTPUT_HASHES_MATCH_ACTUAL_COMPUTATION");check(c.getInt(3)==prediction.mask&&AppConfig.countK3MaskNumbers(c.getInt(3))==3,"RECORDED_OUTPUT_FIXED_K3");}
  db.beginTransaction();try{
   throwsState(()->ComputationReceiptR974.record(db,target,"CHILD",fusion.structuralHash,model.hash(),V51ChecksumEngine.sha256("DIFFERENT_INPUT"),prediction),"REPLAY_WITH_CHANGED_INPUT_REJECTED");
   CandidateSandboxR952.Prediction forged=new CandidateSandboxR952.Prediction(prediction.mask,"NOT_ACTUAL_MODEL_INPUT",prediction.computation);
   throwsState(()->ComputationReceiptR974.record(db,target,"FORGED",fusion.structuralHash,model.hash(),h,forged),"UNCONSUMED_FEATURE_COUNTER_CANNOT_BE_RECEIPT");
   ComputationReceiptR974.record(db,target,"ROLLBACK",fusion.structuralHash,model.hash(),h,prediction);
  }finally{db.endTransaction();}
  check(count(db,ComputationReceiptR974.TABLE)==1,"CRASH_ROLLBACK_LEAVES_NO_COMPUTATION_RECEIPT");
  throwsState(()->db.execSQL("DELETE FROM "+ComputationReceiptR974.LINEAGE+" WHERE child_id='CHILD'"),"PARENT_FORMULAS_NOT_DELETED");
  check(count(db,"r974_exact_real_receipt")==0,"WORLD_RESEARCH_DOES_NOT_CREATE_REAL");
  System.out.println("R974_WORLD_RECEIPTS_PASS|checks="+checks+"|actual_PIT_matrix_model_SQL=true|future_win_rate=NOT_TESTED");
 }finally{db.shutdown();}}
}
