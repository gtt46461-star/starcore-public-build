package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import android.database.Cursor;import com.ethk3.novaonepro.r952.*;import java.io.*;
/** Resume the persisted automatic-owner fixture after a host/session interruption. No state injection. */
public final class ContinueResearchR65 {
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 static void need(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS|"+s);}
 public static void main(String[]a)throws Exception{
  int goal=a.length>3?Integer.parseInt(a[3]):1;File root=new File(a[1]);try(SQLiteDatabase db=new SQLiteDatabase(a[0],new File(root,"database.db").getPath())){
   DbStore store=HostProductR974.store(db);android.app.Application app=new android.app.Application(new File(root,"app"),new File(a[2]));
   HostProductR974.set(store.coordinatorR36(),"app",app);HostProductR974.set(store,"researchAppCP12",app);HostProductR974.set(store,"researchInputRootCP11",new File(root,"cas"));
   long before=count(db,"research_training_feature_row_cp11");need(before>0,"PERSISTED_PARTIAL_TRAINING_PRESENT");
   need(AtomCatalogImportCP12.completeCP12(db)&&ModernSeedFactoryCP12.completeCP12(db),"AUTOMATIC_CATALOG_IDENTITIES_RETAINED");
   long started=System.nanoTime();int calls=0;
   while(++calls<=6000&&(System.nanoTime()-started)<1_200_000_000_000L){
    ResearchLearningContinuityR912.Result result=ResearchLearningContinuityR912.pulse(store,1000);
    if(calls%20==0)System.out.println("RESUME|calls="+calls+"|features="+count(db,"research_training_feature_row_cp11")+"|receipts="+count(db,"research_development_receipt_v2")+"|frozen="+count(db,"r967_frozen_evaluation_v2")+"|"+result.status);
    if(count(db,"research_development_receipt_v2")>=goal&&currentFrozen(db)>=goal)break;
    needContinuation(store);Thread.sleep(ResearchLearningContinuityR912.nextContinuationDelayMs(store));
   }
   need(count(db,"research_training_feature_row_cp11")>=before,"PERSISTED_FEATURES_NEVER_RESTARTED");
   need(count(db,"research_development_receipt_v2")>=goal,"AUTOMATIC_DEVELOPMENT_RECEIPT");need(currentFrozen(db)>=goal,"AUTOMATIC_FROZEN_MODEL_WITH_CURRENT_RULE");
   try(Cursor c=db.rawQuery("SELECT SUM(future_n),SUM(may_create_real) FROM r952_candidate",null)){c.moveToFirst();need(c.getLong(0)==0&&c.getLong(1)==0,"NO_FABRICATED_FUTURE_OR_REAL_AUTHORITY");}
   try(Cursor c=db.rawQuery("SELECT kind,status,last_error,COUNT(*) FROM v57_research_task_r967 GROUP BY kind,status,last_error",null)){while(c.moveToNext())System.out.println("TASK|"+c.getString(0)+"|"+c.getString(1)+"|"+c.getString(2)+"|"+c.getLong(3));}
   try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){need(c.moveToFirst()&&"ok".equals(c.getString(0)),"DATABASE_INTEGRITY");}
   System.out.println("R65_AUTOMATIC_RESTART_PASS|calls="+calls+"|elapsedMs="+(System.nanoTime()-started)/1_000_000+"|scope=SERVICE_CONTINUITY_OWNER_HOST_PAST_DATA_NOT_PHONE_NOT_FUTURE");
  }
 }
 static void needContinuation(DbStore store){if(!ResearchLearningContinuityR912.needsContinuation(store))throw new AssertionError("PERSISTED_WORK_LOST_RETRY");}
 static long currentFrozen(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM r967_frozen_evaluation_v2 WHERE rule_hash=?",new String[]{TrainingCodeIdentityR964.SHA256})){c.moveToFirst();return c.getLong(0);}}
}
