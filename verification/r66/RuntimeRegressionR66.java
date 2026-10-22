package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;import java.nio.file.*;import org.json.*;
/** Real SQLite and actual product owners; fixtures are synthetic, never live qualifications. */
public final class RuntimeRegressionR66 {
 static int checks;
 static void need(boolean b,String name){if(!b)throw new AssertionError(name);checks++;System.out.println("PASS|"+name);}
 public static void main(String[] args)throws Exception {
  Path root=Path.of(args[1]);Files.createDirectories(root);Path dbfile=root.resolve("regression.db");
  try(SQLiteDatabase db=new SQLiteDatabase(args[0],dbfile.toString())) {
   DbStore store=HostProductR974.store(db);
   db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
   db.beginTransaction();try{ResearchTaskQueueR967.schema(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
   String hash=V51ChecksumEngine.sha256("R66_SYNTHETIC_TASK");
   ResearchTaskQueueR967.Lease lease=store.coordinatorR36().runAtomicWriteOrThrowF01("R66_TEST_CLAIM",d->{
    ResearchTaskQueueR967.enqueue(d,ResearchTaskQueueR967.MAINTENANCE,"lane:13","202610120001","1",hash,"MAINTENANCE_WATERMARK:202610120001");
    return ResearchTaskQueueR967.claimFor(d,ResearchTaskQueueR967.MAINTENANCE,"lane:13",System.currentTimeMillis());
   });need(lease!=null,"ACTUAL_TASK_CLAIMED");
   ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(1);Thread.sleep(5);
   try {
    boolean yielded=false;try{ResearchWorkBudgetR927.checkpoint();}catch(ResearchWorkBudgetR927.Yield expected){yielded=true;}
    need(yielded,"EXPIRED_COMPUTE_REJECTED");
    RealtimePressureGateR811.beginAnalysis();
    try {
     Boolean returned=ResearchWorkBudgetR927.finishLeaseReceiptR66(()->store.coordinatorR36().runAtomicWrite("R967_LANE_RECEIPT",d->ResearchTaskQueueR967.yield(d,lease,lease.cursor,"R66_TEST_PREEMPTION",0),false));
     need(Boolean.TRUE.equals(returned),"PREEMPTED_TASK_LEASE_RETURNED_WITHOUT_45S_WAIT");
     boolean restored=false;try{ResearchWorkBudgetR927.checkpoint();}catch(ResearchWorkBudgetR927.Yield expected){restored=true;}
     need(restored,"COMPUTE_PREEMPTION_STILL_ENFORCED_AFTER_RECEIPT");
    }finally{RealtimePressureGateR811.endAnalysis();}
   }finally{ResearchWorkBudgetR927.leave(scope);}
   ResearchTaskQueueR967.Lease retry=store.coordinatorR36().runAtomicWriteOrThrowF01("R66_TEST_RECLAIM",d->ResearchTaskQueueR967.claimFor(d,ResearchTaskQueueR967.MAINTENANCE,"lane:13",System.currentTimeMillis()));
   need(retry!=null&&retry.generation>lease.generation,"IMMEDIATE_RECLAIM_ADVANCES_FENCE");
   db.execSQL("CREATE TABLE research_lifecycle_r932(candidate_key TEXT PRIMARY KEY,state TEXT,future_n INTEGER,paired_n INTEGER,evidence_hash TEXT)");
   for(String state:new String[]{"EVIDENCE_READY","CHALLENGER","SHADOW","INFLUENCE","CHAMPION","QUALITY_PRUNED","QUARANTINED","RESOURCE_RETIRED"}){
    db.execSQL("INSERT OR REPLACE INTO research_lifecycle_r932 VALUES('r952:fixture',?,300,300,?)",new Object[]{state,hash});
    need(state.equals(ResearchLifecycleR932.observeR952(db,"r952:fixture","202610120002",hash,301)),"ONLINE_LABEL_PRESERVES_"+state);
    try(Cursor c=db.rawQuery("SELECT paired_n,evidence_hash FROM research_lifecycle_r932",null)){need(c.moveToFirst()&&c.getLong(0)==300&&hash.equals(c.getString(1)),"PAIRED_EVIDENCE_PRESERVED_"+state);}
   }
   need(TrainingContinuationR973.compatible("233fbf6bf3b2313d56576c9c0a7953323a76d0e5f6a3d9fc6297b88f98648e09"),"EXACT_R65_TRAINING_CONTINUES");
   need(!TrainingContinuationR973.compatible(hash),"UNKNOWN_TRAINING_RULE_REJECTED");
   db.execSQL("CREATE TABLE r952_candidate(candidate_id TEXT PRIMARY KEY,state TEXT,reason TEXT,created_issue TEXT)");
   db.beginTransaction();try{for(int i=0;i<321;i++)db.execSQL("INSERT INTO r952_candidate VALUES(?,'OBSERVING',?,'202610120001')",new Object[]{"candidate-"+i,"完整证据".repeat(700)+"END-"+i});db.setTransactionSuccessful();}finally{db.endTransaction();}
   db.execSQL("INSERT INTO app_state VALUES('secret_fixture','NOT_FOR_REPORT',1)");
  }
  String before=ReportArchiveCP4.hash(dbfile.toFile(),()->true);
  try(SQLiteDatabase db=SQLiteDatabase.openDatabase(dbfile.toString(),null,SQLiteDatabase.OPEN_READONLY)){
   File result=ReportDiagnosticEvidenceR64.write(db,root.toFile(),before,()->true);
   String text=Files.readString(result.toPath());JSONObject report=new JSONObject(text);
   need(!text.contains("NOT_FOR_REPORT"),"UNRELATED_SECRETS_EXCLUDED");
   boolean all=false;JSONArray evidence=report.getJSONArray("recentEvidence");for(int i=0;i<evidence.length();i++){
    JSONObject table=evidence.getJSONObject(i);if(!"r952_candidate".equals(table.getString("table")))continue;
    JSONArray rows=table.getJSONArray("rows");all=rows.length()==321&&table.getLong("exportedRows")==321&&table.getBoolean("complete")&&rows.getJSONObject(320).getString("reason").endsWith("END-320");
   }need(all,"ALL_321_ROWS_AND_LONG_TEXT_EXPORTED");
  }
  need(before.equals(ReportArchiveCP4.hash(dbfile.toFile(),()->true)),"REPORT_DID_NOT_MUTATE_DATABASE");
  System.out.println("R66_REGRESSION_PASS|checks="+checks+"|scope=HOST_REAL_SQLITE_SYNTHETIC_INPUT_NOT_PHONE");
 }
}
