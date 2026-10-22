package com.ethk3.novaonepro;
import java.util.*;import android.database.*;import android.database.sqlite.*;import com.ethk3.novaonepro.r952.*;
/** Synthetic quality fixtures. Actual shared cert evaluator, selector, gate, DAO and SQL transactions.
 * Does not claim live future accuracy or Android startup/device verification. */
public final class ExactRealHarnessR974 {
 static int checks;static String h=V51ChecksumEngine.sha256("R974_SYNTHETIC_ONLY"),id,structure;static CertificateBundleServiceR929.Certificate cert;
 static void check(boolean b,String s){if(!b)throw new AssertionError(s);checks++;System.out.println("PASS "+s);}
 static long count(SQLiteDatabase db,String table){return DatabaseUtils.longForQuery(db,"SELECT COUNT(*) FROM "+table,null);}
 static void draw(SQLiteDatabase db,int n){Object[] v=new Object[12];v[0]=String.format(Locale.US,"20260920%04d",n);v[1]=1L;for(int j=0;j<10;j++)v[j+2]=(j+n)%10+1;db.execSQL("INSERT INTO draws(issue,open_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",v);}
 static CertificateBundleServiceR929.EvidenceInput input(double hit){return new CertificateBundleServiceR929.EvidenceInput(true,true,true,1000,1000,4,hit,.31,1.0,.22,.03,.01,.01,true,true,h);}
 static void fixtures(SQLiteDatabase db,DbStore store)throws Exception{
  String bundle=ModelBundleRuntimeR932.current().hash,schema=V51ActionMetaFeatureEngine.featureManifestHash(),code=SourceManifestR815.SOURCE_TREE_SHA256;
  db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_BUNDLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{bundle,"fixture","Binance1-PK10",schema,code,h,"TEST_KEY","RSA",1,6,100,"202609200100","202609200101","ACTIVE","SYNTHETIC_NOT_PRODUCTION_EVIDENCE",1,1,"",h});
  CandidateAstR952 ast=new CandidateAstR952(CandidateAstR952.Operator.POSITION_FREQUENCY,1,20);CandidateCompilerR952.Model m=CandidateCompilerR952.Model.initial();double[] x=new double[10];Arrays.fill(x,.1);for(int i=0;i<120;i++)m=m.update(x,i%10+1);
  structure=FrozenCandidateAdapterR958.structure(ast,m);id=FrozenCandidateAdapterR958.PREFIX+structure.substring(0,24);
  db.execSQL("INSERT INTO r958_frozen_candidate VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{id,"SYNTHETIC_LEARNER",ast.canonical,m.payload(),structure,m.hash(),"202609200100","202609200101",h,1});
  db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_TRIAL+"(trial_id,identity_hash,stage,status,dataset_hash,schema_hash,source_hash,hyper_hash,family_id,structural_hash,split_hash,seed,registered_at,finished_at,record_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{h,h,"EVALUATE","COMPLETED",h,schema,code,h,"TEST_FAMILY",structure,h,1,1,1,h});
  CertificateBundleServiceR929.Evaluation e=CertificateBundleServiceR929.evaluate(input(.34));check(CertificateBundleServiceR929.validatedInfluenceCap(e.gates)>0,"34_PERCENT_AT_BOUNDARY_VALID_QUALITY");
  check(CertificateBundleServiceR929.validatedInfluenceCap(CertificateBundleServiceR929.evaluate(input(.339)).gates)==0,"33_9_PERCENT_REJECTED");
  CertificateBundleServiceR929.CandidateKey key=new CertificateBundleServiceR929.CandidateKey(CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"TEST","TEST_FAMILY",id,structure);
  cert=new CertificateBundleServiceR929.Certificate(key,e.level,CertificateBundleServiceR929.CertificateState.ACTIVE,bundle,schema,AtomCaptureR932.graphHash(store),CertificateDependenciesR932.modelHash(),CertificateDependenciesR932.component("POLICY"),CertificateDependenciesR932.component("CALIBRATION"),DomainRouterR929.installedSchemaHash(),h,CertificateDependenciesR932.selectionHash(),code,"202609200102","202609200400",2,e.weightCap,e.gates,"SYNTHETIC_FIXTURE_NOT_LIVE_EVIDENCE");
  check(ResearchPlatformStoreR929.putCertificate(store,cert,h),"SHARED_CERTIFICATE_OWNER_PERSISTED");
  String broken="R958-F-"+h.substring(0,24);String badTrial=V51ChecksumEngine.sha256("BROKEN_EXECUTION_VALID_SYNTHETIC_QUALITY");
  db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_TRIAL+"(trial_id,identity_hash,stage,status,dataset_hash,schema_hash,source_hash,hyper_hash,family_id,structural_hash,split_hash,seed,registered_at,finished_at,record_hash) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{badTrial,badTrial,"EVALUATE","COMPLETED",h,schema,code,h,"TEST_FAMILY",h,h,1,1,1,badTrial});
  db.execSQL("INSERT INTO r958_frozen_candidate VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{broken,"BROKEN_FIXTURE",ast.canonical,"INVALID_MODEL_PAYLOAD",h,h,"202609200100","202609200101",h,1});
  CertificateBundleServiceR929.EvidenceInput betterInput=new CertificateBundleServiceR929.EvidenceInput(true,true,true,1000,1000,4,.44,.38,1.0,.22,.03,.01,.01,true,true,h);
  CertificateBundleServiceR929.Evaluation better=CertificateBundleServiceR929.evaluate(betterInput);
  CertificateBundleServiceR929.Certificate brokenCert=new CertificateBundleServiceR929.Certificate(new CertificateBundleServiceR929.CandidateKey(CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,"TEST","TEST_FAMILY",broken,h),better.level,CertificateBundleServiceR929.CertificateState.ACTIVE,bundle,schema,AtomCaptureR932.graphHash(store),CertificateDependenciesR932.modelHash(),CertificateDependenciesR932.component("POLICY"),CertificateDependenciesR932.component("CALIBRATION"),DomainRouterR929.installedSchemaHash(),badTrial,CertificateDependenciesR932.selectionHash(),code,"202609200102","202609200400",2,better.weightCap,better.gates,"SYNTHETIC_BROKEN_MODEL_SHOULD_SKIP");
  check(ResearchPlatformStoreR929.putCertificate(store,brokenCert,h),"HIGHER_RANKED_BAD_MODEL_SYNTHETIC_GATES_PERSISTED");

  store.setState(BuildIdentity.STATE_STATUS,BuildIdentity.STATUS_OK);
  check(BuildIdentity.isBetCompatible(store.getState(BuildIdentity.STATE_STATUS,"")),"BUILD_BOUNDARY_ALLOWS_CONTROLLED_GATE");
 }
 static V51Models.PositionAction[] actions(){ArrayList<V51Models.PositionAction> list=new ArrayList<>();for(int p=1;p<=10;p++)for(int a=1;a<=8;a++)for(int b=a+1;b<=9;b++)for(int c=b+1;c<=10;c++){
  int d=1;while(d==a||d==b||d==c)d++;list.add(new V51Models.PositionAction(p,a,b,c,d,.30,.30,0,0,.99,.1,new double[0],h,list.size()+1,V51ChecksumEngine.sha256(p+":"+a+":"+b+":"+c)));
 }return list.toArray(new V51Models.PositionAction[0]);}
 static V51RuntimeResult predict(DbStore store,int issue,boolean expired)throws Exception{
  String target=String.format(Locale.US,"20260920%04d",issue),base=String.format(Locale.US,"20260920%04d",issue-1);long now=System.currentTimeMillis();
  V51Models.PredictionContext c=new V51Models.PredictionContext(base,target,base,h,V51BuildManifest.COHORT_ID,ModelBundleRuntimeR932.current().hash,now,20);
  check(UnifiedResearchChoiceR932.freeze(store,target),"FROZEN_SELECTION_"+issue);
  System.out.println("AUTH="+ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,id,structure,target,.05).reason);
  CertifiedPredictionR974.Prepared p=CertifiedPredictionR974.prepare(store,c,actions(),new AnalysisSingleFlightR36.Request(base,target,1,now,expired?now-1:now+120000,"SYNTHETIC",h,h));
  if(expired){check(p==null,"EXPIRED_REQUEST_NO_PREDICTION");return null;}
  check(p!=null,"ACTUAL_CERTIFIED_MODEL_EXECUTED_"+issue);
  check(p.proof.candidate.equals(id),"BROKEN_HIGHER_RANK_FALLS_BACK_TO_EXECUTABLE_"+issue);
  V51Models.SelectedActionEnvelope s=new V51Models.SelectedActionEnvelope(c,p.rankOne,Double.NaN,null,null,V51Enums.Lane.CONTROLLED_PROBE,1,1,h);
  V51CompiledActionContract compiled=new V51CompiledActionContract(s,new V51SelectiveRiskController.Result(V51Enums.Lane.CONTROLLED_PROBE,V51Enums.RejectCode.NONE,null,true,h),null,h,h,h,h,h,p.proof);
  V51CandidateRealGateR915.Evaluation evaluation=V51CandidateRealGateR915.evaluate(store,compiled);check(evaluation.eligible,"ELIGIBLE_NO_LEGACY_ENTROPY_STABILITY_QUOTA_"+evaluation.code);
  V51Models.AuthorizedDecision authorized=new DecisionGateEngine().decideV51B12(store,compiled);check(authorized.action==V51Enums.DecisionAction.BET,"SOLE_GATE_AUTHORIZED_"+issue);
  return new V51RuntimeResult(true,c,null,null,null,null,null,p.actions,s,authorized,compiled,"SYNTHETIC_TEST",1,h);
 }
 static long commit(SQLiteDatabase db,DbStore store,V51RuntimeResult r){db.beginTransaction();try{long n=store.insertAuthorizedTradeWithinCanonicalR915(db,r,System.currentTimeMillis());db.setTransactionSuccessful();return n;}finally{db.endTransaction();}}
 public static void main(String[] args)throws Exception{
  SQLiteDatabase db=new SQLiteDatabase(args[0]);try{DbStore store=HostProductR974.store(db);HostProductR974.schema(db,store);for(int i=200;i<250;i++)draw(db,i);
   try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609200250",System.currentTimeMillis())){
    fixtures(db,store);V51RuntimeResult r=predict(store,250,false);long first=commit(db,store,r);check(first>0&&count(db,"trades")==1,"ACTUAL_TRADE_INSERTED");check(count(db,"r974_prediction_rejection")>0,"ACTUAL_FALLBACK_REASON_DURABLE");check(commit(db,store,r)==first&&count(db,"trades")==1,"DUPLICATE_EXACT_COMMIT_IDEMPOTENT");check(!V51CandidateRealGateR915.evaluate(store,r.contract).eligible,"DUPLICATE_EVENT_NO_SECOND_AUTHORIZATION");
   }
   draw(db,250);
   try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609200251",System.currentTimeMillis())){
    V51RuntimeResult r=predict(store,251,false);check(commit(db,store,r)>0&&count(db,"trades")==2,"CONSECUTIVE_QUALIFIED_ISSUES_WITH_OPEN_PREDECESSOR");check(count(db,"r974_exact_real_receipt")==2,"BOTH_REAL_RECEIPTS_ATOMIC");
   }
   draw(db,251);
   try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609200252",System.currentTimeMillis())){
    predict(store,252,true);V51RuntimeResult r=predict(store,252,false);
    db.execSQL("CREATE TRIGGER test_receipt_failure BEFORE INSERT ON r974_exact_real_receipt BEGIN SELECT RAISE(ABORT,'SYNTHETIC_CRASH_BEFORE_RECEIPT');END");
    try{commit(db,store,r);throw new AssertionError("CRASH_NOT_TRIGGERED");}catch(IllegalStateException expected){check(count(db,"trades")==2&&count(db,"r974_exact_real_receipt")==2,"CRASH_ROLLS_BACK_TRADE_AND_RECEIPT");}
    db.execSQL("DROP TRIGGER test_receipt_failure");check(commit(db,store,r)>0,"RETRY_AFTER_TRANSACTION_CRASH");
   }
   draw(db,252);
   try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609200253",System.currentTimeMillis())){
    V51RuntimeResult r=predict(store,253,false);check(ResearchPlatformStoreR929.revoke(store,cert.certificateHash,"SYNTHETIC_REVOCATION"),"CERTIFICATE_REVOKED");
    check(!V51CandidateRealGateR915.evaluate(store,r.contract).eligible,"REVOCATION_BLOCKS_OLD_PROOF");try{commit(db,store,r);throw new AssertionError("REVOKED_COMMITTED");}catch(IllegalStateException expected){check(count(db,"trades")==3,"COMMIT_REVALIDATES_CERTIFICATE");}
    draw(db,253);try{commit(db,store,r);throw new AssertionError("POSTDRAW_COMMITTED");}catch(IllegalStateException expected){check(count(db,"trades")==3,"POSTDRAW_COMMIT_REJECTED");}
   }
   try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609200254",System.currentTimeMillis())){
    check(UnifiedResearchChoiceR932.freeze(store,"202609200254"),"NO_QUALIFIED_SELECTION_FROZEN_EMPTY");long now=System.currentTimeMillis();
    V51Models.PredictionContext c=new V51Models.PredictionContext("202609200253","202609200254","202609200253",h,V51BuildManifest.COHORT_ID,ModelBundleRuntimeR932.current().hash,now,20);
    check(CertifiedPredictionR974.prepare(store,c,actions(),new AnalysisSingleFlightR36.Request(c.baseIssue,c.targetIssue,1,now,now+30000,"SYNTHETIC",h,h))==null,"NO_QUALIFIED_MODEL_NO_FAKE_PROOF");
    check(count(db,"trades")==3,"NO_QUALIFIED_MODEL_NO_NEW_REAL");
    check(!V51CandidateRealGateR915.evaluate(store,(V51Models.SelectedActionEnvelope)null,null).eligible,"OLD_RAW_SCORE_LANE_CANNOT_BYPASS_QUALITY_PROOF");
   }
   System.out.println("R974_EXACT_REAL_PASS|checks="+checks+"|synthetic_quality=true|actual_product_gate_writer=true|device=NOT_RUN");
  }finally{db.shutdown();}
 }
}
