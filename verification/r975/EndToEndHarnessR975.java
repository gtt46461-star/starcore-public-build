package com.ethk3.novaonepro;
import android.database.*;import android.database.sqlite.*;import java.nio.file.*;import java.util.*;import com.ethk3.novaonepro.r952.*;
/** Controlled 601 future issues; constant permutation deliberately supplies a strong signal.
 * Actual numeric model scoring -> fixed calibration -> windows -> publisher -> sole REAL owner.
 * No fabricated certificate, trial or gate rows. NOT a live accuracy or profit test. */
public final class EndToEndHarnessR975 {
 static int checks;static String h=V51ChecksumEngine.sha256("R975_SYNTHETIC_CONSTANT_PERMUTATION_NOT_LIVE"),id,structure;
 static String issue(int i){return String.format(Locale.US,"20260920%04d",i);}
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
 static long n(SQLiteDatabase d,String sql){return DatabaseUtils.longForQuery(d,sql,null);}
 static String text(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
 static void tx(SQLiteDatabase d,Runnable action){d.beginTransaction();try{action.run();d.setTransactionSuccessful();}finally{d.endTransaction();}}
 static Models.Draw draw(SQLiteDatabase db,int i){Models.Draw d=new Models.Draw();d.issue=issue(i);for(int j=0;j<10;j++)d.numbers[j]=j+1;db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(?,1,2,3,4,5,6,7,8,9,10,0)",new Object[]{d.issue});return d;}
 static V51RuntimeResult predict(DbStore store,int i,boolean expired){String target=issue(i),base=issue(i-1);long now=System.currentTimeMillis();
  V51Models.PredictionContext c=new V51Models.PredictionContext(base,target,base,h,V51BuildManifest.COHORT_ID,ModelBundleRuntimeR932.current().hash,now,20);
  check(UnifiedResearchChoiceR932.freeze(store,target),"CURRENT_ISSUE_SELECTION_"+i);
  CertifiedPredictionR974.Prepared p=CertifiedPredictionR974.prepare(store,c,ExactRealHarnessR974.actions(),new AnalysisSingleFlightR36.Request(base,target,1,now,expired?now-1:now+120000,"CONTROLLED_END_TO_END",h,h));
  if(expired){check(p==null,"END_TO_END_EXPIRED_REJECTED");return null;}
  check(p!=null&&p.proof.candidate.equals(id),"PUBLISHER_CERT_EXECUTES_ACTUAL_MODEL_"+i);
  V51Models.SelectedActionEnvelope s=new V51Models.SelectedActionEnvelope(c,p.rankOne,Double.NaN,null,null,V51Enums.Lane.CONTROLLED_PROBE,1,1,h);
  V51CompiledActionContract compiled=new V51CompiledActionContract(s,new V51SelectiveRiskController.Result(V51Enums.Lane.CONTROLLED_PROBE,V51Enums.RejectCode.NONE,null,true,h),null,h,h,h,h,h,p.proof);
  V51Models.AuthorizedDecision authorized=new DecisionGateEngine().decideV51B12(store,compiled);check(authorized.action==V51Enums.DecisionAction.BET,"SOLE_GATE_USES_REAL_PUBLISHER_CERT_"+i);
  return new V51RuntimeResult(true,c,null,null,null,null,null,p.actions,s,authorized,compiled,"CONTROLLED_END_TO_END",1,h);
 }
 public static void main(String[] args)throws Exception{
  Path file=Path.of(args[1]);int first=Integer.parseInt(args[2]),last=Integer.parseInt(args[3]);if(first==201)Files.deleteIfExists(file);
  SQLiteDatabase db=new SQLiteDatabase(args[0],file.toString());try{DbStore store=HostProductR974.store(db);
   CandidateAstR952 ast=new CandidateAstR952(CandidateAstR952.Operator.POSITION_FREQUENCY,1,20);CandidateCompilerR952.Model model=CandidateCompilerR952.Model.initial();double[] x=new double[10];Arrays.fill(x,.01);x[0]=.91;for(int i=0;i<120;i++)model=model.update(x,1);
   structure=FrozenCandidateAdapterR958.structure(ast,model);id=FrozenCandidateAdapterR958.PREFIX+structure.substring(0,24);String key="algorithm:"+id;
   try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,issue(804),System.currentTimeMillis())){
    if(first==201){HostProductR974.schema(db,store);db.execSQL("DROP TABLE "+ResearchLifecycleR932.TABLE);tx(db,()->{ResearchLifecycleR932.schema(db);FixedResearchWindowR932.schema(db);FixedWindowCertificatePublisherR932.schema(db);ResearchPlatformStoreR929.activateRuntimeBundle(db,ModelBundleRuntimeR932.current(),issue(1),1);ResearchMultiplicityR932.registerCandidate(db,key,"family:CONTROLLED_E2E",structure,issue(201));});
     db.execSQL("INSERT INTO r958_frozen_candidate VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{id,"CONTROLLED_WARM_MODEL",ast.canonical,model.payload(),structure,model.hash(),issue(200),issue(201),h,1});for(int i=1;i<=200;i++)draw(db,i);store.setState(BuildIdentity.STATE_STATUS,BuildIdentity.STATUS_OK);
    }
    String bundle=ModelBundleRuntimeR932.current().hash,selector=SelectionEvidenceContractR929.installedPolicy().hash;
    ArrayList<Models.Draw> history=new ArrayList<>();for(int i=first-20;i<first;i++){Models.Draw d=new Models.Draw();d.issue=issue(i);for(int j=0;j<10;j++)d.numbers[j]=j+1;history.add(d);}
    for(int i=first;i<=last;i++){
     final String target=issue(i),base=issue(i-1);FrozenCandidateAdapterR958.Scored scored=FrozenCandidateAdapterR958.score(ast,model,history,true);K3ActionSpaceEngine.ActionIdentity a=K3ActionSpaceEngine.enumerateIdentityUniverse()[scored.ordinal];String ph=V51ChecksumEngine.sha256("CONTROLLED_CAPTURE|"+target+"|"+model.hash());
     tx(db,()->{db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{target,bundle,bundle,selector,1,2,3,4,V51ChecksumEngine.sha256(target+"CONTROLLED_CHAMPION"),1});scored.proof.freeze(db,key,structure,V51ChecksumEngine.sha256("CONTROLLED_INPUT|"+base),base,target,ph);ResearchCaptureLedgerR932.record(db,target,key,"FROZEN","ACTUAL_MODEL_SYNTHETIC_ISSUES",ph,Math.max(1,scored.proof.elapsedNs/1000000),true);FixedResearchWindowR932.freezePair(db,key,structure,target,ph,a.position,a.n1,a.n2,a.n3,scored.scores[scored.ordinal]);});
     history.add(draw(db,i));if(history.size()>120)history.remove(0);FixedResearchWindowR932.maintain(store);FixedResearchWindowR932.maintain(store);
     if((i-first)%40==0)System.out.println("issue="+target+"|cal="+n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)+"|windows="+n(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW));
    }
    System.out.println("STAGE="+first+".."+last+"|cal="+n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)+"|pass_windows="+n(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW+" WHERE state='PASS'"));
    if(last==801){check(n(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW+" WHERE state='PASS'")==2,"ACTUAL_MODEL_CALIBRATION_AND_TWO_INDEPENDENT_WINDOWS_PASS");check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"CERTIFICATES_NOT_SEEDED_OR_FABRICATED");
     for(int j=0;j<20;j++){tx(db,()->FixedWindowCertificatePublisherR932.advance(db,issue(802)));}
     check(n(db,"SELECT COUNT(*) FROM "+FixedWindowCertificatePublisherR932.TABLE+" WHERE state='PUBLISHED'")==2,"BOTH_WINDOWS_EXECUTION_AUDITED_BY_ACTUAL_PUBLISHER");
     check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE all_gates_pass=1 AND maximum_weight>0")==1,"INDEPENDENT_EVIDENCE_PRODUCES_QUALIFIED_CERTIFICATE");
    }
   }
   if(last==801){
    for(int i=802;i<=803;i++){try(ModelBundleRuntimeR932.Scope later=ModelBundleRuntimeR932.open(null,issue(i),System.currentTimeMillis()+1)){V51RuntimeResult r=predict(store,i,false);long row=ExactRealHarnessR974.commit(db,store,r);check(row>0,"END_TO_END_REAL_COMMITTED_"+i);check(ExactRealHarnessR974.commit(db,store,r)==row,"END_TO_END_DUPLICATE_IDEMPOTENT_"+i);}draw(db,i);}
    try(ModelBundleRuntimeR932.Scope later=ModelBundleRuntimeR932.open(null,issue(804),System.currentTimeMillis()+1)){predict(store,804,true);}
    check(n(db,"SELECT COUNT(*) FROM trades")==2&&n(db,"SELECT COUNT(*) FROM r974_exact_real_receipt")==2,"CONSECUTIVE_QUALIFIED_ISSUES_ONE_REAL_EACH_NO_QUOTA");
    System.out.println("R975_MODEL_TO_REAL_END_TO_END_PASS|checks="+checks+"|future_stream=SYNTHETIC_CONSTANT_PERMUTATION|device=NOT_RUN");
   }
  }finally{db.shutdown();}
 }
}
