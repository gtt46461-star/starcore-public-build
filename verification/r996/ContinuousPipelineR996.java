package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import java.io.File;import java.util.*;
/** Runs the current full unmodified production dispatcher on consecutive synthetic periods. */
public final class ContinuousPipelineR996 {
 public static void main(String[] a)throws Exception{
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   DbStore store=HostProductR974.store(db);store.setState(BuildIdentity.STATE_STATUS,BuildIdentity.STATUS_OK);
   RetentionHeadroomR996.prepareAndPublish(db,new File(a[2]));
   java.lang.reflect.Field root=ModelBundleRuntimeR932.class.getDeclaredField("filesRoot");root.setAccessible(true);root.set(null,new File(a[2]));
   for(int period=0;period<3;period++){
    Models.Draw latest=store.getLatestDraw();String target=TargetIssueResolver.next(latest.issue).targetIssue;
    String fetch=FetchLedgerR925.begin(store,"GET","https://test.invalid/r996-synthetic-pipeline");ApiClient.RawHttpResult response=new ApiClient.RawHttpResult();response.ok=true;response.httpCode=200;response.body="EXPLICIT_SYNTHETIC_ACCEPTED_HISTORY_"+latest.issue;FetchLedgerR925.complete(store,fetch,1L,response);FetchLedgerR925.payload(store,fetch,"EXPLICIT_SYNTHETIC",latest.issue,true,"TEST_ONLY");
    AnalysisRequestStoreR36 requests=new AnalysisRequestStoreR36(store);AnalysisSingleFlightR36.Request request=requests.ensurePending(latest.issue,target,System.currentTimeMillis()+55000,"SYNTHETIC_CONTINUOUS_PIPELINE");V51RuntimeInputR36 input=V51RuntimeInputR36.freeze(store.getRecentDraws(240),request);request=request.withHistoryHash(input.historyHash);
    AnalysisSingleFlightR36.SubmitResult accepted=AnalysisSingleFlightR36.shared().submit(request);if(!accepted.startNow)throw new AssertionError("single owner");AnalysisSingleFlightR36.Lease lease=accepted.lease;lease.bindCurrentThread();RuntimeResourceAuditR927.Scope scope=null;long started=System.nanoTime();
    try{if(requests.claim(request,lease.ownerId,System.currentTimeMillis())==null)throw new AssertionError("durable claim");scope=RuntimeResourceAuditR927.begin(store,lease);
     V51RuntimeOrchestrator.OutcomeR36 dispatched=ProductionPredictionDispatcherR815.dispatch(store,input,request,lease);if(!lease.token.isCanonicalCommitted())throw new AssertionError("canonical not ready");
     Models.Decision decision=CanonicalDecisionViewAdapterR36.read(store,target);if(decision==null||!target.equals(decision.targetIssue))throw new AssertionError("UI canonical unavailable");
     if(AppConfig.DECISION_BET.equals(decision.decision))throw new AssertionError("fixture without certificates created REAL");
     System.out.println("PERIOD "+period+" target="+target+" frozenHistory="+input.history.size()+" milliseconds="+((System.nanoTime()-started)/1000000)+" status="+dispatched.runtime.status+" decision="+decision.decision+" numbers="+decision.n1+","+decision.n2+","+decision.n3);System.out.println(dispatched.trace.compactStagesR923());
    }finally{RuntimeResourceAuditR927.finish(store,scope,lease.token.isCanonicalCommitted(),"SYNTHETIC_CONTINUOUS_PIPELINE");AnalysisSingleFlightR36.shared().releaseAndTakeNext(lease);}
    Models.Draw drawn=new Models.Draw();drawn.issue=target;drawn.openTime=System.currentTimeMillis();drawn.fetchTime=drawn.openTime+1;drawn.dirty=0;for(int i=0;i<10;i++)drawn.numbers[i]=(i+period+1)%10+1;if(store.saveDraw(drawn)<=0)throw new AssertionError("draw owner");
    SettlementEngine.SettlementReport closed=new SettlementEngine().settleAfterDrawSaved(store,drawn);System.out.println("SETTLEMENT "+target+" "+closed.detail);if(store.getDrawCount()!=5000)throw new AssertionError("live tail exceeded 5000");
   }
   System.out.println("CONTINUOUS_PIPELINE_R996_PASS periods=3 databaseDraws="+store.getDrawCount()+" scope=ACTUAL_COMPLETE_PRODUCTION_DISPATCHER_100_NUMBERS_1200_ACTIONS_5000_DRAW_SYNTHETIC_HOST_NOT_DEVICE_NOT_WIN_RATE");
  }
 }
}
