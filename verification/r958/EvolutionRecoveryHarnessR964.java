package com.ethk3.novaonepro.r952;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
/** Reproduce the old no-new-child state using actual training and a controlled insert refusal. */
public final class EvolutionRecoveryHarnessR964 {
 static int checks;static void check(boolean x,String n){if(!x)throw new AssertionError(n);checks++;System.out.println("PASS "+n);}
 static long count(SQLiteDatabase db,String q){return FailureRepairHarnessR958.count(db,q);}
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
   DbStore store=new DbStore(db);
   db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER,fetch_time INTEGER)");
   db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT PRIMARY KEY,status TEXT,settlement_hash TEXT,completed_at INTEGER)");
   String base="202609190001";long clock=System.currentTimeMillis()-10000000;
   for(int i=0;i<120;i++){FailureRepairHarnessR958.draw(db,base,i%10+1,clock+i*1000);if(i<119)base=TargetIssueResolver.next(base).targetIssue;}
   for(int i=0;i<240;i++){
    String target=TargetIssueResolver.next(base).targetIssue;
    checkCapture(store,target);
    if(i==120)db.execSQL("CREATE TRIGGER reproduce_old_duplicate BEFORE INSERT ON r952_candidate WHEN (SELECT COUNT(*) FROM r952_candidate)>=2 BEGIN SELECT RAISE(IGNORE); END");
    int mask=(int)count(db,"SELECT prediction_mask FROM r952_candidate_predraw WHERE target_issue='"+target+"'");
    boolean hit=i>=150&&i<220;int actual=1;while(AppConfig.maskContainsNumber(mask,actual)!=hit)actual++;
    long at=System.currentTimeMillis()+1;FailureRepairHarnessR958.draw(db,target,actual,at);String hash=V51ChecksumEngine.sha256("TERMINAL|"+target);
    db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED',?,?)",new Object[]{target,hash,at+1});
    ResearchDirectorR952.onSettlementCompleted(store,store.getDrawByIssue(target),hash,at+1);
    if(!ResearchOutboxR952.drainOne(store))throw new AssertionError("ACTUAL_TRAINING_COMMIT:"+i);base=target;
   }
   check(count(db,"SELECT COUNT(*) FROM r952_candidate")==2,"OLD_NO_NEW_CHILD_STATE_REPRODUCED_WITH_240_REAL_UPDATES");
   db.execSQL("DROP TRIGGER reproduce_old_duplicate");
   check(ResearchDirectorR952.capturePredraw(store,TargetIssueResolver.next(base).targetIssue,V51ChecksumEngine.sha256("RECOVERY|"+TargetIssueResolver.next(base).targetIssue),8)==0,"ALL_EXISTING_LEARNERS_FROZEN_NO_NEW_ONLINE_CAPTURE");
   check(count(db,"SELECT COUNT(*) FROM r958_frozen_candidate")==2,"BOTH_MODELS_FROZEN_FOR_INDEPENDENT_EVALUATION");
   check(count(db,"SELECT COUNT(*) FROM r952_research_outbox WHERE state<>'COMPLETED'")==0,"NO_NEW_TRAINING_LABEL_CAN_TRIGGER_EVOLUTION");
   check(ResearchDirectorR952.pulse(store,1200)==0,"MAINTENANCE_RECOVERY_DOES_NOT_FABRICATE_TRAINING");
   check(count(db,"SELECT COUNT(*) FROM r952_candidate")==3&&count(db,"SELECT COUNT(*) FROM r952_candidate WHERE future_n=0")==1,"IDLE_MAINTENANCE_CREATES_NOVEL_NEXT_GENERATION");
   check(store.getState("r964_evolution_maintenance","").contains("CHILD_REGISTERED"),"IDLE_RECOVERY_REASON_EXPOSED");
   ResearchDirectorR952.pulse(store,1200);
   check(count(db,"SELECT COUNT(*) FROM r952_candidate")==3,"REPEATED_IDLE_MAINTENANCE_IS_IDEMPOTENT");
   checkCapture(store,TargetIssueResolver.next(base).targetIssue);
   check(count(db,"SELECT COUNT(*) FROM r952_candidate_predraw WHERE target_issue='"+TargetIssueResolver.next(base).targetIssue+"'")==1,"RECOVERED_CHILD_REENTERS_REAL_PREDRAW_TRAINING_PATH");
   System.out.println("R964_EVOLUTION_RECOVERY_PASS|checks="+checks+"|ACTUAL_VM_SQLITE_SYNTHETIC_NO_NEW_LABEL_RECOVERY");
  }finally{db.shutdown();}
 }
 static void checkCapture(DbStore store,String target){if(ResearchDirectorR952.capturePredraw(store,target,V51ChecksumEngine.sha256("RECOVERY|"+target),8)!=1)throw new AssertionError("CAPTURE:"+target);}
}
