package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.List;

/** Nine/ten-layer post-settlement Research pipeline. Reads only frozen/settled evidence and never owns Production decisions. */
public final class ResearchOrchestratorR906 {
    public static final String OWNER="RESEARCH_P1_P2_ONLY";
    public static final boolean MAY_AUTHORIZE_REAL=false;
    private ResearchOrchestratorR906(){}

    public static Result pulse(DbStore store,long budgetMs) {
        ResearchWorkBudgetR927.Scope scopeR927=ResearchWorkBudgetR927.enter(budgetMs);
        try { return pulseWithinR927(store,budgetMs); }
        catch (ResearchWorkBudgetR927.Yield yielded) { return Result.fail(yielded.getMessage()); }
        finally { ResearchWorkBudgetR927.leave(scopeR927); }
    }

    public static Result pulseWithinR927(DbStore store,long budgetMs){
        if(store==null)return Result.fail("STORE_NULL");
        if(RealtimePressureGateR811.hasActiveCriticalWork()&&AnalysisSingleFlightR36.shared().activeCount()>0)return Result.fail("YIELD_REALTIME");
        long start=TimeSourceR811.monoNowMs(),deadline=start+Math.max(100L,Math.min(1800L,budgetMs));int compact=0,learned=0,created=0;
        String lastTarget="",lastError="";
        try{
            // Catch up chronologically in a bounded batch. One update per occasional pulse made a
            // healthy learner appear permanently NOT_STARTED after any backlog; each row remains
            // point-in-time ordered and the loop yields immediately to live P0 pressure.
            while(learned<8&&TimeSourceR811.monoNowMs()<deadline&&!RealtimePressureGateR811.hasActiveCriticalWork()){
            ResearchWorkBudgetR927.checkpoint();
                String trained=safe(store.getState("r811_ftrl_trained_until", ""));
                String target=ResearchAdaptiveLearnerStoreR811.nextTrainableIssue(store);
                if(target.length()==0)break;
                ResearchAdaptiveLearnerStoreR811.UpdateResult u=ResearchAdaptiveLearnerStoreR811.updateFromStrictOos(store,target);
                lastTarget=target;store.setState("r906_ftrl_compact_status",u.status);
                if(!u.updated)break;
                learned++;
            }
            while(TimeSourceR811.monoNowMs()<deadline){
            ResearchWorkBudgetR927.checkpoint();String target=nextOutbox(store,"RESEARCH_COMPACT_LABEL");if(target.length()==0)break;lastTarget=target;ResearchCompactLabelR906.Result r=ResearchCompactLabelR906.captureProduction(store,target);if(!r.success){markOutbox(store,"RESEARCH_COMPACT_LABEL",target,false,r.status);lastError=r.status;break;}markOutbox(store,"RESEARCH_COMPACT_LABEL",target,true,"");compact++;if(compact>=8)break;}
            if(TimeSourceR811.monoNowMs()<deadline){
                String creation=DirectedResearchCreationR932.maintain(store,"formula");
                created=creation.startsWith("CREATED_N0|")?1:0;
                store.setState("r932_formula_creation_status",creation);
            }
            int artifactJobs=TimeSourceR811.monoNowMs()<deadline?ResearchArtifactJobR906.ensureWaitingJobs(store,4):0;
            ResearchEvolutionGovernorR906.Summary gov=TimeSourceR811.monoNowMs()<deadline?ResearchEvolutionGovernorR906.govern(store):new ResearchEvolutionGovernorR906.Summary(0,0,0,0,"BUDGET_YIELD");
            String certification=(gov.challenger>0&&TimeSourceR811.monoNowMs()<deadline)?ResearchCertificateR906.tryIssuePreRegisteredCohort(store):"NOT_DUE";
            closeCompletedDeferredOutbox(store,8);
            String status="OK|compact="+compact+"|learned="+learned+"|created="+created+"|artifactJobs="+artifactJobs+"|certification="+certification+"|"+gov.canonical();
            store.setState("r906_research_orchestrator_status",status);store.setState("r906_research_orchestrator_at",String.valueOf(System.currentTimeMillis()));store.setState("r906_research_orchestrator_owner",OWNER);return new Result(true,status,lastTarget,compact,learned,created,TimeSourceR811.monoNowMs()-start);
        }catch(ResearchWorkBudgetR927.Yield yielded){return Result.fail(yielded.getMessage());}catch(OutOfMemoryError oom){RuntimeMemoryGuardR811.recoverAfterFailure();return Result.fail("OOM_RECOVERED");}catch(Throwable t){lastError=t.getClass().getSimpleName()+":"+safe(t.getMessage());try{store.setState("r906_research_orchestrator_error",lastError);}catch(Throwable ignore){}return Result.fail(lastError);}
    }

