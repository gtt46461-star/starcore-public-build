package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import java.io.*;import java.nio.file.*;import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
public final class RepairReportR64 {
 static int checks;
 static void need(boolean b,String s){if(!b)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
 static long count(SQLiteDatabase db,String t){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+t,null)){c.moveToFirst();return c.getLong(0);}}
 public static void main(String[] a)throws Exception{
  File job=new File(a[2]);job.mkdirs();File data=new File(job,"snapshot.db");Files.copy(Path.of(a[1]),data.toPath());
  SQLiteDatabase db=new SQLiteDatabase(a[0],data.getPath());
  DbStore store=HostProductR974.store(db);DatabaseCoordinator owner=store.coordinatorR36();
  HostProductR974.set(owner,"r811ResearchSchemaReady",new AtomicBoolean(false));
  long draws=count(db,"draws"),shadow=count(db,"v57_shadow_observation_r35");
  ResearchWorkBudgetR927.Scope budget=ResearchWorkBudgetR927.enter(2000);
  RealtimePressureGateR811.beginAnalysis();boolean yielded=false;
  try{SchemaDomainCoordinatorR926.runStep(db,SchemaDomainCoordinatorR926.researchContract(),"R64_INTERRUPTION_NEGATIVE",x->DbHelper.ensureR906ResearchSchema(x));}
  catch(ResearchWorkBudgetR927.Yield e){yielded=true;}
  finally{RealtimePressureGateR811.endAnalysis();ResearchWorkBudgetR927.leave(budget);}
  need(yielded,"PREEMPTED_SCHEMA_THROWS");
  need(!SchemaDomainCoordinatorR926.stepComplete(db,SchemaDomainCoordinatorR926.researchContract(),"R64_INTERRUPTION_NEGATIVE"),"PREEMPTED_SCHEMA_NEVER_COMPLETED");
  boolean ready=false;int pulses=0;long maxPulseMs=0;boolean fastContinuation=false;
  need(ResearchLearningContinuityR912.needsContinuation(store),"UNFINISHED_SCHEMA_SCHEDULES_CONTINUATION");
  while(!ready&&pulses++<400){long tick=System.nanoTime();budget=ResearchWorkBudgetR927.enter(2000);try{ready=store.ensureResearchSidecarR811();}finally{ResearchWorkBudgetR927.leave(budget);}maxPulseMs=Math.max(maxPulseMs,(System.nanoTime()-tick)/1000000L);if(pulses<8||pulses%10==0||ready)System.out.println("PULSE "+pulses+" "+ResearchSchemaCoordinatorR926.summary());
   if(!ready&&ResearchSchemaCoordinatorR926.summary().startsWith("INITIALIZING|")) {
    need(ResearchLearningContinuityR912.nextContinuationDelayMs(store)==50L,"SUCCESSFUL_SCHEMA_UNIT_CONTINUES_WITHOUT_MISSING_QUEUE_DELAY");fastContinuation=true;
    RealtimePressureGateR811.beginAnalysis();try{need(ResearchLearningContinuityR912.nextContinuationDelayMs(store)>=1000L,"SCHEMA_CONTINUATION_RESPECTS_CRITICAL_PRESSURE");}finally{RealtimePressureGateR811.endAnalysis();}
   }
   need(owner.runWrite("R64_CRITICAL_INTERLEAVE",x->Boolean.valueOf(count(x,"draws")==draws),false),"CRITICAL_WRITER_AVAILABLE_BETWEEN_SCHEMA_UNITS");}
  need(fastContinuation,"INCREMENTAL_BOOTSTRAP_CONTINUATION_EXERCISED");
  need(ready,"ACTUAL_REPORT_UPGRADE_REACHES_READY");
  System.out.println("MIGRATION_METRICS|pulses="+pulses+"|maxPulseMs="+maxPulseMs);
  need(count(db,"draws")==draws&&count(db,"v57_shadow_observation_r35")==shadow,"HISTORY_AND_SHADOW_PRESERVED");
  need(count(db,"r952_candidate")==0,"MIGRATION_DOES_NOT_FABRICATE_CANDIDATES");
  need(count(db,"research_new_asset_genome_v41")==0,"GENESIS_SCHEMA_EXISTS_WITHOUT_FAKE_ALGORITHMS");
  try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){need(c.moveToFirst()&&"ok".equals(c.getString(0)),"INTEGRITY_OK");}
  System.out.println("PRE_CLOSE_BYTES="+data.length()+"|tx="+db.inTransaction());
  try(Cursor c=db.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)",null)){while(c.moveToNext())System.out.println("CHECKPOINT="+c.getString(0)+","+c.getString(1)+","+c.getString(2));}
  db.close();
  System.out.println("POST_CLOSE_BYTES="+data.length());
  SQLiteDatabase copy=SQLiteDatabase.openDatabase(data.getPath(),null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS);
  try(Cursor c=copy.rawQuery("PRAGMA integrity_check",null)){need(c.moveToFirst()&&"ok".equals(c.getString(0)),"CLOSED_SNAPSHOT_INTEGRITY");}
  JSONObject observation=new JSONObject(Files.readString(Path.of(a[3],"runtime-observation.json")));
  JSONObject source=new JSONObject(Files.readString(Path.of(a[3],"source-manifest.json"))).put("reportProfile","COMPACT_DIAGNOSTIC_R64");
  String hash=ReportArchiveCP4.hash(data,()->true);
  try(ReportReadContextCP4 context=new ReportReadContextCP4(copy,"host-r64-report",hash,System.currentTimeMillis(),new ReportReadContextCP4.RuntimeObservationCP4(observation.toString()),new ReportReadContextCP4.SourceManifestCP4(source.toString()),new android.os.CancellationSignal(),3L<<20)){
   ReportCatalogCP4.Result result=ReportCatalogCP4.write(context,job,(phase,n)->{});
   ReportCatalogCP4.writeReadableSummary(job,result,()->true);
   ReportDiagnosticEvidenceR64.write(copy,job,hash,()->true);
   need(result.checks==96,"ALL_96_CHECKS_EXECUTED");
   System.out.println("REPORT_METRICS|checks="+result.checks+"|errors="+result.queryErrors+"|complete="+result.checksComplete+"|passed="+result.allChecksPassed);
   need(result.queryErrors==0,"ZERO_DIAGNOSTIC_QUERY_ERRORS");
   need(!result.allChecksPassed,"NO_FALSE_ALL_FUNCTIONS_PASS");
   boolean observedFailure=false;
   for(File page:result.indexFiles)for(String line:Files.readAllLines(page.toPath())){JSONObject row=new JSONObject(line);if("check_result".equals(row.optString("kind"))&&"RSEC-078".equals(row.optString("id")))observedFailure="FAIL".equals(row.optString("health"))&&"RESEARCH_SCHEMA_NOT_READY".equals(row.optString("state"));}
   need(observedFailure,"ORIGINAL_SCHEMA_FAILURE_NOT_HIDDEN_BY_LATEST_PIPELINE_PASS");
  }copy.close();
  new JSONObject(Files.readString(new File(job,"DIAGNOSTIC_SCOPE.json").toPath()));
  System.out.println("R64_REPORT_REPAIR_PASS|checks="+checks+"|scope=USER_REPORT_COPY_HOST_SQLITE_NOT_PHONE");
 }
}
