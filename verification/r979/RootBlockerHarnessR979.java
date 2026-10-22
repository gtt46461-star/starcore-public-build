package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.*;
import android.database.*;import android.database.sqlite.*;
/** Faults are injected by a host-only SQLite adapter. Product bytecode and SQL are unchanged. */
public final class RootBlockerHarnessR979 {
 static int checks;
 static void check(boolean b,String name){if(!b)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
 static long count(SQLiteDatabase d,String table){return DatabaseUtils.longForQuery(d,"SELECT COUNT(*) FROM "+table,null);}
 static String scalar(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
 static final class FaultDb extends SQLiteDatabase {
  String failOn;
  FaultDb(String bridge)throws Exception{super(bridge);}
  @Override public synchronized Cursor rawQuery(String sql,String[] args){
   if(failOn!=null&&sql.startsWith(failOn)){failOn=null;ResearchWorkBudgetR927.Scope s=ResearchWorkBudgetR927.enter(1);try{try{Thread.sleep(5);}catch(InterruptedException e){throw new IllegalStateException(e);}ResearchWorkBudgetR927.checkpoint();}finally{ResearchWorkBudgetR927.leave(s);}}
   return super.rawQuery(sql,args);
  }
 }
 public static void main(String[] args)throws Exception{
  boolean fixed=args.length>1&&args[1].equals("fixed");
  try(Fixture f=new Fixture(args[0])){
   SQLiteDatabase d=f.db;String h=V51ChecksumEngine.sha256("HOST_ONLY_VALID_LEGACY_AUTHORITY"),issue="202609200200",model="HOST_PARENT";
   d.beginTransaction();try{
    d.execSQL("INSERT INTO r952_candidate_predraw(target_issue,candidate_id,prediction_mask,position,snapshot_hash,structure_hash,model_hash,code_hash,data_hash,captured_at,settled,actual_number,hit,settlement_hash,label_hash,input_hash,evidence_role) VALUES(?,?,14,1,?,?,?,?,?,1,1,1,1,?,?,?,'ONLINE_TRAINING')",new Object[]{issue,model,h,h,h,h,h,h,h,h});
    d.execSQL("INSERT INTO r952_research_outbox(delivery_hash,target_issue,candidate_id,settlement_hash,label_hash,model_input_hash,event_type,state,created_at,updated_at) VALUES(?,?,?,?,?,?,'TRAIN','COMPLETED',1,1)",new Object[]{h,issue,model,h,h,h});
    String input=V51ChecksumEngine.sha256("R967_POST_ONLINE|"+h+'|'+h+'|'+h+'|'+h);
    ResearchTaskQueueR967.enqueue(d,ResearchTaskQueueR967.POST_TRAIN,model,issue,h,input,h);d.setTransactionSuccessful();
   }finally{d.endTransaction();}
   f.db.failOn="SELECT o.label_hash,o.settlement_hash";
   try{PostSettlementResearchLoopR952.drainOne(f.store);throw new AssertionError("FAULT_NOT_REACHED");}catch(ResearchWorkBudgetR927.Yield expected){check(true,"POST_PROJECTION_BUDGET_FAULT_REACHED");}
   String status=scalar(d,"SELECT status FROM "+ResearchTaskQueueR967.TABLE+" WHERE kind='POST_TRAIN'");
   check(status.equals(fixed?"PENDING":"RUNNING"),fixed?"POST_CLAIM_ROLLBACK_IMMEDIATELY_RETRYABLE":"BASELINE_REPRODUCED_STRANDED_RUNNING_LEASE");
   check(count(d,"r952_knowledge")==0,"NO_KNOWLEDGE_AFTER_FAILED_TRANSACTION");
   if(fixed){check(PostSettlementResearchLoopR952.drainOne(f.store),"POST_RETRY_COMPLETES");check(count(d,"r952_knowledge")==1,"ONE_ACTUAL_FEEDBACK");check(!PostSettlementResearchLoopR952.drainOne(f.store),"DUPLICATE_DOES_NOT_PROJECT_AGAIN");}
  }
  try(Fixture f=new Fixture(args[0])){
   SQLiteDatabase d=f.db;d.beginTransaction();try{FixedResearchWindowR932.schema(d);d.setTransactionSuccessful();}finally{d.endTransaction();}
   String h=V51ChecksumEngine.sha256("SYNTHETIC_CAL_READY");
   d.execSQL("INSERT INTO "+CandidateCalibrationR932.STUDY+" VALUES(?,?,241,241,'READY','202609200001')",new Object[]{h,"algorithm:HOST_STATIC"});
   d.beginTransaction();try{for(int i=1;i<=241;i++){
    String issue=String.format(java.util.Locale.US,"20260920%04d",i);
    d.execSQL("INSERT INTO "+CandidateCalibrationR932.OBS+" VALUES(?,?,?,?,?,?,?,1,1,2,3,?,1,?)",new Object[]{h,issue,"algorithm:HOST_STATIC",h,h,h,.2+(i%7)*.03,h,(i%10)<4?1:0});
   }d.setTransactionSuccessful();}finally{d.endTransaction();}
   f.db.failOn="SELECT target_issue FROM (";
   try{FixedResearchWindowR932.maintain(f.store);throw new AssertionError("CAL_FAULT_NOT_REACHED");}catch(ResearchWorkBudgetR927.Yield expected){check(true,"SETTLEMENT_BUDGET_FAULT_REACHED");}
   if(fixed) {
    check(count(d,CandidateCalibrationR932.MODEL)==0,"SETTLEMENT_FIRST_SLOT_DOES_NOT_FAKE_A_FIT");
    f.db.failOn="SELECT target_issue FROM (";
    try{FixedResearchWindowR932.maintain(f.store);throw new AssertionError("SECOND_CAL_FAULT_NOT_REACHED");}catch(ResearchWorkBudgetR927.Yield expected){check(true,"SECOND_SETTLEMENT_FAULT_REACHED_AFTER_RESERVED_FIT_SLOT");}
   }
   check(count(d,CandidateCalibrationR932.MODEL)==(fixed?1:0),fixed?"READY_FIT_PUBLISHED_IN_NEXT_RESERVED_SLOT_DESPITE_SLOW_SETTLEMENT":"BASELINE_REPRODUCED_READY_FIT_STARVATION");
   if(fixed){check(scalar(d,"SELECT state FROM "+CandidateCalibrationR932.STUDY).equals("FROZEN"),"READY_TO_FROZEN_REAL_MODEL_PUBLISHED");CandidateCalibrationR932.fitOne(f.store);check(count(d,CandidateCalibrationR932.MODEL)==1,"FIT_PUBLISH_IDEMPOTENT");}
  }
  System.out.println("ROOT_BLOCKERS_"+(fixed?"FIXED":"BASELINE_REPRODUCED")+"|checks="+checks+"|device=NOT_RUN");
 }
 static final class Fixture implements AutoCloseable{
  final FaultDb db;final DbStore store;
  Fixture(String path)throws Exception{db=new FaultDb(path);store=HostProductR974.store(db);HostProductR974.schema(db,store);}
  public void close(){db.shutdown();}
 }
}
