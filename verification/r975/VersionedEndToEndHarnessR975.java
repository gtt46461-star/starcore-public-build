package com.ethk3.novaonepro;
import android.database.*;import android.database.sqlite.*;import java.nio.file.*;import java.util.*;import com.ethk3.novaonepro.r952.*;
import static com.ethk3.novaonepro.EndToEndHarnessR975.*;
/** Current V3 run with 120 actual predraw->settlement->numeric commits, then 601 disjoint
 * constant-permutation synthetic future issues. No manual frozen model/certificate rows. */
public final class VersionedEndToEndHarnessR975 {
 public static void main(String[] args)throws Exception{
  Path file=Path.of(args[1]);int first=Integer.parseInt(args[2]),last=Integer.parseInt(args[3]);if(first==321)Files.deleteIfExists(file);
  SQLiteDatabase db=new SQLiteDatabase(args[0],file.toString());try{DbStore store=HostProductR974.store(db);
   try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,issue(924),System.currentTimeMillis())){
    if(first==321){HostProductR974.schema(db,store);db.execSQL("DROP TABLE "+ResearchLifecycleR932.TABLE);tx(db,()->{ResearchLifecycleR932.schema(db);FixedResearchWindowR932.schema(db);FixedWindowCertificatePublisherR932.schema(db);ResearchPlatformStoreR929.activateRuntimeBundle(db,ModelBundleRuntimeR932.current(),issue(1),1);});
     db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT PRIMARY KEY,status TEXT,settlement_hash TEXT,completed_at INTEGER)");
     for(int i=1;i<=200;i++)draw(db,i);store.setState(BuildIdentity.STATE_STATUS,BuildIdentity.STATUS_OK);
     for(int i=201;i<=320;i++){
      if(ResearchDirectorR952.capturePredraw(store,issue(i),h,16)!=1)throw new AssertionError("WARM_CAPTURE_"+i);
      Models.Draw d=draw(db,i);d.openTime=System.currentTimeMillis()+1000;db.execSQL("UPDATE draws SET open_time=? WHERE issue=?",new Object[]{d.openTime,d.issue});String sh=V51ChecksumEngine.sha256("CONTROLLED_TERMINAL|"+d.issue);
      db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED',?,?)",new Object[]{d.issue,sh,d.openTime+1000});
      if(ResearchDirectorR952.onSettlementCompleted(store,d,sh,d.openTime+1000)!=1||!ResearchOutboxR952.drainOne(store)||!PostSettlementResearchLoopR952.drainOne(store))throw new AssertionError("WARM_REAL_NUMERIC_COMMIT_"+i);
     }
     check(n(db,"SELECT COUNT(*) FROM r952_artifact")==120,"V3_ACTUAL_PREQUENTIAL_TRAINING_COMMITS_120");
     check(n(db,"SELECT initial_updates FROM r967_numeric_training_run")==0,"V3_RUN_STARTED_ZERO_NO_WARM_COUNTER_INHERITANCE");
     check(FrozenCandidateAdapterR958.prepare(store,issue(321))==1,"ACTUAL_FREEZE_OWNER_CREATES_CURRENT_V3_FROZEN_MODEL");
    }
    id=text(db,"SELECT candidate_id FROM r967_frozen_evaluation_v2 ORDER BY created_issue LIMIT 1");FrozenCandidateAdapterR958.Frozen frozen=FrozenCandidateAdapterR958.load(db,id);if(frozen==null)throw new AssertionError("FROZEN_MODEL_MISSING");structure=frozen.structure;
    check(id.startsWith("R958-F-V3-"),"CURRENT_VERSIONED_V3_EVALUATION_DOMAIN");
    String key="algorithm:"+id;if(first==321)tx(db,()->ResearchMultiplicityR932.registerCandidate(db,key,"family:V3_CONTROLLED",structure,issue(321)));
    String bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash;
    ArrayList<Models.Draw> history=new ArrayList<>();for(int i=first-120;i<first;i++){Models.Draw d=new Models.Draw();d.issue=issue(i);for(int j=0;j<10;j++)d.numbers[j]=j+1;history.add(d);}
    for(int i=first;i<=last;i++){
     String target=issue(i),base=issue(i-1);FrozenCandidateAdapterR958.Scored scored=FrozenCandidateAdapterR958.score(frozen.ast,frozen.model,history,true);K3ActionSpaceEngine.ActionIdentity a=K3ActionSpaceEngine.enumerateIdentityUniverse()[scored.ordinal];String ph=V51ChecksumEngine.sha256("CONTROLLED_V3_CAPTURE|"+target+"|"+frozen.modelHash);
     tx(db,()->{db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{target,bundle,bundle,selector,1,2,3,4,V51ChecksumEngine.sha256(target+"CONTROLLED_CHAMPION"),1});scored.proof.freeze(db,key,structure,V51ChecksumEngine.sha256("CONTROLLED_INPUT|"+base),base,target,ph);ResearchCaptureLedgerR932.record(db,target,key,"FROZEN","ACTUAL_V3_MODEL_SYNTHETIC_ISSUES",ph,Math.max(1,scored.proof.elapsedNs/1000000),true);FixedResearchWindowR932.freezePair(db,key,structure,target,ph,a.position,a.n1,a.n2,a.n3,scored.scores[scored.ordinal]);});
     history.add(draw(db,i));history.remove(0);FixedResearchWindowR932.maintain(store);FixedResearchWindowR932.maintain(store);
     if((i-first)%40==0)System.out.println("issue="+target+"|cal="+n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)+"|windows="+n(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW));
    }
    System.out.println("V3_STAGE="+first+".."+last+"|PASS_WINDOWS="+n(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW+" WHERE state='PASS'"));
    if(last==921){check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"V3_NO_MANUALLY_SEEDED_CERTIFICATE");
     for(int j=0;j<20;j++)tx(db,()->FixedWindowCertificatePublisherR932.advance(db,issue(922)));
     check(n(db,"SELECT COUNT(*) FROM "+FixedWindowCertificatePublisherR932.TABLE+" WHERE state='PUBLISHED'")==2,"V3_BOTH_INDEPENDENT_WINDOWS_AUDITED_AND_PUBLISHED");
     check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE all_gates_pass=1 AND maximum_weight>0")==1,"V3_REAL_PUBLISHER_QUALIFIED_CERTIFICATE");
    }
   }
   if(last==921){for(int i=922;i<=923;i++){try(ModelBundleRuntimeR932.Scope later=ModelBundleRuntimeR932.open(null,issue(i),System.currentTimeMillis()+1)){V51RuntimeResult r=predict(store,i,false);long row=ExactRealHarnessR974.commit(db,store,r);check(row>0,"V3_END_TO_END_REAL_COMMITTED_"+i);check(ExactRealHarnessR974.commit(db,store,r)==row,"V3_DUPLICATE_IDEMPOTENT_"+i);}draw(db,i);}
    check(n(db,"SELECT COUNT(*) FROM trades")==2&&n(db,"SELECT COUNT(*) FROM r974_exact_real_receipt")==2,"V3_CONSECUTIVE_QUALIFIED_ISSUES_HAVE_ONE_REAL_EACH");
    check(n(db,"SELECT COUNT(*) FROM r952_artifact")==120,"V3_601_EVALUATION_LABELS_NOT_TRAINED_INTO_FROZEN_MODEL");
    System.out.println("R975_V3_TRAIN_TO_REAL_END_TO_END_PASS|checks_this_process="+checks+"|scope=120_TRAIN_PLUS_601_INDEPENDENT_SYNTHETIC_ISSUES|live=NOT_RUN");
   }
  }finally{db.shutdown();}
 }
}
