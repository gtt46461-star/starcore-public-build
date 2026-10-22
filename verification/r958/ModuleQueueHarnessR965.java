package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import java.util.Random;
public final class ModuleQueueHarnessR965 {
 static int checks;static void check(boolean x,String label){if(!x)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
 static void snapshot(SQLiteDatabase db,String base,String target){DeferredEvidenceSnapshotR36.Snapshot s=ReportedCaptureHarnessR945.snapshot(base,target);byte[] blob=DeferredEvidenceSnapshotR36.encode(s);db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue,job_type,status,created_at,updated_at,record_hash,snapshot_blob,snapshot_hash) VALUES(?,?,?,0,0,?,?,?)",new Object[]{target,"QUEUE_NEGATIVE_FIXTURE","PENDING",s.recordHash,blob,DeferredEvidenceSnapshotR36.sha256(blob)});}
 public static void main(String[] args)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try {
   DbStore store=DevelopmentTrainingHarnessR959.init(db);snapshot(db,"202609190934","202609190935");snapshot(db,"202609200279","202609200280");
   DevelopmentTrainingHarnessR959.draw(db,"202609190935",new Random(1));DevelopmentTrainingHarnessR959.draw(db,"202609200280",new Random(2));
   store.setState("r932_ablation_cursor","202609190934");store.setState("r932_ablation_current","202609190935");store.setState("r932_ablation_module","3");
   ModuleAblationR932.maintain(store);
   check(store.getState("r932_ablation_cursor","").equals("202609200280"),"NEW_CODE_ANCHORS_CURRENT_DRAW_INSTEAD_OF_OLD_INCOMPATIBLE_BACKLOG");
   check(store.getState("r965_ablation_previous_cursor_LEGACY","").contains("202609190935")&&store.getState("r965_ablation_previous_cursor_LEGACY","").contains("NOT_REPLAYED"),"OLD_UNPROCESSED_CURSOR_ARCHIVED_NOT_CLAIMED_COMPLETE");
   check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+ModuleAblationR932.TABLE+" WHERE state='BLOCKED_INPUT_OR_PARITY'")==1,"MISSING_REPLAY_INPUT_STILL_REJECTED_AFTER_QUEUE_FIX");
   ModuleAblationR932.maintain(store);check(DevelopmentTrainingHarnessR959.n(db,"SELECT COUNT(*) FROM "+ModuleAblationR932.TABLE)==1,"SAME_SOURCE_RESTART_DOES_NOT_REANCHOR_OR_DUPLICATE");
   System.out.println("R965_MODULE_QUEUE_PASS|checks="+checks+"|QUEUE_BOUNDARY_NEGATIVE_INPUT_NOT_MODULE_QUALITY");
  }finally{db.shutdown();}
 }
}
