package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;import java.io.*;import java.util.*;
/** Original report terminal failure -> public research/evolution owners -> new train/freeze.
 * Explicit isolated fixture; never a phone, natural Future, or quality claim. */
public final class DevelopmentRepairR78 {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);System.out.println("PASS "+m);}
 static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static String text(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getString(0):"";}}
 static final class InterruptDb extends SQLiteDatabase {
  boolean armed;int injections;InterruptDb(String a,String b)throws Exception{super(a,b);}
  @Override public synchronized Cursor rawQuery(String q,String[]a){Cursor c=super.rawQuery(q,a);if(armed&&q.startsWith("SELECT row_index,target_issue,role,actual_number,feature_payload")){armed=false;injections++;RealtimePressureGateR811.beginAnalysis();}return c;}
 }
 static boolean research(DbStore s){ResearchWorkBudgetR927.Scope b=ResearchWorkBudgetR927.enter(850);try{return ResearchFeedbackCP12.drainOne(s,System.nanoTime()+750_000_000L);}finally{ResearchWorkBudgetR927.leave(b);}}
 public static void main(String[]a)throws Exception{
  try(InterruptDb d=new InterruptDb(a[0],a[1])){
   DbStore s=HostProductR974.store(d);File files=new File(a[2]);files.mkdirs();android.content.Context app=new android.app.Application(files,new File(a[3]));HostProductR974.set(s,"researchAppCP12",app);HostProductR974.set(s.coordinatorForResearchR952(),"app",app);HostProductR974.set(s,"researchInputRootCP11",new File(files,"research-input-v2"));s.researchInputRootCP11().mkdirs();RealtimePressureGateR811.resetForTests();
   String parent=text(d,"SELECT candidate_id FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE'AND status='FAILED_TERMINAL'"),source=text(d,"SELECT task_id FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE'AND status='FAILED_TERMINAL'"),original=text(d,"SELECT ast FROM r952_candidate WHERE candidate_id='"+parent+"'");
   ok(!parent.isEmpty()&&original.startsWith("AST3:"),"original reported failed parent exists");
   if(a.length>4&&a[4].equals("corrupt")){
    String trigger=text(d,"SELECT sql FROM sqlite_master WHERE name='cp12_feature_no_update'");d.execSQL("DROP TRIGGER cp12_feature_no_update");d.execSQL("UPDATE research_training_feature_row_cp11 SET feature_hash=? WHERE task_id=? AND row_index=0",new Object[]{V51ChecksumEngine.sha256("EXPLICIT_CORRUPTION_NEGATIVE_FIXTURE"),source});d.execSQL(trigger);
    ok(!research(s),"corrupt original feature never creates repair");ok(n(d,"SELECT count(*)FROM research_action_v2 WHERE operator='DEV_FEATURE_REPAIR_R78'")==0,"no action from corruption");ok(s.getState("r78_dev_repair_blocker","").contains("DEV_REPAIR_FEATURE_HASH"),"typed feature hash blocker preserved");return;
   }
   d.armed=true;boolean yielded=false;try{research(s);}catch(ResearchWorkBudgetR927.Yield expected){yielded=true;}finally{RealtimePressureGateR811.endAnalysis();}
   ok(yielded&&d.injections==1,"mid-diagnosis P0 preemption");ok(n(d,"SELECT count(*)FROM research_action_v2 WHERE operator='DEV_FEATURE_REPAIR_R78'")==0,"preempted diagnosis creates no partial proposal");
   ok(research(s),"public RESEARCH owner diagnoses original failed task");
   String action=text(d,"SELECT action_id FROM research_action_v2 WHERE operator='DEV_FEATURE_REPAIR_R78'");ok(!action.isEmpty(),"durable exact repair action exists");
   research(s);ok(n(d,"SELECT count(*)FROM research_action_v2 WHERE operator='DEV_FEATURE_REPAIR_R78'")==1,"repeated research cannot duplicate repair");
   String child="";for(int i=0;i<8&&child.isEmpty();i++){
    ResearchWorkBudgetR927.Scope b=ResearchWorkBudgetR927.enter(850);try{EvolutionControllerR952.drainActionCP12(s,System.nanoTime()+750_000_000L);}finally{ResearchWorkBudgetR927.leave(b);}
    child=text(d,"SELECT child_id FROM research_action_v2 WHERE operator='DEV_FEATURE_REPAIR_R78' AND state='WAIT_FUTURE'");if(child.isEmpty())Thread.sleep(1000);
   }
   ok(!child.isEmpty(),"public EVOLVE owner registers new child and train task");CandidateAstR952 ast=CandidateAstR952.parse(text(d,"SELECT ast FROM r952_candidate WHERE candidate_id='"+child+"'"));
   ok(ast.graphSpec.featureOutputIds.size()==7,"targeted new child has seven real features");ok(original.equals(text(d,"SELECT ast FROM r952_candidate WHERE candidate_id='"+parent+"'")),"failed parent AST immutable");
   ok("QUARANTINED".equals(text(d,"SELECT state FROM r952_candidate WHERE candidate_id='"+parent+"'")),"failed parent stays quarantined");ok("FAILED_TERMINAL".equals(text(d,"SELECT status FROM v57_research_task_r967 WHERE task_id='"+source+"'")),"original task failure is not erased");
   String cutoff=text(d,"SELECT validation_last FROM research_training_protocol_v2 WHERE candidate_id='"+parent+"'"),newCutoff=text(d,"SELECT validation_last FROM research_training_protocol_v2 WHERE candidate_id='"+child+"'");ok(cutoff.equals(newCutoff),"repair development cannot consume later outcomes");
   ok(n(d,"SELECT future_n FROM r952_candidate WHERE candidate_id='"+child+"'")==0,"new child inherits zero Future");
   final String id=child;boolean done=false;for(int turn=0;turn<150&&!done;turn++){
    ResearchWorkBudgetR927.Scope b=ResearchWorkBudgetR927.enter(850);try{
     ResearchTaskQueueR967.Lease lease=s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("HOST_REPAIR_TRAIN_CLAIM",db->ResearchTaskQueueR967.claimFor(db,"TRAIN_CANDIDATE",id,System.currentTimeMillis()));
     if(lease!=null){ResearchTrainingHookRegistryR952.DevelopmentBatchCP11 batch=DevelopmentProductionCP12.prepareBatch(s,lease,System.nanoTime()+700_000_000L);if(batch!=null)s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("HOST_REPAIR_TRAIN_COMMIT",db->ResearchDirectorR952.commitCandidateBatchCP11(db,lease,batch));}
     done="COMPLETED".equals(text(d,"SELECT status FROM v57_research_task_r967 WHERE candidate_id='"+id+"'AND kind='TRAIN_CANDIDATE'"));
    }finally{ResearchWorkBudgetR927.leave(b);}if(turn%10==0||done)System.out.println("REPAIR_TRAIN_TURN="+turn+" done="+done);if(!done)Thread.sleep(270);
   }ok(done,"independent repaired child numerical training completes");
   ResearchTaskQueueR967.Lease freeze=s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("HOST_REPAIR_FREEZE_CLAIM",db->ResearchTaskQueueR967.claimFor(db,"FREEZE_CANDIDATE",id,System.currentTimeMillis()));ok(freeze!=null,"repaired child freeze task exists");
   try(FrozenCandidateAdapterR958.PreparedFreezeCP8 p=FrozenCandidateAdapterR958.prepareDevelopmentFreezeCP8(s,freeze,TargetIssueResolver.next(s.getLatestDrawIssue()).targetIssue)){String result=s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("HOST_REPAIR_FREEZE_COMMIT",db->FrozenCandidateAdapterR958.commitDevelopmentFreezeCP8(db,freeze,p));ok(result!=null&&!result.isEmpty(),"repaired child actually frozen");}
   research(s);ok(n(d,"SELECT count(*)FROM research_result_v2 WHERE action_id='"+action+"'")==0,"missing prospective lifetime remains waiting, not fabricated");
   ok(n(d,"SELECT count(*)FROM research_certificate_r929")==0&&n(d,"SELECT count(*)FROM trades")==0,"repair creates no certificate or REAL authority");
   System.out.println("PASS DevelopmentRepairR78 checks="+checks+" parent="+parent+" child="+child+" original_terminal_preserved=TRUE scope=ORIGINAL_REPORT_DEV_HOST_NOT_PHONE_OR_FUTURE_QUALITY");
  }
 }
}
