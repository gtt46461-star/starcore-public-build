package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
public final class ParentRetryHarnessR966 {
 static int checks;static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;}
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
   DbStore store=DevelopmentTrainingHarnessR959.init(db);Random random=new Random(1966);String base="202609200001";
   for(int i=0;i<121;i++){DevelopmentTrainingHarnessR959.draw(db,base,random);if(i<120)base=TargetIssueResolver.next(base).targetIssue;}
   AlgorithmGenomeR908 parent=AlgorithmGenomeR908.seed(196601,47,"SYNTHETIC_INITIAL_PARENT");
   db.beginTransaction();try{AlgorithmInventionLabR908.registerDirected(db,parent,base,TargetIssueResolver.next(base).targetIssue,"FIXTURE","SYNTHETIC_DRAWS");ResearchLifecycleR932.enroll(db,"algorithm:"+parent.genomeId,parent.structuralHash,base,"C");db.setTransactionSuccessful();}finally{db.endTransaction();}
   int step=0;for(int period=0;period<30&&step<24;period++){
    String target=TargetIssueResolver.next(base).targetIssue;DeferredEvidenceSnapshotR36.Snapshot surface=ReportedCaptureHarnessR945.snapshot(base,target);byte[] blob=DeferredEvidenceSnapshotR36.encode(surface);
    db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue,job_type,status,created_at,updated_at,record_hash,snapshot_blob,snapshot_hash) VALUES(?,?,?,0,0,?,?,?)",new Object[]{target,"R966_SYNTHETIC_RETRY_TEST","PENDING",surface.recordHash,blob,DeferredEvidenceSnapshotR36.sha256(blob)});
    for(int retry=0;retry<30;retry++){try{ResearchPredrawCaptureR923.captureTarget(store,target,1600);}catch(ResearchWorkBudgetR927.Yield y){}if(ResearchCaptureQueueR946.complete(store,target))break;}
    check(ResearchCaptureQueueR946.complete(store,target),"CAPTURE_COMPLETED:"+step);
    int pos,n1,n2,n3;try(Cursor c=db.rawQuery("SELECT position,n1,n2,n3 FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE genome_id=? AND target_issue=?",new String[]{parent.genomeId,target})){if(!c.moveToFirst()){
      DevelopmentTrainingHarnessR959.draw(db,target,random);
      db.beginTransaction();try{ResearchLifecycleR932.review(db,"algorithm:"+parent.genomeId,target);db.setTransactionSuccessful();}finally{db.endTransaction();}
      base=target;continue;
     }step++;pos=c.getInt(0);n1=c.getInt(1);n2=c.getInt(2);n3=c.getInt(3);}
    DevelopmentTrainingHarnessR959.draw(db,target,random);int actual=1;while(actual==n1||actual==n2||actual==n3)actual++;
    ArrayList<Integer> nums=new ArrayList<Integer>();for(int k=1;k<=10;k++)nums.add(k);Collections.swap(nums,pos-1,actual-1);
    for(int k=1;k<=10;k++)db.execSQL("UPDATE draws SET n"+k+"=? WHERE issue=?",new Object[]{nums.get(k-1),target});
    for(int retry=0;retry<6;retry++)ResearchPredrawSettlementPumpR908.drainBatch(store,32,1600);base=target;
    if(step==12||step==24){
     String result=DirectedResearchCreationR932.maintain(store,"algorithm");check(result.startsWith("CREATED_N0|"),"SAME_PARENT_NEW_PROOF_BLOCK_CREATES_SIBLING:"+step);
     check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT+" WHERE parent_key='algorithm:"+parent.genomeId+"' AND kind='PREDRAW_FAILURE_REPAIR' AND state='CREATED'")==step/12,"PAID_RETRY_FROM_SAME_PARENT");
     db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET status='PRUNED_FIXTURE_ISOLATION' WHERE genome_id<>?",new Object[]{parent.genomeId});
     check(ResearchFailureEvolutionR965.choose(db,base)==null,"SAME_DIAGNOSTIC_LABELS_NOT_REUSED");
    }else if(step>12){check(ResearchFailureEvolutionR965.choose(db,base)==null,"FEWER_THAN_TWELVE_NEW_LABELS_WAIT:"+step);}
   }
   check(DevelopmentTrainingHarnessR959.n(db,"SELECT state_updates FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+parent.genomeId+"'")==24,"24_REAL_SETTLEMENT_UPDATES_ON_SYNTHETIC_DRAWS");
   check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE generation=48")==2,"TWO_DISTINCT_GENERATION_48_PROGRAMS");
   check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"NO_RETRY_CERTIFICATE_FABRICATION");
   System.out.println("R966_PARENT_RETRY_PASS|checks="+checks+"|livePathLabels=24|siblings=2|generation=48|SYNTHETIC_FORCED_MISSES_NOT_PHONE_QUALITY");
  }finally{db.shutdown();}
 }
}
