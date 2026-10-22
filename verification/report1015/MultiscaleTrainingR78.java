package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;import java.io.*;import java.util.*;import java.nio.file.*;
/** Real generated child registration, feature pages, numerical checkpoints and freeze.
 * Source draws are a historical report fixture; no natural Future or REAL claim. */
public final class MultiscaleTrainingR78 {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}
 static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){return c.moveToFirst()?c.getLong(0):0;}}
 public static void main(String[]a)throws Exception{
  try(SQLiteDatabase d=new SQLiteDatabase(a[0],a[1])){
   DbStore s=HostProductR974.store(d);File files=new File(a[2]);files.mkdirs();
   android.content.Context context=new android.app.Application(files,new File(a[3]));HostProductR974.set(s,"researchAppCP12",context);HostProductR974.set(s.coordinatorForResearchR952(),"app",context);HostProductR974.set(s,"researchInputRootCP11",new File(files,"research-input-v2"));s.researchInputRootCP11().mkdirs();RealtimePressureGateR811.resetForTests();
   CandidateAstR952 parent=CandidateAstR952.parse(Files.readString(Path.of(a[4])));
   List<AtomSpecCP4> registry=new ArrayList<>();try(Cursor c=d.rawQuery("SELECT atom_id,spec_hash,params_json,output_type,min_history,missing_policy,state FROM research_atom_spec_v2 WHERE position=?",new String[]{String.valueOf(parent.position)})){while(c.moveToNext())registry.add(AtomSpecCP4.fromRegistry(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getInt(4),c.getString(5),c.getString(6)));}
   CandidateAstR952 child=ModernFamilyFactoryCP12.newFamilyFromParentCP12(parent,CandidateAstR952.Family.NEURAL,registry,Collections.emptyList(),0);
   ok(parent.graphSpec.featureOutputIds.size()==1,"original report parent has one feature");
   ok(child.graphSpec.featureOutputIds.size()==7&&child.graphSpec.inputDimension==14,"seven explicit features plus masks");
   ok(child.graphSpec.manifest.requiredHistory==500,"longest window is declared");
   ok(child.graphSpec.featureOutputIds.contains(parent.graphSpec.featureOutputIds.get(0)),"parent feature preserved");
   ok(ModernFamilyFactoryCP12.newFamilyFromParentCP12(child,CandidateAstR952.Family.NEURAL,registry,Collections.emptyList(),0).structuralHash.equals(child.structuralHash),"same graph expansion is NO_OP, no duplicate nodes/features");
   String action=V51ChecksumEngine.sha256("EXPLICIT_HOST_MULTISCALE_ACTION"),finding=V51ChecksumEngine.sha256("EXPLICIT_HOST_MULTISCALE_FINDING"),evidence=V51ChecksumEngine.sha256("ORIGINAL_REPORT_FEATURE_MASK_FAILURE"),id="HOST-R78-"+child.structuralHash;
   String source=s.getLatestDrawIssue(),target=TargetIssueResolver.next(source).targetIssue;
   d.beginTransaction();try{
    d.execSQL("INSERT INTO research_finding_v2(finding_id,game_code,candidate_version,source_issue,episode_start_issue,problem_type,severity,evidence_hash,evidence_json,hypothesis,falsification_test,state,created_at,updated_at)VALUES(?,'PK10','host-seed',?,?,'FUTURE_DEFICIT','P1',?,'{}','MULTISCALE_FEATURE_SUPPORT','REPLAY_NEW_CHILD_WITHOUT_FUTURE','PLANNED',1,1)",new Object[]{finding,source,source,evidence});
    d.execSQL("INSERT INTO research_action_v2(action_id,finding_id,action_type,operator,proposal_json,evidence_hash,seed,parent_hashes_json,protocol_hash,planned_endpoint,alpha_identity,state,last_error)VALUES(?,?,'NEW_FAMILY','CREATE','{}',?,42,'[]','HOST_MULTISCALE_FEATURE_SUPPORT',360,'HOST_NOT_A_FORMAL_CLAIM','PROPOSED','')",new Object[]{action,finding,evidence});
    String boundary=V51ChecksumEngine.sha256("CP12_ACTION_BOUNDARY|"+action+'|'+source+'|'+target+'|'+evidence);
    d.execSQL("INSERT INTO research_action_input_boundary_cp12 VALUES(?,?,?,?,?,1)",new Object[]{action,source,target,evidence,boundary});d.setTransactionSuccessful();
   }finally{d.endTransaction();}
   try(DevelopmentProductionCP12.PreparedProtocol p=ResearchDirectorR952.prepareTrainingProtocolCP5(s,child,id,action,target)){
    ResearchDirectorR952.PreparedEvolution expected=new ResearchDirectorR952.PreparedEvolution(id,action,"","HOST_MULTISCALE_INTENT","",evidence);
    String task=s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("HOST_REGISTER_MULTISCALE",db->ResearchDirectorR952.commitPreparedCandidateCP5(db,p,expected));ok(task!=null&&!task.startsWith("NO_OP:"),"real child registered");
   }
   boolean done=false;for(int turn=0;turn<150&&!done;turn++){
    ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(850);
    try{
     ResearchTaskQueueR967.Lease lease=s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("HOST_CLAIM_MULTISCALE",db->ResearchTaskQueueR967.claimFor(db,"TRAIN_CANDIDATE",id,System.currentTimeMillis()));
     if(lease!=null){ResearchTrainingHookRegistryR952.DevelopmentBatchCP11 b=DevelopmentProductionCP12.prepareBatch(s,lease,System.nanoTime()+700_000_000L);if(b!=null)s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("HOST_COMMIT_MULTISCALE",db->ResearchDirectorR952.commitCandidateBatchCP11(db,lease,b));}
     try(Cursor c=d.rawQuery("SELECT status FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND candidate_id=?",new String[]{id})){done=c.moveToFirst()&&"COMPLETED".equals(c.getString(0));}
    }finally{ResearchWorkBudgetR927.leave(scope);}
    if(turn%10==0||done)System.out.println("MULTISCALE_TURN="+turn+" features="+n(d,"SELECT count(*) FROM research_training_feature_row_cp11 WHERE task_id IN(SELECT task_id FROM v57_research_task_r967 WHERE candidate_id='"+id+"')")+" done="+done);
    if(!done)Thread.sleep(270);
   }
   ok(done,"actual numerical training completed");
   ResearchTaskQueueR967.Lease freeze=s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("HOST_CLAIM_FREEZE",db->ResearchTaskQueueR967.claimFor(db,"FREEZE_CANDIDATE",id,System.currentTimeMillis()));ok(freeze!=null,"freeze task exists");
   try(FrozenCandidateAdapterR958.PreparedFreezeCP8 prepared=FrozenCandidateAdapterR958.prepareDevelopmentFreezeCP8(s,freeze,target)){String f=s.coordinatorForResearchR952().runAtomicWriteOrThrowF01("HOST_FREEZE",db->FrozenCandidateAdapterR958.commitDevelopmentFreezeCP8(db,freeze,prepared));ok(f!=null&&!f.isEmpty(),"actual freeze committed");}
   ok(n(d,"SELECT COUNT(*) FROM research_development_receipt_v2 p JOIN research_training_protocol_v2 t ON p.protocol_hash=t.protocol_hash WHERE t.candidate_id='"+id+"'")==1,"one real development receipt");
   ok(n(d,"SELECT future_n FROM r952_candidate WHERE candidate_id='"+id+"'")==0,"no Future fabricated");
   ok(n(d,"SELECT COUNT(*) FROM research_certificate_r929")==0&&n(d,"SELECT COUNT(*) FROM trades")==0,"no certificate or REAL fabricated");
   System.out.println("PASS MultiscaleTrainingR78 checks="+checks+" raw_report_draws=5000 features=7 input_dimension=14 train=1200 validation=120 frozen=1 scope=HOST_DEVELOPMENT_NOT_QUALITY");
  }
 }
}
