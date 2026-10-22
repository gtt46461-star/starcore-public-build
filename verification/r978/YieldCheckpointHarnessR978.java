package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import org.json.JSONObject;import java.util.concurrent.atomic.AtomicBoolean;
public final class YieldCheckpointHarnessR978 {
 public static void main(String[] args)throws Exception {
  android.os.SystemClock.setForTest(1000000L,0L);SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
   DbStore s=DevelopmentHarnessR978.setup(db);db.beginTransaction();String id;try{
    for(int i=1;i<=320;i++)DevelopmentHarnessR978.draw(db,i);id=DevelopmentHarnessR978.seed(db);
    db.execSQL("INSERT INTO "+DirectedResearchCreationR932.ATTEMPT+" VALUES('host-receipt','algorithm','OPEN','','','978','202609210320','CREATED',?,'s','','{}',1)",new Object[]{"algorithm:"+id});db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   AtomicBoolean triggered=new AtomicBoolean();android.os.SystemClock.armDirectDevelopmentCheckpoint(2,()->{triggered.set(true);android.os.SystemClock.advanceForTest(2000L);});
   String status;try{status=ResearchDynamicBootstrapR922.pulse(s,1000).status;}finally{RealtimePressureGateR811.endAnalysis();}
   JSONObject job=new JSONObject(s.getState("r959_development_job_"+id,"{}"));
   DevelopmentHarnessR978.check(triggered.get(),"BUDGET_EXHAUSTION_INJECTED_AFTER_ONE_COMPLETED_LABEL");
   DevelopmentHarnessR978.check(job.optInt("n")==1,"COMPLETED_LABEL_PERSISTS_ON_NEXT_CHECKPOINT_YIELD:"+status);
   DevelopmentHarnessR978.check(AlgorithmGenomeVmR908.State.decode(job.getString("state")).updates==job.getInt("n"),"SAVED_STATE_AND_CURSOR_AGREE");
   System.out.println("R978_YIELD_CHECKPOINT failures="+DevelopmentHarnessR978.fail+" fault=ANDROID_CLOCK_CALLBACK_NOT_PRODUCT_METHOD_SUBSTITUTION");if(DevelopmentHarnessR978.fail>0)throw new AssertionError("yield checkpoint failed");
  }finally{db.shutdown();}
 }
}
