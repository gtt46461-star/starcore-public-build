package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import android.database.Cursor;import java.util.*;
/** Fault injection only at the Android monotonic clock; product arithmetic is never replaced. */
public final class RecoveryHarnessR979 {
 static int checks,failures;
 static void check(boolean ok,String n){checks++;if(!ok)failures++;System.out.println((ok?"PASS ":"FAIL ")+n);}
 static long count(SQLiteDatabase db,String q){try(Cursor c=db.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static void feature(String bridge)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(bridge);try {
   DbStore store=DevelopmentHarnessR978.setup(db);
   for(int i=1;i<=81;i++)DevelopmentHarnessR978.draw(db,i);
   List<Models.Draw> history=new ArrayList<Models.Draw>();for(int i=1;i<=80;i++)history.add(store.getDrawByIssue(String.format(Locale.ROOT,"202609210%03d",i)));
   Models.Draw label=store.getDrawByIssue("202609210081");AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(979,1,"");
   AlgorithmGenomeVmR908.State a=AlgorithmGenomeVmR908.State.decode("");
   AlgorithmGenomeVmR908.State b=AlgorithmGenomeVmR908.State.decode(a.encode());
   String initial=b.encode();
   AlgorithmInventionLabR908.DevelopmentStep expected=AlgorithmInventionLabR908.trainDevelopmentStep(null,g,a,history,label,Long.MAX_VALUE);
   AlgorithmInventionLabR908.DevelopmentStep actual=null;int yields=0;
   for(int call=0;call<12&&actual==null;call++) {
    android.os.SystemClock.interruptDevelopmentAt(30,()->android.os.SystemClock.charge(2000));
    ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(1000);
    try {actual=AlgorithmInventionLabR908.trainDevelopmentStep(store,g,b,history,label,TimeSourceR811.monoNowMs()+900);}
    catch(ResearchWorkBudgetR927.Yield expectedYield){yields++;check(initial.equals(b.encode()),"PARTIAL_FEATURE_OR_SCAN_YIELD_HAS_NO_NUMERIC_UPDATE_"+call);}
    finally{ResearchWorkBudgetR927.leave(scope);android.os.SystemClock.interruptDevelopmentAt(0,null);}
   }
   check(yields>=2,"REPEATED_FEATURE_PREPARATION_INTERRUPTION_INJECTED");
   check(actual!=null,"FEATURE_PREPARATION_RESUMES_INSTEAD_OF_RESTARTING_FOREVER");
   if(actual!=null){check(actual.ordinal==expected.ordinal&&actual.hit==expected.hit&&Double.doubleToLongBits(actual.score)==Double.doubleToLongBits(expected.score),"RESUMED_DECISION_BIT_IDENTICAL_TO_UNINTERRUPTED_FULL1200");check(a.encode().equals(b.encode()),"RESUMED_NUMERIC_STATE_BIT_IDENTICAL");}
   System.out.println("MEASURE feature_yields="+yields+" completed="+(actual!=null)+" updates="+b.updates+" baseline_state_hash="+V51ChecksumEngine.sha256(a.encode()));
  }finally{db.shutdown();}
 }
 static void calibration(String bridge)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(bridge);try{
   DbStore store=HostProductR974.store(db);HostProductR974.schema(db,store);
   store.coordinatorR36().runAtomicWrite("FIXTURE_SCHEMA",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase d){CandidateCalibrationR932.schema(d);}});
   db.beginTransaction();try {
    db.execSQL("INSERT INTO "+CandidateCalibrationR932.STUDY+" VALUES('base','algorithm:gap',241,241,'BLOCKED_INVALID_INPUT','202609210001')");
    db.execSQL("INSERT INTO r975_calibration_failure VALUES('base','algorithm:gap','R969_CAL_EVALUATION_SCHEDULE_GAP',1)");
    String next=CandidateCalibrationR932.restartAfterGap(db,"base","base","algorithm:gap","202609220001");
    check(!next.equals("base"),"GAP_ONLY_NEW_PROSPECTIVE_STUDY_ID");
    check(count(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.STUDY+" WHERE study_hash='base' AND captured_n=241 AND settled_n=241 AND state='BLOCKED_INVALID_INPUT'")==1,"OLD_INVALID_STUDY_PRESERVED_UNCHANGED");
    check(CandidateCalibrationR932.restartAfterGap(db,"base",next,"algorithm:gap","202609220002").equals(next),"NEW_HEAD_NOT_RESTARTED_EVERY_CAPTURE");
    check(count(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)==0&&count(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS)==0,"RESTART_DOES_NOT_MINT_MODEL_OR_COPY_LABELS");
    db.execSQL("INSERT INTO "+CandidateCalibrationR932.STUDY+" VALUES('low','algorithm:low',241,241,'BLOCKED_INVALID_INPUT','202609210001')");
    db.execSQL("INSERT INTO r975_calibration_failure VALUES('low','algorithm:low','LOW_ACCURACY',1)");
    check(CandidateCalibrationR932.restartAfterGap(db,"low","low","algorithm:low","202609220001").equals("low"),"LOW_ACCURACY_NEVER_TRIGGERS_RETRY_SELECTION");
    db.execSQL("INSERT INTO "+CandidateCalibrationR932.STUDY+" VALUES('late','algorithm:late',241,241,'BLOCKED_INVALID_INPUT','202609210001')");
    db.execSQL("INSERT INTO r975_calibration_failure VALUES('late','algorithm:late','R969_CAL_EVALUATION_SCHEDULE_GAP',1)");
    DevelopmentHarnessR978.draw(db,300);
    check(CandidateCalibrationR932.restartAfterGap(db,"late","late","algorithm:late","202609210299").equals("late"),"ALREADY_KNOWN_DRAW_CANNOT_START_CALIBRATION");
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
   check(count(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"RECOVERY_CANNOT_ISSUE_CERTIFICATE");
  }finally{db.shutdown();}
 }
 public static void main(String[] args)throws Exception {
  android.os.SystemClock.setForTest(1000000,0);if(args[1].equals("feature"))feature(args[0]);else calibration(args[0]);
  System.out.println("R979_RECOVERY checks="+checks+" failures="+failures+" device=NOT_RUN");if(failures>0)throw new AssertionError("failures="+failures);
 }
}
