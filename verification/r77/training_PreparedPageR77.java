package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;import java.io.*;import java.nio.file.*;
/** Inject a failed actual feature transaction, then reclaim its task generation. */
public final class training_PreparedPageR77 {
 static int checks;static void need(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 static final class FailingDb extends SQLiteDatabase {
  boolean fail,p0;int writes;
  FailingDb(String bridge,String path)throws Exception{super(bridge,path);}
  @Override public synchronized void execSQL(String sql,Object[]args){if(sql.startsWith("INSERT INTO research_training_feature_row_cp11")){writes++;if(fail){fail=false;if(p0){RealtimePressureGateR811.beginAnalysis();ResearchWorkBudgetR927.checkpoint();}throw new IllegalStateException("R77_INJECTED_FEATURE_WRITE_FAILURE");}}super.execSQL(sql,args);}
 }
 static long count(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM research_training_feature_row_cp11",null)){c.moveToFirst();return c.getLong(0);}}
 static ResearchTaskQueueR967.Lease claim(DbStore s,String candidate){return s.coordinatorForResearchR952().runAtomicWrite("TEST_R77_CLAIM",d->ResearchTaskQueueR967.claimFor(d,"TRAIN_CANDIDATE",candidate,System.currentTimeMillis()),null);}
 public static void main(String[]a)throws Exception{
  try(FailingDb d=new FailingDb(a[0],a[1])){
   DbStore s=HostProductR974.store(d);HostProductR974.set(s,"researchAppCP12",new android.content.Context(new File(a[2]),new File(a[3])));HostProductR974.set(s,"researchInputRootCP11",new File(a[2],"research-input-v2"));RealtimePressureGateR811.resetForTests();
   String candidate;try(Cursor c=d.rawQuery("SELECT candidate_id FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' ORDER BY created_at LIMIT 1",null)){need(c.moveToFirst(),"candidate");candidate=c.getString(0);}
   long before=count(d),memory=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used();
   ResearchTaskQueueR967.Lease first=claim(s,candidate);need(first!=null,"first_lease");d.fail=true;d.p0=a.length>4&&a[4].equals("p0");ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(4000);
   try{DevelopmentProductionCP12.prepareBatch(s,first,System.nanoTime()+3_000_000_000L);throw new AssertionError("failure_not_injected");}catch(DevelopmentProductionCP12.Wait expected){need(expected.reason.equals("TRAIN_FEATURE_COMMIT_FAILED"),"write_failure_preserved:"+expected.reason);}catch(ResearchWorkBudgetR927.Yield expected){need(d.p0,"P0_yield");}finally{if(d.p0)RealtimePressureGateR811.endAnalysis();ResearchWorkBudgetR927.leave(scope);}
   need(count(d)==before,"rollback_has_no_feature_receipt");need(d.writes==1,"first_write_failure");
   s.coordinatorForResearchR952().runAtomicWrite("TEST_R77_YIELD",db->ResearchTaskQueueR967.yield(db,first,first.cursor,"FIXTURE_RETRY",0),false);
   ResearchTaskQueueR967.Lease next=claim(s,candidate);need(next!=null&&next.generation>first.generation,"new_generation");
   Path raw=new File(s.researchInputRootCP11(),next.inputHash+".scdev").toPath(),park=Path.of(raw.toString()+".probe");Files.move(raw,park);
   try {need(DevelopmentProductionCP12.prepareBatch(s,next,System.nanoTime()+3_000_000_000L)==null,"cached_page_committed_without_redecode");} finally {Files.move(park,raw);}
   long rows=count(d)-before;need(rows>16&&rows<=64,"bounded_useful_page:"+rows);need(RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used()==memory,"cached_memory_released");
   try(Cursor c=d.rawQuery("SELECT status,cursor FROM v57_research_task_r967 WHERE task_id=?",new String[]{next.id})){need(c.moveToFirst()&&c.getString(0).equals("YIELDED")&&c.getString(1).startsWith("FEATURES|"),"same_owner_atomic_cursor");}
   System.out.println("PASS checks="+checks+" rows="+rows+" generation="+next.generation+" memory="+memory);
  }
 }
}