    private static FailurePortraitSnapshot portrait(DbStore store){int n=scalarInt(store,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1");int miss=scalarInt(store,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1 AND hit_label=0");SettlementCoverageTruthR906 cov=IssueClosureLedgerR906.coverage(store);boolean coverage=cov.predictedEligible>=8&&cov.settlementCoverage<0.90d;boolean rank=n>=8&&ratio(miss,n)>0.60d;boolean window="UNAVAILABLE".equals(safe(store.getState("r906_ui_window_consensus_status", "")))||ratio(miss,n)>0.65d;boolean relation=n>=16&&ratio(miss,n)>0.58d;String hash=V51ChecksumEngine.sha256("R906_FAILURE_PORTRAIT|n="+n+"|miss="+miss+"|coverage="+cov.canonical()+"|rank="+rank+"|window="+window+"|relation="+relation);return new FailurePortraitSnapshot(n,rank,window,relation,coverage,hash);}
    private static String nextOutbox(DbStore store,String type){Cursor c=null;try{c=store.readable().rawQuery("SELECT target_issue FROM "+DbHelper.TABLE_R906_SETTLEMENT_OUTBOX+" WHERE event_type=? AND status IN ('PENDING','RETRY_WAIT') AND next_attempt_at<=? ORDER BY priority DESC,created_at ASC LIMIT 1",new String[]{type,String.valueOf(System.currentTimeMillis())});return c.moveToFirst()?safe(c.getString(0)):"";}finally{if(c!=null)c.close();}}
    private static void markOutbox(final DbStore store,final String type,final String target,final boolean done,final String error){store.coordinatorR36().runWrite("R906_OUTBOX_RESULT",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){ContentValues v=new ContentValues();long now=System.currentTimeMillis();v.put("status",done?SettlementOutboxR906.DONE:SettlementOutboxR906.RETRY_WAIT);v.put("updated_at",now);v.put("completed_at",done?now:0L);v.put("last_progress_at",now);v.put("last_error_code",safe(error));if(!done)v.put("next_attempt_at",now+5000L);db.update(DbHelper.TABLE_R906_SETTLEMENT_OUTBOX,v,"event_type=? AND target_issue=?",new String[]{type,target});}});}
    private static void closeCompletedDeferredOutbox(final DbStore store,final int max){store.coordinatorR36().runWrite("R906_DEFERRED_OUTBOX_RECONCILE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){db.execSQL("UPDATE "+DbHelper.TABLE_R906_SETTLEMENT_OUTBOX+" SET status='DONE',completed_at=?,updated_at=?,last_progress_at=? WHERE event_type='DEFERRED_ACTION_EVIDENCE' AND status<>'DONE' AND target_issue IN (SELECT target_issue FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" WHERE status='COMPLETED' AND action_rows=1200 ORDER BY target_issue LIMIT "+Math.max(1,Math.min(64,max))+")",new Object[]{System.currentTimeMillis(),System.currentTimeMillis(),System.currentTimeMillis()});}});}
    private static int scalarInt(DbStore store,String sql){Cursor c=null;try{c=store.readable().rawQuery(sql,null);return c.moveToFirst()?c.getInt(0):0;}finally{if(c!=null)c.close();}}
    private static double ratio(int a,int b){return b<=0?0d:(double)a/(double)b;}private static int parseInt(String x){try{return Integer.parseInt(safe(x));}catch(Throwable t){return 0;}}private static String safe(String x){return x==null?"":x.trim();}
    private static final class FailurePortraitSnapshot{final int strictOosN;final boolean rankFailure,windowFailure,relationFailure,coverageFailure;final String hash;FailurePortraitSnapshot(int n,boolean r,boolean w,boolean rel,boolean cov,String h){strictOosN=n;rankFailure=r;windowFailure=w;relationFailure=rel;coverageFailure=cov;hash=h;}}
    public static final class Result{public final boolean success;public final String status,targetIssue;public final int compactLabels,learnerUpdates,createdAssets;public final long costMs;Result(boolean ok,String s,String t,int c,int l,int a,long ms){success=ok;status=safe(s);targetIssue=safe(t);compactLabels=c;learnerUpdates=l;createdAssets=a;costMs=ms;}static Result fail(String s){return new Result(false,s,"",0,0,0,0L);}}
}
