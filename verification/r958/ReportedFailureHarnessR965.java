package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.*;
import org.json.*;
/** Uploaded definitions and actual settled diagnostic labels. Additional draws are explicitly synthetic. */
public final class ReportedFailureHarnessR965 {
 static int checks;
 static void check(boolean x,String label){if(!x)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
 static long n(SQLiteDatabase d,String q){return DevelopmentTrainingHarnessR959.n(d,q);}
 public static void main(String[] args)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try {
   DbStore store=DevelopmentTrainingHarnessR959.init(db);
   JSONObject report=new JSONObject(new String(Files.readAllBytes(Paths.get("verification/r958/reported_r964_failure.json")),"UTF-8"));
   for(String t:new String[]{DbHelper.TABLE_R908_ALGORITHM_GENOME,ResearchLifecycleR932.TABLE,DirectedResearchCreationR932.ATTEMPT,CandidateCalibrationR932.OBS,CandidateExecutionAuditR932.TABLE})ReportedWorldHarnessR964.table(db,report,t);
   check(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE generation=1")==28&&n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE generation=2")==2,"ACTUAL_R964_REPORT_28_FIRST_2_SECOND_GENERATION");
   check(n(db,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.GROUP)==0&&n(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.ELITE)==0,"ACTUAL_REPAIR_GROUP_AND_ELITE_EMPTY");
   String history="202609190001";for(int h=0;h<320;h++){DevelopmentTrainingHarnessR959.draw(db,history,new Random(1965+h));history=TargetIssueResolver.next(history).targetIssue;}
   String base="202609200280";DevelopmentTrainingHarnessR959.draw(db,base,new Random(965));
   String result=DirectedResearchCreationR932.maintain(store,"algorithm");check(result.startsWith("CREATED_N0|"),"ACTUAL_REPORT_FAILURES_CREATE_CHILD_WITHOUT_CERTIFICATE_OR_ELITE");
   String childId="",parentId="";JSONObject plan=null;
   try(Cursor c=db.rawQuery("SELECT child_key,parent_key,plan_json FROM "+DirectedResearchCreationR932.ATTEMPT+" WHERE kind='PREDRAW_FAILURE_REPAIR'",null)){check(c.moveToFirst(),"PAID_FAILURE_ATTEMPT_RECORDED");childId=c.getString(0).substring(10);parentId=c.getString(1).substring(10);plan=new JSONObject(c.getString(2));}
   AlgorithmGenomeR908 child,parent;
   try(Cursor c=db.rawQuery("SELECT genome_spec,future_n,state_updates FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{childId})){c.moveToFirst();child=AlgorithmGenomeR908.parse(c.getString(0));check(c.getInt(1)==0&&c.getInt(2)==0,"CHILD_HAS_NO_INHERITED_EVIDENCE_OR_UPDATES");}
   try(Cursor c=db.rawQuery("SELECT genome_spec FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{parentId})){c.moveToFirst();parent=AlgorithmGenomeR908.parse(c.getString(0));}
   check(child.generation==parent.generation+1&&child.parentIds.equals(parentId)&&!child.structuralHash.equals(parent.structuralHash),"REAL_NEXT_GENERATION_PROGRAM_AND_PARENT_ID");
   check(!child.canonical().split("\\|update=")[1].equals(parent.canonical().split("\\|update=")[1]),"FAILURE_REPAIR_CHANGES_UPDATE_PROGRAM");
   check(plan.getInt("research_failure_labels")>=12&&plan.getString("evidence_role").contains("NOT_CERTIFICATE"),"ACTUAL_FAILURE_SUPPORT_AND_DIAGNOSTIC_BOUNDARY_REPORTED");
   check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"NO_CERTIFICATE_CREATED_FROM_DIAGNOSTIC_LABELS");
   DirectedResearchCreationR932.maintain(store,"algorithm");check(n(db,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT+" WHERE kind='PREDRAW_FAILURE_REPAIR'")==1,"RETRY_DOES_NOT_DUPLICATE_REPAIR_ATTEMPT");
   check(ResearchFailureEvolutionR965.diagnose(db,parent,"202609190001")==null,"FUTURE_TO_DIAGNOSTIC_CUTOFF_REJECTED");
   db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET proof_hash='tampered' WHERE candidate_key=?",new Object[]{"algorithm:"+parentId});
   check(ResearchFailureEvolutionR965.diagnose(db,parent,base)==null,"TAMPERED_PROOF_CANNOT_TRIGGER_RESEARCH_REPAIR");
   // Isolate the new program for continuation. These fixture changes do not describe phone state.
   db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET status='PRUNED_FIXTURE_ONLY' WHERE genome_id<>?",new Object[]{childId});
   db.execSQL("UPDATE "+ResearchLifecycleR932.TABLE+" SET state='QUALITY_PRUNED' WHERE candidate_key<>?",new Object[]{"algorithm:"+childId});
   int received=0;
   for(int i=0;i<20;i++) {
    String target=TargetIssueResolver.next(base).targetIssue;
    DeferredEvidenceSnapshotR36.Snapshot surface=ReportedCaptureHarnessR945.snapshot(base,target);byte[] blob=DeferredEvidenceSnapshotR36.encode(surface);
    db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue,job_type,status,created_at,updated_at,record_hash,snapshot_blob,snapshot_hash) VALUES(?,?,?,0,0,?,?,?)",new Object[]{target,"R965_SYNTHETIC_CHILD_CONTINUATION","PENDING",surface.recordHash,blob,DeferredEvidenceSnapshotR36.sha256(blob)});
    for(int retry=0;retry<20;retry++){try{System.out.println("CAPTURE|"+ResearchPredrawCaptureR923.captureTarget(store,target,1200).canonical());}catch(ResearchWorkBudgetR927.Yield y){}if(ResearchCaptureQueueR946.complete(store,target))break;}
    check(ResearchCaptureQueueR946.complete(store,target),"CHILD_FULL_CAPTURE_FINISHES:"+i);
    DevelopmentTrainingHarnessR959.draw(db,target,new Random(965+i));
    try(Cursor c=db.rawQuery("SELECT position,n1,n2,n3 FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE genome_id=? AND target_issue=?",new String[]{childId,target})) {
     if(c.moveToFirst()) {
      // Deliberately losing synthetic outcomes exercise the next failure-driven generation.
      int pos=c.getInt(0),actual=1;while(actual==c.getInt(1)||actual==c.getInt(2)||actual==c.getInt(3))actual++;
      ArrayList<Integer> nums=new ArrayList<Integer>();for(int k=1;k<=10;k++)nums.add(k);
      Collections.swap(nums,pos-1,actual-1);for(int k=1;k<=10;k++)db.execSQL("UPDATE draws SET n"+k+"=? WHERE issue=?",new Object[]{nums.get(k-1),target});received++;
     }
    }
    for(int retry=0;retry<6;retry++)ResearchPredrawSettlementPumpR908.drainBatch(store,32,1600);
    db.beginTransaction();try{ResearchLifecycleR932.review(db,"algorithm:"+childId,target);db.setTransactionSuccessful();}finally{db.endTransaction();}
    base=target;if(received>=12)break;
   }
   check(received>=12&&n(db,"SELECT future_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id='"+childId+"'")>=12,"CHILD_ACTUALLY_SCORES_SETTLES_AND_TRAINS_TWELVE_NEW_LABELS");
   System.out.println("CHILD_DIAGNOSE="+ResearchFailureEvolutionR965.diagnose(db,child,base)+"|CAL="+n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.OBS+" WHERE candidate_key='algorithm:"+childId+"' AND hit IN (0,1)"));
   result=DirectedResearchCreationR932.maintain(store,"algorithm");System.out.println("NEXT_CREATION="+result);
   check(result.startsWith("CREATED_N0|"),"FAILED_CHILD_CONTINUES_TO_NEXT_GENERATION");
   check(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE generation="+(child.generation+1))>=1,"THIRD_GENERATION_IS_A_REAL_NEW_PROGRAM");
   System.out.println("R965_REPORTED_FAILURE_PASS|checks="+checks+"|ACTUAL_REPORT_DIAGNOSTIC_THEN_SYNTHETIC_FUTURE_NOT_PHONE_QUALITY");
  }finally {db.shutdown();}
 }
}
