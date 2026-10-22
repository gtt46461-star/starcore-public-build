package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import java.lang.reflect.*;
import com.ethk3.novaonepro.r952.ResearchDirectorR952;
/** Real POST owner/transactions with synthetic immutable upstream receipts. */
public final class PostBatchR78 {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static final class Db extends SQLiteDatabase {
  int project;boolean pressure;
  Db(String a,String b)throws Exception{super(a,b);}
  @Override public synchronized void execSQL(String sql,Object[]a){
   if(sql.startsWith("INSERT OR IGNORE INTO r952_knowledge")&&pressure&&++project==2){RealtimePressureGateR811.beginAnalysis();ResearchWorkBudgetR927.checkpoint();}
   super.execSQL(sql,a);
  }
 }
 public static void main(String[]a)throws Exception{
  try(Db d=new Db(a[0],a[1])){
   DbStore s=HostProductR974.store(d);RealtimePressureGateR811.resetForTests();
   d.beginTransaction();try{for(int i=0;i<24;i++)PostRetryHarnessR976.seed(d,"batch-"+i,"202610150100",false);d.setTransactionSuccessful();}finally{d.endTransaction();}
   Method m=ResearchDirectorR952.class.getDeclaredMethod("runDirectorSlotR65",DbStore.class,String.class,long.class);m.setAccessible(true);
   ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(5000);
   try{
    int n=(Integer)m.invoke(null,s,"POST",System.nanoTime()+3_000_000_000L);
    ok(n==16,"bounded batch cap: "+n);ok(PostRetryHarnessR976.n(d,"SELECT COUNT(*) FROM r952_knowledge")==16,"one projection per completed intent");
    ok(PostRetryHarnessR976.n(d,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='POST_TRAIN' AND status='COMPLETED'")==16,"atomic ACK count");
    ok((Integer)m.invoke(null,s,"POST",System.nanoTime())==0,"expired slot publishes nothing");
    d.pressure=true;d.project=0;
    try{m.invoke(null,s,"POST",System.nanoTime()+3_000_000_000L);throw new AssertionError("P0 not exercised");}catch(InvocationTargetException e){ok(e.getCause() instanceof ResearchWorkBudgetR927.Yield,"P0 propagates");}finally{RealtimePressureGateR811.endAnalysis();d.pressure=false;}
    ok(PostRetryHarnessR976.n(d,"SELECT COUNT(*) FROM r952_knowledge")==17,"one committed prefix survives P0");
    ok(PostRetryHarnessR976.n(d,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE status='RUNNING'")==0,"interrupted claim rolled back");
    ok((Integer)m.invoke(null,s,"POST",System.nanoTime()+3_000_000_000L)==7,"resume remaining seven");
    ok(PostRetryHarnessR976.n(d,"SELECT COUNT(*) FROM r952_knowledge")==24,"exactly once after resume");
    ok((Integer)m.invoke(null,s,"POST",System.nanoTime()+3_000_000_000L)==0,"empty repeat no duplicate");
    ok(PostRetryHarnessR976.n(d,"SELECT COUNT(*) FROM research_certificate_r929")==0,"no certificate fabricated");
    System.out.println("PASS PostBatchR78 checks="+checks+" cap=16 P0=rollback resume=exactly_once scope=HOST_NOT_PHONE_TIMING");
   }finally{ResearchWorkBudgetR927.leave(scope);}
  }
 }
}
