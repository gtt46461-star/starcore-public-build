package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import com.ethk3.novaonepro.r952.*;import java.io.*;import java.nio.file.*;
/** Existing service continuity owner on real past data and exact APK assets; no forced qualifications. */
public final class AutomaticResearchR65 {
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 static void need(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println("PASS|"+s);}
 static final class TrackedDb extends SQLiteDatabase {int starts;TrackedDb(String bridge,String path)throws Exception{super(bridge,path);}public void beginTransaction(){starts++;super.beginTransaction();}}
 public static void main(String[]a)throws Exception{
  File root=new File(a[2]);root.mkdirs();File file=new File(root,"database.db");Files.copy(Path.of(a[1]),file.toPath());
  try(TrackedDb db=new TrackedDb(a[0],file.getPath())){
   DbStore store=HostProductR974.store(db);android.app.Application app=new android.app.Application(new File(root,"app"),new File(a[3]));
   HostProductR974.set(store.coordinatorR36(),"app",app);HostProductR974.set(store,"researchAppCP12",app);HostProductR974.set(store,"researchInputRootCP11",new File(root,"cas"));
   need(count(db,"research_atom_spec_v2")==0&&count(db,"research_training_protocol_v2")==0,"COLD_RESEARCH_START");
   if(a.length>4&&"ui".equals(a[4])){
    int before=db.starts;RuntimeTruthSnapshotR906 ui=RuntimeTruthSnapshotR906.captureSummary(store);
    need(db.starts==before,"UI_REFRESH_NEVER_OPENS_WRITER_TRANSACTION");need(ui.readError.isEmpty(),"UI_REFRESH_READS_REAL_TABLES");
    need(ui.researchSummary.contains("候选="),"CURRENT_OWNER_COUNTS_VISIBLE");return;
   }
   RealtimePressureGateR811.beginAnalysis();try{
    need(ResearchLearningContinuityR912.needsContinuation(store),"PREEMPTION_PRESERVES_RETRY");
    need(ResearchLearningContinuityR912.nextContinuationDelayMs(store)>=1000,"PREEMPTION_BACKS_OFF");
    int before=db.starts;need(ResearchLearningContinuityR912.pulse(store,1000).status.contains("YIELD"),"PREEMPTED_OWNER_YIELDS");need(db.starts==before,"PREEMPTED_OWNER_DOES_NOT_WRITE");
   }finally{RealtimePressureGateR811.endAnalysis();}
   need(ResearchRuntimeSqlGateR911.verifyAndPublish(store).passed,"SERVICE_SQL_GATE_PASSES");
   long started=System.nanoTime();int calls=0;
   while(++calls<=6000&&(System.nanoTime()-started)<1_200_000_000_000L){
    ResearchLearningContinuityR912.Result result=ResearchLearningContinuityR912.pulse(store,1000);
    if(calls%20==0)System.out.println("AUTO|calls="+calls+"|atoms="+count(db,"research_atom_spec_v2")+"|molecules="+count(db,"research_molecule_seed_v2")+"|protocols="+count(db,"research_training_protocol_v2")+"|receipts="+count(db,"research_development_receipt_v2")+"|"+result.status);
    if(count(db,"research_development_receipt_v2")>=1)break;
    needQuietContinuation(store,calls);
    Thread.sleep(ResearchLearningContinuityR912.nextContinuationDelayMs(store));
   }
   need(AtomCatalogImportCP12.completeCP12(db),"AUTOMATIC_ATOM_CATALOG_COMPLETE");need(ModernSeedFactoryCP12.completeCP12(db),"AUTOMATIC_MOLECULE_CATALOG_COMPLETE");
   need(count(db,"research_family_creation_cp12")>0,"AUTOMATIC_FAMILY_CREATION");need(count(db,"research_training_protocol_v2")>0,"AUTOMATIC_TRAINING_REGISTERED");
   need(count(db,"research_training_feature_row_cp11")>0,"AUTOMATIC_FEATURES_MATERIALIZED");need(count(db,"research_development_receipt_v2")>0,"AUTOMATIC_DEVELOPMENT_RECEIPT");
   try(Cursor c=db.rawQuery("SELECT SUM(future_n),SUM(may_create_real) FROM r952_candidate",null)){c.moveToFirst();need(c.getLong(0)==0&&c.getLong(1)==0,"NO_FABRICATED_FUTURE_OR_REAL_AUTHORITY");}
   try(Cursor c=db.rawQuery("SELECT kind,status,COUNT(*) FROM v57_research_task_r967 GROUP BY kind,status",null)){while(c.moveToNext())System.out.println("TASK|"+c.getString(0)+"|"+c.getString(1)+"|"+c.getLong(2));}
   try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){need(c.moveToFirst()&&"ok".equals(c.getString(0)),"DATABASE_INTEGRITY");}
   System.out.println("R65_AUTOMATIC_OWNER_PASS|calls="+calls+"|elapsedMs="+(System.nanoTime()-started)/1_000_000+"|scope=SERVICE_CONTINUITY_OWNER_HOST_PAST_DATA_NOT_PHONE_NOT_FUTURE");
  }
 }
 static void needQuietContinuation(DbStore store,int call){if(!ResearchLearningContinuityR912.needsContinuation(store))throw new AssertionError("AUTOMATIC_CHAIN_LOST_WAKEUP_AT_"+call);}
}
