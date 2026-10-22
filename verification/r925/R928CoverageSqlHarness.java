package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
public final class R928CoverageSqlHarness {
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0]);
  try{
   db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER)");
   db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue TEXT PRIMARY KEY,pre_draw_frozen INTEGER,decision_action TEXT,authorization_hash TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER)");
   db.execSQL("CREATE TABLE "+DbHelper.TABLE_TRADES+"(target_issue TEXT,action TEXT,source_type TEXT,is_realtime_real INTEGER,stat_scope TEXT,authorization_valid INTEGER,authorized_mode TEXT,contract_valid INTEGER,authorization_id TEXT,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER)");
   db.execSQL("CREATE TABLE "+ControlledCandidateOpportunityAuditR922.TABLE+"(target_issue TEXT)");
   db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_ANALYSIS_REQUEST+"(target_issue TEXT,status TEXT,error_code TEXT,last_stage TEXT)");
   for(int i=1;i<=30;i++)db.execSQL("INSERT INTO draws VALUES(?,0)",new Object[]{String.format("20260910%04d",i)});
   DbStore store=new DbStore(db);
   PredictionGenerationCoverageR923.Report r=PredictionGenerationCoverageR923.capture(store);
   if(!"OK".equals(r.readStatus)||r.windows[0].draws!=30||r.windows[0].validReal!=0||!r.summary().contains("realPerDraw=0.00%"))throw new AssertionError(r.summary());
   for(int i=1;i<=30;i++){
    String issue=String.format("20260910%04d",i);boolean real=i<=5;
    db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" VALUES(?,1,?,'auth',1,1,2,3)",new Object[]{issue,real?"BET":"SHADOW"});
    if(real)db.execSQL("INSERT INTO "+DbHelper.TABLE_TRADES+" VALUES(?,'BET','REALTIME',1,'REAL_TRADE',1,'BET',1,'auth',1,1,2,3)",new Object[]{issue});
   }
   r=PredictionGenerationCoverageR923.capture(store);
   if(r.windows[0].validReal!=5||r.windows[0].shadowObservations!=25||!r.summary().contains("nonREALShadowCoverage=100.00%"))throw new AssertionError(r.summary());
   db.execSQL("UPDATE "+DbHelper.TABLE_TRADES+" SET source_type='UNKNOWN' WHERE target_issue='202609100001'");
   r=PredictionGenerationCoverageR923.capture(store);
   if(r.windows[0].validReal!=4||!r.summary().contains("minimum15=NOT_MET")||!r.summary().contains("invalidREALProvenance=1"))throw new AssertionError(r.summary());
   System.out.println("R928_COVERAGE_SQL_PASS|ZERO_PREDICTIONS|SHADOW_COMPLETE|BAD_SOURCE_EXCLUDED|FULL_DRAW_DENOMINATOR");
  }finally{db.shutdown();}
 }
}
