package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
/** Runs actual maintenance owners with measured SQL count and explicit per-SQL cost injection. */
public final class ThroughputHarnessR979 {
 static int checks,failures;
 static void check(boolean ok,String name){checks++;if(!ok)failures++;System.out.println((ok?"PASS ":"FAIL ")+name);}
 static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static void seed(SQLiteDatabase db,String issue,int offset,int n){
  db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,open_time,fetch_time) VALUES(?,1,2,3,4,5,6,7,8,9,10,0,1,1)",new Object[]{issue});
  for(int i=offset;i<offset+n;i++){
   String id="cost-study-"+i,key="algorithm:cost-"+i;
   db.execSQL("INSERT INTO "+CandidateCalibrationR932.STUDY+" VALUES(?,?,1,0,'COLLECTING',?)",new Object[]{id,key,issue});
   db.execSQL("INSERT INTO "+CandidateCalibrationR932.OBS+"(study_hash,target_issue,candidate_key,structure_hash,bundle_hash,selection_hash,raw_score,position,n1,n2,n3,prediction_hash,frozen_at) VALUES(?,?,?,'s','b','p',0.5,1,1,2,3,'fixture',0)",new Object[]{id,issue,key});
  }
 }
 public static void main(String[] args)throws Exception{
  android.os.SystemClock.setForTest(1000000,0);SQLiteDatabase db=new SQLiteDatabase(args[0]);
  try{
   DbStore store=HostProductR974.store(db);HostProductR974.schema(db,store);
   store.coordinatorR36().runAtomicWrite("TEST_WARM_SCHEMA",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase x){FixedResearchWindowR932.schema(x);}});
   db.resetCounts();store.coordinatorR36().runAtomicWrite("TEST_SECOND_SCHEMA",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase x){FixedResearchWindowR932.schema(x);}});
   System.out.println("MEASURE repeated_schema_sql="+db.sqlCount+" ddl="+db.ddlCount);
   check(db.ddlCount==0,"WARM_SCHEMA_NOT_REEXECUTED_EACH_CALLBACK");
   seed(db,"202609220100",0,40);seed(db,"202609220101",40,40);seed(db,"202609220102",80,40);
   db.chargeMs=5;long before=0;int progress=0;
   for(int i=0;i<10;i++){
    ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(350);String status="";
    try{status=FixedResearchWindowR932.maintainReviewed(store);}catch(ResearchWorkBudgetR927.Yield y){status="YIELD:"+y.getMessage();}finally{ResearchWorkBudgetR927.leave(scope);}
    long n=count(db,"SELECT COALESCE(SUM(settled_n),0) FROM "+CandidateCalibrationR932.STUDY);
    if(n>before)progress++;before=n;System.out.println("MEASURE pulse="+i+" settled="+n+" status="+status);
   }
   check(before>0,"CALIBRATION_PROGRESS_UNDER_350MS_SQL_COST_BUDGET");
   check(progress>=3,"SUCCESSIVE_PULSES_PERSIST_LABELS_NOT_ROLLBACK_ENTIRE_PAGE");
   long settled=before;db.chargeMs=0;
   for(int i=0;i<50&&count(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS+" WHERE hit=-1")>0;i++)FixedResearchWindowR932.maintainReviewed(store);
   check(count(db,"SELECT SUM(settled_n) FROM "+CandidateCalibrationR932.STUDY)==120,"ALL_120_LABELS_DRAIN_EXACTLY_ONCE");
   check(count(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"NO_CERTIFICATE_FABRICATED_FROM_SETTLEMENT_FIXTURE");
   System.out.println("R979_THROUGHPUT checks="+checks+" failures="+failures+" injected_sql_ms=5 device=NOT_RUN");
  }finally{db.shutdown();}
  if(failures>0)throw new AssertionError("failures="+failures);
 }
}
