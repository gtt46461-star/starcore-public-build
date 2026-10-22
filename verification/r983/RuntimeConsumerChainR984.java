package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
/** Isolated synthetic certificates; exercises actual runtime, gate, canonical DAO and settlement. */
public final class RuntimeConsumerChainR984 {
 static int checks;
 static void ok(boolean b,String why){checks++;if(!b)throw new AssertionError(why);System.out.println("PASS "+why);}
 static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){c.moveToFirst();return c.getLong(0);}}
 public static void main(String[] a)throws Exception{
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   DbStore store=HostProductR974.store(db);
   store.setState(BuildIdentity.STATE_STATUS,BuildIdentity.STATUS_OK);
   String target="202609230361",base="202609230360";
   String fetch=FetchLedgerR925.begin(store,"GET","https://test.invalid/explicit-synthetic-fixture");
   ok(!fetch.isEmpty(),"actual source request ledger");
   ApiClient.RawHttpResult response=new ApiClient.RawHttpResult();response.ok=true;response.httpCode=200;response.body="EXPLICIT_TEST_ONLY_ACCEPTED_HISTORY_360";
   FetchLedgerR925.complete(store,fetch,1L,response);
   FetchLedgerR925.payload(store,fetch,"EXPLICIT_TEST_ONLY_PROVIDER",base,true,"TEST_ONLY_ACCEPTED");
   ok(n(db,"SELECT COUNT(*) FROM v57_fetch_ledger_r925 WHERE status='BUSINESS_ACCEPTED' AND latest_issue='"+base+"'")==1,"actual accepted predraw source event");
   AnalysisRequestStoreR36 requests=new AnalysisRequestStoreR36(store);
   AnalysisSingleFlightR36.Request request=requests.ensurePending(base,target,System.currentTimeMillis()+120000,"EXPLICIT_TEST_ONLY_RUNTIME_CHAIN");
   ok(request!=null,"actual durable request");
   V51RuntimeInputR36 input=V51RuntimeInputR36.freeze(store.getDrawsAsc(5000),request);
   request=request.withHistoryHash(input.historyHash);
   AnalysisSingleFlightR36.SubmitResult submitted=AnalysisSingleFlightR36.shared().submit(request);
   ok(submitted.startNow,"single actual lease");
   AnalysisSingleFlightR36.Lease lease=submitted.lease;lease.bindCurrentThread();
   V51RuntimeOrchestrator.OutcomeR36 outcome;
   RuntimeResourceAuditR927.Scope resource=null;
   try{
    ok(requests.claim(request,lease.ownerId,System.currentTimeMillis())!=null,"durable owner claim");
    resource=RuntimeResourceAuditR927.begin(store,lease);
    outcome=new V51RuntimeOrchestrator().runR36(store,input,request,lease);
   }finally{RuntimeResourceAuditR927.finish(store,resource,lease.token.isCanonicalCommitted(),"EXPLICIT_TEST_ONLY_CHAIN");AnalysisSingleFlightR36.shared().releaseAndTakeNext(lease);}
   System.out.println("RUNTIME_OUTCOME "+outcome.runtime.status);
   ok(n(db,"SELECT COUNT(*) FROM r974_exact_real_receipt WHERE target_issue='"+target+"'")==1,"actual certified REAL receipt");
   ok(n(db,"SELECT COUNT(*) FROM trades WHERE target_issue='"+target+"' AND action='BET'")==1,"one REAL row");
   ok(n(db,"SELECT COUNT(*) FROM v57_shadow_observation_r35 WHERE target_issue='"+target+"' AND pre_draw_frozen=1")==1,"canonical predraw snapshot");
   try(Cursor c=db.rawQuery("SELECT snapshot_blob,snapshot_hash FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" WHERE target_issue=?",new String[]{target})){
    ok(c.moveToFirst(),"durable full action snapshot");
    DeferredEvidenceSnapshotR36.Snapshot full=DeferredEvidenceSnapshotR36.decodeCached(c.getBlob(0),c.getString(1));
    full.validate();ok(full.actions.size()==1200&&full.numbers.size()==100,"full 1200 action ranking and 100 number events");
    java.lang.reflect.Method m=Batch2GateEvidenceR926.class.getDeclaredMethod("certificateEvidence",SQLiteDatabase.class,DbStore.class,String.class,V51CompiledActionContract.class,DeferredEvidenceSnapshotR36.Header.class,String.class);m.setAccessible(true);
    try{m.invoke(null,db,store,target,null,full.header(),c.getString(1));throw new AssertionError("Missing exact proof accepted");}
    catch(java.lang.reflect.InvocationTargetException rejected){ok("B11_EXACT_CERTIFICATE_PROOF_MISSING".equals(rejected.getCause().getMessage()),"exact receipt cannot use legacy proof fallback");}
   }
   Models.Draw draw=new Models.Draw();draw.issue=target;draw.openTime=System.currentTimeMillis();draw.fetchTime=draw.openTime+1;
   for(int i=0;i<10;i++)draw.numbers[i]=i+1;
   ok(store.saveDraw(draw)>0,"actual history owner stores exact draw");
   SettlementEngine.SettlementReport settlement=new SettlementEngine().settleAfterDrawSaved(store,draw);
   System.out.println("SETTLEMENT "+settlement.detail);
   ok(n(db,"SELECT COUNT(*) FROM trades WHERE target_issue='"+target+"' AND settled=1")==1,"actual sole settlement completes REAL");
   ok(n(db,"SELECT COUNT(*) FROM v57_settlement_task_r36 WHERE target_issue='"+target+"' AND status='COMPLETED'")==1,"exact action OOS completes task");
   ok(n(db,"SELECT COUNT(*) FROM v57_oos_label_r35 WHERE target_issue='"+target+"' AND strict_oos=1 AND selected_hit_p=-1 AND brier=-1 AND hit_label IN(0,1)")==1,"unavailable calibrated probability remains unavailable");
   System.out.println("SIDECAR "+SettlementPostCommitSidecarR909.reconcileIssue(store,target));
   ResearchOrchestratorR906.Result feedback=ResearchOrchestratorR906.pulse(store,1800);
   System.out.println("RESEARCH_CONSUMER "+feedback.status);
   ok(n(db,"SELECT COUNT(*) FROM v57_settlement_outbox_r906 WHERE target_issue='"+target+"' AND event_type='RESEARCH_COMPACT_LABEL' AND status='DONE'")==1,"actual outbox downstream acknowledged");
   ok(n(db,"SELECT COUNT(*) FROM v57_research_compact_label_r906 WHERE target_issue='"+target+"' AND asset_id='PRODUCTION_SELECTED'")==1,"actual compact research label consumed");
   ResearchAdaptiveLearnerStoreR811.UpdateResult trained=ResearchAdaptiveLearnerStoreR811.updateFromStrictOos(store,target);
   System.out.println("LEARNER "+trained.status);
   ok(trained.updated,"actual settlement feedback updates saved model");
   ok(n(db,"SELECT COUNT(*) FROM v57_research_artifact_r811 WHERE trained_until_issue='"+target+"' AND artifact_type='FTRL_SHADOW'")==1,"one persisted trained artifact");
   new SettlementEngine().settleAllPending(store);
   ok(n(db,"SELECT COUNT(*) FROM trades WHERE target_issue='"+target+"'")==1,"settlement recovery does not duplicate REAL");
   String nextTarget="202609230362";
   String nextFetch=FetchLedgerR925.begin(store,"GET","https://test.invalid/explicit-synthetic-next-period");
   response.body="EXPLICIT_TEST_ONLY_ACCEPTED_HISTORY_361";
   FetchLedgerR925.complete(store,nextFetch,1L,response);FetchLedgerR925.payload(store,nextFetch,"EXPLICIT_TEST_ONLY_PROVIDER",target,true,"TEST_ONLY_NEXT_PERIOD");
   AnalysisSingleFlightR36.Request next=requests.ensurePending(target,nextTarget,System.currentTimeMillis()+120000,"EXPLICIT_TEST_ONLY_NEXT_PERIOD");
   V51RuntimeInputR36 nextInput=V51RuntimeInputR36.freeze(store.getDrawsAsc(5000),next);next=next.withHistoryHash(nextInput.historyHash);
   AnalysisSingleFlightR36.SubmitResult nextSubmit=AnalysisSingleFlightR36.shared().submit(next);ok(nextSubmit.startNow,"next period recovers sole runtime lease");
   AnalysisSingleFlightR36.Lease nextLease=nextSubmit.lease;nextLease.bindCurrentThread();
   RuntimeResourceAuditR927.Scope nextScope=null;
   try{ok(requests.claim(next,nextLease.ownerId,System.currentTimeMillis())!=null,"next period durable claim");
    nextScope=RuntimeResourceAuditR927.begin(store,nextLease);
    V51RuntimeOrchestrator.OutcomeR36 nextOutcome=new V51RuntimeOrchestrator().runR36(store,nextInput,next,nextLease);
    System.out.println("NEXT_PERIOD_RUNTIME "+nextOutcome.runtime.status);
   }finally{RuntimeResourceAuditR927.finish(store,nextScope,nextLease.token.isCanonicalCommitted(),"EXPLICIT_TEST_ONLY_NEXT_PERIOD");AnalysisSingleFlightR36.shared().releaseAndTakeNext(nextLease);}
   ok(n(db,"SELECT COUNT(*) FROM v57_shadow_observation_r35 WHERE target_issue='"+nextTarget+"' AND pre_draw_frozen=1")==1,"next period pipeline creates canonical predraw fact");
   ok(n(db,"SELECT COUNT(*) FROM trades WHERE target_issue='"+nextTarget+"' AND action='BET'")<=1,"next period maintains maximum one REAL");
   Models.Draw nextDraw=new Models.Draw();nextDraw.issue=nextTarget;nextDraw.openTime=System.currentTimeMillis();nextDraw.fetchTime=nextDraw.openTime+1;for(int i=0;i<10;i++)nextDraw.numbers[i]=(i+1)%10+1;
   ok(store.saveDraw(nextDraw)>0,"next-period actual draw owner");new SettlementEngine().settleAfterDrawSaved(store,nextDraw);
   String certificate;try(Cursor c=db.rawQuery("SELECT certificate_hash FROM r974_exact_real_receipt WHERE target_issue=?",new String[]{nextTarget})){ok(c.moveToFirst(),"next-period exact receipt available");certificate=c.getString(0);}
   ok(ResearchPlatformStoreR929.revoke(store,certificate,"EXPLICIT_TEST_ONLY_NO_CERTIFICATE_PERIOD"),"actual certificate revocation owner");
   String noBet=runFollowingPeriod(store,db,nextTarget,"202609230363");
   System.out.println("NO_CERTIFICATE_RUNTIME "+noBet);
   ok(n(db,"SELECT COUNT(*) FROM trades WHERE target_issue='202609230363' AND action='BET'")==0,"no certificate produces zero REAL");
   ok(n(db,"SELECT COUNT(*) FROM v57_shadow_observation_r35 WHERE target_issue='202609230363' AND pre_draw_frozen=1")==1,"no-certificate upstream pipeline still commits predraw fact");
   String reason=UnifiedResearchChoiceR932.currentChoiceReasonCP12(store,"202609230363");System.out.println("NO_CERTIFICATE_CHOICE_REASON "+reason);
   ok(reason.contains("NO_VALID_CERTIFICATE")&&reason.contains("preflight=EXECUTED")&&reason.contains("EXPLICIT_TEST_ONLY_NO_CERTIFICATE_PERIOD")&&reason.contains("missing=")&&reason.contains("recovery="),"precise durable revoked-certificate cause and recovery");
   try(Cursor c=db.rawQuery("SELECT governance_status FROM "+ControlledCandidateOpportunityAuditR922.TABLE+" WHERE target_issue='202609230363'",null)){ok(c.moveToFirst()&&c.getString(0).contains(reason),"actual canonical consumer persists original NO_BET cause");}
   System.out.println("RUNTIME_CONSUMER_CHAIN_PASS checks="+checks+" scope=SYNTHETIC_HOST_NOT_NATURAL_FUTURE_NOT_DEVICE");
  }
 }
 static String runFollowingPeriod(DbStore store,SQLiteDatabase db,String base,String target)throws Exception{
  String fetch=FetchLedgerR925.begin(store,"GET","https://test.invalid/explicit-synthetic-no-certificate");
  ApiClient.RawHttpResult response=new ApiClient.RawHttpResult();response.ok=true;response.httpCode=200;response.body="EXPLICIT_TEST_ONLY_NO_CERTIFICATE_SOURCE";
  FetchLedgerR925.complete(store,fetch,1L,response);FetchLedgerR925.payload(store,fetch,"EXPLICIT_TEST_ONLY_PROVIDER",base,true,"TEST_ONLY_NO_CERTIFICATE");
  AnalysisRequestStoreR36 requests=new AnalysisRequestStoreR36(store);
  AnalysisSingleFlightR36.Request request=requests.ensurePending(base,target,System.currentTimeMillis()+120000,"EXPLICIT_TEST_ONLY_NO_CERTIFICATE");
  V51RuntimeInputR36 input=V51RuntimeInputR36.freeze(store.getDrawsAsc(5000),request);request=request.withHistoryHash(input.historyHash);
  AnalysisSingleFlightR36.SubmitResult submitted=AnalysisSingleFlightR36.shared().submit(request);ok(submitted.startNow,"no-certificate actual runtime lease");
  AnalysisSingleFlightR36.Lease lease=submitted.lease;lease.bindCurrentThread();RuntimeResourceAuditR927.Scope scope=null;
  try{ok(requests.claim(request,lease.ownerId,System.currentTimeMillis())!=null,"no-certificate actual durable claim");scope=RuntimeResourceAuditR927.begin(store,lease);
   V51RuntimeOrchestrator.OutcomeR36 out=new V51RuntimeOrchestrator().runR36(store,input,request,lease);
   System.out.println("NO_CERTIFICATE_UPSTREAM_STAGES "+out.trace.compactStagesR923());return out.runtime.status;
  }finally{RuntimeResourceAuditR927.finish(store,scope,lease.token.isCanonicalCommitted(),"EXPLICIT_TEST_ONLY_NO_CERTIFICATE");AnalysisSingleFlightR36.shared().releaseAndTakeNext(lease);}
 }

}
